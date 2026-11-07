package com.schoolos.android.feature.quizzes

import com.schoolos.android.core.R as CoreR
import androidx.compose.ui.res.painterResource

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.schoolos.android.core.designsystem.*

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
    val isPrincipal = com.schoolos.android.core.auth.isPrincipalRole(state.userRole)
    val isTeacher = com.schoolos.android.core.auth.isTeacherRole(state.userRole) || isPrincipal
    var isSpeedDialOpen by remember { mutableStateOf(false) }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
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

    val scheduledQuizzes = if (isTeacher) {
        state.quizzes.filter { it.isScheduled || it.status.lowercase() == "draft" }
    } else emptyList()

    val doneQuizzes = state.quizzes.filter {
        it !in scheduledQuizzes && (
            it.studentHasCompleted ||
            it.studentAttemptStatus?.lowercase() in listOf("completed", "submitted", "graded") ||
            it.status.lowercase() in listOf("closed", "archived", "graded", "submitted", "completed")
        )
    }
    val activeQuizzes = state.quizzes.filter {
        it !in scheduledQuizzes && it !in doneQuizzes && (isTeacher || it.status.lowercase() in listOf("active", "open", "published"))
    }

    val isDark = LocalIsDarkTheme.current
    val cardBg = if (isDark) Color(0xFF111113) else Color.White
    val cardBorder = if (isDark) Color(0xFF1F1F23) else Color(0xFFE2E8F0)
    val innerCardBg = if (isDark) Color(0xFF161618) else Color(0xFFF8FAFC)
    val innerCardBorder = if (isDark) Color(0xFF27272A) else Color(0xFFE2E8F0)
    val textPrimary = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFFA1A1AA) else Color(0xFF64748B)
    val textTertiary = if (isDark) Color(0xFF71717A) else Color(0xFF94A3B8)

    Scaffold(
        containerColor = CosmicBlack,
        topBar = {
            ExecutiveTopBar(
                title = if (isPrincipal) "Monitoring Kuis & CBT" else "Kuis & Evaluasi",
                subtitle = when {
                    isPrincipal -> "Pemantauan pelaksanaan kuis & ujian CBT sekolah"
                    isTeacher -> "Kelola & pantau kuis kelas"
                    else -> "Kerjakan kuis & pantau hasil belajar"
                },
                onBack = onBack,
                actions = {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isTeacher) TeacherNeon.copy(alpha = 0.12f) else StudentNeon.copy(alpha = 0.12f))
                            .border(0.5.dp, if (isTeacher) TeacherNeon.copy(alpha = 0.25f) else StudentNeon.copy(alpha = 0.25f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = com.schoolos.android.core.auth.resolveRoleDisplay(if (isPrincipal) "principal" else if (isTeacher) "teacher" else "student", isFormalUpper = true),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isTeacher) TeacherNeon else StudentNeon,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
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
                    label = "fabRotation"
                )

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.navigationBarsPadding().padding(bottom = 8.dp)
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
                                }
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = speedDialCardBg,
                                border = BorderStroke(1.dp, speedDialBorder),
                                shadowElevation = 0.dp,
                            ) {
                                Text(
                                    text = "Buat Kuis",
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
                                        contentDescription = "Buat Kuis",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Main Executive Floating Button
                    Surface(
                        onClick = { isSpeedDialOpen = !isSpeedDialOpen },
                        shape = CircleShape,
                        color = if (isSpeedDialOpen) (if (isDark) Color(0xFF27272A) else Color(0xFFF1F5F9)) else primaryColor,
                        border = BorderStroke(1.dp, if (isSpeedDialOpen) speedDialBorder else primaryBorder),
                        shadowElevation = 0.dp,
                        modifier = Modifier.size(52.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isSpeedDialOpen) Icons.Default.Close else Icons.Default.Add,
                                contentDescription = if (isSpeedDialOpen) "Tutup Menu" else "Buat Kuis",
                                tint = if (isSpeedDialOpen) (if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A)) else Color.White,
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
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    // Active Subject / Class Filter (from session/schedule)
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

                    // Hero Banner
                    item {
                        if (isTeacher) {
                            TeacherQuizHeroBanner(
                                activeCount = activeQuizzes.size,
                                totalCount = state.quizzes.size
                            )
                        } else {
                            StudentQuizHeroBanner(
                                activeCount = activeQuizzes.size,
                                doneCount = doneQuizzes.size,
                                totalCount = state.quizzes.size
                            )
                        }
                    }

                    // Empty State
                    if (state.quizzes.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(22.dp))
                                    .background(cardBg)
                                    .border(1.dp, cardBorder, RoundedCornerShape(22.dp))
                                    .padding(36.dp),
                                contentAlignment = Alignment.Center
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
                                        Icon(
                                            painter = painterResource(id = CoreR.drawable.ic_modern_quiz),
                                            contentDescription = null,
                                            tint = textTertiary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    Spacer(Modifier.height(12.dp))
                                    Text(
                                        text = "Belum Ada Kuis",
                                        color = textPrimary,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = if (isTeacher) "Buat kuis baru melalui tombol tambah kuis di pojok kanan bawah" else "Guru belum menambahkan kuis untuk kelas ini",
                                        color = textSecondary,
                                        fontSize = 12.sp,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 17.sp
                                    )
                                }
                            }
                        }
                    }

                    // Quiz list content
                    if (isTeacher) {
                        teacherQuizListContent(
                            activeQuizzes = activeQuizzes,
                            doneQuizzes = doneQuizzes,
                            scheduledQuizzes = scheduledQuizzes,
                            onQuizClick = onQuizClick
                        )
                    } else {
                        studentQuizListContent(
                            activeQuizzes = activeQuizzes,
                            doneQuizzes = doneQuizzes,
                            onQuizClick = onQuizClick
                        )
                    }
                }
            }
        }

        // Speed dial scrim
        AnimatedVisibility(
            visible = isSpeedDialOpen,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(150))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.32f))
                    .clickable { isSpeedDialOpen = false }
            )
        }
    }
}
}

