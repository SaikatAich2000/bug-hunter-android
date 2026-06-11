package com.bughunter.feature.settings.changePassword

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
class ChangePasswordViewModelTest {

    private lateinit var server: MockWebServer
    private lateinit var vm: ChangePasswordViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Unconfined)
        server = MockWebServer().apply { start() }
        vm = ChangePasswordViewModel(AuthRepoTestFactory.create(server))
    }

    @After
    fun tearDown() {
        runCatching { server.shutdown() }
        Dispatchers.resetMain()
    }

    private suspend fun awaitUntil(timeoutMs: Long = 4_000, check: () -> Boolean) {
        withTimeout(timeoutMs) { while (!check()) delay(10) }
    }

    // --- Derived UI state (pure) ------------------------------------------
    @Test
    fun `strength label and fraction track the new password`() {
        vm.onNewChange("")
        assertThat(vm.state.value.strengthLabel).isEqualTo("")
        vm.onNewChange("abc")
        assertThat(vm.state.value.strengthLabel).isEqualTo("Too short")
        vm.onNewChange("abcdefgh")          // 8 chars, no digit
        assertThat(vm.state.value.strengthLabel).isEqualTo("Add a letter and a digit")
        vm.onNewChange("abcd1234")          // floor met, < 14
        assertThat(vm.state.value.strengthLabel).isEqualTo("OK")
        vm.onNewChange("abcd1234efgh56")    // >= 14
        assertThat(vm.state.value.strengthLabel).isEqualTo("Strong")
        assertThat(vm.state.value.strengthFraction).isEqualTo(1f)
    }

    @Test
    fun `passwordsMatch only when non-empty and equal`() {
        vm.onNewChange("abcd1234")
        vm.onConfirmChange("abcd1234")
        assertThat(vm.state.value.passwordsMatch).isTrue()
        vm.onConfirmChange("different")
        assertThat(vm.state.value.passwordsMatch).isFalse()
    }

    // --- submit() guards ---------------------------------------------------
    @Test
    fun `submit is a no-op when the floor is unmet`() = runBlocking {
        vm.onCurrentChange("old")
        vm.onNewChange("weak")              // fails strengthFloor
        vm.onConfirmChange("weak")
        vm.submit()
        delay(50)
        assertThat(server.requestCount).isEqualTo(0)
    }

    @Test
    fun `submit is a no-op when passwords differ`() = runBlocking {
        vm.onCurrentChange("old")
        vm.onNewChange("abcd1234")
        vm.onConfirmChange("abcd9999")
        vm.submit()
        delay(50)
        assertThat(server.requestCount).isEqualTo(0)
    }

    // --- submit() network paths -------------------------------------------
    @Test
    fun `successful change marks finished`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(204))
        vm.onCurrentChange("oldpass1")
        vm.onNewChange("abcd1234")
        vm.onConfirmChange("abcd1234")
        vm.submit()
        awaitUntil { vm.state.value.finished }
        assertThat(vm.state.value.error).isNull()
    }

    @Test
    fun `breach validation error sets breachRejected`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(422)
                .setBody("""{"detail":"This password appeared in a known breach."}"""),
        )
        vm.onCurrentChange("oldpass1")
        vm.onNewChange("abcd1234")
        vm.onConfirmChange("abcd1234")
        vm.submit()
        awaitUntil { !vm.state.value.isSubmitting }
        assertThat(vm.state.value.finished).isFalse()
        assertThat(vm.state.value.error).isNotNull()
        assertThat(vm.state.value.breachRejected).isTrue()
    }
}
