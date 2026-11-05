package com.schoolos.android.feature.home

import com.schoolos.android.core.R as CoreR
import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.schoolos.android.core.designsystem.LocalIsDarkTheme
import com.schoolos.android.core.designsystem.NeonBlue
import com.schoolos.android.core.designsystem.NeonError
import com.schoolos.android.core.designsystem.NeonSuccess
import com.schoolos.android.core.designsystem.NeonWarning
import com.schoolos.android.core.designsystem.TeacherNeon
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
    teacherName: String = "",
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
    quizzesCount: String = "0",
    urgentTeachingSession: UrgentTeachingSession? = null,
) {
    val rawClass = when {
        activeClass.isNotBlank() && activeClass != "-" && !isUuid(activeClass) -> activeClass
        isHomeroom && teacherClasses.isNotEmpty() && !isUuid(teacherClasses.first().name) -> teacherClasses.first().name
        teacherClasses.isNotEmpty() && !isUuid(teacherClasses.first().name) -> teacherClasses.first().name
        else -> "-"
    }

    // ── 0. URGENT TEACHING SCHEDULE ALERT ALARM BANNER ──────────────────────────
    if (urgentTeachingSession != null) {
        item {
            TeacherScheduleAlertCard(
                urgentSession = urgentTeachingSession,
                onAddMaterial = onNavigateToLearning,
                onCreateAssignment = onNavigateToAssignmentCreator,
                onCreateQuiz = onNavigateToQuizBuilder,
            )
        }
    }

    // ── 0. KARTU SAPAAN GURU (GREETING HERO CARD SAMA DENGAN PORTAL SISWA & KEPALA SEKOLAH) ──
    item {
        val effectiveClassLabel = if (rawClass != "-") formatClassName(rawClass).uppercase() else "TUTOR PENGAMPU"
        val effectiveSubjectLabel = formatSubjectAbbreviation(activeSubject)
        StudentGreetingHeroCard(
            studentName = teacherName.ifBlank { "Tutor Pengampu" },
            studentClass = effectiveClassLabel,
            studentNisn = effectiveSubjectLabel,
            streakDays = 0,
            subtitle = "Siap mengampu sesi belajar hari ini? Pantau pengumpulan tugas siswa dan agenda kelas di bawah.",
            showStreak = false,
        )
    }

    // ── HEADER AKSES UTAMA ──
    item {
        HomeAksesUtamaSectionHeader()
    }

    // ── 1. AKSES UTAMA (2x2 GRID MODERN ROUNDED IDENTIK SISWA & KEPALA SEKOLAH) ──
    item {
        val isDark = LocalIsDarkTheme.current
        val cardBg = if (isDark) Color(0xFF111113) else Color.White
        val cardBorder = if (isDark) Color(0xFF1F1F23) else Color(0xFFE2E8F0)
        val textPrimary = if (isDark) Color.White else Color(0xFF0F172A)
        val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

        val tugasBadgeBg = if (isDark) Color(0xFF0C243B) else Color(0xFFE0F2FE)
        val tugasColor = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)
        val kuisBadgeBg = if (isDark) Color(0xFF0A2B1D) else Color(0xFFDCFCE7)
        val kuisColor = if (isDark) Color(0xFF34D399) else Color(0xFF16A34A)
        val materiBadgeBg = if (isDark) Color(0xFF261447) else Color(0xFFF3E8FF)
        val materiColor = if (isDark) Color(0xFFA78BFA) else Color(0xFF9333EA)
        val nilaiBadgeBg = if (isDark) Color(0xFF31220A) else Color(0xFFFEF3C7)
        val nilaiColor = if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706)

        val assBadge = if (pendingAssignmentsCount.isNotBlank() && pendingAssignmentsCount != "0") "$pendingAssignmentsCount Tugas" else "Aktif"
        val quizBadge = if (quizzesCount.isNotBlank() && quizzesCount != "0") "$quizzesCount Ujian" else "CBT Online"
        val materiBadge = if (materialsCount.isNotBlank() && materialsCount != "0") "$materialsCount Modul" else "Bahan Ajar"

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Row 1: Tugas Siswa & Kuis CBT
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                HomeMenuGridCard(
                    modifier = Modifier.weight(1f),
                    title = "Tugas Siswa",
                    subtitle = "Periksa pengumpulan & koreksi tugas mandiri",
                    badgeText = assBadge,
                    badgeBg = tugasBadgeBg,
                    badgeColor = tugasColor,
                    icon = Icons.AutoMirrored.Filled.Assignment,
                    iconCircleBg = tugasBadgeBg,
                    iconTint = tugasColor,
                    actionText = "Koreksi Tugas",
                    cardBg = cardBg,
                    cardBorder = cardBorder,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    onClick = onNavigateToAssignments,
                )
                HomeMenuGridCard(
                    modifier = Modifier.weight(1f),
                    title = "Kuis & Ujian",
                    subtitle = "Kelola asesmen CBT online & bank butir soal",
                    badgeText = quizBadge,
                    badgeBg = kuisBadgeBg,
                    badgeColor = kuisColor,
                    icon = Icons.Default.AccessTime,
                    iconCircleBg = kuisBadgeBg,
                    iconTint = kuisColor,
                    actionText = "Kelola Ujian",
                    cardBg = cardBg,
                    cardBorder = cardBorder,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    onClick = onNavigateToQuizzes,
                )
            }
            // Row 2: Bahan Ajar & Buku Nilai
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                HomeMenuGridCard(
                    modifier = Modifier.weight(1f),
                    title = "Bahan Ajar",
                    subtitle = "Unggah & bagikan modul digital serta materi",
                    badgeText = materiBadge,
                    badgeBg = materiBadgeBg,
                    badgeColor = materiColor,
                    icon = Icons.AutoMirrored.Filled.MenuBook,
                    iconCircleBg = materiBadgeBg,
                    iconTint = materiColor,
                    actionText = "Kelola Modul",
                    cardBg = cardBg,
                    cardBorder = cardBorder,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    onClick = onNavigateToLearning,
                )
                HomeMenuGridCard(
                    modifier = Modifier.weight(1f),
                    title = "Buku Nilai",
                    subtitle = "Rekapitulasi nilai, transkrip & capaian siswa",
                    badgeText = "Buku Nilai",
                    badgeBg = nilaiBadgeBg,
                    badgeColor = nilaiColor,
                    icon = Icons.Default.EmojiEvents,
                    iconCircleBg = nilaiBadgeBg,
                    iconTint = nilaiColor,
                    actionText = "Lihat Nilai",
                    cardBg = cardBg,
                    cardBorder = cardBorder,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    onClick = onNavigateToGrades,
                )
            }
        }
    }

    // ── 2. JADWAL MENGAJAR HARI INI (UNIFIED DESIGN SAMA DENGAN PORTAL SISWA) ────
    item {
        HomeTodayScheduleCard(
            todaySessions = todaySessions,
            onNavigateToSessions = onNavigateToSessions,
            onNavigateToSessionDetail = onNavigateToSessionDetail,
            cardTitle = "Jadwal Mengajar Hari Ini",
            subtitleBuilder = { session ->
                val sessionClass = formatClassName(session.className ?: session.room)
                if (session.status.equals("active", ignoreCase = true)) {
                    "$sessionClass • Sedang Berlangsung"
                } else {
                    sessionClass
                }
            },
        )
    }

    // ── 3. CLASS COMMAND HERO (WALI KELAS / KELAS PENGAMPU) ─────────────────────
    item {
        val isDark = LocalIsDarkTheme.current
        val cardBg = if (isDark) Color(0xFF111113) else Color.White
        val cardBorder = if (isDark) Color(0xFF232326) else Color(0xFFE2E8F0)
        val innerCardBg = if (isDark) Color(0xFF1B1B1E) else Color(0xFFF8FAFC)
        val innerCardBorder = if (isDark) Color(0xFF2E2E32) else Color(0xFFE2E8F0)
        val textPrimary = if (isDark) Color(0xFFF1F5F9) else Color(0xFF0F172A)
        val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

        val heroBadge = if (isHomeroom) "WALI KELAS" else "KELAS PENGAMPU"
        val heroTitle = if (rawClass != "-") formatClassName(rawClass) else if (isHomeroom) "Wali Kelas" else "Kelas Pengampu"
        val heroSubtitle = if (isHomeroom && heroTitle != "Wali Kelas") {
            "Wali Kelas $heroTitle • Rekap data siswa, kehadiran presensi harian, dan pantauan kelas"
        } else {
            "Rekap data siswa, kehadiran presensi harian, dan pantauan kelas"
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .clickable {
                    onNavigateToRombelStudents(if (teacherClasses.size > 1) "ALL" else rawClass)
                },
            shape = RoundedCornerShape(22.dp),
            color = cardBg,
            border = BorderStroke(1.dp, cardBorder),
            shadowElevation = if (isDark) 0.dp else 1.dp,
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
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
                            .clip(RoundedCornerShape(8.dp))
                            .background(badgeBg)
                            .border(0.5.dp, badgeBorder, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                    ) {
                        Text(
                            text = heroBadge,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = badgeFg,
                            letterSpacing = 0.5.sp,
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
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
                            modifier = Modifier.size(12.dp),
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
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(innerCardBg)
                            .border(1.dp, innerCardBorder, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Group,
                            contentDescription = null,
                            tint = textPrimary,
                            modifier = Modifier.size(22.dp),
                        )
                    }

                    Spacer(Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = heroTitle,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = heroSubtitle,
                            fontSize = 11.5.sp,
                            color = textSecondary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }

    // ── 4. STUDIO AKSI PENGAJAR (Executive Console Hub) ─────────────────────────
    item {
        TeacherSectionHeader(
            title = "Studio & Aksi Pengajar",
            subtitle = "Pusat pembuatan penugasan, asesmen kuis, dan modul ajar",
        )
    }

    item {
        TeacherStudioConsole(
            onCreateAssignment = onNavigateToAssignmentCreator,
            onCreateQuiz = onNavigateToQuizBuilder,
            onNavigateToLearning = onNavigateToLearning,
            onNavigateToGrades = onNavigateToGrades,
        )
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
            val isDark = LocalIsDarkTheme.current
            val cardBg = if (isDark) Color(0xFF111113) else Color.White
            val cardBorder = if (isDark) Color(0xFF232326) else Color(0xFFE2E8F0)
            val innerCardBg = if (isDark) Color(0xFF1B1B1E) else Color(0xFFF8FAFC)
            val textPrimary = if (isDark) Color(0xFFF1F5F9) else Color(0xFF0F172A)
            val textTertiary = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .clickable(onClick = onNavigateToAssignments),
                shape = RoundedCornerShape(22.dp),
                color = cardBg,
                border = BorderStroke(1.dp, cardBorder),
                shadowElevation = if (isDark) 0.dp else 1.dp,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = assignment.title.ifBlank { "Tugas Siswa" },
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary,
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
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(innerCardBg)
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                            ) {
                                Text(
                                    text = formatClassName(assignment.className ?: assignment.subjectName),
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textTertiary,
                                )
                            }

                            Text("•", fontSize = 8.sp, color = textTertiary)

                            Text(
                                text = formatDueDate(assignment.dueAt),
                                fontSize = 11.sp,
                                color = textTertiary,
                            )
                        }

                        if (assignment.createdAt.isNotBlank()) {
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "Diterbitkan: ${com.schoolos.android.core.common.formatPublishTimestamp(assignment.createdAt)}",
                                fontSize = 10.5.sp,
                                color = textTertiary
                            )
                        }
                    }

                    Spacer(Modifier.width(10.dp))

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(TeacherNeon.copy(alpha = 0.12f))
                            .border(1.dp, TeacherNeon.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    ) {
                        Text(
                            text = "Periksa",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TeacherNeon,
                        )
                    }
                }
            }
        }
    } else {
        item {
            val isDark = LocalIsDarkTheme.current
            val cardBg = if (isDark) Color(0xFF111113) else Color.White
            val cardBorder = if (isDark) Color(0xFF232326) else Color(0xFFE2E8F0)
            val textPrimary = if (isDark) Color(0xFFF1F5F9) else Color(0xFF0F172A)
            val textTertiary = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                color = cardBg,
                border = BorderStroke(1.dp, cardBorder),
                shadowElevation = if (isDark) 0.dp else 1.dp,
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Belum Ada Tugas Aktif",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "Buat penugasan baru untuk melatih pemahaman siswa.",
                            fontSize = 11.5.sp,
                            color = textTertiary,
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(TeacherNeon.copy(alpha = 0.12f))
                            .border(1.dp, TeacherNeon.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                            .clickable(onClick = onNavigateToAssignmentCreator)
                            .padding(horizontal = 10.dp, vertical = 6.dp),
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
            val isDark = LocalIsDarkTheme.current
            val cardBg = if (isDark) Color(0xFF111113) else Color.White
            val cardBorder = if (isDark) Color(0xFF232326) else Color(0xFFE2E8F0)
            val textPrimary = if (isDark) Color(0xFFF1F5F9) else Color(0xFF0F172A)
            val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
            val textTertiary = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .clickable(onClick = onNavigateToNotifications),
                shape = RoundedCornerShape(22.dp),
                color = cardBg,
                border = BorderStroke(1.dp, cardBorder),
                shadowElevation = if (isDark) 0.dp else 1.dp,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(NeonBlue.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Campaign,
                            contentDescription = null,
                            tint = NeonBlue,
                            modifier = Modifier.size(19.dp),
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
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                text = formatTimeAgo(notif.createdAt),
                                fontSize = 10.5.sp,
                                color = textTertiary,
                            )
                        }
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = notif.body,
                            fontSize = 11.5.sp,
                            color = textSecondary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    } else {
        item {
            val isDark = LocalIsDarkTheme.current
            val cardBg = if (isDark) Color(0xFF111113) else Color.White
            val cardBorder = if (isDark) Color(0xFF232326) else Color(0xFFE2E8F0)
            val textPrimary = if (isDark) Color(0xFFF1F5F9) else Color(0xFF0F172A)
            val textTertiary = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .clickable(onClick = onNavigateToBroadcastCenter),
                shape = RoundedCornerShape(22.dp),
                color = cardBg,
                border = BorderStroke(1.dp, cardBorder),
                shadowElevation = if (isDark) 0.dp else 1.dp,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(NeonBlue.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Campaign,
                                contentDescription = null,
                                tint = NeonBlue,
                                modifier = Modifier.size(19.dp),
                            )
                        }

                        Column {
                            Text(
                                text = "Kirim Pengumuman Rombel",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = textPrimary,
                            )
                            Text(
                                text = "Kirim broadcast pesan cepat ke seluruh siswa dan orang tua",
                                fontSize = 11.5.sp,
                                color = textTertiary,
                            )
                        }
                    }

                    Icon(
                        painter = painterResource(id = CoreR.drawable.ic_modern_chevron_right),
                        contentDescription = null,
                        tint = textTertiary,
                        modifier = Modifier.size(13.dp),
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
    val isDark = LocalIsDarkTheme.current
    val cardBg = if (isDark) Color(0xFF111113) else Color.White
    val cardBorder = if (isDark) Color(0xFF232326) else Color(0xFFE2E8F0)
    val textPrimary = if (isDark) Color(0xFFF1F5F9) else Color(0xFF0F172A)
    val textTertiary = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder),
        shadowElevation = if (isDark) 0.dp else 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(accentColor.copy(alpha = 0.12f))
                        .border(1.dp, accentColor.copy(alpha = 0.25f), RoundedCornerShape(9.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(17.dp),
                    )
                }

                Icon(
                    painter = painterResource(id = CoreR.drawable.ic_modern_chevron_right),
                    contentDescription = null,
                    tint = textTertiary,
                    modifier = Modifier.size(12.dp),
                )
            }

            Column {
                Text(
                    text = value,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(1.dp))
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = textTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

// ─── TEACHER STUDIO CONSOLE & ACTION HUB ─────────────────────────────────────

@Composable
private fun TeacherStudioConsole(
    onCreateAssignment: () -> Unit,
    onCreateQuiz: () -> Unit,
    onNavigateToLearning: () -> Unit,
    onNavigateToGrades: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDark = LocalIsDarkTheme.current
    val cardBg = if (isDark) Color(0xFF111113) else Color.White
    val cardBorder = if (isDark) Color(0xFF232326) else Color(0xFFE2E8F0)
    val innerCardBorder = if (isDark) Color(0xFF2E2E32) else Color(0xFFE2E8F0)

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder),
        shadowElevation = if (isDark) 0.dp else 1.dp,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // 1. Buat Tugas
            StudioConsoleRow(
                title = "Buat Tugas",
                subtitle = "PR, proyek, & lembar mandiri",
                actionLabel = "+ Buat",
                iconRes = CoreR.drawable.ic_modern_tasks,
                accentColor = TeacherNeon,
                onClick = onCreateAssignment,
            )

            HorizontalDivider(
                modifier = Modifier.padding(start = 62.dp, end = 14.dp),
                thickness = 0.5.dp,
                color = innerCardBorder,
            )

            // 2. Buat Kuis
            StudioConsoleRow(
                title = "Buat Kuis",
                subtitle = "Ujian CBT, bank soal, & evaluasi",
                actionLabel = "+ Buat",
                iconRes = CoreR.drawable.ic_modern_quiz,
                accentColor = NeonWarning,
                onClick = onCreateQuiz,
            )

            HorizontalDivider(
                modifier = Modifier.padding(start = 62.dp, end = 14.dp),
                thickness = 0.5.dp,
                color = innerCardBorder,
            )

            // 3. Bahan Ajar
            StudioConsoleRow(
                title = "Bahan Ajar",
                subtitle = "Modul dokumen, materi, & PDF",
                actionLabel = "Buka",
                iconRes = CoreR.drawable.ic_modern_book,
                accentColor = NeonSuccess,
                onClick = onNavigateToLearning,
            )

            HorizontalDivider(
                modifier = Modifier.padding(start = 62.dp, end = 14.dp),
                thickness = 0.5.dp,
                color = innerCardBorder,
            )

            // 4. Buku Nilai
            StudioConsoleRow(
                title = "Buku Nilai",
                subtitle = "Rekap capaian & ketuntasan KKTP",
                actionLabel = "Buka",
                iconRes = CoreR.drawable.ic_modern_grades,
                accentColor = NeonBlue,
                onClick = onNavigateToGrades,
            )
        }
    }
}

