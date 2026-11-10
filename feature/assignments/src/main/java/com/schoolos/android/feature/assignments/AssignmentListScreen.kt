package com.schoolos.android.feature.assignments

import com.schoolos.android.core.R as CoreR
import androidx.compose.ui.res.painterResource
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
    className: String? = null,
    viewModel: AssignmentListViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    var selectedTab by remember { mutableStateOf("Semua") }
    var speedDialExpanded by remember { mutableStateOf(false) }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.refresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val isParent  = com.schoolos.android.core.auth.isParentRole(state.userRole)
    val isPrincipal = com.schoolos.android.core.auth.isPrincipalRole(state.userRole)
    val isTeacher = com.schoolos.android.core.auth.isTeacherRole(state.userRole) || isPrincipal

    // Tab counts mapping
    val tabCounts = if (isTeacher) {
        mapOf(
            "Semua" to (state.active.size + state.dueSoon.size + state.scheduled.size),
            "Terjadwal" to state.scheduled.size,
            "Aktif" to state.active.size,
            "Perlu Dinilai" to state.dueSoon.size
        )
    } else {
        mapOf(
            "Semua" to (state.active.size + state.dueSoon.size + state.completed.size),
            "Segera" to state.dueSoon.size,
            "Aktif" to (state.active.size + state.dueSoon.size),
            "Selesai" to state.completed.size
        )
    }

    Scaffold(
        containerColor = CosmicBlack,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            ExecutiveTopBar(
                title = if (subjectId.isNotBlank()) "Tugas • $subjectId"
                    else if (isPrincipal) "Pemantauan Tugas Sekolah"
                    else if (isTeacher) "Tugas & Evaluasi"
                    else "Daftar Tugas",
                subtitle = if (subjectId.isNotBlank()) "Mata Pelajaran Aktif"
                    else if (isPrincipal) "Rekapitulasi penugasan seluruh guru & rombel"
                    else if (isTeacher) "Kelola penugasan siswa"
                    else "PR & latihan mandiri",
                onBack = onBack,
            )
        },
        floatingActionButton = {
            if (isTeacher) {
                val isDark = LocalIsDarkTheme.current
                val speedDialCardBg = if (isDark) Color(0xFF141416) else Color.White
                val speedDialBorder = if (isDark) Color(0xFF27272A) else Color(0xFFE2E8F0)
                val primaryColor = if (isDark) Color(0xFF059669) else Color(0xFF10B981)
                val primaryBorder = if (isDark) Color(0xFF34D399).copy(alpha = 0.35f) else Color(0xFF059669).copy(alpha = 0.20f)
                val rotation by animateFloatAsState(
                    targetValue = if (speedDialExpanded) 135f else 0f,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "fabRotation"
                )

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.navigationBarsPadding().padding(bottom = 8.dp)
                ) {
                    AnimatedVisibility(
                        visible = speedDialExpanded,
                        enter = fadeIn(tween(180)) + slideInVertically(tween(180)) { it / 2 },
                        exit = fadeOut(tween(140)) + slideOutVertically(tween(140)) { it / 2 },
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    speedDialExpanded = false
                                    onCreateAssignment()
                                }
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = speedDialCardBg,
                                border = BorderStroke(1.dp, speedDialBorder),
                                shadowElevation = 0.dp,
                            ) {
                                Text(
                                    text = "Buat Tugas",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color(0xFF34D399) else Color(0xFF047857),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                            Surface(
                                shape = CircleShape,
                                color = primaryColor,
                                border = BorderStroke(1.dp, primaryBorder),
                                shadowElevation = 0.dp,
                                modifier = Modifier.size(42.dp),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Buat Tugas",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Main Executive Floating Button
                    Surface(
                        onClick = { speedDialExpanded = !speedDialExpanded },
                        shape = CircleShape,
                        color = if (speedDialExpanded) (if (isDark) Color(0xFF27272A) else Color(0xFFF1F5F9)) else primaryColor,
                        border = BorderStroke(1.dp, if (speedDialExpanded) speedDialBorder else primaryBorder),
                        shadowElevation = 0.dp,
                        modifier = Modifier.size(52.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (speedDialExpanded) Icons.Default.Close else Icons.Default.Add,
                                contentDescription = if (speedDialExpanded) "Tutup Menu" else "Buat Tugas",
                                tint = if (speedDialExpanded) (if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A)) else Color.White,
                                modifier = Modifier
                                    .size(22.dp)
                                    .graphicsLayer { rotationZ = rotation }
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            PullRefreshContainer(
                isRefreshing = state.isRefreshing,
                onRefresh = { viewModel.refresh(isPullRefresh = true) },
                modifier = Modifier.fillMaxSize(),
            ) {
                if (state.isLoading) {
                    LoadingState()
                } else if (state.error != null) {
                    ErrorState(message = state.error!!, onRetry = viewModel::refresh)
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
                                    scheduledCount = state.scheduled.size,
                                    pendingGradeCount = state.dueSoon.size,
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

                        // ── ACTIVE SUBJECT / CLASS / DATE FILTER ──────────────
                        if (state.subjectFilter != null || state.classFilter != null || state.dateFilter != null) {
                            item {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (state.subjectFilter != null || state.classFilter != null) {
                                        SubjectFilterChip(
                                            subject = state.subjectFilter.orEmpty(),
                                            className = state.classFilter,
                                            canClear = false,
                                        )
                                    }
                                    if (state.dateFilter != null) {
                                        SessionDateFilterBadge(
                                            dateStr = state.dateFilter!!,
                                            onShowAllArchive = { viewModel.clearDateFilter() }
                                        )
                                    }
                                }
                            }
                        }

                        // ── PREMIUM TAB FILTER CHIPS ─────────────────────────
                        item {
                            AssignmentTabFilter(
                                isTeacher = isTeacher,
                                selectedTab = selectedTab,
                                onTabSelected = { selectedTab = it },
                                counts = tabCounts
                            )
                        }

                        // ── DELEGATE TO MODULAR CONTENT ──────────────────────
                        if (isTeacher) {
                            teacherAssignmentListContent(
                                activeItems = state.active,
                                dueSoonItems = state.dueSoon,
                                scheduledItems = state.scheduled,
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

            // Speed dial scrim
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

// ── STUDENT HERO HEADER (ADAPTIVE LIGHT / DARK & 22.dp ROUNDED) ──────────────

@Composable
private fun StudentAssignmentHeroHeader(
    activeCount: Int,
    dueSoonCount: Int,
    completedCount: Int,
    isParent: Boolean,
    childName: String = ""
) {
    val isDark = LocalIsDarkTheme.current
    val cardBg = if (isDark) Color(0xFF111113) else Color.White
    val cardBorder = if (isDark) Color(0xFF1F1F23) else Color(0xFFE2E8F0)
    val textPrimary = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFFA1A1AA) else Color(0xFF64748B)
    val textTertiary = if (isDark) Color(0xFF71717A) else Color(0xFF94A3B8)
    val innerCardBg = if (isDark) Color(0xFF161618) else Color(0xFFF8FAFC)
    val innerCardBorder = if (isDark) Color(0xFF27272A) else Color(0xFFE2E8F0)
    val successColor = if (isDark) Color(0xFF10B981) else Color(0xFF16A34A)
    val studentAccentColor = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)

    val totalCount = activeCount + dueSoonCount + completedCount
    val completionPercent = if (totalCount > 0) (completedCount * 100) / totalCount else 100

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder),
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top Tag Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isDark) Color(0xFF1E293B) else Color(0xFFE0F2FE),
                    border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFBAE6FD))
                ) {
                    Text(
                        text = "STATUS PENUGASAN",
                        color = studentAccentColor,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isDark) successColor.copy(alpha = 0.15f) else Color(0xFFDCFCE7),
                    border = BorderStroke(1.dp, if (isDark) successColor.copy(alpha = 0.35f) else Color(0xFF86EFAC))
                ) {
                    Text(
                        text = if (totalCount > 0) "$completionPercent% Selesai" else "Bebas Tugas",
                        color = if (isDark) successColor else Color(0xFF15803D),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            // Title & Subtitle with Student Accent
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isDark) Color(0xFF1E293B) else Color(0xFFE0F2FE))
                        .border(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFBAE6FD), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = CoreR.drawable.ic_modern_tasks),
                        contentDescription = null,
                        tint = studentAccentColor,
                        modifier = Modifier.size(19.dp)
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    val child = if (childName.isNotBlank()) childName else "Anak"
                    Text(
                        text = if (isParent) "Status Tugas $child" else "Daftar Tugas Mandiri",
                        fontSize = 15.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary,
                        letterSpacing = (-0.2).sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Pantau tenggat waktu dan kumpulkan tepat waktu.",
                        fontSize = 11.sp,
                        color = textSecondary,
                        lineHeight = 14.sp
                    )
                }
            }

            // Completion Progress Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(if (isDark) Color(0xFF27272A) else Color(0xFFE2E8F0))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(if (totalCount > 0) (completionPercent / 100f).coerceIn(0f, 1f) else 1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (totalCount > 0 && completionPercent < 100) studentAccentColor else successColor)
                )
            }

            // 3-Metric Stat Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                HeroMetricCard(
                    label = "TUGAS AKTIF",
                    value = "${activeCount + dueSoonCount}",
                    subtitle = "Belum Selesai",
                    accent = textPrimary,
                    cardBg = innerCardBg,
                    cardBorder = innerCardBorder,
                    textTertiary = textTertiary,
                    modifier = Modifier.weight(1f)
                )
                HeroMetricCard(
                    label = "MENDEKATI BATAS",
                    value = "$dueSoonCount",
                    subtitle = "Mendesak",
                    accent = if (dueSoonCount > 0) NeonWarning else textPrimary,
                    cardBg = innerCardBg,
                    cardBorder = innerCardBorder,
                    textTertiary = textTertiary,
                    modifier = Modifier.weight(1f)
                )
                HeroMetricCard(
                    label = "TERKUMPUL",
                    value = "$completedCount",
                    subtitle = "Selesai",
                    accent = successColor,
                    cardBg = innerCardBg,
                    cardBorder = innerCardBorder,
                    textTertiary = textTertiary,
                    modifier = Modifier.weight(1f)
                )
            }

            // Quiet Alert Pill if tasks are due soon
            if (dueSoonCount > 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isDark) NeonWarning.copy(alpha = 0.12f) else Color(0xFFFFFBEB))
                        .border(1.dp, if (isDark) NeonWarning.copy(alpha = 0.35f) else Color(0xFFFDE68A), RoundedCornerShape(10.dp))
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
                        color = if (isDark) NeonWarning else Color(0xFFB45309),
                        fontSize = 11.5.sp,
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

