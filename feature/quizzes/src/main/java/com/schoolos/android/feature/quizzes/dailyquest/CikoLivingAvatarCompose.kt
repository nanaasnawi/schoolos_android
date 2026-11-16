package com.schoolos.android.feature.quizzes.dailyquest

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.sin

private enum class CikoMood { IDLE, PETTED, CATCHING, CHEWING, EVOLVING }

private data class StagePalette(
    val primary: Color,
    val secondary: Color,
    val lines: List<String>,
)

private fun paletteForStage(stage: Int): StagePalette = when (stage.coerceIn(1, 6)) {
    1 -> StagePalette(
        Color(0xFFF97316),
        Color(0xFFFBBF24),
        listOf("Miau! Ayo main!", "Aku lapar nih~", "Elus kepalaku!", "Main TTS yuk!"),
    )
    2 -> StagePalette(
        Color(0xFF0EA5E9),
        Color(0xFF38BDF8),
        listOf("Semangat belajar!", "Kumpulkan EXP!", "Catat ilmu baru!", "Tugas sudah beres?"),
    )
    3 -> StagePalette(
        Color(0xFF14B8A6),
        Color(0xFF2DD4BF),
        listOf("Hmm, menarik!", "Analisis tajam!", "Kamu makin pintar!", "Misteri terpecahkan!"),
    )
    4 -> StagePalette(
        Color(0xFFF59E0B),
        Color(0xFFFDE047),
        listOf("Luar biasa, Nak!", "Ilmu itu cahaya!", "Terus berkarya!", "Profesor bangga!"),
    )
    5 -> StagePalette(
        Color(0xFF8B5CF6),
        Color(0xFF38BDF8),
        listOf("Siap meluncur!", "Galaksi ilmu menanti!", "Cosmic brain aktif!", "Menuju tak terbatas!"),
    )
    else -> StagePalette(
        Color(0xFF06B6D4),
        Color(0xFFFBBF24),
        listOf("AI Mode: OPTIMAL", "Grandmaster Sejati!", "Sinergi Cyber 100%", "Tak terkalahkan!"),
    )
}

