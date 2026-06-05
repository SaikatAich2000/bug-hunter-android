package com.bughunter.core.network

import java.time.Instant

sealed interface AuthEvent {
    data class LoggedOut(val reason: Reason) : AuthEvent {
        enum class Reason { ServerExpired, UserInitiated, CsrfBootstrapFailed }
    }

    data class LockedOut(val until: Instant) : AuthEvent

    data object CsrfReseedNeeded : AuthEvent
}
