package com.schoolos.android.feature.home.analytics

import java.util.Locale
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.MenuBook
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
import com.schoolos.android.core.designsystem.*
import com.schoolos.android.domain.model.StudentReadingRow
import com.schoolos.android.domain.model.TeacherReadingMetrics

@Composable
fun TeacherReadingAnalyticsContent(
    state: TeacherReadingAnalyticsUiState,
    isDark: Boolean,
    onRefresh: () -> Unit,
    onSelectClassFilter: (String) -> Unit,
    onSelectMaterialFilter: (String) -> Unit,
    onSelectStatusFilter: (String) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onNavigateToStudentDetail: ((studentId: String, name: String, nisn: String, className: String) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current

    PullRefreshContainer(
        isRefreshing = state.isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            // ── 1. MINIMALIST HERO HEADER (NO GRADIENT, PURE SOLID DESIGN) ────
            item {
                ReadingAnalyticsHeroHeader(
                    isDark = isDark,
                )
            }

            // ── 2. 4 KPI METRIC SUMMARY CARDS ────────────────────────────────
            item {
                Spacer(Modifier.height(6.dp))
                ReadingMetricsGrid(
                    metrics = state.dynamicMetrics,
                    isDark = isDark
                )
            }

            // ── 3. FILTER CONTROLS (SEARCH & DROPDOWNS) ──────────────────────
            item {
                Spacer(Modifier.height(6.dp))
                ReadingFilterSection(
                    classes = state.availableClasses,
                    selectedClass = state.selectedClassFilter,
                    onSelectClass = onSelectClassFilter,
                    materials = state.materials.map { it.materialId to it.title },
                    selectedMaterialId = state.selectedMaterialFilter,
                    onSelectMaterial = onSelectMaterialFilter,
                    searchQuery = state.searchQuery,
                    onSearchChange = onSearchQueryChange,
                    onClearSearch = { onSearchQueryChange(""); focusManager.clearFocus() },
                    isDark = isDark
                )
            }

            // ── 4. STATUS FILTER TABS ────────────────────────────────────────
            item {
                Spacer(Modifier.height(6.dp))
                ReadingStatusFilterTabs(
                    selectedStatus = state.selectedStatusFilter,
                    statusCounts = state.statusCounts,
                    isDark = isDark,
                    onSelectStatus = onSelectStatusFilter
                )
            }

            // ── 5. SECTION TITLE & STUDENT LIST ──────────────────────────────
            item {
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Daftar Peserta Didik (${state.filteredRows.size})",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    if (state.selectedClassFilter != "ALL" || state.selectedMaterialFilter != "ALL" || state.selectedStatusFilter != "ALL" || state.searchQuery.isNotBlank()) {
                        Text(
                            text = "Reset Filter",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = NeonBlue,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable {
                                    onSelectClassFilter("ALL")
                                    onSelectMaterialFilter("ALL")
                                    onSelectStatusFilter("ALL")
                                    onSearchQueryChange("")
                                }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            if (state.isLoading && state.allReadingRows.isEmpty()) {
                items(5) {
                    ReadingProgressSkeletonCard(isDark = isDark)
                    Spacer(Modifier.height(10.dp))
                }
            } else if (state.filteredRows.isEmpty()) {
                item {
                    EmptyReadingState(isDark = isDark)
                }
            } else {
                items(
                    items = state.filteredRows,
                    key = { "${it.studentId}_${it.materialId}" }
                ) { row ->
                    StudentReadingProgressCard(
                        row = row,
                        isDark = isDark,
                        onClickDetail = {
                            onNavigateToStudentDetail?.invoke(row.studentId, row.studentName, row.nisn, row.className)
                        }
                    )
                    Spacer(Modifier.height(10.dp))
                }
            }
        }
    }
}

// ── SUBCOMPONENTS (MINIMALIST & ZERO GRADIENT) ────────────────────────────────

@Composable
private fun ReadingAnalyticsHeroHeader(
    isDark: Boolean,
) {
    val cardBg = if (isDark) Color(0xFF111113) else Color.White
    val cardBorder = if (isDark) Color(0xFF232326) else Color(0xFFE2E8F0)
    val textPrimary = if (isDark) Color(0xFFF1F5F9) else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    val emeraldAccent = if (isDark) Color(0xFF10B981) else Color(0xFF059669)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp),
        shape = RoundedCornerShape(22.dp),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder)
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
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(emeraldAccent.copy(alpha = 0.12f))
                        .border(1.dp, emeraldAccent.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.MenuBook,
                        contentDescription = null,
                        tint = emeraldAccent,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(
                        text = "Reading Progress",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Monitoring progress literasi",
                        fontSize = 11.sp,
                        color = textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = emeraldAccent.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, emeraldAccent.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(emeraldAccent)
                    )
                    Text(
                        text = "Live Sync",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = emeraldAccent
                    )
                }
            }
        }
    }
}

