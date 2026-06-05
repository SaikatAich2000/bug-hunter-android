package com.bughunter.core.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.bughunter.core.ui.theme.BhModalShape

@Composable
fun BhConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    cancelLabel: String = "Cancel",
    danger: Boolean = false,
    loading: Boolean = false,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        shape = BhModalShape,
        containerColor = AlertDialogDefaults.containerColor,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurface,
        title = {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
        },
        text = {
            Text(text = message, style = MaterialTheme.typography.bodyMedium)
        },
        confirmButton = {
            if (danger) {
                BhDangerButton(
                    text = confirmLabel,
                    onClick = onConfirm,
                    loading = loading,
                )
            } else {
                BhPrimaryButton(
                    text = confirmLabel,
                    onClick = onConfirm,
                    loading = loading,
                )
            }
        },
        dismissButton = {
            BhGhostButton(
                text = cancelLabel,
                onClick = onDismiss,
            )
        },
    )
}
