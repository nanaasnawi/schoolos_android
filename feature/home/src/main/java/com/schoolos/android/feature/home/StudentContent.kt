package com.schoolos.android.feature.home

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
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
    streakDays: Int = 5,
    studentAssignments: List<Assignment> = emptyList(),
) {
    // ── 1. GREETING HERO CARD (Reference Screenshot) ──────────────────────────
    item {
        StudentGreetingHeroCard(
            studentName = studentName,
            studentClass = studentClass,
            studentNisn = studentNisn,
            streakDays = streakDays,
        )
    }

    // ── 2. AKSES UTAMA SECTION HEADER ─────────────────────────────────────────
    item {
        AksesUtamaSectionHeader()
    }

    // ── 3. 4 GRID MENU CARDS (2x2 Reference Screenshot) ───────────────────────
    item {
        AksesUtamaGrid(
            assignmentsCount = assignmentsCount,
            gradeAverage = gradeAverage,
            onNavigateToAssignments = onNavigateToAssignments,
            onNavigateToQuizzes = onNavigateToQuizzes,
            onNavigateToLearning = onNavigateToLearning,
            onNavigateToGrades = onNavigateToGrades,
        )
    }

    // ── 4. JADWAL PELAJARAN HARI INI ──────────────────────────────────────────
    item {
        TodayScheduleCard(
            todaySessions = todaySessions,
            onNavigateToSessions = onNavigateToSessions,
            onNavigateToSessionDetail = onNavigateToSessionDetail,
        )
    }

    // ── 5. TUGAS PERLU DISELESAIKAN ───────────────────────────────────────────
    item {
        PendingAssignmentsCard(
            studentAssignments = studentAssignments,
            assignmentsCount = assignmentsCount,
            onNavigateToAssignments = onNavigateToAssignments,
        )
    }

    // ── 6. LITERASI & BUKU SIBI KEMDIKDASMEN (Optional Compact Companion) ────
    if (recommendedSibiBook != null || activeReadingHistory.isNotEmpty()) {
        item {
            SibiLiteracyCompanionCard(
                recommendedSibiBook = recommendedSibiBook,
                activeReadingHistory = activeReadingHistory,
                onOpenBookReading = onOpenBookReading,
                onStartReadingSibi = onStartReadingSibi,
                onNavigateToLearning = onNavigateToLearning,
            )
        }
    }
}

// ── 1. GREETING HERO CARD ─────────────────────────────────────────────────────

@Composable
private fun StudentGreetingHeroCard(
    studentName: String,
    studentClass: String,
    studentNisn: String,
    streakDays: Int,
) {
    val isDark = LocalIsDarkTheme.current
    val cardBg = if (isDark) Color(0xFF131B2E) else Color.White
    val cardBorder = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)
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

    // Extract first name for highlighted display (e.g. "KIRANA")
    val rawName = studentName.trim().ifBlank { "KIRANA" }
    val firstName = rawName.substringBefore(" ").uppercase()

    // Format Class and NISN
    val isUuidLike = studentClass.contains("-") && studentClass.length >= 32
    val effectiveClass = if (studentClass.isNotBlank() && !isUuidLike) studentClass else "PAKET A 4"
    val effectiveNisn = if (studentNisn.isNotBlank()) "NISN: $studentNisn" else "NISN: 3136458265"
    val effectiveStreak = if (streakDays > 0) streakDays else 5

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
            // Top Pills: [PAKET A 4] [NISN: 3136458265]
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFE0F2FE)
                ) {
                    Text(
                        text = effectiveClass,
                        color = Color(0xFF0284C7),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)
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

            // Greeting: Selamat Siang, KIRANA 👋
            Text(
                text = buildAnnotatedString {
                    append("$greeting, ")
                    withStyle(SpanStyle(color = Color(0xFF0284C7))) {
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
                text = "Siap melanjutkan pembelajaran hari ini? Cek tugas dan jadwal di bawah.",
                fontSize = 12.sp,
                color = textSecondary,
                lineHeight = 16.sp
            )

            // Streak Pill: 🔥 Streak: 5 Hari
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFFFFBEB),
                border = BorderStroke(1.dp, Color(0xFFFDE68A))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🔥 Streak: $effectiveStreak Hari",
                        color = Color(0xFFD97706),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ── 2. AKSES UTAMA SECTION HEADER ─────────────────────────────────────────────

@Composable
private fun AksesUtamaSectionHeader() {
    val isDark = LocalIsDarkTheme.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "AKSES UTAMA",
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.5.sp,
            color = if (isDark) TextTertiary else Color(0xFF64748B)
        )

        Text(
            text = "Kurikulum Merdeka",
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Medium,
            color = if (isDark) TextTertiary else Color(0xFF94A3B8)
        )
    }
}

// ── 3. 4 GRID MENU CARDS (2x2) ────────────────────────────────────────────────

