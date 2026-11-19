package com.schoolos.android.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest

/**
 * Resolves relative `/uploads/...` media paths from the SchoolOS backend into absolute URLs
 * while keeping full URLs, `data:`, `content://`, and `file://` URIs untouched.
 */
fun resolveServerMediaUrl(rawUrl: String): String {
    val trimmed = rawUrl.trim()
    if (trimmed.isBlank()) return ""
    if (trimmed.startsWith("http://", ignoreCase = true) ||
        trimmed.startsWith("https://", ignoreCase = true) ||
        trimmed.startsWith("data:", ignoreCase = true) ||
        trimmed.startsWith("content://", ignoreCase = true) ||
        trimmed.startsWith("file://", ignoreCase = true)
    ) {
        return trimmed
    }
    val rawApi = com.schoolos.android.core.common.BuildConfig.API_BASE_URL.trimEnd('/')
    val rootBase = when {
        rawApi.endsWith("/api/v1") -> rawApi.removeSuffix("/api/v1").trimEnd('/')
        rawApi.endsWith("/api") -> rawApi.removeSuffix("/api").trimEnd('/')
        else -> rawApi
    }
    return when {
        trimmed.startsWith("/") -> "$rootBase$trimmed"
        trimmed.startsWith("uploads/", ignoreCase = true) -> "$rootBase/$trimmed"
        else -> "$rootBase/uploads/$trimmed"
    }
}

private val QUESTION_MD_IMAGE_REGEX = Regex("""!\[[^\]\n]*\]\(([^)\s]+)\)""")

/**
 * Extracts a safe diagram/image URL embedded in question text (`![Diagram Soal](...)`)
 * and returns `(resolvedImageUrlOrNull, cleanedQuestionText)`.
 */
fun extractQuestionDiagramAndText(rawText: String, explicitImageUrl: String? = null): Pair<String?, String> {
    val firstMdUrl = QUESTION_MD_IMAGE_REGEX.find(rawText)?.groupValues?.getOrNull(1)?.trim()
    val cleanedText = QUESTION_MD_IMAGE_REGEX.replace(rawText, "").trim()
    val chosenRaw = explicitImageUrl?.trim()?.takeIf { it.isNotBlank() } ?: firstMdUrl
    val resolved = chosenRaw
        ?.takeIf { !it.contains("unsplash.com", ignoreCase = true) }
        ?.let { resolveServerMediaUrl(it) }
        ?.takeIf { it.isNotBlank() }
    return resolved to cleanedText
}

/**
 * Crisp white-card diagram viewer ensuring SVG charts, geometry, and science schemas
 * remain sharp and high-contrast across both Light and Dark app themes.
 */
@Composable
fun QuestionDiagramCard(
    imageUrl: String?,
    modifier: Modifier = Modifier,
    maxHeight: Dp = 210.dp,
) {
    val resolved = remember(imageUrl) {
        imageUrl?.trim()?.takeIf { it.isNotBlank() }?.let { resolveServerMediaUrl(it) }
    }
    if (resolved.isNullOrBlank()) return

    val context = LocalContext.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 120.dp, max = maxHeight)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(12.dp))
            .padding(8.dp),
        contentAlignment = Alignment.Center,
    ) {
        SubcomposeAsyncImage(
            model = remember(resolved) {
                ImageRequest.Builder(context)
                    .data(resolved)
                    .crossfade(true)
                    .build()
            },
            contentDescription = "Diagram Soal",
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
            loading = {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
                }
            },
        )
    }
}

/**
 * Renders optional diagram stimulus card above cleaned question text.
 */
@Composable
fun QuestionPromptWithDiagram(
    rawText: String,
    explicitImageUrl: String? = null,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = 13.sp,
    lineHeight: TextUnit = 20.sp,
    fontWeight: FontWeight = FontWeight.SemiBold,
    modifier: Modifier = Modifier,
) {
    val (diagramUrl, cleanedText) = remember(rawText, explicitImageUrl) {
        extractQuestionDiagramAndText(rawText, explicitImageUrl)
    }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (!diagramUrl.isNullOrBlank()) {
            QuestionDiagramCard(imageUrl = diagramUrl)
        }
        if (cleanedText.isNotBlank()) {
            Text(
                text = cleanedText,
                color = color,
                fontSize = fontSize,
                lineHeight = lineHeight,
                fontWeight = fontWeight,
            )
        }
    }
}

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

/**
 * Recovers leaked `"glossary": [...]` JSON fragments if an older AI article stored one,
 * expands collapsed single-line pipe tables (`| Col | | :--- | | Val |`) onto separate lines,
 * and fixes asymmetrical LLM bold syntax (`*Term:**` -> `**Term:**`).
 */
