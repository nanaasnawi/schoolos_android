package com.schoolos.android.feature.grades

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Class
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.schoolos.android.core.common.DapodikPeriod
import com.schoolos.android.core.designsystem.*
import com.schoolos.android.domain.model.SubjectGradeSummary

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState

fun LazyListScope.teacherGradebookContent(
    subjects: List<SubjectGradeSummary>,
    className: String,
    totalSubjectCount: Int = subjects.size,
    selectedFilter: String = "Semua Mapel",
    onSelectFilter: (String) -> Unit = {},
    onSubjectClick: (String, String) -> Unit,
) {
    // ── TEACHER CLASS HERO BANNER (WALI KELAS RESMI) ─────────────────────
    item {
        TeacherClassHeroHeader(subjects = subjects, className = className)
    }

    item {
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "LEGER NILAI MATA PELAJARAN",
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = TeacherNeon,
                letterSpacing = 1.sp
            )
            Text(
                "${subjects.size} Mapel",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextTertiary
            )
        }
    }

    item {
        val filters = listOf("Semua Mapel", "Wajib", "MIPA", "Bahasa", "Muatan Lokal")
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 4.dp)
        ) {
            filters.forEach { filter ->
                val isSelected = filter == selectedFilter
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) TeacherNeon.copy(alpha = 0.15f) else CosmicNavy)
                        .border(
                            0.5.dp,
                            if (isSelected) TeacherNeon.copy(alpha = 0.4f) else GlassBorder,
                            RoundedCornerShape(8.dp),
                        )
                        .clickable { onSelectFilter(filter) }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Text(
                        filter,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isSelected) TeacherNeon else TextTertiary,
                    )
                }
            }
        }
    }

    // ── SUBJECT GRADE CARDS ──────────────────────────────────────────────
    if (subjects.isEmpty()) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(CosmicNavy)
                    .border(1.dp, GlassBorder, RoundedCornerShape(20.dp))
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📚", fontSize = 36.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Belum ada mata pelajaran tercatat untuk Kelas $className.",
                        color = TextTertiary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    } else {
        items(subjects, key = { it.subjectId }) { subject ->
            TeacherSubjectGradeCard(
                summary = subject,
                className = className,
                onClick = { onSubjectClick(subject.subjectId, subject.subjectName) }
            )
        }
    }
}