@Composable
internal fun CikoLivingAvatarCompose(
    stage: Int,
    stageTitle: String,
    readyToEvolve: Boolean,
    biscuits: Int,
    feedPulse: Int,
    evolvePulse: Int,
    onPetTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val safeStage = stage.coerceIn(1, 6)
    val palette = remember(safeStage) { paletteForStage(safeStage) }
    val haptic = LocalHapticFeedback.current

    var mood by remember { mutableStateOf(CikoMood.IDLE) }
    var isBlinking by remember { mutableStateOf(false) }
    var earTwitch by remember { mutableStateOf(false) }
    var speech by remember(safeStage, readyToEvolve, biscuits) {
        mutableStateOf(
            when {
                readyToEvolve -> "Aku siap Evolusi!"
                biscuits > 0 -> palette.lines.first()
                else -> "Main game yuk!"
            }
        )
    }
    var particleTrigger by remember { mutableIntStateOf(0) }

    val cookieProgress = remember { Animatable(0f) }
    val evolveBurst = remember { Animatable(0f) }
    val particleAnim = remember { Animatable(0f) }

    // Continuous 60 FPS breathing, tail wag, orbit rotation
    val infiniteTransition = rememberInfiniteTransition(label = "ciko_life")
    val breathPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(2600, easing = LinearEasing), RepeatMode.Restart),
        label = "breath",
    )
    val tailPhase by infiniteTransition.animateFloat(
        initialValue = -12f,
        targetValue = 16f,
        animationSpec = infiniteRepeatable(tween(850, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "tail",
    )
    val orbitAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(7500, easing = LinearEasing), RepeatMode.Restart),
        label = "orbit",
    )

    // Natural eye blink loop
    LaunchedEffect(Unit) {
        while (true) {
            delay(3200L)
            isBlinking = true
            delay(150L)
            isBlinking = false
        }
    }

    // Ear twitch loop
    LaunchedEffect(Unit) {
        while (true) {
            delay(4600L)
            earTwitch = true
            delay(260L)
            earTwitch = false
        }
    }

    // Flying biscuit catch + chew sequence
    LaunchedEffect(feedPulse) {
        if (feedPulse <= 0) return@LaunchedEffect
        mood = CikoMood.CATCHING
        speech = "Owah! Biskuit emas!"
        cookieProgress.snapTo(0f)
        cookieProgress.animateTo(1f, tween(450, easing = FastOutSlowInEasing))
        mood = CikoMood.CHEWING
        speech = "Nyum nyum... Enak!"
        particleTrigger++
        delay(1300L)
        if (mood == CikoMood.CHEWING) mood = CikoMood.IDLE
    }

    // Golden evolution transformation sequence
    LaunchedEffect(evolvePulse) {
        if (evolvePulse <= 0) return@LaunchedEffect
        mood = CikoMood.EVOLVING
        speech = "EVOLUSI Lv.$safeStage!"
        evolveBurst.snapTo(0f)
        evolveBurst.animateTo(1f, tween(1800, easing = FastOutSlowInEasing))
        particleTrigger++
        delay(800L)
        if (mood == CikoMood.EVOLVING) mood = CikoMood.IDLE
    }

    // Rising heart/star particle float
    LaunchedEffect(particleTrigger) {
        if (particleTrigger <= 0) return@LaunchedEffect
        particleAnim.snapTo(0f)
        particleAnim.animateTo(1f, tween(1100, easing = FastOutSlowInEasing))
    }

    Box(
        modifier = modifier
            .width(150.dp)
            .height(158.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onPetTap()
                if (mood != CikoMood.EVOLVING && mood != CikoMood.CATCHING) {
                    mood = CikoMood.PETTED
                    speech = palette.lines.random()
                    particleTrigger++
                }
            },
        contentAlignment = Alignment.BottomCenter,
    ) {
        // Reset PETTED mood cleanly
        LaunchedEffect(mood) {
            if (mood == CikoMood.PETTED) {
                delay(1000L)
                if (mood == CikoMood.PETTED) mood = CikoMood.IDLE
            }
        }

        // Speech Bubble at Top
        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 2.dp),
            shape = RoundedCornerShape(50),
            color = Color(0xFF0F172A).copy(alpha = 0.88f),
            border = BorderStroke(1.dp, palette.secondary),
        ) {
            Text(
                text = speech,
                color = Color.White,
                fontSize = 9.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            )
        }

        // Full 60 FPS Articulated 3D Pixar Character Canvas
        Canvas(
            modifier = Modifier
                .size(134.dp)
                .align(Alignment.BottomCenter),
        ) {
            val s = size.minDimension / 160f
            val breathWave = sin(breathPhase)
            val bobOffset = when (mood) {
                CikoMood.PETTED -> -11f * s * (0.5f + 0.5f * sin(breathPhase * 3f))
                CikoMood.CHEWING -> -3.5f * s * (0.5f + 0.5f * sin(breathPhase * 4f))
                CikoMood.EVOLVING -> -12f * s * (0.5f + 0.5f * sin(evolveBurst.value * 4f * PI.toFloat()))
                else -> -3.5f * s * (0.5f + 0.5f * breathWave)
            }
            val squashY = when (mood) {
                CikoMood.CHEWING -> 1f + 0.03f * sin(breathPhase * 4f)
                CikoMood.PETTED -> 1.04f
                else -> 1f + 0.024f * breathWave
            }
            val squashX = 2f - squashY

            // 1. Volumetric Stage Radial Glow Behind Ciko
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        palette.primary.copy(alpha = if (mood == CikoMood.EVOLVING) 0.58f else 0.36f),
                        palette.secondary.copy(alpha = 0.14f),
                        Color.Transparent,
                    ),
                    center = Offset(80f * s, 94f * s),
                    radius = 74f * s,
                ),
                center = Offset(80f * s, 94f * s),
                radius = 74f * s,
            )

            // Rotating dashed orbit ring when Ready to Evolve / Evolving
            if (readyToEvolve || mood == CikoMood.EVOLVING) {
                rotate(degrees = orbitAngle, pivot = Offset(80f * s, 88f * s)) {
                    drawCircle(
                        color = palette.secondary.copy(alpha = if (mood == CikoMood.EVOLVING) 0.85f else 0.45f),
                        radius = 66f * s,
                        center = Offset(80f * s, 88f * s),
                        style = Stroke(width = 2.2f * s),
                    )
                }
            }

            // 2. Dynamic Floor Contact Shadow (shrinks as Ciko jumps!)
            val shadowScale = (1f + bobOffset / (55f * s)).coerceIn(0.65f, 1.05f)
            drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(Color.Black.copy(alpha = 0.58f), Color.Transparent),
                    center = Offset(80f * s, 149f * s),
                    radius = 44f * s * shadowScale,
                ),
                topLeft = Offset(80f * s - 44f * s * shadowScale, 142f * s),
                size = Size(88f * s * shadowScale, 14f * s),
            )

            // 3. Articulated Ciko Rig (Scale & Bob around feet)
            translate(top = bobOffset) {
                scale(scaleX = squashX, scaleY = squashY, pivot = Offset(80f * s, 142f * s)) {
                    // STAGE 6: Cyber Luminous Wings Behind
                    if (safeStage >= 6) {
                        val wingBrush = Brush.linearGradient(
                            listOf(Color(0xFF22D3EE), Color(0xFF3B82F6), Color(0xFFFBBF24))
                        )
                        val leftWing = Path().apply {
                            moveTo(48f * s, 98f * s)
                            cubicTo(14f * s, 72f * s, 8f * s, 98f * s, 22f * s, 118f * s)
                            cubicTo(32f * s, 127f * s, 46f * s, 120f * s, 52f * s, 114f * s)
                            close()
                        }
                        val rightWing = Path().apply {
                            moveTo(112f * s, 98f * s)
                            cubicTo(146f * s, 72f * s, 152f * s, 98f * s, 138f * s, 118f * s)
                            cubicTo(128f * s, 127f * s, 114f * s, 120f * s, 108f * s, 114f * s)
                            close()
                        }
                        drawPath(leftWing, brush = wingBrush, alpha = 0.88f)
                        drawPath(rightWing, brush = wingBrush, alpha = 0.88f)
                    }

                    // ANIMATED TAIL
                    val tailSpeedMult = if (mood == CikoMood.PETTED || mood == CikoMood.CHEWING) 1.5f else 1f
                    rotate(degrees = tailPhase * tailSpeedMult, pivot = Offset(106f * s, 122f * s)) {
                        val tailPath = Path().apply {
                            moveTo(104f * s, 125f * s)
                            cubicTo(132f * s, 123f * s, 144f * s, 96f * s, 131f * s, 82f * s)
                            cubicTo(122f * s, 73f * s, 115f * s, 92f * s, 120f * s, 105f * s)
                            cubicTo(121f * s, 112f * s, 111f * s, 116f * s, 103f * s, 118f * s)
                            close()
                        }
                        drawPath(
                            tailPath,
                            brush = Brush.radialGradient(
                                listOf(Color(0xFFFDBA74), Color(0xFFEA580C), Color(0xFF9A3412)),
                                center = Offset(126f * s, 104f * s),
                                radius = 34f * s,
                            ),
                        )
                        drawCircle(Color(0xFFFFF7ED), radius = 6f * s, center = Offset(131f * s, 84f * s))
                    }

                    // TORSO & EVOLUTION COSTUMES
                    drawOval(
                        brush = Brush.radialGradient(
                            listOf(Color(0xFFFDBA74), Color(0xFFF97316), Color(0xFF9A3412)),
                            center = Offset(72f * s, 110f * s),
                            radius = 40f * s,
                        ),
                        topLeft = Offset(49f * s, 93f * s),
                        size = Size(62f * s, 54f * s),
                    )

                    when (safeStage) {
                        1 -> {
                            // Fluffy cream chest + red bandana + golden bell collar
                            drawOval(
                                color = Color(0xFFFFF7ED),
                                topLeft = Offset(62f * s, 103f * s),
                                size = Size(36f * s, 38f * s),
                            )
                            val scarf = Path().apply {
                                moveTo(55f * s, 100f * s)
                                quadraticTo(80f * s, 116f * s, 105f * s, 100f * s)
                                lineTo(80f * s, 120f * s)
                                close()
                            }
                            drawPath(scarf, Color(0xFFEF4444))
                            drawCircle(
                                brush = Brush.radialGradient(listOf(Color(0xFFFEF08A), Color(0xFFF59E0B), Color(0xFFB45309))),
                                radius = 5.5f * s,
                                center = Offset(80f * s, 114f * s),
                            )
                            drawCircle(Color.White.copy(alpha = 0.85f), radius = 1.5f * s, center = Offset(78.5f * s, 112.5f * s))
                        }
                        2 -> {
                            // Blue Student Jacket + Yellow Backpack Straps
                            drawOval(Color(0xFF1D4ED8), topLeft = Offset(52f * s, 98f * s), size = Size(56f * s, 48f * s))
                            drawOval(Color(0xFFFFF7ED), topLeft = Offset(63f * s, 106f * s), size = Size(34f * s, 36f * s))
                            drawLine(Color(0xFFFBBF24), Offset(59f * s, 102f * s), Offset(64f * s, 134f * s), strokeWidth = 4f * s, cap = StrokeCap.Round)
                            drawLine(Color(0xFFFBBF24), Offset(101f * s, 102f * s), Offset(96f * s, 134f * s), strokeWidth = 4f * s, cap = StrokeCap.Round)
                        }
                        3 -> {
                            // Detective Trenchcoat
                            drawOval(Color(0xFFB45309), topLeft = Offset(50f * s, 97f * s), size = Size(60f * s, 50f * s))
                            drawOval(Color(0xFFFFF7ED), topLeft = Offset(68f * s, 106f * s), size = Size(24f * s, 32f * s))
                            drawCircle(Color(0xFF78350F), radius = 2.2f * s, center = Offset(80f * s, 128f * s))
                            drawCircle(Color(0xFF78350F), radius = 2.2f * s, center = Offset(80f * s, 136f * s))
                        }
                        4 -> {
                            // Gold & Navy Professor Scholar Robe
                            drawOval(Color(0xFF1E3A8A), topLeft = Offset(49f * s, 97f * s), size = Size(62f * s, 50f * s))
                            drawLine(Color(0xFFFBBF24), Offset(65f * s, 98f * s), Offset(80f * s, 126f * s), strokeWidth = 4.5f * s, cap = StrokeCap.Round)
                            drawLine(Color(0xFFFBBF24), Offset(95f * s, 98f * s), Offset(80f * s, 126f * s), strokeWidth = 4.5f * s, cap = StrokeCap.Round)
                            drawOval(Color(0xFFFFF7ED), topLeft = Offset(67f * s, 108f * s), size = Size(26f * s, 30f * s))
                        }
                        5 -> {
                            // Cosmic White Astronaut Suit
                            drawOval(Color(0xFFF8FAFC), topLeft = Offset(48f * s, 97f * s), size = Size(64f * s, 50f * s))
                            drawRoundRect(
                                color = Color(0xFF0F172A),
                                topLeft = Offset(66f * s, 113f * s),
                                size = Size(28f * s, 15f * s),
                                cornerRadius = CornerRadius(6f * s),
                            )
                            drawCircle(Color(0xFF22D3EE), radius = 3f * s, center = Offset(74f * s, 120.5f * s))
                            drawCircle(Color(0xFFFBBF24), radius = 3f * s, center = Offset(84f * s, 120.5f * s))
                        }
                        else -> {
                            // Cyber AI Suit
                            drawOval(Color(0xFF0F172A), topLeft = Offset(48f * s, 97f * s), size = Size(64f * s, 50f * s))
                            drawOval(Color(0xFF22D3EE), topLeft = Offset(48f * s, 97f * s), size = Size(64f * s, 50f * s), style = Stroke(2f * s))
                            drawCircle(Color(0xFFFEF08A), radius = 6f * s, center = Offset(80f * s, 121f * s))
                        }
                    }

                    // Front Paws
                    val pawY = if (mood == CikoMood.PETTED) 115f * s else 131f * s
                    drawOval(Color(0xFFFFEDD5), topLeft = Offset(49f * s, pawY - 6f * s), size = Size(18f * s, 14f * s))
                    drawOval(Color(0xFFFFEDD5), topLeft = Offset(93f * s, pawY - 6f * s), size = Size(18f * s, 14f * s))

                    // EARS WITH TWITCH
                    val twitchAngle = if (earTwitch) -9f else 0f
                    rotate(twitchAngle, pivot = Offset(48f * s, 48f * s)) {
                        val lEar = Path().apply {
                            moveTo(38f * s, 60f * s)
                            lineTo(35f * s, 23f * s)
                            lineTo(64f * s, 42f * s)
                            close()
                        }
                        drawPath(lEar, Color(0xFFFB923C))
                        val lInner = Path().apply {
                            moveTo(41f * s, 52f * s)
                            lineTo(39f * s, 30f * s)
                            lineTo(56f * s, 43f * s)
                            close()
                        }
                        drawPath(lInner, Color(0xFFFDA4AF))
                    }
                    rotate(-twitchAngle, pivot = Offset(112f * s, 48f * s)) {
                        val rEar = Path().apply {
                            moveTo(122f * s, 60f * s)
                            lineTo(125f * s, 23f * s)
                            lineTo(96f * s, 42f * s)
                            close()
                        }
                        drawPath(rEar, Color(0xFFFB923C))
                        val rInner = Path().apply {
                            moveTo(119f * s, 52f * s)
                            lineTo(121f * s, 30f * s)
                            lineTo(104f * s, 43f * s)
                            close()
                        }
                        drawPath(rInner, Color(0xFFFDA4AF))
                    }

                    // 3D PIXAR ROUNDED HEAD
                    drawOval(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFFFED7AA), Color(0xFFFB923C), Color(0xFFEA580C), Color(0xFF9A3412)),
                            center = Offset(66f * s, 52f * s),
                            radius = 56f * s,
                        ),
                        topLeft = Offset(37f * s, 31f * s),
                        size = Size(86f * s, 74f * s),
                    )
                    // Cream Cheek Muzzle
                    drawOval(Color(0xFFFFF7ED).copy(alpha = 0.94f), topLeft = Offset(45f * s, 67f * s), size = Size(38f * s, 26f * s))
                    drawOval(Color(0xFFFFF7ED).copy(alpha = 0.94f), topLeft = Offset(77f * s, 67f * s), size = Size(38f * s, 26f * s))
                    drawOval(Color.White.copy(alpha = 0.95f), topLeft = Offset(62f * s, 70f * s), size = Size(36f * s, 24f * s))

                    // Cute Forehead Stripes
                    drawLine(Color(0xFF9A3412).copy(alpha = 0.65f), Offset(80f * s, 33f * s), Offset(80f * s, 42f * s), strokeWidth = 3f * s, cap = StrokeCap.Round)
                    drawLine(Color(0xFF9A3412).copy(alpha = 0.55f), Offset(71f * s, 35f * s), Offset(73f * s, 43f * s), strokeWidth = 2.4f * s, cap = StrokeCap.Round)
                    drawLine(Color(0xFF9A3412).copy(alpha = 0.55f), Offset(89f * s, 35f * s), Offset(87f * s, 43f * s), strokeWidth = 2.4f * s, cap = StrokeCap.Round)

                    // Blushed Cheeks
                    val blushAlpha = if (mood == CikoMood.PETTED || mood == CikoMood.CHEWING) 0.65f else 0.35f
                    drawOval(Color(0xFFFB7185).copy(alpha = blushAlpha), topLeft = Offset(42.5f * s, 72f * s), size = Size(13f * s, 7.6f * s))
                    drawOval(Color(0xFFFB7185).copy(alpha = blushAlpha), topLeft = Offset(104.5f * s, 72f * s), size = Size(13f * s, 7.6f * s))

                    // EXPRESSIVE PIXAR EYES
                    val isHappy = mood == CikoMood.PETTED || mood == CikoMood.CHEWING || mood == CikoMood.EVOLVING
                    if (isHappy) {
                        val lArc = Path().apply { moveTo(52f * s, 67f * s); quadraticTo(60f * s, 58f * s, 68f * s, 67f * s) }
                        val rArc = Path().apply { moveTo(92f * s, 67f * s); quadraticTo(100f * s, 58f * s, 108f * s, 67f * s) }
                        drawPath(lArc, Color(0xFF1E293B), style = Stroke(width = 3.5f * s, cap = StrokeCap.Round))
                        drawPath(rArc, Color(0xFF1E293B), style = Stroke(width = 3.5f * s, cap = StrokeCap.Round))
                    } else if (isBlinking) {
                        drawLine(Color(0xFF1E293B), Offset(52f * s, 66f * s), Offset(68f * s, 66f * s), strokeWidth = 3f * s, cap = StrokeCap.Round)
                        drawLine(Color(0xFF1E293B), Offset(92f * s, 66f * s), Offset(108f * s, 66f * s), strokeWidth = 3f * s, cap = StrokeCap.Round)
                    } else {
                        listOf(60f * s to 60.8f * s, 100f * s to 99.2f * s).forEach { (ex, ix) ->
                            drawCircle(Color.White, radius = 10.5f * s, center = Offset(ex, 65f * s))
                            drawCircle(
                                brush = Brush.radialGradient(listOf(Color(0xFF2DD4BF), Color(0xFF0284C7), Color(0xFF0F172A)), center = Offset(ix, 65.5f * s), radius = 9f * s),
                                radius = 8.2f * s,
                                center = Offset(ix, 65.5f * s),
                            )
                            drawCircle(Color(0xFF0F172A), radius = 4.3f * s, center = Offset(ix, 65.8f * s))
                            drawCircle(Color.White, radius = 2.7f * s, center = Offset(ex - 2.8f * s, 61.8f * s))
                            drawCircle(Color.White.copy(alpha = 0.9f), radius = 1.1f * s, center = Offset(ex + 3.2f * s, 68.8f * s))
                        }
                    }

                    // 3D Cute Nose + Whiskers
                    val nosePath = Path().apply {
                        moveTo(75.5f * s, 75.5f * s)
                        quadraticTo(80f * s, 73.5f * s, 84.5f * s, 75.5f * s)
                        lineTo(80f * s, 80.5f * s)
                        close()
                    }
                    drawPath(nosePath, Color(0xFFFB7185))
                    val wColor = Color(0xFF9A3412).copy(alpha = 0.65f)
                    drawLine(wColor, Offset(34f * s, 73f * s), Offset(47f * s, 75f * s), strokeWidth = 1.3f * s)
                    drawLine(wColor, Offset(33f * s, 80f * s), Offset(47f * s, 80f * s), strokeWidth = 1.3f * s)
                    drawLine(wColor, Offset(126f * s, 73f * s), Offset(113f * s, 75f * s), strokeWidth = 1.3f * s)
                    drawLine(wColor, Offset(127f * s, 80f * s), Offset(113f * s, 80f * s), strokeWidth = 1.3f * s)

                    // Mouth: Open / Chewing / Smile
                    when (mood) {
                        CikoMood.CATCHING -> drawOval(Color(0xFF9F1239), topLeft = Offset(74.5f * s, 80.5f * s), size = Size(11f * s, 13f * s))
                        CikoMood.CHEWING -> drawOval(Color(0xFFFB7185), topLeft = Offset(75.8f * s, 82.8f * s), size = Size(8.4f * s, 6.4f * s))
                        else -> {
                            val mouth = Path().apply {
                                moveTo(72f * s, 84f * s)
                                quadraticTo(76f * s, 89f * s, 80f * s, 84f * s)
                                quadraticTo(84f * s, 89f * s, 88f * s, 84f * s)
                            }
                            drawPath(mouth, Color(0xFF7C2D12), style = Stroke(width = 2.1f * s, cap = StrokeCap.Round))
                        }
                    }

                    // STAGE EVOLUTION HEADGEAR & PROPS
                    if (safeStage == 2) {
                        val cap = Path().apply {
                            moveTo(48f * s, 41f * s)
                            quadraticTo(80f * s, 20f * s, 112f * s, 41f * s)
                            close()
                        }
                        drawPath(cap, Color(0xFF0284C7))
                        val visor = Path().apply {
                            moveTo(52f * s, 40f * s)
                            quadraticTo(92f * s, 34f * s, 120f * s, 43f * s)
                            quadraticTo(95f * s, 47f * s, 52f * s, 40f * s)
                            close()
                        }
                        drawPath(visor, Color(0xFF38BDF8))
                    }
                    if (safeStage == 3 || safeStage == 4) {
                        // Gold Smart Glasses
                        drawCircle(Color(0xFFBAE6FD).copy(alpha = 0.2f), radius = 13f * s, center = Offset(60f * s, 65f * s))
                        drawCircle(Color(0xFFFBBF24), radius = 13f * s, center = Offset(60f * s, 65f * s), style = Stroke(2.4f * s))
                        drawCircle(Color(0xFFBAE6FD).copy(alpha = 0.2f), radius = 13f * s, center = Offset(100f * s, 65f * s))
                        drawCircle(Color(0xFFFBBF24), radius = 13f * s, center = Offset(100f * s, 65f * s), style = Stroke(2.4f * s))
                        drawLine(Color(0xFFFBBF24), Offset(73f * s, 65f * s), Offset(87f * s, 65f * s), strokeWidth = 2.4f * s)
                    }
                    if (safeStage == 3) {
                        val dCap = Path().apply {
                            moveTo(50f * s, 40f * s)
                            quadraticTo(80f * s, 21f * s, 110f * s, 40f * s)
                            close()
                        }
                        drawPath(dCap, Color(0xFF92400E))
                        // Glowing Magnifying Glass
                        drawLine(Color(0xFF78350F), Offset(115f * s, 108f * s), Offset(122f * s, 123f * s), strokeWidth = 4f * s, cap = StrokeCap.Round)
                        drawCircle(Color(0xFF38BDF8).copy(alpha = 0.28f), radius = 11f * s, center = Offset(113f * s, 96f * s))
                        drawCircle(Color(0xFFFBBF24), radius = 11f * s, center = Offset(113f * s, 96f * s), style = Stroke(2.8f * s))
                    }
                    if (safeStage == 4) {
                        // Graduation Mortarboard Toga Cap
                        val board = Path().apply {
                            moveTo(80f * s, 15f * s)
                            lineTo(118f * s, 29f * s)
                            lineTo(80f * s, 41f * s)
                            lineTo(42f * s, 29f * s)
                            close()
                        }
                        drawPath(board, Color(0xFF0F172A))
                        drawPath(board, Color(0xFFFBBF24), style = Stroke(1.8f * s))
                        drawLine(Color(0xFFFBBF24), Offset(115f * s, 30f * s), Offset(118f * s, 47f * s), strokeWidth = 2.5f * s, cap = StrokeCap.Round)
                        drawCircle(Color(0xFFFBBF24), radius = 3.2f * s, center = Offset(118f * s, 48f * s))
                    }
                    if (safeStage == 5) {
                        // Astronaut 3D Glass Dome Helmet + Mini Planet
                        drawCircle(
                            brush = Brush.radialGradient(
                                listOf(Color.White.copy(alpha = 0.4f), Color(0xFFBAE6FD).copy(alpha = 0.1f), Color(0xFF38BDF8).copy(alpha = 0.28f)),
                                center = Offset(62f * s, 44f * s),
                                radius = 58f * s,
                            ),
                            radius = 51f * s,
                            center = Offset(80f * s, 65f * s),
                        )
                        drawCircle(Color(0xFFE2E8F0), radius = 51f * s, center = Offset(80f * s, 65f * s), style = Stroke(2.5f * s))
                        drawCircle(Color(0xFF38BDF8), radius = 7f * s, center = Offset(132f * s, 34f * s))
                    }
                    if (safeStage >= 6) {
                        // Cyber Hologram Visor + Hovering Crystalline Crown
                        drawRoundRect(
                            color = Color(0xFF22D3EE).copy(alpha = 0.34f),
                            topLeft = Offset(86f * s, 54f * s),
                            size = Size(28f * s, 19f * s),
                            cornerRadius = CornerRadius(5f * s),
                        )
                        drawRoundRect(
                            color = Color(0xFF67E8F9),
                            topLeft = Offset(86f * s, 54f * s),
                            size = Size(28f * s, 19f * s),
                            cornerRadius = CornerRadius(5f * s),
                            style = Stroke(1.8f * s),
                        )
                        val crown = Path().apply {
                            moveTo(64f * s, 29f * s)
                            lineTo(68f * s, 17f * s)
                            lineTo(76f * s, 25f * s)
                            lineTo(80f * s, 13f * s)
                            lineTo(84f * s, 25f * s)
                            lineTo(92f * s, 17f * s)
                            lineTo(96f * s, 29f * s)
                            close()
                        }
                        drawPath(crown, Color(0xFFFBBF24))
                        drawPath(crown, Color(0xFF67E8F9), style = Stroke(1.4f * s))
                    }
                }
            }

            // 4. Flying Golden Biscuit Treat into Ciko's Mouth!
            if (mood == CikoMood.CATCHING) {
                val p = cookieProgress.value
                val cx = (142f * (1f - p) + 80f * p) * s
                val arcLift = -42f * sin(p * PI).toFloat()
                val cy = (48f * (1f - p) + 87f * p + arcLift) * s
                val cRadius = (10f * (1f - 0.55f * p)) * s
                drawCircle(
                    brush = Brush.radialGradient(listOf(Color(0xFFFEF08A), Color(0xFFF59E0B), Color(0xFFB45309))),
                    radius = cRadius,
                    center = Offset(cx, cy),
                )
                drawCircle(Color(0xFFFEF08A), radius = cRadius, center = Offset(cx, cy), style = Stroke(1.5f * s))
            }

            // 5. Floating Heart/Star Particles
            if (particleAnim.value in 0.01f..0.99f) {
                val ptAlpha = (1f - particleAnim.value).coerceIn(0f, 1f)
                val rise = particleAnim.value * 44f * s
                listOf(42f * s, 78f * s, 118f * s).forEachIndexed { i, px ->
                    val yPos = (76f * s - rise - (i * 8f * s))
                    drawCircle(
                        color = (if (mood == CikoMood.EVOLVING) Color(0xFFFDE047) else Color(0xFFFB7185)).copy(alpha = ptAlpha),
                        radius = 4.2f * s,
                        center = Offset(px, yPos),
                    )
                }
            }
        }
    }
}
