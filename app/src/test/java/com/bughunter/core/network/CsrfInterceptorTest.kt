package com.bughunter.core.network

import com.google.common.truth.Truth.assertThat
import okhttp3.Cookie
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test

class CsrfInterceptorTest {

    private lateinit var server: MockWebServer
    private lateinit var jar: EncryptedCookieJar

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
        jar = EncryptedCookieJar(InMemoryCookiePersistence())
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun client(): OkHttpClient = OkHttpClient.Builder()
        .cookieJar(jar)
        .addInterceptor(CsrfInterceptor(jar))
        .build()

    private fun seedCsrf(url: HttpUrl, value: String = "csrf-token-1") {
        val cookie = Cookie.Builder()
            .name("bh_csrf")
            .value(value)
            .domain(url.host)
            .path("/")
            .expiresAt(System.currentTimeMillis() + 60_000)
            .build()
        jar.saveFromResponse(url, listOf(cookie))
    }

    @Test
    fun `does not set CSRF on GET`() {
        seedCsrf(server.url("/"))
        server.enqueue(MockResponse().setResponseCode(200).setBody("{}"))
        val req = Request.Builder().url(server.url("/api/bugs")).get().build()
        client().newCall(req).execute().close()
        val recorded = server.takeRequest()
        assertThat(recorded.getHeader("X-CSRF-Token")).isNull()
    }

    @Test
    fun `sets CSRF header on POST when cookie present`() {
        seedCsrf(server.url("/"))
        server.enqueue(MockResponse().setResponseCode(204))
        val req = Request.Builder()
            .url(server.url("/api/bugs"))
            .post("{}".toRequestBody())
            .build()
        client().newCall(req).execute().close()
        val recorded = server.takeRequest()
        assertThat(recorded.getHeader("X-CSRF-Token")).isEqualTo("csrf-token-1")
    }

    @Test
    fun `omits header on exempt login path`() {
        seedCsrf(server.url("/"))
        server.enqueue(MockResponse().setResponseCode(200).setBody("{}"))
        val req = Request.Builder()
            .url(server.url("/api/auth/login"))
            .post("{}".toRequestBody())
            .build()
        client().newCall(req).execute().close()
        val recorded = server.takeRequest()
        assertThat(recorded.getHeader("X-CSRF-Token")).isNull()
    }

    @Test
    fun `omits header on signup forgot reset accept`() {
        seedCsrf(server.url("/"))
        val paths = listOf(
            "/api/auth/signup",
            "/api/auth/forgot-password",
            "/api/auth/reset-password",
            "/api/invitations/accept",
        )
        paths.forEach { _ -> server.enqueue(MockResponse().setResponseCode(200).setBody("{}")) }
        for (p in paths) {
            val req = Request.Builder().url(server.url(p)).post("{}".toRequestBody()).build()
            client().newCall(req).execute().close()
            val recorded = server.takeRequest()
            assertThat(recorded.getHeader("X-CSRF-Token")).isNull()
        }
    }

    @Test
    fun `cookie absent triggers inline api health seed then attaches header on retry`() {
        // Cold-start path: no bh_csrf cookie in the jar. Interceptor must
        // do a one-shot GET /api/health, read the Set-Cookie the backend
        // returns, then attach X-CSRF-Token to the original POST. Without
        // this safety net, the POST 403s and the user sees a phantom
        // permission error.
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"ok":true}""")
                .addHeader("Set-Cookie", "bh_csrf=seed-from-health; Path=/"),
        )
        server.enqueue(MockResponse().setResponseCode(204))

        val req = Request.Builder().url(server.url("/api/bugs")).post("{}".toRequestBody()).build()
        client().newCall(req).execute().close()

        assertThat(server.requestCount).isEqualTo(2)
        val seed = server.takeRequest()
        val post = server.takeRequest()
        assertThat(seed.path).isEqualTo("/api/health")
        assertThat(seed.method).isEqualTo("GET")
        assertThat(post.path).isEqualTo("/api/bugs")
        assertThat(post.getHeader("X-CSRF-Token")).isEqualTo("seed-from-health")
    }

    @Test
    fun `cookie absent and seed fails to set cookie proceeds without header`() {
        // If even the /api/health probe doesn't issue a cookie (server
        // misconfig, network glitch), don't loop forever — proceed without
        // the header and let the resulting 403 bubble up so AuthInterceptor
        // (or the UI) can react.
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"ok":true}"""))
        server.enqueue(MockResponse().setResponseCode(403))

        val req = Request.Builder().url(server.url("/api/bugs")).post("{}".toRequestBody()).build()
        client().newCall(req).execute().close()

        assertThat(server.requestCount).isEqualTo(2)
        server.takeRequest() // /api/health
        val post = server.takeRequest()
        assertThat(post.getHeader("X-CSRF-Token")).isNull()
    }

    @Test
    fun `sets CSRF on PUT PATCH DELETE`() {
        seedCsrf(server.url("/"))
        repeat(3) { server.enqueue(MockResponse().setResponseCode(204)) }
        listOf("PUT", "PATCH", "DELETE").forEach { method ->
            val builder = Request.Builder().url(server.url("/api/bugs/1"))
            val req = when (method) {
                "PUT" -> builder.put("{}".toRequestBody()).build()
                "PATCH" -> builder.patch("{}".toRequestBody()).build()
                else -> builder.delete().build()
            }
            client().newCall(req).execute().close()
            val recorded = server.takeRequest()
            assertThat(recorded.getHeader("X-CSRF-Token")).isEqualTo("csrf-token-1")
        }
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

    @Suppress("unused")
    private fun unused(): Interceptor.Chain? = null
}
