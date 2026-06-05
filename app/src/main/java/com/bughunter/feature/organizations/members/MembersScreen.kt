package com.bughunter.feature.organizations.members

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bughunter.core.network.dto.UserOut
import com.bughunter.core.ui.components.BhAvatar
import com.bughunter.core.ui.components.BhCard
import com.bughunter.core.ui.components.BhEmptyState
import com.bughunter.core.ui.components.BhGhostButton
import com.bughunter.core.ui.components.BhPrimaryButton
import com.bughunter.core.ui.components.BhTextField
import com.bughunter.core.ui.components.BhTopAppBar
import com.bughunter.core.ui.theme.BhPillShape
import com.bughunter.core.ui.theme.LocalBrandTokens
import com.bughunter.core.ui.util.UiState

@Composable
internal fun MembersScreen(
    onInviteClicked: () -> Unit,
    onBack: () -> Unit,
    viewModel: MembersViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    MembersContent(
        state = state,
        onQueryChange = viewModel::onQueryChange,
        onNextPage = viewModel::nextPage,
        onPrevPage = viewModel::prevPage,
        onInvite = onInviteClicked,
        onBack = onBack,
    )
}

@Composable
internal fun MembersScreenTestHarness(
    state: UiState<MembersModel>,
    onQueryChange: (String) -> Unit = {},
    onNextPage: () -> Unit = {},
    onPrevPage: () -> Unit = {},
    onInvite: () -> Unit = {},
    onBack: () -> Unit = {},
) {
    MembersContent(state, onQueryChange, onNextPage, onPrevPage, onInvite, onBack)
}

@Composable
private fun MembersContent(
    state: UiState<MembersModel>,
    onQueryChange: (String) -> Unit,
    onNextPage: () -> Unit,
    onPrevPage: () -> Unit,
    onInvite: () -> Unit,
    onBack: () -> Unit,
) {
    val tokens = LocalBrandTokens.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        BhTopAppBar(
            title = "Members",
            navigationIcon = { BhGhostButton(text = "Back", onClick = onBack) },
        )
        when (state) {
            UiState.Loading -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator(color = MaterialTheme.colorScheme.primary) }
            is UiState.Error -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) { Text("Couldn't load members.", color = MaterialTheme.colorScheme.error) }
            UiState.Empty -> BhEmptyState(title = "No users yet — click + to add.")
            is UiState.Success -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            BhTextField(
                                value = state.data.query,
                                onValueChange = onQueryChange,
                                label = "Search members",
                                placeholder = "name or email",
                            )
                        }
                        BhPrimaryButton(text = "+ Invite teammate", onClick = onInvite)
                    }
                    val rows = state.data.pagedUsers
                    if (rows.isEmpty()) {
                        BhEmptyState(title = "No users match your search.")
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(items = rows, key = { it.id }) { user -> MemberRow(user) }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        BhGhostButton(text = "Prev", onClick = onPrevPage, enabled = state.data.page > 0)
                        Text(
                            text = "Page ${state.data.page + 1} of ${state.data.totalPages}",
                            color = tokens.textMuted,
                            style = MaterialTheme.typography.bodySmall,
                        )
                        BhGhostButton(
                            text = "Next",
                            onClick = onNextPage,
                            enabled = state.data.page < state.data.totalPages - 1,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MemberRow(user: UserOut) {
    val tokens = LocalBrandTokens.current
    BhCard(padding = androidx.compose.foundation.layout.PaddingValues(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            BhAvatar(displayName = user.name, userId = user.id.toString(), email = user.email, sizeDp = 36)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(text = user.email, style = MaterialTheme.typography.labelSmall, color = tokens.textMuted)
            }
            Box(
                modifier = Modifier
                    .clip(BhPillShape)
                    .background(tokens.accentSoft, BhPillShape)
                    .border(1.dp, tokens.border, BhPillShape)
                    .padding(horizontal = 10.dp, vertical = 3.dp),
            ) {
                Text(
                    text = user.role.name.lowercase().replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
