package com.bughunter.feature.bugs.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bughunter.core.ui.components.BhGhostButton
import com.bughunter.core.ui.theme.LocalBrandTokens

@Composable
internal fun PaginationFooter(
    pageLabel: String,
    hasMore: Boolean,
    isLoading: Boolean,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalBrandTokens.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = pageLabel,
            style = MaterialTheme.typography.bodySmall,
            color = tokens.textMuted,
        )
        if (isLoading) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 6.dp),
            )
        } else if (hasMore) {
            BhGhostButton(text = "Load more", onClick = onLoadMore)
        }
    }
}
