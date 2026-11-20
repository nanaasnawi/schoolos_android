package com.schoolos.android.feature.assignments

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlin.math.cos
import kotlin.math.sin
import com.schoolos.android.core.designsystem.AccentNeonCyan
import com.schoolos.android.core.designsystem.AccentPurple
import com.schoolos.android.core.designsystem.CosmicBlack
import com.schoolos.android.core.designsystem.CosmicDark
import com.schoolos.android.core.designsystem.CosmicNavy
import com.schoolos.android.core.designsystem.CosmicSurface2
import com.schoolos.android.core.designsystem.GlassBorder
import com.schoolos.android.core.designsystem.GlassBorder2
import com.schoolos.android.core.designsystem.LocalIsDarkTheme
import com.schoolos.android.core.designsystem.NeonBlue
import com.schoolos.android.core.designsystem.NeonError
import com.schoolos.android.core.designsystem.NeonSuccess
import com.schoolos.android.core.designsystem.NeonWarning
import com.schoolos.android.core.designsystem.QuestionPromptWithDiagram
import com.schoolos.android.core.designsystem.TextPrimary
import com.schoolos.android.core.designsystem.TextSecondary
import com.schoolos.android.core.designsystem.TextTertiary
import com.schoolos.android.domain.model.AssignmentQuestion
import com.schoolos.android.domain.model.SubmissionAnswer
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.ui.text.font.FontFamily
import org.json.JSONArray
import org.json.JSONObject

fun isInteractiveGameQuestionType(type: String?): Boolean {
    return when (type?.trim()?.uppercase()) {
        "MATCHING", "ORDERING", "FILL_BLANK", "CROSSWORD" -> true
        else -> false
    }
}

fun questionTypeDisplayLabel(type: String?): String {
    return when (type?.trim()?.uppercase()) {
        "MATCHING" -> "Game Menjodohkan"
        "ORDERING" -> "Game Urutkan Langkah"
        "FILL_BLANK" -> "Game Kalimat Rumpang"
        "CROSSWORD" -> "Game Teka-Teki Silang"
        "ESSAY", "URAIAN" -> "Uraian / Esai"
        else -> "Pilihan Ganda"
    }
}

fun questionTypeShortLabel(type: String?): String {
    return when (type?.trim()?.uppercase()) {
        "MATCHING" -> "Jodohkan"
        "ORDERING" -> "Urutan"
        "FILL_BLANK" -> "Rumpang"
        "CROSSWORD" -> "TTS"
        "ESSAY", "URAIAN" -> "Esai"
        else -> "PG"
    }
}

@Composable
fun questionTypeAccentColor(type: String?): Color {
    return when (type?.trim()?.uppercase()) {
        "MATCHING" -> NeonBlue
        "ORDERING" -> NeonWarning
        "FILL_BLANK" -> NeonSuccess
        "CROSSWORD" -> AccentPurple
        "ESSAY", "URAIAN" -> AccentNeonCyan
        else -> NeonSuccess
    }
}

fun createDefaultInteractionConfigJson(type: String): String {
    return when (type.trim().uppercase()) {
        "MATCHING" -> JSONObject().apply {
            put(
                "pairs",
                JSONArray().apply {
                    put(JSONObject().apply { put("id", "p1"); put("left", "Konsep 1"); put("right", "Penjelasan 1") })
                    put(JSONObject().apply { put("id", "p2"); put("left", "Konsep 2"); put("right", "Penjelasan 2") })
                    put(JSONObject().apply { put("id", "p3"); put("left", "Konsep 3"); put("right", "Penjelasan 3") })
                }
            )
        }.toString()
        "ORDERING" -> JSONObject().apply {
            put(
                "items",
                JSONArray().apply {
                    put(JSONObject().apply { put("id", "o1"); put("text", "Langkah pertama...") })
                    put(JSONObject().apply { put("id", "o2"); put("text", "Langkah kedua...") })
                    put(JSONObject().apply { put("id", "o3"); put("text", "Langkah ketiga...") })
                }
            )
        }.toString()
        "FILL_BLANK" -> JSONObject().apply {
            put("template", "Isi bagian rumpang {{1}} dan lanjutkan dengan {{2}}.")
            put("blanks", JSONArray().apply { put("jawaban1"); put("jawaban2") })
            put("distractors", JSONArray().apply { put("pengecoh1"); put("pengecoh2") })
        }.toString()
        "CROSSWORD" -> compileCrosswordConfigJson(
            listOf(
                "EVAPORASI" to "Proses penguapan air akibat panas matahari",
                "KONDENSASI" to "Perubahan uap air menjadi titik air di awan",
                "HUJAN" to "Titik air yang jatuh dari awan ke permukaan bumi",
                "AWAN" to "Kumpulan uap air dan kristal es di langit",
            )
        ).toString()
        else -> "{}"
    }
}

fun defaultPromptForGameType(type: String): String {
    return when (type.trim().uppercase()) {
        "MATCHING" -> "Jodohkan setiap konsep di kolom kiri dengan pasangan yang paling tepat!"
        "ORDERING" -> "Urutkan tahapan atau langkah berikut dari yang paling awal hingga akhir!"
        "FILL_BLANK" -> "Lengkapi bagian rumpang pada kalimat berikut menggunakan kata yang tepat!"
        "CROSSWORD" -> "Isilah Teka-Teki Silang (TTS) berikut sesuai petunjuk mendatar dan menurun!"
        else -> ""
    }
}

private data class CrosswordClueItem(
    val number: Int,
    val direction: String,
    val row: Int,
    val col: Int,
    val length: Int,
    val clue: String,
    val answer: String = "",
)

private fun normalizeCrosswordAnswer(raw: String): String {
    val sb = StringBuilder()
    for (ch in raw) {
        if (ch in 'a'..'z' || ch in 'A'..'Z') sb.append(ch.uppercaseChar())
    }
    return sb.toString()
}

private fun canPlaceCwWord(
    grid: Array<CharArray>,
    word: String,
    r0: Int,
    c0: Int,
    dir: String,
): Boolean {
    val rows = grid.size
    val cols = if (rows > 0) grid[0].size else 0
    return if (dir == "ACROSS") {
        if (r0 < 0 || r0 >= rows || c0 < 0 || c0 + word.length > cols) return false
        if (c0 > 0 && grid[r0][c0 - 1] != '#') return false
        if (c0 + word.length < cols && grid[r0][c0 + word.length] != '#') return false
        for (k in word.indices) {
            val cc = c0 + k
            val cell = grid[r0][cc]
            if (cell == word[k]) continue
            if (cell != '#') return false
            if (r0 > 0 && grid[r0 - 1][cc] != '#') return false
            if (r0 + 1 < rows && grid[r0 + 1][cc] != '#') return false
        }
        true
    } else {
        if (c0 < 0 || c0 >= cols || r0 < 0 || r0 + word.length > rows) return false
        if (r0 > 0 && grid[r0 - 1][c0] != '#') return false
        if (r0 + word.length < rows && grid[r0 + word.length][c0] != '#') return false
        for (k in word.indices) {
            val rr = r0 + k
            val cell = grid[rr][c0]
            if (cell == word[k]) continue
            if (cell != '#') return false
            if (c0 > 0 && grid[rr][c0 - 1] != '#') return false
            if (c0 + 1 < cols && grid[rr][c0 + 1] != '#') return false
        }
        true
    }
}

private fun compileCrosswordConfigJson(
    rawWords: List<Pair<String, String>>,
    preserveRawWordsArray: Boolean = false,
): JSONObject {
    val seen = HashSet<String>()
    val cleanWords = mutableListOf<Pair<String, String>>()
    for ((rawAns, rawClue) in rawWords) {
        val ans = normalizeCrosswordAnswer(rawAns)
        val clue = rawClue.trim()
        if (ans.length in 2..15 && clue.isNotEmpty() && seen.add(ans)) {
            cleanWords.add(ans to clue)
        }
    }

    val rawWordsArr = JSONArray().apply {
        val src = if (preserveRawWordsArray) rawWords else cleanWords
        src.forEach { (a, c) ->
            put(JSONObject().apply {
                put("answer", if (preserveRawWordsArray) a.uppercase() else a)
                put("clue", c)
            })
        }
    }

    if (cleanWords.isEmpty()) {
        return JSONObject().apply {
            put("words", rawWordsArr)
            put("rows", 0)
            put("cols", 0)
            put("grid", JSONArray())
            put("clues", JSONArray())
        }
    }

    val candidates = cleanWords.sortedWith(
        compareByDescending<Pair<String, String>> { it.first.length }.thenBy { it.first }
    )
    val maxLen = maxOf(candidates.maxOf { it.first.length }, 8)
    val size = (maxLen + 8).coerceIn(15, 21)
    var grid = MutableList(size) { CharArray(size) { '#' } }

    data class PlacedWord(val ans: String, val clue: String, val r: Int, val c: Int, val dir: String)
    val placed = mutableListOf<PlacedWord>()
    val used = BooleanArray(candidates.size)

    fun commitWord(w: Pair<String, String>, r0: Int, c0: Int, dir: String) {
        for (k in w.first.indices) {
            if (dir == "ACROSS") grid[r0][c0 + k] = w.first[k]
            else grid[r0 + k][c0] = w.first[k]
        }
        placed.add(PlacedWord(w.first, w.second, r0, c0, dir))
    }

    commitWord(candidates[0], size / 2, ((size - candidates[0].first.length) / 2).coerceAtLeast(0), "ACROSS")
    used[0] = true

    for (pass in candidates.indices) {
        var progress = false
        for (idx in 1 until candidates.size) {
            if (used[idx]) continue
            val word = candidates[idx]
            var bestScore = -1
            var bestR = 0
            var bestC = 0
            var bestDir = "ACROSS"

            val gridArr = grid.toTypedArray()
            for (r in 0 until size) {
                for (c in 0 until size) {
                    val existing = grid[r][c]
                    if (existing == '#') continue
                    for (k in word.first.indices) {
                        if (word.first[k] != existing) continue
                        val sc = c - k
                        if (sc >= 0 && canPlaceCwWord(gridArr, word.first, r, sc, "ACROSS")) {
                            var crosses = 0
                            for (i in word.first.indices) {
                                if (grid[r][sc + i] == word.first[i]) crosses++
                            }
                            if (crosses >= 1) {
                                val dist = kotlin.math.abs(r - size / 2) + kotlin.math.abs(sc + word.first.length / 2 - size / 2)
                                val score = crosses * 100 + (40 - dist).coerceAtLeast(0)
                                if (score > bestScore) {
                                    bestScore = score; bestR = r; bestC = sc; bestDir = "ACROSS"
                                }
                            }
                        }
                        val sr = r - k
                        if (sr >= 0 && canPlaceCwWord(gridArr, word.first, sr, c, "DOWN")) {
                            var crosses = 0
                            for (i in word.first.indices) {
                                if (grid[sr + i][c] == word.first[i]) crosses++
                            }
                            if (crosses >= 1) {
                                val dist = kotlin.math.abs(sr + word.first.length / 2 - size / 2) + kotlin.math.abs(c - size / 2)
                                val score = crosses * 100 + (40 - dist).coerceAtLeast(0)
                                if (score > bestScore) {
                                    bestScore = score; bestR = sr; bestC = c; bestDir = "DOWN"
                                }
                            }
                        }
                    }
                }
            }

            if (bestScore >= 0) {
                commitWord(word, bestR, bestC, bestDir)
                used[idx] = true
                progress = true
            }
        }
        if (!progress) break
    }

    for (idx in candidates.indices) {
        if (used[idx]) continue
        val word = candidates[idx]
        var placedFallback = false
        val gridArr = grid.toTypedArray()
        loop@ for (rr in grid.indices) {
            for (cc in grid[0].indices) {
                if (canPlaceCwWord(gridArr, word.first, rr, cc, "ACROSS")) {
                    commitWord(word, rr, cc, "ACROSS")
                    placedFallback = true
                    break@loop
                }
            }
        }
        if (!placedFallback) {
            val neededCols = maxOf(grid[0].size, word.first.length + 2)
            for (rIdx in grid.indices) {
                if (grid[rIdx].size < neededCols) {
                    val extended = CharArray(neededCols) { '#' }
                    grid[rIdx].copyInto(extended)
                    grid[rIdx] = extended
                }
            }
            grid.add(CharArray(neededCols) { '#' })
            val targetRow = grid.size
            grid.add(CharArray(neededCols) { '#' })
            commitWord(word, targetRow, ((neededCols - word.first.length) / 2).coerceAtLeast(0), "ACROSS")
        }
        used[idx] = true
    }

    var minR = grid.size
    var maxR = 0
    var minC = grid[0].size
    var maxC = 0
    for (r in grid.indices) {
        for (c in grid[r].indices) {
            if (grid[r][c] != '#') {
                if (r < minR) minR = r
                if (r > maxR) maxR = r
                if (c < minC) minC = c
                if (c > maxC) maxC = c
            }
        }
    }

    val trimmedRows = maxR - minR + 1
    val trimmedCols = maxC - minC + 1
    val gridJson = JSONArray()
    for (r in minR..maxR) {
        gridJson.put(String(grid[r], minC, trimmedCols))
    }

    val starts = placed
        .map { (it.r - minR) to (it.c - minC) }
        .distinct()
        .sortedWith(compareBy<Pair<Int, Int>> { it.first }.thenBy { it.second })
    val numMap = HashMap<Pair<Int, Int>, Int>()
    starts.forEachIndexed { idx, pos -> numMap[pos] = idx + 1 }

    val sortedPlaced = placed.sortedWith(
        compareBy<PlacedWord> { if (it.dir == "ACROSS") 0 else 1 }
            .thenBy { numMap[(it.r - minR) to (it.c - minC)] ?: 0 }
    )

    val cluesJson = JSONArray()
    sortedPlaced.forEach { p ->
        val rr = p.r - minR
        val cc = p.c - minC
        cluesJson.put(
            JSONObject().apply {
                put("number", numMap[rr to cc] ?: 1)
                put("direction", p.dir)
                put("row", rr)
                put("col", cc)
                put("length", p.ans.length)
                put("answer", p.ans)
                put("clue", p.clue)
            }
        )
    }

    return JSONObject().apply {
        put("words", rawWordsArr)
        put("rows", trimmedRows)
        put("cols", trimmedCols)
        put("grid", gridJson)
        put("clues", cluesJson)
    }
}

