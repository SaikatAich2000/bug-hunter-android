package com.bughunter.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
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

enum class BhBadgeKind {
    STATUS_NEW,
    STATUS_OPEN,
    STATUS_REOPENED,
    STATUS_INPROGRESS,
    STATUS_RESOLVED,
    STATUS_CLOSED,
    PRIORITY_LOW,
    PRIORITY_MED,
    PRIORITY_HIGH,
    PRIORITY_CRITICAL,
    ENV_DEV,
    ENV_STAGING,
    ENV_PROD,
    NEUTRAL,
}

@Composable
fun BhBadge(
    label: String,
    kind: BhBadgeKind,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalBrandTokens.current
    val color: Color = when (kind) {
        BhBadgeKind.STATUS_NEW -> tokens.statusColor("new")
        BhBadgeKind.STATUS_OPEN -> tokens.statusColor("new")
        BhBadgeKind.STATUS_REOPENED -> tokens.statusColor("reopened")
        BhBadgeKind.STATUS_INPROGRESS -> tokens.statusColor("in progress")
        BhBadgeKind.STATUS_RESOLVED -> tokens.statusColor("resolved")
        BhBadgeKind.STATUS_CLOSED -> tokens.statusColor("closed")
        BhBadgeKind.PRIORITY_LOW -> tokens.priorityColor("low")
        BhBadgeKind.PRIORITY_MED -> tokens.priorityColor("medium")
        BhBadgeKind.PRIORITY_HIGH -> tokens.priorityColor("high")
        BhBadgeKind.PRIORITY_CRITICAL -> tokens.priorityColor("critical")
        BhBadgeKind.ENV_DEV -> tokens.envColor("dev")
        BhBadgeKind.ENV_STAGING -> tokens.envColor("uat")
        BhBadgeKind.ENV_PROD -> tokens.envColor("prod")
        BhBadgeKind.NEUTRAL -> tokens.textMuted
    }
    val fillAlpha = if (kind == BhBadgeKind.PRIORITY_CRITICAL) 0.16f else 0.10f
    val borderAlpha = if (kind == BhBadgeKind.ENV_PROD) 0.45f else 1.0f
    Box(
        modifier = modifier
            .clip(BhPillShape)
            .background(color.copy(alpha = fillAlpha), BhPillShape)
            .border(BorderStroke(1.dp, color.copy(alpha = borderAlpha)), BhPillShape)
            .padding(horizontal = 10.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
        )
    }
}
