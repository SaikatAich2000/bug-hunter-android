package com.bughunter.core.data.repository

import com.bughunter.core.network.Result2
import com.bughunter.core.network.api.InvitationsApi
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test

class InvitationsRepositoryTest {

    private lateinit var server: MockWebServer
    private lateinit var api: InvitationsApi
    private val moshi = RepoTestSupport.moshi()
    private val mapper = RepoTestSupport.errorMapper(moshi)

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
        api = RepoTestSupport.retrofit(server, moshi).create(InvitationsApi::class.java)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `preview parses payload`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(PREVIEW))
        val repo = InvitationsRepository(api, mapper)
        val result = repo.preview("tok-1")
        assertThat(result).isInstanceOf(Result2.Ok::class.java)
        val recorded = server.takeRequest()
        assertThat(recorded.path).isEqualTo("/api/invitations/preview/tok-1")
    }

    @Test
    fun `preview 404 maps to NotFound`() = runTest {
        server.enqueue(MockResponse().setResponseCode(404).setBody("""{"detail":"gone"}"""))
        val repo = InvitationsRepository(api, mapper)
        val result = repo.preview("missing")
        assertThat(result).isInstanceOf(Result2.Err::class.java)
    }

    private companion object {
        const val PREVIEW = """
            {"email":"a@b.c","organization_name":"Acme","role":"member",
             "expires_at":"2026-02-01T00:00:00Z","invited_by_name":"alice"}
        """
    }
}
