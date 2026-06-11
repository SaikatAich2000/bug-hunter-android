package com.bughunter.feature.analytics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bughunter.core.network.DomainError
import com.bughunter.core.ui.components.BhCard
import com.bughunter.core.ui.components.BhEmptyState
import com.bughunter.core.ui.components.BhSectionHeader
import com.bughunter.core.ui.theme.LocalBrandTokens
import com.bughunter.core.ui.util.UiState
import com.bughunter.feature.analytics.charts.BhBarChart
import com.bughunter.feature.analytics.charts.BhHorizontalBars
import com.bughunter.feature.analytics.charts.BhLineChart
import com.bughunter.feature.analytics.charts.ChartPalette
import com.bughunter.feature.analytics.charts.HorizontalBarRow
import com.bughunter.feature.analytics.charts.colorAt
import com.bughunter.feature.analytics.charts.rememberChartPalette
import com.bughunter.feature.dashboard.DashboardTypeTab
import com.bughunter.feature.dashboard.TypeTabsComposable
import com.bughunter.feature.dashboard.buildTypeTabs

@Composable
internal fun AnalyticsScreen(
    viewModel: AnalyticsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    AnalyticsContent(state = state, onTabSelect = viewModel::onTabChange, onRetry = viewModel::refresh)
}

@Composable
internal fun AnalyticsScreenTestHarness(
    state: UiState<AnalyticsScreenModel>,
    onTabSelect: (DashboardTypeTab) -> Unit = {},
    onRetry: () -> Unit = {},
) {
    AnalyticsContent(state = state, onTabSelect = onTabSelect, onRetry = onRetry)
}

@Composable
private fun AnalyticsContent(
    state: UiState<AnalyticsScreenModel>,
    onTabSelect: (DashboardTypeTab) -> Unit,
    onRetry: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        when (state) {
            is UiState.Loading -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
            is UiState.Empty -> BhEmptyState(
                title = "No analytics data",
                helper = "Create items to populate the charts.",
                modifier = Modifier.padding(24.dp),
            )
            is UiState.Error -> ErrorPanel(error = state.error, onRetry = onRetry)
            is UiState.Success -> AnalyticsSuccess(
                model = state.data,
                onTabSelect = onTabSelect,
            )
        }
    }
}

@Composable
private fun AnalyticsSuccess(
    model: AnalyticsScreenModel,
    onTabSelect: (DashboardTypeTab) -> Unit,
) {
    val palette = rememberChartPalette()
    // remember(stats): these projections allocate fresh lists; without
    // memoisation every recomposition rebuilds them and breaks chart
    // skipping further down the tree.
    val stats = model.stats
    val timelineData = remember(stats) { stats.timeline.map { it.label to it.count.toDouble() } }
    val priorityData = remember(stats) { stats.byPriority.map { (k, v) -> k to v.toDouble() } }
    val statusData = remember(stats) { stats.byStatus.take(STATUS_BARS).map { (k, v) -> k to v.toDouble() } }
    val envData = remember(stats) { stats.byEnvironment.map { (k, v) -> k to v.toDouble() } }
    val projectRows = remember(stats) {
        stats.byProject.map {
            HorizontalBarRow(
                label = it.key?.let { key -> "$key  ${it.name}" } ?: it.name,
                value = it.count.toDouble(),
                swatchColor = it.color?.let(::parseHexColor),
            )
        }
    }
    val assigneeRows = remember(stats) {
        stats.byAssignee.map {
            HorizontalBarRow(
                label = it.name,
                value = it.count.toDouble(),
                secondary = it.email,
            )
        }
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item("tabs") {
            TypeTabsComposable(
                tabs = buildTypeTabs(model.stats),
                selected = model.tab,
                onSelect = onTabSelect,
            )
        }
        item("timeline") {
            ChartCard(title = timelineTitle(model.tab)) {
                if (timelineData.isEmpty()) {
                    EmptyChartHint()
                } else {
                    BhLineChart(data = timelineData, palette = palette)
                }
            }
        }
        item("severity") {
            ChartCard(title = "Severity") {
                SeverityDonut(data = priorityData, palette = palette)
            }
        }
        item("by_status") {
            ChartCard(title = "By Status") {
                if (statusData.isEmpty()) EmptyChartHint() else BhBarChart(data = statusData, palette = palette)
            }
        }
        if (model.tab == DashboardTypeTab.ALL || model.tab == DashboardTypeTab.BUGS) {
            item("by_env") {
                ChartCard(title = "By Environment") {
                    if (envData.isEmpty()) EmptyChartHint() else BhBarChart(data = envData, palette = palette)
                }
            }
        }
        item("by_project_header") {
            BhSectionHeader(text = "By Project")
        }
        item("by_project") {
            BhCard {
                if (projectRows.isEmpty()) EmptyChartHint() else BhHorizontalBars(rows = projectRows, palette = palette)
            }
        }
        item("by_assignee_header") {
            BhSectionHeader(text = "Top Assignees")
        }
        item("by_assignee") {
            BhCard {
                if (assigneeRows.isEmpty()) EmptyChartHint() else BhHorizontalBars(rows = assigneeRows, palette = palette)
            }
        }
    }
}

