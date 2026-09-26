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
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.schoolos.android.core.designsystem.CosmicNavy
import com.schoolos.android.core.designsystem.CosmicSurface2
import com.schoolos.android.core.designsystem.GlassBorder
import com.schoolos.android.core.designsystem.LineTrendChart
import com.schoolos.android.core.designsystem.NeonBlue
import com.schoolos.android.core.designsystem.NeonSuccess
import com.schoolos.android.core.designsystem.NeonWarning
import com.schoolos.android.core.designsystem.StudentNeon
import com.schoolos.android.core.designsystem.TextPrimary
import com.schoolos.android.core.designsystem.TextSecondary
import com.schoolos.android.core.designsystem.TextTertiary
import com.schoolos.android.domain.model.BookReadingItem
import com.schoolos.android.domain.model.LibraryBook

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
    activeReadingHistory: List<BookReadingItem> = emptyList(),
    recommendedSibiBook: LibraryBook? = null,
    onOpenBookReading: (BookReadingItem) -> Unit = {},
    onStartReadingSibi: (LibraryBook) -> Unit = {},
    xpCount: String = "0",
    assignmentsCount: String = "0",
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
                .clip(RoundedCornerShape(16.dp))
                .background(CosmicNavy)
                .border(1.dp, GlassBorder, RoundedCornerShape(16.dp)),
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

    // ── 3. ACADEMIC PERFORMANCE & GRADE SNAPSHOT ─────────────────────────────────
    item {
        LightSectionHeader(
            title = "Performa Akademik & Nilai",
            sub = "Indeks capaian belajar semester berjalan",
            onSeeAll = onNavigateToGrades,
        )
    }

    item {
        val avgNum = gradeAverage.toDoubleOrNull() ?: 0.0
        val hasGrade = gradeAverage != "-" && avgNum > 0.0
        val predicateLabel = when {
            !hasGrade -> "Semester Berjalan"
            avgNum >= 85.0 -> "Predikat A • Sangat Baik"
            avgNum >= 75.0 -> "Predikat B • Tuntas KKM"
            else -> "Predikat C • Perlu Penguatan"
        }
        val predicateColor = when {
            !hasGrade -> TextTertiary
            avgNum >= 85.0 -> NeonSuccess
            avgNum >= 75.0 -> NeonBlue
            else -> NeonWarning
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.linearGradient(
                        listOf(
                            CosmicNavy,
                            CosmicSurface2.copy(alpha = 0.85f),
                        )
                    )
                )
                .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
                .clickable(onClick = onNavigateToGrades)
                .padding(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Header Row: Icon + Label + Status Pill
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f, fill = false),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(predicateColor.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Assessment,
                                contentDescription = null,
                                tint = predicateColor,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "RATA-RATA NILAI",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextPrimary,
                                letterSpacing = 0.5.sp,
                            )
                            Text(
                                text = "Evaluasi Hasil Belajar",
                                fontSize = 10.sp,
                                color = TextTertiary,
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(predicateColor.copy(alpha = 0.12f))
                            .border(0.75.dp, predicateColor.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = predicateLabel,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = predicateColor,
                            maxLines = 1,
                        )
                    }
                }

                // Core Metric Row: Big Score on Left + Circular Grade Gauge on Right
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = if (hasGrade) String.format(java.util.Locale.US, "%.1f", avgNum) else gradeAverage,
                                fontSize = 42.sp,
                                fontWeight = FontWeight.Black,
                                color = if (hasGrade) TextPrimary else TextTertiary,
                                letterSpacing = (-1.5).sp,
                                lineHeight = 44.sp,
                            )
                            if (hasGrade) {
                                Text(
                                    text = " / 100",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextTertiary,
                                    modifier = Modifier.padding(bottom = 6.dp, start = 4.dp),
                                )
                            }
                        }
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = if (hasGrade) gradeStatus else "Belum ada nilai yang diinput guru",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    // Visual Letter-Grade Gauge Meter
                    Box(
                        modifier = Modifier.size(56.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        androidx.compose.material3.CircularProgressIndicator(
                            progress = { if (hasGrade) (avgNum / 100.0).toFloat().coerceIn(0f, 1f) else 0f },
                            modifier = Modifier.fillMaxSize(),
                            color = predicateColor,
                            trackColor = CosmicSurface2,
                            strokeWidth = 5.dp,
                            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round,
                        )
                        val letterGrade = when {
                            !hasGrade -> "-"
                            avgNum >= 90.0 -> "A"
                            avgNum >= 80.0 -> "B"
                            avgNum >= 70.0 -> "C"
                            avgNum >= 60.0 -> "D"
                            else -> "E"
                        }
                        Text(
                            text = letterGrade,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = predicateColor,
                        )
                    }
                }

                // Balanced 2-Card Micro-Bento Row (KKM Status + Top Subject or XP)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    // Card 1: KKM Compliance
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(CosmicSurface2.copy(alpha = 0.6f))
                            .border(0.5.dp, GlassBorder, RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(if (avgNum >= 75.0) NeonSuccess.copy(alpha = 0.15f) else NeonWarning.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = if (avgNum >= 75.0) "✓" else "!",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = if (avgNum >= 75.0) NeonSuccess else NeonWarning,
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "STANDAR KKM",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextTertiary,
                            )
                            Text(
                                text = if (avgNum >= 75.0) "Tuntas KKM (75)" else "Perlu Penguatan",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (avgNum >= 75.0) NeonSuccess else NeonWarning,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }

                    // Card 2: Highest Subject OR XP Gamification
                    if (topGradeSubjects.isNotEmpty()) {
                        val top1 = topGradeSubjects.first()
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(CosmicSurface2.copy(alpha = 0.6f))
                                .border(0.5.dp, GlassBorder, RoundedCornerShape(10.dp))
                                .padding(horizontal = 10.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(StudentNeon.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "★",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    color = StudentNeon,
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "MAPEL TERTINGGI",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextTertiary,
                                )
                                Text(
                                    text = "${top1.subjectName} (${String.format(java.util.Locale.US, "%.0f", top1.finalScore)})",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = StudentNeon,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(CosmicSurface2.copy(alpha = 0.6f))
                                .border(0.5.dp, GlassBorder, RoundedCornerShape(10.dp))
                                .padding(horizontal = 10.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(StudentNeon.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "⭐",
                                    fontSize = 11.sp,
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "POIN PRESTASI",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextTertiary,
                                )
                                Text(
                                    text = "$xpCount XP Aktif",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = StudentNeon,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }

                // If multiple top subjects, clean 2-column showcase
                if (topGradeSubjects.size >= 2) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        topGradeSubjects.take(2).forEachIndexed { idx, subj ->
                            val rankIcon = if (idx == 0) "🥇" else "🥈"
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(CosmicNavy.copy(alpha = 0.7f))
                                    .border(0.5.dp, GlassBorder.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f, fill = false),
                                ) {
                                    Text(rankIcon, fontSize = 11.sp)
                                    Spacer(Modifier.width(5.dp))
                                    Text(
                                        text = subj.subjectName,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = String.format(java.util.Locale.US, "%.0f (%s)", subj.finalScore, subj.letterGrade),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = StudentNeon,
                                )
                            }
                        }
                    }
                }

                // Clean Footer Divider & Action
                HorizontalDivider(color = GlassBorder.copy(alpha = 0.5f), thickness = 0.7.dp)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Lihat Buku Rapor & Transkrip Lengkap",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Transkrip",
                            fontSize = 11.sp,
                            color = StudentNeon,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = StudentNeon,
                            modifier = Modifier.size(12.dp),
                        )
                    }
                }
            }
        }
    }

    // ── 4. READING PROGRESS & SIBI BOOK INTEGRATION ────────────────────────────
    if (activeReadingHistory.isNotEmpty()) {
        item {
            LightSectionHeader(
                title = "Lanjutkan Membaca",
                sub = "${activeReadingHistory.size} Buku dalam progres",
                onSeeAll = onNavigateToLearning,
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                activeReadingHistory.take(2).forEach { item ->
                    ReadingHistoryCard(
                        item = item,
                        onContinueReading = { onOpenBookReading(item) },
                    )
                }
            }
        }
    } else {
        item {
            LightSectionHeader(
                title = "Rekomendasi Buku SIBI",
                sub = "Buku resmi Kemendikdasmen RI",
                onSeeAll = onNavigateToLearning,
            )
        }

        item {
            val book = recommendedSibiBook
            if (book != null) {
                SibiBookRecommendationCard(
                    book = book,
                    onStartReading = { onStartReadingSibi(book) },
                )
            } else {
                SibiCuratedPlaceholderCard(
                    onExploreLibrary = onNavigateToLearning,
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

@Composable
private fun ReadingHistoryCard(
    item: BookReadingItem,
    onContinueReading: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CosmicNavy)
            .border(1.dp, GlassBorder, RoundedCornerShape(14.dp))
            .clickable(onClick = onContinueReading)
            .padding(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Cover Image with fallback
            Box(
                modifier = Modifier
                    .size(width = 54.dp, height = 76.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(CosmicSurface2)
                    .border(0.5.dp, GlassBorder, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center,
            ) {
                if (!item.coverUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = item.coverUrl,
                        contentDescription = item.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.MenuBook,
                        contentDescription = null,
                        tint = NeonBlue,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }

            // Book Metadata & Progress
            Column(
                modifier = Modifier.weight(1f),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(NeonBlue.copy(alpha = 0.12f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = (item.subjectName ?: "Materi Bacaan").uppercase(),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonBlue,
                        )
                    }
                    val gradeLevel = item.gradeLevelName
                    if (!gradeLevel.isNullOrBlank()) {
                        Text(
                            text = gradeLevel,
                            fontSize = 10.sp,
                            color = TextTertiary,
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))

                Text(
                    text = item.title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                val authorInfo = item.author ?: item.publisher ?: "Pusat Perbukuan"
                Text(
                    text = authorInfo,
                    fontSize = 11.sp,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Spacer(Modifier.height(8.dp))

                // Progress Bar & Info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val pageText = if (item.startPage != null && item.endPage != null) {
                        "Hal. ${item.currentPage} (Tugas: ${item.startPage}-${item.endPage})"
                    } else {
                        "Halaman ${item.currentPage} / ${item.totalPages}"
                    }
                    Text(
                        text = pageText,
                        fontSize = 10.sp,
                        color = TextTertiary,
                    )
                    Text(
                        text = "${(item.progressPercentage * 100).toInt()}%",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonBlue,
                    )
                }

                Spacer(Modifier.height(4.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(CosmicSurface2),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(item.progressPercentage.coerceIn(0.02f, 1f))
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(NeonBlue),
                    )
                }
            }

            // Quick Arrow Icon
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Lanjutkan",
                tint = TextTertiary,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun SibiBookRecommendationCard(
    book: LibraryBook,
    onStartReading: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CosmicNavy)
            .border(1.dp, GlassBorder, RoundedCornerShape(14.dp))
            .padding(14.dp),
    ) {
        Column {
            // Top SIBI badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(NeonBlue.copy(alpha = 0.12f))
                        .border(0.5.dp, NeonBlue.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                ) {
                    Text(
                        text = "SIBI KEMENDIKDASMEN • RESMI",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonBlue,
                        letterSpacing = 0.4.sp,
                    )
                }

                Text(
                    text = "Kurikulum Merdeka",
                    fontSize = 10.sp,
                    color = TextTertiary,
                )
            }

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Book cover
                Box(
                    modifier = Modifier
                        .size(width = 64.dp, height = 88.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(CosmicSurface2)
                        .border(0.5.dp, GlassBorder, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (!book.coverUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = book.coverUrl,
                            contentDescription = book.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                    } else {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = null,
                            tint = NeonBlue,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = book.title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        lineHeight = 18.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )

                    Spacer(Modifier.height(4.dp))

                    val authorText = book.author ?: "Pusat Perbukuan"
                    Text(
                        text = "Oleh: $authorText",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )

                    val subjectAndLevel = listOfNotNull(book.subjectName, book.gradeLevelName).joinToString(" • ")
                    if (subjectAndLevel.isNotBlank()) {
                        Text(
                            text = subjectAndLevel,
                            fontSize = 10.sp,
                            color = TextTertiary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    Text(
                        text = "${book.totalPages} Halaman Tersedia",
                        fontSize = 10.sp,
                        color = TextTertiary,
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Action button: Mulai Membaca
            Button(
                onClick = onStartReading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonBlue,
                    contentColor = Color.White,
                ),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.MenuBook,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Mulai Membaca Buku Ini",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun SibiCuratedPlaceholderCard(
    onExploreLibrary: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CosmicNavy)
            .border(1.dp, GlassBorder, RoundedCornerShape(14.dp))
            .clickable(onClick = onExploreLibrary)
            .padding(14.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(NeonBlue.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.MenuBook,
                    contentDescription = null,
                    tint = NeonBlue,
                    modifier = Modifier.size(24.dp),
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Perpustakaan Digital SIBI",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                )
                Text(
                    text = "Jelajahi 600+ buku teks resmi Kurikulum Merdeka",
                    fontSize = 11.sp,
                    color = TextTertiary,
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = TextTertiary,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

