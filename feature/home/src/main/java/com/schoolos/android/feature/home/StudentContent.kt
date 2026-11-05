package com.schoolos.android.feature.home

import com.schoolos.android.core.R as CoreR
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.schoolos.android.core.designsystem.*
import com.schoolos.android.domain.model.*
import java.text.SimpleDateFormat
import java.time.LocalTime
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle

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
    todaySessions: List<LearningSession> = emptyList(),
    gradeAverage: String = "-",
    gradeStatus: String = "Belum ada data nilai",
    gradeTrendPoints: List<Float> = emptyList(),
    topGradeSubjects: List<SubjectGradeSummary> = emptyList(),
    studentProgress: Progress? = null,
    progressPercentage: Float = 0f,
    activeReadingHistory: List<BookReadingItem> = emptyList(),
    recommendedSibiBook: LibraryBook? = null,
    onOpenBookReading: (BookReadingItem) -> Unit = {},
    onStartReadingSibi: (LibraryBook) -> Unit = {},
    xpCount: String = "0",
    assignmentsCount: String = "0",
    weeklyActivity: WeeklyActivity? = null,
    studentName: String = "",
    studentClass: String = "",
    studentNisn: String = "",
    streakDays: Int = 0,
    studentAssignments: List<Assignment> = emptyList(),
) {
    // ── 0. KARTU SAPAAN PERSONAL (GREETING HERO CARD) ────────────────────────
    item {
        StudentGreetingHeroCard(
            studentName = studentName,
            studentClass = studentClass,
            studentNisn = studentNisn,
            streakDays = streakDays,
        )
    }

    // ── HEADER AKSES UTAMA ────────────────────────────────────────────────────
    item {
        HomeAksesUtamaSectionHeader()
    }

    // ── 1. AKSES UTAMA (2x2 GRID CARD DESIGN MODERN) ──────────────────────────
    item {
        HomeAksesUtamaGrid(
            assignmentsCount = assignmentsCount,
            gradeAverage = gradeAverage,
            onNavigateToAssignments = onNavigateToAssignments,
            onNavigateToQuizzes = onNavigateToQuizzes,
            onNavigateToLearning = onNavigateToLearning,
            onNavigateToGrades = onNavigateToGrades,
        )
    }

    // ── 2. JADWAL PELAJARAN HARI INI ──────────────────────────────────────────
    item {
        HomeTodayScheduleCard(
            todaySessions = todaySessions,
            onNavigateToSessions = onNavigateToSessions,
            onNavigateToSessionDetail = onNavigateToSessionDetail,
        )
    }

    // ── 3. PERFORMA AKADEMIK & NILAI ──────────────────────────────────────────
    item {
        HomeAcademicPerformanceCard(
            gradeAverage = gradeAverage,
            gradeStatus = gradeStatus,
            gradeTrendPoints = gradeTrendPoints,
            topGradeSubjects = topGradeSubjects,
            onNavigateToGrades = onNavigateToGrades,
        )
    }

    // ── 4. BUKU & MODUL SIBI KEMDIKDASMEN / LANJUTKAN MEMBACA ─────────────────
    item {
        HomeSibiReadingCard(
            activeReadingHistory = activeReadingHistory,
            recommendedSibiBook = recommendedSibiBook,
            onOpenBookReading = onOpenBookReading,
            onStartReadingSibi = onStartReadingSibi,
            onNavigateToLearning = onNavigateToLearning,
        )
    }

    // ── 5. AKTIVITAS BELAJAR MINGGU INI ───────────────────────────────────────
    item {
        HomeWeeklyActivityCard(
            weeklyActivity = weeklyActivity,
            onNavigateToMaterials = onNavigateToLearning,
            onNavigateToAssignments = onNavigateToAssignments,
            onNavigateToQuizzes = onNavigateToQuizzes,
            onNavigateToProgress = onNavigateToProgress,
        )
    }
}

// ── 0. KARTU SAPAAN PERSONAL (GREETING HERO CARD) ────────────────────────────

