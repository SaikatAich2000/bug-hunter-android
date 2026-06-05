package com.bughunter.core.data.repository

import com.bughunter.core.network.Result2
import com.bughunter.core.network.api.BrandingApi
import com.bughunter.core.network.dto.BrandingIn
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test

class BrandingRepositoryTest {

    private lateinit var server: MockWebServer
    private lateinit var api: BrandingApi
    private val moshi = RepoTestSupport.moshi()
    private val mapper = RepoTestSupport.errorMapper(moshi)

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
        api = RepoTestSupport.retrofit(server, moshi).create(BrandingApi::class.java)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `get returns branding`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(BRANDING_JSON))
        val repo = BrandingRepository(api, mapper)
        val result = repo.get()
        assertThat(result).isInstanceOf(Result2.Ok::class.java)
    }

    @Test
    fun `update on 422 maps to Validation`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(422)
                .setBody("""{"detail":[{"loc":["body","accent_color"],"msg":"bad","type":"value_error"}]}"""),
        )
        val repo = BrandingRepository(api, mapper)
        val result = repo.update(BrandingIn(accentColor = "?", logoDataUrl = null, emailFromOverride = null))
        assertThat(result).isInstanceOf(Result2.Err::class.java)
    }

    private companion object {
        const val BRANDING_JSON = """
            {"logo_data_url":null,"accent_color":"#123456","email_from_override":null}
        """
    }
}
