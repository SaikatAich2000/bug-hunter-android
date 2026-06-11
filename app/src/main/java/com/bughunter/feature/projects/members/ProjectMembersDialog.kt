package com.bughunter.feature.projects.members

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.bughunter.core.network.dto.ProjectMembershipOut
import com.bughunter.core.network.dto.ProjectRole
import com.bughunter.core.network.dto.UserOut
import com.bughunter.core.ui.components.BhAvatar
import com.bughunter.core.ui.components.BhDangerButton
import com.bughunter.core.ui.components.BhGhostButton
import com.bughunter.core.ui.components.BhPrimaryButton
import com.bughunter.core.ui.components.BhSectionHeader
import com.bughunter.core.ui.components.BhTextField
import com.bughunter.core.ui.theme.LocalBrandTokens

@Composable
internal fun ProjectMembersDialog(
    projectId: Int,
    onDismiss: () -> Unit,
    viewModel: ProjectMembersViewModel = hiltViewModel(),
) {
    LaunchedEffect(projectId) { viewModel.load(projectId) }
    val state by viewModel.state.collectAsState()
    ProjectMembersDialogContent(
        members = (state as? com.bughunter.core.ui.util.UiState.Success)?.data?.members.orEmpty(),
        candidates = (state as? com.bughunter.core.ui.util.UiState.Success)?.data?.candidates.orEmpty(),
        query = (state as? com.bughunter.core.ui.util.UiState.Success)?.data?.query.orEmpty(),
        loading = state is com.bughunter.core.ui.util.UiState.Loading,
        onQueryChange = viewModel::onQueryChange,
        onAdd = viewModel::add,
        onChangeRole = viewModel::changeRole,
        onRemove = viewModel::remove,
        onDismiss = onDismiss,
    )
}

@Composable
internal fun ProjectMembersDialogContent(
    members: List<ProjectMembershipOut>,
    candidates: List<UserOut>,
    query: String,
    loading: Boolean,
    onQueryChange: (String) -> Unit,
    onAdd: (Int, ProjectRole) -> Unit,
    onChangeRole: (Int, ProjectRole) -> Unit,
    onRemove: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp,
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "Project members",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (loading) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        contentAlignment = Alignment.Center,
                    ) { CircularProgressIndicator(color = MaterialTheme.colorScheme.primary) }
                } else {
                    BhSectionHeader(text = "Add a teammate")
                    BhTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        label = "Find users",
                        placeholder = "name or email",
                    )
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 0.dp, max = 200.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        items(items = candidates, key = { it.id }) { user ->
                            CandidateRow(user = user, onAdd = onAdd)
                        }
                    }
                    BhSectionHeader(text = "Current members")
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 0.dp, max = 280.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        items(items = members, key = { it.id }) { member ->
                            MemberRow(member = member, onChangeRole = onChangeRole, onRemove = onRemove)
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    BhPrimaryButton(text = "Done", onClick = onDismiss)
                }
            }
        }
    }
}

@Composable
private fun CandidateRow(user: UserOut, onAdd: (Int, ProjectRole) -> Unit) {
    val tokens = LocalBrandTokens.current
    var role by remember { mutableStateOf(ProjectRole.MEMBER) }
    var expanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BhAvatar(displayName = user.name, userId = user.id.toString(), email = user.email, sizeDp = 28)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = user.name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(text = user.email, style = MaterialTheme.typography.labelSmall, color = tokens.textMuted)
        }
        Box {
            BhGhostButton(text = role.name.lowercase().replaceFirstChar { it.uppercase() }, onClick = { expanded = true })
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                DropdownMenuItem(text = { Text("Member") }, onClick = { role = ProjectRole.MEMBER; expanded = false })
                DropdownMenuItem(text = { Text("Lead") }, onClick = { role = ProjectRole.LEAD; expanded = false })
            }
        }
        BhPrimaryButton(text = "Add", onClick = { onAdd(user.id, role) })
    }
}

@Composable
private fun MemberRow(
    member: ProjectMembershipOut,
    onChangeRole: (Int, ProjectRole) -> Unit,
    onRemove: (Int) -> Unit,
) {
    val tokens = LocalBrandTokens.current
    var expanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BhAvatar(displayName = member.userName, userId = member.userId.toString(), email = member.userEmail, sizeDp = 28)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = member.userName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(text = member.userEmail, style = MaterialTheme.typography.labelSmall, color = tokens.textMuted)
        }
        Box {
            BhGhostButton(
                text = member.projectRole.name.lowercase().replaceFirstChar { it.uppercase() },
                onClick = { expanded = true },
            )
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                DropdownMenuItem(
                    text = { Text("Member") },
                    onClick = { onChangeRole(member.userId, ProjectRole.MEMBER); expanded = false },
                )
                DropdownMenuItem(
                    text = { Text("Lead") },
                    onClick = { onChangeRole(member.userId, ProjectRole.LEAD); expanded = false },
                )
            }
        }
        BhDangerButton(text = "Remove", onClick = { onRemove(member.userId) })
    }
}
