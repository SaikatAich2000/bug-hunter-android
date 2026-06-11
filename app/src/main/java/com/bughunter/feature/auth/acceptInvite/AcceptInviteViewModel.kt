package com.bughunter.feature.auth.acceptInvite

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bughunter.core.data.repository.AuthRepository
import com.bughunter.core.data.repository.InvitationsRepository
import com.bughunter.core.network.DomainError
import com.bughunter.core.network.Result2
import com.bughunter.core.network.dto.InvitationAccept
import com.bughunter.core.network.dto.InvitationPreview
import com.bughunter.core.nav.BhRoute
import com.bughunter.core.ui.util.Password
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

internal data class AcceptInviteUiState(
    val token: String = "",
    val preview: InvitationPreview? = null,
    val isLoadingPreview: Boolean = true,
    val previewError: DomainError? = null,
    val name: String = "",
    val password: String = "",
    val isSubmitting: Boolean = false,
    val submitError: DomainError? = null,
) {
    val canSubmit: Boolean
        get() = preview != null && name.isNotBlank() && Password.strengthFloor(password)
}

@HiltViewModel
internal class AcceptInviteViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val invitationsRepository: InvitationsRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(
        AcceptInviteUiState(
            token = savedStateHandle[BhRoute.AcceptInvite.ARG_TOKEN] ?: "",
        ),
    )
    val state: StateFlow<AcceptInviteUiState> = _state.asStateFlow()

    init {
        loadPreview()
    }

    fun onNameChange(value: String) {
        _state.update { it.copy(name = value, submitError = null) }
    }

    fun onPasswordChange(value: String) {
        _state.update { it.copy(password = value, submitError = null) }
    }

    fun retryPreview() {
        loadPreview()
    }

    fun onSubmit() {
        val current = _state.value
        if (current.isSubmitting || !current.canSubmit) return
        _state.update { it.copy(isSubmitting = true, submitError = null) }
        viewModelScope.launch {
            val payload = InvitationAccept(
                token = current.token,
                name = current.name.trim(),
                password = current.password,
            )
            when (val result = invitationsRepository.accept(payload)) {
                is Result2.Ok -> {
                    authRepository.onInviteAccepted(result.value)
                    _state.update { it.copy(isSubmitting = false) }
                }
                is Result2.Err -> _state.update {
                    it.copy(isSubmitting = false, submitError = result.error)
                }
            }
        }
    }

    private fun loadPreview() {
        val token = _state.value.token
        if (token.isBlank()) {
            _state.update { it.copy(isLoadingPreview = false, previewError = DomainError.NotFound) }
            return
        }
        _state.update { it.copy(isLoadingPreview = true, previewError = null) }
        viewModelScope.launch {
            when (val result = invitationsRepository.preview(token)) {
                is Result2.Ok -> _state.update {
                    it.copy(isLoadingPreview = false, preview = result.value)
                }
                is Result2.Err -> _state.update {
                    it.copy(isLoadingPreview = false, previewError = result.error)
                }
            }
        }
    }
}
