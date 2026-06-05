package com.bughunter.feature.auth.totp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bughunter.core.data.repository.AuthRepository
import com.bughunter.core.network.DomainError
import com.bughunter.core.network.Result2
import com.bughunter.core.network.dto.LoginTotpStepIn
import com.bughunter.core.ui.util.OtpCode
import com.bughunter.feature.auth.AuthState
import com.bughunter.feature.auth.AuthStateHolder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

internal data class LoginTotpUiState(
    val email: String = "",
    val code: String = "",
    val isSubmitting: Boolean = false,
    val error: DomainError? = null,
)

@HiltViewModel
internal class LoginTotpViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val authStateHolder: AuthStateHolder,
) : ViewModel() {

    private val _state = MutableStateFlow(LoginTotpUiState())
    val state: StateFlow<LoginTotpUiState> = _state.asStateFlow()

    init {
        val current = authStateHolder.state.value
        if (current is AuthState.AwaitingTotp) {
            _state.update { it.copy(email = current.email) }
        }
    }

    fun onCodeChange(value: String) {
        val cleaned = value.filter { it.isDigit() || it.isLetter() }.take(MAX_CODE_LEN)
        _state.update { it.copy(code = cleaned, error = null) }
    }

    fun onSubmit() {
        val current = _state.value
        if (current.isSubmitting) return
        if (!OtpCode.isValid(current.code)) return
        val authState = authStateHolder.state.value as? AuthState.AwaitingTotp ?: return
        _state.update { it.copy(isSubmitting = true, error = null) }
        viewModelScope.launch {
            val payload = LoginTotpStepIn(
                pendingToken = authState.pendingToken,
                code = current.code.trim().uppercase(),
            )
            when (val result = authRepository.loginTotp(payload)) {
                is Result2.Ok -> _state.update { it.copy(isSubmitting = false) }
                is Result2.Err -> _state.update { it.copy(isSubmitting = false, error = result.error) }
            }
        }
    }

    companion object {
        private const val MAX_CODE_LEN = 12
    }
}
