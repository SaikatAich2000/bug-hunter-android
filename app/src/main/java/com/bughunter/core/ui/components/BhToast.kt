package com.bughunter.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.bughunter.core.ui.theme.BhPillShape
import com.bughunter.core.ui.theme.LocalBrandTokens

enum class BhToastKind { INFO, SUCCESS, ERROR }

@Composable
fun BhToast(
    message: String,
    kind: BhToastKind,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalBrandTokens.current
    val (bg: Color, border: Color, fg: Color) = when (kind) {
        BhToastKind.SUCCESS -> Triple(
            tokens.statusColor("resolved").copy(alpha = 0.14f),
            tokens.statusColor("resolved"),
            tokens.statusColor("resolved"),
        )
        BhToastKind.ERROR -> Triple(
            MaterialTheme.colorScheme.error.copy(alpha = 0.14f),
            MaterialTheme.colorScheme.error,
            MaterialTheme.colorScheme.error,
        )
        BhToastKind.INFO -> Triple(
            MaterialTheme.colorScheme.surface,
            tokens.borderStrong,
            MaterialTheme.colorScheme.onSurface,
        )
    }
    Row(
        modifier = modifier
            .clip(BhPillShape)
            .background(bg, BhPillShape)
            .border(BorderStroke(1.dp, border), BhPillShape)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = fg,
        )
    }
}
