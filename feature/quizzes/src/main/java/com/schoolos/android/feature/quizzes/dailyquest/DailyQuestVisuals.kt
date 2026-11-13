package com.schoolos.android.feature.quizzes.dailyquest

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.schoolos.android.domain.model.DailyQuestChallenge
import com.schoolos.android.domain.model.MapPinItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/* ─── Cute Shiba Mascot Avatar (Matches Mockup Top-Right & Corner Shiba) ─── */
@Composable
fun ShibaMascotAvatar(
    size: Dp = 36.dp,
    happy: Boolean = true,
) {
    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val s = w / 64f

        drawCircle(color = Color(0xFFE0F2FE), radius = 30f * s)
        drawCircle(color = Color(0xFF38BDF8), radius = 30f * s, style = Stroke(width = 3f * s))

        // Left Ear
        val leftEar = Path().apply {
            moveTo(13f * s, 24f * s)
            lineTo(18f * s, 9f * s)
            lineTo(28f * s, 19f * s)
            close()
        }
        drawPath(leftEar, color = Color(0xFFF59E0B))
        // Right Ear
        val rightEar = Path().apply {
            moveTo(51f * s, 24f * s)
            lineTo(46f * s, 9f * s)
            lineTo(36f * s, 19f * s)
            close()
        }
        drawPath(rightEar, color = Color(0xFFF59E0B))

        // Head
        drawOval(
            color = Color(0xFFF59E0B),
            topLeft = Offset(12f * s, 17f * s),
            size = Size(40f * s, 36f * s),
        )
        // Cream lower muzzle
        drawOval(
            color = Color(0xFFFEF3C7),
            topLeft = Offset(15f * s, 31f * s),
            size = Size(34f * s, 21f * s),
        )

        // Eyes
        if (happy) {
            drawCircle(color = Color(0xFF1E293B), radius = 2.7f * s, center = Offset(24f * s, 30f * s))
            drawCircle(color = Color(0xFF1E293B), radius = 2.7f * s, center = Offset(40f * s, 30f * s))
            drawCircle(color = Color.White, radius = 0.9f * s, center = Offset(25f * s, 29f * s))
            drawCircle(color = Color.White, radius = 0.9f * s, center = Offset(41f * s, 29f * s))
        } else {
            drawLine(Color(0xFF1E293B), Offset(21f * s, 29f * s), Offset(26f * s, 31f * s), strokeWidth = 2f * s, cap = StrokeCap.Round)
            drawLine(Color(0xFF1E293B), Offset(43f * s, 29f * s), Offset(38f * s, 31f * s), strokeWidth = 2f * s, cap = StrokeCap.Round)
        }

        // Rosy blush
        drawOval(color = Color(0xFFFB7185).copy(alpha = 0.65f), topLeft = Offset(16f * s, 34f * s), size = Size(6f * s, 3.6f * s))
        drawOval(color = Color(0xFFFB7185).copy(alpha = 0.65f), topLeft = Offset(42f * s, 34f * s), size = Size(6f * s, 3.6f * s))

        // Nose & Smile
        drawOval(color = Color(0xFF1E293B), topLeft = Offset(28.8f * s, 35f * s), size = Size(6.4f * s, 4.4f * s))
        drawArc(
            color = if (happy) Color(0xFFFB7185) else Color(0xFF1E293B),
            startAngle = 10f,
            sweepAngle = 160f,
            useCenter = happy,
            topLeft = Offset(27f * s, 39f * s),
            size = Size(10f * s, 6f * s),
            style = Stroke(width = 2f * s, cap = StrokeCap.Round),
        )
    }
}

