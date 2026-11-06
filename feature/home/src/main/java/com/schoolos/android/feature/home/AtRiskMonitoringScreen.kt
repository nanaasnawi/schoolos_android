package com.schoolos.android.feature.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.schoolos.android.core.designsystem.*
import com.schoolos.android.domain.model.AtRiskStudent

@Composable
fun AtRiskMonitoringScreen(
    showHeader: Boolean = true,
    onBack: (() -> Unit)? = null,
    onNavigateToStudentDetail: ((studentId: String, name: String, nisn: String, className: String) -> Unit)? = null,
    viewModel: AtRiskMonitoringViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val isDark = LocalIsDarkTheme.current
    val snackbarHostState = remember { SnackbarHostState() }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(state.userMessage) {
        state.userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearUserMessage()
        }
    }

    if (showHeader) {
        Scaffold(
            containerColor = if (isDark) Color(0xFF09090B) else Color(0xFFF8FAFC),
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                AtRiskTopHeader(
                    onBack = onBack,
                    isDark = isDark,
                    isRefreshing = state.isRefreshing,
                    onRefresh = { viewModel.refresh() }
                )
            }
        ) { padding ->
            AtRiskMonitoringContent(
                state = state,
                isDark = isDark,
                focusManager = focusManager,
                viewModel = viewModel,
                onNavigateToStudentDetail = onNavigateToStudentDetail,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = padding.calculateTopPadding())
            )
        }
    } else {
        AtRiskMonitoringContent(
            state = state,
            isDark = isDark,
            focusManager = focusManager,
            viewModel = viewModel,
            onNavigateToStudentDetail = onNavigateToStudentDetail,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun AtRiskMonitoringContent(
    state: AtRiskUiState,
    isDark: Boolean,
    focusManager: FocusManager,
    viewModel: AtRiskMonitoringViewModel,
    onNavigateToStudentDetail: ((studentId: String, name: String, nisn: String, className: String) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    PullRefreshContainer(
        isRefreshing = state.isRefreshing,
        onRefresh = { viewModel.refresh() },
        modifier = modifier
    ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 96.dp)
            ) {
                // 1. Compact Early Warning Action Card
                item {
                    EarlyWarningHeroBanner(
                        totalAtRisk = state.stats.total,
                        isSendingBatch = state.isSendingBatch,
                        isDark = isDark,
                        onSendBatch = { viewModel.sendBatchReminder() }
                    )
                }

                // 2. Interactive KPI Filter Tiles (Combined Stats + Filter)
                item {
                    Spacer(Modifier.height(6.dp))
                    AtRiskInteractiveStatBar(
                        stats = state.stats,
                        selectedCategory = state.selectedCategoryFilter,
                        onSelectCategory = { viewModel.onSelectCategoryFilter(it) }
                    )
                }

                // 3. Compact Search & Rombel Filter Toolbar
                item {
                    Spacer(Modifier.height(6.dp))
                    AtRiskFilterToolbar(
                        searchQuery = state.searchQuery,
                        onSearchChange = { viewModel.onSearchQueryChanged(it) },
                        onClearSearch = { viewModel.onSearchQueryChanged(""); focusManager.clearFocus() },
                        classes = state.availableClasses.map { it.name },
                        selectedClass = state.selectedClassFilter,
                        onSelectClass = { viewModel.onSelectClassFilter(it) },
                        isDark = isDark
                    )
                }

                // 4. Compact Section Header & Student Count
                item {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "DAFTAR SISWA PERLU PERHATIAN",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.6.sp,
                            color = TextTertiary
                        )
                        Text(
                            text = "${state.filteredStudents.size} Peserta Didik",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (state.filteredStudents.isNotEmpty()) NeonError else TextTertiary
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                }

                // 5. Loading / Error / Empty States
                if (state.isLoading) {
                    items(4) {
                        AtRiskStudentCardSkeleton()
                        Spacer(Modifier.height(10.dp))
                    }
                } else if (state.error != null && state.allStudents.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = NeonError,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = state.error ?: "Terjadi kendala memuat data",
                                    fontSize = 13.sp,
                                    color = TextPrimary,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else if (state.filteredStudents.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Surface(
                                    modifier = Modifier.size(52.dp),
                                    shape = CircleShape,
                                    color = NeonSuccess.copy(alpha = 0.12f),
                                    border = BorderStroke(0.5.dp, NeonSuccess.copy(alpha = 0.3f))
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = NeonSuccess,
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }
                                }
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    text = "Semua Siswa Terpantau Baik",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "Tidak ditemukan kendala belajar pada kriteria filter yang dipilih.",
                                    fontSize = 12.sp,
                                    color = TextSecondary,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    // 6. Student Roster Cards
                    itemsIndexed(
                        items = state.filteredStudents,
                        key = { _, item -> "${item.studentId}_${item.materialId ?: item.assignmentId ?: item.quizId ?: item.category}" }
                    ) { index, student ->
                        AtRiskStudentItemCard(
                            student = student,
                            isSending = state.sendingStudentId == student.studentId,
                            isDark = isDark,
                            onSendReminder = { viewModel.sendSingleReminder(student) },
                            onClick = {
                                onNavigateToStudentDetail?.invoke(
                                    student.studentId,
                                    student.studentName,
                                    student.nisn,
                                    student.className
                                )
                            }
                        )
                        Spacer(Modifier.height(10.dp))
                    }
                }
            }
        }
    }

// ─── Header Components ─────────────────────────────────────────────────────────

@Composable
private fun AtRiskTopHeader(
    onBack: (() -> Unit)?,
    isDark: Boolean,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
) {
    val headerBg = if (isDark) Color(0xFF09090B) else Color(0xFFF8FAFC)
    val textPrimary = if (isDark) Color(0xFFF1F5F9) else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding(),
        color = headerBg
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onBack != null) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = textPrimary
                        )
                    }
                }
                Column(
                    modifier = Modifier.padding(start = if (onBack != null) 0.dp else 4.dp),
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    Text(
                        text = "Siswa Perlu Perhatian",
                        fontSize = 15.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                    Text(
                        text = "Early Warning System (At-Risk Monitoring)",
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
                        color = textSecondary
                    )
                }
            }

            IconButton(
                onClick = onRefresh,
                enabled = !isRefreshing
            ) {
                if (isRefreshing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = NeonError
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Muat Ulang",
                        tint = textSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun EarlyWarningHeroBanner(
    totalAtRisk: Int,
    isSendingBatch: Boolean,
    isDark: Boolean,
    onSendBatch: () -> Unit,
) {
    val cardBg = if (isDark) Color(0xFF111113) else Color.White
    val cardBorder = if (isDark) Color(0xFF232326) else Color(0xFFE2E8F0)
    val textPrimary = if (isDark) Color(0xFFF1F5F9) else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(22.dp),
        color = cardBg,
        border = BorderStroke(1.dp, if (totalAtRisk > 0) NeonError.copy(alpha = 0.35f) else cardBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(NeonError.copy(alpha = 0.12f))
                        .border(1.dp, NeonError.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = NeonError,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Column(verticalArrangement = Arrangement.Center) {
                    Text(
                        text = if (totalAtRisk > 0) "$totalAtRisk Siswa Perlu Perhatian" else "Kondisi Siswa Terpantau Aman",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (totalAtRisk > 0) "Kendala materi, tugas, atau nilai KKM" else "Tidak ada indikasi kendala belajar aktif",
                        fontSize = 11.sp,
                        color = textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(Modifier.width(8.dp))

            if (totalAtRisk > 0) {
                Button(
                    onClick = onSendBatch,
                    enabled = !isSendingBatch,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    modifier = Modifier.height(34.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonError,
                        contentColor = Color.White
                    )
                ) {
                    if (isSendingBatch) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(12.dp),
                            strokeWidth = 1.5.dp,
                            color = Color.White
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "Ingatkan",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// ─── Interactive KPI Stat & Category Filter Bar ──────────────────────────────

@Composable
private fun AtRiskInteractiveStatBar(
    stats: com.schoolos.android.domain.model.AtRiskStats,
    selectedCategory: String,
    onSelectCategory: (String) -> Unit,
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            StatFilterTile(
                count = stats.total,
                label = "Semua",
                icon = Icons.Default.Warning,
                isSelected = selectedCategory == "ALL",
                accentColor = NeonBlue,
                onClick = { onSelectCategory("ALL") }
            )
        }
        item {
            StatFilterTile(
                count = stats.unreadMaterials,
                label = "Materi",
                icon = Icons.AutoMirrored.Filled.MenuBook,
                isSelected = selectedCategory == "UNREAD_MATERIAL",
                accentColor = AccentNeonAmber,
                onClick = { onSelectCategory("UNREAD_MATERIAL") }
            )
        }
        item {
            StatFilterTile(
                count = stats.overdueAssignments,
                label = "Tugas",
                icon = Icons.AutoMirrored.Filled.Assignment,
                isSelected = selectedCategory == "OVERDUE_ASSIGNMENT",
                accentColor = NeonWarning,
                onClick = { onSelectCategory("OVERDUE_ASSIGNMENT") }
            )
        }
        item {
            StatFilterTile(
                count = stats.quizAlerts,
                label = "Kuis & CBT",
                icon = Icons.Default.Quiz,
                isSelected = selectedCategory == "QUIZ_ALERT",
                accentColor = Color(0xFFA855F7),
                onClick = { onSelectCategory("QUIZ_ALERT") }
            )
        }
        item {
            StatFilterTile(
                count = stats.lowScores,
                label = "Di Bawah KKM",
                icon = Icons.AutoMirrored.Filled.TrendingDown,
                isSelected = selectedCategory == "LOW_SCORE",
                accentColor = NeonError,
                onClick = { onSelectCategory("LOW_SCORE") }
            )
        }
    }
}

@Composable
private fun StatFilterTile(
    count: Int,
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
) {
    val isDark = LocalIsDarkTheme.current
    val innerCardBg = if (isDark) Color(0xFF1B1B1E) else Color(0xFFF1F5F9)
    val innerCardBorder = if (isDark) Color(0xFF2E2E32) else Color(0xFFE2E8F0)
    val textPrimary = if (isDark) Color(0xFFF1F5F9) else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    val textTertiary = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)

    val bg = if (isSelected) accentColor.copy(alpha = if (isDark) 0.18f else 0.12f) else innerCardBg
    val border = if (isSelected) accentColor else innerCardBorder
    val fg = if (isSelected) accentColor else textSecondary

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = bg,
        border = BorderStroke(1.dp, border)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = fg,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = count.toString(),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) textPrimary else fg
            )
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isSelected) textPrimary else textTertiary
            )
        }
    }
}

