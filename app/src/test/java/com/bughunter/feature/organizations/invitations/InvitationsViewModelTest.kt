package com.bughunter.feature.organizations.invitations

import com.bughunter.core.data.repository.InvitationsRepository
import com.bughunter.core.data.repository.RepoTestSupport
import com.bughunter.core.network.api.InvitationsApi
import com.bughunter.core.network.dto.InvitationOut
import com.bughunter.core.network.dto.Role
import com.bughunter.core.ui.util.UiState
import com.google.common.truth.Truth.assertThat
import java.time.Instant
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
class InvitationsViewModelTest {

    private lateinit var server: MockWebServer
    private lateinit var repo: InvitationsRepository
    private val moshi = RepoTestSupport.moshi()
    private val mapper = RepoTestSupport.errorMapper(moshi)
    private val jar = RepoTestSupport.cookieJar()

    @Before
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Unconfined)
        server = MockWebServer().apply { start() }
        RepoTestSupport.seedCsrf(jar, server.url("/"))
        val api = RepoTestSupport.retrofit(server, moshi, client = RepoTestSupport.client(jar))
            .create(InvitationsApi::class.java)
        repo = InvitationsRepository(api, mapper)
    }

    @After
    fun tearDown() {
        runCatching { server.shutdown() }
        Dispatchers.resetMain()
    }

    private suspend fun awaitUntil(timeoutMs: Long = 4_000, check: () -> Boolean) {
        withTimeout(timeoutMs) { while (!check()) delay(10) }
    }

    private fun vmAfterLoad(body: String): InvitationsViewModel {
        server.enqueue(MockResponse().setResponseCode(200).setBody(body))
        return InvitationsViewModel(repo)
    }

    // --- displayStatus (pure) ---------------------------------------------
    @Test
    fun `displayStatus classifies pending, accepted, revoked and expired`() {
        val now = Instant.parse("2026-06-11T00:00:00Z")
        val model = InvitationsModel(invitations = emptyList(), now = now)
        fun inv(accepted: Instant? = null, revoked: Instant? = null, expires: Instant) =
            InvitationOut(
                id = 1, orgId = 1, email = "a@b.io", role = Role.MEMBER,
                invitedByName = "Admin", expiresAt = expires,
                acceptedAt = accepted, revokedAt = revoked,
                createdAt = now.minusSeconds(86_400),
            )
        assertThat(model.displayStatus(inv(expires = now.plusSeconds(3600)))).isEqualTo("pending")
        assertThat(model.displayStatus(inv(accepted = now, expires = now.plusSeconds(3600)))).isEqualTo("accepted")
        assertThat(model.displayStatus(inv(revoked = now, expires = now.plusSeconds(3600)))).isEqualTo("revoked")
        assertThat(model.displayStatus(inv(expires = now.minusSeconds(3600)))).isEqualTo("expired")
    }

    // --- list / actions ----------------------------------------------------
    @Test
    fun `empty list lands in Empty state`() = runBlocking {
        val vm = vmAfterLoad("[]")
        awaitUntil { vm.state.value is UiState.Empty }
        assertThat(vm.state.value).isInstanceOf(UiState.Empty::class.java)
    }

    @Test
    fun `non-empty list lands in Success state`() = runBlocking {
        val vm = vmAfterLoad("[${invite(1, "a@b.io")}]")
        awaitUntil { vm.state.value is UiState.Success }
        assertThat((vm.state.value as UiState.Success).data.invitations).hasSize(1)
    }

    @Test
    fun `list failure lands in Error state`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(500).setBody("""{"detail":"x"}"""))
        val vm = InvitationsViewModel(repo)
        awaitUntil { vm.state.value is UiState.Error }
        assertThat(vm.state.value).isInstanceOf(UiState.Error::class.java)
    }

    @Test
    fun `invite success reloads and invokes onDone(true)`() = runBlocking {
        val vm = vmAfterLoad("[]")
        awaitUntil { vm.state.value is UiState.Empty }
        server.enqueue(MockResponse().setResponseCode(201).setBody(invite(2, "new@b.io")))   // create
        server.enqueue(MockResponse().setResponseCode(200).setBody("[${invite(2, "new@b.io")}]")) // reload
        var done: Boolean? = null
        vm.invite("new@b.io", Role.MEMBER, emptyList(), asLead = false) { done = it }
        awaitUntil { done != null }
        assertThat(done).isTrue()
        awaitUntil { vm.state.value is UiState.Success }
    }

    @Test
    fun `invite failure records an action error`() = runBlocking {
        val vm = vmAfterLoad("[${invite(1, "a@b.io")}]")
        awaitUntil { vm.state.value is UiState.Success }
        server.enqueue(MockResponse().setResponseCode(409).setBody("""{"detail":"already invited"}"""))
        var done: Boolean? = null
        vm.invite("dupe@b.io", Role.MEMBER, emptyList(), asLead = false) { done = it }
        awaitUntil { done != null }
        assertThat(done).isFalse()
        assertThat((vm.state.value as UiState.Success).data.actionError).isNotNull()
        vm.dismissActionError()
        assertThat((vm.state.value as UiState.Success).data.actionError).isNull()
    }

    private fun invite(id: Int, email: String): String = """
        {"id":$id,"org_id":1,"email":"$email","role":"member","invited_by_name":"Admin",
         "initial_project_ids":"","expires_at":"2026-12-01T00:00:00Z",
         "created_at":"2026-01-01T00:00:00Z"}
    """.trimIndent()
}
