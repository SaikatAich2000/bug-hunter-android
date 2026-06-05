package com.bughunter.core.network

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

    suspend fun set(url: String) {
        val normalised = if (url.endsWith("/")) url else "$url/"
        appPrefs.setBaseUrl(normalised)
    }

    companion object {
        // Mirrors AppPrefs.BASE_URL_DEFAULT — the URL baked in via the
        // bh.baseUrl Gradle property. Kept here so NetworkModule has a
        // synchronous fallback even before DataStore loads.
        val DEFAULT_BASE_URL: String = AppPrefs.BASE_URL_DEFAULT
    }
}
