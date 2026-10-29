package com.schoolos.android.feature.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.schoolos.android.core.R as CoreR
import com.schoolos.android.core.designsystem.CosmicBlack
import com.schoolos.android.core.designsystem.CosmicDark
import com.schoolos.android.core.designsystem.CosmicNavy
import com.schoolos.android.core.designsystem.ErrorBg
import com.schoolos.android.core.designsystem.GlassBorder
import com.schoolos.android.core.designsystem.GlassBorder2
import com.schoolos.android.core.designsystem.InfoBg
import com.schoolos.android.core.designsystem.LocalIsDarkTheme
import com.schoolos.android.core.designsystem.NeonBlue
import com.schoolos.android.core.designsystem.NeonError
import com.schoolos.android.core.designsystem.NeonSuccess
import com.schoolos.android.core.designsystem.NeonWarning
import com.schoolos.android.core.designsystem.SuccessBg
import com.schoolos.android.core.designsystem.TextDisabled
import com.schoolos.android.core.designsystem.TextPrimary
import com.schoolos.android.core.designsystem.TextSecondary
import com.schoolos.android.core.designsystem.TextTertiary
import com.schoolos.android.core.designsystem.WarningBg
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            // 1. Executive Early Warning Hero Banner
            item {
                EarlyWarningHeroBanner(
                    totalAtRisk = state.stats.total,
                    isSendingBatch = state.isSendingBatch,
                    isDark = isDark,
                    onSendBatch = { viewModel.sendBatchReminder() }
                )
            }

            // 2. 4 Stat KPI Cards
            item {
                Spacer(Modifier.height(14.dp))
                StatKpiGrid(
                    stats = state.stats,
                    isDark = isDark
                )
            }

            // 3. Category Filter Tabs
            item {
                Spacer(Modifier.height(18.dp))
                CategoryFilterTabs(
                    selectedCategory = state.selectedCategoryFilter,
                    stats = state.stats,
                    isDark = isDark,
                    onSelect = { viewModel.onSelectCategoryFilter(it) }
                )
            }

            // 4. Class Selector Chips & Search Bar
            item {
                Spacer(Modifier.height(14.dp))
                RombelSelectorChips(
                    classes = state.availableClasses.map { it.name },
                    selectedClass = state.selectedClassFilter,
                    isDark = isDark,
                    onSelect = { viewModel.onSelectClassFilter(it) }
                )
                Spacer(Modifier.height(10.dp))
                SearchInputBar(
                    query = state.searchQuery,
                    onQueryChange = { viewModel.onSearchQueryChanged(it) },
                    onClear = { viewModel.onSearchQueryChanged("") },
                    isDark = isDark
                )
                Spacer(Modifier.height(14.dp))
            }

            // 5. Section Header & Student Count
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "DAFTAR SISWA PERLU PERHATIAN",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp,
                        color = TextTertiary
                    )
                    Text(
                        text = "${state.filteredStudents.size} Peserta Didik",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = NeonError
                    )
                }
                Spacer(Modifier.height(8.dp))
            }

            // 6. Loading / Error / Empty States
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
                                modifier = Modifier.size(40.dp)
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
                                modifier = Modifier.size(56.dp),
                                shape = CircleShape,
                                color = SuccessBg
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = NeonSuccess,
                                        modifier = Modifier.size(28.dp)
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
                // 7. Student Roster Cards
                itemsIndexed(
                    items = state.filteredStudents,
                    key = { _, item -> "${item.studentId}_${item.materialId ?: item.category}" }
                ) { index, student ->
                    AtRiskStudentItemCard(
                        index = index + 1,
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
                    Spacer(Modifier.height(8.dp))
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
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Kembali",
                            tint = TextPrimary
                        )
                    }
                }
                Column(modifier = Modifier.padding(start = if (onBack != null) 0.dp else 4.dp)) {
                    Text(
                        text = "Siswa Perlu Perhatian",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Early Warning System (At-Risk Monitoring)",
                        fontSize = 11.5.sp,
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
    val bannerBg = if (isDark) CosmicNavy else Color.White
    val bannerBorder = if (isDark) GlassBorder else Color(0xFFE2E8F0)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(bannerBg)
            .border(1.dp, bannerBorder, RoundedCornerShape(18.dp))
            .padding(16.dp)
    ) {
        Column {
            // Top Badge
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isDark) Color(0xFF450A0A) else Color(0xFFFEE2E2))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(NeonError)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "SUPERVISI EARLY WARNING SYSTEM — KEPALA SEKOLAH",
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.4.sp,
                    color = NeonError
                )
            }

            Spacer(Modifier.height(10.dp))

            // Title & Warning Icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Surface(
                    modifier = Modifier.size(38.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = NeonError
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Daftar Siswa Perlu Perhatian",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = "Pemantauan komprehensif kendala belajar: keterlambatan membaca materi modul, tugas belum dikumpulkan, atau nilai di bawah KKM.",
                        fontSize = 11.5.sp,
                        lineHeight = 16.sp,
                        color = TextSecondary
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // Action Button: Kirim Pengingat Serentak
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(enabled = !isSendingBatch && totalAtRisk > 0, onClick = onSendBatch),
                color = if (totalAtRisk > 0) NeonError else TextDisabled,
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp, horizontal = 14.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isSendingBatch) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Mengirimkan Notifikasi...",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    } else {
                        Icon(
                            painter = painterResource(id = CoreR.drawable.ic_modern_chat),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Kirim Pengingat Serentak ($totalAtRisk Siswa)",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

// ─── KPI Cards ─────────────────────────────────────────────────────────────────

@Composable
private fun StatKpiGrid(
    stats: com.schoolos.android.domain.model.AtRiskStats,
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
            KpiStatCard(
                title = "Total Perlu Perhatian",
                value = stats.total.toString(),
                unit = "Peserta Didik",
                accentColor = NeonError,
                bgTint = ErrorBg,
                iconRes = CoreR.drawable.ic_modern_warning,
                isDark = isDark,
                modifier = Modifier.weight(1f)
            )
            KpiStatCard(
                title = "Materi Tertinggal",
                value = stats.unreadMaterials.toString(),
                unit = "Belum Selesai",
                accentColor = if (isDark) Color(0xFFFB923C) else Color(0xFFEA580C),
                bgTint = if (isDark) Color(0xFF431407) else Color(0xFFFFF7ED),
                iconRes = CoreR.drawable.ic_modern_classes,
                isDark = isDark,
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            KpiStatCard(
                title = "Tugas Tertunda",
                value = stats.overdueAssignments.toString(),
                unit = "Belum Kumpul",
                accentColor = NeonWarning,
                bgTint = WarningBg,
                iconRes = CoreR.drawable.ic_modern_tasks,
                isDark = isDark,
                modifier = Modifier.weight(1f)
            )
            KpiStatCard(
                title = "Di Bawah Standar KKM",
                value = stats.lowScores.toString(),
                unit = "Perlu Remedial",
                accentColor = NeonError,
                bgTint = ErrorBg,
                iconRes = CoreR.drawable.ic_modern_grades,
                isDark = isDark,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun KpiStatCard(
    title: String,
    value: String,
    unit: String,
    accentColor: Color,
    bgTint: Color,
    accentColorBorder: Color = GlassBorder,
    iconRes: Int,
    isDark: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = CosmicNavy,
        border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    modifier = Modifier.size(24.dp),
                    shape = CircleShape,
                    color = bgTint
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(id = iconRes),
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = value,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (value != "0") accentColor else TextPrimary
            )

            Spacer(Modifier.height(2.dp))

            Text(
                text = unit,
                fontSize = 10.5.sp,
                color = TextTertiary
            )
        }
    }
}

// ─── Filter Tabs ───────────────────────────────────────────────────────────────

@Composable
private fun CategoryFilterTabs(
    selectedCategory: String,
    stats: com.schoolos.android.domain.model.AtRiskStats,
    isDark: Boolean,
    onSelect: (String) -> Unit,
) {
    val tabs = listOf(
        Triple("ALL", "Semua", stats.total),
        Triple("UNREAD_MATERIAL", "Materi Tertinggal", stats.unreadMaterials),
        Triple("OVERDUE_ASSIGNMENT", "Tugas Tertunda", stats.overdueAssignments),
        Triple("LOW_SCORE", "Nilai Rendah", stats.lowScores),
    )

    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(tabs) { (key, label, count) ->
            val isSelected = selectedCategory == key
            val animatedBg by animateColorAsState(
                targetValue = if (isSelected) {
                    if (key == "ALL") NeonBlue else NeonError
                } else {
                    if (isDark) CosmicNavy else Color.White
                },
                label = "filterTabBg"
            )

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = animatedBg,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isSelected) Color.Transparent else GlassBorder
                ),
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onSelect(key) }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$label ($count)",
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.White else TextPrimary
                    )
                }
            }
        }
    }
}

