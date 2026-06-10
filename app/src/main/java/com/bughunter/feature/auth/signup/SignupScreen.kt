package com.bughunter.feature.auth.signup

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import com.bughunter.core.ui.util.Password

@Composable
internal fun SignupScreen(
    onBack: () -> Unit,
    viewModel: SignupViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    SignupContent(
        state = state,
        onNameChange = viewModel::onNameChange,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onOrganizationChange = viewModel::onOrganizationChange,
        onSubmit = viewModel::onSubmit,
        onBack = onBack,
    )
}

@Composable
internal fun SignupTestHarness(
    state: SignupUiState,
    onNameChange: (String) -> Unit = {},
    onEmailChange: (String) -> Unit = {},
    onPasswordChange: (String) -> Unit = {},
    onOrganizationChange: (String) -> Unit = {},
    onSubmit: () -> Unit = {},
    onBack: () -> Unit = {},
) {
    SignupContent(
        state = state,
        onNameChange = onNameChange,
        onEmailChange = onEmailChange,
        onPasswordChange = onPasswordChange,
        onOrganizationChange = onOrganizationChange,
        onSubmit = onSubmit,
        onBack = onBack,
    )
}

@Composable
private fun SignupContent(
    state: SignupUiState,
    onNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onOrganizationChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onBack: () -> Unit,
) {
    val tokens = LocalBrandTokens.current
    // Inset strategy: we own the top/bottom system bars on the outer Box
    // and let the inner content handle the keyboard via imePadding(). The
    // earlier version double-applied safeDrawing (once via Scaffold's
    // contentWindowInsets, once manually on the Box) AND had no
    // imePadding, so on real phones the bottom inputs + submit button
    // were pushed under the keyboard with no way to scroll to them.
    // That's the bug the user filed as "create organization button not
    // clickable" + "page not responsive, things getting cut".
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        // Scaffold handles only the status-bar inset; the navigation-bar
        // inset is applied inside the scrolling Column so the keyboard
        // (which lives in the same region) doesn't double-pad.
        contentWindowInsets = WindowInsets.statusBars,
        topBar = {
            TopAppBar(
                title = { Text("Create organization") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
    ) { inner: PaddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .background(MaterialTheme.colorScheme.background)
                // imePadding lifts the form when the keyboard opens so
                // the submit button stays visible above the keys. Must
                // sit BEFORE verticalScroll so the scroll viewport
                // itself shrinks when the keyboard opens.
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 480.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    text = "Create your organization",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "You'll be the first admin for this organization.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = tokens.textMuted,
                )
                BhTextField(
                    value = state.organizationName,
                    onValueChange = onOrganizationChange,
                    label = "Organization name",
                    required = true,
                )
                BhTextField(
                    value = state.name,
                    onValueChange = onNameChange,
                    label = "Your name",
                    required = true,
                )
                // Show client-side validation hints for each field once
                // the user has typed something. Without these, the
                // submit button silently greys out when input is invalid
                // and the user has no idea why — bug #1 in the v2.9
                // follow-up report.
                val emailInlineError = when {
                    state.error is DomainError.Conflict ->
                        "An account with that email already exists."
                    state.email.isNotBlank() && !Email.isValid(state.email) ->
                        "Enter a valid email address."
                    else -> null
                }
                BhTextField(
                    value = state.email,
                    onValueChange = onEmailChange,
                    label = "Work email",
                    required = true,
                    keyboardType = KeyboardType.Email,
                    error = emailInlineError,
                )
                val passwordInlineError = when {
                    state.breachRejected ->
                        "This password appears in a known breach corpus. Please choose a different one."
                    state.password.isNotBlank() && !Password.strengthFloor(state.password) ->
                        "At least 8 characters, including a letter and a digit."
                    else -> null
                }
                BhTextField(
                    value = state.password,
                    onValueChange = onPasswordChange,
                    label = "Password",
                    required = true,
                    isPassword = true,
                    keyboardType = KeyboardType.Password,
                    helper = "At least 8 characters, including a letter and a digit.",
                    error = passwordInlineError,
                )
                val errorText = state.error?.takeUnless {
                    it is DomainError.Conflict || state.breachRejected
                }?.let(::messageFor)
                if (errorText != null) {
                    // More prominent error banner — full-width tinted
                    // background instead of small red text, so the user
                    // notices it on slower networks where the submit
                    // result arrives after several seconds.
                    Text(
                        text = errorText,
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
                    text = "Create organization",
                    onClick = onSubmit,
                    loading = state.isSubmitting,
                    enabled = state.canSubmit && !state.isSubmitting,
                    modifier = Modifier.fillMaxWidth(),
                    fullWidth = true,
                )
                BhGhostButton(
                    text = "Already have an account? Sign in",
                    onClick = onBack,
                    modifier = Modifier.fillMaxWidth(),
                )
                // Bottom-bar inset as a Spacer rather than a windowInsets-
                // Padding on the outer container, so it scrolls with the
                // content when the form is tall and the keyboard is open.
                Spacer(
                    modifier = Modifier.height(
                        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 16.dp,
                    ),
                )
            }
        }
    }
}

private fun messageFor(error: DomainError): String = when (error) {
    DomainError.Forbidden -> "Public sign-up is disabled. Ask your administrator for an invite."
    is DomainError.Validation -> error.message ?: "Please review the form."
    is DomainError.RateLimited -> "Too many attempts. Please try again later."
    is DomainError.Server -> error.message
    DomainError.Network -> "Network unavailable."
    else -> "Something went wrong."
}
