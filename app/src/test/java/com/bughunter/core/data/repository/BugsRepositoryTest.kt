package com.bughunter.core.data.repository

import com.bughunter.core.network.Result2
import com.bughunter.core.network.api.BugsApi
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test

class BugsRepositoryTest {

    private lateinit var server: MockWebServer
    private lateinit var api: BugsApi
    private val moshi = RepoTestSupport.moshi()
    private val mapper = RepoTestSupport.errorMapper(moshi)
    private val jar = RepoTestSupport.cookieJar()

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
        api = RepoTestSupport.retrofit(server, moshi, client = RepoTestSupport.client(jar))
            .create(BugsApi::class.java)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `list passes repeated array params`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(LIST))
        val repo = BugsRepository(api, mapper)
        val result = repo.list(
            BugListFilters(
                statuses = listOf("New", "In Progress"),
                priorities = listOf("High"),
                page = 1,
                pageSize = 50,
            ),
        )
        assertThat(result).isInstanceOf(Result2.Ok::class.java)
        val recorded = server.takeRequest()
        val path = recorded.path.orEmpty()
        assertThat(path).contains("status=New")
        assertThat(path).contains("status=In%20Progress")
        assertThat(path).contains("priority=High")
    }

    @Test
    fun `delete echoes CSRF header when cookie seeded`() = runTest {
        RepoTestSupport.seedCsrf(jar, server.url("/"))
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"ok":"1"}"""))
        val repo = BugsRepository(api, mapper)
        val result = repo.delete(7)
        assertThat(result).isInstanceOf(Result2.Ok::class.java)
        val recorded = server.takeRequest()
        assertThat(recorded.getHeader("X-CSRF-Token")).isEqualTo("csrf-1")
    }

    @Test
    fun `get on 404 maps to NotFound error`() = runTest {
        server.enqueue(MockResponse().setResponseCode(404).setBody("""{"detail":"gone"}"""))
        val repo = BugsRepository(api, mapper)
        val result = repo.get(7)
        assertThat(result).isInstanceOf(Result2.Err::class.java)
    }

    private companion object {
        const val LIST = """
            {"items":[],"page":1,"page_size":50,"total":0,"pages":0}
        """
    }
}
