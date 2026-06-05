package com.bughunter.feature.bugs.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bughunter.core.ui.components.BhIconButton
import com.bughunter.core.ui.components.BhPrimaryButton
import com.bughunter.core.ui.components.BhTextField

@Composable
internal fun CommentComposer(
    value: String,
    isPosting: Boolean,
    onValueChange: (String) -> Unit,
    onPost: () -> Unit,
    onAttach: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BhTextField(
            value = value,
            onValueChange = onValueChange,
            label = "Write a comment",
            singleLine = false,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BhIconButton(
                icon = Icons.Filled.AttachFile,
                contentDescription = "Attach files",
                onClick = onAttach,
            )
            BhPrimaryButton(
                text = "Post comment",
                onClick = onPost,
                enabled = value.isNotBlank() && !isPosting,
                loading = isPosting,
            )
        }
    }
}
