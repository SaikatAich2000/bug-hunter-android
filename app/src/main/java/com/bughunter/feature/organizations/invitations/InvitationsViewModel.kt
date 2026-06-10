package com.bughunter.feature.organizations.invitations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bughunter.core.data.repository.InvitationsRepository
import com.bughunter.core.network.DomainError
import com.bughunter.core.network.Result2
import com.bughunter.core.network.dto.InvitationCreate
import com.bughunter.core.network.dto.InvitationOut
import com.bughunter.core.network.dto.Role
import com.bughunter.core.ui.util.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

internal data class InvitationsModel(
    val invitations: List<InvitationOut>,
    val now: Instant = Instant.now(),
    // Error from the last action (revoke or invite). Rendered as a top-of-
    // screen BhErrorBanner so a failed Revoke button doesn't silently
    // appear to succeed — the previous behavior was the user seeing the
    // row stay put with zero feedback.
    val actionError: DomainError? = null,
) {
    fun displayStatus(item: InvitationOut): String = when {
        item.revokedAt != null -> "revoked"
        item.acceptedAt != null -> "accepted"
        item.expiresAt.isBefore(now) -> "expired"
        else -> "pending"
    }
}

@HiltViewModel
internal class InvitationsViewModel @Inject constructor(
    private val repo: InvitationsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<InvitationsModel>>(UiState.Loading)
    val state: StateFlow<UiState<InvitationsModel>> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            when (val result = repo.list()) {
                is Result2.Ok -> _state.value = if (result.value.isEmpty()) UiState.Empty
                else UiState.Success(InvitationsModel(invitations = result.value))
                is Result2.Err -> _state.value = UiState.Error(result.error)
            }
        }
    }

    fun revoke(id: Int) {
        viewModelScope.launch {
            when (val result = repo.delete(id)) {
                is Result2.Ok -> load()
                is Result2.Err -> setActionError(result.error)
            }
        }
    }

    fun invite(email: String, role: Role, projectIds: List<Int>, asLead: Boolean, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            val body = InvitationCreate(email = email, role = role, projectIds = projectIds, asLead = asLead)
            when (val result = repo.create(body)) {
                is Result2.Ok -> { load(); onDone(true) }
                is Result2.Err -> { setActionError(result.error); onDone(false) }
            }
        }
    }

    fun dismissActionError() {
        val current = _state.value as? UiState.Success ?: return
        _state.value = UiState.Success(current.data.copy(actionError = null))
    }

    private fun setActionError(error: DomainError) {
        val current = _state.value as? UiState.Success ?: return
        _state.value = UiState.Success(current.data.copy(actionError = error))
    }
}
