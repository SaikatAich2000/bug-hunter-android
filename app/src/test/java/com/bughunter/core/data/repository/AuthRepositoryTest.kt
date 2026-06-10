package com.bughunter.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import com.bughunter.core.data.local.AppPrefs
import com.bughunter.core.data.local.AuthPrefs
import com.bughunter.core.network.Result2
import com.bughunter.core.network.api.AuthApi
import com.bughunter.core.network.api.MetaApi
import com.bughunter.core.network.api.TotpApi
import com.bughunter.core.network.dto.LoginIn
import com.bughunter.core.network.dto.LoginResponse
import com.bughunter.core.push.PushTokenSync
import com.bughunter.feature.auth.AuthStateHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test

class AuthRepositoryTest {

    private lateinit var server: MockWebServer
    private lateinit var authApi: AuthApi
    private lateinit var totpApi: TotpApi
    private lateinit var metaApi: MetaApi
    private val moshi = RepoTestSupport.moshi()
    private val mapper = RepoTestSupport.errorMapper(moshi)
    private val jar = RepoTestSupport.cookieJar()

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
        val retrofit = RepoTestSupport.retrofit(server, moshi, client = RepoTestSupport.client(jar))
        authApi = retrofit.create(AuthApi::class.java)
        totpApi = retrofit.create(TotpApi::class.java)
        metaApi = retrofit.create(MetaApi::class.java)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun buildRepo(): AuthRepository = AuthRepository(
        authApi = authApi,
        totpApi = totpApi,
        metaApi = metaApi,
        errorMapper = mapper,
        authPrefs = newAuthPrefs(),
        appPrefs = newAppPrefs(),
        cookieJar = jar,
        stateHolder = AuthStateHolder(),
        pushTokenSyncer = PushTokenSync.Noop,
        pushScope = CoroutineScope(SupervisorJob()),
        moshi = moshi,
    )

    @Test
    fun `login returns AwaitingTotp variant and updates state`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200)
                .setBody("""{"pending_2fa":true,"pending_token":"opaque-1"}"""),
        )
        val repo = buildRepo()
        val result = repo.login(LoginIn(email = "a@b.c", password = "p"))
        assertThat(result).isInstanceOf(Result2.Ok::class.java)
        assertThat((result as Result2.Ok).value).isInstanceOf(LoginResponse.AwaitingTotp::class.java)
        assertThat(repo.state.value).isInstanceOf(AuthState.AwaitingTotp::class.java)
    }

    @Test
    fun `login 429 maps to LockedOut state`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(429)
                .setHeader("Retry-After", "30")
                .setBody("""{"detail":"Too many failed sign-in attempts."}"""),
        )
        val repo = buildRepo()
        val result = repo.login(LoginIn(email = "a@b.c", password = "p"))
        assertThat(result).isInstanceOf(Result2.Err::class.java)
        assertThat(repo.state.value).isInstanceOf(AuthState.LockedOut::class.java)
    }

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

internal class InMemoryDataStore : DataStore<Preferences> {
    private val state = MutableStateFlow<Preferences>(emptyPreferences())
    override val data: Flow<Preferences> get() = state

    override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences {
        val current = state.value
        val produced = transform(current)
        val mutable = mutablePreferencesOf()
        for ((k, v) in produced.asMap()) {
            @Suppress("UNCHECKED_CAST")
            mutable[k as Preferences.Key<Any>] = v
        }
        state.value = mutable
        return mutable
    }
}
