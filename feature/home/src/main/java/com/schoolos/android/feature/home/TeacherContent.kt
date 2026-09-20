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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.School
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

private fun formatSessionTime(session: LearningSession): String {
    if (session.status.equals("active", ignoreCase = true)) return "Sedang Berlangsung"
    val scheduled = session.scheduledAt ?: return "Hari Ini"
    return try {
        val dt = java.time.Instant.parse(scheduled).atZone(java.time.ZoneId.systemDefault())
        java.time.format.DateTimeFormatter.ofPattern("HH:mm").format(dt) + " WIB"
    } catch (_: Exception) {
        "Hari Ini"
    }
}

private fun formatDueDate(iso: String?): String {
    if (iso.isNullOrBlank()) return "Tanpa batas tenggat"
    return try {
        val dt = java.time.Instant.parse(iso).atZone(java.time.ZoneId.systemDefault())
        "Tenggat: " + java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", java.util.Locale("id", "ID")).format(dt)
    } catch (_: Exception) {
        "Tenggat segera"
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
                            text = "Ketuk untuk melihat daftar & presensi siswa",
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

    // ── 2. BENTO ACTION GRID GURU ────────────────────────────────────────────────
    item {
        LightSectionHeader(
            title = "Pusat Aksi Pengajar",
            sub = "Kelola materi, tugas, dan evaluasi",
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
                    subtitle = "Tugaskan PR/proyek",
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
                    title = "Pengumuman",
                    subtitle = "Broadcast ke siswa/wali",
                    icon = Icons.Default.Campaign,
                    accentColor = NeonBlue,
                    onClick = onNavigateToBroadcastCenter,
                    modifier = Modifier.weight(1f),
                )

                BentoActionCard(
                    title = "Bahan Ajar",
                    subtitle = "Kelola modul belajar",
                    icon = Icons.Default.Book,
                    accentColor = TeacherNeon,
                    onClick = onNavigateToLearning,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }

    // ── 3. STATISTIK PENGAJAR CEPAT ──────────────────────────────────────────────
    item {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TeacherStatPillCard(
                title = "Agenda",
                value = "${todaySessions.size}",
                unit = "Sesi",
                icon = Icons.Default.DateRange,
                accentColor = NeonBlue,
                modifier = Modifier.weight(1f),
                onClick = onNavigateToSessions,
            )
            TeacherStatPillCard(
                title = "Tugas Aktif",
                value = pendingAssignmentsCount.ifBlank { "0" },
                unit = "Tugas",
                icon = Icons.AutoMirrored.Filled.Assignment,
                accentColor = TeacherNeon,
                modifier = Modifier.weight(1f),
                onClick = onNavigateToAssignments,
            )
            TeacherStatPillCard(
                title = "Modul Ajar",
                value = materialsCount.ifBlank { "0" },
                unit = "Bahan",
                icon = Icons.Default.Book,
                accentColor = NeonWarning,
                modifier = Modifier.weight(1f),
                onClick = onNavigateToLearning,
            )
        }
    }

    // ── 4. AGENDA SESI HARI INI ─────────────────────────────────────────────────
    item {
        LightSectionHeader(
            title = "Agenda Mengajar Hari Ini",
            sub = "Sesi pembelajaran kelas hari ini",
            onSeeAll = onNavigateToSessions,
        )
    }

    if (todaySessions.isNotEmpty()) {
        items(todaySessions.take(3)) { session ->
            TeacherSessionRowCard(
                session = session,
                onClick = onNavigateToSessions,
            )
        }
    } else {
        item {
            EmptyTeachingCard(
                title = "Tidak Ada Jadwal Mengajar Hari Ini",
                subtitle = "Belum ada sesi tatap muka terjadwal untuk hari ini. Waktu yang baik untuk menyusun bahan ajar atau menilai tugas siswa.",
                actionText = "Buka Jadwal Kelas",
                onClick = onNavigateToSessions,
            )
        }
    }

    // ── 5. EVALUASI & TUGAS SISWA ────────────────────────────────────────────────
    item {
        LightSectionHeader(
            title = "Tugas & Evaluasi Pembelajaran",
            sub = "Kelola penugasan yang diberikan ke siswa",
            onSeeAll = onNavigateToAssignments,
        )
    }

    if (teacherAssignments.isNotEmpty()) {
        items(teacherAssignments.take(3)) { asg ->
            TeacherAssignmentRowCard(
                assignment = asg,
                onClick = onNavigateToAssignments,
            )
        }
    } else {
        item {
            EmptyTeachingCard(
                title = "Belum Ada Tugas Aktif",
                subtitle = "Berikan penugasan latihan atau proyek baru untuk menguji pemahaman materi siswa.",
                actionText = "Buat Tugas Sekarang",
                onClick = onNavigateToAssignmentCreator,
            )
        }
    }

    // ── 6. PENGUMUMAN SEKOLAH ───────────────────────────────────────────────────
    item {
        LightSectionHeader(
            title = "Pengumuman Sekolah",
            sub = "Pemberitahuan resmi & surat edaran terkini",
            onSeeAll = onNavigateToNotifications,
        )
    }

    if (teacherAnnouncements.isNotEmpty()) {
        items(teacherAnnouncements.take(2)) { notif ->
            TeacherAnnouncementCard(
                notification = notif,
                onClick = onNavigateToNotifications,
            )
        }
    } else {
        item {
            EmptyTeachingCard(
                title = "Tidak Ada Pengumuman Baru",
                subtitle = "Semua surat edaran dan pengumuman sekolah terkini sudah Anda tinjau.",
                actionText = "Lihat Notifikasi",
                onClick = onNavigateToNotifications,
            )
        }
    }

    item {
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun TeacherStatPillCard(
    title: String,
    value: String,
    unit: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(CosmicNavy)
            .border(0.5.dp, GlassBorder, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    fontSize = 10.sp,
                    color = TextTertiary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(12.dp),
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 1,
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = unit,
                    fontSize = 10.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.Normal,
                    modifier = Modifier.padding(bottom = 2.dp),
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun TeacherSessionRowCard(
    session: LearningSession,
    onClick: () -> Unit,
) {
    val isLive = session.status.equals("active", ignoreCase = true)
    val isCompleted = session.status.equals("completed", ignoreCase = true)
    val subject = session.subjectName ?: session.notes?.substringBefore(" • ") ?: "Pelajaran"
    val room = formatClassName(session.className ?: session.room)
    val timeLabel = formatSessionTime(session)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CosmicNavy)
            .border(0.5.dp, if (isLive) NeonSuccess.copy(alpha = 0.5f) else GlassBorder, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
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
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isLive) NeonSuccess.copy(alpha = 0.15f) else CosmicSurface2)
                        .border(
                            0.5.dp,
                            if (isLive) NeonSuccess.copy(alpha = 0.4f) else GlassBorder,
                            RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.School,
                        contentDescription = null,
                        tint = if (isLive) NeonSuccess else TeacherNeon,
                        modifier = Modifier.size(18.dp),
                    )
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = subject,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "$room • $timeLabel",
                        fontSize = 11.sp,
                        color = if (isLive) NeonSuccess else TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        when {
                            isLive -> NeonSuccess.copy(alpha = 0.15f)
                            isCompleted -> TextTertiary.copy(alpha = 0.15f)
                            else -> NeonBlue.copy(alpha = 0.15f)
                        }
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Text(
                    text = when {
                        isLive -> "AKTIF"
                        isCompleted -> "SELESAI"
                        else -> "TERJADWAL"
                    },
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        isLive -> NeonSuccess
                        isCompleted -> TextTertiary
                        else -> NeonBlue
                    },
                )
            }
        }
    }
}