/* ─── Cartoon Topic Illustration Drawn Crisp with Compose Canvas ─── */
@Composable
fun TopicCartoonIllustrationCanvas(topicText: String) {
    val lower = topicText.lowercase()
    val kind = when {
        "05.00" in lower || "bangun" in lower || "wakes" in lower || "jam" in lower -> "CLOCK"
        "fotosintesis" in lower || "oksigen" in lower || "tanaman" in lower || "ipa" in lower || "biologi" in lower -> "LEAF"
        "ekonomi" in lower || "inflasi" in lower || "diskon" in lower || "rupiah" in lower || "uang" in lower -> "ECON"
        else -> "CLASS"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(126.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                when (kind) {
                    "LEAF" -> Color(0xFFECFDF5)
                    "ECON" -> Color(0xFFFFFBEB)
                    "CLOCK" -> Color(0xFFDBEAFE)
                    else -> Color(0xFFF0F9FF)
                }
            ),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            when (kind) {
                "LEAF" -> {
                    drawCircle(Color(0xFFFACC15), radius = h * 0.16f, center = Offset(w * 0.17f, h * 0.25f))
                    drawLine(
                        Color(0xFFF59E0B),
                        Offset(w * 0.27f, h * 0.30f),
                        Offset(w * 0.42f, h * 0.42f),
                        strokeWidth = 5f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)),
                    )
                    val leaf = Path().apply {
                        moveTo(w * 0.40f, h * 0.65f)
                        cubicTo(w * 0.40f, h * 0.34f, w * 0.62f, h * 0.26f, w * 0.74f, h * 0.38f)
                        cubicTo(w * 0.70f, h * 0.68f, w * 0.54f, h * 0.78f, w * 0.40f, h * 0.65f)
                    }
                    drawPath(leaf, color = Color(0xFF22C55E))
                    drawLine(Color(0xFF15803D), Offset(w * 0.40f, h * 0.65f), Offset(w * 0.38f, h * 0.90f), strokeWidth = 8f, cap = StrokeCap.Round)
                    drawCircle(Color(0xFF38BDF8), radius = h * 0.12f, center = Offset(w * 0.84f, h * 0.32f))
                }
                "ECON" -> {
                    drawRoundRect(
                        color = Color(0xFFFEF3C7),
                        topLeft = Offset(w * 0.10f, h * 0.16f),
                        size = Size(w * 0.52f, h * 0.52f),
                        cornerRadius = CornerRadius(18f, 18f),
                    )
                    val chart = Path().apply {
                        moveTo(w * 0.16f, h * 0.54f)
                        lineTo(w * 0.27f, h * 0.40f)
                        lineTo(w * 0.37f, h * 0.46f)
                        lineTo(w * 0.54f, h * 0.26f)
                    }
                    drawPath(chart, color = Color(0xFF10B981), style = Stroke(width = 7f, cap = StrokeCap.Round))
                    drawCircle(Color(0xFFFACC15), radius = h * 0.14f, center = Offset(w * 0.24f, h * 0.80f))
                    // Student
                    drawCircle(Color(0xFFFED7AA), radius = h * 0.14f, center = Offset(w * 0.78f, h * 0.38f))
                    drawRoundRect(
                        color = Color(0xFF0EA5E9),
                        topLeft = Offset(w * 0.70f, h * 0.54f),
                        size = Size(w * 0.16f, h * 0.36f),
                        cornerRadius = CornerRadius(14f, 14f),
                    )
                }
                else -> {
                    // Student + speech board
                    drawRoundRect(
                        color = Color(0xFFFEF9C3),
                        topLeft = Offset(w * 0.08f, h * 0.14f),
                        size = Size(w * 0.50f, h * 0.55f),
                        cornerRadius = CornerRadius(18f, 18f),
                    )
                    drawRoundRect(
                        color = Color(0xFF94A3B8),
                        topLeft = Offset(w * 0.08f, h * 0.14f),
                        size = Size(w * 0.50f, h * 0.55f),
                        cornerRadius = CornerRadius(18f, 18f),
                        style = Stroke(width = 3f),
                    )
                    drawCircle(Color(0xFFFED7AA), radius = h * 0.15f, center = Offset(w * 0.76f, h * 0.38f))
                    drawArc(Color(0xFF1E293B), startAngle = 180f, sweepAngle = 180f, useCenter = true, topLeft = Offset(w * 0.76f - h * 0.15f, h * 0.22f), size = Size(h * 0.30f, h * 0.22f))
                    drawRoundRect(
                        color = Color.White,
                        topLeft = Offset(w * 0.68f, h * 0.55f),
                        size = Size(w * 0.16f, h * 0.36f),
                        cornerRadius = CornerRadius(12f, 12f),
                    )
                    drawRoundRect(
                        color = Color(0xFF0EA5E9),
                        topLeft = Offset(w * 0.73f, h * 0.55f),
                        size = Size(w * 0.06f, h * 0.24f),
                        cornerRadius = CornerRadius(6f, 6f),
                    )
                }
            }
        }
        if (kind == "CLASS" || kind == "CLOCK") {
            Text(
                text = if (kind == "CLOCK") "05:00" else "BENAR / SALAH?",
                color = if (kind == "CLOCK") Color(0xFF1D4ED8) else Color(0xFF0284C7),
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 26.dp, top = 40.dp),
            )
        }
    }
}

/* ═══════════════════════════════════════════════════════════════════════════
   1. SWIPE CARD ARENA (Matches Screenshot 3/4 Top: Horizontal Gesture Drag!)
   ═══════════════════════════════════════════════════════════════════════════ */
