package com.bughunter.core.ui.util

import com.google.common.truth.Truth.assertThat
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import org.junit.Test

/**
 * Pure-logic tests for the display formatters. All time inputs are passed
 * an explicit `now` so the relative-time buckets are deterministic and
 * don't depend on the wall clock.
 */
class FormattersTest {

    private val now = Instant.parse("2026-06-11T12:00:00Z")

    @Test
    fun `formatRelative buckets recent instants`() {
        assertThat(now.minusSeconds(5).formatRelative(now)).isEqualTo("just now")
        assertThat(now.minusSeconds(120).formatRelative(now)).isEqualTo("2m ago")
        assertThat(now.minus(3, ChronoUnit.HOURS).formatRelative(now)).isEqualTo("3h ago")
        assertThat(now.minus(2, ChronoUnit.DAYS).formatRelative(now)).isEqualTo("2d ago")
    }

    @Test
    fun `formatRelative falls back to absolute for far past and any future`() {
        // Older than a week → absolute date-time, not a relative bucket.
        val old = now.minus(30, ChronoUnit.DAYS).formatRelative(now)
        assertThat(old).contains("2026")
        // Future instant (negative delta) → absolute, never "in N".
        val future = now.plus(1, ChronoUnit.HOURS).formatRelative(now)
        assertThat(future).contains("2026")
    }

    @Test
    fun `formatShort renders a local date`() {
        assertThat(LocalDate.of(2026, 1, 9).formatShort()).isEqualTo("9 Jan 2026")
        assertThat(LocalDate.of(2026, 12, 25).formatShort()).isEqualTo("25 Dec 2026")
    }

    @Test
    fun `formatBytes scales through units`() {
        assertThat(0L.formatBytes()).isEqualTo("0 B")
        assertThat(512L.formatBytes()).isEqualTo("512 B")
        assertThat(1024L.formatBytes()).isEqualTo("1.0 KB")
        assertThat((1024L * 1024).formatBytes()).isEqualTo("1.0 MB")
        assertThat((1024L * 1024 * 1024).formatBytes()).isEqualTo("1.0 GB")
        assertThat((-5L).formatBytes()).isEqualTo("0 B")
    }

    @Test
    fun `displayInitials prefers name, falls back to email, then placeholder`() {
        assertThat(displayInitials("Ada Lovelace", "ada@x.io")).isEqualTo("AL")
        assertThat(displayInitials("Madonna", null)).isEqualTo("MA")
        assertThat(displayInitials(null, "grace.hopper@navy.mil")).isEqualTo("GH")
        assertThat(displayInitials("  ", "  ")).isEqualTo("?")
        assertThat(displayInitials("jean-luc.picard", null)).isEqualTo("JL")
    }
}
