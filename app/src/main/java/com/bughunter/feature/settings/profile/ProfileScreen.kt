package com.bughunter.feature.settings.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import com.bughunter.core.network.dto.MeOut
import com.bughunter.core.ui.components.BhAvatar
import com.bughunter.core.ui.components.BhCard
import com.bughunter.core.ui.components.BhPrimaryButton
import com.bughunter.core.ui.components.BhSectionHeader
import com.bughunter.core.ui.components.BhTextField
import com.bughunter.core.ui.theme.LocalBrandTokens

@Composable
internal fun ProfileScreen(
    onChangePassword: () -> Unit,
    onChangeEmail: () -> Unit,
    onTwoFactor: () -> Unit,
    onMySessions: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    ProfileContent(
        state = state,
        onNameChange = viewModel::onNameChange,
        onSaveName = viewModel::saveName,
        onChangePassword = onChangePassword,
        onChangeEmail = onChangeEmail,
        onTwoFactor = onTwoFactor,
        onMySessions = onMySessions,
    )
}

@Composable
internal fun ProfileTestHarness(
    state: ProfileUiState,
    onNameChange: (String) -> Unit = {},
    onSaveName: () -> Unit = {},
    onChangePassword: () -> Unit = {},
    onChangeEmail: () -> Unit = {},
    onTwoFactor: () -> Unit = {},
    onMySessions: () -> Unit = {},
) {
    ProfileContent(
        state = state,
        onNameChange = onNameChange,
        onSaveName = onSaveName,
        onChangePassword = onChangePassword,
        onChangeEmail = onChangeEmail,
        onTwoFactor = onTwoFactor,
        onMySessions = onMySessions,
    )
}

@Composable
private fun ProfileContent(
    state: ProfileUiState,
    onNameChange: (String) -> Unit,
    onSaveName: () -> Unit,
    onChangePassword: () -> Unit,
    onChangeEmail: () -> Unit,
    onTwoFactor: () -> Unit,
    onMySessions: () -> Unit,
) {
    val tokens = LocalBrandTokens.current
    val me: MeOut? = state.me
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Profile",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
        )
        BhCard {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                BhAvatar(
                    displayName = me?.name,
                    userId = me?.id?.toString() ?: "?",
                    sizeDp = 56,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = me?.name ?: "—",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = me?.email.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = tokens.textMuted,
                    )
                    Text(
                        text = "${me?.role?.name?.lowercase()?.replaceFirstChar(Char::uppercase) ?: ""} · ${me?.organizationName.orEmpty()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = tokens.textFaint,
                    )
                }
            }
        }
        BhSectionHeader(text = "Identity")
        BhTextField(
            value = state.nameEdit,
            onValueChange = onNameChange,
            label = "Display name",
            required = true,
            error = if (state.error != null) "Couldn't save your name." else null,
            helper = if (state.savedName) "Saved." else null,
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            BhPrimaryButton(
                text = "Save",
                onClick = onSaveName,
                loading = state.isSavingName,
                enabled = state.nameEdit.isNotBlank() && state.nameEdit != me?.name,
            )
        }
        BhSectionHeader(text = "Security")
        ProfileLinkRow(
            title = "Change password",
            description = "Rotate the password for this account.",
            onClick = onChangePassword,
        )
        ProfileLinkRow(
            title = "Change email",
            description = "Stage a new address, verify with the code.",
            onClick = onChangeEmail,
        )
        ProfileLinkRow(
            title = "Two-factor authentication",
            description = "Authenticator app and recovery codes.",
            onClick = onTwoFactor,
        )
        ProfileLinkRow(
            title = "My sessions",
            description = "View signed-in devices, sign out remotely.",
            onClick = onMySessions,
        )
    }
}

@Composable
private fun ProfileLinkRow(
    title: String,
    description: String,
    onClick: () -> Unit,
) {
    val tokens = LocalBrandTokens.current
    BhCard(onClick = onClick) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.labelSmall,
                color = tokens.textMuted,
            )
        }
    }
}
