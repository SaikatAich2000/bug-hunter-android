package com.bughunter.feature.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bughunter.core.ui.theme.BhPillShape
import com.bughunter.core.ui.theme.LocalBrandTokens

internal data class TypeTabItem(
    val tab: DashboardTypeTab,
    val label: String,
    val count: Int,
)

@Composable
internal fun TypeTabsComposable(
    tabs: List<TypeTabItem>,
    selected: DashboardTypeTab,
    onSelect: (DashboardTypeTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalBrandTokens.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        tabs.forEach { item ->
            val isActive = item.tab == selected
            Row(
                modifier = Modifier
                    .clickable { onSelect(item.tab) }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = item.label,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Medium,
                    ),
                    color = if (isActive) MaterialTheme.colorScheme.onSurface else tokens.textMuted,
                )
                Box(
                    modifier = Modifier
                        .clip(BhPillShape)
                        .background(
                            if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            BhPillShape,
                        )
                        .border(BorderStroke(1.dp, tokens.borderSoft), BhPillShape)
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = item.count.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isActive) Color.White else tokens.textMuted,
                    )
                }
            }
        }
    }
}

internal fun buildTypeTabs(stats: StatsView): List<TypeTabItem> {
    val totalAll = stats.byType.values.sum().takeIf { it > 0 } ?: stats.totals.total
    return listOf(
        TypeTabItem(DashboardTypeTab.ALL, "All", totalAll),
        TypeTabItem(DashboardTypeTab.BUGS, "Bugs", stats.byType["Bug"] ?: 0),
        TypeTabItem(DashboardTypeTab.REQUIREMENTS, "Requirements", stats.byType["Requirement"] ?: 0),
        TypeTabItem(DashboardTypeTab.TASKS, "Tasks", stats.byType["Task"] ?: 0),
    )
}
