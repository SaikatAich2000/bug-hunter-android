package com.bughunter.core.data.repository

import com.bughunter.core.network.Result2
import com.bughunter.core.network.api.SessionsApi
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test

class SessionsRepositoryTest {

    private lateinit var server: MockWebServer
    private lateinit var api: SessionsApi
    private val moshi = RepoTestSupport.moshi()
    private val mapper = RepoTestSupport.errorMapper(moshi)

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
        api = RepoTestSupport.retrofit(server, moshi).create(SessionsApi::class.java)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `list returns sessions`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("[]"))
        val repo = SessionsRepository(api, mapper)
        val result = repo.list()
        assertThat(result).isInstanceOf(Result2.Ok::class.java)
    }

    @Test
    fun `revoke 409 maps to Conflict`() = runTest {
        server.enqueue(MockResponse().setResponseCode(409).setBody("""{"detail":"current session"}"""))
        val repo = SessionsRepository(api, mapper)
        val result = repo.revoke(1)
        assertThat(result).isInstanceOf(Result2.Err::class.java)
    }
}
