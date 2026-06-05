package com.bughunter.feature.analytics.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

internal data class HorizontalBarRow(
    val label: String,
    val value: Double,
    val swatchColor: Color? = null,
    val secondary: String? = null,
)

@Composable
internal fun BhHorizontalBars(
    rows: List<HorizontalBarRow>,
    modifier: Modifier = Modifier,
    palette: ChartPalette = rememberChartPalette(),
) {
    val maxValue = rows.maxOfOrNull { it.value }?.coerceAtLeast(1.0) ?: 1.0
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        rows.forEachIndexed { index, row ->
            HorizontalBarRowItem(
                row = row,
                ratio = (row.value / maxValue).toFloat(),
                color = row.swatchColor ?: palette.colorAt(index),
                palette = palette,
            )
        }
    }
}

@Composable
private fun HorizontalBarRowItem(
    row: HorizontalBarRow,
    ratio: Float,
    color: Color,
    palette: ChartPalette,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = row.label,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium,
                ),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = row.value.toInt().toString(),
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
                color = palette.text,
                modifier = Modifier.widthIn(min = 28.dp),
            )
        }
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
                .height(10.dp),
        ) {
            drawRoundRect(
                color = palette.grid,
                topLeft = Offset(0f, 0f),
                size = Size(size.width, size.height),
                cornerRadius = CornerRadius(6f, 6f),
            )
            val width = size.width * ratio.coerceIn(0f, 1f)
            if (width > 0f) {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(0f, 0f),
                    size = Size(width, size.height),
                    cornerRadius = CornerRadius(6f, 6f),
                )
            }
        }
        if (row.secondary != null) {
            Text(
                text = row.secondary,
                style = MaterialTheme.typography.labelSmall.copy(color = palette.text),
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

