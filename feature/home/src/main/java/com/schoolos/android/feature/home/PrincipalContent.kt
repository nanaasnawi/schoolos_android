package com.schoolos.android.feature.home

import com.schoolos.android.core.R as CoreR
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.filled.AccessTime as FilledAccessTime
import androidx.compose.material.icons.filled.Campaign as FilledCampaign
import androidx.compose.material.icons.automirrored.filled.Assignment as FilledAssignment
import androidx.compose.material.icons.automirrored.filled.MenuBook as FilledMenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.schoolos.android.core.auth.isPkbmInstitution
import com.schoolos.android.core.designsystem.*
import com.schoolos.android.domain.model.AcademicClass
import com.schoolos.android.domain.model.Assignment
import com.schoolos.android.domain.model.LearningSession
import com.schoolos.android.domain.model.Notification
import com.schoolos.android.domain.model.SchoolHealthMetrics
import com.schoolos.android.domain.model.SchoolStabilityLevel

private fun isUuid(str: String?): Boolean {
    if (str.isNullOrBlank()) return false
    val clean = str.trim()
    return clean.length >= 32 && clean.contains("-")
}

private fun formatClassName(raw: String?, fallback: String = "Kelas"): String {
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

private fun formatHourMinute(raw: String?): String? {
    if (raw.isNullOrBlank()) return null
    val trimmed = raw.trim()
    if (trimmed.matches(Regex("""^\d{1,2}:\d{2}(:\d{2})?$"""))) {
        val parts = trimmed.split(":")
        val h = parts[0].toIntOrNull() ?: return null
        val m = parts[1].toIntOrNull() ?: return null
        return String.format(java.util.Locale.US, "%02d.%02d", h, m)
    }
    if (trimmed.contains(" ") && trimmed.length >= 16) {
        val timePart = trimmed.substringAfter(" ").trim()
        if (timePart.matches(Regex("""^\d{1,2}:\d{2}(:\d{2})?$"""))) {
            val parts = timePart.split(":")
            val h = parts[0].toIntOrNull()
            val m = parts[1].toIntOrNull()
            if (h != null && m != null) {
                return String.format(java.util.Locale.US, "%02d.%02d", h, m)
            }
        }
    }
    return try {
        val instant = java.time.Instant.parse(trimmed)
        val zdt = instant.atZone(java.time.ZoneId.of("Asia/Jakarta"))
        String.format(java.util.Locale.US, "%02d.%02d", zdt.hour, zdt.minute)
    } catch (_: Exception) {
        try {
            val zdt = java.time.ZonedDateTime.parse(trimmed).withZoneSameInstant(java.time.ZoneId.of("Asia/Jakarta"))
            String.format(java.util.Locale.US, "%02d.%02d", zdt.hour, zdt.minute)
        } catch (_: Exception) {
            null
        }
    }
}

fun LazyListScope.principalContent(
    onNavigateToSessions: () -> Unit,
    onNavigateToSessionDetail: (String) -> Unit = {},
    onNavigateToAssignments: () -> Unit,
    onNavigateToQuizzes: () -> Unit,
    onNavigateToGrades: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToBroadcastCenter: () -> Unit,
    onNavigateToLearning: () -> Unit = {},
    onNavigateToRombelStudents: (String) -> Unit = {},
    onNavigateToAiChat: () -> Unit = {},
    principalName: String = "",
    schoolClasses: List<AcademicClass> = emptyList(),
    totalStudentsCount: String = "0",
    todaySessions: List<LearningSession> = emptyList(),
    teacherAssignments: List<Assignment> = emptyList(),
    teacherAnnouncements: List<Notification> = emptyList(),
    pendingAssignmentsCount: String = "0",
    materialsCount: String = "0",
    scheduleCount: String = "0",
    quizzesCount: String = "0",
    schoolHealthMetrics: SchoolHealthMetrics? = null,
    schoolName: String = "",
) {
    val isPkbm = isPkbmInstitution(schoolName)
    val principalRole = "Kepala Sekolah"
    val greetingSubtitle = if (isPkbm) {
        "Pantau operasional PKBM, keaktifan tutor, dan mutu pembelajaran hari ini."
    } else {
        "Pantau operasional sekolah, keaktifan guru, dan mutu pembelajaran hari ini."
    }

    // ── 0. KARTU SAPAAN (SAMA DENGAN PORTAL SISWA) ──────────────────────────
    item {
        StudentGreetingHeroCard(
            studentName = principalName.ifBlank { principalRole },
            studentClass = principalRole,
            studentNisn = "",
            streakDays = 0,
            subtitle = greetingSubtitle,
            showStreak = false,
        )
    }

    // ── 0.5. EARLY WARNING DISPOSISI (HANYA MUNCUL JIKA ADA ANOMALI RIIL) ────────
    val topAnomaly = schoolHealthMetrics?.anomalies?.firstOrNull()
    if (topAnomaly != null) {
        item {
            val isDark = LocalIsDarkTheme.current
            val cardBg = if (isDark) Color(0xFF111113) else Color.White
            val textPrimary = if (isDark) Color.White else Color(0xFF0F172A)
            val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(cardBg)
                    .border(1.dp, NeonWarning.copy(alpha = 0.5f), RoundedCornerShape(22.dp))
                    .clickable(onClick = onNavigateToBroadcastCenter)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(NeonWarning.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.WarningAmber,
                                contentDescription = null,
                                tint = NeonWarning,
                                modifier = Modifier.size(18.dp),
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = topAnomaly.title,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "Ketuk untuk terbitkan maklumat / disposisi kepala sekolah",
                                fontSize = 11.sp,
                                color = textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Text(
                            text = "Disposisi",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = NeonWarning,
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                            contentDescription = null,
                            tint = NeonWarning,
                            modifier = Modifier.size(13.dp),
                        )
                    }
                }
            }
        }
    }

    // ── AKSES UTAMA (KOMPONEN SAMA DENGAN PORTAL SISWA) ──────────────────────
    item {
        HomeAksesUtamaSectionHeader()
    }

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
        val memoBadgeBg = if (isDark) Color(0xFF31220A) else Color(0xFFFEF3C7)
        val memoColor = if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706)

        val actualAssignmentCount = pendingAssignmentsCount.toIntOrNull() ?: teacherAssignments.size
        val assBadge = if (actualAssignmentCount > 0) "$actualAssignmentCount Tugas" else "Aktif"
        val quizBadge = if (quizzesCount.isNotBlank() && quizzesCount != "0") "$quizzesCount Kuis" else "CBT Online"
        val materiBadge = if (materialsCount.isNotBlank() && materialsCount != "0") "$materialsCount Modul" else "Modul"

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                HomeMenuGridCard(
                    modifier = Modifier.weight(1f),
                    title = "Tugas Sekolah",
                    subtitle = if (isPkbm) "Pantau penugasan mandiri dari para tutor" else "Pantau penugasan mandiri dari para guru",
                    badgeText = assBadge,
                    badgeBg = tugasBadgeBg,
                    badgeColor = tugasColor,
                    icon = Icons.AutoMirrored.Filled.FilledAssignment,
                    iconCircleBg = tugasBadgeBg,
                    iconTint = tugasColor,
                    actionText = "Pantau Tugas",
                    cardBg = cardBg,
                    cardBorder = cardBorder,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    onClick = onNavigateToAssignments,
                )
                HomeMenuGridCard(
                    modifier = Modifier.weight(1f),
                    title = "Kuis & Ujian",
                    subtitle = "Rekap asesmen CBT & kuis seluruh rombel",
                    badgeText = quizBadge,
                    badgeBg = kuisBadgeBg,
                    badgeColor = kuisColor,
                    icon = Icons.Filled.FilledAccessTime,
                    iconCircleBg = kuisBadgeBg,
                    iconTint = kuisColor,
                    actionText = "Pantau Ujian",
                    cardBg = cardBg,
                    cardBorder = cardBorder,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    onClick = onNavigateToQuizzes,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                HomeMenuGridCard(
                    modifier = Modifier.weight(1f),
                    title = "Materi Belajar",
                    subtitle = if (isPkbm) "Modul & bahan ajar yang diunggah tutor" else "Modul & bahan ajar yang diunggah guru",
                    badgeText = materiBadge,
                    badgeBg = materiBadgeBg,
                    badgeColor = materiColor,
                    icon = Icons.AutoMirrored.Filled.FilledMenuBook,
                    iconCircleBg = materiBadgeBg,
                    iconTint = materiColor,
                    actionText = "Lihat Materi",
                    cardBg = cardBg,
                    cardBorder = cardBorder,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    onClick = onNavigateToLearning,
                )
                HomeMenuGridCard(
                    modifier = Modifier.weight(1f),
                    title = "Maklumat & Memo",
                    subtitle = "Terbitkan instruksi resmi kepala sekolah",
                    badgeText = "Broadcast",
                    badgeBg = memoBadgeBg,
                    badgeColor = memoColor,
                    icon = Icons.Filled.FilledCampaign,
                    iconCircleBg = memoBadgeBg,
                    iconTint = memoColor,
                    actionText = "Buat Maklumat",
                    cardBg = cardBg,
                    cardBorder = cardBorder,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    onClick = onNavigateToBroadcastCenter,
                )
            }
        }
    }

    // ── 0.8. DEDICATED EXECUTIVE AI ANALITIK KEPALA SEKOLAH (REAL-TIME INTELLIGENCE) ──
    item {
        val isDark = LocalIsDarkTheme.current
        val cardBg = if (isDark) Color(0xFF111113) else Color.White
        val innerCardBg = if (isDark) Color(0xFF1B1B1E) else Color(0xFFF8FAFC)
        val innerCardBorder = if (isDark) Color(0xFF2E2E32) else Color(0xFFE2E8F0)
        val textPrimary = if (isDark) Color.White else Color(0xFF0F172A)
        val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
        val principalAccent = Color(0xFF8B5CF6) // Executive Violet

        val hasData = schoolHealthMetrics != null && schoolHealthMetrics.hasRealData
        val attendance = if (hasData && schoolHealthMetrics.attendanceRate > 0) "${schoolHealthMetrics.attendanceRate.toInt()}%" else "—"

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(cardBg)
                .border(
                    width = 1.2.dp,
                    color = principalAccent.copy(alpha = if (isDark) 0.45f else 0.35f),
                    shape = RoundedCornerShape(22.dp)
                )
                .clickable { onNavigateToAiChat() }
                .padding(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Header with Mascot and NVIDIA tag
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(principalAccent.copy(alpha = 0.15f))
                                .border(1.dp, principalAccent.copy(alpha = 0.4f), CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Image(
                                painter = painterResource(id = CoreR.drawable.mascot_akselerasi_edu),
                                contentDescription = "AI Analitik Kepala Sekolah",
                                modifier = Modifier.size(30.dp),
                                contentScale = ContentScale.Fit
                            )
                        }

                        Column {
                            Text(
                                text = "AI Analitik Kepala Sekolah",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = textPrimary,
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "Analisis instan keaktifan guru, tugas, kuis, & mutu rombel",
                                fontSize = 11.sp,
                                color = textSecondary,
                            )
                        }
                    }

                }

                // Live Snapshot metrics row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(innerCardBg)
                        .border(1.dp, innerCardBorder, RoundedCornerShape(14.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MicroHealthStat(
                        label = if (isPkbm) "Modul Tutor" else "Modul Guru",
                        value = if (materialsCount.isNotBlank()) materialsCount else "0",
                        accent = NeonSuccess,
                    )
                    Box(modifier = Modifier.width(1.dp).height(20.dp).background(innerCardBorder))
                    MicroHealthStat(
                        label = "Tugas Aktif",
                        value = if (pendingAssignmentsCount.isNotBlank()) pendingAssignmentsCount else "0",
                        accent = TeacherNeon,
                    )
                    Box(modifier = Modifier.width(1.dp).height(20.dp).background(innerCardBorder))
                    MicroHealthStat(
                        label = "Kuis CBT",
                        value = if (quizzesCount.isNotBlank()) quizzesCount else "0",
                        accent = NeonBlue,
                    )
                    Box(modifier = Modifier.width(1.dp).height(20.dp).background(innerCardBorder))
                    MicroHealthStat(
                        label = "Kehadiran",
                        value = attendance,
                        accent = principalAccent,
                    )
                }

                // Quick-Action Chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    item {
                        PrincipalAiQuickChip(
                            icon = Icons.AutoMirrored.Outlined.Assignment,
                            label = "Keaktifan Guru & Modul",
                            accent = TeacherNeon,
                            onClick = onNavigateToAiChat,
                        )
                    }
                    item {
                        PrincipalAiQuickChip(
                            icon = Icons.Outlined.Quiz,
                            label = "Rekap Tugas & Kuis CBT",
                            accent = NeonBlue,
                            onClick = onNavigateToAiChat,
                        )
                    }
                    item {
                        PrincipalAiQuickChip(
                            icon = Icons.Outlined.WarningAmber,
                            label = "Deteksi Siswa At-Risk",
                            accent = NeonWarning,
                            onClick = onNavigateToAiChat,
                        )
                    }
                    item {
                        PrincipalAiQuickChip(
                            icon = Icons.Outlined.Insights,
                            label = "Ketercapaian KKTP",
                            accent = principalAccent,
                            onClick = onNavigateToAiChat,
                        )
                    }
                }
            }
        }
    }

    // ── 1. PRINCIPAL COMMAND HERO (PEMANTAUAN SELURUH ROMBEL PKBM) ─────────
    item {
        val isDark = LocalIsDarkTheme.current
        val cardBg = if (isDark) Color(0xFF111113) else Color.White
        val cardBorder = if (isDark) Color(0xFF232326) else Color(0xFFE2E8F0)
        val innerCardBg = if (isDark) Color(0xFF1B1B1E) else Color(0xFFF8FAFC)
        val innerCardBorder = if (isDark) Color(0xFF2E2E32) else Color(0xFFE2E8F0)
        val textPrimary = if (isDark) Color.White else Color(0xFF0F172A)
        val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

        val classCount = if (schoolClasses.isNotEmpty()) schoolClasses.size else schoolHealthMetrics?.totalClasses ?: 0
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(cardBg)
                .border(1.dp, cardBorder, RoundedCornerShape(22.dp))
                .clickable { onNavigateToRombelStudents("SUPERVISION") }
                .padding(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(TeacherNeon.copy(alpha = 0.12f))
                            .border(0.5.dp, TeacherNeon.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                    ) {
                        Text(
                            text = if (isPkbm) "PENGAWASAN ROMBEL PKBM" else "PENGAWASAN ROMBEL",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = TeacherNeon,
                            letterSpacing = 0.5.sp,
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Text(
                            text = "Buka Rombel",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TeacherNeon,
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                            contentDescription = null,
                            tint = TeacherNeon,
                            modifier = Modifier.size(12.dp),
                        )
                    }
                }

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
                            imageVector = Icons.Outlined.SupervisorAccount,
                            contentDescription = null,
                            tint = textPrimary,
                            modifier = Modifier.size(22.dp),
                        )
                    }

                    Spacer(Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isPkbm) "Supervisi $classCount Rombel PKBM" else "Supervisi $classCount Rombel",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = if (isPkbm) {
                                "Akses rekapitulasi data Warga Belajar, keaktifan belajar, dan progres modul"
                            } else {
                                "Akses rekapitulasi data Siswa, keaktifan belajar, dan progres materi"
                            },
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

    // ── 4. JADWAL MENGAJAR / PELAJARAN HARI INI (UNIFIED DESIGN SAMA DENGAN PORTAL SISWA) ──
    item {
        HomeTodayScheduleCard(
            todaySessions = todaySessions,
            onNavigateToSessions = onNavigateToSessions,
            onNavigateToSessionDetail = onNavigateToSessionDetail,
            cardTitle = "Jadwal Mengajar Hari Ini",
            subtitleBuilder = { session ->
                val sessionClass = formatClassName(session.className ?: session.room)
                val tutor = session.teacherName?.trim().orEmpty()
                if (tutor.isNotBlank()) "$sessionClass • $tutor" else sessionClass
            },
        )
    }

    // ── 5. MONITORING PENUGASAN WARGA BELAJAR ──────────────────────────────────
    if (teacherAssignments.isNotEmpty()) {
        item {
            PrincipalSectionHeader(
                title = "Monitoring Tugas Warga Belajar",
                subtitle = "Rekapitulasi penugasan aktif dari tutor",
                count = pendingAssignmentsCount.toIntOrNull() ?: teacherAssignments.size,
                actionLabel = "Lihat Semua",
                onActionClick = onNavigateToAssignments,
            )
        }

        items(teacherAssignments.take(3), key = { "assign-${it.id}" }) { assignment ->
            PrincipalAssignmentItemCard(
                assignment = assignment,
                onClick = onNavigateToAssignments
            )
        }
    }

    // ── 5.5. WIDGET BUKU NILAI & LEGER SISWA (PINDAH DARI BOTTOM NAV) ────────
    item {
        val isDark = LocalIsDarkTheme.current
        val cardBg = if (isDark) Color(0xFF111113) else Color.White
        val cardBorder = if (isDark) Color(0xFF1F1F23) else Color(0xFFE2E8F0)
        val innerBg = if (isDark) Color(0xFF161618) else Color(0xFFF8FAFC)
        val innerBorder = if (isDark) Color(0xFF27272A) else Color(0xFFE2E8F0)
        val textPrimary = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A)
        val textSecondary = if (isDark) Color(0xFFA1A1AA) else Color(0xFF64748B)
        val textTertiary = if (isDark) Color(0xFF71717A) else Color(0xFF94A3B8)
        val emeraldFg = if (isDark) Color(0xFF34D399) else Color(0xFF10B981)
        val emeraldBg = if (isDark) Color(0xFF0A2B1D) else Color(0xFFDCFCE7)

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            PrincipalSectionHeader(
                title = "Buku Nilai & Leger Siswa",
                subtitle = "Standar Kurikulum Merdeka (Kemendikdasmen RI)",
                actionLabel = "Buka Leger",
                onActionClick = onNavigateToGrades,
            )

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onNavigateToGrades),
                shape = RoundedCornerShape(22.dp),
                color = cardBg,
                border = BorderStroke(1.dp, cardBorder),
                shadowElevation = 0.dp,
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    // Header Row with Badge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(emeraldBg)
                                    .border(1.dp, emeraldFg.copy(alpha = 0.35f), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    painter = painterResource(id = CoreR.drawable.ic_modern_grades),
                                    contentDescription = null,
                                    tint = emeraldFg,
                                    modifier = Modifier.size(22.dp),
                                )
                            }
                            Column {
                                Text(
                                    text = "Buku Nilai (Teacher Gradebook)",
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textPrimary,
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = if (isPkbm) "Asesmen Sumatif & Evaluasi KKTP PKBM" else "Asesmen Sumatif & Evaluasi KKTP",
                                    fontSize = 11.sp,
                                    color = textSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = emeraldBg,
                            border = BorderStroke(1.dp, emeraldFg.copy(alpha = 0.35f)),
                        ) {
                            Text(
                                text = "Skema ATS & SAS",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = emeraldFg,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                            )
                        }
                    }

                    // Metric Strip: Total Siswa, Rombel Aktif, Kategori
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            color = innerBg,
                            border = BorderStroke(1.dp, innerBorder),
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(
                                    text = totalStudentsCount.ifBlank { "0" },
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textPrimary,
                                )
                                Text(
                                    text = "Peserta Didik",
                                    fontSize = 10.5.sp,
                                    color = textTertiary,
                                )
                            }
                        }

                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            color = innerBg,
                            border = BorderStroke(1.dp, innerBorder),
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                val classCount = if (schoolClasses.isNotEmpty()) schoolClasses.size.toString() else "Semua"
                                Text(
                                    text = classCount,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = emeraldFg,
                                )
                                Text(
                                    text = "Rombel / Kelas",
                                    fontSize = 10.5.sp,
                                    color = textTertiary,
                                )
                            }
                        }

                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            color = innerBg,
                            border = BorderStroke(1.dp, innerBorder),
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(
                                    text = "KKTP",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7),
                                )
                                Text(
                                    text = "Skema B Murni",
                                    fontSize = 10.5.sp,
                                    color = textTertiary,
                                )
                            }
                        }
                    }

                    // Action Footer
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = innerBg,
                        border = BorderStroke(1.dp, innerBorder),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Akses Rekap Nilai & Leger Lengkap",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = textPrimary,
                            )
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
        }
    }

    // ── 6. GRAFIK KEAKTIFAN GURU 1 MINGGU (REAL-TIME LINE CHART) ──────────────
    item {
        PrincipalTeacherWeeklyActivityCard(
            metrics = schoolHealthMetrics,
            isPkbm = isPkbm,
            sessionsCount = todaySessions.size,
        )
    }

    // ── 7. GRAFIK KEAKTIFAN MURID 1 MINGGU (REAL-TIME ROUNDED BAR CHART) ──────
    item {
        PrincipalStudentWeeklyActivityCard(
            metrics = schoolHealthMetrics,
            assignmentsCount = teacherAssignments.size,
            totalStudents = totalStudentsCount,
            isPkbm = isPkbm,
        )
    }

    item {
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun PrincipalAiQuickChip(
    icon: ImageVector,
    label: String,
    accent: Color,
    onClick: () -> Unit,
) {
    val isDark = LocalIsDarkTheme.current
    val innerCardBg = if (isDark) Color(0xFF1B1B1E) else Color(0xFFF8FAFC)
    val innerCardBorder = if (isDark) Color(0xFF2E2E32) else Color(0xFFE2E8F0)
    val textPrimary = if (isDark) Color.White else Color(0xFF0F172A)

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(innerCardBg)
            .border(1.dp, innerCardBorder, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = textPrimary,
        )
    }
}

