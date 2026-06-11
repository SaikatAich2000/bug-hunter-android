package com.bughunter.feature.dashboard

import com.bughunter.core.data.repository.BugsRepository
import com.bughunter.core.data.repository.RepoTestSupport
import com.bughunter.core.data.repository.StatsRepository
import com.bughunter.core.domain.usecase.ToggleKpiFilterUseCase
import com.bughunter.core.network.api.BugsApi
import com.bughunter.core.network.api.StatsApi
import com.bughunter.core.network.dto.BugOut
import com.bughunter.core.ui.util.UiState
import com.google.common.truth.Truth.assertThat
import java.time.Instant
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

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    private lateinit var server: MockWebServer
    private lateinit var statsRepo: StatsRepository
    private lateinit var bugsRepo: BugsRepository

    private val moshi = RepoTestSupport.moshi()
    private val mapper = RepoTestSupport.errorMapper(moshi)
    private val jar = RepoTestSupport.cookieJar()

    @Before
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Unconfined)
        server = MockWebServer().apply { start() }
        RepoTestSupport.seedCsrf(jar, server.url("/"))
        val rf = RepoTestSupport.retrofit(server, moshi, client = RepoTestSupport.client(jar))
        statsRepo = StatsRepository(rf.create(StatsApi::class.java), mapper)
        bugsRepo = BugsRepository(rf.create(BugsApi::class.java), mapper)
    }

    @After
    fun tearDown() {
        runCatching { server.shutdown() }
        Dispatchers.resetMain()
    }

    private suspend fun awaitUntil(timeoutMs: Long = 4_000, check: () -> Boolean) {
        withTimeout(timeoutMs) { while (!check()) delay(10) }
    }

    private fun newVm() = DashboardViewModel(statsRepo, bugsRepo, ToggleKpiFilterUseCase())

    private fun statsBody(): String = STATS

    private fun bug(id: Int): BugOut = BugOut(
        id = id,
        projectId = 1,
        itemType = "Bug",
        title = "Bug $id",
        status = "New",
        priority = "High",
        environment = "PROD",
        createdAt = Instant.EPOCH,
        updatedAt = Instant.EPOCH,
    )

    private fun bugsBody(count: Int): String {
        val items = (1..count).joinToString(",") { id ->
            moshi.adapter(BugOut::class.java).toJson(bug(id))
        }
        return """{"items":[$items],"page":1,"page_size":6,"total":$count,"pages":1}"""
    }

    private fun enqueueStats() =
        server.enqueue(MockResponse().setResponseCode(200).setBody(statsBody()))

    private fun enqueueBugs(count: Int) =
        server.enqueue(MockResponse().setResponseCode(200).setBody(bugsBody(count)))

    private fun enqueueError() =
        server.enqueue(MockResponse().setResponseCode(500).setBody("""{"detail":"boom"}"""))

    // --- init -------------------------------------------------------------
    @Test
    fun `init success populates stats and recent bugs taking 6`() = runBlocking {
        enqueueStats()
        enqueueBugs(7)
        val vm = newVm()
        awaitUntil { vm.state.value is UiState.Success }
        val data = (vm.state.value as UiState.Success).data
        assertThat(data.recentBugs).hasSize(6)
        assertThat(data.stats.totals.total).isEqualTo(3)
        assertThat(data.stats.totals.open).isEqualTo(2)
        assertThat(data.tab).isEqualTo(DashboardTypeTab.ALL)
        assertThat(data.activeTile).isNull()
    }

    @Test
    fun `init stats error lands in Error state`() = runBlocking {
        // refresh() calls stats.get() THEN bugs.list() before checking the
        // error, so both endpoints are hit — enqueue a bugs response too.
        enqueueError()
        enqueueBugs(0)
        val vm = newVm()
        awaitUntil { vm.state.value is UiState.Error }
        assertThat(vm.state.value).isInstanceOf(UiState.Error::class.java)
    }

    @Test
    fun `init bugs error after stats ok lands in Error state`() = runBlocking {
        enqueueStats()
        enqueueError()
        val vm = newVm()
        awaitUntil { vm.state.value is UiState.Error }
        assertThat(vm.state.value).isInstanceOf(UiState.Error::class.java)
    }

    // --- onTabChange ------------------------------------------------------
    @Test
    fun `onTabChange BUGS re-refreshes and updates tab and filter itemTypes`() = runBlocking {
        enqueueStats()
        enqueueBugs(2)
        val vm = newVm()
        awaitUntil { vm.state.value is UiState.Success }

        enqueueStats()
        enqueueBugs(1)
        vm.onTabChange(DashboardTypeTab.BUGS)
        awaitUntil { vm.tab.value == DashboardTypeTab.BUGS }
        awaitUntil {
            (vm.state.value as? UiState.Success)?.data?.tab == DashboardTypeTab.BUGS
        }
        assertThat(vm.tab.value).isEqualTo(DashboardTypeTab.BUGS)
        assertThat(vm.filters.value.itemTypes).containsExactly("Bug")
        assertThat((vm.state.value as UiState.Success).data.recentBugs).hasSize(1)
    }

    @Test
    fun `onTabChange to same tab is a no-op`() = runBlocking {
        enqueueStats()
        enqueueBugs(2)
        val vm = newVm()
        awaitUntil { vm.state.value is UiState.Success }
        vm.onTabChange(DashboardTypeTab.ALL)
        assertThat(vm.tab.value).isEqualTo(DashboardTypeTab.ALL)
        assertThat(vm.filters.value.itemTypes).isEmpty()
    }

    // --- onKpiToggle ------------------------------------------------------
    @Test
    fun `onKpiToggle sets active tile and re-fetches bugs`() = runBlocking {
        enqueueStats()
        enqueueBugs(3)
        val vm = newVm()
        awaitUntil { vm.state.value is UiState.Success }

        enqueueBugs(2)
        vm.onKpiToggle(ToggleKpiFilterUseCase.KpiTile.RESOLVED)
        awaitUntil {
            (vm.state.value as? UiState.Success)?.data?.activeTile ==
                ToggleKpiFilterUseCase.KpiTile.RESOLVED
        }
        val data = (vm.state.value as UiState.Success).data
        assertThat(data.activeTile).isEqualTo(ToggleKpiFilterUseCase.KpiTile.RESOLVED)
        assertThat(data.recentBugs).hasSize(2)
        assertThat(vm.filters.value.statuses).containsExactly("Resolved")
        Unit
    }

    @Test
    fun `onKpiToggle TOTAL clears the active tile`() = runBlocking {
        enqueueStats()
        enqueueBugs(3)
        val vm = newVm()
        awaitUntil { vm.state.value is UiState.Success }

        enqueueBugs(1)
        vm.onKpiToggle(ToggleKpiFilterUseCase.KpiTile.TOTAL)
        awaitUntil { vm.filters.value.statuses.isEmpty() }
        val data = (vm.state.value as UiState.Success).data
        assertThat(data.activeTile).isNull()
        assertThat(vm.filters.value.statuses).isEmpty()
    }

    @Test
    fun `onKpiToggle on bugs error lands in Error state`() = runBlocking {
        enqueueStats()
        enqueueBugs(3)
        val vm = newVm()
        awaitUntil { vm.state.value is UiState.Success }

        enqueueError()
        vm.onKpiToggle(ToggleKpiFilterUseCase.KpiTile.OPEN)
        awaitUntil { vm.state.value is UiState.Error }
        assertThat(vm.state.value).isInstanceOf(UiState.Error::class.java)
    }

    private companion object {
        const val STATS = """
            {"bugs":3,"open":2,"resolved":1,"closed":0,"resolve_later":0,
             "projects":4,"users":5,
             "by_status":{"New":2,"Resolved":1},
             "by_priority":{"High":2,"Low":1},
             "by_environment":{"PROD":3},
             "by_type":{"Bug":3},
             "by_project":[{"project_id":1,"project_name":"P","count":3}],
             "by_assignee":[{"user_id":1,"name":"Ada","count":2}],
             "timeline":[{"date":"2026-01-01","count":1}]}
        """
    }
}
