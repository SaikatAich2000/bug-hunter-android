package com.bughunter.core.data.repository

import com.bughunter.core.network.Result2
import com.bughunter.core.network.api.ProjectsApi
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test

class ProjectsRepositoryTest {

    private lateinit var server: MockWebServer
    private lateinit var api: ProjectsApi
    private val moshi = RepoTestSupport.moshi()
    private val mapper = RepoTestSupport.errorMapper(moshi)

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
        api = RepoTestSupport.retrofit(server, moshi).create(ProjectsApi::class.java)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `list returns projects`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("[$PROJECT]"))
        val repo = ProjectsRepository(api, mapper)
        val result = repo.list()
        assertThat(result).isInstanceOf(Result2.Ok::class.java)
    }

    @Test
    fun `delete 404 maps to NotFound`() = runTest {
        server.enqueue(MockResponse().setResponseCode(404).setBody("""{"detail":"gone"}"""))
        val repo = ProjectsRepository(api, mapper)
        val result = repo.delete(99)
        assertThat(result).isInstanceOf(Result2.Err::class.java)
    }

    private companion object {
        const val PROJECT = """
            {"id":1,"name":"Apollo","key":"APO","description":"d","color":"#c9764f",
             "created_at":"2026-01-01T00:00:00Z","updated_at":"2026-01-01T00:00:00Z",
             "can_manage":true,"member_count":3}
        """
    }
}
