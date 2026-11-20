package com.schoolos.android.feature.quizzes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.schoolos.android.domain.model.QuizQuestion
import org.json.JSONArray
import org.json.JSONObject

internal enum class AndroidVariatifQuestionType {
    MULTIPLE_CHOICE,
    TRUE_FALSE_MATRIX,
    MULTIPLE_SELECT,
    SHORT_ANSWER,
    MATCHING,
    ORDERING,
    ESSAY,
}

internal fun normalizeAndroidQuestionType(raw: String?): AndroidVariatifQuestionType {
    return when (raw.orEmpty().trim().uppercase()) {
        "TRUE_FALSE_MATRIX", "TRUE_FALSE", "BENAR_SALAH", "MATRIX_TF" ->
            AndroidVariatifQuestionType.TRUE_FALSE_MATRIX
        "MULTIPLE_SELECT", "MULTI_SELECT", "PG_KOMPLEKS", "CHECKBOX" ->
            AndroidVariatifQuestionType.MULTIPLE_SELECT
        "SHORT_ANSWER", "ISIAN_SINGKAT", "FILL_BLANK" ->
            AndroidVariatifQuestionType.SHORT_ANSWER
        "MATCHING", "MENJODOHKAN" ->
            AndroidVariatifQuestionType.MATCHING
        "ORDERING", "URUTAN", "SEQUENCE" ->
            AndroidVariatifQuestionType.ORDERING
        "ESSAY", "URAIAN" ->
            AndroidVariatifQuestionType.ESSAY
        else -> AndroidVariatifQuestionType.MULTIPLE_CHOICE
    }
}

internal fun androidQuestionTypeLabel(type: AndroidVariatifQuestionType): String = when (type) {
    AndroidVariatifQuestionType.MULTIPLE_CHOICE -> "Pilihan Ganda"
    AndroidVariatifQuestionType.TRUE_FALSE_MATRIX -> "Benar / Salah"
    AndroidVariatifQuestionType.MULTIPLE_SELECT -> "PG Kompleks"
    AndroidVariatifQuestionType.SHORT_ANSWER -> "Isian Singkat"
    AndroidVariatifQuestionType.MATCHING -> "Menjodohkan"
    AndroidVariatifQuestionType.ORDERING -> "Urutkan"
    AndroidVariatifQuestionType.ESSAY -> "Uraian / Essay"
}

private fun parseJsonObjectSafe(raw: String?): JSONObject? {
    if (raw.isNullOrBlank()) return null
    return try {
        JSONObject(raw)
    } catch (_: Exception) {
        null
    }
}

internal fun parseTrueFalseMatrixAnswer(raw: String?): Map<String, Boolean> {
    val root = parseJsonObjectSafe(raw) ?: return emptyMap()
    val rows = root.optJSONObject("rows") ?: root
    val out = mutableMapOf<String, Boolean>()
    val keys = rows.keys()
    while (keys.hasNext()) {
        val k = keys.next()
        when (val v = rows.opt(k)) {
            is Boolean -> out[k] = v
            is String -> when (v.lowercase()) {
                "true", "benar" -> out[k] = true
                "false", "salah" -> out[k] = false
            }
        }
    }
    return out
}

internal fun serializeTrueFalseMatrixAnswer(rows: Map<String, Boolean>): String {
    val obj = JSONObject()
    obj.put("format", "TRUE_FALSE_MATRIX")
    val rowsObj = JSONObject()
    rows.forEach { (k, v) -> rowsObj.put(k, v) }
    obj.put("rows", rowsObj)
    return obj.toString()
}

internal fun parseMultipleSelectAnswer(raw: String?): List<String> {
    if (raw.isNullOrBlank()) return emptyList()
    return try {
        val arr = if (raw.trim().startsWith("[")) {
            JSONArray(raw)
        } else {
            JSONObject(raw).optJSONArray("choice_ids") ?: JSONArray()
        }
        (0 until arr.length()).mapNotNull { idx ->
            arr.optString(idx).takeIf { it.isNotBlank() }
        }.distinct()
    } catch (_: Exception) {
        emptyList()
    }
}

internal fun serializeMultipleSelectAnswer(ids: List<String>): String {
    val obj = JSONObject()
    obj.put("format", "MULTIPLE_SELECT")
    obj.put("choice_ids", JSONArray(ids.distinct()))
    return obj.toString()
}

