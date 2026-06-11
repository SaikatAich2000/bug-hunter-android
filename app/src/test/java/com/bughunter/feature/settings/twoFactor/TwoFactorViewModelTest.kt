package com.bughunter.feature.settings.twoFactor

import com.bughunter.core.data.repository.AuthRepoTestFactory
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
class TwoFactorViewModelTest {

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

    // --- response helpers ---------------------------------------------------

    private fun statusBody(enabled: Boolean = false, codes: Int = 0): MockResponse =
        MockResponse().setResponseCode(200).setBody(
            """{"enabled":$enabled,"unused_recovery_codes":$codes}""",
        )

    private fun beginBody(): MockResponse =
        MockResponse().setResponseCode(200).setBody(
            """{"secret":"JBSWY3DPEHPK3PXP","otpauth_uri":"otpauth://totp/BugHunter:a@b.c?secret=JBSWY3DPEHPK3PXP"}""",
        )

    private fun confirmBody(): MockResponse =
        MockResponse().setResponseCode(200).setBody(
            """{"enabled":true,"recovery_codes":["AAAA111122","BBBB333344"]}""",
        )

    private fun errorBody(code: Int = 400): MockResponse =
        MockResponse().setResponseCode(code).setBody("""{"detail":"nope"}""")

    /** Build a VM whose init{} GET status succeeds. */
    private fun buildVm(enabled: Boolean = false): TwoFactorViewModel {
        server.enqueue(statusBody(enabled = enabled))
        return TwoFactorViewModel(AuthRepoTestFactory.create(server))
    }

    private suspend fun awaitUntil(timeoutMs: Long = 4_000, check: () -> Boolean) {
        withTimeout(timeoutMs) { while (!check()) delay(10) }
    }

    // --- init / refreshStatus ----------------------------------------------

    @Test
    fun `init loads status into Success`() = runBlocking {
        val vm = buildVm(enabled = true)
        awaitUntil { vm.state.value.status is UiState.Success }
        val status = (vm.state.value.status as UiState.Success).data
        assertThat(status.enabled).isTrue()
        assertThat(vm.state.value.view).isEqualTo(TwoFactorView.STATUS)
    }

    @Test
    fun `init surfaces status error into Error`() = runBlocking {
        server.enqueue(errorBody(500))
        val vm = TwoFactorViewModel(AuthRepoTestFactory.create(server))
        awaitUntil { vm.state.value.status is UiState.Error }
        assertThat(vm.state.value.status).isInstanceOf(UiState.Error::class.java)
    }

    @Test
    fun `refreshStatus reloads status`() = runBlocking {
        val vm = buildVm(enabled = false)
        awaitUntil { vm.state.value.status is UiState.Success }
        server.enqueue(statusBody(enabled = true, codes = 5))
        vm.refreshStatus()
        awaitUntil {
            (vm.state.value.status as? UiState.Success)?.data?.enabled == true
        }
        val status = (vm.state.value.status as UiState.Success).data
        assertThat(status.unusedRecoveryCodes).isEqualTo(5)
    }

    // --- beginEnrol ---------------------------------------------------------

    @Test
    fun `beginEnrol success moves to ENROL view with beginOut`() = runBlocking {
        val vm = buildVm()
        awaitUntil { vm.state.value.status is UiState.Success }
        server.enqueue(beginBody())
        vm.beginEnrol()
        awaitUntil { vm.state.value.view == TwoFactorView.ENROL }
        assertThat(vm.state.value.beginOut).isNotNull()
        assertThat(vm.state.value.beginOut!!.secret).isEqualTo("JBSWY3DPEHPK3PXP")
        assertThat(vm.state.value.isSubmitting).isFalse()
        assertThat(vm.state.value.error).isNull()
    }

    @Test
    fun `beginEnrol error surfaces and stays on STATUS`() = runBlocking {
        val vm = buildVm()
        awaitUntil { vm.state.value.status is UiState.Success }
        server.enqueue(errorBody())
        vm.beginEnrol()
        awaitUntil { vm.state.value.error != null }
        assertThat(vm.state.value.view).isEqualTo(TwoFactorView.STATUS)
        assertThat(vm.state.value.isSubmitting).isFalse()
    }

    // --- onConfirmCodeChange (pure) ----------------------------------------

    @Test
    fun `onConfirmCodeChange cleans non-alnum and caps at 12`() {
        val vm = buildVm()
        vm.onConfirmCodeChange("12-34 56")
        assertThat(vm.state.value.confirmCode).isEqualTo("123456")
        vm.onConfirmCodeChange("ABCDEFGHIJKLMNOP")
        assertThat(vm.state.value.confirmCode).isEqualTo("ABCDEFGHIJKL")
        assertThat(vm.state.value.error).isNull()
    }

    // --- confirmEnrol -------------------------------------------------------

    @Test
    fun `confirmEnrol is a no-op for an invalid code`() = runBlocking {
        val vm = buildVm()
        awaitUntil { vm.state.value.status is UiState.Success }
        val before = server.requestCount
        vm.onConfirmCodeChange("123")
        vm.confirmEnrol()
        delay(50)
        assertThat(server.requestCount).isEqualTo(before)
        assertThat(vm.state.value.view).isEqualTo(TwoFactorView.STATUS)
    }

