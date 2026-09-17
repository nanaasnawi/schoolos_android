package com.schoolos.android.feature.quizzes

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
    viewModel: QuizListViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val role = state.userRole.lowercase()
    val isTeacher = role == "teacher" || role == "guru"

    val activeQuizzes = state.quizzes.filter { it.status.lowercase() in listOf("active", "open", "published") }
    val doneQuizzes   = state.quizzes.filter { it.status.lowercase() in listOf("graded", "submitted", "completed") }

    var selectedTab by remember { mutableStateOf("Semua") }

    Scaffold(
        containerColor = CosmicBlack,
        floatingActionButton = {
            if (isTeacher) {
                ExtendedFloatingActionButton(
                    onClick = onCreateQuiz,
                    containerColor = StudentNeon,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(16.dp),
                    icon = { Icon(Icons.Default.Add, null) },
                    text = { Text("Buat Kuis", fontWeight = FontWeight.Bold) }
                )
            }
        }
    ) { padding ->
        PullRefreshContainer(
            isRefreshing = state.isRefreshing,
            onRefresh = viewModel::refresh,
            modifier = Modifier.fillMaxSize(),
        ) {
            if (state.isLoading) {
                LoadingState()
            } else if (state.error != null) {
                ErrorState(message = state.error!!, onRetry = viewModel::refresh)
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding(),
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    // ── TOP APP BAR ──────────────────────────────────────────
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 2.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CustomBackButton(onClick = onBack)
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Kuis & Tes",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Black,
                                        color = TextPrimary,
                                        letterSpacing = (-0.3).sp
                                    )
                                    Text(
                                        text = if (isTeacher) "Kelola & pantau kuis kelas" else "Kerjakan kuis & lihat hasilmu",
                                        fontSize = 11.sp,
                                        color = TextTertiary,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            // Role badge
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isTeacher) TeacherNeon.copy(alpha = 0.12f) else StudentNeon.copy(alpha = 0.12f))
                                    .border(1.dp, if (isTeacher) TeacherNeon.copy(alpha = 0.25f) else StudentNeon.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 9.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (isTeacher) "GURU" else "SISWA",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isTeacher) TeacherNeon else StudentNeon,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    }

                    // ── ACTIVE SUBJECT FILTER ──────────────────────────────
                    if (state.subjectFilter != null) {
                        item {
                            SubjectFilterChip(
                                subject = state.subjectFilter!!,
                                onClear = viewModel::clearSubjectFilter,
                            )
                        }
                    }

                    // ── HERO BANNER ───────────────────────────────────────
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

                    // ── EMPTY STATE ───────────────────────────────────────
                    if (state.quizzes.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .shadow(3.dp, RoundedCornerShape(20.dp), spotColor = GlassOverlay)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(CosmicNavy)
                                    .border(1.dp, GlassBorder, RoundedCornerShape(20.dp))
                                    .padding(36.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("🧠", fontSize = 44.sp)
                                    Spacer(Modifier.height(12.dp))
                                    Text(
                                        text = "Belum Ada Kuis",
                                        color = TextPrimary,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = if (isTeacher) "Buat kuis baru melalui tombol di bawah" else "Guru belum menambahkan kuis untuk kelas ini",
                                        color = TextTertiary,
                                        fontSize = 12.sp,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 17.sp
                                    )
                                }
                            }
                        }
                    }

                    // ── QUIZ LIST CONTENT ─────────────────────────────────
                    if (isTeacher) {
                        teacherQuizListContent(
                            activeQuizzes = activeQuizzes,
                            doneQuizzes = doneQuizzes,
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
    }
}

// ── STUDENT QUIZ HERO BANNER ──────────────────────────────────────────────────