@Composable
private fun TeacherAssignmentRowCard(
    assignment: Assignment,
    onClick: () -> Unit,
) {
    val rawCls = assignment.className ?: assignment.classId
    val clsName = formatClassName(rawCls, fallback = "Semua Kelas")
    val dueText = formatDueDate(assignment.dueAt)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CosmicNavy)
            .border(0.5.dp, GlassBorder, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(TeacherNeon.copy(alpha = 0.12f))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = clsName,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TeacherNeon,
                        )
                    }
                    if (!assignment.subjectName.isNullOrBlank() && !isUuid(assignment.subjectName)) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = assignment.subjectName!!,
                            fontSize = 10.sp,
                            color = TextTertiary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))

                Text(
                    text = assignment.title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Spacer(Modifier.height(2.dp))

                Text(
                    text = dueText,
                    fontSize = 11.sp,
                    color = TextTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(CosmicSurface2)
                    .border(0.5.dp, GlassBorder, RoundedCornerShape(6.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
            ) {
                Text(
                    text = "Periksa",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary,
                )
            }
        }
    }
}

@Composable
private fun TeacherAnnouncementCard(
    notification: Notification,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CosmicNavy)
            .border(0.5.dp, GlassBorder, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(NeonBlue.copy(alpha = 0.12f))
                    .border(0.5.dp, NeonBlue.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Campaign,
                    contentDescription = null,
                    tint = NeonBlue,
                    modifier = Modifier.size(18.dp),
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = notification.title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = notification.body,
                    fontSize = 11.sp,
                    color = TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 15.sp,
                )
            }
        }
    }
}

@Composable
private fun EmptyTeachingCard(
    title: String,
    subtitle: String,
    actionText: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CosmicNavy)
            .border(0.5.dp, GlassBorder, RoundedCornerShape(12.dp))
            .padding(16.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = TextTertiary,
                lineHeight = 15.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            Spacer(Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(CosmicSurface2)
                    .border(0.5.dp, GlassBorder, RoundedCornerShape(6.dp))
                    .clickable(onClick = onClick)
                    .padding(horizontal = 14.dp, vertical = 6.dp),
            ) {
                Text(
                    text = actionText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary,
                )
            }
        }
    }
}
