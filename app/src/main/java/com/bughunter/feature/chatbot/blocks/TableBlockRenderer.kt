package com.bughunter.feature.chatbot.blocks

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bughunter.core.ui.theme.BhMonoFontFamily
import com.bughunter.core.ui.theme.LocalBrandTokens
import com.bughunter.feature.chatbot.RenderedChatBlock

@Composable
internal fun TableBlockRenderer(
    block: RenderedChatBlock.Table,
    onRowClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalBrandTokens.current
    val shape = RoundedCornerShape(10.dp)
    val scrollState = rememberScrollState()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(BorderStroke(1.dp, tokens.border), shape),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            block.columns.forEach { col ->
                Text(
                    text = col.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = BhMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                    ),
                    color = tokens.textMuted,
                    modifier = Modifier.widthIn(min = 80.dp),
                )
            }
        }
        HorizontalDivider(color = tokens.border)
        block.rows.forEachIndexed { rowIdx, row ->
            val key = block.rowKeys.getOrNull(rowIdx)
            val rowModifier = if (key != null) {
                Modifier
                    .fillMaxWidth()
                    .clickable { onRowClick(key) }
            } else {
                Modifier.fillMaxWidth()
            }
            Row(
                modifier = rowModifier
                    .horizontalScroll(scrollState)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                row.forEach { cell ->
                    Text(
                        text = cell,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.widthIn(min = 80.dp),
                    )
                }
            }
            if (rowIdx != block.rows.lastIndex) {
                HorizontalDivider(color = tokens.borderSoft)
            }
        }
        if (block.rows.isEmpty()) {
            Box(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "(no rows)",
                    style = MaterialTheme.typography.bodySmall,
                    color = tokens.textFaint,
                )
            }
        }
    }
}
