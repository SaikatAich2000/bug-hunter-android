package com.bughunter.feature.events.detail

import androidx.lifecycle.SavedStateHandle
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
 * EventDetailViewModel: reads the "eventId" nav arg, loads the event on init
 * via repository.get (HTTP GET), and exposes form/delete-confirm toggles plus
 * a delete action. The MockWebServer feeds the repository so we exercise the
 * real GET/DELETE plumbing without mocks.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class EventDetailViewModelTest {

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

    private fun detail(id: Int = 1, name: String = "Launch"): String = """
        {"id":$id,"name":"$name","description":"desc",
         "scheduled_for":null,"managers":[],"item_count":0,
         "created_at":"2026-01-01T00:00:00Z","updated_at":"2026-01-01T00:00:00Z",
         "can_edit":true,"can_delete":true,"items":[]}
    """.trimIndent()

    /** Enqueues the init GET response, then constructs the VM. */
    private fun vmAfterInitialLoad(body: String = detail()): EventDetailViewModel {
        server.enqueue(MockResponse().setResponseCode(200).setBody(body))
        return EventDetailViewModel(repo, SavedStateHandle(mapOf("eventId" to 1)))
    }

    @Test
    fun `init load success exposes the event`() = runBlocking {
        val vm = vmAfterInitialLoad(detail(name = "Launch Day"))
        awaitUntil { vm.state.value.event is UiState.Success }
        val event = (vm.state.value.event as UiState.Success).data
        assertThat(event.name).isEqualTo("Launch Day")
        assertThat(event.id).isEqualTo(1)
    }

    @Test
    fun `init load error lands in Error state`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(500).setBody("""{"detail":"down"}"""))
        val vm = EventDetailViewModel(repo, SavedStateHandle(mapOf("eventId" to 1)))
        awaitUntil { vm.state.value.event is UiState.Error }
        Unit
    }

    @Test
    fun `missing eventId arg fails fast`() {
        try {
            EventDetailViewModel(repo, SavedStateHandle())
            assertThat(false).isTrue() // should not reach here
        } catch (e: IllegalStateException) {
            assertThat(e).hasMessageThat().contains("eventId")
        }
    }

    @Test
    fun `refresh reloads the event`() = runBlocking {
        val vm = vmAfterInitialLoad(detail(name = "First"))
        awaitUntil { vm.state.value.event is UiState.Success }
        server.enqueue(MockResponse().setResponseCode(200).setBody(detail(name = "Second")))
        vm.refresh()
        awaitUntil {
            val e = vm.state.value.event
            e is UiState.Success && e.data.name == "Second"
        }
        assertThat(((vm.state.value.event) as UiState.Success).data.name).isEqualTo("Second")
    }

    @Test
    fun `openEdit opens the form`() = runBlocking {
        val vm = vmAfterInitialLoad()
        awaitUntil { vm.state.value.event is UiState.Success }
        vm.openEdit()
        assertThat(vm.state.value.isFormOpen).isTrue()
    }

    @Test
    fun `closeForm closes the form and refreshes`() = runBlocking {
        val vm = vmAfterInitialLoad()
        awaitUntil { vm.state.value.event is UiState.Success }
        vm.openEdit()
        assertThat(vm.state.value.isFormOpen).isTrue()
        // closeForm triggers a refresh; enqueue the reload response.
        server.enqueue(MockResponse().setResponseCode(200).setBody(detail(name = "Reloaded")))
        vm.closeForm()
        assertThat(vm.state.value.isFormOpen).isFalse()
        awaitUntil {
            val e = vm.state.value.event
            e is UiState.Success && e.data.name == "Reloaded"
        }
        Unit
    }

    @Test
    fun `openDeleteConfirm and dismissDeleteConfirm toggle the flag`() = runBlocking {
        val vm = vmAfterInitialLoad()
        awaitUntil { vm.state.value.event is UiState.Success }
        vm.openDeleteConfirm()
        assertThat(vm.state.value.isDeleteConfirmOpen).isTrue()
        vm.dismissDeleteConfirm()
        assertThat(vm.state.value.isDeleteConfirmOpen).isFalse()
    }

    @Test
    fun `confirmDelete success invokes callback and clears flags`() = runBlocking {
        val vm = vmAfterInitialLoad()
        awaitUntil { vm.state.value.event is UiState.Success }
        vm.openDeleteConfirm()
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"status":"ok"}"""))
        var deleted = false
        vm.confirmDelete { deleted = true }
        awaitUntil { deleted }
        assertThat(vm.state.value.isDeleting).isFalse()
        assertThat(vm.state.value.isDeleteConfirmOpen).isFalse()
    }

    @Test
    fun `confirmDelete error surfaces error and does not invoke callback`() = runBlocking {
        val vm = vmAfterInitialLoad()
        awaitUntil { vm.state.value.event is UiState.Success }
        vm.openDeleteConfirm()
        server.enqueue(MockResponse().setResponseCode(500).setBody("""{"detail":"boom"}"""))
        var deleted = false
        vm.confirmDelete { deleted = true }
        awaitUntil { vm.state.value.event is UiState.Error }
        assertThat(deleted).isFalse()
        assertThat(vm.state.value.isDeleting).isFalse()
    }
}
