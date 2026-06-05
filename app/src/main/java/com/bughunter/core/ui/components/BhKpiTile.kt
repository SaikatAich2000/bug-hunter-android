package com.bughunter.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.border
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bughunter.core.ui.theme.BhKpiShape
import com.bughunter.core.ui.theme.LocalAccentGradient
import com.bughunter.core.ui.theme.LocalBrandTokens

@Composable
fun BhKpiTile(
    label: String,
    value: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    numberGradient: Brush? = null,
) {
    val tokens = LocalBrandTokens.current
    val gradient = numberGradient ?: LocalAccentGradient.current
    val borderColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        tokens.border
    }
    Box(
        modifier = modifier
            .clip(BhKpiShape)
            .background(brush = tokens.kpiSurfaceGradient, shape = BhKpiShape)
            .border(BorderStroke(if (selected) 2.dp else 1.dp, borderColor), BhKpiShape)
            .clickable { onClick() }
            .padding(horizontal = 18.dp, vertical = 16.dp),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            BhGradientText(
                text = value,
                brush = gradient,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = tokens.textMuted,
                ),
            )
        }
    }
}
