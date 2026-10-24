package com.schoolos.android.feature.quizzes

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.schoolos.android.core.designsystem.AnimatedScoreCircle
import com.schoolos.android.core.designsystem.CosmicBlack
import com.schoolos.android.core.designsystem.CosmicNavy
import com.schoolos.android.core.designsystem.GlassBorder2
import com.schoolos.android.core.designsystem.ShimmerBox
import com.schoolos.android.core.designsystem.NeonBlue
import com.schoolos.android.core.designsystem.NeonError
import com.schoolos.android.core.designsystem.NeonSuccess
import com.schoolos.android.core.designsystem.NeonWarning
import com.schoolos.android.core.designsystem.StudentNeon
import com.schoolos.android.core.designsystem.TextPrimary
import com.schoolos.android.core.designsystem.TextSecondary
import com.schoolos.android.core.designsystem.TextTertiary
import kotlin.random.Random

private fun statusFromAttempt(isPassed: Boolean): Pair<String, Color> {
    return if (isPassed) {
        Pair("Kompeten & Tuntas KKTP", NeonSuccess)
    } else {
        Pair("Perlu Penguatan Modul", NeonWarning)
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

            Box(modifier = Modifier.fillMaxSize()) {
                // Dark cosmic background
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(CosmicBlack)
                )

                // Confetti particles (only if celebration and already graded by teacher)
                if (isCelebration) {
                    ConfettiCanvas(modifier = Modifier.fillMaxSize())
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(Modifier.height(32.dp))

                    if (!isGraded) {
                        // Belum Dinilai Tutor (Menunggu Koreksi)
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(NeonWarning.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Assessment,
                                contentDescription = null,
                                tint = NeonWarning,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Jawaban Berhasil Terkirim",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 22.sp,
                            color = NeonWarning,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Lembar pengerjaan telah tersimpan di server",
                            fontSize = 13.sp,
                            color = TextTertiary,
                        )

                        Spacer(Modifier.height(24.dp))

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = CosmicNavy,
                            border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder2)
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    "Menunggu Koreksi Tutor Pamong",
                                    fontWeight = FontWeight.Bold,
                                    color = NeonWarning,
                                    fontSize = 16.sp
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "Koreksi hasil kuis dan evaluasi modul diserahkan kepada Tutor pengampu. Nilai resmi dan pembahasan akan tampil setelah diverifikasi oleh Tutor.",
                                    style = MaterialTheme.typography.bodySmall,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    color = TextSecondary,
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
                                .background(statusColor.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = if (isCelebration) Icons.Default.EmojiEvents else Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = statusColor,
                                modifier = Modifier.size(38.dp)
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            if (isCelebration) "Selamat!" else "Kamu sudah berusaha!",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 24.sp,
                            color = statusColor,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Uji kompetensi telah dinilai oleh Tutor Pamong",
                            fontSize = 13.sp,
                            color = TextTertiary,
                        )

                        Spacer(Modifier.height(28.dp))

                        // Animated score circle
                        AnimatedScoreCircle(
                            score = score,
                            maxScore = maxScore,
                            size = 140.dp,
                            color = statusColor,
                        )

                        Spacer(Modifier.height(16.dp))

                        // KKTP Achievement badge
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = statusColor.copy(alpha = 0.12f),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = if (isPassed) Icons.Default.CheckCircle else Icons.Default.Info,
                                    contentDescription = null,
                                    tint = statusColor,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    statusLabel,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = statusColor,
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    // Stats breakdown
                    val attempt = state.attempt
                    val hasAnswers = attempt?.answers?.isNotEmpty() == true
                    val correctCount = if (hasAnswers) attempt!!.correctCount else (pct * 10).toInt()
                    val incorrectCount = if (hasAnswers) attempt!!.incorrectCount else (10 - correctCount).coerceAtLeast(0)

                    Box(
                        modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(CosmicNavy)
                            .border(1.dp, GlassBorder2, RoundedCornerShape(20.dp))
                            .padding(14.dp),
                    ) {
                        Column {
                            Text(
                                "Ringkasan Hasil Evaluasi",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = TextPrimary,
                            )
                            Spacer(Modifier.height(14.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                StatItem(Icons.Default.CheckCircle, "Benar", "$correctCount", NeonSuccess)
                                StatItem(Icons.Default.Close, "Salah", "$incorrectCount", NeonError)
                                StatItem(Icons.Default.Assessment, "Nilai", "$score", StudentNeon)
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Motivational quote card
                    Box(
                        modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(statusColor.copy(alpha = 0.08f))
                            .border(1.dp, statusColor.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                            .padding(14.dp),
                    ) {
                        Text(
                            motivationQuote(isPassed),
                            fontSize = 14.sp,
                            color = statusColor,
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Medium,
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
                                color = TextPrimary,
                                modifier = Modifier.padding(start = 4.dp),
                            )

                            attempt!!.answers.forEachIndexed { idx, ans ->
                                val isCorrect = ans.isCorrect == true
                                val badgeColor = if (isCorrect) NeonSuccess else NeonError
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(CosmicNavy)
                                        .border(1.dp, badgeColor.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                                        .padding(14.dp)
                                ) {
                                    Column {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Text(
                                                "Soal ${idx + 1}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = TextSecondary,
                                            )
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = badgeColor.copy(alpha = 0.12f),
                                            ) {
                                                Text(
                                                    if (isCorrect) "+${ans.pointsEarned} pts (Benar)" else "0 pts (Salah)",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = badgeColor,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                )
                                            }
                                        }

                                        val qText = ans.questionText
                                        if (!qText.isNullOrBlank()) {
                                            Spacer(Modifier.height(6.dp))
                                            Text(
                                                qText,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = TextPrimary,
                                                fontWeight = FontWeight.Medium,
                                            )
                                        }

                                        val answerText = ans.chosenChoiceText ?: ans.textAnswer
                                        if (!answerText.isNullOrBlank()) {
                                            Spacer(Modifier.height(6.dp))
                                            Text(
                                                "Jawaban kamu: $answerText",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (isCorrect) NeonSuccess else TextTertiary,
                                            )
                                        }

                                        val feedback = ans.teacherFeedback
                                        if (!feedback.isNullOrBlank()) {
                                            Spacer(Modifier.height(6.dp))
                                            Text(
                                                "Catatan Guru: $feedback",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = NeonWarning,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(32.dp))

                    // Action buttons
                    Box(
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Brush.horizontalGradient(listOf(statusColor, statusColor.copy(alpha = 0.7f))))
                            .clickable(onClick = onBack),
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Home, null, tint = CosmicBlack)
                            Spacer(Modifier.width(8.dp))
                            Text("Kembali ke Beranda", color = CosmicBlack, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                        }
                    }

                    Spacer(Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
private fun StatItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        Spacer(Modifier.height(4.dp))
        Text(value, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = color)
        Text(label, fontSize = 11.sp, color = TextTertiary)
    }
}

/**
 * Skeleton for the quiz / CBT result page. Mirrors the real layout (reaction emoji,
 * headline, score ring, grade badge, summary card, quote, answer review, CTA) so the
 * result fades in without layout jump instead of showing a bare spinner.
 */
@Composable
private fun QuizResultShimmer() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CosmicBlack)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState(), enabled = false)
            .padding(horizontal = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(32.dp))
        // Reaction emoji
        ShimmerBox(Modifier.size(76.dp), CircleShape)
        Spacer(Modifier.height(12.dp))
        // Headline + subtitle
        ShimmerBox(Modifier.width(170.dp).height(24.dp), RoundedCornerShape(6.dp))
        Spacer(Modifier.height(8.dp))
        ShimmerBox(Modifier.width(190.dp).height(12.dp), RoundedCornerShape(4.dp))

        Spacer(Modifier.height(28.dp))
        // Score ring: shimmering ring with a hollow centre
        Box(contentAlignment = Alignment.Center) {
            ShimmerBox(Modifier.size(140.dp), CircleShape)
            Box(
                modifier = Modifier
                    .size(116.dp)
                    .clip(CircleShape)
                    .background(CosmicBlack),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    ShimmerBox(Modifier.width(56.dp).height(28.dp), RoundedCornerShape(6.dp))
                    Spacer(Modifier.height(6.dp))
                    ShimmerBox(Modifier.width(36.dp).height(10.dp), RoundedCornerShape(3.dp))
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        // Grade badge
        ShimmerBox(Modifier.width(220.dp).height(44.dp), RoundedCornerShape(14.dp))

        Spacer(Modifier.height(24.dp))
        // Summary card
        ResultSkeletonCard {
            ShimmerBox(Modifier.width(170.dp).height(14.dp), RoundedCornerShape(4.dp))
            Spacer(Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                repeat(3) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        ShimmerBox(Modifier.size(24.dp), CircleShape)
                        Spacer(Modifier.height(6.dp))
                        ShimmerBox(Modifier.width(32.dp).height(22.dp), RoundedCornerShape(5.dp))
                        Spacer(Modifier.height(4.dp))
                        ShimmerBox(Modifier.width(40.dp).height(10.dp), RoundedCornerShape(3.dp))
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        // Motivational quote
        ShimmerBox(Modifier.fillMaxWidth().height(62.dp), RoundedCornerShape(20.dp))

        Spacer(Modifier.height(20.dp))
        // Answer review header + items
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ShimmerBox(
                Modifier.padding(start = 4.dp).width(150.dp).height(15.dp),
                RoundedCornerShape(4.dp),
            )
            repeat(3) {
                ResultSkeletonCard(cornerRadius = 16.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ShimmerBox(Modifier.width(56.dp).height(13.dp), RoundedCornerShape(4.dp))
                        ShimmerBox(Modifier.width(96.dp).height(22.dp), RoundedCornerShape(8.dp))
                    }
                    Spacer(Modifier.height(10.dp))
                    ShimmerBox(Modifier.fillMaxWidth(0.9f).height(13.dp), RoundedCornerShape(4.dp))
                    Spacer(Modifier.height(6.dp))
                    ShimmerBox(Modifier.fillMaxWidth(0.55f).height(11.dp), RoundedCornerShape(4.dp))
                }
            }
        }

        Spacer(Modifier.height(32.dp))
        // CTA button
        ShimmerBox(Modifier.fillMaxWidth().height(52.dp), RoundedCornerShape(14.dp))
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun ResultSkeletonCard(
    cornerRadius: androidx.compose.ui.unit.Dp = 20.dp,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(cornerRadius))
            .background(CosmicNavy)
            .border(1.dp, GlassBorder2, RoundedCornerShape(cornerRadius))
            .padding(14.dp),
    ) {
        Column { content() }
    }
}

@Composable
private fun ConfettiCanvas(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "confetti")
    val animProgress by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3000, easing = FastOutSlowInEasing), RepeatMode.Restart),
        label = "confettiAnim",
    )
    val confettiColors = listOf(
        Color(0xFFB18CFF), Color(0xFF00E676), Color(0xFF4FC3F7),
        Color(0xFFFFB300), Color(0xFFFF5252), Color(0xFF18FFFF),
    )
    val particles = remember {
        List(50) {
            Triple(Random.nextFloat(), Random.nextFloat(), Random.nextInt(confettiColors.size))
        }
    }
    Canvas(modifier = modifier) {
        particles.forEach { (startX, startY, colorIdx) ->
            val x = startX * size.width
            val y = ((startY + animProgress * 1.2f) % 1f) * size.height
            rotate(animProgress * 360f, Offset(x, y)) {
                drawRect(
                    color = confettiColors[colorIdx].copy(alpha = 0.7f),
                    topLeft = Offset(x - 4f, y - 4f),
                    size = androidx.compose.ui.geometry.Size(8f, 8f),
                )
            }
        }
    }
}
