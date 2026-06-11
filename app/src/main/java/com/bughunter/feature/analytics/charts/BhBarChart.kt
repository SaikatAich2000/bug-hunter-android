package com.bughunter.feature.analytics.charts

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
internal fun BhBarChart(
    data: List<Pair<String, Double>>,
    modifier: Modifier = Modifier,
    height: Int = 180,
    palette: ChartPalette = rememberChartPalette(),
    colorOverrides: Map<String, Color> = emptyMap(),
) {
    // Bars grow from the baseline when the data lands (and re-run when
    // it changes) — progress scales every bar height in the draw pass.
    val growth = remember(data) { Animatable(0f) }
    LaunchedEffect(data) {
        growth.animateTo(1f, animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing))
    }
    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(height.dp),
        ) {
            if (data.isEmpty()) return@Canvas
            val maxValue = data.maxOf { it.second }.coerceAtLeast(1.0)
            val padTop = 12f
            val padBottom = 8f
            val plotH = size.height - padTop - padBottom
            val barCount = data.size
            val groupWidth = size.width / barCount
            val barWidth = (groupWidth * 0.6f).coerceAtMost(48.dp.toPx())
            val gridLines = 4
            repeat(gridLines + 1) { line ->
                val y = padTop + line.toFloat() / gridLines.toFloat() * plotH
                drawLine(
                    color = palette.grid,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1f,
                )
            }
            data.forEachIndexed { index, (label, value) ->
                val xCenter = groupWidth * index + groupWidth / 2f
                // Reading growth.value inside the draw scope means only the
                // draw phase re-runs per animation frame, not composition.
                val barHeight = (value / maxValue).toFloat() * plotH * growth.value
                val color = colorOverrides[label] ?: palette.colorAt(index)
                drawRoundRect(
                    color = color,
                    topLeft = Offset(xCenter - barWidth / 2f, padTop + plotH - barHeight),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(4f, 4f),
                )
            }
        }
        BarLabels(labels = data.map { it.first }, palette = palette)
    }
}

@Composable
private fun BarLabels(labels: List<String>, palette: ChartPalette) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        labels.forEach { label ->
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = palette.text,
                    fontWeight = FontWeight.Medium,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 2.dp),
            )
        }
    }
}
