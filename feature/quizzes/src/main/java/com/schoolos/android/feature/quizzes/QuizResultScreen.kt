package com.schoolos.android.feature.quizzes

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.schoolos.android.core.R as CoreR
import com.schoolos.android.core.designsystem.*
import kotlin.random.Random

@Composable
private fun statusFromAttempt(isPassed: Boolean): Pair<String, Color> {
    val isDark = LocalIsDarkTheme.current
    return if (isPassed) {
        Pair("Kompeten & Tuntas KKTP", if (isDark) Color(0xFF34D399) else Color(0xFF16A34A))
    } else {
        Pair("Perlu Penguatan Modul", if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706))
    }
}

private fun motivationQuote(isPassed: Boolean): String {
    return if (isPassed) {
        "\"Capaian pembelajaran modul tuntas. Pertahankan prestasi dan ketekunan belajar!\""
    } else {
        "\"Tetap semangat. Manfaatkan bimbingan tutorial bersama tutor pamong untuk penguatan capaian modul.\""
    }
}

@Composable
fun QuizResultScreen(
    onBack: () -> Unit = {},
    viewModel: QuizResultViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    val isDark = LocalIsDarkTheme.current
    val cardBg = if (isDark) Color(0xFF111113) else Color.White
    val cardBorder = if (isDark) Color(0xFF1F1F23) else Color(0xFFE2E8F0)
    val innerCardBg = if (isDark) Color(0xFF161618) else Color(0xFFF8FAFC)
    val innerCardBorder = if (isDark) Color(0xFF27272A) else Color(0xFFE2E8F0)
    val textPrimary = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFFA1A1AA) else Color(0xFF64748B)
    val textTertiary = if (isDark) Color(0xFF71717A) else Color(0xFF94A3B8)
    val studentAccent = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)
    val warningColor = if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706)
    val successColor = if (isDark) Color(0xFF34D399) else Color(0xFF16A34A)
    val errorColor = if (isDark) Color(0xFFF87171) else Color(0xFFDC2626)

    when {
        state.isLoading -> QuizResultShimmer()
        else -> {
            val isGraded = state.attempt?.status == "graded"
            val score    = state.attempt?.score ?: 0
            val maxScore = state.attempt?.totalPoints?.let { if (it > 0) it else 100 } ?: 100
            val pct = if (maxScore == 0) 0f else score.toFloat() / maxScore
            val isPassed = state.attempt?.passed ?: (pct >= 0.75f)
            val (statusLabel, statusColor) = statusFromAttempt(isPassed)
            val isCelebration = isGraded && isPassed

            Box(modifier = Modifier.fillMaxSize().background(CosmicBlack)) {
                if (isCelebration) {
                    ConfettiCanvas(modifier = Modifier.fillMaxSize())
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(Modifier.height(28.dp))

                    if (!isGraded) {
                        // Belum Dinilai Tutor (Menunggu Koreksi)
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(warningColor.copy(alpha = 0.14f))
                                .border(1.dp, warningColor.copy(alpha = 0.35f), CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                painter = painterResource(id = CoreR.drawable.ic_modern_clock),
                                contentDescription = null,
                                tint = warningColor,
                                modifier = Modifier.size(34.dp)
                            )
                        }
                        Spacer(Modifier.height(14.dp))
                        Text(
                            "Jawaban Berhasil Terkirim",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = textPrimary,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Lembar pengerjaan kuis CBT telah tersimpan di server",
                            fontSize = 12.5.sp,
                            color = textSecondary,
                            textAlign = TextAlign.Center
                        )

                        Spacer(Modifier.height(20.dp))

                        Surface(
                            shape = RoundedCornerShape(22.dp),
                            color = cardBg,
                            border = BorderStroke(1.dp, cardBorder),
                            shadowElevation = if (isDark) 0.dp else 1.dp
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = warningColor.copy(alpha = 0.12f),
                                    border = BorderStroke(1.dp, warningColor.copy(alpha = 0.35f))
                                ) {
                                    Text(
                                        text = "MENUNGGU VERIFIKASI TUTOR",
                                        fontWeight = FontWeight.Bold,
                                        color = warningColor,
                                        fontSize = 10.5.sp,
                                        letterSpacing = 0.5.sp,
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                    )
                                }
                                Spacer(Modifier.height(10.dp))
                                Text(
                                    "Koreksi hasil evaluasi dan soal uraian diserahkan kepada Tutor Pamong pengampu. Nilai resmi dan umpan balik belajar akan tampil setelah diverifikasi.",
                                    fontSize = 12.5.sp,
                                    textAlign = TextAlign.Center,
                                    color = textSecondary,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    } else {
                        // Sudah Dinilai oleh Tutor
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(statusColor.copy(alpha = 0.14f))
                                .border(1.dp, statusColor.copy(alpha = 0.35f), CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                painter = painterResource(id = CoreR.drawable.ic_modern_check_circle),
                                contentDescription = null,
                                tint = statusColor,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(
                            if (isCelebration) "Luar Biasa, Selamat!" else "Evaluasi Selesai!",
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            color = textPrimary,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Hasil evaluasi telah dinilai oleh Tutor Pamong",
                            fontSize = 12.5.sp,
                            color = textSecondary,
                        )

                        Spacer(Modifier.height(24.dp))

                        // Animated score circle
                        AnimatedScoreCircle(
                            score = score,
                            maxScore = maxScore,
                            size = 140.dp,
                            color = statusColor,
                        )

                        Spacer(Modifier.height(14.dp))

                        // KKTP Achievement badge
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = statusColor.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, statusColor.copy(alpha = 0.35f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Icon(
                                    painter = painterResource(id = CoreR.drawable.ic_modern_check_circle),
                                    contentDescription = null,
                                    tint = statusColor,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    statusLabel,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = statusColor,
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    // Stats breakdown Card
                    val attempt = state.attempt
                    val hasAnswers = attempt?.answers?.isNotEmpty() == true
                    val correctCount = if (hasAnswers) attempt!!.correctCount else (pct * 10).toInt()
                    val incorrectCount = if (hasAnswers) attempt!!.incorrectCount else (10 - correctCount).coerceAtLeast(0)

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        color = cardBg,
                        border = BorderStroke(1.dp, cardBorder),
                        shadowElevation = if (isDark) 0.dp else 1.dp,
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            Text(
                                "Ringkasan Hasil Evaluasi",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.5.sp,
                                color = textPrimary,
                            )
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                ModernStatItem(
                                    iconRes = CoreR.drawable.ic_modern_check_circle,
                                    label = "Benar",
                                    value = "$correctCount",
                                    color = successColor,
                                    innerCardBg = innerCardBg,
                                    innerCardBorder = innerCardBorder,
                                    textTertiary = textTertiary,
                                    modifier = Modifier.weight(1f)
                                )
                                ModernStatItem(
                                    iconRes = CoreR.drawable.ic_modern_warning,
                                    label = "Salah",
                                    value = "$incorrectCount",
                                    color = errorColor,
                                    innerCardBg = innerCardBg,
                                    innerCardBorder = innerCardBorder,
                                    textTertiary = textTertiary,
                                    modifier = Modifier.weight(1f)
                                )
                                ModernStatItem(
                                    iconRes = CoreR.drawable.ic_modern_grades,
                                    label = "Nilai Akhir",
                                    value = "$score",
                                    color = studentAccent,
                                    innerCardBg = innerCardBg,
                                    innerCardBorder = innerCardBorder,
                                    textTertiary = textTertiary,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    // Motivational quote card
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        color = innerCardBg,
                        border = BorderStroke(1.dp, innerCardBorder),
                    ) {
                        Text(
                            text = motivationQuote(isPassed),
                            fontSize = 13.sp,
                            color = statusColor,
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 18.sp,
                            modifier = Modifier.padding(16.dp)
                        )
                    }

                    // Detailed Answer Review List (if available)
                    if (hasAnswers) {
                        Spacer(Modifier.height(20.dp))
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Text(
                                "Rincian Jawaban Soal",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = textPrimary,
                                modifier = Modifier.padding(start = 4.dp),
                            )

                            attempt!!.answers.forEachIndexed { idx, ans ->
                                val isCorrect = ans.isCorrect == true
                                val badgeColor = if (isCorrect) successColor else errorColor
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(18.dp),
                                    color = cardBg,
                                    border = BorderStroke(1.dp, cardBorder),
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Text(
                                                "Soal ${idx + 1}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = textPrimary,
                                            )
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = badgeColor.copy(alpha = 0.12f),
                                                border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.35f))
                                            ) {
                                                Text(
                                                    if (isCorrect) "+${ans.pointsEarned} Pts (Benar)" else "0 Pts (Salah)",
                                                    fontSize = 10.5.sp,
                                                    color = badgeColor,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                                )
                                            }
                                        }

                                        val qText = ans.questionText
                                        if (!qText.isNullOrBlank()) {
                                            Text(
                                                qText,
                                                fontSize = 13.sp,
                                                color = textPrimary,
                                                fontWeight = FontWeight.Medium,
                                                lineHeight = 18.sp,
                                            )
                                        }

                                        val answerText = ans.chosenChoiceText ?: ans.textAnswer
                                        if (!answerText.isNullOrBlank()) {
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = innerCardBg,
                                                border = BorderStroke(1.dp, innerCardBorder),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text(
                                                    "Jawaban kamu: $answerText",
                                                    fontSize = 12.sp,
                                                    color = if (isCorrect) successColor else textSecondary,
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                                )
                                            }
                                        }

                                        val feedback = ans.teacherFeedback
                                        if (!feedback.isNullOrBlank()) {
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = warningColor.copy(alpha = 0.10f),
                                                border = BorderStroke(1.dp, warningColor.copy(alpha = 0.35f)),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text(
                                                    "Catatan Tutor: $feedback",
                                                    fontSize = 11.5.sp,
                                                    color = warningColor,
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(28.dp))

                    // Action button: Return to Home
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable(onClick = onBack),
                        shape = RoundedCornerShape(16.dp),
                        color = studentAccent,
                        shadowElevation = if (isDark) 0.dp else 2.dp,
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                painter = painterResource(id = CoreR.drawable.ic_modern_home),
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Kembali ke Beranda", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }

                    Spacer(Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
private fun ModernStatItem(
    iconRes: Int,
    label: String,
    value: String,
    color: Color,
    innerCardBg: Color,
    innerCardBorder: Color,
    textTertiary: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.height(72.dp),
        shape = RoundedCornerShape(14.dp),
        color = innerCardBg,
        border = BorderStroke(1.dp, innerCardBorder),
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.height(3.dp))
            Text(value, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = color)
            Text(label, fontSize = 10.5.sp, color = textTertiary)
        }
    }
}

@Composable
private fun QuizResultShimmer() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CosmicBlack)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState(), enabled = false)
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(40.dp))
        ShimmerBox(modifier = Modifier.size(80.dp).clip(CircleShape))
        Spacer(Modifier.height(16.dp))
        ShimmerBox(modifier = Modifier.width(180.dp).height(24.dp).clip(RoundedCornerShape(8.dp)))
        Spacer(Modifier.height(8.dp))
        ShimmerBox(modifier = Modifier.width(220.dp).height(16.dp).clip(RoundedCornerShape(6.dp)))
        Spacer(Modifier.height(32.dp))
        ShimmerBox(modifier = Modifier.size(140.dp).clip(CircleShape))
        Spacer(Modifier.height(24.dp))
        ShimmerBox(modifier = Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(22.dp)))
        Spacer(Modifier.height(16.dp))
        ShimmerBox(modifier = Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(16.dp)))
    }
}

