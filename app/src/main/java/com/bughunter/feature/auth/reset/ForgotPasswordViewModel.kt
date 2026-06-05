package com.bughunter.feature.auth.reset

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bughunter.core.data.repository.AuthRepository
import com.bughunter.core.network.DomainError
import com.bughunter.core.network.Result2
import com.bughunter.core.network.dto.ForgotPasswordIn
import com.bughunter.core.ui.util.Email
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

internal data class ForgotPasswordUiState(
    val email: String = "",
    val isSubmitting: Boolean = false,
    val submitted: Boolean = false,
    val error: DomainError? = null,
)

@HiltViewModel
internal class ForgotPasswordViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ForgotPasswordUiState())
    val state: StateFlow<ForgotPasswordUiState> = _state.asStateFlow()

    fun onEmailChange(value: String) {
        _state.update { it.copy(email = value, error = null) }
    }

    fun onSubmit() {
        val current = _state.value
        if (current.isSubmitting || current.submitted) return
        if (!Email.isValid(current.email)) return
        _state.update { it.copy(isSubmitting = true, error = null) }
        viewModelScope.launch {
            when (val result = authRepository.forgotPassword(ForgotPasswordIn(current.email.trim()))) {
                is Result2.Ok -> _state.update { it.copy(isSubmitting = false, submitted = true) }
                // We never leak whether the email exists; surface a generic success on NotFound too.
                is Result2.Err -> if (result.error is DomainError.NotFound) {
                    _state.update { it.copy(isSubmitting = false, submitted = true) }
                } else {
                    _state.update { it.copy(isSubmitting = false, error = result.error) }
                }
            }
        }
    }
}
