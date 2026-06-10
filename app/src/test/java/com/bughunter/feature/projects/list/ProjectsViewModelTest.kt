package com.bughunter.feature.projects.list

import com.bughunter.core.data.repository.ProjectsRepository
import com.bughunter.core.data.repository.RepoTestSupport
import com.bughunter.core.network.api.ProjectsApi
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
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
 * Tests that errors from the projects-create network call REACH the
 * ViewModel state (so the dialog can render them) instead of being
 * silently swallowed.
 *
 * This was the v2.10 bug: createProject discarded Result2.Err via an
 * onDone(false) callback that screens treated as a no-op. The user
 * saw the dialog close (or stay open) with no message either way.
 *
 * Test setup notes:
 *   - runBlocking + Dispatchers.Unconfined for Main: viewModelScope's
 *     launches run on Main; Unconfined runs them synchronously in the
 *     same thread.
 *   - Real (wall-clock) `withTimeout` — runTest's virtual time freezes
 *     while we wait for OkHttp's real-thread response.
 */
class ProjectsViewModelTest {

    private lateinit var server: MockWebServer
    private lateinit var api: ProjectsApi
    private val moshi = RepoTestSupport.moshi()
    private val mapper = RepoTestSupport.errorMapper(moshi)
    private val jar = RepoTestSupport.cookieJar()

    @Before
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Unconfined)
        server = MockWebServer().apply { start() }
        // Pre-seed CSRF — without this, the first POST through
        // CsrfInterceptor would do an inline GET /api/health that
        // consumes the queued 403/201 the test author meant for the
        // POST itself.
        RepoTestSupport.seedCsrf(jar, server.url("/"))
        api = RepoTestSupport.retrofit(server, moshi, client = RepoTestSupport.client(jar))
            .create(ProjectsApi::class.java)
    }

    @After
    fun tearDown() {
        runCatching { server.shutdown() }
        Dispatchers.resetMain()
    }

    private fun buildVm(): ProjectsViewModel {
        // init { load() } makes the first HTTP call. Pre-enqueue an
        // empty list so the load resolves and the VM lands in a stable
        // state for the test to manipulate.
        server.enqueue(MockResponse().setResponseCode(200).setBody("[]"))
        return ProjectsViewModel(ProjectsRepository(api, mapper))
    }

    private suspend fun awaitUntil(timeoutMs: Long = 4_000, check: () -> Boolean) {
        withTimeout(timeoutMs) {
            while (!check()) delay(10)
        }
    }

    @Test
    fun `createProject surfaces 403 CSRF failure in createState error`() = runBlocking {
        val vm = buildVm()
        awaitUntil { server.requestCount >= 1 }
        server.enqueue(
            MockResponse()
                .setResponseCode(403)
                .setBody("""{"detail":"CSRF check failed. Reload the page and try again."}"""),
        )

        var successCalled = false
        vm.createProject("New project", null, "#6366f1", "") { successCalled = true }
        awaitUntil { !vm.createState.value.isSubmitting }

        assertThat(successCalled).isFalse()
        assertThat(vm.createState.value.error).isNotNull()
    }

    @Test
    fun `createProject surfaces network failure in createState error`() = runBlocking {
        val vm = buildVm()
        awaitUntil { server.requestCount >= 1 }
        // Shut down AFTER the initial load completed so subsequent calls
        // see a connection error.
        server.shutdown()

        var successCalled = false
        vm.createProject("Whatever", null, "#000000", "") { successCalled = true }
        awaitUntil { !vm.createState.value.isSubmitting }

        assertThat(successCalled).isFalse()
        assertThat(vm.createState.value.error).isNotNull()
    }

    @Test
    fun `createProject success clears state and invokes onSuccess`() = runBlocking {
        val vm = buildVm()
        awaitUntil { server.requestCount >= 1 }
        server.enqueue(
            MockResponse().setResponseCode(201).setBody(PROJECT_201_BODY),
        )
        // The post-create reload() fires a second list() call.
        server.enqueue(MockResponse().setResponseCode(200).setBody("[]"))

        var successCalled = false
        vm.createProject("Good", null, "#6366f1", "") { successCalled = true }
        awaitUntil { successCalled || vm.createState.value.error != null }

        assertThat(successCalled).isTrue()
        assertThat(vm.createState.value.isSubmitting).isFalse()
        assertThat(vm.createState.value.error).isNull()
    }

    companion object {
        private const val PROJECT_201_BODY = """
            {"id": 1, "name": "Good", "key": "", "color": "#6366f1",
             "description": "", "member_count": 1, "can_manage": true,
             "created_at": "2026-01-01T00:00:00Z",
             "updated_at": "2026-01-01T00:00:00Z"}
        """
    }
}
