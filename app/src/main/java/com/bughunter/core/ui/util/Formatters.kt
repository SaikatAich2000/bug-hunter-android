package com.bughunter.core.ui.util

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val shortDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMM yyyy", Locale.US)

private val shortDateTimeFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale.US)

internal fun Instant.formatRelative(now: Instant = Instant.now()): String {
    val delta = Duration.between(this, now)
    val seconds = delta.seconds
    return when {
        seconds < 0L -> formatShortDateTime()
        seconds < 60L -> "just now"
        seconds < 3_600L -> "${seconds / 60L}m ago"
        seconds < 86_400L -> "${seconds / 3_600L}h ago"
        seconds < 604_800L -> "${seconds / 86_400L}d ago"
        else -> formatShortDateTime()
    }
}

internal fun Instant.formatShortDateTime(zone: ZoneId = ZoneId.systemDefault()): String =
    shortDateTimeFormatter.format(LocalDateTime.ofInstant(this, zone))

internal fun LocalDate.formatShort(): String = shortDateFormatter.format(this)

internal fun Long.formatBytes(): String {
    if (this < 0L) return "0 B"
    if (this < 1024L) return "$this B"
    val units = arrayOf("KB", "MB", "GB", "TB")
    var value = this.toDouble() / 1024.0
    var unitIdx = 0
    while (value >= 1024.0 && unitIdx < units.size - 1) {
        value /= 1024.0
        unitIdx += 1
    }
    return String.format(Locale.US, "%.1f %s", value, units[unitIdx])
}

internal fun displayInitials(displayName: String?, email: String?): String {
    val source = displayName?.trim().takeUnless { it.isNullOrEmpty() }
        ?: email?.substringBefore('@').orEmpty()
    val parts = source.split(' ', '.', '_', '-').filter { it.isNotBlank() }
    return when {
        parts.isEmpty() -> "?"
        parts.size == 1 -> parts[0].take(2).uppercase(Locale.US)
        else -> (parts[0].take(1) + parts[1].take(1)).uppercase(Locale.US)
    }
}
