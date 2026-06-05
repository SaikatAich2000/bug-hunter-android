package com.bughunter.feature.settings.changePassword

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
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
import com.bughunter.core.ui.components.BhPrimaryButton
import com.bughunter.core.ui.components.BhTextField
import com.bughunter.core.ui.theme.BhPillShape
import com.bughunter.core.ui.theme.LocalAccentGradient
import com.bughunter.core.ui.theme.LocalBrandTokens

@Composable
internal fun ChangePasswordScreen(
    onDone: () -> Unit,
    viewModel: ChangePasswordViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(state.finished) { if (state.finished) onDone() }
    ChangePasswordContent(
        state = state,
        onCurrentChange = viewModel::onCurrentChange,
        onNewChange = viewModel::onNewChange,
        onConfirmChange = viewModel::onConfirmChange,
        onSubmit = viewModel::submit,
    )
}

@Composable
internal fun ChangePasswordTestHarness(
    state: ChangePasswordUiState,
    onCurrentChange: (String) -> Unit = {},
    onNewChange: (String) -> Unit = {},
    onConfirmChange: (String) -> Unit = {},
    onSubmit: () -> Unit = {},
) {
    ChangePasswordContent(
        state = state,
        onCurrentChange = onCurrentChange,
        onNewChange = onNewChange,
        onConfirmChange = onConfirmChange,
        onSubmit = onSubmit,
    )
}

@Composable
private fun ChangePasswordContent(
    state: ChangePasswordUiState,
    onCurrentChange: (String) -> Unit,
    onNewChange: (String) -> Unit,
    onConfirmChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    val tokens = LocalBrandTokens.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 16.dp)
            .widthIn(max = 520.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Change password",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
        )
        BhTextField(
            value = state.currentPassword,
            onValueChange = onCurrentChange,
            label = "Current password",
            isPassword = true,
            required = true,
            keyboardType = KeyboardType.Password,
            error = if (state.error is DomainError.Validation && !state.breachRejected) state.error.message else null,
        )
        BhTextField(
            value = state.newPassword,
            onValueChange = onNewChange,
            label = "New password",
            isPassword = true,
            required = true,
            keyboardType = KeyboardType.Password,
            helper = "8+ chars, mix letters and numbers",
            error = if (state.breachRejected) "This password appears in a known breach corpus." else null,
        )
        if (state.newPassword.isNotEmpty()) {
            StrengthMeter(fraction = state.strengthFraction, label = state.strengthLabel)
        }
        BhTextField(
            value = state.confirmPassword,
            onValueChange = onConfirmChange,
            label = "Confirm new password",
            isPassword = true,
            required = true,
            keyboardType = KeyboardType.Password,
            error = if (state.confirmPassword.isNotEmpty() && state.confirmPassword != state.newPassword) "Passwords don't match" else null,
        )
        BhPrimaryButton(
            text = "Update password",
            onClick = onSubmit,
            loading = state.isSubmitting,
            enabled = state.currentPassword.isNotBlank() && state.passwordsMatch && state.strengthFraction >= 0.45f,
            modifier = Modifier.fillMaxWidth(),
            fullWidth = true,
        )
        Text(
            text = "Updating your password signs out all other devices.",
            style = MaterialTheme.typography.labelSmall,
            color = tokens.textMuted,
        )
    }
}

@Composable
private fun StrengthMeter(fraction: Float, label: String) {
    val tokens = LocalBrandTokens.current
    val accent = LocalAccentGradient.current
    Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(BhPillShape)
                .background(tokens.borderSoft, BhPillShape),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                    .height(6.dp)
                    .background(brush = accent, shape = BhPillShape),
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = tokens.textMuted,
        )
    }
}
