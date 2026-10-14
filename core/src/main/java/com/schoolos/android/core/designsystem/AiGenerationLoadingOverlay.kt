package com.schoolos.android.core.designsystem

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

enum class AiGenerationType {
    MATERIAL,
    ASSIGNMENT,
    QUIZ
}

private data class AiStepData(
    val title: String,
    val subtitle: String
)

@Composable
fun AiGenerationLoadingOverlay(
    isVisible: Boolean,
    type: AiGenerationType,
    subjectName: String = "",
    topic: String = "",
    gradeLevel: String = "",
    difficulty: String = "Sedang",
    modifier: Modifier = Modifier.fillMaxSize()
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(250)),
        exit = fadeOut(animationSpec = tween(200)),
        modifier = modifier
    ) {
        var currentStep by remember { mutableIntStateOf(0) }

        LaunchedEffect(isVisible) {
            if (isVisible) {
                currentStep = 0
                while (true) {
                    delay(2200L)
                    currentStep = (currentStep + 1) % 5
                }
            } else {
                currentStep = 0
            }
        }

        val effectiveSub = subjectName.ifBlank { "Mata Pelajaran" }
        val effectiveTopic = topic.ifBlank { "Topik Materi" }
        val effectiveGrade = gradeLevel.ifBlank { "Semua Jenjang" }

        val badgeText = when (type) {
            AiGenerationType.MATERIAL -> "AI SEDANG MERACIK MATERI & MODUL AJAR"
            AiGenerationType.ASSIGNMENT -> "AI SEDANG MENYINTESIS LEMBAR TUGAS"
            AiGenerationType.QUIZ -> "AI SEDANG MENYINTESIS UJIAN CBT"
        }

        val stepData = remember(type, effectiveSub, effectiveTopic, effectiveGrade, difficulty) {
            when (type) {
                AiGenerationType.MATERIAL -> listOf(
                    AiStepData(
                        title = "Menghubungkan ke Engine AI Kurikulum Merdeka",
                        subtitle = "Menginisialisasi parameter model kecerdasan buatan kurikulum..."
                    ),
                    AiStepData(
                        title = "Mempelajari Silabus & Capaian Mapel",
                        subtitle = "Menyesuaikan capaian pembelajaran $effectiveSub ($effectiveGrade)..."
                    ),
                    AiStepData(
                        title = "Menyintesis Konsep Visual & Rangkuman",
                        subtitle = "Menyusun struktur hierarki konsep materi '$effectiveTopic'..."
                    ),
                    AiStepData(
                        title = "Menyelaraskan Gambar Diagram Edukatif",
                        subtitle = "Memilih referensi visual edukasi yang aman dan kontekstual..."
                    ),
                    AiStepData(
                        title = "Memfinalisasi Modul Ajar Siap Publikasi",
                        subtitle = "Mengemas tata letak materi siap didistribusikan ke siswa..."
                    )
                )

                AiGenerationType.ASSIGNMENT -> listOf(
                    AiStepData(
                        title = "Menghubungkan ke Engine AI Kurikulum Merdeka",
                        subtitle = "Mempersiapkan parameter lembar kerja siswa terstruktur..."
                    ),
                    AiStepData(
                        title = "Menganalisis Capaian Pembelajaran & Indikator",
                        subtitle = "Menyelaraskan indikator kompetensi mapel $effectiveSub ($effectiveGrade)..."
                    ),
                    AiStepData(
                        title = "Menyusun Petunjuk Praktik & Langkah Tugas",
                        subtitle = "Menyusun instruksi penugasan langkah demi langkah untuk '$effectiveTopic'..."
                    ),
                    AiStepData(
                        title = "Merancang Rubrik Penilaian & Kriteria Skor",
                        subtitle = "Membuat pedoman penskoran objektif dan rubrik penilaian terstandar..."
                    ),
                    AiStepData(
                        title = "Memfinalisasi Lembar Kerja Tugas Siswa",
                        subtitle = "Mengemas lembar tugas lengkap siap didistribusikan ke siswa..."
                    )
                )

                AiGenerationType.QUIZ -> listOf(
                    AiStepData(
                        title = "Menghubungkan ke Engine Bank Soal CBT AI",
                        subtitle = "Mempersiapkan taksonomi Bloom dan parameter CBT..."
                    ),
                    AiStepData(
                        title = "Menganalisis Silabus Mapel & Tingkat Kesulitan",
                        subtitle = "Menyesuaikan standar $effectiveSub ($effectiveGrade) tingkat kesulitan $difficulty..."
                    ),
                    AiStepData(
                        title = "Menyintesis Butir Soal CBT Pilihan Ganda",
                        subtitle = "Menyusun butir pertanyaan pilihan ganda objektif untuk '$effectiveTopic'..."
                    ),
                    AiStepData(
                        title = "Menuliskan Pembahasan & Kunci Jawaban Valid",
                        subtitle = "Memvalidasi kunci jawaban tunggal yang benar dan penjelasan rasional..."
                    ),
                    AiStepData(
                        title = "Memfinalisasi Paket Bank Soal Ujian",
                        subtitle = "Mengemas seluruh butir soal siap diujikan di platform CBT..."
                    )
                )
            }
        }

        val colorScheme = androidx.compose.material3.MaterialTheme.colorScheme

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colorScheme.background)
                .padding(vertical = 24.dp, horizontal = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Dual-Ring Radar HUD
                AiDualRingRadar(
                    primaryColor = colorScheme.primary,
                    secondaryColor = colorScheme.tertiary,
                    modifier = Modifier.padding(bottom = 20.dp)
                )

                // Pulsing LED Status Badge
                AiStatusLedBadge(
                    text = badgeText,
                    badgeColor = colorScheme.primary,
                    modifier = Modifier.padding(bottom = 14.dp)
                )

                // Dynamic Stepped Title
                val current = stepData.getOrElse(currentStep) { stepData[0] }

                Text(
                    text = current.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    color = colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )

                Text(
                    text = current.subtitle,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp,
                    minLines = 2,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )

                // Flowing Gradient Progress Bar
                AiFlowingProgressBar(
                    primaryColor = colorScheme.primary,
                    secondaryColor = colorScheme.tertiary,
                    trackColor = colorScheme.outlineVariant.copy(alpha = 0.35f),
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .padding(top = 14.dp, bottom = 12.dp)
                )

                // Step Dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 0 until 5) {
                        val isActive = i == currentStep
                        val width by animateDpAsState(
                            targetValue = if (isActive) 22.dp else 6.dp,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                            label = "DotWidth"
                        )
                        Box(
                            modifier = Modifier
                                .height(5.dp)
                                .width(width)
                                .clip(CircleShape)
                                .background(if (isActive) colorScheme.primary else colorScheme.outlineVariant)
                        )
                    }
                }

                val footerText = when (type) {
                    AiGenerationType.MATERIAL -> "Mohon tunggu sejenak, AI sedang menyusun materi berkualitas tinggi..."
                    AiGenerationType.ASSIGNMENT -> "Mohon tunggu sejenak, AI sedang menyusun lembar tugas terstandar..."
                    AiGenerationType.QUIZ -> "Mohon tunggu sejenak, AI sedang menyusun paket soal CBT berkualitas tinggi..."
                }

                Text(
                    text = footerText,
                    fontSize = 10.sp,
                    color = colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
        }
    }
}

