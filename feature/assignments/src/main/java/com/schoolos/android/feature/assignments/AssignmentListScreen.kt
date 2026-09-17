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
        Box(modifier = Modifier.fillMaxSize()) {
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
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding(),
                        contentPadding = PaddingValues(
                            start = 10.dp,
                            end = 10.dp,
                            top = 8.dp,
                            bottom = 100.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
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

// ── SPEED DIAL FAB ─────────────────────────────────────────────────────────────

@Composable
private fun TeacherSpeedDialFab(
    expanded: Boolean,
    onToggle: () -> Unit,
    onCreateAssignment: () -> Unit,
    onCreateQuiz: () -> Unit,
) {
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 45f else 0f,
        animationSpec = tween(250),
        label = "fab_rotation"
    )

    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Sub-actions — animate in/out
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(tween(200)) + expandVertically(tween(200), expandFrom = Alignment.Bottom),
            exit = fadeOut(tween(150)) + shrinkVertically(tween(150), shrinkTowards = Alignment.Bottom)
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SpeedDialOption(
                    label = "Buat Kuis",
                    icon = Icons.Default.Quiz,
                    containerColor = NeonBlue,
                    onClick = onCreateQuiz
                )
                SpeedDialOption(
                    label = "Buat Tugas",
                    icon = Icons.Default.Edit,
                    containerColor = TeacherNeon,
                    onClick = onCreateAssignment
                )
            }
        }

        // Main FAB
        FloatingActionButton(
            onClick = onToggle,
            containerColor = if (expanded) CosmicNavy else TeacherNeon,
            contentColor = if (expanded) TeacherNeon else Color.White,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(18.dp),
                spotColor = TeacherNeon.copy(alpha = 0.4f)
            )
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = if (expanded) "Tutup" else "Buat",
                modifier = Modifier.rotate(rotation)
            )
        }
    }
}

@Composable
private fun SpeedDialOption(
    label: String,
    icon: ImageVector,
    containerColor: Color,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End
    ) {
        // Label chip
        Box(
            modifier = Modifier
                .shadow(4.dp, RoundedCornerShape(10.dp), spotColor = containerColor.copy(alpha = 0.3f))
                .clip(RoundedCornerShape(10.dp))
                .background(CosmicNavy)
                .border(1.dp, containerColor.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = containerColor
            )
        }

        Spacer(Modifier.width(10.dp))

        // Mini FAB
        SmallFloatingActionButton(
            onClick = onClick,
            containerColor = containerColor,
            contentColor = Color.White,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(14.dp),
                spotColor = containerColor.copy(alpha = 0.4f)
            )
        ) {
            Icon(icon, contentDescription = label, modifier = Modifier.size(20.dp))
        }
    }
}

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
            .shadow(10.dp, RoundedCornerShape(26.dp), spotColor = Color(0x354F46E5), ambientColor = Color(0x204F46E5))
            .clip(RoundedCornerShape(26.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        Color(0xFF4338CA), // Deep Indigo
                        Color(0xFF6D28D9), // Rich Violet
                        Color(0xFF7C3AED), // Vibrant Purple
                    )
                )
            )
    ) {
        // Decorative background geometric accents for depth
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 24.dp, y = (-20).dp)
                .size(130.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.08f))
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = (-16).dp, y = 16.dp)
                .size(80.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.06f))
        )

        // Content
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            // Top Tag Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White.copy(alpha = 0.18f))
                        .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text("📝", fontSize = 12.sp)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "PORTAL TUGAS & PROYEK",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.2.sp
                    )
                }

                // Progress Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.18f))
                        .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        "$completionPercent% Selesai",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            val child = if (childName.isNotBlank()) childName else "Anak"
            Text(
                text = if (isParent) "Status Tugas $child" else "Daftar Tugas Kamu",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                lineHeight = 30.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Pantau perkembangan tugas dan selesaikan tepat waktu",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(Modifier.height(18.dp))

            // 3-Metric Stat Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                HeroMetricCard(
                    label = "BELUM",
                    value = "${activeCount + dueSoonCount}",
                    subtitle = "Tugas aktif",
                    accent = Color.White,
                    modifier = Modifier.weight(1f)
                )
                HeroMetricCard(
                    label = "SEGERA",
                    value = "$dueSoonCount",
                    subtitle = "Mendesak",
                    accent = if (dueSoonCount > 0) Color(0xFFFDE047) else Color.White,
                    modifier = Modifier.weight(1f)
                )
                HeroMetricCard(
                    label = "SELESAI",
                    value = "$completedCount",
                    subtitle = "Terkumpul",
                    accent = Color(0xFF86EFAC),
                    modifier = Modifier.weight(1f)
                )
            }

            // High-Contrast Alert Banner if tasks are due soon
            if (dueSoonCount > 0) {
                Spacer(Modifier.height(14.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFBE123C).copy(alpha = 0.35f))
                        .border(1.dp, Color(0xFFF43F5E).copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF43F5E))
                                .border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("⚡", fontSize = 14.sp)
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "$dueSoonCount tugas mendekati batas waktu!",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                "Kumpulkan segera agar tidak terlambat",
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TeacherAssignmentHeroHeader(
    activeCount: Int,
    pendingGradeCount: Int,
    quizActiveCount: Int = 0,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(10.dp, RoundedCornerShape(26.dp), spotColor = Color(0x35059669), ambientColor = Color(0x20059669))
            .clip(RoundedCornerShape(26.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        Color(0xFF047857), // Emerald Dark
                        Color(0xFF059669), // Emerald
                        Color(0xFF0D9488), // Teal
                    )
                )
            )
    ) {
        // Decorative circle
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(120.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.07f))
        )

        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White.copy(alpha = 0.18f))
                        .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text("👨‍🏫", fontSize = 13.sp)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "PORTAL EVALUASI",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.2.sp
                    )
                }

                // Speed Dial hint badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.16f))
                        .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("✚", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black)
                        Spacer(Modifier.width(5.dp))
                        Text(
                            "Tugas / Kuis",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            Text(
                "Tugas & Kuis Siswa",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                lineHeight = 30.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Kelola penugasan, kuis, dan beri penilaian tepat waktu",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(Modifier.height(18.dp))

            // 3 Metric Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HeroMetricCard(
                    label = "TUGAS AKTIF",
                    value = "$activeCount",
                    subtitle = "Berjalan",
                    accent = Color.White,
                    modifier = Modifier.weight(1f)
                )
                HeroMetricCard(
                    label = "PERLU NILAI",
                    value = "$pendingGradeCount",
                    subtitle = "Menunggu",
                    accent = if (pendingGradeCount > 0) Color(0xFFFDE047) else Color.White,
                    modifier = Modifier.weight(1f)
                )
                HeroMetricCard(
                    label = "KUIS AKTIF",
                    value = "$quizActiveCount",
                    subtitle = "Berlangsung",
                    accent = if (quizActiveCount > 0) Color(0xFF93C5FD) else Color.White,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

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
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.16f))
            .border(1.dp, Color.White.copy(alpha = 0.28f), RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = value,
            color = accent,
            fontSize = 24.sp,
            fontWeight = FontWeight.Black
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = subtitle,
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
    }
}

