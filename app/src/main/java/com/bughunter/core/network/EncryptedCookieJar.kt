package com.bughunter.core.network

import android.content.SharedPreferences
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import javax.inject.Inject
import javax.inject.Singleton

internal class EncryptedCookiePersistence(
    private val prefs: SharedPreferences,
) : CookiePersistence {

    private val lock = Any()

    override fun loadAll(): List<Cookie> = synchronized(lock) {
        val raw = prefs.getString(KEY_COOKIES, null) ?: return emptyList()
        if (raw.isEmpty()) return emptyList()
        return raw.split("\n").mapNotNull(::deserialize)
    }

    override fun saveAll(cookies: List<Cookie>) = synchronized(lock) {
        val joined = cookies.joinToString("\n", transform = ::serialize)
        prefs.edit().putString(KEY_COOKIES, joined).apply()
    }

    override fun mergeAndSave(host: String, fresh: List<Cookie>) = synchronized(lock) {
        val existing = loadAll()
        val freshNames = fresh.map { it.name to it.domain }.toSet()
        val kept = existing.filter { c ->
            !(c.name to c.domain in freshNames) && !c.isExpired()
        }
        val freshValid = fresh.filterNot { it.isExpired() }
        saveAll(kept + freshValid)
    }

    override fun clear() = synchronized(lock) {
        prefs.edit().remove(KEY_COOKIES).apply()
    }

    override fun matching(url: HttpUrl): List<Cookie> = synchronized(lock) {
        loadAll().filter { it.matches(url) && !it.isExpired() }
    }

    private fun serialize(c: Cookie): String =
        listOf(
            c.name,
            c.value,
            c.expiresAt.toString(),
            c.domain,
            c.path,
            c.secure.toString(),
            c.httpOnly.toString(),
            c.hostOnly.toString(),
        ).joinToString("|") { it.replace("|", "%7C").replace("\n", "%0A") }

    private fun deserialize(line: String): Cookie? {
        val parts = line.split("|").map { it.replace("%7C", "|").replace("%0A", "\n") }
        if (parts.size != 8) return null
        return try {
            val builder = Cookie.Builder()
                .name(parts[0])
                .value(parts[1])
                .expiresAt(parts[2].toLong())
                .path(parts[4])
            val builder1 = if (parts[7].toBoolean()) builder.hostOnlyDomain(parts[3]) else builder.domain(parts[3])
            val builder2 = if (parts[5].toBoolean()) builder1.secure() else builder1
            val builder3 = if (parts[6].toBoolean()) builder2.httpOnly() else builder2
            builder3.build()
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    private fun Cookie.isExpired(): Boolean = expiresAt < System.currentTimeMillis()

    private companion object {
        const val KEY_COOKIES = "bh_cookies_blob"
    }
}

@Singleton
internal class EncryptedCookieJar(
    private val persistence: CookiePersistence,
) : CookieJar {

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        if (cookies.isEmpty()) return
        persistence.mergeAndSave(url.host, cookies)
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> = persistence.matching(url)

    fun csrfToken(): String? = persistence.loadAll().firstOrNull { it.name == CSRF_COOKIE }?.value

    fun clear() = persistence.clear()

    companion object {
        const val SESSION_COOKIE = "bh_session"
        const val CSRF_COOKIE = "bh_csrf"
    }
}
