package com.bughunter.feature.events.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bughunter.core.network.dto.EventOut
import com.bughunter.core.ui.components.BhEmptyState
import com.bughunter.core.ui.components.BhGhostButton
import com.bughunter.core.ui.components.BhIconButton
import com.bughunter.core.ui.components.BhPrimaryButton
import com.bughunter.core.ui.components.BhTextField
import com.bughunter.core.ui.util.UiState
import com.bughunter.feature.events.create.EventFormDialog

@Composable
internal fun EventsListScreen(
    onEventClick: (Int) -> Unit,
    viewModel: EventsListViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    EventsListContent(
        state = state,
        onEventClick = onEventClick,
        onCreate = viewModel::openCreate,
        onRefresh = viewModel::refresh,
        onQueryChange = viewModel::onQueryChange,
        onScheduledForChange = viewModel::onScheduledForChange,
        onClearFilters = viewModel::clearFilters,
        onDismissForm = viewModel::closeForm,
    )
}

@Composable
internal fun EventsListTestHarness(
    state: EventsListUiState,
    onEventClick: (Int) -> Unit = {},
    onCreate: () -> Unit = {},
    onRefresh: () -> Unit = {},
    onQueryChange: (String) -> Unit = {},
    onScheduledForChange: (String?) -> Unit = {},
    onClearFilters: () -> Unit = {},
) {
    EventsListContent(
        state = state,
        onEventClick = onEventClick,
        onCreate = onCreate,
        onRefresh = onRefresh,
        onQueryChange = onQueryChange,
        onScheduledForChange = onScheduledForChange,
        onClearFilters = onClearFilters,
        onDismissForm = {},
    )
}

@Composable
private fun EventsListContent(
    state: EventsListUiState,
    onEventClick: (Int) -> Unit,
    onCreate: () -> Unit,
    onRefresh: () -> Unit,
    onQueryChange: (String) -> Unit,
    onScheduledForChange: (String?) -> Unit,
    onClearFilters: () -> Unit,
    onDismissForm: () -> Unit,
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
            Text(
                text = "Events",
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
            BhPrimaryButton(text = "+ New Event", onClick = onCreate)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BhTextField(
                value = state.filters.query,
                onValueChange = onQueryChange,
                label = "Search events",
                placeholder = "Name or description",
                modifier = Modifier.weight(1f),
            )
            BhTextField(
                value = state.filters.scheduledFor.orEmpty(),
                onValueChange = { onScheduledForChange(it.ifBlank { null }) },
                label = "Date",
                placeholder = "YYYY-MM-DD",
                keyboardType = KeyboardType.Number,
                modifier = Modifier.weight(1f),
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            BhGhostButton(text = "Clear", onClick = onClearFilters)
        }
        Spacer(modifier = Modifier.height(4.dp))
        when (val list = state.list) {
            UiState.Loading -> Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CircularProgressIndicator()
            }
            UiState.Empty -> {
                val helper = if (state.filters.query.isNotBlank() || state.filters.scheduledFor != null) {
                    "No events match the current filter. Try clearing the search or date"
                } else {
                    "No events yet. Click + New Event to create one — a standup, a sprint meeting, whatever you want to track"
                }
                BhEmptyState(
                    title = "No events",
                    helper = helper,
                    icon = Icons.Outlined.Event,
                )
            }
            is UiState.Error -> Text(
                text = "Couldn't load events.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
            is UiState.Success -> EventsList(
                events = list.data,
                onEventClick = onEventClick,
            )
        }
    }
    if (state.isFormOpen) {
        EventFormDialog(
            eventId = state.editingEventId,
            onDismiss = onDismissForm,
            onSaved = onDismissForm,
        )
    }
}

@Composable
private fun EventsList(
    events: List<EventOut>,
    onEventClick: (Int) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(items = events, key = { it.id }) { event ->
            EventCardComposable(
                event = event,
                onClick = { onEventClick(event.id) },
            )
        }
    }
}