@Composable
private fun ReadingMetricsGrid(
    metrics: TeacherReadingMetrics,
    isDark: Boolean,
) {
    val emeraldAccent = if (isDark) Color(0xFF10B981) else Color(0xFF059669)

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            CompactMetricTile(
                title = "Materi Terbit",
                value = metrics.totalMaterials.toString(),
                icon = Icons.AutoMirrored.Filled.MenuBook,
                accentColor = emeraldAccent,
                isDark = isDark
            )
        }
        item {
            CompactMetricTile(
                title = "Siswa Pembaca",
                value = metrics.totalReaders.toString(),
                icon = Icons.Default.Group,
                accentColor = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7),
                isDark = isDark
            )
        }
        item {
            CompactMetricTile(
                title = "Ketercapaian",
                value = "${metrics.overallCompletionRate}%",
                icon = Icons.Default.CheckCircle,
                accentColor = emeraldAccent,
                isDark = isDark
            )
        }
        item {
            CompactMetricTile(
                title = "Total Halaman",
                value = String.format(Locale.US, "%,d", metrics.totalPagesRead),
                icon = Icons.Default.AutoStories,
                accentColor = if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706),
                isDark = isDark
            )
        }
    }
}

@Composable
private fun CompactMetricTile(
    title: String,
    value: String,
    icon: ImageVector,
    accentColor: Color,
    isDark: Boolean,
) {
    val cardBg = if (isDark) Color(0xFF111113) else Color.White
    val cardBorder = if (isDark) Color(0xFF232326) else Color(0xFFE2E8F0)
    val textPrimary = if (isDark) Color(0xFFF1F5F9) else Color(0xFF0F172A)
    val textTertiary = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = value,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary
            )
            Text(
                text = title,
                fontSize = 11.sp,
                color = textTertiary
            )
        }
    }
}

