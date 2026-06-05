package com.bughunter.feature.chatbot.messages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bughunter.core.ui.theme.BhPillShape
import com.bughunter.core.ui.theme.LocalBrandTokens

@Composable
internal fun SystemMessageBubble(
    text: String,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalBrandTokens.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = tokens.textMuted,
            modifier = Modifier
                .background(color = MaterialTheme.colorScheme.surfaceVariant, shape = BhPillShape)
                .padding(horizontal = 14.dp, vertical = 6.dp),
        )
    }
}
