package com.schoolos.android.feature.quizzes.dailyquest

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.schoolos.android.domain.model.CrosswordClueItem
import com.schoolos.android.domain.model.DailyQuestChallenge
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

// ═════════════════════════════════════════════════════════════════════════════
// 1. TEKA-TEKI SILANG (CROSSWORD PUZZLE)
// ═════════════════════════════════════════════════════════════════════════════

private val QWERTY_ROWS = listOf(
    listOf("Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P"),
    listOf("A", "S", "D", "F", "G", "H", "J", "K", "L"),
    listOf("Z", "X", "C", "V", "B", "N", "M", "DEL"),
)

@Composable
internal fun CrosswordGameCompose(
    challenge: DailyQuestChallenge.Crossword,
    disabled: Boolean,
    surfaceColor: Color,
    elevatedColor: Color,
    borderColor: Color,
    textPrimary: Color,
    textSecondary: Color,
    onPlayTap: () -> Unit,
    onPlayGood: () -> Unit,
    onFinish: (correct: Int, total: Int, maxCombo: Int) -> Unit,
    onNewLevel: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val rows = challenge.rows
    val cols = challenge.cols

    var board by remember(challenge.id) {
        mutableStateOf(
            challenge.grid.map { rowStr ->
                rowStr.map { ch -> if (ch == '#') "#" else "" }
            }
        )
    }
    var selectedRow by remember(challenge.id) { mutableIntStateOf(challenge.clues.firstOrNull()?.row ?: 0) }
    var selectedCol by remember(challenge.id) { mutableIntStateOf(challenge.clues.firstOrNull()?.col ?: 0) }
    var activeDir by remember(challenge.id) { mutableStateOf(challenge.clues.firstOrNull()?.direction ?: "ACROSS") }
    var hintsUsed by remember(challenge.id) { mutableIntStateOf(0) }
    var feedbackNote by remember(challenge.id) { mutableStateOf<String?>(null) }
    var solvedSent by remember(challenge.id) { mutableStateOf(false) }

    val cellNumbers = remember(challenge.id) {
        val map = mutableMapOf<Pair<Int, Int>, Int>()
        challenge.clues.forEach { cl ->
            val key = cl.row to cl.col
            if (!map.containsKey(key)) map[key] = cl.number
        }
        map
    }

    fun findClueAt(r: Int, c: Int, dir: String): CrosswordClueItem? =
        challenge.clues.firstOrNull { cl ->
            cl.direction == dir && if (dir == "ACROSS") {
                cl.row == r && c >= cl.col && c < cl.col + cl.length
            } else {
                cl.col == c && r >= cl.row && r < cl.row + cl.length
            }
        }

    val activeClue = findClueAt(selectedRow, selectedCol, activeDir)
        ?: findClueAt(selectedRow, selectedCol, if (activeDir == "ACROSS") "DOWN" else "ACROSS")
        ?: challenge.clues.firstOrNull()

    val highlightedCells = remember(activeClue, challenge.id) {
        val set = mutableSetOf<Pair<Int, Int>>()
        activeClue?.let { cl ->
            for (k in 0 until cl.length) {
                val rr = if (cl.direction == "DOWN") cl.row + k else cl.row
                val cc = if (cl.direction == "ACROSS") cl.col + k else cl.col
                set.add(rr to cc)
            }
        }
        set
    }

    val totalPlayable = remember(challenge.id) {
        challenge.grid.sumOf { rowStr -> rowStr.count { it != '#' } }
    }

    fun evaluateWin(nextBoard: List<List<String>>) {
        if (solvedSent || disabled) return
        var correct = 0
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val target = challenge.grid.getOrNull(r)?.getOrNull(c) ?: '#'
                if (target != '#' && nextBoard.getOrNull(r)?.getOrNull(c) == target.toString()) {
                    correct++
                }
            }
        }
        if (totalPlayable > 0 && correct == totalPlayable) {
            solvedSent = true
            onPlayGood()
            onFinish(
                (challenge.clues.size - hintsUsed.coerceAtMost(1)).coerceAtLeast(1),
                challenge.clues.size.coerceAtLeast(1),
                (challenge.clues.size - hintsUsed).coerceAtLeast(1),
            )
        }
    }

    fun handleKey(key: String) {
        if (disabled || solvedSent) return
        if (challenge.grid.getOrNull(selectedRow)?.getOrNull(selectedCol) == '#') return
        onPlayTap()
        feedbackNote = null

        if (key == "DEL") {
            val next = board.map { it.toMutableList() }
            if (next[selectedRow][selectedCol].isNotEmpty()) {
                next[selectedRow][selectedCol] = ""
                board = next
            } else if (activeClue != null) {
                val idx = if (activeClue.direction == "ACROSS") selectedCol - activeClue.col else selectedRow - activeClue.row
                if (idx > 0) {
                    val pr = if (activeClue.direction == "DOWN") activeClue.row + idx - 1 else activeClue.row
                    val pc = if (activeClue.direction == "ACROSS") activeClue.col + idx - 1 else activeClue.col
                    next[pr][pc] = ""
                    board = next
                    selectedRow = pr
                    selectedCol = pc
                }
            }
            return
        }

        val next = board.map { it.toMutableList() }
        next[selectedRow][selectedCol] = key
        board = next

        if (activeClue != null) {
            val idx = if (activeClue.direction == "ACROSS") selectedCol - activeClue.col else selectedRow - activeClue.row
            if (idx + 1 < activeClue.length) {
                selectedRow = if (activeClue.direction == "DOWN") activeClue.row + idx + 1 else activeClue.row
                selectedCol = if (activeClue.direction == "ACROSS") activeClue.col + idx + 1 else activeClue.col
            }
        }
        evaluateWin(next)
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Active Clue Card
        if (activeClue != null) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = surfaceColor,
                border = BorderStroke(1.2.dp, Color(0xFF0EA5E9).copy(alpha = 0.45f)),
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFF0EA5E9)) {
                            Text(
                                text = "#${activeClue.number} ${if (activeClue.direction == "ACROSS") "MENDATAR" else "MENURUN"} • ${activeClue.length} HURUF",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            )
                        }
                        activeClue.topicTag?.takeIf { it.isNotBlank() }?.let { tag ->
                            Text(
                                text = tag,
                                color = textSecondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                    Text(
                        text = activeClue.clue,
                        color = textPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }

        // Crossword 2D Board
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = surfaceColor,
            border = BorderStroke(1.dp, borderColor),
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val cellDp = ((maxWidth - 20.dp) / cols.coerceAtLeast(1)).coerceIn(26.dp, 38.dp)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = elevatedColor,
                        modifier = Modifier.align(Alignment.Center),
                    ) {
                        Column(
                            modifier = Modifier.padding(6.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp),
                        ) {
                            for (r in 0 until rows) {
                                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                    for (c in 0 until cols) {
                                        val target = challenge.grid.getOrNull(r)?.getOrNull(c) ?: '#'
                                        if (target == '#') {
                                            Spacer(modifier = Modifier.size(cellDp))
                                        } else {
                                            val isSel = r == selectedRow && c == selectedCol
                                            val isWord = (r to c) in highlightedCells
                                            val num = cellNumbers[r to c]
                                            val letter = board.getOrNull(r)?.getOrNull(c).orEmpty()

                                            Box(
                                                modifier = Modifier
                                                    .size(cellDp)
                                                    .clip(RoundedCornerShape(7.dp))
                                                    .background(
                                                        when {
                                                            isSel -> Color(0xFF0369A1)
                                                            letter.isNotEmpty() -> Color(0xFFD97706)
                                                            isWord -> Color(0xFF0EA5E9).copy(alpha = 0.22f)
                                                            else -> surfaceColor
                                                        }
                                                    )
                                                    .padding(bottom = if (isSel || letter.isNotEmpty()) 2.5.dp else 0.dp)
                                                    .clip(RoundedCornerShape(7.dp))
                                                    .background(
                                                        when {
                                                            isSel -> Color(0xFF0EA5E9)
                                                            letter.isNotEmpty() -> Color(0xFFFEF3C7)
                                                            isWord -> Color(0xFF0EA5E9).copy(alpha = 0.22f)
                                                            else -> surfaceColor
                                                        }
                                                    )
                                                    .border(
                                                        width = if (isSel) 1.5.dp else 1.dp,
                                                        color = when {
                                                            isSel -> Color(0xFF38BDF8)
                                                            letter.isNotEmpty() -> Color(0xFFF59E0B)
                                                            isWord -> Color(0xFF0EA5E9).copy(alpha = 0.6f)
                                                            else -> borderColor
                                                        },
                                                        shape = RoundedCornerShape(7.dp),
                                                    )
                                                    .clickable {
                                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                        if (selectedRow == r && selectedCol == c) {
                                                            val alt = if (activeDir == "ACROSS") "DOWN" else "ACROSS"
                                                            if (findClueAt(r, c, alt) != null) activeDir = alt
                                                        } else {
                                                            selectedRow = r
                                                            selectedCol = c
                                                            if (findClueAt(r, c, activeDir) == null) {
                                                                val alt = if (activeDir == "ACROSS") "DOWN" else "ACROSS"
                                                                if (findClueAt(r, c, alt) != null) activeDir = alt
                                                            }
                                                        }
                                                    },
                                            ) {
                                                if (num != null) {
                                                    Text(
                                                        text = num.toString(),
                                                        color = when {
                                                            isSel -> Color.White.copy(alpha = 0.9f)
                                                            letter.isNotEmpty() -> Color(0xFF92400E)
                                                            else -> textSecondary
                                                        },
                                                        fontSize = 8.sp,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        modifier = Modifier
                                                            .align(Alignment.TopStart)
                                                            .padding(start = 2.dp, top = 0.dp),
                                                    )
                                                }
                                                Text(
                                                    text = letter,
                                                    color = when {
                                                        isSel -> Color.White
                                                        letter.isNotEmpty() -> Color(0xFF78350F)
                                                        else -> textPrimary
                                                    },
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Black,
                                                    modifier = Modifier.align(Alignment.Center),
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Action Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFF59E0B).copy(alpha = 0.16f),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(enabled = !disabled) {
                                val target = challenge.grid.getOrNull(selectedRow)?.getOrNull(selectedCol)
                                var tr = selectedRow
                                var tc = selectedCol
                                if (target != null && target != '#' && board[tr][tc] == target.toString() && activeClue != null) {
                                    for (k in 0 until activeClue.length) {
                                        val rr = if (activeClue.direction == "DOWN") activeClue.row + k else activeClue.row
                                        val cc = if (activeClue.direction == "ACROSS") activeClue.col + k else activeClue.col
                                        if (board[rr][cc] != challenge.grid[rr][cc].toString()) {
                                            tr = rr
                                            tc = cc
                                            break
                                        }
                                    }
                                }
                                val ansChar = challenge.grid.getOrNull(tr)?.getOrNull(tc)
                                if (ansChar != null && ansChar != '#') {
                                    hintsUsed++
                                    val next = board.map { it.toMutableList() }
                                    next[tr][tc] = ansChar.toString()
                                    board = next
                                    evaluateWin(next)
                                }
                            },
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Default.Lightbulb, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(13.dp))
                            Text(" Petunjuk", color = Color(0xFFF59E0B), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, softWrap = false)
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF10B981).copy(alpha = 0.16f),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(enabled = !disabled) {
                                var correct = 0
                                for (r in 0 until rows) {
                                    for (c in 0 until cols) {
                                        val t = challenge.grid[r][c]
                                        if (t != '#' && board[r][c] == t.toString()) correct++
                                    }
                                }
                                if (correct == totalPlayable) {
                                    evaluateWin(board)
                                } else {
                                    feedbackNote = "$correct dari $totalPlayable huruf sudah benar."
                                }
                            },
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(13.dp))
                            Text(" Periksa", color = Color(0xFF10B981), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, softWrap = false)
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = elevatedColor,
                        border = BorderStroke(1.dp, borderColor),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onNewLevel() },
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = textSecondary, modifier = Modifier.size(13.dp))
                            Text(" TTS Baru", color = textPrimary, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, softWrap = false)
                        }
                    }
                }

                if (feedbackNote != null) {
                    Text(
                        text = feedbackNote!!,
                        color = Color(0xFF0EA5E9),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }

        // On-Screen QWERTY Keyboard
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = surfaceColor,
            border = BorderStroke(1.dp, borderColor),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                QWERTY_ROWS.forEach { kRow ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
                    ) {
                        kRow.forEach { k ->
                            val isDel = k == "DEL"
                            Box(
                                modifier = Modifier
                                    .weight(if (isDel) 1.5f else 1f)
                                    .height(38.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isDel) Color(0xFFF43F5E).copy(alpha = 0.16f) else elevatedColor)
                                    .border(
                                        1.dp,
                                        if (isDel) Color(0xFFF43F5E).copy(alpha = 0.35f) else borderColor,
                                        RoundedCornerShape(8.dp),
                                    )
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        handleKey(k)
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                if (isDel) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Backspace,
                                        contentDescription = "Hapus",
                                        tint = Color(0xFFF43F5E),
                                        modifier = Modifier.size(16.dp),
                                    )
                                } else {
                                    Text(
                                        text = k,
                                        color = textPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Black,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Clues List
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = surfaceColor,
            border = BorderStroke(1.dp, borderColor),
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val across = challenge.clues.filter { it.direction == "ACROSS" }
                val down = challenge.clues.filter { it.direction == "DOWN" }
                Text("MENDATAR (${across.size})", color = Color(0xFF0EA5E9), fontSize = 11.sp, fontWeight = FontWeight.Black)
                across.forEach { cl ->
                    val isAct = activeClue?.number == cl.number && activeClue.direction == cl.direction
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isAct) Color(0xFF0EA5E9).copy(alpha = 0.14f) else elevatedColor,
                        border = if (isAct) BorderStroke(1.dp, Color(0xFF0EA5E9).copy(alpha = 0.45f)) else null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                selectedRow = cl.row
                                selectedCol = cl.col
                                activeDir = cl.direction
                            },
                    ) {
                        Text(
                            text = "${cl.number}. ${cl.clue}",
                            color = textPrimary,
                            fontSize = 11.5.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))
                Text("MENURUN (${down.size})", color = Color(0xFF10B981), fontSize = 11.sp, fontWeight = FontWeight.Black)
                down.forEach { cl ->
                    val isAct = activeClue?.number == cl.number && activeClue.direction == cl.direction
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isAct) Color(0xFF10B981).copy(alpha = 0.14f) else elevatedColor,
                        border = if (isAct) BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.45f)) else null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                selectedRow = cl.row
                                selectedCol = cl.col
                                activeDir = cl.direction
                            },
                    ) {
                        Text(
                            text = "${cl.number}. ${cl.clue}",
                            color = textPrimary,
                            fontSize = 11.5.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        )
                    }
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// 2. PUZZLE GESER (SLIDING PICTURE & NUMBER PUZZLE 3x3 / 4x4)
// ═════════════════════════════════════════════════════════════════════════════

