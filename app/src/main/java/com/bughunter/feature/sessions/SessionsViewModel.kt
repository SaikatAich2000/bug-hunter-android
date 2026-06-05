package com.bughunter.feature.sessions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bughunter.core.data.repository.SessionsRepository
import com.bughunter.core.network.Result2
import com.bughunter.core.network.dto.SessionOut
import com.bughunter.core.ui.util.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

internal data class SessionsUiState(
    val list: UiState<List<SessionOut>> = UiState.Loading,
    val revokingId: Int? = null,
    val pendingRevokeId: Int? = null,
)

@HiltViewModel
internal class SessionsViewModel @Inject constructor(
    private val repository: SessionsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(SessionsUiState())
    val state: StateFlow<SessionsUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(list = UiState.Loading) }
            when (val result = repository.list()) {
                is Result2.Ok -> _state.update {
                    it.copy(
                        list = if (result.value.isEmpty()) UiState.Empty
                        else UiState.Success(result.value),
                    )
                }
                is Result2.Err -> _state.update { it.copy(list = UiState.Error(result.error)) }
            }
        }
    }

    fun requestRevoke(sessionId: Int) {
        _state.update { it.copy(pendingRevokeId = sessionId) }
    }

    fun dismissRevoke() {
        _state.update { it.copy(pendingRevokeId = null) }
    }

    fun confirmRevoke() {
        val id = _state.value.pendingRevokeId ?: return
        viewModelScope.launch {
            _state.update { it.copy(revokingId = id, pendingRevokeId = null) }
            when (repository.revoke(id)) {
                is Result2.Ok -> {
                    _state.update { it.copy(revokingId = null) }
                    refresh()
                }
                is Result2.Err -> _state.update { it.copy(revokingId = null) }
            }
        }
    }
}
