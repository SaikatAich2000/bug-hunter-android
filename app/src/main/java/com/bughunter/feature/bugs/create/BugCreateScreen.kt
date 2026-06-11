package com.bughunter.feature.bugs.create

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bughunter.core.network.dto.ProjectOut
import com.bughunter.core.network.dto.UserOut
import com.bughunter.core.ui.components.BhAssigneeChip
import com.bughunter.core.ui.components.BhErrorBanner
import com.bughunter.core.ui.components.BhIconButton
import com.bughunter.core.ui.components.BhPrimaryButton
import com.bughunter.core.ui.components.BhSectionHeader
import com.bughunter.core.ui.components.BhTextField
import com.bughunter.core.ui.components.BhTopAppBar
import com.bughunter.core.ui.theme.LocalBrandTokens

@Composable
internal fun BugCreateScreen(
    onBack: () -> Unit,
    onCreated: (Int) -> Unit,
    viewModel: BugCreateViewModel = hiltViewModel(),
) {
    val model by viewModel.state.collectAsState()
    LaunchedEffect(model.createdBugId) {
        val id = model.createdBugId ?: return@LaunchedEffect
        viewModel.consumeCreated()
        onCreated(id)
    }
    BugCreateContent(
        model = model,
        onBack = onBack,
        onTitleChange = viewModel::onTitleChange,
        onDescriptionChange = viewModel::onDescriptionChange,
        onProjectSelect = viewModel::onProjectSelect,
        onItemTypeSelect = viewModel::onItemTypeSelect,
        onStatusSelect = viewModel::onStatusSelect,
        onPrioritySelect = viewModel::onPrioritySelect,
        onEnvironmentSelect = viewModel::onEnvironmentSelect,
        onDueDateChange = viewModel::onDueDateChange,
        onToggleAssignee = viewModel::toggleAssignee,
        onSubmit = viewModel::submit,
    )
}

