package com.bughunter.feature.webhooks

import com.bughunter.core.data.repository.RepoTestSupport
import com.bughunter.core.data.repository.WebhooksRepository
import com.bughunter.core.network.api.WebhooksApi
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
 * WebhooksViewModel: list + CRUD with an optimistic reload after each
 * mutation. The create() callback contract (onDone(true/false)) is the
 * part screens depend on, so it's asserted explicitly.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class WebhooksViewModelTest {

    private lateinit var server: MockWebServer
    private lateinit var repo: WebhooksRepository
    private val moshi = RepoTestSupport.moshi()
    private val mapper = RepoTestSupport.errorMapper(moshi)
    private val jar = RepoTestSupport.cookieJar()

    @Before
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Unconfined)
        server = MockWebServer().apply { start() }
        RepoTestSupport.seedCsrf(jar, server.url("/"))
        val api = RepoTestSupport.retrofit(server, moshi, client = RepoTestSupport.client(jar))
            .create(WebhooksApi::class.java)
        repo = WebhooksRepository(api, mapper)
    }

    @After
    fun tearDown() {
        runCatching { server.shutdown() }
        Dispatchers.resetMain()
    }

    private suspend fun awaitUntil(timeoutMs: Long = 4_000, check: () -> Boolean) {
        withTimeout(timeoutMs) { while (!check()) delay(10) }
    }

    private fun vmAfterInitialLoad(initialBody: String): WebhooksViewModel {
        server.enqueue(MockResponse().setResponseCode(200).setBody(initialBody))
        return WebhooksViewModel(repo)
    }

    @Test
    fun `empty list lands in Empty state`() = runBlocking {
        val vm = vmAfterInitialLoad("[]")
        awaitUntil { vm.state.value is UiState.Empty }
        assertThat(vm.state.value).isInstanceOf(UiState.Empty::class.java)
    }

    @Test
    fun `non-empty list lands in Success state`() = runBlocking {
        val vm = vmAfterInitialLoad("[$HOOK_JSON]")
        awaitUntil { vm.state.value is UiState.Success }
        val data = (vm.state.value as UiState.Success).data
        assertThat(data.webhooks).hasSize(1)
        assertThat(data.webhooks[0].url).isEqualTo("https://x.io/hook")
    }

    @Test
    fun `list failure lands in Error state`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(500).setBody("""{"detail":"down"}"""))
        val vm = WebhooksViewModel(repo)
        awaitUntil { vm.state.value is UiState.Error }
        assertThat(vm.state.value).isInstanceOf(UiState.Error::class.java)
    }

    @Test
    fun `create success invokes onDone(true) and reloads`() = runBlocking {
        val vm = vmAfterInitialLoad("[]")
        awaitUntil { vm.state.value is UiState.Empty }
        server.enqueue(MockResponse().setResponseCode(201).setBody(HOOK_JSON))   // create
        server.enqueue(MockResponse().setResponseCode(200).setBody("[$HOOK_JSON]")) // reload
        var done: Boolean? = null
        vm.create("https://x.io/hook", null, listOf("bug.created")) { done = it }
        awaitUntil { done != null }
        assertThat(done).isTrue()
        awaitUntil { vm.state.value is UiState.Success }
    }

    @Test
    fun `create failure invokes onDone(false)`() = runBlocking {
        val vm = vmAfterInitialLoad("[]")
        awaitUntil { vm.state.value is UiState.Empty }
        server.enqueue(MockResponse().setResponseCode(400).setBody("""{"detail":"bad url"}"""))
        var done: Boolean? = null
        vm.create("not-a-url", null, emptyList()) { done = it }
        awaitUntil { done != null }
        assertThat(done).isFalse()
    }

    @Test
    fun `delete reloads the list`() = runBlocking {
        val vm = vmAfterInitialLoad("[$HOOK_JSON]")
        awaitUntil { vm.state.value is UiState.Success }
        server.enqueue(MockResponse().setResponseCode(204))          // delete
        server.enqueue(MockResponse().setResponseCode(200).setBody("[]")) // reload
        vm.delete(1)
        awaitUntil { vm.state.value is UiState.Empty }
        assertThat(vm.state.value).isInstanceOf(UiState.Empty::class.java)
    }

    @Test
    fun `toggleActive sends an update and reloads`() = runBlocking {
        val vm = vmAfterInitialLoad("[$HOOK_JSON]")
        awaitUntil { vm.state.value is UiState.Success }
        val hook = (vm.state.value as UiState.Success).data.webhooks[0]
        server.enqueue(MockResponse().setResponseCode(200).setBody(HOOK_JSON))      // update
        server.enqueue(MockResponse().setResponseCode(200).setBody("[$HOOK_JSON]")) // reload
        val before = server.requestCount
        vm.toggleActive(hook)
        awaitUntil { server.requestCount >= before + 2 }
        assertThat(server.requestCount).isAtLeast(before + 2)
    }

    companion object {
        private const val HOOK_JSON = """
            {"id":1,"url":"https://x.io/hook","secret_masked":"***","active":true,
             "event_types":["bug.created"],"created_at":"2026-01-01T00:00:00Z"}
        """
    }
}