@Composable
private fun TeacherClassHeroHeader(
    subjects: List<SubjectGradeSummary>,
    className: String,
) {
    val gradedSubjects = subjects.filter { it.finalScore > 0.0 && it.gradedComponentCount > 0 }
    val hasGrades = gradedSubjects.isNotEmpty()
    val avgScore = if (hasGrades) gradedSubjects.map { it.finalScore }.average() else 0.0
    val (predikatLabel, predikatDesc) = when {
        !hasGrades -> Pair("Belum Dinilai", "Menunggu input nilai")
        avgScore >= 85.0 -> Pair("Predikat A", "Sangat Baik")
        avgScore >= 75.0 -> Pair("Predikat B", "Baik")
        avgScore >= 65.0 -> Pair("Predikat C", "Cukup")
        else -> Pair("Predikat D", "Perlu Bimbingan")
    }

    val passRateText = if (hasGrades) {
        val passedCount = gradedSubjects.count { it.finalScore >= 75.0 }
        "${(passedCount * 100) / gradedSubjects.size}% Tuntas"
    } else {
        "—"
    }

    val isDark = LocalIsDarkTheme.current
    val heroBg = if (isDark) {
        Brush.linearGradient(
            listOf(
                Color(0xFF064E3B).copy(alpha = 0.35f),
                Color(0xFF0B192E).copy(alpha = 0.7f),
                CosmicNavy
            )
        )
    } else {
        Brush.linearGradient(
            listOf(
                TeacherNeon.copy(alpha = 0.08f),
                Color(0xFFF0FDF4),
                CosmicNavy
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(heroBg)
            .border(
                width = 1.dp,
                color = if (isDark) TeacherNeon.copy(alpha = 0.35f) else TeacherNeon.copy(alpha = 0.25f),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(16.dp)
    ) {
        // TOP BADGE ROW
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(TeacherNeon.copy(alpha = 0.12f))
                    .border(1.dp, TeacherNeon.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 9.dp, vertical = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(TeacherNeon)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "WALI KELAS • ${DapodikPeriod.getActiveSemester().uppercase()}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = TeacherNeon,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(NeonSuccess.copy(alpha = 0.1f))
                    .border(1.dp, NeonSuccess.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, null, tint = NeonSuccess, modifier = Modifier.size(11.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Dapodik Ready", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NeonSuccess)
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // CLASS TITLE & IDENTITY
        Text(
            text = "Kelas $className",
            fontSize = 22.sp,
            fontWeight = FontWeight.Black,
            color = TextPrimary,
            letterSpacing = (-0.5).sp
        )
        Text(
            text = "Leger Nilai • ${DapodikPeriod.getFullPeriodLabel()}",
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = TextSecondary
        )

        Spacer(Modifier.height(14.dp))

        // BIG STATS ROW
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = if (hasGrades) "%.1f".format(avgScore) else "—",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Black,
                    color = TextPrimary,
                    letterSpacing = (-1.5).sp
                )
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.padding(bottom = 4.dp)) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (hasGrades) TeacherNeon.copy(alpha = 0.15f) else CosmicSurface2)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = predikatLabel,
                            color = if (hasGrades) TeacherNeon else TextTertiary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(predikatDesc, color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // 3 METRIC PILLS
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TeacherMetricCard(
                label = "Mata Pelajaran",
                value = "${subjects.size} Mapel",
                modifier = Modifier.weight(1f)
            )
            TeacherMetricCard(
                label = "Target KKM",
                value = "75.0",
                modifier = Modifier.weight(1f)
            )
            TeacherMetricCard(
                label = "Kelulusan",
                value = passRateText,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun TeacherMetricCard(label: String, value: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(CosmicSurface2)
            .border(0.5.dp, GlassBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Column {
            Text(label, fontSize = 9.sp, color = TextTertiary, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(2.dp))
            Text(value, fontSize = 13.sp, color = TextPrimary, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun TeacherSubjectGradeCard(
    summary: SubjectGradeSummary,
    className: String,
    onClick: () -> Unit,
) {
    val (emoji, accentColor) = when {
        summary.subjectName.contains("Matematika", ignoreCase = true) -> Pair("🧮", StudentNeon)
        summary.subjectName.contains("IPA", ignoreCase = true) || summary.subjectName.contains("Fisika", ignoreCase = true) || summary.subjectName.contains("Biologi", ignoreCase = true) -> Pair("🔬", NeonBlue)
        summary.subjectName.contains("Bahasa", ignoreCase = true) -> Pair("📚", NeonSuccess)
        summary.subjectName.contains("IPS", ignoreCase = true) || summary.subjectName.contains("Ekonomi", ignoreCase = true) || summary.subjectName.contains("Geografi", ignoreCase = true) -> Pair("🌍", NeonWarning)
        summary.subjectName.contains("Olahraga", ignoreCase = true) || summary.subjectName.contains("Penjas", ignoreCase = true) -> Pair("⚽", NeonError)
        summary.subjectName.contains("Informatika", ignoreCase = true) || summary.subjectName.contains("TIK", ignoreCase = true) -> Pair("💻", Color(0xFF00E5FF))
        else -> Pair("📖", TeacherNeon)
    }

    val isGraded = summary.finalScore > 0.0 && summary.gradedComponentCount > 0

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CosmicNavy)
            .border(0.5.dp, GlassBorder, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Subject Emoji Chip
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(accentColor.copy(alpha = 0.12f))
                        .border(1.dp, accentColor.copy(alpha = 0.25f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(emoji, fontSize = 20.sp)
                }

                Spacer(Modifier.width(12.dp))

                // Title & Class Info
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = summary.subjectName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(accentColor.copy(alpha = 0.1f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Kelas $className",
                                fontSize = 10.sp,
                                color = accentColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(CosmicSurface2)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "KKM 75",
                                fontSize = 10.sp,
                                color = TextTertiary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // Score & Predikat Column
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (isGraded) "%.1f".format(summary.finalScore) else "—",
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp,
                        color = if (isGraded) accentColor else TextTertiary,
                        letterSpacing = (-1).sp
                    )
                    Spacer(Modifier.height(2.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isGraded) accentColor.copy(alpha = 0.12f) else CosmicSurface2)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (isGraded) "Predikat ${summary.letterGrade}" else "Belum Dinilai",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isGraded) accentColor else TextTertiary
                        )
                    }
                }
            }

            // Divider
            androidx.compose.material3.HorizontalDivider(color = GlassBorder, thickness = 0.5.dp)

            // PROGRESS & CTA ROW
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f).padding(end = 12.dp)
                ) {
                    val progress = if (summary.componentCount > 0) {
                        (summary.gradedComponentCount.toFloat() / summary.componentCount.toFloat()).coerceIn(0f, 1f)
                    } else 0f
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(CircleShape),
                        color = if (isGraded) accentColor else TextTertiary.copy(alpha = 0.4f),
                        trackColor = CosmicSurface2,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (summary.componentCount > 0) {
                            "${summary.gradedComponentCount}/${summary.componentCount} Nilai"
                        } else {
                            "0 Nilai Masuk"
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextTertiary
                    )
                }

                // Open Leger CTA
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(accentColor.copy(alpha = 0.1f))
                        .border(1.dp, accentColor.copy(alpha = 0.25f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("Buka Leger", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = accentColor)
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(11.dp)
                        )
                    }
                }
            }
        }
    }
}
