package com.bughunter.feature.projects.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bughunter.core.ui.components.BhGhostButton
import com.bughunter.core.ui.components.BhPrimaryButton
import com.bughunter.core.ui.components.BhTextField
import com.bughunter.core.ui.theme.BhModalShape

@Composable
internal fun ProjectCreateDialog(
    onDismiss: () -> Unit,
    onSubmit: (name: String, key: String?, color: String, description: String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var key by remember { mutableStateOf("") }
    var color by remember { mutableStateOf("#6366f1") }
    var description by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = BhModalShape,
        title = { Text("New project", style = MaterialTheme.typography.titleMedium) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                BhTextField(value = name, onValueChange = { name = it }, label = "Name", required = true)
                BhTextField(
                    value = key,
                    onValueChange = { key = it.uppercase() },
                    label = "Key",
                    helper = "Uppercase letters and digits, 2-16 chars",
                )
                BhTextField(value = color, onValueChange = { color = it }, label = "Color", helper = "Hex e.g. #6366f1")
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
                onClick = { onSubmit(name.trim(), key.trim().ifBlank { null }, color, description) },
                enabled = name.isNotBlank(),
            )
        },
        dismissButton = { BhGhostButton(text = "Cancel", onClick = onDismiss) },
    )
}
