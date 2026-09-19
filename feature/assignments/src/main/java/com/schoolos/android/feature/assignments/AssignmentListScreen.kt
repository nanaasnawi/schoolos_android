package com.schoolos.android.feature.assignments

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.schoolos.android.core.designsystem.*
import com.schoolos.android.domain.model.Assignment

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignmentListScreen(
    onBack: (() -> Unit)? = null,
    onAssignmentClick: (String) -> Unit = {},
    onCreateAssignment: () -> Unit = {},
    onCreateQuiz: () -> Unit = {},
    subjectId: String = "",
    viewModel: AssignmentListViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    var selectedTab by remember { mutableStateOf("Semua") }
    var speedDialExpanded by remember { mutableStateOf(false) }

    val isParent  = com.schoolos.android.core.auth.isParentRole(state.userRole)
    val isTeacher = com.schoolos.android.core.auth.isTeacherRole(state.userRole)

    // Tab counts mapping
    val tabCounts = if (isTeacher) {
        mapOf(
            "Semua" to (state.active.size + state.dueSoon.size),
            "Aktif" to state.active.size,
            "Perlu Dinilai" to state.dueSoon.size
        )
    } else {
        mapOf(
            "Semua" to (state.active.size + state.dueSoon.size + state.completed.size),
            "Segera" to state.dueSoon.size,
            "Aktif" to state.active.size,
            "Selesai" to state.completed.size
        )
    }

    Scaffold(
        containerColor = CosmicBlack,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            ExecutiveTopBar(
                title = if (isTeacher) "Tugas & Evaluasi" else "Daftar Tugas",
                subtitle = if (isTeacher) "Kelola penugasan siswa" else "PR & latihan mandiri",
                onBack = onBack,
            )
        },
        floatingActionButton = {
            if (isTeacher) {
                TeacherSpeedDialFab(
                    expanded = speedDialExpanded,
                    onToggle = { speedDialExpanded = !speedDialExpanded },
                    onCreateAssignment = {
                        speedDialExpanded = false
                        onCreateAssignment()
                    },
                    onCreateQuiz = {
                        speedDialExpanded = false
                        onCreateQuiz()
                    }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            PullRefreshContainer(
                isRefreshing = state.isRefreshing,
                onRefresh = viewModel::refresh,
                modifier = Modifier.fillMaxSize(),
            ) {
                if (state.isLoading) {
                    LoadingState()
                } else if (state.error != null) {
                    ErrorState(message = state.error!!, onRetry = viewModel::refresh)
                } else if (state.active.isEmpty() && state.dueSoon.isEmpty() && state.completed.isEmpty() && !isTeacher) {
                    EmptyState("Belum ada tugas yang diberikan!", Icons.AutoMirrored.Filled.Assignment)
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 8.dp,
                            bottom = 100.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        // ── IMMERSIVE HERO HEADER ────────────────────────────
                        item {
                            if (isTeacher) {
                                TeacherAssignmentHeroHeader(
                                    activeCount = state.active.size,
                                    pendingGradeCount = state.dueSoon.size,
                                    quizActiveCount = 0 // TODO: wire from ViewModel when quiz count available
                                )
                            } else {
                                StudentAssignmentHeroHeader(
                                    activeCount = state.active.size,
                                    dueSoonCount = state.dueSoon.size,
                                    completedCount = state.completed.size,
                                    isParent = isParent,
                                    childName = state.childName
                                )
                            }
                        }

                        // ── ACTIVE SUBJECT FILTER (from session detail) ──────
                        if (state.subjectFilter != null) {
                            item {
                                SubjectFilterChip(
                                    subject = state.subjectFilter!!,
                                    onClear = viewModel::clearSubjectFilter,
                                )
                            }
                        }

                        // Empty state for teachers — 2 action cards
                        if (isTeacher && state.active.isEmpty() && state.dueSoon.isEmpty() && state.completed.isEmpty()) {
                            item {
                                TeacherEmptyState(
                                    onCreateAssignment = onCreateAssignment,
                                    onCreateQuiz = onCreateQuiz
                                )
                            }
                        }

                        // ── PREMIUM TAB FILTER CHIPS ─────────────────────────
                        if (!isTeacher || (state.active.isNotEmpty() || state.dueSoon.isNotEmpty() || state.completed.isNotEmpty())) {
                            item {
                                AssignmentTabFilter(
                                    isTeacher = isTeacher,
                                    selectedTab = selectedTab,
                                    onTabSelected = { selectedTab = it },
                                    counts = tabCounts
                                )
                            }
                        }

                        // ── DELEGATE TO MODULAR CONTENT ──────────────────────
                        if (isTeacher) {
                            teacherAssignmentListContent(
                                activeItems = state.active,
                                dueSoonItems = state.dueSoon,
                                selectedTab = selectedTab,
                                onAssignmentClick = onAssignmentClick
                            )
                        } else {
                            studentAssignmentListContent(
                                activeItems = state.active,
                                dueSoonItems = state.dueSoon,
                                completedItems = state.completed,
                                selectedTab = selectedTab,
                                onAssignmentClick = onAssignmentClick
                            )
                        }
                    }
                }
            }

            // Speed dial scrim — tap to dismiss
            AnimatedVisibility(
                visible = speedDialExpanded,
                enter = fadeIn(tween(200)),
                exit = fadeOut(tween(150))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.32f))
                        .clickable { speedDialExpanded = false }
                )
            }
        }
    }
}

