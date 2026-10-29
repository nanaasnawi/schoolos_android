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
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
    var selectedRowForDetail by remember { mutableStateOf<StudentGradebookRow?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    val isPrincipal = com.schoolos.android.core.auth.isPrincipalRole(state.userRole)
    val isTeacher = com.schoolos.android.core.auth.isTeacherRole(state.userRole) || isPrincipal

    LaunchedEffect(state.userMessage) {
        state.userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearUserMessage()
        }
    }

    val filteredSubjects = remember(state.subjects, selectedFilter) {
        if (selectedFilter == "Semua Mapel") {
            state.subjects
        } else {
            state.subjects.filter {
                it.category.equals(selectedFilter, ignoreCase = true) ||
                        it.subjectName.contains(selectedFilter, ignoreCase = true)
            }
        }
    }

    Scaffold(
        containerColor = CosmicBlack,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            ExecutiveTopBar(
                title = if (isPrincipal) "Buku Nilai (Teacher Gradebook)"
                    else if (isTeacher) "Analisis & Buku Nilai Guru"
                    else "Buku Nilai",
                subtitle = if (isPrincipal || isTeacher) "Standar Kurikulum Merdeka (Kemendikdasmen RI)"
                    else "Rekapitulasi Capaian Modul",
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
            } else if (state.error != null && state.allActiveStudents.isEmpty() && state.subjects.isEmpty()) {
                ErrorState(message = state.error!!, onRetry = viewModel::refresh)
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    // ── SEGMENTED TOP TABS (FOR TEACHER / PRINCIPAL) ─────────
                    if (isTeacher) {
                        val isDark = LocalIsDarkTheme.current
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isDark) CosmicNavy else Color(0xFFF1F5F9))
                                .border(0.5.dp, GlassBorder, RoundedCornerShape(12.dp))
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Tab 0: Buku Nilai Siswa (Matches Web Dashboard)
                            val isTab0 = state.activeTab == 0
                            val tab0Bg = if (isTab0) {
                                if (isDark) TeacherNeon.copy(alpha = 0.22f) else Color.White
                            } else Color.Transparent
                            val tab0Border = if (isTab0) TeacherNeon else Color.Transparent

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(9.dp))
                                    .background(tab0Bg)
                                    .border(
                                        if (isTab0) 1.dp else 0.dp,
                                        tab0Border,
                                        RoundedCornerShape(9.dp)
                                    )
                                    .clickable { viewModel.selectTab(0) }
                                    .padding(vertical = 9.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        Icons.Default.MenuBook,
                                        contentDescription = null,
                                        tint = if (isTab0) TeacherNeon else TextTertiary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        "Buku Nilai Siswa (${state.totalStudentsCount})",
                                        fontSize = 12.sp,
                                        fontWeight = if (isTab0) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isTab0) TeacherNeon else TextTertiary
                                    )
                                }
                            }

                            // Tab 1: Rekap 18 Modul Overview
                            val isTab1 = state.activeTab == 1
                            val tab1Bg = if (isTab1) {
                                if (isDark) TeacherNeon.copy(alpha = 0.22f) else Color.White
                            } else Color.Transparent
                            val tab1Border = if (isTab1) TeacherNeon else Color.Transparent

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(9.dp))
                                    .background(tab1Bg)
                                    .border(
                                        if (isTab1) 1.dp else 0.dp,
                                        tab1Border,
                                        RoundedCornerShape(9.dp)
                                    )
                                    .clickable { viewModel.selectTab(1) }
                                    .padding(vertical = 9.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Assessment,
                                        contentDescription = null,
                                        tint = if (isTab1) TeacherNeon else TextTertiary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        "Rekap ${state.subjects.size} Modul",
                                        fontSize = 12.sp,
                                        fontWeight = if (isTab1) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isTab1) TeacherNeon else TextTertiary
                                    )
                                }
                            }
                        }
                    }

                    // ── LAZY COLUMN CONTENT ──────────────────────────────────
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        if (!isTeacher) {
                            // ── STUDENT VIEW ─────────────────────────────────
                            item {
                                StudentGradeHeroHeader(state.subjects)
                            }
                            val dynamicCategories = (listOf("Semua Mapel") + state.subjects.map { it.category }.filter { it.isNotBlank() && it != "Wajib" }).distinct()
                            if (dynamicCategories.size > 1) {
                                item {
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                    ) {
                                        items(dynamicCategories) { filter ->
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
                            }
                            studentGradebookContent(
                                subjects = filteredSubjects,
                                onSubjectClick = onSubjectClick
                            )
                        } else {
                            // ── TEACHER / PRINCIPAL VIEW ──────────────────────
                            if (state.activeTab == 0) {
                                // View 0: Buku Nilai Siswa (Kurikulum Merdeka SSOT)
                                kurikulumMerdekaGradebookContent(
                                    totalStudents = state.totalStudentsCount,
                                    averageScore = state.averageScore,
                                    passingPercentage = state.passingPercentage,
                                    passedCount = state.passedCount,
                                    remedialCount = state.remedialCount,
                                    selectedSubjectName = state.selectedSubjectName,
                                    availableSubjects = state.availableSubjects,
                                    selectedClassFilter = state.selectedClassFilter,
                                    availableClasses = state.availableClasses,
                                    activeScheme = state.activeScheme,
                                    searchQuery = state.searchQuery,
                                    filteredRows = state.filteredGradebookRows,
                                    onSelectSubject = { subj ->
                                        viewModel.selectSubject(subj.id, subj.name)
                                    },
                                    onSelectClassFilter = { cls ->
                                        viewModel.selectClassFilter(cls)
                                    },
                                    onSelectScheme = { scheme ->
                                        viewModel.selectScheme(scheme)
                                    },
                                    onSearchQueryChanged = { q ->
                                        viewModel.onSearchQueryChanged(q)
                                    },
                                    onOpenDetail = { row ->
                                        selectedRowForDetail = row
                                    }
                                )
                            } else {
                                // View 1: Rekap 18 Modul Overview Cards
                                teacherGradebookContent(
                                    subjects = filteredSubjects,
                                    className = state.className,
                                    teacherSubject = state.teacherSubject,
                                    totalSubjectCount = state.subjects.size,
                                    selectedFilter = selectedFilter,
                                    isPrincipal = isPrincipal,
                                    onSelectFilter = { selectedFilter = it },
                                    onSubjectClick = { subjectId, subjectName ->
                                        viewModel.selectSubject(subjectId, subjectName)
                                        viewModel.selectTab(0)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // ── MODAL BOTTOM SHEET FOR INPUT NILAI & DETAIL CAPAIAN ──────────────────
    selectedRowForDetail?.let { row ->
        GradeDetailModalSheet(
            row = row,
            selectedSubjectName = state.selectedSubjectName,
            activeScheme = state.activeScheme,
            isSaving = state.isSaving,
            onDismiss = { selectedRowForDetail = null },
            onSave = { m1, m2, m3, ats, sas, highest, lowest, notes, m4 ->
                viewModel.saveStudentGrade(
                    studentId = row.studentId,
                    studentName = row.name,
                    m1 = m1,
                    m2 = m2,
                    m3 = m3,
                    ats = ats,
                    sas = sas,
                    highestDesc = highest,
                    lowestDesc = lowest,
                    formatifNotes = notes,
                    m4 = m4,
                )
                selectedRowForDetail = null
            }
        )
    }
}

@Composable
private fun StudentGradeHeroHeader(subjects: List<SubjectGradeSummary>) {
    val gradedSubjects = subjects.filter { it.gradedComponentCount > 0 && it.finalScore > 0 }
    val avgScore = if (gradedSubjects.isEmpty()) 0.0 else gradedSubjects.map { it.finalScore }.average()
    val totalGraded = subjects.sumOf { it.gradedComponentCount }
    val totalComponents = subjects.sumOf { it.componentCount }

    val statusText = if (gradedSubjects.isNotEmpty()) "Terdata" else "Belum Dinilai"
    val statusDesc = if (gradedSubjects.isNotEmpty()) "${gradedSubjects.size} dari ${subjects.size} Mapel Dinilai" else "Menunggu evaluasi modul"

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
                        "CAPAIAN MODUL BELAJAR",
                        color = TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.6.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (gradedSubjects.isNotEmpty()) StudentNeon.copy(alpha = 0.12f) else CosmicSurface2)
                        .border(0.5.dp, if (gradedSubjects.isNotEmpty()) StudentNeon.copy(alpha = 0.25f) else GlassBorder, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                ) {
                    Text(
                        statusText,
                        color = if (gradedSubjects.isNotEmpty()) StudentNeon else TextTertiary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = if (gradedSubjects.isNotEmpty()) "%.1f".format(java.util.Locale.US, avgScore) else "—",
                    color = TextPrimary,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-1).sp,
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = statusDesc,
                    color = TextTertiary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            Spacer(Modifier.height(12.dp))

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
