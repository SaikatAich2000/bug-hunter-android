package com.bughunter.feature.settings.changeEmail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bughunter.core.data.repository.AuthRepository
import com.bughunter.core.network.DomainError
import com.bughunter.core.network.Result2
import com.bughunter.core.network.dto.EmailChangeConfirmIn
import com.bughunter.core.network.dto.EmailChangeRequestIn
import com.bughunter.core.ui.util.Email
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

internal enum class ChangeEmailStep { REQUEST, CONFIRM, DONE }

internal data class ChangeEmailUiState(
    val step: ChangeEmailStep = ChangeEmailStep.REQUEST,
    val newEmail: String = "",
    val currentPassword: String = "",
    val code: String = "",
    val isSubmitting: Boolean = false,
    val message: String? = null,
    val error: DomainError? = null,
)

@HiltViewModel
internal class ChangeEmailViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ChangeEmailUiState())
    val state: StateFlow<ChangeEmailUiState> = _state.asStateFlow()

    fun onNewEmailChange(value: String) {
        _state.update { it.copy(newEmail = value, error = null) }
    }

    fun onPasswordChange(value: String) {
        _state.update { it.copy(currentPassword = value, error = null) }
    }

    fun onCodeChange(value: String) {
        val cleaned = value.filter { it.isDigit() }.take(MAX_CODE_LEN)
        _state.update { it.copy(code = cleaned, error = null) }
    }

    fun requestChange() {
        val current = _state.value
        if (current.isSubmitting) return
        if (!Email.isValid(current.newEmail)) return
        if (current.currentPassword.isBlank()) return
        _state.update { it.copy(isSubmitting = true, error = null, message = null) }
        viewModelScope.launch {
            val body = EmailChangeRequestIn(
                newEmail = current.newEmail.trim(),
                currentPassword = current.currentPassword,
            )
            when (val result = authRepository.requestEmailChange(body)) {
                is Result2.Ok -> _state.update {
                    it.copy(
                        isSubmitting = false,
                        step = ChangeEmailStep.CONFIRM,
                        message = result.value["message"],
                        currentPassword = "",
                    )
                }
                is Result2.Err -> _state.update {
                    it.copy(isSubmitting = false, error = result.error)
                }
            }
        }
    }

    fun confirmChange() {
        val current = _state.value
        if (current.isSubmitting) return
        if (current.code.length != CONFIRM_CODE_LEN) return
        _state.update { it.copy(isSubmitting = true, error = null) }
        viewModelScope.launch {
            when (val result = authRepository.confirmEmailChange(EmailChangeConfirmIn(code = current.code))) {
                is Result2.Ok -> _state.update {
                    it.copy(
                        isSubmitting = false,
                        step = ChangeEmailStep.DONE,
                        message = "Email updated to ${result.value.email}",
                    )
                }
                is Result2.Err -> _state.update {
                    it.copy(isSubmitting = false, error = result.error)
                }
            }
        }
    }

    fun reset() {
        _state.value = ChangeEmailUiState()
    }

    companion object {
        private const val MAX_CODE_LEN = 6
        private const val CONFIRM_CODE_LEN = 6
    }
}
