package com.bughunter.core.data.repository

import com.bughunter.core.network.Result2
import com.bughunter.core.network.api.StatsApi
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test

class StatsRepositoryTest {

    private lateinit var server: MockWebServer
    private lateinit var api: StatsApi
    private val moshi = RepoTestSupport.moshi()
    private val mapper = RepoTestSupport.errorMapper(moshi)

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
        api = RepoTestSupport.retrofit(server, moshi).create(StatsApi::class.java)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `get parses stats payload`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(STATS))
        val repo = StatsRepository(api, mapper)
        val result = repo.get(itemType = "Bug")
        assertThat(result).isInstanceOf(Result2.Ok::class.java)
        val recorded = server.takeRequest()
        assertThat(recorded.path).contains("item_type=Bug")
    }

    @Test
    fun `get on network failure maps to Network`() = runTest {
        server.shutdown()
        val repo = StatsRepository(api, mapper)
        val result = repo.get()
        assertThat(result).isInstanceOf(Result2.Err::class.java)
    }

    private companion object {
        const val STATS = """
            {"bugs":0,"open":0,"resolved":0,"closed":0,"resolve_later":0,
             "projects":0,"users":0,
             "by_status":{},"by_priority":{},"by_environment":{},"by_type":{},
             "by_project":[],"by_assignee":[],"timeline":[]}
        """
    }
}
