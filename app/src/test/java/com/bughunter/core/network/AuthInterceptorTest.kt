package com.bughunter.core.network

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
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
    fun `401 on non-login path emits logged out`() = runBlocking {
        // Subscribe BEFORE the network call. AuthEventBus uses replay=0,
        // so a subscriber registered after the emit would miss the event
        // (previous test fired the call first, then subscribed — race lost
        // the event 100% of the time, the suite just happened to retry).
        server.enqueue(MockResponse().setResponseCode(401).setBody("""{"detail":"Not authenticated"}"""))
        val req = Request.Builder().url(server.url("/api/bugs")).get().build()

        bus.events.test {
            launch(Dispatchers.IO) {
                client().newCall(req).execute().close()
            }
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
    fun `403 CSRF check failed triggers re-seed via api health and retry`() = runTest {
        server.enqueue(MockResponse().setResponseCode(403).setBody("""{"detail":"CSRF check failed. Reload the page and try again."}"""))
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"ok":true}""")) // /api/health reseed
        server.enqueue(MockResponse().setResponseCode(204))                            // retry

        val req = Request.Builder().url(server.url("/api/bugs")).post("{}".toRequestBody()).build()
        val response = client().newCall(req).execute()
        assertThat(response.code).isEqualTo(204)
        response.close()

        assertThat(server.requestCount).isEqualTo(3)
        // The reseed MUST target /api/health — that is the one non-HTML
        // GET on which the backend issues Set-Cookie: bh_csrf=...
        // (see app/csrf.py). Targeting /api/auth/me (the previous bug)
        // was a no-op for cookie issuance, so the retry would always
        // 403 again.
        val first = server.takeRequest()
        val second = server.takeRequest()
        assertThat(first.path).isEqualTo("/api/bugs")
        assertThat(second.path).isEqualTo("/api/health")
    }

    @Test
    fun `429 with Retry-After seconds emits LockedOut`() = runBlocking {
        // Same subscribe-first pattern as the 401 test — see comment there.
        server.enqueue(MockResponse().setResponseCode(429).setHeader("Retry-After", "60"))
        val req = Request.Builder().url(server.url("/api/auth/login")).post("{}".toRequestBody()).build()

        bus.events.test {
            launch(Dispatchers.IO) {
                client().newCall(req).execute().close()
            }
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
        assertThat(recorded.getHeader("User-Agent")).isEqualTo("Bug Hunter Android/2.9.0")
        assertThat(recorded.getHeader("Accept")).isEqualTo("application/json")
    }
}