@Composable
private fun TeacherEmptyState(
    onCreateAssignment: () -> Unit,
    onCreateQuiz: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Info card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(4.dp, RoundedCornerShape(24.dp), spotColor = GlassOverlay)
                .clip(RoundedCornerShape(24.dp))
                .background(CosmicNavy)
                .border(1.dp, GlassBorder, RoundedCornerShape(24.dp))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(TeacherNeon.copy(alpha = 0.1f))
                        .border(1.dp, TeacherNeon.copy(alpha = 0.2f), RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("📋", fontSize = 28.sp)
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    "Belum Ada Penugasan",
                    color = TextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Black
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Mulai dengan membuat tugas atau kuis baru\nuntuk siswa di kelas Anda",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 19.sp
                )
            }
        }

        // 2 action cards side by side — no duplicate button inside
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TeacherActionCard(
                emoji = "📝",
                title = "Buat Tugas",
                subtitle = "Penugasan teks\natau file upload",
                accentColor = TeacherNeon,
                onClick = onCreateAssignment,
                modifier = Modifier.weight(1f)
            )
            TeacherActionCard(
                emoji = "🧠",
                title = "Buat Kuis",
                subtitle = "Pilihan ganda\ndengan batas waktu",
                accentColor = NeonBlue,
                onClick = onCreateQuiz,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun TeacherActionCard(
    emoji: String,
    title: String,
    subtitle: String,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .shadow(4.dp, RoundedCornerShape(20.dp), spotColor = accentColor.copy(alpha = 0.2f))
            .clip(RoundedCornerShape(20.dp))
            .background(CosmicNavy)
            .border(1.dp, accentColor.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(accentColor.copy(alpha = 0.12f))
                    .border(1.dp, accentColor.copy(alpha = 0.25f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(emoji, fontSize = 22.sp)
            }
            Spacer(Modifier.height(10.dp))
            Text(
                title,
                color = accentColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            Text(
                subtitle,
                color = TextTertiary,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                lineHeight = 15.sp
            )
        }
    }
}

@Composable
private fun AssignmentTabFilter(
    isTeacher: Boolean,
    selectedTab: String,
    onTabSelected: (String) -> Unit,
    counts: Map<String, Int> = emptyMap(),
) {
    val tabs = if (isTeacher) listOf("Semua", "Aktif", "Perlu Dinilai")
               else listOf("Semua", "Segera", "Aktif", "Selesai")

    val activeAccent = if (isTeacher) TeacherNeon else StudentNeon

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        items(tabs) { tab ->
            val isSelected = tab == selectedTab
            val count = counts[tab] ?: 0

            val tabColor = when (tab) {
                "Segera", "Perlu Dinilai" -> NeonError
                "Aktif"                  -> activeAccent
                "Selesai"                -> NeonSuccess
                else                     -> activeAccent
            }

            val backgroundColor = if (isSelected) tabColor else CosmicNavy
            val contentColor = if (isSelected) Color.White else TextSecondary
            val borderColor = if (isSelected) tabColor else GlassBorder

            Box(
                modifier = Modifier
                    .shadow(
                        elevation = if (isSelected) 4.dp else 0.dp,
                        shape = RoundedCornerShape(14.dp),
                        spotColor = tabColor.copy(alpha = 0.4f)
                    )
                    .clip(RoundedCornerShape(14.dp))
                    .background(backgroundColor)
                    .border(1.dp, borderColor, RoundedCornerShape(14.dp))
                    .clickable { onTabSelected(tab) }
                    .padding(horizontal = 14.dp, vertical = 9.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = tab,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.SemiBold,
                        color = contentColor,
                    )
                    // Only show count badge when there's something to show
                    if (count > 0) {
                        Spacer(Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) Color.White.copy(alpha = 0.25f) else CosmicDark)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "$count",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isSelected) Color.White else TextTertiary
                            )
                        }
                    }
                }
            }
        }
    }
}

