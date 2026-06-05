package com.bughunter.feature.projects.detail

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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bughunter.core.network.dto.ProjectMembershipOut
import com.bughunter.core.network.dto.ProjectOut
import com.bughunter.core.ui.components.BhAvatar
import com.bughunter.core.ui.components.BhCard
import com.bughunter.core.ui.components.BhGhostButton
import com.bughunter.core.ui.components.BhSectionHeader
import com.bughunter.core.ui.components.BhTopAppBar
import com.bughunter.core.ui.theme.LocalBrandTokens
import com.bughunter.core.ui.util.UiState

@Composable
internal fun ProjectDetailScreen(
    projectId: Int,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenMembers: () -> Unit,
    viewModel: ProjectDetailViewModel = hiltViewModel(),
) {
    LaunchedEffect(projectId) { viewModel.load(projectId) }
    val state by viewModel.state.collectAsState()
    ProjectDetailContent(
        state = state,
        onBack = onBack,
        onOpenSettings = onOpenSettings,
        onOpenMembers = onOpenMembers,
    )
}

@Composable
internal fun ProjectDetailScreenTestHarness(
    state: UiState<ProjectDetailModel>,
    onBack: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onOpenMembers: () -> Unit = {},
) {
    ProjectDetailContent(state, onBack, onOpenSettings, onOpenMembers)
}

@Composable
private fun ProjectDetailContent(
    state: UiState<ProjectDetailModel>,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenMembers: () -> Unit,
) {
    val tokens = LocalBrandTokens.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        BhTopAppBar(
            title = (state as? UiState.Success)?.data?.project?.name ?: "Project",
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
            ) { Text("Couldn't load project.", color = MaterialTheme.colorScheme.error) }
            UiState.Empty -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) { Text("No data.", color = tokens.textMuted) }
            is UiState.Success -> ProjectBody(
                model = state.data,
                onOpenSettings = onOpenSettings,
                onOpenMembers = onOpenMembers,
            )
        }
    }
}

@Composable
private fun ProjectBody(
    model: ProjectDetailModel,
    onOpenSettings: () -> Unit,
    onOpenMembers: () -> Unit,
) {
    val tokens = LocalBrandTokens.current
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { OverviewCard(model.project, onOpenSettings = onOpenSettings) }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BhSectionHeader(text = "Members")
                BhGhostButton(text = "Manage", onClick = onOpenMembers)
            }
        }
        if (model.members.isEmpty()) {
            item {
                Text(
                    text = "No members yet.",
                    color = tokens.textFaint,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        } else {
            items(items = model.members, key = { it.id }) { member ->
                MemberRow(member)
            }
        }
        item { BhSectionHeader(text = "Recent bugs") }
        item {
            Text(
                text = "Recent bugs appear here once items are filed.",
                style = MaterialTheme.typography.bodySmall,
                color = tokens.textMuted,
            )
        }
    }
}

@Composable
private fun OverviewCard(project: ProjectOut, onOpenSettings: () -> Unit) {
    val tokens = LocalBrandTokens.current
    BhCard {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = project.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (project.canManage) {
                    BhGhostButton(text = "Settings", onClick = onOpenSettings)
                }
            }
            if (project.key.isNotBlank()) {
                Text(
                    text = project.key,
                    style = MaterialTheme.typography.labelSmall,
                    color = tokens.textMuted,
                )
            }
            if (project.description.isNotBlank()) {
                Text(
                    text = project.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Text(
                text = "${project.memberCount} member${if (project.memberCount == 1) "" else "s"}",
                style = MaterialTheme.typography.labelSmall,
                color = tokens.textFaint,
            )
        }
    }
}

@Composable
private fun MemberRow(member: ProjectMembershipOut) {
    val tokens = LocalBrandTokens.current
    BhCard(padding = androidx.compose.foundation.layout.PaddingValues(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BhAvatar(
                displayName = member.userName,
                userId = member.userId.toString(),
                email = member.userEmail,
                sizeDp = 32,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = member.userName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = member.userEmail,
                    style = MaterialTheme.typography.labelSmall,
                    color = tokens.textMuted,
                )
            }
            Text(
                text = member.projectRole.name.lowercase().replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.labelSmall,
                color = tokens.textMuted,
            )
        }
    }
}
