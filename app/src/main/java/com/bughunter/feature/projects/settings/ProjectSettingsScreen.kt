package com.bughunter.feature.projects.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bughunter.core.ui.components.BhGhostButton
import com.bughunter.core.ui.components.BhPrimaryButton
import com.bughunter.core.ui.components.BhSectionHeader
import com.bughunter.core.ui.components.BhTextField
import com.bughunter.core.ui.components.BhTopAppBar
import com.bughunter.core.ui.theme.LocalBrandTokens
import com.bughunter.core.ui.util.UiState

@Composable
internal fun ProjectSettingsScreen(
    projectId: Int,
    onBack: () -> Unit,
    viewModel: ProjectSettingsViewModel = hiltViewModel(),
) {
    LaunchedEffect(projectId) { viewModel.load(projectId) }
    val state by viewModel.state.collectAsState()
    ProjectSettingsContent(
        state = state,
        onBack = onBack,
        onName = viewModel::onName,
        onKey = viewModel::onKey,
        onColor = viewModel::onColor,
        onDescription = viewModel::onDescription,
        onDefaultItemType = viewModel::onDefaultItemType,
        onSave = { viewModel.save { /* no-op */ } },
    )
}

@Composable
internal fun ProjectSettingsScreenTestHarness(
    state: UiState<ProjectSettingsModel>,
    onBack: () -> Unit = {},
    onName: (String) -> Unit = {},
    onKey: (String) -> Unit = {},
    onColor: (String) -> Unit = {},
    onDescription: (String) -> Unit = {},
    onDefaultItemType: (String) -> Unit = {},
    onSave: () -> Unit = {},
) {
    ProjectSettingsContent(state, onBack, onName, onKey, onColor, onDescription, onDefaultItemType, onSave)
}

@Composable
private fun ProjectSettingsContent(
    state: UiState<ProjectSettingsModel>,
    onBack: () -> Unit,
    onName: (String) -> Unit,
    onKey: (String) -> Unit,
    onColor: (String) -> Unit,
    onDescription: (String) -> Unit,
    onDefaultItemType: (String) -> Unit,
    onSave: () -> Unit,
) {
    val tokens = LocalBrandTokens.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        BhTopAppBar(
            title = "Project settings",
            navigationIcon = { BhGhostButton(text = "Back", onClick = onBack) },
        )
        when (state) {
            UiState.Loading -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator(color = MaterialTheme.colorScheme.primary) }
            is UiState.Error -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) { Text("Couldn't load settings.", color = MaterialTheme.colorScheme.error) }
            UiState.Empty -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) { Text("No data.", color = tokens.textMuted) }
            is UiState.Success -> SettingsForm(
                model = state.data,
                onName = onName,
                onKey = onKey,
                onColor = onColor,
                onDescription = onDescription,
                onDefaultItemType = onDefaultItemType,
                onSave = onSave,
            )
        }
    }
}

@Composable
private fun SettingsForm(
    model: ProjectSettingsModel,
    onName: (String) -> Unit,
    onKey: (String) -> Unit,
    onColor: (String) -> Unit,
    onDescription: (String) -> Unit,
    onDefaultItemType: (String) -> Unit,
    onSave: () -> Unit,
) {
    val tokens = LocalBrandTokens.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        BhSectionHeader(text = "Project")
        BhTextField(value = model.name, onValueChange = onName, label = "Name", required = true)
        BhTextField(
            value = model.key,
            onValueChange = onKey,
            label = "Key",
            helper = "Uppercase letters and digits, 2-16 chars",
        )
        BhTextField(value = model.color, onValueChange = onColor, label = "Color", helper = "Hex e.g. #6366f1")
        BhTextField(
            value = model.description,
            onValueChange = onDescription,
            label = "Description",
            singleLine = false,
        )
        BhSectionHeader(text = "Defaults")
        BhTextField(
            value = model.defaultItemType,
            onValueChange = onDefaultItemType,
            label = "Default item type",
            helper = "Bug, Requirement or Task",
        )
        BhSectionHeader(text = "Custom fields")
        Text(
            text = "Configure custom fields from the web app.",
            style = MaterialTheme.typography.bodySmall,
            color = tokens.textMuted,
        )
        if (model.saveError != null) {
            Text(
                text = model.saveError,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            BhPrimaryButton(
                text = "Save",
                onClick = onSave,
                loading = model.saving,
                enabled = model.name.isNotBlank(),
            )
        }
    }
}
