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

fun LazyListScope.teacherGradebookContent(
    subjects: List<SubjectGradeSummary>,
    className: String,
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

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        Color(0xFF0F2B48),
                        Color(0xFF0B192E),
                        Color(0xFF050E1D)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(TeacherNeon.copy(alpha = 0.5f), Color.Transparent, TeacherNeon.copy(alpha = 0.2f))
                ),
                shape = RoundedCornerShape(22.dp)
            )
            .padding(horizontal = 16.dp, vertical = 14.dp)
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
                    .background(TeacherNeon.copy(alpha = 0.15f))
                    .border(1.dp, TeacherNeon.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
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
                    .background(NeonSuccess.copy(alpha = 0.12f))
                    .border(1.dp, NeonSuccess.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, null, tint = NeonSuccess, modifier = Modifier.size(11.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Dapodik Ready", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NeonSuccess)
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // CLASS TITLE & IDENTITY
        Text(
            text = "Kelas $className",
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            color = Color.White,
            letterSpacing = (-0.5).sp
        )
        Text(
            text = "Leger Nilai • ${DapodikPeriod.getFullPeriodLabel()}",
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White.copy(alpha = 0.75f)
        )

        Spacer(Modifier.height(12.dp))

        // BIG STATS ROW
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = if (hasGrades) "%.1f".format(avgScore) else "—",
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = (-1.5).sp
                )
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.padding(bottom = 4.dp)) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (hasGrades) TeacherNeon.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.1f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = predikatLabel,
                            color = if (hasGrades) TeacherNeon else Color.White.copy(alpha = 0.8f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(predikatDesc, color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Spacer(Modifier.height(12.dp))

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
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.07f))
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(14.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Column {
            Text(label, fontSize = 9.sp, color = Color.White.copy(alpha = 0.65f), fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(2.dp))
            Text(value, fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Black)
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
            .shadow(4.dp, RoundedCornerShape(20.dp), spotColor = GlassOverlay)
            .clip(RoundedCornerShape(20.dp))
            .background(CosmicNavy)
            .border(1.dp, GlassBorder, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
    ) {
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            // Left vibrant color accent stripe
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(accentColor)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
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
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(3.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Kelas $className",
                                fontSize = 11.sp,
                                color = accentColor,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("•", fontSize = 10.sp, color = TextTertiary)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "KKM 75",
                                fontSize = 11.sp,
                                color = TextTertiary,
                                fontWeight = FontWeight.Medium
                            )
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
                                .background(if (isGraded) accentColor.copy(alpha = 0.12f) else GlassBorder)
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

                Spacer(Modifier.height(12.dp))

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
                            color = if (isGraded) accentColor else TextTertiary.copy(alpha = 0.5f),
                            trackColor = CosmicDark,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (summary.componentCount > 0) {
                                "${summary.gradedComponentCount}/${summary.componentCount} Nilai"
                            } else {
                                "0 Nilai Masuk"
                            },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextTertiary
                        )
                    }

                    // Open Leger CTA
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(CosmicDark)
                            .border(1.dp, GlassBorder, RoundedCornerShape(20.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("Buka Leger", fontSize = 11.sp, fontWeight = FontWeight.Black, color = accentColor)
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
}