@Composable
private fun ChartCard(title: String, content: @Composable () -> Unit) {
    BhCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
            )
            content()
        }
    }
}

@Composable
private fun EmptyChartHint() {
    Text(
        text = "No data yet.",
        style = MaterialTheme.typography.bodyMedium,
        color = LocalBrandTokens.current.textFaint,
        modifier = Modifier.padding(vertical = 12.dp),
    )
}

@Composable
private fun SeverityDonut(
    data: List<Pair<String, Double>>,
    palette: ChartPalette,
) {
    val total = data.sumOf { it.second }.coerceAtLeast(1.0)
    val tokens = LocalBrandTokens.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Canvas(
            modifier = Modifier
                .size(140.dp),
        ) {
            val stroke = Stroke(width = 22f)
            val pad = 12f
            val topLeft = Offset(pad, pad)
            val arcSize = Size(size.width - pad * 2, size.height - pad * 2)
            var startAngle = -90f
            data.forEachIndexed { index, (label, value) ->
                val sweep = (value / total).toFloat() * 360f
                val color = priorityColor(label, tokens) ?: palette.colorAt(index)
                drawArc(
                    color = color,
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = stroke,
                )
                startAngle += sweep
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            data.forEachIndexed { index, (label, value) ->
                val color = priorityColor(label, tokens) ?: palette.colorAt(index)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(color),
                    )
                    Text(
                        text = "$label  ${value.toInt()}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            if (data.isEmpty()) EmptyChartHint()
        }
    }
}

@Composable
private fun ErrorPanel(error: DomainError, onRetry: () -> Unit) {
    val tokens = LocalBrandTokens.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Couldn't load analytics",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = describe(error),
            style = MaterialTheme.typography.bodyMedium,
            color = tokens.textMuted,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .background(MaterialTheme.colorScheme.surface)
                .clickable { onRetry() },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Tap to retry",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

private fun describe(error: DomainError): String = when (error) {
    DomainError.Unauthorized -> "Your session expired. Please sign in again."
    DomainError.Forbidden -> "You don't have access to this view."
    DomainError.NotFound -> "Stats endpoint not found."
    DomainError.Conflict -> "Conflicting state."
    is DomainError.Validation -> error.message ?: "Validation failed."
    is DomainError.RateLimited -> "Too many requests. Please wait."
    is DomainError.Server -> error.message
    DomainError.Network -> "Network unavailable. Check your connection."
    is DomainError.Unknown -> "Something went wrong."
}

private fun priorityColor(label: String, tokens: com.bughunter.core.ui.theme.BrandTokens): Color? {
    val canonical = label.lowercase()
    if (canonical in setOf("low", "medium", "high", "critical")) {
        return tokens.priorityColor(canonical)
    }
    return null
}

private fun parseHexColor(hex: String): Color? = runCatching {
    val normalised = hex.removePrefix("#")
    val long = normalised.toLong(16)
    val withAlpha = if (normalised.length == 6) 0xFF000000 or long else long
    Color(withAlpha)
}.getOrNull()

private fun timelineTitle(tab: DashboardTypeTab): String = when (tab) {
    DashboardTypeTab.ALL -> "Items over the last 14 days"
    DashboardTypeTab.BUGS -> "Bugs over the last 14 days"
    DashboardTypeTab.REQUIREMENTS -> "Requirements over the last 14 days"
    DashboardTypeTab.TASKS -> "Tasks over the last 14 days"
}

private const val STATUS_BARS: Int = 6
