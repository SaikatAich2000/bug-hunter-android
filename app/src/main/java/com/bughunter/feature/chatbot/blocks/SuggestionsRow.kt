package com.bughunter.feature.chatbot.blocks

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bughunter.core.ui.components.BhFilterChip
import com.bughunter.feature.chatbot.RenderedChatBlock

@Composable
internal fun SuggestionsRow(
    block: RenderedChatBlock.Suggestions,
    onSuggestionTap: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        block.items.forEach { item ->
            BhFilterChip(
                label = item,
                selected = false,
                onClick = { onSuggestionTap(item) },
            )
        }
    }
}
