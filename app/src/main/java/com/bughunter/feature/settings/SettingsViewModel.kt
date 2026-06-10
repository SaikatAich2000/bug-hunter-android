package com.bughunter.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bughunter.core.data.local.AppPrefs
import com.bughunter.core.data.repository.AuthRepository
import com.bughunter.core.network.DomainError
import com.bughunter.core.network.Result2
import com.bughunter.core.network.dto.DeleteAccountIn
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

internal data class SettingsUiState(
    val themeMode: AppPrefs.ThemeMode = AppPrefs.ThemeMode.SYSTEM,
    val defaultNewType: String = "Bug",
    val baseUrl: String = "",
    val isDebug: Boolean = false,
)

/**
 * State of the "Delete account" confirmation flow.
 *
 * isDeleted = true is the terminal success signal: the calling screen
 * observes it and navigates back to login (the AuthRepository has already
 * cleared the local session in its delete-account success path).
 */
internal data class DeleteAccountUiState(
    val isSubmitting: Boolean = false,
    val error: DomainError? = null,
    val isDeleted: Boolean = false,
)

@HiltViewModel
internal class SettingsViewModel @Inject constructor(
    private val appPrefs: AppPrefs,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    private val _deleteState = MutableStateFlow(DeleteAccountUiState())
    val deleteState: StateFlow<DeleteAccountUiState> = _deleteState.asStateFlow()

    init {
        combine(
            appPrefs.themeMode,
            appPrefs.defaultNewType,
            appPrefs.baseUrl,
        ) { theme, type, url ->
            SettingsUiState(themeMode = theme, defaultNewType = type, baseUrl = url, isDebug = _state.value.isDebug)
        }.onEach { snapshot ->
            _state.value = snapshot
        }.launchIn(viewModelScope)
    }

    fun setIsDebug(value: Boolean) {
        _state.value = _state.value.copy(isDebug = value)
    }

    fun onThemeModeChange(mode: AppPrefs.ThemeMode) {
        viewModelScope.launch { appPrefs.setThemeMode(mode) }
    }

    fun onDefaultNewTypeChange(value: String) {
        viewModelScope.launch { appPrefs.setDefaultNewType(value) }
    }

    fun onBaseUrlChange(value: String) {
        viewModelScope.launch { appPrefs.setBaseUrl(value) }
    }

    /**
     * Required for Google Play (web AND in-app deletion paths must exist).
     * Sends DELETE /api/auth/account with the user's password for
     * re-authentication; backend clears all of the user's data and
     * returns 204. On success, AuthRepository tears down the local
     * session — the screen reacts by navigating to Login.
     */
    fun deleteAccount(password: String) {
        if (_deleteState.value.isSubmitting) return
        _deleteState.value = DeleteAccountUiState(isSubmitting = true)
        viewModelScope.launch {
            when (val result = authRepository.deleteAccount(DeleteAccountIn(password = password))) {
                is Result2.Ok -> {
                    _deleteState.value = DeleteAccountUiState(isDeleted = true)
                }
                is Result2.Err -> {
                    _deleteState.value = DeleteAccountUiState(error = result.error)
                }
            }
        }
    }

    fun dismissDeleteError() {
        _deleteState.value = _deleteState.value.copy(error = null)
    }

    fun resetDeleteState() {
        _deleteState.value = DeleteAccountUiState()
    }
}
