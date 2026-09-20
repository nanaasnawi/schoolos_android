package com.schoolos.android.feature.home

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.schoolos.android.core.designsystem.CosmicNavy
import com.schoolos.android.core.designsystem.CosmicSurface2
import com.schoolos.android.core.designsystem.GlassBorder
import com.schoolos.android.core.designsystem.NeonBlue
import com.schoolos.android.core.designsystem.NeonSuccess
import com.schoolos.android.core.designsystem.NeonWarning
import com.schoolos.android.core.designsystem.TeacherNeon
import com.schoolos.android.core.designsystem.TextPrimary
import com.schoolos.android.core.designsystem.TextSecondary
import com.schoolos.android.core.designsystem.TextTertiary
import com.schoolos.android.domain.model.AcademicClass
import com.schoolos.android.domain.model.AcademicSubject
import com.schoolos.android.domain.model.Assignment
import com.schoolos.android.domain.model.LearningSession
import com.schoolos.android.domain.model.Notification

private fun isUuid(str: String?): Boolean {
    if (str.isNullOrBlank()) return false
    val clean = str.trim()
    return clean.length >= 32 && clean.contains("-")
}

private fun formatClassName(raw: String?, fallback: String = "Kelas Pengampu"): String {
    if (raw.isNullOrBlank() || raw == "-" || isUuid(raw)) {
        return fallback
    }
    val clean = raw.trim()
    return if (clean.startsWith("Kelas", ignoreCase = true) ||
        clean.startsWith("Paket", ignoreCase = true) ||
        clean.startsWith("Ruang", ignoreCase = true)
    ) {
        clean
    } else {
        "Kelas $clean"
    }
}

