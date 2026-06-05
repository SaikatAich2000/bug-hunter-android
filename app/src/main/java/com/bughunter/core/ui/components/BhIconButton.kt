package com.bughunter.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.bughunter.core.ui.theme.BhIconBtnShape
import com.bughunter.core.ui.theme.LocalBrandTokens

@Composable
fun BhIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    danger: Boolean = false,
) {
    val tokens = LocalBrandTokens.current
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val pressed by interactionSource.collectIsPressedAsState()
    val active = hovered || pressed
    val bg: Color = when {
        !enabled -> Color.Transparent
        active && danger -> MaterialTheme.colorScheme.error
        active -> MaterialTheme.colorScheme.surfaceVariant
        else -> Color.Transparent
    }
    val fg: Color = when {
        !enabled -> tokens.textFaint
        active && danger -> Color.White
        active -> MaterialTheme.colorScheme.onSurface
        else -> tokens.textMuted
    }
    Box(
        modifier = modifier
            .size(30.dp)
            .background(color = bg, shape = BhIconBtnShape)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = fg,
            modifier = Modifier.size(18.dp),
        )
    }
}
