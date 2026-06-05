package com.bughunter.core.nav

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bughunter.core.data.local.AppPrefs
import com.bughunter.core.ui.theme.BugHunterTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class BhRootViewModel @Inject constructor(
    private val appPrefs: AppPrefs,
) : ViewModel() {
    val themeMode: StateFlow<AppPrefs.ThemeMode> = appPrefs.themeMode
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppPrefs.ThemeMode.SYSTEM)

    fun setThemeMode(mode: AppPrefs.ThemeMode) {
        viewModelScope.launch { appPrefs.setThemeMode(mode) }
    }
}

@Composable
internal fun BhRoot(viewModel: BhRootViewModel = hiltViewModel()) {
    val themeMode by viewModel.themeMode.collectAsState()
    val systemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        AppPrefs.ThemeMode.LIGHT -> false
        AppPrefs.ThemeMode.DARK -> true
        AppPrefs.ThemeMode.SYSTEM -> systemDark
    }
    BugHunterTheme(darkTheme = isDark) {
        BhAppShell(
            currentThemeMode = themeMode,
            onThemeModeChange = viewModel::setThemeMode,
        )
    }
}
