package com.bughunter.core.domain.usecase

import com.bughunter.core.data.repository.AuthRepoTestFactory
import com.bughunter.core.data.repository.ChatRepository
import com.bughunter.core.data.repository.MetaRepository
import com.bughunter.core.data.repository.RepoTestSupport
import com.bughunter.core.network.Result2
import com.bughunter.core.network.api.ChatApi
import com.bughunter.core.network.api.MetaApi
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test

class LogoutUseCaseTest {

    private lateinit var server: MockWebServer
    private val moshi = RepoTestSupport.moshi()

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
    }

    @After
    fun tearDown() {
        runCatching { server.shutdown() }
    }

    private fun buildUseCase(): LogoutUseCase {
        val mapper = RepoTestSupport.errorMapper(moshi)
        val retrofit = RepoTestSupport.retrofit(server, moshi)
        val chatRepo = ChatRepository(retrofit.create(ChatApi::class.java), mapper)
        val metaRepo = MetaRepository(retrofit.create(MetaApi::class.java), mapper)
        return LogoutUseCase(AuthRepoTestFactory.create(server, moshi), chatRepo, metaRepo)
    }

    @Test
    fun `invoke logs out and clears local repositories`() = runBlocking {
        val useCase = buildUseCase()
        server.enqueue(MockResponse().setResponseCode(204))
        val result = useCase()
        assertThat(result).isInstanceOf(Result2.Ok::class.java)
    }

    @Test
    fun `invoke returns Err when logout fails`() = runBlocking {
        val useCase = buildUseCase()
        server.enqueue(
            MockResponse().setResponseCode(500)
                .setBody("""{"detail":"boom"}"""),
        )
        val result = useCase()
        assertThat(result).isInstanceOf(Result2.Err::class.java)
    }
}
