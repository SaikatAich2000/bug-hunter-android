package com.bughunter.core.network

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import okhttp3.Headers
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.time.Duration
import java.time.Instant
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ErrorMapper @Inject constructor(
    moshi: Moshi,
) {
    private val mapAdapter: JsonAdapter<Map<String, Any?>> = moshi.adapter(
        Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java),
    )

    fun map(throwable: Throwable): DomainError = when (throwable) {
        is HttpException -> fromHttp(throwable.code(), readErrorBody(throwable.response()), throwable.response()?.headers())
        is IOException -> DomainError.Network
        else -> DomainError.Unknown(throwable)
    }

    fun fromResponse(code: Int, body: String?, headers: Headers?): DomainError =
        fromHttp(code, body, headers)

    private fun readErrorBody(response: Response<*>?): String? {
        val source = response?.errorBody()?.source() ?: return null
        return try {
            source.request(Long.MAX_VALUE)
            source.buffer.clone().readUtf8()
        } catch (_: Exception) {
            null
        }
    }

    private fun fromHttp(code: Int, body: String?, headers: Headers?): DomainError {
        val parsed = body?.let(::parse)
        return when (code) {
            401 -> DomainError.Unauthorized
            403 -> DomainError.Forbidden
            404 -> DomainError.NotFound
            409 -> DomainError.Conflict
            422 -> DomainError.Validation(parsed?.fieldErrors.orEmpty(), parsed?.message)
            429 -> DomainError.RateLimited(headers?.let(::parseRetryAfter))
            in 500..599 -> DomainError.Server(parsed?.message ?: "Server error ($code)")
            else -> {
                if (parsed?.fieldErrors?.isNotEmpty() == true) {
                    DomainError.Validation(parsed.fieldErrors, parsed.message)
                } else {
                    DomainError.Server(parsed?.message ?: "HTTP $code")
                }
            }
        }
    }

    internal fun parse(body: String): ParsedDetail? = try {
        if (body.isBlank()) {
            null
        } else {
            val map = mapAdapter.fromJson(body) ?: emptyMap()
            when (val detail = map["detail"]) {
                is String -> ParsedDetail(message = detail, fieldErrors = emptyList())
                is List<*> -> {
                    val fields = detail.mapNotNull { entry ->
                        if (entry !is Map<*, *>) return@mapNotNull null
                        val loc = (entry["loc"] as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
                        val msg = entry["msg"]?.toString() ?: return@mapNotNull null
                        val type = entry["type"]?.toString() ?: "validation_error"
                        FieldError(location = loc, message = msg, type = type)
                    }
                    ParsedDetail(message = null, fieldErrors = fields)
                }
                else -> null
            }
        }
    } catch (_: Exception) {
        null
    }

    internal data class ParsedDetail(
        val message: String?,
        val fieldErrors: List<FieldError>,
    )

    companion object {
        fun parseRetryAfter(headers: Headers): Duration? {
            val raw = headers["Retry-After"] ?: return null
            raw.toLongOrNull()?.let { return Duration.ofSeconds(it) }
            return try {
                val zdt = ZonedDateTime.parse(raw, DateTimeFormatter.RFC_1123_DATE_TIME)
                Duration.between(Instant.now(), zdt.toInstant())
            } catch (_: Exception) {
                null
            }
        }
    }
}
