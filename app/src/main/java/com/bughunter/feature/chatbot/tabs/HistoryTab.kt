package com.bughunter.feature.chatbot.tabs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.bughunter.core.ui.components.BhEmptyState
import com.bughunter.core.ui.theme.LocalBrandTokens
import com.bughunter.core.ui.util.formatRelative
import com.bughunter.feature.chatbot.SleuthHistoryEntry
import java.time.Instant

@Composable
internal fun HistoryTab(
    entries: List<SleuthHistoryEntry>,
    onEntryClick: (SleuthHistoryEntry) -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalBrandTokens.current
    if (entries.isEmpty()) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BhEmptyState(
                title = "No conversations yet",
                helper = "Start chatting and your sessions will appear here.",
            )
        }
        return
    }
    Column(modifier = modifier.fillMaxSize()) {
        Text(
            text = "Sessions",
            style = MaterialTheme.typography.labelSmall,
            color = tokens.textMuted,
            modifier = Modifier.padding(14.dp, 12.dp, 14.dp, 6.dp),
        )
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(entries, key = { it.id }) { entry ->
                HistoryRow(entry = entry, onClick = { onEntryClick(entry) })
            }
        }
        Text(
            text = "Clear all sessions",
            style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier
                .clickable { onClearAll() }
                .padding(14.dp),
        )
    }
}

@Composable
private fun HistoryRow(
    entry: SleuthHistoryEntry,
    onClick: () -> Unit,
) {
    val tokens = LocalBrandTokens.current
    val shape = RoundedCornerShape(10.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, shape)
            .border(BorderStroke(1.dp, tokens.border), shape)
            .clickable { onClick() }
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = entry.firstPrompt,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
        )
        Text(
            text = "${entry.turnCount} messages · ${Instant.ofEpochMilli(entry.startedAtEpochMs).formatRelative()}",
            style = MaterialTheme.typography.bodySmall,
            color = tokens.textFaint,
        )
    }
}
