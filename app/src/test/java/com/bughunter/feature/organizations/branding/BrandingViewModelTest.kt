package com.bughunter.feature.organizations.branding

import com.bughunter.core.data.repository.BrandingRepository
import com.bughunter.core.data.repository.RepoTestSupport
import com.bughunter.core.network.api.BrandingApi
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
class BrandingViewModelTest {

    private lateinit var server: MockWebServer
    private lateinit var repo: BrandingRepository
    private val moshi = RepoTestSupport.moshi()
    private val mapper = RepoTestSupport.errorMapper(moshi)
    private val jar = RepoTestSupport.cookieJar()

    @Before
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Unconfined)
        server = MockWebServer().apply { start() }
        RepoTestSupport.seedCsrf(jar, server.url("/"))
        val api = RepoTestSupport.retrofit(server, moshi, client = RepoTestSupport.client(jar))
            .create(BrandingApi::class.java)
        repo = BrandingRepository(api, mapper)
    }

    @After
    fun tearDown() {
        runCatching { server.shutdown() }
        Dispatchers.resetMain()
    }

    private suspend fun awaitUntil(timeoutMs: Long = 4_000, check: () -> Boolean) {
        withTimeout(timeoutMs) { while (!check()) delay(10) }
    }

    private fun vmAfterLoad(body: String): BrandingViewModel {
        server.enqueue(MockResponse().setResponseCode(200).setBody(body))
        return BrandingViewModel(repo)
    }

    @Test
    fun `load maps accent color, falling back to default when absent`() = runBlocking {
        val vm = vmAfterLoad("""{"accent_color":"#abcdef","logo_data_url":"data:x"}""")
        awaitUntil { vm.state.value is UiState.Success }
        val data = (vm.state.value as UiState.Success).data
        assertThat(data.primary).isEqualTo("#abcdef")
        assertThat(data.logoDataUrl).isEqualTo("data:x")
    }

    @Test
    fun `load uses default accent when server returns null`() = runBlocking {
        val vm = vmAfterLoad("{}")
        awaitUntil { vm.state.value is UiState.Success }
        assertThat((vm.state.value as UiState.Success).data.primary).isEqualTo("#6366f1")
    }

    @Test
    fun `load failure lands in Error state`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(500).setBody("""{"detail":"x"}"""))
        val vm = BrandingViewModel(repo)
        awaitUntil { vm.state.value is UiState.Error }
        assertThat(vm.state.value).isInstanceOf(UiState.Error::class.java)
    }

    @Test
    fun `edits mutate draft colors`() = runBlocking {
        val vm = vmAfterLoad("{}")
        awaitUntil { vm.state.value is UiState.Success }
        vm.onPrimary("#111111")
        vm.onAccent("#222222")
        val data = (vm.state.value as UiState.Success).data
        assertThat(data.primary).isEqualTo("#111111")
        assertThat(data.accent).isEqualTo("#222222")
    }

    @Test
    fun `save success invokes onDone(true)`() = runBlocking {
        val vm = vmAfterLoad("{}")
        awaitUntil { vm.state.value is UiState.Success }
        server.enqueue(MockResponse().setResponseCode(200).setBody("{}"))
        var done: Boolean? = null
        vm.save { done = it }
        awaitUntil { done != null }
        assertThat(done).isTrue()
    }

    @Test
    fun `save failure invokes onDone(false) and sets saveError`() = runBlocking {
        val vm = vmAfterLoad("{}")
        awaitUntil { vm.state.value is UiState.Success }
        server.enqueue(MockResponse().setResponseCode(403).setBody("""{"detail":"no"}"""))
        var done: Boolean? = null
        vm.save { done = it }
        awaitUntil { done != null }
        assertThat(done).isFalse()
        assertThat((vm.state.value as UiState.Success).data.saveError).isNotNull()
    }
}
