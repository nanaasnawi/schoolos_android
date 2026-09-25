package com.schoolos.android.feature.quizzes

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.schoolos.android.core.designsystem.AccentNeonAmber
import com.schoolos.android.core.designsystem.CosmicBlack
import com.schoolos.android.core.designsystem.CosmicNavy
import com.schoolos.android.core.designsystem.CosmicSurface
import com.schoolos.android.core.designsystem.CosmicSurface2
import com.schoolos.android.core.designsystem.ErrorState
import com.schoolos.android.core.designsystem.ExecutiveTopBar
import com.schoolos.android.core.designsystem.GlassBorder
import com.schoolos.android.core.designsystem.GlassBorder2
import com.schoolos.android.core.designsystem.GlassCard
import com.schoolos.android.core.designsystem.LoadingState
import com.schoolos.android.core.designsystem.NeonBlue
import com.schoolos.android.core.designsystem.NeonError
import com.schoolos.android.core.designsystem.NeonSuccess
import com.schoolos.android.core.designsystem.NeonWarning
import com.schoolos.android.core.designsystem.StatusChip
import com.schoolos.android.core.designsystem.StudentNeon
import com.schoolos.android.core.designsystem.TextPrimary
import com.schoolos.android.core.designsystem.TextSecondary
import com.schoolos.android.core.designsystem.TextTertiary

@Composable
fun QuizDetailScreen(
    onBack: (() -> Unit) = {},
    onAttemptStarted: (String) -> Unit = {},
    viewModel: QuizDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.attempt) {
        state.attempt?.let { onAttemptStarted(it.id) }
    }

    Scaffold(
        containerColor = CosmicBlack,
        topBar = {
            ExecutiveTopBar(
                title = state.quiz?.title ?: "Detail Kuis CBT",
                subtitle = state.quiz?.let { "Kuis CBT • ${it.subjectName ?: "Evaluasi Mandiri"}" } ?: "Evaluasi Siswa",
                onBack = onBack,
                actions = {
                    state.quiz?.let { q ->
                        StatusChip(label = q.status)
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                state.isLoading -> LoadingState()
                state.error != null -> {
                    ErrorState(message = state.error!!, onRetry = viewModel::load)
                }
                state.quiz != null -> {
                    val q = state.quiz!!
                    val isAvailable = q.status.lowercase() in listOf("active", "open", "published", "draft")
                    val timeLimit = q.timeLimitMinutes

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // ── 1. COSMIC HERO BANNER CARD ────────────────────────
                        CosmicQuizHeroCard(
                            title = q.title,
                            subjectName = q.subjectName ?: "CBT Evaluasi",
                            maxScore = q.maxScore
                        )

                        // ── 2. METRICS ROW (Soal, Waktu, Poin) ────────────────
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CosmicMetricCard(
                                label = "Total Soal",
                                value = "${q.questionsCount}",
                                unit = "Soal",
                                icon = Icons.AutoMirrored.Filled.List,
                                accentColor = NeonBlue,
                                modifier = Modifier.weight(1f)
                            )
                            CosmicMetricCard(
                                label = "Batas Waktu",
                                value = if (timeLimit != null && timeLimit > 0) "$timeLimit" else "∞",
                                unit = if (timeLimit != null && timeLimit > 0) "Menit" else "Bebas",
                                icon = Icons.Default.Timer,
                                accentColor = NeonWarning,
                                modifier = Modifier.weight(1f)
                            )
                            CosmicMetricCard(
                                label = "Poin Maksimal",
                                value = "${q.maxScore}",
                                unit = "Poin",
                                icon = Icons.Default.EmojiEvents,
                                accentColor = NeonSuccess,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // ── 3. DESKRIPSI KUIS ─────────────────────────────────
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Deskripsi & Instruksi",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = TextPrimary
                            )
                            GlassCard(cornerRadius = 14.dp) {
                                Text(
                                    text = q.description?.takeIf { it.isNotBlank() }
                                        ?: "Kuis evaluasi pemahaman materi. Kerjakan dengan teliti dan jujur untuk mengukur kompetensi pembelajaran Anda.",
                                    fontSize = 13.sp,
                                    color = TextSecondary,
                                    lineHeight = 20.sp,
                                    modifier = Modifier.padding(14.dp)
                                )
                            }
                        }

                        // ── 4. TATA TERTIB & PANDUAN PENGERJAAN ────────────────
                        CbtRulesCard()

                        // ── 5. ERROR ALERT (Stylized if start fails) ───────────
                        if (state.startError != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(NeonError.copy(alpha = 0.12f))
                                    .border(1.dp, NeonError.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = NeonError,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = state.startError ?: "Terjadi kesalahan saat memulai kuis.",
                                    color = NeonError,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        // ── 6. PRIMARY ACTION BUTTON ──────────────────────────
                        Button(
                            onClick = viewModel::startAttempt,
                            enabled = !state.isStarting && isAvailable,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (isAvailable && !state.isStarting) {
                                        Brush.horizontalGradient(
                                            listOf(StudentNeon, Color(0xFF00B4D8))
                                        )
                                    } else {
                                        Brush.horizontalGradient(
                                            listOf(CosmicSurface2, CosmicSurface)
                                        )
                                    }
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isAvailable && !state.isStarting) GlassBorder2 else GlassBorder,
                                    shape = RoundedCornerShape(14.dp)
                                )
                        ) {
                            if (state.isStarting) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    "Menyiapkan Soal...",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            } else {
                                Text(
                                    if (isAvailable) "MULAI KERJAKAN" else "KUIS BELUM TERSEDIA",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (isAvailable) Color.White else TextTertiary,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }

                        Spacer(Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun CosmicQuizHeroCard(
    title: String,
    subjectName: String,
    maxScore: Int,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(CosmicNavy, CosmicSurface)
                )
            )
            .border(0.5.dp, GlassBorder, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Potensi XP Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(AccentNeonAmber.copy(alpha = 0.15f))
                        .border(0.5.dp, AccentNeonAmber.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "⭐ Potensi ${maxScore * 5} XP",
                        color = AccentNeonAmber,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Mapel Pill
                Text(
                    text = subjectName,
                    color = TextTertiary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary,
                lineHeight = 24.sp
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(NeonSuccess)
                )
                Text(
                    text = "CBT Online Terstandarisasi",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun CosmicMetricCard(
    label: String,
    value: String,
    unit: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(CosmicNavy)
            .border(0.5.dp, GlassBorder, RoundedCornerShape(14.dp))
            .padding(vertical = 12.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = value,
                fontWeight = FontWeight.Black,
                fontSize = 16.sp,
                color = TextPrimary
            )
            Text(
                text = "$unit • $label",
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextTertiary,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun CbtRulesCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CosmicNavy.copy(alpha = 0.7f))
            .border(0.5.dp, NeonWarning.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = NeonWarning,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Panduan Pengerjaan CBT",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonWarning
                )
            }

            RuleBulletItem(text = "Pastikan koneksi internet stabil sebelum menekan tombol Mulai.")
            RuleBulletItem(text = "Jawaban otomatis tersimpan setiap kali Anda berpindah butir soal.")
            RuleBulletItem(text = "Pengatur waktu (timer) berjalan otomatis dan kuis akan dikumpulkan saat waktu habis.")
        }
    }
}

@Composable
private fun RuleBulletItem(text: String) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(start = 2.dp)
    ) {
        Text("•", fontSize = 12.sp, color = TextTertiary, fontWeight = FontWeight.Bold)
        Text(
            text = text,
            fontSize = 11.sp,
            color = TextSecondary,
            lineHeight = 16.sp
        )
    }
}
