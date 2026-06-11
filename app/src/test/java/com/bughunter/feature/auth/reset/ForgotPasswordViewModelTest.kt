package com.bughunter.feature.auth.reset

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

/**
 * ForgotPasswordViewModel intentionally never leaks whether an email is
 * registered: a 404 from the backend is surfaced as the SAME generic
 * "submitted" success the user sees for a real address. These tests pin
 * that privacy-preserving behaviour plus the client-side validation gate.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ForgotPasswordViewModelTest {

    private lateinit var server: MockWebServer
    private lateinit var vm: ForgotPasswordViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Unconfined)
        server = MockWebServer().apply { start() }
        vm = ForgotPasswordViewModel(AuthRepoTestFactory.create(server))
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
    fun `onSubmit with invalid email is a no-op`() = runBlocking {
        vm.onEmailChange("not-an-email")
        vm.onSubmit()
        delay(50)
        assertThat(vm.state.value.isSubmitting).isFalse()
        assertThat(vm.state.value.submitted).isFalse()
        assertThat(server.requestCount).isEqualTo(0)
    }

    @Test
    fun `successful request marks submitted`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"status":"ok"}"""))
        vm.onEmailChange("user@example.com")
        vm.onSubmit()
        awaitUntil { vm.state.value.submitted }
        assertThat(vm.state.value.error).isNull()
    }

    @Test
    fun `404 is masked as a generic submitted success`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(404).setBody("""{"detail":"No such user"}"""))
        vm.onEmailChange("ghost@example.com")
        vm.onSubmit()
        awaitUntil { vm.state.value.submitted }
        // Privacy: a not-found address must NOT produce a visible error.
        assertThat(vm.state.value.error).isNull()
    }

    @Test
    fun `server error surfaces a visible error`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(500).setBody("""{"detail":"boom"}"""))
        vm.onEmailChange("user@example.com")
        vm.onSubmit()
        awaitUntil { !vm.state.value.isSubmitting }
        assertThat(vm.state.value.submitted).isFalse()
        assertThat(vm.state.value.error).isNotNull()
    }

    @Test
    fun `submitting twice does not double-send`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"status":"ok"}"""))
        vm.onEmailChange("user@example.com")
        vm.onSubmit()
        awaitUntil { vm.state.value.submitted }
        vm.onSubmit() // already submitted -> guarded
        delay(50)
        assertThat(server.requestCount).isEqualTo(1)
    }
}
