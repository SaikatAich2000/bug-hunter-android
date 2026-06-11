package com.bughunter.feature.events.list

import com.bughunter.core.data.repository.EventsRepository
import com.bughunter.core.data.repository.RepoTestSupport
import com.bughunter.core.network.api.EventsApi
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
 * EventsListViewModel: list + client-side query filtering + form open/close
 * state. The query filter is applied locally over the fetched list, so the
 * filtering tests don't depend on server-side search.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class EventsListViewModelTest {

    private lateinit var server: MockWebServer
    private lateinit var repo: EventsRepository
    private val moshi = RepoTestSupport.moshi()
    private val mapper = RepoTestSupport.errorMapper(moshi)
    private val jar = RepoTestSupport.cookieJar()

    @Before
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Unconfined)
        server = MockWebServer().apply { start() }
        RepoTestSupport.seedCsrf(jar, server.url("/"))
        val api = RepoTestSupport.retrofit(server, moshi, client = RepoTestSupport.client(jar))
            .create(EventsApi::class.java)
        repo = EventsRepository(api, mapper)
    }

    @After
    fun tearDown() {
        runCatching { server.shutdown() }
        Dispatchers.resetMain()
    }

    private suspend fun awaitUntil(timeoutMs: Long = 4_000, check: () -> Boolean) {
        withTimeout(timeoutMs) { while (!check()) delay(10) }
    }

    private fun vmAfterInitialLoad(initialBody: String): EventsListViewModel {
        server.enqueue(MockResponse().setResponseCode(200).setBody(initialBody))
        return EventsListViewModel(repo)
    }

    @Test
    fun `empty list lands in Empty state`() = runBlocking {
        val vm = vmAfterInitialLoad("[]")
        awaitUntil { vm.state.value.list is UiState.Empty }
        assertThat(vm.state.value.isRefreshing).isFalse()
    }

    @Test
    fun `non-empty list lands in Success state`() = runBlocking {
        val vm = vmAfterInitialLoad("[${event("Launch")}]")
        awaitUntil { vm.state.value.list is UiState.Success }
        assertThat((vm.state.value.list as UiState.Success).data).hasSize(1)
    }

    @Test
    fun `query filters the fetched list locally`() = runBlocking {
        val body = "[${event("Launch Day")},${event("Retro")}]"
        val vm = vmAfterInitialLoad(body)
        awaitUntil { vm.state.value.list is UiState.Success }
        // onQueryChange triggers a refresh; enqueue the same list again.
        server.enqueue(MockResponse().setResponseCode(200).setBody(body))
        vm.onQueryChange("retro")
        awaitUntil {
            val l = vm.state.value.list
            l is UiState.Success && l.data.size == 1
        }
        assertThat((vm.state.value.list as UiState.Success).data[0].name).isEqualTo("Retro")
    }

    @Test
    fun `list failure lands in Error state`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(500).setBody("""{"detail":"down"}"""))
        val vm = EventsListViewModel(repo)
        awaitUntil { vm.state.value.list is UiState.Error }
        assertThat(vm.state.value.isRefreshing).isFalse()
    }

    @Test
    fun `form open and close state transitions`() = runBlocking {
        val vm = vmAfterInitialLoad("[]")
        awaitUntil { vm.state.value.list is UiState.Empty }
        vm.openCreate()
        assertThat(vm.state.value.isFormOpen).isTrue()
        assertThat(vm.state.value.editingEventId).isNull()
        vm.openEdit(7)
        assertThat(vm.state.value.editingEventId).isEqualTo(7)
        vm.closeForm()
        assertThat(vm.state.value.isFormOpen).isFalse()
        assertThat(vm.state.value.editingEventId).isNull()
    }

    private fun event(name: String): String = """
        {"id":${name.hashCode() and 0xffff},"name":"$name","description":"",
         "scheduled_for":null,"managers":[],"item_count":0,
         "created_at":"2026-01-01T00:00:00Z","updated_at":"2026-01-01T00:00:00Z"}
    """.trimIndent()
}
