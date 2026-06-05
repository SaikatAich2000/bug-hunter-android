package com.bughunter.feature.bugs.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bughunter.core.network.dto.BugDetail
import com.bughunter.core.ui.components.BhAssigneeChip
import com.bughunter.core.ui.components.BhCard
import com.bughunter.core.ui.theme.LocalBrandTokens
import com.bughunter.core.ui.util.formatShortDateTime

@Composable
internal fun MetaSidePanel(
    bug: BugDetail,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalBrandTokens.current
    BhCard(modifier = modifier.fillMaxWidth(), padding = PaddingValues(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            MetaRow(label = "Project", value = bug.projectName ?: "—")
            MetaRow(label = "Type", value = bug.itemType)
            MetaRow(label = "Reporter", value = bug.reporter?.name ?: "—")
            Column {
                Text(
                    text = "Assignees",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = tokens.textMuted,
                )
                if (bug.assignees.isEmpty()) {
                    Text(
                        text = "Unassigned",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                } else {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        bug.assignees.take(3).forEach { a ->
                            BhAssigneeChip(name = a.name, userId = a.id.toString())
                        }
                        if (bug.assignees.size > 3) {
                            Text(
                                text = "+${bug.assignees.size - 3}",
                                style = MaterialTheme.typography.labelSmall,
                                color = tokens.textMuted,
                            )
                        }
                    }
                }
            }
            MetaRow(label = "Due date", value = bug.dueDate ?: "—")
            MetaRow(label = "Event", value = bug.eventName ?: "—")
            MetaRow(label = "Created", value = bug.createdAt.formatShortDateTime())
            MetaRow(label = "Updated", value = bug.updatedAt.formatShortDateTime())
        }
    }
}

@Composable
private fun MetaRow(label: String, value: String) {
    val tokens = LocalBrandTokens.current
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = tokens.textMuted,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