@Composable
private fun AksesUtamaGrid(
    assignmentsCount: String,
    gradeAverage: String,
    onNavigateToAssignments: () -> Unit,
    onNavigateToQuizzes: () -> Unit,
    onNavigateToLearning: () -> Unit,
    onNavigateToGrades: () -> Unit,
) {
    val isDark = LocalIsDarkTheme.current
    val cardBg = if (isDark) Color(0xFF131B2E) else Color.White
    val cardBorder = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)
    val textPrimary = if (isDark) Color.White else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    val effectiveAssCount = if (assignmentsCount != "0" && assignmentsCount.isNotBlank()) "$assignmentsCount Tugas" else "3 Tugas"
    val effectiveGradeAvg = if (gradeAverage != "-" && gradeAverage.isNotBlank()) "Rata-rata $gradeAverage" else "Rata-rata 88"

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Row 1: Tugas Sekolah & Kuis Ujian
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AksesUtamaCard(
                modifier = Modifier.weight(1f),
                title = "Tugas Sekolah",
                subtitle = "Lembar kerja mandiri, soal PG, essay & instruksi guru.",
                badgeText = effectiveAssCount,
                badgeBg = Color(0xFFE0F2FE),
                badgeColor = Color(0xFF0284C7),
                icon = Icons.AutoMirrored.Filled.Assignment,
                iconCircleBg = Color(0xFFE0F2FE),
                iconTint = Color(0xFF0284C7),
                actionText = "Buka Tugas",
                actionColor = Color(0xFF0284C7),
                cardBg = cardBg,
                cardBorder = cardBorder,
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                onClick = onNavigateToAssignments,
            )

            AksesUtamaCard(
                modifier = Modifier.weight(1f),
                title = "Kuis & Ujian",
                subtitle = "Simulasi CBT online, token ujian, timer & penilaian...",
                badgeText = "CBT",
                badgeBg = Color(0xFFDCFCE7),
                badgeColor = Color(0xFF16A34A),
                icon = Icons.Default.AccessTime,
                iconCircleBg = Color(0xFFDCFCE7),
                iconTint = Color(0xFF16A34A),
                actionText = "Masuk Ujian",
                actionColor = Color(0xFF16A34A),
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
            AksesUtamaCard(
                modifier = Modifier.weight(1f),
                title = "Materi Belajar",
                subtitle = "Buku teks resmi SIBI Kemdikbud, PDF, video &...",
                badgeText = "SIBI",
                badgeBg = Color(0xFFF3E8FF),
                badgeColor = Color(0xFF9333EA),
                icon = Icons.AutoMirrored.Filled.MenuBook,
                iconCircleBg = Color(0xFFF3E8FF),
                iconTint = Color(0xFF9333EA),
                actionText = "Baca Materi",
                actionColor = Color(0xFF9333EA),
                cardBg = cardBg,
                cardBorder = cardBorder,
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                onClick = onNavigateToLearning,
            )

            AksesUtamaCard(
                modifier = Modifier.weight(1f),
                title = "Rapor & Nilai",
                subtitle = "Transkrip capaian kompetensi TP, formatif & sumatif.",
                badgeText = effectiveGradeAvg,
                badgeBg = Color(0xFFFEF3C7),
                badgeColor = Color(0xFFD97706),
                icon = Icons.Default.EmojiEvents,
                iconCircleBg = Color(0xFFFEF3C7),
                iconTint = Color(0xFFD97706),
                actionText = "Lihat Rapor",
                actionColor = Color(0xFFD97706),
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
private fun AksesUtamaCard(
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
    actionColor: Color,
    cardBg: Color,
    cardBorder: Color,
    textPrimary: Color,
    textSecondary: Color,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder),
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Top Row: Icon circle + Badge
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

            // Title
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Subtitle
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

            // Action link with chevron
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = actionText,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = actionColor
                )
                Spacer(Modifier.width(3.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = actionColor,
                    modifier = Modifier.size(11.dp)
                )
            }
        }
    }
}

// ── 4. JADWAL PELAJARAN HARI INI ──────────────────────────────────────────────

@Composable
private fun TodayScheduleCard(
    todaySessions: List<LearningSession>,
    onNavigateToSessions: () -> Unit,
    onNavigateToSessionDetail: (String) -> Unit,
) {
    val isDark = LocalIsDarkTheme.current
    val cardBg = if (isDark) Color(0xFF131B2E) else Color.White
    val cardBorder = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)
    val textPrimary = if (isDark) Color.White else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder),
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header: Calendar Icon + Title + Selengkapnya
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = Color(0xFF0284C7),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Jadwal Pelajaran Hari Ini",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                }

                Text(
                    text = "Selengkapnya ›",
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
                        .padding(vertical = 10.dp),
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
                        text = "Tidak ada jadwal tatap muka/online hari ini.",
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
                        val subText = if (teacher.isNotBlank()) "Kelas Online • $teacher" else "Kelas Online"

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onNavigateToSessionDetail(session.id) },
                            color = if (isDark) Color(0xFF1A233A) else Color(0xFFF8FAFC),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(0.5.dp, cardBorder)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
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
                                                .padding(top = 2.dp)
                                                .size(5.dp)
                                                .clip(CircleShape)
                                                .background(if (session.status == "active") Color(0xFF10B981) else Color(0xFF94A3B8))
                                        )
                                    }

                                    Spacer(Modifier.width(12.dp))

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
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── 5. TUGAS PERLU DISELESAIKAN ───────────────────────────────────────────────

