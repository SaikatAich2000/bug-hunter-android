package com.bughunter.feature.analytics.charts

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
internal fun BhLineChart(
    data: List<Pair<String, Double>>,
    modifier: Modifier = Modifier,
    height: Int = 180,
    palette: ChartPalette = rememberChartPalette(),
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(height.dp),
        ) {
            if (data.isEmpty()) return@Canvas
            val maxValue = (data.maxOf { it.second }).coerceAtLeast(1.0)
            val padLeft = 28f
            val padRight = 12f
            val padTop = 14f
            val padBottom = 18f
            val plotW = size.width - padLeft - padRight
            val plotH = size.height - padTop - padBottom
            drawGridLines(palette = palette, padLeft = padLeft, padRight = padRight, padTop = padTop, plotH = plotH)
            val points = data.mapIndexed { index, point ->
                val xRatio = if (data.size == 1) 0.5f else index.toFloat() / (data.size - 1).toFloat()
                val x = padLeft + xRatio * plotW
                val y = padTop + plotH - ((point.second / maxValue).toFloat() * plotH)
                Offset(x, y)
            }
            val areaPath = Path().apply {
                moveTo(points.first().x, padTop + plotH)
                points.forEach { lineTo(it.x, it.y) }
                lineTo(points.last().x, padTop + plotH)
                close()
            }
            drawPath(
                path = areaPath,
                brush = Brush.verticalGradient(
                    0f to palette.primary.copy(alpha = 0.35f),
                    1f to palette.primary.copy(alpha = 0f),
                    startY = padTop,
                    endY = padTop + plotH,
                ),
            )
            val linePath = Path().apply {
                moveTo(points.first().x, points.first().y)
                points.drop(1).forEach { lineTo(it.x, it.y) }
            }
            drawPath(
                path = linePath,
                color = palette.primary,
                style = Stroke(width = 2.5f),
            )
            points.forEach { point ->
                drawCircle(color = palette.primary, radius = 3.5f, center = point)
            }
        }
        if (data.isNotEmpty()) {
            AxisLabels(labels = data.map { it.first }, palette = palette)
        }
    }
}

@Composable
private fun AxisLabels(labels: List<String>, palette: ChartPalette) {
    val displayed = sampleLabels(labels)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 28.dp, end = 12.dp, top = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        displayed.forEach { label ->
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = palette.text,
                    fontWeight = FontWeight.Medium,
                ),
            )
        }
    }
}

private fun sampleLabels(labels: List<String>): List<String> {
    if (labels.size <= 5) return labels
    val step = labels.size / 4
    return (0 until labels.size step step).map { labels[it] }.take(5)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawGridLines(
    palette: ChartPalette,
    padLeft: Float,
    padRight: Float,
    padTop: Float,
    plotH: Float,
) {
    val gridLines = 4
    val plotW = size.width - padLeft - padRight
    repeat(gridLines + 1) { line ->
        val y = padTop + line.toFloat() / gridLines.toFloat() * plotH
        drawLine(
            color = palette.grid,
            start = Offset(padLeft, y),
            end = Offset(padLeft + plotW, y),
            strokeWidth = 1f,
        )
    }
    drawLine(
        color = palette.axis,
        start = Offset(padLeft, padTop),
        end = Offset(padLeft, padTop + plotH),
        strokeWidth = 1f,
    )
}

