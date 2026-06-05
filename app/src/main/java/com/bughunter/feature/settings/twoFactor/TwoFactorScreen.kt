package com.bughunter.feature.settings.twoFactor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bughunter.core.network.dto.TotpStatus
import com.bughunter.core.ui.components.BhCard
import com.bughunter.core.ui.components.BhCodeField
import com.bughunter.core.ui.components.BhDangerButton
import com.bughunter.core.ui.components.BhGhostButton
import com.bughunter.core.ui.components.BhPrimaryButton
import com.bughunter.core.ui.components.BhSectionHeader
import com.bughunter.core.ui.components.BhTextField
import com.bughunter.core.ui.theme.LocalBrandTokens
import com.bughunter.core.ui.util.OtpCode
import com.bughunter.core.ui.util.UiState
import com.bughunter.core.ui.util.formatRelative

@Composable
internal fun TwoFactorScreen(
    viewModel: TwoFactorViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    TwoFactorContent(
        state = state,
        onBegin = viewModel::beginEnrol,
        onConfirmCodeChange = viewModel::onConfirmCodeChange,
        onConfirm = viewModel::confirmEnrol,
        onOpenDisable = viewModel::openDisable,
        onOpenStatus = viewModel::openStatus,
        onDisablePasswordChange = viewModel::onDisablePasswordChange,
        onConfirmDisable = viewModel::confirmDisable,
        onRegenerate = viewModel::regenerateRecoveryCodes,
    )
}

@Composable
internal fun TwoFactorTestHarness(
    state: TwoFactorUiState,
    onBegin: () -> Unit = {},
    onConfirmCodeChange: (String) -> Unit = {},
    onConfirm: () -> Unit = {},
    onOpenDisable: () -> Unit = {},
    onOpenStatus: () -> Unit = {},
    onDisablePasswordChange: (String) -> Unit = {},
    onConfirmDisable: () -> Unit = {},
    onRegenerate: () -> Unit = {},
) {
    TwoFactorContent(
        state = state,
        onBegin = onBegin,
        onConfirmCodeChange = onConfirmCodeChange,
        onConfirm = onConfirm,
        onOpenDisable = onOpenDisable,
        onOpenStatus = onOpenStatus,
        onDisablePasswordChange = onDisablePasswordChange,
        onConfirmDisable = onConfirmDisable,
        onRegenerate = onRegenerate,
    )
}

@Composable
private fun TwoFactorContent(
    state: TwoFactorUiState,
    onBegin: () -> Unit,
    onConfirmCodeChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onOpenDisable: () -> Unit,
    onOpenStatus: () -> Unit,
    onDisablePasswordChange: (String) -> Unit,
    onConfirmDisable: () -> Unit,
    onRegenerate: () -> Unit,
) {
    val tokens = LocalBrandTokens.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 16.dp)
            .widthIn(max = 560.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Two-factor authentication",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
        )
        when (state.view) {
            TwoFactorView.STATUS -> StatusView(
                statusUi = state.status,
                isSubmitting = state.isSubmitting,
                onBegin = onBegin,
                onOpenDisable = onOpenDisable,
                onRegenerate = onRegenerate,
            )
            TwoFactorView.ENROL -> EnrolView(
                state = state,
                onConfirmCodeChange = onConfirmCodeChange,
                onConfirm = onConfirm,
                onCancel = onOpenStatus,
            )
            TwoFactorView.RECOVERY -> RecoveryView(
                state = state,
                onDone = onOpenStatus,
            )
            TwoFactorView.DISABLE -> DisableView(
                state = state,
                onPasswordChange = onDisablePasswordChange,
                onConfirm = onConfirmDisable,
                onCancel = onOpenStatus,
            )
        }
        if (state.error != null) {
            Text(
                text = "Couldn't complete the action.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        Text(
            text = "Recovery codes are shown once. Save them in your password manager — they let you sign in if you lose access to your authenticator.",
            style = MaterialTheme.typography.labelSmall,
            color = tokens.textMuted,
        )
    }
}