@Composable
internal fun StudentGreetingHeroCard(
    studentName: String,
    studentClass: String,
    studentNisn: String,
    streakDays: Int,
    subtitle: String = "Siap melanjutkan pembelajaran hari ini? Cek tugas dan jadwal di bawah.",
    showStreak: Boolean = true,
) {
    val isDark = LocalIsDarkTheme.current
    val cardBg = if (isDark) Color(0xFF111113) else Color.White
    val cardBorder = if (isDark) Color(0xFF1F1F23) else Color(0xFFE2E8F0)
    val textPrimary = if (isDark) Color.White else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    // Dynamic greeting based on current local hour
    val hour = remember {
        try { LocalTime.now().hour } catch (_: Throwable) { 12 }
    }
    val greeting = when (hour) {
        in 4..10 -> "Selamat Pagi"
        in 11..14 -> "Selamat Siang"
        in 15..18 -> "Selamat Sore"
        else -> "Selamat Malam"
    }

    // Extract first name for highlighted display
    val rawName = studentName.trim().ifBlank { "Siswa" }
    val firstName = rawName.substringBefore(" ").uppercase()

    // Format Class and NISN with real user data
    val isUuidLike = studentClass.contains("-") && studentClass.length >= 32
    val effectiveClass = if (studentClass.isNotBlank() && !isUuidLike) studentClass.uppercase() else "WARGA BELAJAR"
    val effectiveNisn = if (studentNisn.isNotBlank()) "NISN: $studentNisn" else null
    val effectiveStreak = streakDays

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder),
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Top Pills: [KELAS] [NISN]
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val classBadgeBg = if (isDark) Color(0xFF0C243B) else Color(0xFFE0F2FE)
                val classBadgeBorder = if (isDark) Color(0xFF0284C7).copy(alpha = 0.45f) else Color(0xFFBAE6FD)
                val classBadgeText = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = classBadgeBg,
                    border = BorderStroke(0.5.dp, classBadgeBorder)
                ) {
                    Text(
                        text = effectiveClass,
                        color = classBadgeText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                if (effectiveNisn != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isDark) Color(0xFF27272A) else Color(0xFFF1F5F9)
                    ) {
                        Text(
                            text = effectiveNisn,
                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            // Greeting: Selamat Siang, [NAMA] 👋
            Text(
                text = buildAnnotatedString {
                    append("$greeting, ")
                    withStyle(SpanStyle(color = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7))) {
                        append(firstName)
                    }
                    append(" 👋")
                },
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary,
                letterSpacing = (-0.3).sp
            )

            // Subtitle
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = textSecondary,
                lineHeight = 16.sp
            )

            // Streak Pill: 🔥 Streak: 5 Hari
            val streakBadgeBg = if (isDark) Color(0xFF311C08) else Color(0xFFFFFBEB)
            val streakBadgeBorder = if (isDark) Color(0xFFD97706).copy(alpha = 0.45f) else Color(0xFFFDE68A)
            val streakBadgeText = if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706)

            if (showStreak) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = streakBadgeBg,
                    border = BorderStroke(1.dp, streakBadgeBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🔥 Streak: $effectiveStreak Hari",
                            color = streakBadgeText,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// ── HEADER AKSES UTAMA ────────────────────────────────────────────────────────

@Composable
internal fun HomeAksesUtamaSectionHeader() {
    val isDark = LocalIsDarkTheme.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "AKSES UTAMA",
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.5.sp,
            color = if (isDark) TextTertiary else Color(0xFF64748B)
        )
    }
}

// ── 1. AKSES UTAMA (2x2 GRID MODERN ROUNDED) ──────────────────────────────────

