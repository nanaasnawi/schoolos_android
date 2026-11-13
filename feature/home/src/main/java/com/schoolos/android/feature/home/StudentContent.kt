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
    onNavigateToAssignmentDetail: (String) -> Unit = {},
    onNavigateToQuizzes: () -> Unit,
    onNavigateToQuizDetail: (String) -> Unit = {},
    onNavigateToGrades: () -> Unit,
    onNavigateToProgress: () -> Unit,
    onNavigateToAchievements: () -> Unit,
    onNavigateToLearning: () -> Unit,
    onNavigateToDailyQuest: () -> Unit = {},
    onNavigateToMaterialDetail: (String) -> Unit = {},
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
    dailyQuestSummary: DailyQuest? = null,
    assignmentsCount: String = "0",
    materialsCount: String = "0",
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

    // ── 0.5. MISI HARIAN DINAMIS (GAME QUEST ARENA BANNER) ────────────────────
    item {
        HomeDailyQuestGameCard(
            dailyQuest = dailyQuestSummary,
            onClick = onNavigateToDailyQuest,
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
            materialsCount = materialsCount,
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

    // ── 6. WIDGET SHORTCUT CAPAIAN & STATUS BELAJAR (Di Bawah Aktivitas Belajar Minggu Ini) ──
    item {
        HomeStudentStudyTrackerWidget(
            weeklyActivity = weeklyActivity,
            studentAssignments = studentAssignments,
            activeReadingHistory = activeReadingHistory,
            onNavigateToMaterials = onNavigateToLearning,
            onNavigateToAssignments = onNavigateToAssignments,
            onNavigateToQuizzes = onNavigateToQuizzes,
            onNavigateToProgress = onNavigateToProgress,
            onNavigateToAssignmentDetail = onNavigateToAssignmentDetail,
            onNavigateToMaterialDetail = onNavigateToMaterialDetail,
            onNavigateToQuizDetail = onNavigateToQuizDetail,
            onOpenBookReading = onOpenBookReading,
        )
    }
}

// ── 0.5. MISI HARIAN DINAMIS (GAME QUEST ARENA BANNER) ───────────────────────

@Composable
internal fun HomeDailyQuestGameCard(
    dailyQuest: DailyQuest?,
    onClick: () -> Unit,
) {
    val isDark = LocalIsDarkTheme.current
    val lipColor = if (isDark) Color(0xFF0369A1) else Color(0xFF0284C7)
    val cardBg = if (isDark) Color(0xFF0E1729) else Color(0xFFF0F9FF)
    val cardBorder = if (isDark) Color(0xFF0EA5E9).copy(alpha = 0.55f) else Color(0xFF7DD3FC)
    val textPrimary = if (isDark) Color.White else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)

    val completed = dailyQuest?.alreadyCompleted == true
    val xpReward = dailyQuest?.xpReward ?: 80

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(22.dp),
        color = lipColor.copy(alpha = 0.7f),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 3.dp),
            shape = RoundedCornerShape(22.dp),
            color = cardBg,
            border = BorderStroke(1.5.dp, cardBorder),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF0369A1),
                ) {
                    Box(
                        modifier = Modifier
                            .padding(bottom = 3.dp)
                            .size(46.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF0EA5E9)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.SportsEsports,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFF0EA5E9)) {
                            Text(
                                text = "GAME HARIAN",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFF59E0B).copy(alpha = 0.18f),
                            border = BorderStroke(0.5.dp, Color(0xFFF59E0B).copy(alpha = 0.45f)),
                        ) {
                            Text(
                                text = "+$xpReward XP",
                                color = Color(0xFFF59E0B),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                        if (completed) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF10B981).copy(alpha = 0.18f),
                            ) {
                                Text(
                                    text = "✓ TUNTAS",
                                    color = Color(0xFF10B981),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                )
                            }
                        }
                    }

                    Text(
                        text = dailyQuest?.title ?: "Tantangan Belajar Seru Hari Ini (3 Menit)",
                        color = textPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = dailyQuest?.subjectName ?: "Fakta/Mitos • Detektif Kasus • Rangkai Logika",
                        color = textSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF0369A1),
                ) {
                    Surface(
                        modifier = Modifier.padding(bottom = 2.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF0EA5E9),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                        ) {
                            Text(
                                text = if (completed) "LATIHAN" else "MAIN",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(15.dp),
                            )
                        }
                    }
                }
            }
        }
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
    val effectiveNisn = if (studentNisn.isNotBlank()) {
        if (studentNisn.startsWith("NISN:", ignoreCase = true) || studentNisn.contains(" ") || studentNisn.any { it.isLetter() }) {
            studentNisn
        } else {
            "NISN: $studentNisn"
        }
    } else null
    val effectiveStreak = streakDays

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder),
        shadowElevation = 0.dp
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

            // Greeting: Selamat Siang, [NAMA] 👋 & Subtitle
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    text = buildAnnotatedString {
                        append("$greeting, ")
                        withStyle(SpanStyle(color = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7))) {
                            append(firstName)
                        }
                        append(" 👋")
                    },
                    fontSize = 15.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary,
                    letterSpacing = (-0.2).sp
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = textSecondary,
                    lineHeight = 14.sp
                )
            }

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
    materialsCount: String = "0",
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
    val materiBadge = if (materialsCount.isNotBlank() && materialsCount != "0") "$materialsCount Modul" else "Modul"
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
                badgeText = materiBadge,
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
        shadowElevation = 0.dp
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

