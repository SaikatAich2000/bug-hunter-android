package com.bughunter.feature.auth.reset

import androidx.lifecycle.SavedStateHandle
import com.bughunter.core.data.repository.AuthRepoTestFactory
import com.bughunter.core.nav.BhRoute
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
class ResetPasswordViewModelTest {

    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Unconfined)
        server = MockWebServer().apply { start() }
    }

    @After
    fun tearDown() {
        runCatching { server.shutdown() }
        Dispatchers.resetMain()
    }

    private fun buildVm(token: String = "tok-123"): ResetPasswordViewModel {
        val handle = SavedStateHandle(mapOf(BhRoute.ResetPassword.ARG_TOKEN to token))
        return ResetPasswordViewModel(handle, AuthRepoTestFactory.create(server))
    }

    private suspend fun awaitUntil(timeoutMs: Long = 4_000, check: () -> Boolean) {
        withTimeout(timeoutMs) { while (!check()) delay(10) }
    }

    @Test
    fun `token is read from the saved-state handle`() {
        assertThat(buildVm("abc").state.value.token).isEqualTo("abc")
    }

    @Test
    fun `strengthOk and passwordsMatch track the inputs`() {
        val vm = buildVm()
        vm.onNewPasswordChange("abcd1234")
        assertThat(vm.state.value.strengthOk).isTrue()
        vm.onConfirmPasswordChange("abcd1234")
        assertThat(vm.state.value.passwordsMatch).isTrue()
        vm.onConfirmPasswordChange("nope")
        assertThat(vm.state.value.passwordsMatch).isFalse()
    }

    @Test
    fun `onSubmit is a no-op when weak or mismatched`() = runBlocking {
        val vm = buildVm()
        vm.onNewPasswordChange("weak")
        vm.onConfirmPasswordChange("weak")
        vm.onSubmit()
        delay(50)
        assertThat(server.requestCount).isEqualTo(0)
    }

    @Test
    fun `onSubmit success sets isSuccess`() = runBlocking {
        val vm = buildVm()
        vm.onNewPasswordChange("abcd1234")
        vm.onConfirmPasswordChange("abcd1234")
        server.enqueue(MockResponse().setResponseCode(204))
        vm.onSubmit()
        awaitUntil { vm.state.value.isSuccess }
        assertThat(vm.state.value.error).isNull()
    }

    @Test
    fun `onSubmit error surfaces the failure`() = runBlocking {
        val vm = buildVm()
        vm.onNewPasswordChange("abcd1234")
        vm.onConfirmPasswordChange("abcd1234")
        server.enqueue(MockResponse().setResponseCode(400).setBody("""{"detail":"expired token"}"""))
        vm.onSubmit()
        awaitUntil { !vm.state.value.isSubmitting }
        assertThat(vm.state.value.isSuccess).isFalse()
        assertThat(vm.state.value.error).isNotNull()
    }
}
