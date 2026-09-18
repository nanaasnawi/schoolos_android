package com.schoolos.android.feature.home

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.schoolos.android.core.designsystem.CosmicNavy
import com.schoolos.android.core.designsystem.GlassBorder
import com.schoolos.android.core.designsystem.LineTrendChart
import com.schoolos.android.core.designsystem.NeonBlue
import com.schoolos.android.core.designsystem.NeonSuccess
import com.schoolos.android.core.designsystem.NeonWarning
import com.schoolos.android.core.designsystem.StudentNeon
import com.schoolos.android.core.designsystem.TextPrimary
import com.schoolos.android.core.designsystem.TextSecondary
import com.schoolos.android.core.designsystem.TextTertiary

fun LazyListScope.studentContent(
    onNavigateToSessions: () -> Unit,
    onNavigateToSessionDetail: (String) -> Unit = {},
    onNavigateToAssignments: () -> Unit,
    onNavigateToQuizzes: () -> Unit,
    onNavigateToGrades: () -> Unit,
    onNavigateToProgress: () -> Unit,
    onNavigateToAchievements: () -> Unit,
    onNavigateToLearning: () -> Unit,
    onNavigateToAssignmentWithSubject: (String) -> Unit = {},
    onNavigateToQuizWithSubject: (String) -> Unit = {},
    onNavigateToMaterialWithSubject: (String) -> Unit = {},
    nextSessionSubject: String = "-",
    nextSessionRoom: String = "-",
    nextSessionTime: String = "-",
    nextSessionIsLive: Boolean = false,
    todaySessions: List<com.schoolos.android.domain.model.LearningSession> = emptyList(),
    gradeAverage: String = "-",
    gradeStatus: String = "Belum ada data nilai",
    gradeTrendPoints: List<Float> = emptyList(),
    topGradeSubjects: List<com.schoolos.android.domain.model.SubjectGradeSummary> = emptyList(),
    studentProgress: com.schoolos.android.domain.model.Progress? = null,
    progressPercentage: Float = 0f,
) {
    val currentSubject = todaySessions.firstOrNull { it.status == "active" }?.subjectName
        ?: todaySessions.firstOrNull()?.subjectName
        ?: todaySessions.firstOrNull()?.notes?.substringBefore(" • ")?.trim()
        ?: ""
    val currentSubjectClean = currentSubject.substringBefore(" • ").substringBefore(" (Ruang").trim()

    // ── 1. BENTO QUICK ACCESS GRID (2x2) ─────────────────────────────────────────
    item {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Row 1: Tugas & Kuis
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                BentoActionCard(
                    title = "Tugas Sekolah",
                    subtitle = "Cek tenggat PR & proyek",
                    icon = Icons.AutoMirrored.Filled.Assignment,
                    accentColor = StudentNeon,
                    badgeText = "Aktif",
                    onClick = { onNavigateToAssignmentWithSubject(currentSubjectClean) },
                    modifier = Modifier.weight(1f),
                )

                BentoActionCard(
                    title = "Kuis & Ujian",
                    subtitle = "Latihan & evaluasi",
                    icon = Icons.Default.Quiz,
                    accentColor = NeonWarning,
                    badgeText = "Online",
                    onClick = { onNavigateToQuizWithSubject(currentSubjectClean) },
                    modifier = Modifier.weight(1f),
                )
            }

            // Row 2: Materi & Nilai Rapor
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                BentoActionCard(
                    title = "Materi Belajar",
                    subtitle = "Buku digital & modul",
                    icon = Icons.Default.Book,
                    accentColor = NeonBlue,
                    onClick = { onNavigateToMaterialWithSubject(currentSubjectClean) },
                    modifier = Modifier.weight(1f),
                )

                BentoActionCard(
                    title = "Rapor & Nilai",
                    subtitle = "Transkrip akademik",
                    icon = Icons.Default.Assessment,
                    accentColor = NeonSuccess,
                    badgeText = if (gradeAverage != "-") "Rata $gradeAverage" else "",
                    onClick = onNavigateToGrades,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }

    // ── 2. TODAY'S CLASS SCHEDULE TIMELINE PREVIEW ──────────────────────────────
    item {
        LightSectionHeader(
            title = "Jadwal Pelajaran Hari Ini",
            sub = "${todaySessions.size} Sesi terjadwal",
            onSeeAll = onNavigateToSessions,
        )
    }

    item {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(4.dp, RoundedCornerShape(22.dp), spotColor = GlassBorder)
                .clip(RoundedCornerShape(22.dp))
                .background(CosmicNavy)
                .border(1.dp, GlassBorder, RoundedCornerShape(22.dp)),
        ) {
            if (todaySessions.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("🏝️", fontSize = 32.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Tidak Ada Kelas Hari Ini",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = "Nikmati waktu istirahat atau pelajari materi berikutnya.",
                        fontSize = 12.sp,
                        color = TextTertiary,
                    )
                }
            } else {
                Column {
                    todaySessions.take(4).forEachIndexed { index, s ->
                        val title = s.subjectName ?: s.notes?.substringBefore(" • ") ?: "Mapel"
                        val room = s.room ?: "Ruang Kelas"
                        val teacher = s.notes?.substringAfter(" • ")?.substringBefore(" (")?.trim() ?: ""
                        val time = formatSessionTime(s.scheduledAt ?: s.startedAt)

                        ModernTimelineRow(
                            time = time,
                            subject = title,
                            room = room,
                            teacher = teacher,
                            status = s.status,
                            accentColor = StudentNeon,
                            onClick = { onNavigateToSessionDetail(s.id) },
                        )

                        if (index < todaySessions.take(4).size - 1) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = GlassBorder.copy(alpha = 0.6f),
                                thickness = 0.7.dp,
                            )
                        }
                    }
                }
            }
        }
    }

    // ── 3. ACADEMIC PERFORMANCE & LEVEL SNAPSHOT ─────────────────────────────────
    item {
        LightSectionHeader(
            title = "Performa & Capaian Belajar",
            sub = "Ringkasan nilai semester berjalan",
            onSeeAll = onNavigateToGrades,
        )
    }

    item {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(6.dp, RoundedCornerShape(22.dp), spotColor = GlassBorder)
                .clip(RoundedCornerShape(22.dp))
                .background(CosmicNavy)
                .border(1.dp, GlassBorder, RoundedCornerShape(22.dp))
                .clickable(onClick = onNavigateToGrades)
                .padding(18.dp),
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            text = "RATA-RATA NILAI",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextTertiary,
                            letterSpacing = 0.6.sp,
                        )
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = gradeAverage,
                                fontSize = 34.sp,
                                fontWeight = FontWeight.Black,
                                color = StudentNeon,
                                letterSpacing = (-1).sp,
                            )
                            Spacer(Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .padding(bottom = 6.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(NeonSuccess.copy(alpha = 0.15f))
                                    .padding(horizontal = 7.dp, vertical = 3.dp),
                            ) {
                                Text(
                                    text = "Stabil",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = NeonSuccess,
                                )
                            }
                        }
                    }

                    // Level & Badge Button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(StudentNeon.copy(alpha = 0.18f), StudentNeon.copy(alpha = 0.06f))
                                )
                            )
                            .border(1.dp, StudentNeon.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                            .clickable(onClick = onNavigateToAchievements)
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = null,
                                tint = StudentNeon,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Prestasi Siswa",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = StudentNeon,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Trend chart or visual meter
                if (gradeTrendPoints.isNotEmpty() && gradeTrendPoints.size > 1) {
                    LineTrendChart(
                        dataPoints = gradeTrendPoints,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(65.dp),
                        lineColor = StudentNeon,
                    )
                    Spacer(Modifier.height(12.dp))
                }

                // Status message
                Text(
                    text = gradeStatus,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextSecondary,
                )
            }
        }
    }
}

private fun formatSessionTime(rawTime: String?): String {
    if (rawTime == null) return "--:--"
    return try {
        val parser = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault()).apply {
            timeZone = java.util.TimeZone.getTimeZone("UTC")
        }
        val out = java.text.SimpleDateFormat("HH.mm", java.util.Locale.getDefault()).apply {
            timeZone = java.util.TimeZone.getTimeZone("Asia/Jakarta")
        }
        out.format(parser.parse(rawTime.substringBefore(".")) ?: java.util.Date())
    } catch (e: Exception) {
        "--:--"
    }
}
