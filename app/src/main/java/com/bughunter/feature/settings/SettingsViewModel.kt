package com.bughunter.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bughunter.core.data.local.AppPrefs
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

@HiltViewModel
internal class SettingsViewModel @Inject constructor(
    private val appPrefs: AppPrefs,
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

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
}
