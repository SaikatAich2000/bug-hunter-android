package com.bughunter.feature.bugs.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bughunter.core.network.dto.BugOut
import com.bughunter.core.ui.components.BhAssigneeChip
import com.bughunter.core.ui.components.BhBadge
import com.bughunter.core.ui.components.BhBadgeKind
import com.bughunter.core.ui.components.BhCard
import com.bughunter.core.ui.theme.LocalBrandTokens

@Composable
internal fun BugRowCard(
    bug: BugOut,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BhCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        padding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BugIdBadge(bug)
                Text(
                    text = bug.title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BhBadge(label = bug.status, kind = statusKind(bug.status))
                BhBadge(label = bug.priority, kind = priorityKind(bug.priority))
                if (bug.itemType != "Requirement" && bug.itemType != "Task") {
                    BhBadge(label = bug.environment, kind = envKind(bug.environment))
                }
            }
            if (bug.assignees.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    bug.assignees.take(3).forEach { assignee ->
                        BhAssigneeChip(
                            name = assignee.name,
                            userId = assignee.id.toString(),
                        )
                    }
                    val extra = bug.assignees.size - 3
                    if (extra > 0) {
                        Text(
                            text = "+$extra",
                            style = MaterialTheme.typography.labelSmall,
                            color = LocalBrandTokens.current.textMuted,
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun BugRowTablet(
    bug: BugOut,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.width(96.dp)) { BugIdBadge(bug) }
        Text(
            text = bug.title,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(2f),
        )
        Text(
            text = bug.projectName ?: "—",
            style = MaterialTheme.typography.bodySmall,
            color = LocalBrandTokens.current.textMuted,
            modifier = Modifier.widthIn(min = 100.dp).weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        BhBadge(label = bug.status, kind = statusKind(bug.status))
        BhBadge(label = bug.priority, kind = priorityKind(bug.priority))
        BhBadge(label = bug.environment, kind = envKind(bug.environment))
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.weight(1f),
        ) {
            bug.assignees.take(2).forEach { user ->
                BhAssigneeChip(name = user.name, userId = user.id.toString())
            }
            val extra = bug.assignees.size - 2
            if (extra > 0) {
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "+$extra",
                    style = MaterialTheme.typography.labelSmall,
                    color = LocalBrandTokens.current.textMuted,
                )
            }
        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp),
    )
}

@Composable
private fun BugIdBadge(bug: BugOut) {
    val key = bug.projectKey?.takeIf { it.isNotBlank() }
    val label = if (key != null) "$key-${bug.id}" else "#${bug.id}"
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.primary,
    )
}

internal fun statusKind(value: String): BhBadgeKind = when (value.lowercase()) {
    "new" -> BhBadgeKind.STATUS_NEW
    "in progress" -> BhBadgeKind.STATUS_INPROGRESS
    "resolved" -> BhBadgeKind.STATUS_RESOLVED
    "closed" -> BhBadgeKind.STATUS_CLOSED
    "reopened" -> BhBadgeKind.STATUS_REOPENED
    else -> BhBadgeKind.NEUTRAL
}

internal fun priorityKind(value: String): BhBadgeKind = when (value.lowercase()) {
    "low" -> BhBadgeKind.PRIORITY_LOW
    "medium" -> BhBadgeKind.PRIORITY_MED
    "high" -> BhBadgeKind.PRIORITY_HIGH
    "critical" -> BhBadgeKind.PRIORITY_CRITICAL
    else -> BhBadgeKind.NEUTRAL
}

internal fun envKind(value: String): BhBadgeKind = when (value.uppercase()) {
    "DEV" -> BhBadgeKind.ENV_DEV
    "UAT" -> BhBadgeKind.ENV_STAGING
    "PROD" -> BhBadgeKind.ENV_PROD
    else -> BhBadgeKind.NEUTRAL
}
