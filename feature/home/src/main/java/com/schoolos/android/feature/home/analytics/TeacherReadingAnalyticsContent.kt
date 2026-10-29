package com.schoolos.android.feature.home.analytics

import java.util.Locale
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
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
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(36.dp),
                            color = NeonBlue,
                            strokeWidth = 3.dp
                        )
                    }
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
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp),
        shape = RoundedCornerShape(12.dp),
        color = CosmicNavy,
        border = BorderStroke(0.5.dp, GlassBorder)
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
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(NeonBlue.copy(alpha = 0.12f))
                        .border(0.5.dp, NeonBlue.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.MenuBook,
                        contentDescription = null,
                        tint = NeonBlue,
                        modifier = Modifier.size(17.dp)
                    )
                }
                Column(verticalArrangement = Arrangement.Center) {
                    Text(
                        text = "Teacher Analytics & Reading Progress",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Monitoring progres literasi & keterbacaan materi",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(6.dp),
                color = NeonSuccess.copy(alpha = 0.1f),
                border = BorderStroke(0.5.dp, NeonSuccess.copy(alpha = 0.25f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(NeonSuccess)
                    )
                    Text(
                        text = "Live Sync",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = NeonSuccess
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CompactMetricTile(
            title = "Materi Terbit",
            value = metrics.totalMaterials.toString(),
            icon = Icons.AutoMirrored.Filled.MenuBook,
            accentColor = NeonBlue
        )
        CompactMetricTile(
            title = "Siswa Pembaca",
            value = metrics.totalReaders.toString(),
            icon = Icons.Default.Group,
            accentColor = StudentNeon
        )
        CompactMetricTile(
            title = "Ketercapaian",
            value = "${metrics.overallCompletionRate}%",
            icon = Icons.Default.CheckCircle,
            accentColor = NeonSuccess
        )
        CompactMetricTile(
            title = "Total Halaman",
            value = String.format(Locale.US, "%,d", metrics.totalPagesRead),
            icon = Icons.Default.AutoStories,
            accentColor = AccentNeonAmber
        )
    }
}

@Composable
private fun CompactMetricTile(
    title: String,
    value: String,
    icon: ImageVector,
    accentColor: Color,
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = CosmicNavy,
        border = BorderStroke(0.5.dp, GlassBorder)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = value,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = title,
                fontSize = 11.sp,
                color = TextTertiary
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
                    shape = RoundedCornerShape(10.dp),
                    color = CosmicNavy,
                    border = BorderStroke(
                        0.5.dp,
                        if (selectedClass != "ALL") NeonBlue else GlassBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f, fill = false)) {
                            Text(
                                text = "Rombel",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextTertiary
                            )
                            Text(
                                text = if (selectedClass == "ALL") "Semua Rombel" else selectedClass,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (selectedClass != "ALL") NeonBlue else TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Pilih",
                            tint = TextTertiary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                DropdownMenu(
                    expanded = classDropdownOpen,
                    onDismissRequest = { classDropdownOpen = false },
                    modifier = Modifier.background(CosmicNavy)
                ) {
                    DropdownMenuItem(
                        text = { Text("Semua Rombel", fontWeight = if (selectedClass == "ALL") FontWeight.Bold else FontWeight.Normal) },
                        onClick = {
                            onSelectClass("ALL")
                            classDropdownOpen = false
                        }
                    )
                    classes.forEach { cls ->
                        DropdownMenuItem(
                            text = { Text(cls, fontWeight = if (selectedClass == cls) FontWeight.Bold else FontWeight.Normal) },
                            onClick = {
                                onSelectClass(cls)
                                classDropdownOpen = false
                            }
                        )
                    }
                }
            }

            // Materi Dropdown
            Box(modifier = Modifier.weight(1.3f)) {
                val selectedTitle = materials.firstOrNull { it.first == selectedMaterialId }?.second ?: "Semua Materi"

                Surface(
                    onClick = { materialDropdownOpen = true },
                    shape = RoundedCornerShape(10.dp),
                    color = CosmicNavy,
                    border = BorderStroke(
                        0.5.dp,
                        if (selectedMaterialId != "ALL") NeonBlue else GlassBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f, fill = false)) {
                            Text(
                                text = "Materi / Modul",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextTertiary
                            )
                            Text(
                                text = selectedTitle,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (selectedMaterialId != "ALL") NeonBlue else TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Pilih",
                            tint = TextTertiary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                DropdownMenu(
                    expanded = materialDropdownOpen,
                    onDismissRequest = { materialDropdownOpen = false },
                    modifier = Modifier.background(CosmicNavy)
                ) {
                    DropdownMenuItem(
                        text = { Text("Semua Materi / Modul", fontWeight = if (selectedMaterialId == "ALL") FontWeight.Bold else FontWeight.Normal) },
                        onClick = {
                            onSelectMaterial("ALL")
                            materialDropdownOpen = false
                        }
                    )
                    materials.forEach { (mId, mTitle) ->
                        DropdownMenuItem(
                            text = { Text(mTitle, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = if (selectedMaterialId == mId) FontWeight.Bold else FontWeight.Normal) },
                            onClick = {
                                onSelectMaterial(mId)
                                materialDropdownOpen = false
                            }
                        )
                    }
                }
            }
        }

        // Compact Search Field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = {
                Text(
                    text = "Cari siswa, NISN, atau judul modul...",
                    fontSize = 12.sp,
                    color = TextTertiary
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = TextTertiary,
                    modifier = Modifier.size(16.dp)
                )
            },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = onClearSearch) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear",
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = CosmicNavy,
                unfocusedContainerColor = CosmicNavy,
                focusedBorderColor = NeonBlue,
                unfocusedBorderColor = GlassBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
        )
    }
}

