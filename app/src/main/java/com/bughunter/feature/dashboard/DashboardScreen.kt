package com.bughunter.feature.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bughunter.core.domain.usecase.ToggleKpiFilterUseCase
import com.bughunter.core.network.DomainError
import com.bughunter.core.network.dto.BugOut
import com.bughunter.core.ui.components.BhBadge
import com.bughunter.core.ui.components.BhBadgeKind
import com.bughunter.core.ui.components.BhCard
import com.bughunter.core.ui.components.BhEmptyState
import com.bughunter.core.ui.components.BhSectionHeader
import com.bughunter.core.ui.theme.LocalBrandTokens
import com.bughunter.core.ui.util.UiState

@Composable
internal fun DashboardScreen(
    onBugClick: (Int) -> Unit,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    DashboardContent(
        state = state,
        onTabSelect = viewModel::onTabChange,
        onKpiClick = viewModel::onKpiToggle,
        onBugClick = onBugClick,
        onRetry = viewModel::refresh,
    )
}

@Composable
internal fun DashboardScreenTestHarness(
    state: UiState<DashboardScreenModel>,
    onTabSelect: (DashboardTypeTab) -> Unit = {},
    onKpiClick: (ToggleKpiFilterUseCase.KpiTile) -> Unit = {},
    onBugClick: (Int) -> Unit = {},
    onRetry: () -> Unit = {},
) {
    DashboardContent(
        state = state,
        onTabSelect = onTabSelect,
        onKpiClick = onKpiClick,
        onBugClick = onBugClick,
        onRetry = onRetry,
    )
}

@Composable
private fun DashboardContent(
    state: UiState<DashboardScreenModel>,
    onTabSelect: (DashboardTypeTab) -> Unit,
    onKpiClick: (ToggleKpiFilterUseCase.KpiTile) -> Unit,
    onBugClick: (Int) -> Unit,
    onRetry: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        when (state) {
            is UiState.Loading -> LoadingState()
            is UiState.Error -> ErrorState(state.error, onRetry)
            is UiState.Empty -> BhEmptyState(
                title = "No data yet",
                helper = "Items you create will appear here.",
                modifier = Modifier.padding(24.dp),
            )
            is UiState.Success -> DashboardSuccess(
                model = state.data,
                onTabSelect = onTabSelect,
                onKpiClick = onKpiClick,
                onBugClick = onBugClick,
            )
        }
    }
}

@Composable
private fun DashboardSuccess(
    model: DashboardScreenModel,
    onTabSelect: (DashboardTypeTab) -> Unit,
    onKpiClick: (ToggleKpiFilterUseCase.KpiTile) -> Unit,
    onBugClick: (Int) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item("kpi") {
            KpiStripComposable(
                tiles = buildKpiTiles(model.stats),
                activeTile = model.activeTile,
                onTileClick = onKpiClick,
            )
        }
        item("tabs") {
            TypeTabsComposable(
                tabs = buildTypeTabs(model.stats),
                selected = model.tab,
                onSelect = onTabSelect,
            )
        }
        item("recent_header") {
            BhSectionHeader(text = "Recent items")
        }
        if (model.recentBugs.isEmpty()) {
            item("recent_empty") {
                BhEmptyState(title = "No items match your filters")
            }
        } else {
            items(items = model.recentBugs, key = { it.id }) { bug ->
                BugSnippet(bug = bug, onClick = { onBugClick(bug.id) })
            }
        }
        item("activity_header") {
            BhSectionHeader(text = "Recent activity")
        }
        item("activity_helper") {
            BhCard {
                Text(
                    text = "Activity feed will appear here as your team updates items.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = LocalBrandTokens.current.textMuted,
                )
            }
        }
    }
}

@Composable
private fun BugSnippet(bug: BugOut, onClick: () -> Unit) {
    BhCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = bug.projectKey?.let { "$it-${bug.id}" } ?: "#${bug.id}",
                    style = MaterialTheme.typography.labelSmall,
                    color = LocalBrandTokens.current.textMuted,
                )
                Text(
                    text = bug.title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                BhBadge(label = bug.status, kind = statusKind(bug.status))
                BhBadge(label = bug.priority, kind = priorityKind(bug.priority))
                BhBadge(label = bug.environment, kind = envKind(bug.environment))
            }
        }
    }
}

@Composable
private fun LoadingState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun ErrorState(error: DomainError, onRetry: () -> Unit) {
    val tokens = LocalBrandTokens.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Couldn't load the dashboard",
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
                .padding(top = 8.dp)
                .background(MaterialTheme.colorScheme.surface)
                .clickable { onRetry() }
                .padding(horizontal = 18.dp, vertical = 10.dp),
        ) {
            Text(
                text = "Retry",
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

private fun statusKind(status: String): BhBadgeKind = when (status.lowercase()) {
    "new" -> BhBadgeKind.STATUS_NEW
    "in progress" -> BhBadgeKind.STATUS_INPROGRESS
    "resolved" -> BhBadgeKind.STATUS_RESOLVED
    "closed" -> BhBadgeKind.STATUS_CLOSED
    "reopened" -> BhBadgeKind.STATUS_REOPENED
    else -> BhBadgeKind.STATUS_NEW
}

private fun priorityKind(priority: String): BhBadgeKind = when (priority.lowercase()) {
    "low" -> BhBadgeKind.PRIORITY_LOW
    "medium" -> BhBadgeKind.PRIORITY_MED
    "high" -> BhBadgeKind.PRIORITY_HIGH
    "critical" -> BhBadgeKind.PRIORITY_CRITICAL
    else -> BhBadgeKind.PRIORITY_MED
}

private fun envKind(env: String): BhBadgeKind = when (env.uppercase()) {
    "DEV" -> BhBadgeKind.ENV_DEV
    "UAT" -> BhBadgeKind.ENV_STAGING
    "PROD" -> BhBadgeKind.ENV_PROD
    else -> BhBadgeKind.ENV_DEV
}
