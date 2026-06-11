package com.bughunter.feature.events.create

import com.bughunter.core.data.repository.EventsRepository
import com.bughunter.core.data.repository.RepoTestSupport
import com.bughunter.core.network.api.EventsApi
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
 * EventFormViewModel: pure form-field setters, prefill via repository.get(),
 * and submit() which routes to create (eventId == null) vs update.
 *
 * Repository is built exactly like EventsListViewModelTest: a MockWebServer
 * fronted by a CSRF-preseeded client so the mutating POST/PUT calls don't
 * burn a queued response on an inline GET /api/health.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class EventFormViewModelTest {

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

    private fun vm() = EventFormViewModel(repo)

    private fun eventDetail(
        id: Int = 5,
        name: String = "Launch",
        description: String = "desc",
        scheduledFor: String? = "2026-07-01",
        managers: String = """{"id":3,"name":"Mgr","email":"m@x.io","role":"manager"}""",
    ): String = """
        {"id":$id,"name":"$name","description":"$description",
         "scheduled_for":${if (scheduledFor == null) "null" else "\"$scheduledFor\""},
         "managers":[$managers],"item_count":0,
         "created_at":"2026-01-01T00:00:00Z","updated_at":"2026-01-01T00:00:00Z"}
    """.trimIndent()

    private fun eventOut(id: Int = 9, name: String = "Launch"): String = """
        {"id":$id,"name":"$name","description":"",
         "scheduled_for":null,"managers":[],"item_count":0,
         "created_at":"2026-01-01T00:00:00Z","updated_at":"2026-01-01T00:00:00Z"}
    """.trimIndent()

    // ----- initial state -----

    @Test
    fun `initial state is empty and idle`() {
        val s = vm().state.value
        assertThat(s.eventId).isNull()
        assertThat(s.name).isEmpty()
        assertThat(s.description).isEmpty()
        assertThat(s.scheduledFor).isEmpty()
        assertThat(s.managerIdsCsv).isEmpty()
        assertThat(s.isSubmitting).isFalse()
        assertThat(s.isPrefilling).isFalse()
        assertThat(s.error).isNull()
        assertThat(s.savedSuccessfully).isFalse()
    }

    // ----- form-field setters (pure) -----

    @Test
    fun `onNameChange updates name and clears error`() {
        val vm = vm()
        vm.onNameChange("Sprint Review")
        assertThat(vm.state.value.name).isEqualTo("Sprint Review")
        assertThat(vm.state.value.error).isNull()
    }

    @Test
    fun `onDescriptionChange updates description`() {
        val vm = vm()
        vm.onDescriptionChange("the details")
        assertThat(vm.state.value.description).isEqualTo("the details")
        assertThat(vm.state.value.error).isNull()
    }

    @Test
    fun `onScheduledForChange updates scheduledFor`() {
        val vm = vm()
        vm.onScheduledForChange("2026-09-09")
        assertThat(vm.state.value.scheduledFor).isEqualTo("2026-09-09")
        assertThat(vm.state.value.error).isNull()
    }

    @Test
    fun `onManagersChange updates managerIdsCsv`() {
        val vm = vm()
        vm.onManagersChange("1, 2, 3")
        assertThat(vm.state.value.managerIdsCsv).isEqualTo("1, 2, 3")
        assertThat(vm.state.value.error).isNull()
    }

    // ----- start() create mode (no prefill) -----

    @Test
    fun `start with null eventId resets to blank create form`() {
        val vm = vm()
        vm.onNameChange("stale")
        vm.start(null)
        val s = vm.state.value
        assertThat(s.eventId).isNull()
        assertThat(s.name).isEmpty()
        assertThat(s.isPrefilling).isFalse()
    }

    // ----- start() edit mode: prefill via repository.get() -----

    @Test
    fun `start with eventId prefills form from get success`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody(eventDetail(id = 5)))
        val vm = vm()
        vm.start(5)
        awaitUntil { !vm.state.value.isPrefilling }
        val s = vm.state.value
        assertThat(s.eventId).isEqualTo(5)
        assertThat(s.name).isEqualTo("Launch")
        assertThat(s.description).isEqualTo("desc")
        assertThat(s.scheduledFor).isEqualTo("2026-07-01")
        assertThat(s.managerIdsCsv).isEqualTo("3")
        assertThat(s.error).isNull()
    }

    @Test
    fun `prefill tolerates null scheduledFor and empty managers`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200)
                .setBody(eventDetail(id = 8, scheduledFor = null, managers = "")),
        )
        val vm = vm()
        vm.start(8)
        awaitUntil { !vm.state.value.isPrefilling }
        val s = vm.state.value
        assertThat(s.scheduledFor).isEmpty()
        assertThat(s.managerIdsCsv).isEmpty()
    }

    @Test
    fun `start with eventId sets error on get failure`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(500).setBody("""{"detail":"boom"}"""))
        val vm = vm()
        vm.start(99)
        awaitUntil { !vm.state.value.isPrefilling }
        assertThat(vm.state.value.error).isNotNull()
    }

    // ----- submit(): create path -----

    @Test
    fun `submit creates event and lands savedSuccessfully on success`() = runBlocking {
        val vm = vm()
        vm.onNameChange("  New Event  ")
        vm.onDescriptionChange("body")
        vm.onScheduledForChange("2026-10-10")
        vm.onManagersChange("1, 2, 2, x")
        server.enqueue(MockResponse().setResponseCode(201).setBody(eventOut(id = 9)))
        vm.submit()
        awaitUntil { vm.state.value.savedSuccessfully }
        val s = vm.state.value
        assertThat(s.isSubmitting).isFalse()
        assertThat(s.error).isNull()
        val recorded = server.takeRequest()
        assertThat(recorded.method).isEqualTo("POST")
        assertThat(recorded.path).isEqualTo("/api/events")
    }

    @Test
    fun `submit create sets error on server failure`() = runBlocking {
        val vm = vm()
        vm.onNameChange("Bad Event")
        server.enqueue(MockResponse().setResponseCode(400).setBody("""{"detail":"nope"}"""))
        vm.submit()
        awaitUntil { vm.state.value.error != null }
        val s = vm.state.value
        assertThat(s.savedSuccessfully).isFalse()
        assertThat(s.isSubmitting).isFalse()
    }

    // ----- submit(): update path (eventId set via prefill) -----

    @Test
    fun `submit updates existing event via PUT`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody(eventDetail(id = 5)))
        val vm = vm()
        vm.start(5)
        awaitUntil { !vm.state.value.isPrefilling }
        server.takeRequest() // consume the prefill GET
        vm.onNameChange("Renamed")
        server.enqueue(MockResponse().setResponseCode(200).setBody(eventOut(id = 5, name = "Renamed")))
        vm.submit()
        awaitUntil { vm.state.value.savedSuccessfully }
        val recorded = server.takeRequest()
        assertThat(recorded.method).isEqualTo("PUT")
        assertThat(recorded.path).isEqualTo("/api/events/5")
    }

    // ----- submit() validation guard: blank name blocks the request -----

    @Test
    fun `submit with blank name makes no request`() = runBlocking {
        val vm = vm()
        vm.onNameChange("   ")
        vm.submit()
        delay(100)
        assertThat(server.requestCount).isEqualTo(0)
        assertThat(vm.state.value.isSubmitting).isFalse()
        assertThat(vm.state.value.savedSuccessfully).isFalse()
    }
}
