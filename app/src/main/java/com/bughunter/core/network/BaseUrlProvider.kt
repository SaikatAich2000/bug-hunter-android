package com.bughunter.core.network

import com.bughunter.BuildConfig
import com.bughunter.core.data.local.AppPrefs
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BaseUrlProvider @Inject constructor(
    private val appPrefs: AppPrefs,
) {
    val baseUrlFlow: Flow<String> = appPrefs.baseUrl

    fun currentBlocking(): String = runBlocking { appPrefs.baseUrl.first() }

    suspend fun current(): String = appPrefs.baseUrl.first()

    /**
     * Persist a new base URL. Scheme is validated here — not just in the
     * Settings UI — so nothing else (a future caller, a malformed pref
     * restore) can point the authenticated OkHttp client, cookie jar
     * attached, at a non-HTTP(S) or, in release, a cleartext host.
     * Release builds accept https only; debug additionally allows http
     * for emulator/dev servers (10.0.2.2 etc.).
     */
    suspend fun set(url: String) {
        val trimmed = url.trim()
        val isHttps = trimmed.startsWith("https://", ignoreCase = true)
        val isHttp = trimmed.startsWith("http://", ignoreCase = true)
        require(isHttps || (BuildConfig.DEBUG && isHttp)) {
            "Base URL must use https:// (http:// is allowed in debug builds only)"
        }
        val normalised = if (trimmed.endsWith("/")) trimmed else "$trimmed/"
        appPrefs.setBaseUrl(normalised)
    }

    companion object {
        // Mirrors AppPrefs.BASE_URL_DEFAULT — the URL baked in via the
        // bh.baseUrl Gradle property. Kept here so NetworkModule has a
        // synchronous fallback even before DataStore loads.
        val DEFAULT_BASE_URL: String = AppPrefs.BASE_URL_DEFAULT
    }
}