// ─── Class Chips & Search Bar ──────────────────────────────────────────────────

@Composable
private fun RombelSelectorChips(
    classes: List<String>,
    selectedClass: String,
    isDark: Boolean,
    onSelect: (String) -> Unit,
) {
    val allOptions = listOf("ALL") + classes

    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(allOptions) { rombel ->
            val isSelected = selectedClass == rombel
            val label = if (rombel == "ALL") "Semua Rombel" else rombel

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isSelected) {
                    if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)
                } else {
                    if (isDark) CosmicNavy else Color.White
                },
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isSelected) (if (isDark) NeonBlue else Color(0xFF0F172A)) else GlassBorder
                ),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onSelect(rombel) }
            ) {
                Text(
                    text = label,
                    fontSize = 11.5.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) TextPrimary else TextSecondary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }
    }
}

@Composable
private fun SearchInputBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    isDark: Boolean,
) {
    val focusManager = LocalFocusManager.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    text = "Cari siswa, NISN, modul...",
                    fontSize = 12.5.sp,
                    color = TextTertiary
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            },
            trailingIcon = {
                if (query.isNotBlank()) {
                    IconButton(onClick = onClear) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Hapus",
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = CosmicNavy,
                unfocusedContainerColor = CosmicNavy,
                focusedBorderColor = NeonBlue,
                unfocusedBorderColor = GlassBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() })
        )
    }
}

