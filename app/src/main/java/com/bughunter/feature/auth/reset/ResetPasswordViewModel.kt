package com.bughunter.feature.auth.reset

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bughunter.core.data.repository.AuthRepository
import com.bughunter.core.network.DomainError
import com.bughunter.core.network.Result2
import com.bughunter.core.network.dto.ResetPasswordIn
import com.bughunter.core.nav.BhRoute
import com.bughunter.core.ui.util.Password
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

internal data class ResetPasswordUiState(
    val token: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val isSubmitting: Boolean = false,
    val isSuccess: Boolean = false,
    val error: DomainError? = null,
) {
    val strengthOk: Boolean get() = Password.strengthFloor(newPassword)
    val passwordsMatch: Boolean get() = newPassword.isNotEmpty() && newPassword == confirmPassword
}

@HiltViewModel
internal class ResetPasswordViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(
        ResetPasswordUiState(
            token = savedStateHandle.get<String>(BhRoute.ResetPassword.ARG_TOKEN).orEmpty(),
        ),
    )
    val state: StateFlow<ResetPasswordUiState> = _state.asStateFlow()

    fun onNewPasswordChange(value: String) {
        _state.update { it.copy(newPassword = value, error = null) }
    }

    fun onConfirmPasswordChange(value: String) {
        _state.update { it.copy(confirmPassword = value, error = null) }
    }

    fun onSubmit() {
        val current = _state.value
        if (current.isSubmitting) return
        if (!current.strengthOk || !current.passwordsMatch) return
        _state.update { it.copy(isSubmitting = true, error = null) }
        viewModelScope.launch {
            val payload = ResetPasswordIn(token = current.token, newPassword = current.newPassword)
            when (val result = authRepository.resetPassword(payload)) {
                is Result2.Ok -> _state.update { it.copy(isSubmitting = false, isSuccess = true) }
                is Result2.Err -> _state.update { it.copy(isSubmitting = false, error = result.error) }
            }
        }
    }
}
