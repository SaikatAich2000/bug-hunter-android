package com.bughunter.core.data.repository

import com.bughunter.core.network.Result2
import com.bughunter.core.network.api.WebhooksApi
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test

class WebhooksRepositoryTest {

    private lateinit var server: MockWebServer
    private lateinit var api: WebhooksApi
    private val moshi = RepoTestSupport.moshi()
    private val mapper = RepoTestSupport.errorMapper(moshi)

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
        api = RepoTestSupport.retrofit(server, moshi).create(WebhooksApi::class.java)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `test endpoint returns 202`() = runTest {
        server.enqueue(MockResponse().setResponseCode(202).setBody("""{"queued":"1"}"""))
        val repo = WebhooksRepository(api, mapper)
        val result = repo.test(1)
        assertThat(result).isInstanceOf(Result2.Ok::class.java)
    }

    @Test
    fun `delete 401 maps to Unauthorized`() = runTest {
        server.enqueue(MockResponse().setResponseCode(401).setBody("""{"detail":"Not authenticated"}"""))
        val repo = WebhooksRepository(api, mapper)
        val result = repo.delete(1)
        assertThat(result).isInstanceOf(Result2.Err::class.java)
    }
}