private fun buildSolvableSlidingBoard(size: Int, seed: Long): List<Int> {
    val total = size * size
    val arr = ((1 until total).toList() + 0).toMutableList()
    var blank = total - 1
    var prev = blank
    var s = seed xor System.currentTimeMillis()
    fun nextRand(): Int {
        s = s * 6364136223846793005L + 1442695040888963407L
        return ((s ushr 32) and 0x7FFF_FFFFL).toInt()
    }
    repeat(total * 16) {
        val br = blank / size
        val bc = blank % size
        val neighbors = mutableListOf<Int>()
        if (br > 0) neighbors.add((br - 1) * size + bc)
        if (br + 1 < size) neighbors.add((br + 1) * size + bc)
        if (bc > 0) neighbors.add(br * size + (bc - 1))
        if (bc + 1 < size) neighbors.add(br * size + (bc + 1))
        val cands = neighbors.filter { it != prev }
        val pick = cands[nextRand() % cands.size]
        val tmp = arr[blank]
        arr[blank] = arr[pick]
        arr[pick] = tmp
        prev = blank
        blank = pick
    }
    return arr
}

@Composable
internal fun SlidingPuzzleGameCompose(
    challenge: DailyQuestChallenge.SlidingPuzzle,
    disabled: Boolean,
    surfaceColor: Color,
    elevatedColor: Color,
    borderColor: Color,
    textPrimary: Color,
    textSecondary: Color,
    onPlayTap: () -> Unit,
    onPlayGood: () -> Unit,
    onFinish: (correct: Int, total: Int, maxCombo: Int) -> Unit,
    onNewLevel: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    var gridSize by remember(challenge.id) { mutableIntStateOf(challenge.gridSize.coerceIn(3, 4)) }
    var tiles by remember(challenge.id) { mutableStateOf(challenge.initialTiles) }
    var moves by remember(challenge.id) { mutableIntStateOf(0) }
    var showNumbers by remember(challenge.id) { mutableStateOf(true) }
    var showPreview by remember(challenge.id) { mutableStateOf(false) }
    var won by remember(challenge.id) { mutableStateOf(false) }

    val correctCount = tiles.withIndex().count { (idx, valTile) -> valTile != 0 && valTile == idx + 1 }

    fun checkWin(nextTiles: List<Int>, nextMoves: Int) {
        if (won || disabled) return
        val total = gridSize * gridSize
        val solved = nextTiles.withIndex().all { (idx, valTile) ->
            if (idx == total - 1) valTile == 0 else valTile == idx + 1
        }
        if (solved) {
            won = true
            onPlayGood()
            val combo = ((challenge.parMoves.toFloat() / nextMoves.coerceAtLeast(1)) * 6f).roundToInt().coerceIn(1, 10)
            onFinish(total, total, combo)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = surfaceColor,
            border = BorderStroke(1.dp, borderColor),
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(challenge.themeTitle, color = textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Black)
                    Text("$moves Langkah • $correctCount/${gridSize * gridSize - 1} Posisi Benar", color = textSecondary, fontSize = 11.sp)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(3, 4).forEach { sz ->
                        Surface(
                            shape = RoundedCornerShape(9.dp),
                            color = if (gridSize == sz) Color(0xFF0EA5E9) else elevatedColor,
                            border = BorderStroke(1.dp, borderColor),
                            modifier = Modifier
                                .clip(RoundedCornerShape(9.dp))
                                .clickable {
                                    gridSize = sz
                                    tiles = buildSolvableSlidingBoard(sz, challenge.seed)
                                    moves = 0
                                    won = false
                                },
                        ) {
                            Text(
                                text = "${sz}×$sz",
                                color = if (gridSize == sz) Color.White else textPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            )
                        }
                    }
                }
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = surfaceColor,
            border = BorderStroke(1.dp, borderColor),
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (showPreview) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .border(2.dp, Color(0xFF0EA5E9), RoundedCornerShape(14.dp)),
                    ) {
                        SlidingPuzzleArtworkSlice(
                            themeId = challenge.themeId,
                            gridSize = gridSize,
                            goalRow = -1,
                            goalCol = -1,
                        )
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = elevatedColor,
                        border = BorderStroke(1.dp, borderColor),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .padding(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            for (r in 0 until gridSize) {
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    for (c in 0 until gridSize) {
                                        val idx = r * gridSize + c
                                        val tileVal = tiles.getOrElse(idx) { 0 }
                                        if (tileVal == 0) {
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .fillMaxSize()
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(Color(0xFF0EA5E9).copy(alpha = 0.08f))
                                                    .border(1.dp, borderColor, RoundedCornerShape(10.dp)),
                                            )
                                        } else {
                                            val goalIdx = tileVal - 1
                                            val goalR = goalIdx / gridSize
                                            val goalC = goalIdx % gridSize
                                            val isCorrect = goalIdx == idx

                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .fillMaxSize()
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .border(
                                                        width = if (isCorrect) 2.dp else 1.dp,
                                                        color = if (isCorrect) Color(0xFF10B981) else borderColor,
                                                        shape = RoundedCornerShape(10.dp),
                                                    )
                                                    .clickable(enabled = !disabled && !won) {
                                                        val blankIdx = tiles.indexOf(0)
                                                        if (blankIdx >= 0) {
                                                            val br = blankIdx / gridSize
                                                            val bc = blankIdx % gridSize
                                                            if (abs(r - br) + abs(c - bc) == 1) {
                                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                                onPlayTap()
                                                                val next = tiles.toMutableList()
                                                                next[blankIdx] = tileVal
                                                                next[idx] = 0
                                                                val nextMoves = moves + 1
                                                                tiles = next
                                                                moves = nextMoves
                                                                checkWin(next, nextMoves)
                                                            }
                                                        }
                                                    },
                                            ) {
                                                SlidingPuzzleArtworkSlice(
                                                    themeId = challenge.themeId,
                                                    gridSize = gridSize,
                                                    goalRow = goalR,
                                                    goalCol = goalC,
                                                )
                                                if (showNumbers) {
                                                    Surface(
                                                        shape = RoundedCornerShape(6.dp),
                                                        color = if (isCorrect) Color(0xFF10B981) else Color(0xFF0F172A).copy(alpha = 0.72f),
                                                        modifier = Modifier
                                                            .align(Alignment.TopStart)
                                                            .padding(4.dp),
                                                    ) {
                                                        Text(
                                                            text = tileVal.toString(),
                                                            color = Color.White,
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Black,
                                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
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

                // Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (showPreview) Color(0xFF0EA5E9) else elevatedColor,
                        border = BorderStroke(1.dp, borderColor),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { showPreview = !showPreview },
                    ) {
                        Row(modifier = Modifier.padding(vertical = 8.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Visibility, contentDescription = null, tint = if (showPreview) Color.White else textPrimary, modifier = Modifier.size(13.dp))
                            Text(
                                text = if (showPreview) " Lanjut" else " Intip",
                                color = if (showPreview) Color.White else textPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                softWrap = false,
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = elevatedColor,
                        border = BorderStroke(1.dp, borderColor),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { showNumbers = !showNumbers },
                    ) {
                        Row(modifier = Modifier.padding(vertical = 8.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Tag, contentDescription = null, tint = textPrimary, modifier = Modifier.size(13.dp))
                            Text(
                                text = if (showNumbers) " Angka" else " Polos",
                                color = textPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                softWrap = false,
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = elevatedColor,
                        border = BorderStroke(1.dp, borderColor),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                tiles = buildSolvableSlidingBoard(gridSize, challenge.seed)
                                moves = 0
                                won = false
                            },
                    ) {
                        Row(modifier = Modifier.padding(vertical = 8.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Apps, contentDescription = null, tint = textPrimary, modifier = Modifier.size(13.dp))
                            Text(" Acak", color = textPrimary, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, softWrap = false)
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF10B981).copy(alpha = 0.14f),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onNewLevel() },
                    ) {
                        Row(modifier = Modifier.padding(vertical = 8.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(13.dp))
                            Text(" Tema Baru", color = Color(0xFF10B981), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, softWrap = false)
                        }
                    }
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// 3. BLOCK BLAST 8x8
// ═════════════════════════════════════════════════════════════════════════════

private data class BlockPiece(
    val uid: Int,
    val cells: List<Pair<Int, Int>>,
    val color: Color,
    val rows: Int,
    val cols: Int,
)

private val BLOCK_SHAPES = listOf(
    listOf(0 to 0),
    listOf(0 to 0, 0 to 1),
    listOf(0 to 0, 1 to 0),
    listOf(0 to 0, 0 to 1, 0 to 2),
    listOf(0 to 0, 1 to 0, 2 to 0),
    listOf(0 to 0, 0 to 1, 0 to 2, 0 to 3),
    listOf(0 to 0, 1 to 0, 2 to 0, 3 to 0),
    listOf(0 to 0, 0 to 1, 1 to 0, 1 to 1),
    listOf(0 to 0, 1 to 0, 1 to 1),
    listOf(0 to 1, 1 to 0, 1 to 1),
    listOf(0 to 0, 0 to 1, 0 to 2, 1 to 0),
    listOf(0 to 0, 0 to 1, 0 to 2, 1 to 2),
    listOf(0 to 0, 0 to 1, 0 to 2, 1 to 1),
    listOf(0 to 0, 0 to 1, 1 to 1, 1 to 2),
    listOf(0 to 1, 0 to 2, 1 to 0, 1 to 1),
    listOf(0 to 0, 0 to 1, 0 to 2, 1 to 0, 1 to 1, 1 to 2),
)

private val BLOCK_PALETTE = listOf(
    Color(0xFF0EA5E9),
    Color(0xFF10B981),
    Color(0xFFF59E0B),
    Color(0xFF8B5CF6),
    Color(0xFFF43F5E),
    Color(0xFFEC4899),
)

private fun canPlaceBlock(board: List<List<Color?>>, piece: BlockPiece, baseR: Int, baseC: Int): Boolean {
    for ((dr, dc) in piece.cells) {
        val rr = baseR + dr
        val cc = baseC + dc
        if (rr !in 0..7 || cc !in 0..7) return false
        if (board[rr][cc] != null) return false
    }
    return true
}

private fun anyBlockMoveLeft(board: List<List<Color?>>, tray: List<BlockPiece?>): Boolean {
    for (p in tray) {
        if (p == null) continue
        for (r in 0..7) for (c in 0..7) {
            if (canPlaceBlock(board, p, r, c)) return true
        }
    }
    return false
}

@Composable
internal fun BlockBlastGameCompose(
    challenge: DailyQuestChallenge.BlockPuzzle,
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
    onNewLevel: () -> Unit,
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("schoolos_games", Context.MODE_PRIVATE) }
    val haptic = LocalHapticFeedback.current

    var rngState by remember(challenge.id) { mutableLongStateOf(challenge.seed) }
    var pieceUid by remember(challenge.id) { mutableIntStateOf(1) }

    fun nextRand(): Int {
        rngState = rngState * 6364136223846793005L + 1442695040888963407L
        return ((rngState ushr 32) and 0x7FFF_FFFFL).toInt()
    }

    fun makeTray(): List<BlockPiece?> = (0..2).map {
        val rv = nextRand()
        val cells = BLOCK_SHAPES[rv % BLOCK_SHAPES.size]
        val col = BLOCK_PALETTE[(rv ushr 4) % BLOCK_PALETTE.size]
        BlockPiece(
            uid = pieceUid++,
            cells = cells,
            color = col,
            rows = (cells.maxOf { it.first }) + 1,
            cols = (cells.maxOf { it.second }) + 1,
        )
    }

    fun buildInitialBoard(): List<List<Color?>> {
        val g = MutableList(8) { MutableList<Color?>(8) { null } }
        challenge.prefilledCells.forEach { (r, c) ->
            if (r in 0..7 && c in 0..7) g[r][c] = Color(0xFF64748B)
        }
        return g
    }

    var board by remember(challenge.id) { mutableStateOf(buildInitialBoard()) }
    var tray by remember(challenge.id) { mutableStateOf(makeTray()) }
    var selectedTrayIdx by remember(challenge.id) { mutableIntStateOf(0) }
    var previewAnchor by remember(challenge.id) { mutableStateOf<Pair<Int, Int>?>(null) }
    var score by remember(challenge.id) { mutableIntStateOf(0) }
    var bestScore by remember(challenge.id) { mutableIntStateOf(prefs.getInt("block_best", 0)) }
    var linesCleared by remember(challenge.id) { mutableIntStateOf(0) }
    var maxCombo by remember(challenge.id) { mutableIntStateOf(1) }
    var comboText by remember(challenge.id) { mutableStateOf<String?>(null) }
    var gameOver by remember(challenge.id) { mutableStateOf(false) }
    var claimed by remember(challenge.id) { mutableStateOf(false) }

    val selectedPiece = tray.getOrNull(selectedTrayIdx)
    val targetScore = challenge.targetScore.coerceAtLeast(250)
    val targetLines = challenge.targetLines.coerceAtLeast(3)
    val missionReady = score >= targetScore || linesCleared >= targetLines

    val previewSet = remember(previewAnchor, selectedPiece, board) {
        val anchor = previewAnchor ?: return@remember Triple(emptySet<Pair<Int, Int>>(), false, emptySet<Int>() to emptySet<Int>())
        val piece = selectedPiece ?: return@remember Triple(emptySet<Pair<Int, Int>>(), false, emptySet<Int>() to emptySet<Int>())
        val valid = canPlaceBlock(board, piece, anchor.first, anchor.second)
        val cells = piece.cells.map { (dr, dc) -> (anchor.first + dr) to (anchor.second + dc) }
            .filter { (rr, cc) -> rr in 0..7 && cc in 0..7 }.toSet()
        val clearRows = if (valid) (0..7).filter { rr -> (0..7).all { cc -> board[rr][cc] != null || (rr to cc) in cells } }.toSet() else emptySet()
        val clearCols = if (valid) (0..7).filter { cc -> (0..7).all { rr -> board[rr][cc] != null || (rr to cc) in cells } }.toSet() else emptySet()
        Triple(cells, valid, clearRows to clearCols)
    }

    fun placePieceAt(r: Int, c: Int) {
        if (disabled || gameOver || selectedPiece == null) return
        val currentAnchor = previewAnchor
        if (currentAnchor != (r to c)) {
            previewAnchor = r to c
            onPlayTap()
            return
        }
        if (!canPlaceBlock(board, selectedPiece, r, c)) {
            onPlayError()
            return
        }
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        val nextBoard = board.map { it.toMutableList() }
        selectedPiece.cells.forEach { (dr, dc) ->
            nextBoard[r + dr][c + dc] = selectedPiece.color
        }

        val fullRows = (0..7).filter { rr -> nextBoard[rr].all { it != null } }
        val fullCols = (0..7).filter { cc -> (0..7).all { rr -> nextBoard[rr][cc] != null } }
        fullRows.forEach { rr -> for (cc in 0..7) nextBoard[rr][cc] = null }
        fullCols.forEach { cc -> for (rr in 0..7) nextBoard[rr][cc] = null }

        val cleared = fullRows.size + fullCols.size
        val gained = selectedPiece.cells.size * 10 + if (cleared > 0) cleared * cleared * 80 else 0
        if (cleared > 0) onPlayGood() else onPlayTap()

        score += gained
        linesCleared += cleared
        if (cleared >= 2) {
            maxCombo = max(maxCombo, cleared)
            comboText = "MEGA BLAST ×$cleared! +$gained Poin"
        } else if (cleared == 1) {
            comboText = "LINE CLEAR! +$gained Poin"
        } else {
            comboText = null
        }

        if (score > bestScore) {
            bestScore = score
            prefs.edit().putInt("block_best", bestScore).apply()
        }

        val nextTray = tray.mapIndexed { idx, p -> if (idx == selectedTrayIdx) null else p }
        val finalTray = if (nextTray.all { it == null }) makeTray() else nextTray
        val nextSel = finalTray.indexOfFirst { it != null }.coerceAtLeast(0)

        board = nextBoard
        tray = finalTray
        selectedTrayIdx = nextSel
        previewAnchor = null
        if (!anyBlockMoveLeft(nextBoard, finalTray)) {
            gameOver = true
        }
    }

    fun submitClaim() {
        if (claimed || disabled) return
        claimed = true
        val pct = ((score.toFloat() / targetScore) * 100f).roundToInt().coerceIn(40, 100)
        onFinish(pct, 100, maxCombo)
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Score & Target Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = surfaceColor,
            border = BorderStroke(1.dp, borderColor),
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Column {
                            Text("SKOR", color = textSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text("$score", color = Color(0xFF0EA5E9), fontSize = 19.sp, fontWeight = FontWeight.Black)
                        }
                        Column {
                            Text("REKOR", color = textSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text("$bestScore", color = Color(0xFFF59E0B), fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                        }
                        Column {
                            Text("BARIS", color = textSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text("$linesCleared", color = Color(0xFF10B981), fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }

                    Button(
                        onClick = { submitClaim() },
                        enabled = missionReady && !disabled && !claimed,
                        shape = RoundedCornerShape(11.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF10B981),
                            contentColor = Color.White,
                            disabledContainerColor = elevatedColor,
                            disabledContentColor = textSecondary,
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    ) {
                        Icon(Icons.Default.WorkspacePremium, contentDescription = null, modifier = Modifier.size(14.dp))
                        Text(
                            text = if (missionReady) " Klaim +XP" else " Target $targetScore",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            maxLines = 1,
                            softWrap = false,
                        )
                    }
                }

                val pctBar = (score.toFloat() / targetScore.toFloat()).coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(elevatedColor),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(pctBar.coerceAtLeast(0.03f))
                            .height(6.dp)
                            .clip(CircleShape)
                            .background(if (missionReady) Color(0xFF10B981) else Color(0xFF0EA5E9)),
                    )
                }

                if (comboText != null) {
                    Text(
                        text = comboText!!,
                        color = Color(0xFFF59E0B),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        // 8x8 Grid + 3 Piece Tray
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = surfaceColor,
            border = BorderStroke(1.dp, borderColor),
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // Sleek Deep Arcade Cabinet 8x8 Board
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color(0xFF020617),
                ) {
                    Surface(
                        modifier = Modifier.padding(bottom = 5.dp),
                        shape = RoundedCornerShape(18.dp),
                        color = Color(0xFF0F172A),
                        border = BorderStroke(2.dp, Color(0xFF38BDF8).copy(alpha = 0.4f)),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            for (r in 0..7) {
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    for (c in 0..7) {
                                        val cellColor = board[r][c]
                                        val isGhost = (r to c) in previewSet.first
                                        val willClear = r in previewSet.third.first || c in previewSet.third.second

                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxSize()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(
                                                    when {
                                                        cellColor != null -> cellColor.copy(alpha = 0.65f)
                                                        isGhost && previewSet.second -> (selectedPiece?.color ?: Color(0xFF10B981)).copy(alpha = 0.55f)
                                                        isGhost -> Color(0xFFF43F5E).copy(alpha = 0.45f)
                                                        willClear -> Color(0xFFFBBF24).copy(alpha = 0.25f)
                                                        else -> Color(0xFF1E293B)
                                                    }
                                                )
                                                .padding(bottom = if (cellColor != null) 3.5.dp else 0.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(
                                                    when {
                                                        cellColor != null -> cellColor
                                                        isGhost && previewSet.second -> (selectedPiece?.color ?: Color(0xFF10B981)).copy(alpha = 0.45f)
                                                        isGhost -> Color(0xFFF43F5E).copy(alpha = 0.42f)
                                                        willClear -> Color(0xFFFBBF24).copy(alpha = 0.22f)
                                                        else -> Color(0xFF1E293B)
                                                    }
                                                )
                                                .border(
                                                    width = if (willClear) 1.5.dp else 1.dp,
                                                    color = when {
                                                        willClear -> Color(0xFFFDE047)
                                                        cellColor != null -> Color.White.copy(alpha = 0.55f)
                                                        isGhost -> Color.White.copy(alpha = 0.7f)
                                                        else -> Color.White.copy(alpha = 0.08f)
                                                    },
                                                    shape = RoundedCornerShape(8.dp),
                                                )
                                                .clickable { placePieceAt(r, c) },
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            // Faceted Gem Highlight Inner Square
                                            if (cellColor != null) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxSize(0.62f)
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(Color.White.copy(alpha = 0.26f)),
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Text(
                    text = if (previewAnchor != null) {
                        if (previewSet.second) "Ketuk lagi kotak yang sama untuk PASANG!" else "Posisi terhalang! Pilih kotak kosong lain."
                    } else {
                        "Pilih balok di bawah, ketuk 1× pratinjau lalu ketuk lagi untuk pasang:"
                    },
                    color = if (previewAnchor != null && !previewSet.second) Color(0xFFF43F5E) else textSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    tray.forEachIndexed { idx, piece ->
                        val isSel = idx == selectedTrayIdx && piece != null
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(78.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable(enabled = piece != null && !gameOver) {
                                    selectedTrayIdx = idx
                                    previewAnchor = null
                                },
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSel) Color(0xFF0EA5E9).copy(alpha = 0.16f) else elevatedColor,
                            border = BorderStroke(if (isSel) 2.dp else 1.dp, if (isSel) Color(0xFF38BDF8) else borderColor),
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                if (piece != null) {
                                    Column(verticalArrangement = Arrangement.spacedBy(2.5.dp)) {
                                        for (pr in 0 until piece.rows) {
                                            Row(horizontalArrangement = Arrangement.spacedBy(2.5.dp)) {
                                                for (pc in 0 until piece.cols) {
                                                    val active = (pr to pc) in piece.cells
                                                    Box(
                                                        modifier = Modifier
                                                            .size(14.dp)
                                                            .clip(RoundedCornerShape(3.5.dp))
                                                            .background(if (active) piece.color else Color.Transparent)
                                                            .border(
                                                                if (active) 1.dp else 0.dp,
                                                                if (active) Color.White.copy(alpha = 0.45f) else Color.Transparent,
                                                                RoundedCornerShape(3.5.dp),
                                                            ),
                                                    )
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    Text("Kosong", color = textSecondary, fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }

                if (gameOver) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFF43F5E).copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, Color(0xFFF43F5E).copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text("Papan Penuh! Skor Akhir: $score", color = Color(0xFFF43F5E), fontSize = 13.sp, fontWeight = FontWeight.Black)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { submitClaim() },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                    shape = RoundedCornerShape(10.dp),
                                ) {
                                    Icon(Icons.Default.EmojiEvents, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Text(" Simpan XP", fontSize = 11.sp, fontWeight = FontWeight.Black)
                                }
                                Button(
                                    onClick = {
                                        board = buildInitialBoard()
                                        tray = makeTray()
                                        selectedTrayIdx = 0
                                        score = 0
                                        linesCleared = 0
                                        gameOver = false
                                        comboText = null
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = surfaceColor, contentColor = textPrimary),
                                    shape = RoundedCornerShape(10.dp),
                                ) {
                                    Text("Main Ulang", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = elevatedColor,
                        border = BorderStroke(1.dp, borderColor),
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                board = buildInitialBoard()
                                tray = makeTray()
                                selectedTrayIdx = 0
                                score = 0
                                linesCleared = 0
                                gameOver = false
                                comboText = null
                            },
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = textSecondary, modifier = Modifier.size(13.dp))
                            Text(" Reset", color = textPrimary, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF0EA5E9).copy(alpha = 0.14f),
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onNewLevel() },
                    ) {
                        Text(
                            text = "Level Baru",
                            color = Color(0xFF0EA5E9),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        )
                    }
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// 4. 2048 ANGKA (NUMBER MERGE GAME)
// ═════════════════════════════════════════════════════════════════════════════

private fun slideRowLeft(line: List<Int>): Pair<List<Int>, Int> {
    val compact = line.filter { it != 0 }
    val out = mutableListOf<Int>()
    var gained = 0
    var i = 0
    while (i < compact.size) {
        if (i + 1 < compact.size && compact[i] == compact[i + 1]) {
            val merged = compact[i] * 2
            out.add(merged)
            gained += merged
            i += 2
        } else {
            out.add(compact[i])
            i += 1
        }
    }
    while (out.size < 4) out.add(0)
    return out to gained
}

private fun has2048Moves(grid: List<List<Int>>): Boolean {
    for (r in 0..3) for (c in 0..3) {
        if (grid[r][c] == 0) return true
        if (r + 1 < 4 && grid[r][c] == grid[r + 1][c]) return true
        if (c + 1 < 4 && grid[r][c] == grid[r][c + 1]) return true
    }
    return false
}

private fun tileColors(v: Int, surfaceColor: Color, textPrimary: Color): Pair<Color, Color> = when (v) {
    0 -> surfaceColor to textPrimary
    2 -> Color(0xFFE0F2FE) to Color(0xFF0369A1)
    4 -> Color(0xFFBAE6FD) to Color(0xFF0C4A6E)
    8 -> Color(0xFF38BDF8) to Color.White
    16 -> Color(0xFF0EA5E9) to Color.White
    32 -> Color(0xFF8B5CF6) to Color.White
    64 -> Color(0xFFEC4899) to Color.White
    128 -> Color(0xFFF59E0B) to Color.White
    256 -> Color(0xFF10B981) to Color.White
    512 -> Color(0xFF059669) to Color.White
    1024 -> Color(0xFFEAB308) to Color(0xFF0F172A)
    else -> Color(0xFFF43F5E) to Color.White
}

@Composable
internal fun NumberMergeGameCompose(
    challenge: DailyQuestChallenge.NumberMerge,
    disabled: Boolean,
    surfaceColor: Color,
    elevatedColor: Color,
    borderColor: Color,
    textPrimary: Color,
    textSecondary: Color,
    onPlayTap: () -> Unit,
    onPlayGood: () -> Unit,
    onFinish: (correct: Int, total: Int, maxCombo: Int) -> Unit,
    onNewLevel: () -> Unit,
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("schoolos_games", Context.MODE_PRIVATE) }
    val haptic = LocalHapticFeedback.current

    var rngState by remember(challenge.id) { mutableLongStateOf(challenge.seed) }
    fun nextRand(): Int {
        rngState = rngState * 6364136223846793005L + 1442695040888963407L
        return ((rngState ushr 32) and 0x7FFF_FFFFL).toInt()
    }

    fun buildStart(): List<List<Int>> {
        val g = MutableList(4) { MutableList(4) { 0 } }
        if (challenge.initialTiles.isNotEmpty()) {
            challenge.initialTiles.forEach { t ->
                if (t.row in 0..3 && t.col in 0..3) g[t.row][t.col] = t.value
            }
        } else {
            g[0][0] = 2
            g[2][2] = 2
        }
        return g
    }

    var grid by remember(challenge.id) { mutableStateOf(buildStart()) }
    var prevGrid by remember(challenge.id) { mutableStateOf<Pair<List<List<Int>>, Int>?>(null) }
    var score by remember(challenge.id) { mutableIntStateOf(0) }
    var bestScore by remember(challenge.id) { mutableIntStateOf(prefs.getInt("best_2048", 0)) }
    var moves by remember(challenge.id) { mutableIntStateOf(0) }
    var gameOver by remember(challenge.id) { mutableStateOf(false) }
    var claimed by remember(challenge.id) { mutableStateOf(false) }

    val maxTile = grid.flatten().maxOrNull() ?: 2
    val targetTile = challenge.targetTile.coerceAtLeast(128)
    val missionReady = maxTile >= targetTile

    fun moveDir(dir: String) {
        if (disabled || gameOver) return
        val working = grid.map { it.toMutableList() }
        var changed = false
        var totalGained = 0

        if (dir == "LEFT" || dir == "RIGHT") {
            for (r in 0..3) {
                val src = if (dir == "LEFT") working[r].toList() else working[r].reversed()
                val (res, g) = slideRowLeft(src)
                val restored = if (dir == "LEFT") res else res.reversed()
                for (c in 0..3) {
                    if (working[r][c] != restored[c]) changed = true
                    working[r][c] = restored[c]
                }
                totalGained += g
            }
        } else {
            for (c in 0..3) {
                val col = listOf(working[0][c], working[1][c], working[2][c], working[3][c])
                val src = if (dir == "UP") col else col.reversed()
                val (res, g) = slideRowLeft(src)
                val restored = if (dir == "UP") res else res.reversed()
                for (r in 0..3) {
                    if (working[r][c] != restored[r]) changed = true
                    working[r][c] = restored[r]
                }
                totalGained += g
            }
        }

        if (!changed) return
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        if (totalGained > 0) onPlayGood() else onPlayTap()

        prevGrid = grid.map { it.toList() } to score

        val empties = mutableListOf<Pair<Int, Int>>()
        for (r in 0..3) for (c in 0..3) if (working[r][c] == 0) empties.add(r to c)
        if (empties.isNotEmpty()) {
            val (sr, sc) = empties[nextRand() % empties.size]
            working[sr][sc] = if (nextRand() % 8 == 0) 4 else 2
        }

        val nextScore = score + totalGained
        if (nextScore > bestScore) {
            bestScore = nextScore
            prefs.edit().putInt("best_2048", bestScore).apply()
        }

        grid = working
        score = nextScore
        moves += 1
        if (!has2048Moves(working)) gameOver = true
    }

    fun submitClaim() {
        if (claimed || disabled) return
        claimed = true
        val pct = ((maxTile.toFloat() / targetTile.toFloat()) * 100f).roundToInt().coerceIn(40, 100)
        val combo = (ln(max(2.0, maxTile.toDouble())) / ln(2.0)).roundToInt().coerceIn(1, 10)
        onFinish(pct, 100, combo)
    }

    var dragDx = 0f
    var dragDy = 0f

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = surfaceColor,
            border = BorderStroke(1.dp, borderColor),
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Column {
                        Text("SKOR", color = textSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text("$score", color = Color(0xFF8B5CF6), fontSize = 19.sp, fontWeight = FontWeight.Black)
                    }
                    Column {
                        Text("UBIN", color = textSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text("$maxTile / $targetTile", color = Color(0xFF10B981), fontSize = 15.sp, fontWeight = FontWeight.Black)
                    }
                    Column {
                        Text("REKOR", color = textSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text("$bestScore", color = Color(0xFFF59E0B), fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }

                Button(
                    onClick = { submitClaim() },
                    enabled = missionReady && !disabled && !claimed,
                    shape = RoundedCornerShape(11.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF10B981),
                        contentColor = Color.White,
                        disabledContainerColor = elevatedColor,
                        disabledContentColor = textSecondary,
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Icon(Icons.Default.WorkspacePremium, contentDescription = null, modifier = Modifier.size(14.dp))
                    Text(
                        text = if (missionReady) " Klaim +XP" else " Capai $targetTile",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        softWrap = false,
                    )
                }
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = surfaceColor,
            border = BorderStroke(1.dp, borderColor),
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = elevatedColor,
                    border = BorderStroke(1.dp, borderColor),
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .pointerInput(challenge.id, gameOver) {
                            detectDragGestures(
                                onDragStart = {
                                    dragDx = 0f
                                    dragDy = 0f
                                },
                                onDrag = { change, amount ->
                                    change.consume()
                                    dragDx += amount.x
                                    dragDy += amount.y
                                },
                                onDragEnd = {
                                    if (max(abs(dragDx), abs(dragDy)) > 36f) {
                                        if (abs(dragDx) > abs(dragDy)) {
                                            moveDir(if (dragDx > 0) "RIGHT" else "LEFT")
                                        } else {
                                            moveDir(if (dragDy > 0) "DOWN" else "UP")
                                        }
                                    }
                                },
                            )
                        },
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        for (r in 0..3) {
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                for (c in 0..3) {
                                    val v = grid[r][c]
                                    val (bg, fg) = tileColors(v, surfaceColor, textPrimary)
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(13.dp))
                                            .background(if (v > 0) Color.Black.copy(alpha = 0.28f) else surfaceColor)
                                            .padding(bottom = if (v > 0) 4.dp else 0.dp)
                                            .clip(RoundedCornerShape(13.dp))
                                            .background(bg)
                                            .border(
                                                width = if (v == 0) 1.dp else 1.5.dp,
                                                color = if (v == 0) borderColor else Color.White.copy(alpha = 0.4f),
                                                shape = RoundedCornerShape(13.dp),
                                            ),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        if (v > 0) {
                                            Text(
                                                text = v.toString(),
                                                color = fg,
                                                fontSize = if (v >= 1024) 18.sp else if (v >= 128) 21.sp else 24.sp,
                                                fontWeight = FontWeight.Black,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Text(
                    text = "Geser papan atau gunakan tombol arah ($moves langkah):",
                    color = textSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                )

                // D-pad buttons
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = elevatedColor,
                        border = BorderStroke(1.dp, borderColor),
                        modifier = Modifier
                            .size(width = 56.dp, height = 42.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { moveDir("UP") },
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Atas", tint = textPrimary)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = elevatedColor,
                            border = BorderStroke(1.dp, borderColor),
                            modifier = Modifier
                                .size(width = 56.dp, height = 42.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { moveDir("LEFT") },
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Kiri", tint = textPrimary)
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = elevatedColor,
                            border = BorderStroke(1.dp, borderColor),
                            modifier = Modifier
                                .size(width = 56.dp, height = 42.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { moveDir("DOWN") },
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Bawah", tint = textPrimary)
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = elevatedColor,
                            border = BorderStroke(1.dp, borderColor),
                            modifier = Modifier
                                .size(width = 56.dp, height = 42.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { moveDir("RIGHT") },
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Kanan", tint = textPrimary)
                            }
                        }
                    }
                }

                if (gameOver) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF43F5E).copy(alpha = 0.14f),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text("Tidak Ada Langkah Tersisa! Skor: $score", color = Color(0xFFF43F5E), fontSize = 13.sp, fontWeight = FontWeight.Black)
                            Button(
                                onClick = { submitClaim() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                shape = RoundedCornerShape(10.dp),
                            ) {
                                Text("Simpan Skor & XP", fontSize = 11.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = elevatedColor,
                        border = BorderStroke(1.dp, borderColor),
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(enabled = prevGrid != null) {
                                prevGrid?.let { (pg, ps) ->
                                    grid = pg
                                    score = ps
                                    prevGrid = null
                                    gameOver = false
                                }
                            },
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = null, tint = textPrimary, modifier = Modifier.size(13.dp))
                            Text(" Undo", color = if (prevGrid != null) textPrimary else textSecondary, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = elevatedColor,
                        border = BorderStroke(1.dp, borderColor),
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                grid = buildStart()
                                prevGrid = null
                                score = 0
                                moves = 0
                                gameOver = false
                            },
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = textSecondary, modifier = Modifier.size(13.dp))
                            Text(" Ulangi", color = textPrimary, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF8B5CF6).copy(alpha = 0.14f),
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onNewLevel() },
                    ) {
                        Text(
                            text = "Level Baru",
                            color = Color(0xFF8B5CF6),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        )
                    }
                }
            }
        }
    }
}
