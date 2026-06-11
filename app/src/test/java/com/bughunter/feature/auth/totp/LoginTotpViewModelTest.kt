package com.bughunter.feature.auth.totp

import com.bughunter.core.data.repository.AuthRepoTestFactory
import com.bughunter.feature.auth.AuthStateHolder
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
class LoginTotpViewModelTest {

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

    private fun holderAwaiting(
        token: String = "pending-tok",
        email: String = "user@example.com",
    ): AuthStateHolder = AuthStateHolder().apply { setAwaitingTotp(token, email) }

    private fun buildVm(holder: AuthStateHolder): LoginTotpViewModel =
        LoginTotpViewModel(AuthRepoTestFactory.create(server), holder)

    private suspend fun awaitUntil(timeoutMs: Long = 4_000, check: () -> Boolean) {
        withTimeout(timeoutMs) { while (!check()) delay(10) }
    }

    private fun meBody(): MockResponse = MockResponse().setResponseCode(200).setBody(
        """
        {
          "id": 1, "name": "S", "email": "user@example.com", "role": "member",
          "is_active": true, "org_id": 1, "organization_name": "x",
          "organization_slug": "x", "totp_enabled": false
        }
        """.trimIndent(),
    )

    @Test
    fun `init seeds email from awaiting-totp auth state`() {
        val vm = buildVm(holderAwaiting(email = "alice@b.c"))
        assertThat(vm.state.value.email).isEqualTo("alice@b.c")
        assertThat(vm.state.value.code).isEmpty()
        assertThat(vm.state.value.isSubmitting).isFalse()
        assertThat(vm.state.value.error).isNull()
    }

    @Test
    fun `init leaves email empty when not awaiting totp`() {
        // Fresh holder is in the Checking state, not AwaitingTotp.
        val vm = buildVm(AuthStateHolder())
        assertThat(vm.state.value.email).isEmpty()
    }

    @Test
    fun `onCodeChange keeps digits and letters and caps at 12`() {
        val vm = buildVm(holderAwaiting())
        vm.onCodeChange("12-34 56!@#")
        assertThat(vm.state.value.code).isEqualTo("123456")
        vm.onCodeChange("ABcd1234567890XYZ")
        assertThat(vm.state.value.code).isEqualTo("ABcd12345678")
        assertThat(vm.state.value.code.length).isEqualTo(12)
    }

    @Test
    fun `onCodeChange clears any prior error`() {
        val holder = holderAwaiting()
        val vm = buildVm(holder)
        // Drive an error first, then confirm onCodeChange clears it.
        runBlocking {
            vm.onCodeChange("123456")
            server.enqueue(MockResponse().setResponseCode(400).setBody("""{"detail":"bad code"}"""))
            vm.onSubmit()
            awaitUntil { vm.state.value.error != null }
        }
        vm.onCodeChange("654321")
        assertThat(vm.state.value.error).isNull()
    }

    @Test
    fun `onSubmit is a no-op when code is invalid`() = runBlocking {
        val vm = buildVm(holderAwaiting())
        vm.onCodeChange("123") // too short for a 6-digit otp
        vm.onSubmit()
        delay(50)
        assertThat(server.requestCount).isEqualTo(0)
        assertThat(vm.state.value.isSubmitting).isFalse()
    }

    @Test
    fun `onSubmit is a no-op when not awaiting totp`() = runBlocking {
        val vm = buildVm(AuthStateHolder()) // Checking state, no pending token
        vm.onCodeChange("123456")
        vm.onSubmit()
        delay(50)
        assertThat(server.requestCount).isEqualTo(0)
        assertThat(vm.state.value.isSubmitting).isFalse()
    }

    @Test
    fun `onSubmit success clears submitting and leaves no error`() = runBlocking {
        val vm = buildVm(holderAwaiting())
        vm.onCodeChange("123456")
        server.enqueue(meBody())
        vm.onSubmit()
        awaitUntil { !vm.state.value.isSubmitting }
        assertThat(server.requestCount).isEqualTo(1)
        assertThat(vm.state.value.error).isNull()
    }

    @Test
    fun `onSubmit accepts a recovery code`() = runBlocking {
        val vm = buildVm(holderAwaiting())
        vm.onCodeChange("ABCD123456") // 10-char recovery code
        server.enqueue(meBody())
        vm.onSubmit()
        awaitUntil { !vm.state.value.isSubmitting }
        assertThat(server.requestCount).isEqualTo(1)
        assertThat(vm.state.value.error).isNull()
    }

    @Test
    fun `onSubmit error surfaces the failure and clears submitting`() = runBlocking {
        val vm = buildVm(holderAwaiting())
        vm.onCodeChange("123456")
        server.enqueue(MockResponse().setResponseCode(400).setBody("""{"detail":"invalid code"}"""))
        vm.onSubmit()
        awaitUntil { !vm.state.value.isSubmitting }
        assertThat(vm.state.value.error).isNotNull()
    }

    @Test
    fun `onSubmit ignores a second call while already submitting`() = runBlocking {
        val vm = buildVm(holderAwaiting())
        vm.onCodeChange("123456")
        // Only one response is enqueued; a second in-flight submit would
        // need a second response. We rely on the isSubmitting guard.
        server.enqueue(meBody())
        vm.onSubmit()
        vm.onSubmit() // guard should drop this before any HTTP call
        awaitUntil { !vm.state.value.isSubmitting }
        assertThat(server.requestCount).isEqualTo(1)
    }
}