private fun parseCrosswordGrid(configJson: String?): List<String> {
    if (configJson.isNullOrBlank()) return emptyList()
    return runCatching {
        val arr = JSONObject(configJson).optJSONArray("grid") ?: return emptyList()
        buildList {
            for (i in 0 until arr.length()) add(arr.optString(i, ""))
        }
    }.getOrElse { emptyList() }
}

private fun parseCrosswordClues(configJson: String?): List<CrosswordClueItem> {
    if (configJson.isNullOrBlank()) return emptyList()
    return runCatching {
        val arr = JSONObject(configJson).optJSONArray("clues") ?: return emptyList()
        buildList {
            for (i in 0 until arr.length()) {
                val c = arr.optJSONObject(i) ?: continue
                add(
                    CrosswordClueItem(
                        number = c.optInt("number", i + 1),
                        direction = c.optString("direction", "ACROSS").uppercase(),
                        row = c.optInt("row", 0),
                        col = c.optInt("col", 0),
                        length = c.optInt("length", 0),
                        clue = c.optString("clue", ""),
                        answer = c.optString("answer", ""),
                    )
                )
            }
        }
    }.getOrElse { emptyList() }
}

private val MATCH_PAIR_COLORS = listOf(
    Color(0xFF0284C7),
    Color(0xFF10B981),
    Color(0xFFD97706),
    Color(0xFF8B5CF6),
    Color(0xFFF43F5E),
)

data class GameItemPair(val id: String, val text: String)
data class TeacherMatchPair(val id: String, val left: String, val right: String)

private data class ParsedFillBlank(
    val template: String,
    val blankCount: Int,
    val wordBank: List<String>,
)

private fun parseMatchingStudentItems(configJson: String?): Pair<List<GameItemPair>, List<String>> {
    if (configJson.isNullOrBlank()) return emptyList<GameItemPair>() to emptyList()
    return runCatching {
        val obj = JSONObject(configJson)
        val leftList = mutableListOf<GameItemPair>()
        val rightOptions = mutableListOf<String>()

        val leftArr = obj.optJSONArray("left_items")
        val rightArr = obj.optJSONArray("right_options")
        if (leftArr != null && leftArr.length() > 0) {
            for (i in 0 until leftArr.length()) {
                val item = leftArr.optJSONObject(i) ?: continue
                leftList.add(GameItemPair(item.optString("id", "p${i + 1}"), item.optString("text", "")))
            }
            if (rightArr != null) {
                for (i in 0 until rightArr.length()) {
                    val opt = rightArr.optString(i, "")
                    if (opt.isNotBlank()) rightOptions.add(opt)
                }
            }
        } else {
            val pairsArr = obj.optJSONArray("pairs")
            if (pairsArr != null) {
                for (i in 0 until pairsArr.length()) {
                    val item = pairsArr.optJSONObject(i) ?: continue
                    val id = item.optString("id", "p${i + 1}")
                    val left = item.optString("left", "")
                    val right = item.optString("right", "")
                    leftList.add(GameItemPair(id, left))
                    if (right.isNotBlank()) rightOptions.add(right)
                }
            }
        }
        leftList to rightOptions
    }.getOrElse { emptyList<GameItemPair>() to emptyList() }
}

private fun parseOrderingItems(configJson: String?): List<GameItemPair> {
    if (configJson.isNullOrBlank()) return emptyList()
    return runCatching {
        val obj = JSONObject(configJson)
        val itemsArr = obj.optJSONArray("items") ?: return@runCatching emptyList()
        buildList {
            for (i in 0 until itemsArr.length()) {
                val item = itemsArr.optJSONObject(i) ?: continue
                add(GameItemPair(item.optString("id", "o${i + 1}"), item.optString("text", "")))
            }
        }
    }.getOrElse { emptyList() }
}

private fun normalizeFillBlankTemplate(template: String): String {
    if (template.isBlank()) return template
    var maxIdx = 0
    Regex("\\{\\{(\\d+)\\}\\}").findAll(template).forEach { m ->
        val n = m.groupValues[1].toIntOrNull() ?: 0
        if (n > maxIdx) maxIdx = n
    }
    var next = Regex("(?<!\\{)\\{(\\d+)\\}(?!\\})").replace(template) { m ->
        val n = m.groupValues[1].toIntOrNull() ?: 0
        if (n > maxIdx) maxIdx = n
        "{{${m.groupValues[1]}}}"
    }
    next = Regex("\\[\\s*(?:\\d+|_+|[Bb]lank)\\s*]|\\{\\s*(?:_+|[Bb]lank)\\s*\\}|_{2,}").replace(next) {
        maxIdx += 1
        "{{$maxIdx}}"
    }
    return next
}

private fun countTemplateBlanks(template: String): Int {
    val norm = normalizeFillBlankTemplate(template)
    val numbered = Regex("\\{\\{(\\d+)\\}\\}").findAll(norm).count()
    return maxOf(numbered, 1)
}

/**
 * Strips duplicated cloze `{{1}}` / `___` template sentences from `questionText` when
 * rendering `FILL_BLANK` cards so the student sees a clean single interactive reading card.
 */
private fun extractFillBlankStudentHeadline(rawQuestionText: String, configJson: String?): String {
    val defaultInstr = "Lengkapi kotak kosong pada kalimat berikut menggunakan kata dari Bank Kata:"
    val trimmed = rawQuestionText.trim()
    if (trimmed.isBlank()) return defaultInstr

    val imageRegex = Regex("!\\[[^\\]\\n]*\\]\\([^)\\s]+\\)")
    val images = imageRegex.findAll(trimmed).map { it.value }.toList()
    val textOnly = imageRegex.replace(trimmed, "").trim()
    if (textOnly.isBlank()) {
        return if (images.isNotEmpty()) "${images.joinToString("\n\n")}\n\n$defaultInstr" else defaultInstr
    }

    val rawTpl = runCatching { JSONObject(configJson ?: "{}").optString("template", "") }.getOrDefault("")
    val tplNorm = normalizeFillBlankTemplate(rawTpl)
        .lowercase()
        .replace(Regex("\\{\\{\\d+\\}\\}"), "")
        .replace(Regex("\\s+"), " ")
        .trim()
    val blankRegex = Regex("\\{\\{\\d+\\}\\}|(?<!\\{)\\{\\d+\\}(?!\\})|\\[\\s*(?:\\d+|_+|[Bb]lank)\\s*]|\\{\\s*(?:_+|[Bb]lank)\\s*\\}|_{2,}")

    val keptLines = textOnly.lines()
        .map { it.trim() }
        .filter { line ->
            if (line.isBlank()) return@filter false
            if (blankRegex.containsMatchIn(line)) return@filter false
            if (tplNorm.length > 18) {
                val lineClean = line.lowercase().replace(Regex("\\s+"), " ")
                if (tplNorm.contains(lineClean) || lineClean.contains(tplNorm.take(24))) return@filter false
            }
            true
        }

    val headline = if (keptLines.isNotEmpty()) keptLines.joinToString("\n") else defaultInstr
    return if (images.isNotEmpty()) "${images.joinToString("\n\n")}\n\n$headline" else headline
}

private fun <T> shuffleDeterministic(items: List<T>, seed: String): List<T> {
    if (items.size <= 1) return items
    return items.shuffled(java.util.Random(seed.hashCode().toLong()))
}

private fun parseFillBlankStudentConfig(configJson: String?): ParsedFillBlank {
    if (configJson.isNullOrBlank()) return ParsedFillBlank("", 1, emptyList())
    return runCatching {
        val obj = JSONObject(configJson)
        val template = obj.optString("template", "")
        val wordBank = mutableListOf<String>()
        val wbArr = obj.optJSONArray("word_bank")
        val blanksArr = obj.optJSONArray("blanks")
        val distractorsArr = obj.optJSONArray("distractors")
        val blankCount = obj.optInt("blank_count", 0).let { bc ->
            if (bc > 0) bc
            else if (blanksArr != null && blanksArr.length() > 0) blanksArr.length()
            else countTemplateBlanks(template)
        }
        if (wbArr != null) {
            for (i in 0 until wbArr.length()) {
                val w = wbArr.optString(i, "")
                if (w.isNotBlank()) wordBank.add(w)
            }
        } else {
            if (blanksArr != null) {
                for (i in 0 until blanksArr.length()) {
                    val w = blanksArr.optString(i, "")
                    if (w.isNotBlank()) wordBank.add(w)
                }
            }
            if (distractorsArr != null) {
                for (i in 0 until distractorsArr.length()) {
                    val w = distractorsArr.optString(i, "")
                    if (w.isNotBlank()) wordBank.add(w)
                }
            }
        }
        ParsedFillBlank(template, blankCount, wordBank)
    }.getOrElse { ParsedFillBlank("", 1, emptyList()) }
}

fun isInteractiveAnswerComplete(
    questionType: String,
    answerJson: String?,
    interactionConfigJson: String?,
): Boolean {
    if (answerJson.isNullOrBlank()) return false
    return runCatching {
        val ansObj = JSONObject(answerJson)
        when (questionType.trim().uppercase()) {
            "MATCHING" -> {
                val matches = ansObj.optJSONObject("matches") ?: return@runCatching false
                val (leftItems, _) = parseMatchingStudentItems(interactionConfigJson)
                if (leftItems.isEmpty()) matches.length() > 0
                else leftItems.all { matches.optString(it.id, "").isNotBlank() }
            }
            "ORDERING" -> {
                val order = ansObj.optJSONArray("order")
                    ?: ansObj.optJSONArray("ordered_ids")
                    ?: return@runCatching false
                val items = parseOrderingItems(interactionConfigJson)
                if (items.isEmpty()) order.length() > 0
                else order.length() >= items.size
            }
            "FILL_BLANK" -> {
                val fills = ansObj.optJSONArray("fills")
                    ?: ansObj.optJSONArray("answers")
                    ?: return@runCatching false
                val cfg = parseFillBlankStudentConfig(interactionConfigJson)
                if (fills.length() < cfg.blankCount) return@runCatching false
                (0 until cfg.blankCount).all { i -> fills.optString(i, "").isNotBlank() }
            }
            "CROSSWORD" -> {
                val targetGrid = parseCrosswordGrid(interactionConfigJson)
                val boardArr = ansObj.optJSONArray("board") ?: ansObj.optJSONArray("grid")
                if (targetGrid.isNotEmpty() && boardArr != null && boardArr.length() >= targetGrid.size) {
                    var totalCells = 0
                    for (r in targetGrid.indices) {
                        val tRow = targetGrid[r]
                        val sRow = boardArr.optString(r, "")
                        for (c in tRow.indices) {
                            if (tRow[c] == '#') continue
                            totalCells++
                            val ch = sRow.getOrNull(c) ?: '.'
                            if (ch !in 'A'..'Z' && ch !in 'a'..'z') return@runCatching false
                        }
                    }
                    totalCells > 0
                } else {
                    val clues = parseCrosswordClues(interactionConfigJson)
                    val wordsMap = ansObj.optJSONObject("words") ?: return@runCatching false
                    if (clues.isEmpty()) wordsMap.length() > 0
                    else clues.all { cl ->
                        wordsMap.optString("${cl.number}_${cl.direction}", "").trim().length >= cl.length
                    }
                }
            }
            else -> false
        }
    }.getOrElse { false }
}

