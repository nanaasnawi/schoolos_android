package com.schoolos.android.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Notifications
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
import com.schoolos.android.core.designsystem.CosmicDark
import com.schoolos.android.core.designsystem.CosmicNavy
import com.schoolos.android.core.designsystem.CosmicSurface2
import com.schoolos.android.core.designsystem.GlassBorder
import com.schoolos.android.core.designsystem.GlassBorder2
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

private fun formatSessionTime(scheduledAt: String?, startedAt: String?): String {
    val raw = scheduledAt ?: startedAt ?: return "Hari ini"
    return try {
        val dt = java.time.Instant.parse(raw).atZone(java.time.ZoneId.systemDefault())
        val formatter = java.time.format.DateTimeFormatter.ofPattern("HH:mm", java.util.Locale.getDefault())
        "${dt.format(formatter)} WIB"
    } catch (_: Exception) {
        "Hari ini"
    }
}

private fun formatDueDate(dueAt: String?): String {
    if (dueAt.isNullOrBlank()) return "Tanpa batas waktu"
    return try {
        val dt = java.time.Instant.parse(dueAt).atZone(java.time.ZoneId.systemDefault())
        val formatter = java.time.format.DateTimeFormatter.ofPattern("d MMM, HH:mm", java.util.Locale("id", "ID"))
        "Tenggat: ${dt.format(formatter)}"
    } catch (_: Exception) {
        "Tenggat segera"
    }
}

private fun formatTimeAgo(timestampStr: String?): String {
    if (timestampStr.isNullOrBlank()) return "Terkini"
    return try {
        val dt = java.time.Instant.parse(timestampStr).toEpochMilli()
        val diff = System.currentTimeMillis() - dt
        if (diff < 0) return "Baru saja"
        val mins = diff / (60 * 1000)
        if (mins < 1) return "Baru saja"
        if (mins < 60) return "$mins mnt lalu"
        val hours = mins / 60
        if (hours < 24) return "$hours jam lalu"
        val days = hours / 24
        if (days == 1L) return "Kemarin"
        if (days < 30) return "$days hr lalu"
        "${days / 30} bln lalu"
    } catch (_: Exception) {
        "Terkini"
    }
}

