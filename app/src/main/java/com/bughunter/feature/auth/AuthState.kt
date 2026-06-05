package com.bughunter.feature.auth

import com.bughunter.core.network.dto.MeOut

internal sealed interface AuthState {
    data object Checking : AuthState
    data object Unauthenticated : AuthState
    data class AwaitingTotp(
        val pendingToken: String,
        val email: String,
    ) : AuthState
    data class Authenticated(
        val me: MeOut,
    ) : AuthState
    data class LockedOutUntil(
        val epochMs: Long,
    ) : AuthState
}
