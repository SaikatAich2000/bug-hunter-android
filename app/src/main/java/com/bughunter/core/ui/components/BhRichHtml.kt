package com.bughunter.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.bughunter.core.ui.theme.BhMonoFontFamily
import com.bughunter.core.ui.util.HtmlSanitizer

@Composable
fun BhRichHtml(
    html: String,
    modifier: Modifier = Modifier,
) {
    val sanitized = remember(html) { HtmlSanitizer.sanitize(html) }
    val nodes = remember(sanitized) { parseSimpleHtml(sanitized) }
    val accent = MaterialTheme.colorScheme.primary
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        for (node in nodes) {
            when (node) {
                is HtmlNode.Paragraph -> {
                    val annotated = buildInlineAnnotated(node.spans, accent)
                    Text(
                        text = annotated,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                is HtmlNode.Heading -> {
                    Text(
                        text = buildInlineAnnotated(node.spans, accent),
                        style = when (node.level) {
                            1 -> MaterialTheme.typography.headlineMedium
                            2 -> MaterialTheme.typography.headlineSmall
                            3 -> MaterialTheme.typography.titleLarge
                            4 -> MaterialTheme.typography.titleMedium
                            else -> MaterialTheme.typography.titleSmall
                        },
                    )
                }
                is HtmlNode.ListBlock -> {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        node.items.forEachIndexed { idx, item ->
                            val prefix = if (node.ordered) "${idx + 1}. " else "• "
                            Text(
                                text = buildAnnotatedString {
                                    append(prefix)
                                    append(buildInlineAnnotated(item, accent))
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(start = 8.dp),
                            )
                        }
                    }
                }
                is HtmlNode.CodeBlock -> {
                    Text(
                        text = node.text,
                        style = TextStyle(
                            fontFamily = BhMonoFontFamily,
                            fontSize = 13.sp,
                            color = LocalContentColor.current,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                    )
                }
                is HtmlNode.BlockQuote -> {
                    Text(
                        text = buildInlineAnnotated(node.spans, accent),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontStyle = FontStyle.Italic,
                        ),
                        modifier = Modifier.padding(start = 12.dp),
                    )
                }
                is HtmlNode.Image -> {
                    AsyncImage(
                        model = node.src,
                        contentDescription = node.alt,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 320.dp),
                    )
                }
            }
        }
    }
}

private sealed interface HtmlNode {
    data class Paragraph(val spans: List<InlineSpan>) : HtmlNode
    data class Heading(val level: Int, val spans: List<InlineSpan>) : HtmlNode
    data class ListBlock(val ordered: Boolean, val items: List<List<InlineSpan>>) : HtmlNode
    data class CodeBlock(val text: String) : HtmlNode
    data class BlockQuote(val spans: List<InlineSpan>) : HtmlNode
    data class Image(val src: String, val alt: String?) : HtmlNode
}

private sealed interface InlineSpan {
    data class Plain(val text: String, val styles: Set<InlineStyle>) : InlineSpan
    data class Link(val text: String, val href: String, val styles: Set<InlineStyle>) : InlineSpan
    data class LineBreak(val ignored: Unit = Unit) : InlineSpan
}

private enum class InlineStyle { BOLD, ITALIC, UNDERLINE, STRIKE, CODE }

