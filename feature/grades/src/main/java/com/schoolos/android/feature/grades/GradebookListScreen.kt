package com.schoolos.android.feature.grades

import com.schoolos.android.core.R as CoreR
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
    showHeader: Boolean = true,
    onBack: (() -> Unit)? = null,
    onSubjectClick: (String, String) -> Unit = { _, _ -> },
    viewModel: GradebookListViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    var selectedFilter by remember { mutableStateOf("Semua Mapel") }
    var selectedRowForDetail by remember { mutableStateOf<StudentGradebookRow?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    val isDark = LocalIsDarkTheme.current
    val cardBg = if (isDark) Color(0xFF111113) else Color.White
    val cardBorder = if (isDark) Color(0xFF1F1F23) else Color(0xFFE2E8F0)
    val textPrimary = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFFA1A1AA) else Color(0xFF64748B)
    val textTertiary = if (isDark) Color(0xFF71717A) else Color(0xFF94A3B8)
    val innerCardBg = if (isDark) Color(0xFF161618) else Color(0xFFF8FAFC)
    val innerCardBorder = if (isDark) Color(0xFF27272A) else Color(0xFFE2E8F0)

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
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (showHeader) {
                ExecutiveTopBar(
                    title = if (isPrincipal) "Buku Nilai & Leger Siswa"
                        else if (isTeacher) "Analisis & Buku Nilai Guru"
                        else "Buku Nilai",
                    subtitle = if (isPrincipal || isTeacher) "Standar Kurikulum Merdeka (Kemendikdasmen RI)"
                        else "Rekapitulasi Capaian Modul",
                    onBack = onBack,
                )
            }
        }
    ) { padding ->
        PullRefreshContainer(
            isRefreshing = state.isRefreshing,
            onRefresh = viewModel::refresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding()),
        ) {
            if (state.isLoading) {
                LoadingState()
            } else if (state.error != null && state.allActiveStudents.isEmpty() && state.subjects.isEmpty()) {
                ErrorState(message = state.error!!, onRetry = viewModel::refresh)
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    // ── SEGMENTED TOP TABS (FOR TEACHER / PRINCIPAL) ─────────
                    if (isTeacher) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(14.dp),
                            color = cardBg,
                            border = BorderStroke(0.5.dp, cardBorder)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                // Tab 0: Buku Nilai Siswa (Matches Web Dashboard)
                                val isTab0 = state.activeTab == 0
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { viewModel.selectTab(0) },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isTab0) {
                                        if (isDark) TeacherNeon.copy(alpha = 0.22f) else TeacherContainer
                                    } else Color.Transparent,
                                    border = BorderStroke(
                                        if (isTab0) 1.dp else 0.dp,
                                        if (isTab0) TeacherNeon else Color.Transparent
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(vertical = 9.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            painter = painterResource(CoreR.drawable.ic_modern_book),
                                            contentDescription = null,
                                            tint = if (isTab0) TeacherNeon else textTertiary,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            "Buku Nilai Siswa (${state.totalStudentsCount})",
                                            fontSize = 12.sp,
                                            fontWeight = if (isTab0) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isTab0) TeacherNeon else textTertiary
                                        )
                                    }
                                }

                                // Tab 1: Rekap 18 Modul Overview
                                val isTab1 = state.activeTab == 1
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { viewModel.selectTab(1) },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isTab1) {
                                        if (isDark) TeacherNeon.copy(alpha = 0.22f) else TeacherContainer
                                    } else Color.Transparent,
                                    border = BorderStroke(
                                        if (isTab1) 1.dp else 0.dp,
                                        if (isTab1) TeacherNeon else Color.Transparent
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(vertical = 9.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            painter = painterResource(CoreR.drawable.ic_modern_grades),
                                            contentDescription = null,
                                            tint = if (isTab1) TeacherNeon else textTertiary,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            "Rekap ${state.subjects.size} Modul",
                                            fontSize = 12.sp,
                                            fontWeight = if (isTab1) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isTab1) TeacherNeon else textTertiary
                                        )
                                    }
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
                                    val studentAccent = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                    ) {
                                        items(dynamicCategories) { filter ->
                                            val isSelected = filter == selectedFilter
                                            val chipBg = if (isSelected) studentAccent.copy(alpha = if (isDark) 0.16f else 0.10f) else (if (isDark) Color(0xFF141417) else Color.White)
                                            val chipBorder = if (isSelected) studentAccent.copy(alpha = if (isDark) 0.55f else 0.45f) else (if (isDark) Color(0xFF232326) else Color(0xFFE2E8F0))
                                            val chipTextColor = if (isSelected) studentAccent else (if (isDark) Color(0xFFA1A1AA) else Color(0xFF64748B))
                                            Surface(
                                                shape = RoundedCornerShape(12.dp),
                                                color = chipBg,
                                                border = BorderStroke(1.dp, chipBorder),
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .clickable { selectedFilter = filter }
                                            ) {
                                                Text(
                                                    text = filter,
                                                    fontSize = 12.5.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    color = chipTextColor,
                                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
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
    val isDark = LocalIsDarkTheme.current
    val cardBg = if (isDark) Color(0xFF111113) else Color.White
    val cardBorder = if (isDark) Color(0xFF1F1F23) else Color(0xFFE2E8F0)
    val textPrimary = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFFA1A1AA) else Color(0xFF64748B)
    val textTertiary = if (isDark) Color(0xFF71717A) else Color(0xFF94A3B8)
    val innerCardBg = if (isDark) Color(0xFF161618) else Color(0xFFF8FAFC)
    val innerCardBorder = if (isDark) Color(0xFF27272A) else Color(0xFFE2E8F0)

    val gradedSubjects = subjects.filter { it.gradedComponentCount > 0 && it.finalScore > 0 }
    val avgScore = if (gradedSubjects.isEmpty()) 0.0 else gradedSubjects.map { it.finalScore }.average()
    val totalGraded = subjects.sumOf { it.gradedComponentCount }
    val totalComponents = subjects.sumOf { it.componentCount }
    val passedCount = subjects.count { it.finalScore >= it.passingThreshold && it.finalScore > 0 }

    val statusText = if (gradedSubjects.isNotEmpty()) "Terdata" else "Belum Dinilai"
    val statusDesc = if (gradedSubjects.isNotEmpty()) "${gradedSubjects.size} dari ${subjects.size} Mapel Dinilai" else "Menunggu evaluasi modul"

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = cardBg,
        border = BorderStroke(0.5.dp, cardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(StudentNeon)
                    )
                    Text(
                        "CAPAIAN MODUL BELAJAR",
                        color = textTertiary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (gradedSubjects.isNotEmpty()) StudentNeon.copy(alpha = 0.12f) else innerCardBg,
                    border = BorderStroke(0.5.dp, if (gradedSubjects.isNotEmpty()) StudentNeon.copy(alpha = 0.35f) else innerCardBorder)
                ) {
                    Text(
                        statusText,
                        color = if (gradedSubjects.isNotEmpty()) StudentNeon else textTertiary,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = if (gradedSubjects.isNotEmpty()) "%.1f".format(java.util.Locale.US, avgScore) else "—",
                        color = textPrimary,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-1).sp,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = statusDesc,
                        color = textTertiary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal
                    )
                }

                if (gradedSubjects.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = NeonSuccess.copy(alpha = 0.12f),
                        border = BorderStroke(0.5.dp, NeonSuccess.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = "$passedCount Tuntas KKTP",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonSuccess,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            HorizontalDivider(color = innerCardBorder, thickness = 0.5.dp)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GradeMetricPill(
                    label = "Total Mapel",
                    value = "${subjects.size} Mapel",
                    bg = innerCardBg,
                    border = innerCardBorder,
                    textColor = textPrimary,
                    subColor = textTertiary,
                    modifier = Modifier.weight(1f)
                )
                GradeMetricPill(
                    label = "Capaian Asesmen",
                    value = "$totalGraded / $totalComponents Dinilai",
                    bg = innerCardBg,
                    border = innerCardBorder,
                    textColor = textPrimary,
                    subColor = textTertiary,
                    modifier = Modifier.weight(1.4f)
                )
            }
        }
    }
}

@Composable
private fun GradeMetricPill(
    label: String,
    value: String,
    bg: Color,
    border: Color,
    textColor: Color,
    subColor: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = bg,
        border = BorderStroke(0.5.dp, border)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(label, color = subColor, fontSize = 10.sp, fontWeight = FontWeight.Medium)
            Text(value, color = textColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}
