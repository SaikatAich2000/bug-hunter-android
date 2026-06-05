package com.bughunter.feature.organizations.branding

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
import com.bughunter.core.ui.components.BhAttachmentCard
import com.bughunter.core.ui.components.BhGhostButton
import com.bughunter.core.ui.components.BhPrimaryButton
import com.bughunter.core.ui.components.BhSectionHeader
import com.bughunter.core.ui.components.BhTextField
import com.bughunter.core.ui.components.BhTopAppBar
import com.bughunter.core.ui.theme.LocalBrandTokens
import com.bughunter.core.ui.util.UiState

@Composable
internal fun BrandingScreen(
    onBack: () -> Unit,
    viewModel: BrandingViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    BrandingContent(
        state = state,
        onBack = onBack,
        onPrimary = viewModel::onPrimary,
        onSecondary = viewModel::onSecondary,
        onAccent = viewModel::onAccent,
        onFavicon = viewModel::onFavicon,
        onLogoCleared = { viewModel.onLogo(null) },
        onSave = { viewModel.save { /* no-op */ } },
    )
}

@Composable
internal fun BrandingScreenTestHarness(
    state: UiState<BrandingModel>,
    onBack: () -> Unit = {},
    onPrimary: (String) -> Unit = {},
    onSecondary: (String) -> Unit = {},
    onAccent: (String) -> Unit = {},
    onFavicon: (String?) -> Unit = {},
    onLogoCleared: () -> Unit = {},
    onSave: () -> Unit = {},
) {
    BrandingContent(state, onBack, onPrimary, onSecondary, onAccent, onFavicon, onLogoCleared, onSave)
}

@Composable
private fun BrandingContent(
    state: UiState<BrandingModel>,
    onBack: () -> Unit,
    onPrimary: (String) -> Unit,
    onSecondary: (String) -> Unit,
    onAccent: (String) -> Unit,
    onFavicon: (String?) -> Unit,
    onLogoCleared: () -> Unit,
    onSave: () -> Unit,
) {
    val tokens = LocalBrandTokens.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        BhTopAppBar(
            title = "Branding",
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
            ) { Text("Couldn't load branding.", color = MaterialTheme.colorScheme.error) }
            UiState.Empty -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) { Text("No data.", color = tokens.textMuted) }
            is UiState.Success -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                BhSectionHeader(text = "Colors")
                HslColorPicker(
                    label = "Primary",
                    hexValue = state.data.primary,
                    onHexChange = onPrimary,
                )
                HslColorPicker(
                    label = "Secondary",
                    hexValue = state.data.secondary,
                    onHexChange = onSecondary,
                )
                HslColorPicker(
                    label = "Accent",
                    hexValue = state.data.accent,
                    onHexChange = onAccent,
                )
                BhSectionHeader(text = "Logo")
                if (state.data.logoDataUrl != null) {
                    BhAttachmentCard(
                        filename = "Org logo",
                        sizeBytes = 0L,
                        uploader = "Branding",
                        previewUrl = state.data.logoDataUrl,
                        isImage = true,
                        onDownload = {},
                        onDelete = onLogoCleared,
                    )
                } else {
                    Text(
                        text = "No logo uploaded.",
                        style = MaterialTheme.typography.bodySmall,
                        color = tokens.textMuted,
                    )
                }
                BhSectionHeader(text = "Favicon")
                BhTextField(
                    value = state.data.faviconUrl.orEmpty(),
                    onValueChange = { onFavicon(it.ifBlank { null }) },
                    label = "Favicon URL",
                    helper = "Optional — leave empty to use default",
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
                    )
                }
            }
        }
    }
}