fun normalizeMarkdownContent(rawInput: String): String {
    if (rawInput.isBlank()) return rawInput
    var text = rawInput.replace("\r\n", "\n")

    val siblingJsonLeakRe = Regex(
        pattern = """(?:(?:^|\n)\s*["'],\s*(?:\n\s*)+|\n\s*[•*\-]?\s*)"(?:glossary|soal_essay|soal_pilihan_ganda|soal_pg|questions|quiz|exercises|practice_questions|rubric|tasks)"\s*:\s*(?:\[|\{|")[\s\S]*$|(?:^|\n)\s*["'],\s*\n+\s*"[a-zA-Z_][a-zA-Z0-9_]*"\s*:\s*(?:\[|\{|")[\s\S]*$""",
        options = setOf(RegexOption.IGNORE_CASE)
    )
    val leakMatch = siblingJsonLeakRe.find(text)
    if (leakMatch != null) {
        val before = text.substring(0, leakMatch.range.first)
            .replace(Regex("""["'],?\s*$"""), "")
            .trimEnd()
        val matchedTail = leakMatch.value
        if (Regex(""""glossary"\s*:""", RegexOption.IGNORE_CASE).containsMatchIn(matchedTail)) {
            val pairRe = Regex(""""term"\s*:\s*"([^"]+)"[\s\S]*?"definition"\s*:\s*"([^"\r\n]+)(?:"|$)""")
            val extractedPairs = pairRe.findAll(matchedTail)
                .map { "- **${it.groupValues[1]}:** ${it.groupValues[2]}" }
                .toList()
            text = if (extractedPairs.isNotEmpty()) {
                "$before\n\n${extractedPairs.joinToString("\n")}"
            } else {
                before
            }
        } else {
            text = before
        }
    }

    text = text.replace(Regex("""",\s*$"""), "").trimEnd()

    val rawLines = text.split('\n')
    val expandedLines = ArrayList<String>(rawLines.size + 8)
    val sepPattern = Regex("""((?:\|\s*:?-{2,}:?\s*)+\|)""")

    for (line in rawLines) {
        val sepMatch = sepPattern.find(line)
        if (sepMatch != null) {
            val sepText = sepMatch.groupValues[1]
            val colCount = sepText.split('|').map { it.trim() }.count { it.isNotEmpty() }
            val beforeSep = line.substring(0, sepMatch.range.first)
            val pipeIndices = ArrayList<Int>()
            for (idx in beforeSep.indices) {
                if (beforeSep[idx] == '|') pipeIndices.add(idx)
            }
            if (colCount >= 2 && pipeIndices.size >= colCount + 1) {
                val tailAfterLast = beforeSep.substring(pipeIndices.last() + 1).trim()
                val headerPipeOffset = if (tailAfterLast.isEmpty()) 1 else 0
                val startPipePos = (pipeIndices.size - (colCount + headerPipeOffset)).coerceAtLeast(0)
                val headerStartIdx = pipeIndices[startPipePos]
                val prefix = beforeSep.substring(0, headerStartIdx).trim()
                val tablePart = line.substring(headerStartIdx).trim()

                if (prefix.isNotEmpty()) {
                    expandedLines.add(prefix)
                    expandedLines.add("")
                }
                if (Regex("""\|\s+\|""").containsMatchIn(tablePart)) {
                    tablePart.replace(Regex("""\|\s+\|"""), "|\n|")
                        .split('\n')
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                        .forEach { expandedLines.add(it) }
                    continue
                }
            }
        }
        expandedLines.add(line)
    }

    val asymBoldOpenSingleRe = Regex("""(^|\s)\*([^*\n]+)\*\*(?=\s|$|[.,:;!?])""")
    val asymBoldCloseSingleRe = Regex("""(^|\s)\*\*([^*\n]+)\*(?!\*)(?=\s|$|[.,:;!?])""")
    val unclosedBoldLineRe = Regex("""^(\s*[-*•]\s+|\s*\d+[.)]\s+)?\*\*[^*\n]+$""")
    val labelColonRe = Regex("""^(\s*(?:[-*•]\s+|\d+[.)]\s+)?\*\*[^*\n:]{1,65}:)(\s+.+)$""")

    return expandedLines.joinToString("\n") { line ->
        var l = asymBoldOpenSingleRe.replace(line, "$1**$2**")
        l = asymBoldCloseSingleRe.replace(l, "$1**$2**")

        val trimmed = l.trim()
        val boldPairsCount = Regex("""\*\*""").findAll(trimmed).count()
        if (boldPairsCount % 2 == 1 && unclosedBoldLineRe.matches(l)) {
            val labelColonMatch = labelColonRe.matchEntire(l)
            l = if (labelColonMatch != null && labelColonMatch.groupValues[2].trim().length > 25) {
                "${labelColonMatch.groupValues[1]}**${labelColonMatch.groupValues[2]}"
            } else {
                "${l.trimEnd()}**"
            }
        }
        l
    }
}

