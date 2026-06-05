package com.bughunter.feature.dsar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bughunter.core.ui.components.BhCard
import com.bughunter.core.ui.components.BhDangerButton
import com.bughunter.core.ui.components.BhEmptyState
import com.bughunter.core.ui.components.BhPrimaryButton
import com.bughunter.core.ui.theme.LocalBrandTokens

@Composable
internal fun DsarExportScreen(
    onAccountDeleted: () -> Unit,
    viewModel: DsarExportViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    if (state.deleteCompleted) {
        onAccountDeleted()
    }
    DsarExportContent(
        state = state,
        onExport = viewModel::exportData,
        onDeleteRequest = viewModel::openDeleteDialog,
    )
    if (state.isDeleteDialogOpen) {
        DeleteAccountDialog(
            error = state.deleteError,
            isDeleting = state.isDeleting,
            onConfirm = viewModel::confirmDelete,
            onDismiss = viewModel::dismissDeleteDialog,
        )
    }
}

@Composable
internal fun DsarExportTestHarness(
    state: DsarExportUiState,
    onExport: () -> Unit = {},
    onDeleteRequest: () -> Unit = {},
) {
    DsarExportContent(state = state, onExport = onExport, onDeleteRequest = onDeleteRequest)
}

@Composable
private fun DsarExportContent(
    state: DsarExportUiState,
    onExport: () -> Unit,
    onDeleteRequest: () -> Unit,
) {
    val tokens = LocalBrandTokens.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Your data",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "Download a complete copy of your personal data — profile, comments, attachments metadata, and audit footprint.",
            style = MaterialTheme.typography.bodyMedium,
            color = tokens.textMuted,
        )
        BhPrimaryButton(
            text = "Download my data",
            onClick = onExport,
            loading = state.isExporting,
            fullWidth = true,
            modifier = Modifier.fillMaxWidth(),
        )
        if (state.error != null) {
            Text(
                text = "Couldn't generate export. Try again.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        Text(
            text = "Recent exports",
            style = MaterialTheme.typography.labelSmall,
            color = tokens.textMuted,
        )
        if (state.exports.isEmpty()) {
            BhEmptyState(
                title = "No exports yet",
                helper = "Press the button above to start one.",
                icon = Icons.Outlined.Inventory2,
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                items(items = state.exports, key = { it.fileName }) { entry ->
                    BhCard {
                        Row(
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            androidx.compose.material3.Icon(
                                imageVector = Icons.Outlined.Download,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = entry.fileName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = entry.timestampLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = tokens.textMuted,
                                )
                            }
                        }
                    }
                }
            }
        }
        Text(
            text = "Delete account",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 12.dp),
        )
        Text(
            text = "This is permanent. You will lose access to every project you don't own, and your data export becomes unavailable.",
            style = MaterialTheme.typography.bodyMedium,
            color = tokens.textMuted,
        )
        BhDangerButton(
            text = "Delete my account",
            onClick = onDeleteRequest,
        )
    }
}
