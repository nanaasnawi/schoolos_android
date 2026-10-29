package com.schoolos.android.feature.home.analytics

import java.util.Locale
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.schoolos.android.core.designsystem.CosmicBlack
import com.schoolos.android.core.designsystem.CosmicDark
import com.schoolos.android.core.designsystem.CosmicNavy
import com.schoolos.android.core.designsystem.GlassBorder
import com.schoolos.android.core.designsystem.InfoBg
import com.schoolos.android.core.designsystem.NeonBlue
import com.schoolos.android.core.designsystem.NeonSuccess
import com.schoolos.android.core.designsystem.NeonWarning
import com.schoolos.android.core.designsystem.PullRefreshContainer
import com.schoolos.android.core.designsystem.SuccessBg
import com.schoolos.android.core.designsystem.TextPrimary
import com.schoolos.android.core.designsystem.TextSecondary
import com.schoolos.android.core.designsystem.TextTertiary
import com.schoolos.android.core.designsystem.WarningBg
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
            // ── 1. MINIMALIST HERO HEADER (NO GRADIENT, NO BUTTONS) ──────────
            item {
                ReadingAnalyticsHeroHeader(
                    isDark = isDark,
                )
            }

            // ── 2. 4 KPI METRIC SUMMARY CARDS ────────────────────────────────
            item {
                Spacer(Modifier.height(14.dp))
                ReadingMetricsGrid(
                    metrics = state.dynamicMetrics,
                    isDark = isDark
                )
            }

            // ── 3. FILTER CONTROLS (ROMBEL, MATERI, SEARCH) ───────────────────
            item {
                Spacer(Modifier.height(16.dp))
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
                Spacer(Modifier.height(14.dp))
                ReadingStatusFilterTabs(
                    selectedStatus = state.selectedStatusFilter,
                    statusCounts = state.statusCounts,
                    isDark = isDark,
                    onSelectStatus = onSelectStatusFilter
                )
            }

            // ── 5. SECTION TITLE & STUDENT LIST ──────────────────────────────
            item {
                Spacer(Modifier.height(16.dp))
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
                        color = if (isDark) TextPrimary else Color(0xFF0F172A)
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
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (isDark) CosmicNavy else Color.White)
            .border(
                0.8.dp,
                if (isDark) GlassBorder else Color(0xFFE2E8F0),
                RoundedCornerShape(14.dp)
            )
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Badge Workstation
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isDark) Color(0x266366F1) else Color(0x184F46E5))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF6366F1))
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Teacher Workstation",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0xFFA5B4FC) else Color(0xFF4338CA)
                    )
                }

                // Live Sync badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isDark) Color(0x2210B981) else Color(0x1810B981))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(NeonSuccess)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Live Sync",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = NeonSuccess
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Text(
                text = "Teacher Analytics & Reading Progress",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) TextPrimary else Color(0xFF0F172A)
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = "Monitoring keterbacaan materi & capaian literasi digital siswa secara real-time.",
                fontSize = 12.sp,
                lineHeight = 16.sp,
                color = if (isDark) TextSecondary else Color(0xFF64748B)
            )
        }
    }
}

