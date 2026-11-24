package com.schoolos.android.feature.quizzes

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.FactCheck
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.MonitorHeart
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.schoolos.android.core.auth.isPrincipalRole
import com.schoolos.android.core.auth.isTeacherRole
import com.schoolos.android.core.auth.resolveRoleDisplay
import com.schoolos.android.core.designsystem.CosmicBlack
import com.schoolos.android.core.designsystem.ErrorState
import com.schoolos.android.core.designsystem.ExecutiveTopBar
import com.schoolos.android.core.designsystem.LoadingState
import com.schoolos.android.core.designsystem.LocalIsDarkTheme
import com.schoolos.android.core.designsystem.NeonBlue
import com.schoolos.android.core.designsystem.NeonError
import com.schoolos.android.core.designsystem.NeonSuccess
import com.schoolos.android.core.designsystem.NeonWarning
import com.schoolos.android.core.designsystem.PullRefreshContainer
import com.schoolos.android.core.designsystem.SessionDateFilterBadge
import com.schoolos.android.core.designsystem.StudentNeon
import com.schoolos.android.core.designsystem.SubjectFilterChip
import com.schoolos.android.core.designsystem.TeacherNeon
import com.schoolos.android.domain.model.Quiz
import com.schoolos.android.domain.model.QuizProctorScheduleDashboard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizListScreen(
    onBack: () -> Unit = {},
    onQuizClick: (String) -> Unit = {},
    onCreateQuiz: () -> Unit = {},
    subjectId: String = "",
    className: String? = null,
    viewModel: QuizListViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val isPrincipal = isPrincipalRole(state.userRole)
    val isTeacher = isTeacherRole(state.userRole) || isPrincipal
    var isSpeedDialOpen by remember { mutableStateOf(false) }

    var staffMainTab by rememberSaveable { mutableIntStateOf(0) } // 0: Daftar & Token, 1: Jadwal & Pantau
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var statusFilter by rememberSaveable { mutableStateOf("ALL") }

    val nowMs = rememberTickerMillis(1000L)

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.forceRefresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val queryLower = searchQuery.trim().lowercase()
    val searchMatchedQuizzes = remember(state.quizzes, queryLower) {
        if (queryLower.isEmpty()) {
            state.quizzes
        } else {
            state.quizzes.filter { q ->
                q.title.lowercase().contains(queryLower) ||
                    (q.subjectName?.lowercase()?.contains(queryLower) == true) ||
                    (q.effectiveDisplayClass?.lowercase()?.contains(queryLower) == true) ||
                    (q.teacherName?.lowercase()?.contains(queryLower) == true) ||
                    (q.examToken?.lowercase()?.contains(queryLower) == true)
            }
        }
    }

    val statusMatchedQuizzes = remember(searchMatchedQuizzes, statusFilter, isTeacher) {
        when (statusFilter) {
            "LIVE" -> searchMatchedQuizzes.filter { q ->
                if (!isTeacher) {
                    val done = q.studentHasCompleted || q.studentAttemptStatus?.lowercase() in listOf("completed", "submitted", "graded")
                    !done && q.effectivePhase == "LIVE"
                } else {
                    q.effectivePhase == "LIVE"
                }
            }
            "UPCOMING" -> searchMatchedQuizzes.filter { it.effectivePhase == "UPCOMING" }
            "DRAFT" -> searchMatchedQuizzes.filter { it.effectivePhase == "DRAFT" }
            "ENDED" -> searchMatchedQuizzes.filter { q ->
                q.effectivePhase == "ENDED" || (!isTeacher && (q.studentHasCompleted || q.studentAttemptStatus?.lowercase() in listOf("completed", "submitted", "graded")))
            }
            else -> searchMatchedQuizzes
        }
    }

    val liveStaffQuizzes = statusMatchedQuizzes.filter { it.effectivePhase == "LIVE" }
    val upcomingStaffQuizzes = statusMatchedQuizzes.filter { it.effectivePhase == "UPCOMING" }
    val draftStaffQuizzes = statusMatchedQuizzes.filter { it.effectivePhase == "DRAFT" }
    val endedStaffQuizzes = statusMatchedQuizzes.filter { it.effectivePhase == "ENDED" }

    val doneStudentQuizzes = statusMatchedQuizzes.filter {
        it.effectivePhase == "ENDED" ||
            it.studentHasCompleted ||
            it.studentAttemptStatus?.lowercase() in listOf("completed", "submitted", "graded")
    }
    val upcomingStudentQuizzes = statusMatchedQuizzes.filter {
        it !in doneStudentQuizzes && it.effectivePhase == "UPCOMING"
    }
    val activeStudentQuizzes = statusMatchedQuizzes.filter {
        it !in doneStudentQuizzes && it !in upcomingStudentQuizzes && it.isCurrentlyOpenForExam
    }

    val sessionsById = remember(state.proctorSchedule) {
        state.proctorSchedule?.sessions?.associateBy { it.id }.orEmpty()
    }
    val filteredScheduleSessions = remember(state.proctorSchedule, queryLower, statusFilter) {
        val base = state.proctorSchedule?.sessions.orEmpty()
        base.filter { s ->
            val matchQ = queryLower.isEmpty() ||
                s.title.lowercase().contains(queryLower) ||
                (s.subjectName?.lowercase()?.contains(queryLower) == true) ||
                (s.className?.lowercase()?.contains(queryLower) == true) ||
                (s.teacherName?.lowercase()?.contains(queryLower) == true) ||
                (s.examToken?.lowercase()?.contains(queryLower) == true)
            val matchStatus = statusFilter == "ALL" || s.examPhase.equals(statusFilter, ignoreCase = true)
            matchQ && matchStatus
        }
    }

    val isDark = LocalIsDarkTheme.current
    val cardBg = if (isDark) Color(0xFF111113) else Color.White
    val cardBorder = if (isDark) Color(0xFF1F1F23) else Color(0xFFE2E8F0)
    val innerCardBg = if (isDark) Color(0xFF161618) else Color(0xFFF8FAFC)
    val innerCardBorder = if (isDark) Color(0xFF27272A) else Color(0xFFE2E8F0)
    val textPrimary = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFFA1A1AA) else Color(0xFF64748B)
    val textTertiary = if (isDark) Color(0xFF71717A) else Color(0xFF94A3B8)
    val primaryAccent = if (isTeacher) TeacherNeon else StudentNeon

    Scaffold(
        containerColor = CosmicBlack,
        topBar = {
            ExecutiveTopBar(
                title = when {
                    isPrincipal -> "Supervisi Ujian & CBT"
                    isTeacher -> "Kendali Kuis & Ujian CBT"
                    else -> "Portal Ujian & CBT Siswa"
                },
                subtitle = when {
                    isPrincipal -> "Token, jadwal & pantauan room proktor seluruh rombel"
                    isTeacher -> "Kelola jadwal, bagikan token & pantau peserta realtime"
                    else -> "Jadwal ujian CBT, persiapan token & hasil evaluasi"
                },
                onBack = onBack,
                actions = {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(primaryAccent.copy(alpha = 0.12f))
                            .border(0.5.dp, primaryAccent.copy(alpha = 0.25f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                    ) {
                        Text(
                            text = resolveRoleDisplay(
                                if (isPrincipal) "principal" else if (isTeacher) "teacher" else "student",
                                isFormalUpper = true,
                            ),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryAccent,
                            letterSpacing = 0.5.sp,
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            if (isTeacher) {
                val speedDialCardBg = if (isDark) Color(0xFF141416) else Color.White
                val speedDialBorder = if (isDark) Color(0xFF27272A) else Color(0xFFE2E8F0)
                val primaryColor = if (isDark) Color(0xFF059669) else Color(0xFF10B981)
                val primaryBorder = if (isDark) Color(0xFF34D399).copy(alpha = 0.35f) else Color(0xFF059669).copy(alpha = 0.20f)
                val rotation by animateFloatAsState(
                    targetValue = if (isSpeedDialOpen) 135f else 0f,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "fabRotation",
                )

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.navigationBarsPadding().padding(bottom = 8.dp),
                ) {
                    AnimatedVisibility(
                        visible = isSpeedDialOpen,
                        enter = fadeIn(tween(180)) + slideInVertically(tween(180)) { it / 2 },
                        exit = fadeOut(tween(140)) + slideOutVertically(tween(140)) { it / 2 },
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    isSpeedDialOpen = false
                                    onCreateQuiz()
                                },
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = speedDialCardBg,
                                border = BorderStroke(1.dp, speedDialBorder),
                                shadowElevation = 0.dp,
                            ) {
                                Text(
                                    text = "Buat Kuis / Ujian",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color(0xFF34D399) else Color(0xFF047857),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
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
                                    Icon(Icons.Default.Add, contentDescription = "Buat Kuis", tint = Color.White, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }

                    Surface(
                        onClick = { isSpeedDialOpen = !isSpeedDialOpen },
                        shape = CircleShape,
                        color = if (isSpeedDialOpen) (if (isDark) Color(0xFF27272A) else Color(0xFFF1F5F9)) else primaryColor,
                        border = BorderStroke(1.dp, if (isSpeedDialOpen) speedDialBorder else primaryBorder),
                        shadowElevation = 0.dp,
                        modifier = Modifier.size(52.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isSpeedDialOpen) Icons.Default.Close else Icons.Default.Add,
                                contentDescription = if (isSpeedDialOpen) "Tutup Menu" else "Buat Kuis",
                                tint = if (isSpeedDialOpen) (if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A)) else Color.White,
                                modifier = Modifier
                                    .size(22.dp)
                                    .graphicsLayer { rotationZ = rotation },
                            )
                        }
                    }
                }
            }
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            PullRefreshContainer(
                isRefreshing = state.isRefreshing,
                onRefresh = { viewModel.refresh(isPullRefresh = true) },
                modifier = Modifier.fillMaxSize(),
            ) {
                when {
                    state.isLoading -> LoadingState()
                    state.error != null -> ErrorState(message = state.error!!, onRetry = viewModel::forceRefresh)
                    else -> {
                        AdaptiveQuizWidthContainer(maxWidth = 1160.dp) { widthClass ->
                            val columns = if (widthClass == QuizWindowWidthClass.COMPACT) 1 else 2
                            val hPadding = widthClass.contentHorizontalPadding()

                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(
                                    start = hPadding,
                                    end = hPadding,
                                    top = 12.dp,
                                    bottom = 90.dp,
                                ),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                // Active route filters
                                if (state.subjectFilter != null || state.classFilter != null || state.dateFilter != null) {
                                    item(key = "nav-filters") {
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
                                                    onShowAllArchive = { viewModel.clearDateFilter() },
                                                )
                                            }
                                        }
                                    }
                                }

                                // Hero / Spotlight Banner
                                item(key = "hero-banner") {
                                    if (isTeacher) {
                                        TeacherProctorHeroBanner(
                                            isPrincipal = isPrincipal,
                                            quizzes = state.quizzes,
                                            schedule = state.proctorSchedule,
                                            widthClass = widthClass,
                                        )
                                    } else {
                                        val spotlightQuiz = state.quizzes.firstOrNull { q ->
                                            val done = q.studentHasCompleted || q.studentAttemptStatus?.lowercase() in listOf("completed", "submitted", "graded")
                                            !done && (q.effectivePhase == "LIVE" || q.effectivePhase == "UPCOMING")
                                        }
                                        StudentQuizHeroBanner(
                                            activeCount = state.quizzes.count { it.isCurrentlyOpenForExam },
                                            upcomingCount = state.quizzes.count {
                                                !it.studentHasCompleted && it.effectivePhase == "UPCOMING"
                                            },
                                            doneCount = state.quizzes.count {
                                                it.studentHasCompleted ||
                                                    it.studentAttemptStatus?.lowercase() in listOf("completed", "submitted", "graded") ||
                                                    it.effectivePhase == "ENDED"
                                            },
                                            totalCount = state.quizzes.size,
                                            spotlightQuiz = spotlightQuiz,
                                            nowMs = nowMs,
                                            onOpenSpotlight = { qId -> onQuizClick(qId) },
                                        )
                                    }
                                }

                                // Staff Primary Mode Tabs: Daftar & Token vs Jadwal & Pantau
                                if (isTeacher) {
                                    item(key = "staff-mode-tabs") {
                                        StaffHubModeTabs(
                                            selectedTab = staffMainTab,
                                            onSelectTab = { staffMainTab = it },
                                            scheduleCount = state.proctorSchedule?.sessions?.size ?: state.quizzes.size,
                                            conflictCount = state.proctorSchedule?.summary?.conflictCount ?: 0,
                                        )
                                    }
                                }

                                // Search + Status Filter Chips
                                item(key = "search-and-filter") {
                                    QuizSearchAndFilterBar(
                                        query = searchQuery,
                                        onQueryChange = { searchQuery = it },
                                        selectedStatus = statusFilter,
                                        onSelectStatus = { statusFilter = it },
                                        isTeacher = isTeacher,
                                        quizzes = state.quizzes,
                                    )
                                }

                                if (isTeacher && staffMainTab == 1) {
                                    if (filteredScheduleSessions.isEmpty()) {
                                        item(key = "empty-schedule") {
                                            EmptyQuizStateCard(
                                                title = "Belum Ada Jadwal Ujian",
                                                subtitle = "Tidak ada sesi ujian CBT yang cocok dengan filter pencarian Anda.",
                                                cardBg = cardBg,
                                                cardBorder = cardBorder,
                                                innerCardBg = innerCardBg,
                                                innerCardBorder = innerCardBorder,
                                                textPrimary = textPrimary,
                                                textSecondary = textSecondary,
                                                textTertiary = textTertiary,
                                            )
                                        }
                                    } else {
                                        proctorScheduleTabContent(
                                            dashboard = state.proctorSchedule,
                                            filteredSessions = filteredScheduleSessions,
                                            columns = columns,
                                            nowMs = nowMs,
                                            onQuizClick = onQuizClick,
                                        )
                                    }
                                } else if (statusMatchedQuizzes.isEmpty()) {
                                    item(key = "empty-quizzes") {
                                        EmptyQuizStateCard(
                                            title = if (state.quizzes.isEmpty()) "Belum Ada Paket Ujian" else "Tidak Ditemukan",
                                            subtitle = if (state.quizzes.isEmpty()) {
                                                if (isTeacher) "Buat paket kuis atau ujian baru melalui tombol + di pojok kanan bawah"
                                                else "Guru belum menerbitkan jadwal kuis atau ujian CBT untuk kelas Anda"
                                            } else {
                                                "Ubah kata kunci pencarian atau pilih filter status lainnya."
                                            },
                                            cardBg = cardBg,
                                            cardBorder = cardBorder,
                                            innerCardBg = innerCardBg,
                                            innerCardBorder = innerCardBorder,
                                            textPrimary = textPrimary,
                                            textSecondary = textSecondary,
                                            textTertiary = textTertiary,
                                        )
                                    }
                                } else if (isTeacher) {
                                    teacherQuizListContent(
                                        liveQuizzes = liveStaffQuizzes,
                                        upcomingQuizzes = upcomingStaffQuizzes,
                                        draftQuizzes = draftStaffQuizzes,
                                        endedQuizzes = endedStaffQuizzes,
                                        sessionsById = sessionsById,
                                        columns = columns,
                                        nowMs = nowMs,
                                        onQuizClick = onQuizClick,
                                    )
                                } else {
                                    studentQuizListContent(
                                        activeQuizzes = activeStudentQuizzes,
                                        upcomingQuizzes = upcomingStudentQuizzes,
                                        doneQuizzes = doneStudentQuizzes,
                                        columns = columns,
                                        nowMs = nowMs,
                                        onQuizClick = onQuizClick,
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
private fun StaffHubModeTabs(
    selectedTab: Int,
    onSelectTab: (Int) -> Unit,
    scheduleCount: Int,
    conflictCount: Int,
) {
    val isDark = LocalIsDarkTheme.current
    val trackBg = if (isDark) Color(0xFF141416) else Color(0xFFF1F5F9)
    val trackBorder = if (isDark) Color(0xFF27272A) else Color(0xFFE2E8F0)
    val activeBg = if (isDark) Color(0xFF064E3B) else Color.White
    val activeText = if (isDark) Color(0xFF34D399) else Color(0xFF059669)
    val inactiveText = if (isDark) Color(0xFFA1A1AA) else Color(0xFF64748B)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = trackBg,
        border = BorderStroke(1.dp, trackBorder),
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            listOf(
                Triple(0, Icons.Rounded.Key, "Daftar & Token"),
                Triple(1, Icons.Rounded.CalendarMonth, "Jadwal & Pantau ($scheduleCount)"),
            ).forEach { (idx, icon, label) ->
                val isSelected = selectedTab == idx
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onSelectTab(idx) },
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) activeBg else Color.Transparent,
                    border = if (isSelected) BorderStroke(1.dp, activeText.copy(alpha = 0.35f)) else null,
                    shadowElevation = if (isSelected && !isDark) 1.dp else 0.dp,
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 9.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isSelected) activeText else inactiveText,
                            modifier = Modifier.size(15.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                            color = if (isSelected) activeText else inactiveText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (idx == 1 && conflictCount > 0) {
                            Spacer(Modifier.width(5.dp))
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(NeonError)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuizSearchAndFilterBar(
    query: String,
    onQueryChange: (String) -> Unit,
    selectedStatus: String,
    onSelectStatus: (String) -> Unit,
    isTeacher: Boolean,
    quizzes: List<Quiz>,
) {
    val isDark = LocalIsDarkTheme.current
    val inputBg = if (isDark) Color(0xFF141416) else Color.White
    val inputBorder = if (isDark) Color(0xFF27272A) else Color(0xFFE2E8F0)
    val textPrimary = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFFA1A1AA) else Color(0xFF64748B)
    val accent = if (isTeacher) TeacherNeon else StudentNeon

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            placeholder = {
                Text(
                    text = if (isTeacher) {
                        "Cari judul ujian, mapel, kelas (PAKET A), guru, atau token..."
                    } else {
                        "Cari ujian atau mata pelajaran..."
                    },
                    fontSize = 12.5.sp,
                    color = textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            leadingIcon = {
                Icon(Icons.Rounded.Search, contentDescription = null, tint = textSecondary, modifier = Modifier.size(18.dp))
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Hapus Pencarian", tint = textSecondary, modifier = Modifier.size(16.dp))
                    }
                }
            },
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = inputBg,
                unfocusedContainerColor = inputBg,
                focusedBorderColor = accent,
                unfocusedBorderColor = inputBorder,
                focusedTextColor = textPrimary,
                unfocusedTextColor = textPrimary,
            ),
            modifier = Modifier.fillMaxWidth(),
        )

        val liveCount = quizzes.count { it.effectivePhase == "LIVE" }
        val upcomingCount = quizzes.count { it.effectivePhase == "UPCOMING" }
        val draftCount = quizzes.count { it.effectivePhase == "DRAFT" }
        val endedCount = quizzes.count { it.effectivePhase == "ENDED" }

        val chips = if (isTeacher) {
            listOf(
                "ALL" to "Semua (${quizzes.size})",
                "LIVE" to "Live ($liveCount)",
                "UPCOMING" to "Terjadwal ($upcomingCount)",
                "DRAFT" to "Draf ($draftCount)",
                "ENDED" to "Berakhir ($endedCount)",
            )
        } else {
            listOf(
                "ALL" to "Semua (${quizzes.size})",
                "LIVE" to "Dibuka ($liveCount)",
                "UPCOMING" to "Terjadwal ($upcomingCount)",
                "ENDED" to "Selesai ($endedCount)",
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            chips.forEach { (key, label) ->
                val isSelected = selectedStatus == key
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onSelectStatus(key) },
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) accent.copy(alpha = 0.14f) else inputBg,
                    border = BorderStroke(1.dp, if (isSelected) accent else inputBorder),
                ) {
                    Text(
                        text = label,
                        fontSize = 11.5.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) accent else textSecondary,
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                        maxLines = 1,
                        softWrap = false,
                    )
                }
            }
        }
    }
}

@Composable
private fun StudentQuizHeroBanner(
    activeCount: Int,
    upcomingCount: Int,
    doneCount: Int,
    totalCount: Int,
    spotlightQuiz: Quiz?,
    nowMs: Long,
    onOpenSpotlight: (String) -> Unit,
) {
    val isDark = LocalIsDarkTheme.current
    val cardBg = if (isDark) Color(0xFF111113) else Color.White
    val cardBorder = if (isDark) Color(0xFF1F1F23) else Color(0xFFE2E8F0)
    val innerCardBg = if (isDark) Color(0xFF161618) else Color(0xFFF8FAFC)
    val innerCardBorder = if (isDark) Color(0xFF27272A) else Color(0xFFE2E8F0)
    val textPrimary = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFFA1A1AA) else Color(0xFF64748B)
    val textTertiary = if (isDark) Color(0xFF71717A) else Color(0xFF94A3B8)
    val studentAccent = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)

    val progressFraction = if (totalCount > 0) doneCount.toFloat() / totalCount.toFloat() else 0f
    val animatedProgress by animateFloatAsState(targetValue = progressFraction, label = "quizProgress")
    val completionPercent = (progressFraction * 100).toInt()
    val allDone = activeCount == 0 && upcomingCount == 0 && totalCount > 0

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder),
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = studentAccent.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, studentAccent.copy(alpha = 0.35f)),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Icon(Icons.Rounded.FactCheck, contentDescription = null, tint = studentAccent, modifier = Modifier.size(14.dp))
                        Text("PORTAL UJIAN & CBT", color = studentAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (allDone) (if (isDark) Color(0xFF064E3B) else Color(0xFFDCFCE7)) else innerCardBg,
                    border = BorderStroke(1.dp, if (allDone) NeonSuccess.copy(alpha = 0.5f) else innerCardBorder),
                ) {
                    Text(
                        text = if (allDone) "Semua Selesai" else "$completionPercent% Tuntas",
                        color = if (allDone) NeonSuccess else textSecondary,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                    )
                }
            }

            if (spotlightQuiz != null) {
                val spPhase = spotlightQuiz.effectivePhase
                val spWindow = formatExamWindowWib(spotlightQuiz.startAt, spotlightQuiz.endAt, spotlightQuiz.releaseAt)
                val spCountdown = if (spPhase == "LIVE") {
                    computeSecondsRemaining(spotlightQuiz.endAt, spotlightQuiz.secondsRemaining, nowMs)?.let { "Berakhir ${formatCountdownHuman(it)}" }
                } else {
                    computeSecondsUntilStart(spotlightQuiz.startAt, spotlightQuiz.releaseAt, spotlightQuiz.secondsUntilStart, nowMs)?.let { "Dimulai ${formatCountdownHuman(it)}" }
                }
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onOpenSpotlight(spotlightQuiz.id) },
                    shape = RoundedCornerShape(14.dp),
                    color = (if (spPhase == "LIVE") studentAccent else NeonWarning).copy(alpha = 0.11f),
                    border = BorderStroke(1.dp, (if (spPhase == "LIVE") studentAccent else NeonWarning).copy(alpha = 0.4f)),
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(
                                text = if (spPhase == "LIVE") "UJIAN SEDANG BERLANGSUNG" else "UJIAN BERIKUTNYA",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (spPhase == "LIVE") studentAccent else NeonWarning,
                                letterSpacing = 0.4.sp,
                            )
                            Text(
                                text = spotlightQuiz.title,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = listOfNotNull(spWindow.combinedLabel, spCountdown).joinToString(" • "),
                                fontSize = 11.sp,
                                color = textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = { onOpenSpotlight(spotlightQuiz.id) },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (spPhase == "LIVE") studentAccent else NeonWarning,
                                contentColor = Color.White,
                            ),
                            modifier = Modifier.height(34.dp),
                        ) {
                            if (spPhase == "LIVE") {
                                Icon(Icons.Rounded.PlayArrow, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(2.dp))
                            }
                            Text(if (spPhase == "LIVE") "Mulai" else "Jadwal", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ModernQuizStatCard(
                    title = "Aktif Live",
                    value = "$activeCount",
                    icon = Icons.Rounded.PlayArrow,
                    valueColor = if (activeCount > 0) studentAccent else textSecondary,
                    innerCardBg = innerCardBg,
                    innerCardBorder = innerCardBorder,
                    textTertiary = textTertiary,
                    modifier = Modifier.weight(1f),
                )
                ModernQuizStatCard(
                    title = "Terjadwal",
                    value = "$upcomingCount",
                    icon = Icons.Rounded.CalendarMonth,
                    valueColor = if (upcomingCount > 0) NeonWarning else textSecondary,
                    innerCardBg = innerCardBg,
                    innerCardBorder = innerCardBorder,
                    textTertiary = textTertiary,
                    modifier = Modifier.weight(1f),
                )
                ModernQuizStatCard(
                    title = "Selesai",
                    value = "$doneCount",
                    icon = Icons.Rounded.FactCheck,
                    valueColor = NeonSuccess,
                    innerCardBg = innerCardBg,
                    innerCardBorder = innerCardBorder,
                    textTertiary = textTertiary,
                    modifier = Modifier.weight(1f),
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(innerCardBorder),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(3.dp))
                        .background(studentAccent),
                )
            }
        }
    }
}

