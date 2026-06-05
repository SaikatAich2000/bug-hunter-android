package com.bughunter.feature.settings.twoFactor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bughunter.core.data.repository.AuthRepository
import com.bughunter.core.network.DomainError
import com.bughunter.core.network.Result2
import com.bughunter.core.network.dto.TotpBeginOut
import com.bughunter.core.network.dto.TotpConfirmIn
import com.bughunter.core.network.dto.TotpConfirmOut
import com.bughunter.core.network.dto.TotpDisableIn
import com.bughunter.core.network.dto.TotpStatus
import com.bughunter.core.ui.util.OtpCode
import com.bughunter.core.ui.util.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

internal enum class TwoFactorView { STATUS, ENROL, RECOVERY, DISABLE }

internal data class TwoFactorUiState(
    val view: TwoFactorView = TwoFactorView.STATUS,
    val status: UiState<TotpStatus> = UiState.Loading,
    val beginOut: TotpBeginOut? = null,
    val confirmCode: String = "",
    val recovery: TotpConfirmOut? = null,
    val disablePassword: String = "",
    val isSubmitting: Boolean = false,
    val error: DomainError? = null,
)

@HiltViewModel
internal class TwoFactorViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(TwoFactorUiState())
    val state: StateFlow<TwoFactorUiState> = _state.asStateFlow()

    init {
        refreshStatus()
    }

    fun refreshStatus() {
        viewModelScope.launch {
            _state.update { it.copy(status = UiState.Loading) }
            when (val result = authRepository.totpStatus()) {
                is Result2.Ok -> _state.update { it.copy(status = UiState.Success(result.value)) }
                is Result2.Err -> _state.update { it.copy(status = UiState.Error(result.error)) }
            }
        }
    }

    fun beginEnrol() {
        viewModelScope.launch {
            _state.update { it.copy(isSubmitting = true, error = null) }
            when (val result = authRepository.totpBegin()) {
                is Result2.Ok -> _state.update {
                    it.copy(
                        isSubmitting = false,
                        view = TwoFactorView.ENROL,
                        beginOut = result.value,
                        confirmCode = "",
                    )
                }
                is Result2.Err -> _state.update {
                    it.copy(isSubmitting = false, error = result.error)
                }
            }
        }
    }

    fun onConfirmCodeChange(value: String) {
        val cleaned = value.filter { it.isDigit() || it.isLetter() }.take(MAX_CODE_LEN)
        _state.update { it.copy(confirmCode = cleaned, error = null) }
    }

    fun confirmEnrol() {
        val current = _state.value
        if (current.isSubmitting || !OtpCode.isValid(current.confirmCode)) return
        _state.update { it.copy(isSubmitting = true, error = null) }
        viewModelScope.launch {
            val payload = TotpConfirmIn(code = current.confirmCode.trim().uppercase())
            when (val result = authRepository.totpConfirm(payload)) {
                is Result2.Ok -> _state.update {
                    it.copy(
                        isSubmitting = false,
                        view = TwoFactorView.RECOVERY,
                        recovery = result.value,
                    )
                }
                is Result2.Err -> _state.update {
                    it.copy(isSubmitting = false, error = result.error)
                }
            }
        }
    }

    fun openDisable() {
        _state.update { it.copy(view = TwoFactorView.DISABLE, disablePassword = "", error = null) }
    }

    fun openStatus() {
        _state.update {
            it.copy(view = TwoFactorView.STATUS, beginOut = null, recovery = null, error = null)
        }
        refreshStatus()
    }

    fun onDisablePasswordChange(value: String) {
        _state.update { it.copy(disablePassword = value, error = null) }
    }

    fun confirmDisable() {
        val current = _state.value
        if (current.isSubmitting || current.disablePassword.isBlank()) return
        _state.update { it.copy(isSubmitting = true, error = null) }
        viewModelScope.launch {
            when (val result = authRepository.totpDisable(TotpDisableIn(password = current.disablePassword))) {
                is Result2.Ok -> {
                    _state.update {
                        it.copy(
                            isSubmitting = false,
                            view = TwoFactorView.STATUS,
                            disablePassword = "",
                        )
                    }
                    refreshStatus()
                }
                is Result2.Err -> _state.update {
                    it.copy(isSubmitting = false, error = result.error)
                }
            }
        }
    }

    fun regenerateRecoveryCodes() {
        viewModelScope.launch {
            _state.update { it.copy(isSubmitting = true, error = null) }
            when (val result = authRepository.totpRegenerateRecoveryCodes()) {
                is Result2.Ok -> _state.update {
                    it.copy(
                        isSubmitting = false,
                        view = TwoFactorView.RECOVERY,
                        recovery = result.value,
                    )
                }
                is Result2.Err -> _state.update {
                    it.copy(isSubmitting = false, error = result.error)
                }
            }
        }
    }

    companion object {
        private const val MAX_CODE_LEN = 12
    }
}
