package com.bughunter.feature.auth.acceptInvite

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bughunter.core.network.DomainError
import com.bughunter.core.network.dto.InvitationPreview
import com.bughunter.core.ui.components.BhGhostButton
import com.bughunter.core.ui.components.BhPrimaryButton
import com.bughunter.core.ui.components.BhTextField
import com.bughunter.core.ui.theme.LocalBrandTokens

@Composable
internal fun AcceptInviteScreen(
    token: String,
    onBackToLogin: () -> Unit,
    viewModel: AcceptInviteViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    AcceptInviteContent(
        state = state,
        onNameChange = viewModel::onNameChange,
        onPasswordChange = viewModel::onPasswordChange,
        onRetryPreview = viewModel::retryPreview,
        onSubmit = viewModel::onSubmit,
        onBackToLogin = onBackToLogin,
    )
}

@Composable
internal fun AcceptInviteTestHarness(
    state: AcceptInviteUiState,
    onNameChange: (String) -> Unit = {},
    onPasswordChange: (String) -> Unit = {},
    onRetryPreview: () -> Unit = {},
    onSubmit: () -> Unit = {},
    onBackToLogin: () -> Unit = {},
) {
    AcceptInviteContent(
        state = state,
        onNameChange = onNameChange,
        onPasswordChange = onPasswordChange,
        onRetryPreview = onRetryPreview,
        onSubmit = onSubmit,
        onBackToLogin = onBackToLogin,
    )
}

@Composable
private fun AcceptInviteContent(
    state: AcceptInviteUiState,
    onNameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onRetryPreview: () -> Unit,
    onSubmit: () -> Unit,
    onBackToLogin: () -> Unit,
) {
    val tokens = LocalBrandTokens.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 480.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    text = "Join your team",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                )
                when {
                    state.isLoadingPreview -> Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                    state.previewError != null -> InvitePreviewError(
                        error = state.previewError,
                        onRetry = onRetryPreview,
                        onBackToLogin = onBackToLogin,
                    )
                    state.preview != null -> InviteFormSection(
                        state = state,
                        preview = state.preview,
                        onNameChange = onNameChange,
                        onPasswordChange = onPasswordChange,
                        onSubmit = onSubmit,
                        onBackToLogin = onBackToLogin,
                    )
                }
            }
        }
    }
}

@Composable
private fun InvitePreviewError(
    error: DomainError,
    onRetry: () -> Unit,
    onBackToLogin: () -> Unit,
) {
    val tokens = LocalBrandTokens.current
    Surface(
        color = tokens.readonlyBannerBg,
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = when (error) {
                    DomainError.NotFound -> "This invitation is invalid or no longer available."
                    is DomainError.Validation -> error.message
                        ?: "This invitation cannot be accepted."
                    else -> "We couldn't load this invitation."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
    BhGhostButton(
        text = "Try again",
        onClick = onRetry,
        modifier = Modifier.fillMaxWidth(),
    )
    BhGhostButton(
        text = "Go to sign in",
        onClick = onBackToLogin,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun InviteFormSection(
    state: AcceptInviteUiState,
    preview: InvitationPreview,
    onNameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onBackToLogin: () -> Unit,
) {
    val tokens = LocalBrandTokens.current
    Surface(
        color = tokens.accentSoft,
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "You've been invited to join ${preview.organizationName}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "By ${preview.invitedByName} as ${preview.role.name.lowercase().replaceFirstChar { it.titlecase() }}.",
                style = MaterialTheme.typography.bodySmall,
                color = tokens.textMuted,
            )
            Text(
                text = preview.email,
                style = MaterialTheme.typography.bodySmall,
                color = tokens.textFaint,
            )
        }
    }
    BhTextField(
        value = state.name,
        onValueChange = onNameChange,
        label = "Your name",
        required = true,
    )
    BhTextField(
        value = state.password,
        onValueChange = onPasswordChange,
        label = "Password",
        required = true,
        isPassword = true,
        keyboardType = KeyboardType.Password,
        helper = "At least 8 characters, including a letter and a digit.",
    )
    val errorText = state.submitError?.let(::messageFor)
    if (errorText != null) {
        Text(
            text = errorText,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
        )
    }
    BhPrimaryButton(
        text = "Accept & sign in",
        onClick = onSubmit,
        loading = state.isSubmitting,
        enabled = state.canSubmit && !state.isSubmitting,
        modifier = Modifier.fillMaxWidth(),
        fullWidth = true,
    )
    BhGhostButton(
        text = "Back to sign in",
        onClick = onBackToLogin,
        modifier = Modifier.fillMaxWidth(),
    )
}

private fun messageFor(error: DomainError): String = when (error) {
    DomainError.Conflict -> "That email is already registered."
    is DomainError.Validation -> error.message ?: "Please review the form."
    is DomainError.Server -> error.message
    DomainError.Network -> "Network unavailable."
    else -> "Something went wrong."
}