@Composable
private fun StudioConsoleRow(
    title: String,
    subtitle: String,
    actionLabel: String,
    @DrawableRes iconRes: Int,
    accentColor: Color,
    onClick: () -> Unit,
) {
    val isDark = LocalIsDarkTheme.current
    val textPrimary = if (isDark) Color(0xFFF1F5F9) else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Squircle Icon
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(accentColor.copy(alpha = 0.12f))
                .border(1.dp, accentColor.copy(alpha = 0.28f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = title,
                tint = accentColor,
                modifier = Modifier.size(19.dp),
            )
        }

        // Title and Subtitle
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                fontSize = 11.5.sp,
                color = textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        // Action Button
        Box(
            modifier = Modifier
                .width(68.dp)
                .height(29.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(accentColor.copy(alpha = 0.12f))
                .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = actionLabel,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(11.dp),
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
    val isDark = LocalIsDarkTheme.current
    val textPrimary = if (isDark) Color(0xFFF1F5F9) else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    val textTertiary = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)
    val innerCardBg = if (isDark) Color(0xFF1B1B1E) else Color(0xFFF8FAFC)
    val innerCardBorder = if (isDark) Color(0xFF2E2E32) else Color(0xFFE2E8F0)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f, fill = false),
            verticalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary,
                )
                if (count != null && count > 0) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(innerCardBg)
                            .border(1.dp, innerCardBorder, RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = "$count",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = textSecondary,
                        )
                    }
                }
            }
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    color = textTertiary,
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
                    fontSize = 11.5.sp,
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
    }
}