private sealed interface MdRenderBlock {
    data class TextChunk(val rawLines: List<String>) : MdRenderBlock
    data class Table(
        val headers: List<String>,
        val alignments: List<TextAlign>,
        val rows: List<List<String>>,
    ) : MdRenderBlock
}

private fun splitTableCells(line: String): List<String> {
    var trimmed = line.trim()
    if (trimmed.startsWith('|')) trimmed = trimmed.substring(1)
    if (trimmed.endsWith('|')) trimmed = trimmed.substring(0, trimmed.length - 1)
    return trimmed.split('|').map { it.trim() }
}

private fun parseTableSeparatorRow(line: String): List<TextAlign>? {
    val trimmed = line.trim()
    if (!trimmed.contains('|') || !trimmed.contains('-')) return null
    val cells = splitTableCells(trimmed)
    if (cells.isEmpty()) return null
    val aligns = ArrayList<TextAlign>(cells.size)
    val sepCellRe = Regex("""^:?-{2,}:?$""")
    for (c in cells) {
        if (!sepCellRe.matches(c)) return null
        aligns.add(
            when {
                c.startsWith(':') && c.endsWith(':') -> TextAlign.Center
                c.endsWith(':') -> TextAlign.End
                else -> TextAlign.Start
            }
        )
    }
    return aligns
}

