package com.bughunter.core.network

import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Attaches X-CSRF-Token to every mutating request, sourced from the
 * bh_csrf cookie in the encrypted cookie jar (double-submit pattern).
 *
 * Sits below AuthInterceptor in the chain so a 403 → reseed → retry
 * cycle initiated by Auth flows back through this interceptor with the
 * freshly-issued cookie. See NetworkModule.provideOkHttpClient.
 */
@Singleton
internal class CsrfInterceptor @Inject constructor(
    private val cookieJar: EncryptedCookieJar,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val req = chain.request()
        val method = req.method
        if (method !in MUTATING_METHODS) return chain.proceed(req)

        val path = req.url.encodedPath
        if (path in EXEMPT_PATHS) return chain.proceed(req)

        // Cold-start safety net: if a mutating call happens before the
        // bh_csrf cookie was ever seeded (e.g., first action right after
        // install or after a process death that cleared in-memory state),
        // silently sending the request gets a 403 from the backend that
        // looks like a permission error to the user. Instead, do a
        // one-shot inline GET /api/health to seed the cookie, then re-read
        // and attach. If the seed still doesn't yield a token, we proceed
        // without the header and let the 403 → Auth.handle403 retry path
        // take over.
        val csrf = cookieJar.csrfToken() ?: seedAndRead(chain, req.url)
        if (csrf == null) return chain.proceed(req)

        val withHeader = req.newBuilder().header(CSRF_HEADER, csrf).build()
        return chain.proceed(withHeader)
    }

    private fun seedAndRead(chain: Interceptor.Chain, originalUrl: HttpUrl): String? {
        return try {
            val healthUrl = originalUrl.newBuilder()
                .encodedPath("/api/health")
                .query(null)
                .build()
            val seedReq = Request.Builder().url(healthUrl).get().build()
            chain.proceed(seedReq).close()
            cookieJar.csrfToken()
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        const val CSRF_HEADER = "X-CSRF-Token"
        private val MUTATING_METHODS = setOf("POST", "PUT", "PATCH", "DELETE")
        private val EXEMPT_PATHS = setOf(
            "/api/auth/login",
            "/api/auth/login/totp",
            "/api/auth/signup",
            "/api/auth/forgot-password",
            "/api/auth/reset-password",
            "/api/invitations/accept",
            "/api/health",
        )
    }
}
