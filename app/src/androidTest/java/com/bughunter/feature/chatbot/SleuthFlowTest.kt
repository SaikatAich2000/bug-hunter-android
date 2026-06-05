package com.bughunter.feature.chatbot

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.bughunter.core.ui.theme.BugHunterTheme
import org.junit.Rule
import org.junit.Test

internal class SleuthFlowTest {

    @get:Rule val composeRule = createComposeRule()

    @Test
    fun fabOpensPanelAndSendAppendsUserBubble() {
        composeRule.setContent {
            BugHunterTheme {
                var state by remember { mutableStateOf(SleuthUiState()) }
                SleuthOverlayContent(
                    state = state,
                    onFabClick = { state = state.copy(isPanelOpen = !state.isPanelOpen) },
                    onClose = { state = state.copy(isPanelOpen = false) },
                    onClear = { state = state.copy(turns = emptyList()) },
                    onTabSelected = { tab -> state = state.copy(selectedTab = tab) },
                    onInputChange = { v -> state = state.copy(input = v) },
                    onSend = {
                        val raw = state.input.trim()
                        if (raw.isNotEmpty()) {
                            state = state.copy(
                                turns = state.turns + ChatTurn.UserSaid(text = raw, createdAtEpochMs = 0L) +
                                    ChatTurn.BotSaid(
                                        blocks = listOf(RenderedChatBlock.Text("Sleuth heard you", null)),
                                        createdAtEpochMs = 1L,
                                    ),
                                input = "",
                            )
                        }
                    },
                    onSuggestionTap = {},
                    onConfirm = { _, _, _ -> },
                    onRowTap = {},
                    onDownload = {},
                    onAutoOpenChange = {},
                    onShowTypingChange = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription("Open Sleuth").performClick()
        composeRule.onNodeWithText("Chat").assertIsDisplayed()
        composeRule.onNodeWithText("Ask Sleuth…").performTextInput("hello sleuth")
        composeRule.onNodeWithContentDescription("Send").performClick()
        composeRule.onNodeWithText("hello sleuth").assertIsDisplayed()
        composeRule.onNodeWithText("Sleuth heard you").assertIsDisplayed()
    }
}
