package com.schoolos.android.feature.grades

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.schoolos.android.core.designsystem.CosmicBlack
import com.schoolos.android.core.designsystem.CosmicNavy
import com.schoolos.android.core.designsystem.CosmicSurface2
import com.schoolos.android.core.designsystem.GlassBorder
import com.schoolos.android.core.designsystem.GlassBorder2
import com.schoolos.android.core.designsystem.GlassOverlay
import com.schoolos.android.core.designsystem.LocalIsDarkTheme
import com.schoolos.android.core.designsystem.NeonBlue
import com.schoolos.android.core.designsystem.NeonError
import com.schoolos.android.core.designsystem.NeonSuccess
import com.schoolos.android.core.designsystem.NeonWarning
import com.schoolos.android.core.designsystem.StudentContainer
import com.schoolos.android.core.designsystem.StudentNeon
import com.schoolos.android.core.designsystem.TeacherNeon
import com.schoolos.android.core.designsystem.TextPrimary
import com.schoolos.android.core.designsystem.TextSecondary
import com.schoolos.android.core.designsystem.TextTertiary
import com.schoolos.android.domain.model.SubjectGradeDetail
import com.schoolos.android.domain.model.WeightComponent

@Composable
fun StudentGradeDetailContent(
    detail: SubjectGradeDetail
) {
    val isDark = LocalIsDarkTheme.current
    val score = detail.summary.finalScore
    val letter = detail.summary.letterGrade
    val kkm = detail.summary.passingThreshold
    val kkmDiff = score - kkm
    val isPassing = detail.summary.isPassed
    val hasGrades = score > 0.0

    // Theme-adaptive status colors
    val successColor = if (isDark) NeonSuccess else Color(0xFF047857)
    val successBg = if (isDark) NeonSuccess.copy(alpha = 0.12f) else Color(0xFFECFDF5)
    val successBorder = if (isDark) NeonSuccess.copy(alpha = 0.35f) else Color(0xFFA7F3D0)

    val warningColor = if (isDark) NeonWarning else Color(0xFFB45309)
    val warningBg = if (isDark) NeonWarning.copy(alpha = 0.12f) else Color(0xFFFEF3C7)
    val warningBorder = if (isDark) NeonWarning.copy(alpha = 0.35f) else Color(0xFFFDE68A)

    val neutralIndigoColor = if (isDark) StudentNeon else Color(0xFF4338CA)
    val neutralIndigoBg = if (isDark) StudentNeon.copy(alpha = 0.12f) else Color(0xFFEEF2FF)
    val neutralIndigoBorder = if (isDark) StudentNeon.copy(alpha = 0.35f) else Color(0xFFC7D2FE)

    val evaluationTitle = when {
        score >= kkm + 10.0 -> "Predikat Istimewa (A)"
        score >= kkm -> "Kompeten & Tuntas (B)"
        score >= kkm - 10.0 -> "Cukup Memadai (C)"
        hasGrades -> "Perlu Remedial (D)"
        else -> "Belum Ada Catatan Nilai"
    }

    val evaluationDesc = when {
        score >= kkm + 10.0 -> "Prestasi luar biasa! Capaian akademik Anda berada pada kuadran kompetensi tertinggi."
        score >= kkm -> "Kerja bagus! Nilai Anda telah memenuhi dan melampaui standar kelulusan KKM yang ditetapkan."
        score >= kkm - 10.0 -> "Capaian mendekati target KKM. Perbanyak latihan mandiri untuk mengoptimalkan nilai akhir."
        hasGrades -> "Nilai saat ini di bawah standar KKM ($kkm). Konsultasikan jadwal bimbingan remedial dengan guru pengampu."
        else -> "Komponen penilaian sedang dalam proses evaluasi oleh guru mata pelajaran."
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // ── 1. HERO ACADEMIC PERFORMANCE CARD ────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(4.dp, RoundedCornerShape(22.dp), spotColor = GlassOverlay)
                .clip(RoundedCornerShape(22.dp))
                .background(
                    Brush.linearGradient(
                        listOf(
                            CosmicNavy,
                            CosmicSurface2.copy(alpha = if (isDark) 0.65f else 0.45f)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    color = when {
                        !hasGrades -> GlassBorder
                        isPassing -> successBorder
                        else -> warningBorder
                    },
                    shape = RoundedCornerShape(22.dp)
                )
                .padding(18.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Top Tag + KKM Status Chip
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val dotColor = when {
                            !hasGrades -> neutralIndigoColor
                            isPassing -> successColor
                            else -> warningColor
                        }
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(dotColor)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "STATUS AKADEMIK",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = TextTertiary,
                            letterSpacing = 1.2.sp
                        )
                    }

                    // KKM Tag Chip (theme-aware colors)
                    val (chipText, chipIcon, chipTextColor, chipBgColor, chipBorderColor) = when {
                        !hasGrades -> Tuple5(
                            "Menunggu Evaluasi",
                            Icons.Default.HourglassEmpty,
                            neutralIndigoColor,
                            neutralIndigoBg,
                            neutralIndigoBorder
                        )
                        isPassing -> Tuple5(
                            "+${"%.1f".format(kkmDiff)} dari KKM",
                            Icons.Default.CheckCircle,
                            successColor,
                            successBg,
                            successBorder
                        )
                        else -> Tuple5(
                            "${"%.1f".format(kkmDiff)} dari KKM",
                            Icons.Default.Warning,
                            warningColor,
                            warningBg,
                            warningBorder
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(chipBgColor)
                            .border(1.dp, chipBorderColor, RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = chipIcon,
                                contentDescription = null,
                                tint = chipTextColor,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(
                                text = chipText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = chipTextColor
                            )
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                // Score + Grade Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    val scoreDisplayColor = when {
                        !hasGrades -> if (isDark) TextSecondary else Color(0xFF64748B)
                        isPassing -> successColor
                        else -> warningColor
                    }

                    Text(
                        text = "%.1f".format(score),
                        fontSize = 54.sp,
                        fontWeight = FontWeight.Black,
                        color = scoreDisplayColor,
                        letterSpacing = (-2).sp
                    )

                    Spacer(Modifier.width(18.dp))

                    Column(horizontalAlignment = Alignment.Start) {
                        // Dynamic Theme-Aware Predikat Badge
                        val (predBg, predBorder, predTextColor) = when {
                            !hasGrades || letter == "-" -> Triple(
                                if (isDark) Color(0xFF312E81).copy(alpha = 0.45f) else Color(0xFFEEF2FF),
                                if (isDark) Color(0xFF6366F1).copy(alpha = 0.6f) else Color(0xFFA5B4FC),
                                if (isDark) Color(0xFFE0E7FF) else Color(0xFF3730A3)
                            )
                            isPassing -> Triple(
                                if (isDark) NeonSuccess.copy(alpha = 0.15f) else Color(0xFFECFDF5),
                                if (isDark) NeonSuccess.copy(alpha = 0.5f) else Color(0xFFA7F3D0),
                                successColor
                            )
                            else -> Triple(
                                if (isDark) NeonWarning.copy(alpha = 0.15f) else Color(0xFFFEF3C7),
                                if (isDark) NeonWarning.copy(alpha = 0.5f) else Color(0xFFFDE68A),
                                warningColor
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(predBg)
                                .border(1.dp, predBorder, RoundedCornerShape(12.dp))
                                .padding(horizontal = 14.dp, vertical = 7.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = predTextColor,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "Predikat $letter",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black,
                                    color = predTextColor
                                )
                            }
                        }

                        Spacer(Modifier.height(5.dp))

                        Text(
                            text = "Standar KKM: ${kkm.toInt()}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextTertiary
                        )
                    }
                }

                Spacer(Modifier.height(18.dp))

                // Evaluation Capsule Box (Structured & Clear)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(CosmicSurface2.copy(alpha = if (isDark) 0.6f else 0.85f))
                        .border(1.dp, GlassBorder, RoundedCornerShape(14.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = evaluationTitle,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = evaluationDesc,
                            fontSize = 11.sp,
                            color = TextSecondary,
                            lineHeight = 16.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(Modifier.height(18.dp))
                HorizontalDivider(color = GlassBorder, thickness = 1.dp)
                Spacer(Modifier.height(16.dp))

                // BENCHMARK PROGRESS BAR WITH KKM MARKER
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Skor Akhir Berbobot",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "${"%.1f".format(score)} / 100",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            color = TextPrimary
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    // Progress bar track with HIGH CONTRAST (CosmicSurface2)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(CircleShape)
                            .background(CosmicSurface2)
                            .border(1.dp, GlassBorder, CircleShape)
                    ) {
                        val fillFrac = (score / 100.0).coerceIn(0.0, 1.0).toFloat()
                        if (fillFrac > 0f) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(fillFrac)
                                    .height(10.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(
                                                StudentNeon,
                                                if (isPassing) successColor else warningColor
                                            )
                                        )
                                    )
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // Indicator labels: 0, KKM benchmark pill, 100
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("0", fontSize = 10.sp, color = TextTertiary, fontWeight = FontWeight.Bold)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(warningBg)
                                .border(0.5.dp, warningBorder, RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(warningColor)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "Target KKM: ${kkm.toInt()}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = warningColor
                            )
                        }
                        Text("100", fontSize = 10.sp, color = TextTertiary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // ── 2. COMPONENT BREAKDOWN SECTION ───────────────────────────────────
        val totalWeight = detail.weightBreakdown.sumOf { it.weightPercentage }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Rincian Komponen Nilai",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    color = TextPrimary
                )
                Text(
                    text = "Bobot kurikulum & kontribusi poin akhir",
                    fontSize = 11.sp,
                    color = TextTertiary
                )
            }

            // Theme-aware Total Bobot badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(neutralIndigoBg)
                    .border(1.dp, neutralIndigoBorder, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = "Total Bobot: ${totalWeight.toInt()}%",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = neutralIndigoColor
                )
            }
        }

        if (detail.weightBreakdown.isEmpty()) {
            // Elevated, Informative & Beautiful Empty State Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                CosmicNavy,
                                CosmicSurface2.copy(alpha = if (isDark) 0.6f else 0.4f)
                            )
                        )
                    )
                    .border(1.dp, GlassBorder, RoundedCornerShape(20.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(neutralIndigoBg)
                            .border(1.dp, neutralIndigoBorder, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.HourglassEmpty,
                            contentDescription = null,
                            tint = neutralIndigoColor,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Spacer(Modifier.height(14.dp))

                    Text(
                        text = "Komponen Penilaian Sedang Berjalan",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(6.dp))

                    Text(
                        text = "Guru mata pelajaran sedang mempersiapkan rubrik tugas, kuis, dan ujian untuk periode ini. Rincian nilai dan persentase bobot akan tampil otomatis di sini setelah dipublikasikan.",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    Spacer(Modifier.height(16.dp))

                    // Educational Guide Badges
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(CosmicSurface2)
                                .border(1.dp, GlassBorder, RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.FactCheck,
                                    contentDescription = null,
                                    tint = neutralIndigoColor,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "Bobot Kurikulum Terstandar",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        } else {
            detail.weightBreakdown.forEach { component ->
                GradeBreakdownCardItem(
                    component = component
                )
            }
        }

        // ── 3. ANALYTICAL DEEP-DIVE DASHBOARD ────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(2.dp, RoundedCornerShape(20.dp), spotColor = GlassOverlay)
                .clip(RoundedCornerShape(20.dp))
                .background(CosmicNavy)
                .border(1.dp, GlassBorder, RoundedCornerShape(20.dp))
                .padding(16.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isDark) NeonBlue.copy(alpha = 0.15f) else Color(0xFFEFF6FF)
                            )
                            .border(
                                1.dp,
                                if (isDark) NeonBlue.copy(alpha = 0.35f) else Color(0xFFBFDBFE),
                                RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = null,
                            tint = if (isDark) NeonBlue else Color(0xFF1D4ED8),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "ANALISIS PERFORMA BELAJAR",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = TextPrimary,
                            letterSpacing = 0.6.sp
                        )
                        Text(
                            text = "Ringkasan evaluasi capaian semester aktif",
                            fontSize = 10.sp,
                            color = TextTertiary
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                val gradedCount = detail.summary.gradedComponentCount
                val totalCount = detail.summary.componentCount
                val compText = if (totalCount > 0) "$gradedCount dari $totalCount Dinilai" else "Belum Dinilai"
                val pctText = "${detail.summary.completionPercentage.toInt()}% Lengkap"
                val kkmStatusText = when {
                    !hasGrades -> "Dalam Proses"
                    isPassing -> "Tuntas KKM ✓"
                    else -> "Belum Tuntas !"
                }
                val kkmStatusColor = when {
                    !hasGrades -> neutralIndigoColor
                    isPassing -> successColor
                    else -> warningColor
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AnalyticsMiniTile(
                        label = "Komponen",
                        value = compText,
                        color = if (isDark) NeonBlue else Color(0xFF1D4ED8),
                        modifier = Modifier.weight(1f)
                    )
                    AnalyticsMiniTile(
                        label = "Progres",
                        value = pctText,
                        color = neutralIndigoColor,
                        modifier = Modifier.weight(1f)
                    )
                    AnalyticsMiniTile(
                        label = "Kelulusan",
                        value = kkmStatusText,
                        color = kkmStatusColor,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(16.dp))

                // Recommendation Advice Banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(CosmicSurface2)
                        .border(1.dp, GlassBorder, RoundedCornerShape(14.dp))
                        .padding(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = if (isDark) NeonBlue else Color(0xFF2563EB),
                            modifier = Modifier
                                .size(18.dp)
                                .padding(top = 1.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Rekomendasi Akademik",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(Modifier.height(3.dp))
                            val tipText = when {
                                score >= 85.0 -> "Pertahankan konsistensi performa pada tugas harian dan ujian untuk mengamankan nilai akhir sempurna."
                                score >= 75.0 -> "Hasil penilaian Anda stabil. Maksimalkan pemahaman materi praktikum dan kuis untuk meningkatkan predikat menjadi A."
                                hasGrades -> "Tingkatkan nilai komponen yang masih belum optimal. Hubungi guru mata pelajaran untuk konsultasi perbaikan nilai."
                                else -> "Pantau materi pembelajaran dan penugasan secara berkala untuk mempersiapkan evaluasi nilai harian."
                            }
                            Text(
                                text = tipText,
                                fontSize = 11.sp,
                                color = TextSecondary,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        }

        // ── 4. GRADE SCALE REFERENCE LEGEND ──────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CosmicNavy)
                .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "PEDOMAN SKALA PENILAIAN",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = TextTertiary,
                    letterSpacing = 0.8.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    GradeScaleTierItem(
                        letter = "A",
                        range = "85 - 100",
                        desc = "Sangat Baik",
                        color = if (isDark) StudentNeon else Color(0xFF4338CA)
                    )
                    GradeScaleTierItem(
                        letter = "B",
                        range = "75 - 84",
                        desc = "Baik (KKM)",
                        color = successColor
                    )
                    GradeScaleTierItem(
                        letter = "C",
                        range = "65 - 74",
                        desc = "Cukup",
                        color = warningColor
                    )
                    GradeScaleTierItem(
                        letter = "D",
                        range = "< 65",
                        desc = "Remedial",
                        color = if (isDark) NeonError else Color(0xFFDC2626)
                    )
                }
            }
        }
    }
}

@Composable
private fun GradeBreakdownCardItem(
    component: WeightComponent
) {
    val isDark = LocalIsDarkTheme.current
    val name = component.name
    val weight = component.weightPercentage
    val rawScore = component.score ?: 0.0
    val maxRawScore = component.maxScore ?: 100.0
    val pct = if (maxRawScore > 0) (rawScore / maxRawScore).coerceIn(0.0, 1.0).toFloat() else 0f
    val contribution = if (maxRawScore > 0) (rawScore / maxRawScore) * weight else 0.0

    // Determine intuitive component icon & color
    val lower = name.lowercase()
    val (icon: ImageVector, accentColor: Color) = when {
        lower.contains("kuis") || lower.contains("quiz") || lower.contains("ulangan") ->
            Pair(Icons.Default.Bolt, if (isDark) NeonWarning else Color(0xFFD97706))
        lower.contains("uts") || lower.contains("tengah") || lower.contains("mid") ->
            Pair(Icons.Default.Assessment, if (isDark) TeacherNeon else Color(0xFF059669))
        lower.contains("uas") || lower.contains("akhir") || lower.contains("pas") || lower.contains("pat") ->
            Pair(Icons.Default.School, if (isDark) StudentNeon else Color(0xFF4F46E5))
        lower.contains("praktik") || lower.contains("proyek") || lower.contains("lab") ->
            Pair(Icons.Default.Science, if (isDark) NeonSuccess else Color(0xFF059669))
        else ->
            Pair(Icons.AutoMirrored.Filled.Assignment, if (isDark) NeonBlue else Color(0xFF2563EB))
    }

    val successText = if (isDark) NeonSuccess else Color(0xFF047857)
    val successBg = if (isDark) NeonSuccess.copy(alpha = 0.12f) else Color(0xFFECFDF5)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(18.dp), spotColor = GlassOverlay)
            .clip(RoundedCornerShape(18.dp))
            .background(CosmicNavy)
            .border(1.dp, GlassBorder, RoundedCornerShape(18.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Header: Icon + Title + Weight Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(accentColor.copy(alpha = if (isDark) 0.14f else 0.10f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(Modifier.width(12.dp))

                    Column {
                        Text(
                            text = name,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Nilai Mentah: ${rawScore.toInt()} / ${maxRawScore.toInt()}",
                            fontSize = 11.sp,
                            color = TextTertiary
                        )
                    }
                }

                // Weight Chip
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(accentColor.copy(alpha = if (isDark) 0.12f else 0.08f))
                        .border(1.dp, accentColor.copy(alpha = if (isDark) 0.35f else 0.25f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 9.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Bobot: ${weight.toInt()}%",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                }
            }

            // Score Ratio & Final Grade Contribution Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${(pct * 100).toInt()}%",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "capaian",
                        fontSize = 11.sp,
                        color = TextTertiary
                    )
                }

                // Score Contribution Pill (Points added to total)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(successBg)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "+${"%.1f".format(contribution)} Poin Akhir",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = successText
                    )
                }
            }

            // Progress Bar (Track uses CosmicSurface2 so it's always distinct from card background!)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape)
                    .background(CosmicSurface2)
                    .border(0.5.dp, GlassBorder, CircleShape)
            ) {
                if (pct > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(pct)
                            .height(8.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(accentColor, accentColor.copy(alpha = 0.8f))
                                )
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun AnalyticsMiniTile(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(CosmicSurface2)
            .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
            .padding(10.dp)
    ) {
        Column {
            Text(label, fontSize = 10.sp, color = TextTertiary, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                color = color,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun GradeScaleTierItem(
    letter: String,
    range: String,
    desc: String,
    color: Color
) {
    val isDark = LocalIsDarkTheme.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = if (isDark) 0.16f else 0.12f))
                .border(1.dp, color.copy(alpha = if (isDark) 0.45f else 0.35f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(letter, fontSize = 13.sp, fontWeight = FontWeight.Black, color = color)
        }
        Spacer(Modifier.height(4.dp))
        Text(range, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Text(desc, fontSize = 8.sp, color = TextTertiary)
    }
}

private data class Tuple5<A, B, C, D, E>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E
)
