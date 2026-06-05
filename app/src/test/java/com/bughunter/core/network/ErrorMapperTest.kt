package com.bughunter.core.network

import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.Moshi
import okhttp3.Headers
import org.junit.Before
import org.junit.Test
import java.time.Duration

class ErrorMapperTest {

    private lateinit var mapper: ErrorMapper

    @Before
    fun setUp() {
        mapper = ErrorMapper(Moshi.Builder().build())
    }

    @Test
    fun `401 maps to Unauthorized`() {
        val err = mapper.fromResponse(401, """{"detail":"Not authenticated"}""", Headers.headersOf())
        assertThat(err).isEqualTo(DomainError.Unauthorized)
    }

    @Test
    fun `403 maps to Forbidden`() {
        val err = mapper.fromResponse(403, """{"detail":"CSRF check failed."}""", Headers.headersOf())
        assertThat(err).isEqualTo(DomainError.Forbidden)
    }

    @Test
    fun `404 maps to NotFound`() {
        val err = mapper.fromResponse(404, """{"detail":"not found"}""", Headers.headersOf())
        assertThat(err).isEqualTo(DomainError.NotFound)
    }

    @Test
    fun `409 maps to Conflict`() {
        val err = mapper.fromResponse(409, """{"detail":"conflict"}""", Headers.headersOf())
        assertThat(err).isEqualTo(DomainError.Conflict)
    }

    @Test
    fun `422 maps to Validation with parsed field errors`() {
        val body = """
            {"detail":[
              {"loc":["body","email"],"msg":"invalid email","type":"value_error.email"},
              {"loc":["body","password"],"msg":"too short","type":"value_error.any_str.min_length"}
            ]}
        """.trimIndent()
        val err = mapper.fromResponse(422, body, Headers.headersOf())
        assertThat(err).isInstanceOf(DomainError.Validation::class.java)
        val v = err as DomainError.Validation
        assertThat(v.fieldErrors).hasSize(2)
        assertThat(v.fieldErrors[0].fieldName).isEqualTo("email")
        assertThat(v.fieldErrors[0].message).isEqualTo("invalid email")
        assertThat(v.fieldErrors[1].fieldName).isEqualTo("password")
    }

    @Test
    fun `429 maps to RateLimited and parses Retry-After seconds`() {
        val headers = Headers.headersOf("Retry-After", "30")
        val err = mapper.fromResponse(429, """{"detail":"too many"}""", headers)
        assertThat(err).isInstanceOf(DomainError.RateLimited::class.java)
        val r = err as DomainError.RateLimited
        assertThat(r.retryAfter).isEqualTo(Duration.ofSeconds(30))
    }

    @Test
    fun `429 with HTTP-date Retry-After parses approximately`() {
        // Date 24h in the future.
        val future = java.time.ZonedDateTime.now(java.time.ZoneOffset.UTC).plusHours(24)
        val raw = future.format(java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME)
        val headers = Headers.headersOf("Retry-After", raw)
        val err = mapper.fromResponse(429, """{"detail":"x"}""", headers)
        val r = err as DomainError.RateLimited
        assertThat(r.retryAfter).isNotNull()
        assertThat(r.retryAfter!!.toHours()).isAtLeast(23L)
    }

    @Test
    fun `500 maps to Server with message`() {
        val err = mapper.fromResponse(500, """{"detail":"boom"}""", Headers.headersOf())
        val s = err as DomainError.Server
        assertThat(s.message).isEqualTo("boom")
    }

    @Test
    fun `parses string detail envelope`() {
        val parsed = mapper.parse("""{"detail":"hello"}""")!!
        assertThat(parsed.message).isEqualTo("hello")
        assertThat(parsed.fieldErrors).isEmpty()
    }

    @Test
    fun `parses list detail envelope`() {
        val body = """{"detail":[{"loc":["body","x"],"msg":"bad","type":"value_error"}]}"""
        val parsed = mapper.parse(body)!!
        assertThat(parsed.message).isNull()
        assertThat(parsed.fieldErrors).hasSize(1)
        assertThat(parsed.fieldErrors[0].fieldName).isEqualTo("x")
    }
}