@Composable
private fun TeacherScheduleAlertCard(
    urgentSession: UrgentTeachingSession,
    onAddMaterial: () -> Unit,
    onCreateAssignment: () -> Unit,
    onCreateQuiz: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDark = LocalIsDarkTheme.current
    val cardBg = if (isDark) Color(0xFF111113) else Color.White
    val textPrimary = if (isDark) Color(0xFFF1F5F9) else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    val textTertiary = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)

    val isLive = urgentSession.isLive
    val cardBorder = if (isLive) NeonError.copy(alpha = 0.7f) else NeonWarning.copy(alpha = 0.6f)
    val headerColor = if (isLive) NeonError else NeonWarning
    val headerBg = if (isLive) NeonError.copy(alpha = 0.15f) else NeonWarning.copy(alpha = 0.15f)
    val headerTitle = if (isLive) {
        "PERINGATAN: SESI TUTORIAL SEDANG BERLANGSUNG"
    } else {
        "PENGINGAT: ${urgentSession.minutesUntilStart} MENIT MENUJU JADWAL SESI"
    }
    val descriptionText = if (isLive) {
        "Sesi tutorial telah dimulai. Segera siapkan materi modul ajar, penugasan, atau evaluasi KKTP untuk warga belajar."
    } else {
        "Jadwal tutorial dimulai pukul ${urgentSession.scheduledTimeStr}. Siapkan modul pembelajaran dan instrumen asesmen sebelum sesi dimulai."
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = cardBg,
        border = BorderStroke(1.2.dp, cardBorder),
        shadowElevation = if (isDark) 0.dp else 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top Badge & Schedule Time
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(headerBg)
                        .border(1.dp, cardBorder, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                ) {
                    Text(
                        text = headerTitle,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = headerColor,
                        letterSpacing = 0.3.sp,
                    )
                }

                Text(
                    text = urgentSession.scheduledTimeStr,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isLive) NeonError else NeonWarning,
                )
            }

            // Subject and Class Name
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = urgentSession.subjectName,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = urgentSession.className,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // Description / Warning message
            Text(
                text = descriptionText,
                fontSize = 11.5.sp,
                lineHeight = 16.sp,
                color = textTertiary,
            )

            // 3 Quick Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                AlertActionButton(
                    label = "+ Materi",
                    accentColor = NeonWarning,
                    icon = Icons.Default.Book,
                    onClick = onAddMaterial,
                    modifier = Modifier.weight(1f),
                )
                AlertActionButton(
                    label = "+ Tugas",
                    accentColor = TeacherNeon,
                    icon = Icons.AutoMirrored.Filled.Assignment,
                    onClick = onCreateAssignment,
                    modifier = Modifier.weight(1f),
                )
                AlertActionButton(
                    label = "+ Kuis",
                    accentColor = NeonBlue,
                    icon = Icons.Default.Quiz,
                    onClick = onCreateQuiz,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun AlertActionButton(
    label: String,
    accentColor: Color,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(accentColor.copy(alpha = 0.12f))
            .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 7.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(13.dp),
            )
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = accentColor,
                maxLines = 1,
            )
        }
    }
}

