package com.bughunter.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.bughunter.core.ui.theme.LocalBrandTokens
import com.bughunter.core.ui.util.formatBytes

@Composable
fun BhAttachmentCard(
    filename: String,
    sizeBytes: Long,
    uploader: String,
    onDownload: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    previewUrl: String? = null,
    isImage: Boolean = false,
    canDelete: Boolean = true,
) {
    val tokens = LocalBrandTokens.current
    BhCard(modifier = modifier, padding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center,
            ) {
                if (isImage && previewUrl != null) {
                    AsyncImage(
                        model = previewUrl,
                        contentDescription = filename,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.InsertDriveFile,
                        contentDescription = null,
                        tint = tokens.textMuted,
                        modifier = Modifier.size(38.dp),
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.outline),
            )
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = filename,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${sizeBytes.formatBytes()} • $uploader",
                    style = MaterialTheme.typography.bodySmall,
                    color = tokens.textMuted,
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.outline),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BhIconButton(
                    icon = Icons.Filled.FileDownload,
                    contentDescription = "Download",
                    onClick = onDownload,
                )
                if (canDelete) {
                    BhIconButton(
                        icon = Icons.Filled.Delete,
                        contentDescription = "Delete",
                        onClick = onDelete,
                        danger = true,
                    )
                } else {
                    Box(modifier = Modifier.width(30.dp))
                }
            }
        }
    }
}