internal data class MatchingUiData(
    val leftItems: List<Pair<String, String>>,
    val rightOptions: List<String>,
)

internal fun extractMatchingUiData(question: QuizQuestion): MatchingUiData {
    val cfg = parseJsonObjectSafe(question.interactionConfigJson) ?: return MatchingUiData(emptyList(), emptyList())
    val leftArr = cfg.optJSONArray("left_items")
    val rightArr = cfg.optJSONArray("right_options")
    if (leftArr != null && rightArr != null) {
        val left = (0 until leftArr.length()).mapNotNull { i ->
            val obj = leftArr.optJSONObject(i) ?: return@mapNotNull null
            val id = obj.optString("id").ifBlank { "pair-${i + 1}" }
            val text = obj.optString("text").trim()
            if (text.isEmpty()) null else id to text
        }
        val rights = (0 until rightArr.length()).mapNotNull { i ->
            rightArr.optString(i).trim().takeIf { it.isNotEmpty() }
        }
        return MatchingUiData(left, rights)
    }

    val pairsArr = cfg.optJSONArray("pairs") ?: return MatchingUiData(emptyList(), emptyList())
    val left = mutableListOf<Pair<String, String>>()
    val rights = mutableListOf<String>()
    for (i in 0 until pairsArr.length()) {
        val obj = pairsArr.optJSONObject(i) ?: continue
        val id = obj.optString("id").ifBlank { "pair-${i + 1}" }
        val l = obj.optString("left").trim()
        val r = obj.optString("right").trim()
        if (l.isNotEmpty()) left.add(id to l)
        if (r.isNotEmpty()) rights.add(r)
    }
    return MatchingUiData(left, rights.distinct().sorted())
}

internal fun parseMatchingAnswer(raw: String?): Map<String, String> {
    val root = parseJsonObjectSafe(raw) ?: return emptyMap()
    val pairs = root.optJSONObject("pairs") ?: root
    val out = mutableMapOf<String, String>()
    val keys = pairs.keys()
    while (keys.hasNext()) {
        val k = keys.next()
        val v = pairs.optString(k).trim()
        if (v.isNotEmpty()) out[k] = v
    }
    return out
}

internal fun serializeMatchingAnswer(pairs: Map<String, String>): String {
    val obj = JSONObject()
    obj.put("format", "MATCHING")
    val mapObj = JSONObject()
    pairs.forEach { (k, v) -> mapObj.put(k, v) }
    obj.put("pairs", mapObj)
    return obj.toString()
}

internal fun extractOrderingItems(question: QuizQuestion): List<Pair<String, String>> {
    val cfg = parseJsonObjectSafe(question.interactionConfigJson) ?: return emptyList()
    val itemsArr = cfg.optJSONArray("items") ?: return emptyList()
    return (0 until itemsArr.length()).mapNotNull { idx ->
        val obj = itemsArr.optJSONObject(idx) ?: return@mapNotNull null
        val id = obj.optString("id").ifBlank { "step-${idx + 1}" }
        val text = obj.optString("text").trim()
        if (text.isEmpty()) null else id to text
    }
}

internal fun parseOrderingAnswer(raw: String?): List<String> {
    if (raw.isNullOrBlank()) return emptyList()
    return try {
        val arr = if (raw.trim().startsWith("[")) {
            JSONArray(raw)
        } else {
            JSONObject(raw).optJSONArray("order") ?: JSONArray()
        }
        (0 until arr.length()).mapNotNull { i ->
            arr.optString(i).takeIf { it.isNotBlank() }
        }
    } catch (_: Exception) {
        emptyList()
    }
}

internal fun serializeOrderingAnswer(order: List<String>): String {
    val obj = JSONObject()
    obj.put("format", "ORDERING")
    obj.put("order", JSONArray(order))
    return obj.toString()
}