private fun parseSimpleHtml(html: String): List<HtmlNode> {
    val nodes = mutableListOf<HtmlNode>()
    val tokens = tokenize(html)
    var i = 0
    val styleStack: ArrayDeque<InlineStyle> = ArrayDeque()
    val inlineBuffer = mutableListOf<InlineSpan>()
    var pendingHref: String? = null

    fun flushParagraph() {
        if (inlineBuffer.isNotEmpty()) {
            nodes.add(HtmlNode.Paragraph(inlineBuffer.toList()))
            inlineBuffer.clear()
        }
    }

    while (i < tokens.size) {
        val tok = tokens[i]
        when (tok) {
            is Token.Open -> {
                when (tok.tag) {
                    "p", "div" -> {
                        flushParagraph()
                    }
                    "br" -> inlineBuffer.add(InlineSpan.LineBreak())
                    "b", "strong" -> styleStack.addLast(InlineStyle.BOLD)
                    "i", "em" -> styleStack.addLast(InlineStyle.ITALIC)
                    "u" -> styleStack.addLast(InlineStyle.UNDERLINE)
                    "s", "del", "strike" -> styleStack.addLast(InlineStyle.STRIKE)
                    "code" -> styleStack.addLast(InlineStyle.CODE)
                    "a" -> pendingHref = tok.attrs["href"]
                    "h1", "h2", "h3", "h4", "h5", "h6" -> {
                        flushParagraph()
                        val level = tok.tag.substring(1).toInt()
                        val collected = collectInlineUntilClose(tokens, i + 1, tok.tag, styleStack)
                        nodes.add(HtmlNode.Heading(level, collected.spans))
                        i = collected.nextIndex
                        continue
                    }
                    "blockquote" -> {
                        flushParagraph()
                        val collected = collectInlineUntilClose(tokens, i + 1, "blockquote", styleStack)
                        nodes.add(HtmlNode.BlockQuote(collected.spans))
                        i = collected.nextIndex
                        continue
                    }
                    "pre" -> {
                        flushParagraph()
                        val sb = StringBuilder()
                        var j = i + 1
                        while (j < tokens.size) {
                            val t = tokens[j]
                            if (t is Token.Close && t.tag == "pre") break
                            if (t is Token.Text) sb.append(t.text)
                            j++
                        }
                        nodes.add(HtmlNode.CodeBlock(sb.toString()))
                        i = j + 1
                        continue
                    }
                    "ul", "ol" -> {
                        flushParagraph()
                        val ordered = tok.tag == "ol"
                        val items = mutableListOf<List<InlineSpan>>()
                        var j = i + 1
                        while (j < tokens.size) {
                            val t = tokens[j]
                            if (t is Token.Close && (t.tag == "ul" || t.tag == "ol")) break
                            if (t is Token.Open && t.tag == "li") {
                                val itemCollected = collectInlineUntilClose(tokens, j + 1, "li", styleStack)
                                items.add(itemCollected.spans)
                                j = itemCollected.nextIndex
                            } else {
                                j++
                            }
                        }
                        nodes.add(HtmlNode.ListBlock(ordered, items))
                        i = j + 1
                        continue
                    }
                    "img" -> {
                        val src = tok.attrs["src"]
                        if (!src.isNullOrEmpty()) {
                            flushParagraph()
                            nodes.add(HtmlNode.Image(src, tok.attrs["alt"]))
                        }
                    }
                    else -> Unit
                }
            }
            is Token.Close -> {
                when (tok.tag) {
                    "p", "div" -> flushParagraph()
                    "b", "strong" -> if (styleStack.lastOrNull() == InlineStyle.BOLD) styleStack.removeLast()
                    "i", "em" -> if (styleStack.lastOrNull() == InlineStyle.ITALIC) styleStack.removeLast()
                    "u" -> if (styleStack.lastOrNull() == InlineStyle.UNDERLINE) styleStack.removeLast()
                    "s", "del", "strike" -> if (styleStack.lastOrNull() == InlineStyle.STRIKE) styleStack.removeLast()
                    "code" -> if (styleStack.lastOrNull() == InlineStyle.CODE) styleStack.removeLast()
                    "a" -> pendingHref = null
                    else -> Unit
                }
            }
            is Token.Text -> {
                val styles = styleStack.toSet()
                val href = pendingHref
                if (href != null) {
                    inlineBuffer.add(InlineSpan.Link(tok.text, href, styles))
                } else if (tok.text.isNotEmpty()) {
                    inlineBuffer.add(InlineSpan.Plain(tok.text, styles))
                }
            }
        }
        i++
    }
    flushParagraph()
    return nodes
}

private data class InlineCollect(val spans: List<InlineSpan>, val nextIndex: Int)