// ─── Filter Toolbar & Rombel Chips ───────────────────────────────────────────

@Composable
private fun AtRiskFilterToolbar(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    classes: List<String>,
    selectedClass: String,
    onSelectClass: (String) -> Unit,
    isDark: Boolean,
) {
    val cardBg = if (isDark) Color(0xFF111113) else Color.White
    val cardBorder = if (isDark) Color(0xFF232326) else Color(0xFFE2E8F0)
    val innerCardBg = if (isDark) Color(0xFF1B1B1E) else Color(0xFFF1F5F9)
    val innerCardBorder = if (isDark) Color(0xFF2E2E32) else Color(0xFFE2E8F0)
    val textPrimary = if (isDark) Color(0xFFF1F5F9) else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    val textTertiary = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)
    val emeraldAccent = if (isDark) Color(0xFF10B981) else Color(0xFF059669)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Compact Search Field
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(44.dp),
            shape = RoundedCornerShape(14.dp),
            color = cardBg,
            border = BorderStroke(
                1.dp,
                if (searchQuery.isNotBlank()) Color(0xFFEF4444) else cardBorder
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = if (searchQuery.isNotBlank()) Color(0xFFEF4444) else textTertiary,
                    modifier = Modifier.size(18.dp)
                )
                BasicTextField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    singleLine = true,
                    textStyle = TextStyle(
                        fontSize = 13.sp,
                        color = textPrimary
                    ),
                    cursorBrush = SolidColor(Color(0xFFEF4444)),
                    modifier = Modifier.weight(1f),
                    decorationBox = { innerTextField ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Cari siswa, NISN, atau kendala belajar...",
                                    fontSize = 13.sp,
                                    color = textTertiary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            innerTextField()
                        }
                    }
                )
                if (searchQuery.isNotBlank()) {
                    IconButton(
                        onClick = onClearSearch,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear",
                            tint = textSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // Rombel Selector Chips (LazyRow edge-to-edge scroll)
        val allRombelOptions = listOf("ALL") + classes
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(allRombelOptions.size) { index ->
                val rombel = allRombelOptions[index]
                val isSelected = selectedClass == rombel
                val label = if (rombel == "ALL") "Semua Rombel" else rombel

                val chipBg = if (isSelected) emeraldAccent.copy(alpha = if (isDark) 0.18f else 0.12f) else innerCardBg
                val chipBorder = if (isSelected) emeraldAccent else innerCardBorder
                val chipFg = if (isSelected) emeraldAccent else textSecondary

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = chipBg,
                    border = BorderStroke(1.dp, chipBorder),
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onSelectClass(rombel) }
                ) {
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = chipFg,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AtRiskFilterPill(
    label: String,
    selected: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
) {
    val isDark = LocalIsDarkTheme.current
    val innerCardBg = if (isDark) Color(0xFF1B1B1E) else Color(0xFFF1F5F9)
    val innerCardBorder = if (isDark) Color(0xFF2E2E32) else Color(0xFFE2E8F0)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) accentColor.copy(alpha = if (isDark) 0.18f else 0.12f) else innerCardBg,
        border = BorderStroke(
            1.dp,
            if (selected) accentColor else innerCardBorder
        )
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) accentColor else textSecondary,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
        )
    }
}

