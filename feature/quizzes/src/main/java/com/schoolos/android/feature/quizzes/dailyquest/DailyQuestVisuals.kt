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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Lock
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.schoolos.android.domain.model.DailyQuestChallenge
import com.schoolos.android.domain.model.MapPinItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/* ─── Cute Shiba Mascot Avatar (Top-Right Sky Circle Ring) ─── */
@Composable
fun ShibaMascotAvatar(
    size: Dp = 36.dp,
    happy: Boolean = true,
) {
    Canvas(modifier = Modifier.size(size)) {
        val s = this.size.width / 64f

        drawCircle(color = Color(0xFFBAE6FD), radius = 30f * s)
        drawCircle(color = Color(0xFF38BDF8), radius = 30f * s, style = Stroke(width = 3.5f * s))

        val leftEar = Path().apply {
            moveTo(13f * s, 24f * s)
            lineTo(18f * s, 9f * s)
            lineTo(28f * s, 19f * s)
            close()
        }
        drawPath(leftEar, color = Color(0xFFF59E0B))
        val rightEar = Path().apply {
            moveTo(51f * s, 24f * s)
            lineTo(46f * s, 9f * s)
            lineTo(36f * s, 19f * s)
            close()
        }
        drawPath(rightEar, color = Color(0xFFF59E0B))

        drawOval(
            color = Color(0xFFF59E0B),
            topLeft = Offset(12f * s, 17f * s),
            size = Size(40f * s, 36f * s),
        )
        drawOval(
            color = Color(0xFFFFFBEB),
            topLeft = Offset(15f * s, 31f * s),
            size = Size(34f * s, 21f * s),
        )

        // White eyebrows
        drawOval(color = Color(0xFFFEF3C7), topLeft = Offset(20.5f * s, 25f * s), size = Size(6f * s, 3.2f * s))
        drawOval(color = Color(0xFFFEF3C7), topLeft = Offset(37.5f * s, 25f * s), size = Size(6f * s, 3.2f * s))

        if (happy) {
            drawCircle(color = Color(0xFF1E293B), radius = 2.7f * s, center = Offset(24f * s, 31f * s))
            drawCircle(color = Color(0xFF1E293B), radius = 2.7f * s, center = Offset(40f * s, 31f * s))
            drawCircle(color = Color.White, radius = 0.9f * s, center = Offset(25f * s, 30f * s))
            drawCircle(color = Color.White, radius = 0.9f * s, center = Offset(41f * s, 30f * s))
        } else {
            drawLine(Color(0xFF1E293B), Offset(21f * s, 30f * s), Offset(26f * s, 32f * s), strokeWidth = 2f * s, cap = StrokeCap.Round)
            drawLine(Color(0xFF1E293B), Offset(43f * s, 30f * s), Offset(38f * s, 32f * s), strokeWidth = 2f * s, cap = StrokeCap.Round)
        }

        drawOval(color = Color(0xFFFB7185).copy(alpha = 0.65f), topLeft = Offset(16f * s, 35f * s), size = Size(6f * s, 3.6f * s))
        drawOval(color = Color(0xFFFB7185).copy(alpha = 0.65f), topLeft = Offset(42f * s, 35f * s), size = Size(6f * s, 3.6f * s))
        drawOval(color = Color(0xFF1E293B), topLeft = Offset(28.8f * s, 36f * s), size = Size(6.4f * s, 4.4f * s))
        drawArc(
            color = if (happy) Color(0xFFFB7185) else Color(0xFF1E293B),
            startAngle = 10f,
            sweepAngle = 160f,
            useCenter = happy,
            topLeft = Offset(27f * s, 40f * s),
            size = Size(10f * s, 6f * s),
            style = Stroke(width = 2f * s, cap = StrokeCap.Round),
        )
    }
}

/* ─── Cute Full-Body Standing Shiba Mascot (Bottom-Right Corner of 3-Panel Comic) ─── */
@Composable
fun ShibaFullBodyMascot(
    size: Dp = 54.dp,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(size)) {
        val s = this.size.width / 80f

        // Curled Tail
        drawOval(color = Color(0xFFF59E0B), topLeft = Offset(53f * s, 40f * s), size = Size(16f * s, 18f * s))

        // Body & Cream Chest
        drawOval(color = Color(0xFFF59E0B), topLeft = Offset(22f * s, 44f * s), size = Size(36f * s, 32f * s))
        drawOval(color = Color(0xFFFFFBEB), topLeft = Offset(29f * s, 48f * s), size = Size(22f * s, 24f * s))

        // Cheering Paws
        drawLine(Color(0xFFFFFBEB), Offset(24f * s, 54f * s), Offset(14f * s, 41f * s), strokeWidth = 8f * s, cap = StrokeCap.Round)
        drawLine(Color(0xFFFFFBEB), Offset(56f * s, 54f * s), Offset(66f * s, 41f * s), strokeWidth = 8f * s, cap = StrokeCap.Round)

        // Collar & Bell
        drawRoundRect(color = Color(0xFF0EA5E9), topLeft = Offset(27f * s, 46f * s), size = Size(26f * s, 5f * s), cornerRadius = CornerRadius(4f * s, 4f * s))
        drawCircle(color = Color(0xFFFACC15), radius = 3.2f * s, center = Offset(40f * s, 52f * s))

        // Ears
        val lEar = Path().apply { moveTo(18f * s, 24f * s); lineTo(22f * s, 7f * s); lineTo(34f * s, 18f * s); close() }
        val rEar = Path().apply { moveTo(62f * s, 24f * s); lineTo(58f * s, 7f * s); lineTo(46f * s, 18f * s); close() }
        drawPath(lEar, Color(0xFFF59E0B))
        drawPath(rEar, Color(0xFFF59E0B))

        // Head
        drawOval(color = Color(0xFFF59E0B), topLeft = Offset(18f * s, 13f * s), size = Size(44f * s, 36f * s))
        drawOval(color = Color(0xFFFFFBEB), topLeft = Offset(22f * s, 29f * s), size = Size(36f * s, 19f * s))
        drawCircle(color = Color(0xFF1E293B), radius = 2.6f * s, center = Offset(30f * s, 28f * s))
        drawCircle(color = Color(0xFF1E293B), radius = 2.6f * s, center = Offset(50f * s, 28f * s))
        drawOval(color = Color(0xFFFB7185).copy(alpha = 0.65f), topLeft = Offset(21f * s, 32f * s), size = Size(6f * s, 4f * s))
        drawOval(color = Color(0xFFFB7185).copy(alpha = 0.65f), topLeft = Offset(53f * s, 32f * s), size = Size(6f * s, 4f * s))
        drawOval(color = Color(0xFF1E293B), topLeft = Offset(36.5f * s, 32f * s), size = Size(7f * s, 4.5f * s))
        drawArc(color = Color(0xFFFB7185), startAngle = 0f, sweepAngle = 180f, useCenter = true, topLeft = Offset(34f * s, 36f * s), size = Size(12f * s, 7f * s))
    }
}

