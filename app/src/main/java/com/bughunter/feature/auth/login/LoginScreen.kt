package com.bughunter.feature.auth.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bughunter.R
import com.bughunter.core.network.DomainError
import com.bughunter.core.ui.components.BhGhostButton
import com.bughunter.core.ui.components.BhGradientText
import com.bughunter.core.ui.components.BhPrimaryButton
import com.bughunter.core.ui.components.BhTextField
import com.bughunter.core.ui.theme.LocalAccentGradient
import com.bughunter.core.ui.theme.LocalBrandTokens

private const val APP_VERSION_LABEL = "Version 2.10"
private const val BRAND_NAME = "Bug Hunter"

@Composable
internal fun LoginScreen(
    onNavigateForgotPassword: () -> Unit,
    onNavigateSignup: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    LoginContent(
        state = state,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onSubmit = viewModel::onSubmit,
        onForgotPassword = onNavigateForgotPassword,
        onSignup = onNavigateSignup,
    )
}

@Composable
internal fun LoginScreenTestHarness(
    state: LoginUiState,
    onEmailChange: (String) -> Unit = {},
    onPasswordChange: (String) -> Unit = {},
    onSubmit: () -> Unit = {},
    onForgotPassword: () -> Unit = {},
    onSignup: () -> Unit = {},
) {
    LoginContent(
        state = state,
        onEmailChange = onEmailChange,
        onPasswordChange = onPasswordChange,
        onSubmit = onSubmit,
        onForgotPassword = onForgotPassword,
        onSignup = onSignup,
    )
}

@Composable
private fun LoginContent(
    state: LoginUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onForgotPassword: () -> Unit,
    onSignup: () -> Unit,
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        val wide = maxWidth >= 600.dp
        if (wide) {
            Row(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.weight(1f)) { LoginBrandPanel() }
                Box(modifier = Modifier.weight(1f)) {
                    LoginFormPanel(
                        state = state,
                        showInlineLogo = false,
                        onEmailChange = onEmailChange,
                        onPasswordChange = onPasswordChange,
                        onSubmit = onSubmit,
                        onForgotPassword = onForgotPassword,
                        onSignup = onSignup,
                    )
                }
            }
        } else {
            LoginFormPanel(
                state = state,
                showInlineLogo = true,
                onEmailChange = onEmailChange,
                onPasswordChange = onPasswordChange,
                onSubmit = onSubmit,
                onForgotPassword = onForgotPassword,
                onSignup = onSignup,
            )
        }
    }
}

@Composable
private fun BugHunterLogo(size: Int) {
    Image(
        painter = painterResource(id = R.drawable.ic_launcher_foreground),
        contentDescription = BRAND_NAME,
        contentScale = ContentScale.Fit,
        modifier = Modifier.size(size.dp),
    )
}

@Composable
private fun LoginBrandPanel() {
    val gradient = LocalAccentGradient.current
    val tokens = LocalBrandTokens.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BugHunterLogo(size = 132)
        Spacer(modifier = Modifier.height(24.dp))
        BhGradientText(
            text = BRAND_NAME,
            brush = gradient,
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold,
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "Track bugs, requirements, and tasks —\nin one multi-tenant tracker.",
            style = MaterialTheme.typography.bodyMedium,
            color = tokens.textMuted,
        )
    }
}

@Composable
private fun LoginFormPanel(
    state: LoginUiState,
    showInlineLogo: Boolean,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onForgotPassword: () -> Unit,
    onSignup: () -> Unit,
) {
    val tokens = LocalBrandTokens.current
    val gradient = LocalAccentGradient.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (showInlineLogo) {
            Spacer(modifier = Modifier.height(20.dp))
            BugHunterLogo(size = 96)
            Spacer(modifier = Modifier.height(12.dp))
            BhGradientText(
                text = BRAND_NAME,
                brush = gradient,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Track bugs, requirements, and tasks",
                style = MaterialTheme.typography.bodyMedium,
                color = tokens.textMuted,
            )
            Text(
                text = "in one multi-tenant tracker",
                style = MaterialTheme.typography.bodyMedium,
                color = tokens.textMuted,
            )
            Spacer(modifier = Modifier.height(20.dp))
        } else {
            Spacer(modifier = Modifier.height(24.dp))
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 420.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = "Sign in",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
            )
            val errorText = state.error?.let(::messageFor)
            BhTextField(
                value = state.email,
                onValueChange = onEmailChange,
                label = "Email",
                required = true,
                keyboardType = KeyboardType.Email,
                error = if (state.error is DomainError.Unauthorized) errorText else null,
            )
            BhTextField(
                value = state.password,
                onValueChange = onPasswordChange,
                label = "Password",
                required = true,
                isPassword = true,
                keyboardType = KeyboardType.Password,
                error = if (state.error is DomainError.RateLimited) errorText else null,
            )
            // Tinted-background banner so the message is noticeable even
            // when the user is mid-tap on the submit button. Expands +
            // fades in rather than popping so the form shift reads as
            // intentional.
            androidx.compose.animation.AnimatedVisibility(
                visible = errorText != null &&
                    state.error !is DomainError.Unauthorized &&
                    state.error !is DomainError.RateLimited,
                enter = androidx.compose.animation.expandVertically() + androidx.compose.animation.fadeIn(),
                exit = androidx.compose.animation.shrinkVertically() + androidx.compose.animation.fadeOut(),
            ) {
                Text(
                    text = errorText ?: "",
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                        )
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                )
            }
            BhPrimaryButton(
                text = "Sign in",
                onClick = onSubmit,
                loading = state.isSubmitting,
                enabled = state.email.isNotBlank() && state.password.isNotBlank() && state.lockedUntil == null,
                modifier = Modifier.fillMaxWidth(),
                fullWidth = true,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                BhGhostButton(text = "Forgot password?", onClick = onForgotPassword)
                BhGhostButton(text = "Create organization", onClick = onSignup)
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
        Text(
            text = APP_VERSION_LABEL,
            style = MaterialTheme.typography.labelSmall,
            color = tokens.textFaint,
        )
        Spacer(modifier = Modifier.height(8.dp))
    }
}

private fun messageFor(error: DomainError): String = when (error) {
    DomainError.Unauthorized -> "Invalid email or password"
    DomainError.Forbidden -> "Sign-in not allowed"
    DomainError.NotFound -> "Account not found"
    DomainError.Conflict -> "Account conflict"
    is DomainError.Validation -> error.message ?: "Please fix the highlighted fields."
    is DomainError.RateLimited -> "Too many attempts. Please try again later."
    is DomainError.Server -> error.message
    DomainError.Network -> "Network unavailable. Check your connection."
    is DomainError.Unknown -> "Something went wrong: ${error.throwable.javaClass.simpleName}${error.throwable.message?.let { ": $it" } ?: ""}"
}
