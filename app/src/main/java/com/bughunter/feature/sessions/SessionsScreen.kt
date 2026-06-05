package com.bughunter.feature.sessions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bughunter.core.network.dto.SessionOut
import com.bughunter.core.ui.components.BhAvatar
import com.bughunter.core.ui.components.BhCard
import com.bughunter.core.ui.components.BhConfirmDialog
import com.bughunter.core.ui.components.BhDangerButton
import com.bughunter.core.ui.components.BhEmptyState
import com.bughunter.core.ui.components.BhIconButton
import com.bughunter.core.ui.theme.BhPillShape
import com.bughunter.core.ui.theme.LocalBrandTokens
import com.bughunter.core.ui.util.UiState
import com.bughunter.core.ui.util.formatRelative

@Composable
internal fun SessionsScreen(
    viewModel: SessionsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    SessionsContent(
        state = state,
        onRefresh = viewModel::refresh,
        onRevokeRequest = viewModel::requestRevoke,
        onRevokeConfirm = viewModel::confirmRevoke,
        onRevokeDismiss = viewModel::dismissRevoke,
    )
}

@Composable
internal fun SessionsTestHarness(
    state: SessionsUiState,
    onRefresh: () -> Unit = {},
    onRevokeRequest: (Int) -> Unit = {},
    onRevokeConfirm: () -> Unit = {},
    onRevokeDismiss: () -> Unit = {},
) {
    SessionsContent(
        state = state,
        onRefresh = onRefresh,
        onRevokeRequest = onRevokeRequest,
        onRevokeConfirm = onRevokeConfirm,
        onRevokeDismiss = onRevokeDismiss,
    )
}

@Composable
private fun SessionsContent(
    state: SessionsUiState,
    onRefresh: () -> Unit,
    onRevokeRequest: (Int) -> Unit,
    onRevokeConfirm: () -> Unit,
    onRevokeDismiss: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "Active sessions",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            BhIconButton(
                icon = Icons.Outlined.Refresh,
                contentDescription = "Refresh",
                onClick = onRefresh,
            )
        }
        Text(
            text = "Admin view — every active sign-in in your organisation. Revoke any device to force a re-login.",
            style = MaterialTheme.typography.bodyMedium,
            color = LocalBrandTokens.current.textMuted,
        )
        when (val list = state.list) {
            UiState.Loading -> Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) { CircularProgressIndicator() }
            UiState.Empty -> BhEmptyState(
                title = "No active sessions.",
                icon = Icons.Outlined.Lock,
            )
            is UiState.Error -> Text(
                text = "Couldn't load sessions.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
            is UiState.Success -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(items = list.data, key = { it.id }) { session ->
                    SessionRow(
                        session = session,
                        revoking = state.revokingId == session.id,
                        onRevoke = { onRevokeRequest(session.id) },
                    )
                }
            }
        }
    }
    if (state.pendingRevokeId != null) {
        BhConfirmDialog(
            title = "Revoke session?",
            message = "The signed-in device will be logged out on its next request.",
            confirmLabel = "Revoke",
            onConfirm = onRevokeConfirm,
            onDismiss = onRevokeDismiss,
            danger = true,
        )
    }
}

@Composable
private fun SessionRow(
    session: SessionOut,
    revoking: Boolean,
    onRevoke: () -> Unit,
) {
    val tokens = LocalBrandTokens.current
    BhCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            BhAvatar(
                displayName = session.userName.orEmpty().ifBlank { session.userEmail.orEmpty() },
                userId = session.userId.toString(),
                sizeDp = 36,
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = session.userName ?: session.userEmail.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (session.isCurrent) {
                        CurrentFlag()
                    }
                }
                Text(
                    text = session.userEmail.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = tokens.textMuted,
                )
                Text(
                    text = session.userAgent,
                    style = MaterialTheme.typography.labelSmall,
                    color = tokens.textFaint,
                    maxLines = 2,
                )
                Text(
                    text = "${session.ipAddress} · last seen ${session.lastSeenAt.formatRelative()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = tokens.textFaint,
                )
            }
            BhDangerButton(
                text = "Revoke",
                onClick = onRevoke,
                enabled = !session.isCurrent,
                loading = revoking,
            )
        }
    }
}

@Composable
private fun CurrentFlag() {
    Box(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.primary, BhPillShape)
            .padding(horizontal = 8.dp, vertical = 2.dp),
    ) {
        Text(
            text = "Current",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimary,
        )
    }
}
