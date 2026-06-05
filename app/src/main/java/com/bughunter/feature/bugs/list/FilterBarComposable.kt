package com.bughunter.feature.bugs.list

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bughunter.core.network.dto.ProjectOut
import com.bughunter.core.network.dto.UserOut
import com.bughunter.core.ui.components.BhGhostButton
import com.bughunter.core.ui.theme.LocalBrandTokens

internal data class FilterOption(val key: String, val label: String)

@Composable
internal fun FilterBar(
    filters: BugListFilters,
    projects: List<ProjectOut>,
    users: List<UserOut>,
    onToggleProject: (Int) -> Unit,
    onToggleStatus: (String) -> Unit,
    onTogglePriority: (String) -> Unit,
    onToggleEnvironment: (String) -> Unit,
    onToggleItemType: (String) -> Unit,
    onToggleAssignee: (Int) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MultiSelectDropdown(
            label = "Project",
            options = projects.map { FilterOption(it.id.toString(), it.name) },
            selectedKeys = filters.projectIds.map { it.toString() }.toSet(),
            onToggle = { key -> onToggleProject(key.toInt()) },
        )
        MultiSelectDropdown(
            label = "Status",
            options = STATUS_OPTIONS,
            selectedKeys = filters.statuses,
            onToggle = onToggleStatus,
        )
        MultiSelectDropdown(
            label = "Priority",
            options = PRIORITY_OPTIONS,
            selectedKeys = filters.priorities,
            onToggle = onTogglePriority,
        )
        MultiSelectDropdown(
            label = "Severity",
            options = PRIORITY_OPTIONS,
            selectedKeys = filters.priorities,
            onToggle = onTogglePriority,
        )
        MultiSelectDropdown(
            label = "Environment",
            options = ENV_OPTIONS,
            selectedKeys = filters.environments,
            onToggle = onToggleEnvironment,
        )
        MultiSelectDropdown(
            label = "Assignee",
            options = users.map { FilterOption(it.id.toString(), it.name) },
            selectedKeys = filters.assigneeIds.map { it.toString() }.toSet(),
            onToggle = { key -> onToggleAssignee(key.toInt()) },
        )
        if (filters.hasActiveFilters) {
            BhGhostButton(text = "Clear", onClick = onClear)
        }
    }
}

@Composable
private fun MultiSelectDropdown(
    label: String,
    options: List<FilterOption>,
    selectedKeys: Set<String>,
    onToggle: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val tokens = LocalBrandTokens.current
    val summary = when (selectedKeys.size) {
        0 -> "All"
        1 -> options.firstOrNull { it.key in selectedKeys }?.label ?: "1 selected"
        else -> "${selectedKeys.size} selected"
    }
    Box {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .border(BorderStroke(1.dp, tokens.border), RoundedCornerShape(8.dp))
                .clickable { expanded = true }
                .widthIn(min = 140.dp)
                .heightIn(min = 44.dp)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = tokens.textMuted,
                )
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                imageVector = Icons.Filled.ArrowDropDown,
                contentDescription = null,
                tint = tokens.textMuted,
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            if (options.isEmpty()) {
                DropdownMenuItem(
                    text = { Text("No options", color = tokens.textMuted) },
                    onClick = { expanded = false },
                )
            }
            options.forEach { option ->
                val selected = option.key in selectedKeys
                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            if (selected) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            } else {
                                Box(modifier = Modifier.padding(start = 20.dp))
                            }
                            Text(option.label)
                        }
                    },
                    onClick = { onToggle(option.key) },
                )
            }
        }
    }
}

internal val STATUS_OPTIONS: List<FilterOption> = listOf(
    FilterOption("New", "New"),
    FilterOption("In Progress", "In Progress"),
    FilterOption("Resolved", "Resolved"),
    FilterOption("Closed", "Closed"),
    FilterOption("Reopened", "Reopened"),
    FilterOption("Not a Bug", "Not a Bug"),
    FilterOption("Resolve Later", "Resolve Later"),
)

internal val PRIORITY_OPTIONS: List<FilterOption> = listOf(
    FilterOption("Low", "Low"),
    FilterOption("Medium", "Medium"),
    FilterOption("High", "High"),
    FilterOption("Critical", "Critical"),
)

internal val ENV_OPTIONS: List<FilterOption> = listOf(
    FilterOption("DEV", "DEV"),
    FilterOption("UAT", "UAT"),
    FilterOption("PROD", "PROD"),
)