// ── TEACHER HERO HEADER (ADAPTIVE LIGHT / DARK & 22.dp ROUNDED) ──────────────

@Composable
private fun TeacherAssignmentHeroHeader(
    activeCount: Int,
    scheduledCount: Int = 0,
    pendingGradeCount: Int = 0,
) {
    val isDark = LocalIsDarkTheme.current
    val cardBg = if (isDark) Color(0xFF111113) else Color.White
    val cardBorder = if (isDark) Color(0xFF1F1F23) else Color(0xFFE2E8F0)
    val textPrimary = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFFA1A1AA) else Color(0xFF64748B)
    val textTertiary = if (isDark) Color(0xFF71717A) else Color(0xFF94A3B8)
    val innerCardBg = if (isDark) Color(0xFF161618) else Color(0xFFF8FAFC)
    val innerCardBorder = if (isDark) Color(0xFF27272A) else Color(0xFFE2E8F0)
    val teacherAccentColor = if (isDark) Color(0xFF10B981) else Color(0xFF059669)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder),
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val tagText = when {
                activeCount > 0 && scheduledCount > 0 -> "$activeCount Berjalan • $scheduledCount Draf"
                scheduledCount > 0 -> "$scheduledCount Draf Terjadwal"
                activeCount > 0 -> "$activeCount Berjalan"
                pendingGradeCount > 0 -> "$pendingGradeCount Perlu Nilai"
                else -> "0 Berjalan"
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isDark) Color(0xFF064E3B).copy(alpha = 0.35f) else Color(0xFFECFDF5),
                    border = BorderStroke(1.dp, if (isDark) Color(0xFF10B981).copy(alpha = 0.4f) else Color(0xFFA7F3D0))
                ) {
                    Text(
                        text = "PORTAL EVALUASI PENGAJAR",
                        color = teacherAccentColor,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = innerCardBg,
                    border = BorderStroke(1.dp, innerCardBorder)
                ) {
                    Text(
                        text = tagText,
                        color = if (activeCount > 0 || scheduledCount > 0) teacherAccentColor else textTertiary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isDark) Color(0xFF064E3B).copy(alpha = 0.35f) else Color(0xFFECFDF5))
                        .border(1.dp, if (isDark) Color(0xFF10B981).copy(alpha = 0.4f) else Color(0xFFA7F3D0), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = CoreR.drawable.ic_modern_tasks),
                        contentDescription = null,
                        tint = teacherAccentColor,
                        modifier = Modifier.size(19.dp)
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    Text(
                        text = "Penugasan & Evaluasi Siswa",
                        fontSize = 15.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary,
                        letterSpacing = (-0.2).sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Kelola penugasan, kuis, dan review hasil kerja siswa.",
                        fontSize = 11.sp,
                        color = textSecondary,
                        lineHeight = 14.sp
                    )
                }
            }

            // 3 Metric Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                HeroMetricCard(
                    label = "TUGAS AKTIF",
                    value = "$activeCount",
                    subtitle = "Berjalan",
                    accent = if (activeCount > 0) teacherAccentColor else textPrimary,
                    cardBg = innerCardBg,
                    cardBorder = innerCardBorder,
                    textTertiary = textTertiary,
                    modifier = Modifier.weight(1f)
                )
                HeroMetricCard(
                    label = "DRAF TERJADWAL",
                    value = "$scheduledCount",
                    subtitle = "Terjadwal",
                    accent = if (scheduledCount > 0) AccentNeonAmber else textPrimary,
                    cardBg = innerCardBg,
                    cardBorder = innerCardBorder,
                    textTertiary = textTertiary,
                    modifier = Modifier.weight(1f)
                )
                HeroMetricCard(
                    label = "PERLU NILAI",
                    value = "$pendingGradeCount",
                    subtitle = "Menunggu",
                    accent = if (pendingGradeCount > 0) NeonWarning else textPrimary,
                    cardBg = innerCardBg,
                    cardBorder = innerCardBorder,
                    textTertiary = textTertiary,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

// ── HERO METRIC CARD (14.dp ROUNDED, ADAPTIVE) ───────────────────────────────

@Composable
private fun HeroMetricCard(
    label: String,
    value: String,
    subtitle: String,
    accent: Color,
    cardBg: Color,
    cardBorder: Color,
    textTertiary: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .height(66.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(cardBg)
            .border(1.dp, cardBorder, RoundedCornerShape(14.dp))
            .padding(vertical = 8.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = value,
            color = accent,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = subtitle,
            color = textTertiary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// ── TEACHER EMPTY STATE (22.dp ROUNDED, ADAPTIVE) ────────────────────────────

@Composable
private fun TeacherEmptyState() {
    val isDark = LocalIsDarkTheme.current
    val cardBg = if (isDark) Color(0xFF111113) else Color.White
    val cardBorder = if (isDark) Color(0xFF1F1F23) else Color(0xFFE2E8F0)
    val textPrimary = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A)
    val textTertiary = if (isDark) Color(0xFF71717A) else Color(0xFF94A3B8)
    val innerCardBg = if (isDark) Color(0xFF161618) else Color(0xFFF8FAFC)
    val innerCardBorder = if (isDark) Color(0xFF27272A) else Color(0xFFE2E8F0)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder),
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(innerCardBg)
                    .border(1.dp, innerCardBorder, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = CoreR.drawable.ic_modern_tasks),
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(Modifier.height(14.dp))
            Text(
                text = "Belum Ada Penugasan",
                color = textPrimary,
                fontSize = 15.5.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Gunakan tombol '+ Buat Tugas' di pojok bawah untuk merancang tugas atau latihan bagi siswa Anda.",
                color = textTertiary,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ── ASSIGNMENT TAB FILTER (12.dp ROUNDED, ADAPTIVE) ──────────────────────────

@Composable
private fun AssignmentTabFilter(
    isTeacher: Boolean,
    selectedTab: String,
    onTabSelected: (String) -> Unit,
    counts: Map<String, Int> = emptyMap(),
) {
    val isDark = LocalIsDarkTheme.current
    val textSecondary = if (isDark) Color(0xFFA1A1AA) else Color(0xFF64748B)
    val accentColor = if (isTeacher) {
        if (isDark) Color(0xFF10B981) else Color(0xFF059669)
    } else {
        if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)
    }

    val tabs = if (isTeacher) listOf("Semua", "Terjadwal", "Aktif", "Perlu Dinilai")
               else listOf("Semua", "Segera", "Aktif", "Selesai")

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        items(tabs) { tab ->
            val isSelected = tab == selectedTab
            val count = counts[tab] ?: 0

            val chipBg = when {
                isSelected -> accentColor.copy(alpha = if (isDark) 0.16f else 0.10f)
                else -> if (isDark) Color(0xFF141417) else Color.White
            }
            val chipBorder = when {
                isSelected -> accentColor.copy(alpha = if (isDark) 0.55f else 0.45f)
                else -> if (isDark) Color(0xFF232326) else Color(0xFFE2E8F0)
            }
            val chipTextColor = when {
                isSelected -> accentColor
                else -> textSecondary
            }
            val badgeBg = when {
                isSelected -> accentColor.copy(alpha = if (isDark) 0.24f else 0.16f)
                else -> if (isDark) Color(0xFF1F1F23) else Color(0xFFF1F5F9)
            }
            val badgeTextColor = when {
                isSelected -> accentColor
                else -> textSecondary
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(chipBg)
                    .border(1.dp, chipBorder, RoundedCornerShape(12.dp))
                    .clickable { onTabSelected(tab) }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = tab,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = chipTextColor,
                        maxLines = 1,
                        softWrap = false,
                    )
                    if (count > 0) {
                        Spacer(Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(badgeBg)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "$count",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = badgeTextColor,
                                maxLines = 1,
                                softWrap = false,
                            )
                        }
                    }
                }
            }
        }
    }
}
