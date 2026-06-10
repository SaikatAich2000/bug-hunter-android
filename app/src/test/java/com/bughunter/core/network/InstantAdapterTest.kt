package com.bughunter.core.network

import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.Moshi
import org.junit.Test
import java.time.Instant

/**
 * Pins the three datetime shapes the enterprise backend can return.
 *
 * The bug this guards against (v2.10): the backend's default SQLAlchemy
 * + SQLite datetime serialization emits "2026-06-10T08:52:25" — no Z,
 * no offset. The old InstantAdapter only handled `Instant.parse`
 * (requires Z) and `ISO_OFFSET_DATE_TIME` (requires offset), so every
 * response with a date field threw JsonDataException → silent failure
 * across the app (empty lists, "couldn't load X", dialogs stuck open).
 *
 * Every DTO with a created_at / updated_at field — ProjectOut, BugOut,
 * ActivityOut, UserOut, InvitationOut, MeOut, … — flows through this
 * adapter. A regression here breaks the entire UI.
 */
class InstantAdapterTest {

    private val moshi = Moshi.Builder().add(InstantAdapter()).build()
    private val adapter = moshi.adapter(Instant::class.java)

    @Test
    fun `parses canonical ISO instant with Z`() {
        val parsed = adapter.fromJson("\"2026-06-10T08:52:25Z\"")
        assertThat(parsed).isEqualTo(Instant.parse("2026-06-10T08:52:25Z"))
    }

    @Test
    fun `parses ISO offset datetime`() {
        val parsed = adapter.fromJson("\"2026-06-10T08:52:25+00:00\"")
        assertThat(parsed).isEqualTo(Instant.parse("2026-06-10T08:52:25Z"))
    }

    @Test
    fun `parses ISO offset datetime with non-zero offset`() {
        val parsed = adapter.fromJson("\"2026-06-10T13:52:25+05:00\"")
        assertThat(parsed).isEqualTo(Instant.parse("2026-06-10T08:52:25Z"))
    }

    @Test
    fun `parses naive timezone-less datetime as UTC`() {
        // THIS is the case that broke the entire app. Backend SQLite
        // stores naive datetimes; SQLAlchemy serializes them like this.
        val parsed = adapter.fromJson("\"2026-06-10T08:52:25\"")
        assertThat(parsed).isEqualTo(Instant.parse("2026-06-10T08:52:25Z"))
    }

    @Test
    fun `parses naive datetime with fractional seconds`() {
        val parsed = adapter.fromJson("\"2026-06-10T08:52:25.123456\"")
        assertThat(parsed).isEqualTo(Instant.parse("2026-06-10T08:52:25.123456Z"))
    }

    @Test
    fun `serializes back to canonical ISO instant`() {
        val written = adapter.toJson(Instant.parse("2026-06-10T08:52:25Z"))
        assertThat(written).isEqualTo("\"2026-06-10T08:52:25Z\"")
    }

    @Test
    fun `garbage input throws so callers see a real error`() {
        // Don't swallow malformed strings to a sentinel value — silent
        // recovery would hide genuine backend regressions.
        val ex = runCatching { adapter.fromJson("\"not a date\"") }
        assertThat(ex.isFailure).isTrue()
    }
}
