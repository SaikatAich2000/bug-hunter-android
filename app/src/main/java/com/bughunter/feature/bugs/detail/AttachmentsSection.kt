package com.bughunter.feature.bugs.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bughunter.core.network.dto.AttachmentBrief
import com.bughunter.core.network.dto.Role
import com.bughunter.core.ui.components.BhAttachmentCard
import com.bughunter.core.ui.components.BhEmptyState
import com.bughunter.core.ui.components.BhSectionHeader

@Composable
internal fun AttachmentsSection(
    attachments: List<AttachmentBrief>,
    currentUserRole: Role?,
    onDownload: (Int) -> Unit,
    onDelete: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BhSectionHeader(text = "Attachments (${attachments.size})")
        if (attachments.isEmpty()) {
            BhEmptyState(title = "No attachments yet…")
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 160.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 80.dp, max = 720.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(items = attachments, key = { it.id }) { attachment ->
                    val isImage = attachment.contentType.startsWith("image/") &&
                        !attachment.contentType.contains("svg", ignoreCase = true)
                    BhAttachmentCard(
                        filename = attachment.filename,
                        sizeBytes = attachment.sizeBytes,
                        uploader = attachment.uploaderName,
                        onDownload = { onDownload(attachment.id) },
                        onDelete = { onDelete(attachment.id) },
                        previewUrl = null,
                        isImage = isImage,
                        canDelete = currentUserRole == Role.ADMIN || currentUserRole == Role.MANAGER,
                    )
                }
            }
        }
    }
}
