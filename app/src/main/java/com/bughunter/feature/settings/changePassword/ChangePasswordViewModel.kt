package com.bughunter.feature.settings.changePassword

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bughunter.core.data.repository.AuthRepository
import com.bughunter.core.network.DomainError
import com.bughunter.core.network.Result2
import com.bughunter.core.network.dto.ChangePasswordIn
import com.bughunter.core.ui.util.Password
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

internal data class ChangePasswordUiState(
    val currentPassword: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val isSubmitting: Boolean = false,
    val error: DomainError? = null,
    val breachRejected: Boolean = false,
    val finished: Boolean = false,
) {
    val strengthLabel: String get() = when {
        newPassword.isEmpty() -> ""
        newPassword.length < 8 -> "Too short"
        !Password.strengthFloor(newPassword) -> "Add a letter and a digit"
        newPassword.length >= 14 -> "Strong"
        else -> "OK"
    }
    val strengthFraction: Float get() = when {
        newPassword.isEmpty() -> 0f
        newPassword.length < 8 -> 0.2f
        !Password.strengthFloor(newPassword) -> 0.45f
        newPassword.length >= 14 -> 1f
        else -> 0.7f
    }
    val passwordsMatch: Boolean get() = newPassword.isNotEmpty() && newPassword == confirmPassword
}

@HiltViewModel
internal class ChangePasswordViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ChangePasswordUiState())
    val state: StateFlow<ChangePasswordUiState> = _state.asStateFlow()

    fun onCurrentChange(value: String) {
        _state.update { it.copy(currentPassword = value, error = null) }
    }

    fun onNewChange(value: String) {
        _state.update { it.copy(newPassword = value, breachRejected = false, error = null) }
    }

    fun onConfirmChange(value: String) {
        _state.update { it.copy(confirmPassword = value, error = null) }
    }

    fun submit() {
        val current = _state.value
        if (current.isSubmitting) return
        if (!Password.strengthFloor(current.newPassword)) return
        if (!current.passwordsMatch) return
        _state.update { it.copy(isSubmitting = true, error = null, breachRejected = false) }
        viewModelScope.launch {
            val payload = ChangePasswordIn(
                currentPassword = current.currentPassword,
                newPassword = current.newPassword,
            )
            when (val result = authRepository.changePassword(payload)) {
                is Result2.Ok -> _state.update {
                    ChangePasswordUiState(finished = true)
                }
                is Result2.Err -> {
                    val breach = (result.error as? DomainError.Validation)?.message
                        ?.contains("breach", ignoreCase = true) == true
                    _state.update {
                        it.copy(
                            isSubmitting = false,
                            error = result.error,
                            breachRejected = breach,
                        )
                    }
                }
            }
        }
    }
}
