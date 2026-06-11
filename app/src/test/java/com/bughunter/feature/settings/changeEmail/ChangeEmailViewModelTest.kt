package com.bughunter.feature.settings.changeEmail

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
class ChangeEmailViewModelTest {

    private lateinit var server: MockWebServer
    private lateinit var vm: ChangeEmailViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Unconfined)
        server = MockWebServer().apply { start() }
        vm = ChangeEmailViewModel(AuthRepoTestFactory.create(server))
    }

    @After
    fun tearDown() {
        runCatching { server.shutdown() }
        Dispatchers.resetMain()
    }

    private suspend fun awaitUntil(timeoutMs: Long = 4_000, check: () -> Boolean) {
        withTimeout(timeoutMs) { while (!check()) delay(10) }
    }

    @Test
    fun `onCodeChange keeps digits only and caps at six`() {
        vm.onCodeChange("12ab34cd567")
        assertThat(vm.state.value.code).isEqualTo("123456")
    }

    @Test
    fun `requestChange is a no-op for invalid email`() = runBlocking {
        vm.onNewEmailChange("nope")
        vm.onPasswordChange("secret1")
        vm.requestChange()
        delay(50)
        assertThat(server.requestCount).isEqualTo(0)
    }

    @Test
    fun `requestChange is a no-op when password is blank`() = runBlocking {
        vm.onNewEmailChange("new@example.com")
        vm.onPasswordChange("   ")
        vm.requestChange()
        delay(50)
        assertThat(server.requestCount).isEqualTo(0)
    }

    @Test
    fun `requestChange success advances to CONFIRM and surfaces message`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody("""{"message":"Check your inbox"}"""),
        )
        vm.onNewEmailChange("new@example.com")
        vm.onPasswordChange("secret1")
        vm.requestChange()
        awaitUntil { vm.state.value.step == ChangeEmailStep.CONFIRM }
        assertThat(vm.state.value.message).isEqualTo("Check your inbox")
        // Password is wiped from state once the request succeeds.
        assertThat(vm.state.value.currentPassword).isEmpty()
    }

    @Test
    fun `requestChange error stays on REQUEST with an error`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(422).setBody("""{"detail":"Email already in use"}"""),
        )
        vm.onNewEmailChange("taken@example.com")
        vm.onPasswordChange("secret1")
        vm.requestChange()
        awaitUntil { !vm.state.value.isSubmitting }
        assertThat(vm.state.value.step).isEqualTo(ChangeEmailStep.REQUEST)
        assertThat(vm.state.value.error).isNotNull()
    }

    @Test
    fun `confirmChange is a no-op until the code is six digits`() = runBlocking {
        vm.onCodeChange("123")
        vm.confirmChange()
        delay(50)
        assertThat(server.requestCount).isEqualTo(0)
    }

    @Test
    fun `reset returns to the initial state`() {
        vm.onNewEmailChange("x@y.io")
        vm.onCodeChange("123456")
        vm.reset()
        assertThat(vm.state.value.newEmail).isEmpty()
        assertThat(vm.state.value.code).isEmpty()
        assertThat(vm.state.value.step).isEqualTo(ChangeEmailStep.REQUEST)
    }
}
