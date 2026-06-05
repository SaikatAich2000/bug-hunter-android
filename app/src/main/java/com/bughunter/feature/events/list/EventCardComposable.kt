package com.bughunter.feature.events.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bughunter.core.network.dto.EventOut
import com.bughunter.core.ui.components.BhAssigneeChip
import com.bughunter.core.ui.components.BhCard
import com.bughunter.core.ui.theme.LocalBrandTokens

@Composable
internal fun EventCardComposable(
    event: EventOut,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalBrandTokens.current
    BhCard(modifier = modifier.fillMaxWidth(), onClick = onClick) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Event,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = event.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
            if (!event.scheduledFor.isNullOrBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CalendarMonth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = event.scheduledFor,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Text(
                text = "${event.itemCount} item${if (event.itemCount == 1) "" else "s"}",
                style = MaterialTheme.typography.bodySmall,
                color = tokens.textMuted,
            )
            if (event.managers.isNotEmpty()) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    event.managers.take(MAX_MANAGER_CHIPS).forEach { manager ->
                        BhAssigneeChip(
                            name = manager.name,
                            userId = manager.id.toString(),
                        )
                    }
                    val overflow = event.managers.size - MAX_MANAGER_CHIPS
                    if (overflow > 0) {
                        Text(
                            text = "+$overflow",
                            style = MaterialTheme.typography.labelSmall,
                            color = tokens.textMuted,
                            modifier = Modifier.padding(start = 4.dp),
                        )
                    }
                }
            }
        }
    }
}

private const val MAX_MANAGER_CHIPS = 4
