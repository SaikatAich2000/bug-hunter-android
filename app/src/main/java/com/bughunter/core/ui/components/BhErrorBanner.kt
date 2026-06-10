package com.bughunter.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bughunter.core.network.DomainError

/**
 * Tinted error banner used across every form (login, signup, create
 * project, create bug, create event, settings, ...).
 *
 * Goals:
 *   - Make errors VISIBLE: previously, the pattern was a small red
 *     `Text` underneath the form. On slow networks the user already had
 *     their thumb on the submit button when it arrived and couldn't
 *     see it. The tinted-background banner forces attention.
 *   - Single canonical UX: every screen looks the same on error, so
 *     users learn to scan for the banner.
 *   - Single canonical error mapping: every screen used to roll its
 *     own `messageFor(error)`; lots of subtle differences. The
 *     `domainErrorMessage` helper here is the one truth.
 *
 * Returns a Composable that no-ops when `error` is null — so callers
 * can sprinkle it without conditional blocks.
 */
@Composable
fun BhErrorBanner(
    error: DomainError?,
    modifier: Modifier = Modifier,
    overrideMessage: String? = null,
) {
    if (error == null && overrideMessage == null) return
    val text = overrideMessage ?: domainErrorMessage(error!!)
    Text(
        text = text,
        color = MaterialTheme.colorScheme.onErrorContainer,
        style = MaterialTheme.typography.bodyMedium,
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(8.dp),
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
    )
}

/**
 * Canonical mapping from DomainError to user-facing copy. Per-screen
 * overrides (e.g. "Invalid email or password" on login) still belong in
 * the screen layer; this helper handles the generic ones every form
 * shares.
 */
fun domainErrorMessage(error: DomainError): String = when (error) {
    DomainError.Network ->
        "Network unavailable. Check your connection and try again."
    DomainError.Unauthorized ->
        "Your session expired. Please sign in again."
    DomainError.Forbidden ->
        "You don't have permission to perform this action."
    DomainError.NotFound ->
        "We couldn't find what you were looking for."
    DomainError.Conflict ->
        "That conflicts with something that already exists."
    is DomainError.Validation ->
        error.message
            ?: error.fieldErrors.firstOrNull()?.message
            ?: "Please review the form."
    is DomainError.RateLimited ->
        "Too many attempts. Please wait a bit and try again."
    is DomainError.Server ->
        error.message.ifBlank { "The server hit an error. Please try again." }
    is DomainError.Unknown -> {
        val cause = error.throwable.message
        if (cause.isNullOrBlank()) "Something went wrong." else "Error: $cause"
    }
}