internal fun isAndroidQuestionAnswerComplete(question: QuizQuestion, rawAnswer: String?): Boolean {
    if (rawAnswer.isNullOrBlank()) return false
    return when (normalizeAndroidQuestionType(question.questionType)) {
        AndroidVariatifQuestionType.MULTIPLE_CHOICE -> true
        AndroidVariatifQuestionType.TRUE_FALSE_MATRIX -> {
            val rows = parseTrueFalseMatrixAnswer(rawAnswer)
            question.choices.isNotEmpty() && rows.size >= question.choices.size
        }
        AndroidVariatifQuestionType.MULTIPLE_SELECT ->
            parseMultipleSelectAnswer(rawAnswer).isNotEmpty()
        AndroidVariatifQuestionType.MATCHING -> {
            val data = extractMatchingUiData(question)
            val pairs = parseMatchingAnswer(rawAnswer)
            data.leftItems.isNotEmpty() && data.leftItems.all { (id, _) -> !pairs[id].isNullOrBlank() }
        }
        AndroidVariatifQuestionType.ORDERING ->
            parseOrderingAnswer(rawAnswer).isNotEmpty()
        AndroidVariatifQuestionType.SHORT_ANSWER, AndroidVariatifQuestionType.ESSAY ->
            rawAnswer.isNotBlank()
    }
}

