package com.schoolos.android.feature.grades

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.schoolos.android.core.common.DapodikPeriod
import com.schoolos.android.core.designsystem.*
import com.schoolos.android.domain.model.SubjectGradeSummary
import java.util.Locale

fun LazyListScope.teacherGradebookContent(
    subjects: List<SubjectGradeSummary>,
    className: String,
    teacherSubject: String? = null,
    totalSubjectCount: Int = subjects.size,
    selectedFilter: String = "Semua Mapel",
    isPrincipal: Boolean = false,
    onSelectFilter: (String) -> Unit = {},
    onSubjectClick: (String, String) -> Unit,
) {
    // ── TEACHER / PRINCIPAL CLASS HERO BANNER ─────────────────────
    item {
        TeacherClassHeroHeader(
            subjects = subjects,
            className = className,
            teacherSubject = teacherSubject,
            isPrincipal = isPrincipal,
        )
    }

    item {
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                if (isPrincipal) "LEGER NILAI SELURUH MODUL KESETARAAN"
                else if (!teacherSubject.isNullOrBlank()) "LEGER NILAI MAPEL DIAMPU${if (className.isNotBlank()) " • $className" else ""}"
                else if (className.isNotBlank()) "LEGER NILAI ROMBEL $className"
                else "REKAP NILAI MODUL DIAMPU",
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = TeacherNeon,
                letterSpacing = 1.sp
            )
            Text(
                if (!teacherSubject.isNullOrBlank()) "${subjects.size} Mapel Diampu" else "${subjects.size} Mapel",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextTertiary
            )
        }
    }

    // Dynamic Filter Chips (No hardcoded MIPA or fake categories)
    val dynamicCategories = (listOf("Semua Mapel") + subjects.map { it.category }.filter { it.isNotBlank() && it != "Wajib" }).distinct()
    if (dynamicCategories.size > 1) {
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 4.dp)
            ) {
                dynamicCategories.forEach { filter ->
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
                    Icon(
                        imageVector = Icons.Default.MenuBook,
                        contentDescription = null,
                        tint = TextTertiary,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = if (!teacherSubject.isNullOrBlank()) "Belum ada nilai modul tercatat untuk mata pelajaran $teacherSubject."
                            else if (className.isNotBlank()) "Belum ada modul pelajaran tercatat untuk Rombel $className."
                            else "Belum ada modul mata pelajaran tercatat.",
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
    teacherSubject: String? = null,
    isPrincipal: Boolean = false,
) {
    val gradedSubjects = subjects.filter { it.finalScore > 0.0 && it.gradedComponentCount > 0 }
    val hasGrades = gradedSubjects.isNotEmpty()
    val avgScore = if (hasGrades) gradedSubjects.map { it.finalScore }.average() else 0.0

    val headerBadgeText = when {
        isPrincipal -> "KEPALA PKBM • EKSEKUTIF LEGER KESETARAAN"
        !teacherSubject.isNullOrBlank() -> "GURU MAPEL ${teacherSubject.uppercase()} • ${DapodikPeriod.getActiveSemester().uppercase()}"
        className.isNotBlank() -> "TUTOR PAMONG • ${DapodikPeriod.getActiveSemester().uppercase()}"
        else -> "TUTOR MODUL / MAPEL • ${DapodikPeriod.getActiveSemester().uppercase()}"
    }

    val headerTitleText = when {
        isPrincipal -> "Leger Capaian Belajar PKBM"
        !teacherSubject.isNullOrBlank() -> teacherSubject
        className.isNotBlank() -> if (className.startsWith("Paket", ignoreCase = true)) className else "Rombel $className"
        else -> "Buku Penilaian Modul & Capaian Belajar"
    }

    val headerSubtitleText = when {
        isPrincipal -> "Monitoring Capaian KKTP & Mutu Belajar Rombel • ${DapodikPeriod.getFullPeriodLabel()}"
        !teacherSubject.isNullOrBlank() && className.isNotBlank() -> "Penilaian Sumatif Modul & Capaian KKTP • Rombel $className • ${DapodikPeriod.getFullPeriodLabel()}"
        className.isNotBlank() -> "Leger Nilai Rombel • ${DapodikPeriod.getFullPeriodLabel()}"
        else -> "Penilaian Capaian & Evaluasi Modul • ${DapodikPeriod.getFullPeriodLabel()}"
    }

    val passRateText = if (hasGrades) {
        val passedCount = gradedSubjects.count { it.isPassed }
        "${(passedCount * 100) / gradedSubjects.size}% Tuntas"
    } else {
        "—"
    }

    val isDark = LocalIsDarkTheme.current
    val heroBg = if (isDark) {
        Brush.linearGradient(
            listOf(
                TeacherContainer.copy(alpha = 0.35f),
                CosmicNavy,
                CosmicSurface2
            )
        )
    } else {
        Brush.linearGradient(
            listOf(
                TeacherNeon.copy(alpha = 0.08f),
                CosmicNavy,
                CosmicSurface2.copy(alpha = 0.5f)
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
        // TOP BADGE ROW (Pure Clean Presentation - No fake Dapodik Ready)
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
                    text = headerBadgeText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    color = TeacherNeon,
                    letterSpacing = 0.5.sp
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // CLASS TITLE & IDENTITY
        Text(
            text = headerTitleText,
            fontSize = 22.sp,
            fontWeight = FontWeight.Black,
            color = TextPrimary,
            letterSpacing = (-0.5).sp
        )
        Text(
            text = headerSubtitleText,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = TextSecondary
        )

        Spacer(Modifier.height(14.dp))

        // BIG STATS ROW (Factual state rendering - ADR-0011 Thin Client)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = if (hasGrades) "%.1f".format(Locale.US, avgScore) else "—",
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
                            text = if (hasGrades) "Terdata" else "Belum Dinilai",
                            color = if (hasGrades) TeacherNeon else TextTertiary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = if (hasGrades) "${gradedSubjects.size} dari ${subjects.size} Mapel Dinilai" else "Menunggu input nilai",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // 3 METRIC PILLS (Factual, NO hardcoded 75.0 fallback)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TeacherMetricCard(
                label = if (!teacherSubject.isNullOrBlank()) "Mapel Diampu" else "Modul / Pelajaran",
                value = if (subjects.isNotEmpty()) {
                    if (!teacherSubject.isNullOrBlank()) "${subjects.size} Mapel" else "${subjects.size} Modul"
                } else "—",
                modifier = Modifier.weight(1f)
            )
            val firstThreshold = subjects.firstOrNull { it.passingThreshold > 0 }?.passingThreshold
            val targetKktpText = if (firstThreshold != null && firstThreshold > 0) "${firstThreshold.toInt()}" else "—"
            TeacherMetricCard(
                label = "Target KKTP",
                value = targetKktpText,
                modifier = Modifier.weight(1f)
            )
            TeacherMetricCard(
                label = "Ketercapaian",
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
    val icon = subjectIcon(summary.subjectName)
    val accentColor = when {
        summary.subjectName.contains("Matematika", ignoreCase = true) -> StudentNeon
        summary.subjectName.contains("IPA", ignoreCase = true) || summary.subjectName.contains("Fisika", ignoreCase = true) || summary.subjectName.contains("Biologi", ignoreCase = true) -> NeonBlue
        summary.subjectName.contains("Bahasa", ignoreCase = true) -> NeonSuccess
        summary.subjectName.contains("IPS", ignoreCase = true) || summary.subjectName.contains("Ekonomi", ignoreCase = true) || summary.subjectName.contains("Geografi", ignoreCase = true) -> NeonWarning
        summary.subjectName.contains("Olahraga", ignoreCase = true) || summary.subjectName.contains("Penjas", ignoreCase = true) -> NeonError
        summary.subjectName.contains("Informatika", ignoreCase = true) || summary.subjectName.contains("TIK", ignoreCase = true) || summary.subjectName.contains("Komputer", ignoreCase = true) -> AccentNeonCyan
        else -> TeacherNeon
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
                // Subject Vector Icon Chip (Zero Emoji)
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(accentColor.copy(alpha = 0.12f))
                        .border(1.dp, accentColor.copy(alpha = 0.25f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(22.dp)
                    )
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
                        if (className.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(accentColor.copy(alpha = 0.1f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (className.startsWith("Paket", ignoreCase = true)) className else "Rombel $className",
                                    fontSize = 10.sp,
                                    color = accentColor,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        if (summary.passingThreshold > 0.0) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(CosmicSurface2)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "KKTP ${summary.passingThreshold.toInt()}",
                                    fontSize = 10.sp,
                                    color = TextTertiary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                // Score & Status Column (Thin Client SSOT)
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (isGraded) "%.1f".format(Locale.US, summary.finalScore) else "—",
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
                            text = if (isGraded) {
                                if (summary.letterGrade.isNotBlank() && summary.letterGrade != "-") "Capaian ${summary.letterGrade}" else "Sudah Dinilai"
                            } else "Belum Dinilai",
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
