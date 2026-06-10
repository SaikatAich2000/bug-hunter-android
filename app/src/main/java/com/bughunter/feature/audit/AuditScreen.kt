package com.bughunter.feature.audit

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bughunter.core.network.dto.ActivityOut
import com.bughunter.core.ui.components.BhCard
import com.bughunter.core.ui.components.BhEmptyState
import com.bughunter.core.ui.components.BhErrorBanner
import com.bughunter.core.ui.components.BhGhostButton
import com.bughunter.core.ui.components.BhPrimaryButton
import com.bughunter.core.ui.components.BhTextField
import androidx.compose.runtime.LaunchedEffect
import com.bughunter.core.ui.components.BhToast
import com.bughunter.core.ui.components.BhToastKind
import com.bughunter.core.ui.theme.BhPillShape
import kotlinx.coroutines.delay
import com.bughunter.core.ui.theme.LocalBrandTokens
import com.bughunter.core.ui.util.UiState
import com.bughunter.core.ui.util.formatRelative

@Composable
internal fun AuditScreen(
    viewModel: AuditViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    AuditContent(
        state = state,
        onEntityTypeChange = viewModel::onEntityTypeChange,
        onActorIdChange = viewModel::onActorIdChange,
        onQueryChange = viewModel::onQueryChange,
        onApply = viewModel::applyFilters,
        onClear = viewModel::clearFilters,
        onRefresh = viewModel::refresh,
        onExport = viewModel::exportCsv,
        onDismissExport = viewModel::dismissExportToast,
    )
}

@Composable
internal fun AuditTestHarness(
    state: AuditUiState,
    onEntityTypeChange: (String?) -> Unit = {},
    onActorIdChange: (String) -> Unit = {},
    onQueryChange: (String) -> Unit = {},
    onApply: () -> Unit = {},
    onClear: () -> Unit = {},
    onRefresh: () -> Unit = {},
    onExport: () -> Unit = {},
) {
    AuditContent(
        state = state,
        onEntityTypeChange = onEntityTypeChange,
        onActorIdChange = onActorIdChange,
        onQueryChange = onQueryChange,
        onApply = onApply,
        onClear = onClear,
        onRefresh = onRefresh,
        onExport = onExport,
        onDismissExport = {},
    )
}

@Composable
private fun AuditContent(
    state: AuditUiState,
    onEntityTypeChange: (String?) -> Unit,
    onActorIdChange: (String) -> Unit,
    onQueryChange: (String) -> Unit,
    onApply: () -> Unit,
    onClear: () -> Unit,
    onRefresh: () -> Unit,
    onExport: () -> Unit,
    onDismissExport: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Shield,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "Audit trail",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                BhPrimaryButton(
                    text = "Export CSV",
                    onClick = onExport,
                    loading = state.isExporting,
                )
            }
            BhErrorBanner(error = state.exportError)
            BhCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    BhTextField(
                        value = state.filters.entityType.orEmpty(),
                        onValueChange = { onEntityTypeChange(it.ifBlank { null }) },
                        label = "Entity type",
                        placeholder = "bug, user, project, event",
                    )
                    BhTextField(
                        value = state.filters.actorUserIdText,
                        onValueChange = onActorIdChange,
                        label = "Actor user id",
                        placeholder = "e.g. 7",
                    )
                    BhTextField(
                        value = state.filters.query,
                        onValueChange = onQueryChange,
                        label = "Search",
                        placeholder = "Match IDs, actions, types",
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        BhGhostButton(text = "Clear", onClick = onClear)
                        BhGhostButton(text = "Refresh", onClick = onRefresh)
                        BhPrimaryButton(text = "Apply", onClick = onApply)
                    }
                }
            }
            when (val list = state.list) {
                UiState.Loading -> Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) { CircularProgressIndicator() }
                UiState.Empty -> BhEmptyState(
                    title = "No audit events match",
                    icon = Icons.Outlined.Shield,
                )
                is UiState.Error -> Text(
                    text = "Couldn't load audit log.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
                is UiState.Success -> AuditList(items = list.data)
            }
        }
        if (state.exportedFileName != null) {
            LaunchedEffect(state.exportedFileName) {
                delay(3500)
                onDismissExport()
            }
            BhToast(
                message = "Saved ${state.exportedFileName} to Downloads/BugHunter",
                kind = BhToastKind.SUCCESS,
                modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
            )
        }
    }
}

@Composable
private fun AuditList(items: List<ActivityOut>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(items = items, key = { it.id }) { item -> AuditRow(item = item) }
    }
}

@Composable
private fun AuditRow(item: ActivityOut) {
    val tokens = LocalBrandTokens.current
    BhCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(tokens.accentSoft, shape = BhPillShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Download,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = item.actorName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                    )
                    AuditPill(text = item.action)
                    AuditPill(text = item.entityType, muted = true)
                }
                if (item.detail.isNotBlank()) {
                    Text(
                        text = item.detail,
                        style = MaterialTheme.typography.bodySmall,
                        color = tokens.textMuted,
                    )
                }
                Text(
                    text = item.createdAt.formatRelative(),
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                    color = tokens.textFaint,
                )
            }
        }
    }
}

@Composable
private fun AuditPill(text: String, muted: Boolean = false) {
    val tokens = LocalBrandTokens.current
    val fg = if (muted) tokens.textMuted else MaterialTheme.colorScheme.primary
    val bg = if (muted) tokens.borderSoft else tokens.accentSoft
    Box(
        modifier = Modifier
            .background(bg, BhPillShape)
            .border(width = 1.dp, color = fg.copy(alpha = 0.3f), shape = BhPillShape)
            .padding(horizontal = 8.dp, vertical = 2.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
            color = fg,
        )
    }
}