/**
 * Cleanly abbreviates and standardizes subject names for teacher profile badge dynamically.
 * Zero hardcoded teacher name tables or arbitrary fallback mappings.
 */
private fun abbreviateSingleSubject(sub: String): String {
    val s = sub.trim().uppercase()
    return when {
        s.contains("AGAMA") -> "PAI"
        s.contains("ALAM DAN SOSIAL") || s == "IPAS" || s.contains("IPAS") -> "IPAS"
        s.contains("ALAM") || s == "IPA" -> "IPA"
        (s.contains("SOSIAL") && !s.contains("SOSIOLOGI")) || s == "IPS" -> "IPS"
        s.contains("PANCASILA") || s.contains("PKN") || s.contains("PPKN") -> "PPKn"
        s.contains("INDONESIA") -> "B. INDONESIA"
        s.contains("INGGRIS") -> "B. INGGRIS"
        s.contains("CIREBON") -> "B. CIREBON"
        s.contains("MATEMATIKA") || s.contains("MTK") -> "MATEMATIKA"
        s.contains("KOMPUTER") || s.contains("TIK") || s.contains("INFORMATIKA") -> "TIK / KOMPUTER"
        s.contains("FUNGSIONAL") || s.contains("KETERAMPILAN") -> "KETERAMPILAN"
        s.contains("SENI") -> "SENI BUDAYA"
        s.contains("GEOGRAFI") -> "GEOGRAFI"
        s.contains("SOSIOLOGI") -> "SOSIOLOGI"
        s.contains("EKONOMI") -> "EKONOMI"
        s.contains("SEJARAH") -> "SEJARAH"
        s.contains("PJOK") || s.contains("OLAHRAGA") || s.contains("PENJAS") -> "PJOK"
        s.length > 20 -> s.take(18) + "..."
        else -> s
    }
}

private fun formatSubjectAbbreviation(rawSubject: String?): String {
    val s = rawSubject?.trim()?.uppercase().orEmpty()
    if (s.isBlank() || s == "-" || s == "PENDIDIK & GTK" || s == "GURU" || s == "TUTOR" || s == "UMUM") {
        return "TUTOR PENGAMPU"
    }
    val delimiter = when {
        s.contains("&") -> "&"
        s.contains(",") -> ","
        s.contains(";") -> ";"
        s.contains("/") -> "/"
        else -> null
    }
    if (delimiter != null) {
        val parts = s.split(delimiter).map { it.trim() }.filter { it.isNotBlank() }
        val formattedParts = parts.map { abbreviateSingleSubject(it) }.distinct()
        return formattedParts.joinToString(" & ")
    }
    return abbreviateSingleSubject(s)
}
