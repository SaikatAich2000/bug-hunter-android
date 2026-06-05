package com.bughunter.core.nav

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bughunter.core.data.repository.AuthRepository
import com.bughunter.core.domain.usecase.LogoutUseCase
import com.bughunter.feature.auth.AuthState
import com.bughunter.feature.auth.AuthStateHolder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class BhAppShellViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val authStateHolder: AuthStateHolder,
    private val logoutUseCase: LogoutUseCase,
) : ViewModel() {

    val authState: StateFlow<AuthState> = authStateHolder.state

    init {
        viewModelScope.launch { authRepository.bootstrap() }
    }

    fun onLogout() {
        viewModelScope.launch { logoutUseCase() }
    }
}
