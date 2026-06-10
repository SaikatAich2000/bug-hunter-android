package com.bughunter.feature.organizations.invitations

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bughunter.core.network.dto.InvitationOut
import com.bughunter.core.ui.components.BhCard
import com.bughunter.core.ui.components.BhDangerButton
import com.bughunter.core.ui.components.BhEmptyState
import com.bughunter.core.ui.components.BhErrorBanner
import com.bughunter.core.ui.components.BhGhostButton
import com.bughunter.core.ui.components.BhPrimaryButton
import com.bughunter.core.ui.components.BhTopAppBar
import com.bughunter.core.ui.theme.BhPillShape
import com.bughunter.core.ui.theme.LocalBrandTokens
import com.bughunter.core.ui.util.UiState

@Composable
internal fun InvitationsScreen(
    onBack: () -> Unit,
    viewModel: InvitationsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    InvitationsContent(
        state = state,
        onBack = onBack,
        onRevoke = viewModel::revoke,
        onInvite = { email, role, ids, asLead ->
            viewModel.invite(email, role, ids, asLead) { /* no-op */ }
        },
        onRefresh = viewModel::load,
    )
}

@Composable
internal fun InvitationsScreenTestHarness(
    state: UiState<InvitationsModel>,
    onBack: () -> Unit = {},
    onRevoke: (Int) -> Unit = {},
    onInvite: (String, com.bughunter.core.network.dto.Role, List<Int>, Boolean) -> Unit = { _, _, _, _ -> },
    onRefresh: () -> Unit = {},
) {
    InvitationsContent(state, onBack, onRevoke, onInvite, onRefresh)
}

@Composable
private fun InvitationsContent(
    state: UiState<InvitationsModel>,
    onBack: () -> Unit,
    onRevoke: (Int) -> Unit,
    onInvite: (String, com.bughunter.core.network.dto.Role, List<Int>, Boolean) -> Unit,
    onRefresh: () -> Unit,
) {
    val tokens = LocalBrandTokens.current
    var showInvite by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("invitations_root"),
    ) {
        BhTopAppBar(
            title = "Invitations",
            navigationIcon = { BhGhostButton(text = "Back", onClick = onBack) },
            actions = { BhGhostButton(text = "Refresh", onClick = onRefresh) },
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                BhPrimaryButton(
                    text = "+ Invite teammate",
                    onClick = { showInvite = true },
                )
            }
            when (state) {
                UiState.Loading -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator(color = MaterialTheme.colorScheme.primary) }
                is UiState.Error -> Text(
                    "Couldn't load invitations.",
                    color = MaterialTheme.colorScheme.error,
                )
                UiState.Empty -> BhEmptyState(
                    title = "No invitations yet.",
                    helper = "Click + Invite a teammate to send one.",
                )
                is UiState.Success -> {
                    BhErrorBanner(error = state.data.actionError)
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(items = state.data.invitations, key = { it.id }) { item ->
                            InvitationRow(
                                item = item,
                                status = state.data.displayStatus(item),
                                onRevoke = { onRevoke(item.id) },
                            )
                        }
                    }
                }
            }
        }
    }

    if (showInvite) {
        InviteTeammateDialog(
            onDismiss = { showInvite = false },
            onSubmit = { email, role, projectIds, asLead ->
                onInvite(email, role, projectIds, asLead)
                showInvite = false
            },
        )
    }
}

@Composable
private fun InvitationRow(
    item: InvitationOut,
    status: String,
    onRevoke: () -> Unit,
) {
    val tokens = LocalBrandTokens.current
    val pair = tokens.inviteStatusColors[status]
    val (bg, fg) = pair ?: (MaterialTheme.colorScheme.surfaceVariant to tokens.textMuted)
    BhCard(padding = androidx.compose.foundation.layout.PaddingValues(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.email,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Invited by ${item.invitedByName}",
                    style = MaterialTheme.typography.labelSmall,
                    color = tokens.textMuted,
                )
            }
            Box(
                modifier = Modifier
                    .clip(BhPillShape)
                    .background(bg, BhPillShape)
                    .border(1.dp, fg.copy(alpha = 0.4f), BhPillShape)
                    .padding(horizontal = 10.dp, vertical = 3.dp),
            ) {
                Text(
                    text = status.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.labelSmall,
                    color = fg,
                )
            }
            if (status == "pending") {
                BhDangerButton(text = "Revoke", onClick = onRevoke)
            }
        }
    }
}
