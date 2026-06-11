package com.bughunter.core.ui.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Tests the client-side HTML allow-list. This is defence-in-depth: the
 * server pre-sanitizes, but BhRichHtml must never render an event handler,
 * a javascript: URL, or a tag outside the allow-list even if a malicious
 * payload slips past the backend.
 */
class HtmlSanitizerTest {

    @Test
    fun `keeps allow-listed tags`() {
        val out = HtmlSanitizer.sanitize("<p>Hello <b>world</b> and <em>friends</em></p>")
        assertThat(out).isEqualTo("<p>Hello <b>world</b> and <em>friends</em></p>")
    }

    @Test
    fun `drops disallowed tags but keeps their text`() {
        val out = HtmlSanitizer.sanitize("<script>alert(1)</script><p>safe</p>")
        assertThat(out).doesNotContain("<script>")
        assertThat(out).doesNotContain("</script>")
        assertThat(out).contains("alert(1)")     // inner text survives
        assertThat(out).contains("<p>safe</p>")
    }

    @Test
    fun `strips event-handler attributes`() {
        val out = HtmlSanitizer.sanitize("""<a href="https://x.io" onclick="evil()">link</a>""")
        assertThat(out).contains("""href="https://x.io"""")
        assertThat(out).doesNotContain("onclick")
    }

    @Test
    fun `drops javascript and data URIs but keeps safe schemes`() {
        val js = HtmlSanitizer.sanitize("""<a href="javascript:alert(1)">x</a>""")
        assertThat(js).doesNotContain("javascript:")
        val mailto = HtmlSanitizer.sanitize("""<a href="mailto:a@b.io">mail</a>""")
        assertThat(mailto).contains("mailto:a@b.io")
        val relative = HtmlSanitizer.sanitize("""<a href="/local/path">rel</a>""")
        assertThat(relative).contains("""href="/local/path"""")
    }

    @Test
    fun `drops attributes not allow-listed for a tag`() {
        // `style` is never allow-listed; `class` is allowed on span.
        val out = HtmlSanitizer.sanitize("""<span class="ok" style="color:red">t</span>""")
        assertThat(out).contains("""class="ok"""")
        assertThat(out).doesNotContain("style")
    }

    @Test
    fun `escapes special characters inside kept attribute values`() {
        // A raw ampersand and angle bracket in a kept value must come back
        // entity-escaped so they can't break out of the attribute.
        val out = HtmlSanitizer.sanitize("""<a href="/path" title="r&d <x">t</a>""")
        assertThat(out).contains("&amp;")
        assertThat(out).contains("&lt;")
    }

    @Test
    fun `empty input yields empty output`() {
        assertThat(HtmlSanitizer.sanitize("")).isEmpty()
    }

    @Test
    fun `stripAllTags removes markup and decodes entities`() {
        val out = HtmlSanitizer.stripAllTags("<p>Hello&nbsp;<b>there</b> &amp; welcome &lt;3</p>")
        assertThat(out).isEqualTo("Hello there & welcome <3")
    }
}
