package com.schoolos.android.feature.assignments

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.schoolos.android.core.designsystem.CosmicBlack
import com.schoolos.android.core.designsystem.CosmicNavy
import com.schoolos.android.core.designsystem.GlassBorder
import com.schoolos.android.core.designsystem.GlassBorder2
import com.schoolos.android.core.designsystem.LocalIsDarkTheme
import com.schoolos.android.core.designsystem.NeonSuccess
import com.schoolos.android.core.designsystem.TextPrimary
import com.schoolos.android.core.designsystem.TextSecondary
import com.schoolos.android.core.designsystem.TextTertiary
import com.schoolos.android.domain.model.AssignmentQuestion
import com.schoolos.android.domain.model.SubmissionAnswer
import org.json.JSONArray
import org.json.JSONObject

fun isInteractiveGameQuestionType(type: String?): Boolean {
    return when (type?.trim()?.uppercase()) {
        "MATCHING", "ORDERING", "FILL_BLANK" -> true
        else -> false
    }
}

fun questionTypeDisplayLabel(type: String?): String {
    return when (type?.trim()?.uppercase()) {
        "MATCHING" -> "Game Menjodohkan"
        "ORDERING" -> "Game Urutkan Langkah"
        "FILL_BLANK" -> "Game Kalimat Rumpang"
        "ESSAY", "URAIAN" -> "Uraian / Esai"
        else -> "Pilihan Ganda"
    }
}

fun questionTypeShortLabel(type: String?): String {
    return when (type?.trim()?.uppercase()) {
        "MATCHING" -> "Jodohkan"
        "ORDERING" -> "Urutan"
        "FILL_BLANK" -> "Rumpang"
        "ESSAY", "URAIAN" -> "Esai"
        else -> "PG"
    }
}

fun questionTypeAccentColor(type: String?): Color {
    return when (type?.trim()?.uppercase()) {
        "MATCHING" -> Color(0xFF0284C7)
        "ORDERING" -> Color(0xFFD97706)
        "FILL_BLANK" -> Color(0xFF059669)
        "ESSAY", "URAIAN" -> Color(0xFF0891B2)
        else -> Color(0xFF10B981)
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
        else -> "{}"
    }
}