@Composable
private fun HomeAksesUtamaGrid(
    assignmentsCount: String,
    gradeAverage: String,
    onNavigateToAssignments: () -> Unit,
    onNavigateToQuizzes: () -> Unit,
    onNavigateToLearning: () -> Unit,
    onNavigateToGrades: () -> Unit,
) {
    val isDark = LocalIsDarkTheme.current
    val cardBg = if (isDark) Color(0xFF111113) else Color.White
    val cardBorder = if (isDark) Color(0xFF1F1F23) else Color(0xFFE2E8F0)
    val textPrimary = if (isDark) Color.White else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    val assBadge = if (assignmentsCount.isNotBlank() && assignmentsCount != "0") "$assignmentsCount Tugas" else "Aktif"
    val gradeBadge = if (gradeAverage.isNotBlank() && gradeAverage != "-") "Rata-rata $gradeAverage" else "Transkrip"

    // Theme-adaptive badge and icon backgrounds (dark translucent in dark mode, gentle pastel in light mode)
    val tugasBadgeBg = if (isDark) Color(0xFF0C243B) else Color(0xFFE0F2FE)
    val tugasColor = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)

    val kuisBadgeBg = if (isDark) Color(0xFF0A2B1D) else Color(0xFFDCFCE7)
    val kuisColor = if (isDark) Color(0xFF34D399) else Color(0xFF16A34A)

    val materiBadgeBg = if (isDark) Color(0xFF261447) else Color(0xFFF3E8FF)
    val materiColor = if (isDark) Color(0xFFA78BFA) else Color(0xFF9333EA)

    val raporBadgeBg = if (isDark) Color(0xFF31220A) else Color(0xFFFEF3C7)
    val raporColor = if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706)

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Row 1: Tugas Sekolah & Kuis Ujian
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            HomeMenuGridCard(
                modifier = Modifier.weight(1f),
                title = "Tugas Sekolah",
                subtitle = "Cek tenggat PR & lembar kerja mandiri",
                badgeText = assBadge,
                badgeBg = tugasBadgeBg,
                badgeColor = tugasColor,
                icon = Icons.AutoMirrored.Filled.Assignment,
                iconCircleBg = tugasBadgeBg,
                iconTint = tugasColor,
                actionText = "Buka Tugas",
                cardBg = cardBg,
                cardBorder = cardBorder,
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                onClick = onNavigateToAssignments,
            )

            HomeMenuGridCard(
                modifier = Modifier.weight(1f),
                title = "Kuis & Ujian",
                subtitle = "Simulasi CBT online, token & latihan",
                badgeText = "CBT Online",
                badgeBg = kuisBadgeBg,
                badgeColor = kuisColor,
                icon = Icons.Default.AccessTime,
                iconCircleBg = kuisBadgeBg,
                iconTint = kuisColor,
                actionText = "Masuk Ujian",
                cardBg = cardBg,
                cardBorder = cardBorder,
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                onClick = onNavigateToQuizzes,
            )
        }

        // Row 2: Materi Belajar & Rapor Nilai
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            HomeMenuGridCard(
                modifier = Modifier.weight(1f),
                title = "Materi Belajar",
                subtitle = "Buku teks resmi SIBI, modul & video",
                badgeText = "Modul SIBI",
                badgeBg = materiBadgeBg,
                badgeColor = materiColor,
                icon = Icons.AutoMirrored.Filled.MenuBook,
                iconCircleBg = materiBadgeBg,
                iconTint = materiColor,
                actionText = "Baca Materi",
                cardBg = cardBg,
                cardBorder = cardBorder,
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                onClick = onNavigateToLearning,
            )

            HomeMenuGridCard(
                modifier = Modifier.weight(1f),
                title = "Rapor & Nilai",
                subtitle = "Transkrip capaian kompetensi TP & nilai",
                badgeText = gradeBadge,
                badgeBg = raporBadgeBg,
                badgeColor = raporColor,
                icon = Icons.Default.EmojiEvents,
                iconCircleBg = raporBadgeBg,
                iconTint = raporColor,
                actionText = "Lihat Rapor",
                cardBg = cardBg,
                cardBorder = cardBorder,
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                onClick = onNavigateToGrades,
            )
        }
    }
}

@Composable
internal fun HomeMenuGridCard(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    badgeText: String,
    badgeBg: Color,
    badgeColor: Color,
    icon: ImageVector,
    iconCircleBg: Color,
    iconTint: Color,
    actionText: String,
    cardBg: Color,
    cardBorder: Color,
    textPrimary: Color,
    textSecondary: Color,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder),
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(iconCircleBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = badgeBg
                ) {
                    Text(
                        text = badgeText,
                        color = badgeColor,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
            }

            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = textSecondary,
                minLines = 2,
                maxLines = 2,
                lineHeight = 15.sp,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(2.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = actionText,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = iconTint
                )
                Spacer(Modifier.width(3.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(11.dp)
                )
            }
        }
    }
}

// ── 2. JADWAL PELAJARAN HARI INI ──────────────────────────────────────────────