private fun parseMarkdownBlocks(rawText: String): List<MdRenderBlock> {
    val lines = normalizeMarkdownContent(rawText).split('\n')
    val blocks = ArrayList<MdRenderBlock>()
    val textBuffer = ArrayList<String>()

    fun flushTextBuffer() {
        if (textBuffer.isNotEmpty()) {
            blocks.add(MdRenderBlock.TextChunk(textBuffer.toList()))
            textBuffer.clear()
        }
    }

    var i = 0
    var inCodeBlock = false
    while (i < lines.size) {
        val trimmed = lines[i].trim()
        if (trimmed.startsWith("```")) {
            inCodeBlock = !inCodeBlock
            textBuffer.add(lines[i])
            i++
            continue
        }
        if (!inCodeBlock && trimmed.contains('|') && i + 1 < lines.size) {
            val aligns = parseTableSeparatorRow(lines[i + 1])
            if (aligns != null && aligns.isNotEmpty()) {
                flushTextBuffer()
                val headers = splitTableCells(trimmed)
                val rows = ArrayList<List<String>>()
                i += 2
                while (i < lines.size) {
                    val rowTrim = lines[i].trim()
                    if (rowTrim.isEmpty() || !rowTrim.contains('|') || rowTrim.startsWith("#")) break
                    rows.add(splitTableCells(rowTrim))
                    i++
                }
                blocks.add(MdRenderBlock.Table(headers = headers, alignments = aligns, rows = rows))
                continue
            }
        }
        textBuffer.add(lines[i])
        i++
    }
    flushTextBuffer()
    return blocks
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

        // 5. Italic: *text* or _text_
        if ((line[i] == '*' || line[i] == '_') && i + 1 < n && !line[i + 1].isWhitespace()) {
            val delimChar = line[i]
            val prevIsWord = i > 0 && (line[i - 1].isLetterOrDigit() || line[i - 1] == '_')
            if (!(delimChar == '_' && prevIsWord)) {
                val close = line.indexOf(delimChar, i + 1)
                if (close > i + 1 && !line[close - 1].isWhitespace()) {
                    val inner = line.substring(i + 1, close)
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        appendMarkdownInline(inner, codeBg, codeColor, linkColor)
                    }
                    i = close + 1
                    continue
                }
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
    val lines = normalizeMarkdownContent(rawText).split('\n')
    return linesToAnnotatedString(lines, baseFontSize, headingColor, codeBg, codeColor, linkColor, mutedColor)
}

private fun linesToAnnotatedString(
    lines: List<String>,
    baseFontSize: TextUnit,
    headingColor: Color,
    codeBg: Color,
    codeColor: Color,
    linkColor: Color,
    mutedColor: Color,
): AnnotatedString {
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

            // Unordered list - item / * item / • item
            val bulletMatch = Regex("^(\\s*)[-*•]\\s+(.+)$").matchEntire(rawLine)
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

@Composable
private fun MarkdownTableBlockView(
    table: MdRenderBlock.Table,
    baseFontSize: TextUnit,
    lineHeight: TextUnit,
    textColor: Color,
    headingColor: Color,
    codeBg: Color,
    codeColor: Color,
    linkColor: Color,
) {
    val borderColor = headingColor.copy(alpha = 0.24f)
    val headerBg = codeBg.copy(alpha = (codeBg.alpha * 1.4f).coerceIn(0.14f, 0.35f))
    val stripeBg = headingColor.copy(alpha = 0.05f)

    val colWidths: List<Dp> = remember(table) {
        table.headers.indices.map { colIdx ->
            val maxChars = maxOf(
                table.headers.getOrNull(colIdx)?.length ?: 0,
                table.rows.maxOfOrNull { it.getOrNull(colIdx)?.length ?: 0 } ?: 0
            )
            (maxChars * 7.5f + 24f).coerceIn(92f, 220f).dp
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
            .horizontalScroll(rememberScrollState())
    ) {
        Column {
            // Header row
            Row(
                modifier = Modifier
                    .height(IntrinsicSize.Min)
                    .background(headerBg)
            ) {
                table.headers.forEachIndexed { colIdx, header ->
                    val cellText = remember(header, codeBg, codeColor, linkColor) {
                        buildAnnotatedString {
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                appendMarkdownInline(header, codeBg, codeColor, linkColor)
                            }
                        }
                    }
                    Text(
                        text = cellText,
                        color = headingColor,
                        fontSize = (baseFontSize.value * 0.92f).sp,
                        lineHeight = (lineHeight.value * 0.90f).sp,
                        textAlign = table.alignments.getOrNull(colIdx) ?: TextAlign.Start,
                        modifier = Modifier
                            .width(colWidths.getOrElse(colIdx) { 110.dp })
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    )
                    if (colIdx < table.headers.lastIndex) {
                        VerticalDivider(
                            color = borderColor,
                            modifier = Modifier.fillMaxHeight()
                        )
                    }
                }
            }

            HorizontalDivider(color = borderColor)

            // Body rows
            table.rows.forEachIndexed { rowIdx, row ->
                Row(
                    modifier = Modifier
                        .height(IntrinsicSize.Min)
                        .background(if (rowIdx % 2 == 1) stripeBg else Color.Transparent)
                ) {
                    table.headers.indices.forEach { colIdx ->
                        val rawCell = row.getOrNull(colIdx).orEmpty()
                        val cellAnnotated = remember(rawCell, codeBg, codeColor, linkColor) {
                            buildAnnotatedString {
                                appendMarkdownInline(rawCell, codeBg, codeColor, linkColor)
                            }
                        }
                        Text(
                            text = cellAnnotated,
                            color = textColor,
                            fontSize = (baseFontSize.value * 0.90f).sp,
                            lineHeight = (lineHeight.value * 0.90f).sp,
                            textAlign = table.alignments.getOrNull(colIdx) ?: TextAlign.Start,
                            modifier = Modifier
                                .width(colWidths.getOrElse(colIdx) { 110.dp })
                                .padding(horizontal = 10.dp, vertical = 7.dp)
                        )
                        if (colIdx < table.headers.lastIndex) {
                            VerticalDivider(
                                color = borderColor.copy(alpha = 0.6f),
                                modifier = Modifier.fillMaxHeight()
                            )
                        }
                    }
                }
                if (rowIdx < table.rows.lastIndex) {
                    HorizontalDivider(color = borderColor.copy(alpha = 0.6f))
                }
            }
        }
    }
}

/**
 * Pure-Compose Markdown text + GFM table component tailored for SchoolOS AI tutor, articles,
 * infographics, and teacher workstation responses.
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

    val blocks = remember(text) { parseMarkdownBlocks(text) }

    val content: @Composable () -> Unit = {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            blocks.forEach { block ->
                when (block) {
                    is MdRenderBlock.TextChunk -> {
                        val trimmedLines = block.rawLines
                            .dropWhile { it.isBlank() }
                            .dropLastWhile { it.isBlank() }
                        if (trimmedLines.isNotEmpty()) {
                            val parsed = remember(
                                trimmedLines,
                                fontSize,
                                resolvedHeading,
                                codeBackground,
                                codeColor,
                                linkColor,
                                resolvedMuted
                            ) {
                                linesToAnnotatedString(
                                    lines = trimmedLines,
                                    baseFontSize = fontSize,
                                    headingColor = resolvedHeading,
                                    codeBg = codeBackground,
                                    codeColor = codeColor,
                                    linkColor = linkColor,
                                    mutedColor = resolvedMuted,
                                )
                            }
                            Text(
                                text = parsed,
                                color = resolvedColor,
                                fontSize = fontSize,
                                lineHeight = lineHeight,
                                style = style,
                            )
                        }
                    }

                    is MdRenderBlock.Table -> {
                        MarkdownTableBlockView(
                            table = block,
                            baseFontSize = fontSize,
                            lineHeight = lineHeight,
                            textColor = resolvedColor,
                            headingColor = resolvedHeading,
                            codeBg = codeBackground,
                            codeColor = codeColor,
                            linkColor = linkColor,
                        )
                    }
                }
            }
        }
    }

    if (selectable) {
        SelectionContainer {
            content()
        }
    } else {
        content()
    }
}
