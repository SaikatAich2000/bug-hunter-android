package com.bughunter.feature.chatbot

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bughunter.core.ui.components.BhIconButton
import com.bughunter.core.ui.theme.BhInputShape
import com.bughunter.core.ui.theme.BhSleuthPanelShape
import com.bughunter.core.ui.theme.LocalAccentGradient
import com.bughunter.core.ui.theme.LocalBrandTokens
import com.bughunter.feature.chatbot.blocks.ConfirmBlockRenderer
import com.bughunter.feature.chatbot.blocks.FileBlockRenderer
import com.bughunter.feature.chatbot.blocks.SuggestionsRow
import com.bughunter.feature.chatbot.blocks.TableBlockRenderer
import com.bughunter.feature.chatbot.blocks.TextBlockRenderer
import com.bughunter.feature.chatbot.messages.BotMessageBubble
import com.bughunter.feature.chatbot.messages.SystemMessageBubble
import com.bughunter.feature.chatbot.messages.TypingIndicator
import com.bughunter.feature.chatbot.messages.UserMessageBubble
import com.bughunter.feature.chatbot.tabs.HistoryTab
import com.bughunter.feature.chatbot.tabs.SettingsTab

@Composable
internal fun SleuthPanel(
    state: SleuthUiState,
    onClose: () -> Unit,
    onClear: () -> Unit,
    onTabSelected: (SleuthTab) -> Unit,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onSuggestionTap: (String) -> Unit,
    onConfirm: (turnIndex: Int, blockIndex: Int, approved: Boolean) -> Unit,
    onRowTap: (Int) -> Unit,
    onDownload: (String) -> Unit,
    onAutoOpenChange: (Boolean) -> Unit,
    onShowTypingChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalBrandTokens.current
    Column(
        modifier = modifier
            .widthIn(max = 400.dp)
            .heightIn(max = 560.dp)
            .clip(BhSleuthPanelShape)
            .background(MaterialTheme.colorScheme.surface)
            .border(BorderStroke(1.dp, tokens.border), BhSleuthPanelShape),
    ) {
        PanelHeader(onClose = onClose, onClear = onClear)
        TabsRow(selected = state.selectedTab, onSelected = onTabSelected)
        Box(modifier = Modifier.weight(1f)) {
            when (state.selectedTab) {
                SleuthTab.CHAT -> ChatTab(
                    state = state,
                    onSuggestionTap = onSuggestionTap,
                    onConfirm = onConfirm,
                    onRowTap = onRowTap,
                    onDownload = onDownload,
                )
                SleuthTab.HISTORY -> HistoryTab(
                    entries = state.history,
                    onEntryClick = { /* in-memory only for v2.8 */ },
                    onClearAll = onClear,
                )
                SleuthTab.SETTINGS -> SettingsTab(
                    settings = state.settings,
                    onAutoOpenChange = onAutoOpenChange,
                    onShowTypingChange = onShowTypingChange,
                )
            }
        }
        if (state.selectedTab == SleuthTab.CHAT) {
            InputRow(
                value = state.input,
                onChange = onInputChange,
                onSend = onSend,
            )
        }
    }
}

@Composable
private fun PanelHeader(
    onClose: () -> Unit,
    onClear: () -> Unit,
) {
    val gradient = LocalAccentGradient.current
    val tokens = LocalBrandTokens.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(brush = gradient)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(color = Color.White.copy(alpha = 0.18f), shape = CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "S",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold,
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Sleuth",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold,
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(color = tokens.sleuthOnline, shape = CircleShape),
                )
                Text(
                    text = "Online",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.9f),
                )
            }
        }
        BhIconButton(
            icon = Icons.Filled.DeleteOutline,
            contentDescription = "Clear conversation",
            onClick = onClear,
        )
        BhIconButton(
            icon = Icons.Filled.Close,
            contentDescription = "Close",
            onClick = onClose,
        )
    }
}

@Composable
private fun TabsRow(selected: SleuthTab, onSelected: (SleuthTab) -> Unit) {
    val tokens = LocalBrandTokens.current
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            SleuthTab.entries.forEach { tab ->
                val label = when (tab) {
                    SleuthTab.CHAT -> "Chat"
                    SleuthTab.HISTORY -> "History"
                    SleuthTab.SETTINGS -> "Settings"
                }
                val isSelected = tab == selected
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(MaterialTheme.shapes.extraSmall)
                        .clickable { onSelected(tab) }
                        .padding(vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else tokens.textMuted,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    )
                    Box(
                        modifier = Modifier
                            .padding(top = 6.dp)
                            .height(2.dp)
                            .widthIn(min = 24.dp)
                            .background(
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                            ),
                    )
                }
            }
        }
        HorizontalDivider(color = tokens.border)
    }
}

@Composable
private fun ChatTab(
    state: SleuthUiState,
    onSuggestionTap: (String) -> Unit,
    onConfirm: (turnIndex: Int, blockIndex: Int, approved: Boolean) -> Unit,
    onRowTap: (Int) -> Unit,
    onDownload: (String) -> Unit,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(state.turns.size) {
        if (state.turns.isNotEmpty()) listState.animateScrollToItem(state.turns.size - 1)
    }
    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(vertical = 10.dp),
    ) {
        itemsIndexed(state.turns, key = { i, t -> "${t.createdAtEpochMs}-$i" }) { turnIndex, turn ->
            // New messages fade/slide into place instead of popping in.
            // Keys are stable so animateItem only fires for fresh turns.
            Box(modifier = Modifier.animateItem()) {
                when (turn) {
                    is ChatTurn.UserSaid -> UserMessageBubble(text = turn.text)
                    is ChatTurn.SystemSaid -> SystemMessageBubble(text = turn.text)
                    is ChatTurn.BotTyping -> {
                        if (state.settings.showTypingIndicator) {
                            BotMessageBubble { TypingIndicator() }
                        }
                    }
                    is ChatTurn.BotSaid -> BotMessageBubble {
                    turn.blocks.forEachIndexed { blockIndex, block ->
                        when (block) {
                            is RenderedChatBlock.Text -> TextBlockRenderer(block = block)
                            is RenderedChatBlock.Table -> TableBlockRenderer(
                                block = block,
                                onRowClick = onRowTap,
                            )
                            is RenderedChatBlock.File -> FileBlockRenderer(
                                block = block,
                                onDownload = onDownload,
                            )
                            is RenderedChatBlock.Suggestions -> SuggestionsRow(
                                block = block,
                                onSuggestionTap = onSuggestionTap,
                            )
                            is RenderedChatBlock.Confirm -> ConfirmBlockRenderer(
                                block = block,
                                onConfirm = { approved -> onConfirm(turnIndex, blockIndex, approved) },
                            )
                            is RenderedChatBlock.Unknown -> Text(
                                text = "(unsupported block)",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                    }
                }
            }
        }
    }
}

@Composable
private fun InputRow(
    value: String,
    onChange: (String) -> Unit,
    onSend: () -> Unit,
) {
    val gradient = LocalAccentGradient.current
    val tokens = LocalBrandTokens.current
    HorizontalDivider(color = tokens.border)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text("Ask Sleuth…", color = tokens.textFaint) },
            shape = BhInputShape,
            singleLine = false,
            maxLines = 4,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = tokens.border,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        )
        Box(
            modifier = Modifier
                .size(width = 55.dp, height = 38.dp)
                .clip(BhInputShape)
                .background(brush = gradient, shape = BhInputShape)
                .clickable(enabled = value.isNotBlank()) { onSend() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Send,
                contentDescription = "Send",
                tint = Color.White,
            )
        }
    }
}
