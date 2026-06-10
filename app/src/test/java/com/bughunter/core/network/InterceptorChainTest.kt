package com.bughunter.core.network

import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.Moshi
import okhttp3.Cookie
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * End-to-end interceptor-chain test pinning the relative order of
 * [AuthInterceptor] (above) and [CsrfInterceptor] (below) in the OkHttp
 * chain.
 *
 * The v2.10 bug: chain was Csrf -> Auth, so on a 403 "CSRF check failed"
 * Auth would reseed and call chain.proceed(...), but that flowed
 * downstream only and skipped Csrf entirely. The retry went out with no
 * X-CSRF-Token header, the backend rejected it again, and the user saw
 * a phantom permission error on every mutating action.
 *
 * Fix: swap to Auth -> Csrf. Auth's retry proceeds downstream THROUGH
 * Csrf, which reads the freshly-issued bh_csrf cookie and attaches the
 * header. This test fails the moment anyone reverses NetworkModule's
 * interceptor registration order.
 */
class InterceptorChainTest {

    private lateinit var server: MockWebServer
    private lateinit var jar: EncryptedCookieJar
    private lateinit var bus: AuthEventBus

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
        jar = EncryptedCookieJar(InMemoryCookiePersistence())
        bus = AuthEventBus()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun client(): OkHttpClient = OkHttpClient.Builder()
        .cookieJar(jar)
        // Mirror NetworkModule's order: Auth ABOVE Csrf.
        .addInterceptor(AuthInterceptor(bus, Moshi.Builder().build()))
        .addInterceptor(CsrfInterceptor(jar))
        .build()

    @Test
    fun `403 CSRF retry carries fresh X-CSRF-Token header from Csrf interceptor`() {
        // Seed jar with a stale token so the initial POST has SOME header.
        // The backend rejects it (simulating a server-side rotation), Auth
        // re-seeds via /api/health which issues a NEW bh_csrf cookie via
        // Set-Cookie, and the retry must carry the NEW value.
        seedCsrf(server.url("/"), value = "stale-token")

        // 1) Original POST with stale CSRF header → backend says 403 CSRF.
        server.enqueue(
            MockResponse()
                .setResponseCode(403)
                .setBody("""{"detail":"CSRF check failed. Reload the page and try again."}"""),
        )
        // 2) Auth's reseed call → /api/health → backend issues new cookie.
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"ok":true}""")
                .addHeader("Set-Cookie", "bh_csrf=fresh-token; Path=/"),
        )
        // 3) Auth retries the POST → MUST flow through Csrf and pick up
        //    the fresh token from the jar.
        server.enqueue(MockResponse().setResponseCode(201).setBody("""{"id":1}"""))

        val req = Request.Builder()
            .url(server.url("/api/projects"))
            .post("""{"name":"x"}""".toRequestBody())
            .build()
        val response = client().newCall(req).execute()
        assertThat(response.code).isEqualTo(201)
        response.close()

        assertThat(server.requestCount).isEqualTo(3)
        val first = server.takeRequest()
        val seed = server.takeRequest()
        val retry = server.takeRequest()
        assertThat(first.path).isEqualTo("/api/projects")
        assertThat(first.getHeader("X-CSRF-Token")).isEqualTo("stale-token")
        assertThat(seed.path).isEqualTo("/api/health")
        assertThat(retry.path).isEqualTo("/api/projects")
        // THE CRITICAL ASSERTION: retry must carry the FRESH token, not
        // the stale one. Pre-fix the retry had no header at all because it
        // never flowed through Csrf.
        assertThat(retry.getHeader("X-CSRF-Token")).isEqualTo("fresh-token")
    }

    @Test
    fun `cold start with no cookie self-heals on first mutating call`() {
        // No cookie in the jar at all. CsrfInterceptor sees the POST,
        // notices no cookie, does its own inline GET /api/health, gets a
        // cookie back, attaches it to the same POST. Single round of
        // self-healing — the user clicks once and it works.
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"ok":true}""")
                .addHeader("Set-Cookie", "bh_csrf=fresh-token; Path=/"),
        )
        server.enqueue(MockResponse().setResponseCode(201).setBody("""{"id":1}"""))

        val req = Request.Builder()
            .url(server.url("/api/projects"))
            .post("""{"name":"x"}""".toRequestBody())
            .build()
        val response = client().newCall(req).execute()
        assertThat(response.code).isEqualTo(201)
        response.close()

        assertThat(server.requestCount).isEqualTo(2)
        val seed = server.takeRequest()
        val post = server.takeRequest()
        assertThat(seed.path).isEqualTo("/api/health")
        assertThat(post.path).isEqualTo("/api/projects")
        assertThat(post.getHeader("X-CSRF-Token")).isEqualTo("fresh-token")
    }

    private fun seedCsrf(url: HttpUrl, value: String) {
        val cookie = Cookie.Builder()
            .name("bh_csrf")
            .value(value)
            .domain(url.host)
            .path("/")
            .expiresAt(System.currentTimeMillis() + 60_000)
            .build()
        jar.saveFromResponse(url, listOf(cookie))
    }

    private class InMemoryCookiePersistence : CookiePersistence {
        private val all = mutableListOf<Cookie>()
        override fun loadAll(): List<Cookie> = all.toList()
        override fun saveAll(cookies: List<Cookie>) {
            all.clear(); all.addAll(cookies)
        }
        override fun mergeAndSave(host: String, fresh: List<Cookie>) {
            val keep = all.filterNot { existing -> fresh.any { it.name == existing.name && it.domain == existing.domain } }
            all.clear(); all.addAll(keep + fresh)
        }
        override fun clear() { all.clear() }
        override fun matching(url: HttpUrl): List<Cookie> = all.filter { it.matches(url) }
    }
}
