package com.bughunter.core.ui.components

import com.bughunter.core.network.DomainError
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.Duration

/**
 * Verifies the canonical DomainError → user-facing string mapping used
 * by the shared BhErrorBanner. The composable itself is JVM-pure (no
 * Android dependency in the mapping function), so we can assert without
 * Robolectric or a device.
 *
 * Locking these messages down prevents the silent-fail regression: if
 * anyone replaces a mapping with `""` or a null, every form using
 * BhErrorBanner would render an empty banner (no text), and users
 * would once again hit "no error displayed" — the bug this whole
 * banner-everywhere effort was meant to kill.
 */
class BhErrorBannerTest {

    @Test
    fun `network error is human-readable`() {
        val msg = domainErrorMessage(DomainError.Network)
        assertThat(msg).isNotEmpty()
        assertThat(msg.lowercase()).contains("network")
    }

    @Test
    fun `forbidden mentions permission`() {
        val msg = domainErrorMessage(DomainError.Forbidden)
        assertThat(msg.lowercase()).contains("permission")
    }

    @Test
    fun `unauthorized prompts re-sign-in`() {
        val msg = domainErrorMessage(DomainError.Unauthorized)
        assertThat(msg.lowercase()).contains("sign in")
    }

    @Test
    fun `validation prefers explicit message`() {
        val msg = domainErrorMessage(
            DomainError.Validation(emptyList(), message = "Name is required."),
        )
        assertThat(msg).isEqualTo("Name is required.")
    }

    @Test
    fun `validation falls back to first field error when message null`() {
        val msg = domainErrorMessage(
            DomainError.Validation(
                listOf(
                    com.bughunter.core.network.FieldError(
                        location = listOf("body", "name"),
                        message = "Name must be at least 2 characters.",
                        type = "value_error",
                    ),
                ),
                message = null,
            ),
        )
        assertThat(msg).contains("Name must be at least 2 characters.")
    }

    @Test
    fun `validation falls back to generic when nothing to show`() {
        val msg = domainErrorMessage(DomainError.Validation(emptyList(), message = null))
        assertThat(msg).isNotEmpty()
    }

    @Test
    fun `rate-limited mentions waiting`() {
        val msg = domainErrorMessage(DomainError.RateLimited(Duration.ofSeconds(30)))
        assertThat(msg.lowercase()).contains("wait")
    }

    @Test
    fun `server uses backend message when present`() {
        val msg = domainErrorMessage(DomainError.Server("Database is down."))
        assertThat(msg).isEqualTo("Database is down.")
    }

    @Test
    fun `server falls back when backend message is blank`() {
        val msg = domainErrorMessage(DomainError.Server(""))
        assertThat(msg).isNotEmpty()
    }

    @Test
    fun `unknown surfaces throwable message`() {
        val msg = domainErrorMessage(DomainError.Unknown(IllegalStateException("Boom")))
        assertThat(msg).contains("Boom")
    }

    @Test
    fun `unknown without message still gives generic copy`() {
        val msg = domainErrorMessage(DomainError.Unknown(IllegalStateException()))
        assertThat(msg).isNotEmpty()
    }

    @Test
    fun `no DomainError variant maps to empty string`() {
        // Iterate every known DomainError variant we can synthesise and
        // assert non-empty output. Catches the "silent empty banner"
        // regression where a future variant returns "" by accident.
        val variants: List<DomainError> = listOf(
            DomainError.Network,
            DomainError.Unauthorized,
            DomainError.Forbidden,
            DomainError.NotFound,
            DomainError.Conflict,
            DomainError.Validation(emptyList(), "Test"),
            DomainError.RateLimited(null),
            DomainError.Server("S"),
            DomainError.Unknown(RuntimeException("X")),
        )
        variants.forEach { v ->
            assertThat(domainErrorMessage(v)).isNotEmpty()
        }
    }
}
