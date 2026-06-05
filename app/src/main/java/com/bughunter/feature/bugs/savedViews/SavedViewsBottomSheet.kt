package com.bughunter.feature.bugs.savedViews

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bughunter.core.network.dto.SavedViewOut
import com.bughunter.core.ui.components.BhEmptyState
import com.bughunter.core.ui.components.BhIconButton
import com.bughunter.core.ui.components.BhPrimaryButton
import com.bughunter.core.ui.components.BhTextField
import com.bughunter.core.ui.theme.LocalBrandTokens
import com.bughunter.core.ui.util.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SavedViewsBottomSheet(
    onDismiss: () -> Unit,
    currentFiltersBlob: Map<String, Any?>,
    onApply: (SavedViewOut) -> Unit,
    viewModel: SavedViewsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        SavedViewsBottomSheetContent(
            state = state,
            currentFiltersBlob = currentFiltersBlob,
            onApply = onApply,
            onDraftNameChange = viewModel::onDraftNameChange,
            onSaveCurrent = { viewModel.saveCurrent(currentFiltersBlob) },
            onDelete = viewModel::delete,
        )
    }
}

@Composable
internal fun SavedViewsBottomSheetContent(
    state: UiState<SavedViewsScreenModel>,
    currentFiltersBlob: Map<String, Any?>,
    onApply: (SavedViewOut) -> Unit,
    onDraftNameChange: (String) -> Unit,
    onSaveCurrent: () -> Unit,
    onDelete: (Int) -> Unit,
) {
    val tokens = LocalBrandTokens.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Saved views",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
        )
        when (state) {
            UiState.Loading -> Text(
                text = "Loading…",
                style = MaterialTheme.typography.bodySmall,
                color = tokens.textMuted,
            )
            UiState.Empty -> BhEmptyState(title = "No saved views yet.")
            is UiState.Error -> Text(
                text = "Failed to load saved views.",
                color = MaterialTheme.colorScheme.error,
            )
            is UiState.Success -> {
                if (state.data.views.isEmpty()) {
                    BhEmptyState(title = "No saved views yet.")
                } else {
                    state.data.views.forEach { view ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = view.name,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                text = if (view.isShared) "Shared" else "Private",
                                style = MaterialTheme.typography.labelSmall,
                                color = tokens.textMuted,
                            )
                            BhIconButton(
                                icon = Icons.Filled.Delete,
                                contentDescription = "Delete view",
                                onClick = { onDelete(view.id) },
                                danger = true,
                            )
                            BhPrimaryButton(
                                text = "Apply",
                                onClick = { onApply(view) },
                            )
                        }
                    }
                }
                BhTextField(
                    value = state.data.draftName,
                    onValueChange = onDraftNameChange,
                    label = "New view name",
                )
                BhPrimaryButton(
                    text = "Save current view",
                    onClick = onSaveCurrent,
                    enabled = state.data.draftName.isNotBlank() && !state.data.isSaving,
                    loading = state.data.isSaving,
                    modifier = Modifier.fillMaxWidth(),
                    fullWidth = true,
                )
            }
        }
    }
}
