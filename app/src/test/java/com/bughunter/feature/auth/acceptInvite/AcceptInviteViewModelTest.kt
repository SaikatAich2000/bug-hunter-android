package com.bughunter.feature.auth.acceptInvite

import androidx.lifecycle.SavedStateHandle
import com.bughunter.core.data.repository.AuthRepoTestFactory
import com.bughunter.core.data.repository.InvitationsRepository
import com.bughunter.core.data.repository.RepoTestSupport
import com.bughunter.core.nav.BhRoute
import com.bughunter.core.network.api.InvitationsApi
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
class AcceptInviteViewModelTest {

    private lateinit var server: MockWebServer
    private lateinit var invitationsRepo: InvitationsRepository
    private val moshi = RepoTestSupport.moshi()
    private val mapper = RepoTestSupport.errorMapper(moshi)
    private val jar = RepoTestSupport.cookieJar()

    @Before
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Unconfined)
        server = MockWebServer().apply { start() }
        // Pre-seed CSRF on the same cookie jar / server so the accept POST does
        // not trigger an inline GET /api/health that would consume a queued
        // MockResponse the test did not intend.
        RepoTestSupport.seedCsrf(jar, server.url("/"))
        val api = RepoTestSupport.retrofit(server, moshi, client = RepoTestSupport.client(jar))
            .create(InvitationsApi::class.java)
        invitationsRepo = InvitationsRepository(api, mapper)
    }

    @After
    fun tearDown() {
        runCatching { server.shutdown() }
        Dispatchers.resetMain()
    }

    private suspend fun awaitUntil(timeoutMs: Long = 4_000, check: () -> Boolean) {
        withTimeout(timeoutMs) { while (!check()) delay(10) }
    }

    /** authRepository points at the SAME server so responses are consumed in call order. */
    private fun buildVm(token: String = "tok-123"): AcceptInviteViewModel {
        val handle = SavedStateHandle(mapOf(BhRoute.AcceptInvite.ARG_TOKEN to token))
        return AcceptInviteViewModel(
            savedStateHandle = handle,
            invitationsRepository = invitationsRepo,
            authRepository = AuthRepoTestFactory.create(server),
        )
    }

    // --- token read from handle --------------------------------------------

    @Test
    fun `token is read from the saved-state handle`() {
        // init() will fire a preview GET; enqueue something so it does not 404-fail noisily.
        server.enqueue(MockResponse().setResponseCode(200).setBody(PREVIEW))
        assertThat(buildVm("abc").state.value.token).isEqualTo("abc")
    }

    // --- init invite-validation --------------------------------------------

    @Test
    fun `init validates the invite and exposes the preview on success`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody(PREVIEW))
        val vm = buildVm()
        awaitUntil { !vm.state.value.isLoadingPreview }
        val recorded = server.takeRequest()
        assertThat(recorded.path).isEqualTo("/api/invitations/preview/tok-123")
        assertThat(vm.state.value.preview).isNotNull()
        assertThat(vm.state.value.preview!!.organizationName).isEqualTo("Acme")
        assertThat(vm.state.value.previewError).isNull()
    }

    @Test
    fun `init preview 404 surfaces a preview error`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(404).setBody("""{"detail":"gone"}"""))
        val vm = buildVm()
        awaitUntil { !vm.state.value.isLoadingPreview }
        assertThat(vm.state.value.preview).isNull()
        assertThat(vm.state.value.previewError).isNotNull()
    }

    @Test
    fun `blank token short-circuits to NotFound without any HTTP call`() = runBlocking {
        val vm = buildVm("")
        awaitUntil { !vm.state.value.isLoadingPreview }
        assertThat(vm.state.value.previewError).isNotNull()
        assertThat(server.requestCount).isEqualTo(0)
    }

    @Test
    fun `retryPreview re-runs the validation`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(404).setBody("""{"detail":"gone"}"""))
        val vm = buildVm()
        awaitUntil { vm.state.value.previewError != null }
        server.enqueue(MockResponse().setResponseCode(200).setBody(PREVIEW))
        vm.retryPreview()
        awaitUntil { vm.state.value.preview != null }
        assertThat(vm.state.value.previewError).isNull()
    }

    // --- form setters (pure) -----------------------------------------------

    @Test
    fun `name and password setters update state and clear submit error`() {
        server.enqueue(MockResponse().setResponseCode(200).setBody(PREVIEW))
        val vm = buildVm()
        vm.onNameChange("Ada")
        vm.onPasswordChange("abcd1234")
        assertThat(vm.state.value.name).isEqualTo("Ada")
        assertThat(vm.state.value.password).isEqualTo("abcd1234")
        assertThat(vm.state.value.submitError).isNull()
    }

    @Test
    fun `canSubmit is true only with a preview, a name and a strong password`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody(PREVIEW))
        val vm = buildVm()
        awaitUntil { vm.state.value.preview != null }
        assertThat(vm.state.value.canSubmit).isFalse()
        vm.onNameChange("Ada")
        vm.onPasswordChange("weak")            // < 8 chars / no digit
        assertThat(vm.state.value.canSubmit).isFalse()
        vm.onPasswordChange("abcd1234")
        assertThat(vm.state.value.canSubmit).isTrue()
    }

    // --- accept / submit ----------------------------------------------------
    // Call order: init -> GET api/invitations/preview/{token};
    //             onSubmit -> POST api/invitations/accept (returns MeOut);
    //             authRepository.onInviteAccepted makes NO further HTTP call.

    @Test
    fun `onSubmit success posts accept then finishes without a submit error`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody(PREVIEW))   // 1: init preview
        val vm = buildVm()
        awaitUntil { vm.state.value.preview != null }
        vm.onNameChange("Ada")
        vm.onPasswordChange("abcd1234")
        server.enqueue(MockResponse().setResponseCode(200).setBody(ME))        // 2: accept POST
        vm.onSubmit()
        awaitUntil { !vm.state.value.isSubmitting && vm.state.value.preview != null }
        server.takeRequest() // drain the init preview GET first
        val accept = server.takeRequest()
        assertThat(accept.path).isEqualTo("/api/invitations/accept")
        assertThat(accept.method).isEqualTo("POST")
        assertThat(vm.state.value.submitError).isNull()
    }

    @Test
    fun `onSubmit error surfaces the accept failure`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody(PREVIEW))   // init preview
        val vm = buildVm()
        awaitUntil { vm.state.value.preview != null }
        vm.onNameChange("Ada")
        vm.onPasswordChange("abcd1234")
        server.enqueue(MockResponse().setResponseCode(400).setBody("""{"detail":"bad"}""")) // accept
        vm.onSubmit()
        awaitUntil { !vm.state.value.isSubmitting }
        assertThat(vm.state.value.submitError).isNotNull()
    }

    @Test
    fun `onSubmit is a no-op when the form cannot be submitted`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody(PREVIEW))   // init preview only
        val vm = buildVm()
        awaitUntil { vm.state.value.preview != null }
        // no name / password -> canSubmit is false
        vm.onSubmit()
        delay(50)
        assertThat(vm.state.value.isSubmitting).isFalse()
        assertThat(server.requestCount).isEqualTo(1) // only the init preview ran
    }

    private companion object {
        const val PREVIEW = """
            {"email":"a@b.c","organization_name":"Acme","role":"member",
             "expires_at":"2026-02-01T00:00:00Z","invited_by_name":"alice"}
        """

        const val ME = """
            {"id":1,"name":"Ada","email":"a@b.c","role":"member","is_active":true,
             "org_id":1,"organization_name":"Acme","organization_slug":"acme",
             "totp_enabled":false}
        """
    }
}
