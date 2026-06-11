package com.bughunter.feature.bugs.list

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import com.bughunter.core.network.DomainError
import com.bughunter.core.network.dto.BugOut
import com.bughunter.core.network.dto.ProjectOut
import com.bughunter.core.network.dto.UserOut
import com.bughunter.core.ui.components.BhEmptyState
import com.bughunter.core.ui.components.BhPrimaryButton
import com.bughunter.core.ui.components.BhShimmerList
import com.bughunter.core.ui.components.BhTextField
import com.bughunter.core.ui.theme.LocalBrandTokens
import com.bughunter.core.ui.util.UiState

@Composable
internal fun BugListScreen(
    onOpenBug: (Int) -> Unit,
    onCreateBug: () -> Unit,
    viewModel: BugListViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    BugListContent(
        state = state,
        onOpenBug = onOpenBug,
        onCreateBug = onCreateBug,
        onQueryChange = viewModel::onQueryChange,
        onToggleProject = viewModel::toggleProject,
        onToggleStatus = viewModel::toggleStatus,
        onTogglePriority = viewModel::togglePriority,
        onToggleEnvironment = viewModel::toggleEnvironment,
        onToggleItemType = viewModel::toggleItemType,
        onSetOnlyItemType = viewModel::setOnlyItemType,
        onToggleAssignee = viewModel::toggleAssignee,
        onClearFilters = viewModel::clearFilters,
        onLoadMore = viewModel::loadMore,
        onRetry = viewModel::refresh,
    )
}

@Composable
internal fun BugListContent(
    state: UiState<BugListScreenModel>,
    onOpenBug: (Int) -> Unit,
    onCreateBug: () -> Unit,
    onQueryChange: (String) -> Unit,
    onToggleProject: (Int) -> Unit,
    onToggleStatus: (String) -> Unit,
    onTogglePriority: (String) -> Unit,
    onToggleEnvironment: (String) -> Unit,
    onToggleItemType: (String) -> Unit,
    onToggleAssignee: (Int) -> Unit,
    onClearFilters: () -> Unit,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit,
    onSetOnlyItemType: (String?) -> Unit = {},
) {
    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreateBug,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("New") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            when (state) {
                UiState.Loading -> CenteredLoader()
                UiState.Empty -> BhEmptyState(
                    title = "No items match your filters",
                    modifier = Modifier.padding(24.dp),
                )
                is UiState.Error -> ErrorPanel(error = state.error, onRetry = onRetry)
                is UiState.Success -> BugListBody(
                    model = state.data,
                    onOpenBug = onOpenBug,
                    onQueryChange = onQueryChange,
                    onToggleProject = onToggleProject,
                    onToggleStatus = onToggleStatus,
                    onTogglePriority = onTogglePriority,
                    onToggleEnvironment = onToggleEnvironment,
                    onToggleItemType = onToggleItemType,
                    onSetOnlyItemType = onSetOnlyItemType,
                    onToggleAssignee = onToggleAssignee,
                    onClearFilters = onClearFilters,
                    onLoadMore = onLoadMore,
                )
            }
        }
    }
}