// ─── Student Item Card ─────────────────────────────────────────────────────────

@Composable
private fun AtRiskStudentItemCard(
    index: Int,
    student: AtRiskStudent,
    isSending: Boolean,
    isDark: Boolean,
    onSendReminder: () -> Unit,
    onClick: () -> Unit,
) {
    val initial = student.studentName.firstOrNull()?.uppercase() ?: "S"
    val avatarBg = when (index % 4) {
        0 -> if (isDark) Color(0xFF312E81) else Color(0xFFEEF2FF)
        1 -> if (isDark) Color(0xFF064E3B) else Color(0xFFECFDF5)
        2 -> if (isDark) Color(0xFF451A03) else Color(0xFFFFFBEB)
        else -> if (isDark) Color(0xFF4C0519) else Color(0xFFFEF2F2)
    }
    val avatarText = when (index % 4) {
        0 -> NeonBlue
        1 -> NeonSuccess
        2 -> NeonWarning
        else -> NeonError
    }

    val categoryBadgeBg = when (student.category) {
        "UNREAD_MATERIAL" -> if (isDark) Color(0xFF431407) else Color(0xFFFFF7ED)
        "OVERDUE_ASSIGNMENT" -> WarningBg
        "LOW_SCORE" -> ErrorBg
        else -> InfoBg
    }
    val categoryBadgeText = when (student.category) {
        "UNREAD_MATERIAL" -> if (isDark) Color(0xFFFB923C) else Color(0xFFC2410C)
        "OVERDUE_ASSIGNMENT" -> NeonWarning
        "LOW_SCORE" -> NeonError
        else -> NeonBlue
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = CosmicNavy,
        border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Row 1: Index, Avatar, Name, NISN, Class & Category Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Index
                Text(
                    text = "$index",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextTertiary,
                    modifier = Modifier.width(22.dp)
                )

                // Avatar
                Surface(
                    modifier = Modifier.size(36.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = avatarBg
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = initial,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = avatarText
                        )
                    }
                }

                Spacer(Modifier.width(10.dp))

                // Name & NISN
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = student.studentName,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = student.nisn.ifBlank { "NISN -" },
                        fontSize = 11.sp,
                        color = TextTertiary
                    )
                }

                // Rombel Pill
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
                ) {
                    Text(
                        text = student.className,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // Category & Problem Description
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isDark) CosmicDark else Color(0xFFF8FAFC))
                    .padding(10.dp),
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    // Category Badge
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(categoryBadgeBg)
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = student.categoryLabel,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = categoryBadgeText
                        )
                    }

                    Spacer(Modifier.height(6.dp))

                    Text(
                        text = student.title,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )

                    Spacer(Modifier.height(2.dp))

                    Text(
                        text = student.description,
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        color = TextSecondary
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // Row 3: Action Buttons (Kirim Pengingat / Terkirim)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (student.isReminded) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = NeonSuccess,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "Pengingat terkirim ${student.remindedAt ?: "baru saja"}",
                            fontSize = 11.sp,
                            color = NeonSuccess
                        )
                    }
                } else {
                    Text(
                        text = "Perlu tindakan pengingat",
                        fontSize = 11.sp,
                        color = TextTertiary
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (student.isReminded) {
                        if (isDark) Color(0xFF064E3B).copy(alpha = 0.5f) else Color(0xFFECFDF5)
                    } else {
                        if (isDark) Color(0xFF450A0A) else Color(0xFFFEF2F2)
                    },
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (student.isReminded) NeonSuccess.copy(alpha = 0.5f) else NeonError.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(enabled = !isSending && !student.isReminded, onClick = onSendReminder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isSending) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(12.dp),
                                strokeWidth = 1.5.dp,
                                color = NeonError
                            )
                            Spacer(Modifier.width(6.dp))
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
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "Kirim Pengingat",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonError
                            )
                        }
                    }
                }
            }
        }
    }
}
