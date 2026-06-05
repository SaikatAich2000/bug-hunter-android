package com.bughunter.feature.dashboard

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.bughunter.core.domain.usecase.ToggleKpiFilterUseCase
import com.bughunter.core.ui.components.BhKpiTile
import com.bughunter.core.ui.theme.LocalAccentGradient
import com.bughunter.core.ui.theme.LocalBrandTokens

internal data class KpiTileModel(
    val tile: ToggleKpiFilterUseCase.KpiTile,
    val label: String,
    val value: String,
)

@Composable
internal fun KpiStripComposable(
    tiles: List<KpiTileModel>,
    activeTile: ToggleKpiFilterUseCase.KpiTile?,
    onTileClick: (ToggleKpiFilterUseCase.KpiTile) -> Unit,
    modifier: Modifier = Modifier,
) {
    val gradients = tileGradients()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        tiles.forEach { model ->
            BhKpiTile(
                label = model.label,
                value = model.value,
                selected = activeTile == model.tile,
                onClick = { onTileClick(model.tile) },
                numberGradient = gradients[model.tile],
                modifier = Modifier.widthIn(min = 112.dp),
            )
        }
    }
}

@Composable
private fun tileGradients(): Map<ToggleKpiFilterUseCase.KpiTile, Brush> {
    val tokens = LocalBrandTokens.current
    val accent = LocalAccentGradient.current
    return mapOf(
        ToggleKpiFilterUseCase.KpiTile.TOTAL to accent,
        ToggleKpiFilterUseCase.KpiTile.OPEN to Brush.linearGradient(
            0f to tokens.statusColor("new"),
            1f to tokens.statusColor("in progress"),
            start = Offset.Zero,
            end = Offset.Infinite,
        ),
        ToggleKpiFilterUseCase.KpiTile.RESOLVED to Brush.linearGradient(
            0f to tokens.statusColor("resolved"),
            1f to Color(0xFF34D399),
            start = Offset.Zero,
            end = Offset.Infinite,
        ),
        ToggleKpiFilterUseCase.KpiTile.CLOSED to Brush.linearGradient(
            0f to tokens.statusColor("closed"),
            1f to Color(0xFFCBD5E1),
            start = Offset.Zero,
            end = Offset.Infinite,
        ),
        ToggleKpiFilterUseCase.KpiTile.RESOLVE_LATER to Brush.linearGradient(
            0f to tokens.statusColor("resolve later"),
            1f to Color(0xFFFBBF24),
            start = Offset.Zero,
            end = Offset.Infinite,
        ),
    )
}

internal fun buildKpiTiles(stats: StatsView): List<KpiTileModel> = listOf(
    KpiTileModel(ToggleKpiFilterUseCase.KpiTile.TOTAL, "Total", stats.totals.total.toString()),
    KpiTileModel(ToggleKpiFilterUseCase.KpiTile.OPEN, "Open", stats.totals.open.toString()),
    KpiTileModel(ToggleKpiFilterUseCase.KpiTile.RESOLVED, "Resolved", stats.totals.resolved.toString()),
    KpiTileModel(ToggleKpiFilterUseCase.KpiTile.CLOSED, "Closed", stats.totals.closed.toString()),
    KpiTileModel(ToggleKpiFilterUseCase.KpiTile.RESOLVE_LATER, "Resolve Later", stats.totals.resolveLater.toString()),
)
