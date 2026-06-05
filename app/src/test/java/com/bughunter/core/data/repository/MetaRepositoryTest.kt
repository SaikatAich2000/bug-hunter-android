package com.bughunter.core.data.repository

import com.bughunter.core.network.Result2
import com.bughunter.core.network.api.MetaApi
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test

class MetaRepositoryTest {

    private lateinit var server: MockWebServer
    private lateinit var api: MetaApi
    private val moshi = RepoTestSupport.moshi()
    private val mapper = RepoTestSupport.errorMapper(moshi)

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
        api = RepoTestSupport.retrofit(server, moshi).create(MetaApi::class.java)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `meta caches result for an hour`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(META_JSON))
        var now = 1_000_000L
        val repo = MetaRepository(api, mapper, clock = { now })

        val first = repo.meta()
        assertThat(first).isInstanceOf(Result2.Ok::class.java)
        // Within TTL — should not hit the wire again.
        now += 10_000L
        val cached = repo.meta()
        assertThat(cached).isInstanceOf(Result2.Ok::class.java)
        assertThat(server.requestCount).isEqualTo(1)
    }

    @Test
    fun `health surfaces server error`() = runTest {
        server.enqueue(MockResponse().setResponseCode(500).setBody("""{"detail":"boom"}"""))
        val repo = MetaRepository(api, mapper)
        val result = repo.health()
        assertThat(result).isInstanceOf(Result2.Err::class.java)
    }

    private companion object {
        const val META_JSON = """
            {
              "statuses": ["New","In Progress"],
              "statuses_by_type": {"Bug": ["New"]},
              "priorities": ["Low","Medium","High","Critical"],
              "environments": ["DEV","UAT","PROD"],
              "allow_public_signup": true
            }
        """
    }
}
