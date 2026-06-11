package com.bughunter.core.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
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
    // Border colour eases between states instead of snapping; the tile
    // itself gives a small spring pop when it becomes selected.
    val borderColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else tokens.border,
        animationSpec = tween(durationMillis = 200),
        label = "kpiBorderColor",
    )
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.03f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "kpiSelectScale",
    )
    // Numeric KPI values count up/down to their new value when the
    // underlying stats change; non-numeric values render as-is.
    val numericValue = value.toIntOrNull()
    val animatedNumber by animateIntAsState(
        targetValue = numericValue ?: 0,
        animationSpec = tween(durationMillis = 600),
        label = "kpiCounter",
    )
    val displayValue = if (numericValue != null) animatedNumber.toString() else value
    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
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
                text = displayValue,
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
