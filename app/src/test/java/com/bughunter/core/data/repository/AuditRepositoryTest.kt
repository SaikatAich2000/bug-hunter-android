package com.bughunter.core.data.repository

import com.bughunter.core.network.Result2
import com.bughunter.core.network.api.AuditApi
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test

class AuditRepositoryTest {

    private lateinit var server: MockWebServer
    private lateinit var api: AuditApi
    private val moshi = RepoTestSupport.moshi()
    private val mapper = RepoTestSupport.errorMapper(moshi)

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
        api = RepoTestSupport.retrofit(server, moshi).create(AuditApi::class.java)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `list forwards filter params`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("[]"))
        val repo = AuditRepository(api, mapper)
        val result = repo.list(entityType = "bug", actorUserId = 5, query = "boom")
        assertThat(result).isInstanceOf(Result2.Ok::class.java)
        val recorded = server.takeRequest()
        val path = recorded.path.orEmpty()
        assertThat(path).contains("entity_type=bug")
        assertThat(path).contains("actor_user_id=5")
        assertThat(path).contains("q=boom")
    }

    @Test
    fun `list 403 maps to Forbidden`() = runTest {
        server.enqueue(MockResponse().setResponseCode(403).setBody("""{"detail":"admin only"}"""))
        val repo = AuditRepository(api, mapper)
        val result = repo.list()
        assertThat(result).isInstanceOf(Result2.Err::class.java)
    }
}
