package com.schoolos.android.core.designsystem

import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * Strips raw Markdown syntax cleanly for single-line chat list previews.
 */
fun stripMarkdownForPreview(raw: String): String {
    if (raw.isBlank()) return raw
    return raw
        .replace(Regex("```[\\w-]*\\n?"), "")
        .replace("```", "")
        .replace(Regex("(?m)^\\s{0,3}#{1,6}\\s+"), "")
        .replace(Regex("(?m)^\\s{0,3}>\\s?"), "")
        .replace(Regex("\\[([^\\]]+)]\\([^)]+\\)"), "$1")
        .replace(Regex("\\*\\*([^*]+)\\*\\*"), "$1")
        .replace(Regex("__([^_]+)__"), "$1")
        .replace(Regex("~~([^~]+)~~"), "$1")
        .replace(Regex("`([^`]+)`"), "$1")
        .replace(Regex("(?m)^\\s*[-*]\\s+"), "• ")
        .replace(Regex("\\s+"), " ")
        .trim()
}

private fun AnnotatedString.Builder.appendMarkdownInline(
    line: String,
    codeBg: Color,
    codeColor: Color,
    linkColor: Color,
) {
    var i = 0
    val n = line.length
    while (i < n) {
        // 1. Bold: **text** or __text__
        if (i + 1 < n && ((line[i] == '*' && line[i + 1] == '*') || (line[i] == '_' && line[i + 1] == '_'))) {
            val delim = line.substring(i, i + 2)
            val close = line.indexOf(delim, i + 2)
            if (close > i + 2) {
                val inner = line.substring(i + 2, close)
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    appendMarkdownInline(inner, codeBg, codeColor, linkColor)
                }
                i = close + 2
                continue
            }
        }

        // 2. Strikethrough: ~~text~~
        if (i + 1 < n && line[i] == '~' && line[i + 1] == '~') {
            val close = line.indexOf("~~", i + 2)
            if (close > i + 2) {
                val inner = line.substring(i + 2, close)
                withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                    appendMarkdownInline(inner, codeBg, codeColor, linkColor)
                }
                i = close + 2
                continue
            }
        }

        // 3. Inline code: `code`
        if (line[i] == '`') {
            val close = line.indexOf('`', i + 1)
            if (close > i + 1) {
                val inner = line.substring(i + 1, close)
                withStyle(
                    SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        background = codeBg,
                        color = codeColor,
                    )
                ) {
                    append(" $inner ")
                }
                i = close + 1
                continue
            }
        }

        // 4. Markdown Link: [label](url)
        if (line[i] == '[') {
            val closeBracket = line.indexOf(']', i + 1)
            if (closeBracket > i + 1 && closeBracket + 1 < n && line[closeBracket + 1] == '(') {
                val closeParen = line.indexOf(')', closeBracket + 2)
                if (closeParen > closeBracket + 2) {
                    val label = line.substring(i + 1, closeBracket)
                    withStyle(
                        SpanStyle(
                            color = linkColor,
                            textDecoration = TextDecoration.Underline,
                            fontWeight = FontWeight.Medium,
                        )
                    ) {
                        append(label)
                    }
                    i = closeParen + 1
                    continue
                }
            }
        }

        // 5. Italic: *text* (only when not preceded/followed blindly inside whitespace)
        if (line[i] == '*' && i + 1 < n && !line[i + 1].isWhitespace()) {
            val close = line.indexOf('*', i + 1)
            if (close > i + 1 && !line[close - 1].isWhitespace()) {
                val inner = line.substring(i + 1, close)
                withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                    appendMarkdownInline(inner, codeBg, codeColor, linkColor)
                }
                i = close + 1
                continue
            }
        }

        append(line[i])
        i++
    }
}

/**
 * Converts AI / Markdown-formatted strings (`### Heading`, `**bold**`, `*italic*`,
 * bullet lists, code spans, blockquotes, links) into styled Compose [AnnotatedString].
 */
