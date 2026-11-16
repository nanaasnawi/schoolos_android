package com.schoolos.android.feature.quizzes.dailyquest

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.schoolos.android.domain.model.DailyQuestChallenge
import kotlinx.coroutines.delay

// ═════════════════════════════════════════════════════════════════════════════
// 5. KUIS KILAT ARENA (KAHOOT-STYLE BLITZ)
// ═════════════════════════════════════════════════════════════════════════════

private data class KahootTileStyle(
    val top: Color,
    val bottom: Color,
    val lip: Color,
    val shape: String,
)

private val KAHOOT_TILE_STYLES = listOf(
    KahootTileStyle(Color(0xFFFB7185), Color(0xFFE11D48), Color(0xFF9F1239), "▲"),
    KahootTileStyle(Color(0xFF38BDF8), Color(0xFF0284C7), Color(0xFF0369A1), "◆"),
    KahootTileStyle(Color(0xFFFBBF24), Color(0xFFD97706), Color(0xFFB45309), "●"),
    KahootTileStyle(Color(0xFF34D399), Color(0xFF059669), Color(0xFF047857), "■"),
)

@Composable
internal fun KahootBlitzGameCompose(
    challenge: DailyQuestChallenge.KahootBlitz,
    disabled: Boolean,
    surfaceColor: Color,
    elevatedColor: Color,
    borderColor: Color,
    textPrimary: Color,
    textSecondary: Color,
    onPlayTap: () -> Unit,
    onPlayGood: () -> Unit,
    onPlayError: () -> Unit,
    onFinish: (correct: Int, total: Int, maxCombo: Int) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val questions = challenge.questions
    val timeLimit = challenge.timePerQuestion.coerceAtLeast(8)

    var qIdx by remember(challenge.id) { mutableIntStateOf(0) }
    var timeLeft by remember(challenge.id) { mutableIntStateOf(timeLimit) }
    var selectedOpt by remember(challenge.id) { mutableStateOf<Int?>(null) }
    var timedOut by remember(challenge.id) { mutableStateOf(false) }
    var score by remember(challenge.id) { mutableIntStateOf(0) }
    var correctCount by remember(challenge.id) { mutableIntStateOf(0) }
    var streak by remember(challenge.id) { mutableIntStateOf(0) }
    var maxStreak by remember(challenge.id) { mutableIntStateOf(1) }
    var gainedPoints by remember(challenge.id) { mutableIntStateOf(0) }
    var doneSent by remember(challenge.id) { mutableStateOf(false) }

    val q = questions.getOrNull(qIdx) ?: return
    val answered = selectedOpt != null || timedOut

    LaunchedEffect(challenge.id, qIdx, answered, disabled) {
        if (answered || disabled) return@LaunchedEffect
        while (timeLeft > 0) {
            delay(1_000L)
            timeLeft -= 1
        }
        timedOut = true
        streak = 0
        gainedPoints = 0
        onPlayError()
    }

    val timerRatio = (timeLeft.toFloat() / timeLimit.toFloat()).coerceIn(0f, 1f)
    val isCorrectChoice = selectedOpt == q.correctIndex

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Arena HUD + Timer Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = surfaceColor,
            border = BorderStroke(1.5.dp, borderColor),
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Brush.horizontalGradient(listOf(Color(0xFF8B5CF6), Color(0xFFEC4899))))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = "SOAL ${qIdx + 1}/${questions.size} • ${q.topicTag}",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            maxLines = 1,
                            softWrap = false,
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (streak >= 2) {
                            Surface(shape = RoundedCornerShape(50), color = Color(0xFFF59E0B).copy(alpha = 0.16f)) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                ) {
                                    Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(12.dp))
                                    Text("×$streak", color = Color(0xFFF59E0B), fontSize = 11.sp, fontWeight = FontWeight.Black)
                                }
                            }
                        }
                        Text("$score pts", color = Color(0xFF0EA5E9), fontSize = 14.sp, fontWeight = FontWeight.Black)
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Timer, contentDescription = null, tint = if (timeLeft <= 5) Color(0xFFF43F5E) else Color(0xFF8B5CF6), modifier = Modifier.size(14.dp))
                    Text("${timeLeft}s", color = if (timeLeft <= 5) Color(0xFFF43F5E) else Color(0xFF8B5CF6), fontSize = 12.sp, fontWeight = FontWeight.Black)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(10.dp)
                            .clip(CircleShape)
                            .background(elevatedColor),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(timerRatio.coerceAtLeast(0.03f))
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(
                                    Brush.horizontalGradient(
                                        if (timeLeft <= 5) listOf(Color(0xFFF43F5E), Color(0xFFEF4444))
                                        else listOf(Color(0xFFA855F7), Color(0xFFEC4899))
                                    )
                                ),
                        )
                    }
                }
            }
        }

        // Question Banner
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = surfaceColor,
            border = BorderStroke(1.5.dp, borderColor),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.Bolt, contentDescription = null, tint = Color(0xFF8B5CF6), modifier = Modifier.size(14.dp))
                    Text("ARENA KUIS KILAT", color = Color(0xFF8B5CF6), fontSize = 10.sp, fontWeight = FontWeight.Black)
                }
                Text(
                    text = q.question,
                    color = textPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp,
                )
            }
        }

        // 2x2 Signature Kahoot Shape Option Grid
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            for (rowIdx in 0..1) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    for (colIdx in 0..1) {
                        val optIdx = rowIdx * 2 + colIdx
                        val optText = q.options.getOrNull(optIdx)
                        if (optText != null) {
                            val tile = KAHOOT_TILE_STYLES[optIdx % 4]
                            val isCorrectOpt = optIdx == q.correctIndex
                            val isSelOpt = optIdx == selectedOpt
                            val dimmed = answered && !isCorrectOpt && !isSelOpt

                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = tile.lip,
                                modifier = Modifier
                                    .weight(1f)
                                    .alpha(if (dimmed) 0.42f else 1f)
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable(enabled = !answered && !disabled) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        selectedOpt = optIdx
                                        if (isCorrectOpt) {
                                            onPlayGood()
                                            val ns = streak + 1
                                            val pts = 500 + timeLeft * 35 + (ns - 1) * 100
                                            score += pts
                                            correctCount += 1
                                            streak = ns
                                            maxStreak = maxOf(maxStreak, ns)
                                            gainedPoints = pts
                                        } else {
                                            onPlayError()
                                            streak = 0
                                            gainedPoints = 0
                                        }
                                    },
                            ) {
                                Column(
                                    modifier = Modifier
                                        .padding(bottom = 5.dp)
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Brush.verticalGradient(listOf(tile.top, tile.bottom)))
                                        .border(
                                            width = if (answered && isCorrectOpt) 2.5.dp else 1.dp,
                                            color = if (answered && isCorrectOpt) Color.White else Color.White.copy(alpha = 0.35f),
                                            shape = RoundedCornerShape(16.dp),
                                        )
                                        .padding(horizontal = 10.dp, vertical = 16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Text(tile.shape, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
                                    Text(
                                        text = optText,
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Black,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 16.sp,
                                    )
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        if (answered) {
            val fColor = if (isCorrectChoice) Color(0xFF10B981) else Color(0xFFF43F5E)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = fColor.copy(alpha = 0.14f),
                border = BorderStroke(1.5.dp, fColor.copy(alpha = 0.45f)),
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(
                            imageVector = if (isCorrectChoice) Icons.Default.CheckCircle else Icons.Default.Close,
                            contentDescription = null,
                            tint = fColor,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = when {
                                timedOut -> "Waktu Habis!"
                                isCorrectChoice -> "Tepat Sekali! +$gainedPoints Poin"
                                else -> "Kurang Tepat!"
                            },
                            color = fColor,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                        )
                    }
                    q.explanation?.let { exp ->
                        Text(exp, color = textPrimary, fontSize = 12.sp, lineHeight = 17.sp)
                    }
                    Surface(
                        shape = RoundedCornerShape(13.dp),
                        color = Color(0xFF0369A1),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(13.dp))
                            .clickable {
                                onPlayTap()
                                if (qIdx + 1 < questions.size) {
                                    qIdx += 1
                                    timeLeft = timeLimit
                                    selectedOpt = null
                                    timedOut = false
                                    gainedPoints = 0
                                } else if (!doneSent) {
                                    doneSent = true
                                    onFinish(correctCount, questions.size, maxStreak)
                                }
                            },
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(bottom = 4.dp)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(13.dp))
                                .background(Brush.verticalGradient(listOf(Color(0xFF38BDF8), Color(0xFF0284C7))))
                                .padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = if (qIdx + 1 < questions.size) "Soal Berikutnya" else "Selesai & Klaim +XP",
                                color = Color.White,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Black,
                            )
                            Spacer(Modifier.width(5.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                        }
                    }
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// 6. SUSUN & TEBAK KATA (BAHASA INDONESIA & ENGLISH)
// ═════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun WordScrambleGameCompose(
    challenge: DailyQuestChallenge.WordScramble,
    disabled: Boolean,
    surfaceColor: Color,
    elevatedColor: Color,
    borderColor: Color,
    textPrimary: Color,
    textSecondary: Color,
    onPlayTap: () -> Unit,
    onPlayGood: () -> Unit,
    onPlayError: () -> Unit,
    onFinish: (correct: Int, total: Int, maxCombo: Int) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val items = challenge.items
    var idx by remember(challenge.id) { mutableIntStateOf(0) }
    var availableLetters by remember(challenge.id) { mutableStateOf(items.firstOrNull()?.scrambledLetters.orEmpty()) }
    var usedPoolIndices by remember(challenge.id) { mutableStateOf<List<Int>>(emptyList()) }
    var status by remember(challenge.id) { mutableStateOf("PLAYING") } // PLAYING, CORRECT, WRONG
    var correctCount by remember(challenge.id) { mutableIntStateOf(0) }
    var streak by remember(challenge.id) { mutableIntStateOf(0) }
    var maxStreak by remember(challenge.id) { mutableIntStateOf(1) }
    var doneSent by remember(challenge.id) { mutableStateOf(false) }

    val item = items.getOrNull(idx) ?: return
    val targetLen = item.word.length

    fun handleTapPool(poolIdx: Int) {
        if (status == "CORRECT" || disabled) return
        if (poolIdx in usedPoolIndices || usedPoolIndices.size >= targetLen) return
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        onPlayTap()
        val next = usedPoolIndices + poolIdx
        usedPoolIndices = next
        status = "PLAYING"

        if (next.size == targetLen) {
            val candidate = next.joinToString("") { availableLetters[it] }
            if (candidate == item.word) {
                onPlayGood()
                val ns = streak + 1
                streak = ns
                maxStreak = maxOf(maxStreak, ns)
                correctCount += 1
                status = "CORRECT"
            } else {
                onPlayError()
                streak = 0
                status = "WRONG"
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = surfaceColor,
            border = BorderStroke(1.5.dp, borderColor),
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val isEn = item.language == "EN"
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(
                            Brush.horizontalGradient(
                                if (isEn) listOf(Color(0xFF0EA5E9), Color(0xFF2563EB))
                                else listOf(Color(0xFFF43F5E), Color(0xFFE11D48))
                            )
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = "${item.category} • ${idx + 1}/${items.size}",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        softWrap = false,
                    )
                }
                Text("Terjawab: $correctCount/${items.size}", color = Color(0xFF10B981), fontSize = 12.sp, fontWeight = FontWeight.Black)
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = surfaceColor,
            border = BorderStroke(1.5.dp, borderColor),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("PETUNJUK KOSAKATA ($targetLen HURUF)", color = Color(0xFFF59E0B), fontSize = 10.5.sp, fontWeight = FontWeight.Black)
                    Text(item.clue, color = textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                }

                // Target 3D Tile Slots
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    for (slotIdx in 0 until targetLen) {
                        val pIdx = usedPoolIndices.getOrNull(slotIdx)
                        val ch = if (pIdx != null) availableLetters.getOrNull(pIdx).orEmpty() else ""
                        val lip = when {
                            status == "CORRECT" -> Color(0xFF047857)
                            status == "WRONG" -> Color(0xFFBE123C)
                            ch.isNotEmpty() -> Color(0xFFD97706)
                            else -> borderColor
                        }
                        val face = when {
                            status == "CORRECT" -> Color(0xFF10B981)
                            status == "WRONG" -> Color(0xFFF43F5E)
                            ch.isNotEmpty() -> Color(0xFFFEF3C7)
                            else -> elevatedColor
                        }
                        Box(
                            modifier = Modifier
                                .size(width = 40.dp, height = 46.dp)
                                .clip(RoundedCornerShape(11.dp))
                                .background(lip)
                                .padding(bottom = if (ch.isNotEmpty()) 3.5.dp else 0.dp)
                                .clip(RoundedCornerShape(11.dp))
                                .background(face)
                                .border(1.dp, if (ch.isEmpty()) borderColor else Color.White.copy(alpha = 0.5f), RoundedCornerShape(11.dp))
                                .clickable(enabled = ch.isNotEmpty() && status != "CORRECT") {
                                    usedPoolIndices = usedPoolIndices.filterIndexed { i, _ -> i != slotIdx }
                                    status = "PLAYING"
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = ch,
                                color = if (status == "CORRECT" || status == "WRONG") Color.White else if (ch.isNotEmpty()) Color(0xFF78350F) else textSecondary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                            )
                        }
                    }
                }

                if (status == "WRONG") {
                    Text(
                        text = "Susunan belum tepat! Ketuk huruf di atas untuk melepas.",
                        color = Color(0xFFF43F5E),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }

                // Scrambled Letter Pool
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    availableLetters.forEachIndexed { pIdx, ch ->
                        val used = pIdx in usedPoolIndices
                        Box(
                            modifier = Modifier
                                .size(width = 44.dp, height = 48.dp)
                                .alpha(if (used) 0.3f else 1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (used) borderColor else Color(0xFF0369A1))
                                .padding(bottom = if (used) 0.dp else 4.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    Brush.verticalGradient(
                                        if (used) listOf(elevatedColor, elevatedColor)
                                        else listOf(Color(0xFF38BDF8), Color(0xFF0284C7))
                                    )
                                )
                                .clickable(enabled = !used && status != "CORRECT") {
                                    handleTapPool(pIdx)
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(if (used) "" else ch, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }

                if (status != "CORRECT") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFF59E0B).copy(alpha = 0.16f),
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    val nextPos = usedPoolIndices.size
                                    if (nextPos < targetLen) {
                                        val need = item.word[nextPos].toString()
                                        val matchIdx = availableLetters.indices.firstOrNull { pIdx ->
                                            availableLetters[pIdx] == need && pIdx !in usedPoolIndices
                                        }
                                        if (matchIdx != null) handleTapPool(matchIdx)
                                    }
                                },
                        ) {
                            Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Lightbulb, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(13.dp))
                                Text(" Huruf", color = Color(0xFFF59E0B), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = elevatedColor,
                            border = BorderStroke(1.dp, borderColor),
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    availableLetters = availableLetters.shuffled()
                                    usedPoolIndices = emptyList()
                                    status = "PLAYING"
                                },
                        ) {
                            Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Shuffle, contentDescription = null, tint = textPrimary, modifier = Modifier.size(13.dp))
                                Text(" Acak", color = textPrimary, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = elevatedColor,
                            border = BorderStroke(1.dp, borderColor),
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    usedPoolIndices = emptyList()
                                    status = "PLAYING"
                                },
                        ) {
                            Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Refresh, contentDescription = null, tint = textSecondary, modifier = Modifier.size(13.dp))
                                Text(" Reset", color = textPrimary, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                }
            }
        }

        if (status == "CORRECT") {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = Color(0xFF10B981).copy(alpha = 0.14f),
                border = BorderStroke(1.5.dp, Color(0xFF10B981).copy(alpha = 0.45f)),
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(18.dp))
                        Text("Tepat! Kata: ${item.word}", color = Color(0xFF10B981), fontSize = 14.sp, fontWeight = FontWeight.Black)
                    }
                    item.explanation?.let { Text(it, color = textPrimary, fontSize = 12.sp) }
                    Surface(
                        shape = RoundedCornerShape(13.dp),
                        color = Color(0xFF047857),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(13.dp))
                            .clickable {
                                onPlayTap()
                                if (idx + 1 < items.size) {
                                    idx += 1
                                    availableLetters = items[idx].scrambledLetters
                                    usedPoolIndices = emptyList()
                                    status = "PLAYING"
                                } else if (!doneSent) {
                                    doneSent = true
                                    onFinish(correctCount, items.size, maxStreak)
                                }
                            },
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(bottom = 4.dp)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(13.dp))
                                .background(Brush.verticalGradient(listOf(Color(0xFF34D399), Color(0xFF10B981))))
                                .padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = if (idx + 1 < items.size) "Kata Berikutnya" else "Selesai & Klaim +XP",
                                color = Color.White,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Black,
                            )
                            Spacer(Modifier.width(5.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                        }
                    }
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// 7. LATIH KOGNITIF (PAKET A, B & C ADAPTIVE BRAIN TRAINER)
// ═════════════════════════════════════════════════════════════════════════════