/* ─── Storybook Cartoon Illustrations for Swipe & Comic Panel 1 ─── */
@Composable
fun TopicCartoonIllustrationCanvas(
    topicText: String,
    speechQuote: String? = null,
    height: Dp = 112.dp,
) {
    val lower = topicText.lowercase()
    val kind = when {
        "05.00" in lower || "05:00" in lower || "bangun" in lower || "wakes" in lower -> "CLOCK"
        "fotosintesis" in lower || "oksigen" in lower || "daun" in lower || "tanaman" in lower || "kloroplas" in lower -> "LEAF"
        "ekonomi" in lower || "inflasi" in lower || "diskon" in lower || "rupiah" in lower || "usaha" in lower -> "ECON"
        else -> "CLASS"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(12.dp))
            .background(
                when (kind) {
                    "LEAF" -> Color(0xFFDBEAFE)
                    "ECON" -> Color(0xFFFEF3C7)
                    "CLOCK" -> Color(0xFFDCEEFB)
                    else -> Color(0xFFF1F5F9)
                }
            ),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            when (kind) {
                "CLOCK" -> {
                    // Striped wallpaper & sunny window
                    for (i in 1..8) {
                        val x = w * (i / 9f)
                        drawLine(Color(0xFFC5E1F8), Offset(x, 0f), Offset(x, h * 0.72f), strokeWidth = 10f)
                    }
                    drawRoundRect(Color(0xFF93C5FD), topLeft = Offset(w * 0.75f, h * 0.10f), size = Size(w * 0.20f, h * 0.36f), cornerRadius = CornerRadius(8f, 8f))
                    drawCircle(Color.White, radius = h * 0.14f, center = Offset(w * 0.17f, h * 0.25f))
                    drawCircle(Color(0xFF475569), radius = h * 0.14f, center = Offset(w * 0.17f, h * 0.25f), style = Stroke(width = 4f))
                    // Bed + blanket + digital 05.00
                    drawRoundRect(Color(0xFF3B82F6), topLeft = Offset(w * 0.08f, h * 0.60f), size = Size(w * 0.84f, h * 0.36f), cornerRadius = CornerRadius(14f, 14f))
                    drawRoundRect(Color.White, topLeft = Offset(w * 0.26f, h * 0.48f), size = Size(w * 0.48f, h * 0.16f), cornerRadius = CornerRadius(10f, 10f))
                    drawRoundRect(Color(0xFF1E293B), topLeft = Offset(w * 0.06f, h * 0.65f), size = Size(w * 0.22f, h * 0.16f), cornerRadius = CornerRadius(8f, 8f))
                    // Stretching kid
                    drawCircle(Color(0xFFFDBA74), radius = h * 0.14f, center = Offset(w * 0.50f, h * 0.34f))
                    drawArc(Color(0xFF1E293B), startAngle = 180f, sweepAngle = 180f, useCenter = true, topLeft = Offset(w * 0.50f - h * 0.14f, h * 0.19f), size = Size(h * 0.28f, h * 0.20f))
                    drawRoundRect(Color.White, topLeft = Offset(w * 0.41f, h * 0.48f), size = Size(w * 0.18f, h * 0.20f), cornerRadius = CornerRadius(10f, 10f))
                }
                "LEAF" -> {
                    // Hills + Sun rays + Detailed Leaf
                    drawRect(Color(0xFF86EFAC), topLeft = Offset(0f, h * 0.72f), size = Size(w, h * 0.28f))
                    drawCircle(Color(0xFFFACC15), radius = h * 0.16f, center = Offset(w * 0.17f, h * 0.24f))
                    for (deg in listOf(0f, 35f, 70f, 105f, 320f)) {
                        val rad = Math.toRadians(deg.toDouble())
                        drawLine(
                            Color(0xFFFBBF24),
                            Offset(w * 0.17f, h * 0.24f),
                            Offset(w * 0.17f + cos(rad).toFloat() * h * 0.26f, h * 0.24f + sin(rad).toFloat() * h * 0.26f),
                            strokeWidth = 4f,
                            cap = StrokeCap.Round,
                        )
                    }
                    val leaf = Path().apply {
                        moveTo(w * 0.42f, h * 0.68f)
                        cubicTo(w * 0.40f, h * 0.34f, w * 0.66f, h * 0.22f, w * 0.84f, h * 0.32f)
                        cubicTo(w * 0.80f, h * 0.68f, w * 0.58f, h * 0.82f, w * 0.42f, h * 0.68f)
                    }
                    drawPath(leaf, Color(0xFF22C55E))
                    drawPath(leaf, Color(0xFF14532D), style = Stroke(width = 4f))
                    drawLine(Color(0xFFDCFCE7), Offset(w * 0.42f, h * 0.68f), Offset(w * 0.80f, h * 0.35f), strokeWidth = 4f, cap = StrokeCap.Round)
                }
                "ECON" -> {
                    drawRoundRect(Color.White, topLeft = Offset(w * 0.08f, h * 0.14f), size = Size(w * 0.52f, h * 0.52f), cornerRadius = CornerRadius(14f, 14f))
                    val chart = Path().apply {
                        moveTo(w * 0.14f, h * 0.52f)
                        lineTo(w * 0.26f, h * 0.38f)
                        lineTo(w * 0.36f, h * 0.45f)
                        lineTo(w * 0.52f, h * 0.25f)
                    }
                    drawPath(chart, color = Color(0xFF10B981), style = Stroke(width = 6f, cap = StrokeCap.Round))
                    drawCircle(Color(0xFFFACC15), radius = h * 0.13f, center = Offset(w * 0.22f, h * 0.80f))
                    drawCircle(Color(0xFFFDBA74), radius = h * 0.14f, center = Offset(w * 0.78f, h * 0.36f))
                    drawRoundRect(Color(0xFF0EA5E9), topLeft = Offset(w * 0.69f, h * 0.52f), size = Size(w * 0.18f, h * 0.38f), cornerRadius = CornerRadius(12f, 12f))
                }
                else -> {
                    // Mockup 3 Top: Speech Board + Indonesian Schoolboy holding tablet!
                    drawRoundRect(Color(0xFFFEF9C3), topLeft = Offset(w * 0.06f, h * 0.10f), size = Size(w * 0.54f, h * 0.56f), cornerRadius = CornerRadius(14f, 14f))
                    drawRoundRect(Color(0xFF94A3B8), topLeft = Offset(w * 0.06f, h * 0.10f), size = Size(w * 0.54f, h * 0.56f), cornerRadius = CornerRadius(14f, 14f), style = Stroke(width = 3f))
                    drawCircle(Color(0xFFFDBA74), radius = h * 0.14f, center = Offset(w * 0.78f, h * 0.33f))
                    drawArc(Color(0xFF1E293B), startAngle = 180f, sweepAngle = 180f, useCenter = true, topLeft = Offset(w * 0.78f - h * 0.14f, h * 0.18f), size = Size(h * 0.28f, h * 0.20f))
                    drawRoundRect(Color.White, topLeft = Offset(w * 0.69f, h * 0.48f), size = Size(w * 0.18f, h * 0.28f), cornerRadius = CornerRadius(10f, 10f))
                    drawRoundRect(Color(0xFF0284C7), topLeft = Offset(w * 0.75f, h * 0.48f), size = Size(w * 0.06f, h * 0.20f), cornerRadius = CornerRadius(4f, 4f))
                    drawRoundRect(Color(0xFF334155), topLeft = Offset(w * 0.70f, h * 0.74f), size = Size(w * 0.16f, h * 0.26f), cornerRadius = CornerRadius(4f, 4f))
                    drawRoundRect(Color(0xFF64748B), topLeft = Offset(w * 0.65f, h * 0.60f), size = Size(w * 0.11f, h * 0.24f), cornerRadius = CornerRadius(6f, 6f))
                }
            }
        }
        if (kind == "CLOCK") {
            Text(
                text = "05.00",
                color = Color(0xFF4ADE80),
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 18.dp, bottom = 22.dp),
            )
        } else if (kind == "CLASS" && !speechQuote.isNullOrBlank()) {
            val shortText = if (speechQuote.length > 36) speechQuote.take(33) + "..." else speechQuote
            Text(
                text = shortText,
                color = Color(0xFF1E293B),
                fontSize = 9.sp,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 11.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth(0.54f)
                    .align(Alignment.TopStart)
                    .padding(start = 16.dp, top = 16.dp, end = 8.dp),
            )
        }
    }
}

