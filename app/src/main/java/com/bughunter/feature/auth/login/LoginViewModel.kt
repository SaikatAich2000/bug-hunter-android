package com.bughunter.feature.auth.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bughunter.core.domain.usecase.LoginUseCase
import com.bughunter.core.network.DomainError
import com.bughunter.core.network.Result2
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

internal data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isSubmitting: Boolean = false,
    val error: DomainError? = null,
    val lockedUntil: Instant? = null,
)

@HiltViewModel
internal class LoginViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    fun onEmailChange(value: String) {
        _state.update { it.copy(email = value, error = null) }
    }

    fun onPasswordChange(value: String) {
        _state.update { it.copy(password = value, error = null) }
    }

    fun onSubmit() {
        val current = _state.value
        if (current.isSubmitting) return
        if (current.email.isBlank() || current.password.isBlank()) return
        _state.update { it.copy(isSubmitting = true, error = null) }
        viewModelScope.launch {
            when (val result = loginUseCase(current.email.trim(), current.password)) {
                is Result2.Ok -> _state.update { it.copy(isSubmitting = false) }
                is Result2.Err -> {
                    val lockedUntil = (result.error as? DomainError.RateLimited)?.retryAfter
                        ?.let { Instant.now().plus(it) }
                    _state.update {
                        it.copy(
                            isSubmitting = false,
                            error = result.error,
                            lockedUntil = lockedUntil ?: it.lockedUntil,
                        )
                    }
                }
            }
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}
