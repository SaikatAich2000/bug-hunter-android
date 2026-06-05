package com.bughunter.feature.chatbot

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bughunter.core.data.repository.ChatRepository
import com.bughunter.core.data.repository.ChatTurn as RepoTurn
import com.bughunter.core.data.repository.ChatTurn.BotSaid as RepoBotSaid
import com.bughunter.core.data.repository.ChatTurn.BotTyping as RepoBotTyping
import com.bughunter.core.data.repository.ChatTurn.SystemSaid as RepoSystemSaid
import com.bughunter.core.data.repository.ChatTurn.UserSaid as RepoUserSaid
import com.bughunter.core.network.Result2
import com.bughunter.core.network.dto.ChatBlock
import com.bughunter.core.network.dto.ChatIn
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

internal enum class SleuthTab { CHAT, HISTORY, SETTINGS }

internal data class SleuthSettings(
    val autoOpenOnFirstLaunch: Boolean = false,
    val showTypingIndicator: Boolean = true,
)

internal data class SleuthHistoryEntry(
    val id: Long,
    val firstPrompt: String,
    val startedAtEpochMs: Long,
    val turnCount: Int,
)

internal data class SleuthUiState(
    val isPanelOpen: Boolean = false,
    val isTyping: Boolean = false,
    val turns: List<ChatTurn> = emptyList(),
    val input: String = "",
    val selectedTab: SleuthTab = SleuthTab.CHAT,
    val unread: Int = 0,
    val errorMessage: String? = null,
    val history: List<SleuthHistoryEntry> = emptyList(),
    val settings: SleuthSettings = SleuthSettings(),
)