    @Test
    fun `confirmEnrol success moves to RECOVERY with codes`() = runBlocking {
        val vm = buildVm()
        awaitUntil { vm.state.value.status is UiState.Success }
        vm.onConfirmCodeChange("123456")
        server.enqueue(confirmBody())
        vm.confirmEnrol()
        awaitUntil { vm.state.value.view == TwoFactorView.RECOVERY }
        assertThat(vm.state.value.recovery).isNotNull()
        assertThat(vm.state.value.recovery!!.recoveryCodes).containsExactly("AAAA111122", "BBBB333344")
        assertThat(vm.state.value.isSubmitting).isFalse()
        Unit
    }

    @Test
    fun `confirmEnrol error surfaces failure`() = runBlocking {
        val vm = buildVm()
        awaitUntil { vm.state.value.status is UiState.Success }
        vm.onConfirmCodeChange("123456")
        server.enqueue(errorBody())
        vm.confirmEnrol()
        awaitUntil { vm.state.value.error != null }
        assertThat(vm.state.value.view).isEqualTo(TwoFactorView.STATUS)
        assertThat(vm.state.value.isSubmitting).isFalse()
    }

    // --- openDisable / openStatus ------------------------------------------

    @Test
    fun `openDisable switches to DISABLE view and clears password`() = runBlocking {
        val vm = buildVm()
        awaitUntil { vm.state.value.status is UiState.Success }
        vm.onDisablePasswordChange("secret")
        vm.openDisable()
        assertThat(vm.state.value.view).isEqualTo(TwoFactorView.DISABLE)
        assertThat(vm.state.value.disablePassword).isEmpty()
        assertThat(vm.state.value.error).isNull()
    }

    @Test
    fun `openStatus switches to STATUS view and reloads`() = runBlocking {
        val vm = buildVm()
        awaitUntil { vm.state.value.status is UiState.Success }
        vm.openDisable()
        assertThat(vm.state.value.view).isEqualTo(TwoFactorView.DISABLE)
        server.enqueue(statusBody(enabled = true))
        vm.openStatus()
        awaitUntil { vm.state.value.view == TwoFactorView.STATUS }
        awaitUntil {
            (vm.state.value.status as? UiState.Success)?.data?.enabled == true
        }
        assertThat(vm.state.value.beginOut).isNull()
        assertThat(vm.state.value.recovery).isNull()
    }

    // --- onDisablePasswordChange (pure) ------------------------------------

    @Test
    fun `onDisablePasswordChange updates password and clears error`() {
        val vm = buildVm()
        vm.onDisablePasswordChange("hunter2")
        assertThat(vm.state.value.disablePassword).isEqualTo("hunter2")
        assertThat(vm.state.value.error).isNull()
    }

    // --- confirmDisable -----------------------------------------------------

    @Test
    fun `confirmDisable is a no-op when password is blank`() = runBlocking {
        val vm = buildVm()
        awaitUntil { vm.state.value.status is UiState.Success }
        val before = server.requestCount
        vm.confirmDisable()
        delay(50)
        assertThat(server.requestCount).isEqualTo(before)
    }

    @Test
    fun `confirmDisable success returns to STATUS and reloads`() = runBlocking {
        val vm = buildVm(enabled = true)
        awaitUntil { vm.state.value.status is UiState.Success }
        vm.openDisable()
        vm.onDisablePasswordChange("hunter2")
        server.enqueue(MockResponse().setResponseCode(204)) // disable
        server.enqueue(statusBody(enabled = false))         // refreshStatus reload
        vm.confirmDisable()
        awaitUntil { vm.state.value.view == TwoFactorView.STATUS }
        awaitUntil {
            (vm.state.value.status as? UiState.Success)?.data?.enabled == false
        }
        assertThat(vm.state.value.disablePassword).isEmpty()
        assertThat(vm.state.value.isSubmitting).isFalse()
    }

    @Test
    fun `confirmDisable error surfaces failure`() = runBlocking {
        val vm = buildVm(enabled = true)
        awaitUntil { vm.state.value.status is UiState.Success }
        vm.openDisable()
        vm.onDisablePasswordChange("wrongpass")
        server.enqueue(errorBody(403))
        vm.confirmDisable()
        awaitUntil { vm.state.value.error != null }
        assertThat(vm.state.value.view).isEqualTo(TwoFactorView.DISABLE)
        assertThat(vm.state.value.isSubmitting).isFalse()
    }

    // --- regenerateRecoveryCodes -------------------------------------------

    @Test
    fun `regenerateRecoveryCodes success moves to RECOVERY`() = runBlocking {
        val vm = buildVm(enabled = true)
        awaitUntil { vm.state.value.status is UiState.Success }
        server.enqueue(confirmBody())
        vm.regenerateRecoveryCodes()
        awaitUntil { vm.state.value.view == TwoFactorView.RECOVERY }
        assertThat(vm.state.value.recovery).isNotNull()
        assertThat(vm.state.value.recovery!!.recoveryCodes).hasSize(2)
        assertThat(vm.state.value.isSubmitting).isFalse()
    }

    @Test
    fun `regenerateRecoveryCodes error surfaces failure`() = runBlocking {
        val vm = buildVm(enabled = true)
        awaitUntil { vm.state.value.status is UiState.Success }
        server.enqueue(errorBody(500))
        vm.regenerateRecoveryCodes()
        awaitUntil { vm.state.value.error != null }
        assertThat(vm.state.value.isSubmitting).isFalse()
    }
}