@Composable
internal fun TrueFalseMatrixSection(
    question: QuizQuestion,
    rawAnswer: String?,
    onAnswerChange: (String) -> Unit,
    isDark: Boolean,
    cardBg: Color,
    cardBorder: Color,
    innerCardBg: Color,
    textPrimary: Color,
    textSecondary: Color,
    studentAccent: Color,
) {
    val cfg = remember(question.interactionConfigJson) {
        parseJsonObjectSafe(question.interactionConfigJson)
    }
    val statementLabel = cfg?.optString("statement_label")?.takeIf { it.isNotBlank() } ?: "Pernyataan"
    val trueLabel = cfg?.optString("true_label")?.takeIf { it.isNotBlank() } ?: "True"
    val falseLabel = cfg?.optString("false_label")?.takeIf { it.isNotBlank() } ?: "False"
    val rows = remember(rawAnswer) { parseTrueFalseMatrixAnswer(rawAnswer) }
    val filledCount = question.choices.count { c -> rows.containsKey(c.id) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Pilih $trueLabel atau $falseLabel pada setiap baris:",
                fontSize = 12.sp,
                color = textSecondary,
            )
            Text(
                text = "$filledCount / ${question.choices.size} terisi",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (filledCount == question.choices.size) Color(0xFF10B981) else studentAccent,
            )
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = cardBg,
            border = BorderStroke(1.dp, cardBorder),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Table Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min)
                        .background(innerCardBg),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "#",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .width(40.dp)
                            .padding(vertical = 11.dp),
                    )
                    VerticalDivider(modifier = Modifier.fillMaxHeight(), color = cardBorder)
                    Text(
                        text = statementLabel,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp, vertical = 11.dp),
                    )
                    VerticalDivider(modifier = Modifier.fillMaxHeight(), color = cardBorder)
                    Text(
                        text = trueLabel,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .width(72.dp)
                            .padding(vertical = 11.dp),
                    )
                    VerticalDivider(modifier = Modifier.fillMaxHeight(), color = cardBorder)
                    Text(
                        text = falseLabel,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .width(72.dp)
                            .padding(vertical = 11.dp),
                    )
                }

                // Table Rows
                question.choices.forEachIndexed { idx, rowChoice ->
                    val currentPick: Boolean? = rows[rowChoice.id]
                    HorizontalDivider(color = cardBorder)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "${idx + 1}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .width(40.dp)
                                .padding(vertical = 12.dp),
                        )
                        VerticalDivider(modifier = Modifier.fillMaxHeight(), color = cardBorder)
                        Text(
                            text = rowChoice.choiceText,
                            fontSize = 13.5.sp,
                            lineHeight = 19.sp,
                            color = textPrimary,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                        )
                        VerticalDivider(modifier = Modifier.fillMaxHeight(), color = cardBorder)
                        Box(
                            modifier = Modifier
                                .width(72.dp)
                                .fillMaxHeight()
                                .background(
                                    if (currentPick == true) studentAccent.copy(alpha = if (isDark) 0.14f else 0.08f)
                                    else Color.Transparent
                                )
                                .clickable {
                                    val next = rows.toMutableMap()
                                    next[rowChoice.id] = true
                                    onAnswerChange(serializeTrueFalseMatrixAnswer(next))
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            RadioButton(
                                selected = currentPick == true,
                                onClick = {
                                    val next = rows.toMutableMap()
                                    next[rowChoice.id] = true
                                    onAnswerChange(serializeTrueFalseMatrixAnswer(next))
                                },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = studentAccent,
                                    unselectedColor = textSecondary,
                                ),
                            )
                        }
                        VerticalDivider(modifier = Modifier.fillMaxHeight(), color = cardBorder)
                        Box(
                            modifier = Modifier
                                .width(72.dp)
                                .fillMaxHeight()
                                .background(
                                    if (currentPick == false) studentAccent.copy(alpha = if (isDark) 0.14f else 0.08f)
                                    else Color.Transparent
                                )
                                .clickable {
                                    val next = rows.toMutableMap()
                                    next[rowChoice.id] = false
                                    onAnswerChange(serializeTrueFalseMatrixAnswer(next))
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            RadioButton(
                                selected = currentPick == false,
                                onClick = {
                                    val next = rows.toMutableMap()
                                    next[rowChoice.id] = false
                                    onAnswerChange(serializeTrueFalseMatrixAnswer(next))
                                },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = studentAccent,
                                    unselectedColor = textSecondary,
                                ),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun MultipleSelectSection(
    question: QuizQuestion,
    rawAnswer: String?,
    onAnswerChange: (String?) -> Unit,
    isDark: Boolean,
    cardBg: Color,
    cardBorder: Color,
    textPrimary: Color,
    textSecondary: Color,
) {
    val selectedIds = remember(rawAnswer) { parseMultipleSelectAnswer(rawAnswer) }
    val emerald = if (isDark) Color(0xFF34D399) else Color(0xFF10B981)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Pilih lebih dari satu jawaban yang benar (${selectedIds.size} dipilih):",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = textSecondary,
        )

        question.choices.forEach { choice ->
            val checked = selectedIds.contains(choice.id)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val next = if (checked) selectedIds - choice.id else selectedIds + choice.id
                        onAnswerChange(if (next.isEmpty()) null else serializeMultipleSelectAnswer(next))
                    },
                shape = RoundedCornerShape(14.dp),
                color = if (checked) emerald.copy(alpha = if (isDark) 0.16f else 0.1f) else cardBg,
                border = BorderStroke(if (checked) 1.5.dp else 1.dp, if (checked) emerald else cardBorder),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = checked,
                        onCheckedChange = {
                            val next = if (checked) selectedIds - choice.id else selectedIds + choice.id
                            onAnswerChange(if (next.isEmpty()) null else serializeMultipleSelectAnswer(next))
                        },
                        colors = CheckboxDefaults.colors(
                            checkedColor = emerald,
                            uncheckedColor = textSecondary,
                        ),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = choice.choiceText,
                        fontSize = 14.sp,
                        color = textPrimary,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
internal fun ShortAnswerSection(
    question: QuizQuestion,
    rawAnswer: String?,
    onAnswerChange: (String) -> Unit,
    cardBg: Color,
    cardBorder: Color,
    textPrimary: Color,
    textTertiary: Color,
    studentAccent: Color,
) {
    val cfg = remember(question.interactionConfigJson) {
        parseJsonObjectSafe(question.interactionConfigJson)
    }
    val placeholder = cfg?.optString("placeholder")?.takeIf { it.isNotBlank() }
        ?: "Ketik jawaban singkat yang tepat..."

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Isian Singkat:",
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Bold,
            color = textPrimary,
        )
        OutlinedTextField(
            value = rawAnswer ?: "",
            onValueChange = onAnswerChange,
            placeholder = { Text(placeholder, color = textTertiary, fontSize = 13.5.sp) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = cardBg,
                unfocusedContainerColor = cardBg,
                focusedBorderColor = studentAccent,
                unfocusedBorderColor = cardBorder,
                focusedTextColor = textPrimary,
                unfocusedTextColor = textPrimary,
            ),
        )
    }
}

