package com.bughunter.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bughunter.core.ui.theme.BhPillShape

@Composable
fun BhAssigneeChip(
    name: String,
    userId: String,
    modifier: Modifier = Modifier,
    avatarUrl: String? = null,
) {
    Row(
        modifier = modifier
            .clip(BhPillShape)
            .background(MaterialTheme.colorScheme.surfaceVariant, BhPillShape)
            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), BhPillShape)
            .padding(start = 4.dp, end = 8.dp, top = 2.dp, bottom = 2.dp)
            .widthIn(max = 200.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        BhAvatar(
            displayName = name,
            userId = userId,
            avatarUrl = avatarUrl,
            sizeDp = 22,
        )
        Text(
            text = name,
            style = MaterialTheme.typography.labelSmall.copy(
                color = MaterialTheme.colorScheme.onSurface,
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
