package com.bughunter.feature.bugs.create

import com.bughunter.core.data.repository.BugsRepository
import com.bughunter.core.data.repository.ProjectsRepository
import com.bughunter.core.data.repository.RepoTestSupport
import com.bughunter.core.data.repository.UsersRepository
import com.bughunter.core.network.api.BugsApi
import com.bughunter.core.network.api.ProjectsApi
import com.bughunter.core.network.api.UsersApi
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
class BugCreateViewModelTest {

    private lateinit var server: MockWebServer
    private lateinit var bugsRepo: BugsRepository
    private lateinit var projectsRepo: ProjectsRepository
    private lateinit var usersRepo: UsersRepository

    private val moshi = RepoTestSupport.moshi()
    private val mapper = RepoTestSupport.errorMapper(moshi)
    private val jar = RepoTestSupport.cookieJar()

    @Before
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Unconfined)
        server = MockWebServer().apply { start() }
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

    private fun newVm() = BugCreateViewModel(bugsRepo, projectsRepo, usersRepo)

    // init load order: projects.list() then users.list()
    private fun enqueueDropdowns() {
        server.enqueue(MockResponse().setResponseCode(200).setBody("[$PROJECT]"))
        server.enqueue(MockResponse().setResponseCode(200).setBody("[$USER_ROW]"))
    }

    // --- init -------------------------------------------------------------
    @Test
    fun `init loads projects then users into state`() = runBlocking {
        enqueueDropdowns()
        val vm = newVm()
        awaitUntil { vm.state.value.projects.isNotEmpty() && vm.state.value.users.isNotEmpty() }
        assertThat(vm.state.value.projects).hasSize(1)
        assertThat(vm.state.value.users).hasSize(1)
    }

    @Test
    fun `init tolerates dropdown load failures`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(500).setBody("""{"detail":"x"}"""))
        server.enqueue(MockResponse().setResponseCode(500).setBody("""{"detail":"x"}"""))
        val vm = newVm()
        delay(80)
        assertThat(vm.state.value.projects).isEmpty()
        assertThat(vm.state.value.users).isEmpty()
    }

    // --- form setters (pure) ---------------------------------------------
    @Test
    fun `form setters update form state`() = runBlocking {
        enqueueDropdowns()
        val vm = newVm()
        awaitUntil { vm.state.value.projects.isNotEmpty() }
        vm.onTitleChange("Crash on save")
        vm.onDescriptionChange("repro steps")
        vm.onProjectSelect(1)
        vm.onItemTypeSelect("Task")
        vm.onStatusSelect("In Progress")
        vm.onPrioritySelect("High")
        vm.onEnvironmentSelect("PROD")
        vm.onDueDateChange("2026-12-01")
        vm.toggleAssignee(1)
        val form = vm.state.value.form
        assertThat(form.title).isEqualTo("Crash on save")
        assertThat(form.description).isEqualTo("repro steps")
        assertThat(form.projectId).isEqualTo(1)
        assertThat(form.itemType).isEqualTo("Task")
        assertThat(form.status).isEqualTo("In Progress")
        assertThat(form.priority).isEqualTo("High")
        assertThat(form.environment).isEqualTo("PROD")
        assertThat(form.dueDate).isEqualTo("2026-12-01")
        assertThat(form.assigneeIds).containsExactly(1)
        assertThat(form.canSubmit).isTrue()
    }

    @Test
    fun `toggleAssignee removes when already present`() = runBlocking {
        enqueueDropdowns()
        val vm = newVm()
        awaitUntil { vm.state.value.projects.isNotEmpty() }
        vm.toggleAssignee(5)
        assertThat(vm.state.value.form.assigneeIds).containsExactly(5)
        vm.toggleAssignee(5)
        assertThat(vm.state.value.form.assigneeIds).isEmpty()
    }

    // --- submit -----------------------------------------------------------
    @Test
    fun `submit success sets createdBugId and consumeCreated clears it`() = runBlocking {
        enqueueDropdowns()
        val vm = newVm()
        awaitUntil { vm.state.value.projects.isNotEmpty() }
        vm.onProjectSelect(1)
        vm.onTitleChange("A valid title")
        server.enqueue(MockResponse().setResponseCode(201).setBody(BUG_OUT))
        vm.submit()
        awaitUntil { vm.state.value.createdBugId != null }
        assertThat(vm.state.value.createdBugId).isEqualTo(1)
        assertThat(vm.state.value.form.isSubmitting).isFalse()
        vm.consumeCreated()
        assertThat(vm.state.value.createdBugId).isNull()
    }

    @Test
    fun `submit error records form error`() = runBlocking {
        enqueueDropdowns()
        val vm = newVm()
        awaitUntil { vm.state.value.projects.isNotEmpty() }
        vm.onProjectSelect(1)
        vm.onTitleChange("A valid title")
        server.enqueue(MockResponse().setResponseCode(409).setBody("""{"detail":"exists"}"""))
        vm.submit()
        awaitUntil { vm.state.value.form.error != null }
        assertThat(vm.state.value.form.error).isNotNull()
        assertThat(vm.state.value.form.isSubmitting).isFalse()
        assertThat(vm.state.value.createdBugId).isNull()
    }

    @Test
    fun `submit guard blocks when title too short`() = runBlocking {
        enqueueDropdowns()
        val vm = newVm()
        awaitUntil { vm.state.value.projects.isNotEmpty() }
        vm.onProjectSelect(1)
        vm.onTitleChange("ab") // < 3 chars -> canSubmit false
        val before = server.requestCount
        vm.submit()
        delay(120)
        assertThat(server.requestCount).isEqualTo(before)
        assertThat(vm.state.value.createdBugId).isNull()
    }

    @Test
    fun `submit guard blocks when no project selected`() = runBlocking {
        enqueueDropdowns()
        val vm = newVm()
        awaitUntil { vm.state.value.projects.isNotEmpty() }
        vm.onTitleChange("A valid title") // projectId still null -> toCreate() returns null
        val before = server.requestCount
        vm.submit()
        delay(120)
        assertThat(server.requestCount).isEqualTo(before)
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
        const val BUG_OUT = """
            {"id":1,"project_id":1,"item_type":"Bug","title":"A valid title","status":"New",
             "priority":"Medium","environment":"DEV",
             "created_at":"2026-01-01T00:00:00Z","updated_at":"2026-01-01T00:00:00Z"}
        """
    }
}