@Composable
internal fun CognitiveTrainerGameCompose(
    challenge: DailyQuestChallenge.CognitiveTrainer,
    disabled: Boolean,
    surfaceColor: Color,
    elevatedColor: Color,
    borderColor: Color,
    textPrimary: Color,
    textSecondary: Color,
    onPlayTap: () -> Unit,
    onPlayGood: () -> Unit,
    onPlayError: () -> Unit,
    onFinish: (correct: Int, total: Int, maxCombo: Int) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val stages = challenge.stages
    var stageIdx by remember(challenge.id) { mutableIntStateOf(0) }
    var correctCount by remember(challenge.id) { mutableIntStateOf(0) }
    var streak by remember(challenge.id) { mutableIntStateOf(0) }
    var maxStreak by remember(challenge.id) { mutableIntStateOf(1) }

    var memPhase by remember(challenge.id, stageIdx) { mutableStateOf("SHOWING") }
    var memPicked by remember(challenge.id, stageIdx) { mutableStateOf<List<Int>>(emptyList()) }
    var selectedOpt by remember(challenge.id, stageIdx) { mutableStateOf<Int?>(null) }
    var stageOk by remember(challenge.id, stageIdx) { mutableStateOf<Boolean?>(null) }
    var doneSent by remember(challenge.id) { mutableStateOf(false) }

    val stage = stages.getOrNull(stageIdx) ?: return

    LaunchedEffect(challenge.id, stageIdx) {
        if (stage.kind == "MEMORY_GRID") {
            memPhase = "SHOWING"
            delay(stage.viewMs.toLong().coerceIn(1500L, 4000L))
            if (memPhase == "SHOWING") memPhase = "RECALL"
        } else {
            memPhase = "DONE"
        }
    }

    fun registerResult(ok: Boolean) {
        stageOk = ok
        if (ok) {
            onPlayGood()
            val ns = streak + 1
            streak = ns
            maxStreak = maxOf(maxStreak, ns)
            correctCount += 1
        } else {
            onPlayError()
            streak = 0
        }
    }

    val badgeInfo = when (stage.kind) {
        "MEMORY_GRID" -> "MEMORI VISUAL" to Color(0xFF0EA5E9)
        "STROOP_FOCUS" -> "FOKUS STROOP" to Color(0xFFEC4899)
        "PATTERN_SEQUENCE" -> "POLA LOGIKA" to Color(0xFF8B5CF6)
        else -> "KECEPATAN HITUNG" to Color(0xFF10B981)
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = surfaceColor,
            border = BorderStroke(1.5.dp, borderColor),
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(shape = RoundedCornerShape(50), color = badgeInfo.second) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(Icons.Default.Psychology, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                        Text(badgeInfo.first, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black, maxLines = 1, softWrap = false)
                    }
                }
                Text(
                    text = "Tahap ${stageIdx + 1}/${stages.size} • ${challenge.gradeBand.replace("_", " ")}",
                    color = textPrimary,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = surfaceColor,
            border = BorderStroke(1.5.dp, borderColor),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(stage.prompt, color = textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)

                when (stage.kind) {
                    "MEMORY_GRID" -> {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (memPhase == "SHOWING") Color(0xFFF59E0B).copy(alpha = 0.16f) else Color(0xFF0EA5E9).copy(alpha = 0.16f),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Icon(Icons.Default.Visibility, contentDescription = null, tint = if (memPhase == "SHOWING") Color(0xFFF59E0B) else Color(0xFF0EA5E9), modifier = Modifier.size(13.dp))
                                Text(
                                    text = when (memPhase) {
                                        "SHOWING" -> "HAFALKAN KOTAK MENYALA..."
                                        "RECALL" -> "KETUK ${stage.targets.size} KOTAK (${memPicked.size}/${stage.targets.size})"
                                        else -> "SELESAI!"
                                    },
                                    color = if (memPhase == "SHOWING") Color(0xFFF59E0B) else Color(0xFF0EA5E9),
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Black,
                                )
                            }
                        }

                        val n = stage.gridSize.coerceIn(3, 4)
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = elevatedColor,
                            border = BorderStroke(1.5.dp, borderColor),
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                for (r in 0 until n) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        for (c in 0 until n) {
                                            val cIdx = r * n + c
                                            val isTarget = cIdx in stage.targets
                                            val wasPicked = cIdx in memPicked
                                            val litOk = (memPhase == "SHOWING" && isTarget) || (wasPicked && isTarget) || (memPhase == "DONE" && isTarget)
                                            val litWrong = wasPicked && !isTarget

                                            Box(
                                                modifier = Modifier
                                                    .size(58.dp)
                                                    .clip(RoundedCornerShape(13.dp))
                                                    .background(
                                                        when {
                                                            litWrong -> Color(0xFF9F1239)
                                                            litOk -> Color(0xFF0369A1)
                                                            else -> surfaceColor
                                                        }
                                                    )
                                                    .padding(bottom = if (litWrong || litOk) 4.dp else 0.dp)
                                                    .clip(RoundedCornerShape(13.dp))
                                                    .background(
                                                        when {
                                                            litWrong -> Color(0xFFF43F5E)
                                                            litOk -> Color(0xFF0EA5E9)
                                                            else -> surfaceColor
                                                        }
                                                    )
                                                    .border(
                                                        1.5.dp,
                                                        when {
                                                            litWrong -> Color(0xFFFDA4AF)
                                                            litOk -> Color(0xFFBAE6FD)
                                                            else -> borderColor
                                                        },
                                                        RoundedCornerShape(13.dp),
                                                    )
                                                    .clickable(enabled = memPhase == "RECALL" && stageOk == null) {
                                                        if (cIdx !in memPicked) {
                                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                            onPlayTap()
                                                            val nextPicked = memPicked + cIdx
                                                            memPicked = nextPicked
                                                            if (!isTarget) {
                                                                memPhase = "DONE"
                                                                registerResult(false)
                                                            } else if (nextPicked.size == stage.targets.size) {
                                                                memPhase = "DONE"
                                                                registerResult(true)
                                                            }
                                                        }
                                                    },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    "STROOP_FOCUS" -> {
                        val inkCol = when (stage.inkColor) {
                            "RED" -> Color(0xFFF43F5E)
                            "BLUE" -> Color(0xFF0EA5E9)
                            "GREEN" -> Color(0xFF10B981)
                            else -> Color(0xFFF59E0B)
                        }
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = elevatedColor,
                            border = BorderStroke(1.dp, borderColor),
                        ) {
                            Text(
                                text = stage.displayWord.orEmpty(),
                                color = inkCol,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Black,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 22.dp),
                            )
                        }
                    }

                    "PATTERN_SEQUENCE" -> {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = elevatedColor,
                            border = BorderStroke(1.dp, borderColor),
                        ) {
                            Text(
                                text = stage.sequence.orEmpty(),
                                color = Color(0xFF8B5CF6),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 18.dp, horizontal = 12.dp),
                            )
                        }
                    }

                    else -> { // QUICK_COMPARE
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFF0EA5E9).copy(alpha = 0.14f),
                                border = BorderStroke(1.5.dp, Color(0xFF0EA5E9).copy(alpha = 0.45f)),
                            ) {
                                Text(
                                    text = stage.leftExpr.orEmpty(),
                                    color = Color(0xFF0EA5E9),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 16.dp),
                                )
                            }
                            Text("VS", color = textSecondary, fontSize = 14.sp, fontWeight = FontWeight.Black)
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFFF59E0B).copy(alpha = 0.14f),
                                border = BorderStroke(1.5.dp, Color(0xFFF59E0B).copy(alpha = 0.45f)),
                            ) {
                                Text(
                                    text = stage.rightExpr.orEmpty(),
                                    color = Color(0xFFF59E0B),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 16.dp),
                                )
                            }
                        }
                    }
                }

                if (stage.kind != "MEMORY_GRID" && stage.options.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        stage.options.forEachIndexed { oIdx, opt ->
                            val isChosen = selectedOpt == oIdx
                            val isCorrect = oIdx == stage.correctIndex
                            val revealed = stageOk != null
                            Surface(
                                shape = RoundedCornerShape(13.dp),
                                color = when {
                                    revealed && isCorrect -> Color(0xFF10B981)
                                    revealed && isChosen -> Color(0xFFF43F5E)
                                    else -> elevatedColor
                                },
                                border = BorderStroke(1.dp, borderColor),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(13.dp))
                                    .clickable(enabled = !revealed && !disabled) {
                                        selectedOpt = oIdx
                                        registerResult(isCorrect)
                                    },
                            ) {
                                Text(
                                    text = opt,
                                    color = if (revealed && (isCorrect || isChosen)) Color.White else textPrimary,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 12.dp, horizontal = 10.dp),
                                )
                            }
                        }
                    }
                }
            }
        }

        if (stageOk != null) {
            val fColor = if (stageOk == true) Color(0xFF10B981) else Color(0xFFF43F5E)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = fColor.copy(alpha = 0.14f),
                border = BorderStroke(1.5.dp, fColor.copy(alpha = 0.45f)),
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(
                            imageVector = if (stageOk == true) Icons.Default.CheckCircle else Icons.Default.Close,
                            contentDescription = null,
                            tint = fColor,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = if (stageOk == true) "Hebat! Respon Kognitif Tepat!" else "Hampir! Latih lagi fokusnya.",
                            color = fColor,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                        )
                    }
                    stage.explanation?.let { Text(it, color = textPrimary, fontSize = 12.sp) }
                    Surface(
                        shape = RoundedCornerShape(13.dp),
                        color = Color(0xFF134E4A),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(13.dp))
                            .clickable {
                                onPlayTap()
                                if (stageIdx + 1 < stages.size) {
                                    stageIdx += 1
                                } else if (!doneSent) {
                                    doneSent = true
                                    onFinish(correctCount, stages.size, maxStreak)
                                }
                            },
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(bottom = 4.dp)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(13.dp))
                                .background(Brush.verticalGradient(listOf(Color(0xFF2DD4BF), Color(0xFF0D9488))))
                                .padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = if (stageIdx + 1 < stages.size) "Tahap Berikutnya" else "Selesai & Klaim +XP",
                                color = Color.White,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Black,
                            )
                            Spacer(Modifier.width(5.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                        }
                    }
                }
            }
        }
    }
}
