package com.bughunter.feature.events.create

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.bughunter.core.ui.components.BhGhostButton
import com.bughunter.core.ui.components.BhPrimaryButton
import com.bughunter.core.ui.components.BhTextField
import com.bughunter.core.ui.theme.BhModalShape

@Composable
internal fun EventFormDialog(
    eventId: Int?,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    viewModel: EventFormViewModel = hiltViewModel(),
) {
    LaunchedEffect(eventId) { viewModel.start(eventId) }
    val state by viewModel.state.collectAsState()
    LaunchedEffect(state.savedSuccessfully) {
        if (state.savedSuccessfully) onSaved()
    }
    EventFormDialogContent(
        state = state,
        onNameChange = viewModel::onNameChange,
        onDescriptionChange = viewModel::onDescriptionChange,
        onScheduledForChange = viewModel::onScheduledForChange,
        onManagersChange = viewModel::onManagersChange,
        onSubmit = viewModel::submit,
        onDismiss = onDismiss,
    )
}

@Composable
internal fun EventFormDialogTestHarness(
    state: EventFormUiState,
    onNameChange: (String) -> Unit = {},
    onDescriptionChange: (String) -> Unit = {},
    onScheduledForChange: (String) -> Unit = {},
    onManagersChange: (String) -> Unit = {},
    onSubmit: () -> Unit = {},
    onDismiss: () -> Unit = {},
) {
    EventFormDialogContent(
        state = state,
        onNameChange = onNameChange,
        onDescriptionChange = onDescriptionChange,
        onScheduledForChange = onScheduledForChange,
        onManagersChange = onManagersChange,
        onSubmit = onSubmit,
        onDismiss = onDismiss,
    )
}

@Composable
private fun EventFormDialogContent(
    state: EventFormUiState,
    onNameChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onScheduledForChange: (String) -> Unit,
    onManagersChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = BhModalShape,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp,
            modifier = Modifier
                .widthIn(max = 560.dp)
                .padding(16.dp),
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = if (state.eventId == null) "New event" else "Edit event",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                )
                BhTextField(
                    value = state.name,
                    onValueChange = onNameChange,
                    label = "Name",
                    required = true,
                )
                BhTextField(
                    value = state.scheduledFor,
                    onValueChange = onScheduledForChange,
                    label = "Scheduled for",
                    placeholder = "YYYY-MM-DD",
                    keyboardType = KeyboardType.Number,
                )
                BhTextField(
                    value = state.managerIdsCsv,
                    onValueChange = onManagersChange,
                    label = "Manager user IDs",
                    placeholder = "Comma-separated",
                    helper = "Admins or managers can be assigned.",
                )
                BhTextField(
                    value = state.description,
                    onValueChange = onDescriptionChange,
                    label = "Description",
                    singleLine = false,
                )
                if (state.error != null) {
                    Text(
                        text = "Couldn't save event.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    BhGhostButton(text = "Cancel", onClick = onDismiss)
                    BhPrimaryButton(
                        text = if (state.eventId == null) "Create" else "Save",
                        onClick = onSubmit,
                        loading = state.isSubmitting,
                        enabled = state.name.isNotBlank() && !state.isPrefilling,
                    )
                }
            }
        }
    }
}