@Composable
private fun ReadingFilterSection(
    classes: List<String>,
    selectedClass: String,
    onSelectClass: (String) -> Unit,
    materials: List<Pair<String, String>>,
    selectedMaterialId: String,
    onSelectMaterial: (String) -> Unit,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    isDark: Boolean,
) {
    var classDropdownOpen by remember { mutableStateOf(false) }
    var materialDropdownOpen by remember { mutableStateOf(false) }

    val cardBg = if (isDark) Color(0xFF111113) else Color.White
    val cardBorder = if (isDark) Color(0xFF232326) else Color(0xFFE2E8F0)
    val menuBg = if (isDark) Color(0xFF1B1B1E) else Color.White
    val textPrimary = if (isDark) Color(0xFFF1F5F9) else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    val textTertiary = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)
    val emeraldAccent = if (isDark) Color(0xFF10B981) else Color(0xFF059669)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Dropdowns Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Rombel Dropdown
            Box(modifier = Modifier.weight(1f)) {
                Surface(
                    onClick = { classDropdownOpen = true },
                    shape = RoundedCornerShape(14.dp),
                    color = cardBg,
                    border = BorderStroke(
                        1.dp,
                        if (selectedClass != "ALL") emeraldAccent else cardBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f, fill = false)) {
                            Text(
                                text = "Rombel",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = textTertiary
                            )
                            Text(
                                text = if (selectedClass == "ALL") "Semua Rombel" else selectedClass,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (selectedClass != "ALL") emeraldAccent else textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Pilih",
                            tint = textTertiary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                if (classDropdownOpen) {
                    val classOptions = remember(classes) { listOf("Semua Rombel") + classes }
                    val currentClassLabel = if (selectedClass == "ALL") "Semua Rombel" else selectedClass
                    ExecutiveSelectionBottomSheet(
                        title = "Pilih Filter Rombel",
                        subtitle = "Tampilkan analitik bacaan berdasarkan kelas",
                        options = classOptions,
                        selectedValue = currentClassLabel,
                        accentColor = emeraldAccent,
                        onDismiss = { classDropdownOpen = false },
                        onSelect = { chosen ->
                            onSelectClass(if (chosen == "Semua Rombel") "ALL" else chosen)
                            classDropdownOpen = false
                        }
                    )
                }
            }

            // Materi Dropdown
            Box(modifier = Modifier.weight(1.3f)) {
                val selectedTitle = materials.firstOrNull { it.first == selectedMaterialId }?.second ?: "Semua Materi"

                Surface(
                    onClick = { materialDropdownOpen = true },
                    shape = RoundedCornerShape(14.dp),
                    color = cardBg,
                    border = BorderStroke(
                        1.dp,
                        if (selectedMaterialId != "ALL") emeraldAccent else cardBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f, fill = false)) {
                            Text(
                                text = "Materi / Modul",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = textTertiary
                            )
                            Text(
                                text = selectedTitle,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (selectedMaterialId != "ALL") emeraldAccent else textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Pilih",
                            tint = textTertiary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                if (materialDropdownOpen) {
                    val materialOptions = remember(materials) {
                        listOf("Semua Materi / Modul") + materials.map { it.second }
                    }
                    val currentMatLabel = if (selectedMaterialId == "ALL") "Semua Materi / Modul" else selectedTitle
                    ExecutiveSelectionBottomSheet(
                        title = "Pilih Filter Materi & Modul",
                        subtitle = "Filter progres bacaan warga belajar per materi",
                        options = materialOptions,
                        selectedValue = currentMatLabel,
                        accentColor = emeraldAccent,
                        onDismiss = { materialDropdownOpen = false },
                        onSelect = { chosen ->
                            if (chosen == "Semua Materi / Modul") {
                                onSelectMaterial("ALL")
                            } else {
                                val foundId = materials.firstOrNull { it.second == chosen }?.first ?: "ALL"
                                onSelectMaterial(foundId)
                            }
                            materialDropdownOpen = false
                        }
                    )
                }
            }
        }

        // Compact Search Field
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            shape = RoundedCornerShape(14.dp),
            color = cardBg,
            border = BorderStroke(
                1.dp,
                if (searchQuery.isNotBlank()) emeraldAccent else cardBorder
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
                    tint = if (searchQuery.isNotBlank()) emeraldAccent else textTertiary,
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
                    cursorBrush = SolidColor(emeraldAccent),
                    modifier = Modifier.weight(1f),
                    decorationBox = { innerTextField ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Cari siswa, NISN, atau judul modul...",
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
    }
}

@Composable
private fun ReadingStatusFilterTabs(
    selectedStatus: String,
    statusCounts: ReadingStatusCounts,
    isDark: Boolean,
    onSelectStatus: (String) -> Unit,
) {
    val emeraldAccent = if (isDark) Color(0xFF10B981) else Color(0xFF059669)

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            StatusFilterPill(
                label = "Semua (${statusCounts.all})",
                selected = selectedStatus == "ALL",
                accentColor = emeraldAccent,
                isDark = isDark,
                onClick = { onSelectStatus("ALL") }
            )
        }

        item {
            StatusFilterPill(
                label = "Selesai (${statusCounts.completed})",
                selected = selectedStatus == "COMPLETED",
                accentColor = emeraldAccent,
                isDark = isDark,
                onClick = { onSelectStatus("COMPLETED") }
            )
        }

        item {
            StatusFilterPill(
                label = "Sedang Baca (${statusCounts.inProgress})",
                selected = selectedStatus == "IN_PROGRESS",
                accentColor = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7),
                isDark = isDark,
                onClick = { onSelectStatus("IN_PROGRESS") }
            )
        }

        item {
            StatusFilterPill(
                label = "Belum Mulai (${statusCounts.notStarted})",
                selected = selectedStatus == "NOT_STARTED",
                accentColor = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8),
                isDark = isDark,
                onClick = { onSelectStatus("NOT_STARTED") }
            )
        }
    }
}

