package com.bughunter.feature.auth.signup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bughunter.core.data.repository.AuthRepository
import com.bughunter.core.network.DomainError
import com.bughunter.core.network.Result2
import com.bughunter.core.network.dto.SignupIn
import com.bughunter.core.ui.util.Email
import com.bughunter.core.ui.util.Password
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

internal data class SignupUiState(
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val organizationName: String = "",
    val isSubmitting: Boolean = false,
    val error: DomainError? = null,
    val breachRejected: Boolean = false,
) {
    val canSubmit: Boolean
        get() = name.isNotBlank() &&
            organizationName.isNotBlank() &&
            Email.isValid(email) &&
            Password.strengthFloor(password)
}

@HiltViewModel
internal class SignupViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(SignupUiState())
    val state: StateFlow<SignupUiState> = _state.asStateFlow()

    fun onNameChange(value: String) {
        _state.update { it.copy(name = value, error = null) }
    }

    fun onEmailChange(value: String) {
        _state.update { it.copy(email = value, error = null) }
    }

    fun onPasswordChange(value: String) {
        _state.update { it.copy(password = value, error = null, breachRejected = false) }
    }

    fun onOrganizationChange(value: String) {
        _state.update { it.copy(organizationName = value, error = null) }
    }

    fun onSubmit() {
        val current = _state.value
        if (current.isSubmitting || !current.canSubmit) return
        _state.update { it.copy(isSubmitting = true, error = null, breachRejected = false) }
        viewModelScope.launch {
            val payload = SignupIn(
                name = current.name.trim(),
                email = current.email.trim(),
                password = current.password,
                organizationName = current.organizationName.trim(),
            )
            when (val result = authRepository.signup(payload)) {
                is Result2.Ok -> _state.update { it.copy(isSubmitting = false) }
                is Result2.Err -> {
                    val breach = result.error is DomainError.Validation &&
                        result.error.message?.contains("breach", ignoreCase = true) == true
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
