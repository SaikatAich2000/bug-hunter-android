package com.bughunter.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phonelink
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.bughunter.core.data.local.AppPrefs
import com.bughunter.core.ui.components.BhCard
import com.bughunter.core.ui.components.BhDangerButton
import com.bughunter.core.ui.components.BhErrorBanner
import com.bughunter.core.ui.components.BhFilterChip
import com.bughunter.core.ui.components.BhGhostButton
import com.bughunter.core.ui.components.BhSectionHeader
import com.bughunter.core.ui.components.BhTextField
import com.bughunter.core.ui.theme.BhModalShape
import com.bughunter.core.ui.theme.LocalBrandTokens

@Composable
internal fun SettingsScreen(
    isDebug: Boolean,
    onProfile: () -> Unit,
    onChangePassword: () -> Unit,
    onTwoFactor: () -> Unit,
    onMySessions: () -> Unit,
    onLogout: () -> Unit,
    onAccountDeleted: () -> Unit = onLogout,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    viewModel.setIsDebug(isDebug)
    val state by viewModel.state.collectAsState()
    val deleteState by viewModel.deleteState.collectAsState()

    // Successful deletion is a terminal signal: the AuthRepository has
    // already cleared the local session in its delete-account success
    // path, so we navigate to login (or wherever the host handles the
    // "logged out" state).
    LaunchedEffect(deleteState.isDeleted) {
        if (deleteState.isDeleted) {
            viewModel.resetDeleteState()
            onAccountDeleted()
        }
    }

    SettingsContent(
        state = state,
        deleteState = deleteState,
        onThemeModeChange = viewModel::onThemeModeChange,
        onDefaultNewTypeChange = viewModel::onDefaultNewTypeChange,
        onBaseUrlChange = viewModel::onBaseUrlChange,
        onProfile = onProfile,
        onChangePassword = onChangePassword,
        onTwoFactor = onTwoFactor,
        onMySessions = onMySessions,
        onLogout = onLogout,
        onDeleteAccount = viewModel::deleteAccount,
        onDismissDeleteError = viewModel::dismissDeleteError,
        onResetDeleteState = viewModel::resetDeleteState,
    )
}

@Composable
internal fun SettingsTestHarness(
    state: SettingsUiState,
    deleteState: DeleteAccountUiState = DeleteAccountUiState(),
    onThemeModeChange: (AppPrefs.ThemeMode) -> Unit = {},
    onDefaultNewTypeChange: (String) -> Unit = {},
    onBaseUrlChange: (String) -> Unit = {},
    onProfile: () -> Unit = {},
    onChangePassword: () -> Unit = {},
    onTwoFactor: () -> Unit = {},
    onMySessions: () -> Unit = {},
    onLogout: () -> Unit = {},
    onDeleteAccount: (String) -> Unit = {},
    onDismissDeleteError: () -> Unit = {},
    onResetDeleteState: () -> Unit = {},
) {
    SettingsContent(
        state = state,
        deleteState = deleteState,
        onThemeModeChange = onThemeModeChange,
        onDefaultNewTypeChange = onDefaultNewTypeChange,
        onBaseUrlChange = onBaseUrlChange,
        onProfile = onProfile,
        onChangePassword = onChangePassword,
        onTwoFactor = onTwoFactor,
        onMySessions = onMySessions,
        onLogout = onLogout,
        onDeleteAccount = onDeleteAccount,
        onDismissDeleteError = onDismissDeleteError,
        onResetDeleteState = onResetDeleteState,
    )
}

