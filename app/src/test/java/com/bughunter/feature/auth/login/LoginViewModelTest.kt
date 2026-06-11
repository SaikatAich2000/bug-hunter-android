package com.bughunter.feature.auth.login

import com.bughunter.core.data.repository.AuthRepoTestFactory
import com.bughunter.core.data.repository.RepoTestSupport
import com.bughunter.core.domain.usecase.LoginUseCase
import com.bughunter.core.network.DomainError
import com.bughunter.core.network.EncryptedCookieJar
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
class LoginViewModelTest {

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

    /** CSRF pre-seeded so onSubmit doesn't fire an inline GET /api/health. */
    private fun seededJar(): EncryptedCookieJar =
        RepoTestSupport.cookieJar().also { RepoTestSupport.seedCsrf(it, server.url("/")) }

    private fun buildVm(): LoginViewModel =
        LoginViewModel(LoginUseCase(AuthRepoTestFactory.create(server), seededJar()))

    private suspend fun awaitUntil(timeoutMs: Long = 4_000, check: () -> Boolean) {
        withTimeout(timeoutMs) { while (!check()) delay(10) }
    }

    @Test
    fun `onEmailChange updates email and clears error`() {
        val vm = buildVm()
        vm.onEmailChange("a@b.c")
        assertThat(vm.state.value.email).isEqualTo("a@b.c")
        assertThat(vm.state.value.error).isNull()
    }

    @Test
    fun `onPasswordChange updates password and clears error`() {
        val vm = buildVm()
        vm.onPasswordChange("secret")
        assertThat(vm.state.value.password).isEqualTo("secret")
        assertThat(vm.state.value.error).isNull()
    }

    @Test
    fun `onSubmit is a no-op when email or password blank`() = runBlocking {
        val vm = buildVm()
        vm.onEmailChange("   ")
        vm.onPasswordChange("")
        vm.onSubmit()
        delay(50)
        assertThat(server.requestCount).isEqualTo(0)
        assertThat(vm.state.value.isSubmitting).isFalse()
    }

    @Test
    fun `onSubmit success clears submitting with no error`() = runBlocking {
        val vm = buildVm()
        vm.onEmailChange("a@b.c")
        vm.onPasswordChange("secret")
        server.enqueue(
            MockResponse().setResponseCode(200)
                .setBody("""{"pending_2fa":true,"pending_token":"opaque-1"}"""),
        )
        vm.onSubmit()
        awaitUntil { !vm.state.value.isSubmitting }
        assertThat(vm.state.value.error).isNull()
        assertThat(vm.state.value.lockedUntil).isNull()
    }

    @Test
    fun `onSubmit error surfaces the failure`() = runBlocking {
        val vm = buildVm()
        vm.onEmailChange("a@b.c")
        vm.onPasswordChange("wrong")
        server.enqueue(
            MockResponse().setResponseCode(401)
                .setBody("""{"detail":"Invalid credentials"}"""),
        )
        vm.onSubmit()
        awaitUntil { !vm.state.value.isSubmitting }
        assertThat(vm.state.value.error).isNotNull()
    }

    @Test
    fun `onSubmit rate limited sets lockedUntil`() = runBlocking {
        val vm = buildVm()
        vm.onEmailChange("a@b.c")
        vm.onPasswordChange("secret")
        server.enqueue(
            MockResponse().setResponseCode(429)
                .setHeader("Retry-After", "30")
                .setBody("""{"detail":"Too many failed sign-in attempts."}"""),
        )
        vm.onSubmit()
        awaitUntil { !vm.state.value.isSubmitting }
        assertThat(vm.state.value.error).isInstanceOf(DomainError.RateLimited::class.java)
        assertThat(vm.state.value.lockedUntil).isNotNull()
    }

    @Test
    fun `onSubmit is guarded against double submission`() = runBlocking {
        val vm = buildVm()
        vm.onEmailChange("a@b.c")
        vm.onPasswordChange("secret")
        // Only one response queued; a second concurrent submit must not hit the server.
        server.enqueue(
            MockResponse().setResponseCode(200)
                .setBody("""{"pending_2fa":true,"pending_token":"opaque-1"}"""),
        )
        // Force isSubmitting=true then attempt a re-entrant submit before completion.
        vm.onSubmit()
        vm.onSubmit()
        awaitUntil { !vm.state.value.isSubmitting }
        assertThat(server.requestCount).isEqualTo(1)
    }

    @Test
    fun `clearError resets the error`() = runBlocking {
        val vm = buildVm()
        vm.onEmailChange("a@b.c")
        vm.onPasswordChange("wrong")
        server.enqueue(
            MockResponse().setResponseCode(401)
                .setBody("""{"detail":"Invalid credentials"}"""),
        )
        vm.onSubmit()
        awaitUntil { vm.state.value.error != null }
        vm.clearError()
        assertThat(vm.state.value.error).isNull()
    }
}