fun formatInteractiveAnswerSummary(
    questionType: String,
    answerJson: String?,
    interactionConfigJson: String?,
): List<String> {
    if (answerJson.isNullOrBlank()) return listOf("(Belum ada jawaban yang dikirim)")
    return runCatching {
        val ansObj = JSONObject(answerJson)
        when (questionType.trim().uppercase()) {
            "MATCHING" -> {
                val matches = ansObj.optJSONObject("matches") ?: return@runCatching listOf(answerJson)
                val (leftItems, _) = parseMatchingStudentItems(interactionConfigJson)
                if (leftItems.isNotEmpty()) {
                    leftItems.mapIndexed { idx, item ->
                        val chosen = matches.optString(item.id, "-")
                        "${idx + 1}. ${item.text} -> ${chosen.ifBlank { "(belum dipasangkan)" }}"
                    }
                } else {
                    val keys = matches.keys()
                    buildList {
                        var idx = 1
                        while (keys.hasNext()) {
                            val k = keys.next()
                            add("$idx. $k -> ${matches.optString(k, "-")}")
                            idx++
                        }
                    }.ifEmpty { listOf("-") }
                }
            }
            "ORDERING" -> {
                val order = ansObj.optJSONArray("order")
                    ?: ansObj.optJSONArray("ordered_ids")
                    ?: return@runCatching listOf(answerJson)
                val itemsMap = parseOrderingItems(interactionConfigJson).associateBy { it.id }
                buildList {
                    for (i in 0 until order.length()) {
                        val itemId = order.optString(i, "")
                        val label = itemsMap[itemId]?.text ?: itemId
                        add("${i + 1}. $label")
                    }
                }.ifEmpty { listOf("-") }
            }
            "FILL_BLANK" -> {
                val fills = ansObj.optJSONArray("fills")
                    ?: ansObj.optJSONArray("answers")
                    ?: return@runCatching listOf(answerJson)
                val cfg = parseFillBlankStudentConfig(interactionConfigJson)
                val fillWords = buildList {
                    for (i in 0 until fills.length()) {
                        add(fills.optString(i, ""))
                    }
                }
                val lines = mutableListOf<String>()
                if (cfg.template.isNotBlank()) {
                    var idx = 0
                    val rendered = Regex("\\{\\{(\\d+)\\}\\}").replace(normalizeFillBlankTemplate(cfg.template)) { m ->
                        val slotIdx = (m.groupValues[1].toIntOrNull() ?: (idx + 1)) - 1
                        val w = fillWords.getOrNull(slotIdx)?.takeIf { it.isNotBlank() } ?: "___"
                        idx++
                        "[$w]"
                    }
                    lines.add(rendered)
                }
                fillWords.forEachIndexed { idx, w ->
                    lines.add("Isian #${idx + 1}: ${w.ifBlank { "-" }}")
                }
                lines.ifEmpty { listOf("-") }
            }
            "CROSSWORD" -> {
                val clues = parseCrosswordClues(interactionConfigJson)
                val boardArr = ansObj.optJSONArray("board") ?: ansObj.optJSONArray("grid")
                val wordsMap = ansObj.optJSONObject("words")
                if (clues.isNotEmpty()) {
                    clues.map { cl ->
                        val sb = StringBuilder()
                        if (boardArr != null && boardArr.length() > 0) {
                            for (k in 0 until cl.length) {
                                val rr = if (cl.direction == "DOWN") cl.row + k else cl.row
                                val cc = if (cl.direction == "DOWN") cl.col else cl.col + k
                                val ch = boardArr.optString(rr, "").getOrNull(cc) ?: '.'
                                sb.append(if (ch in 'A'..'Z' || ch in 'a'..'z') ch.uppercaseChar() else '_')
                            }
                        } else if (wordsMap != null) {
                            sb.append(wordsMap.optString("${cl.number}_${cl.direction}", "-"))
                        }
                        val ansText = sb.toString().ifBlank { "-" }
                        val dirLabel = if (cl.direction == "DOWN") "Menurun" else "Mendatar"
                        val suffix = if (cl.answer.isNotBlank()) {
                            if (ansText == normalizeCrosswordAnswer(cl.answer)) " [Benar]" else " [Kunci: ${cl.answer}]"
                        } else ""
                        "${cl.number}. $dirLabel: $ansText$suffix"
                    }
                } else {
                    listOf(answerJson)
                }
            }
            else -> listOf(answerJson)
        }
    }.getOrElse { listOf(answerJson) }
}

fun formatTeacherConfigSummary(
    questionType: String,
    interactionConfigJson: String?,
): List<String> {
    if (interactionConfigJson.isNullOrBlank()) return emptyList()
    return runCatching {
        val obj = JSONObject(interactionConfigJson)
        when (questionType.trim().uppercase()) {
            "MATCHING" -> {
                val pairs = obj.optJSONArray("pairs")
                if (pairs != null && pairs.length() > 0) {
                    buildList {
                        for (i in 0 until pairs.length()) {
                            val p = pairs.optJSONObject(i) ?: continue
                            add("${i + 1}. ${p.optString("left", "")} <-> ${p.optString("right", "")}")
                        }
                    }
                } else {
                    val (leftItems, _) = parseMatchingStudentItems(interactionConfigJson)
                    leftItems.mapIndexed { idx, it -> "${idx + 1}. ${it.text}" }
                }
            }
            "ORDERING" -> {
                parseOrderingItems(interactionConfigJson).mapIndexed { idx, it -> "${idx + 1}. ${it.text}" }
            }
            "FILL_BLANK" -> {
                val template = obj.optString("template", "")
                val blanks = obj.optJSONArray("blanks")
                val bank = obj.optJSONArray("word_bank")
                val keys = buildList {
                    val src = blanks ?: bank
                    if (src != null) {
                        for (i in 0 until src.length()) add(src.optString(i, ""))
                    }
                }
                buildList {
                    if (template.isNotBlank()) add("Kalimat: $template")
                    if (keys.isNotEmpty()) {
                        add((if (blanks != null) "Kunci: " else "Bank Kata: ") + keys.joinToString(", "))
                    }
                }
            }
            "CROSSWORD" -> {
                val clues = parseCrosswordClues(interactionConfigJson)
                if (clues.isNotEmpty()) {
                    clues.map { cl ->
                        val dir = if (cl.direction == "DOWN") "Menurun" else "Mendatar"
                        val ansPart = if (cl.answer.isNotBlank()) "${cl.answer} — " else ""
                        "${cl.number} $dir: $ansPart${cl.clue}"
                    }
                } else {
                    val wordsArr = obj.optJSONArray("words")
                    buildList {
                        if (wordsArr != null) {
                            for (i in 0 until wordsArr.length()) {
                                val w = wordsArr.optJSONObject(i) ?: continue
                                add("${i + 1}. ${w.optString("answer", "")} — ${w.optString("clue", "")}")
                            }
                        }
                    }
                }
            }
            else -> emptyList()
        }
    }.getOrElse { emptyList() }
}

// ── STUDENT PLAYABLE GAME QUESTION CARD ───────────────────────────────────────

@Composable
fun InteractiveGameStudentCard(
    index: Int,
    question: AssignmentQuestion,
    answerJson: String?,
    onAnswerJsonChanged: (String) -> Unit,
    isParent: Boolean,
) {
    val normType = question.questionType.trim().uppercase()
    val accent = questionTypeAccentColor(normType)
    val typeLabel = questionTypeDisplayLabel(normType)
    val isCompleted = isInteractiveAnswerComplete(normType, answerJson, question.interactionConfigJson)
    val haptic = LocalHapticFeedback.current
    var wasCompleted by remember(question.id) { mutableStateOf(isCompleted) }
    val confettiProgress = remember(question.id) { Animatable(0f) }

    LaunchedEffect(isCompleted) {
        if (isCompleted && !wasCompleted) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            confettiProgress.snapTo(0.01f)
            confettiProgress.animateTo(1f, animationSpec = tween(durationMillis = 900))
            confettiProgress.snapTo(0f)
        }
        wasCompleted = isCompleted
    }

    val animatedBorderColor by animateColorAsState(
        targetValue = if (isCompleted) NeonSuccess.copy(alpha = 0.7f) else GlassBorder,
        label = "gameCardBorder",
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(CosmicNavy)
            .border(
                width = if (isCompleted) 1.5.dp else 1.dp,
                color = animatedBorderColor,
                shape = RoundedCornerShape(18.dp)
            )
            .padding(14.dp)
    ) {
        if (confettiProgress.value > 0f && confettiProgress.value < 1f) {
            val burst = confettiProgress.value
            val particleColors = listOf(
                Color(0xFF10B981),
                Color(0xFF38BDF8),
                Color(0xFFF59E0B),
                Color(0xFFA78BFA),
                Color(0xFFF43F5E),
            )
            Canvas(
                modifier = Modifier
                    .matchParentSize()
                    .zIndex(5f)
            ) {
                val origin = Offset(size.width * 0.85f, 24.dp.toPx())
                val count = 14
                for (i in 0 until count) {
                    val angle = (i * (360f / count)) * (Math.PI / 180.0)
                    val radius = (18f + 135f * burst)
                    val px = origin.x + (cos(angle) * radius).toFloat()
                    val py = origin.y + (sin(angle) * radius * 0.72f).toFloat() + (burst * burst * 38f)
                    drawCircle(
                        color = particleColors[i % particleColors.size].copy(alpha = (1f - burst).coerceIn(0f, 1f)),
                        radius = (5.dp.toPx() * (1f - burst * 0.45f)),
                        center = Offset(px, py)
                    )
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(accent.copy(alpha = 0.16f))
                            .border(1.dp, accent.copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "$index",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = accent
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(accent.copy(alpha = 0.15f))
                                    .padding(horizontal = 7.dp, vertical = 2.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        Icons.Default.SportsEsports,
                                        contentDescription = null,
                                        tint = accent,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        typeLabel,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = accent
                                    )
                                }
                            }
                            if (question.points != null) {
                                Text(
                                    "${question.points} poin",
                                    fontSize = 10.sp,
                                    color = TextTertiary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        val studentPrompt = remember(normType, question.questionText, question.interactionConfigJson) {
                            if (normType == "FILL_BLANK") {
                                extractFillBlankStudentHeadline(question.questionText, question.interactionConfigJson)
                            } else {
                                question.questionText
                            }
                        }
                        QuestionPromptWithDiagram(
                            rawText = studentPrompt,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary,
                            lineHeight = 20.sp
                        )
                    }
                }

                if (isCompleted) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(NeonSuccess.copy(alpha = 0.16f))
                            .border(1.dp, NeonSuccess.copy(alpha = 0.4f), RoundedCornerShape(999.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Celebration,
                            contentDescription = "Lengkap",
                            tint = NeonSuccess,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "Siap",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = NeonSuccess
                        )
                    }
                }
            }

            HorizontalDivider(color = GlassBorder, thickness = 0.5.dp)

            when (normType) {
                "MATCHING" -> MatchingGamePlayer(
                    questionId = question.id ?: "q-$index",
                    configJson = question.interactionConfigJson,
                    answerJson = answerJson,
                    onAnswerJsonChanged = onAnswerJsonChanged,
                    readOnly = isParent,
                )
                "ORDERING" -> OrderingGamePlayer(
                    questionId = question.id ?: "q-$index",
                    configJson = question.interactionConfigJson,
                    answerJson = answerJson,
                    onAnswerJsonChanged = onAnswerJsonChanged,
                    readOnly = isParent,
                )
                "FILL_BLANK" -> FillBlankGamePlayer(
                    questionId = question.id ?: "q-$index",
                    configJson = question.interactionConfigJson,
                    answerJson = answerJson,
                    onAnswerJsonChanged = onAnswerJsonChanged,
                    readOnly = isParent,
                )
                "CROSSWORD" -> CrosswordGamePlayer(
                    questionId = question.id ?: "q-$index",
                    configJson = question.interactionConfigJson,
                    answerJson = answerJson,
                    onAnswerJsonChanged = onAnswerJsonChanged,
                    readOnly = isParent,
                )
            }
        }
    }
}