@Composable
private fun BugListBody(
    model: BugListScreenModel,
    onOpenBug: (Int) -> Unit,
    onQueryChange: (String) -> Unit,
    onToggleProject: (Int) -> Unit,
    onToggleStatus: (String) -> Unit,
    onTogglePriority: (String) -> Unit,
    onToggleEnvironment: (String) -> Unit,
    onToggleItemType: (String) -> Unit,
    onSetOnlyItemType: (String?) -> Unit,
    onToggleAssignee: (Int) -> Unit,
    onClearFilters: () -> Unit,
    onLoadMore: () -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val tablet = maxWidth >= 720.dp
        Column(modifier = Modifier.fillMaxSize()) {
            BugTypeTabsRow(
                selected = model.filters.itemTypes.singleOrNull(),
                onSelect = onSetOnlyItemType,
            )
            Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                BhTextField(
                    value = model.filters.query.orEmpty(),
                    onValueChange = onQueryChange,
                    label = "Search title, description, or #id",
                )
            }
            FilterBar(
                filters = model.filters,
                projects = model.projects,
                users = model.users,
                onToggleProject = onToggleProject,
                onToggleStatus = onToggleStatus,
                onTogglePriority = onTogglePriority,
                onToggleEnvironment = onToggleEnvironment,
                onToggleItemType = onToggleItemType,
                onToggleAssignee = onToggleAssignee,
                onClear = onClearFilters,
            )
            if (tablet) {
                BugListTableHeader()
            }
            val items = model.allItems
            if (items.isEmpty()) {
                BhEmptyState(
                    title = "No items match your filters",
                    helper = "Adjust filters or clear them to see more.",
                    icon = Icons.Filled.BugReport,
                    modifier = Modifier.padding(24.dp),
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(items = items, key = { it.id }) { bug ->
                        // animateItem(): rows fade in on first appearance and
                        // glide (rather than jump) when filtering/sorting
                        // reorders the list. Keyed items make this stable.
                        val rowModifier = Modifier.animateItem()
                        if (tablet) {
                            BugRowTablet(bug = bug, onClick = { onOpenBug(bug.id) }, modifier = rowModifier)
                        } else {
                            BugRowCard(bug = bug, onClick = { onOpenBug(bug.id) }, modifier = rowModifier)
                        }
                    }
                    item {
                        val pageLabel = "Page ${model.pages.lastOrNull()?.page ?: 1} of " +
                            "${model.pages.lastOrNull()?.totalPages ?: 1} (${model.pages.lastOrNull()?.total ?: items.size} items)"
                        PaginationFooter(
                            pageLabel = pageLabel,
                            hasMore = model.hasMore,
                            isLoading = model.isLoadingMore,
                            onLoadMore = onLoadMore,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BugTypeTabsRow(
    selected: String?,
    onSelect: (String?) -> Unit,
) {
    val tabs = listOf(
        null to "All",
        "Bug" to "Bugs",
        "Requirement" to "Requirements",
        "Task" to "Tasks",
    )
    val tokens = LocalBrandTokens.current
    val shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        tabs.forEach { (key, label) ->
            val isActive = key == selected
            Box(
                modifier = Modifier
                    .clip(shape)
                    .background(
                        color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        shape = shape,
                    )
                    .clickable(onClick = { onSelect(key) })
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isActive) MaterialTheme.colorScheme.onPrimary else tokens.textMuted,
                    fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun BugListTableHeader() {
    val tokens = LocalBrandTokens.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(BorderStroke(1.dp, tokens.border))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("ID", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), modifier = Modifier.padding(end = 84.dp))
        Text("Title", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), modifier = Modifier.weight(2f))
        Text("Project", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), modifier = Modifier.weight(1f))
        Text("Status", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
        Text("Priority", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
        Text("Env", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
        Text("Assignees", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), modifier = Modifier.weight(1f))
    }
}

@Composable
private fun CenteredLoader() {
    // Skeleton rows instead of a bare spinner: the page keeps its list
    // layout while loading so the content landing doesn't cause a jump.
    BhShimmerList(
        count = 8,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 12.dp),
    )
}

@Composable
private fun ErrorPanel(error: DomainError, onRetry: () -> Unit) {
    val tokens = LocalBrandTokens.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.Search,
            contentDescription = null,
            tint = tokens.textMuted,
        )
        Text(
            text = errorMessage(error),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        BhPrimaryButton(text = "Retry", onClick = onRetry)
    }
}

private fun errorMessage(error: DomainError): String = when (error) {
    DomainError.Network -> "Network unavailable. Check your connection."
    DomainError.Unauthorized -> "Session expired. Please sign in again."
    DomainError.Forbidden -> "You don't have permission to view this list."
    is DomainError.Server -> error.message
    is DomainError.RateLimited -> "Too many requests. Please try again shortly."
    else -> "Something went wrong loading bugs."
}

// Re-export for tests
internal fun fakeBugList(
    items: List<BugOut>,
    projects: List<ProjectOut> = emptyList(),
    users: List<UserOut> = emptyList(),
    filters: BugListFilters = BugListFilters(),
): BugListScreenModel = BugListScreenModel(
    filters = filters,
    pages = listOf(
        BugListPage(
            items = items,
            page = 1,
            totalPages = 1,
            total = items.size,
        ),
    ),
    isLoadingMore = false,
    projects = projects,
    users = users,
)
