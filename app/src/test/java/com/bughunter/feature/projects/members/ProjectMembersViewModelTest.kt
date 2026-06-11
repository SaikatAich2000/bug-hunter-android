package com.bughunter.feature.projects.members

import com.bughunter.core.data.repository.MembershipsRepository
import com.bughunter.core.data.repository.RepoTestSupport
import com.bughunter.core.data.repository.UsersRepository
import com.bughunter.core.network.api.MembershipsApi
import com.bughunter.core.network.api.UsersApi
import com.bughunter.core.network.dto.ProjectRole
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
internal class ProjectMembersViewModelTest {

    private lateinit var server: MockWebServer
    private lateinit var membersRepo: MembershipsRepository
    private lateinit var usersRepo: UsersRepository
    private val moshi = RepoTestSupport.moshi()
    private val mapper = RepoTestSupport.errorMapper(moshi)
    private val jar = RepoTestSupport.cookieJar()

    @Before
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Unconfined)
        server = MockWebServer().apply { start() }
        RepoTestSupport.seedCsrf(jar, server.url("/"))
        val rf = RepoTestSupport.retrofit(server, moshi, client = RepoTestSupport.client(jar))
        membersRepo = MembershipsRepository(rf.create(MembershipsApi::class.java), mapper)
        usersRepo = UsersRepository(rf.create(UsersApi::class.java), mapper)
    }

    @After
    fun tearDown() {
        runCatching { server.shutdown() }
        Dispatchers.resetMain()
    }

    private suspend fun awaitUntil(timeoutMs: Long = 4_000, check: () -> Boolean) {
        withTimeout(timeoutMs) { while (!check()) delay(10) }
    }

    private fun newVm() = ProjectMembersViewModel(membersRepo, usersRepo)

    /** Enqueue the two responses a successful load consumes (members, then users). */
    private fun enqueueLoad() {
        server.enqueue(MockResponse().setResponseCode(200).setBody("[$ROW]"))
        server.enqueue(MockResponse().setResponseCode(200).setBody("[$USER_ROW,$USER_ROW2]"))
    }

    // --- ProjectMembersModel pure logic -----------------------------------
    @Test
    fun `candidates exclude existing members and respect query`() {
        val model = ProjectMembersModel(
            members = listOf(membershipOut(userId = 9)),
            allUsers = listOf(
                userOut(2, "Ada", "ada@x.io"),
                userOut(3, "Bob", "bob@acme.io"),
                userOut(9, "Taken", "taken@x.io"),
            ),
        )
        // member with userId 9 is excluded from the pool.
        assertThat(model.candidates.map { it.id }).containsExactly(2, 3)
        // query by name (case-insensitive)
        assertThat(model.copy(query = "ada").candidates.map { it.id }).containsExactly(2)
        // query by email
        assertThat(model.copy(query = "ACME").candidates.map { it.id }).containsExactly(3)
    }

    // --- load --------------------------------------------------------------
    @Test
    fun `load success lands in Success state`() = runBlocking {
        enqueueLoad()
        val vm = newVm()
        vm.load(1)
        awaitUntil { vm.state.value is UiState.Success }
        val data = (vm.state.value as UiState.Success).data
        assertThat(data.members).hasSize(1)
        assertThat(data.allUsers).hasSize(2)
    }

    @Test
    fun `load members error lands in Error state`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(500).setBody("""{"detail":"boom"}"""))
        server.enqueue(MockResponse().setResponseCode(200).setBody("[$USER_ROW]"))
        val vm = newVm()
        vm.load(1)
        awaitUntil { vm.state.value is UiState.Error }
        assertThat(vm.state.value).isInstanceOf(UiState.Error::class.java)
    }

    @Test
    fun `load users error lands in Error state`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody("[$ROW]"))
        server.enqueue(MockResponse().setResponseCode(403).setBody("""{"detail":"nope"}"""))
        val vm = newVm()
        vm.load(1)
        awaitUntil { vm.state.value is UiState.Error }
        assertThat(vm.state.value).isInstanceOf(UiState.Error::class.java)
    }

    @Test
    fun `onQueryChange updates query in Success state`() = runBlocking {
        enqueueLoad()
        val vm = newVm()
        vm.load(1)
        awaitUntil { vm.state.value is UiState.Success }
        vm.onQueryChange("ada")
        assertThat((vm.state.value as UiState.Success).data.query).isEqualTo("ada")
    }

    @Test
    fun `onQueryChange is a no-op while Loading`() {
        val vm = newVm()
        vm.onQueryChange("x")
        assertThat(vm.state.value).isInstanceOf(UiState.Loading::class.java)
    }

    // --- add ---------------------------------------------------------------
    @Test
    fun `add success reloads to Success state`() = runBlocking {
        enqueueLoad()
        val vm = newVm()
        vm.load(1)
        awaitUntil { vm.state.value is UiState.Success }
        // add POST returns a membership, then load() fires two more responses.
        server.enqueue(MockResponse().setResponseCode(201).setBody(ROW))
        enqueueLoad()
        vm.add(userId = 3, role = ProjectRole.MEMBER)
        awaitUntil { server.requestCount >= 5 && vm.state.value is UiState.Success }
        assertThat(vm.state.value).isInstanceOf(UiState.Success::class.java)
    }

    @Test
    fun `add error keeps prior Success state`() = runBlocking {
        enqueueLoad()
        val vm = newVm()
        vm.load(1)
        awaitUntil { vm.state.value is UiState.Success }
        server.enqueue(MockResponse().setResponseCode(409).setBody("""{"detail":"exists"}"""))
        vm.add(userId = 3, role = ProjectRole.MEMBER)
        awaitUntil { server.requestCount >= 3 }
        assertThat(vm.state.value).isInstanceOf(UiState.Success::class.java)
    }

    // --- changeRole --------------------------------------------------------
    @Test
    fun `changeRole success reloads to Success state`() = runBlocking {
        enqueueLoad()
        val vm = newVm()
        vm.load(1)
        awaitUntil { vm.state.value is UiState.Success }
        server.enqueue(MockResponse().setResponseCode(200).setBody(ROW))
        enqueueLoad()
        vm.changeRole(userId = 2, role = ProjectRole.LEAD)
        awaitUntil { server.requestCount >= 5 && vm.state.value is UiState.Success }
        assertThat(vm.state.value).isInstanceOf(UiState.Success::class.java)
    }

    @Test
    fun `changeRole error keeps prior Success state`() = runBlocking {
        enqueueLoad()
        val vm = newVm()
        vm.load(1)
        awaitUntil { vm.state.value is UiState.Success }
        server.enqueue(MockResponse().setResponseCode(403).setBody("""{"detail":"nope"}"""))
        vm.changeRole(userId = 2, role = ProjectRole.LEAD)
        awaitUntil { server.requestCount >= 3 }
        assertThat(vm.state.value).isInstanceOf(UiState.Success::class.java)
    }

    // --- remove ------------------------------------------------------------
    @Test
    fun `remove success reloads to Success state`() = runBlocking {
        enqueueLoad()
        val vm = newVm()
        vm.load(1)
        awaitUntil { vm.state.value is UiState.Success }
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"ok":true}"""))
        enqueueLoad()
        vm.remove(userId = 2)
        awaitUntil { server.requestCount >= 5 && vm.state.value is UiState.Success }
        assertThat(vm.state.value).isInstanceOf(UiState.Success::class.java)
    }

    @Test
    fun `remove error keeps prior Success state`() = runBlocking {
        enqueueLoad()
        val vm = newVm()
        vm.load(1)
        awaitUntil { vm.state.value is UiState.Success }
        server.enqueue(MockResponse().setResponseCode(403).setBody("""{"detail":"nope"}"""))
        vm.remove(userId = 2)
        awaitUntil { server.requestCount >= 3 }
        assertThat(vm.state.value).isInstanceOf(UiState.Success::class.java)
    }

    private fun membershipOut(userId: Int) =
        com.bughunter.core.network.dto.ProjectMembershipOut(
            id = userId, userId = userId, userName = "n$userId", userEmail = "m$userId@x.io",
            userRole = com.bughunter.core.network.dto.Role.MEMBER,
            projectRole = ProjectRole.MEMBER, createdAt = java.time.Instant.EPOCH,
        )

    private fun userOut(id: Int, name: String, email: String) =
        com.bughunter.core.network.dto.UserOut(
            id = id, name = name, email = email,
            role = com.bughunter.core.network.dto.Role.MEMBER, isActive = true,
            createdAt = java.time.Instant.EPOCH, updatedAt = java.time.Instant.EPOCH,
        )

    private companion object {
        const val ROW = """
            {"id":1,"user_id":2,"user_name":"x","user_email":"x@y.z","user_role":"member",
             "project_role":"member","created_at":"2026-01-01T00:00:00Z"}
        """
        const val USER_ROW = """
            {"id":2,"name":"u","email":"u@example.com","role":"admin","is_active":true,
             "created_at":"2026-01-01T00:00:00Z","updated_at":"2026-01-01T00:00:00Z"}
        """
        const val USER_ROW2 = """
            {"id":3,"name":"v","email":"v@example.com","role":"member","is_active":true,
             "created_at":"2026-01-01T00:00:00Z","updated_at":"2026-01-01T00:00:00Z"}
        """
    }
}
