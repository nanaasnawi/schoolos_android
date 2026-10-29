package com.schoolos.android.feature.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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

    Scaffold(
        containerColor = CosmicBlack,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (showHeader) {
                AtRiskTopHeader(
                    onBack = onBack,
                    isDark = isDark,
                    isRefreshing = state.isRefreshing,
                    onRefresh = { viewModel.refresh() }
                )
            }
        }
    ) { padding ->
        PullRefreshContainer(
            isRefreshing = state.isRefreshing,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
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
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(
                                    color = NeonError,
                                    strokeWidth = 2.5.dp,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    text = "Memeriksa progres belajar & kelulusan siswa...",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }
                        }
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
                        key = { _, item -> "${item.studentId}_${item.materialId ?: item.category}" }
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
}

// ─── Header Components ─────────────────────────────────────────────────────────

@Composable
private fun AtRiskTopHeader(
    onBack: (() -> Unit)?,
    isDark: Boolean,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding(),
        color = CosmicBlack
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
                            tint = TextPrimary
                        )
                    }
                }
                Column(modifier = Modifier.padding(start = if (onBack != null) 0.dp else 4.dp)) {
                    Text(
                        text = "Siswa Perlu Perhatian",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Early Warning System (At-Risk Monitoring)",
                        fontSize = 11.sp,
                        color = TextSecondary
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
                        tint = TextSecondary
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
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp),
        shape = RoundedCornerShape(12.dp),
        color = CosmicNavy,
        border = BorderStroke(0.5.dp, if (totalAtRisk > 0) NeonError.copy(alpha = 0.35f) else GlassBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
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
                        .size(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(NeonError.copy(alpha = 0.12f))
                        .border(0.5.dp, NeonError.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = NeonError,
                        modifier = Modifier.size(17.dp)
                    )
                }
                Column(verticalArrangement = Arrangement.Center) {
                    Text(
                        text = if (totalAtRisk > 0) "$totalAtRisk Siswa Perlu Perhatian" else "Kondisi Siswa Terpantau Aman",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (totalAtRisk > 0) "Kendala materi, tugas, atau nilai KKM" else "Tidak ada indikasi kendala belajar aktif",
                        fontSize = 11.sp,
                        color = TextSecondary,
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
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                    modifier = Modifier.height(32.dp),
                    shape = RoundedCornerShape(8.dp),
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatFilterTile(
            count = stats.total,
            label = "Semua",
            icon = Icons.Default.Warning,
            isSelected = selectedCategory == "ALL",
            accentColor = NeonBlue,
            onClick = { onSelectCategory("ALL") }
        )
        StatFilterTile(
            count = stats.unreadMaterials,
            label = "Materi",
            icon = Icons.AutoMirrored.Filled.MenuBook,
            isSelected = selectedCategory == "UNREAD_MATERIAL",
            accentColor = AccentNeonAmber,
            onClick = { onSelectCategory("UNREAD_MATERIAL") }
        )
        StatFilterTile(
            count = stats.overdueAssignments,
            label = "Tugas",
            icon = Icons.AutoMirrored.Filled.Assignment,
            isSelected = selectedCategory == "OVERDUE_ASSIGNMENT",
            accentColor = NeonWarning,
            onClick = { onSelectCategory("OVERDUE_ASSIGNMENT") }
        )
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

@Composable
private fun StatFilterTile(
    count: Int,
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
) {
    val bg = if (isSelected) accentColor.copy(alpha = 0.14f) else CosmicNavy
    val border = if (isSelected) accentColor.copy(alpha = 0.45f) else GlassBorder
    val fg = if (isSelected) accentColor else TextSecondary

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = bg,
        border = BorderStroke(if (isSelected) 1.dp else 0.5.dp, border)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = fg,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = count.toString(),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) TextPrimary else fg
            )
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isSelected) TextPrimary else TextTertiary
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Compact Search Field (Custom Surface with BasicTextField - Zero Clipping!)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(38.dp),
            shape = RoundedCornerShape(8.dp),
            color = CosmicNavy,
            border = BorderStroke(0.5.dp, GlassBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = TextTertiary,
                    modifier = Modifier.size(16.dp)
                )
                BasicTextField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    singleLine = true,
                    textStyle = TextStyle(
                        fontSize = 12.sp,
                        color = TextPrimary
                    ),
                    cursorBrush = SolidColor(NeonError),
                    modifier = Modifier.weight(1f),
                    decorationBox = { innerTextField ->
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "Cari siswa, NISN, atau kendala belajar...",
                                fontSize = 12.sp,
                                color = TextTertiary
                            )
                        }
                        innerTextField()
                    }
                )
                if (searchQuery.isNotBlank()) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear",
                        tint = TextSecondary,
                        modifier = Modifier
                            .size(16.dp)
                            .clickable(onClick = onClearSearch)
                    )
                }
            }
        }

        // Rombel Selector Chips (Horizontal Scroll)
        val rombelScroll = rememberScrollState()
        val allRombelOptions = listOf("ALL") + classes
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rombelScroll),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            allRombelOptions.forEach { rombel ->
                val isSelected = selectedClass == rombel
                val label = if (rombel == "ALL") "Semua Rombel" else rombel

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isSelected) NeonBlue.copy(alpha = 0.12f) else CosmicSurface2,
                    border = BorderStroke(
                        0.5.dp,
                        if (isSelected) NeonBlue.copy(alpha = 0.4f) else GlassBorder
                    ),
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onSelectClass(rombel) }
                ) {
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isSelected) NeonBlue else TextSecondary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
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
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (selected) accentColor.copy(alpha = 0.12f) else CosmicSurface2,
        border = BorderStroke(
            0.5.dp,
            if (selected) accentColor.copy(alpha = 0.4f) else GlassBorder
        )
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) accentColor else TextSecondary,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
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
    val initial = student.studentName.firstOrNull()?.uppercase() ?: "S"

    val (badgeText, badgeColor) = when (student.category) {
        "UNREAD_MATERIAL" -> "Materi Tertinggal" to AccentNeonAmber
        "OVERDUE_ASSIGNMENT" -> "Tugas Tertunda" to NeonWarning
        "LOW_SCORE" -> "Di Bawah KKM" to NeonError
        else -> "Perlu Perhatian" to NeonError
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = CosmicNavy,
        border = BorderStroke(0.5.dp, GlassBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
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
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(CosmicSurface2)
                            .border(0.5.dp, GlassBorder, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initial,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary
                        )
                    }

                    Spacer(Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = student.studentName,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "NISN: ${student.nisn}",
                                fontSize = 11.sp,
                                color = TextTertiary
                            )
                            Spacer(Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = CosmicSurface2,
                                border = BorderStroke(0.5.dp, GlassBorder)
                            ) {
                                Text(
                                    text = student.className,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonBlue,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                }

                // Category Badge
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = badgeColor.copy(alpha = 0.12f),
                    border = BorderStroke(0.5.dp, badgeColor.copy(alpha = 0.3f))
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

            Spacer(Modifier.height(10.dp))

            // Issue Description Box
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = CosmicSurface2,
                border = BorderStroke(0.5.dp, GlassBorder)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = badgeColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (student.title.isNotBlank()) "${student.title}: ${student.description}" else student.description,
                        fontSize = 11.5.sp,
                        color = TextPrimary,
                        lineHeight = 16.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // Footer Row: Last Seen / Status & Send Reminder Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Perlu tindak lanjut guru",
                    fontSize = 10.5.sp,
                    color = TextTertiary
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Send Single Reminder Button
                    Surface(
                        onClick = onSendReminder,
                        shape = RoundedCornerShape(6.dp),
                        color = if (student.isReminded) NeonSuccess.copy(alpha = 0.12f) else NeonError.copy(alpha = 0.12f),
                        border = BorderStroke(
                            0.5.dp,
                            if (student.isReminded) NeonSuccess.copy(alpha = 0.35f) else NeonError.copy(alpha = 0.35f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isSending) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(11.dp),
                                    strokeWidth = 1.5.dp,
                                    color = NeonError
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "Mengirim...",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonError
                                )
                            } else if (student.isReminded) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = NeonSuccess,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "Terkirim",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonSuccess
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = null,
                                    tint = NeonError,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "Ingatkan",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonError
                                )
                            }
                        }
                    }

                    Spacer(Modifier.width(8.dp))

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = "Detail",
                        tint = TextTertiary,
                        modifier = Modifier.size(11.dp)
                    )
                }
            }
        }
    }
}
