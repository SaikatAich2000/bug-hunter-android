package com.bughunter.feature.organizations.settings

import com.bughunter.core.data.repository.OrganizationRepository
import com.bughunter.core.data.repository.RepoTestSupport
import com.bughunter.core.network.api.OrganizationApi
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
class OrganizationViewModelTest {

    private lateinit var server: MockWebServer
    private lateinit var repo: OrganizationRepository
    private val moshi = RepoTestSupport.moshi()
    private val mapper = RepoTestSupport.errorMapper(moshi)
    private val jar = RepoTestSupport.cookieJar()

    @Before
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Unconfined)
        server = MockWebServer().apply { start() }
        RepoTestSupport.seedCsrf(jar, server.url("/"))
        val api = RepoTestSupport.retrofit(server, moshi, client = RepoTestSupport.client(jar))
            .create(OrganizationApi::class.java)
        repo = OrganizationRepository(api, mapper)
    }

    @After
    fun tearDown() {
        runCatching { server.shutdown() }
        Dispatchers.resetMain()
    }

    private suspend fun awaitUntil(timeoutMs: Long = 4_000, check: () -> Boolean) {
        withTimeout(timeoutMs) { while (!check()) delay(10) }
    }

    private fun vmAfterLoad(body: String): OrganizationViewModel {
        server.enqueue(MockResponse().setResponseCode(200).setBody(body))
        return OrganizationViewModel(repo)
    }

    @Test
    fun `load maps the org into editable fields`() = runBlocking {
        val vm = vmAfterLoad(ORG_JSON)
        awaitUntil { vm.state.value is UiState.Success }
        val data = (vm.state.value as UiState.Success).data
        assertThat(data.name).isEqualTo("Acme")
        assertThat(data.description).isEqualTo("We make things")
    }

    @Test
    fun `load failure lands in Error state`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(500).setBody("""{"detail":"x"}"""))
        val vm = OrganizationViewModel(repo)
        awaitUntil { vm.state.value is UiState.Error }
        assertThat(vm.state.value).isInstanceOf(UiState.Error::class.java)
    }

    @Test
    fun `edits mutate the draft fields`() = runBlocking {
        val vm = vmAfterLoad(ORG_JSON)
        awaitUntil { vm.state.value is UiState.Success }
        vm.onName("Renamed")
        vm.onDescription("New desc")
        val data = (vm.state.value as UiState.Success).data
        assertThat(data.name).isEqualTo("Renamed")
        assertThat(data.description).isEqualTo("New desc")
    }

    @Test
    fun `save success invokes onDone(true) and updates org`() = runBlocking {
        val vm = vmAfterLoad(ORG_JSON)
        awaitUntil { vm.state.value is UiState.Success }
        vm.onName("Acme Two")
        server.enqueue(MockResponse().setResponseCode(200).setBody(ORG_JSON_RENAMED))
        var done: Boolean? = null
        vm.save { done = it }
        awaitUntil { done != null }
        assertThat(done).isTrue()
        assertThat((vm.state.value as UiState.Success).data.saving).isFalse()
    }

    @Test
    fun `save failure invokes onDone(false) and sets saveError`() = runBlocking {
        val vm = vmAfterLoad(ORG_JSON)
        awaitUntil { vm.state.value is UiState.Success }
        server.enqueue(MockResponse().setResponseCode(403).setBody("""{"detail":"nope"}"""))
        var done: Boolean? = null
        vm.save { done = it }
        awaitUntil { done != null }
        assertThat(done).isFalse()
        assertThat((vm.state.value as UiState.Success).data.saveError).isNotNull()
    }

    companion object {
        private const val ORG_JSON = """
            {"id":1,"name":"Acme","slug":"acme","description":"We make things",
             "created_at":"2026-01-01T00:00:00Z"}
        """
        private const val ORG_JSON_RENAMED = """
            {"id":1,"name":"Acme Two","slug":"acme","description":"We make things",
             "created_at":"2026-01-01T00:00:00Z"}
        """
    }
}
