package com.bughunter.core.network

import java.time.Duration

data class FieldError(
    val location: List<String>,
    val message: String,
    val type: String,
) {
    val fieldName: String? get() = location.lastOrNull()
}

sealed class DomainError {
    data object Unauthorized : DomainError()
    data object Forbidden : DomainError()
    data object NotFound : DomainError()
    data object Conflict : DomainError()
    data class Validation(val fieldErrors: List<FieldError>, val message: String? = null) : DomainError()
    data class RateLimited(val retryAfter: Duration?) : DomainError()
    data class Server(val message: String) : DomainError()
    data object Network : DomainError()
    data class Unknown(val throwable: Throwable) : DomainError()
}