// ── SPEED DIAL FAB (Apple Minimalist) ──────────────────────────────────────────

@Composable
private fun TeacherSpeedDialFab(
    expanded: Boolean,
    onToggle: () -> Unit,
    onCreateAssignment: () -> Unit,
    onCreateQuiz: () -> Unit,
) {
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 45f else 0f,
        animationSpec = tween(200),
        label = "fab_rotation"
    )

    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Sub-actions
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(tween(160)) + expandVertically(tween(160), expandFrom = Alignment.Bottom),
            exit = fadeOut(tween(120)) + shrinkVertically(tween(120), shrinkTowards = Alignment.Bottom)
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SpeedDialOption(
                    label = "Buat Kuis",
                    icon = Icons.Default.Quiz,
                    accentColor = NeonBlue,
                    onClick = onCreateQuiz
                )
                SpeedDialOption(
                    label = "Buat Tugas",
                    icon = Icons.Default.Edit,
                    accentColor = TeacherNeon,
                    onClick = onCreateAssignment
                )
            }
        }

        // Main Minimalist FAB
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(com.schoolos.android.core.designsystem.CosmicSurface2)
                .border(0.5.dp, GlassBorder, RoundedCornerShape(14.dp))
                .clickable(onClick = onToggle),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = if (expanded) "Tutup" else "Buat",
                tint = TextPrimary,
                modifier = Modifier
                    .size(20.dp)
                    .rotate(rotation)
            )
        }
    }
}

