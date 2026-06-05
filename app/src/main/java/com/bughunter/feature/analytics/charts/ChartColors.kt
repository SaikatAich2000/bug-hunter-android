package com.bughunter.feature.analytics.charts

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.bughunter.core.ui.theme.LocalBrandTokens

@Immutable
internal data class ChartPalette(
    val primary: Color,
    val secondary: Color,
    val grid: Color,
    val axis: Color,
    val text: Color,
    val series: List<Color>,
)

@Composable
internal fun rememberChartPalette(): ChartPalette {
    val tokens = LocalBrandTokens.current
    val scheme = MaterialTheme.colorScheme
    return ChartPalette(
        primary = scheme.primary,
        secondary = scheme.secondary,
        grid = tokens.borderSoft,
        axis = tokens.border,
        text = tokens.textMuted,
        series = listOf(
            scheme.primary,
            scheme.secondary,
            tokens.statusColor("resolved"),
            tokens.statusColor("in progress"),
            tokens.priorityColor("critical"),
            tokens.statusColor("reopened"),
            tokens.priorityColor("high"),
            tokens.envColor("dev"),
        ),
    )
}

internal fun ChartPalette.colorAt(index: Int): Color =
    if (series.isEmpty()) primary else series[index % series.size]
