package com.bughunter.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.bughunter.core.data.local.PushPrefs
import com.bughunter.core.network.Result2
import com.bughunter.core.network.api.DevicesApi
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Covers the registerIfChanged optimisation + the unregister/clear path.
 *
 * registerIfChanged is the cold-start optimisation that skips POSTing
 * when the cached token already matches. Without it we'd hammer the
 * server on every launch — important enough to lock in with a test.
 */
class DevicesRepositoryTest {

    private lateinit var server: MockWebServer
    private lateinit var api: DevicesApi
    private val moshi = RepoTestSupport.moshi()
    private val mapper = RepoTestSupport.errorMapper(moshi)
    private val jar = RepoTestSupport.cookieJar()

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
        // Pre-seed CSRF: device registration endpoints mutate (POST), so
        // without a cookie in the jar CsrfInterceptor would do an inline
        // GET /api/health that consumes one of our queued MockResponses.
        RepoTestSupport.seedCsrf(jar, server.url("/"))
        api = RepoTestSupport.retrofit(server, moshi, client = RepoTestSupport.client(jar))
            .create(DevicesApi::class.java)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `registerIfChanged posts on first call`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(REGISTER_BODY))
        val prefs = newPushPrefs()
        val repo = DevicesRepository(api, mapper, prefs)

        val result = repo.registerIfChanged(TOKEN)
        assertThat(result).isInstanceOf(Result2.Ok::class.java)
        assertThat(server.requestCount).isEqualTo(1)
        assertThat(prefs.lastRegisteredToken.first()).isEqualTo(TOKEN)
    }

    @Test
    fun `registerIfChanged skips POST when token unchanged`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(REGISTER_BODY))
        val prefs = newPushPrefs()
        val repo = DevicesRepository(api, mapper, prefs)
        repo.registerIfChanged(TOKEN)
        assertThat(server.requestCount).isEqualTo(1)

        // Second call should NOT hit the server.
        val result = repo.registerIfChanged(TOKEN)
        assertThat(result).isInstanceOf(Result2.Ok::class.java)
        assertThat(server.requestCount).isEqualTo(1)
    }

    @Test
    fun `registerIfChanged re-POSTs when token changes`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(REGISTER_BODY))
        server.enqueue(MockResponse().setResponseCode(200).setBody(REGISTER_BODY))
        val prefs = newPushPrefs()
        val repo = DevicesRepository(api, mapper, prefs)

        repo.registerIfChanged(TOKEN)
        repo.registerIfChanged(OTHER_TOKEN)

        assertThat(server.requestCount).isEqualTo(2)
        assertThat(prefs.lastRegisteredToken.first()).isEqualTo(OTHER_TOKEN)
    }

    @Test
    fun `unregister clears cached token even on server error`() = runTest {
        // Seed a cached token so we can confirm it gets cleared.
        val prefs = newPushPrefs()
        prefs.setLastRegisteredToken(TOKEN)
        server.enqueue(MockResponse().setResponseCode(500))
        val repo = DevicesRepository(api, mapper, prefs)

        val result = repo.unregister(TOKEN)
        // We don't care whether the network call succeeded for this
        // assertion — the cache must be cleared so a fresh login
        // re-registers from zero.
        assertThat(result).isInstanceOf(Result2.Err::class.java)
        assertThat(prefs.lastRegisteredToken.first()).isNull()
    }

    @Test
    fun `forceRegister always POSTs even when cache matches`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(REGISTER_BODY))
        server.enqueue(MockResponse().setResponseCode(200).setBody(REGISTER_BODY))
        val prefs = newPushPrefs()
        val repo = DevicesRepository(api, mapper, prefs)

        repo.forceRegister(TOKEN)
        repo.forceRegister(TOKEN)

        // Both calls must hit the server — onNewToken() flow can't be
        // skipped just because the cache happens to match.
        assertThat(server.requestCount).isEqualTo(2)
    }

    private fun newPushPrefs(): PushPrefs {
        val ctor = PushPrefs::class.java.getDeclaredConstructor(DataStore::class.java)
        ctor.isAccessible = true
        return ctor.newInstance(InMemoryDataStore())
    }

    @Suppress("UNUSED_PARAMETER")
    private fun unusedJarKeepImport(p: Preferences) = Unit

    companion object {
        // Synthetic FCM token strings used by the local MockWebServer.
        // Real tokens are opaque 150+ char strings; the substance doesn't
        // matter for these tests, only that registerIfChanged compares
        // equality.
        private const val TOKEN = "fake-fcm-token-1234567890"
        private const val OTHER_TOKEN = "fake-fcm-token-other-0987654321"

        private const val REGISTER_BODY = """
            {"id": 1, "platform": "android", "created_at": "2026-01-01T00:00:00Z"}
        """
    }
}
