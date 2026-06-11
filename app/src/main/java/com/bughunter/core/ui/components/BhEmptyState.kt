package com.bughunter.core.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bughunter.core.ui.theme.LocalBrandTokens

@Composable
fun BhEmptyState(
    title: String,
    modifier: Modifier = Modifier,
    helper: String? = null,
    icon: ImageVector? = null,
) {
    val tokens = LocalBrandTokens.current
    val borderColor = tokens.border
    Column(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                val stroke = Stroke(
                    width = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f),
                )
                drawRoundRect(
                    color = borderColor,
                    size = Size(size.width, size.height),
                    cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx()),
                    style = stroke,
                )
            }
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (icon != null) {
            // Slow breathing animation — keeps the empty state feeling
            // alive without demanding attention.
            val transition = rememberInfiniteTransition(label = "emptyBreath")
            val breath by transition.animateFloat(
                initialValue = 1f,
                targetValue = 1.08f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 1800),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "emptyBreathScale",
            )
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tokens.textMuted,
                modifier = Modifier
                    .size(36.dp)
                    .graphicsLayer {
                        scaleX = breath
                        scaleY = breath
                    },
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        if (helper != null) {
            Text(
                text = helper,
                style = MaterialTheme.typography.bodyMedium,
                color = tokens.textMuted,
                textAlign = TextAlign.Center,
            )
        }
    }
}
