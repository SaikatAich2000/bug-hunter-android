package com.bughunter.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phonelink
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bughunter.core.data.local.AppPrefs
import com.bughunter.core.ui.components.BhCard
import com.bughunter.core.ui.components.BhDangerButton
import com.bughunter.core.ui.components.BhFilterChip
import com.bughunter.core.ui.components.BhGhostButton
import com.bughunter.core.ui.components.BhSectionHeader
import com.bughunter.core.ui.components.BhTextField
import com.bughunter.core.ui.theme.LocalBrandTokens

@Composable
internal fun SettingsScreen(
    isDebug: Boolean,
    onProfile: () -> Unit,
    onChangePassword: () -> Unit,
    onTwoFactor: () -> Unit,
    onMySessions: () -> Unit,
    onLogout: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    viewModel.setIsDebug(isDebug)
    val state by viewModel.state.collectAsState()
    SettingsContent(
        state = state,
        onThemeModeChange = viewModel::onThemeModeChange,
        onDefaultNewTypeChange = viewModel::onDefaultNewTypeChange,
        onBaseUrlChange = viewModel::onBaseUrlChange,
        onProfile = onProfile,
        onChangePassword = onChangePassword,
        onTwoFactor = onTwoFactor,
        onMySessions = onMySessions,
        onLogout = onLogout,
    )
}

@Composable
internal fun SettingsTestHarness(
    state: SettingsUiState,
    onThemeModeChange: (AppPrefs.ThemeMode) -> Unit = {},
    onDefaultNewTypeChange: (String) -> Unit = {},
    onBaseUrlChange: (String) -> Unit = {},
    onProfile: () -> Unit = {},
    onChangePassword: () -> Unit = {},
    onTwoFactor: () -> Unit = {},
    onMySessions: () -> Unit = {},
    onLogout: () -> Unit = {},
) {
    SettingsContent(
        state = state,
        onThemeModeChange = onThemeModeChange,
        onDefaultNewTypeChange = onDefaultNewTypeChange,
        onBaseUrlChange = onBaseUrlChange,
        onProfile = onProfile,
        onChangePassword = onChangePassword,
        onTwoFactor = onTwoFactor,
        onMySessions = onMySessions,
        onLogout = onLogout,
    )
}

@Composable
private fun SettingsContent(
    state: SettingsUiState,
    onThemeModeChange: (AppPrefs.ThemeMode) -> Unit,
    onDefaultNewTypeChange: (String) -> Unit,
    onBaseUrlChange: (String) -> Unit,
    onProfile: () -> Unit,
    onChangePassword: () -> Unit,
    onTwoFactor: () -> Unit,
    onMySessions: () -> Unit,
    onLogout: () -> Unit,
) {
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
        Text(
            text = "Bug Hunter for Android 2.8",
            style = MaterialTheme.typography.labelSmall,
            color = tokens.textFaint,
        )
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