fun LazyListScope.teacherContent(
    onNavigateToSessions: () -> Unit,
    onNavigateToSessionDetail: (String) -> Unit = {},
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

    // ── 1. EXECUTIVE METRICS COCKPIT (2x2 Grid) ──────────────────────────────────
    item {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TeacherKpiCard(
                    title = "Tugas Aktif",
                    value = if (pendingAssignmentsCount.isNotBlank()) "$pendingAssignmentsCount Tugas" else "0 Tugas",
                    subtitle = "Perlu diperiksa",
                    icon = Icons.AutoMirrored.Filled.Assignment,
                    accentColor = TeacherNeon,
                    onClick = onNavigateToAssignments,
                    modifier = Modifier.weight(1f),
                )

                TeacherKpiCard(
                    title = "Presensi Rombel",
                    value = if (attendanceRate.isNotBlank() && attendanceRate != "-") attendanceRate else "100%",
                    subtitle = "Kehadiran siswa",
                    icon = Icons.Default.CheckCircle,
                    accentColor = NeonSuccess,
                    onClick = { if (rawClass.isNotBlank()) onNavigateToRombelStudents(rawClass) },
                    modifier = Modifier.weight(1f),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TeacherKpiCard(
                    title = "Sesi Mengajar",
                    value = if (todaySessions.isNotEmpty()) "${todaySessions.size} Sesi" else if (scheduleCount.isNotBlank()) "$scheduleCount Sesi" else "0 Sesi",
                    subtitle = "Agenda hari ini",
                    icon = Icons.Default.CalendarMonth,
                    accentColor = NeonBlue,
                    onClick = onNavigateToSessions,
                    modifier = Modifier.weight(1f),
                )

                TeacherKpiCard(
                    title = "Bahan Ajar",
                    value = if (materialsCount.isNotBlank()) "$materialsCount Modul" else "0 Modul",
                    subtitle = "Materi & dokumen",
                    icon = Icons.Default.Book,
                    accentColor = NeonWarning,
                    onClick = onNavigateToLearning,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }

    // ── 2. CLASS COMMAND HERO (WALI KELAS / KELAS PENGAMPU) ─────────────────────
    item {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(CosmicNavy)
                .border(0.5.dp, GlassBorder, RoundedCornerShape(14.dp))
                .clickable {
                    if (rawClass.isNotBlank()) onNavigateToRombelStudents(rawClass)
                }
                .padding(14.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Top Row: Role badge & quick action
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val badgeBg = if (isHomeroom) TeacherNeon.copy(alpha = 0.12f) else NeonBlue.copy(alpha = 0.12f)
                    val badgeFg = if (isHomeroom) TeacherNeon else NeonBlue
                    val badgeBorder = if (isHomeroom) TeacherNeon.copy(alpha = 0.35f) else NeonBlue.copy(alpha = 0.35f)

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(badgeBg)
                            .border(0.5.dp, badgeBorder, RoundedCornerShape(6.dp))
                            .padding(horizontal = 7.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = if (isHomeroom) "WALI KELAS" else "KELAS PENGAMPU",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = badgeFg,
                            letterSpacing = 0.5.sp,
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Text(
                            text = "Presensi & Siswa",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TeacherNeon,
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = TeacherNeon,
                            modifier = Modifier.size(11.dp),
                        )
                    }
                }

                // Middle: Class Title & Detail
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(CosmicSurface2)
                            .border(0.5.dp, GlassBorder, RoundedCornerShape(10.dp)),
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
                            text = displayClass,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "Rekap data siswa, kehadiran presensi harian, dan pantauan kelas",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                // Bottom: Multiple classes quick selector chips (if available)
                if (teacherClasses.size > 1) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        items(teacherClasses) { cls ->
                            val isSelected = cls.name.equals(rawClass, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSelected) CosmicSurface2 else CosmicDark)
                                    .border(0.5.dp, if (isSelected) GlassBorder2 else GlassBorder, RoundedCornerShape(6.dp))
                                    .clickable { onNavigateToRombelStudents(cls.name) }
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                            ) {
                                Text(
                                    text = formatClassName(cls.name),
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (isSelected) TextPrimary else TextTertiary,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // ── 3. STUDIO AKSI PENGAJAR (2x2 Bento Action Cards) ─────────────────────────
    item {
        TeacherSectionHeader(
            title = "Studio & Aksi Pengajar",
            subtitle = "Pusat pembuatan penugasan, asesmen kuis, dan modul ajar",
        )
    }

    item {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                BentoActionCard(
                    title = "Buat Tugas",
                    subtitle = "PR & proyek mandiri",
                    icon = Icons.AutoMirrored.Filled.Assignment,
                    accentColor = TeacherNeon,
                    badgeText = "Tugas",
                    onClick = onNavigateToAssignmentCreator,
                    modifier = Modifier.weight(1f),
                )

                BentoActionCard(
                    title = "Buat Kuis",
                    subtitle = "Bank soal & ujian online",
                    icon = Icons.Default.Quiz,
                    accentColor = NeonWarning,
                    badgeText = "Kuis",
                    onClick = onNavigateToQuizBuilder,
                    modifier = Modifier.weight(1f),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                BentoActionCard(
                    title = "Buku Nilai",
                    subtitle = "Rekap capaian & KKM",
                    icon = Icons.Default.Assessment,
                    accentColor = NeonBlue,
                    badgeText = "Nilai",
                    onClick = onNavigateToGrades,
                    modifier = Modifier.weight(1f),
                )

                BentoActionCard(
                    title = "Bahan Ajar",
                    subtitle = "Upload modul dokumen",
                    icon = Icons.Default.Book,
                    accentColor = NeonSuccess,
                    badgeText = "Modul",
                    onClick = onNavigateToLearning,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }

    // ── 4. JADWAL MENGAJAR HARI INI ──────────────────────────────────────────────
    item {
        TeacherSectionHeader(
            title = "Jadwal Mengajar Hari Ini",
            subtitle = "Agenda tatap muka dan sesi kelas terjadwal",
            count = todaySessions.size,
            actionLabel = "Lihat Semua",
            onActionClick = onNavigateToSessions,
        )
    }

    if (todaySessions.isNotEmpty()) {
        items(todaySessions.take(3), key = { it.id }) { session ->
            val isLive = session.status.equals("active", ignoreCase = true)
            val timeLabel = formatSessionTime(session.scheduledAt, session.startedAt)
            val sessionClass = formatClassName(session.className ?: session.room)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CosmicNavy)
                    .border(
                        0.5.dp,
                        if (isLive) NeonSuccess.copy(alpha = 0.45f) else GlassBorder,
                        RoundedCornerShape(12.dp)
                    )
                    .clickable { onNavigateToSessionDetail(session.id) }
                    .padding(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Time indicator box
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isLive) NeonSuccess.copy(alpha = 0.12f) else CosmicSurface2)
                                .border(
                                    0.5.dp,
                                    if (isLive) NeonSuccess.copy(alpha = 0.35f) else GlassBorder,
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = timeLabel,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isLive) NeonSuccess else TextPrimary,
                            )
                        }

                        Spacer(Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = session.subjectName?.ifBlank { "Mata Pelajaran" } ?: "Mata Pelajaran",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Spacer(Modifier.height(2.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Text(
                                    text = sessionClass,
                                    fontSize = 11.sp,
                                    color = TextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                if (isLive) {
                                    Text("•", fontSize = 8.sp, color = NeonSuccess)
                                    Text(
                                        text = "Sedang Berlangsung",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NeonSuccess,
                                    )
                                }
                            }
                        }
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = TextTertiary,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        }
    } else {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CosmicNavy)
                    .border(0.5.dp, GlassBorder, RoundedCornerShape(12.dp))
                    .padding(14.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(CosmicSurface2),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = TextTertiary,
                            modifier = Modifier.size(18.dp),
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Tidak Ada Sesi Mengajar Hari Ini",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "Jadwal tatap muka bebas. Manfaatkan waktu untuk evaluasi tugas atau menyusun modul ajar.",
                            fontSize = 11.sp,
                            color = TextTertiary,
                        )
                    }
                }
            }
        }
    }

    // ── 5. PENUGASAN BERJALAN & ANTREAN KOREKSI ──────────────────────────────────
    item {
        TeacherSectionHeader(
            title = "Penugasan & Antrean Tugas",
            subtitle = "Tinjau pengumpulan tugas siswa yang aktif",
            count = teacherAssignments.size,
            actionLabel = "Kelola Semua",
            onActionClick = onNavigateToAssignments,
        )
    }

    if (teacherAssignments.isNotEmpty()) {
        items(teacherAssignments.take(3), key = { it.id }) { assignment ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CosmicNavy)
                    .border(0.5.dp, GlassBorder, RoundedCornerShape(12.dp))
                    .clickable(onClick = onNavigateToAssignments)
                    .padding(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = assignment.title.ifBlank { "Tugas Siswa" },
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )

                        Spacer(Modifier.height(4.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(CosmicSurface2)
                                    .padding(horizontal = 5.dp, vertical = 1.dp),
                            ) {
                                Text(
                                    text = formatClassName(assignment.className ?: assignment.subjectName),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextTertiary,
                                )
                            }

                            Text("•", fontSize = 8.sp, color = TextTertiary)

                            Text(
                                text = formatDueDate(assignment.dueAt),
                                fontSize = 11.sp,
                                color = TextTertiary,
                            )
                        }
                    }

                    Spacer(Modifier.width(8.dp))

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(TeacherNeon.copy(alpha = 0.12f))
                            .border(0.5.dp, TeacherNeon.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = "Periksa",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TeacherNeon,
                        )
                    }
                }
            }
        }
    } else {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CosmicNavy)
                    .border(0.5.dp, GlassBorder, RoundedCornerShape(12.dp))
                    .padding(14.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Belum Ada Tugas Aktif",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "Buat penugasan baru untuk melatih pemahaman siswa.",
                            fontSize = 11.sp,
                            color = TextTertiary,
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(TeacherNeon.copy(alpha = 0.12f))
                            .border(0.5.dp, TeacherNeon.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                            .clickable(onClick = onNavigateToAssignmentCreator)
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                    ) {
                        Text(
                            text = "+ Buat Tugas",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TeacherNeon,
                        )
                    }
                }
            }
        }
    }

    // ── 6. PUSAT INFORMASI & BROADCAST SEKOLAH ──────────────────────────────────
    item {
        TeacherSectionHeader(
            title = "Pengumuman & Broadcast",
            subtitle = "Kirim kabar pengumuman atau cek rilis sekolah",
            actionLabel = "Kirim Pesan",
            onActionClick = onNavigateToBroadcastCenter,
        )
    }

    if (teacherAnnouncements.isNotEmpty()) {
        items(teacherAnnouncements.take(2), key = { it.id }) { notif ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CosmicNavy)
                    .border(0.5.dp, GlassBorder, RoundedCornerShape(12.dp))
                    .clickable(onClick = onNavigateToNotifications)
                    .padding(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(NeonBlue.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Campaign,
                            contentDescription = null,
                            tint = NeonBlue,
                            modifier = Modifier.size(17.dp),
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = notif.title.ifBlank { "Pengumuman Sekolah" },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                text = formatTimeAgo(notif.createdAt),
                                fontSize = 10.sp,
                                color = TextTertiary,
                            )
                        }
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = notif.body,
                            fontSize = 11.sp,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    } else {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CosmicNavy)
                    .border(0.5.dp, GlassBorder, RoundedCornerShape(12.dp))
                    .clickable(onClick = onNavigateToBroadcastCenter)
                    .padding(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(NeonBlue.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Campaign,
                                contentDescription = null,
                                tint = NeonBlue,
                                modifier = Modifier.size(17.dp),
                            )
                        }

                        Column {
                            Text(
                                text = "Kirim Pengumuman Rombel",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary,
                            )
                            Text(
                                text = "Kirim broadcast pesan cepat ke seluruh siswa dan orang tua",
                                fontSize = 11.sp,
                                color = TextTertiary,
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = TextTertiary,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        }
    }

    item {
        Spacer(Modifier.height(16.dp))
    }
}

