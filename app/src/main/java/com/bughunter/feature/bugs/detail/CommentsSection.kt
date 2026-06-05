package com.bughunter.feature.bugs.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bughunter.core.network.dto.CommentOut
import com.bughunter.core.network.dto.Role
import com.bughunter.core.ui.components.BhAvatar
import com.bughunter.core.ui.components.BhEmptyState
import com.bughunter.core.ui.components.BhIconButton
import com.bughunter.core.ui.components.BhRichHtml
import com.bughunter.core.ui.components.BhSectionHeader
import com.bughunter.core.ui.theme.LocalBrandTokens
import com.bughunter.core.ui.util.formatRelative

@Composable
internal fun CommentsSection(
    comments: List<CommentOut>,
    currentUserRole: Role?,
    onDelete: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        BhSectionHeader(text = "Comments (${comments.size})")
        if (comments.isEmpty()) {
            BhEmptyState(title = "No comments yet.")
        } else {
            comments.forEach { comment ->
                CommentRow(
                    comment = comment,
                    canDelete = currentUserRole == Role.ADMIN,
                    onDelete = { onDelete(comment.id) },
                )
            }
        }
    }
}

@Composable
private fun CommentRow(
    comment: CommentOut,
    canDelete: Boolean,
    onDelete: () -> Unit,
) {
    val tokens = LocalBrandTokens.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .border(BorderStroke(1.dp, tokens.border), RoundedCornerShape(10.dp))
            .padding(PaddingValues(horizontal = 12.dp, vertical = 10.dp)),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            BhAvatar(
                displayName = comment.authorName,
                userId = comment.authorUserId?.toString() ?: comment.authorName,
                sizeDp = 24,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = comment.authorName,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = comment.createdAt.formatRelative(),
                    style = MaterialTheme.typography.labelSmall,
                    color = tokens.textMuted,
                )
            }
            if (canDelete) {
                BhIconButton(
                    icon = Icons.Filled.Delete,
                    contentDescription = "Delete",
                    onClick = onDelete,
                    danger = true,
                )
            }
        }
        BhRichHtml(html = comment.body)
    }
}