private data class ConfettiParticle(
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val size: Float,
    val color: Color,
    val rotation: Float,
    val rotSpeed: Float,
)

@Composable
private fun ConfettiCanvas(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "confettiTransition")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "confettiAnim",
    )

    val colors = listOf(
        Color(0xFF38BDF8),
        Color(0xFF10B981),
        Color(0xFFFBBF24),
        Color(0xFFA855F7),
        Color(0xFFEC4899),
    )

    val particles = remember {
        val rng = Random(42)
        List(40) {
            ConfettiParticle(
                x = rng.nextFloat(),
                y = rng.nextFloat() * -0.5f,
                vx = (rng.nextFloat() - 0.5f) * 0.3f,
                vy = rng.nextFloat() * 0.8f + 0.4f,
                size = rng.nextFloat() * 7f + 5f,
                color = colors[rng.nextInt(colors.size)],
                rotation = rng.nextFloat() * 360f,
                rotSpeed = (rng.nextFloat() - 0.5f) * 180f,
            )
        }
    }

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        for (p in particles) {
            val curY = ((p.y + p.vy * progress) % 1.2f) * h
            val curX = (p.x + p.vx * progress) * w
            val curRot = p.rotation + p.rotSpeed * progress

            if (curY in 0f..h && curX in 0f..w) {
                rotate(curRot, pivot = Offset(curX, curY)) {
                    drawRect(
                        color = p.color.copy(alpha = (1f - (curY / h) * 0.6f).coerceIn(0f, 1f)),
                        topLeft = Offset(curX - p.size / 2, curY - p.size / 2),
                        size = androidx.compose.ui.geometry.Size(p.size, p.size * 0.6f),
                    )
                }
            }
        }
    }
}
