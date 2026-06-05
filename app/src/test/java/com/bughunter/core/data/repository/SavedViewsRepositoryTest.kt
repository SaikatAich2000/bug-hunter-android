package com.bughunter.core.data.repository

import com.bughunter.core.network.Result2
import com.bughunter.core.network.api.SavedViewsApi
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test

class SavedViewsRepositoryTest {

    private lateinit var server: MockWebServer
    private lateinit var api: SavedViewsApi
    private val moshi = RepoTestSupport.moshi()
    private val mapper = RepoTestSupport.errorMapper(moshi)

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
        api = RepoTestSupport.retrofit(server, moshi).create(SavedViewsApi::class.java)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `list returns saved views`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("[]"))
        val repo = SavedViewsRepository(api, mapper)
        val result = repo.list()
        assertThat(result).isInstanceOf(Result2.Ok::class.java)
    }

    @Test
    fun `delete 401 maps to Unauthorized`() = runTest {
        server.enqueue(MockResponse().setResponseCode(401).setBody("""{"detail":"Not authenticated"}"""))
        val repo = SavedViewsRepository(api, mapper)
        val result = repo.delete(1)
        assertThat(result).isInstanceOf(Result2.Err::class.java)
    }
}
