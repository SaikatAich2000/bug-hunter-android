package com.bughunter.feature.auth

import com.bughunter.core.network.dto.MeOut
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class AuthStateHolder @Inject constructor() {

    private val _state = MutableStateFlow<AuthState>(AuthState.Checking)
    val state: StateFlow<AuthState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<AuthEvent>(
        replay = 0,
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val events: SharedFlow<AuthEvent> = _events.asSharedFlow()

    fun setChecking() {
        _state.value = AuthState.Checking
    }

    fun setUnauthenticated() {
        _state.value = AuthState.Unauthenticated
    }

    fun setAwaitingTotp(pendingToken: String, email: String) {
        _state.value = AuthState.AwaitingTotp(pendingToken = pendingToken, email = email)
        _events.tryEmit(AuthEvent.RequiresTotp(pendingToken = pendingToken, email = email))
    }

    fun setAuthenticated(me: MeOut) {
        _state.value = AuthState.Authenticated(me)
        _events.tryEmit(AuthEvent.LoggedIn(me))
    }

    fun setLockedOut(retryAfter: Duration) {
        val until = Instant.now().plus(retryAfter).toEpochMilli()
        _state.value = AuthState.LockedOutUntil(epochMs = until)
        _events.tryEmit(AuthEvent.LockedOut(retryAfter))
    }

    fun emitLoggedOut() {
        _state.value = AuthState.Unauthenticated
        _events.tryEmit(AuthEvent.LoggedOut)
    }

    fun emitSessionExpired() {
        _state.value = AuthState.Unauthenticated
        _events.tryEmit(AuthEvent.SessionExpired)
    }

    fun emitBreachRejected() {
        _events.tryEmit(AuthEvent.BreachRejected)
    }

    fun currentMe(): MeOut? = (_state.value as? AuthState.Authenticated)?.me
}
