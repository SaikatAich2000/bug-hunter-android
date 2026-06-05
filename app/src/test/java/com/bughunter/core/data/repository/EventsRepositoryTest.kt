package com.bughunter.core.data.repository

import com.bughunter.core.network.Result2
import com.bughunter.core.network.api.EventsApi
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test

class EventsRepositoryTest {

    private lateinit var server: MockWebServer
    private lateinit var api: EventsApi
    private val moshi = RepoTestSupport.moshi()
    private val mapper = RepoTestSupport.errorMapper(moshi)

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
        api = RepoTestSupport.retrofit(server, moshi).create(EventsApi::class.java)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `list passes scheduled_for query`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("[]"))
        val repo = EventsRepository(api, mapper)
        val result = repo.list("2026-06-01")
        assertThat(result).isInstanceOf(Result2.Ok::class.java)
        val recorded = server.takeRequest()
        assertThat(recorded.path).contains("scheduled_for=2026-06-01")
    }

    @Test
    fun `delete 500 maps to Server error`() = runTest {
        server.enqueue(MockResponse().setResponseCode(500).setBody("""{"detail":"boom"}"""))
        val repo = EventsRepository(api, mapper)
        val result = repo.delete(1)
        assertThat(result).isInstanceOf(Result2.Err::class.java)
    }
}
