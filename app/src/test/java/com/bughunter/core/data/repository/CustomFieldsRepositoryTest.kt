package com.bughunter.core.data.repository

import com.bughunter.core.network.Result2
import com.bughunter.core.network.api.CustomFieldsApi
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test

class CustomFieldsRepositoryTest {

    private lateinit var server: MockWebServer
    private lateinit var api: CustomFieldsApi
    private val moshi = RepoTestSupport.moshi()
    private val mapper = RepoTestSupport.errorMapper(moshi)

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
        api = RepoTestSupport.retrofit(server, moshi).create(CustomFieldsApi::class.java)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `listForProject returns rows`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("[]"))
        val repo = CustomFieldsRepository(api, mapper)
        val result = repo.listForProject(7)
        assertThat(result).isInstanceOf(Result2.Ok::class.java)
        val recorded = server.takeRequest()
        assertThat(recorded.path).isEqualTo("/api/projects/7/custom-fields")
    }

    @Test
    fun `delete on 500 maps to Server`() = runTest {
        server.enqueue(MockResponse().setResponseCode(500).setBody("""{"detail":"boom"}"""))
        val repo = CustomFieldsRepository(api, mapper)
        val result = repo.delete(1, 2)
        assertThat(result).isInstanceOf(Result2.Err::class.java)
    }
}
