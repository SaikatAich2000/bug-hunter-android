package com.bughunter.core.domain.usecase

import com.bughunter.core.data.repository.AuthRepoTestFactory
import com.bughunter.core.network.Result2
import com.bughunter.core.network.dto.MeOut
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test

class RefreshSessionUseCaseTest {

    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
    }

    @After
    fun tearDown() {
        runCatching { server.shutdown() }
    }

    private fun buildUseCase(): RefreshSessionUseCase =
        RefreshSessionUseCase(AuthRepoTestFactory.create(server))

    private val meJson = """
        {
          "id": 1,
          "name": "Ada",
          "email": "ada@b.c",
          "role": "admin",
          "is_active": true,
          "org_id": 7,
          "organization_name": "Acme",
          "organization_slug": "acme",
          "totp_enabled": false
        }
    """.trimIndent()

    @Test
    fun `invoke success returns Ok with me`() = runBlocking {
        val useCase = buildUseCase()
        server.enqueue(MockResponse().setResponseCode(200).setBody(meJson))
        val result = useCase()
        assertThat(result).isInstanceOf(Result2.Ok::class.java)
        assertThat((result as Result2.Ok).value).isInstanceOf(MeOut::class.java)
        assertThat(result.value.email).isEqualTo("ada@b.c")
    }

    @Test
    fun `invoke unauthorized returns Err and clears session`() = runBlocking {
        val useCase = buildUseCase()
        server.enqueue(
            MockResponse().setResponseCode(401)
                .setBody("""{"detail":"Not authenticated"}"""),
        )
        val result = useCase()
        assertThat(result).isInstanceOf(Result2.Err::class.java)
    }

    @Test
    fun `invoke server error returns Err without clearing`() = runBlocking {
        val useCase = buildUseCase()
        server.enqueue(
            MockResponse().setResponseCode(500)
                .setBody("""{"detail":"boom"}"""),
        )
        val result = useCase()
        assertThat(result).isInstanceOf(Result2.Err::class.java)
    }
}