@Composable
private fun StatusView(
    statusUi: UiState<TotpStatus>,
    isSubmitting: Boolean,
    onBegin: () -> Unit,
    onOpenDisable: () -> Unit,
    onRegenerate: () -> Unit,
) {
    val tokens = LocalBrandTokens.current
    when (statusUi) {
        UiState.Loading -> CircularProgressIndicator()
        UiState.Empty -> Text("No status.")
        is UiState.Error -> Text(
            text = "Couldn't load 2FA status.",
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium,
        )
        is UiState.Success -> {
            val status = statusUi.data
            BhCard {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = if (status.enabled) "Two-factor is on" else "Two-factor is off",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (status.enabled) {
                        Text(
                            text = "Enrolled ${status.enrolledAt?.formatRelative() ?: ""}",
                            style = MaterialTheme.typography.bodySmall,
                            color = tokens.textMuted,
                        )
                        Text(
                            text = "${status.unusedRecoveryCodes ?: 0} unused recovery code(s)",
                            style = MaterialTheme.typography.bodySmall,
                            color = tokens.textMuted,
                        )
                    } else {
                        Text(
                            text = "Add an authenticator app to require a 6-digit code on sign-in.",
                            style = MaterialTheme.typography.bodySmall,
                            color = tokens.textMuted,
                        )
                    }
                }
            }
            if (status.enabled) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BhPrimaryButton(
                        text = "Regenerate recovery codes",
                        onClick = onRegenerate,
                        loading = isSubmitting,
                    )
                    BhDangerButton(text = "Disable", onClick = onOpenDisable)
                }
            } else {
                BhPrimaryButton(
                    text = "Enable two-factor",
                    onClick = onBegin,
                    loading = isSubmitting,
                    modifier = Modifier.fillMaxWidth(),
                    fullWidth = true,
                )
            }
        }
    }
}

@Composable
private fun EnrolView(
    state: TwoFactorUiState,
    onConfirmCodeChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    val tokens = LocalBrandTokens.current
    val begin = state.beginOut
    if (begin == null) {
        CircularProgressIndicator()
        return
    }
    BhSectionHeader(text = "Step 1 — scan the QR code")
    QrCodeRenderer(otpauthUri = begin.otpauthUri)
    Text(
        text = "Or enter this secret manually:",
        style = MaterialTheme.typography.bodySmall,
        color = tokens.textMuted,
    )
    Text(
        text = begin.secret,
        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
        color = MaterialTheme.colorScheme.onSurface,
    )
    BhSectionHeader(text = "Step 2 — enter the 6-digit code")
    BhCodeField(
        value = state.confirmCode,
        onValueChange = onConfirmCodeChange,
        isError = state.error != null,
    )
    BhPrimaryButton(
        text = "Confirm",
        onClick = onConfirm,
        loading = state.isSubmitting,
        enabled = OtpCode.isValid(state.confirmCode),
        modifier = Modifier.fillMaxWidth(),
        fullWidth = true,
    )
    BhGhostButton(text = "Cancel", onClick = onCancel, modifier = Modifier.fillMaxWidth())
}

@Composable
private fun RecoveryView(
    state: TwoFactorUiState,
    onDone: () -> Unit,
) {
    val tokens = LocalBrandTokens.current
    val recovery = state.recovery ?: return
    BhSectionHeader(text = "Save these recovery codes")
    Text(
        text = "Each can be used once. We won't show them again.",
        style = MaterialTheme.typography.bodySmall,
        color = tokens.textMuted,
    )
    BhCard {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            recovery.recoveryCodes.forEach { code ->
                Text(
                    text = code,
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
    BhPrimaryButton(
        text = "I've saved them",
        onClick = onDone,
        modifier = Modifier.fillMaxWidth(),
        fullWidth = true,
    )
}

@Composable
private fun DisableView(
    state: TwoFactorUiState,
    onPasswordChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    Text(
        text = "Confirm your password to disable two-factor authentication.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
    )
    BhTextField(
        value = state.disablePassword,
        onValueChange = onPasswordChange,
        label = "Current password",
        isPassword = true,
        required = true,
        keyboardType = KeyboardType.Password,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        BhGhostButton(text = "Cancel", onClick = onCancel, modifier = Modifier.weight(1f))
        BhDangerButton(
            text = "Disable 2FA",
            onClick = onConfirm,
            loading = state.isSubmitting,
            enabled = state.disablePassword.isNotBlank(),
            modifier = Modifier.weight(1f),
        )
    }
}
