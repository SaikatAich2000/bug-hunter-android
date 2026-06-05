package com.bughunter.core.data.repository

import com.bughunter.core.network.Result2
import com.bughunter.core.network.api.MembershipsApi
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test

class MembershipsRepositoryTest {

    private lateinit var server: MockWebServer
    private lateinit var api: MembershipsApi
    private val moshi = RepoTestSupport.moshi()
    private val mapper = RepoTestSupport.errorMapper(moshi)

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
        api = RepoTestSupport.retrofit(server, moshi).create(MembershipsApi::class.java)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `list returns memberships`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("[$ROW]"))
        val repo = MembershipsRepository(api, mapper)
        val result = repo.list(1)
        assertThat(result).isInstanceOf(Result2.Ok::class.java)
    }

    @Test
    fun `remove 403 maps to Forbidden`() = runTest {
        server.enqueue(MockResponse().setResponseCode(403).setBody("""{"detail":"nope"}"""))
        val repo = MembershipsRepository(api, mapper)
        val result = repo.remove(1, 1)
        assertThat(result).isInstanceOf(Result2.Err::class.java)
    }

    private companion object {
        const val ROW = """
            {"id":1,"user_id":2,"user_name":"x","user_email":"x@y.z","user_role":"member",
             "project_role":"member","created_at":"2026-01-01T00:00:00Z"}
        """
    }
}
