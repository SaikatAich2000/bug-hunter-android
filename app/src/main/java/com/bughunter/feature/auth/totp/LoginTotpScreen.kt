package com.bughunter.feature.auth.totp

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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bughunter.core.network.DomainError
import com.bughunter.core.ui.components.BhCodeField
import com.bughunter.core.ui.components.BhGhostButton
import com.bughunter.core.ui.components.BhPrimaryButton
import com.bughunter.core.ui.theme.LocalBrandTokens
import com.bughunter.core.ui.util.OtpCode

@Composable
internal fun LoginTotpScreen(
    onBackToLogin: () -> Unit,
    viewModel: LoginTotpViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    LoginTotpContent(
        state = state,
        onCodeChange = viewModel::onCodeChange,
        onSubmit = viewModel::onSubmit,
        onBackToLogin = onBackToLogin,
    )
}

@Composable
internal fun LoginTotpTestHarness(
    state: LoginTotpUiState,
    onCodeChange: (String) -> Unit = {},
    onSubmit: () -> Unit = {},
    onBackToLogin: () -> Unit = {},
) {
    LoginTotpContent(
        state = state,
        onCodeChange = onCodeChange,
        onSubmit = onSubmit,
        onBackToLogin = onBackToLogin,
    )
}

@Composable
private fun LoginTotpContent(
    state: LoginTotpUiState,
    onCodeChange: (String) -> Unit,
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
                    text = "Two-factor verification",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = if (state.email.isNotBlank())
                        "Enter the 6-digit code from your authenticator app for ${state.email}."
                    else "Enter the 6-digit code from your authenticator app.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = tokens.textMuted,
                )
                BhCodeField(
                    value = state.code,
                    onValueChange = onCodeChange,
                    isError = state.error != null,
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
                    text = "Verify",
                    onClick = onSubmit,
                    enabled = OtpCode.isValid(state.code) && !state.isSubmitting,
                    loading = state.isSubmitting,
                    modifier = Modifier.fillMaxWidth(),
                    fullWidth = true,
                )
                BhGhostButton(
                    text = "Use a different account",
                    onClick = onBackToLogin,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

private fun messageFor(error: DomainError): String = when (error) {
    DomainError.Unauthorized -> "Code rejected. Try again."
    is DomainError.Validation -> error.message ?: "Invalid code."
    is DomainError.RateLimited -> "Too many attempts. Please try again later."
    is DomainError.Server -> error.message
    DomainError.Network -> "Network unavailable."
    else -> "Something went wrong."
}