@Composable
fun SwipeCardArenaCompose(
    challenge: DailyQuestChallenge.FactOrMyth,
    feedback: QuestFeedback?,
    elevatedColor: Color,
    borderColor: Color,
    textPrimary: Color,
    textSecondary: Color,
    onSwipeAnswer: (Boolean) -> Unit,
) {
    var dragOffsetX by remember(challenge.id) { mutableFloatStateOf(0f) }
    val enabled = feedback == null

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Left: INCORRECT
            SwipeSideButton(
                label = "INCORRECT",
                icon = Icons.Default.Close,
                accent = when {
                    feedback == null -> Color(0xFFF43F5E)
                    !challenge.isFact -> Color(0xFF10B981)
                    feedback.selectedBoolAnswer == false -> Color(0xFFF43F5E)
                    else -> Color(0xFF94A3B8)
                },
                highlighted = dragOffsetX < -25f,
                enabled = enabled,
                onClick = { onSwipeAnswer(false) },
            )

            // Center Swiping Card
            val swipingRight = dragOffsetX > 25f
            val swipingLeft = dragOffsetX < -25f
            val cardBorder = when {
                swipingRight -> Color(0xFF10B981)
                swipingLeft -> Color(0xFFF43F5E)
                else -> borderColor
            }

            Surface(
                modifier = Modifier
                    .weight(1f)
                    .graphicsLayer {
                        translationX = dragOffsetX * 0.65f
                        rotationZ = (dragOffsetX * 0.035f).coerceIn(-12f, 12f)
                    }
                    .pointerInput(challenge.id, enabled) {
                        if (!enabled) return@pointerInput
                        detectHorizontalDragGestures(
                            onHorizontalDrag = { _, delta ->
                                dragOffsetX = (dragOffsetX + delta).coerceIn(-220f, 220f)
                            },
                            onDragEnd = {
                                when {
                                    dragOffsetX > 85f -> {
                                        dragOffsetX = 0f
                                        onSwipeAnswer(true)
                                    }
                                    dragOffsetX < -85f -> {
                                        dragOffsetX = 0f
                                        onSwipeAnswer(false)
                                    }
                                    else -> dragOffsetX = 0f
                                }
                            },
                            onDragCancel = { dragOffsetX = 0f },
                        )
                    },
                shape = RoundedCornerShape(22.dp),
                color = cardBorder,
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp),
                    shape = RoundedCornerShape(22.dp),
                    color = elevatedColor,
                    border = BorderStroke(1.5.dp, cardBorder),
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        TopicCartoonIllustrationCanvas("${challenge.topicTag.orEmpty()} ${challenge.statement}")
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF0EA5E9).copy(alpha = 0.14f),
                        ) {
                            Text(
                                text = "PERNYATAAN${challenge.topicTag?.let { " • ${it.uppercase()}" } ?: ""}",
                                color = Color(0xFF0EA5E9),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            )
                        }
                        Text(
                            text = challenge.statement,
                            color = textPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            lineHeight = 18.sp,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }

            // Right: CORRECT
            SwipeSideButton(
                label = "CORRECT",
                icon = Icons.Default.Check,
                accent = when {
                    feedback == null -> Color(0xFF10B981)
                    challenge.isFact -> Color(0xFF10B981)
                    feedback.selectedBoolAnswer == true -> Color(0xFFF43F5E)
                    else -> Color(0xFF94A3B8)
                },
                highlighted = dragOffsetX > 25f,
                enabled = enabled,
                onClick = { onSwipeAnswer(true) },
            )
        }

        Text(
            text = "Geser kartu ke Kanan untuk Benar, Kiri untuk Salah (atau tap).",
            color = textSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun SwipeSideButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color,
    highlighted: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(2.dp),
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = accent.copy(alpha = if (highlighted) 1f else 0.9f),
            border = if (highlighted) BorderStroke(2.dp, Color.White) else null,
            shadowElevation = if (highlighted) 6.dp else 2.dp,
        ) {
            Box(
                modifier = Modifier.size(44.dp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(imageVector = icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(24.dp))
            }
        }
        Text(
            text = label,
            color = accent,
            fontSize = 8.sp,
            fontWeight = FontWeight.Black,
        )
    }
}

/* ═══════════════════════════════════════════════════════════════════════════
   2. DUAL-COLUMN STEP BUILDER (Matches Mockup Screenshot 2: Langkah 1..4)
   ═══════════════════════════════════════════════════════════════════════════ */
