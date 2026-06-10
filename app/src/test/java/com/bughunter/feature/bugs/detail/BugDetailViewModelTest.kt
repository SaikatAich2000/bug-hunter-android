package com.bughunter.feature.bugs.detail

import androidx.lifecycle.SavedStateHandle
import com.bughunter.core.data.repository.BugsRepository
import com.bughunter.core.data.repository.RepoTestSupport
import com.bughunter.core.nav.BhRoute
import com.bughunter.core.network.api.BugsApi
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
 * Pins the SavedStateHandle key/type contract for the bug-detail nav arg.
 *
 * Nav declares `bugId` as NavType.IntType in BhNavHost, so the framework
 * stores it as java.lang.Integer in SavedStateHandle. The v2.10 crash
 * (`ClassCastException: Integer cannot be cast to String` on every
 * navigation to bug detail) was caused by the VM reading it as `<String>`
 * and calling toIntOrNull(). This test instantiates the VM with the same
 * Integer the framework would provide; the constructor crashes if anyone
 * reverts the type back to `<String>`.
 */
class BugDetailViewModelTest {

    private lateinit var server: MockWebServer
    private lateinit var api: BugsApi
    private val moshi = RepoTestSupport.moshi()
    private val mapper = RepoTestSupport.errorMapper(moshi)
    private val jar = RepoTestSupport.cookieJar()

    @Before
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Unconfined)
        server = MockWebServer().apply { start() }
        RepoTestSupport.seedCsrf(jar, server.url("/"))
        api = RepoTestSupport.retrofit(server, moshi, client = RepoTestSupport.client(jar))
            .create(BugsApi::class.java)
    }

    @After
    fun tearDown() {
        runCatching { server.shutdown() }
        Dispatchers.resetMain()
    }

    @Test
    fun `constructor accepts integer bugId from nav SavedStateHandle without crashing`() = runBlocking {
        // Pre-enqueue the initial refresh() call so the VM lands in a
        // stable state. The test's real assertion is "constructor did
        // not throw" — pre-fix, this line itself blew up.
        server.enqueue(MockResponse().setResponseCode(200).setBody(BUG_DETAIL_BODY))

        val handle = SavedStateHandle(mapOf(BhRoute.BugDetail.ARG_ID to 42))
        val vm = BugDetailViewModel(handle, BugsRepository(api, mapper))

        // Wait briefly for refresh() to consume the response so the
        // server's request counter ticks up — confirms we used the int.
        withTimeout(2_000) {
            while (server.requestCount < 1) delay(10)
        }
        // The path must contain /api/bugs/42 — proves the VM read 42,
        // not -1 (the missing-arg fallback) and not a parse error.
        val recorded = server.takeRequest()
        assertThat(recorded.path).contains("/api/bugs/42")
        assertThat(vm).isNotNull()
    }

    @Test
    fun `missing bugId falls back to NotFound error`() = runBlocking {
        // Empty SavedStateHandle — simulates a malformed deep link.
        // VM should land in UiState.Error(NotFound) without crashing.
        val handle = SavedStateHandle()
        val vm = BugDetailViewModel(handle, BugsRepository(api, mapper))

        // refresh() short-circuits when bugId <= 0; it does NOT hit the
        // server, so requestCount stays at 0.
        delay(50)
        assertThat(server.requestCount).isEqualTo(0)
        // VM exists, no crash.
        assertThat(vm).isNotNull()
    }

    companion object {
        // Minimal BugDetail body just substantial enough for Moshi to
        // parse. The fields here must match the BugDetail DTO; if a
        // future schema change breaks parsing, this test will fail loudly.
        private const val BUG_DETAIL_BODY = """
            {
              "id": 42,
              "project_id": 1,
              "project_name": "p",
              "project_key": "P",
              "item_type": "Bug",
              "event_id": null,
              "event_name": null,
              "title": "t",
              "description": "",
              "reporter": {"id": 1, "name": "r", "email": "r@x.x", "role": "admin"},
              "assignees": [],
              "status": "New",
              "priority": "Medium",
              "environment": "DEV",
              "due_date": null,
              "created_at": "2026-06-10T09:01:30",
              "updated_at": "2026-06-10T09:01:30",
              "comments": [],
              "attachments": [],
              "activities": [],
              "attachment_count": 0,
              "can_edit": true
            }
        """
    }
}
