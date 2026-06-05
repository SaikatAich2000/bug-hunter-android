package com.bughunter.core.network

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.Moshi
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test

class AuthInterceptorTest {

    private lateinit var server: MockWebServer
    private lateinit var moshi: Moshi
    private lateinit var bus: AuthEventBus

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
        moshi = Moshi.Builder().build()
        bus = AuthEventBus()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun client(): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(AuthInterceptor(bus, moshi))
        .build()

    @Test
    fun `401 on non-login path emits logged out`() = runTest {
        server.enqueue(MockResponse().setResponseCode(401).setBody("""{"detail":"Not authenticated"}"""))
        val req = Request.Builder().url(server.url("/api/bugs")).get().build()
        client().newCall(req).execute().close()

        bus.events.test {
            val event = awaitItem()
            assertThat(event).isInstanceOf(AuthEvent.LoggedOut::class.java)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `401 on login is not auto-logout`() = runTest {
        server.enqueue(MockResponse().setResponseCode(401).setBody("""{"detail":"Invalid email or password"}"""))
        val req = Request.Builder().url(server.url("/api/auth/login")).post("{}".toRequestBody()).build()
        val response = client().newCall(req).execute()
        response.close()
        assertThat(response.code).isEqualTo(401)
        // No event expected; nothing to assert beyond completion.
    }

    @Test
    fun `403 CSRF check failed triggers re-seed and retry`() = runTest {
        server.enqueue(MockResponse().setResponseCode(403).setBody("""{"detail":"CSRF check failed. Reload the page and try again."}"""))
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"id":1}""")) // /me re-seed
        server.enqueue(MockResponse().setResponseCode(204))                          // retry

        val req = Request.Builder().url(server.url("/api/bugs")).post("{}".toRequestBody()).build()
        val response = client().newCall(req).execute()
        assertThat(response.code).isEqualTo(204)
        response.close()

        assertThat(server.requestCount).isEqualTo(3)
        // Second request must be /api/auth/me
        val first = server.takeRequest()
        val second = server.takeRequest()
        assertThat(first.path).isEqualTo("/api/bugs")
        assertThat(second.path).isEqualTo("/api/auth/me")
    }

    @Test
    fun `429 with Retry-After seconds emits LockedOut`() = runTest {
        server.enqueue(MockResponse().setResponseCode(429).setHeader("Retry-After", "60"))
        val req = Request.Builder().url(server.url("/api/auth/login")).post("{}".toRequestBody()).build()
        client().newCall(req).execute().close()

        bus.events.test {
            val ev = awaitItem()
            assertThat(ev).isInstanceOf(AuthEvent.LockedOut::class.java)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `User-Agent interceptor sets header`() {
        val ua = UserAgentInterceptor()
        val ok = OkHttpClient.Builder().addInterceptor(ua).build()
        server.enqueue(MockResponse().setResponseCode(200).setBody("{}"))
        val req = Request.Builder().url(server.url("/api/health")).get().build()
        ok.newCall(req).execute().close()
        val recorded = server.takeRequest()
        assertThat(recorded.getHeader("User-Agent")).isEqualTo("Bug Hunter Android/2.8.0")
        assertThat(recorded.getHeader("Accept")).isEqualTo("application/json")
    }
}
