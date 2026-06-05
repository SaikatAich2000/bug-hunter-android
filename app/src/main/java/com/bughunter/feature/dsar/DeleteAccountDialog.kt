package com.bughunter.feature.dsar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.bughunter.core.network.DomainError
import com.bughunter.core.ui.components.BhDangerButton
import com.bughunter.core.ui.components.BhGhostButton
import com.bughunter.core.ui.components.BhTextField
import com.bughunter.core.ui.theme.BhModalShape

@Composable
internal fun DeleteAccountDialog(
    error: DomainError?,
    isDeleting: Boolean,
    onConfirm: (password: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var password by remember { mutableStateOf("") }
    var ackTyped by remember { mutableStateOf("") }
    val acknowledged = ackTyped.equals("DELETE", ignoreCase = false)
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = BhModalShape,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp,
            modifier = Modifier
                .widthIn(max = 420.dp)
                .padding(16.dp),
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "Delete account",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "This action is irreversible. Type DELETE to confirm.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                BhTextField(
                    value = ackTyped,
                    onValueChange = { ackTyped = it },
                    label = "Type DELETE",
                    placeholder = "DELETE",
                )
                BhTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = "Current password",
                    isPassword = true,
                    keyboardType = KeyboardType.Password,
                    required = true,
                    error = if (error != null) "Couldn't verify your password." else null,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    BhGhostButton(text = "Cancel", onClick = onDismiss)
                    BhDangerButton(
                        text = "Delete account",
                        onClick = { onConfirm(password) },
                        loading = isDeleting,
                        enabled = acknowledged && password.isNotBlank(),
                    )
                }
            }
        }
    }
}