@Composable
private fun MatchingGamePlayer(
    questionId: String,
    configJson: String?,
    answerJson: String?,
    onAnswerJsonChanged: (String) -> Unit,
    readOnly: Boolean,
) {
    val (leftItems, rightOptions) = remember(configJson) { parseMatchingStudentItems(configJson) }
    val matchesMap = remember(questionId, configJson) { mutableStateMapOf<String, String>() }
    var activeLeftId by remember(questionId) { mutableStateOf<String?>(null) }
    val haptic = LocalHapticFeedback.current

    var boardCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val leftPinCenters = remember(questionId) { mutableStateMapOf<String, Offset>() }
    val rightPinCenters = remember(questionId) { mutableStateMapOf<Int, Offset>() }
    val rightCardRects = remember(questionId) { mutableStateMapOf<Int, Rect>() }

    var dragSourceLeftId by remember { mutableStateOf<String?>(null) }
    var dragSourceIdx by remember { mutableIntStateOf(-1) }
    var dragPointerOffset by remember { mutableStateOf<Offset?>(null) }
    var hoveredRightIdx by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(answerJson) {
        if (!answerJson.isNullOrBlank()) {
            runCatching {
                val m = JSONObject(answerJson).optJSONObject("matches")
                if (m != null) {
                    matchesMap.clear()
                    val keys = m.keys()
                    while (keys.hasNext()) {
                        val k = keys.next()
                        val v = m.optString(k, "")
                        if (v.isNotBlank()) matchesMap[k] = v
                    }
                }
            }
        }
    }

    fun emitMatches() {
        val root = JSONObject().apply {
            val m = JSONObject()
            matchesMap.forEach { (k, v) -> m.put(k, v) }
            put("matches", m)
        }
        onAnswerJsonChanged(root.toString())
    }

    fun assignPair(targetLeftId: String, rightOption: String) {
        val prevOwner = matchesMap.entries.firstOrNull { it.value == rightOption }?.key
        if (prevOwner != null && prevOwner != targetLeftId) {
            matchesMap.remove(prevOwner)
        }
        matchesMap[targetLeftId] = rightOption
        activeLeftId = leftItems.firstOrNull { it.id != targetLeftId && matchesMap[it.id].isNullOrBlank() }?.id
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        emitMatches()
    }

    val matchedCount = matchesMap.size
    val activeIdx = leftItems.indexOfFirst { it.id == activeLeftId }
    val isAllMatched = leftItems.isNotEmpty() && matchedCount >= leftItems.size

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (isAllMatched) NeonSuccess.copy(alpha = 0.13f) else Color(0xFF0284C7).copy(alpha = 0.12f)
                )
                .border(
                    1.dp,
                    if (isAllMatched) NeonSuccess.copy(alpha = 0.4f) else Color(0xFF0284C7).copy(alpha = 0.3f),
                    RoundedCornerShape(12.dp)
                )
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when {
                        dragSourceLeftId != null -> "Tarik kabel ke kartu pasangan di sebelah kanan!"
                        activeIdx >= 0 -> "Langkah 2: Ketuk pasangan di kanan untuk Soal #${activeIdx + 1}"
                        isAllMatched -> "Semua kabel tersambung! Ketuk kartu jika ingin mengubah."
                        else -> "Seret dari pin bulat ● atau ketuk kartu kiri → kanan"
                    },
                    fontSize = 11.sp,
                    color = if (isAllMatched) NeonSuccess else Color(0xFF38BDF8),
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 15.sp,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "$matchedCount/${leftItems.size}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isAllMatched) NeonSuccess else Color(0xFF38BDF8)
                    )
                    if (!readOnly && matchesMap.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable {
                                    matchesMap.clear()
                                    activeLeftId = leftItems.firstOrNull()?.id
                                    emitMatches()
                                }
                                .padding(top = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(Icons.Default.Refresh, null, tint = TextTertiary, modifier = Modifier.size(11.dp))
                            Text("Reset", fontSize = 10.sp, color = TextTertiary, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        // 2-Column Tactile Wire Board with Bezier Wires Overlay
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { boardCoords = it }
        ) {
            // Draw connected & active curved Bezier wires cleanly across the center gap
            Canvas(
                modifier = Modifier
                    .matchParentSize()
                    .zIndex(2f)
            ) {
                leftItems.forEachIndexed { idx, item ->
                    val chosenRight = matchesMap[item.id] ?: return@forEachIndexed
                    val rIdx = rightOptions.indexOf(chosenRight)
                    if (rIdx < 0) return@forEachIndexed
                    val p1 = leftPinCenters[item.id] ?: return@forEachIndexed
                    val p2 = rightPinCenters[rIdx] ?: return@forEachIndexed
                    val wireColor = MATCH_PAIR_COLORS[idx % MATCH_PAIR_COLORS.size]
                    val midX = (p1.x + p2.x) / 2f
                    val wirePath = Path().apply {
                        moveTo(p1.x, p1.y)
                        cubicTo(midX, p1.y, midX, p2.y, p2.x, p2.y)
                    }
                    drawPath(
                        path = wirePath,
                        color = wireColor.copy(alpha = 0.24f),
                        style = Stroke(width = 7.dp.toPx(), cap = StrokeCap.Round)
                    )
                    drawPath(
                        path = wirePath,
                        color = wireColor,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )
                    drawCircle(wireColor, radius = 4.5.dp.toPx(), center = p1)
                    drawCircle(wireColor, radius = 4.5.dp.toPx(), center = p2)
                }

                val activeDragId = dragSourceLeftId
                val livePointer = dragPointerOffset
                if (activeDragId != null && livePointer != null) {
                    val p1 = leftPinCenters[activeDragId]
                    if (p1 != null) {
                        val dragColor = MATCH_PAIR_COLORS[(dragSourceIdx.coerceAtLeast(0)) % MATCH_PAIR_COLORS.size]
                        val midX = (p1.x + livePointer.x) / 2f
                        val livePath = Path().apply {
                            moveTo(p1.x, p1.y)
                            cubicTo(midX, p1.y, midX, livePointer.y, livePointer.x, livePointer.y)
                        }
                        drawPath(
                            path = livePath,
                            color = dragColor,
                            style = Stroke(
                                width = 3.5.dp.toPx(),
                                cap = StrokeCap.Round,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f))
                            )
                        )
                        drawCircle(dragColor, radius = 5.5.dp.toPx(), center = p1)
                        drawCircle(dragColor, radius = 6.dp.toPx(), center = livePointer)
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(26.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Left Column: Prompts with right-edge draggable pins
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "KOLOM A",
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextTertiary,
                        letterSpacing = 0.6.sp
                    )
                    leftItems.forEachIndexed { idx, item ->
                        val pairColor = MATCH_PAIR_COLORS[idx % MATCH_PAIR_COLORS.size]
                        val isSelected = activeLeftId == item.id || dragSourceLeftId == item.id
                        val hasPair = !matchesMap[item.id].isNullOrBlank()
                        var pinLocalCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    when {
                                        isSelected -> pairColor.copy(alpha = 0.18f)
                                        hasPair -> pairColor.copy(alpha = 0.12f)
                                        else -> CosmicBlack
                                    }
                                )
                                .border(
                                    width = if (isSelected || hasPair) 1.5.dp else 1.dp,
                                    color = when {
                                        isSelected -> pairColor
                                        hasPair -> pairColor.copy(alpha = 0.7f)
                                        else -> GlassBorder
                                    },
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .then(
                                    if (!readOnly) Modifier.clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        if (hasPair && activeLeftId != item.id) {
                                            matchesMap.remove(item.id)
                                            activeLeftId = item.id
                                            emitMatches()
                                        } else {
                                            activeLeftId = if (isSelected) null else item.id
                                        }
                                    } else Modifier
                                )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 8.dp, end = 4.dp, top = 9.dp, bottom = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(19.dp)
                                        .clip(CircleShape)
                                        .background(pairColor),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "${idx + 1}",
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White
                                    )
                                }
                                Text(
                                    text = item.text,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary,
                                    lineHeight = 15.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                // Draggable Wire Pin Handle (does not hijack card vertical scroll!)
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .onGloballyPositioned { coords ->
                                            pinLocalCoords = coords
                                            val parent = boardCoords
                                            if (parent != null && coords.isAttached && parent.isAttached) {
                                                leftPinCenters[item.id] = parent.localPositionOf(
                                                    coords,
                                                    Offset(coords.size.width / 2f, coords.size.height / 2f)
                                                )
                                            }
                                        }
                                        .then(
                                            if (!readOnly) Modifier.pointerInput(item.id, rightOptions) {
                                                detectDragGestures(
                                                    onDragStart = { startOffset ->
                                                        val parent = boardCoords
                                                        val local = pinLocalCoords
                                                        if (parent != null && local != null && parent.isAttached && local.isAttached) {
                                                            dragSourceLeftId = item.id
                                                            dragSourceIdx = idx
                                                            dragPointerOffset = parent.localPositionOf(local, startOffset)
                                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                        }
                                                    },
                                                    onDrag = { change, dragAmount ->
                                                        change.consume()
                                                        val nextPt = (dragPointerOffset ?: Offset.Zero) + dragAmount
                                                        dragPointerOffset = nextPt
                                                        val hit = rightCardRects.entries.firstOrNull { (_, r) ->
                                                            nextPt.x >= r.left - 18f && nextPt.x <= r.right + 18f &&
                                                                nextPt.y >= r.top - 12f && nextPt.y <= r.bottom + 12f
                                                        }?.key
                                                        if (hit != hoveredRightIdx) {
                                                            hoveredRightIdx = hit
                                                            if (hit != null) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                        }
                                                    },
                                                    onDragEnd = {
                                                        val dropIdx = hoveredRightIdx
                                                        if (dropIdx != null && dropIdx in rightOptions.indices) {
                                                            assignPair(item.id, rightOptions[dropIdx])
                                                        }
                                                        dragSourceLeftId = null
                                                        dragSourceIdx = -1
                                                        dragPointerOffset = null
                                                        hoveredRightIdx = null
                                                    },
                                                    onDragCancel = {
                                                        dragSourceLeftId = null
                                                        dragSourceIdx = -1
                                                        dragPointerOffset = null
                                                        hoveredRightIdx = null
                                                    }
                                                )
                                            } else Modifier
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(if (hasPair || isSelected) pairColor else Color(0xFF38BDF8).copy(alpha = 0.55f))
                                            .border(2.dp, CosmicNavy, CircleShape)
                                    )
                                }
                            }
                        }
                    }
                }

                // Right Column: Target answers with left-edge pins
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "KOLOM B",
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextTertiary,
                        letterSpacing = 0.6.sp,
                        modifier = Modifier.align(Alignment.End)
                    )
                    rightOptions.forEachIndexed { rIdx, optionText ->
                        val pairedIdx = leftItems.indexOfFirst { matchesMap[it.id] == optionText }
                        val isPaired = pairedIdx >= 0
                        val pairColor = if (isPaired) {
                            MATCH_PAIR_COLORS[pairedIdx % MATCH_PAIR_COLORS.size]
                        } else {
                            Color(0xFF38BDF8)
                        }
                        val isHoverTarget = hoveredRightIdx == rIdx

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .onGloballyPositioned { coords ->
                                    val parent = boardCoords
                                    if (parent != null && coords.isAttached && parent.isAttached) {
                                        val tl = parent.localPositionOf(coords, Offset.Zero)
                                        rightCardRects[rIdx] = Rect(
                                            offset = tl,
                                            size = Size(coords.size.width.toFloat(), coords.size.height.toFloat())
                                        )
                                    }
                                }
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    when {
                                        isHoverTarget -> Color(0xFF38BDF8).copy(alpha = 0.22f)
                                        isPaired -> pairColor.copy(alpha = 0.14f)
                                        else -> CosmicBlack
                                    }
                                )
                                .border(
                                    width = if (isHoverTarget || isPaired) 1.5.dp else 1.dp,
                                    color = when {
                                        isHoverTarget -> Color(0xFF38BDF8)
                                        isPaired -> pairColor.copy(alpha = 0.75f)
                                        activeLeftId != null -> Color(0xFF38BDF8).copy(alpha = 0.55f)
                                        else -> GlassBorder2
                                    },
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .then(
                                    if (!readOnly) Modifier.clickable {
                                        val targetLeftId = activeLeftId
                                            ?: leftItems.firstOrNull { matchesMap[it.id].isNullOrBlank() }?.id
                                        if (targetLeftId != null) {
                                            assignPair(targetLeftId, optionText)
                                        } else if (isPaired) {
                                            val owner = leftItems.getOrNull(pairedIdx)?.id
                                            if (owner != null) {
                                                matchesMap.remove(owner)
                                                activeLeftId = owner
                                                emitMatches()
                                            }
                                        }
                                    } else Modifier
                                )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 4.dp, end = 8.dp, top = 9.dp, bottom = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .onGloballyPositioned { coords ->
                                            val parent = boardCoords
                                            if (parent != null && coords.isAttached && parent.isAttached) {
                                                rightPinCenters[rIdx] = parent.localPositionOf(
                                                    coords,
                                                    Offset(coords.size.width / 2f, coords.size.height / 2f)
                                                )
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(11.dp)
                                            .clip(CircleShape)
                                            .background(if (isHoverTarget) Color(0xFF38BDF8) else if (isPaired) pairColor else GlassBorder2)
                                            .border(2.dp, CosmicNavy, CircleShape)
                                    )
                                }
                                Text(
                                    text = optionText,
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isPaired) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isPaired) pairColor else TextPrimary,
                                    lineHeight = 15.sp,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OrderingGamePlayer(
    questionId: String,
    configJson: String?,
    answerJson: String?,
    onAnswerJsonChanged: (String) -> Unit,
    readOnly: Boolean,
) {
    val rawItems = remember(configJson) { parseOrderingItems(configJson) }
    val itemById = remember(rawItems) { rawItems.associateBy { it.id } }
    val orderedIds = remember(questionId, configJson) {
        mutableStateListOf<String>().apply {
            addAll(rawItems.map { it.id })
        }
    }

    LaunchedEffect(answerJson, rawItems) {
        if (!answerJson.isNullOrBlank()) {
            runCatching {
                val obj = JSONObject(answerJson)
                val arr = obj.optJSONArray("order") ?: obj.optJSONArray("ordered_ids")
                if (arr != null && arr.length() > 0) {
                    val parsed = buildList {
                        for (i in 0 until arr.length()) {
                            val id = arr.optString(i, "")
                            if (id.isNotBlank() && itemById.containsKey(id)) add(id)
                        }
                    }
                    if (parsed.isNotEmpty()) {
                        orderedIds.clear()
                        orderedIds.addAll(parsed)
                        rawItems.forEach { if (!orderedIds.contains(it.id)) orderedIds.add(it.id) }
                    }
                }
            }
        } else if (orderedIds.isEmpty() && rawItems.isNotEmpty()) {
            orderedIds.addAll(rawItems.map { it.id })
        }
    }

    fun emitOrder() {
        val obj = JSONObject().apply {
            put("order", JSONArray(orderedIds.toList()))
        }
        onAnswerJsonChanged(obj.toString())
    }

    val isSaved = isInteractiveAnswerComplete("ORDERING", answerJson, configJson)
    val amber = Color(0xFFD97706)
    val haptic = LocalHapticFeedback.current
    var draggedIndex by remember(questionId) { mutableStateOf<Int?>(null) }
    var dragDeltaY by remember { mutableFloatStateOf(0f) }
    val rowHeights = remember(questionId) { mutableStateMapOf<Int, Float>() }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(if (isSaved) NeonSuccess.copy(alpha = 0.12f) else amber.copy(alpha = 0.12f))
                .border(
                    1.dp,
                    if (isSaved) NeonSuccess.copy(alpha = 0.35f) else amber.copy(alpha = 0.3f),
                    RoundedCornerShape(10.dp)
                )
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Seret pegangan ⋮⋮ atau tekan ▲ / ▼ untuk mengurutkan Awal → Akhir",
                    fontSize = 11.sp,
                    color = if (isSaved) NeonSuccess else amber,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 15.sp,
                    modifier = Modifier.weight(1f)
                )
                if (isSaved) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "✓ Siap",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = NeonSuccess
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            orderedIds.forEachIndexed { idx, id ->
                val itemText = itemById[id]?.text ?: id
                val isFirst = idx == 0
                val isLast = idx == orderedIds.lastIndex
                val isDragging = draggedIndex == idx
                val scaleAnim by animateFloatAsState(if (isDragging) 1.025f else 1f, label = "orderDragScale")

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min)
                        .zIndex(if (isDragging) 3f else 0f)
                        .graphicsLayer {
                            translationY = if (isDragging) dragDeltaY else 0f
                            scaleX = scaleAnim
                            scaleY = scaleAnim
                        }
                        .onSizeChanged { rowHeights[idx] = it.height.toFloat() },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Vertical Timeline Rail Track + Number Node
                    Box(
                        modifier = Modifier
                            .width(24.dp)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val cx = size.width / 2f
                            if (!isFirst) {
                                drawLine(
                                    color = amber.copy(alpha = 0.5f),
                                    start = Offset(cx, 0f),
                                    end = Offset(cx, size.height / 2f),
                                    strokeWidth = 2.dp.toPx()
                                )
                            }
                            if (!isLast) {
                                drawLine(
                                    color = Color(0xFF10B981).copy(alpha = 0.45f),
                                    start = Offset(cx, size.height / 2f),
                                    end = Offset(cx, size.height),
                                    strokeWidth = 2.dp.toPx()
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(if (isFirst) amber else if (isLast) NeonSuccess else Color(0xFF1E293B))
                                .border(1.5.dp, if (isFirst) amber else if (isLast) NeonSuccess else amber.copy(alpha = 0.6f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "${idx + 1}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = if (isFirst || isLast) Color.White else amber
                            )
                        }
                    }

                    // Draggable Tactile Step Card
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isDragging) amber.copy(alpha = 0.14f) else CosmicBlack)
                            .border(
                                width = if (isDragging) 1.5.dp else 1.dp,
                                color = if (isDragging) amber else if (isSaved) amber.copy(alpha = 0.45f) else GlassBorder,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (!readOnly) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(amber.copy(alpha = if (isDragging) 0.25f else 0.1f))
                                    .pointerInput(id, orderedIds.size) {
                                        detectDragGestures(
                                            onDragStart = {
                                                draggedIndex = idx
                                                dragDeltaY = 0f
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            },
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                val cur = draggedIndex ?: return@detectDragGestures
                                                dragDeltaY += dragAmount.y
                                                val stepH = (rowHeights[cur] ?: 72f).coerceAtLeast(48f) * 0.65f
                                                if (dragDeltaY > stepH && cur < orderedIds.lastIndex) {
                                                    val tmp = orderedIds[cur + 1]
                                                    orderedIds[cur + 1] = orderedIds[cur]
                                                    orderedIds[cur] = tmp
                                                    draggedIndex = cur + 1
                                                    dragDeltaY -= (rowHeights[cur + 1] ?: 72f)
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                } else if (dragDeltaY < -stepH && cur > 0) {
                                                    val tmp = orderedIds[cur - 1]
                                                    orderedIds[cur - 1] = orderedIds[cur]
                                                    orderedIds[cur] = tmp
                                                    draggedIndex = cur - 1
                                                    dragDeltaY += (rowHeights[cur - 1] ?: 72f)
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                }
                                            },
                                            onDragEnd = {
                                                draggedIndex = null
                                                dragDeltaY = 0f
                                                emitOrder()
                                            },
                                            onDragCancel = {
                                                draggedIndex = null
                                                dragDeltaY = 0f
                                            }
                                        )
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DragIndicator,
                                    contentDescription = "Seret urutan",
                                    tint = amber,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            if (isFirst || isLast) {
                                Text(
                                    text = if (isFirst) "AWAL PROSES" else "TAHAP AKHIR",
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isFirst) amber else NeonSuccess
                                )
                            }
                            Text(
                                text = itemText,
                                fontSize = 12.sp,
                                color = TextPrimary,
                                fontWeight = FontWeight.Medium,
                                lineHeight = 16.sp,
                            )
                        }

                        if (!readOnly) {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Surface(
                                    shape = RoundedCornerShape(7.dp),
                                    color = if (idx > 0) amber.copy(alpha = 0.16f) else Color.Transparent,
                                    border = BorderStroke(1.dp, if (idx > 0) amber.copy(alpha = 0.4f) else GlassBorder),
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(RoundedCornerShape(7.dp))
                                        .clickable(enabled = idx > 0) {
                                            val tmp = orderedIds[idx - 1]
                                            orderedIds[idx - 1] = orderedIds[idx]
                                            orderedIds[idx] = tmp
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            emitOrder()
                                        }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.ArrowUpward,
                                            contentDescription = "Naik",
                                            tint = if (idx > 0) amber else TextTertiary.copy(alpha = 0.4f),
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(7.dp),
                                    color = if (idx < orderedIds.lastIndex) amber.copy(alpha = 0.16f) else Color.Transparent,
                                    border = BorderStroke(1.dp, if (idx < orderedIds.lastIndex) amber.copy(alpha = 0.4f) else GlassBorder),
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(RoundedCornerShape(7.dp))
                                        .clickable(enabled = idx < orderedIds.lastIndex) {
                                            val tmp = orderedIds[idx + 1]
                                            orderedIds[idx + 1] = orderedIds[idx]
                                            orderedIds[idx] = tmp
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            emitOrder()
                                        }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.ArrowDownward,
                                            contentDescription = "Turun",
                                            tint = if (idx < orderedIds.lastIndex) amber else TextTertiary.copy(alpha = 0.4f),
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (!readOnly && !isSaved && orderedIds.isNotEmpty()) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = amber.copy(alpha = 0.14f),
                border = BorderStroke(1.dp, amber.copy(alpha = 0.45f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        emitOrder()
                    }
            ) {
                Box(
                    modifier = Modifier.padding(vertical = 9.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "✓ Tetapkan Urutan Ini sebagai Jawaban",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = amber
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FillBlankGamePlayer(
    questionId: String,
    configJson: String?,
    answerJson: String?,
    onAnswerJsonChanged: (String) -> Unit,
    readOnly: Boolean,
) {
    val cfg = remember(configJson) { parseFillBlankStudentConfig(configJson) }
    val fills = remember(questionId, cfg.blankCount) {
        mutableStateListOf<String>().apply {
            repeat(cfg.blankCount) { add("") }
        }
    }
    var activeSlot by remember(questionId) { mutableIntStateOf(0) }

    LaunchedEffect(answerJson, cfg.blankCount) {
        if (!answerJson.isNullOrBlank()) {
            runCatching {
                val obj = JSONObject(answerJson)
                val arr = obj.optJSONArray("fills") ?: obj.optJSONArray("answers")
                if (arr != null) {
                    for (i in 0 until cfg.blankCount) {
                        fills[i] = arr.optString(i, "")
                    }
                }
            }
        }
    }

    fun emitFills() {
        val obj = JSONObject().apply {
            put("fills", JSONArray(fills.toList()))
        }
        onAnswerJsonChanged(obj.toString())
    }

    val normTemplate = remember(cfg.template) { normalizeFillBlankTemplate(cfg.template) }
    val shuffledWordBank = remember(questionId, cfg.wordBank) {
        shuffleDeterministic(cfg.wordBank, questionId.ifBlank { cfg.template })
    }
    val filledCount = fills.count { it.isNotBlank() }
    val emerald = Color(0xFF10B981)
    val sky = Color(0xFF38BDF8)

    val (annotatedSentence, inlineContentMap) = remember(
        normTemplate,
        fills.toList(),
        activeSlot,
        readOnly,
        cfg.blankCount,
    ) {
        val inlineMap = mutableMapOf<String, InlineTextContent>()
        val tokenRegex = Regex("\\{\\{(\\d+)\\}\\}")
        val annotated = buildAnnotatedString {
            var cursor = 0
            tokenRegex.findAll(normTemplate).forEach { match ->
                if (match.range.first > cursor) {
                    append(normTemplate.substring(cursor, match.range.first))
                }
                val slotNum = match.groupValues[1].toIntOrNull() ?: 1
                val slotIdx = (slotNum - 1).coerceIn(0, (cfg.blankCount - 1).coerceAtLeast(0))
                val inlineKey = "slot_$slotIdx"
                val filledWord = fills.getOrNull(slotIdx)?.takeIf { it.isNotBlank() }
                val isFocused = activeSlot == slotIdx && !readOnly

                val chipLabel = when {
                    filledWord != null -> "${slotIdx + 1}. $filledWord"
                    isFocused -> "${slotIdx + 1}. pilih kata"
                    else -> "${slotIdx + 1}. kosong"
                }
                val estEm = (chipLabel.length * 0.62f + if (filledWord != null && !readOnly) 3.0f else 1.8f)
                    .coerceIn(5.8f, 18.5f)
                    .em

                inlineMap[inlineKey] = InlineTextContent(
                    placeholder = Placeholder(
                        width = estEm,
                        height = 2.05.em,
                        placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter
                    )
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = when {
                            filledWord != null -> emerald.copy(alpha = 0.20f)
                            isFocused -> sky.copy(alpha = 0.20f)
                            else -> CosmicNavy
                        },
                        border = BorderStroke(
                            width = if (isFocused) 1.5.dp else 1.dp,
                            color = when {
                                filledWord != null -> emerald
                                isFocused -> sky
                                else -> GlassBorder2
                            }
                        ),
                        modifier = Modifier
                            .padding(horizontal = 2.dp, vertical = 1.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(enabled = !readOnly) {
                                if (filledWord != null) {
                                    fills[slotIdx] = ""
                                    activeSlot = slotIdx
                                    emitFills()
                                } else {
                                    activeSlot = slotIdx
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = chipLabel,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    filledWord != null -> emerald
                                    isFocused -> sky
                                    else -> TextSecondary
                                },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            if (filledWord != null && !readOnly) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Kosongkan kotak",
                                    tint = emerald,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }
                appendInlineContent(inlineKey, "[#$slotNum]")
                cursor = match.range.last + 1
            }
            if (cursor < normTemplate.length) {
                append(normTemplate.substring(cursor))
            }
        }
        annotated to inlineMap
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Clear 1-line guidance + progress badge
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(emerald.copy(alpha = 0.12f))
                .border(1.dp, emerald.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (filledCount < cfg.blankCount) {
                        "Kotak #${activeSlot.coerceIn(0, (cfg.blankCount - 1).coerceAtLeast(0)) + 1} aktif — ketuk kata di Bank Kata di bawah"
                    } else {
                        "Semua kotak terisi! Ketuk kata di kotak jika ingin diganti."
                    },
                    fontSize = 11.sp,
                    color = emerald,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 15.sp,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "$filledCount / ${cfg.blankCount}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = emerald
                    )
                    if (!readOnly && filledCount > 0) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable {
                                    for (i in fills.indices) fills[i] = ""
                                    activeSlot = 0
                                    emitFills()
                                }
                                .padding(top = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(Icons.Default.Refresh, null, tint = TextTertiary, modifier = Modifier.size(11.dp))
                            Text("Reset", fontSize = 10.sp, color = TextTertiary, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        // Single clean reading box with interactive inline pills
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(CosmicBlack)
                .border(1.dp, GlassBorder2, RoundedCornerShape(12.dp))
                .padding(12.dp)
        ) {
            if (normTemplate.isBlank()) {
                Text(
                    text = "(Kalimat rumpang belum dikonfigurasi)",
                    fontSize = 13.sp,
                    color = TextTertiary,
                    lineHeight = 20.sp
                )
            } else {
                Text(
                    text = annotatedSentence,
                    inlineContent = inlineContentMap,
                    fontSize = 13.5.sp,
                    color = TextPrimary,
                    lineHeight = 26.sp
                )
            }
        }

        // Word Bank: Duolingo-style 3D Tactile Word Tiles
        if (shuffledWordBank.isNotEmpty() && !readOnly) {
            val haptic = LocalHapticFeedback.current
            Text(
                "Bank Kata (ketuk ubin untuk mengisi Kotak #${activeSlot.coerceIn(0, (cfg.blankCount - 1).coerceAtLeast(0)) + 1}):",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                shuffledWordBank.forEach { word ->
                    val usedInSlot = fills.indexOfFirst { it == word }
                    val isUsed = usedInSlot >= 0
                    val interactionSource = remember { MutableInteractionSource() }
                    val isPressed by interactionSource.collectIsPressedAsState()
                    val topOffset by animateDpAsState(
                        targetValue = when {
                            isUsed -> 3.dp
                            isPressed -> 3.dp
                            else -> 0.dp
                        },
                        label = "tilePressOffset"
                    )

                    // Outer 3D Lip Container
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isUsed) Color.Transparent else Color(0xFF047857))
                            .padding(bottom = if (isUsed) 0.dp else 3.5.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(11.dp),
                            color = if (isUsed) emerald.copy(alpha = 0.12f) else CosmicBlack,
                            border = BorderStroke(
                                width = 1.2.dp,
                                color = if (isUsed) emerald.copy(alpha = 0.45f) else emerald
                            ),
                            modifier = Modifier
                                .offset(y = topOffset)
                                .clip(RoundedCornerShape(11.dp))
                                .clickable(
                                    interactionSource = interactionSource,
                                    indication = null,
                                ) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    val targetSlot = activeSlot.coerceIn(0, (cfg.blankCount - 1).coerceAtLeast(0))
                                    if (isUsed && usedInSlot == targetSlot) {
                                        fills[targetSlot] = ""
                                    } else {
                                        if (isUsed) fills[usedInSlot] = ""
                                        fills[targetSlot] = word
                                        val nextEmpty = (0 until cfg.blankCount).firstOrNull { fills[it].isBlank() }
                                        if (nextEmpty != null) activeSlot = nextEmpty
                                    }
                                    emitFills()
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                if (isUsed) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Sudah dipakai",
                                        tint = emerald,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                                Text(
                                    text = word,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isUsed) emerald else TextPrimary,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CrosswordGamePlayer(
    questionId: String,
    configJson: String?,
    answerJson: String?,
    onAnswerJsonChanged: (String) -> Unit,
    readOnly: Boolean,
) {
    val baseGrid = remember(configJson) { parseCrosswordGrid(configJson) }
    val clues = remember(configJson) { parseCrosswordClues(configJson) }
    val haptics = LocalHapticFeedback.current
    val isDark = LocalIsDarkTheme.current
    val accentPurple = AccentPurple
    val emerald = NeonSuccess
    val amber = NeonWarning
    val danger = NeonError
    val boardBg = CosmicDark
    val boardBorder = GlassBorder2
    val playableTileBg = if (isDark) CosmicSurface2 else CosmicNavy
    val keyBg = CosmicNavy
    val selectedBg = MaterialTheme.colorScheme.secondary
    val selectedContentColor = MaterialTheme.colorScheme.onSecondary
    val activeWordBg = MaterialTheme.colorScheme.secondaryContainer
    val activeWordContentColor = MaterialTheme.colorScheme.onSecondaryContainer

    val numberByStart = remember(clues) {
        val map = mutableMapOf<Pair<Int, Int>, Int>()
        clues.forEach { cl ->
            val key = cl.row to cl.col
            if (!map.containsKey(key)) map[key] = cl.number
        }
        map
    }

    var board by remember(questionId, configJson, answerJson) {
        val savedRows = runCatching {
            if (answerJson.isNullOrBlank()) null
            else JSONObject(answerJson).optJSONArray("board")
        }.getOrNull()
        mutableStateOf(
            baseGrid.mapIndexed { rIdx, rowStr ->
                val savedRow = savedRows?.optString(rIdx, "") ?: ""
                rowStr.mapIndexed { cIdx, ch ->
                    if (ch == '#') '#'
                    else {
                        val sch = savedRow.getOrNull(cIdx)?.uppercaseChar()
                        if (sch != null && sch in 'A'..'Z') sch else ' '
                    }
                }.toCharArray()
            }
        )
    }

    var selectedCell by remember(questionId, configJson) {
        mutableStateOf(clues.firstOrNull()?.let { it.row to it.col })
    }
    var direction by remember(questionId, configJson) {
        mutableStateOf(clues.firstOrNull()?.direction ?: "ACROSS")
    }

    fun findClueAt(r: Int, c: Int, preferredDir: String): CrosswordClueItem? {
        fun matches(cl: CrosswordClueItem): Boolean =
            if (cl.direction == "ACROSS") r == cl.row && c in cl.col until (cl.col + cl.length)
            else c == cl.col && r in cl.row until (cl.row + cl.length)
        return clues.firstOrNull { it.direction == preferredDir && matches(it) }
            ?: clues.firstOrNull { matches(it) }
    }

    val activeClue = remember(selectedCell, direction, clues) {
        selectedCell?.let { (r, c) -> findClueAt(r, c, direction) } ?: clues.firstOrNull()
    }

    val highlightedCells = remember(activeClue) {
        val set = HashSet<Pair<Int, Int>>()
        activeClue?.let { cl ->
            for (k in 0 until cl.length) {
                val rr = if (cl.direction == "DOWN") cl.row + k else cl.row
                val cc = if (cl.direction == "DOWN") cl.col else cl.col + k
                set.add(rr to cc)
            }
        }
        set
    }

    fun emitBoard(nextBoard: List<CharArray>) {
        val boardArr = JSONArray()
        nextBoard.forEach { rowArr ->
            boardArr.put(rowArr.map { ch -> if (ch == ' ') '.' else ch }.joinToString(""))
        }
        val wordsObj = JSONObject()
        clues.forEach { cl ->
            val sb = StringBuilder()
            for (k in 0 until cl.length) {
                val rr = if (cl.direction == "DOWN") cl.row + k else cl.row
                val cc = if (cl.direction == "DOWN") cl.col else cl.col + k
                val ch = nextBoard.getOrNull(rr)?.getOrNull(cc) ?: ' '
                if (ch in 'A'..'Z') sb.append(ch)
            }
            if (sb.isNotEmpty()) wordsObj.put("${cl.number}_${cl.direction}", sb.toString())
        }
        onAnswerJsonChanged(
            JSONObject().apply {
                put("board", boardArr)
                put("words", wordsObj)
            }.toString()
        )
    }

    fun stepWithinClue(r: Int, c: Int, delta: Int, clue: CrosswordClueItem?): Pair<Int, Int>? {
        if (clue != null) {
            val idx = if (clue.direction == "DOWN") r - clue.row else c - clue.col
            val nextIdx = idx + delta
            if (nextIdx in 0 until clue.length) {
                return if (clue.direction == "DOWN") (clue.row + nextIdx) to clue.col
                else clue.row to (clue.col + nextIdx)
            }
        }
        val nr = if (direction == "DOWN") r + delta else r
        val nc = if (direction == "ACROSS") c + delta else c
        if (nr in board.indices && nc in board[nr].indices && board[nr][nc] != '#') {
            return nr to nc
        }
        return null
    }

    val totalPlayable = remember(board) {
        board.sumOf { rowArr -> rowArr.count { it != '#' } }
    }
    val filledPlayable = remember(board) {
        board.sumOf { rowArr -> rowArr.count { it in 'A'..'Z' } }
    }

    if (board.isEmpty() || clues.isEmpty()) {
        Text(
            text = "Papan Teka-Teki Silang belum memiliki daftar petunjuk.",
            fontSize = 12.sp,
            color = TextSecondary
        )
        return
    }

    val colsCount = board.firstOrNull()?.size ?: 1
    val cellDp = when {
        colsCount >= 13 -> 24.dp
        colsCount >= 10 -> 28.dp
        else -> 32.dp
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Header row: progress pill + direction toggle + reset
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = accentPurple.copy(alpha = 0.14f),
                border = BorderStroke(1.dp, accentPurple.copy(alpha = 0.35f)),
            ) {
                Text(
                    text = "🧩 $filledPlayable/$totalPlayable kotak terisi",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentPurple,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }

            if (!readOnly) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = accentPurple.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, accentPurple.copy(alpha = 0.35f)),
                        modifier = Modifier.clickable {
                            direction = if (direction == "ACROSS") "DOWN" else "ACROSS"
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                    ) {
                        Text(
                            text = if (direction == "DOWN") "Menurun ↓" else "Mendatar →",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentPurple,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = CosmicNavy,
                        border = BorderStroke(1.dp, boardBorder),
                        modifier = Modifier.clickable {
                            val reset = baseGrid.map { rowStr ->
                                rowStr.map { ch -> if (ch == '#') '#' else ' ' }.toCharArray()
                            }
                            board = reset
                            emitBoard(reset)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset TTS",
                            tint = TextSecondary,
                            modifier = Modifier.padding(6.dp).size(15.dp)
                        )
                    }
                }
            }
        }

        if (totalPlayable > 0 && filledPlayable == totalPlayable) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(emerald.copy(alpha = 0.14f))
                    .border(1.dp, emerald.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 7.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Celebration,
                    contentDescription = "Lengkap",
                    tint = emerald,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = "Semua Kotak TTS Terisi!",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = emerald
                )
            }
        }

        // Active clue highlight box
        if (activeClue != null) {
            val dirName = if (activeClue.direction == "DOWN") "Menurun" else "Mendatar"
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(accentPurple.copy(alpha = 0.10f))
                    .border(1.dp, accentPurple.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "${activeClue.number} $dirName (${activeClue.length} huruf): ${activeClue.clue}",
                    fontSize = 12.sp,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 17.sp
                )
            }
        }

        // Scrollable interactive 2D Crossword grid
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(boardBg)
                .border(1.dp, boardBorder, RoundedCornerShape(14.dp))
                .padding(10.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(3.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                board.forEachIndexed { r, rowArr ->
                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        rowArr.forEachIndexed { c, ch ->
                            if (ch == '#') {
                                Spacer(modifier = Modifier.size(cellDp))
                            } else {
                                val isSel = selectedCell?.first == r && selectedCell?.second == c
                                val isInActiveWord = highlightedCells.contains(r to c)
                                val num = numberByStart[r to c]
                                val cellBg = when {
                                    isSel -> selectedBg
                                    isInActiveWord -> activeWordBg
                                    else -> playableTileBg
                                }
                                val cellBorder = when {
                                    isSel -> accentPurple
                                    isInActiveWord -> accentPurple.copy(alpha = 0.70f)
                                    ch in 'A'..'Z' -> emerald.copy(alpha = 0.65f)
                                    else -> boardBorder
                                }
                                val cellTextColor = when {
                                    isSel -> selectedContentColor
                                    isInActiveWord -> activeWordContentColor
                                    else -> TextPrimary
                                }
                                val numTextColor = when {
                                    isSel -> selectedContentColor.copy(alpha = 0.9f)
                                    isInActiveWord -> activeWordContentColor.copy(alpha = 0.85f)
                                    else -> TextSecondary
                                }
                                Box(
                                    modifier = Modifier
                                        .size(cellDp)
                                        .clip(RoundedCornerShape(5.dp))
                                        .background(cellBg)
                                        .border(if (isSel) 1.5.dp else 1.dp, cellBorder, RoundedCornerShape(5.dp))
                                        .clickable(enabled = !readOnly) {
                                            if (selectedCell == (r to c)) {
                                                val other = if (direction == "ACROSS") "DOWN" else "ACROSS"
                                                if (findClueAt(r, c, other)?.direction == other) {
                                                    direction = other
                                                }
                                            } else {
                                                selectedCell = r to c
                                                findClueAt(r, c, direction)?.let { direction = it.direction }
                                            }
                                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        }
                                ) {
                                    if (num != null) {
                                        Text(
                                            text = num.toString(),
                                            fontSize = 7.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = numTextColor,
                                            modifier = Modifier.align(Alignment.TopStart).padding(start = 2.dp, top = 0.dp)
                                        )
                                    }
                                    if (ch in 'A'..'Z') {
                                        Text(
                                            text = ch.toString(),
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = cellTextColor,
                                            modifier = Modifier.align(Alignment.Center)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Clues summary (clickable to jump to word)
        val acrossClues = clues.filter { it.direction == "ACROSS" }
        val downClues = clues.filter { it.direction == "DOWN" }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (acrossClues.isNotEmpty()) {
                Text("MENDATAR", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = accentPurple)
                acrossClues.forEach { cl ->
                    val filled = (0 until cl.length).count { k ->
                        (board.getOrNull(cl.row)?.getOrNull(cl.col + k) ?: ' ') in 'A'..'Z'
                    }
                    val isActive = activeClue?.number == cl.number && activeClue?.direction == cl.direction
                    Surface(
                        modifier = Modifier.fillMaxWidth().clickable {
                            direction = cl.direction
                            var target: Pair<Int, Int>? = null
                            for (k in 0 until cl.length) {
                                val cellChar = board.getOrNull(cl.row)?.getOrNull(cl.col + k) ?: ' '
                                if (cellChar !in 'A'..'Z') {
                                    target = cl.row to (cl.col + k)
                                    break
                                }
                            }
                            selectedCell = target ?: (cl.row to cl.col)
                        },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isActive) accentPurple.copy(alpha = 0.14f) else playableTileBg,
                        border = BorderStroke(1.dp, if (isActive) accentPurple.copy(alpha = 0.6f) else boardBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${cl.number}. ${cl.clue}",
                                fontSize = 11.5.sp,
                                color = TextPrimary,
                                modifier = Modifier.weight(1f).padding(end = 8.dp)
                            )
                            Text(
                                text = "$filled/${cl.length}",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (filled == cl.length) emerald else TextSecondary
                            )
                        }
                    }
                }
            }
            if (downClues.isNotEmpty()) {
                Text("MENURUN", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = amber)
                downClues.forEach { cl ->
                    val filled = (0 until cl.length).count { k ->
                        (board.getOrNull(cl.row + k)?.getOrNull(cl.col) ?: ' ') in 'A'..'Z'
                    }
                    val isActive = activeClue?.number == cl.number && activeClue?.direction == cl.direction
                    Surface(
                        modifier = Modifier.fillMaxWidth().clickable {
                            direction = cl.direction
                            var target: Pair<Int, Int>? = null
                            for (k in 0 until cl.length) {
                                val cellChar = board.getOrNull(cl.row + k)?.getOrNull(cl.col) ?: ' '
                                if (cellChar !in 'A'..'Z') {
                                    target = (cl.row + k) to cl.col
                                    break
                                }
                            }
                            selectedCell = target ?: (cl.row to cl.col)
                        },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isActive) accentPurple.copy(alpha = 0.14f) else playableTileBg,
                        border = BorderStroke(1.dp, if (isActive) accentPurple.copy(alpha = 0.6f) else boardBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${cl.number}. ${cl.clue}",
                                fontSize = 11.5.sp,
                                color = TextPrimary,
                                modifier = Modifier.weight(1f).padding(end = 8.dp)
                            )
                            Text(
                                text = "$filled/${cl.length}",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (filled == cl.length) emerald else TextSecondary
                            )
                        }
                    }
                }
            }
        }

        // Virtual QWERTY keyboard for quick tap entry without soft keyboard occlusion
        if (!readOnly) {
            val keyRows = remember {
                listOf("QWERTYUIOP", "ASDFGHJKL", "ZXCVBNM")
            }
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                keyRows.forEachIndexed { rowIdx, keysStr ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        keysStr.forEach { keyChar ->
                            Surface(
                                modifier = Modifier
                                    .width(30.dp)
                                    .height(36.dp)
                                    .clickable {
                                        val cur = selectedCell ?: return@clickable
                                        val (r, c) = cur
                                        if (board.getOrNull(r)?.getOrNull(c) == '#') return@clickable
                                        val next = board.map { it.copyOf() }
                                        next[r][c] = keyChar
                                        board = next
                                        emitBoard(next)
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        stepWithinClue(r, c, 1, activeClue)?.let { selectedCell = it }
                                    },
                                shape = RoundedCornerShape(7.dp),
                                color = keyBg,
                                border = BorderStroke(1.dp, boardBorder),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = keyChar.toString(),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                }
                            }
                        }
                        if (rowIdx == keyRows.lastIndex) {
                            Surface(
                                modifier = Modifier
                                    .height(36.dp)
                                    .clickable {
                                        val cur = selectedCell ?: return@clickable
                                        val (r, c) = cur
                                        if (board.getOrNull(r)?.getOrNull(c) == '#') return@clickable
                                        val next = board.map { it.copyOf() }
                                        if (next[r][c] != ' ') {
                                            next[r][c] = ' '
                                            board = next
                                            emitBoard(next)
                                        } else {
                                            val prev = stepWithinClue(r, c, -1, activeClue)
                                            if (prev != null) {
                                                next[prev.first][prev.second] = ' '
                                                board = next
                                                selectedCell = prev
                                                emitBoard(next)
                                            }
                                        }
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    },
                                shape = RoundedCornerShape(7.dp),
                                color = danger.copy(alpha = 0.14f),
                                border = BorderStroke(1.dp, danger.copy(alpha = 0.35f)),
                            ) {
                                Box(
                                    modifier = Modifier.padding(horizontal = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "⌫",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = danger
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── STUDENT REVIEW CARD (AFTER SUBMISSION / AUTO-GRADING) ────────────────────

@Composable
fun SubmittedInteractiveGameReviewCard(
    index: Int,
    question: AssignmentQuestion,
    answer: SubmissionAnswer?,
    isGraded: Boolean,
) {
    val normType = question.questionType.trim().uppercase()
    val accent = questionTypeAccentColor(normType)
    val typeLabel = questionTypeDisplayLabel(normType)
    val maxPoints = question.points ?: 20
    val pointsEarned = answer?.pointsEarned ?: 0
    val summaryLines = remember(normType, answer?.textAnswer, question.interactionConfigJson) {
        formatInteractiveAnswerSummary(normType, answer?.textAnswer, question.interactionConfigJson)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CosmicNavy)
            .border(1.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("$index", fontSize = 11.sp, fontWeight = FontWeight.Black, color = accent)
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    val reviewPrompt = remember(normType, question.questionText, question.interactionConfigJson) {
                        if (normType == "FILL_BLANK") {
                            extractFillBlankStudentHeadline(question.questionText, question.interactionConfigJson)
                        } else {
                            question.questionText
                        }
                    }
                    QuestionPromptWithDiagram(
                        rawText = reviewPrompt,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        lineHeight = 20.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "$maxPoints poin · $typeLabel",
                            fontSize = 10.sp,
                            color = TextTertiary
                        )
                        if (isGraded) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(NeonSuccess.copy(alpha = 0.15f))
                                    .border(0.5.dp, NeonSuccess.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "Skor Otomatis: $pointsEarned / $maxPoints",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonSuccess
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = GlassBorder, thickness = 0.5.dp)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(CosmicBlack)
                    .border(1.dp, GlassBorder2, RoundedCornerShape(10.dp))
                    .padding(12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(
                        text = "JAWABAN MISI GAME KAMU",
                        fontSize = 10.sp,
                        color = accent,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.6.sp
                    )
                    summaryLines.forEach { line ->
                        Text(
                            text = line,
                            fontSize = 12.5.sp,
                            color = TextPrimary,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }
    }
}

// ── TEACHER PREVIEW SUMMARY (AI GENERATOR & REVIEW) ──────────────────────────

@Composable
fun InteractiveGamePreviewBlock(
    questionType: String,
    interactionConfigJson: String?,
    subSurfaceBg: Color,
    subSurfaceBorder: Color,
    textPrimary: Color,
    textSecondary: Color,
) {
    val lines = remember(questionType, interactionConfigJson) {
        formatTeacherConfigSummary(questionType, interactionConfigJson)
    }
    if (lines.isEmpty()) return

    val accent = questionTypeAccentColor(questionType)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(subSurfaceBg)
            .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = questionTypeDisplayLabel(questionType),
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Bold,
            color = accent
        )
        lines.forEach { line ->
            Text(
                text = line,
                fontSize = 11.5.sp,
                color = textPrimary,
                lineHeight = 16.sp
            )
        }
        Spacer(Modifier.height(1.dp))
        Text(
            text = "Siswa akan menerima urutan teracak secara otomatis.",
            fontSize = 10.sp,
            color = textSecondary
        )
    }
}

// ── TEACHER EDITOR IN ASSIGNMENT CREATOR ──────────────────────────────────────

@Composable
fun InteractiveGameTeacherEditor(
    questionType: String,
    configJson: String?,
    onConfigJsonChange: (String) -> Unit,
    subSurfaceBg: Color,
    subSurfaceBorder: Color,
    textPrimary: Color,
    textSecondary: Color,
    textTertiary: Color,
    emeraldAccent: Color,
) {
    val normType = questionType.trim().uppercase()
    val effectiveJson = configJson?.takeIf { it.isNotBlank() && it != "null" }
        ?: createDefaultInteractionConfigJson(normType)

    when (normType) {
        "MATCHING" -> {
            val pairs = remember(effectiveJson) {
                runCatching {
                    val arr = JSONObject(effectiveJson).optJSONArray("pairs")
                    buildList {
                        if (arr != null) {
                            for (i in 0 until arr.length()) {
                                val p = arr.optJSONObject(i) ?: continue
                                add(
                                    TeacherMatchPair(
                                        id = p.optString("id", "p${i + 1}"),
                                        left = p.optString("left", ""),
                                        right = p.optString("right", ""),
                                    )
                                )
                            }
                        }
                    }.ifEmpty {
                        listOf(
                            TeacherMatchPair("p1", "", ""),
                            TeacherMatchPair("p2", "", "")
                        )
                    }
                }.getOrElse {
                    listOf(
                        TeacherMatchPair("p1", "", ""),
                        TeacherMatchPair("p2", "", "")
                    )
                }
            }

            fun savePairs(next: List<TeacherMatchPair>) {
                val arr = JSONArray()
                next.forEachIndexed { idx, pair ->
                    arr.put(JSONObject().apply {
                        put("id", pair.id.ifBlank { "p${idx + 1}" })
                        put("left", pair.left)
                        put("right", pair.right)
                    })
                }
                onConfigJsonChange(JSONObject().put("pairs", arr).toString())
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Pasangan Menjodohkan (${pairs.size} pasang)",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0284C7)
                    )
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF0284C7).copy(alpha = 0.14f))
                            .clickable {
                                savePairs(pairs + TeacherMatchPair("p${pairs.size + 1}", "", ""))
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.Add, null, tint = Color(0xFF0284C7), modifier = Modifier.size(13.dp))
                        Text("Pasangan", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0284C7))
                    }
                }

                pairs.forEachIndexed { idx, pair ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(subSurfaceBg)
                            .border(1.dp, subSurfaceBorder, RoundedCornerShape(10.dp))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Pasangan #${idx + 1}", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = textSecondary)
                            if (pairs.size > 2) {
                                Icon(
                                    Icons.Default.DeleteOutline,
                                    contentDescription = "Hapus pasangan",
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clickable {
                                            savePairs(pairs.filterIndexed { i, _ -> i != idx })
                                        }
                                )
                            }
                        }
                        OutlinedTextField(
                            value = pair.left,
                            onValueChange = { v ->
                                savePairs(pairs.mapIndexed { i, p -> if (i == idx) p.copy(left = v) else p })
                            },
                            label = { Text("Item Kiri", fontSize = 11.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = emeraldAccent,
                                unfocusedBorderColor = subSurfaceBorder,
                                focusedTextColor = textPrimary,
                                unfocusedTextColor = textPrimary,
                            )
                        )
                        OutlinedTextField(
                            value = pair.right,
                            onValueChange = { v ->
                                savePairs(pairs.mapIndexed { i, p -> if (i == idx) p.copy(right = v) else p })
                            },
                            label = { Text("Pasangan Kanan (Kunci)", fontSize = 11.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = emeraldAccent,
                                unfocusedBorderColor = subSurfaceBorder,
                                focusedTextColor = textPrimary,
                                unfocusedTextColor = textPrimary,
                            )
                        )
                    }
                }
            }
        }

        "ORDERING" -> {
            val items = remember(effectiveJson) {
                parseOrderingItems(effectiveJson).ifEmpty {
                    listOf(GameItemPair("o1", ""), GameItemPair("o2", ""))
                }
            }

            fun saveItems(next: List<GameItemPair>) {
                val arr = JSONArray()
                next.forEachIndexed { idx, it ->
                    arr.put(JSONObject().apply {
                        put("id", it.id.ifBlank { "o${idx + 1}" })
                        put("text", it.text)
                    })
                }
                onConfigJsonChange(JSONObject().put("items", arr).toString())
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Urutan Benar (Awal -> Akhir)",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFD97706)
                    )
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFD97706).copy(alpha = 0.14f))
                            .clickable {
                                saveItems(items + GameItemPair("o${items.size + 1}", ""))
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.Add, null, tint = Color(0xFFD97706), modifier = Modifier.size(13.dp))
                        Text("Langkah", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD97706))
                    }
                }

                items.forEachIndexed { idx, item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedTextField(
                            value = item.text,
                            onValueChange = { v ->
                                saveItems(items.mapIndexed { i, it -> if (i == idx) it.copy(text = v) else it })
                            },
                            label = { Text("Langkah ${idx + 1}", fontSize = 11.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = emeraldAccent,
                                unfocusedBorderColor = subSurfaceBorder,
                                focusedTextColor = textPrimary,
                                unfocusedTextColor = textPrimary,
                            )
                        )
                        if (items.size > 2) {
                            IconButton(
                                onClick = { saveItems(items.filterIndexed { i, _ -> i != idx }) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.DeleteOutline, null, tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }

        "FILL_BLANK" -> {
            val (template, blanks, distractors) = remember(effectiveJson) {
                runCatching {
                    val obj = JSONObject(effectiveJson)
                    val tpl = obj.optString("template", "")
                    val bArr = obj.optJSONArray("blanks")
                    val dArr = obj.optJSONArray("distractors")
                    val bList = buildList {
                        if (bArr != null) {
                            for (i in 0 until bArr.length()) add(bArr.optString(i, ""))
                        }
                    }
                    val dList = buildList {
                        if (dArr != null) {
                            for (i in 0 until dArr.length()) add(dArr.optString(i, ""))
                        }
                    }
                    Triple(tpl, bList, dList)
                }.getOrElse { Triple("", emptyList(), emptyList()) }
            }

            fun saveFillBlank(tpl: String, bList: List<String>, dList: List<String>) {
                val obj = JSONObject().apply {
                    put("template", tpl)
                    put("blanks", JSONArray(bList))
                    put("distractors", JSONArray(dList))
                }
                onConfigJsonChange(obj.toString())
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Gunakan {{1}}, {{2}} atau ___ pada kalimat untuk menandai bagian rumpang.",
                    fontSize = 10.5.sp,
                    color = textTertiary
                )
                OutlinedTextField(
                    value = template,
                    onValueChange = { tpl ->
                        val count = countTemplateBlanks(tpl)
                        val resized = List(count) { i -> blanks.getOrElse(i) { "" } }
                        saveFillBlank(tpl, resized, distractors)
                    },
                    label = { Text("Teks Kalimat Rumpang", fontSize = 11.sp) },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = emeraldAccent,
                        unfocusedBorderColor = subSurfaceBorder,
                        focusedTextColor = textPrimary,
                        unfocusedTextColor = textPrimary,
                    )
                )

                val expectedCount = maxOf(blanks.size, countTemplateBlanks(template))
                for (i in 0 until expectedCount) {
                    OutlinedTextField(
                        value = blanks.getOrElse(i) { "" },
                        onValueChange = { v ->
                            val updated = MutableList(expectedCount) { idx -> blanks.getOrElse(idx) { "" } }
                            updated[i] = v
                            saveFillBlank(template, updated, distractors)
                        },
                        label = { Text("Kunci Jawaban Bagian #${i + 1}", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = emeraldAccent,
                            unfocusedBorderColor = subSurfaceBorder,
                            focusedTextColor = textPrimary,
                            unfocusedTextColor = textPrimary,
                        )
                    )
                }

                OutlinedTextField(
                    value = distractors.joinToString(", "),
                    onValueChange = { v ->
                        val nextD = v.split(",").map { it.trim() }.filter { it.isNotBlank() }
                        saveFillBlank(template, blanks, nextD)
                    },
                    label = { Text("Kata Pengecoh (pisahkan koma)", fontSize = 11.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = emeraldAccent,
                        unfocusedBorderColor = subSurfaceBorder,
                        focusedTextColor = textPrimary,
                        unfocusedTextColor = textPrimary,
                    )
                )
            }
        }
        "CROSSWORD" -> {
            val violet = AccentPurple
            val danger = NeonError
            val words = remember(effectiveJson) {
                runCatching {
                    val obj = JSONObject(effectiveJson)
                    val arr = obj.optJSONArray("words") ?: obj.optJSONArray("clues")
                    buildList {
                        if (arr != null) {
                            for (i in 0 until arr.length()) {
                                val w = arr.optJSONObject(i) ?: continue
                                add(w.optString("answer", "") to w.optString("clue", ""))
                            }
                        }
                    }
                }.getOrElse { emptyList() }.ifEmpty {
                    listOf(
                        "EVAPORASI" to "Proses penguapan air akibat panas matahari",
                        "HUJAN" to "Titik air yang jatuh dari awan ke bumi",
                    )
                }
            }

            fun saveWords(nextWords: List<Pair<String, String>>) {
                onConfigJsonChange(compileCrosswordConfigJson(nextWords, preserveRawWordsArray = true).toString())
            }

            val previewObj = remember(words) {
                compileCrosswordConfigJson(words, preserveRawWordsArray = true)
            }
            val previewGrid = remember(previewObj) {
                val arr = previewObj.optJSONArray("grid")
                buildList {
                    if (arr != null) for (i in 0 until arr.length()) add(arr.optString(i, ""))
                }
            }
            val previewCluesCount = previewObj.optJSONArray("clues")?.length() ?: 0

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(subSurfaceBg)
                    .border(1.dp, violet.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Kata & Petunjuk TTS (Minimal 2 kata A–Z)",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = violet
                    )
                    TextButton(
                        onClick = { saveWords(words + ("" to "")) },
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp), tint = violet)
                        Spacer(Modifier.width(3.dp))
                        Text("Tambah Kata", fontSize = 11.sp, color = violet, fontWeight = FontWeight.Bold)
                    }
                }

                words.forEachIndexed { idx, (ansVal, clueVal) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = ansVal,
                            onValueChange = { v ->
                                val next = words.toMutableList()
                                next[idx] = v.uppercase() to clueVal
                                saveWords(next)
                            },
                            label = { Text("Kata #${idx + 1}", fontSize = 10.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(0.95f),
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = violet,
                                unfocusedBorderColor = subSurfaceBorder,
                                focusedTextColor = textPrimary,
                                unfocusedTextColor = textPrimary,
                            )
                        )
                        OutlinedTextField(
                            value = clueVal,
                            onValueChange = { v ->
                                val next = words.toMutableList()
                                next[idx] = ansVal to v
                                saveWords(next)
                            },
                            label = { Text("Petunjuk / Clue", fontSize = 10.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(1.35f),
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = violet,
                                unfocusedBorderColor = subSurfaceBorder,
                                focusedTextColor = textPrimary,
                                unfocusedTextColor = textPrimary,
                            )
                        )
                        if (words.size > 2) {
                            IconButton(
                                onClick = { saveWords(words.filterIndexed { i, _ -> i != idx }) },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Hapus Kata", tint = danger, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                if (previewGrid.isNotEmpty()) {
                    Text(
                        text = "Pratinjau Papan ($previewCluesCount Kata Terpasang):",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = textSecondary
                    )
                    Column(
                        modifier = Modifier
                            .horizontalScroll(rememberScrollState())
                            .clip(RoundedCornerShape(8.dp))
                            .background(CosmicDark)
                            .border(1.dp, subSurfaceBorder, RoundedCornerShape(8.dp))
                            .padding(6.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        previewGrid.forEach { rowStr ->
                            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                rowStr.forEach { ch ->
                                    if (ch == '#') {
                                        Spacer(modifier = Modifier.size(18.dp))
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(18.dp)
                                                .clip(RoundedCornerShape(3.dp))
                                                .background(MaterialTheme.colorScheme.secondaryContainer)
                                                .border(0.75.dp, violet.copy(alpha = 0.45f), RoundedCornerShape(3.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = ch.toString(),
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
