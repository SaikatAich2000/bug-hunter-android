package com.bughunter.feature.events.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bughunter.core.data.repository.EventsRepository
import com.bughunter.core.network.Result2
import com.bughunter.core.network.dto.EventOut
import com.bughunter.core.ui.util.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

internal data class EventsListFilters(
    val query: String = "",
    val scheduledFor: String? = null,
)

internal data class EventsListUiState(
    val list: UiState<List<EventOut>> = UiState.Loading,
    val filters: EventsListFilters = EventsListFilters(),
    val isRefreshing: Boolean = false,
    val isFormOpen: Boolean = false,
    val editingEventId: Int? = null,
)

@HiltViewModel
internal class EventsListViewModel @Inject constructor(
    private val repository: EventsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(EventsListUiState())
    val state: StateFlow<EventsListUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true) }
            val current = _state.value.filters
            when (val result = repository.list(current.scheduledFor)) {
                is Result2.Ok -> _state.update {
                    val visible = filtered(result.value, current.query)
                    it.copy(
                        list = if (visible.isEmpty()) UiState.Empty else UiState.Success(visible),
                        isRefreshing = false,
                    )
                }
                is Result2.Err -> _state.update {
                    it.copy(list = UiState.Error(result.error), isRefreshing = false)
                }
            }
        }
    }

    fun onQueryChange(value: String) {
        _state.update { it.copy(filters = it.filters.copy(query = value)) }
        refresh()
    }

    fun onScheduledForChange(value: String?) {
        _state.update { it.copy(filters = it.filters.copy(scheduledFor = value)) }
        refresh()
    }

    fun clearFilters() {
        _state.update { it.copy(filters = EventsListFilters()) }
        refresh()
    }

    fun openCreate() {
        _state.update { it.copy(isFormOpen = true, editingEventId = null) }
    }

    fun openEdit(eventId: Int) {
        _state.update { it.copy(isFormOpen = true, editingEventId = eventId) }
    }

    fun closeForm() {
        _state.update { it.copy(isFormOpen = false, editingEventId = null) }
    }

    private fun filtered(items: List<EventOut>, query: String): List<EventOut> {
        if (query.isBlank()) return items
        val needle = query.trim().lowercase()
        return items.filter { ev ->
            ev.name.lowercase().contains(needle) ||
                ev.description.lowercase().contains(needle)
        }
    }
}
