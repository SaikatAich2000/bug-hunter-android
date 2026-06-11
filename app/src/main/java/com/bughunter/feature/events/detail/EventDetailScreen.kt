package com.bughunter.feature.events.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bughunter.core.network.dto.EventDetailOut
import com.bughunter.core.network.dto.EventItemBrief
import com.bughunter.core.ui.components.BhAssigneeChip
import com.bughunter.core.ui.components.BhBadge
import com.bughunter.core.ui.components.BhBadgeKind
import com.bughunter.core.ui.components.BhCard
import com.bughunter.core.ui.components.BhConfirmDialog
import com.bughunter.core.ui.components.BhEmptyState
import com.bughunter.core.ui.components.BhIconButton
import com.bughunter.core.ui.components.BhSectionHeader
import com.bughunter.core.ui.theme.LocalBrandTokens
import com.bughunter.core.ui.util.UiState
import com.bughunter.feature.events.create.EventFormDialog

@Composable
internal fun EventDetailScreen(
    onBack: () -> Unit,
    onBugClick: (Int) -> Unit,
    viewModel: EventDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    EventDetailContent(
        state = state,
        onBack = onBack,
        onBugClick = onBugClick,
        onEdit = viewModel::openEdit,
        onCloseForm = viewModel::closeForm,
        onDeleteRequest = viewModel::openDeleteConfirm,
        onDeleteDismiss = viewModel::dismissDeleteConfirm,
        onDeleteConfirm = { viewModel.confirmDelete(onBack) },
    )
}

@Composable
internal fun EventDetailTestHarness(
    state: EventDetailUiState,
    onBack: () -> Unit = {},
    onBugClick: (Int) -> Unit = {},
    onEdit: () -> Unit = {},
    onDeleteRequest: () -> Unit = {},
    onDeleteConfirm: () -> Unit = {},
    onDeleteDismiss: () -> Unit = {},
) {
    EventDetailContent(
        state = state,
        onBack = onBack,
        onBugClick = onBugClick,
        onEdit = onEdit,
        onCloseForm = {},
        onDeleteRequest = onDeleteRequest,
        onDeleteDismiss = onDeleteDismiss,
        onDeleteConfirm = onDeleteConfirm,
    )
}

@Composable
private fun EventDetailContent(
    state: EventDetailUiState,
    onBack: () -> Unit,
    onBugClick: (Int) -> Unit,
    onEdit: () -> Unit,
    onCloseForm: () -> Unit,
    onDeleteRequest: () -> Unit,
    onDeleteDismiss: () -> Unit,
    onDeleteConfirm: () -> Unit,
) {
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
            BhIconButton(
                icon = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "Back",
                onClick = onBack,
            )
            Text(
                text = (state.event as? UiState.Success)?.data?.name ?: "Event",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            val event = (state.event as? UiState.Success)?.data
            if (event?.canEdit == true) {
                BhIconButton(
                    icon = Icons.Outlined.Edit,
                    contentDescription = "Edit",
                    onClick = onEdit,
                )
            }
            if (event?.canDelete == true) {
                BhIconButton(
                    icon = Icons.Outlined.Delete,
                    contentDescription = "Delete",
                    onClick = onDeleteRequest,
                    danger = true,
                )
            }
        }
        when (val ev = state.event) {
            UiState.Loading -> Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) { CircularProgressIndicator() }
            UiState.Empty -> BhEmptyState(title = "Not found")
            is UiState.Error -> Text(
                text = "Couldn't load event.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
            is UiState.Success -> EventBody(event = ev.data, onBugClick = onBugClick)
        }
    }
    if (state.isFormOpen) {
        val ev = (state.event as? UiState.Success)?.data
        EventFormDialog(
            eventId = ev?.id,
            onDismiss = onCloseForm,
            onSaved = onCloseForm,
        )
    }
    if (state.isDeleteConfirmOpen) {
        BhConfirmDialog(
            title = "Delete event?",
            message = "This will detach any linked items but won't delete them.",
            confirmLabel = "Delete",
            onConfirm = onDeleteConfirm,
            onDismiss = onDeleteDismiss,
            danger = true,
            loading = state.isDeleting,
        )
    }
}

@Composable
private fun EventBody(
    event: EventDetailOut,
    onBugClick: (Int) -> Unit,
) {
    val tokens = LocalBrandTokens.current
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        BhCard {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!event.scheduledFor.isNullOrBlank()) {
                    Text(
                        text = "Scheduled: ${event.scheduledFor}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                if (event.description.isNotBlank()) {
                    Text(
                        text = event.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                if (event.managers.isNotEmpty()) {
                    Text(
                        text = "Managers",
                        style = MaterialTheme.typography.labelSmall,
                        color = tokens.textMuted,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        event.managers.forEach { manager ->
                            BhAssigneeChip(
                                name = manager.name,
                                userId = manager.id.toString(),
                            )
                        }
                    }
                }
            }
        }
        BhSectionHeader(text = "Items (${event.items.size})")
        if (event.items.isEmpty()) {
            BhEmptyState(
                title = "No items",
                helper = "No items in this event yet — click + Add Task to create one",
                icon = Icons.Outlined.Event,
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(items = event.items, key = { it.id }) { item ->
                    EventItemRow(item = item, onClick = { onBugClick(item.id) })
                }
            }
        }
    }
}

@Composable
private fun EventItemRow(
    item: EventItemBrief,
    onClick: () -> Unit,
) {
    val tokens = LocalBrandTokens.current
    BhCard(onClick = onClick) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                BhBadge(label = item.status, kind = statusKind(item.status))
                BhBadge(label = item.priority, kind = priorityKind(item.priority))
                BhBadge(label = item.environment, kind = envKind(item.environment))
            }
            if (!item.projectName.isNullOrBlank()) {
                Text(
                    text = item.projectName,
                    style = MaterialTheme.typography.bodySmall,
                    color = tokens.textMuted,
                )
            }
        }
    }
}

private fun statusKind(status: String): BhBadgeKind = when (status.lowercase()) {
    "new" -> BhBadgeKind.STATUS_NEW
    "in progress" -> BhBadgeKind.STATUS_INPROGRESS
    "resolved" -> BhBadgeKind.STATUS_RESOLVED
    "closed" -> BhBadgeKind.STATUS_CLOSED
    "reopened" -> BhBadgeKind.STATUS_REOPENED
    else -> BhBadgeKind.NEUTRAL
}

private fun priorityKind(priority: String): BhBadgeKind = when (priority.lowercase()) {
    "low" -> BhBadgeKind.PRIORITY_LOW
    "medium" -> BhBadgeKind.PRIORITY_MED
    "high" -> BhBadgeKind.PRIORITY_HIGH
    "critical" -> BhBadgeKind.PRIORITY_CRITICAL
    else -> BhBadgeKind.NEUTRAL
}

private fun envKind(env: String): BhBadgeKind = when (env.uppercase()) {
    "DEV" -> BhBadgeKind.ENV_DEV
    "UAT" -> BhBadgeKind.ENV_STAGING
    "PROD" -> BhBadgeKind.ENV_PROD
    else -> BhBadgeKind.NEUTRAL
}
