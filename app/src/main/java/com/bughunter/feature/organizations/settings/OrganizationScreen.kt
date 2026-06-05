package com.bughunter.feature.organizations.settings

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
internal fun OrganizationScreen(
    onBack: () -> Unit,
    viewModel: OrganizationViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    OrganizationContent(
        state = state,
        onBack = onBack,
        onName = viewModel::onName,
        onDescription = viewModel::onDescription,
        onSave = { viewModel.save { /* no-op */ } },
    )
}

@Composable
internal fun OrganizationScreenTestHarness(
    state: UiState<OrganizationModel>,
    onBack: () -> Unit = {},
    onName: (String) -> Unit = {},
    onDescription: (String) -> Unit = {},
    onSave: () -> Unit = {},
) {
    OrganizationContent(state, onBack, onName, onDescription, onSave)
}

@Composable
private fun OrganizationContent(
    state: UiState<OrganizationModel>,
    onBack: () -> Unit,
    onName: (String) -> Unit,
    onDescription: (String) -> Unit,
    onSave: () -> Unit,
) {
    val tokens = LocalBrandTokens.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        BhTopAppBar(
            title = "Organization",
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
            ) { Text("Couldn't load organization.", color = MaterialTheme.colorScheme.error) }
            UiState.Empty -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) { Text("No data.", color = tokens.textMuted) }
            is UiState.Success -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    BhSectionHeader(text = "Identity")
                    BhTextField(
                        value = state.data.name,
                        onValueChange = onName,
                        label = "Organization name",
                        required = true,
                    )
                    BhTextField(
                        value = state.data.org.slug,
                        onValueChange = {},
                        label = "Slug",
                        helper = "URL-safe identifier, set at signup",
                        readOnly = true,
                    )
                    BhTextField(
                        value = state.data.description,
                        onValueChange = onDescription,
                        label = "Description",
                        singleLine = false,
                    )
                    if (state.data.saveError != null) {
                        Text(
                            text = state.data.saveError,
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
                            loading = state.data.saving,
                            enabled = state.data.name.isNotBlank(),
                        )
                    }
                }
            }
        }
    }
}
