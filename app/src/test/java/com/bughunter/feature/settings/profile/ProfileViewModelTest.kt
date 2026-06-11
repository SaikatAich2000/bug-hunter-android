package com.bughunter.feature.settings.profile

import com.bughunter.core.data.repository.AuthRepoTestFactory
import com.bughunter.core.network.dto.MeOut
import com.bughunter.core.network.dto.Role
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
class ProfileViewModelTest {

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

    private suspend fun awaitUntil(timeoutMs: Long = 4_000, check: () -> Boolean) {
        withTimeout(timeoutMs) { while (!check()) delay(10) }
    }

    private fun me(id: Int = 1, name: String = "Ada Lovelace") = MeOut(
        id = id, name = name, email = "u$id@example.com", role = Role.MEMBER,
        isActive = true, orgId = 7, organizationName = "Acme", organizationSlug = "acme",
        totpEnabled = false,
    )

    private fun meJson(id: Int = 1, name: String = "Ada Lovelace"): String = """
        {
          "id": $id, "name": "$name", "email": "u$id@example.com", "role": "member",
          "is_active": true, "org_id": 7, "organization_name": "Acme",
          "organization_slug": "acme", "totp_enabled": false
        }
    """.trimIndent()

    private fun okMe(id: Int = 1, name: String = "Ada Lovelace") =
        MockResponse().setResponseCode(200).setBody(meJson(id, name))

    private fun errResponse() =
        MockResponse().setResponseCode(400).setBody("""{"detail":"nope"}""")

    private fun stateHolderAuthenticated(me: MeOut): AuthStateHolder =
        AuthStateHolder().apply { setAuthenticated(me) }

    // init enqueues the refresh() me() call.
    private fun buildVm(holder: AuthStateHolder = AuthStateHolder()): ProfileViewModel =
        ProfileViewModel(AuthRepoTestFactory.create(server), holder)

    @Test
    fun `init seeds me and nameEdit from authenticated AuthStateHolder`() = runBlocking {
        server.enqueue(okMe(name = "Server Name"))
        val vm = buildVm(stateHolderAuthenticated(me(name = "Seed Name")))
        // Seeded synchronously before refresh resolves.
        assertThat(vm.state.value.me).isNotNull()
        awaitUntil { vm.state.value.me?.name == "Server Name" }
        // nameEdit was seeded non-blank, so refresh keeps it.
        assertThat(vm.state.value.nameEdit).isEqualTo("Seed Name")
    }

    @Test
    fun `init with unauthenticated holder leaves nameEdit blank then refresh fills it`() = runBlocking {
        server.enqueue(okMe(name = "Fetched"))
        val vm = buildVm()
        awaitUntil { vm.state.value.me != null }
        assertThat(vm.state.value.me?.name).isEqualTo("Fetched")
        assertThat(vm.state.value.nameEdit).isEqualTo("Fetched")
        assertThat(vm.state.value.error).isNull()
    }

    @Test
    fun `refresh error surfaces a DomainError`() = runBlocking {
        server.enqueue(errResponse())
        val vm = buildVm()
        awaitUntil { vm.state.value.error != null }
        assertThat(vm.state.value.me).isNull()
    }

    @Test
    fun `onNameChange updates nameEdit and clears flags`() = runBlocking {
        server.enqueue(okMe())
        val vm = buildVm()
        awaitUntil { vm.state.value.me != null }
        vm.onNameChange("New Name")
        assertThat(vm.state.value.nameEdit).isEqualTo("New Name")
        assertThat(vm.state.value.savedName).isFalse()
        assertThat(vm.state.value.error).isNull()
    }

    @Test
    fun `refresh keeps an edited name`() = runBlocking {
        server.enqueue(okMe(name = "Original"))
        val vm = buildVm()
        awaitUntil { vm.state.value.me != null }
        vm.onNameChange("Edited")
        server.enqueue(okMe(name = "Different"))
        vm.refresh()
        awaitUntil { vm.state.value.me?.name == "Different" }
        assertThat(vm.state.value.nameEdit).isEqualTo("Edited")
    }

    @Test
    fun `saveName success sets savedName and updates me`() = runBlocking {
        server.enqueue(okMe(name = "Initial"))
        val vm = buildVm()
        awaitUntil { vm.state.value.me != null }
        vm.onNameChange("  Trimmed Name  ")
        server.enqueue(okMe(name = "Trimmed Name"))
        vm.saveName()
        awaitUntil { vm.state.value.savedName }
        assertThat(vm.state.value.isSavingName).isFalse()
        assertThat(vm.state.value.me?.name).isEqualTo("Trimmed Name")
        assertThat(vm.state.value.error).isNull()
    }

    @Test
    fun `saveName error surfaces failure and clears saving flag`() = runBlocking {
        server.enqueue(okMe())
        val vm = buildVm()
        awaitUntil { vm.state.value.me != null }
        vm.onNameChange("Valid Name")
        server.enqueue(errResponse())
        vm.saveName()
        awaitUntil { !vm.state.value.isSavingName && vm.state.value.error != null }
        assertThat(vm.state.value.savedName).isFalse()
    }

    @Test
    fun `saveName is a no-op when nameEdit is blank`() = runBlocking {
        server.enqueue(okMe())
        val vm = buildVm()
        awaitUntil { vm.state.value.me != null }
        vm.onNameChange("   ")
        val before = server.requestCount
        vm.saveName()
        delay(50)
        assertThat(server.requestCount).isEqualTo(before)
        assertThat(vm.state.value.isSavingName).isFalse()
    }
}