fun defaultPromptForGameType(type: String): String {
    return when (type.trim().uppercase()) {
        "MATCHING" -> "Jodohkan setiap konsep di kolom kiri dengan pasangan yang paling tepat!"
        "ORDERING" -> "Urutkan tahapan atau langkah berikut dari yang paling awal hingga akhir!"
        "FILL_BLANK" -> "Lengkapi bagian rumpang pada kalimat berikut menggunakan kata yang tepat!"
        else -> ""
    }
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

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(CosmicNavy)
            .border(
                width = 1.dp,
                color = if (isCompleted) accent.copy(alpha = 0.55f) else GlassBorder,
                shape = RoundedCornerShape(18.dp)
            )
            .padding(14.dp)
    ) {
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
                        Text(
                            text = question.questionText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary,
                            lineHeight = 20.sp
                        )
                    }
                }

                if (isCompleted) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Lengkap",
                        tint = NeonSuccess,
                        modifier = Modifier.size(18.dp)
                    )
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
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
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

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (activeLeftId != null) {
                    "Sekarang ketuk pasangan di bawah untuk mencocokkan"
                } else {
                    "Ketuk item di bawah, lalu ketuk pasangannya"
                },
                fontSize = 11.sp,
                color = if (activeLeftId != null) Color(0xFF38BDF8) else TextTertiary,
                fontWeight = if (activeLeftId != null) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier.weight(1f)
            )
            if (!readOnly && matchesMap.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable {
                            matchesMap.clear()
                            activeLeftId = null
                            emitMatches()
                        }
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.Refresh, null, tint = TextTertiary, modifier = Modifier.size(12.dp))
                    Text("Reset", fontSize = 10.5.sp, color = TextTertiary, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // Left items
        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            leftItems.forEachIndexed { idx, item ->
                val pairColor = MATCH_PAIR_COLORS[idx % MATCH_PAIR_COLORS.size]
                val isSelected = activeLeftId == item.id
                val chosenRight = matchesMap[item.id]
                val hasPair = !chosenRight.isNullOrBlank()

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            when {
                                isSelected -> pairColor.copy(alpha = 0.18f)
                                hasPair -> pairColor.copy(alpha = 0.10f)
                                else -> CosmicBlack
                            }
                        )
                        .border(
                            width = if (isSelected || hasPair) 1.5.dp else 1.dp,
                            color = when {
                                isSelected -> pairColor
                                hasPair -> pairColor.copy(alpha = 0.65f)
                                else -> GlassBorder
                            },
                            shape = RoundedCornerShape(12.dp)
                        )
                        .then(
                            if (!readOnly) Modifier.clickable {
                                activeLeftId = if (isSelected) null else item.id
                            } else Modifier
                        )
                        .padding(11.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(pairColor),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "${idx + 1}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    item.text,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary,
                                    lineHeight = 18.sp
                                )
                            }
                            if (hasPair && !readOnly) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Lepas pasangan",
                                    tint = TextTertiary,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clickable {
                                            matchesMap.remove(item.id)
                                            emitMatches()
                                        }
                                )
                            }
                        }

                        if (hasPair) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(start = 28.dp)
                            ) {
                                Text("→", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = pairColor)
                                Text(
                                    chosenRight!!,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = pairColor,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Right options pool
        if (rightOptions.isNotEmpty() && !readOnly) {
            Text(
                "Pilihan Pasangan:",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                rightOptions.forEach { optionText ->
                    val pairedIdx = leftItems.indexOfFirst { matchesMap[it.id] == optionText }
                    val isPaired = pairedIdx >= 0
                    val pairColor = if (isPaired) {
                        MATCH_PAIR_COLORS[pairedIdx % MATCH_PAIR_COLORS.size]
                    } else {
                        Color(0xFF38BDF8)
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isPaired) pairColor.copy(alpha = 0.15f) else CosmicBlack,
                        border = BorderStroke(
                            width = 1.dp,
                            color = when {
                                isPaired -> pairColor
                                activeLeftId != null -> Color(0xFF38BDF8).copy(alpha = 0.65f)
                                else -> GlassBorder2
                            }
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                val targetLeftId = activeLeftId
                                    ?: leftItems.firstOrNull { matchesMap[it.id].isNullOrBlank() }?.id
                                    ?: leftItems.firstOrNull()?.id
                                if (targetLeftId != null) {
                                    // Unassign previous left owner if same right option clicked
                                    val prevOwner = matchesMap.entries.firstOrNull { it.value == optionText }?.key
                                    if (prevOwner != null && prevOwner != targetLeftId) {
                                        matchesMap.remove(prevOwner)
                                    }
                                    matchesMap[targetLeftId] = optionText
                                    activeLeftId = null
                                    emitMatches()
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (isPaired) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(pairColor),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "${pairedIdx + 1}",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White
                                    )
                                }
                            }
                            Text(
                                text = optionText,
                                fontSize = 12.sp,
                                fontWeight = if (isPaired) FontWeight.Bold else FontWeight.Medium,
                                color = if (isPaired) pairColor else TextPrimary
                            )
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

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Gunakan tombol ▲ dan ▼ untuk menyusun urutan langkah yang benar.",
            fontSize = 11.sp,
            color = TextTertiary
        )

        orderedIds.forEachIndexed { idx, id ->
            val itemText = itemById[id]?.text ?: id
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CosmicBlack)
                    .border(
                        1.dp,
                        if (isSaved) amber.copy(alpha = 0.45f) else GlassBorder,
                        RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(amber.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "${idx + 1}",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = amber
                    )
                }

                Text(
                    text = itemText,
                    fontSize = 12.5.sp,
                    color = TextPrimary,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 17.sp,
                    modifier = Modifier.weight(1f)
                )

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

        if (!readOnly && !isSaved && orderedIds.isNotEmpty()) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = amber.copy(alpha = 0.14f),
                border = BorderStroke(1.dp, amber.copy(alpha = 0.45f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { emitOrder() }
            ) {
                Box(
                    modifier = Modifier.padding(vertical = 9.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Konfirmasi Urutan Ini",
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

    val renderedSentence = remember(cfg.template, fills.toList()) {
        val normTpl = normalizeFillBlankTemplate(cfg.template)
        var idx = 0
        Regex("\\{\\{(\\d+)\\}\\}").replace(normTpl) { m ->
            val slotIdx = (m.groupValues[1].toIntOrNull() ?: (idx + 1)) - 1
            val w = fills.getOrNull(slotIdx)?.takeIf { it.isNotBlank() }
            idx++
            if (w != null) " [ #${slotIdx + 1}: $w ] " else " [ ___ #${slotIdx + 1} ___ ] "
        }
    }

    val emerald = Color(0xFF10B981)

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Sentence Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(CosmicBlack)
                .border(1.dp, GlassBorder2, RoundedCornerShape(12.dp))
                .padding(12.dp)
        ) {
            Text(
                text = renderedSentence.ifBlank { "(Kalimat rumpang belum dikonfigurasi)" },
                fontSize = 13.sp,
                color = TextPrimary,
                lineHeight = 20.sp
            )
        }

        // Blank Slots Row
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            for (slotIdx in 0 until cfg.blankCount) {
                val word = fills.getOrNull(slotIdx).orEmpty()
                val isFilled = word.isNotBlank()
                val isFocused = activeSlot == slotIdx && !readOnly

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = when {
                        isFocused -> emerald.copy(alpha = 0.2f)
                        isFilled -> emerald.copy(alpha = 0.12f)
                        else -> CosmicBlack
                    },
                    border = BorderStroke(
                        width = if (isFocused) 1.5.dp else 1.dp,
                        color = if (isFocused || isFilled) emerald else GlassBorder2
                    ),
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(enabled = !readOnly) {
                            if (isFilled && isFocused) {
                                fills[slotIdx] = ""
                                emitFills()
                            } else {
                                activeSlot = slotIdx
                            }
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            "#${slotIdx + 1}:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = emerald
                        )
                        Text(
                            text = if (isFilled) word else "Ketuk kata...",
                            fontSize = 12.sp,
                            fontWeight = if (isFilled) FontWeight.Bold else FontWeight.Normal,
                            color = if (isFilled) TextPrimary else TextTertiary
                        )
                        if (isFilled && !readOnly) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Hapus isian",
                                tint = TextTertiary,
                                modifier = Modifier
                                    .size(14.dp)
                                    .clickable {
                                        fills[slotIdx] = ""
                                        activeSlot = slotIdx
                                        emitFills()
                                    }
                            )
                        }
                    }
                }
            }
        }

        // Word Bank Pills
        if (cfg.wordBank.isNotEmpty() && !readOnly) {
            Text(
                "Bank Kata (ketuk untuk mengisi slot #${activeSlot + 1}):",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                cfg.wordBank.forEach { word ->
                    val usedInSlot = fills.indexOfFirst { it == word }
                    val isUsed = usedInSlot >= 0

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isUsed) emerald.copy(alpha = 0.14f) else CosmicBlack,
                        border = BorderStroke(1.dp, if (isUsed) emerald.copy(alpha = 0.6f) else GlassBorder2),
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
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
                        Text(
                            text = if (isUsed) "$word (#${usedInSlot + 1})" else word,
                            fontSize = 12.sp,
                            fontWeight = if (isUsed) FontWeight.Bold else FontWeight.Medium,
                            color = if (isUsed) emerald else TextPrimary,
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp)
                        )
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
                    Text(
                        text = question.questionText,
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
    }
}