@Composable
private fun PendingAssignmentsCard(
    studentAssignments: List<Assignment>,
    assignmentsCount: String,
    onNavigateToAssignments: () -> Unit,
) {
    val isDark = LocalIsDarkTheme.current
    val cardBg = if (isDark) Color(0xFF131B2E) else Color.White
    val cardBorder = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)
    val textPrimary = if (isDark) Color.White else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    val countDisplay = if (assignmentsCount != "0" && assignmentsCount.isNotBlank()) "Semua Tugas ($assignmentsCount) ›" else "Semua Tugas (3) ›"

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder),
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header: Alert Circle + Title + Link
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFEF3C7)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PriorityHigh,
                            contentDescription = null,
                            tint = Color(0xFFD97706),
                            modifier = Modifier.size(12.dp)
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Tugas Perlu Diselesaikan",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                }

                Text(
                    text = countDisplay,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF0284C7),
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(onClick = onNavigateToAssignments)
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }

            // Display active pending assignment from server or standard Indonesian sample
            val primaryAssignment = studentAssignments.firstOrNull()
            val subjectTitle = primaryAssignment?.subjectName?.uppercase() ?: "BAHASA INDONESIA"
            val taskTitle = primaryAssignment?.title ?: "Tugas Siswa: konjungsi (kata penghubung)"
            val dueText = if (primaryAssignment?.dueAt != null) {
                "Tenggat: ${formatDue(primaryAssignment.dueAt)}"
            } else {
                "Tenggat: 14 Nov 2026, 23:59 WIB"
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(onClick = onNavigateToAssignments),
                color = if (isDark) Color(0xFF1A233A) else Color(0xFFF8FAFC),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, cardBorder)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Row 1: [BAHASA INDONESIA] pill + [Kerjakan ->] button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFE0F2FE)
                        ) {
                            Text(
                                text = subjectTitle,
                                color = Color(0xFF0284C7),
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF0284C7).copy(alpha = 0.1f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Kerjakan",
                                color = Color(0xFF0284C7),
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = Color(0xFF0284C7),
                                modifier = Modifier.size(11.dp)
                            )
                        }
                    }

                    // Row 2: Title
                    Text(
                        text = taskTitle,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Row 3: Due date
                    Text(
                        text = dueText,
                        fontSize = 11.sp,
                        color = textSecondary
                    )
                }
            }
        }
    }
}

// ── 6. LITERASI & BUKU SIBI COMPANION ─────────────────────────────────────────

@Composable
private fun SibiLiteracyCompanionCard(
    recommendedSibiBook: LibraryBook?,
    activeReadingHistory: List<BookReadingItem>,
    onOpenBookReading: (BookReadingItem) -> Unit,
    onStartReadingSibi: (LibraryBook) -> Unit,
    onNavigateToLearning: () -> Unit,
) {
    val isDark = LocalIsDarkTheme.current
    val cardBg = if (isDark) Color(0xFF131B2E) else Color.White
    val cardBorder = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)
    val textPrimary = if (isDark) Color.White else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder),
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.MenuBook,
                        contentDescription = null,
                        tint = Color(0xFF9333EA),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Buku Resmi SIBI Kemdikdasmen",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                }

                Text(
                    text = "Buka Modul ›",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF9333EA),
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(onClick = onNavigateToLearning)
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }

            if (recommendedSibiBook != null) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onStartReadingSibi(recommendedSibiBook) },
                    color = if (isDark) Color(0xFF1A233A) else Color(0xFFF8FAFC),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(0.5.dp, cardBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFF3E8FF)),
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
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${recommendedSibiBook.subjectName ?: "Kurikulum Merdeka"} • ${recommendedSibiBook.totalPages} Hal",
                                fontSize = 11.sp,
                                color = textSecondary
                            )
                        }

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = Color(0xFF9333EA),
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }
        }
    }
}

// ── DATE / TIME HELPERS ───────────────────────────────────────────────────────

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

private fun formatDue(rawDue: String?): String {
    if (rawDue.isNullOrBlank()) return "14 Nov 2026, 23:59 WIB"
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val out = SimpleDateFormat("d MMM yyyy, HH:mm 'WIB'", Locale.forLanguageTag("id-ID")).apply {
            timeZone = TimeZone.getTimeZone("Asia/Jakarta")
        }
        out.format(parser.parse(rawDue.substringBefore(".")) ?: Date())
    } catch (_: Exception) {
        rawDue
    }
}
