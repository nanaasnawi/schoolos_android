package com.schoolos.android.feature.grades

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.schoolos.android.core.designsystem.*
import com.schoolos.android.domain.model.SubjectGradeSummary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GradebookListScreen(
    onBack: (() -> Unit)? = null,
    onSubjectClick: (String, String) -> Unit = { _, _ -> },
    viewModel: GradebookListViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    var selectedFilter by remember { mutableStateOf("Semua Mapel") }

    val isTeacher = com.schoolos.android.core.auth.isTeacherRole(state.userRole)

    val filteredSubjects = remember(state.subjects, selectedFilter) {
        when (selectedFilter) {
            "MIPA" -> state.subjects.filter {
                it.subjectName.contains("Matematika", ignoreCase = true) ||
                it.subjectName.contains("IPA", ignoreCase = true) ||
                it.subjectName.contains("Fisika", ignoreCase = true) ||
                it.subjectName.contains("Biologi", ignoreCase = true) ||
                it.subjectName.contains("Kimia", ignoreCase = true)
            }
            "Bahasa" -> state.subjects.filter {
                it.subjectName.contains("Bahasa", ignoreCase = true) ||
                it.subjectName.contains("Inggris", ignoreCase = true) ||
                it.subjectName.contains("Indonesia", ignoreCase = true)
            }
            "Wajib" -> state.subjects.filter {
                it.subjectName.contains("Indonesia", ignoreCase = true) ||
                it.subjectName.contains("Matematika", ignoreCase = true) ||
                it.subjectName.contains("Agama", ignoreCase = true) ||
                it.subjectName.contains("PPKn", ignoreCase = true)
            }
            "Muatan Lokal" -> state.subjects.filter {
                it.subjectName.contains("Seni", ignoreCase = true) ||
                it.subjectName.contains("Prakarya", ignoreCase = true) ||
                it.subjectName.contains("Daerah", ignoreCase = true) ||
                it.subjectName.contains("Jawa", ignoreCase = true) ||
                it.subjectName.contains("Sunda", ignoreCase = true)
            }
            else -> state.subjects
        }
    }

    Scaffold(containerColor = CosmicBlack) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ── TOP BAR WITH PERSISTENT BACK BUTTON & IDENTITY ───────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onBack != null) {
                    CustomBackButton(onClick = onBack)
                    Spacer(Modifier.width(12.dp))
                }
                Column {
                    Text(
                        text = "Buku Nilai",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary,
                        letterSpacing = (-0.5).sp
                    )
                    if (isTeacher && state.className.isNotBlank()) {
                        Text(
                            text = "Wali Kelas • Kelas ${state.className}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TeacherNeon
                        )
                    }
                }
            }

            PullRefreshContainer(
                isRefreshing = state.isRefreshing,
                onRefresh = viewModel::refresh,
                modifier = Modifier.weight(1f),
            ) {
                if (state.isLoading) {
                    LoadingState()
                } else if (state.error != null) {
                    ErrorState(message = state.error!!)
                } else if (state.subjects.isEmpty()) {
                    EmptyState("Belum ada mata pelajaran tercatat.", Icons.Default.EmojiEvents)
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        // ── STUDENT VIEW ─────────────────────────────────
                        if (!isTeacher) {
                            item {
                                StudentGradeHeroHeader(state.subjects)
                            }
                            item {
                                val filters = listOf("Semua Mapel", "Wajib", "MIPA", "Muatan Lokal")
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                ) {
                                    items(filters) { filter ->
                                        val isSelected = filter == selectedFilter
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(if (isSelected) NeonBlue.copy(alpha = 0.12f) else CosmicNavy)
                                                .border(
                                                    1.dp,
                                                    if (isSelected) NeonBlue.copy(alpha = 0.45f) else GlassBorder,
                                                    RoundedCornerShape(14.dp),
                                                )
                                                .clickable { selectedFilter = filter }
                                                .padding(horizontal = 14.dp, vertical = 8.dp),
                                        ) {
                                            Text(
                                                filter,
                                                fontSize = 12.sp,
                                                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                                color = if (isSelected) NeonBlue else TextTertiary,
                                            )
                                        }
                                    }
                                }
                            }
                            studentGradebookContent(
                                subjects = filteredSubjects,
                                onSubjectClick = onSubjectClick
                            )
                        } else {
                            // ── TEACHER VIEW (WALIKELAS LEGER NILAI) ───────────
                            teacherGradebookContent(
                                subjects = filteredSubjects,
                                className = state.className,
                                onSubjectClick = onSubjectClick
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StudentGradeHeroHeader(subjects: List<SubjectGradeSummary>) {
    val gradedSubjects = subjects.filter { it.gradedComponentCount > 0 && it.finalScore > 0 }
    val avgScore = if (gradedSubjects.isEmpty()) 0.0 else gradedSubjects.map { it.finalScore }.average()
    val totalGraded = subjects.sumOf { it.gradedComponentCount }
    val totalComponents = subjects.sumOf { it.componentCount }

    val (predicate, predicateDesc) = when {
        avgScore >= 85.0 -> Pair("Predikat A", "Sangat Baik")
        avgScore >= 75.0 -> Pair("Predikat B", "Baik")
        avgScore >= 65.0 -> Pair("Predikat C", "Cukup")
        avgScore >= 55.0 -> Pair("Predikat D", "Kurang")
        avgScore > 0.0 -> Pair("Predikat E", "Perlu Bimbingan")
        else -> Pair("-", "Belum Ada Nilai")
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(NeonBlue, StudentNeon)))
            .padding(20.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "PERFORMA AKADEMIK",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Semester Aktif",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                )
            }
            Text("🏆", fontSize = 42.sp)
        }

        Spacer(Modifier.height(24.dp))

        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                if (gradedSubjects.isNotEmpty()) "%.1f".format(avgScore) else "-",
                color = Color.White,
                fontSize = 48.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-2).sp,
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.padding(bottom = 6.dp)) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.2f))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Text(predicate, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black)
                }
                Spacer(Modifier.height(4.dp))
                Text(predicateDesc, color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(24.dp))

        // Executive Summary Strip
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            GradeMetricPill("${subjects.size} Mapel", Modifier.weight(1f))
            GradeMetricPill("$totalGraded / $totalComponents Done", Modifier.weight(1.5f))
        }
    }
}

@Composable
private fun GradeMetricPill(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.15f))
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black)
    }
}
