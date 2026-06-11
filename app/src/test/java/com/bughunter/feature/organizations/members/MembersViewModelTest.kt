package com.bughunter.feature.organizations.members

import com.bughunter.core.data.repository.RepoTestSupport
import com.bughunter.core.data.repository.UsersRepository
import com.bughunter.core.network.api.UsersApi
import com.bughunter.core.network.dto.Role
import com.bughunter.core.network.dto.UserOut
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
class MembersViewModelTest {

    private lateinit var server: MockWebServer
    private lateinit var repo: UsersRepository
    private val moshi = RepoTestSupport.moshi()
    private val mapper = RepoTestSupport.errorMapper(moshi)
    private val jar = RepoTestSupport.cookieJar()

    @Before
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Unconfined)
        server = MockWebServer().apply { start() }
        RepoTestSupport.seedCsrf(jar, server.url("/"))
        val api = RepoTestSupport.retrofit(server, moshi, client = RepoTestSupport.client(jar))
            .create(UsersApi::class.java)
        repo = UsersRepository(api, mapper)
    }

    @After
    fun tearDown() {
        runCatching { server.shutdown() }
        Dispatchers.resetMain()
    }

    private suspend fun awaitUntil(timeoutMs: Long = 4_000, check: () -> Boolean) {
        withTimeout(timeoutMs) { while (!check()) delay(10) }
    }

    private fun user(id: Int, name: String, email: String = "u$id@x.io") = UserOut(
        id = id, name = name, email = email, role = Role.MEMBER, isActive = true,
        createdAt = Instant.EPOCH, updatedAt = Instant.EPOCH,
    )

    // --- MembersModel pure logic ------------------------------------------
    @Test
    fun `filtered matches name or email case-insensitively`() {
        val m = MembersModel(users = listOf(user(1, "Ada"), user(2, "Bob", "bob@acme.io")))
        assertThat(m.copy(query = "ada").filtered.map { it.id }).containsExactly(1)
        assertThat(m.copy(query = "ACME").filtered.map { it.id }).containsExactly(2)
        assertThat(m.copy(query = "").filtered).hasSize(2)
    }

    @Test
    fun `pagination slices the filtered list and counts pages`() {
        val users = (1..45).map { user(it, "U$it") }
        val m = MembersModel(users = users, pageSize = 20)
        assertThat(m.totalPages).isEqualTo(3)
        assertThat(m.pagedUsers).hasSize(20)
        assertThat(m.copy(page = 2).pagedUsers).hasSize(5)   // 45 - 40
    }

    @Test
    fun `totalPages is at least 1 even when empty`() {
        assertThat(MembersModel(users = emptyList()).totalPages).isEqualTo(1)
    }

    // --- load / actions ----------------------------------------------------
    @Test
    fun `load success lands in Success state`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody(moshiList(user(1, "Ada"))))
        val vm = MembersViewModel(repo)
        awaitUntil { vm.state.value is UiState.Success }
        assertThat((vm.state.value as UiState.Success).data.users).hasSize(1)
    }

    @Test
    fun `load failure lands in Error state`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(500).setBody("""{"detail":"x"}"""))
        val vm = MembersViewModel(repo)
        awaitUntil { vm.state.value is UiState.Error }
        assertThat(vm.state.value).isInstanceOf(UiState.Error::class.java)
    }

    @Test
    fun `changeRole failure records an action error`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody(moshiList(user(1, "Ada"))))
        val vm = MembersViewModel(repo)
        awaitUntil { vm.state.value is UiState.Success }
        server.enqueue(MockResponse().setResponseCode(403).setBody("""{"detail":"forbidden"}"""))
        vm.changeRole(1, Role.ADMIN)
        awaitUntil { (vm.state.value as? UiState.Success)?.data?.actionError != null }
        assertThat((vm.state.value as UiState.Success).data.actionError).isNotNull()
        vm.dismissActionError()
        assertThat((vm.state.value as UiState.Success).data.actionError).isNull()
    }

    @Test
    fun `query change resets to page 0`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody(moshiList(user(1, "Ada"))))
        val vm = MembersViewModel(repo)
        awaitUntil { vm.state.value is UiState.Success }
        vm.nextPage()
        vm.onQueryChange("a")
        assertThat((vm.state.value as UiState.Success).data.page).isEqualTo(0)
    }

    private fun moshiList(vararg u: UserOut): String {
        val t = com.squareup.moshi.Types.newParameterizedType(List::class.java, UserOut::class.java)
        return moshi.adapter<List<UserOut>>(t).toJson(u.toList())
    }
}