@Composable
private fun MicroHealthStat(
    label: String,
    value: String,
    accent: Color,
) {
    val isDark = LocalIsDarkTheme.current
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Bold,
            color = accent,
        )
        Spacer(Modifier.height(1.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            color = textSecondary,
        )
    }
}

@Composable
private fun PrincipalSectionHeader(
    title: String,
    subtitle: String = "",
    count: Int? = null,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
) {
    val isDark = LocalIsDarkTheme.current
    val innerCardBg = if (isDark) Color(0xFF1B1B1E) else Color(0xFFF8FAFC)
    val innerCardBorder = if (isDark) Color(0xFF2E2E32) else Color(0xFFE2E8F0)
    val textPrimary = if (isDark) Color.White else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    val textTertiary = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)

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
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onActionClick)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = actionLabel,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = TeacherNeon,
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                    contentDescription = null,
                    tint = TeacherNeon,
                    modifier = Modifier.size(12.dp),
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// REDESIGNED JADWAL & MONITORING TUGAS ITEM CARDS
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PrincipalSessionItemCard(
    session: LearningSession,
    onClick: () -> Unit,
) {
    val isDark = LocalIsDarkTheme.current
    val cardBg = if (isDark) Color(0xFF111113) else Color.White
    val cardBorder = if (isDark) Color(0xFF1F1F23) else Color(0xFFE2E8F0)
    val textPrimary = if (isDark) Color.White else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    val textTertiary = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)

    val isLive = session.status.equals("active", ignoreCase = true)
    val sessionClass = formatClassName(session.className ?: session.room)
    val startHour = formatHourMinute(session.startTime)
        ?: formatHourMinute(session.scheduledAt)
        ?: formatHourMinute(session.startedAt)
        ?: "07.30"
    val endHour = formatHourMinute(session.endTime)
        ?: formatHourMinute(session.endedAt)
        ?: "09.00"

    val subjectName = session.subjectName?.ifBlank { "Modul Pembelajaran" } ?: "Modul Pembelajaran"
    val iconBg = if (isLive) {
        if (isDark) Color(0xFF0A2B1D) else Color(0xFFDCFCE7)
    } else {
        if (isDark) Color(0xFF0C243B) else Color(0xFFE0F2FE)
    }
    val iconTint = if (isLive) {
        if (isDark) Color(0xFF34D399) else Color(0xFF16A34A)
    } else {
        if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(cardBg)
            .border(
                1.dp,
                if (isLive) iconTint.copy(alpha = 0.55f) else cardBorder,
                RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick)
            .padding(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Leading Icon Container
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconBg)
                    .border(0.5.dp, iconTint.copy(alpha = 0.35f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (isLive) Icons.Outlined.PlayCircleOutline else Icons.Outlined.School,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp),
                )
            }

            // Middle Information
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = subjectName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (isLive) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(iconTint.copy(alpha = 0.16f))
                                .border(0.5.dp, iconTint.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(iconTint)
                                )
                                Text(
                                    text = "LIVE",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    color = iconTint,
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))

                // Rombel & Tutor info row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isDark) Color(0xFF1E1B4B) else Color(0xFFEEF2FF))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = sessionClass,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDark) Color(0xFFA5B4FC) else Color(0xFF4F46E5),
                        )
                    }

                    Text(
                        text = session.teacherName ?: "Tutor Pengampu",
                        fontSize = 11.5.sp,
                        color = textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Spacer(Modifier.height(4.dp))

                // Time info
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Schedule,
                        contentDescription = null,
                        tint = textTertiary,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = "$startHour - $endHour WIB",
                        fontSize = 11.sp,
                        color = textTertiary,
                    )
                }
            }

            // Trailing Chevron Box
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(if (isDark) Color(0xFF1B1B1E) else Color(0xFFF1F5F9)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                    contentDescription = null,
                    tint = textSecondary,
                    modifier = Modifier.size(13.dp),
                )
            }
        }
    }
}