@Composable
private fun SpeedDialOption(
    label: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(CosmicNavy)
            .border(0.5.dp, GlassBorder, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ── STUDENT HERO HEADER (Apple / Linear Minimalist) ──────────────────────────

@Composable
private fun StudentAssignmentHeroHeader(
    activeCount: Int,
    dueSoonCount: Int,
    completedCount: Int,
    isParent: Boolean,
    childName: String = ""
) {
    val totalCount = activeCount + dueSoonCount + completedCount
    val completionPercent = if (totalCount > 0) (completedCount * 100) / totalCount else 100

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CosmicNavy)
            .border(0.5.dp, GlassBorder, RoundedCornerShape(12.dp))
            .padding(16.dp),
    ) {
        Column {
            // Top Tag Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "STATUS PENUGASAN",
                    color = TextTertiary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = "$completionPercent% Selesai",
                    color = NeonSuccess,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.height(10.dp))

            val child = if (childName.isNotBlank()) childName else "Anak"
            Text(
                text = if (isParent) "Status Tugas $child" else "Daftar Tugas Siswa",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "Pantau tenggat waktu dan kumpulkan tepat waktu",
                fontSize = 11.sp,
                color = TextTertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(12.dp))

            // Completion Progress Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(com.schoolos.android.core.designsystem.CosmicSurface2)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth((completionPercent / 100f).coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(2.dp))
                        .background(NeonBlue)
                )
            }

            Spacer(Modifier.height(12.dp))

            // 3-Metric Stat Cards (Fixed Height 60.dp, perfectly aligned)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HeroMetricCard(
                    label = "TUGAS AKTIF",
                    value = "${activeCount + dueSoonCount}",
                    subtitle = "Belum Selesai",
                    accent = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                HeroMetricCard(
                    label = "MENDEKATI BATAS",
                    value = "$dueSoonCount",
                    subtitle = "Mendesak",
                    accent = if (dueSoonCount > 0) NeonWarning else TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                HeroMetricCard(
                    label = "TERKUMPUL",
                    value = "$completedCount",
                    subtitle = "Selesai",
                    accent = NeonSuccess,
                    modifier = Modifier.weight(1f)
                )
            }

            // Quiet Alert Pill if tasks are due soon
            if (dueSoonCount > 0) {
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(com.schoolos.android.core.designsystem.CosmicSurface2)
                        .border(0.5.dp, NeonWarning.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(NeonWarning)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "$dueSoonCount tugas mendekati batas waktu pengumpulan",
                        color = TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

// ── TEACHER HERO HEADER (Apple / Linear Minimalist) ──────────────────────────

@Composable
private fun TeacherAssignmentHeroHeader(
    activeCount: Int,
    pendingGradeCount: Int,
    quizActiveCount: Int = 0,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CosmicNavy)
            .border(0.5.dp, GlassBorder, RoundedCornerShape(12.dp))
            .padding(16.dp),
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "PORTAL EVALUASI PENGAJAR",
                    color = TextTertiary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = "${activeCount + pendingGradeCount} Berjalan",
                    color = TeacherNeon,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.height(10.dp))

            Text(
                text = "Penugasan & Evaluasi Siswa",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "Kelola penugasan, kuis, dan review hasil kerja siswa",
                fontSize = 11.sp,
                color = TextTertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(12.dp))

            // 3 Metric Cards (Fixed Height 60.dp, perfectly aligned)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HeroMetricCard(
                    label = "TUGAS AKTIF",
                    value = "$activeCount",
                    subtitle = "Berjalan",
                    accent = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                HeroMetricCard(
                    label = "PERLU NILAI",
                    value = "$pendingGradeCount",
                    subtitle = "Menunggu",
                    accent = if (pendingGradeCount > 0) NeonWarning else TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                HeroMetricCard(
                    label = "KUIS AKTIF",
                    value = "$quizActiveCount",
                    subtitle = "Formatif",
                    accent = NeonBlue,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

// ── HERO METRIC CARD (Fixed Height, Strictly Constrained) ─────────────────────

@Composable
private fun HeroMetricCard(
    label: String,
    value: String,
    subtitle: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .height(60.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(com.schoolos.android.core.designsystem.CosmicSurface2)
            .border(0.5.dp, GlassBorder, RoundedCornerShape(8.dp))
            .padding(vertical = 6.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = value,
            color = accent,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = subtitle,
            color = TextTertiary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// ── TEACHER EMPTY STATE (Clean Obsidian) ──────────────────────────────────────

@Composable
private fun TeacherEmptyState(
    onCreateAssignment: () -> Unit,
    onCreateQuiz: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(CosmicNavy)
                .border(0.5.dp, GlassBorder, RoundedCornerShape(12.dp))
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(com.schoolos.android.core.designsystem.CosmicSurface2)
                        .border(0.5.dp, GlassBorder, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Assignment,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "Belum Ada Penugasan",
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Mulai dengan membuat tugas atau kuis baru untuk kelas Anda",
                    color = TextTertiary,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }
        }

        // 2 action cards side by side (Fixed Height 104.dp)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TeacherActionCard(
                icon = Icons.Default.Edit,
                title = "Buat Tugas",
                subtitle = "Tugaskan PR & proyek",
                accentColor = TeacherNeon,
                onClick = onCreateAssignment,
                modifier = Modifier.weight(1f)
            )
            TeacherActionCard(
                icon = Icons.Default.Quiz,
                title = "Buat Kuis",
                subtitle = "Ujian & evaluasi",
                accentColor = NeonBlue,
                onClick = onCreateQuiz,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun TeacherActionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(104.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(CosmicNavy)
            .border(0.5.dp, GlassBorder, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(com.schoolos.android.core.designsystem.CosmicSurface2)
                    .border(0.5.dp, GlassBorder, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = TextPrimary,
                    modifier = Modifier.size(17.dp)
                )
            }
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = TextTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ── ASSIGNMENT TAB FILTER (Apple Segmented Style) ─────────────────────────────

@Composable
private fun AssignmentTabFilter(
    isTeacher: Boolean,
    selectedTab: String,
    onTabSelected: (String) -> Unit,
    counts: Map<String, Int> = emptyMap(),
) {
    val tabs = if (isTeacher) listOf("Semua", "Aktif", "Perlu Dinilai")
               else listOf("Semua", "Segera", "Aktif", "Selesai")

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        items(tabs) { tab ->
            val isSelected = tab == selectedTab
            val count = counts[tab] ?: 0

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) com.schoolos.android.core.designsystem.CosmicSurface2 else CosmicNavy)
                    .border(
                        0.5.dp,
                        if (isSelected) com.schoolos.android.core.designsystem.GlassBorder2 else GlassBorder,
                        RoundedCornerShape(8.dp)
                    )
                    .clickable { onTabSelected(tab) }
                    .padding(horizontal = 12.dp, vertical = 7.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = tab,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isSelected) TextPrimary else TextTertiary,
                        maxLines = 1,
                    )
                    if (count > 0) {
                        Spacer(Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isSelected) com.schoolos.android.core.designsystem.CosmicSurface3 else com.schoolos.android.core.designsystem.CosmicSurface2)
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "$count",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isSelected) TextPrimary else TextTertiary
                            )
                        }
                    }
                }
            }
        }
    }
}

