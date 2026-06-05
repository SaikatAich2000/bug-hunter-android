package com.bughunter.feature.chatbot

internal sealed interface ChatTurn {
    val createdAtEpochMs: Long

    data class UserSaid(
        val text: String,
        override val createdAtEpochMs: Long,
    ) : ChatTurn

    data class BotSaid(
        val blocks: List<RenderedChatBlock>,
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
