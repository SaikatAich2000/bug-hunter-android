package com.bughunter.feature.bugs.savedViews

import com.bughunter.core.data.repository.RepoTestSupport
import com.bughunter.core.data.repository.SavedViewsRepository
import com.bughunter.core.network.api.SavedViewsApi
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
class SavedViewsViewModelTest {

    private lateinit var server: MockWebServer
    private lateinit var repo: SavedViewsRepository
    private val moshi = RepoTestSupport.moshi()
    private val mapper = RepoTestSupport.errorMapper(moshi)
    private val jar = RepoTestSupport.cookieJar()

    @Before
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Unconfined)
        server = MockWebServer().apply { start() }
        RepoTestSupport.seedCsrf(jar, server.url("/"))
        val api = RepoTestSupport.retrofit(server, moshi, client = RepoTestSupport.client(jar))
            .create(SavedViewsApi::class.java)
        repo = SavedViewsRepository(api, mapper)
    }

    @After
    fun tearDown() {
        runCatching { server.shutdown() }
        Dispatchers.resetMain()
    }

    private suspend fun awaitUntil(timeoutMs: Long = 4_000, check: () -> Boolean) {
        withTimeout(timeoutMs) { while (!check()) delay(10) }
    }

    private fun vmAfterLoad(body: String): SavedViewsViewModel {
        server.enqueue(MockResponse().setResponseCode(200).setBody(body))
        return SavedViewsViewModel(repo)
    }

    @Test
    fun `initial load succeeds with the returned views`() = runBlocking {
        val vm = vmAfterLoad("[${view(1, "Mine")}]")
        awaitUntil { vm.state.value is UiState.Success }
        assertThat((vm.state.value as UiState.Success).data.views).hasSize(1)
    }

    @Test
    fun `load failure lands in Error state`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(500).setBody("""{"detail":"x"}"""))
        val vm = SavedViewsViewModel(repo)
        awaitUntil { vm.state.value is UiState.Error }
        assertThat(vm.state.value).isInstanceOf(UiState.Error::class.java)
    }

    @Test
    fun `saveCurrent with blank draft name is a no-op`() = runBlocking {
        val vm = vmAfterLoad("[]")
        awaitUntil { vm.state.value is UiState.Success }
        val before = server.requestCount
        vm.saveCurrent(mapOf("status" to "Open"))   // draftName is empty
        delay(50)
        assertThat(server.requestCount).isEqualTo(before)
    }

    @Test
    fun `saveCurrent appends the created view and clears the draft`() = runBlocking {
        val vm = vmAfterLoad("[]")
        awaitUntil { vm.state.value is UiState.Success }
        vm.onDraftNameChange("High priority")
        server.enqueue(MockResponse().setResponseCode(201).setBody(view(7, "High priority")))
        vm.saveCurrent(mapOf("priority" to "High"))
        awaitUntil {
            val s = vm.state.value
            s is UiState.Success && s.data.views.any { it.id == 7 }
        }
        val data = (vm.state.value as UiState.Success).data
        assertThat(data.draftName).isEmpty()
        assertThat(data.isSaving).isFalse()
    }

    @Test
    fun `delete removes the view from state on success`() = runBlocking {
        val vm = vmAfterLoad("[${view(1, "A")},${view(2, "B")}]")
        awaitUntil { vm.state.value is UiState.Success }
        server.enqueue(MockResponse().setResponseCode(204))
        vm.delete(1)
        awaitUntil {
            val s = vm.state.value
            s is UiState.Success && s.data.views.none { it.id == 1 }
        }
        assertThat((vm.state.value as UiState.Success).data.views.map { it.id }).containsExactly(2)
        Unit  // keep the @Test method's return type Unit (containsExactly returns Ordered)
    }

    private fun view(id: Int, name: String): String = """
        {"id":$id,"name":"$name","owner_user_id":1,"is_shared":false,
         "filters":{},"created_at":"2026-01-01T00:00:00Z"}
    """.trimIndent()
}