@Composable
private fun SettingsContent(
    state: SettingsUiState,
    deleteState: DeleteAccountUiState,
    onThemeModeChange: (AppPrefs.ThemeMode) -> Unit,
    onDefaultNewTypeChange: (String) -> Unit,
    onBaseUrlChange: (String) -> Unit,
    onProfile: () -> Unit,
    onChangePassword: () -> Unit,
    onTwoFactor: () -> Unit,
    onMySessions: () -> Unit,
    onLogout: () -> Unit,
    onDeleteAccount: (String) -> Unit,
    onDismissDeleteError: () -> Unit,
    onResetDeleteState: () -> Unit,
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    val tokens = LocalBrandTokens.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
        )
        BhSectionHeader(text = "Account")
        SettingsRow(
            icon = Icons.Outlined.Person,
            title = "Profile",
            subtitle = "Display name and email",
            onClick = onProfile,
        )
        SettingsRow(
            icon = Icons.Outlined.Lock,
            title = "Change password",
            subtitle = "Rotate your sign-in password",
            onClick = onChangePassword,
        )
        SettingsRow(
            icon = Icons.Outlined.Security,
            title = "Two-factor authentication",
            subtitle = "Manage authenticator app & recovery codes",
            onClick = onTwoFactor,
        )
        SettingsRow(
            icon = Icons.Outlined.Phonelink,
            title = "My sessions",
            subtitle = "See active devices and sign out remotely",
            onClick = onMySessions,
        )
        BhSectionHeader(text = "Appearance")
        BhCard {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Theme",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AppPrefs.ThemeMode.values().forEach { mode ->
                        BhFilterChip(
                            label = mode.name.lowercase().replaceFirstChar(Char::uppercase),
                            selected = state.themeMode == mode,
                            onClick = { onThemeModeChange(mode) },
                        )
                    }
                }
            }
        }
        BhSectionHeader(text = "Defaults")
        BhCard {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Default new item type",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Bug", "Requirement", "Task").forEach { type ->
                        BhFilterChip(
                            label = type,
                            selected = state.defaultNewType == type,
                            onClick = { onDefaultNewTypeChange(type) },
                        )
                    }
                }
            }
        }
        if (state.isDebug) {
            BhSectionHeader(text = "Developer")
            BhCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Base URL (debug only)",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    BhTextField(
                        value = state.baseUrl,
                        onValueChange = onBaseUrlChange,
                        label = "Base URL",
                        helper = "Restart the app to apply.",
                    )
                }
            }
        }
        BhSectionHeader(text = "Session")
        BhDangerButton(
            text = "Sign out",
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth(),
        )

        // Account deletion entry point. Google Play requires this be
        // reachable in-app for any app that supports user accounts.
        // Tapping opens a confirmation dialog (password re-auth) before
        // calling DELETE /api/auth/account.
        BhSectionHeader(text = "Danger zone")
        BhCard {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Delete your account",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "Permanently removes your profile, sessions, and saved views. " +
                        "Bugs you reported stay so your team isn't disrupted, but your " +
                        "name on them is anonymised. This cannot be undone.",
                    style = MaterialTheme.typography.labelSmall,
                    color = tokens.textMuted,
                )
                BhDangerButton(
                    text = "Delete account",
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        Text(
            text = "Bug Hunter for Android 2.9",
            style = MaterialTheme.typography.labelSmall,
            color = tokens.textFaint,
        )
    }

    if (showDeleteDialog) {
        DeleteAccountDialog(
            state = deleteState,
            onConfirm = onDeleteAccount,
            onDismiss = {
                if (!deleteState.isSubmitting) {
                    showDeleteDialog = false
                    onResetDeleteState()
                }
            },
            onDismissError = onDismissDeleteError,
        )
    }
}

@Composable
private fun DeleteAccountDialog(
    state: DeleteAccountUiState,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    onDismissError: () -> Unit,
) {
    var password by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = !state.isSubmitting,
            dismissOnClickOutside = !state.isSubmitting,
        ),
    ) {
        Surface(
            shape = BhModalShape,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp,
            modifier = Modifier
                .widthIn(max = 480.dp)
                .padding(16.dp),
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "Delete your account?",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "This is permanent. Enter your password to confirm.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                BhTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        if (state.error != null) onDismissError()
                    },
                    label = "Password",
                    keyboardType = KeyboardType.Password,
                    isPassword = true,
                    required = true,
                )
                BhErrorBanner(error = state.error)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    BhGhostButton(
                        text = "Cancel",
                        onClick = onDismiss,
                        enabled = !state.isSubmitting,
                    )
                    BhDangerButton(
                        text = if (state.isSubmitting) "Deleting..." else "Delete forever",
                        onClick = { onConfirm(password) },
                        enabled = password.isNotEmpty() && !state.isSubmitting,
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    val tokens = LocalBrandTokens.current
    BhCard(onClick = onClick) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = tokens.textMuted,
                )
            }
        }
    }
}
