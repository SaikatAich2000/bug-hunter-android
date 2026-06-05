package com.bughunter.core.network.api

import com.bughunter.core.network.InstantAdapter
import com.bughunter.core.network.LocalDateAdapter
import com.bughunter.core.network.OmitNullJsonAdapterFactory
import com.bughunter.core.network.dto.registerEnumAdapters
import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.Moshi
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

class MetaApiTest {

    private lateinit var server: MockWebServer
    private lateinit var api: MetaApi

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
        val moshi = registerEnumAdapters(
            Moshi.Builder()
                .add(OmitNullJsonAdapterFactory())
                .add(InstantAdapter())
                .add(LocalDateAdapter()),
        ).build()
        api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(MetaApi::class.java)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `health endpoint hits api health and parses string map`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                "{\"status\":\"ok\",\"version\":\"2.8.0\",\"asset_version\":\"abc123\"}",
            ),
        )
        val out = api.health()
        val recorded = server.takeRequest()
        assertThat(recorded.path).isEqualTo("/api/health")
        assertThat(recorded.method).isEqualTo("GET")
        assertThat(out.status).isEqualTo("ok")
        assertThat(out.version).isEqualTo("2.8.0")
        assertThat(out.assetVersion).isEqualTo("abc123")
    }

    @Test
    fun `meta endpoint hits api meta and parses object`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """
                {
                  "statuses": ["New","In Progress"],
                  "statuses_by_type": {"Bug": ["New"]},
                  "priorities": ["Low","Medium"],
                  "environments": ["DEV","UAT"],
                  "allow_public_signup": true
                }
                """.trimIndent(),
            ),
        )
        val out = api.meta()
        val recorded = server.takeRequest()
        assertThat(recorded.path).isEqualTo("/api/meta")
        assertThat(out.allowPublicSignup).isTrue()
        assertThat(out.priorities).contains("Medium")
    }
}