@Composable
private fun StudentQuizHeroBanner(
    activeCount: Int,
    doneCount: Int,
    totalCount: Int,
) {
    val progressFraction = if (totalCount > 0) doneCount.toFloat() / totalCount.toFloat() else 0f
    val animatedProgress by animateFloatAsState(targetValue = progressFraction, label = "quizProgress")
    val completionPercent = (progressFraction * 100).toInt()

    val allDone = activeCount == 0 && totalCount > 0

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(24.dp), spotColor = Color(0x305B21B6), ambientColor = Color(0x155B21B6))
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        Color(0xFF4C1D95), // Deep Violet
                        Color(0xFF7C3AED), // Rich Purple
                        Color(0xFF2563EB), // Electric Blue
                    )
                )
            )
    ) {
        // Decorative circles
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 28.dp, y = (-22).dp)
                .size(130.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.07f))
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = (-18).dp, y = 18.dp)
                .size(80.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.05f))
        )

        Column(modifier = Modifier.padding(16.dp)) {
            // Top Row
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
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text("🧠", fontSize = 12.sp)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "PORTAL KUIS & TES",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.1.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.18f))
                        .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (allDone) "🎉 Semua Selesai!" else "$completionPercent% Selesai",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = if (allDone) "Hebat! Semua kuis dikerjakan" else "Uji kemampuanmu sekarang!",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-0.3).sp
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = if (activeCount > 0) "$activeCount kuis menunggu untuk dikerjakan. Yuk selesaikan!" else "Pantau nilaimu dan pelajari lagi materi yang sulit.",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 12.sp,
                lineHeight = 16.sp
            )

            Spacer(Modifier.height(14.dp))

            // Quick stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuizStatCard(
                    title = "Total Kuis",
                    value = "$totalCount",
                    emoji = "📝",
                    modifier = Modifier.weight(1f)
                )
                QuizStatCard(
                    title = "Dikerjakan",
                    value = "$doneCount",
                    emoji = "✅",
                    valueColor = Color(0xFF86EFAC),
                    modifier = Modifier.weight(1f)
                )
                QuizStatCard(
                    title = "Aktif",
                    value = "$activeCount",
                    emoji = if (activeCount > 0) "⚡" else "🎉",
                    valueColor = if (activeCount > 0) Color(0xFFFDE047) else Color(0xFF86EFAC),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(12.dp))

            // Progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color.Black.copy(alpha = 0.25f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFFA78BFA), Color(0xFF34D399))
                            )
                        )
                )
            }
        }
    }
}

// ── TEACHER QUIZ HERO BANNER ──────────────────────────────────────────────────

@Composable
private fun TeacherQuizHeroBanner(activeCount: Int, totalCount: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(24.dp), spotColor = Color(0x30047857), ambientColor = Color(0x15047857))
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        Color(0xFF047857),
                        Color(0xFF059669),
                        Color(0xFF0D9488),
                    )
                )
            )
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 22.dp, y = (-18).dp)
                .size(110.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.08f))
        )

        Column(modifier = Modifier.padding(16.dp)) {
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
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text("📊", fontSize = 12.sp)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "PORTAL EVALUASI GURU",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.1.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.18f))
                        .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        "$totalCount Kuis Dibuat",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = "Manajemen Kuis Kelas",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-0.3).sp
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "Pantau partisipasi dan nilai kuis siswa di semua kelas.",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 12.sp,
                lineHeight = 16.sp
            )

            Spacer(Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.14f))
                        .border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(12.dp))
                        .padding(vertical = 8.dp, horizontal = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "$activeCount",
                        color = Color(0xFFFDE047),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        "Kuis Aktif",
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.14f))
                        .border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(12.dp))
                        .padding(vertical = 8.dp, horizontal = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "$totalCount",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        "Total Dibuat",
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun QuizStatCard(
    title: String,
    value: String,
    emoji: String,
    valueColor: Color = Color.White,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.12f))
            .border(1.dp, Color.White.copy(alpha = 0.20f), RoundedCornerShape(12.dp))
            .padding(vertical = 8.dp, horizontal = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(emoji, fontSize = 11.sp)
            Spacer(Modifier.width(4.dp))
            Text(
                text = value,
                color = valueColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black
            )
        }
        Spacer(Modifier.height(2.dp))
        Text(
            text = title,
            color = Color.White.copy(alpha = 0.75f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
