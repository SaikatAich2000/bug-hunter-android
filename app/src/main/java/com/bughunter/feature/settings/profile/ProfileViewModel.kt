package com.bughunter.feature.settings.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bughunter.core.data.repository.AuthRepository
import com.bughunter.core.network.DomainError
import com.bughunter.core.network.Result2
import com.bughunter.core.network.dto.MeOut
import com.bughunter.core.network.dto.ProfileUpdateIn
import com.bughunter.feature.auth.AuthState
import com.bughunter.feature.auth.AuthStateHolder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

internal data class ProfileUiState(
    val me: MeOut? = null,
    val nameEdit: String = "",
    val isSavingName: Boolean = false,
    val savedName: Boolean = false,
    val error: DomainError? = null,
)

@HiltViewModel
internal class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    authStateHolder: AuthStateHolder,
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    init {
        val current = authStateHolder.state.value
        if (current is AuthState.Authenticated) {
            _state.update { it.copy(me = current.me, nameEdit = current.me.name) }
        }
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            when (val result = authRepository.me()) {
                is Result2.Ok -> _state.update {
                    it.copy(
                        me = result.value,
                        nameEdit = if (it.nameEdit.isBlank()) result.value.name else it.nameEdit,
                    )
                }
                is Result2.Err -> _state.update { it.copy(error = result.error) }
            }
        }
    }

    fun onNameChange(value: String) {
        _state.update { it.copy(nameEdit = value, savedName = false, error = null) }
    }

    fun saveName() {
        val current = _state.value
        if (current.isSavingName || current.nameEdit.isBlank()) return
        _state.update { it.copy(isSavingName = true, error = null, savedName = false) }
        viewModelScope.launch {
            when (val result = authRepository.updateProfile(ProfileUpdateIn(name = current.nameEdit.trim()))) {
                is Result2.Ok -> _state.update {
                    it.copy(me = result.value, isSavingName = false, savedName = true)
                }
                is Result2.Err -> _state.update {
                    it.copy(isSavingName = false, error = result.error)
                }
            }
        }
    }
}