/* ═══════════════════════════════════════════════════════════════════════════
   1. SWIPE CARD ARENA (Mockup 3 & 4 Top: [X INCORRECT <-] [Card] [-> ✓ CORRECT])
   ═══════════════════════════════════════════════════════════════════════════ */
@Composable
fun SwipeCardArenaCompose(
    challenge: DailyQuestChallenge.FactOrMyth,
    feedback: QuestFeedback?,
    onSwipeAnswer: (Boolean) -> Unit,
) {
    var dragOffsetX by remember(challenge.id) { mutableFloatStateOf(0f) }
    val enabled = feedback == null

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFFEEF3FA),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                SwipeSideSquareButton(
                    label = "INCORRECT",
                    icon = Icons.Default.Close,
                    topColor = Color(0xFFDC2626),
                    lipColor = Color(0xFF991B1B),
                    dimmed = feedback != null && challenge.isFact && feedback.selectedBoolAnswer != false,
                    enabled = enabled,
                    onClick = { onSwipeAnswer(false) },
                )

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    tint = Color(0xFFDC2626),
                    modifier = Modifier.size(16.dp),
                )

                val swipingRight = dragOffsetX > 22f
                val swipingLeft = dragOffsetX < -22f
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
                                    dragOffsetX = (dragOffsetX + delta).coerceIn(-210f, 210f)
                                },
                                onDragEnd = {
                                    when {
                                        dragOffsetX > 80f -> {
                                            dragOffsetX = 0f
                                            onSwipeAnswer(true)
                                        }
                                        dragOffsetX < -80f -> {
                                            dragOffsetX = 0f
                                            onSwipeAnswer(false)
                                        }
                                        else -> dragOffsetX = 0f
                                    }
                                },
                                onDragCancel = { dragOffsetX = 0f },
                            )
                        },
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    shadowElevation = 6.dp,
                    border = BorderStroke(
                        1.8.dp,
                        when {
                            swipingRight -> Color(0xFF22C55E)
                            swipingLeft -> Color(0xFFDC2626)
                            else -> Color(0xFFE2E8F0)
                        },
                    ),
                ) {
                    Column(
                        modifier = Modifier.padding(9.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        TopicCartoonIllustrationCanvas(
                            topicText = "${challenge.topicTag.orEmpty()} ${challenge.statement}",
                            speechQuote = challenge.statement,
                            height = 96.dp,
                        )
                        Text(
                            text = "PERNYATAAN:",
                            color = Color(0xFF0F172A),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                        )
                        Text(
                            text = challenge.statement,
                            color = Color(0xFF1E293B),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 14.sp,
                        )
                    }
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = Color(0xFF22C55E),
                    modifier = Modifier.size(16.dp),
                )

                SwipeSideSquareButton(
                    label = "CORRECT",
                    icon = Icons.Default.Check,
                    topColor = Color(0xFF22C55E),
                    lipColor = Color(0xFF15803D),
                    dimmed = feedback != null && !challenge.isFact && feedback.selectedBoolAnswer != true,
                    enabled = enabled,
                    onClick = { onSwipeAnswer(true) },
                )
            }
        }

        Text(
            text = "Geser Kanan untuk Benar, Kiri untuk Salah.",
            color = Color(0xFF334155),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun SwipeSideSquareButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    topColor: Color,
    lipColor: Color,
    dimmed: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
        modifier = Modifier
            .alpha(if (dimmed) 0.4f else 1f)
            .clickable(enabled = enabled, onClick = onClick),
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = lipColor,
            shadowElevation = 4.dp,
        ) {
            Surface(
                modifier = Modifier.padding(bottom = 3.dp).size(40.dp),
                shape = RoundedCornerShape(12.dp),
                color = topColor,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(22.dp))
                }
            }
        }
        Text(
            text = label,
            color = lipColor,
            fontSize = 8.sp,
            fontWeight = FontWeight.Black,
        )
    }
}

