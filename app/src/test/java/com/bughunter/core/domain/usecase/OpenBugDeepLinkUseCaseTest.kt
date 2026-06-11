package com.bughunter.core.domain.usecase

import com.bughunter.core.network.DomainError
import com.bughunter.core.network.Result2
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * OpenBugDeepLinkUseCase turns the three deep-link shapes the app accepts
 * (app:// scheme, sleuth chatbot action, and a #bug-N hash) into a bug id,
 * or NotFound for anything it doesn't recognise.
 */
class OpenBugDeepLinkUseCaseTest {

    private val useCase = OpenBugDeepLinkUseCase()

    private fun idOf(raw: String?): Int? =
        (useCase(raw) as? Result2.Ok)?.value

    @Test
    fun `parses the app scheme link`() {
        assertThat(idOf("app://bughunter/bug/42")).isEqualTo(42)
    }

    @Test
    fun `parses the sleuth action link`() {
        assertThat(idOf("sleuth:open-bug?id=7")).isEqualTo(7)
    }

    @Test
    fun `parses the bug hash with and without leading hash`() {
        assertThat(idOf("#bug-15")).isEqualTo(15)
        assertThat(idOf("bug-15")).isEqualTo(15)
    }

    @Test
    fun `trims surrounding whitespace`() {
        assertThat(idOf("  app://bughunter/bug/3  ")).isEqualTo(3)
    }

    @Test
    fun `null, blank, and unrecognised inputs yield NotFound`() {
        for (bad in listOf(null, "", "   ", "https://example.com", "app://bughunter/bug/abc", "bug-")) {
            val result = useCase(bad)
            assertThat(result).isInstanceOf(Result2.Err::class.java)
            assertThat((result as Result2.Err).error).isEqualTo(DomainError.NotFound)
        }
    }
}
