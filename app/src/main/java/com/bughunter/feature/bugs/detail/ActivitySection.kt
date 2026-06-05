package com.bughunter.feature.bugs.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bughunter.core.network.dto.ActivityOut
import com.bughunter.core.ui.components.BhEmptyState
import com.bughunter.core.ui.components.BhSectionHeader
import com.bughunter.core.ui.theme.LocalBrandTokens
import com.bughunter.core.ui.util.formatRelative

@Composable
internal fun ActivitySection(
    activities: List<ActivityOut>,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalBrandTokens.current
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        BhSectionHeader(text = "Activity")
        if (activities.isEmpty()) {
            BhEmptyState(title = "No activity yet.")
        } else {
            activities.forEach { activity ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = activity.actorName,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${activity.action} ${activity.detail}".trim(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = activity.createdAt.formatRelative(),
                            style = MaterialTheme.typography.labelSmall,
                            color = tokens.textMuted,
                        )
                    }
                }
            }
        }
    }
}
