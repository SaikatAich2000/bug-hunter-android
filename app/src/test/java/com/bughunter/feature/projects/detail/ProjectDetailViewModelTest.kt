package com.bughunter.feature.projects.detail

import com.bughunter.core.data.repository.MembershipsRepository
import com.bughunter.core.data.repository.ProjectsRepository
import com.bughunter.core.data.repository.RepoTestSupport
import com.bughunter.core.network.api.MembershipsApi
import com.bughunter.core.network.api.ProjectsApi
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

@OptIn(ExperimentalCoroutinesApi::class)
internal class ProjectDetailViewModelTest {

    private lateinit var server: MockWebServer
    private lateinit var projectsRepo: ProjectsRepository
    private lateinit var membersRepo: MembershipsRepository
    private val moshi = RepoTestSupport.moshi()
    private val mapper = RepoTestSupport.errorMapper(moshi)
    private val jar = RepoTestSupport.cookieJar()

    @Before
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Unconfined)
        server = MockWebServer().apply { start() }
        RepoTestSupport.seedCsrf(jar, server.url("/"))
        val rf = RepoTestSupport.retrofit(server, moshi, client = RepoTestSupport.client(jar))
        projectsRepo = ProjectsRepository(rf.create(ProjectsApi::class.java), mapper)
        membersRepo = MembershipsRepository(rf.create(MembershipsApi::class.java), mapper)
    }

    @After
    fun tearDown() {
        runCatching { server.shutdown() }
        Dispatchers.resetMain()
    }

    private suspend fun awaitUntil(timeoutMs: Long = 4_000, check: () -> Boolean) {
        withTimeout(timeoutMs) { while (!check()) delay(10) }
    }

    private fun newVm() = ProjectDetailViewModel(projectsRepo, membersRepo)

    @Test
    fun `load success lands in Success state with project and members`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody(PROJECT))
        server.enqueue(MockResponse().setResponseCode(200).setBody("[$ROW]"))
        val vm = newVm()
        vm.load(1)
        awaitUntil { vm.state.value is UiState.Success }
        val data = (vm.state.value as UiState.Success).data
        assertThat(data.project.id).isEqualTo(1)
        assertThat(data.members).hasSize(1)
    }

    @Test
    fun `load project error lands in Error state`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(500).setBody("""{"detail":"boom"}"""))
        server.enqueue(MockResponse().setResponseCode(200).setBody("[$ROW]"))
        val vm = newVm()
        vm.load(1)
        awaitUntil { vm.state.value is UiState.Error }
        assertThat(vm.state.value).isInstanceOf(UiState.Error::class.java)
    }

    @Test
    fun `load members error lands in Error state`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody(PROJECT))
        server.enqueue(MockResponse().setResponseCode(403).setBody("""{"detail":"nope"}"""))
        val vm = newVm()
        vm.load(1)
        awaitUntil { vm.state.value is UiState.Error }
        assertThat(vm.state.value).isInstanceOf(UiState.Error::class.java)
    }

    @Test
    fun `reload after error reaches Success`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(500).setBody("""{"detail":"boom"}"""))
        server.enqueue(MockResponse().setResponseCode(200).setBody("[$ROW]"))
        val vm = newVm()
        vm.load(1)
        awaitUntil { vm.state.value is UiState.Error }
        server.enqueue(MockResponse().setResponseCode(200).setBody(PROJECT))
        server.enqueue(MockResponse().setResponseCode(200).setBody("[$ROW]"))
        vm.load(1)
        awaitUntil { vm.state.value is UiState.Success }
        assertThat((vm.state.value as UiState.Success).data.members).hasSize(1)
    }

    private companion object {
        const val PROJECT = """
            {"id":1,"name":"Apollo","key":"APO","description":"d","color":"#c9764f",
             "created_at":"2026-01-01T00:00:00Z","updated_at":"2026-01-01T00:00:00Z",
             "can_manage":true,"member_count":3}
        """
        const val ROW = """
            {"id":1,"user_id":2,"user_name":"x","user_email":"x@y.z","user_role":"member",
             "project_role":"member","created_at":"2026-01-01T00:00:00Z"}
        """
    }
}
