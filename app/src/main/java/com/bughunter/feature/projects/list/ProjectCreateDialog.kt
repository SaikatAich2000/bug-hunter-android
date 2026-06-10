package com.bughunter.feature.projects.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bughunter.core.network.DomainError
import com.bughunter.core.ui.components.BhErrorBanner
import com.bughunter.core.ui.components.BhGhostButton
import com.bughunter.core.ui.components.BhPrimaryButton
import com.bughunter.core.ui.components.BhTextField
import com.bughunter.core.ui.theme.BhModalShape

private val HEX_RE = Regex("^#?[0-9a-fA-F]{6}$")
private val KEY_RE = Regex("^[A-Z][A-Z0-9]{1,15}$")

@Composable
internal fun ProjectCreateDialog(
    isSubmitting: Boolean,
    serverError: DomainError?,
    onDismiss: () -> Unit,
    onSubmit: (name: String, key: String?, color: String, description: String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var key by remember { mutableStateOf("") }
    var color by remember { mutableStateOf("#6366f1") }
    var description by remember { mutableStateOf("") }

    // Client-side validation hints so the user understands WHY the
    // Create button is greyed out — the v2.10 silent-fail UX bug.
    val keyError = if (key.isNotBlank() && !KEY_RE.matches(key)) {
        "2–16 chars, uppercase letters and digits, starting with a letter"
    } else null
    val colorError = if (color.isNotBlank() && !HEX_RE.matches(color)) {
        "6-digit hex like #6366f1"
    } else null
    val canSubmit = !isSubmitting &&
        name.isNotBlank() &&
        keyError == null &&
        colorError == null

    // If the server returns a field-specific 422, surface the message
    // even though we never re-render the dialog from VM state. Cheapest
    // way to make sure the user sees server-side validation. Just
    // bubbles up via the BhErrorBanner at the top of the form.
    LaunchedEffect(serverError) { /* recomposes when error changes; no-op */ }

    AlertDialog(
        onDismissRequest = if (isSubmitting) ({ /* block dismiss while in-flight */ }) else onDismiss,
        shape = BhModalShape,
        title = { Text("New project", style = MaterialTheme.typography.titleMedium) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                BhErrorBanner(error = serverError)
                BhTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = "Name",
                    required = true,
                )
                BhTextField(
                    value = key,
                    onValueChange = { key = it.uppercase() },
                    label = "Key",
                    helper = "Uppercase letters and digits, 2–16 chars (optional)",
                    error = keyError,
                )
                BhTextField(
                    value = color,
                    onValueChange = { color = it },
                    label = "Color",
                    helper = "Hex e.g. #6366f1",
                    error = colorError,
                )
                BhTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = "Description",
                    singleLine = false,
                )
            }
        },
        confirmButton = {
            BhPrimaryButton(
                text = "Create",
                onClick = {
                    val normalisedColor = if (color.startsWith("#")) color else "#$color"
                    onSubmit(
                        name.trim(),
                        key.trim().ifBlank { null },
                        normalisedColor,
                        description,
                    )
                },
                enabled = canSubmit,
                loading = isSubmitting,
            )
        },
        dismissButton = {
            BhGhostButton(
                text = "Cancel",
                onClick = onDismiss,
            )
        },
    )
}