// ── 2. JADWAL PELAJARAN HARI INI (UNIFIED ACROSS STUDENT, TEACHER & PRINCIPAL) ──

@Composable
internal fun HomeTodayScheduleCard(
    todaySessions: List<LearningSession>,
    onNavigateToSessions: () -> Unit,
    onNavigateToSessionDetail: (String) -> Unit,
    cardTitle: String = "Jadwal Pelajaran Hari Ini",
    subtitleBuilder: ((LearningSession) -> String)? = null,
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
        shadowElevation = 0.dp
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
                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text(
                            text = cardTitle,
                            fontSize = 14.5.sp,
                            lineHeight = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                        Text(
                            text = "${todaySessions.size} Sesi terjadwal hari ini",
                            fontSize = 11.sp,
                            lineHeight = 14.sp,
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
                        val title = session.subjectName?.takeIf { it.isNotBlank() }
                            ?: session.notes?.substringBefore(" • ")?.takeIf { it.isNotBlank() }
                            ?: "Mata Pelajaran"
                        val time = formatSessionTime(session.scheduledAt ?: session.startedAt)
                        val subText = if (subtitleBuilder != null) {
                            subtitleBuilder(session)
                        } else {
                            val teacher = session.notes?.substringAfter(" • ")?.substringBefore(" (")?.trim().orEmpty()
                            if (teacher.isNotBlank()) "Kelas Online • $teacher" else "Kelas Pembelajaran"
                        }

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
                                            lineHeight = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = textPrimary
                                        )
                                        Box(
                                            modifier = Modifier
                                                .padding(top = 3.dp)
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(if (session.status.equals("active", ignoreCase = true)) Color(0xFF10B981) else Color(0xFF94A3B8))
                                        )
                                    }

                                    Spacer(Modifier.width(14.dp))

                                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                        Text(
                                            text = title,
                                            fontSize = 13.5.sp,
                                            lineHeight = 16.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = textPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = subText,
                                            fontSize = 11.sp,
                                            lineHeight = 14.sp,
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
        shadowElevation = 0.dp
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
                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text(
                            text = "Performa Akademik & Nilai",
                            fontSize = 14.5.sp,
                            lineHeight = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                        Text(
                            text = "Indeks capaian belajar semester berjalan",
                            fontSize = 11.sp,
                            lineHeight = 14.sp,
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
        shadowElevation = 0.dp
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
                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text(
                            text = if (activeReadingHistory.isNotEmpty()) "Lanjutkan Membaca" else "Rekomendasi Buku SIBI",
                            fontSize = 14.5.sp,
                            lineHeight = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                        Text(
                            text = "Buku teks resmi Kemendikdasmen RI",
                            fontSize = 11.sp,
                            lineHeight = 14.sp,
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
        shadowElevation = 0.dp
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
                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text(
                            text = "Aktivitas Belajar Minggu Ini",
                            fontSize = 14.5.sp,
                            lineHeight = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                        Text(
                            text = "Senin s/d Sabtu • Real-time progres harian",
                            fontSize = 11.sp,
                            lineHeight = 14.sp,
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

// ── 6. WIDGET SHORTCUT CAPAIAN & STATUS BELAJAR ───────────────────────────────

private data class AndroidTrackerItem(
    val id: String,
    val title: String,
    val subjectName: String,
    val category: String, // "MATERIAL", "ASSIGNMENT", "QUIZ", "SESSION"
    val subType: String = "",
    val statusBadge: String,
    val badgeColor: Color,
    val progressPercent: Int? = null,
    val timestampLabel: String,
    val reasonNote: String? = null,
    val actionLabel: String,
    val rawBook: BookReadingItem? = null,
)

@Composable
internal fun HomeStudentStudyTrackerWidget(
    weeklyActivity: WeeklyActivity?,
    studentAssignments: List<Assignment>,
    activeReadingHistory: List<BookReadingItem>,
    onNavigateToMaterials: () -> Unit,
    onNavigateToAssignments: () -> Unit,
    onNavigateToQuizzes: () -> Unit,
    onNavigateToProgress: () -> Unit,
    onNavigateToAssignmentDetail: ((String) -> Unit)? = null,
    onNavigateToMaterialDetail: ((String) -> Unit)? = null,
    onNavigateToQuizDetail: ((String) -> Unit)? = null,
    onOpenBookReading: ((BookReadingItem) -> Unit)? = null,
) {
    val isDark = LocalIsDarkTheme.current
    val cardBg = if (isDark) Color(0xFF111113) else Color.White
    val cardBorder = if (isDark) Color(0xFF1F1F23) else Color(0xFFE2E8F0)
    val textPrimary = if (isDark) Color.White else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    var activeTab by remember { mutableStateOf("missed") } // "missed", "in_progress", "history"

    val todayIso = remember {
        try {
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        } catch (_: Exception) { "" }
    }

    // 1. Riwayat Belajar (Selesai dipelajari / dikerjakan)
    val historyItems = remember(weeklyActivity, studentAssignments, activeReadingHistory) {
        val list = mutableListOf<AndroidTrackerItem>()

        weeklyActivity?.days?.forEach { day ->
            day.activities.forEach { act ->
                val badge = when (act.category) {
                    "MATERIAL" -> "Tuntas 100%"
                    "ASSIGNMENT" -> "Sudah Dikumpulkan"
                    "QUIZ" -> "Kuis Selesai"
                    else -> "Selesai"
                }
                list.add(
                    AndroidTrackerItem(
                        id = act.id,
                        title = act.title,
                        subjectName = act.subjectName ?: "Mata Pelajaran",
                        category = act.category,
                        subType = act.subType,
                        statusBadge = badge,
                        badgeColor = Color(0xFF10B981),
                        progressPercent = 100,
                        timestampLabel = "${day.dayName} • ${act.time}",
                        reasonNote = "Aktivitas belajar ini telah kamu selesaikan.",
                        actionLabel = if (act.category == "MATERIAL") "Baca Lagi" else "Lihat Detail"
                    )
                )
            }
        }

        studentAssignments.filter { it.status.equals("submitted", true) || it.status.equals("graded", true) }.forEach { a ->
            if (list.none { it.id == a.id }) {
                list.add(
                    AndroidTrackerItem(
                        id = a.id,
                        title = a.title,
                        subjectName = a.subjectName ?: "Mata Pelajaran",
                        category = "ASSIGNMENT",
                        subType = a.assignmentType,
                        statusBadge = if (a.status.equals("graded", true)) "Dinilai Guru" else "Sudah Dikumpulkan",
                        badgeColor = Color(0xFF10B981),
                        progressPercent = 100,
                        timestampLabel = a.updatedAt.take(10),
                        reasonNote = "Lembar tugas sudah diserahkan kepada guru pengampu.",
                        actionLabel = "Buka Tugas"
                    )
                )
            }
        }

        list
    }

    // 2. Terlewat / Belum Dikerjakan (Semua tugas yang belum tuntas, baik yang lalu maupun yang sekarang)
    val missedItems = remember(studentAssignments, todayIso) {
        val list = mutableListOf<AndroidTrackerItem>()

        studentAssignments.filter { a ->
            val notCompleted = !a.status.equals("submitted", true) && !a.status.equals("graded", true) && !a.status.equals("returned", true)
            notCompleted
        }.forEach { a ->
            val dueIso = a.dueAt?.take(10) ?: ""
            val isPastDue = dueIso.isNotBlank() && dueIso < todayIso
            list.add(
                AndroidTrackerItem(
                    id = a.id,
                    title = a.title,
                    subjectName = a.subjectName ?: "Mata Pelajaran",
                    category = "ASSIGNMENT",
                    subType = a.assignmentType,
                    statusBadge = if (isPastDue) "Lewat Tenggat" else "Belum Dikerjakan",
                    badgeColor = if (isPastDue) Color(0xFFEF4444) else Color(0xFFF59E0B),
                    timestampLabel = if (dueIso.isNotBlank()) "Tenggat: $dueIso" else "Aktif",
                    reasonNote = if (isPastDue) "Tenggat waktu pengumpulan telah lewat. Segera kerjakan dan serahkan kepada guru." else "Tugas belum kamu kumpulkan. Klik untuk melihat detail dan petunjuk tugas.",
                    actionLabel = "Kirim Tugas"
                )
            )
        }

        list.sortBy { item -> if (item.statusBadge == "Lewat Tenggat") 0 else 1 }
        list
    }

    // 3. Belum Selesai (Materi dibaca sebagian & revisi guru)
    val inProgressItems = remember(activeReadingHistory, studentAssignments) {
        val list = mutableListOf<AndroidTrackerItem>()

        activeReadingHistory.filter { !it.isCompleted && it.currentPage > 1 }.forEach { b ->
            val pct = if (b.totalPages > 0) ((b.currentPage.toFloat() / b.totalPages.toFloat()) * 100f).toInt().coerceIn(10, 95) else 45
            list.add(
                AndroidTrackerItem(
                    id = b.materialId ?: b.id,
                    title = b.title,
                    subjectName = b.subjectName ?: "Materi Bacaan",
                    category = "MATERIAL",
                    statusBadge = "Dibaca $pct%",
                    badgeColor = Color(0xFF0284C7),
                    progressPercent = pct,
                    timestampLabel = "Halaman ${b.currentPage} dari ${b.totalPages}",
                    reasonNote = "Materi baru kamu baca sebagian. Klik untuk melanjutkan membaca.",
                    actionLabel = "Lanjutkan",
                    rawBook = b,
                )
            )
        }

        studentAssignments.filter { it.status.equals("returned", true) }.forEach { a ->
            list.add(
                AndroidTrackerItem(
                    id = a.id,
                    title = a.title,
                    subjectName = a.subjectName ?: "Mata Pelajaran",
                    category = "ASSIGNMENT",
                    subType = a.assignmentType,
                    statusBadge = "Perlu Perbaikan",
                    badgeColor = Color(0xFFEF4444),
                    timestampLabel = "Dikembalikan Guru",
                    reasonNote = "Guru pengampu meminta kamu memperbaiki lembar tugas ini dan mengirimkannya kembali.",
                    actionLabel = "Perbaiki"
                )
            )
        }

        list
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder),
        shadowElevation = 0.dp
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
                            .background(Color(0xFF6366F1).copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color(0xFF6366F1),
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text(
                            text = "Pusat Belajar & Status Tugas",
                            fontSize = 14.5.sp,
                            lineHeight = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                        Text(
                            text = "Semua materi & tugas dari yang lalu hingga sekarang",
                            fontSize = 11.sp,
                            lineHeight = 14.sp,
                            color = textSecondary
                        )
                    }
                }
            }

            // 3 Shortcut Tabs: Terlewat (Kiri), Belum Selesai (Tengah), Riwayat Belajar (Kanan)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Shortcut 1: Terlewat (KIRI)
                TrackerShortcutTab(
                    title = "Terlewat",
                    subtitle = "Perlu dikejar",
                    count = missedItems.size,
                    isSelected = activeTab == "missed",
                    accentColor = Color(0xFFEF4444),
                    modifier = Modifier.weight(1f),
                    onClick = { activeTab = "missed" }
                )

                // Shortcut 2: Belum Selesai (TENGAH)
                TrackerShortcutTab(
                    title = "Belum Selesai",
                    subtitle = "Lanjut belajar",
                    count = inProgressItems.size,
                    isSelected = activeTab == "in_progress",
                    accentColor = Color(0xFFF59E0B),
                    modifier = Modifier.weight(1f),
                    onClick = { activeTab = "in_progress" }
                )

                // Shortcut 3: Riwayat Belajar (KANAN)
                TrackerShortcutTab(
                    title = "Riwayat Belajar",
                    subtitle = "Sudah tuntas",
                    count = historyItems.size,
                    isSelected = activeTab == "history",
                    accentColor = Color(0xFF10B981),
                    modifier = Modifier.weight(1f),
                    onClick = { activeTab = "history" }
                )
            }

            // Tab Content List
            val currentItems = when (activeTab) {
                "missed" -> missedItems
                "in_progress" -> inProgressItems
                else -> historyItems
            }

            val emptyMessage = when (activeTab) {
                "missed" -> "Hebat! Tidak ada materi atau tugas yang terlewat. Pertahankan kedisiplinan belajarmu! 🎉"
                "in_progress" -> "Bagus sekali! Semua materi dan tugas yang kamu mulai sudah selesai. ✨"
                else -> "Belum ada riwayat aktivitas belajar yang tercatat. Yuk mulai baca materi pertamamu!"
            }

            if (currentItems.isEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = if (isDark) Color(0xFF161618) else Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, if (isDark) Color(0xFF27272A) else cardBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = emptyMessage,
                            fontSize = 12.sp,
                            color = textSecondary,
                            lineHeight = 16.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    currentItems.take(5).forEach { item ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    when (item.category) {
                                        "MATERIAL" -> {
                                            if (item.rawBook != null && onOpenBookReading != null) {
                                                onOpenBookReading(item.rawBook)
                                            } else if (onNavigateToMaterialDetail != null) {
                                                onNavigateToMaterialDetail(item.id)
                                            } else {
                                                onNavigateToMaterials()
                                            }
                                        }
                                        "ASSIGNMENT" -> {
                                            if (onNavigateToAssignmentDetail != null) {
                                                onNavigateToAssignmentDetail(item.id)
                                            } else {
                                                onNavigateToAssignments()
                                            }
                                        }
                                        "QUIZ" -> {
                                            if (onNavigateToQuizDetail != null) {
                                                onNavigateToQuizDetail(item.id)
                                            } else {
                                                onNavigateToQuizzes()
                                            }
                                        }
                                        else -> onNavigateToProgress()
                                    }
                                },
                            shape = RoundedCornerShape(14.dp),
                            color = if (isDark) Color(0xFF161618) else Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, if (isDark) Color(0xFF27272A) else cardBorder)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.weight(1f, fill = false)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (isDark) Color(0xFF222226) else Color(0xFFE2E8F0),
                                            modifier = Modifier.weight(1f, fill = false)
                                        ) {
                                            Text(
                                                text = item.subjectName,
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = textPrimary,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                softWrap = false
                                            )
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = item.badgeColor.copy(alpha = 0.14f)
                                        ) {
                                            Text(
                                                text = item.statusBadge,
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = item.badgeColor,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(6.dp))

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = item.badgeColor.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = item.actionLabel,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = item.badgeColor,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.5.dp),
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }

                                Text(
                                    text = item.title,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textPrimary,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    lineHeight = 16.sp
                                )

                                if (item.progressPercent != null && item.progressPercent > 0 && item.progressPercent < 100) {
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        LinearProgressIndicator(
                                            progress = { item.progressPercent / 100f },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(4.dp)
                                                .clip(RoundedCornerShape(2.dp)),
                                            color = Color(0xFF0284C7),
                                            trackColor = if (isDark) Color(0xFF27272A) else Color(0xFFE2E8F0)
                                        )
                                    }
                                }

                                if (!item.reasonNote.isNullOrBlank()) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isDark) Color(0xFF1E1E22) else Color(0xFFF1F5F9),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.Top,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(text = "💡", fontSize = 10.sp)
                                            Text(
                                                text = item.reasonNote,
                                                fontSize = 10.5.sp,
                                                color = textSecondary,
                                                lineHeight = 14.sp
                                            )
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = item.timestampLabel,
                                        fontSize = 10.sp,
                                        color = textSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TrackerShortcutTab(
    title: String,
    subtitle: String,
    count: Int,
    isSelected: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val isDark = LocalIsDarkTheme.current
    val textPrimary = if (isDark) Color.White else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) accentColor.copy(alpha = 0.12f) else if (isDark) Color(0xFF161618) else Color(0xFFF8FAFC),
        border = BorderStroke(
            1.dp,
            if (isSelected) accentColor else if (isDark) Color(0xFF27272A) else Color(0xFFE2E8F0)
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isSelected) accentColor else accentColor.copy(alpha = 0.16f)
                ) {
                    Text(
                        text = "$count",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isSelected) Color.White else accentColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                    )
                }
            }

            Text(
                text = title,
                fontSize = 10.5.sp,
                lineHeight = 13.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = subtitle,
                fontSize = 9.sp,
                lineHeight = 11.sp,
                color = textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