@Composable
internal fun BugCreateContent(
    model: BugCreateScreenModel,
    onBack: () -> Unit,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onProjectSelect: (Int) -> Unit,
    onItemTypeSelect: (String) -> Unit,
    onStatusSelect: (String) -> Unit,
    onPrioritySelect: (String) -> Unit,
    onEnvironmentSelect: (String) -> Unit,
    onDueDateChange: (String?) -> Unit,
    onToggleAssignee: (Int) -> Unit,
    onSubmit: () -> Unit,
) {
    Scaffold(
        topBar = {
            BhTopAppBar(
                title = "New work item",
                navigationIcon = {
                    BhIconButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        onClick = onBack,
                    )
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Form-level error banner — the v2.10 silent-fail bug
            // landed here: `model.form.error` was being set on Result2.Err
            // but no screen rendered it. CSRF 403s, server validation
            // failures, and network blips all silently no-op'd the
            // Create button. The banner is now non-negotiable on every
            // create form.
            BhErrorBanner(error = model.form.error)
            // Helper hint when the form is invalid but no server error
            // is in play — explains WHY the Create button is greyed.
            val canSubmit = model.form.canSubmit
            if (!canSubmit && model.form.error == null) {
                val hint = when {
                    model.form.projectId == null -> "Pick a project to continue."
                    model.form.title.trim().length < 3 -> "Title must be at least 3 characters."
                    model.form.title.trim().length > 200 -> "Title must be 200 characters or fewer."
                    else -> null
                }
                if (hint != null) {
                    Text(
                        text = hint,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            ProjectPicker(
                selected = model.projects.firstOrNull { it.id == model.form.projectId },
                options = model.projects,
                onSelect = onProjectSelect,
            )
            BhTextField(
                value = model.form.title,
                onValueChange = onTitleChange,
                label = "Title",
                required = true,
                helper = "3-200 characters",
                error = model.form.fieldErrors["title"],
            )
            BhTextField(
                value = model.form.description,
                onValueChange = onDescriptionChange,
                label = "Description",
                singleLine = false,
            )
            EnumPicker(
                label = "Type",
                value = model.form.itemType,
                options = listOf("Bug", "Requirement", "Task"),
                onSelect = onItemTypeSelect,
            )
            EnumPicker(
                label = "Status",
                value = model.form.status,
                options = statusesFor(model.form.itemType),
                onSelect = onStatusSelect,
            )
            EnumPicker(
                label = "Priority",
                value = model.form.priority,
                options = listOf("Low", "Medium", "High", "Critical"),
                onSelect = onPrioritySelect,
            )
            EnumPicker(
                label = "Environment",
                value = model.form.environment,
                options = listOf("DEV", "UAT", "PROD"),
                onSelect = onEnvironmentSelect,
            )
            BhTextField(
                value = model.form.dueDate.orEmpty(),
                onValueChange = { onDueDateChange(it.ifBlank { null }) },
                label = "Due date (YYYY-MM-DD)",
            )
            AssigneeMultiPicker(
                users = model.users,
                selected = model.form.assigneeIds,
                onToggle = onToggleAssignee,
            )
            BhPrimaryButton(
                text = "Create",
                onClick = onSubmit,
                enabled = model.form.canSubmit,
                loading = model.form.isSubmitting,
                modifier = Modifier.fillMaxWidth(),
                fullWidth = true,
            )
        }
    }
}

@Composable
private fun ProjectPicker(
    selected: ProjectOut?,
    options: List<ProjectOut>,
    onSelect: (Int) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val tokens = LocalBrandTokens.current
    Box {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .border(BorderStroke(1.dp, tokens.border), RoundedCornerShape(8.dp))
                .clickable { expanded = true }
                .padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            Text(
                text = "PROJECT *",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = tokens.textMuted,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = selected?.name ?: "Select project",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = Icons.Filled.ArrowDropDown,
                    contentDescription = null,
                    tint = tokens.textMuted,
                )
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { project ->
                DropdownMenuItem(
                    text = { Text(project.name) },
                    onClick = {
                        onSelect(project.id)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun EnumPicker(
    label: String,
    value: String,
    options: List<String>,
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val tokens = LocalBrandTokens.current
    Box {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .border(BorderStroke(1.dp, tokens.border), RoundedCornerShape(8.dp))
                .clickable { expanded = true }
                .padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = tokens.textMuted,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = Icons.Filled.ArrowDropDown,
                    contentDescription = null,
                    tint = tokens.textMuted,
                )
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { opt ->
                DropdownMenuItem(
                    text = { Text(opt) },
                    onClick = {
                        onSelect(opt)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun AssigneeMultiPicker(
    users: List<UserOut>,
    selected: Set<Int>,
    onToggle: (Int) -> Unit,
) {
    val tokens = LocalBrandTokens.current
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        BhSectionHeader(text = "Assignees (${selected.size})")
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            users.filter { it.id in selected }.take(4).forEach { u ->
                BhAssigneeChip(name = u.name, userId = u.id.toString())
            }
            if (selected.size > 4) {
                Text(
                    text = "+${selected.size - 4}",
                    style = MaterialTheme.typography.labelSmall,
                    color = tokens.textMuted,
                )
            }
        }
        users.forEach { u ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle(u.id) }
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (u.id in selected) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                } else {
                    Box(modifier = Modifier.widthIn(min = 24.dp))
                }
                Text(
                    text = u.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

internal fun statusesFor(itemType: String): List<String> = when (itemType) {
    "Bug" -> listOf("New", "In Progress", "Resolved", "Closed", "Reopened", "Not a Bug", "Resolve Later")
    "Requirement" -> listOf("New", "In Review", "Approved", "Implemented", "Rejected", "Deferred")
    "Task" -> listOf("New", "In Progress", "Done", "Blocked", "Cancelled")
    else -> listOf("New")
}
