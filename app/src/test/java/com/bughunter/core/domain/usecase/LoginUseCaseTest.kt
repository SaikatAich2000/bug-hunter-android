package com.bughunter.core.domain.usecase

import com.bughunter.core.data.repository.AuthRepoTestFactory
import com.bughunter.core.data.repository.RepoTestSupport
import com.bughunter.core.network.EncryptedCookieJar
import com.bughunter.core.network.Result2
import com.bughunter.core.network.dto.LoginResponse
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test

class LoginUseCaseTest {

    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
    }

    @After
    fun tearDown() {
        runCatching { server.shutdown() }
    }

    /** A cookie jar with a CSRF token pre-seeded so invoke() skips the seedCsrf GET. */
    private fun seededJar(): EncryptedCookieJar =
        RepoTestSupport.cookieJar().also { RepoTestSupport.seedCsrf(it, server.url("/")) }

    private fun buildUseCase(jar: EncryptedCookieJar = seededJar()): LoginUseCase =
        LoginUseCase(AuthRepoTestFactory.create(server), jar)

    @Test
    fun `invoke success returns Ok login response`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200)
                .setBody("""{"pending_2fa":true,"pending_token":"opaque-1"}"""),
        )
        val result = buildUseCase()("a@b.c", "secret")
        assertThat(result).isInstanceOf(Result2.Ok::class.java)
        assertThat((result as Result2.Ok).value).isInstanceOf(LoginResponse.AwaitingTotp::class.java)
    }

    @Test
    fun `invoke error returns Err`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(401)
                .setBody("""{"detail":"Invalid credentials"}"""),
        )
        val result = buildUseCase()("a@b.c", "wrong")
        assertThat(result).isInstanceOf(Result2.Err::class.java)
    }

    @Test
    fun `invoke seeds csrf when token missing`() = runBlocking {
        // Empty jar -> no csrf token -> use-case fires an inline GET /api/health first.
        val emptyJar = RepoTestSupport.cookieJar()
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"status":"ok"}"""))
        server.enqueue(
            MockResponse().setResponseCode(200)
                .setBody("""{"pending_2fa":true,"pending_token":"opaque-2"}"""),
        )
        val result = buildUseCase(emptyJar)("a@b.c", "secret")
        assertThat(result).isInstanceOf(Result2.Ok::class.java)
        assertThat(server.requestCount).isAtLeast(2)
    }
}
