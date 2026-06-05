package com.bughunter.core.data.repository

import com.bughunter.core.network.Result2
import com.bughunter.core.network.api.UsersApi
import com.bughunter.core.network.dto.UserIn
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test

class UsersRepositoryTest {

    private lateinit var server: MockWebServer
    private lateinit var api: UsersApi
    private val moshi = RepoTestSupport.moshi()
    private val mapper = RepoTestSupport.errorMapper(moshi)

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
        api = RepoTestSupport.retrofit(server, moshi).create(UsersApi::class.java)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `list returns users`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("[$USER_ROW]"))
        val repo = UsersRepository(api, mapper)
        val result = repo.list()
        assertThat(result).isInstanceOf(Result2.Ok::class.java)
        assertThat((result as Result2.Ok).value).hasSize(1)
    }

    @Test
    fun `create on 409 maps to Conflict`() = runTest {
        server.enqueue(MockResponse().setResponseCode(409).setBody("""{"detail":"exists"}"""))
        val repo = UsersRepository(api, mapper)
        val result = repo.create(
            UserIn(
                name = "x",
                email = "a@b.c",
                role = com.bughunter.core.network.dto.Role.MEMBER,
                password = "passw0rd",
                isActive = true,
            ),
        )
        assertThat(result).isInstanceOf(Result2.Err::class.java)
    }

    private companion object {
        const val USER_ROW = """
            {"id":1,"name":"u","email":"u@example.com","role":"admin","is_active":true,
             "created_at":"2026-01-01T00:00:00Z","updated_at":"2026-01-01T00:00:00Z"}
        """
    }
}
