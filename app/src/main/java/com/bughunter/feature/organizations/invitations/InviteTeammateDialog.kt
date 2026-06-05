package com.bughunter.feature.organizations.invitations

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bughunter.core.network.dto.Role
import com.bughunter.core.ui.components.BhGhostButton
import com.bughunter.core.ui.components.BhPrimaryButton
import com.bughunter.core.ui.components.BhTextField
import com.bughunter.core.ui.theme.BhModalShape
import com.bughunter.core.ui.theme.LocalBrandTokens

@Composable
internal fun InviteTeammateDialog(
    onDismiss: () -> Unit,
    onSubmit: (email: String, role: Role, projectIds: List<Int>, asLead: Boolean) -> Unit,
) {
    var email by remember { mutableStateOf("") }
    var role by remember { mutableStateOf(Role.MEMBER) }
    var expanded by remember { mutableStateOf(false) }
    val tokens = LocalBrandTokens.current
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = BhModalShape,
        title = { Text("Invite a teammate", style = MaterialTheme.typography.titleMedium) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                BhTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = "Email",
                    required = true,
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Email,
                )
                Text(
                    text = "Role",
                    style = MaterialTheme.typography.labelSmall,
                    color = tokens.textMuted,
                )
                Box {
                    BhGhostButton(
                        text = role.name.lowercase().replaceFirstChar { it.uppercase() },
                        onClick = { expanded = true },
                    )
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        DropdownMenuItem(text = { Text("Member") }, onClick = { role = Role.MEMBER; expanded = false })
                        DropdownMenuItem(text = { Text("Manager") }, onClick = { role = Role.MANAGER; expanded = false })
                        DropdownMenuItem(text = { Text("Admin") }, onClick = { role = Role.ADMIN; expanded = false })
                    }
                }
                Text(
                    text = "They'll get an email with a link to set their password.",
                    style = MaterialTheme.typography.labelSmall,
                    color = tokens.textMuted,
                )
            }
        },
        confirmButton = {
            BhPrimaryButton(
                text = "Send invite",
                onClick = { onSubmit(email.trim(), role, emptyList(), false) },
                enabled = email.contains('@'),
            )
        },
        dismissButton = { BhGhostButton(text = "Cancel", onClick = onDismiss) },
    )
}