@Composable
internal fun MatchingSection(
    question: QuizQuestion,
    rawAnswer: String?,
    onAnswerChange: (String?) -> Unit,
    isDark: Boolean,
    cardBg: Color,
    cardBorder: Color,
    innerCardBg: Color,
    textPrimary: Color,
    textSecondary: Color,
    studentAccent: Color,
) {
    val uiData = remember(question.id, question.interactionConfigJson) { extractMatchingUiData(question) }
    val currentPairs = remember(rawAnswer) { parseMatchingAnswer(rawAnswer) }
    val purple = if (isDark) Color(0xFFA78BFA) else Color(0xFF7C3AED)

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Pasangkan item sebelah atas/kiri dengan opsi pasangannya:",
            fontSize = 12.sp,
            color = textSecondary,
        )

        uiData.leftItems.forEachIndexed { idx, (leftId, leftText) ->
            var menuOpen by remember { mutableStateOf(false) }
            val picked = currentPairs[leftId] ?: ""

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = if (picked.isNotEmpty()) purple.copy(alpha = if (isDark) 0.14f else 0.08f) else cardBg,
                border = BorderStroke(1.dp, if (picked.isNotEmpty()) purple else cardBorder),
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "${idx + 1}. $leftText",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textPrimary,
                    )

                    Box {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, cardBorder, RoundedCornerShape(10.dp))
                                .background(innerCardBg, RoundedCornerShape(10.dp))
                                .clickable { menuOpen = true }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = picked.ifBlank { "-- Pilih Pasangan --" },
                                fontSize = 13.sp,
                                fontWeight = if (picked.isNotBlank()) FontWeight.Bold else FontWeight.Normal,
                                color = if (picked.isNotBlank()) studentAccent else textSecondary,
                                modifier = Modifier.weight(1f),
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = textSecondary,
                            )
                        }

                        DropdownMenu(
                            expanded = menuOpen,
                            onDismissRequest = { menuOpen = false },
                        ) {
                            uiData.rightOptions.forEach { opt ->
                                DropdownMenuItem(
                                    text = { Text(opt, fontSize = 13.sp) },
                                    onClick = {
                                        menuOpen = false
                                        val next = currentPairs.toMutableMap()
                                        next[leftId] = opt
                                        onAnswerChange(serializeMatchingAnswer(next))
                                    },
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
internal fun OrderingSection(
    question: QuizQuestion,
    rawAnswer: String?,
    onAnswerChange: (String) -> Unit,
    isDark: Boolean,
    cardBg: Color,
    cardBorder: Color,
    textPrimary: Color,
    textSecondary: Color,
) {
    val allItems = remember(question.id, question.interactionConfigJson) { extractOrderingItems(question) }
    val itemMap = remember(allItems) { allItems.associate { it.first to it.second } }
    val savedOrder = remember(rawAnswer) { parseOrderingAnswer(rawAnswer) }
    val hasConfirmed = savedOrder.isNotEmpty()
    val orderedIds = remember(savedOrder, allItems) {
        if (savedOrder.size == allItems.size) savedOrder else allItems.map { it.first }
    }
    val rose = if (isDark) Color(0xFFF472B6) else Color(0xFFDB2777)

    fun moveItem(from: Int, delta: Int) {
        val to = from + delta
        if (to !in orderedIds.indices) return
        val mutable = orderedIds.toMutableList()
        val item = mutable.removeAt(from)
        mutable.add(to, item)
        onAnswerChange(serializeOrderingAnswer(mutable))
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Susun urutan dari awal hingga akhir:",
                fontSize = 12.sp,
                color = textSecondary,
            )
            if (!hasConfirmed && orderedIds.isNotEmpty()) {
                TextButton(onClick = { onAnswerChange(serializeOrderingAnswer(orderedIds)) }) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp), tint = rose)
                    Spacer(Modifier.width(4.dp))
                    Text("Konfirmasi", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = rose)
                }
            }
        }

        orderedIds.forEachIndexed { idx, id ->
            val label = itemMap[id] ?: id
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = cardBg,
                border = BorderStroke(1.dp, if (hasConfirmed) rose.copy(alpha = 0.6f) else cardBorder),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.DragIndicator,
                        contentDescription = null,
                        tint = textSecondary,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .background(if (hasConfirmed) rose else cardBorder, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "${idx + 1}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (hasConfirmed) Color.White else textPrimary,
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = label,
                        fontSize = 13.5.sp,
                        color = textPrimary,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { moveItem(idx, -1) }, enabled = idx > 0) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowUp,
                            contentDescription = "Naikkan",
                            tint = if (idx > 0) textPrimary else textSecondary.copy(alpha = 0.3f),
                        )
                    }
                    IconButton(onClick = { moveItem(idx, 1) }, enabled = idx < orderedIds.lastIndex) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Turunkan",
                            tint = if (idx < orderedIds.lastIndex) textPrimary else textSecondary.copy(alpha = 0.3f),
                        )
                    }
                }
            }
        }
    }
}
