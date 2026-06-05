package com.bughunter.core.data.repository

import com.bughunter.core.network.Result2
import com.bughunter.core.network.api.OrganizationApi
import com.bughunter.core.network.dto.OrganizationUpdate
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test

class OrganizationRepositoryTest {

    private lateinit var server: MockWebServer
    private lateinit var api: OrganizationApi
    private val moshi = RepoTestSupport.moshi()
    private val mapper = RepoTestSupport.errorMapper(moshi)

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
        api = RepoTestSupport.retrofit(server, moshi).create(OrganizationApi::class.java)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `get hits the organization endpoint`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(ORG_JSON))
        val repo = OrganizationRepository(api, mapper)
        val result = repo.get()
        assertThat(result).isInstanceOf(Result2.Ok::class.java)
        val recorded = server.takeRequest()
        assertThat(recorded.path).isEqualTo("/api/organization")
        assertThat(recorded.method).isEqualTo("GET")
    }

    @Test
    fun `update forwards 403 as Forbidden`() = runTest {
        server.enqueue(MockResponse().setResponseCode(403).setBody("""{"detail":"nope"}"""))
        val repo = OrganizationRepository(api, mapper)
        val result = repo.update(OrganizationUpdate(name = "Acme", description = null))
        assertThat(result).isInstanceOf(Result2.Err::class.java)
    }

    private companion object {
        const val ORG_JSON = """
            {"id":1,"name":"Acme","slug":"acme","description":"x","created_at":"2026-01-01T00:00:00Z"}
        """
    }
}
