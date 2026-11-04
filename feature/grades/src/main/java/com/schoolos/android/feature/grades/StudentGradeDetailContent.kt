package com.schoolos.android.feature.grades

import com.schoolos.android.core.R as CoreR
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.schoolos.android.core.designsystem.*
import com.schoolos.android.domain.model.SubjectGradeDetail
import com.schoolos.android.domain.model.WeightComponent

@Composable
fun StudentGradeDetailContent(
    detail: SubjectGradeDetail
) {
    val isDark = LocalIsDarkTheme.current
    val cardBg = if (isDark) Color(0xFF111113) else Color.White
    val cardBorder = if (isDark) Color(0xFF1F1F23) else Color(0xFFE2E8F0)
    val textPrimary = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFFA1A1AA) else Color(0xFF64748B)
    val textTertiary = if (isDark) Color(0xFF71717A) else Color(0xFF94A3B8)
    val innerCardBg = if (isDark) Color(0xFF161618) else Color(0xFFF8FAFC)
    val innerCardBorder = if (isDark) Color(0xFF27272A) else Color(0xFFE2E8F0)

    val score = detail.summary.finalScore
    val letter = detail.summary.letterGrade
    val kktp = detail.summary.passingThreshold
    val kktpDiff = score - kktp
    val isPassing = detail.summary.isPassed
    val hasGrades = score > 0.0

    // Theme-adaptive status colors
    val successColor = if (isDark) Color(0xFF10B981) else Color(0xFF059669)
    val successBg = if (isDark) Color(0xFF064E3B).copy(alpha = 0.35f) else Color(0xFFECFDF5)
    val successBorder = if (isDark) Color(0xFF10B981).copy(alpha = 0.45f) else Color(0xFFA7F3D0)

    val warningColor = if (isDark) Color(0xFFF59E0B) else Color(0xFFD97706)
    val warningBg = if (isDark) Color(0xFF451A03).copy(alpha = 0.35f) else Color(0xFFFFFBEB)
    val warningBorder = if (isDark) Color(0xFFF59E0B).copy(alpha = 0.45f) else Color(0xFFFDE68A)

    val neutralIndigoColor = if (isDark) Color(0xFF818CF8) else Color(0xFF4F46E5)
    val neutralIndigoBg = if (isDark) Color(0xFF1E1B4B).copy(alpha = 0.45f) else Color(0xFFEEF2FF)
    val neutralIndigoBorder = if (isDark) Color(0xFF818CF8).copy(alpha = 0.45f) else Color(0xFFC7D2FE)

    val evaluationTitle = when {
        letter.isNotBlank() && letter != "-" -> "Predikat $letter"
        isPassing -> "Kompeten & Tuntas"
        hasGrades -> "Perlu Penguatan Modul"
        else -> "Belum Ada Catatan Nilai"
    }

    val evaluationDesc = when {
        isPassing -> "Capaian pembelajaran Anda telah memenuhi kriteria ketuntasan tujuan pembelajaran (KKTP)."
        hasGrades -> "Capaian saat ini di bawah target KKTP (${kktp.toInt()}). Silakan konsultasikan jadwal bimbingan dan penguatan modul dengan tutor pengampu."
        else -> "Komponen penilaian sedang dalam proses evaluasi oleh tutor pengampu."
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // ── 1. HERO ACADEMIC PERFORMANCE CARD ────────────────────────────────
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = cardBg,
            border = BorderStroke(1.dp, cardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Top Tag + KKTP Status Chip
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
                            color = textTertiary,
                            letterSpacing = 1.2.sp
                        )
                    }

                    // KKTP Tag Chip (theme-aware colors)
                    val (chipText, chipIconRes, chipTextColor, chipBgColor, chipBorderColor) = when {
                        !hasGrades -> Tuple5(
                            "Menunggu Evaluasi",
                            CoreR.drawable.ic_modern_clock,
                            neutralIndigoColor,
                            neutralIndigoBg,
                            neutralIndigoBorder
                        )
                        isPassing -> Tuple5(
                            "+${"%.1f".format(kktpDiff)} dari KKTP",
                            CoreR.drawable.ic_modern_check_circle,
                            successColor,
                            successBg,
                            successBorder
                        )
                        else -> Tuple5(
                            "${"%.1f".format(kktpDiff)} dari KKTP",
                            CoreR.drawable.ic_modern_warning,
                            warningColor,
                            warningBg,
                            warningBorder
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = chipBgColor,
                        border = BorderStroke(1.dp, chipBorderColor)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = chipIconRes),
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
                        !hasGrades -> textSecondary
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
                                neutralIndigoBg,
                                neutralIndigoBorder,
                                neutralIndigoColor
                            )
                            isPassing -> Triple(
                                successBg,
                                successBorder,
                                successColor
                            )
                            else -> Triple(
                                warningBg,
                                warningBorder,
                                warningColor
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = predBg,
                            border = BorderStroke(1.dp, predBorder)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = predTextColor,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "Predikat $letter",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Black,
                                    color = predTextColor
                                )
                            }
                        }

                        Spacer(Modifier.height(6.dp))

                        Text(
                            text = "Target KKTP: ${kktp.toInt()}",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = textTertiary
                        )
                    }
                }

                Spacer(Modifier.height(18.dp))

                // Evaluation Capsule Box (Structured & Clear)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = innerCardBg,
                    border = BorderStroke(1.dp, innerCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = evaluationTitle,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = evaluationDesc,
                            fontSize = 11.5.sp,
                            color = textSecondary,
                            lineHeight = 16.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(Modifier.height(18.dp))
                HorizontalDivider(color = innerCardBorder, thickness = 1.dp)
                Spacer(Modifier.height(16.dp))

                // BENCHMARK PROGRESS BAR WITH KKTP MARKER
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Skor Akhir Berbobot",
                            fontSize = 12.sp,
                            color = textSecondary,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "${"%.1f".format(score)} / 100",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            color = textPrimary
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    // Progress bar track with high contrast
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(CircleShape)
                            .background(innerCardBg)
                            .border(1.dp, innerCardBorder, CircleShape)
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
                                                neutralIndigoColor,
                                                if (isPassing) successColor else warningColor
                                            )
                                        )
                                    )
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // Indicator labels: 0, KKTP benchmark pill, 100
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("0", fontSize = 10.sp, color = textTertiary, fontWeight = FontWeight.Bold)
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = warningBg,
                            border = BorderStroke(0.5.dp, warningBorder)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(5.dp)
                                        .clip(CircleShape)
                                        .background(warningColor)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "Target KKTP: ${kktp.toInt()}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = warningColor
                                )
                            }
                        }
                        Text("100", fontSize = 10.sp, color = textTertiary, fontWeight = FontWeight.Bold)
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
                    color = textPrimary
                )
                Text(
                    text = "Bobot kurikulum & kontribusi poin akhir",
                    fontSize = 11.5.sp,
                    color = textTertiary
                )
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = neutralIndigoBg,
                border = BorderStroke(1.dp, neutralIndigoBorder)
            ) {
                Text(
                    text = "Total Bobot: ${totalWeight.toInt()}%",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = neutralIndigoColor,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }

        if (detail.weightBreakdown.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = cardBg,
                border = BorderStroke(1.dp, cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
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
                            painter = painterResource(id = CoreR.drawable.ic_modern_tasks),
                            contentDescription = null,
                            tint = neutralIndigoColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(Modifier.height(14.dp))

                    Text(
                        text = "Komponen Penilaian Sedang Berjalan",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(6.dp))

                    Text(
                        text = "Guru mata pelajaran sedang mempersiapkan rubrik tugas, kuis, dan ujian untuk periode ini. Rincian nilai dan persentase bobot akan tampil otomatis di sini setelah dipublikasikan.",
                        fontSize = 12.sp,
                        color = textSecondary,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    Spacer(Modifier.height(16.dp))

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = innerCardBg,
                        border = BorderStroke(1.dp, innerCardBorder)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = CoreR.drawable.ic_modern_check_circle),
                                contentDescription = null,
                                tint = neutralIndigoColor,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Bobot Kurikulum Terstandar",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = textSecondary
                            )
                        }
                    }
                }
            }
        } else {
            detail.weightBreakdown.forEach { component ->
                GradeBreakdownCardItem(component = component)
            }
        }

        // ── 3. ANALYTICAL DEEP-DIVE DASHBOARD ────────────────────────────────
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = cardBg,
            border = BorderStroke(1.dp, cardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(neutralIndigoBg)
                            .border(1.dp, neutralIndigoBorder, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = CoreR.drawable.ic_modern_grades),
                            contentDescription = null,
                            tint = neutralIndigoColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "ANALISIS PERFORMA BELAJAR",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = textPrimary,
                            letterSpacing = 0.6.sp
                        )
                        Text(
                            text = "Ringkasan evaluasi capaian semester aktif",
                            fontSize = 10.5.sp,
                            color = textTertiary
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                val gradedCount = detail.summary.gradedComponentCount
                val totalCount = detail.summary.componentCount
                val compText = if (totalCount > 0) "$gradedCount dari $totalCount Dinilai" else "Belum Dinilai"
                val pctText = "${detail.summary.completionPercentage.toInt()}% Lengkap"
                val kktpStatusText = when {
                    !hasGrades -> "Dalam Proses"
                    isPassing -> "Tuntas KKTP"
                    else -> "Perlu Penguatan"
                }
                val kktpStatusColor = when {
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
                        color = neutralIndigoColor,
                        modifier = Modifier.weight(1f)
                    )
                    AnalyticsMiniTile(
                        label = "Progres",
                        value = pctText,
                        color = neutralIndigoColor,
                        modifier = Modifier.weight(1f)
                    )
                    AnalyticsMiniTile(
                        label = "Ketuntasan",
                        value = kktpStatusText,
                        color = kktpStatusColor,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(16.dp))

                // Recommendation Advice Banner
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = innerCardBg,
                    border = BorderStroke(1.dp, innerCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.Top,
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = neutralIndigoColor,
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
                                color = textPrimary
                            )
                            Spacer(Modifier.height(3.dp))
                            val tipText = when {
                                isPassing -> "Pertahankan konsistensi performa pada tugas mandiri dan penilaian modul untuk mengoptimalkan capaian belajar."
                                hasGrades -> "Tingkatkan nilai komponen yang masih belum optimal. Hubungi tutor pamong pengampu modul untuk bimbingan penguatan."
                                else -> "Pantau modul pembelajaran dan penugasan secara berkala untuk mempersiapkan evaluasi capaian belajar."
                            }
                            Text(
                                text = tipText,
                                fontSize = 11.sp,
                                color = textSecondary,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        }

        // ── 4. GRADE SCALE REFERENCE LEGEND ──────────────────────────────────
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = cardBg,
            border = BorderStroke(1.dp, cardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(18.dp)
            ) {
                Text(
                    text = "PEDOMAN SKALA PENILAIAN",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = textTertiary,
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
                        color = neutralIndigoColor
                    )
                    GradeScaleTierItem(
                        letter = "B",
                        range = "75 - 84",
                        desc = "Baik (KKTP)",
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
                        desc = "Perlu Penguatan",
                        color = if (isDark) Color(0xFFEF4444) else Color(0xFFDC2626)
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
    val cardBg = if (isDark) Color(0xFF111113) else Color.White
    val cardBorder = if (isDark) Color(0xFF1F1F23) else Color(0xFFE2E8F0)
    val textPrimary = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A)
    val textTertiary = if (isDark) Color(0xFF71717A) else Color(0xFF94A3B8)
    val innerCardBg = if (isDark) Color(0xFF161618) else Color(0xFFF8FAFC)
    val innerCardBorder = if (isDark) Color(0xFF27272A) else Color(0xFFE2E8F0)

    val name = component.name
    val weight = component.weightPercentage
    val rawScore = component.score ?: 0.0
    val maxRawScore = component.maxScore ?: 100.0
    val pct = if (maxRawScore > 0) (rawScore / maxRawScore).coerceIn(0.0, 1.0).toFloat() else 0f
    val contribution = if (maxRawScore > 0) (rawScore / maxRawScore) * weight else 0.0

    // Determine intuitive component icon & color
    val lower = name.lowercase()
    val (iconRes: Int, accentColor: Color) = when {
        lower.contains("kuis") || lower.contains("quiz") || lower.contains("ulangan") ->
            Pair(CoreR.drawable.ic_modern_quiz, if (isDark) Color(0xFFF59E0B) else Color(0xFFD97706))
        lower.contains("uts") || lower.contains("tengah") || lower.contains("mid") ->
            Pair(CoreR.drawable.ic_modern_grades, if (isDark) Color(0xFF10B981) else Color(0xFF059669))
        lower.contains("uas") || lower.contains("akhir") || lower.contains("pas") || lower.contains("pat") ->
            Pair(CoreR.drawable.ic_modern_graduation, if (isDark) Color(0xFF818CF8) else Color(0xFF4F46E5))
        else ->
            Pair(CoreR.drawable.ic_modern_tasks, if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7))
    }

    val successText = if (isDark) Color(0xFF10B981) else Color(0xFF059669)
    val successBg = if (isDark) Color(0xFF064E3B).copy(alpha = 0.35f) else Color(0xFFECFDF5)

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(18.dp)
        ) {
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
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(accentColor.copy(alpha = if (isDark) 0.16f else 0.10f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = iconRes),
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(Modifier.width(12.dp))

                    Column {
                        Text(
                            text = name,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "Nilai Mentah: ${rawScore.toInt()} / ${maxRawScore.toInt()}",
                            fontSize = 11.5.sp,
                            color = textTertiary
                        )
                    }
                }

                // Weight Chip
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = accentColor.copy(alpha = if (isDark) 0.15f else 0.10f),
                    border = BorderStroke(1.dp, accentColor.copy(alpha = if (isDark) 0.4f else 0.25f))
                ) {
                    Text(
                        text = "Bobot: ${weight.toInt()}%",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
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
                        color = textPrimary
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "capaian",
                        fontSize = 11.5.sp,
                        color = textTertiary
                    )
                }

                // Score Contribution Pill (Points added to total)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = successBg
                ) {
                    Text(
                        text = "+${"%.1f".format(contribution)} Poin Akhir",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = successText,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Progress Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape)
                    .background(innerCardBg)
                    .border(0.5.dp, innerCardBorder, CircleShape)
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
    val isDark = LocalIsDarkTheme.current
    val innerCardBg = if (isDark) Color(0xFF161618) else Color(0xFFF8FAFC)
    val innerCardBorder = if (isDark) Color(0xFF27272A) else Color(0xFFE2E8F0)
    val textTertiary = if (isDark) Color(0xFF71717A) else Color(0xFF94A3B8)

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = innerCardBg,
        border = BorderStroke(1.dp, innerCardBorder),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(label, fontSize = 10.sp, color = textTertiary, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 12.5.sp,
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
    val textPrimary = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A)
    val textTertiary = if (isDark) Color(0xFF71717A) else Color(0xFF94A3B8)

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = if (isDark) 0.16f else 0.12f))
                .border(1.dp, color.copy(alpha = if (isDark) 0.45f else 0.35f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(letter, fontSize = 13.5.sp, fontWeight = FontWeight.Black, color = color)
        }
        Spacer(Modifier.height(5.dp))
        Text(range, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = textPrimary)
        Text(desc, fontSize = 8.5.sp, color = textTertiary)
    }
}

private data class Tuple5<A, B, C, D, E>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E
)
