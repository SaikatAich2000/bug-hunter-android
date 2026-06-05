package com.bughunter.core.ui.util

// Defence in depth: server pre-sanitizes, but BhRichHtml only renders an allow-listed tag set.
internal object HtmlSanitizer {

    private val ALLOWED_TAGS: Set<String> = setOf(
        "b", "strong", "i", "em", "u", "s", "del", "ins",
        "p", "br", "div", "span",
        "ul", "ol", "li",
        "blockquote", "pre", "code",
        "h1", "h2", "h3", "h4", "h5", "h6",
        "a", "img",
    )

    private val ALLOWED_ATTRS: Map<String, Set<String>> = mapOf(
        "a" to setOf("href", "title", "rel", "target"),
        "img" to setOf("src", "alt", "title", "width", "height"),
        "span" to setOf("class"),
        "div" to setOf("class"),
        "code" to setOf("class"),
        "pre" to setOf("class"),
    )

    private val SAFE_URI_SCHEMES: Set<String> = setOf("http", "https", "mailto", "tel")

    private val TAG_REGEX = Regex(
        pattern = "<(/?)([a-zA-Z][a-zA-Z0-9]*)([^>]*)>",
    )

    private val ATTR_REGEX = Regex(
        pattern = "([a-zA-Z_:][-a-zA-Z0-9_:.]*)\\s*=\\s*\"([^\"]*)\"|([a-zA-Z_:][-a-zA-Z0-9_:.]*)\\s*=\\s*'([^']*)'",
    )

    fun sanitize(rawHtml: String): String {
        if (rawHtml.isEmpty()) return ""
        return TAG_REGEX.replace(rawHtml) { match ->
            val closing = match.groupValues[1]
            val tag = match.groupValues[2].lowercase()
            val attrsRaw = match.groupValues[3]
            if (tag !in ALLOWED_TAGS) return@replace ""
            if (closing == "/") return@replace "</$tag>"
            val cleanedAttrs = sanitizeAttrs(tag, attrsRaw)
            if (cleanedAttrs.isEmpty()) "<$tag>" else "<$tag $cleanedAttrs>"
        }
    }

    private fun sanitizeAttrs(tag: String, raw: String): String {
        val allowed = ALLOWED_ATTRS[tag] ?: return ""
        val acc = StringBuilder()
        for (match in ATTR_REGEX.findAll(raw)) {
            val name = (match.groupValues[1].ifEmpty { match.groupValues[3] }).lowercase()
            val value = match.groupValues[2].ifEmpty { match.groupValues[4] }
            if (name !in allowed) continue
            if (name.startsWith("on")) continue
            if ((name == "href" || name == "src") && !isSafeUri(value)) continue
            if (acc.isNotEmpty()) acc.append(' ')
            acc.append(name).append("=\"").append(escapeAttr(value)).append('"')
        }
        return acc.toString()
    }

    private fun isSafeUri(value: String): Boolean {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return false
        if (trimmed.startsWith("/") || trimmed.startsWith("#")) return true
        val colonIdx = trimmed.indexOf(':')
        if (colonIdx < 0) return true
        val scheme = trimmed.substring(0, colonIdx).lowercase()
        return scheme in SAFE_URI_SCHEMES
    }

    private fun escapeAttr(value: String): String =
        value.replace("&", "&amp;").replace("\"", "&quot;").replace("<", "&lt;").replace(">", "&gt;")

    fun stripAllTags(rawHtml: String): String =
        TAG_REGEX.replace(rawHtml, "")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
}