@Composable
fun DualColumnStepBuilderCompose(
    challenge: DailyQuestChallenge.SequenceOrder,
    shuffledPool: List<SequenceStepItem>,
    disabled: Boolean,
    surfaceColor: Color,
    elevatedColor: Color,
    borderColor: Color,
    textPrimary: Color,
    textSecondary: Color,
    onWrongTap: () -> Unit,
    onAllCompleted: () -> Unit,
) {
    var filledCount by remember(challenge.id) { mutableIntStateOf(0) }
    var wrongItemIdx by remember(challenge.id) { mutableStateOf<Int?>(null) }
    val scope = rememberCoroutineScope()

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Prompt Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = elevatedColor,
            border = BorderStroke(1.5.dp, borderColor),
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                val tag = challenge.topicTag
                if (!tag.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF8B5CF6).copy(alpha = 0.16f),
                    ) {
                        Text(
                            text = tag.uppercase(),
                            color = Color(0xFF8B5CF6),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }
                }
                Text(
                    text = challenge.prompt,
                    color = textPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp,
                )
            }
        }

        // Dual Column: Left Slots vs Right Pool
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top,
        ) {
            // LEFT: Langkah 1 ↓ Langkah 2 ↓ Langkah 3 ↓ Langkah 4
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                challenge.steps.forEachIndexed { idx, stepText ->
                    val isDone = idx < filledCount
                    val isActive = idx == filledCount && !disabled
                    Text(
                        text = "Langkah ${idx + 1}",
                        color = textSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = when {
                            isDone -> Color(0xFF10B981).copy(alpha = 0.12f)
                            isActive -> Color(0xFF0EA5E9).copy(alpha = 0.10f)
                            else -> elevatedColor.copy(alpha = 0.7f)
                        },
                        border = BorderStroke(
                            width = 1.5.dp,
                            color = when {
                                isDone -> Color(0xFF10B981)
                                isActive -> Color(0xFF0EA5E9)
                                else -> borderColor
                            },
                        ),
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            if (isDone) {
                                Text(
                                    text = stepText,
                                    color = textPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 15.sp,
                                )
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFF10B981).copy(alpha = 0.2f),
                                ) {
                                    Text(
                                        text = "✓ BENAR",
                                        color = Color(0xFF10B981),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    )
                                }
                            } else {
                                Text(
                                    text = if (isActive) "Tap kartu →" else "Kosong",
                                    color = if (isActive) Color(0xFF0EA5E9) else textSecondary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }

                    if (idx < challenge.steps.lastIndex) {
                        Icon(
                            imageVector = Icons.Default.ArrowDownward,
                            contentDescription = null,
                            tint = textSecondary.copy(alpha = 0.6f),
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
            }

            // RIGHT: Randomized Bank
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(18.dp),
                color = elevatedColor,
                border = BorderStroke(1.5.dp, borderColor),
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "Pilih Langkah ${(filledCount + 1).coerceAtMost(challenge.steps.size)}",
                        color = textSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    shuffledPool.forEach { item ->
                        val isPlaced = item.origIdx < filledCount
                        val isWrong = wrongItemIdx == item.origIdx

                        if (isPlaced) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFF10B981).copy(alpha = 0.08f),
                                border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.3f)),
                            ) {
                                Text(
                                    text = "✓ Terpasang #${item.origIdx + 1}",
                                    color = Color(0xFF10B981),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 12.dp, horizontal = 6.dp),
                                )
                            }
                        } else {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = !disabled) {
                                        if (item.origIdx == filledCount) {
                                            val next = filledCount + 1
                                            filledCount = next
                                            if (next >= challenge.steps.size) {
                                                onAllCompleted()
                                            }
                                        } else {
                                            wrongItemIdx = item.origIdx
                                            onWrongTap()
                                            scope.launch {
                                                delay(450)
                                                wrongItemIdx = null
                                            }
                                        }
                                    },
                                shape = RoundedCornerShape(14.dp),
                                color = if (isWrong) Color(0xFFF43F5E) else borderColor,
                            ) {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 3.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (isWrong) Color(0xFFF43F5E).copy(alpha = 0.16f) else surfaceColor,
                                    border = BorderStroke(1.5.dp, if (isWrong) Color(0xFFF43F5E) else borderColor),
                                ) {
                                    Text(
                                        text = item.text,
                                        color = if (isWrong) Color(0xFFF43F5E) else textPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 15.sp,
                                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
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

/* ═══════════════════════════════════════════════════════════════════════════
   3. MAP PIN & SCENE HOTSPOT ARENA (Matches Screenshot 1 & Middle 3/4!)
   ═══════════════════════════════════════════════════════════════════════════ */
@Composable
fun MapPinHuntArenaCompose(
    challenge: DailyQuestChallenge.MapPinHunt,
    disabled: Boolean,
    surfaceColor: Color,
    elevatedColor: Color,
    borderColor: Color,
    textPrimary: Color,
    textSecondary: Color,
    onWrongTap: () -> Unit,
    onCompleted: (String) -> Unit,
) {
    var unlockedIds by remember(challenge.id) { mutableStateOf(emptySet<String>()) }
    var wrongPinId by remember(challenge.id) { mutableStateOf<String?>(null) }
    var selectedMapPinId by remember(challenge.id) { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val isMapTags = challenge.interactionType == "MAP_TAGS"
    val pins = challenge.pins
    val currentStepIdx = unlockedIds.size
    val currentTargetPin = if (isMapTags) pins.getOrNull(currentStepIdx) else pins.firstOrNull { it.isTarget } ?: pins.firstOrNull()

    fun triggerWrong(pinId: String) {
        wrongPinId = pinId
        onWrongTap()
        scope.launch {
            delay(450)
            wrongPinId = null
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Framed Prompt Card (Matches Screenshot 1)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = elevatedColor,
            border = BorderStroke(1.5.dp, borderColor),
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                val tag = challenge.topicTag
                if (!tag.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF10B981).copy(alpha = 0.16f),
                    ) {
                        Text(
                            text = tag.uppercase(),
                            color = Color(0xFF10B981),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }
                }
                Text(
                    text = challenge.prompt,
                    color = textPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                    lineHeight = 19.sp,
                )
            }
        }

        // Illustrated Map / Scene Box with Exact Coordinate Pin Overlays
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(235.dp)
                .clip(RoundedCornerShape(22.dp)),
        ) {
            val boxWidthPx = constraints.maxWidth.toFloat()
            val boxHeightPx = constraints.maxHeight.toFloat()

            SceneBackdropCanvas(sceneId = challenge.sceneId, modifier = Modifier.fillMaxSize())

            pins.forEachIndexed { idx, pin ->
                val isUnlocked = pin.id in unlockedIds
                val isWrong = wrongPinId == pin.id
                val isSelected = selectedMapPinId == pin.id || (isMapTags && idx == currentStepIdx)

                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                x = (pin.x * boxWidthPx).roundToInt() - 42,
                                y = (pin.y * boxHeightPx).roundToInt() - 42,
                            )
                        }
                        .clickable(enabled = !disabled) {
                            if (isMapTags) {
                                selectedMapPinId = if (selectedMapPinId == pin.id) null else pin.id
                            } else {
                                if (pin.isTarget) {
                                    unlockedIds = unlockedIds + pin.id
                                    onCompleted("Tepat sekali! Titik \"${pin.label}\" ditemukan.")
                                } else {
                                    triggerWrong(pin.id)
                                }
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    if (isMapTags) {
                        if (isUnlocked) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color.White.copy(alpha = 0.95f),
                                border = BorderStroke(1.5.dp, Color(0xFF10B981)),
                                shadowElevation = 4.dp,
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(12.dp))
                                    Text(
                                        text = pin.label,
                                        color = Color(0xFF0F172A),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        maxLines = 1,
                                    )
                                }
                            }
                        } else {
                            Surface(
                                shape = CircleShape,
                                color = if (isSelected) Color(0xFF0EA5E9) else Color(0xFF0369A1).copy(alpha = 0.85f),
                                border = BorderStroke(2.dp, Color.White),
                                shadowElevation = 4.dp,
                                modifier = Modifier.size(34.dp),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Lock, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                                }
                            }
                        }
                    } else {
                        Surface(
                            shape = CircleShape,
                            color = when {
                                isUnlocked -> Color(0xFF10B981)
                                isWrong -> Color(0xFFF43F5E)
                                else -> Color(0xFFEF4444).copy(alpha = 0.88f)
                            },
                            border = BorderStroke(2.dp, Color.White),
                            shadowElevation = 5.dp,
                            modifier = Modifier.size(36.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isUnlocked) Icons.Default.Check else Icons.Default.MyLocation,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }
                }
            }

            // Floating Checklist Card Bottom-Left for MAP_TAGS (Exact Mockup Detail!)
            if (isMapTags) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(10.dp)
                        .widthIn(max = 220.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White.copy(alpha = 0.94f),
                    border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                    shadowElevation = 3.dp,
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        pins.forEachIndexed { idx, pin ->
                            val done = pin.id in unlockedIds
                            val wip = idx == currentStepIdx && !done
                            Text(
                                text = "L${idx + 1}: ${pin.stepLabel ?: pin.label}${if (done) " -> BENAR" else if (wip) " -> [PILIH]" else ""}",
                                color = when {
                                    done -> Color(0xFF059669)
                                    wip -> Color(0xFF0284C7)
                                    else -> Color(0xFF334155)
                                },
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }

        // Bottom 2x2 Tag Chips (Screenshot 1)
        if (isMapTags) {
            Text(
                text = if (currentStepIdx < pins.size) "Tap tag lokasi yang tepat untuk Langkah ${currentStepIdx + 1}:" else "Semua lokasi terpasang!",
                color = textSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                pins.chunked(2).forEach { pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        pair.forEach { pin ->
                            val done = pin.id in unlockedIds
                            val isWrong = wrongPinId == pin.id
                            val targetPinId = selectedMapPinId ?: currentTargetPin?.id
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable(enabled = !disabled && !done) {
                                        if (pin.id == targetPinId) {
                                            val next = unlockedIds + pin.id
                                            unlockedIds = next
                                            selectedMapPinId = null
                                            if (next.size >= pins.size) {
                                                onCompleted("Semua pin lokasi berhasil disematkan dengan tepat!")
                                            }
                                        } else {
                                            triggerWrong(pin.id)
                                        }
                                    },
                                shape = RoundedCornerShape(14.dp),
                                color = if (done) Color(0xFF10B981) else if (isWrong) Color(0xFFF43F5E) else borderColor,
                            ) {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 3.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    color = when {
                                        done -> Color(0xFF10B981).copy(alpha = 0.14f)
                                        isWrong -> Color(0xFFF43F5E).copy(alpha = 0.14f)
                                        else -> surfaceColor
                                    },
                                    border = BorderStroke(
                                        1.5.dp,
                                        if (done) Color(0xFF10B981) else if (isWrong) Color(0xFFF43F5E) else borderColor,
                                    ),
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(26.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (done) Color(0xFF10B981) else Color(0xFFF59E0B).copy(alpha = 0.16f)),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Icon(
                                                imageVector = if (done) Icons.Default.Check else Icons.Default.AccountBalance,
                                                contentDescription = null,
                                                tint = if (done) Color.White else Color(0xFFD97706),
                                                modifier = Modifier.size(14.dp),
                                            )
                                        }
                                        Text(
                                            text = pin.label,
                                            color = textPrimary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            lineHeight = 14.sp,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Default.Place, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(14.dp))
                Text(
                    text = " Ketuk langsung target merah di dalam gambar!",
                    color = textSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
            }
        }
    }
}

@Composable
private fun SceneBackdropCanvas(sceneId: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            when (sceneId) {
                "proklamasi_1945" -> {
                    drawRect(Color(0xFFBAE6FD))
                    drawCircle(Color(0xFF4ADE80), radius = h * 0.22f, center = Offset(w * 0.12f, h * 0.48f))
                    drawCircle(Color(0xFF22C55E), radius = h * 0.24f, center = Offset(w * 0.88f, h * 0.46f))
                    // Roof & Porch
                    val roof = Path().apply {
                        moveTo(w * 0.25f, h * 0.28f)
                        lineTo(w * 0.50f, h * 0.12f)
                        lineTo(w * 0.75f, h * 0.28f)
                        close()
                    }
                    drawPath(roof, Color(0xFFE2E8F0))
                    drawRect(Color(0xFFF8FAFC), topLeft = Offset(w * 0.28f, h * 0.28f), size = Size(w * 0.44f, h * 0.36f))
                    // Lawn
                    drawRect(Color(0xFF86EFAC), topLeft = Offset(0f, h * 0.64f), size = Size(w, h * 0.36f))
                    // Flagpole + Merah Putih
                    drawLine(Color(0xFF475569), Offset(w * 0.27f, h * 0.12f), Offset(w * 0.27f, h * 0.66f), strokeWidth = 5f)
                    drawRect(Color(0xFFEF4444), topLeft = Offset(w * 0.275f, h * 0.13f), size = Size(w * 0.10f, h * 0.06f))
                    drawRect(Color.White, topLeft = Offset(w * 0.275f, h * 0.19f), size = Size(w * 0.10f, h * 0.06f))
                    // Podium Figure
                    drawCircle(Color(0xFFFED7AA), radius = h * 0.045f, center = Offset(w * 0.50f, h * 0.50f))
                    drawRoundRect(Color.White, topLeft = Offset(w * 0.475f, h * 0.55f), size = Size(w * 0.05f, h * 0.14f), cornerRadius = CornerRadius(6f, 6f))
                }
                "morning_routine_room" -> {
                    drawRect(Color(0xFFE0F2FE))
                    drawRect(Color(0xFFBFDBFE).copy(alpha = 0.5f), size = Size(w, h * 0.70f))
                    drawRoundRect(Color(0xFF7DD3FC), topLeft = Offset(w * 0.06f, h * 0.10f), size = Size(w * 0.19f, h * 0.30f), cornerRadius = CornerRadius(8f, 8f))
                    drawCircle(Color(0xFFFDE047), radius = h * 0.06f, center = Offset(w * 0.13f, h * 0.21f))
                    // Bed
                    drawRoundRect(Color(0xFF3B82F6), topLeft = Offset(w * 0.27f, h * 0.40f), size = Size(w * 0.35f, h * 0.27f), cornerRadius = CornerRadius(12f, 12f))
                    // Nightstand & Clock 05:00
                    drawRoundRect(Color(0xFFD97706), topLeft = Offset(w * 0.21f, h * 0.56f), size = Size(w * 0.13f, h * 0.16f), cornerRadius = CornerRadius(8f, 8f))
                    drawRoundRect(Color(0xFF0F172A), topLeft = Offset(w * 0.22f, h * 0.49f), size = Size(w * 0.12f, h * 0.09f), cornerRadius = CornerRadius(6f, 6f))
                    // Floor + Uniform + Soccer ball
                    drawRect(Color(0xFFFDE68A), topLeft = Offset(0f, h * 0.70f), size = Size(w, h * 0.30f))
                    drawRoundRect(Color.White, topLeft = Offset(w * 0.67f, h * 0.24f), size = Size(w * 0.11f, h * 0.17f), cornerRadius = CornerRadius(8f, 8f))
                    drawCircle(Color.White, radius = h * 0.08f, center = Offset(w * 0.80f, h * 0.80f))
                    drawCircle(Color(0xFF0F172A), radius = h * 0.08f, center = Offset(w * 0.80f, h * 0.80f), style = Stroke(width = 3f))
                }
                "photosynthesis_scene" -> {
                    drawRect(Color(0xFFE0F2FE))
                    drawCircle(Color(0xFFFACC15), radius = h * 0.13f, center = Offset(w * 0.24f, h * 0.24f))
                    drawRect(Color(0xFF92400E).copy(alpha = 0.85f), topLeft = Offset(0f, h * 0.72f), size = Size(w, h * 0.28f))
                    drawLine(Color(0xFF15803D), Offset(w * 0.49f, h * 0.72f), Offset(w * 0.49f, h * 0.40f), strokeWidth = 10f)
                    val leaf = Path().apply {
                        moveTo(w * 0.49f, h * 0.48f)
                        cubicTo(w * 0.49f, h * 0.30f, w * 0.65f, h * 0.25f, w * 0.74f, h * 0.36f)
                        cubicTo(w * 0.70f, h * 0.52f, w * 0.58f, h * 0.56f, w * 0.49f, h * 0.48f)
                    }
                    drawPath(leaf, Color(0xFF22C55E))
                    drawCircle(Color(0xFF38BDF8), radius = h * 0.10f, center = Offset(w * 0.78f, h * 0.34f))
                }
                else -> {
                    // Peta Jawa Barat - Cirebon (Screenshot 1 Map!)
                    drawRect(Color(0xFF7DD3FC)) // Laut Jawa
                    val land = Path().apply {
                        moveTo(0f, h * 0.32f)
                        cubicTo(w * 0.20f, h * 0.30f, w * 0.48f, h * 0.34f, w * 0.65f, h * 0.42f)
                        cubicTo(w * 0.80f, h * 0.48f, w * 0.90f, h * 0.46f, w, h * 0.44f)
                        lineTo(w, h)
                        lineTo(0f, h)
                        close()
                    }
                    drawPath(land, Color(0xFFD9F99D))
                    drawPath(land, Color(0xFF65A30D), style = Stroke(width = 4f))

                    // Cirebon Golden Highlighted Territory
                    val cirebon = Path().apply {
                        moveTo(w * 0.48f, h * 0.35f)
                        lineTo(w * 0.63f, h * 0.43f)
                        lineTo(w * 0.59f, h * 0.60f)
                        lineTo(w * 0.46f, h * 0.56f)
                        close()
                    }
                    drawPath(cirebon, Color(0xFFFDE68A))
                    drawPath(cirebon, Color(0xFFD97706), style = Stroke(width = 3.5f))

                    // Compass Top-Left
                    drawCircle(Color(0xFFFEF3C7), radius = h * 0.08f, center = Offset(w * 0.09f, h * 0.15f))
                    drawCircle(Color(0xFFB45309), radius = h * 0.08f, center = Offset(w * 0.09f, h * 0.15f), style = Stroke(width = 2.5f))
                    // Boat bottom right
                    drawRoundRect(Color(0xFF78350F), topLeft = Offset(w * 0.70f, h * 0.84f), size = Size(w * 0.14f, h * 0.06f), cornerRadius = CornerRadius(6f, 6f))
                }
            }
        }

        // Crisp City Labels for Cirebon Map
        if (sceneId == "cirebon_keraton_map") {
            Text("JAWA BARAT", color = Color(0xFF3F6212), fontSize = 9.sp, fontWeight = FontWeight.Black, modifier = Modifier.align(Alignment.TopStart).padding(start = 18.dp, top = 86.dp))
            Text("Laut Jawa", color = Color(0xFF0369A1), fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.TopEnd).padding(end = 28.dp, top = 22.dp))
            Text("JAWA TENGAH", color = Color(0xFF3F6212), fontSize = 9.sp, fontWeight = FontWeight.Black, modifier = Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 52.dp))
        } else if (sceneId == "proklamasi_1945") {
            Surface(
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 10.dp),
                shape = RoundedCornerShape(50),
                color = Color.White.copy(alpha = 0.94f),
            ) {
                Text(
                    text = "Jalan Pegangsaan Timur 56, Jakarta",
                    color = Color(0xFF0F172A),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                )
            }
        }
    }
}

