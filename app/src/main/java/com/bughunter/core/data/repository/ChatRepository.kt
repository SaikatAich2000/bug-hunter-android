package com.bughunter.core.data.repository

import com.bughunter.core.network.ErrorMapper
import com.bughunter.core.network.Result2
import com.bughunter.core.network.api.ChatApi
import com.bughunter.core.network.dto.ChatBlock
import com.bughunter.core.network.dto.ChatIn
import com.bughunter.core.network.dto.ChatOut
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.ResponseBody
import javax.inject.Inject
import javax.inject.Singleton

internal sealed interface ChatTurn {
    val createdAtEpochMs: Long

    data class UserSaid(
        val text: String,
        override val createdAtEpochMs: Long,
    ) : ChatTurn

    data class BotSaid(
        val blocks: List<ChatBlock>,
        override val createdAtEpochMs: Long,
    ) : ChatTurn

    data class BotTyping(
        override val createdAtEpochMs: Long,
    ) : ChatTurn

    data class SystemSaid(
        val text: String,
        override val createdAtEpochMs: Long,
    ) : ChatTurn
}

@Singleton
internal class ChatRepository @Inject constructor(
    private val api: ChatApi,
    private val errorMapper: ErrorMapper,
    private val clock: () -> Long = System::currentTimeMillis,
) {

    private val _turns = MutableStateFlow<List<ChatTurn>>(emptyList())
    val turns: StateFlow<List<ChatTurn>> = _turns.asStateFlow()

    fun appendUser(text: String) {
        _turns.value = _turns.value + ChatTurn.UserSaid(text = text, createdAtEpochMs = clock())
    }

    fun appendSystem(text: String) {
        _turns.value = _turns.value + ChatTurn.SystemSaid(text = text, createdAtEpochMs = clock())
    }

    fun setTyping(typing: Boolean) {
        val current = _turns.value
        val withoutTyping = current.filterNot { it is ChatTurn.BotTyping }
        _turns.value = if (typing) withoutTyping + ChatTurn.BotTyping(clock()) else withoutTyping
    }

    suspend fun ask(body: ChatIn): Result2<ChatOut> {
        setTyping(true)
        val result = runResult(errorMapper) { api.ask(body) }
        setTyping(false)
        if (result is Result2.Ok) {
            _turns.value = _turns.value + ChatTurn.BotSaid(
                blocks = result.value.blocks,
                createdAtEpochMs = clock(),
            )
        }
        return result
    }

    suspend fun download(token: String): Result2<ResponseBody> =
        runResult(errorMapper) { api.download(token) }

    fun clear() {
        _turns.value = emptyList()
    }
}
