package com.bughunter.feature.settings.changeEmail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bughunter.core.network.DomainError
import com.bughunter.core.ui.components.BhCodeField
import com.bughunter.core.ui.components.BhGhostButton
import com.bughunter.core.ui.components.BhPrimaryButton
import com.bughunter.core.ui.components.BhTextField
import com.bughunter.core.ui.theme.LocalBrandTokens
import com.bughunter.core.ui.util.Email

@Composable
internal fun ChangeEmailScreen(
    onDone: () -> Unit,
    viewModel: ChangeEmailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    ChangeEmailContent(
        state = state,
        onNewEmailChange = viewModel::onNewEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onCodeChange = viewModel::onCodeChange,
        onRequestSubmit = viewModel::requestChange,
        onConfirmSubmit = viewModel::confirmChange,
        onReset = viewModel::reset,
        onDone = onDone,
    )
}

@Composable
internal fun ChangeEmailTestHarness(
    state: ChangeEmailUiState,
    onNewEmailChange: (String) -> Unit = {},
    onPasswordChange: (String) -> Unit = {},
    onCodeChange: (String) -> Unit = {},
    onRequestSubmit: () -> Unit = {},
    onConfirmSubmit: () -> Unit = {},
    onReset: () -> Unit = {},
    onDone: () -> Unit = {},
) {
    ChangeEmailContent(
        state = state,
        onNewEmailChange = onNewEmailChange,
        onPasswordChange = onPasswordChange,
        onCodeChange = onCodeChange,
        onRequestSubmit = onRequestSubmit,
        onConfirmSubmit = onConfirmSubmit,
        onReset = onReset,
        onDone = onDone,
    )
}

@Composable
private fun ChangeEmailContent(
    state: ChangeEmailUiState,
    onNewEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onCodeChange: (String) -> Unit,
    onRequestSubmit: () -> Unit,
    onConfirmSubmit: () -> Unit,
    onReset: () -> Unit,
    onDone: () -> Unit,
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
            text = "Change email",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
        )
        when (state.step) {
            ChangeEmailStep.REQUEST -> RequestStep(
                state = state,
                onNewEmailChange = onNewEmailChange,
                onPasswordChange = onPasswordChange,
                onSubmit = onRequestSubmit,
            )
            ChangeEmailStep.CONFIRM -> ConfirmStep(
                state = state,
                onCodeChange = onCodeChange,
                onSubmit = onConfirmSubmit,
                onCancel = onReset,
            )
            ChangeEmailStep.DONE -> DoneStep(
                message = state.message ?: "Email updated.",
                onDone = onDone,
            )
        }
        if (state.error != null && state.step != ChangeEmailStep.DONE) {
            Text(
                text = errorMessage(state.error),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        Text(
            text = "We email a 6-digit verification code to the new address. It expires in 15 minutes.",
            style = MaterialTheme.typography.labelSmall,
            color = tokens.textMuted,
        )
    }
}

@Composable
private fun RequestStep(
    state: ChangeEmailUiState,
    onNewEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    BhTextField(
        value = state.newEmail,
        onValueChange = onNewEmailChange,
        label = "New email",
        required = true,
        keyboardType = KeyboardType.Email,
        error = if (state.newEmail.isNotEmpty() && !Email.isValid(state.newEmail)) "Enter a valid email." else null,
    )
    BhTextField(
        value = state.currentPassword,
        onValueChange = onPasswordChange,
        label = "Current password",
        isPassword = true,
        required = true,
        keyboardType = KeyboardType.Password,
    )
    BhPrimaryButton(
        text = "Send verification code",
        onClick = onSubmit,
        loading = state.isSubmitting,
        enabled = Email.isValid(state.newEmail) && state.currentPassword.isNotBlank(),
        modifier = Modifier.fillMaxWidth(),
        fullWidth = true,
    )
}

@Composable
private fun ConfirmStep(
    state: ChangeEmailUiState,
    onCodeChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onCancel: () -> Unit,
) {
    val tokens = LocalBrandTokens.current
    Text(
        text = state.message ?: "Enter the code we sent to ${state.newEmail}.",
        style = MaterialTheme.typography.bodyMedium,
        color = tokens.textMuted,
    )
    BhCodeField(
        value = state.code,
        onValueChange = onCodeChange,
        isError = state.error != null,
    )
    BhPrimaryButton(
        text = "Confirm",
        onClick = onSubmit,
        loading = state.isSubmitting,
        enabled = state.code.length == 6,
        modifier = Modifier.fillMaxWidth(),
        fullWidth = true,
    )
    BhGhostButton(
        text = "Start over",
        onClick = onCancel,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun DoneStep(message: String, onDone: () -> Unit) {
    Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
    )
    BhPrimaryButton(
        text = "Back to profile",
        onClick = onDone,
        modifier = Modifier.fillMaxWidth(),
        fullWidth = true,
    )
}

private fun errorMessage(error: DomainError): String = when (error) {
    is DomainError.Validation -> error.message ?: "Validation failed."
    DomainError.Conflict -> "That email is already in use. Try signing in with it instead."
    DomainError.Unauthorized -> "Current password is incorrect."
    is DomainError.RateLimited -> "Too many attempts. Try again later."
    DomainError.Network -> "Network unavailable."
    is DomainError.Server -> error.message
    else -> "Couldn't change email."
}
