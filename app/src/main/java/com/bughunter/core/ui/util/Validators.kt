package com.bughunter.core.ui.util

private val EMAIL_REGEX = Regex(
    pattern = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$",
)

private val DIGITS_ONLY = Regex("^\\d+$")
private val RECOVERY_CODE = Regex("^[A-Za-z0-9]{10,12}$")

internal object Email {
    fun isValid(input: String): Boolean {
        val trimmed = input.trim()
        return trimmed.length in 3..254 && EMAIL_REGEX.matches(trimmed)
    }
}

internal object Password {
    // Backend rule: ≥8 chars, at least one letter and one digit.
    fun strengthFloor(input: String): Boolean {
        if (input.length < 8) return false
        var hasLetter = false
        var hasDigit = false
        for (ch in input) {
            if (ch.isLetter()) hasLetter = true
            if (ch.isDigit()) hasDigit = true
            if (hasLetter && hasDigit) return true
        }
        return false
    }

    fun matches(a: String, b: String): Boolean = a.isNotEmpty() && a == b
}

internal object OtpCode {
    fun isValid(input: String): Boolean {
        val trimmed = input.trim()
        return (trimmed.length == 6 && DIGITS_ONLY.matches(trimmed)) ||
            (trimmed.length in 10..12 && RECOVERY_CODE.matches(trimmed))
    }

    fun isNumeric(input: String): Boolean =
        input.length == 6 && DIGITS_ONLY.matches(input)
}

internal object ProjectKey {
    private val KEY = Regex("^[A-Z][A-Z0-9]{1,9}$")
    fun isValid(input: String): Boolean = KEY.matches(input)
}
