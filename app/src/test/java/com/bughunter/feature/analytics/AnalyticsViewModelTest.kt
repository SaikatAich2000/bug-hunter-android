package com.bughunter.feature.analytics

import com.bughunter.core.data.repository.RepoTestSupport
import com.bughunter.core.data.repository.StatsRepository
import com.bughunter.core.network.api.StatsApi
import com.bughunter.core.ui.util.UiState
import com.bughunter.feature.dashboard.DashboardTypeTab
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

@OptIn(ExperimentalCoroutinesApi::class)
class AnalyticsViewModelTest {

    private lateinit var server: MockWebServer
    private lateinit var repo: StatsRepository
    private val moshi = RepoTestSupport.moshi()
    private val mapper = RepoTestSupport.errorMapper(moshi)
    private val jar = RepoTestSupport.cookieJar()

    @Before
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Unconfined)
        server = MockWebServer().apply { start() }
        RepoTestSupport.seedCsrf(jar, server.url("/"))
        val api = RepoTestSupport.retrofit(server, moshi, client = RepoTestSupport.client(jar))
            .create(StatsApi::class.java)
        repo = StatsRepository(api, mapper)
    }

    @After
    fun tearDown() {
        runCatching { server.shutdown() }
        Dispatchers.resetMain()
    }

    private suspend fun awaitUntil(timeoutMs: Long = 4_000, check: () -> Boolean) {
        withTimeout(timeoutMs) { while (!check()) delay(10) }
    }

    // --- init / load -------------------------------------------------------
    @Test
    fun `init success lands in Success state with mapped model`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody(STATS))
        val vm = AnalyticsViewModel(repo)
        awaitUntil { vm.state.value is UiState.Success }
        val model = (vm.state.value as UiState.Success).data
        assertThat(model.tab).isEqualTo(DashboardTypeTab.ALL)
        assertThat(model.stats.totals.total).isEqualTo(10)
        assertThat(model.stats.totals.open).isEqualTo(4)
        assertThat(model.stats.totals.resolved).isEqualTo(3)
        assertThat(model.stats.totals.closed).isEqualTo(2)
        assertThat(model.stats.totals.resolveLater).isEqualTo(1)
        assertThat(model.stats.projectsCount).isEqualTo(5)
        assertThat(model.stats.usersCount).isEqualTo(7)
        // byStatus sorted descending by value.
        assertThat(model.stats.byStatus.first().first).isEqualTo("Open")
        // byPriority follows PRIORITY_ORDER, only present keys.
        assertThat(model.stats.byPriority.map { it.first }).containsExactly("Low", "High").inOrder()
        assertThat(model.stats.byEnvironment.map { it.first }).containsExactly("DEV", "PROD").inOrder()
        assertThat(model.stats.byProject.map { it.name }).containsExactly("Alpha", "Beta").inOrder()
        assertThat(model.stats.byAssignee.first().name).isEqualTo("Ada")
        assertThat(model.stats.timeline.first().count).isEqualTo(2)
    }

    @Test
    fun `init failure lands in Error state`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(500).setBody("""{"detail":"boom"}"""))
        val vm = AnalyticsViewModel(repo)
        awaitUntil { vm.state.value is UiState.Error }
        assertThat(vm.state.value).isInstanceOf(UiState.Error::class.java)
    }

    @Test
    fun `initial tab is ALL`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody(STATS))
        val vm = AnalyticsViewModel(repo)
        awaitUntil { vm.state.value is UiState.Success }
        assertThat(vm.tab.value).isEqualTo(DashboardTypeTab.ALL)
    }

    // --- refresh / retry ---------------------------------------------------
    @Test
    fun `refresh re-fetches and lands in Success`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody(STATS))
        val vm = AnalyticsViewModel(repo)
        awaitUntil { vm.state.value is UiState.Success }
        server.enqueue(MockResponse().setResponseCode(200).setBody(STATS))
        vm.refresh()
        awaitUntil { vm.state.value is UiState.Success }
        assertThat(vm.state.value).isInstanceOf(UiState.Success::class.java)
    }

    @Test
    fun `refresh failure lands in Error`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody(STATS))
        val vm = AnalyticsViewModel(repo)
        awaitUntil { vm.state.value is UiState.Success }
        server.enqueue(MockResponse().setResponseCode(500).setBody("""{"detail":"x"}"""))
        vm.refresh()
        awaitUntil { vm.state.value is UiState.Error }
        assertThat(vm.state.value).isInstanceOf(UiState.Error::class.java)
    }

    // --- onTabChange -------------------------------------------------------
    @Test
    fun `onTabChange to new tab updates tab and re-fetches with item_type`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody(STATS))
        val vm = AnalyticsViewModel(repo)
        awaitUntil { vm.state.value is UiState.Success }
        server.takeRequest() // consume the init request
        server.enqueue(MockResponse().setResponseCode(200).setBody(STATS))
        vm.onTabChange(DashboardTypeTab.BUGS)
        awaitUntil { vm.state.value is UiState.Success && vm.tab.value == DashboardTypeTab.BUGS }
        assertThat(vm.tab.value).isEqualTo(DashboardTypeTab.BUGS)
        val recorded = server.takeRequest()
        assertThat(recorded.path).contains("item_type=Bug")
        val model = (vm.state.value as UiState.Success).data
        assertThat(model.tab).isEqualTo(DashboardTypeTab.BUGS)
    }

    @Test
    fun `onTabChange to same tab is a no-op`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody(STATS))
        val vm = AnalyticsViewModel(repo)
        awaitUntil { vm.state.value is UiState.Success }
        vm.onTabChange(DashboardTypeTab.ALL)
        // No second response enqueued; tab stays ALL and state stays Success.
        assertThat(vm.tab.value).isEqualTo(DashboardTypeTab.ALL)
        assertThat(vm.state.value).isInstanceOf(UiState.Success::class.java)
    }

    private companion object {
        const val STATS = """
            {"bugs":10,"open":4,"resolved":3,"closed":2,"resolve_later":1,
             "projects":5,"users":7,
             "by_status":{"Open":4,"Closed":2,"Resolved":3},
             "by_priority":{"Low":2,"High":5},
             "by_environment":{"DEV":3,"PROD":1},
             "by_type":{"Bug":10},
             "by_project":[{"project_name":"Alpha","count":6},{"project_name":"Beta","count":4}],
             "by_assignee":[{"name":"Ada","count":8}],
             "timeline":[{"date":"2026-01-01","count":2}]}
        """
    }
}
