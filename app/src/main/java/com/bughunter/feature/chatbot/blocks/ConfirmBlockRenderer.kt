package com.bughunter.feature.chatbot.blocks

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bughunter.core.ui.components.BhPrimaryButton
import com.bughunter.core.ui.components.BhSecondaryButton
import com.bughunter.core.ui.theme.LocalBrandTokens
import com.bughunter.feature.chatbot.RenderedChatBlock

@Composable
internal fun ConfirmBlockRenderer(
    block: RenderedChatBlock.Confirm,
    onConfirm: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalBrandTokens.current
    val shape = RoundedCornerShape(10.dp)
    val resolved = block.resolved != RenderedChatBlock.Confirm.Resolution.PENDING
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, shape)
            .border(BorderStroke(1.dp, tokens.border), shape)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = block.prompt,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (resolved) {
            val resolutionText = when (block.resolved) {
                RenderedChatBlock.Confirm.Resolution.APPROVED -> "Approved"
                RenderedChatBlock.Confirm.Resolution.REJECTED -> "Cancelled"
                RenderedChatBlock.Confirm.Resolution.PENDING -> ""
            }
            Text(
                text = resolutionText,
                style = MaterialTheme.typography.labelSmall,
                color = tokens.textMuted,
            )
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BhPrimaryButton(
                    text = block.confirmLabel,
                    onClick = { onConfirm(true) },
                )
                BhSecondaryButton(
                    text = block.cancelLabel,
                    onClick = { onConfirm(false) },
                )
            }
        }
    }
}