// ─── Student Item Cards ────────────────────────────────────────────────────────

@Composable
private fun AtRiskStudentItemCard(
    student: AtRiskStudent,
    isSending: Boolean,
    isDark: Boolean,
    onSendReminder: () -> Unit,
    onClick: () -> Unit,
) {
    val cardBg = if (isDark) Color(0xFF111113) else Color.White
    val cardBorder = if (isDark) Color(0xFF232326) else Color(0xFFE2E8F0)
    val innerCardBg = if (isDark) Color(0xFF1B1B1E) else Color(0xFFF8FAFC)
    val innerCardBorder = if (isDark) Color(0xFF2E2E32) else Color(0xFFE2E8F0)
    val textPrimary = if (isDark) Color(0xFFF1F5F9) else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    val textTertiary = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)
    val emeraldAccent = if (isDark) Color(0xFF10B981) else Color(0xFF059669)

    val initial = student.studentName.firstOrNull()?.uppercase() ?: "S"

    val (badgeText, badgeColor) = when (student.category) {
        "UNREAD_MATERIAL" -> "Materi Tertinggal" to AccentNeonAmber
        "OVERDUE_ASSIGNMENT" -> "Tugas Tertunda" to NeonWarning
        "LOW_SCORE" -> "Di Bawah KKM" to NeonError
        "QUIZ_ALERT" -> "Kuis & CBT" to Color(0xFFA855F7)
        else -> "Perlu Perhatian" to NeonError
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Top Row: Avatar + Name + Class + Category Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Avatar Box
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(innerCardBg)
                            .border(1.dp, innerCardBorder, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initial,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = textPrimary
                        )
                    }

                    Spacer(Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = student.studentName,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "NISN: ${student.nisn}",
                                fontSize = 11.sp,
                                color = textTertiary
                            )
                            Spacer(Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = innerCardBg,
                                border = BorderStroke(1.dp, innerCardBorder)
                            ) {
                                Text(
                                    text = student.className,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = emeraldAccent,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                // Category Badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = badgeColor.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = badgeText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = badgeColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Issue Description Box
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = innerCardBg,
                border = BorderStroke(1.dp, innerCardBorder)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = badgeColor,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (student.title.isNotBlank()) "${student.title}: ${student.description}" else student.description,
                        fontSize = 12.sp,
                        color = textPrimary,
                        lineHeight = 16.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Footer Row: Last Seen / Status & Send Reminder Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Perlu tindak lanjut guru",
                    fontSize = 10.5.sp,
                    color = textTertiary
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Send Single Reminder Button
                    Surface(
                        onClick = onSendReminder,
                        shape = RoundedCornerShape(8.dp),
                        color = if (student.isReminded) emeraldAccent.copy(alpha = 0.12f) else Color(0xFFEF4444).copy(alpha = 0.12f),
                        border = BorderStroke(
                            1.dp,
                            if (student.isReminded) emeraldAccent.copy(alpha = 0.35f) else Color(0xFFEF4444).copy(alpha = 0.35f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isSending) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(11.dp),
                                    strokeWidth = 1.5.dp,
                                    color = Color(0xFFEF4444)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "Mengirim...",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFEF4444)
                                )
                            } else if (student.isReminded) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = emeraldAccent,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "Terkirim",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = emeraldAccent
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = null,
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "Ingatkan",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFEF4444)
                                )
                            }
                        }
                    }

                    Spacer(Modifier.width(8.dp))

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = "Detail",
                        tint = textTertiary,
                        modifier = Modifier.size(11.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AtRiskStudentCardSkeleton(
    modifier: Modifier = Modifier,
) {
    val isDark = LocalIsDarkTheme.current
    val cardBg = if (isDark) Color(0xFF111113) else Color.White
    val cardBorder = if (isDark) Color(0xFF232326) else Color(0xFFE2E8F0)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(22.dp),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Top Row: Avatar + Name + Class + Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ShimmerBox(
                        modifier = Modifier.size(38.dp),
                        shape = RoundedCornerShape(10.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        ShimmerBox(
                            modifier = Modifier
                                .fillMaxWidth(0.65f)
                                .height(14.dp),
                            shape = RoundedCornerShape(4.dp)
                        )
                        Spacer(Modifier.height(5.dp))
                        ShimmerBox(
                            modifier = Modifier
                                .fillMaxWidth(0.4f)
                                .height(11.dp),
                            shape = RoundedCornerShape(4.dp)
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                ShimmerBox(
                    modifier = Modifier
                        .width(90.dp)
                        .height(20.dp),
                    shape = RoundedCornerShape(6.dp)
                )
            }

            Spacer(Modifier.height(12.dp))

            // Reason / Description skeleton
            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(30.dp),
                shape = RoundedCornerShape(8.dp)
            )

            Spacer(Modifier.height(12.dp))

            // Action button skeleton
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ShimmerBox(
                    modifier = Modifier
                        .width(120.dp)
                        .height(12.dp),
                    shape = RoundedCornerShape(4.dp)
                )
                ShimmerBox(
                    modifier = Modifier
                        .width(80.dp)
                        .height(24.dp),
                    shape = RoundedCornerShape(6.dp)
                )
            }
        }
    }
}
