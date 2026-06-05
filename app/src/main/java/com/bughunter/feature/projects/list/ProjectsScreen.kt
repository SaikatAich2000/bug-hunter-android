package com.bughunter.feature.projects.list

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bughunter.core.network.dto.ProjectOut
import com.bughunter.core.ui.components.BhCard
import com.bughunter.core.ui.components.BhEmptyState
import com.bughunter.core.ui.components.BhPrimaryButton
import com.bughunter.core.ui.components.BhTextField
import com.bughunter.core.ui.theme.LocalBrandTokens
import com.bughunter.core.ui.util.UiState

@Composable
internal fun ProjectsScreen(
    onProjectClick: (Int) -> Unit,
    viewModel: ProjectsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val query by viewModel.query.collectAsState()
    ProjectsContent(
        state = state,
        query = query,
        onQueryChange = viewModel::onQueryChange,
        onProjectClick = onProjectClick,
        onCreate = { n, k, c, d -> viewModel.createProject(n, k, c, d) { /* no-op */ } },
    )
}

@Composable
internal fun ProjectsScreenTestHarness(
    state: UiState<ProjectsListModel>,
    query: String = "",
    onQueryChange: (String) -> Unit = {},
    onProjectClick: (Int) -> Unit = {},
    onCreate: (String, String?, String, String) -> Unit = { _, _, _, _ -> },
) {
    ProjectsContent(state, query, onQueryChange, onProjectClick, onCreate)
}

@Composable
private fun ProjectsContent(
    state: UiState<ProjectsListModel>,
    query: String,
    onQueryChange: (String) -> Unit,
    onProjectClick: (Int) -> Unit,
    onCreate: (String, String?, String, String) -> Unit,
) {
    var showCreate by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
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
                        value = query,
                        onValueChange = onQueryChange,
                        label = "Search projects",
                        placeholder = "name or key",
                    )
                }
                BhPrimaryButton(text = "+ New", onClick = { showCreate = true })
            }
            when (state) {
                is UiState.Loading -> Box(
                    modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator(color = MaterialTheme.colorScheme.primary) }
                is UiState.Error -> Text(
                    text = "Couldn't load projects.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
                UiState.Empty -> BhEmptyState(
                    title = "No projects yet.",
                    helper = "Click + New project to create one.",
                )
                is UiState.Success -> {
                    val list = state.data.filtered
                    if (list.isEmpty()) {
                        BhEmptyState(title = "No projects match your search.")
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            items(items = list, key = { it.id }) { project ->
                                ProjectRow(
                                    project = project,
                                    onClick = { onProjectClick(project.id) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreate) {
        ProjectCreateDialog(
            onDismiss = { showCreate = false },
            onSubmit = { name, key, color, description ->
                onCreate(name, key, color, description)
                showCreate = false
            },
        )
    }
}

@Composable
private fun ProjectRow(project: ProjectOut, onClick: () -> Unit) {
    val tokens = LocalBrandTokens.current
    BhCard(onClick = onClick, padding = androidx.compose.foundation.layout.PaddingValues(14.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val swatch = parseHex(project.color)
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(swatch),
            )
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = project.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (project.key.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .clip(MaterialTheme.shapes.extraSmall)
                                .background(tokens.borderSoft)
                                .border(1.dp, tokens.border, MaterialTheme.shapes.extraSmall)
                                .padding(horizontal = 6.dp, vertical = 1.dp),
                        ) {
                            Text(
                                text = project.key,
                                style = MaterialTheme.typography.labelSmall,
                                color = tokens.textMuted,
                            )
                        }
                    }
                }
                if (project.description.isNotBlank()) {
                    Text(
                        text = project.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = tokens.textMuted,
                        maxLines = 2,
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
}

private fun parseHex(hex: String): Color = try {
    val trimmed = hex.removePrefix("#")
    val v = trimmed.toLong(16)
    if (trimmed.length == 6) Color(0xFF000000 or v) else Color(v)
} catch (e: NumberFormatException) {
    Color.Gray
}
