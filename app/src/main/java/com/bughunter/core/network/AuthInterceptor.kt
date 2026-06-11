package com.bughunter.core.network

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import okhttp3.Headers
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import okio.Buffer
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class AuthEventBus @Inject constructor() {
    private val flow = MutableSharedFlow<AuthEvent>(
        replay = 0,
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val events: SharedFlow<AuthEvent> = flow.asSharedFlow()

    fun emit(event: AuthEvent) {
        flow.tryEmit(event)
    }
}

@Singleton
internal class AuthInterceptor @Inject constructor(
    private val authEventBus: AuthEventBus,
    moshi: Moshi,
) : Interceptor {

    private val detailAdapter: JsonAdapter<Map<String, Any?>> = moshi.adapter(
        Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java),
    )

    @Volatile
    private var reseedInFlight: Boolean = false

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)
        val path = request.url.encodedPath

        return when (response.code) {
            401 -> handle401(response, path)
            403 -> handle403(chain, request, response)
            429 -> handle429(response)
            else -> response
        }
    }

    private fun handle401(response: Response, path: String): Response {
        if (path !in AUTOLOGOUT_IGNORED_PATHS) {
            val detail = peekDetail(response)
            if (isSessionLoss(detail)) {
                authEventBus.emit(AuthEvent.LoggedOut(AuthEvent.LoggedOut.Reason.ServerExpired))
            }
        }
        return response
    }

    private fun handle403(
        chain: Interceptor.Chain,
        request: Request,
        response: Response,
    ): Response {
        val detail = peekDetail(response)
        if (detail?.startsWith("CSRF check failed") == true && !reseedInFlight) {
            response.close()
            reseedInFlight = true
            try {
                val reseed = chain.proceed(buildSeedRequest(request.url))
                reseed.close()
                authEventBus.emit(AuthEvent.CsrfReseedNeeded)
                // The retry must flow back through CsrfInterceptor so the
                // freshly-issued bh_csrf cookie is read into the
                // X-CSRF-Token header. NetworkModule keeps Auth above Csrf
                // in the chain specifically so chain.proceed() here hits
                // Csrf on the way down — see NetworkModule.provideOkHttpClient.
                return chain.proceed(request.newBuilder().build())
            } finally {
                reseedInFlight = false
            }
        }
        return response
    }

    private fun handle429(response: Response): Response {
        val until = parseRetryAfter(response.headers)?.let { Instant.now().plus(it) }
        if (until != null) authEventBus.emit(AuthEvent.LockedOut(until))
        return response
    }

    private fun buildSeedRequest(originalUrl: HttpUrl): Request {
        // Must target /api/health — that is the ONE non-HTML GET on which
        // the backend's CSRFMiddleware emits a Set-Cookie: bh_csrf=...
        // header (see app/csrf.py). The previous /api/auth/me target was a
        // no-op for cookie issuance, so the retry would always 403 again.
        val seedUrl = originalUrl.newBuilder()
            .encodedPath("/api/health")
            .query(null)
            .build()
        return Request.Builder().url(seedUrl).get().build()
    }

    private fun peekDetail(response: Response): String? = try {
        val source = response.peekBody(MAX_PEEK_BYTES).source()
        val buffer = Buffer().apply { source.readAll(this) }
        val reader = JsonReader.of(buffer)
        val parsed = detailAdapter.fromJson(reader)
        when (val raw = parsed?.get("detail")) {
            is String -> raw
            is List<*> -> raw.joinToString("; ") { it?.toString().orEmpty() }
            else -> null
        }
    } catch (_: Exception) {
        null
    }

    private fun isSessionLoss(detail: String?): Boolean {
        if (detail == null) return true
        if (detail.equals("Invalid login.", ignoreCase = true)) return true
        if (detail.startsWith("Not authenticated", ignoreCase = true)) return true
        if (detail.startsWith("Invalid email or password", ignoreCase = true)) return false
        return true
    }

    companion object {
        private const val MAX_PEEK_BYTES: Long = 4096
        private val AUTOLOGOUT_IGNORED_PATHS = setOf(
            "/api/auth/login",
            "/api/auth/login/totp",
            "/api/auth/signup",
            "/api/auth/forgot-password",
            "/api/auth/reset-password",
            "/api/invitations/accept",
            "/api/invitations/preview",
        )

        internal fun parseRetryAfter(headers: Headers): java.time.Duration? {
            val raw = headers["Retry-After"] ?: return null
            val seconds = raw.toLongOrNull()
            if (seconds != null) return java.time.Duration.ofSeconds(seconds)
            return try {
                val zdt = java.time.ZonedDateTime.parse(raw, java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME)
                java.time.Duration.between(Instant.now(), zdt.toInstant())
            } catch (_: Exception) {
                null
            }
        }
    }
}
