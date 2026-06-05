package com.bughunter.feature.auth.reset

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bughunter.core.network.DomainError
import com.bughunter.core.ui.components.BhGhostButton
import com.bughunter.core.ui.components.BhPrimaryButton
import com.bughunter.core.ui.components.BhTextField
import com.bughunter.core.ui.theme.LocalBrandTokens

@Composable
internal fun ResetPasswordScreen(
    token: String,
    onBack: () -> Unit,
    onSuccess: () -> Unit,
    viewModel: ResetPasswordViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(state.isSuccess) {
        if (state.isSuccess) onSuccess()
    }
    ResetPasswordContent(
        state = state,
        onNewPasswordChange = viewModel::onNewPasswordChange,
        onConfirmPasswordChange = viewModel::onConfirmPasswordChange,
        onSubmit = viewModel::onSubmit,
        onBack = onBack,
    )
}

@Composable
internal fun ResetPasswordTestHarness(
    state: ResetPasswordUiState,
    onNewPasswordChange: (String) -> Unit = {},
    onConfirmPasswordChange: (String) -> Unit = {},
    onSubmit: () -> Unit = {},
    onBack: () -> Unit = {},
) {
    ResetPasswordContent(
        state = state,
        onNewPasswordChange = onNewPasswordChange,
        onConfirmPasswordChange = onConfirmPasswordChange,
        onSubmit = onSubmit,
        onBack = onBack,
    )
}

@Composable
private fun ResetPasswordContent(
    state: ResetPasswordUiState,
    onNewPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onBack: () -> Unit,
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
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    text = "Set a new password",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Choose a password you do not use anywhere else.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = tokens.textMuted,
                )
                BhTextField(
                    value = state.newPassword,
                    onValueChange = onNewPasswordChange,
                    label = "New password",
                    required = true,
                    isPassword = true,
                    keyboardType = KeyboardType.Password,
                    helper = "At least 8 characters, including a letter and a digit.",
                )
                PasswordStrengthMeter(
                    valid = state.strengthOk,
                    nonEmpty = state.newPassword.isNotEmpty(),
                )
                BhTextField(
                    value = state.confirmPassword,
                    onValueChange = onConfirmPasswordChange,
                    label = "Confirm password",
                    required = true,
                    isPassword = true,
                    keyboardType = KeyboardType.Password,
                    error = if (state.confirmPassword.isNotEmpty() && !state.passwordsMatch) {
                        "Passwords do not match"
                    } else null,
                )
                val errorText = state.error?.let(::messageFor)
                if (errorText != null) {
                    Text(
                        text = errorText,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                BhPrimaryButton(
                    text = "Set new password",
                    onClick = onSubmit,
                    loading = state.isSubmitting,
                    enabled = state.strengthOk && state.passwordsMatch && !state.isSubmitting,
                    modifier = Modifier.fillMaxWidth(),
                    fullWidth = true,
                )
                BhGhostButton(
                    text = "Back to sign in",
                    onClick = onBack,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun PasswordStrengthMeter(valid: Boolean, nonEmpty: Boolean) {
    val progress = when {
        !nonEmpty -> 0f
        valid -> 1f
        else -> 0.4f
    }
    val color = if (valid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
    LinearProgressIndicator(
        progress = { progress },
        color = color,
        trackColor = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp)),
    )
}

private fun messageFor(error: DomainError): String = when (error) {
    is DomainError.Validation -> error.message ?: "The reset link is invalid or expired."
    is DomainError.Server -> error.message
    DomainError.Network -> "Network unavailable."
    else -> "Something went wrong."
}
