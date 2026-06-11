package com.bughunter.feature.bugs.list

import com.bughunter.core.data.repository.BugsRepository
import com.bughunter.core.data.repository.ProjectsRepository
import com.bughunter.core.data.repository.RepoTestSupport
import com.bughunter.core.data.repository.UsersRepository
import com.bughunter.core.network.api.BugsApi
import com.bughunter.core.network.api.ProjectsApi
import com.bughunter.core.network.api.UsersApi
import com.bughunter.core.ui.util.UiState
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.ConcurrentLinkedQueue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * The VM's init fires loadFacets() (projects + users) and refresh() (bugs)
 * as concurrent coroutines, so the three requests can arrive in any order.
 * Rather than depend on FIFO enqueue order, this test routes responses by
 * path: projects/users return fixed facet bodies (overridable per-test via
 * their queues) and bug-list responses are pulled from [bugsQ] in order.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BugListViewModelTest {

    private lateinit var server: MockWebServer
    private lateinit var bugsRepo: BugsRepository
    private lateinit var projectsRepo: ProjectsRepository
    private lateinit var usersRepo: UsersRepository

    private val moshi = RepoTestSupport.moshi()
    private val mapper = RepoTestSupport.errorMapper(moshi)
    private val jar = RepoTestSupport.cookieJar()

    private val bugsQ = ConcurrentLinkedQueue<MockResponse>()
    private val projectsQ = ConcurrentLinkedQueue<MockResponse>()
    private val usersQ = ConcurrentLinkedQueue<MockResponse>()

    @Before
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Unconfined)
        server = MockWebServer().apply { start() }
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val path = request.requestUrl?.encodedPath ?: ""
                return when {
                    path.startsWith("/api/projects") -> projectsQ.poll() ?: ok("[$PROJECT]")
                    path.startsWith("/api/users") -> usersQ.poll() ?: ok("[$USER_ROW]")
                    else -> bugsQ.poll() ?: ok(bugsPage())
                }
            }
        }
        RepoTestSupport.seedCsrf(jar, server.url("/"))
        val rf = RepoTestSupport.retrofit(server, moshi, client = RepoTestSupport.client(jar))
        bugsRepo = BugsRepository(rf.create(BugsApi::class.java), mapper)
        projectsRepo = ProjectsRepository(rf.create(ProjectsApi::class.java), mapper)
        usersRepo = UsersRepository(rf.create(UsersApi::class.java), mapper)
    }

    @After
    fun tearDown() {
        runCatching { server.shutdown() }
        Dispatchers.resetMain()
    }

    private suspend fun awaitUntil(timeoutMs: Long = 4_000, check: () -> Boolean) {
        withTimeout(timeoutMs) { while (!check()) delay(10) }
    }

    private fun newVm() = BugListViewModel(bugsRepo, projectsRepo, usersRepo)

    private fun ok(body: String) = MockResponse().setResponseCode(200).setBody(body)
    private fun err500() = MockResponse().setResponseCode(500).setBody("""{"detail":"x"}""")

    private fun bugsPage(items: String = "", page: Int = 1, pages: Int = 1, total: Int = 1): String =
        """{"items":[$items],"page":$page,"page_size":50,"total":$total,"pages":$pages}"""

    private fun enqueueBugs(body: String) = bugsQ.add(ok(body))

    // --- pure model logic --------------------------------------------------
    @Test
    fun `allItems flattens pages and hasMore reflects last page`() {
        val model = BugListScreenModel(
            pages = listOf(
                BugListPage(items = emptyList(), page = 1, totalPages = 3, total = 10),
                BugListPage(items = emptyList(), page = 2, totalPages = 3, total = 10),
            ),
        )
        assertThat(model.allItems).isEmpty()
        assertThat(model.hasMore).isTrue()
        val last = model.copy(
            pages = listOf(BugListPage(items = emptyList(), page = 3, totalPages = 3, total = 10)),
        )
        assertThat(last.hasMore).isFalse()
    }

    @Test
    fun `hasMore is false when there are no pages`() {
        assertThat(BugListScreenModel().hasMore).isFalse()
    }

    // --- BugListFilters pure logic ----------------------------------------
    @Test
    fun `filters toggle add and remove and reset page`() {
        val f = BugListFilters()
        val s = f.toggleStatus("New")
        assertThat(s.statuses).containsExactly("New")
        assertThat(s.toggleStatus("New").statuses).isEmpty()
        assertThat(f.togglePriority("High").priorities).containsExactly("High")
        assertThat(f.toggleEnvironment("DEV").environments).containsExactly("DEV")
        assertThat(f.toggleItemType("Bug").itemTypes).containsExactly("Bug")
        assertThat(f.toggleProject(7).projectIds).containsExactly(7)
        assertThat(f.toggleAssignee(9).assigneeIds).containsExactly(9)
        assertThat(f.setOnlyItemType("Task").itemTypes).containsExactly("Task")
        assertThat(f.setOnlyItemType(null).itemTypes).isEmpty()
    }

    @Test
    fun `withQuery blank becomes null and hasActiveFilters tracks state`() {
        assertThat(BugListFilters().withQuery("   ").query).isNull()
        assertThat(BugListFilters().withQuery("hi").query).isEqualTo("hi")
        assertThat(BugListFilters().hasActiveFilters).isFalse()
        assertThat(BugListFilters().withQuery("hi").hasActiveFilters).isTrue()
        assertThat(BugListFilters().toggleStatus("New").hasActiveFilters).isTrue()
    }

    @Test
    fun `cleared keeps pageSize and toRepo maps fields`() {
        val cleared = BugListFilters(pageSize = 25, statuses = setOf("New")).cleared()
        assertThat(cleared.pageSize).isEqualTo(25)
        assertThat(cleared.statuses).isEmpty()
        val repo = BugListFilters(statuses = setOf("New"), projectIds = setOf(1), page = 2).toRepo()
        assertThat(repo.statuses).containsExactly("New")
        assertThat(repo.projectIds).containsExactly(1)
        assertThat(repo.page).isEqualTo(2)
    }

    // --- init load --------------------------------------------------------
    @Test
    fun `init loads facets then bugs into Success`() = runBlocking {
        enqueueBugs(bugsPage(items = BUG_ROW))
        val vm = newVm()
        awaitUntil { vm.state.value is UiState.Success }
        val data = (vm.state.value as UiState.Success).data
        assertThat(data.allItems).hasSize(1)
        assertThat(data.projects).hasSize(1)
        assertThat(data.users).hasSize(1)
    }

    @Test
    fun `init bugs error lands in Error state`() = runBlocking {
        bugsQ.add(err500())
        val vm = newVm()
        awaitUntil { vm.state.value is UiState.Error }
        assertThat(vm.state.value).isInstanceOf(UiState.Error::class.java)
    }

    @Test
    fun `facets failing still allows bugs Success`() = runBlocking {
        // projects + users fail, bugs succeed -> still Success with empty facets
        projectsQ.add(err500())
        usersQ.add(err500())
        enqueueBugs(bugsPage())
        val vm = newVm()
        awaitUntil { vm.state.value is UiState.Success }
        val data = (vm.state.value as UiState.Success).data
        assertThat(data.projects).isEmpty()
        assertThat(data.users).isEmpty()
    }

    // --- actions ----------------------------------------------------------
    @Test
    fun `refresh reloads first page`() = runBlocking {
        enqueueBugs(bugsPage(items = BUG_ROW))
        val vm = newVm()
        awaitUntil { vm.state.value is UiState.Success }
        enqueueBugs(bugsPage(items = BUG_ROW))
        vm.refresh()
        awaitUntil { vm.state.value is UiState.Success && (vm.state.value as UiState.Success).data.pages.size == 1 }
        assertThat((vm.state.value as UiState.Success).data.allItems).hasSize(1)
    }

    @Test
    fun `loadMore appends next page`() = runBlocking {
        // first page reports 2 total pages -> hasMore true
        enqueueBugs(bugsPage(items = BUG_ROW, page = 1, pages = 2, total = 2))
        val vm = newVm()
        awaitUntil { vm.state.value is UiState.Success }
        enqueueBugs(bugsPage(items = BUG_ROW_2, page = 2, pages = 2, total = 2))
        vm.loadMore()
        awaitUntil { (vm.state.value as? UiState.Success)?.data?.allItems?.size == 2 }
        val data = (vm.state.value as UiState.Success).data
        assertThat(data.allItems.map { it.id }).containsExactly(1, 2)
        assertThat(data.isLoadingMore).isFalse()
    }

    @Test
    fun `loadMore is a no-op when there is no more`() = runBlocking {
        enqueueBugs(bugsPage(items = BUG_ROW, page = 1, pages = 1, total = 1))
        val vm = newVm()
        awaitUntil { vm.state.value is UiState.Success }
        val before = server.requestCount
        vm.loadMore()
        delay(80)
        assertThat(server.requestCount).isEqualTo(before)
    }

    @Test
    fun `loadMore error keeps state Success`() = runBlocking {
        enqueueBugs(bugsPage(items = BUG_ROW, page = 1, pages = 2, total = 2))
        val vm = newVm()
        awaitUntil { vm.state.value is UiState.Success }
        bugsQ.add(err500())
        vm.loadMore()
        awaitUntil { (vm.state.value as? UiState.Success)?.data?.isLoadingMore == false && server.requestCount >= 4 }
        assertThat(vm.state.value).isInstanceOf(UiState.Success::class.java)
    }

    @Test
    fun `applyFilters via onQueryChange refetches`() = runBlocking {
        enqueueBugs(bugsPage(items = BUG_ROW))
        val vm = newVm()
        awaitUntil { vm.state.value is UiState.Success }
        enqueueBugs(bugsPage(items = BUG_ROW))
        vm.onQueryChange("crash")
        awaitUntil { (vm.state.value as? UiState.Success)?.data?.filters?.query == "crash" }
        assertThat((vm.state.value as UiState.Success).data.filters.query).isEqualTo("crash")
    }

    @Test
    fun `clearFilters resets filters and refetches`() = runBlocking {
        enqueueBugs(bugsPage(items = BUG_ROW))
        val vm = newVm()
        awaitUntil { vm.state.value is UiState.Success }
        enqueueBugs(bugsPage(items = BUG_ROW))
        vm.toggleStatus("New")
        awaitUntil { (vm.state.value as? UiState.Success)?.data?.filters?.statuses?.contains("New") == true }
        enqueueBugs(bugsPage(items = BUG_ROW))
        vm.clearFilters()
        awaitUntil { (vm.state.value as? UiState.Success)?.data?.filters?.statuses?.isEmpty() == true }
        assertThat((vm.state.value as UiState.Success).data.filters.hasActiveFilters).isFalse()
    }

    @Test
    fun `toggle helpers route through applyFilters`() = runBlocking {
        enqueueBugs(bugsPage(items = BUG_ROW))
        val vm = newVm()
        awaitUntil { vm.state.value is UiState.Success }
        // each toggle triggers a refresh -> enqueue a response per call
        repeat(6) { enqueueBugs(bugsPage(items = BUG_ROW)) }
        vm.togglePriority("High")
        vm.toggleEnvironment("DEV")
        vm.toggleItemType("Task")
        vm.setOnlyItemType("Bug")
        vm.toggleProject(1)
        vm.toggleAssignee(1)
        awaitUntil { (vm.state.value as? UiState.Success)?.data?.filters?.assigneeIds?.contains(1) == true }
        val f = (vm.state.value as UiState.Success).data.filters
        assertThat(f.priorities).contains("High")
        assertThat(f.projectIds).contains(1)
    }

    private companion object {
        const val PROJECT = """
            {"id":1,"name":"Apollo","key":"APO","description":"d","color":"#c9764f",
             "created_at":"2026-01-01T00:00:00Z","updated_at":"2026-01-01T00:00:00Z",
             "can_manage":true,"member_count":3}
        """
        const val USER_ROW = """
            {"id":1,"name":"u","email":"u@example.com","role":"admin","is_active":true,
             "created_at":"2026-01-01T00:00:00Z","updated_at":"2026-01-01T00:00:00Z"}
        """
        const val BUG_ROW = """
            {"id":1,"project_id":1,"item_type":"Bug","title":"t","status":"New","priority":"Medium",
             "environment":"DEV","created_at":"2026-01-01T00:00:00Z","updated_at":"2026-01-01T00:00:00Z"}
        """
        const val BUG_ROW_2 = """
            {"id":2,"project_id":1,"item_type":"Bug","title":"t2","status":"New","priority":"Medium",
             "environment":"DEV","created_at":"2026-01-01T00:00:00Z","updated_at":"2026-01-01T00:00:00Z"}
        """
    }
}