// ─── REUSABLE TEACHER KPI CARD ───────────────────────────────────────────────

@Composable
private fun TeacherKpiCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(CosmicNavy)
            .border(0.5.dp, GlassBorder, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(accentColor.copy(alpha = 0.12f))
                        .border(0.5.dp, accentColor.copy(alpha = 0.25f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(16.dp),
                    )
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = TextTertiary,
                    modifier = Modifier.size(12.dp),
                )
            }

            Column {
                Text(
                    text = value,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(1.dp))
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = TextTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

// ─── REUSABLE SECTION HEADER ─────────────────────────────────────────────────

@Composable
private fun TeacherSectionHeader(
    title: String,
    subtitle: String = "",
    count: Int? = null,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f, fill = false)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                )
                if (count != null && count > 0) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(CosmicSurface2)
                            .border(0.5.dp, GlassBorder, RoundedCornerShape(4.dp))
                            .padding(horizontal = 5.dp, vertical = 1.dp),
                    ) {
                        Text(
                            text = "$count",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                        )
                    }
                }
            }
            if (subtitle.isNotBlank()) {
                Spacer(Modifier.height(1.dp))
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = TextTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        if (actionLabel != null && onActionClick != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(onClick = onActionClick)
                    .padding(horizontal = 6.dp, vertical = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = actionLabel,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = TeacherNeon,
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = TeacherNeon,
                    modifier = Modifier.size(11.dp),
                )
            }
        }
    }
}
