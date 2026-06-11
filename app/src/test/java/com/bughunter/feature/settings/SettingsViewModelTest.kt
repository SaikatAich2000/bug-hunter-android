package com.bughunter.feature.settings

import com.bughunter.core.data.local.AppPrefs
import com.bughunter.core.data.repository.AuthRepoTestFactory
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
class SettingsViewModelTest {

    private lateinit var server: MockWebServer
    private lateinit var appPrefs: AppPrefs

    @Before
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Unconfined)
        server = MockWebServer().apply { start() }
        appPrefs = AuthRepoTestFactory.appPrefs()
    }

    @After
    fun tearDown() {
        runCatching { server.shutdown() }
        Dispatchers.resetMain()
    }

    private fun buildVm(): SettingsViewModel =
        SettingsViewModel(appPrefs, AuthRepoTestFactory.create(server))

    private suspend fun awaitUntil(timeoutMs: Long = 4_000, check: () -> Boolean) {
        withTimeout(timeoutMs) { while (!check()) delay(10) }
    }

    @Test
    fun `initial state is collected from AppPrefs flows`() = runBlocking {
        appPrefs.setThemeMode(AppPrefs.ThemeMode.DARK)
        appPrefs.setDefaultNewType("Idea")
        appPrefs.setBaseUrl("https://example.com")
        val vm = buildVm()
        awaitUntil { vm.state.value.themeMode == AppPrefs.ThemeMode.DARK }
        awaitUntil { vm.state.value.defaultNewType == "Idea" }
        awaitUntil { vm.state.value.baseUrl == "https://example.com/" }
        assertThat(vm.state.value.isDebug).isFalse()
    }

    @Test
    fun `default initial state matches AppPrefs defaults`() = runBlocking {
        val vm = buildVm()
        awaitUntil { vm.state.value.defaultNewType == "Bug" }
        assertThat(vm.state.value.themeMode).isEqualTo(AppPrefs.ThemeMode.SYSTEM)
    }

    @Test
    fun `setIsDebug updates state`() {
        val vm = buildVm()
        vm.setIsDebug(true)
        assertThat(vm.state.value.isDebug).isTrue()
        vm.setIsDebug(false)
        assertThat(vm.state.value.isDebug).isFalse()
    }

    @Test
    fun `onThemeModeChange persists to AppPrefs and updates state`() = runBlocking {
        val vm = buildVm()
        vm.onThemeModeChange(AppPrefs.ThemeMode.LIGHT)
        awaitUntil { vm.state.value.themeMode == AppPrefs.ThemeMode.LIGHT }
        Unit
    }

    @Test
    fun `onDefaultNewTypeChange persists to AppPrefs and updates state`() = runBlocking {
        val vm = buildVm()
        vm.onDefaultNewTypeChange("Task")
        awaitUntil { vm.state.value.defaultNewType == "Task" }
        Unit
    }

    @Test
    fun `onBaseUrlChange persists to AppPrefs and updates state`() = runBlocking {
        val vm = buildVm()
        vm.onBaseUrlChange("https://override.example/")
        awaitUntil { vm.state.value.baseUrl == "https://override.example/" }
        Unit
    }

    @Test
    fun `deleteAccount success sets isDeleted`() = runBlocking {
        val vm = buildVm()
        server.enqueue(MockResponse().setResponseCode(204))
        vm.deleteAccount("hunter2")
        awaitUntil { vm.deleteState.value.isDeleted }
        assertThat(vm.deleteState.value.isSubmitting).isFalse()
        assertThat(vm.deleteState.value.error).isNull()
    }

    @Test
    fun `deleteAccount error surfaces the failure`() = runBlocking {
        val vm = buildVm()
        server.enqueue(MockResponse().setResponseCode(400).setBody("""{"detail":"wrong password"}"""))
        vm.deleteAccount("nope")
        awaitUntil { vm.deleteState.value.error != null }
        assertThat(vm.deleteState.value.isDeleted).isFalse()
        assertThat(vm.deleteState.value.isSubmitting).isFalse()
    }

    @Test
    fun `deleteAccount is a no-op while a submission is in flight`() = runBlocking {
        val vm = buildVm()
        // First call flips isSubmitting=true; before it completes there is no
        // enqueued response, so it stays in-flight. The second call must bail.
        vm.deleteAccount("first")
        awaitUntil { vm.deleteState.value.isSubmitting }
        vm.deleteAccount("second")
        delay(50)
        assertThat(server.requestCount).isEqualTo(1)
    }

    @Test
    fun `dismissDeleteError clears the error`() = runBlocking {
        val vm = buildVm()
        server.enqueue(MockResponse().setResponseCode(400).setBody("""{"detail":"wrong password"}"""))
        vm.deleteAccount("nope")
        awaitUntil { vm.deleteState.value.error != null }
        vm.dismissDeleteError()
        assertThat(vm.deleteState.value.error).isNull()
    }

    @Test
    fun `resetDeleteState restores the default delete state`() = runBlocking {
        val vm = buildVm()
        server.enqueue(MockResponse().setResponseCode(204))
        vm.deleteAccount("hunter2")
        awaitUntil { vm.deleteState.value.isDeleted }
        vm.resetDeleteState()
        assertThat(vm.deleteState.value.isDeleted).isFalse()
        assertThat(vm.deleteState.value.isSubmitting).isFalse()
        assertThat(vm.deleteState.value.error).isNull()
    }
}
