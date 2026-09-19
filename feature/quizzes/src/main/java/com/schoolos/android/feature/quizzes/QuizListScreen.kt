package com.schoolos.android.feature.quizzes

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
    viewModel: QuizListViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val role = state.userRole.lowercase()
    val isTeacher = role == "teacher" || role == "guru"

    val activeQuizzes = state.quizzes.filter { it.status.lowercase() in listOf("active", "open", "published") }
    val doneQuizzes = state.quizzes.filter { it.status.lowercase() in listOf("graded", "submitted", "completed") }

    Scaffold(
        containerColor = CosmicBlack,
        topBar = {
            ExecutiveTopBar(
                title = "Kuis & Evaluasi",
                subtitle = if (isTeacher) "Kelola & pantau kuis kelas" else "Kerjakan kuis & pantau hasil belajar",
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
                            text = if (isTeacher) "GURU" else "SISWA",
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
                FloatingActionButton(
                    onClick = onCreateQuiz,
                    containerColor = StudentNeon,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Buat Kuis")
                }
            }
        }
    ) { padding ->
        PullRefreshContainer(
            isRefreshing = state.isRefreshing,
            onRefresh = viewModel::refresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
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
                    // Active Subject Filter
                    if (state.subjectFilter != null) {
                        item {
                            SubjectFilterChip(
                                subject = state.subjectFilter!!,
                                onClear = viewModel::clearSubjectFilter,
                            )
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
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(CosmicNavy)
                                    .border(0.5.dp, GlassBorder, RoundedCornerShape(12.dp))
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.List,
                                        contentDescription = null,
                                        tint = TextTertiary,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Spacer(Modifier.height(10.dp))
                                    Text(
                                        text = "Belum Ada Kuis",
                                        color = TextPrimary,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = if (isTeacher) "Buat kuis baru melalui tombol tambah" else "Guru belum menambahkan kuis untuk kelas ini",
                                        color = TextTertiary,
                                        fontSize = 12.sp,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 16.sp
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
            .clip(RoundedCornerShape(12.dp))
            .background(CosmicNavy)
            .border(0.5.dp, GlassBorder, RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(CosmicSurface2)
                        .border(0.5.dp, GlassBorder, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.List,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        "PORTAL KUIS",
                        color = TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.6.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (allDone) NeonSuccess.copy(alpha = 0.12f) else CosmicSurface2)
                        .border(0.5.dp, if (allDone) NeonSuccess.copy(alpha = 0.25f) else GlassBorder, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (allDone) "Semua Selesai" else "$completionPercent% Selesai",
                        color = if (allDone) NeonSuccess else TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Text(
                text = if (allDone) "Semua kuis telah diselesaikan" else "Uji Pemahaman Materi",
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = if (activeCount > 0) "$activeCount kuis menunggu untuk dikerjakan" else "Pantau hasil evaluasi dan ulas materi yang diperlukan",
                color = TextTertiary,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )

            Spacer(Modifier.height(12.dp))

            // Quick stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuizStatCard(
                    title = "Total Kuis",
                    value = "$totalCount",
                    icon = Icons.Default.List,
                    modifier = Modifier.weight(1f)
                )
                QuizStatCard(
                    title = "Dikerjakan",
                    value = "$doneCount",
                    icon = Icons.Default.CheckCircle,
                    valueColor = NeonSuccess,
                    modifier = Modifier.weight(1f)
                )
                QuizStatCard(
                    title = "Aktif",
                    value = "$activeCount",
                    icon = Icons.Default.PlayArrow,
                    valueColor = if (activeCount > 0) NeonWarning else TextSecondary,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(10.dp))

            // Progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(CosmicSurface2)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(2.dp))
                        .background(NeonBlue)
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
            .clip(RoundedCornerShape(12.dp))
            .background(CosmicNavy)
            .border(0.5.dp, GlassBorder, RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(CosmicSurface2)
                        .border(0.5.dp, GlassBorder, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.List,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        "MANAJEMEN KUIS",
                        color = TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.6.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(CosmicSurface2)
                        .border(0.5.dp, GlassBorder, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        "$totalCount Kuis",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Text(
                text = "Evaluasi & Kuis Siswa",
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "Pantau partisipasi serta distribusi nilai siswa di setiap kelas.",
                color = TextTertiary,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuizStatCard(
                    title = "Kuis Aktif",
                    value = "$activeCount",
                    icon = Icons.Default.PlayArrow,
                    valueColor = NeonWarning,
                    modifier = Modifier.weight(1f)
                )
                QuizStatCard(
                    title = "Total Dibuat",
                    value = "$totalCount",
                    icon = Icons.Default.List,
                    valueColor = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun QuizStatCard(
    title: String,
    value: String,
    icon: ImageVector,
    valueColor: Color = TextPrimary,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .height(58.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(CosmicSurface2)
            .border(0.5.dp, GlassBorder, RoundedCornerShape(8.dp))
            .padding(vertical = 6.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = valueColor,
                modifier = Modifier.size(13.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = value,
                color = valueColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.height(2.dp))
        Text(
            text = title,
            color = TextTertiary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
