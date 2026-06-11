package com.bughunter.feature.projects.settings

import com.bughunter.core.data.repository.ProjectsRepository
import com.bughunter.core.data.repository.RepoTestSupport
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
class ProjectSettingsViewModelTest {

    private lateinit var server: MockWebServer
    private lateinit var repo: ProjectsRepository
    private val moshi = RepoTestSupport.moshi()
    private val mapper = RepoTestSupport.errorMapper(moshi)
    private val jar = RepoTestSupport.cookieJar()

    @Before
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Unconfined)
        server = MockWebServer().apply { start() }
        RepoTestSupport.seedCsrf(jar, server.url("/"))
        val api = RepoTestSupport.retrofit(server, moshi, client = RepoTestSupport.client(jar))
            .create(ProjectsApi::class.java)
        repo = ProjectsRepository(api, mapper)
    }

    @After
    fun tearDown() {
        runCatching { server.shutdown() }
        Dispatchers.resetMain()
    }

    private suspend fun awaitUntil(timeoutMs: Long = 4_000, check: () -> Boolean) {
        withTimeout(timeoutMs) { while (!check()) delay(10) }
    }

    private fun loadedVm(): ProjectSettingsViewModel {
        server.enqueue(MockResponse().setResponseCode(200).setBody(PROJECT_JSON))
        val vm = ProjectSettingsViewModel(repo)
        vm.load(1)
        return vm
    }

    @Test
    fun `load maps the project into editable fields`() = runBlocking {
        val vm = loadedVm()
        awaitUntil { vm.state.value is UiState.Success }
        val data = (vm.state.value as UiState.Success).data
        assertThat(data.name).isEqualTo("Apollo")
        assertThat(data.key).isEqualTo("APOLLO")
        assertThat(data.defaultItemType).isEqualTo("Bug")
    }

    @Test
    fun `load failure lands in Error state`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(404).setBody("""{"detail":"gone"}"""))
        val vm = ProjectSettingsViewModel(repo)
        vm.load(99)
        awaitUntil { vm.state.value is UiState.Error }
        assertThat(vm.state.value).isInstanceOf(UiState.Error::class.java)
    }

    @Test
    fun `onKey uppercases the entered value`() = runBlocking {
        val vm = loadedVm()
        awaitUntil { vm.state.value is UiState.Success }
        vm.onKey("newkey")
        assertThat((vm.state.value as UiState.Success).data.key).isEqualTo("NEWKEY")
    }

    @Test
    fun `save success invokes onDone(true)`() = runBlocking {
        val vm = loadedVm()
        awaitUntil { vm.state.value is UiState.Success }
        vm.onName("Apollo 2")
        server.enqueue(MockResponse().setResponseCode(200).setBody(PROJECT_JSON))
        var done: Boolean? = null
        vm.save { done = it }
        awaitUntil { done != null }
        assertThat(done).isTrue()
    }

    @Test
    fun `save failure invokes onDone(false) and sets saveError`() = runBlocking {
        val vm = loadedVm()
        awaitUntil { vm.state.value is UiState.Success }
        server.enqueue(MockResponse().setResponseCode(409).setBody("""{"detail":"dup key"}"""))
        var done: Boolean? = null
        vm.save { done = it }
        awaitUntil { done != null }
        assertThat(done).isFalse()
        assertThat((vm.state.value as UiState.Success).data.saveError).isNotNull()
    }

    companion object {
        private const val PROJECT_JSON = """
            {"id":1,"name":"Apollo","key":"APOLLO","color":"#6366f1",
             "description":"moon","member_count":3,"can_manage":true,
             "created_at":"2026-01-01T00:00:00Z","updated_at":"2026-01-01T00:00:00Z"}
        """
    }
}