@Composable
private fun ReadingMetricsGrid(
    metrics: TeacherReadingMetrics,
    isDark: Boolean,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Card 1: Total Materi Terbit (Purple)
            MetricKpiCard(
                title = "Materi & Modul Terbit",
                value = metrics.totalMaterials.toString(),
                caption = "Total modul kurikulum digital",
                icon = Icons.AutoMirrored.Filled.MenuBook,
                accentColor = Color(0xFFA855F7),
                bgColor = if (isDark) Color(0x22A855F7) else Color(0x18A855F7),
                isDark = isDark,
                modifier = Modifier.weight(1f)
            )

            // Card 2: Siswa Aktif Membaca (Blue)
            MetricKpiCard(
                title = "Siswa Aktif Membaca",
                value = metrics.totalReaders.toString(),
                caption = "Peserta didik terdata membaca",
                icon = Icons.Default.Group,
                accentColor = NeonBlue,
                bgColor = if (isDark) InfoBg else Color(0x140284C7),
                isDark = isDark,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Card 3: Tingkat Ketercapaian (Emerald)
            MetricKpiCard(
                title = "Tingkat Ketercapaian",
                value = "${metrics.overallCompletionRate}%",
                caption = "${metrics.completedReaders} siswa tuntas membaca",
                icon = Icons.Default.CheckCircle,
                accentColor = NeonSuccess,
                bgColor = if (isDark) SuccessBg else Color(0x1410B981),
                isDark = isDark,
                modifier = Modifier.weight(1f)
            )

            // Card 4: Halaman Digital Kumulatif (Amber)
            MetricKpiCard(
                title = "Halaman Digital Kumulatif",
                value = String.format(Locale.US, "%,d", metrics.totalPagesRead),
                caption = "Total lembar materi dibaca",
                icon = Icons.Default.AutoStories,
                accentColor = NeonWarning,
                bgColor = if (isDark) WarningBg else Color(0x14F59E0B),
                isDark = isDark,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun MetricKpiCard(
    title: String,
    value: String,
    caption: String,
    icon: ImageVector,
    accentColor: Color,
    bgColor: Color,
    isDark: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (isDark) CosmicNavy else Color.White)
            .border(0.8.dp, if (isDark) GlassBorder else Color(0xFFE2E8F0), RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = value,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isDark) TextPrimary else Color(0xFF0F172A)
                )

                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(bgColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) TextSecondary else Color(0xFF334155),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(2.dp))

            Text(
                text = caption,
                fontSize = 10.sp,
                color = if (isDark) TextTertiary else Color(0xFF64748B),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
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
        verticalArrangement = Arrangement.spacedBy(10.dp)
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
                    color = if (isDark) CosmicNavy else Color.White,
                    border = androidx.compose.foundation.BorderStroke(
                        0.8.dp,
                        if (selectedClass != "ALL") NeonBlue else (if (isDark) GlassBorder else Color(0xFFCBD5E1))
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
                                color = if (isDark) TextTertiary else Color(0xFF64748B)
                            )
                            Text(
                                text = if (selectedClass == "ALL") "Semua Rombel" else selectedClass,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isDark) TextPrimary else Color(0xFF0F172A),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Pilih",
                            tint = if (isDark) TextTertiary else Color(0xFF94A3B8),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                DropdownMenu(
                    expanded = classDropdownOpen,
                    onDismissRequest = { classDropdownOpen = false },
                    modifier = Modifier.background(if (isDark) CosmicNavy else Color.White)
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
                    color = if (isDark) CosmicNavy else Color.White,
                    border = androidx.compose.foundation.BorderStroke(
                        0.8.dp,
                        if (selectedMaterialId != "ALL") Color(0xFFA855F7) else (if (isDark) GlassBorder else Color(0xFFCBD5E1))
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
                                color = if (isDark) TextTertiary else Color(0xFF64748B)
                            )
                            Text(
                                text = selectedTitle,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isDark) TextPrimary else Color(0xFF0F172A),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Pilih",
                            tint = if (isDark) TextTertiary else Color(0xFF94A3B8),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                DropdownMenu(
                    expanded = materialDropdownOpen,
                    onDismissRequest = { materialDropdownOpen = false },
                    modifier = Modifier.background(if (isDark) CosmicNavy else Color.White)
                ) {
                    DropdownMenuItem(
                        text = { Text("Semua Materi / Buku", fontWeight = if (selectedMaterialId == "ALL") FontWeight.Bold else FontWeight.Normal) },
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

        // Search Field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = {
                Text(
                    text = "Cari siswa, NISN, atau judul modul...",
                    fontSize = 12.sp,
                    color = if (isDark) TextTertiary else Color(0xFF94A3B8)
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = if (isDark) TextSecondary else Color(0xFF64748B),
                    modifier = Modifier.size(16.dp)
                )
            },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = onClearSearch) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear",
                            tint = if (isDark) TextSecondary else Color(0xFF64748B),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = if (isDark) CosmicNavy else Color.White,
                unfocusedContainerColor = if (isDark) CosmicNavy else Color.White,
                focusedBorderColor = NeonBlue,
                unfocusedBorderColor = if (isDark) GlassBorder else Color(0xFFCBD5E1),
                focusedTextColor = if (isDark) TextPrimary else Color(0xFF0F172A),
                unfocusedTextColor = if (isDark) TextPrimary else Color(0xFF0F172A),
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
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
            accentColor = Color(0xFF94A3B8),
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
        shape = RoundedCornerShape(20.dp),
        color = if (selected) {
            accentColor.copy(alpha = if (isDark) 0.22f else 0.15f)
        } else {
            if (isDark) CosmicNavy else Color(0xFFF1F5F9)
        },
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (selected) accentColor else (if (isDark) GlassBorder else Color(0xFFE2E8F0))
        )
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) accentColor else (if (isDark) TextSecondary else Color(0xFF475569)),
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
    val progressFraction = (row.completionPercentage.toFloat() / 100f).coerceIn(0f, 1f)
    val progressColor = when {
        row.isCompleted -> NeonSuccess
        row.currentPage > 0 -> NeonBlue
        else -> Color(0xFF64748B)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (isDark) CosmicNavy else Color.White)
            .border(0.8.dp, if (isDark) GlassBorder else Color(0xFFE2E8F0), RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header: Student Name + Class Badge + NISN
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
                        color = if (isDark) TextPrimary else Color(0xFF0F172A),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "NISN: ${row.nisn}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isDark) TextTertiary else Color(0xFF64748B)
                        )
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isDark) Color(0x2238BDF8) else Color(0x180284C7))
                                .padding(horizontal = 6.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = row.className,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)
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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isDark) CosmicDark else Color(0xFFF8FAFC))
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Column {
                    Text(
                        text = row.materialTitle,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDark) TextPrimary else Color(0xFF1E293B),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = row.subjectName,
                        fontSize = 10.sp,
                        color = if (isDark) TextSecondary else Color(0xFF64748B)
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
                    color = if (isDark) TextTertiary else Color(0xFF64748B)
                )
                Text(
                    text = "${row.currentPage} / ${row.totalPages} hlm (${row.completionPercentage}%)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = progressColor
                )
            }

            Spacer(Modifier.height(6.dp))

            LinearProgressIndicator(
                progress = { progressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = progressColor,
                trackColor = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0),
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
                    color = if (isDark) TextTertiary else Color(0xFF94A3B8)
                )

                Surface(
                    onClick = onClickDetail,
                    shape = RoundedCornerShape(6.dp),
                    color = if (isDark) Color(0x2238BDF8) else Color(0x180284C7),
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Text(
                        text = "Detail Siswa >",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
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
            if (isDark) Color(0x2210B981) else Color(0x1810B981),
            NeonSuccess,
            "Selesai"
        )
        currentPage > 0 -> Triple(
            if (isDark) Color(0x220284C7) else Color(0x180284C7),
            NeonBlue,
            "Sedang Baca"
        )
        else -> Triple(
            if (isDark) Color(0x2264748B) else Color(0x1864748B),
            Color(0xFF94A3B8),
            "Belum Mulai"
        )
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
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
            Icon(
                imageVector = Icons.Default.Description,
                contentDescription = null,
                tint = if (isDark) TextTertiary else Color(0xFFCBD5E1),
                modifier = Modifier.size(48.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Tidak Ada Data Pembaca",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) TextPrimary else Color(0xFF1E293B)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Sesuaikan filter rombel, materi, atau kata kunci pencarian Anda.",
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                color = if (isDark) TextSecondary else Color(0xFF64748B)
            )
        }
    }
}