private fun collectInlineUntilClose(
    tokens: List<Token>,
    startIdx: Int,
    closeTag: String,
    parentStyles: ArrayDeque<InlineStyle>,
): InlineCollect {
    val spans = mutableListOf<InlineSpan>()
    val styleStack = ArrayDeque<InlineStyle>().apply { addAll(parentStyles) }
    var pendingHref: String? = null
    var i = startIdx
    while (i < tokens.size) {
        val t = tokens[i]
        if (t is Token.Close && t.tag == closeTag) {
            return InlineCollect(spans, i + 1)
        }
        when (t) {
            is Token.Open -> when (t.tag) {
                "b", "strong" -> styleStack.addLast(InlineStyle.BOLD)
                "i", "em" -> styleStack.addLast(InlineStyle.ITALIC)
                "u" -> styleStack.addLast(InlineStyle.UNDERLINE)
                "s", "del", "strike" -> styleStack.addLast(InlineStyle.STRIKE)
                "code" -> styleStack.addLast(InlineStyle.CODE)
                "br" -> spans.add(InlineSpan.LineBreak())
                "a" -> pendingHref = t.attrs["href"]
                else -> Unit
            }
            is Token.Close -> when (t.tag) {
                "b", "strong" -> if (styleStack.lastOrNull() == InlineStyle.BOLD) styleStack.removeLast()
                "i", "em" -> if (styleStack.lastOrNull() == InlineStyle.ITALIC) styleStack.removeLast()
                "u" -> if (styleStack.lastOrNull() == InlineStyle.UNDERLINE) styleStack.removeLast()
                "s", "del", "strike" -> if (styleStack.lastOrNull() == InlineStyle.STRIKE) styleStack.removeLast()
                "code" -> if (styleStack.lastOrNull() == InlineStyle.CODE) styleStack.removeLast()
                "a" -> pendingHref = null
                else -> Unit
            }
            is Token.Text -> {
                val styles = styleStack.toSet()
                val href = pendingHref
                if (href != null) spans.add(InlineSpan.Link(t.text, href, styles))
                else if (t.text.isNotEmpty()) spans.add(InlineSpan.Plain(t.text, styles))
            }
        }
        i++
    }
    return InlineCollect(spans, i)
}

private sealed interface Token {
    data class Open(val tag: String, val attrs: Map<String, String>) : Token
    data class Close(val tag: String) : Token
    data class Text(val text: String) : Token
}

private val TAG_REGEX = Regex("<(/?)([a-zA-Z][a-zA-Z0-9]*)([^>]*)>")
private val ATTR_REGEX = Regex("([a-zA-Z][-a-zA-Z0-9]*)\\s*=\\s*\"([^\"]*)\"")

private fun tokenize(html: String): List<Token> {
    val out = mutableListOf<Token>()
    var cursor = 0
    val matches = TAG_REGEX.findAll(html).toList()
    for (m in matches) {
        if (m.range.first > cursor) {
            val text = decodeEntities(html.substring(cursor, m.range.first))
            if (text.isNotEmpty()) out.add(Token.Text(text))
        }
        val closing = m.groupValues[1] == "/"
        val tag = m.groupValues[2].lowercase()
        if (closing) {
            out.add(Token.Close(tag))
        } else {
            val attrs = ATTR_REGEX.findAll(m.groupValues[3]).associate { am ->
                am.groupValues[1].lowercase() to am.groupValues[2]
            }
            out.add(Token.Open(tag, attrs))
        }
        cursor = m.range.last + 1
    }
    if (cursor < html.length) {
        val tail = decodeEntities(html.substring(cursor))
        if (tail.isNotEmpty()) out.add(Token.Text(tail))
    }
    return out
}

private fun decodeEntities(text: String): String = text
    .replace("&nbsp;", " ")
    .replace("&amp;", "&")
    .replace("&lt;", "<")
    .replace("&gt;", ">")
    .replace("&quot;", "\"")
    .replace("&#39;", "'")

private fun buildInlineAnnotated(spans: List<InlineSpan>, accent: Color): AnnotatedString = buildAnnotatedString {
    for (span in spans) {
        when (span) {
            is InlineSpan.LineBreak -> append('\n')
            is InlineSpan.Plain -> withStyle(spanStyleFor(span.styles, accent)) { append(span.text) }
            is InlineSpan.Link -> {
                val linkStyle = TextLinkStyles(
                    style = SpanStyle(color = accent, textDecoration = TextDecoration.Underline),
                )
                withLink(LinkAnnotation.Url(url = span.href, styles = linkStyle)) {
                    withStyle(spanStyleFor(span.styles, accent)) { append(span.text) }
                }
            }
        }
    }
}

private fun spanStyleFor(styles: Set<InlineStyle>, accent: Color): SpanStyle {
    var weight: FontWeight? = null
    var italic: FontStyle? = null
    val deco = mutableListOf<TextDecoration>()
    var family: FontFamily? = null
    var color: Color = Color.Unspecified
    if (InlineStyle.BOLD in styles) weight = FontWeight.Bold
    if (InlineStyle.ITALIC in styles) italic = FontStyle.Italic
    if (InlineStyle.UNDERLINE in styles) deco.add(TextDecoration.Underline)
    if (InlineStyle.STRIKE in styles) deco.add(TextDecoration.LineThrough)
    if (InlineStyle.CODE in styles) {
        family = BhMonoFontFamily
        color = accent
    }
    return SpanStyle(
        fontWeight = weight,
        fontStyle = italic,
        fontFamily = family,
        textDecoration = if (deco.isEmpty()) null else TextDecoration.combine(deco),
        color = color,
    )
}

