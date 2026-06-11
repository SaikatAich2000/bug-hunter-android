package com.bughunter.feature.auth.signup

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
class SignupViewModelTest {

    private lateinit var server: MockWebServer
    private lateinit var vm: SignupViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Unconfined)
        server = MockWebServer().apply { start() }
        vm = SignupViewModel(AuthRepoTestFactory.create(server))
    }

    @After
    fun tearDown() {
        runCatching { server.shutdown() }
        Dispatchers.resetMain()
    }

    private suspend fun awaitUntil(timeoutMs: Long = 4_000, check: () -> Boolean) {
        withTimeout(timeoutMs) { while (!check()) delay(10) }
    }

    private fun fillValidForm() {
        vm.onNameChange("Ada")
        vm.onOrganizationChange("Acme")
        vm.onEmailChange("ada@example.com")
        vm.onPasswordChange("abcd1234")
    }

    @Test
    fun `canSubmit requires name, org, valid email and strong password`() {
        assertThat(vm.state.value.canSubmit).isFalse()
        vm.onNameChange("Ada")
        vm.onOrganizationChange("Acme")
        vm.onEmailChange("bad")
        vm.onPasswordChange("abcd1234")
        assertThat(vm.state.value.canSubmit).isFalse()   // bad email
        vm.onEmailChange("ada@example.com")
        vm.onPasswordChange("weak")
        assertThat(vm.state.value.canSubmit).isFalse()   // weak password
        vm.onPasswordChange("abcd1234")
        assertThat(vm.state.value.canSubmit).isTrue()
    }

    @Test
    fun `onSubmit is a no-op while the form is invalid`() = runBlocking {
        vm.onEmailChange("nope")
        vm.onSubmit()
        delay(50)
        assertThat(server.requestCount).isEqualTo(0)
    }

    @Test
    fun `breach validation error sets breachRejected`() = runBlocking {
        fillValidForm()
        server.enqueue(
            MockResponse().setResponseCode(422)
                .setBody("""{"detail":"Password found in a known breach."}"""),
        )
        vm.onSubmit()
        awaitUntil { !vm.state.value.isSubmitting }
        assertThat(vm.state.value.error).isNotNull()
        assertThat(vm.state.value.breachRejected).isTrue()
    }

    @Test
    fun `generic error surfaces without breachRejected`() = runBlocking {
        fillValidForm()
        server.enqueue(MockResponse().setResponseCode(409).setBody("""{"detail":"Email taken"}"""))
        vm.onSubmit()
        awaitUntil { !vm.state.value.isSubmitting }
        assertThat(vm.state.value.error).isNotNull()
        assertThat(vm.state.value.breachRejected).isFalse()
    }
}