/* ═══════════════════════════════════════════════════════════════════════════
   4. STORY-DRIVEN COMIC CHOICE (Matches Bottom of Screenshot 3 & 4!)
   ═══════════════════════════════════════════════════════════════════════════ */
@Composable
fun StoryComicArenaCompose(
    challenge: DailyQuestChallenge.CaseDetective,
    feedback: QuestFeedback?,
    surfaceColor: Color,
    elevatedColor: Color,
    borderColor: Color,
    textPrimary: Color,
    textSecondary: Color,
    onChooseOption: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Panel 1: Illustrated Situation Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = elevatedColor,
            border = BorderStroke(1.5.dp, borderColor),
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0F172A)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("1", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black)
                    }
                    Text(
                        text = (challenge.topicTag ?: "SITUASI CERITA").uppercase(),
                        color = Color(0xFF10B981),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                    )
                }

                TopicCartoonIllustrationCanvas("${challenge.topicTag.orEmpty()} ${challenge.scenario}")

                if (challenge.scenario.isNotBlank()) {
                    Text(
                        text = challenge.scenario,
                        color = textPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 17.sp,
                    )
                }
            }
        }

        // Panel 2: Interactive Comic Choice + Shiba Guide
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = elevatedColor,
            border = BorderStroke(1.5.dp, Color(0xFF0EA5E9).copy(alpha = 0.45f)),
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0EA5E9)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("2", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black)
                    }
                    Text(
                        text = "APA KEPUTUSAN TERBAIK?",
                        color = Color(0xFF0EA5E9),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                    )
                }

                Text(
                    text = challenge.question,
                    color = textPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    lineHeight = 18.sp,
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    challenge.options.forEachIndexed { idx, optText ->
                        val letter = ('A' + idx).toString()
                        val isSelected = feedback?.selectedOptionIndex == idx
                        val isAnswer = idx == challenge.correctIndex
                        val tileColor = when {
                            feedback == null -> surfaceColor
                            isAnswer -> Color(0xFF10B981)
                            isSelected -> Color(0xFFF43F5E)
                            else -> surfaceColor.copy(alpha = 0.6f)
                        }
                        val tileText = if (feedback != null && (isAnswer || isSelected)) Color.White else textPrimary

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = feedback == null) { onChooseOption(idx) },
                            shape = RoundedCornerShape(12.dp),
                            color = tileColor,
                            border = BorderStroke(
                                1.5.dp,
                                when {
                                    feedback == null -> borderColor
                                    isAnswer -> Color(0xFF10B981)
                                    isSelected -> Color(0xFFF43F5E)
                                    else -> borderColor.copy(alpha = 0.5f)
                                },
                            ),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    text = "$letter. $optText",
                                    color = tileText,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.weight(1f),
                                )
                                if (feedback != null && isAnswer) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                } else if (feedback != null && isSelected) {
                                    Icon(Icons.Default.Cancel, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "STORY-DRIVEN CHOICE",
                        color = textSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                    )
                    ShibaMascotAvatar(
                        size = 34.dp,
                        happy = feedback == null || feedback.selectedOptionIndex == challenge.correctIndex,
                    )
                }
            }
        }
    }
}