@Composable
private fun AiDualRingRadar(
    primaryColor: Color = Color(0xFF10B981),
    secondaryColor: Color = Color(0xFF0284C7),
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "AiRadarTransition")

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )

    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 0.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseAlpha"
    )

    val rotationFast by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RotationFast"
    )

    val rotationReverse by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RotationReverse"
    )

    val floatOffset by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "FloatOffset"
    )

    Box(
        modifier = modifier.size(88.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = size.minDimension / 2f

            // Outer Pulse Ring
            drawCircle(
                color = primaryColor.copy(alpha = pulseAlpha),
                radius = radius * pulseScale,
                style = Stroke(width = 2.dp.toPx())
            )

            // Middle Fast Rotating Ring (Gradient arc)
            rotate(rotationFast) {
                drawArc(
                    brush = Brush.sweepGradient(
                        listOf(
                            Color.Transparent,
                            primaryColor.copy(alpha = 0.3f),
                            primaryColor,
                            secondaryColor
                        )
                    ),
                    startAngle = 0f,
                    sweepAngle = 270f,
                    useCenter = false,
                    style = Stroke(
                        width = 3.dp.toPx(),
                        cap = StrokeCap.Round
                    ),
                    topLeft = Offset(6.dp.toPx(), 6.dp.toPx()),
                    size = Size(
                        size.width - 12.dp.toPx(),
                        size.height - 12.dp.toPx()
                    )
                )
            }

            // Inner Reverse Dashed Ring
            rotate(rotationReverse) {
                drawCircle(
                    color = secondaryColor.copy(alpha = 0.55f),
                    radius = (radius - 14.dp.toPx()).coerceAtLeast(1f),
                    style = Stroke(
                        width = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(
                            floatArrayOf(12f, 10f), 0f
                        )
                    )
                )
            }
        }

        // Center Floating Glowing Icon
        Icon(
            imageVector = Icons.Default.AutoAwesome,
            contentDescription = null,
            tint = primaryColor,
            modifier = Modifier
                .size(26.dp)
                .offset(y = floatOffset.dp)
        )
    }
}

@Composable
private fun AiStatusLedBadge(
    text: String,
    badgeColor: Color = Color(0xFF10B981),
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "LedTransition")
    val ledAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "LedAlpha"
    )

    Surface(
        shape = CircleShape,
        color = badgeColor.copy(alpha = 0.12f),
        border = androidx.compose.foundation.BorderStroke(1.dp, badgeColor.copy(alpha = 0.35f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(badgeColor.copy(alpha = ledAlpha))
            )
            Text(
                text = text,
                color = badgeColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.4.sp
            )
        }
    }
}

@Composable
private fun AiFlowingProgressBar(
    primaryColor: Color = Color(0xFF10B981),
    secondaryColor: Color = Color(0xFF0284C7),
    trackColor: Color = Color.Gray.copy(alpha = 0.2f),
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "FlowProgressTransition")
    val progressShift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ProgressShift"
    )

    Canvas(
        modifier = modifier
            .height(5.dp)
            .clip(CircleShape)
    ) {
        // Track
        drawRoundRect(
            color = trackColor,
            cornerRadius = CornerRadius(size.height / 2f, size.height / 2f)
        )

        // Flowing beam
        val beamWidth = size.width * 0.45f
        val startX = (size.width + beamWidth) * progressShift - beamWidth
        drawRoundRect(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color.Transparent,
                    primaryColor,
                    secondaryColor,
                    Color.Transparent
                ),
                startX = startX,
                endX = startX + beamWidth
            ),
            topLeft = Offset(startX.coerceAtLeast(0f), 0f),
            size = Size(beamWidth, size.height),
            cornerRadius = CornerRadius(size.height / 2f, size.height / 2f)
        )
    }
}