@Composable
private fun TeacherProctorHeroBanner(
    isPrincipal: Boolean,
    quizzes: List<Quiz>,
    schedule: QuizProctorScheduleDashboard?,
    widthClass: QuizWindowWidthClass,
) {
    val isDark = LocalIsDarkTheme.current
    val cardBg = if (isDark) Color(0xFF111113) else Color.White
    val cardBorder = if (isDark) Color(0xFF1F1F23) else Color(0xFFE2E8F0)
    val innerCardBg = if (isDark) Color(0xFF161618) else Color(0xFFF8FAFC)
    val innerCardBorder = if (isDark) Color(0xFF27272A) else Color(0xFFE2E8F0)
    val textPrimary = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFFA1A1AA) else Color(0xFF64748B)
    val textTertiary = if (isDark) Color(0xFF71717A) else Color(0xFF94A3B8)
    val teacherAccent = if (isDark) Color(0xFF10B981) else Color(0xFF059669)
    val warningColor = if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706)

    val liveCount = schedule?.summary?.liveCount ?: quizzes.count { it.effectivePhase == "LIVE" }
    val upcomingCount = schedule?.summary?.upcomingCount ?: quizzes.count { it.effectivePhase == "UPCOMING" }
    val tokenCount = quizzes.count { !it.examToken.isNullOrBlank() }
    val flaggedOrConflict = (schedule?.summary?.flaggedStudentsTotal ?: 0L) + (schedule?.summary?.conflictCount?.toLong() ?: 0L)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder),
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = teacherAccent.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, teacherAccent.copy(alpha = 0.35f)),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Icon(Icons.Rounded.MonitorHeart, contentDescription = null, tint = teacherAccent, modifier = Modifier.size(14.dp))
                        Text(
                            text = if (isPrincipal) "SUPERVISI PROKTOR & CBT" else "KENDALI PROKTOR & CBT",
                            color = teacherAccent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = innerCardBg,
                    border = BorderStroke(1.dp, innerCardBorder),
                ) {
                    Text(
                        text = "${quizzes.size} Paket Ujian",
                        color = textSecondary,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = if (isPrincipal) "Pusat Kendali Ujian Sekolah" else "Manajemen Sesi & Token CBT",
                    color = textPrimary,
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Lihat & salin token, cek jadwal bentrok, serta pantau kehadiran dan pelanggaran siswa langsung dari perangkat Anda.",
                    color = textSecondary,
                    fontSize = 11.5.sp,
                    lineHeight = 16.sp,
                )
            }

            if (widthClass == QuizWindowWidthClass.COMPACT) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ModernQuizStatCard("Sedang Live", "$liveCount", Icons.Rounded.MonitorHeart, teacherAccent, innerCardBg, innerCardBorder, textTertiary, Modifier.weight(1f))
                        ModernQuizStatCard("Terjadwal", "$upcomingCount", Icons.Rounded.CalendarMonth, warningColor, innerCardBg, innerCardBorder, textTertiary, Modifier.weight(1f))
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ModernQuizStatCard("Token Siap", "$tokenCount", Icons.Rounded.Key, NeonBlue, innerCardBg, innerCardBorder, textTertiary, Modifier.weight(1f))
                        ModernQuizStatCard(
                            title = if (flaggedOrConflict > 0L) "Alert / Bentrok" else "Aman",
                            value = "$flaggedOrConflict",
                            icon = Icons.Rounded.FactCheck,
                            valueColor = if (flaggedOrConflict > 0L) NeonError else textPrimary,
                            innerCardBg = innerCardBg,
                            innerCardBorder = innerCardBorder,
                            textTertiary = textTertiary,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            } else {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ModernQuizStatCard("Sedang Live", "$liveCount", Icons.Rounded.MonitorHeart, teacherAccent, innerCardBg, innerCardBorder, textTertiary, Modifier.weight(1f))
                    ModernQuizStatCard("Terjadwal", "$upcomingCount", Icons.Rounded.CalendarMonth, warningColor, innerCardBg, innerCardBorder, textTertiary, Modifier.weight(1f))
                    ModernQuizStatCard("Token Siap", "$tokenCount", Icons.Rounded.Key, NeonBlue, innerCardBg, innerCardBorder, textTertiary, Modifier.weight(1f))
                    ModernQuizStatCard(
                        title = if (flaggedOrConflict > 0L) "Alert / Bentrok" else "Total Ujian",
                        value = if (flaggedOrConflict > 0L) "$flaggedOrConflict" else "${quizzes.size}",
                        icon = Icons.Rounded.FactCheck,
                        valueColor = if (flaggedOrConflict > 0L) NeonError else textPrimary,
                        innerCardBg = innerCardBg,
                        innerCardBorder = innerCardBorder,
                        textTertiary = textTertiary,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun ModernQuizStatCard(
    title: String,
    value: String,
    icon: ImageVector,
    valueColor: Color,
    innerCardBg: Color,
    innerCardBorder: Color,
    textTertiary: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.height(60.dp),
        shape = RoundedCornerShape(14.dp),
        color = innerCardBg,
        border = BorderStroke(1.dp, innerCardBorder),
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = icon, contentDescription = null, tint = valueColor, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(5.dp))
                Text(
                    text = value,
                    color = valueColor,
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text = title,
                color = textTertiary,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun EmptyQuizStateCard(
    title: String,
    subtitle: String,
    cardBg: Color,
    cardBorder: Color,
    innerCardBg: Color,
    innerCardBorder: Color,
    textPrimary: Color,
    textSecondary: Color,
    textTertiary: Color,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(cardBg)
            .border(1.dp, cardBorder, RoundedCornerShape(22.dp))
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(innerCardBg)
                    .border(1.dp, innerCardBorder, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.FactCheck, contentDescription = null, tint = textTertiary, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text(text = title, color = textPrimary, fontSize = 15.5.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(
                text = subtitle,
                color = textSecondary,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                lineHeight = 17.sp,
            )
        }
    }
}