/* ═══════════════════════════════════════════════════════════════════════════
   2. DUAL-COLUMN STEP BUILDER (Mockup 2: Clean Langkah 1..4 <-> Ice-Blue Tray)
   ═══════════════════════════════════════════════════════════════════════════ */
@Composable
fun DualColumnStepBuilderCompose(
    challenge: DailyQuestChallenge.SequenceOrder,
    shuffledPool: List<SequenceStepItem>,
    disabled: Boolean,
    onWrongTap: () -> Unit,
    onAllCompleted: () -> Unit,
) {
    var filledCount by remember(challenge.id) { mutableIntStateOf(0) }
    var wrongItemIdx by remember(challenge.id) { mutableStateOf<Int?>(null) }
    val scope = rememberCoroutineScope()

    val colonIdx = challenge.prompt.indexOf(':')
    val promptTitle = if (colonIdx > 0) challenge.prompt.substring(0, colonIdx + 1) else challenge.prompt
    val promptFormula = if (colonIdx > 0) challenge.prompt.substring(colonIdx + 1).trim() else ""
    val isMathLike = challenge.steps.any { "=" in it || it.any(Char::isDigit) }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Prompt White Card (Mockup 2)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = Color.White,
            shadowElevation = 4.dp,
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = promptTitle,
                    color = Color(0xFF1E293B),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp,
                )
                if (promptFormula.isNotBlank()) {
                    Text(
                        text = promptFormula,
                        color = Color(0xFF0F172A),
                        fontSize = 21.sp,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        // Dual Column Arena
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            // LEFT COLUMN: Langkah 1..4
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
                        color = Color(0xFF334155),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 52.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = when {
                                isDone -> Color.White
                                isActive -> Color(0xFFD5E1EE)
                                else -> Color(0xFFDFE7F2)
                            },
                            border = BorderStroke(
                                width = if (isDone || isActive) 1.5.dp else 1.dp,
                                color = when {
                                    isDone -> Color(0xFF4ADE80)
                                    isActive -> Color(0xFF38BDF8)
                                    else -> Color(0xFFCBD5E1)
                                },
                            ),
                            shadowElevation = if (isDone) 2.dp else 0.dp,
                        ) {
                            if (isDone) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    Text(
                                        text = stepText,
                                        color = Color(0xFF0F172A),
                                        fontSize = 13.sp,
                                        fontFamily = if (isMathLike) FontFamily.Serif else FontFamily.Default,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 9.dp),
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color(0xFFC8F3D8))
                                            .padding(vertical = 2.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            text = "BENAR",
                                            color = Color(0xFF15803D),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black,
                                        )
                                    }
                                }
                            }
                        }

                        // Overlapping Emerald Checkmark Top-Right
                        if (isDone) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = 5.dp, y = (-5).dp)
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF22C55E))
                                    .zIndex(2f),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(11.dp))
                            }
                        }
                    }

                    if (idx < challenge.steps.lastIndex) {
                        Icon(
                            imageVector = Icons.Default.ArrowDownward,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
            }

            // RIGHT COLUMN: Cool Ice-Blue Option Tray
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(18.dp),
                color = Color(0xFFE4ECF5),
                border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    shuffledPool.forEach { item ->
                        val isPlaced = item.origIdx < filledCount
                        val isWrong = wrongItemIdx == item.origIdx

                        if (isPlaced) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFDCE6F2).copy(alpha = 0.7f)),
                            )
                        } else {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 50.dp)
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
                                shape = RoundedCornerShape(12.dp),
                                color = if (isWrong) Color(0xFFFFF1F2) else Color.White,
                                shadowElevation = 3.dp,
                                border = BorderStroke(
                                    1.2.dp,
                                    if (isWrong) Color(0xFFF43F5E) else Color(0xFFE2E8F0),
                                ),
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp)) {
                                    Text(
                                        text = item.text,
                                        color = if (isWrong) Color(0xFFDC2626) else Color(0xFF0F172A),
                                        fontSize = 13.sp,
                                        fontFamily = if (isMathLike) FontFamily.Serif else FontFamily.Default,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 16.sp,
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
   3. MAP PIN & SCENE HOTSPOT ARENA (Mockup 1 & Mockup 3/4 Middle)
   ═══════════════════════════════════════════════════════════════════════════ */
@Composable
fun MapPinHuntArenaCompose(
    challenge: DailyQuestChallenge.MapPinHunt,
    disabled: Boolean,
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
    val isProklamasi = challenge.sceneId == "proklamasi_1945"
    val isRoutine = challenge.sceneId == "morning_routine_room"

    fun triggerWrong(pinId: String) {
        wrongPinId = pinId
        onWrongTap()
        scope.launch {
            delay(450)
            wrongPinId = null
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (isMapTags) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                shadowElevation = 3.dp,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            ) {
                Text(
                    text = challenge.prompt,
                    color = Color(0xFF1E293B),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(12.dp),
                )
            }
        }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(235.dp)
                .clip(RoundedCornerShape(18.dp)),
        ) {
            val boxWidthPx = constraints.maxWidth.toFloat()
            val boxHeightPx = constraints.maxHeight.toFloat()

            SceneBackdropCanvas(sceneId = challenge.sceneId, modifier = Modifier.fillMaxSize())

            if (isProklamasi) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                        .widthIn(max = 200.dp)
                        .zIndex(2f),
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White.copy(alpha = 0.96f),
                    shadowElevation = 4.dp,
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                ) {
                    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                        Text("TEBAK LOKASI:", color = Color(0xFF0F172A), fontSize = 9.sp, fontWeight = FontWeight.Black)
                        Text("Klik lokasi pembacaan Teks Proklamasi!", color = Color(0xFF1E293B), fontSize = 10.sp, fontWeight = FontWeight.Bold, lineHeight = 13.sp)
                    }
                }
            } else if (isRoutine) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 10.dp)
                        .zIndex(2f),
                    shape = RoundedCornerShape(50),
                    color = Color.White.copy(alpha = 0.96f),
                    shadowElevation = 4.dp,
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                ) {
                    Text(
                        text = "HUNT: Click the time Andi wakes up!",
                        color = Color(0xFF0F172A),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp),
                    )
                }
            }

            pins.forEachIndexed { idx, pin ->
                val isUnlocked = pin.id in unlockedIds
                val isWrong = wrongPinId == pin.id
                val isSelected = selectedMapPinId == pin.id || (isMapTags && idx == currentStepIdx)

                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                x = (pin.x * boxWidthPx).roundToInt() - 40,
                                y = (pin.y * boxHeightPx).roundToInt() - 40,
                            )
                        }
                        .zIndex(3f)
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
                            Box {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color.White,
                                    border = BorderStroke(1.5.dp, Color(0xFF4ADE80)),
                                    shadowElevation = 4.dp,
                                    modifier = Modifier.widthIn(min = 96.dp, max = 128.dp),
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = pin.label,
                                            color = Color(0xFF0F172A),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center,
                                            lineHeight = 11.sp,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                        )
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(Color(0xFFC8F3D8))
                                                .padding(vertical = 2.dp),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Text("BENAR", color = Color(0xFF15803D), fontSize = 8.sp, fontWeight = FontWeight.Black)
                                        }
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .offset(x = 4.dp, y = (-4).dp)
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF22C55E)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(10.dp))
                                }
                            }
                        } else {
                            Surface(
                                shape = CircleShape,
                                color = if (isSelected) Color(0xFF63A4D8) else Color(0xFF6E9EC9).copy(alpha = 0.85f),
                                border = BorderStroke(if (isSelected) 3.dp else 2.dp, Color.White),
                                shadowElevation = 4.dp,
                                modifier = Modifier.size(if (isSelected) 34.dp else 30.dp),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Lock, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    } else {
                        // Concentric Red-White Archery Bullseye Target + Black Number Badge (Mockup 3 & 4 Middle)
                        if (isUnlocked) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF22C55E),
                                border = BorderStroke(2.dp, Color.White),
                                modifier = Modifier.size(30.dp),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                            }
                        } else {
                            Box(modifier = Modifier.size(34.dp), contentAlignment = Alignment.Center) {
                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
                                        .clip(CircleShape)
                                        .background(if (isWrong) Color(0xFFEF4444) else Color.White)
                                        .padding(3.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape)
                                            .background(Color(0xFFDC2626))
                                            .padding(4.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(CircleShape)
                                                .background(Color.White),
                                        )
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .size(15.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF0F172A)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text("${idx + 1}", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Black)
                                }
                            }
                        }
                    }
                }
            }

            // Floating Checklist Bottom-Left (Mockup 1 exact layout!)
            if (isMapTags) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(10.dp)
                        .widthIn(max = 195.dp)
                        .zIndex(2f),
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.95f),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    shadowElevation = 4.dp,
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        pins.forEachIndexed { idx, pin ->
                            val done = pin.id in unlockedIds
                            val wip = idx == currentStepIdx && !done
                            Column {
                                Text(
                                    text = "Langkah ${idx + 1}:",
                                    color = Color(0xFF64748B),
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    text = "${pin.stepLabel ?: pin.label}${if (done) " -> BENAR" else if (wip) " -> [WIP]" else ""}",
                                    color = if (done) Color(0xFF16A34A) else Color(0xFF0F172A),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        }

        // Bottom 2x2 White Location Cards (MAP_TAGS) OR Centered Caption (SCENE_HOTSPOT)
        if (isMapTags) {
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
                                color = when {
                                    done -> Color(0xFFECFDF5)
                                    isWrong -> Color(0xFFFFF1F2)
                                    else -> Color.White
                                },
                                shadowElevation = 3.dp,
                                border = BorderStroke(
                                    1.2.dp,
                                    when {
                                        done -> Color(0xFF4ADE80)
                                        isWrong -> Color(0xFFF43F5E)
                                        else -> Color(0xFFE2E8F0)
                                    },
                                ),
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 9.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(26.dp)
                                            .clip(CircleShape)
                                            .background(if (done) Color(0xFFC8F3D8) else Color(0xFFFEF3C7)),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            imageVector = if (done) Icons.Default.Check else Icons.Default.AccountBalance,
                                            contentDescription = null,
                                            tint = if (done) Color(0xFF15803D) else Color(0xFFB45309),
                                            modifier = Modifier.size(14.dp),
                                        )
                                    }
                                    Text(
                                        text = pin.label,
                                        color = Color(0xFF1E293B),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        lineHeight = 13.sp,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Faint Cultural Watermark Icons Row (Mockup 1 bottom)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(0.25f)
                    .padding(top = 2.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                Icon(Icons.Default.AccountBalance, contentDescription = null, tint = Color(0xFF475569), modifier = Modifier.size(20.dp))
                Icon(Icons.Default.Explore, contentDescription = null, tint = Color(0xFF475569), modifier = Modifier.size(20.dp))
                Icon(Icons.Default.AccountBalance, contentDescription = null, tint = Color(0xFF475569), modifier = Modifier.size(20.dp))
            }
        } else {
            Text(
                text = when {
                    isProklamasi -> "Tebak Gambar Peristiwa Sejarah."
                    isRoutine -> "Tebak Gambar Rutinitas Harian."
                    else -> "Ketuk titik target pada gambar!"
                },
                color = Color(0xFF334155),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
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
                    drawRect(Color(0xFFCFEAFB))
                    drawCircle(Color(0xFF4ADE80), radius = h * 0.22f, center = Offset(w * 0.10f, h * 0.44f))
                    drawCircle(Color(0xFF22C55E), radius = h * 0.16f, center = Offset(w * 0.18f, h * 0.52f))
                    drawCircle(Color(0xFF4ADE80), radius = h * 0.24f, center = Offset(w * 0.90f, h * 0.42f))
                    // Red tiled colonial roof + white columns
                    val roof = Path().apply {
                        moveTo(w * 0.26f, h * 0.27f)
                        lineTo(w * 0.52f, h * 0.10f)
                        lineTo(w * 0.78f, h * 0.27f)
                        close()
                    }
                    drawPath(roof, Color(0xFFB91C1C))
                    drawRect(Color(0xFFF8FAFC), topLeft = Offset(w * 0.29f, h * 0.27f), size = Size(w * 0.46f, h * 0.38f))
                    for (px in listOf(0.32f, 0.40f, 0.62f, 0.70f)) {
                        drawRect(Color(0xFFCBD5E1), topLeft = Offset(w * px, h * 0.28f), size = Size(w * 0.03f, h * 0.37f))
                    }
                    drawRect(Color(0xFF86EFAC), topLeft = Offset(0f, h * 0.65f), size = Size(w, h * 0.35f))
                    drawLine(Color(0xFF475569), Offset(w * 0.24f, h * 0.10f), Offset(w * 0.24f, h * 0.70f), strokeWidth = 6f)
                    drawRect(Color(0xFFEF4444), topLeft = Offset(w * 0.245f, h * 0.11f), size = Size(w * 0.11f, h * 0.07f))
                    drawRect(Color.White, topLeft = Offset(w * 0.245f, h * 0.18f), size = Size(w * 0.11f, h * 0.07f))
                    drawCircle(Color(0xFFFDBA74), radius = h * 0.045f, center = Offset(w * 0.49f, h * 0.52f))
                    drawRoundRect(Color.White, topLeft = Offset(w * 0.465f, h * 0.57f), size = Size(w * 0.05f, h * 0.15f), cornerRadius = CornerRadius(6f, 6f))
                    for (cx in listOf(0.73f, 0.79f, 0.85f)) {
                        drawCircle(Color(0xFF94A3B8), radius = h * 0.04f, center = Offset(w * cx, h * 0.56f))
                        drawRoundRect(Color(0xFF64748B), topLeft = Offset(w * cx - w * 0.02f, h * 0.60f), size = Size(w * 0.04f, h * 0.14f), cornerRadius = CornerRadius(4f, 4f))
                    }
                }
                "morning_routine_room" -> {
                    drawRect(Color(0xFFDCEEFB))
                    for (i in 1..10) {
                        drawLine(Color(0xFFC5E1F8), Offset(w * (i / 11f), 0f), Offset(w * (i / 11f), h * 0.70f), strokeWidth = 10f)
                    }
                    drawRoundRect(Color(0xFF93C5FD), topLeft = Offset(w * 0.13f, h * 0.08f), size = Size(w * 0.18f, h * 0.28f), cornerRadius = CornerRadius(6f, 6f))
                    drawCircle(Color.White, radius = h * 0.09f, center = Offset(w * 0.39f, h * 0.19f))
                    drawCircle(Color(0xFF475569), radius = h * 0.09f, center = Offset(w * 0.39f, h * 0.19f), style = Stroke(width = 3f))
                    drawRect(Color(0xFFE7C294), topLeft = Offset(0f, h * 0.69f), size = Size(w, h * 0.31f))
                    drawOval(Color(0xFF93C5FD), topLeft = Offset(w * 0.25f, h * 0.75f), size = Size(w * 0.55f, h * 0.18f))
                    // Bed & digital alarm
                    drawRoundRect(Color(0xFF3B82F6), topLeft = Offset(w * 0.24f, h * 0.42f), size = Size(w * 0.35f, h * 0.25f), cornerRadius = CornerRadius(10f, 10f))
                    drawRoundRect(Color(0xFF1E293B), topLeft = Offset(w * 0.23f, h * 0.52f), size = Size(w * 0.13f, h * 0.10f), cornerRadius = CornerRadius(6f, 6f))
                    // Hanging uniform & soccer ball
                    drawRoundRect(Color.White, topLeft = Offset(w * 0.65f, h * 0.24f), size = Size(w * 0.11f, h * 0.16f), cornerRadius = CornerRadius(6f, 6f))
                    drawRoundRect(Color(0xFF1E3A8A), topLeft = Offset(w * 0.66f, h * 0.40f), size = Size(w * 0.09f, h * 0.10f), cornerRadius = CornerRadius(4f, 4f))
                    drawCircle(Color.White, radius = h * 0.08f, center = Offset(w * 0.82f, h * 0.78f))
                    drawCircle(Color(0xFF0F172A), radius = h * 0.08f, center = Offset(w * 0.82f, h * 0.78f), style = Stroke(width = 3f))
                }
                "photosynthesis_scene" -> {
                    drawRect(Color(0xFFDDF2FF))
                    drawCircle(Color(0xFFFACC15), radius = h * 0.13f, center = Offset(w * 0.22f, h * 0.23f))
                    drawRect(Color(0xFFA16207), topLeft = Offset(0f, h * 0.72f), size = Size(w, h * 0.28f))
                    drawLine(Color(0xFF15803D), Offset(w * 0.49f, h * 0.72f), Offset(w * 0.49f, h * 0.40f), strokeWidth = 10f)
                    val leaf = Path().apply {
                        moveTo(w * 0.49f, h * 0.48f)
                        cubicTo(w * 0.49f, h * 0.30f, w * 0.66f, h * 0.25f, w * 0.75f, h * 0.36f)
                        cubicTo(w * 0.71f, h * 0.52f, w * 0.58f, h * 0.56f, w * 0.49f, h * 0.48f)
                    }
                    drawPath(leaf, Color(0xFF22C55E))
                    drawCircle(Color(0xFF38BDF8), radius = h * 0.10f, center = Offset(w * 0.78f, h * 0.34f))
                }
                else -> {
                    // Rich West/Central Java + Cirebon Cartographic Map (Mockup 1)
                    drawRect(brush = Brush.verticalGradient(listOf(Color(0xFF9EDAF6), Color(0xFF81CAED))))
                    val land = Path().apply {
                        moveTo(0f, h * 0.26f)
                        cubicTo(w * 0.18f, h * 0.25f, w * 0.35f, h * 0.32f, w * 0.48f, h * 0.32f)
                        cubicTo(w * 0.58f, h * 0.39f, w * 0.75f, h * 0.45f, w, h * 0.43f)
                        lineTo(w, h * 0.80f)
                        cubicTo(w * 0.65f, h * 0.70f, w * 0.30f, h * 0.56f, 0f, h * 0.60f)
                        close()
                    }
                    drawPath(land, Color(0xFFDCEEC1))
                    drawPath(land, Color(0xFF789862), style = Stroke(width = 3f))

                    val cirebon = Path().apply {
                        moveTo(w * 0.38f, h * 0.32f)
                        lineTo(w * 0.58f, h * 0.42f)
                        lineTo(w * 0.54f, h * 0.56f)
                        lineTo(w * 0.36f, h * 0.48f)
                        close()
                    }
                    drawPath(cirebon, Color(0xFFF6E5C3))
                    drawPath(cirebon, Color(0xFFB59369), style = Stroke(width = 2.5f))

                    // Sailboat watermark in ocean
                    drawRoundRect(Color(0xFF475569).copy(alpha = 0.28f), topLeft = Offset(w * 0.58f, h * 0.85f), size = Size(w * 0.09f, h * 0.05f), cornerRadius = CornerRadius(4f, 4f))
                }
            }
        }

        if (sceneId == "cirebon_keraton_map") {
            Text("JAWA BARAT", color = Color(0xFF293822), fontSize = 9.sp, fontWeight = FontWeight.Black, modifier = Modifier.align(Alignment.TopStart).padding(start = 24.dp, top = 74.dp))
            Text("Laut Jawa", color = Color(0xFF23648A), fontSize = 10.sp, fontStyle = FontStyle.Italic, fontWeight = FontWeight.SemiBold, modifier = Modifier.align(Alignment.TopEnd).padding(end = 26.dp, top = 22.dp))
            Text("JAWA TENGAH", color = Color(0xFF293822), fontSize = 9.sp, fontWeight = FontWeight.Black, modifier = Modifier.align(Alignment.BottomEnd).padding(end = 14.dp, bottom = 78.dp))
        } else if (sceneId == "morning_routine_room") {
            Text(
                text = "05.00",
                color = Color(0xFF4ADE80),
                fontSize = 8.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.align(Alignment.CenterStart).padding(start = 84.dp, top = 16.dp),
            )
        } else if (sceneId == "proklamasi_1945") {
            Surface(
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 10.dp),
                shape = RoundedCornerShape(50),
                color = Color.White.copy(alpha = 0.95f),
                border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
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
   4. TRUE 3-PANEL COMIC STRIP (Mockup 3 & 4 Bottom: ① ② ③ Side-by-Side + Shiba)
   ═══════════════════════════════════════════════════════════════════════════ */
@Composable
fun StoryComicArenaCompose(
    challenge: DailyQuestChallenge.CaseDetective,
    feedback: QuestFeedback?,
    onChooseOption: (Int) -> Unit,
) {
    val combined = "${challenge.topicTag.orEmpty()} ${challenge.scenario} ${challenge.question}".lowercase()
    val panel2Caption = when {
        "fotosintesis" in combined || "air" in combined || "biopori" in combined -> "Sistem bekerja mengolah energi..."
        "usaha" in combined || "kopi" in combined || "uang" in combined || "ekonomi" in combined -> "Menghitung margin & biaya usaha..."
        "digital" in combined || "otp" in combined || "iklan" in combined -> "Cek fakta & jaga data pribadi..."
        else -> "Menganalisis solusi kasus..."
    }
    val panel3Caption = when {
        "fotosintesis" in combined || "air" in combined || "biopori" in combined -> "Energi dihasilkan, alam sehat!"
        "usaha" in combined || "kopi" in combined || "uang" in combined -> "Keuangan sehat, usaha tumbuh!"
        "digital" in combined || "otp" in combined || "iklan" in combined -> "Aman dari hoaks & penipuan!"
        else -> "Keputusan tepat tercapai!"
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        shadowElevation = 4.dp,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
    ) {
        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    // PANEL 1: Scenario + Inline Options A/B/C/D
                    Surface(
                        modifier = Modifier.weight(1.1f),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    ) {
                        Column(
                            modifier = Modifier.padding(6.dp),
                            verticalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            Box {
                                TopicCartoonIllustrationCanvas("${challenge.topicTag.orEmpty()} ${challenge.scenario}", height = 74.dp)
                                Box(
                                    modifier = Modifier
                                        .padding(4.dp)
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF0F172A)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text("1", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Black)
                                }
                            }
                            Text(
                                text = "${challenge.scenario} ${challenge.question}",
                                color = Color(0xFF1E293B),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                lineHeight = 11.sp,
                                maxLines = 4,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                challenge.options.forEachIndexed { idx, opt ->
                                    val letter = ('A' + idx).toString()
                                    val isSelected = feedback?.selectedOptionIndex == idx
                                    val isAnswer = idx == challenge.correctIndex
                                    val bg = when {
                                        feedback == null -> Color.White
                                        isAnswer -> Color(0xFF22C55E)
                                        isSelected -> Color(0xFFDC2626)
                                        else -> Color(0xFFF1F5F9)
                                    }
                                    val fg = if (feedback != null && (isAnswer || isSelected)) Color.White else Color(0xFF1E293B)

                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable(enabled = feedback == null) { onChooseOption(idx) },
                                        shape = RoundedCornerShape(50),
                                        color = bg,
                                        border = BorderStroke(1.dp, if (feedback == null) Color(0xFFCBD5E1) else bg),
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                        ) {
                                            Text(
                                                text = "$letter. $opt",
                                                color = fg,
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f),
                                            )
                                            if (feedback != null && isAnswer) {
                                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(10.dp))
                                            } else if (feedback != null && isSelected) {
                                                Icon(Icons.Default.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(10.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // PANEL 2: Process Frame
                    Surface(
                        modifier = Modifier.weight(0.95f),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    ) {
                        Column(modifier = Modifier.padding(6.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(74.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFFEF3C7)),
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val w = size.width
                                    val h = size.height
                                    drawRoundRect(Color(0xFF15803D), topLeft = Offset(w * 0.14f, h * 0.12f), size = Size(w * 0.72f, h * 0.30f), cornerRadius = CornerRadius(6f, 6f))
                                    drawCircle(Color(0xFFFDBA74), radius = h * 0.14f, center = Offset(w * 0.50f, h * 0.58f))
                                    drawRoundRect(Color.White, topLeft = Offset(w * 0.36f, h * 0.72f), size = Size(w * 0.28f, h * 0.24f), cornerRadius = CornerRadius(8f, 8f))
                                }
                                Box(
                                    modifier = Modifier
                                        .padding(4.dp)
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF0F172A)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text("2", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Black)
                                }
                            }
                            Text(
                                text = panel2Caption,
                                color = Color(0xFF1E293B),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                lineHeight = 11.sp,
                            )
                        }
                    }

                    // PANEL 3: Outcome + Space for Cheering Shiba
                    Surface(
                        modifier = Modifier.weight(0.95f),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    ) {
                        Column(modifier = Modifier.padding(6.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(74.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFDBEAFE)),
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val w = size.width
                                    val h = size.height
                                    drawRect(Color(0xFF4ADE80), topLeft = Offset(0f, h * 0.70f), size = Size(w, h * 0.30f))
                                    for ((idx, fx) in listOf(0.32f, 0.66f).withIndex()) {
                                        drawCircle(Color(0xFFFDBA74), radius = h * 0.12f, center = Offset(w * fx, h * 0.42f))
                                        drawRoundRect(
                                            if (idx == 0) Color.White else Color(0xFFEF4444),
                                            topLeft = Offset(w * fx - w * 0.08f, h * 0.54f),
                                            size = Size(w * 0.16f, h * 0.24f),
                                            cornerRadius = CornerRadius(6f, 6f),
                                        )
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .padding(4.dp)
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF0F172A)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text("3", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Black)
                                }
                            }
                            Text(
                                text = panel3Caption,
                                color = Color(0xFF1E293B),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                lineHeight = 11.sp,
                            )
                            Spacer(modifier = Modifier.height(32.dp))
                        }
                    }
                }

                // Cheering Full-Body Shiba overlapping Panel 3 bottom-right (Mockup 3 & 4!)
                ShibaFullBodyMascot(
                    size = 54.dp,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 2.dp, y = 4.dp)
                        .zIndex(4f),
                )
            }

            Text(
                text = "STORY-DRIVEN CHOICE",
                color = Color(0xFF0F172A),
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