// ── STUDENT QUIZ HERO BANNER ──────────────────────────────────────────────────

@Composable
private fun StudentQuizHeroBanner(
    activeCount: Int,
    doneCount: Int,
    totalCount: Int,
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
    val allDone = activeCount == 0 && totalCount > 0

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder),
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = studentAccent.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, studentAccent.copy(alpha = 0.35f))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Icon(
                            painter = painterResource(id = CoreR.drawable.ic_modern_quiz),
                            contentDescription = null,
                            tint = studentAccent,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            "PORTAL EVALUASI & CBT",
                            color = studentAccent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.6.sp
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (allDone) (if (isDark) Color(0xFF064E3B) else Color(0xFFDCFCE7)) else innerCardBg,
                    border = BorderStroke(1.dp, if (allDone) (if (isDark) Color(0xFF10B981).copy(alpha = 0.5f) else Color(0xFF86EFAC)) else innerCardBorder)
                ) {
                    Text(
                        text = if (allDone) "Semua Selesai ✓" else "$completionPercent% Selesai",
                        color = if (allDone) (if (isDark) Color(0xFF34D399) else Color(0xFF15803D)) else textSecondary,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    text = if (allDone) "Semua kuis telah tuntas diselesaikan" else "Uji Pemahaman Materi & CBT",
                    color = textPrimary,
                    fontSize = 15.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = if (activeCount > 0) "$activeCount kuis siap untuk dikerjakan hari ini" else "Pantau hasil evaluasi dan ulas materi pembelajaranmu.",
                    color = textSecondary,
                    fontSize = 11.sp,
                    lineHeight = 14.sp
                )
            }

            // Quick stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ModernQuizStatCard(
                    title = "Total Kuis",
                    value = "$totalCount",
                    iconRes = CoreR.drawable.ic_modern_quiz,
                    valueColor = textPrimary,
                    innerCardBg = innerCardBg,
                    innerCardBorder = innerCardBorder,
                    textTertiary = textTertiary,
                    modifier = Modifier.weight(1f)
                )
                ModernQuizStatCard(
                    title = "Dikerjakan",
                    value = "$doneCount",
                    iconRes = CoreR.drawable.ic_modern_check_circle,
                    valueColor = if (isDark) Color(0xFF34D399) else Color(0xFF16A34A),
                    innerCardBg = innerCardBg,
                    innerCardBorder = innerCardBorder,
                    textTertiary = textTertiary,
                    modifier = Modifier.weight(1f)
                )
                ModernQuizStatCard(
                    title = "Kuis Aktif",
                    value = "$activeCount",
                    iconRes = CoreR.drawable.ic_modern_clock,
                    valueColor = if (activeCount > 0) (if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706)) else textSecondary,
                    innerCardBg = innerCardBg,
                    innerCardBorder = innerCardBorder,
                    textTertiary = textTertiary,
                    modifier = Modifier.weight(1f)
                )
            }

            // Progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(innerCardBorder)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(3.dp))
                        .background(studentAccent)
                )
            }
        }
    }
}