fun LazyListScope.teacherContent(
    onNavigateToSessions: () -> Unit,
    onNavigateToAssignments: () -> Unit,
    onNavigateToQuizzes: () -> Unit,
    onNavigateToGrades: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToAssignmentCreator: () -> Unit,
    onNavigateToQuizBuilder: () -> Unit,
    onNavigateToBroadcastCenter: () -> Unit,
    onNavigateToLearning: () -> Unit = {},
    onNavigateToRombelStudents: (String) -> Unit = {},
    activeSubject: String = "-",
    activeClass: String = "-",
    isHomeroom: Boolean = false,
    teacherClasses: List<AcademicClass> = emptyList(),
    teacherSubjects: List<AcademicSubject> = emptyList(),
    todaySessions: List<LearningSession> = emptyList(),
    teacherAssignments: List<Assignment> = emptyList(),
    teacherAnnouncements: List<Notification> = emptyList(),
    pendingAssignmentsCount: String = "0",
    materialsCount: String = "0",
    scheduleCount: String = "0",
    attendanceRate: String = "-",
) {
    val rawClass = when {
        activeClass.isNotBlank() && activeClass != "-" && !isUuid(activeClass) -> activeClass
        isHomeroom && teacherClasses.isNotEmpty() && !isUuid(teacherClasses.first().name) -> teacherClasses.first().name
        teacherClasses.isNotEmpty() && !isUuid(teacherClasses.first().name) -> teacherClasses.first().name
        else -> "PAKET A5"
    }
    val displayClass = formatClassName(rawClass)

    // ── 1. HOMEROOM / TEACHING CLASS HERO ────────────────────────────────────────
    item {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(CosmicNavy)
                .border(0.5.dp, GlassBorder, RoundedCornerShape(12.dp))
                .clickable {
                    if (rawClass.isNotBlank()) onNavigateToRombelStudents(rawClass)
                }
                .padding(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(CosmicSurface2)
                            .border(0.5.dp, GlassBorder, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Group,
                            contentDescription = null,
                            tint = TextPrimary,
                            modifier = Modifier.size(20.dp),
                        )
                    }

                    Spacer(Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isHomeroom) "WALI KELAS" else "KELAS AMPUAN",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextTertiary,
                            letterSpacing = 0.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = displayClass,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "Ketuk untuk melihat daftar siswa & rekap presensi",
                            fontSize = 11.sp,
                            color = TextTertiary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = TextTertiary,
                    modifier = Modifier.size(15.dp),
                )
            }
        }
    }

    // ── 2. PUSAT AKSI PENGAJAR (PEMBUATAN & PENERBITAN) ─────────────────────────
    item {
        LightSectionHeader(
            title = "Pusat Aksi Pengajar",
            sub = "Kelola materi, tugas, kuis, dan broadcast kelas",
        )
    }

    item {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                BentoActionCard(
                    title = "Buat Tugas",
                    subtitle = "Tugaskan PR & proyek",
                    icon = Icons.AutoMirrored.Filled.Assignment,
                    accentColor = TeacherNeon,
                    badgeText = "Baru",
                    onClick = onNavigateToAssignmentCreator,
                    modifier = Modifier.weight(1f),
                )

                BentoActionCard(
                    title = "Buat Kuis",
                    subtitle = "Ujian & bank soal",
                    icon = Icons.Default.Quiz,
                    accentColor = NeonWarning,
                    onClick = onNavigateToQuizBuilder,
                    modifier = Modifier.weight(1f),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                BentoActionCard(
                    title = "Broadcast",
                    subtitle = "Kirim info ke rombel",
                    icon = Icons.Default.Campaign,
                    accentColor = NeonBlue,
                    onClick = onNavigateToBroadcastCenter,
                    modifier = Modifier.weight(1f),
                )

                BentoActionCard(
                    title = "Bahan Ajar",
                    subtitle = "Modul & dokumen ajar",
                    icon = Icons.Default.Book,
                    accentColor = TeacherNeon,
                    onClick = onNavigateToLearning,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }

    // ── 3. EVALUASI & PENILAIAN SISWA ───────────────────────────────────────────
    item {
        LightSectionHeader(
            title = "Evaluasi & Rekap Nilai",
            sub = "Buku nilai siswa dan koreksi penugasan aktif",
            onSeeAll = onNavigateToGrades,
        )
    }

    item {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            TeacherWorkflowCard(
                title = "Buku Nilai Guru",
                subtitle = "Capaian & KKM",
                detail = "Rekap nilai formatif & sumatif",
                icon = Icons.Default.Assessment,
                accentColor = NeonBlue,
                actionLabel = "Buka",
                onClick = onNavigateToGrades,
                modifier = Modifier.weight(1f),
            )

            TeacherWorkflowCard(
                title = "Koreksi Tugas",
                subtitle = if (pendingAssignmentsCount != "0" && pendingAssignmentsCount.isNotBlank()) "$pendingAssignmentsCount Tugas Aktif" else "Antrean Tugas",
                detail = "Tinjau & beri nilai tugas",
                icon = Icons.AutoMirrored.Filled.Assignment,
                accentColor = TeacherNeon,
                actionLabel = "Periksa",
                onClick = onNavigateToAssignments,
                modifier = Modifier.weight(1f),
            )
        }
    }

    // ── 4. PRESENSI & BANK SOAL KELAS ───────────────────────────────────────────
    item {
        LightSectionHeader(
            title = "Presensi & Bank Soal",
            sub = "Keaktifan kehadiran siswa dan evaluasi kuis",
        )
    }

    item {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            TeacherWorkflowCard(
                title = "Presensi Rombel",
                subtitle = if (attendanceRate != "-" && attendanceRate.isNotBlank()) "Kehadiran $attendanceRate" else "Rekap Kehadiran",
                detail = displayClass,
                icon = Icons.Default.CheckCircle,
                accentColor = NeonSuccess,
                actionLabel = "Rekap",
                onClick = { if (rawClass.isNotBlank()) onNavigateToRombelStudents(rawClass) },
                modifier = Modifier.weight(1f),
            )

            TeacherWorkflowCard(
                title = "Bank Soal & Kuis",
                subtitle = "Bank Butir Soal",
                detail = "Riwayat & arsip ujian kelas",
                icon = Icons.Default.Quiz,
                accentColor = NeonWarning,
                actionLabel = "Kelola",
                onClick = onNavigateToQuizzes,
                modifier = Modifier.weight(1f),
            )
        }
    }

    item {
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun TeacherWorkflowCard(
    title: String,
    subtitle: String,
    detail: String,
    icon: ImageVector,
    accentColor: Color,
    actionLabel: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(CosmicNavy)
            .border(0.5.dp, GlassBorder, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(accentColor.copy(alpha = 0.12f))
                        .border(0.5.dp, accentColor.copy(alpha = 0.28f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(17.dp),
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(CosmicSurface2)
                        .border(0.5.dp, GlassBorder, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = actionLabel,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary,
                    )
                    Spacer(Modifier.width(3.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = TextTertiary,
                        modifier = Modifier.size(10.dp),
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(Modifier.height(2.dp))

            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = accentColor,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(Modifier.height(2.dp))

            Text(
                text = detail,
                fontSize = 10.sp,
                color = TextTertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
