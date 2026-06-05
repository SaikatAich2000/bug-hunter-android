package com.bughunter.feature.webhooks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bughunter.core.network.dto.WebhookOut
import com.bughunter.core.ui.components.BhGhostButton
import com.bughunter.core.ui.components.BhPrimaryButton
import com.bughunter.core.ui.components.BhTextField

@Composable
internal fun WebhookFormDialog(
    initial: WebhookOut?,
    onDismiss: () -> Unit,
    onSubmit: (url: String, secret: String, active: Boolean, events: List<String>) -> Unit,
) {
    var url by remember { mutableStateOf(initial?.url ?: "") }
    var secret by remember { mutableStateOf("") }
    var active by remember { mutableStateOf(initial?.active ?: true) }
    var events by remember { mutableStateOf((initial?.eventTypes ?: emptyList()).joinToString(", ")) }
    val isEditing = initial != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEditing) "Edit webhook" else "New webhook") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                BhTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = "Endpoint URL",
                    required = true,
                    placeholder = "https://your-host/path",
                )
                BhTextField(
                    value = secret,
                    onValueChange = { secret = it },
                    label = if (isEditing) "Secret (leave blank to keep)" else "Signing secret",
                    placeholder = "Used to HMAC the payload",
                )
                BhTextField(
                    value = events,
                    onValueChange = { events = it },
                    label = "Event types",
                    helper = "Comma-separated (e.g. bug.created, bug.updated)",
                    singleLine = false,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Switch(checked = active, onCheckedChange = { active = it })
                    Text(
                        text = if (active) "Active" else "Inactive",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        },
        confirmButton = {
            BhPrimaryButton(
                text = if (isEditing) "Save" else "Create",
                onClick = {
                    val parsedEvents = events.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    onSubmit(url.trim(), secret, active, parsedEvents)
                },
                enabled = url.isNotBlank(),
            )
        },
        dismissButton = {
            BhGhostButton(text = "Cancel", onClick = onDismiss)
        },
    )
}
