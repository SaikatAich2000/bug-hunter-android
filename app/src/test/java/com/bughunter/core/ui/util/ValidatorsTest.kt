package com.bughunter.core.ui.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Pure-logic tests for the input validators. These guard the client-side
 * gates that mirror the backend's accept/reject rules — keeping them in
 * sync matters because a mismatch shows the user a form that submits and
 * then 422s (or blocks a value the server would have accepted).
 */
class ValidatorsTest {

    // --- Email -------------------------------------------------------------
    @Test
    fun `email accepts well-formed addresses`() {
        for (good in listOf(
            "a@b.co", "user.name+tag@example.com", "x_y-z@sub.domain.io",
            "  trimmed@example.com  ", "DIGITS123@host.dev",
        )) {
            assertThat(Email.isValid(good)).isTrue()
        }
    }

    @Test
    fun `email rejects malformed or out-of-range addresses`() {
        for (bad in listOf(
            "", "  ", "a@", "@b.com", "no-at-sign", "a@b", "a@b.c",
            "two@@at.com", "spaces in@email.com", "a@b.com @c.com",
        )) {
            assertThat(Email.isValid(bad)).isFalse()
        }
    }

    @Test
    fun `email rejects addresses longer than 254 chars`() {
        val tooLong = "a".repeat(250) + "@x.com"
        assertThat(Email.isValid(tooLong)).isFalse()
    }

    // --- Password ----------------------------------------------------------
    @Test
    fun `password strengthFloor requires 8+ chars with a letter and a digit`() {
        assertThat(Password.strengthFloor("abcd1234")).isTrue()
        assertThat(Password.strengthFloor("a1a1a1a1a1")).isTrue()
    }

    @Test
    fun `password strengthFloor rejects short or single-class inputs`() {
        assertThat(Password.strengthFloor("a1b2")).isFalse()       // too short
        assertThat(Password.strengthFloor("abcdefgh")).isFalse()   // no digit
        assertThat(Password.strengthFloor("12345678")).isFalse()   // no letter
        assertThat(Password.strengthFloor("")).isFalse()
    }

    @Test
    fun `password matches only when non-empty and equal`() {
        assertThat(Password.matches("hunter2!", "hunter2!")).isTrue()
        assertThat(Password.matches("a", "b")).isFalse()
        assertThat(Password.matches("", "")).isFalse()
    }

    // --- OtpCode -----------------------------------------------------------
    @Test
    fun `otp accepts 6-digit codes and 10-12 char recovery codes`() {
        assertThat(OtpCode.isValid("123456")).isTrue()
        assertThat(OtpCode.isValid("  654321 ")).isTrue()
        assertThat(OtpCode.isValid("ABCD123456")).isTrue()        // 10 chars
        assertThat(OtpCode.isValid("ABCD12345678")).isTrue()      // 12 chars
    }

    @Test
    fun `otp rejects wrong lengths and non-alphanumerics`() {
        assertThat(OtpCode.isValid("12345")).isFalse()
        assertThat(OtpCode.isValid("1234567")).isFalse()
        assertThat(OtpCode.isValid("ABCD-12345")).isFalse()
        assertThat(OtpCode.isValid("ABCD123456789")).isFalse()    // 13 chars
        assertThat(OtpCode.isValid("")).isFalse()
    }

    @Test
    fun `otp isNumeric is strict 6-digit only`() {
        assertThat(OtpCode.isNumeric("123456")).isTrue()
        assertThat(OtpCode.isNumeric("12345")).isFalse()
        assertThat(OtpCode.isNumeric("12345a")).isFalse()
    }

    // --- ProjectKey --------------------------------------------------------
    @Test
    fun `project key requires uppercase leading letter then 1-9 alnum`() {
        assertThat(ProjectKey.isValid("AB")).isTrue()
        assertThat(ProjectKey.isValid("PROJ1")).isTrue()
        assertThat(ProjectKey.isValid("A1B2C3D4E5")).isTrue()     // 10 chars
    }

    @Test
    fun `project key rejects lowercase, too-short, or too-long`() {
        assertThat(ProjectKey.isValid("A")).isFalse()             // 1 char
        assertThat(ProjectKey.isValid("ab")).isFalse()            // lowercase
        assertThat(ProjectKey.isValid("1AB")).isFalse()           // leading digit
        assertThat(ProjectKey.isValid("ABCDEFGHIJK")).isFalse()   // 11 chars
        assertThat(ProjectKey.isValid("")).isFalse()
    }
}
