package com.bughunter.feature.auth

import com.bughunter.core.network.dto.MeOut
import java.time.Duration

internal sealed interface AuthEvent {
    data class LoggedIn(val me: MeOut) : AuthEvent
    data object LoggedOut : AuthEvent
    data class RequiresTotp(val pendingToken: String, val email: String) : AuthEvent
    data object SessionExpired : AuthEvent
    data object BreachRejected : AuthEvent
    data class LockedOut(val retryAfter: Duration) : AuthEvent
}