@Composable
private fun PrincipalAssignmentItemCard(
    assignment: Assignment,
    onClick: () -> Unit,
) {
    val isDark = LocalIsDarkTheme.current
    val cardBg = if (isDark) Color(0xFF111113) else Color.White
    val cardBorder = if (isDark) Color(0xFF1F1F23) else Color(0xFFE2E8F0)
    val textPrimary = if (isDark) Color.White else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    val textTertiary = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)

    val iconBg = if (isDark) Color(0xFF0C243B) else Color(0xFFE0F2FE)
    val iconTint = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)

    val subjectLabel = assignment.subjectName?.ifBlank { "Modul" } ?: "Modul"
    val classLabel = formatClassName(assignment.className, "Semua Rombel")

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(cardBg)
            .border(1.dp, cardBorder, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Leading Icon Container
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconBg)
                    .border(0.5.dp, iconTint.copy(alpha = 0.35f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.Assignment,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp),
                )
            }

            // Middle Column
            Column(modifier = Modifier.weight(1f)) {
                // Subject & Class Chips
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isDark) Color(0xFF0A2B1D) else Color(0xFFDCFCE7))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = subjectLabel,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDark) Color(0xFF34D399) else Color(0xFF16A34A),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isDark) Color(0xFF261447) else Color(0xFFF3E8FF))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = classLabel,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDark) Color(0xFFA78BFA) else Color(0xFF9333EA),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                Spacer(Modifier.height(5.dp))

                Text(
                    text = assignment.title.ifBlank { "Tugas Warga Belajar" },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                Spacer(Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = null,
                        tint = textTertiary,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = "Tugas Aktif Tutor • Penugasan Mandiri",
                        fontSize = 11.sp,
                        color = textTertiary,
                    )
                }
            }

            // Trailing Chevron Box
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(if (isDark) Color(0xFF1B1B1E) else Color(0xFFF1F5F9)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                    contentDescription = null,
                    tint = textSecondary,
                    modifier = Modifier.size(13.dp),
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 6. REAL-TIME WEEKLY LINE CHART: KEAKTIFAN GURU (7 HARI)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PrincipalTeacherWeeklyActivityCard(
    metrics: SchoolHealthMetrics? = null,
    isPkbm: Boolean = false,
    sessionsCount: Int = 0,
) {
    val isDark = LocalIsDarkTheme.current
    val cardBg = if (isDark) Color(0xFF111113) else Color.White
    val cardBorder = if (isDark) Color(0xFF1F1F23) else Color(0xFFE2E8F0)
    val textPrimary = if (isDark) Color.White else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    val textTertiary = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)
    val innerCardBg = if (isDark) Color(0xFF161618) else Color(0xFFF8FAFC)
    val innerCardBorder = if (isDark) Color(0xFF27272A) else Color(0xFFE2E8F0)

    val emerald = if (isDark) Color(0xFF10B981) else Color(0xFF059669)

    // Data points & Days from real metrics
    val rawTrends = metrics?.teacherWeeklyTrend.orEmpty()
    val fallbackDays = listOf("Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min")
    val days = if (rawTrends.isNotEmpty()) rawTrends.map { it.day } else fallbackDays
    val dataPoints = if (rawTrends.isNotEmpty()) {
        rawTrends.map { it.value }
    } else {
        val baseVal = (metrics?.complianceRate?.toFloat() ?: 0f).coerceIn(0f, 100f)
        if (baseVal > 0f) {
            List(7) { baseVal }
        } else {
            List(7) { 0f }
        }
    }

    val maxVal = dataPoints.maxOrNull() ?: 0f
    val peakIndex = if (maxVal > 0f) dataPoints.indexOfFirst { it == maxVal } else -1

    val compliancePct = metrics?.complianceRate ?: 0.0
    val avgKeaktifanStr = if (compliancePct > 0.0) {
        String.format(java.util.Locale.US, "%.1f%%", compliancePct)
    } else {
        "0%"
    }

    val onTimeStr = if (metrics != null && metrics.totalScheduledSessions > 0) {
        val pct = (metrics.completedSessionsCount * 100 / metrics.totalScheduledSessions).coerceIn(0, 100)
        "$pct%"
    } else if (metrics != null && metrics.overdueSessionsCount == 0 && metrics.totalActiveTeachers > 0) {
        "100%"
    } else if (sessionsCount > 0) {
        "100%"
    } else {
        "0%"
    }

    val teacherHadirStr = if (metrics != null && metrics.totalActiveTeachers > 0) {
        "${metrics.totalActiveTeachers} Aktif"
    } else if (sessionsCount > 0) {
        "$sessionsCount Sesi"
    } else {
        "0 Hadir"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(cardBg)
            .border(1.dp, cardBorder, RoundedCornerShape(22.dp))
            .padding(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(emerald.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ShowChart,
                            contentDescription = null,
                            tint = emerald,
                            modifier = Modifier.size(20.dp),
                        )
                    }

                    Column {
                        Text(
                            text = if (isPkbm) "Tren Keaktifan Tutor (7 Hari)" else "Tren Keaktifan Guru (7 Hari)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary,
                        )
                        Spacer(Modifier.height(1.dp))
                        Text(
                            text = if (isPkbm) {
                                "Monitoring presensi, unggah modul & review tugas"
                            } else {
                                "Monitoring presensi, unggah materi & review tugas"
                            },
                            fontSize = 11.sp,
                            color = textTertiary,
                        )
                    }
                }
            }

            // Stat Summary Strip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PrincipalMicroMetric(
                    modifier = Modifier.weight(1f),
                    label = "Rerata Keaktifan",
                    value = avgKeaktifanStr,
                    color = emerald,
                    innerCardBg = innerCardBg,
                    innerCardBorder = innerCardBorder,
                    textSecondary = textSecondary,
                )
                PrincipalMicroMetric(
                    modifier = Modifier.weight(1f),
                    label = "Sesi Tepat Waktu",
                    value = onTimeStr,
                    color = Color(0xFF38BDF8),
                    innerCardBg = innerCardBg,
                    innerCardBorder = innerCardBorder,
                    textSecondary = textSecondary,
                )
                PrincipalMicroMetric(
                    modifier = Modifier.weight(1f),
                    label = if (isPkbm) "Tutor Hadir" else "Guru Hadir",
                    value = teacherHadirStr,
                    color = Color(0xFFA78BFA),
                    innerCardBg = innerCardBg,
                    innerCardBorder = innerCardBorder,
                    textSecondary = textSecondary,
                )
            }

            Spacer(Modifier.height(4.dp))

            // Smooth Line Chart Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(125.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(innerCardBg)
                    .border(0.5.dp, innerCardBorder, RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val n = dataPoints.size
                    val stepX = if (n > 1) w / (n - 1) else w
                    val minY = 0f
                    val maxY = 100f
                    val range = maxY - minY

                    // Guidelines
                    val lineCount = 3
                    for (i in 0..lineCount) {
                        val y = h * (i.toFloat() / lineCount)
                        drawLine(
                            color = innerCardBorder,
                            start = androidx.compose.ui.geometry.Offset(0f, y),
                            end = androidx.compose.ui.geometry.Offset(w, y),
                            strokeWidth = 1f,
                        )
                    }

                    // Compute points
                    val points = dataPoints.mapIndexed { idx, value ->
                        val x = idx * stepX
                        val normalized = ((value - minY) / range).coerceIn(0f, 1f)
                        val y = h - (normalized * (h * 0.75f)) - (h * 0.12f)
                        androidx.compose.ui.geometry.Offset(x, y)
                    }

                    // Build smooth cubic bezier path
                    val path = Path()
                    val fillPath = Path()

                    points.forEachIndexed { i, pt ->
                        if (i == 0) {
                            path.moveTo(pt.x, pt.y)
                            fillPath.moveTo(pt.x, h)
                            fillPath.lineTo(pt.x, pt.y)
                        } else {
                            val prev = points[i - 1]
                            val cx = (prev.x + pt.x) / 2f
                            path.cubicTo(cx, prev.y, cx, pt.y, pt.x, pt.y)
                            fillPath.cubicTo(cx, prev.y, cx, pt.y, pt.x, pt.y)
                        }
                    }

                    fillPath.lineTo(points.last().x, h)
                    fillPath.close()

                    // Draw gradient area
                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            listOf(emerald.copy(alpha = 0.35f), emerald.copy(alpha = 0.02f))
                        )
                    )

                    // Draw smooth curve stroke
                    drawPath(
                        path = path,
                        color = emerald,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Draw node circles
                    points.forEachIndexed { idx, pt ->
                        val isPeak = idx == peakIndex
                        drawCircle(
                            color = if (isPeak) emerald.copy(alpha = 0.35f) else emerald.copy(alpha = 0.15f),
                            radius = if (isPeak) 7.dp.toPx() else 4.dp.toPx(),
                            center = pt
                        )
                        drawCircle(
                            color = emerald,
                            radius = if (isPeak) 4.5.dp.toPx() else 3.dp.toPx(),
                            center = pt
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 2.dp.toPx(),
                            center = pt
                        )
                    }
                }
            }

            // Days Label Row
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                days.forEachIndexed { idx, day ->
                    val isPeak = idx == peakIndex
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = day,
                            fontSize = 11.sp,
                            fontWeight = if (isPeak) FontWeight.ExtraBold else FontWeight.Medium,
                            color = if (isPeak) emerald else textTertiary,
                        )
                        if (isPeak && dataPoints.getOrNull(idx) != null && dataPoints[idx] > 0f) {
                            Text(
                                text = "${dataPoints[idx].toInt()}%",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = emerald,
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 7. REAL-TIME WEEKLY ROUNDED BAR CHART: KEAKTIFAN WARGA BELAJAR (7 HARI)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PrincipalStudentWeeklyActivityCard(
    metrics: SchoolHealthMetrics? = null,
    assignmentsCount: Int = 0,
    totalStudents: String = "0",
    isPkbm: Boolean = false,
) {
    val isDark = LocalIsDarkTheme.current
    val cardBg = if (isDark) Color(0xFF111113) else Color.White
    val cardBorder = if (isDark) Color(0xFF1F1F23) else Color(0xFFE2E8F0)
    val textPrimary = if (isDark) Color.White else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    val textTertiary = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)
    val innerCardBg = if (isDark) Color(0xFF161618) else Color(0xFFF8FAFC)
    val innerCardBorder = if (isDark) Color(0xFF27272A) else Color(0xFFE2E8F0)

    val skyBlue = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)

    val attendancePct = metrics?.attendanceRate ?: 0.0
    val avgAttendanceStr = if (attendancePct > 0.0) {
        String.format(java.util.Locale.US, "%.1f%%", attendancePct)
    } else {
        "0%"
    }

    val cbtPartStr = if (metrics != null && metrics.academicMasteryRate > 0.0) {
        String.format(java.util.Locale.US, "%.1f%%", metrics.academicMasteryRate)
    } else {
        "0%"
    }

    val rawTrends = metrics?.studentWeeklyTrend.orEmpty()
    val fallbackDays = listOf("Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min")
    val days = if (rawTrends.isNotEmpty()) rawTrends.map { it.day } else fallbackDays
    val dataPoints = if (rawTrends.isNotEmpty()) {
        rawTrends.map { it.value }
    } else {
        val baseVal = (attendancePct.toFloat()).coerceIn(0f, 100f)
        if (baseVal > 0f) {
            List(7) { baseVal }
        } else {
            List(7) { 0f }
        }
    }

    val maxVal = dataPoints.maxOrNull() ?: 0f
    val peakIndex = if (maxVal > 0f) dataPoints.indexOfFirst { it == maxVal } else -1

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(cardBg)
            .border(1.dp, cardBorder, RoundedCornerShape(22.dp))
            .padding(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(skyBlue.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.BarChart,
                            contentDescription = null,
                            tint = skyBlue,
                            modifier = Modifier.size(20.dp),
                        )
                    }

                    Column {
                        Text(
                            text = if (isPkbm) "Keaktifan Warga Belajar (7 Hari)" else "Keaktifan Siswa (7 Hari)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary,
                        )
                        Spacer(Modifier.height(1.dp))
                        Text(
                            text = if (isPkbm) {
                                "Kehadiran tutorial, submisi tugas & asesmen CBT"
                            } else {
                                "Kehadiran kelas, submisi tugas & asesmen CBT"
                            },
                            fontSize = 11.sp,
                            color = textTertiary,
                        )
                    }
                }
            }

            // Stat Summary Strip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PrincipalMicroMetric(
                    modifier = Modifier.weight(1f),
                    label = "Rerata Kehadiran",
                    value = avgAttendanceStr,
                    color = skyBlue,
                    innerCardBg = innerCardBg,
                    innerCardBorder = innerCardBorder,
                    textSecondary = textSecondary,
                )
                PrincipalMicroMetric(
                    modifier = Modifier.weight(1f),
                    label = "Tugas Aktif",
                    value = "$assignmentsCount Tugas",
                    color = Color(0xFF10B981),
                    innerCardBg = innerCardBg,
                    innerCardBorder = innerCardBorder,
                    textSecondary = textSecondary,
                )
                PrincipalMicroMetric(
                    modifier = Modifier.weight(1f),
                    label = "Partisipasi CBT",
                    value = cbtPartStr,
                    color = Color(0xFFA78BFA),
                    innerCardBg = innerCardBg,
                    innerCardBorder = innerCardBorder,
                    textSecondary = textSecondary,
                )
            }

            Spacer(Modifier.height(4.dp))

            // Rounded Bar Chart Container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(innerCardBg)
                    .border(0.5.dp, innerCardBorder, RoundedCornerShape(14.dp))
                    .padding(horizontal = 12.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    days.forEachIndexed { idx, day ->
                        val pct = dataPoints[idx]
                        val isPeak = idx == peakIndex

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            // Percentage Label
                            Text(
                                text = "${pct.toInt()}%",
                                fontSize = 10.sp,
                                fontWeight = if (isPeak) FontWeight.ExtraBold else FontWeight.SemiBold,
                                color = if (isPeak) skyBlue else textTertiary,
                            )

                            // Bar Track Container
                            Box(
                                modifier = Modifier
                                    .width(26.dp)
                                    .height(95.dp)
                                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 4.dp, bottomEnd = 4.dp))
                                    .background(if (isDark) Color(0xFF222226) else Color(0xFFE2E8F0)),
                                contentAlignment = Alignment.BottomCenter,
                            ) {
                                val fillHeight = (pct.coerceIn(0f, 100f) / 100f * 95f).dp.coerceAtLeast(4.dp)
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(fillHeight)
                                        .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 4.dp, bottomEnd = 4.dp))
                                    .background(
                                        Brush.verticalGradient(
                                            if (isPeak) listOf(Color(0xFF38BDF8), Color(0xFF0284C7))
                                            else listOf(Color(0xFF818CF8), Color(0xFF4F46E5))
                                        )
                                    )
                                )
                            }

                            // Day Label
                            Text(
                                text = day,
                                fontSize = 11.sp,
                                fontWeight = if (isPeak) FontWeight.Bold else FontWeight.Medium,
                                color = if (isPeak) skyBlue else textTertiary,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PrincipalMicroMetric(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    color: Color,
    innerCardBg: Color,
    innerCardBorder: Color,
    textSecondary: Color,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(innerCardBg)
            .border(0.5.dp, innerCardBorder, RoundedCornerShape(10.dp))
            .padding(horizontal = 8.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = value,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                color = color,
                maxLines = 1,
            )
            Spacer(Modifier.height(1.dp))
            Text(
                text = label,
                fontSize = 9.sp,
                color = textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