@HiltViewModel
internal class SleuthViewModel @Inject constructor(
    private val repository: ChatRepository,
    private val openBugBus: OpenBugBus,
    private val clock: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    private val _state = MutableStateFlow(SleuthUiState())
    val state: StateFlow<SleuthUiState> = _state.asStateFlow()

    private val sessions = mutableListOf<SleuthHistoryEntry>()

    init {
        repository.turns
            .onEach { repoTurns -> mergeRepoTurns(repoTurns) }
            .launchIn(viewModelScope)
    }

    fun openPanel() {
        _state.value = _state.value.copy(isPanelOpen = true, unread = 0)
        if (_state.value.turns.isEmpty()) {
            seedWelcome()
        }
    }

    fun closePanel() {
        _state.value = _state.value.copy(isPanelOpen = false)
    }

    fun togglePanel() {
        if (_state.value.isPanelOpen) closePanel() else openPanel()
    }

    fun selectTab(tab: SleuthTab) {
        _state.value = _state.value.copy(selectedTab = tab)
    }

    fun onInputChange(value: String) {
        _state.value = _state.value.copy(input = value)
    }

    fun dismissError() {
        _state.value = _state.value.copy(errorMessage = null)
    }

    fun toggleAutoOpen(enabled: Boolean) {
        _state.value = _state.value.copy(
            settings = _state.value.settings.copy(autoOpenOnFirstLaunch = enabled),
        )
    }

    fun toggleShowTyping(enabled: Boolean) {
        _state.value = _state.value.copy(
            settings = _state.value.settings.copy(showTypingIndicator = enabled),
        )
    }

    fun send(textOverride: String? = null) {
        val raw = (textOverride ?: _state.value.input).trim()
        if (raw.isEmpty()) return
        _state.value = _state.value.copy(input = "", errorMessage = null)
        repository.appendUser(raw)
        appendHistory(raw)
        viewModelScope.launch {
            val result = repository.ask(ChatIn(question = raw))
            if (result is Result2.Err) {
                _state.value = _state.value.copy(
                    errorMessage = result.error.toString(),
                )
                repository.appendSystem("Could not reach Sleuth. Try again in a moment.")
            }
        }
    }

    fun onSuggestionTap(suggestion: String) {
        send(textOverride = suggestion)
    }

    fun onConfirm(turnIndex: Int, blockIndex: Int, approved: Boolean) {
        val current = _state.value.turns.toMutableList()
        val turn = current.getOrNull(turnIndex) as? ChatTurn.BotSaid ?: return
        val blocks = turn.blocks.toMutableList()
        val block = blocks.getOrNull(blockIndex) as? RenderedChatBlock.Confirm ?: return
        val resolved = if (approved) {
            RenderedChatBlock.Confirm.Resolution.APPROVED
        } else {
            RenderedChatBlock.Confirm.Resolution.REJECTED
        }
        blocks[blockIndex] = block.copy(resolved = resolved)
        current[turnIndex] = turn.copy(blocks = blocks)
        _state.value = _state.value.copy(turns = current)
        val replyText = if (approved) block.confirmLabel else block.cancelLabel
        send(textOverride = replyText)
    }

    fun onTableRowTap(bugId: Int) {
        viewModelScope.launch { openBugBus.emit(bugId) }
        closePanel()
    }

    fun clearHistory() {
        repository.clear()
        sessions.clear()
        _state.value = _state.value.copy(turns = emptyList(), history = emptyList())
    }

    private fun seedWelcome() {
        val welcome = ChatTurn.SystemSaid(
            text = WELCOME_TEXT,
            createdAtEpochMs = clock(),
        )
        _state.value = _state.value.copy(turns = listOf(welcome))
    }

    private fun appendHistory(prompt: String) {
        if (sessions.isEmpty() || sessions.last().turnCount > MAX_SESSION_TURNS) {
            sessions += SleuthHistoryEntry(
                id = clock(),
                firstPrompt = prompt,
                startedAtEpochMs = clock(),
                turnCount = 1,
            )
        } else {
            val last = sessions.last()
            sessions[sessions.lastIndex] = last.copy(turnCount = last.turnCount + 1)
        }
        _state.value = _state.value.copy(history = sessions.toList())
    }

    private fun mergeRepoTurns(repoTurns: List<RepoTurn>) {
        val current = _state.value
        val converted = repoTurns.map { it.toFeatureTurn() }
        // Preserve any locally-seeded system messages (welcome banner) that are not in the repo log.
        val localOnly = current.turns
            .filter { it is ChatTurn.SystemSaid }
            .filterNot { local -> converted.any { it is ChatTurn.SystemSaid && (it as ChatTurn.SystemSaid).text == (local as ChatTurn.SystemSaid).text } }
        val merged = (localOnly + converted).sortedBy { it.createdAtEpochMs }
        val typing = converted.any { it is ChatTurn.BotTyping }
        val previousBotCount = current.turns.count { it is ChatTurn.BotSaid }
        val newBotCount = converted.count { it is ChatTurn.BotSaid }
        val newUnread = if (current.isPanelOpen) 0 else (current.unread + (newBotCount - previousBotCount)).coerceAtLeast(0)
        _state.value = current.copy(
            turns = merged,
            isTyping = typing,
            unread = newUnread,
        )
    }

    private fun RepoTurn.toFeatureTurn(): ChatTurn = when (this) {
        is RepoUserSaid -> ChatTurn.UserSaid(text, createdAtEpochMs)
        is RepoSystemSaid -> ChatTurn.SystemSaid(text, createdAtEpochMs)
        is RepoBotTyping -> ChatTurn.BotTyping(createdAtEpochMs)
        is RepoBotSaid -> ChatTurn.BotSaid(
            blocks = blocks.map(ChatBlock::toRendered),
            createdAtEpochMs = createdAtEpochMs,
        )
    }

    private companion object {
        const val MAX_SESSION_TURNS = 40
        const val WELCOME_TEXT = "Hi! I'm Sleuth, your Bug Hunter assistant. " +
            "Ask me things like 'show open bugs in PROD', 'bug 42', or 'export bugs in apollo to excel'. " +
            "Type 'help' for the full guide."
    }
}
