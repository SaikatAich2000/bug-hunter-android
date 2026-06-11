package com.bughunter.core.data.repository

import androidx.datastore.core.DataStore
import com.bughunter.core.data.local.AppPrefs
import com.bughunter.core.data.local.AuthPrefs
import com.bughunter.core.network.api.AuthApi
import com.bughunter.core.network.api.MetaApi
import com.bughunter.core.network.api.TotpApi
import com.bughunter.core.push.PushTokenSync
import com.bughunter.feature.auth.AuthStateHolder
import com.squareup.moshi.Moshi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import okhttp3.mockwebserver.MockWebServer

/**
 * Builds a fully-wired [AuthRepository] against a [MockWebServer] for
 * ViewModel tests that depend on it. The DataStore-backed prefs are
 * swapped for an in-memory implementation (via the same reflection trick
 * AuthRepositoryTest uses), push sync is a no-op, and a fresh CSRF cookie
 * is pre-seeded so mutating endpoints don't trigger an inline /api/health.
 */
internal object AuthRepoTestFactory {

    fun create(server: MockWebServer, moshi: Moshi = RepoTestSupport.moshi()): AuthRepository {
        val mapper = RepoTestSupport.errorMapper(moshi)
        val jar = RepoTestSupport.cookieJar()
        RepoTestSupport.seedCsrf(jar, server.url("/"))
        val retrofit = RepoTestSupport.retrofit(server, moshi, client = RepoTestSupport.client(jar))
        return AuthRepository(
            authApi = retrofit.create(AuthApi::class.java),
            totpApi = retrofit.create(TotpApi::class.java),
            metaApi = retrofit.create(MetaApi::class.java),
            errorMapper = mapper,
            authPrefs = newAuthPrefs(),
            appPrefs = newAppPrefs(),
            cookieJar = jar,
            stateHolder = AuthStateHolder(),
            pushTokenSyncer = PushTokenSync.Noop,
            pushScope = CoroutineScope(SupervisorJob()),
            moshi = moshi,
        )
    }

    /** A fresh in-memory [AppPrefs] for ViewModels that depend on it directly. */
    fun appPrefs(): AppPrefs = newAppPrefs()

    private fun newAuthPrefs(): AuthPrefs {
        val ctor = AuthPrefs::class.java.getDeclaredConstructor(DataStore::class.java)
        ctor.isAccessible = true
        return ctor.newInstance(InMemoryDataStore())
    }

    private fun newAppPrefs(): AppPrefs {
        val ctor = AppPrefs::class.java.getDeclaredConstructor(DataStore::class.java)
        ctor.isAccessible = true
        return ctor.newInstance(InMemoryDataStore())
    }
}
