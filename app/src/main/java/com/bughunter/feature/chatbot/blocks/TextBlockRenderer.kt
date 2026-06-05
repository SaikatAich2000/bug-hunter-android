package com.bughunter.feature.chatbot.blocks

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.bughunter.core.ui.components.BhRichHtml
import com.bughunter.feature.chatbot.RenderedChatBlock

@Composable
internal fun TextBlockRenderer(
    block: RenderedChatBlock.Text,
    modifier: Modifier = Modifier,
) {
    val rendered = if (block.format == "html") {
        block.text
    } else {
        markdownToHtml(block.text)
    }
    if (block.format == "plain" || rendered == block.text) {
        Text(
            text = block.text,
            style = MaterialTheme.typography.bodyMedium,
            modifier = modifier,
        )
    } else {
        BhRichHtml(html = rendered, modifier = modifier)
    }
}

private val BOLD_RE = Regex("\\*\\*([^*]+)\\*\\*")
private val ITALIC_RE = Regex("(?<!\\*)\\*([^*]+)\\*(?!\\*)")
private val INLINE_CODE_RE = Regex("`([^`]+)`")
private val LINK_RE = Regex("\\[([^\\]]+)]\\(([^)]+)\\)")

internal fun markdownToHtml(input: String): String {
    val lines = input.lines()
    val out = StringBuilder()
    var inList = false
    for (line in lines) {
        val trimmed = line.trimStart()
        if (trimmed.startsWith("- ") || trimmed.startsWith("* ")) {
            if (!inList) {
                out.append("<ul>")
                inList = true
            }
            out.append("<li>").append(inline(trimmed.removePrefix("- ").removePrefix("* "))).append("</li>")
        } else {
            if (inList) {
                out.append("</ul>")
                inList = false
            }
            if (trimmed.isNotEmpty()) {
                out.append("<p>").append(inline(trimmed)).append("</p>")
            }
        }
    }
    if (inList) out.append("</ul>")
    return out.toString()
}

private fun inline(text: String): String {
    var out = text
    out = LINK_RE.replace(out) { m -> "<a href=\"${m.groupValues[2]}\">${m.groupValues[1]}</a>" }
    out = BOLD_RE.replace(out) { m -> "<strong>${m.groupValues[1]}</strong>" }
    out = ITALIC_RE.replace(out) { m -> "<em>${m.groupValues[1]}</em>" }
    out = INLINE_CODE_RE.replace(out) { m -> "<code>${m.groupValues[1]}</code>" }
    return out
}
