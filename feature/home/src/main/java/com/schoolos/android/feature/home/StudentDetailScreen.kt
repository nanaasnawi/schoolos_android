package com.schoolos.android.feature.home

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.schoolos.android.core.designsystem.*
import com.schoolos.android.domain.model.Achievement
import com.schoolos.android.domain.model.ClassStudent
import com.schoolos.android.domain.model.Progress

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentDetailScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: StudentDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val student = uiState.student
    val progress = uiState.progress
    val achievements = uiState.achievements
    val isLoading = uiState.isLoading
    val errorMessage = uiState.errorMessage
    val selectedTab = uiState.selectedTab

    val tabs = listOf("Perkembangan", "Metrik", "Pencapaian", "Kontak")

    val isFemale = student.gender?.trim()?.uppercase() == "P"
    val genderLabel = if (isFemale) "Perempuan" else "Laki-laki"
    val genderIcon = if (isFemale) Icons.Default.Female else Icons.Default.Male
    val heroAccent = if (isFemale) Color(0xFFEC4899) else StudentNeon

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = CosmicBlack,
        topBar = {
            ExecutiveTopBar(
                title = "Detail Profil Siswa",
                subtitle = if (student.className.isNotBlank()) "Kelas ${student.className} • Portal Guru" else "Portal Guru",
                onBack = onBack,
                actions = {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(CosmicNavy)
                            .border(0.5.dp, GlassBorder, RoundedCornerShape(10.dp))
                            .clickable { viewModel.refresh() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Muat Ulang",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ── HERO HEADER ────────────────────────────────────────────────
            item {
                StudentHeroCard(
                    student = student,
                    genderLabel = genderLabel,
                    genderIcon = genderIcon,
                    heroAccent = heroAccent,
                )
            }

            // ── 4 KEY METRICS ──────────────────────────────────────────────
            item {
                val avgGrade = progress?.overallProgress?.let { "%.0f%%".format(it) } ?: "--"
                val quizDone = progress?.let { "${it.quizCompleted}/${it.quizTotal}" } ?: "--"
                val assignDone = progress?.let { "${it.assignmentCompleted}/${it.assignmentTotal}" } ?: "--"
                val attendance = progress?.let {
                    val pct = if (it.sessionTotal > 0) (it.sessionAttended * 100 / it.sessionTotal) else 0
                    "$pct%"
                } ?: "--"

                MetricsRow(
                    avgGrade = avgGrade,
                    quizDone = quizDone,
                    assignDone = assignDone,
                    attendance = attendance,
                    isLoading = isLoading,
                )
            }

            // ── TAB ROW ───────────────────────────────────────────────────
            item {
                StudentTabRow(
                    tabs = tabs,
                    selectedTab = selectedTab,
                    onTabSelected = viewModel::selectTab
                )
            }

            // ── ERROR BANNER ──────────────────────────────────────────────
            if (errorMessage != null) {
                item {
                    ErrorBanner(
                        message = errorMessage,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            // ── TAB CONTENT ───────────────────────────────────────────────
            when (selectedTab) {
                0 -> {
                    item {
                        AcademicProgressTab(
                            progress = progress,
                            isLoading = isLoading,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }

                1 -> {
                    item {
                        LearningMetricsTab(
                            progress = progress,
                            isLoading = isLoading,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }

                2 -> {
                    if (achievements.isEmpty() && !isLoading) {
                        item {
                            EmptyAchievementsCard(modifier = Modifier.padding(horizontal = 16.dp))
                        }
                    } else {
                        items(achievements, key = { it.id }) { achievement ->
                            AchievementItem(
                                achievement = achievement,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                }

                3 -> {
                    item {
                        ContactTab(
                            student = student,
                            context = context,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }
        }
    }
}

// ─── HERO CARD ───────────────────────────────────────────────────────────────
@Composable
private fun StudentHeroCard(
    student: ClassStudent,
    genderLabel: String,
    genderIcon: ImageVector,
    heroAccent: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        heroAccent.copy(alpha = 0.18f),
                        CosmicNavy,
                    )
                )
            )
            .border(0.5.dp, heroAccent.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
            .padding(18.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Avatar circle with double glowing border
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(heroAccent.copy(alpha = 0.12f))
                    .border(2.dp, heroAccent, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = student.fullName.firstOrNull()?.toString()?.uppercase() ?: "?",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = heroAccent
                )
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = student.fullName.ifBlank { "Siswa SchoolOS" },
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (student.nisn.isNotBlank()) {
                    Text(
                        text = "NISN: ${student.nisn}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    InfoPill(
                        label = if (student.className.isNotBlank()) "Kelas ${student.className}" else "Siswa Aktif",
                        icon = Icons.Default.Class,
                        color = heroAccent,
                    )
                    InfoPill(
                        label = genderLabel,
                        icon = genderIcon,
                        color = TextSecondary,
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoPill(
    label: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.12f))
            .border(0.5.dp, color.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(11.dp)
            )
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ─── METRICS ROW ──────────────────────────────────────────────────────────────
@Composable
private fun MetricsRow(
    avgGrade: String,
    quizDone: String,
    assignDone: String,
    attendance: String,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MetricBentoCard(
            label = "Rata-rata",
            value = if (isLoading) "…" else avgGrade,
            icon = Icons.Default.Grade,
            accentColor = StudentNeon,
            modifier = Modifier.weight(1f)
        )
        MetricBentoCard(
            label = "Kuis",
            value = if (isLoading) "…" else quizDone,
            icon = Icons.Default.Quiz,
            accentColor = NeonBlue,
            modifier = Modifier.weight(1f)
        )
        MetricBentoCard(
            label = "Tugas",
            value = if (isLoading) "…" else assignDone,
            icon = Icons.Default.AssignmentTurnedIn,
            accentColor = NeonSuccess,
            modifier = Modifier.weight(1f)
        )
        MetricBentoCard(
            label = "Hadir",
            value = if (isLoading) "…" else attendance,
            icon = Icons.Default.EventAvailable,
            accentColor = NeonWarning,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun MetricBentoCard(
    label: String,
    value: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(86.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(CosmicNavy)
            .border(0.5.dp, GlassBorder, RoundedCornerShape(14.dp))
            .padding(10.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(accentColor.copy(alpha = 0.14f))
                    .border(0.5.dp, accentColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(15.dp)
                )
            }
            Column {
                Text(
                    text = value,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(1.dp))
                Text(
                    text = label,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ─── TAB ROW ─────────────────────────────────────────────────────────────────
@Composable
private fun StudentTabRow(
    tabs: List<String>,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(horizontal = 16.dp)) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = CosmicNavy,
            contentColor = StudentNeon,
            modifier = Modifier.clip(RoundedCornerShape(12.dp)).border(0.5.dp, GlassBorder, RoundedCornerShape(12.dp)),
            divider = {},
            indicator = { tabPositions ->
                if (selectedTab < tabPositions.size) {
                    Box(
                        modifier = Modifier
                            .tabIndicatorOffset(tabPositions[selectedTab])
                            .fillMaxHeight()
                            .padding(4.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(StudentNeon.copy(alpha = 0.20f))
                            .border(0.5.dp, StudentNeon.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                    )
                }
            }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { onTabSelected(index) },
                ) {
                    Text(
                        text = title,
                        fontSize = 12.sp,
                        fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                        color = if (selectedTab == index) StudentNeon else TextSecondary,
                        modifier = Modifier.padding(vertical = 10.dp)
                    )
                }
            }
        }
    }
}

// ─── TAB 0 — ACADEMIC PROGRESS ───────────────────────────────────────────────
@Composable
private fun AcademicProgressTab(
    progress: Progress?,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {

        // Status Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CosmicNavy)
                .border(0.5.dp, GlassBorder, RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(StudentNeon.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = null,
                            tint = StudentNeon,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Status Akademik Siswa",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Ringkasan evaluasi belajar siswa",
                            fontSize = 11.sp,
                            color = TextTertiary
                        )
                    }
                }

                HorizontalDivider(color = GlassBorder, thickness = 0.5.dp)

                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(CosmicSurface)
                    )
                } else {
                    val status = progress?.academicStatus ?: "Data perkembangan belum tersedia"
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(StudentNeon.copy(alpha = 0.10f))
                            .border(0.5.dp, StudentNeon.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                            .padding(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = StudentNeon,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = status,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                        }
                    }

                    val notes = progress?.teacherNotes
                    if (!notes.isNullOrBlank()) {
                        Text(
                            text = "Catatan Evaluasi Guru",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary
                        )
                        Text(
                            text = notes,
                            fontSize = 12.sp,
                            color = TextSecondary,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }

        // Coaching Notes Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CosmicNavy)
                .border(0.5.dp, GlassBorder, RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(NeonBlue.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notes,
                            contentDescription = null,
                            tint = NeonBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = "Catatan Pembinaan Siswa",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
                HorizontalDivider(color = GlassBorder, thickness = 0.5.dp)
                val coachingNotes = progress?.teacherNotes?.takeIf { it.isNotBlank() }
                    ?: "Belum ada catatan pembinaan khusus dari guru pengampu."
                Text(
                    text = coachingNotes,
                    fontSize = 12.sp,
                    color = TextSecondary,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

// ─── TAB 1 — LEARNING METRICS ───────────────────────────────────────────────
@Composable
private fun LearningMetricsTab(
    progress: Progress?,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CosmicNavy)
            .border(0.5.dp, GlassBorder, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(StudentNeon.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.BarChart,
                        contentDescription = null,
                        tint = StudentNeon,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = "Metrik & Pencapaian Modul",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            HorizontalDivider(color = GlassBorder, thickness = 0.5.dp)

            if (isLoading) {
                repeat(4) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(CosmicSurface)
                    )
                }
            } else {
                val lessonsDone = progress?.lessonCompleted ?: 0
                val lessonsTotal = progress?.lessonTotal ?: 0
                val assignDone = progress?.assignmentCompleted ?: 0
                val assignTotal = progress?.assignmentTotal ?: 0
                val quizDone = progress?.quizCompleted ?: 0
                val quizTotal = progress?.quizTotal ?: 0
                val sessionDone = progress?.sessionAttended ?: 0
                val sessionTotal = progress?.sessionTotal ?: 0

                MetricProgressRow(
                    title = "Modul & Materi Pembelajaran",
                    current = lessonsDone,
                    total = lessonsTotal,
                    color = StudentNeon,
                    icon = Icons.AutoMirrored.Filled.MenuBook
                )
                MetricProgressRow(
                    title = "Tugas & Praktik Lapangan",
                    current = assignDone,
                    total = assignTotal,
                    color = NeonBlue,
                    icon = Icons.Default.Assignment
                )
                MetricProgressRow(
                    title = "Kuis & Asesmen Mandiri",
                    current = quizDone,
                    total = quizTotal,
                    color = NeonSuccess,
                    icon = Icons.Default.Quiz
                )
                MetricProgressRow(
                    title = "Kehadiran Sesi Pembelajaran",
                    current = sessionDone,
                    total = sessionTotal,
                    color = NeonWarning,
                    icon = Icons.Default.EventAvailable
                )
            }
        }
    }
}

@Composable
private fun MetricProgressRow(
    title: String,
    current: Int,
    total: Int,
    color: Color,
    icon: ImageVector,
) {
    val pct = if (total > 0) (current.toFloat() / total.toFloat()).coerceIn(0f, 1f) else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = pct,
        animationSpec = tween(600),
        label = "progress_$title"
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )
            }
            Text(
                text = if (total > 0) "$current/$total (${(pct * 100).toInt()}%)" else "--",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }

        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape),
            color = color,
            trackColor = color.copy(alpha = 0.12f)
        )
    }
}

// ─── TAB 2 — ACHIEVEMENTS ───────────────────────────────────────────────────
@Composable
private fun EmptyAchievementsCard(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CosmicNavy)
            .border(0.5.dp, GlassBorder, RoundedCornerShape(16.dp))
            .padding(28.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(StudentNeon.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = StudentNeon,
                    modifier = Modifier.size(28.dp)
                )
            }
            Text(
                text = "Belum Ada Lencana Prestasi",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = "Lencana prestasi akan tercatat otomatis saat siswa menyelesaikan tantangan & kuis.",
                fontSize = 11.sp,
                color = TextTertiary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun AchievementItem(
    achievement: Achievement,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CosmicNavy)
            .border(0.5.dp, GlassBorder, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(StudentNeon.copy(alpha = 0.12f))
                    .border(1.dp, StudentNeon.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = StudentNeon,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = achievement.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = achievement.description,
                    fontSize = 11.sp,
                    color = TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (!achievement.earnedAt.isNullOrBlank()) {
                    Text(
                        text = "Diraih: ${achievement.earnedAt}",
                        fontSize = 10.sp,
                        color = StudentNeon,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ─── TAB 3 — CONTACT ─────────────────────────────────────────────────────────
@Composable
private fun ContactTab(
    student: ClassStudent,
    context: Context,
    modifier: Modifier = Modifier,
) {
    val phoneRaw = student.noHp ?: ""
    val cleanPhone = phoneRaw.replace(Regex("[^0-9]"), "")
    val waPhone = if (cleanPhone.startsWith("0")) "62" + cleanPhone.substring(1) else cleanPhone
    val studentEmail = student.email ?: "${student.fullName.lowercase().replace(" ", ".")}@schoolos.id"

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Kontak & Koordinasi Orang Tua",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            modifier = Modifier.padding(bottom = 2.dp)
        )

        // WhatsApp Card
        ContactActionCard(
            title = "WhatsApp Wali Murid",
            subtitle = phoneRaw.ifBlank { "Nomor telepon tidak tersedia" },
            icon = Icons.AutoMirrored.Filled.Chat,
            accentColor = NeonSuccess,
            enabled = cleanPhone.isNotBlank(),
            onClick = {
                val message = "Halo Bapak/Ibu wali dari ${student.fullName} (${student.className}), saya dari pihak guru School OS ingin berkoordinasi mengenai perkembangan siswa."
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("https://api.whatsapp.com/send?phone=$waPhone&text=${Uri.encode(message)}")
                }
                context.startActivity(intent)
            }
        )

        // Phone Call Card
        ContactActionCard(
            title = "Telepon Langsung",
            subtitle = phoneRaw.ifBlank { "Nomor telepon tidak tersedia" },
            icon = Icons.Default.Call,
            accentColor = NeonBlue,
            enabled = cleanPhone.isNotBlank(),
            onClick = {
                val intent = Intent(Intent.ACTION_DIAL).apply {
                    data = Uri.parse("tel:$cleanPhone")
                }
                context.startActivity(intent)
            }
        )

        // Email Card
        ContactActionCard(
            title = "Email Resmi Siswa",
            subtitle = studentEmail,
            icon = Icons.Default.Email,
            accentColor = StudentNeon,
            enabled = true,
            onClick = {
                val intent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("mailto:$studentEmail")
                }
                context.startActivity(intent)
            }
        )
    }
}

@Composable
private fun ContactActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val alpha = if (enabled) 1f else 0.4f
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CosmicNavy)
            .border(0.5.dp, GlassBorder, RoundedCornerShape(14.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accentColor.copy(alpha = 0.12f * alpha))
                    .border(0.5.dp, accentColor.copy(alpha = 0.3f * alpha), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor.copy(alpha = alpha),
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary.copy(alpha = alpha),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = accentColor.copy(alpha = alpha),
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (enabled) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = TextTertiary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

// ─── ERROR BANNER ────────────────────────────────────────────────────────────
@Composable
private fun ErrorBanner(
    message: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(NeonError.copy(alpha = 0.15f))
            .border(0.5.dp, NeonError.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = null,
                tint = NeonError,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = message,
                fontSize = 12.sp,
                color = NeonError,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