// ── TEACHER QUIZ HERO BANNER ──────────────────────────────────────────────────

@Composable
private fun TeacherQuizHeroBanner(activeCount: Int, totalCount: Int) {
    val isDark = LocalIsDarkTheme.current
    val cardBg = if (isDark) Color(0xFF111113) else Color.White
    val cardBorder = if (isDark) Color(0xFF1F1F23) else Color(0xFFE2E8F0)
    val innerCardBg = if (isDark) Color(0xFF161618) else Color(0xFFF8FAFC)
    val innerCardBorder = if (isDark) Color(0xFF27272A) else Color(0xFFE2E8F0)
    val textPrimary = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFFA1A1AA) else Color(0xFF64748B)
    val textTertiary = if (isDark) Color(0xFF71717A) else Color(0xFF94A3B8)
    val teacherAccent = if (isDark) Color(0xFF10B981) else Color(0xFF059669)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder),
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = teacherAccent.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, teacherAccent.copy(alpha = 0.35f))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Icon(
                            painter = painterResource(id = CoreR.drawable.ic_modern_quiz),
                            contentDescription = null,
                            tint = teacherAccent,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            "MANAJEMEN KUIS & CBT",
                            color = teacherAccent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.6.sp
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = innerCardBg,
                    border = BorderStroke(1.dp, innerCardBorder)
                ) {
                    Text(
                        "$totalCount Total Kuis",
                        color = textSecondary,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    text = "Evaluasi & Kuis CBT Siswa",
                    color = textPrimary,
                    fontSize = 15.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Pantau pengerjaan siswa, berikan koreksi dan distribusi nilai secara realtime.",
                    color = textSecondary,
                    fontSize = 11.sp,
                    lineHeight = 14.sp
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ModernQuizStatCard(
                    title = "Kuis Aktif",
                    value = "$activeCount",
                    iconRes = CoreR.drawable.ic_modern_clock,
                    valueColor = if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706),
                    innerCardBg = innerCardBg,
                    innerCardBorder = innerCardBorder,
                    textTertiary = textTertiary,
                    modifier = Modifier.weight(1f)
                )
                ModernQuizStatCard(
                    title = "Total Dibuat",
                    value = "$totalCount",
                    iconRes = CoreR.drawable.ic_modern_quiz,
                    valueColor = textPrimary,
                    innerCardBg = innerCardBg,
                    innerCardBorder = innerCardBorder,
                    textTertiary = textTertiary,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ModernQuizStatCard(
    title: String,
    value: String,
    iconRes: Int,
    valueColor: Color,
    innerCardBg: Color,
    innerCardBorder: Color,
    textTertiary: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.height(64.dp),
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
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    tint = valueColor,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    text = value,
                    color = valueColor,
                    fontSize = 15.sp,
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
