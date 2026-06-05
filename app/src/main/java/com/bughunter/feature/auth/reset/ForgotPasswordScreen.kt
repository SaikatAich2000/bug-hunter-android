package com.bughunter.feature.auth.reset

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
import androidx.compose.material3.MaterialTheme
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
import com.bughunter.core.ui.components.BhGhostButton
import com.bughunter.core.ui.components.BhPrimaryButton
import com.bughunter.core.ui.components.BhTextField
import com.bughunter.core.ui.theme.LocalBrandTokens
import com.bughunter.core.ui.util.Email

@Composable
internal fun ForgotPasswordScreen(
    onBack: () -> Unit,
    viewModel: ForgotPasswordViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    ForgotPasswordContent(
        state = state,
        onEmailChange = viewModel::onEmailChange,
        onSubmit = viewModel::onSubmit,
        onBack = onBack,
    )
}

@Composable
internal fun ForgotPasswordTestHarness(
    state: ForgotPasswordUiState,
    onEmailChange: (String) -> Unit = {},
    onSubmit: () -> Unit = {},
    onBack: () -> Unit = {},
) {
    ForgotPasswordContent(
        state = state,
        onEmailChange = onEmailChange,
        onSubmit = onSubmit,
        onBack = onBack,
    )
}

@Composable
private fun ForgotPasswordContent(
    state: ForgotPasswordUiState,
    onEmailChange: (String) -> Unit,
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
                    text = "Forgot password",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Enter the email associated with your account and we'll send you a reset link.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = tokens.textMuted,
                )
                if (state.submitted) {
                    Text(
                        text = "If that email exists, a reset link was sent.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                } else {
                    BhTextField(
                        value = state.email,
                        onValueChange = onEmailChange,
                        label = "Email",
                        required = true,
                        keyboardType = KeyboardType.Email,
                        error = state.error?.let(::messageFor),
                    )
                    BhPrimaryButton(
                        text = "Send reset link",
                        onClick = onSubmit,
                        loading = state.isSubmitting,
                        enabled = Email.isValid(state.email) && !state.isSubmitting,
                        modifier = Modifier.fillMaxWidth(),
                        fullWidth = true,
                    )
                }
                BhGhostButton(
                    text = "Back to sign in",
                    onClick = onBack,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

private fun messageFor(error: DomainError): String = when (error) {
    is DomainError.RateLimited -> "Too many attempts. Please try again later."
    DomainError.Network -> "Network unavailable."
    is DomainError.Server -> error.message
    else -> "Something went wrong."
}
