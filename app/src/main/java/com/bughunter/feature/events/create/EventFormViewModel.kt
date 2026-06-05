package com.bughunter.feature.events.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bughunter.core.data.repository.EventsRepository
import com.bughunter.core.network.DomainError
import com.bughunter.core.network.Result2
import com.bughunter.core.network.dto.EventCreate
import com.bughunter.core.network.dto.EventUpdate
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

internal data class EventFormUiState(
    val eventId: Int? = null,
    val name: String = "",
    val description: String = "",
    val scheduledFor: String = "",
    val managerIdsCsv: String = "",
    val isSubmitting: Boolean = false,
    val isPrefilling: Boolean = false,
    val error: DomainError? = null,
    val savedSuccessfully: Boolean = false,
)

@HiltViewModel
internal class EventFormViewModel @Inject constructor(
    private val repository: EventsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(EventFormUiState())
    val state: StateFlow<EventFormUiState> = _state.asStateFlow()

    fun start(eventId: Int?) {
        _state.update { EventFormUiState(eventId = eventId, isPrefilling = eventId != null) }
        if (eventId != null) loadExisting(eventId)
    }

    private fun loadExisting(eventId: Int) {
        viewModelScope.launch {
            when (val result = repository.get(eventId)) {
                is Result2.Ok -> _state.update {
                    it.copy(
                        isPrefilling = false,
                        name = result.value.name,
                        description = result.value.description,
                        scheduledFor = result.value.scheduledFor.orEmpty(),
                        managerIdsCsv = result.value.managers.joinToString(",") { m -> m.id.toString() },
                    )
                }
                is Result2.Err -> _state.update {
                    it.copy(isPrefilling = false, error = result.error)
                }
            }
        }
    }

    fun onNameChange(value: String) {
        _state.update { it.copy(name = value, error = null) }
    }

    fun onDescriptionChange(value: String) {
        _state.update { it.copy(description = value, error = null) }
    }

    fun onScheduledForChange(value: String) {
        _state.update { it.copy(scheduledFor = value, error = null) }
    }

    fun onManagersChange(value: String) {
        _state.update { it.copy(managerIdsCsv = value, error = null) }
    }

    fun submit() {
        val current = _state.value
        if (current.isSubmitting || current.name.isBlank()) return
        _state.update { it.copy(isSubmitting = true, error = null) }
        val managerIds = parseIds(current.managerIdsCsv)
        viewModelScope.launch {
            val result: Result2<*> = if (current.eventId == null) {
                repository.create(
                    EventCreate(
                        name = current.name.trim(),
                        description = current.description,
                        scheduledFor = current.scheduledFor.ifBlank { null },
                        managerIds = managerIds,
                    ),
                )
            } else {
                repository.update(
                    eventId = current.eventId,
                    body = EventUpdate(
                        name = current.name.trim(),
                        description = current.description,
                        scheduledFor = current.scheduledFor.ifBlank { null },
                        managerIds = managerIds,
                    ),
                )
            }
            when (result) {
                is Result2.Ok -> _state.update {
                    it.copy(isSubmitting = false, savedSuccessfully = true)
                }
                is Result2.Err -> _state.update {
                    it.copy(isSubmitting = false, error = result.error)
                }
            }
        }
    }

    private fun parseIds(csv: String): List<Int> = csv.split(',', ' ')
        .mapNotNull { token -> token.trim().toIntOrNull() }
        .distinct()
}