@Composable
private fun ReadingStatusFilterTabs(
    selectedStatus: String,
    statusCounts: ReadingStatusCounts,
    isDark: Boolean,
    onSelectStatus: (String) -> Unit,
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatusFilterPill(
            label = "Semua (${statusCounts.all})",
            selected = selectedStatus == "ALL",
            accentColor = NeonBlue,
            isDark = isDark,
            onClick = { onSelectStatus("ALL") }
        )

        StatusFilterPill(
            label = "Selesai (${statusCounts.completed})",
            selected = selectedStatus == "COMPLETED",
            accentColor = NeonSuccess,
            isDark = isDark,
            onClick = { onSelectStatus("COMPLETED") }
        )

        StatusFilterPill(
            label = "Sedang Baca (${statusCounts.inProgress})",
            selected = selectedStatus == "IN_PROGRESS",
            accentColor = NeonBlue,
            isDark = isDark,
            onClick = { onSelectStatus("IN_PROGRESS") }
        )

        StatusFilterPill(
            label = "Belum Mulai (${statusCounts.notStarted})",
            selected = selectedStatus == "NOT_STARTED",
            accentColor = TextTertiary,
            isDark = isDark,
            onClick = { onSelectStatus("NOT_STARTED") }
        )
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

@Composable
private fun StudentReadingProgressCard(
    row: StudentReadingRow,
    isDark: Boolean,
    onClickDetail: () -> Unit,
) {
    val progressFraction = (row.completionPercentage.toFloat() / 100f).coerceIn(0f, 1f)
    val progressColor = when {
        row.isCompleted -> NeonSuccess
        row.currentPage > 0 -> NeonBlue
        else -> TextTertiary
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable(onClick = onClickDetail),
        shape = RoundedCornerShape(14.dp),
        color = CosmicNavy,
        border = BorderStroke(0.5.dp, GlassBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
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
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "NISN: ${row.nisn}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextTertiary
                        )
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = CosmicSurface2,
                            border = BorderStroke(0.5.dp, GlassBorder)
                        ) {
                            Text(
                                text = row.className,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonBlue,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
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

            Spacer(Modifier.height(10.dp))

            // Material title & subject
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = CosmicSurface2,
                border = BorderStroke(0.5.dp, GlassBorder)
            ) {
                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                    Text(
                        text = row.materialTitle,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = row.subjectName,
                        fontSize = 10.sp,
                        color = TextSecondary
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // Progress Bar & Stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Progress Membaca:",
                    fontSize = 11.sp,
                    color = TextTertiary
                )
                Text(
                    text = "${row.currentPage} / ${row.totalPages} hlm (${row.completionPercentage}%)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = progressColor
                )
            }

            Spacer(Modifier.height(6.dp))

            LinearProgressIndicator(
                progress = { progressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = progressColor,
                trackColor = CosmicSurface2,
            )

            Spacer(Modifier.height(10.dp))

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
                    color = TextTertiary
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable(onClick = onClickDetail)
                ) {
                    Text(
                        text = "Detail Siswa",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = NeonBlue
                    )
                    Spacer(Modifier.width(3.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = null,
                        tint = NeonBlue,
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
    val (bgColor, textColor, text) = when {
        isCompleted -> Triple(
            NeonSuccess.copy(alpha = 0.12f),
            NeonSuccess,
            "Selesai"
        )
        currentPage > 0 -> Triple(
            NeonBlue.copy(alpha = 0.12f),
            NeonBlue,
            "Sedang Baca"
        )
        else -> Triple(
            CosmicSurface2,
            TextTertiary,
            "Belum Mulai"
        )
    }

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = bgColor,
        border = BorderStroke(0.5.dp, textColor.copy(alpha = 0.3f))
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

@Composable
private fun EmptyReadingState(isDark: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 48.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = CosmicSurface2,
                border = BorderStroke(0.5.dp, GlassBorder)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = TextTertiary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Tidak Ada Data Pembaca",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Sesuaikan filter rombel, materi, atau kata kunci pencarian Anda.",
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                color = TextSecondary
            )
        }
    }
}
