package com.schoolos.android.feature.grades

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material3.ExperimentalMaterial3Api
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

    Scaffold(
        containerColor = CosmicBlack,
        topBar = {
            ExecutiveTopBar(
                title = "Buku Nilai",
                subtitle = if (isTeacher && state.className.isNotBlank()) "Wali Kelas • Kelas ${state.className}" else "Rekapitulasi Nilai Akademik",
                onBack = onBack,
            )
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
                ErrorState(message = state.error!!)
            } else if (state.subjects.isEmpty()) {
                EmptyState("Belum ada mata pelajaran tercatat.", Icons.Default.Assessment)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
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
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) CosmicSurface2 else CosmicNavy)
                                            .border(
                                                0.5.dp,
                                                if (isSelected) GlassBorder2 else GlassBorder,
                                                RoundedCornerShape(8.dp),
                                            )
                                            .clickable { selectedFilter = filter }
                                            .padding(horizontal = 12.dp, vertical = 6.dp),
                                    ) {
                                        Text(
                                            filter,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                            color = if (isSelected) TextPrimary else TextTertiary,
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

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CosmicNavy)
            .border(0.5.dp, GlassBorder, RoundedCornerShape(12.dp))
            .padding(14.dp),
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
                    Text(
                        "PERFORMA AKADEMIK",
                        color = TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.6.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(NeonSuccess.copy(alpha = 0.12f))
                        .border(0.5.dp, NeonSuccess.copy(alpha = 0.25f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                ) {
                    Text(predicate, color = NeonSuccess, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = if (gradedSubjects.isNotEmpty()) "%.1f".format(avgScore) else "-",
                    color = TextPrimary,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-1).sp,
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = predicateDesc,
                    color = TextTertiary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            Spacer(Modifier.height(12.dp))

            // Executive Summary Strip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GradeMetricPill("${subjects.size} Mapel", Modifier.weight(1f))
                GradeMetricPill("$totalGraded / $totalComponents Dinilai", Modifier.weight(1.5f))
            }
        }
    }
}

@Composable
private fun GradeMetricPill(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(CosmicSurface2)
            .border(0.5.dp, GlassBorder, RoundedCornerShape(6.dp))
            .padding(vertical = 6.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}