@Composable
private fun HomeTodayScheduleCard(
    todaySessions: List<LearningSession>,
    onNavigateToSessions: () -> Unit,
    onNavigateToSessionDetail: (String) -> Unit,
) {
    val isDark = LocalIsDarkTheme.current
    val cardBg = if (isDark) Color(0xFF111113) else Color.White
    val cardBorder = if (isDark) Color(0xFF1F1F23) else Color(0xFFE2E8F0)
    val textPrimary = if (isDark) Color.White else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder),
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0284C7).copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = Color(0xFF0284C7),
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Jadwal Pelajaran Hari Ini",
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                        Text(
                            text = "${todaySessions.size} Sesi terjadwal hari ini",
                            fontSize = 11.sp,
                            color = textSecondary
                        )
                    }
                }

                Text(
                    text = "Lihat Semua ›",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF0284C7),
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(onClick = onNavigateToSessions)
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }

            if (todaySessions.isEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF94A3B8))
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "Tidak ada jadwal tatap muka atau kelas online hari ini.",
                        fontSize = 12.sp,
                        color = textSecondary
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    todaySessions.take(3).forEach { session ->
                        val title = session.subjectName ?: session.notes?.substringBefore(" • ") ?: "Mata Pelajaran"
                        val time = formatSessionTime(session.scheduledAt ?: session.startedAt)
                        val teacher = session.notes?.substringAfter(" • ")?.substringBefore(" (")?.trim().orEmpty()
                        val subText = if (teacher.isNotBlank()) "Kelas Online • $teacher" else "Kelas Pembelajaran"

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { onNavigateToSessionDetail(session.id) },
                            color = if (isDark) Color(0xFF161618) else Color(0xFFF8FAFC),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, if (isDark) Color(0xFF27272A) else cardBorder)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = time,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = textPrimary
                                        )
                                        Box(
                                            modifier = Modifier
                                                .padding(top = 3.dp)
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(if (session.status == "active") Color(0xFF10B981) else Color(0xFF94A3B8))
                                        )
                                    }

                                    Spacer(Modifier.width(14.dp))

                                    Column {
                                        Text(
                                            text = title,
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = textPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = subText,
                                            fontSize = 11.sp,
                                            color = textSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = textSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── 3. PERFORMA AKADEMIK & NILAI ──────────────────────────────────────────────

@Composable
private fun HomeAcademicPerformanceCard(
    gradeAverage: String,
    gradeStatus: String,
    gradeTrendPoints: List<Float>,
    topGradeSubjects: List<SubjectGradeSummary>,
    onNavigateToGrades: () -> Unit,
) {
    val isDark = LocalIsDarkTheme.current
    val cardBg = if (isDark) Color(0xFF111113) else Color.White
    val cardBorder = if (isDark) Color(0xFF1F1F23) else Color(0xFFE2E8F0)
    val textPrimary = if (isDark) Color.White else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    val avgNum = gradeAverage.toDoubleOrNull() ?: 0.0
    val hasGrade = gradeAverage != "-" && avgNum > 0.0

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onNavigateToGrades),
        shape = RoundedCornerShape(22.dp),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder),
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFD97706).copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Assessment,
                            contentDescription = null,
                            tint = Color(0xFFD97706),
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Performa Akademik & Nilai",
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                        Text(
                            text = "Indeks capaian belajar semester berjalan",
                            fontSize = 11.sp,
                            color = textSecondary
                        )
                    }
                }

                Text(
                    text = "Lihat Semua ›",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF0284C7)
                )
            }

            // Skor Rata-rata dan Progress Circular
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = if (hasGrade) String.format(Locale.US, "%.1f", avgNum) else gradeAverage,
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Black,
                            color = textPrimary,
                            letterSpacing = (-1).sp
                        )
                        if (hasGrade) {
                            Text(
                                text = " / 100",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = textSecondary,
                                modifier = Modifier.padding(bottom = 6.dp, start = 4.dp)
                            )
                        }
                    }
                    Text(
                        text = if (hasGrade) gradeStatus else "Belum ada nilai yang diinput guru",
                        fontSize = 11.5.sp,
                        color = textSecondary
                    )
                }

                Box(
                    modifier = Modifier.size(52.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val progress = if (hasGrade) (avgNum / 100.0).toFloat().coerceIn(0f, 1f) else 0f
                    CircularProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxSize(),
                        color = if (hasGrade) Color(0xFF10B981) else Color(0xFF27272A),
                        trackColor = if (isDark) Color(0xFF1F1F23) else Color(0xFFF1F5F9),
                        strokeWidth = 5.dp
                    )
                    Icon(
                        imageVector = if (hasGrade) Icons.Default.CheckCircle else Icons.Default.School,
                        contentDescription = null,
                        tint = if (hasGrade) Color(0xFF10B981) else textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Line Trend Chart (Jika ada data tren)
            if (gradeTrendPoints.size >= 2) {
                LineTrendChart(
                    dataPoints = gradeTrendPoints,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                )
            }

            // Top Subjects Preview
            if (topGradeSubjects.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    topGradeSubjects.take(2).forEach { subj ->
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            color = if (isDark) Color(0xFF161618) else Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, if (isDark) Color(0xFF27272A) else cardBorder)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = subj.subjectName,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Nilai: ${subj.finalScore.toInt()}",
                                    fontSize = 10.5.sp,
                                    color = Color(0xFF10B981),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── 4. BUKU & MODUL SIBI KEMDIKDASMEN / LANJUTKAN MEMBACA ───────────────────────

@Composable
private fun HomeSibiReadingCard(
    activeReadingHistory: List<BookReadingItem>,
    recommendedSibiBook: LibraryBook?,
    onOpenBookReading: (BookReadingItem) -> Unit,
    onStartReadingSibi: (LibraryBook) -> Unit,
    onNavigateToLearning: () -> Unit,
) {
    val isDark = LocalIsDarkTheme.current
    val cardBg = if (isDark) Color(0xFF111113) else Color.White
    val cardBorder = if (isDark) Color(0xFF1F1F23) else Color(0xFFE2E8F0)
    val textPrimary = if (isDark) Color.White else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder),
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF9333EA).copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = null,
                            tint = Color(0xFF9333EA),
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            text = if (activeReadingHistory.isNotEmpty()) "Lanjutkan Membaca" else "Rekomendasi Buku SIBI",
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                        Text(
                            text = "Buku teks resmi Kemendikdasmen RI",
                            fontSize = 11.sp,
                            color = textSecondary
                        )
                    }
                }

                Text(
                    text = "Lihat Semua ›",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF9333EA),
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(onClick = onNavigateToLearning)
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }

            if (activeReadingHistory.isNotEmpty()) {
                val currentBook = activeReadingHistory.first()
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onOpenBookReading(currentBook) },
                    color = if (isDark) Color(0xFF161618) else Color(0xFFF8FAFC),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, if (isDark) Color(0xFF27272A) else cardBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF9333EA).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Book,
                                contentDescription = null,
                                tint = Color(0xFF9333EA),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = currentBook.title,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            val percent = (currentBook.progressPercentage * 100).toInt()
                            Text(
                                text = "Halaman ${currentBook.currentPage} dari ${currentBook.totalPages} • Capaian $percent%",
                                fontSize = 11.sp,
                                color = textSecondary
                            )
                        }

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = Color(0xFF9333EA),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            } else if (recommendedSibiBook != null) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onStartReadingSibi(recommendedSibiBook) },
                    color = if (isDark) Color(0xFF161618) else Color(0xFFF8FAFC),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, if (isDark) Color(0xFF27272A) else cardBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF9333EA).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Book,
                                contentDescription = null,
                                tint = Color(0xFF9333EA),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = recommendedSibiBook.title,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${recommendedSibiBook.subjectName ?: "Kurikulum Merdeka"} • ${recommendedSibiBook.totalPages} Halaman",
                                fontSize = 11.sp,
                                color = textSecondary
                            )
                        }

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = Color(0xFF9333EA),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            } else {
                Text(
                    text = "Buku teks digital resmi Kemendikdasmen siap dibaca di perpustakaan digital.",
                    fontSize = 12.sp,
                    color = textSecondary,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}

// ── 5. AKTIVITAS BELAJAR MINGGU INI ───────────────────────────────────────────

@Composable
private fun HomeWeeklyActivityCard(
    weeklyActivity: WeeklyActivity?,
    onNavigateToMaterials: () -> Unit,
    onNavigateToAssignments: () -> Unit,
    onNavigateToQuizzes: () -> Unit,
    onNavigateToProgress: () -> Unit,
) {
    val isDark = LocalIsDarkTheme.current
    val cardBg = if (isDark) Color(0xFF111113) else Color.White
    val cardBorder = if (isDark) Color(0xFF1F1F23) else Color(0xFFE2E8F0)
    val textPrimary = if (isDark) Color.White else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    val days = weeklyActivity?.days ?: emptyList()
    val todayIdx = days.indexOfFirst { it.isToday }.coerceAtLeast(0)
    var selectedDayIndex by remember(days) { mutableIntStateOf(todayIdx) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder),
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981).copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timeline,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Aktivitas Belajar Minggu Ini",
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                        Text(
                            text = "Senin s/d Sabtu • Real-time progres harian",
                            fontSize = 11.sp,
                            color = textSecondary
                        )
                    }
                }

                Text(
                    text = "Lihat Semua ›",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF0284C7),
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(onClick = onNavigateToProgress)
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }

            if (days.isNotEmpty()) {
                // Baris Pemilih Hari (Senin s/d Sabtu)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    days.take(6).forEachIndexed { index, day ->
                        val isSelected = index == selectedDayIndex
                        val dayShort = when (day.dayName.lowercase()) {
                            "senin" -> "Sen"
                            "selasa" -> "Sel"
                            "rabu" -> "Rab"
                            "kamis" -> "Kam"
                            "jumat" -> "Jum"
                            "sabtu" -> "Sab"
                            else -> day.dayName.take(3)
                        }

                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { selectedDayIndex = index },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) Color(0xFF2563EB) else if (isDark) Color(0xFF161618) else Color(0xFFF1F5F9),
                            border = if (isSelected) null else BorderStroke(1.dp, if (isDark) Color(0xFF27272A) else cardBorder)
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = dayShort,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else textSecondary
                                )
                                Text(
                                    text = "${day.totalCount}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else if (day.totalCount > 0) Color(0xFF10B981) else textSecondary
                                )
                            }
                        }
                    }
                }

                // Konten Aktivitas Hari Terpilih
                val currentDay = days.getOrNull(selectedDayIndex) ?: days.first()
                if (currentDay.activities.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        currentDay.activities.forEach { act ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        when (act.category) {
                                            "MATERIAL" -> onNavigateToMaterials()
                                            "ASSIGNMENT" -> onNavigateToAssignments()
                                            "QUIZ" -> onNavigateToQuizzes()
                                        }
                                    },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isDark) Color(0xFF161618) else Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, if (isDark) Color(0xFF27272A) else cardBorder)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    val (catColor, catIcon) = when (act.category) {
                                        "MATERIAL" -> Pair(Color(0xFF9333EA), Icons.Default.Book)
                                        "ASSIGNMENT" -> Pair(Color(0xFF0284C7), Icons.AutoMirrored.Filled.Assignment)
                                        "QUIZ" -> Pair(Color(0xFF16A34A), Icons.Default.AccessTime)
                                        else -> Pair(Color(0xFFD97706), Icons.Default.Assessment)
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(30.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(catColor.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = catIcon,
                                            contentDescription = null,
                                            tint = catColor,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = act.title,
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = textPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${act.subjectName ?: "Pembelajaran"} • ${act.categoryLabel}",
                                            fontSize = 10.5.sp,
                                            color = textSecondary
                                        )
                                    }

                                    if (act.time.isNotBlank()) {
                                        Text(
                                            text = "${act.time} WIB",
                                            fontSize = 10.5.sp,
                                            color = textSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF94A3B8))
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Belum ada aktivitas belajar pada hari ${currentDay.dayName}.",
                            fontSize = 12.sp,
                            color = textSecondary
                        )
                    }
                }
            } else {
                Text(
                    text = "Aktivitas belajar mingguan Anda akan tercatat secara otomatis di sini.",
                    fontSize = 12.sp,
                    color = textSecondary,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}

// ── BANTUAN WAKTU ─────────────────────────────────────────────────────────────

private fun formatSessionTime(rawTime: String?): String {
    if (rawTime.isNullOrBlank()) return "08.00"
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val out = SimpleDateFormat("HH.mm", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("Asia/Jakarta")
        }
        out.format(parser.parse(rawTime.substringBefore(".")) ?: Date())
    } catch (_: Exception) {
        "08.00"
    }
}
