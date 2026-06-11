package com.bughunter.feature.sessions

import com.bughunter.core.data.repository.RepoTestSupport
import com.bughunter.core.data.repository.SessionsRepository
import com.bughunter.core.network.api.SessionsApi
import com.bughunter.core.ui.util.UiState
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * SessionsViewModel drives the "active sessions" panel: list, then a
 * two-step revoke (request -> confirm) that reloads on success. These tests
 * pin the UiState transitions through a real repository + MockWebServer.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SessionsViewModelTest {

    private lateinit var server: MockWebServer
    private lateinit var repo: SessionsRepository
    private val moshi = RepoTestSupport.moshi()
    private val mapper = RepoTestSupport.errorMapper(moshi)
    private val jar = RepoTestSupport.cookieJar()

    @Before
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Unconfined)
        server = MockWebServer().apply { start() }
        RepoTestSupport.seedCsrf(jar, server.url("/"))
        val api = RepoTestSupport.retrofit(server, moshi, client = RepoTestSupport.client(jar))
            .create(SessionsApi::class.java)
        repo = SessionsRepository(api, mapper)
    }

    @After
    fun tearDown() {
        runCatching { server.shutdown() }
        Dispatchers.resetMain()
    }

    private suspend fun awaitUntil(timeoutMs: Long = 4_000, check: () -> Boolean) {
        withTimeout(timeoutMs) { while (!check()) delay(10) }
    }

    private fun vmAfterInitialLoad(initialBody: String): SessionsViewModel {
        server.enqueue(MockResponse().setResponseCode(200).setBody(initialBody))
        return SessionsViewModel(repo)
    }

    @Test
    fun `empty session list lands in Empty state`() = runBlocking {
        val vm = vmAfterInitialLoad("[]")
        awaitUntil { vm.state.value.list is UiState.Empty }
        assertThat(vm.state.value.list).isInstanceOf(UiState.Empty::class.java)
    }

    @Test
    fun `non-empty list lands in Success state`() = runBlocking {
        val vm = vmAfterInitialLoad("[$SESSION_JSON]")
        awaitUntil { vm.state.value.list is UiState.Success }
        val success = vm.state.value.list as UiState.Success
        assertThat(success.data).hasSize(1)
        assertThat(success.data[0].id).isEqualTo(1)
    }

    @Test
    fun `list failure lands in Error state`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(500).setBody("""{"detail":"boom"}"""))
        val vm = SessionsViewModel(repo)
        awaitUntil { vm.state.value.list is UiState.Error }
        assertThat(vm.state.value.list).isInstanceOf(UiState.Error::class.java)
    }

    @Test
    fun `request then dismiss clears the pending revoke id`() = runBlocking {
        val vm = vmAfterInitialLoad("[]")
        awaitUntil { vm.state.value.list is UiState.Empty }
        vm.requestRevoke(42)
        assertThat(vm.state.value.pendingRevokeId).isEqualTo(42)
        vm.dismissRevoke()
        assertThat(vm.state.value.pendingRevokeId).isNull()
    }

    @Test
    fun `confirmRevoke calls revoke then reloads`() = runBlocking {
        val vm = vmAfterInitialLoad("[$SESSION_JSON]")
        awaitUntil { vm.state.value.list is UiState.Success }
        vm.requestRevoke(1)
        // revoke() returns a map; then refresh() lists again (now empty).
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"status":"ok"}"""))
        server.enqueue(MockResponse().setResponseCode(200).setBody("[]"))
        vm.confirmRevoke()
        awaitUntil { vm.state.value.list is UiState.Empty && vm.state.value.revokingId == null }
        assertThat(vm.state.value.pendingRevokeId).isNull()
    }

    @Test
    fun `confirmRevoke with no pending id is a no-op`() = runBlocking {
        val vm = vmAfterInitialLoad("[]")
        awaitUntil { vm.state.value.list is UiState.Empty }
        val before = server.requestCount
        vm.confirmRevoke() // pendingRevokeId is null
        assertThat(server.requestCount).isEqualTo(before)
    }

    companion object {
        private const val SESSION_JSON = """
            {"id":1,"user_id":7,"ip_address":"1.2.3.4","user_agent":"UA",
             "created_at":"2026-01-01T00:00:00Z","last_seen_at":"2026-01-01T00:00:00Z",
             "expires_at":"2026-02-01T00:00:00Z","is_current":true}
        """
    }
}
