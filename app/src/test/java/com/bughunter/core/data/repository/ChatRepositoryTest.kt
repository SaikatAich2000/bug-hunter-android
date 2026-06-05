package com.bughunter.core.data.repository

import com.bughunter.core.network.Result2
import com.bughunter.core.network.api.ChatApi
import com.bughunter.core.network.dto.ChatIn
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test

class ChatRepositoryTest {

    private lateinit var server: MockWebServer
    private lateinit var api: ChatApi
    private val moshi = RepoTestSupport.moshi()
    private val mapper = RepoTestSupport.errorMapper(moshi)
    private var nowMs = 1_000L

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
        api = RepoTestSupport.retrofit(server, moshi).create(ChatApi::class.java)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `ask appends a bot turn on success`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(REPLY))
        val repo = ChatRepository(api, mapper, clock = { nowMs })
        val result = repo.ask(ChatIn(question = "show me"))
        assertThat(result).isInstanceOf(Result2.Ok::class.java)
        val turns = repo.turns.value
        assertThat(turns).hasSize(1)
        assertThat(turns.first()).isInstanceOf(ChatTurn.BotSaid::class.java)
    }

    @Test
    fun `ask on 500 leaves no bot turn`() = runTest {
        server.enqueue(MockResponse().setResponseCode(500).setBody("""{"detail":"boom"}"""))
        val repo = ChatRepository(api, mapper, clock = { nowMs })
        val result = repo.ask(ChatIn(question = "x"))
        assertThat(result).isInstanceOf(Result2.Err::class.java)
        assertThat(repo.turns.value.none { it is ChatTurn.BotSaid }).isTrue()
    }

    private companion object {
        const val REPLY = """
            {"blocks":[{"type":"text","text":"hello"}]}
        """
    }
}
