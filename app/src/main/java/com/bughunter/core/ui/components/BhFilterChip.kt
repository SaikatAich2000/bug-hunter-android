package com.bughunter.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import com.bughunter.core.ui.theme.LocalAccentGradient
import com.bughunter.core.ui.theme.LocalBrandTokens

@Composable
fun BhFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val tokens = LocalBrandTokens.current
    val gradient = LocalAccentGradient.current
    val baseModifier = modifier
        .clip(BhPillShape)
        .clickable(enabled = enabled) { onClick() }
    val styledBox = if (selected) {
        baseModifier
            .background(brush = gradient, shape = BhPillShape)
            .border(BorderStroke(1.dp, Color.Transparent), BhPillShape)
    } else {
        baseModifier
            .background(MaterialTheme.colorScheme.surface, BhPillShape)
            .border(BorderStroke(1.dp, tokens.border), BhPillShape)
    }
    Box(
        modifier = styledBox.padding(horizontal = 11.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
        )
    }
}
