package com.bughunter.core.data.repository

import com.bughunter.core.network.CsrfInterceptor
import com.bughunter.core.network.CookiePersistence
import com.bughunter.core.network.EncryptedCookieJar
import com.bughunter.core.network.ErrorMapper
import com.bughunter.core.network.InstantAdapter
import com.bughunter.core.network.LocalDateAdapter
import com.bughunter.core.network.OmitNullJsonAdapterFactory
import com.bughunter.core.network.dto.ChatBlockAdapter
import com.bughunter.core.network.dto.LoginResponseAdapter
import com.bughunter.core.network.dto.registerEnumAdapters
import com.squareup.moshi.Moshi
import okhttp3.Cookie
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockWebServer
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

internal object RepoTestSupport {

    fun moshi(): Moshi {
        val builder = Moshi.Builder()
            .add(OmitNullJsonAdapterFactory())
            .add(InstantAdapter())
            .add(LocalDateAdapter())
        registerEnumAdapters(builder)
        val transitional = builder.build()
        // Wrap with polymorphic adapters that need the base Moshi for delegation.
        return transitional.newBuilder()
            .add(com.bughunter.core.network.dto.LoginResponse::class.java, LoginResponseAdapter(transitional))
            .add(com.bughunter.core.network.dto.ChatBlock::class.java, ChatBlockAdapter(transitional))
            .build()
    }

    fun errorMapper(moshi: Moshi): ErrorMapper = ErrorMapper(moshi)

    fun cookieJar(): EncryptedCookieJar = EncryptedCookieJar(InMemoryCookiePersistence())

    fun client(cookieJar: EncryptedCookieJar = cookieJar()): OkHttpClient = OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .addInterceptor(CsrfInterceptor(cookieJar))
        .build()

    /**
     * Client wired to [server] with a CSRF cookie pre-seeded in [cookieJar].
     *
     * Use this in tests that exercise mutating endpoints — otherwise
     * [CsrfInterceptor] (correctly) does an inline GET /api/health on
     * the first POST/PUT/PATCH/DELETE, which consumes a queued
     * MockResponse the test author didn't intend.
     */
    fun preseededClient(server: MockWebServer, cookieJar: EncryptedCookieJar = cookieJar()): OkHttpClient {
        seedCsrf(cookieJar, server.url("/"))
        return OkHttpClient.Builder()
            .cookieJar(cookieJar)
            .addInterceptor(CsrfInterceptor(cookieJar))
            .build()
    }

    fun retrofit(
        server: MockWebServer,
        moshi: Moshi = moshi(),
        // Default to a pre-seeded client so existing repo tests that
        // hit mutating endpoints don't have to add boilerplate. Tests
        // that need the cold-start "no cookie" path can build their own
        // client via [client] explicitly.
        client: OkHttpClient = preseededClient(server),
    ): Retrofit =
        Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

    fun seedCsrf(jar: EncryptedCookieJar, url: HttpUrl, value: String = "csrf-1") {
        val cookie = Cookie.Builder()
            .name("bh_csrf")
            .value(value)
            .domain(url.host)
            .path("/")
            .expiresAt(System.currentTimeMillis() + 60_000)
            .build()
        jar.saveFromResponse(url, listOf(cookie))
    }
}

internal class InMemoryCookiePersistence : CookiePersistence {
    private val all = mutableListOf<Cookie>()
    override fun loadAll(): List<Cookie> = all.toList()
    override fun saveAll(cookies: List<Cookie>) {
        all.clear(); all.addAll(cookies)
    }

    override fun mergeAndSave(host: String, fresh: List<Cookie>) {
        val keep = all.filterNot { existing -> fresh.any { it.name == existing.name && it.domain == existing.domain } }
        all.clear(); all.addAll(keep + fresh)
    }

    override fun clear() {
        all.clear()
    }

    override fun matching(url: HttpUrl): List<Cookie> = all.filter { it.matches(url) }
}