fun parseMarkdownToAnnotatedString(
    rawText: String,
    baseFontSize: TextUnit,
    headingColor: Color,
    codeBg: Color,
    codeColor: Color,
    linkColor: Color,
    mutedColor: Color,
): AnnotatedString {
    val normalized = rawText.replace("\r\n", "\n").trimEnd()
    val lines = normalized.split('\n')

    return buildAnnotatedString {
        var inCodeBlock = false
        lines.forEachIndexed { idx, rawLine ->
            val trimmed = rawLine.trim()

            // Fenced code block toggle ```
            if (trimmed.startsWith("```")) {
                inCodeBlock = !inCodeBlock
                if (idx < lines.lastIndex) append('\n')
                return@forEachIndexed
            }

            if (inCodeBlock) {
                withStyle(
                    SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = (baseFontSize.value * 0.90f).sp,
                        background = codeBg,
                        color = codeColor,
                    )
                ) {
                    append(rawLine)
                }
                if (idx < lines.lastIndex) append('\n')
                return@forEachIndexed
            }

            // Horizontal rule --- or ***
            if (trimmed == "---" || trimmed == "***" || trimmed == "___") {
                withStyle(SpanStyle(color = mutedColor.copy(alpha = 0.55f))) {
                    append("────────────")
                }
                if (idx < lines.lastIndex) append('\n')
                return@forEachIndexed
            }

            // Headings # .. ######
            val headingMatch = Regex("^#{1,6}\\s+(.+)$").matchEntire(trimmed)
            if (headingMatch != null) {
                val hashes = trimmed.takeWhile { it == '#' }.length
                val titleBody = headingMatch.groupValues[1].trim()
                val scale = when (hashes) {
                    1 -> 1.16f
                    2 -> 1.11f
                    3 -> 1.06f
                    else -> 1.02f
                }
                withStyle(
                    SpanStyle(
                        fontWeight = FontWeight.Bold,
                        fontSize = (baseFontSize.value * scale).sp,
                        color = headingColor,
                    )
                ) {
                    appendMarkdownInline(titleBody, codeBg, codeColor, linkColor)
                }
                if (idx < lines.lastIndex) append('\n')
                return@forEachIndexed
            }

            // Blockquote > ...
            if (trimmed.startsWith(">")) {
                val quoteBody = trimmed.removePrefix(">").trimStart()
                withStyle(SpanStyle(color = linkColor, fontWeight = FontWeight.Bold)) {
                    append("┃ ")
                }
                withStyle(SpanStyle(fontStyle = FontStyle.Italic, color = mutedColor)) {
                    appendMarkdownInline(quoteBody, codeBg, codeColor, linkColor)
                }
                if (idx < lines.lastIndex) append('\n')
                return@forEachIndexed
            }

            // Unordered list - item / * item
            val bulletMatch = Regex("^(\\s*)[-*]\\s+(.+)$").matchEntire(rawLine)
            if (bulletMatch != null) {
                val indent = if (bulletMatch.groupValues[1].length >= 2) "   " else ""
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = linkColor)) {
                    append("$indent• ")
                }
                appendMarkdownInline(bulletMatch.groupValues[2], codeBg, codeColor, linkColor)
                if (idx < lines.lastIndex) append('\n')
                return@forEachIndexed
            }

            // Ordered list 1. item
            val orderedMatch = Regex("^(\\s*)(\\d+)\\.\\s+(.+)$").matchEntire(rawLine)
            if (orderedMatch != null) {
                val indent = if (orderedMatch.groupValues[1].length >= 2) "   " else ""
                withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = headingColor)) {
                    append("$indent${orderedMatch.groupValues[2]}. ")
                }
                appendMarkdownInline(orderedMatch.groupValues[3], codeBg, codeColor, linkColor)
                if (idx < lines.lastIndex) append('\n')
                return@forEachIndexed
            }

            appendMarkdownInline(rawLine, codeBg, codeColor, linkColor)
            if (idx < lines.lastIndex) append('\n')
        }
    }
}

/**
 * Pure-Compose Markdown text component tailored for SchoolOS AI tutor/copilot/analytics responses.
 */
@Composable
fun MarkdownText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = 13.5.sp,
    lineHeight: TextUnit = 20.sp,
    style: TextStyle = LocalTextStyle.current,
    headingColor: Color = color,
    codeBackground: Color = Color(0x2638BDF8),
    codeColor: Color = Color(0xFF38BDF8),
    linkColor: Color = Color(0xFF38BDF8),
    mutedColor: Color = color.copy(alpha = 0.78f),
    selectable: Boolean = false,
) {
    val resolvedColor = if (color != Color.Unspecified) color else style.color
    val resolvedHeading = if (headingColor != Color.Unspecified) headingColor else resolvedColor
    val resolvedMuted = if (mutedColor != Color.Unspecified) mutedColor else resolvedColor.copy(alpha = 0.78f)

    val parsed = remember(text, fontSize, resolvedHeading, codeBackground, codeColor, linkColor, resolvedMuted) {
        parseMarkdownToAnnotatedString(
            rawText = text,
            baseFontSize = fontSize,
            headingColor = resolvedHeading,
            codeBg = codeBackground,
            codeColor = codeColor,
            linkColor = linkColor,
            mutedColor = resolvedMuted,
        )
    }

    if (selectable) {
        SelectionContainer {
            Text(
                text = parsed,
                modifier = modifier,
                color = resolvedColor,
                fontSize = fontSize,
                lineHeight = lineHeight,
                style = style,
            )
        }
    } else {
        Text(
            text = parsed,
            modifier = modifier,
            color = resolvedColor,
            fontSize = fontSize,
            lineHeight = lineHeight,
            style = style,
        )
    }
}
