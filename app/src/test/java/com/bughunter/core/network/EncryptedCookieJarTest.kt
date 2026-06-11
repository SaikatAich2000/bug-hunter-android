package com.bughunter.core.network

import com.google.common.truth.Truth.assertThat
import okhttp3.Cookie
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Test

/**
 * In-memory [CookiePersistence] mirroring the production semantics:
 * host/path scoping via [Cookie.matches] and expiry filtering, so the
 * jar's delegated behavior (scoping, merge, expiry) is exercised here.
 */
private class FakePersistence : CookiePersistence {
    private val all = mutableListOf<Cookie>()

    private fun Cookie.isExpired(): Boolean = expiresAt < System.currentTimeMillis()

    override fun loadAll(): List<Cookie> = all.toList()

    override fun saveAll(cookies: List<Cookie>) {
        all.clear(); all.addAll(cookies)
    }

    override fun mergeAndSave(host: String, fresh: List<Cookie>) {
        val freshKeys = fresh.map { it.name to it.domain }.toSet()
        val kept = all.filter { (it.name to it.domain) !in freshKeys && !it.isExpired() }
        val freshValid = fresh.filterNot { it.isExpired() }
        all.clear(); all.addAll(kept + freshValid)
    }

    override fun clear() {
        all.clear()
    }

    override fun matching(url: HttpUrl): List<Cookie> =
        all.filter { it.matches(url) && !it.isExpired() }
}

class EncryptedCookieJarTest {

    private val url: HttpUrl = "https://example.com/".toHttpUrl()

    private fun cookie(
        name: String,
        value: String,
        host: String = url.host,
        path: String = "/",
        expiresAt: Long = System.currentTimeMillis() + 60_000L,
    ): Cookie = Cookie.Builder()
        .name(name)
        .value(value)
        .domain(host)
        .path(path)
        .expiresAt(expiresAt)
        .build()

    @Test
    fun saveThenLoad_returnsCookie() {
        val jar = EncryptedCookieJar(FakePersistence())
        val c = cookie("a", "1")

        jar.saveFromResponse(url, listOf(c))
        val loaded = jar.loadForRequest(url)

        assertThat(loaded).hasSize(1)
        assertThat(loaded[0].name).isEqualTo("a")
        assertThat(loaded[0].value).isEqualTo("1")
    }

    @Test
    fun saveFromResponse_emptyList_isNoOp() {
        val jar = EncryptedCookieJar(FakePersistence())

        jar.saveFromResponse(url, emptyList())

        assertThat(jar.loadForRequest(url)).isEmpty()
    }

    @Test
    fun hostScoping_cookieForHostA_notReturnedForHostB() {
        val jar = EncryptedCookieJar(FakePersistence())
        val urlA = "https://a.example.com/".toHttpUrl()
        val urlB = "https://b.example.com/".toHttpUrl()
        val cA = cookie("scoped", "fromA", host = urlA.host)

        jar.saveFromResponse(urlA, listOf(cA))

        assertThat(jar.loadForRequest(urlA)).hasSize(1)
        assertThat(jar.loadForRequest(urlB)).isEmpty()
    }

    @Test
    fun merge_sameName_replacesOldValue() {
        val jar = EncryptedCookieJar(FakePersistence())

        jar.saveFromResponse(url, listOf(cookie("session", "old")))
        jar.saveFromResponse(url, listOf(cookie("session", "new")))

        val loaded = jar.loadForRequest(url)
        assertThat(loaded).hasSize(1)
        assertThat(loaded[0].value).isEqualTo("new")
    }

    @Test
    fun merge_differentNames_keepsBoth() {
        val jar = EncryptedCookieJar(FakePersistence())

        jar.saveFromResponse(url, listOf(cookie("a", "1")))
        jar.saveFromResponse(url, listOf(cookie("b", "2")))

        val names = jar.loadForRequest(url).map { it.name }
        assertThat(names).containsExactly("a", "b")
    }

    @Test
    fun expiredCookie_notReturnedOnLoad() {
        val jar = EncryptedCookieJar(FakePersistence())
        val expired = cookie("stale", "x", expiresAt = System.currentTimeMillis() - 60_000L)

        jar.saveFromResponse(url, listOf(expired))

        assertThat(jar.loadForRequest(url)).isEmpty()
    }

    @Test
    fun expiredCookie_droppedDuringMerge() {
        val jar = EncryptedCookieJar(FakePersistence())
        // Seed a valid cookie, then merge in an expired one with a different name.
        jar.saveFromResponse(url, listOf(cookie("live", "1")))
        jar.saveFromResponse(
            url,
            listOf(cookie("dead", "2", expiresAt = System.currentTimeMillis() - 1_000L)),
        )

        val names = jar.loadForRequest(url).map { it.name }
        assertThat(names).containsExactly("live")
    }

    @Test
    fun clear_emptiesEverything() {
        val jar = EncryptedCookieJar(FakePersistence())
        jar.saveFromResponse(url, listOf(cookie("a", "1"), cookie("b", "2")))

        jar.clear()

        assertThat(jar.loadForRequest(url)).isEmpty()
    }

    @Test
    fun csrfToken_returnsValueOfCsrfCookie() {
        val jar = EncryptedCookieJar(FakePersistence())
        jar.saveFromResponse(
            url,
            listOf(cookie(EncryptedCookieJar.CSRF_COOKIE, "csrf-123")),
        )

        assertThat(jar.csrfToken()).isEqualTo("csrf-123")
    }

    @Test
    fun csrfToken_nullWhenAbsent() {
        val jar = EncryptedCookieJar(FakePersistence())
        jar.saveFromResponse(url, listOf(cookie(EncryptedCookieJar.SESSION_COOKIE, "s")))

        assertThat(jar.csrfToken()).isNull()
    }

    @Test
    fun csrfToken_nullWhenJarEmpty() {
        val jar = EncryptedCookieJar(FakePersistence())

        assertThat(jar.csrfToken()).isNull()
    }
}