@Composable
private fun StatusFilterPill(
    label: String,
    selected: Boolean,
    accentColor: Color,
    isDark: Boolean,
    onClick: () -> Unit,
) {
    val pillBg = if (selected) accentColor.copy(alpha = if (isDark) 0.18f else 0.12f) else (if (isDark) Color(0xFF1B1B1E) else Color(0xFFF1F5F9))
    val pillBorder = if (selected) accentColor else (if (isDark) Color(0xFF2E2E32) else Color(0xFFE2E8F0))
    val pillText = if (selected) accentColor else (if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B))

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = pillBg,
        border = BorderStroke(1.dp, pillBorder)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = pillText,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
        )
    }
}

@Composable
private fun StudentReadingProgressCard(
    row: StudentReadingRow,
    isDark: Boolean,
    onClickDetail: () -> Unit,
) {
    val cardBg = if (isDark) Color(0xFF111113) else Color.White
    val cardBorder = if (isDark) Color(0xFF232326) else Color(0xFFE2E8F0)
    val innerCardBg = if (isDark) Color(0xFF1B1B1E) else Color(0xFFF8FAFC)
    val innerCardBorder = if (isDark) Color(0xFF2E2E32) else Color(0xFFE2E8F0)
    val textPrimary = if (isDark) Color(0xFFF1F5F9) else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    val textTertiary = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)
    val emeraldAccent = if (isDark) Color(0xFF10B981) else Color(0xFF059669)

    val progressFraction = (row.completionPercentage.toFloat() / 100f).coerceIn(0f, 1f)
    val progressColor = when {
        row.isCompleted -> emeraldAccent
        row.currentPage > 0 -> if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)
        else -> textTertiary
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable(onClick = onClickDetail),
        shape = RoundedCornerShape(22.dp),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header: Student Name + Class Badge + NISN + Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = row.studentName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(3.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "NISN: ${row.nisn}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = textTertiary
                        )
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = innerCardBg,
                            border = BorderStroke(1.dp, innerCardBorder)
                        ) {
                            Text(
                                text = row.className,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = emeraldAccent,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // Status Badge
                ReadingStatusBadge(
                    isCompleted = row.isCompleted,
                    currentPage = row.currentPage,
                    isDark = isDark
                )
            }

            Spacer(Modifier.height(12.dp))

            // Material title & subject
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = innerCardBg,
                border = BorderStroke(1.dp, innerCardBorder)
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                    Text(
                        text = row.materialTitle,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = row.subjectName,
                        fontSize = 11.sp,
                        color = textSecondary
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Progress Bar & Stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Progress Membaca:",
                    fontSize = 11.sp,
                    color = textTertiary
                )
                Text(
                    text = "${row.currentPage} / ${row.totalPages} hlm (${row.completionPercentage}%)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = progressColor
                )
            }

            Spacer(Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { progressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = progressColor,
                trackColor = if (isDark) Color(0xFF232326) else Color(0xFFE2E8F0),
            )

            Spacer(Modifier.height(12.dp))

            // Footer Row: Last Read & Detail Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val lastRead = row.lastReadAt
                Text(
                    text = if (lastRead != null) "Terakhir: ${lastRead.take(16).replace("T", " ")}" else "Belum ada aktivitas",
                    fontSize = 10.sp,
                    color = textTertiary
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable(onClick = onClickDetail)
                ) {
                    Text(
                        text = "Detail Siswa",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = emeraldAccent
                    )
                    Spacer(Modifier.width(3.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = null,
                        tint = emeraldAccent,
                        modifier = Modifier.size(10.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ReadingStatusBadge(
    isCompleted: Boolean,
    currentPage: Int,
    isDark: Boolean,
) {
    val emeraldAccent = if (isDark) Color(0xFF10B981) else Color(0xFF059669)
    val blueAccent = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)
    val textTertiary = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)
    val innerCardBg = if (isDark) Color(0xFF1B1B1E) else Color(0xFFF1F5F9)
    val innerCardBorder = if (isDark) Color(0xFF2E2E32) else Color(0xFFCBD5E1)

    val (bgColor, textColor, border, text) = when {
        isCompleted -> Quad(
            emeraldAccent.copy(alpha = 0.12f),
            emeraldAccent,
            emeraldAccent.copy(alpha = 0.3f),
            "Selesai"
        )
        currentPage > 0 -> Quad(
            blueAccent.copy(alpha = 0.12f),
            blueAccent,
            blueAccent.copy(alpha = 0.3f),
            "Sedang Baca"
        )
        else -> Quad(
            innerCardBg,
            textTertiary,
            innerCardBorder,
            "Belum Mulai"
        )
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = bgColor,
        border = BorderStroke(1.dp, border)
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Composable
private fun EmptyReadingState(isDark: Boolean) {
    val innerCardBg = if (isDark) Color(0xFF1B1B1E) else Color(0xFFF1F5F9)
    val innerCardBorder = if (isDark) Color(0xFF2E2E32) else Color(0xFFE2E8F0)
    val textPrimary = if (isDark) Color(0xFFF1F5F9) else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    val textTertiary = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 48.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                modifier = Modifier.size(52.dp),
                shape = CircleShape,
                color = innerCardBg,
                border = BorderStroke(1.dp, innerCardBorder)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = textTertiary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            Text(
                text = "Tidak Ada Data Pembaca",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Sesuaikan filter rombel, materi, atau kata kunci pencarian Anda.",
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                color = textSecondary
            )
        }
    }
}

@Composable
private fun ReadingProgressSkeletonCard(
    isDark: Boolean,
) {
    val cardBg = if (isDark) Color(0xFF111113) else Color.White
    val cardBorder = if (isDark) Color(0xFF232326) else Color(0xFFE2E8F0)
    val innerCardBg = if (isDark) Color(0xFF1B1B1E) else Color(0xFFF8FAFC)
    val innerCardBorder = if (isDark) Color(0xFF2E2E32) else Color(0xFFE2E8F0)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(22.dp),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header: Name + NISN + Class Badge + Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    ShimmerBox(
                        modifier = Modifier.width(140.dp).height(16.dp),
                        shape = RoundedCornerShape(4.dp)
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ShimmerBox(
                            modifier = Modifier.width(90.dp).height(12.dp),
                            shape = RoundedCornerShape(4.dp)
                        )
                        ShimmerBox(
                            modifier = Modifier.width(64.dp).height(18.dp),
                            shape = RoundedCornerShape(6.dp)
                        )
                    }
                }
                ShimmerBox(
                    modifier = Modifier.width(60.dp).height(20.dp),
                    shape = RoundedCornerShape(8.dp)
                )
            }

            // Material title box placeholder
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = innerCardBg,
                border = BorderStroke(1.dp, innerCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ShimmerBox(
                        modifier = Modifier.fillMaxWidth(0.85f).height(14.dp),
                        shape = RoundedCornerShape(4.dp)
                    )
                    ShimmerBox(
                        modifier = Modifier.width(70.dp).height(11.dp),
                        shape = RoundedCornerShape(4.dp)
                    )
                }
            }

            // Progress bar placeholder
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    ShimmerBox(
                        modifier = Modifier.width(110.dp).height(12.dp),
                        shape = RoundedCornerShape(3.dp)
                    )
                    ShimmerBox(
                        modifier = Modifier.width(70.dp).height(12.dp),
                        shape = RoundedCornerShape(3.dp)
                    )
                }
                ShimmerBox(
                    modifier = Modifier.fillMaxWidth().height(6.dp),
                    shape = RoundedCornerShape(3.dp)
                )
            }
        }
    }
}
