package com.bughunter.feature.events.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bughunter.core.data.repository.EventsRepository
import com.bughunter.core.network.Result2
import com.bughunter.core.network.dto.EventDetailOut
import com.bughunter.core.ui.util.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

internal data class EventDetailUiState(
    val event: UiState<EventDetailOut> = UiState.Loading,
    val isFormOpen: Boolean = false,
    val isDeleteConfirmOpen: Boolean = false,
    val isDeleting: Boolean = false,
)

@HiltViewModel
internal class EventDetailViewModel @Inject constructor(
    private val repository: EventsRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val eventId: Int = checkNotNull(savedStateHandle["eventId"]) {
        "eventId nav arg missing"
    }

    private val _state = MutableStateFlow(EventDetailUiState())
    val state: StateFlow<EventDetailUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(event = UiState.Loading) }
            when (val result = repository.get(eventId)) {
                is Result2.Ok -> _state.update { it.copy(event = UiState.Success(result.value)) }
                is Result2.Err -> _state.update { it.copy(event = UiState.Error(result.error)) }
            }
        }
    }

    fun openEdit() {
        _state.update { it.copy(isFormOpen = true) }
    }

    fun closeForm() {
        _state.update { it.copy(isFormOpen = false) }
        refresh()
    }

    fun openDeleteConfirm() {
        _state.update { it.copy(isDeleteConfirmOpen = true) }
    }

    fun dismissDeleteConfirm() {
        _state.update { it.copy(isDeleteConfirmOpen = false) }
    }

    fun confirmDelete(onDeleted: () -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(isDeleting = true) }
            when (val result = repository.delete(eventId)) {
                is Result2.Ok -> {
                    _state.update { it.copy(isDeleting = false, isDeleteConfirmOpen = false) }
                    onDeleted()
                }
                is Result2.Err -> _state.update {
                    it.copy(isDeleting = false, event = UiState.Error(result.error))
                }
            }
        }
    }
}
