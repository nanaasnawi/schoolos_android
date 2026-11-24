package com.schoolos.android.feature.home

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.schoolos.android.core.common.BuildConfig
import com.schoolos.android.core.designsystem.*
import com.schoolos.android.domain.model.StaffMember
import com.schoolos.android.domain.model.TeacherProductivityTier
import com.schoolos.android.domain.model.TeacherSupervisionProfile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherStaffSupervisionScreen(
    onBack: () -> Unit,
    onNavigateToTeacherDetail: (String) -> Unit,
    viewModel: TeacherStaffSupervisionViewModel = hiltViewModel(),
) {
    val uiState by viewModel.state.collectAsState()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val snackbarHostState = remember { SnackbarHostState() }

    val isDark = LocalIsDarkTheme.current
    val screenBg = if (isDark) Color(0xFF09090B) else Color(0xFFF8FAFC)
    val cardBg = if (isDark) Color(0xFF111113) else Color.White
    val cardBorder = if (isDark) Color(0xFF1F1F23) else Color(0xFFE2E8F0)
    val innerCardBg = if (isDark) Color(0xFF161618) else Color(0xFFF8FAFC)
    val innerCardBorder = if (isDark) Color(0xFF27272A) else Color(0xFFE2E8F0)
    val textPrimary = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFFA1A1AA) else Color(0xFF64748B)
    val textTertiary = if (isDark) Color(0xFF71717A) else Color(0xFF94A3B8)
    val accentBlue = if (isDark) NeonBlue else Color(0xFF0284C7)

    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearUserMessage()
        }
    }

    val overview = uiState.overview
    val subtitleText = if (uiState.isPkbm) {
        "${overview.totalTeachers} Tutor • ${overview.totalStaff} Tendik Terdaftar"
    } else {
        "${overview.totalTeachers} Guru • ${overview.totalStaff} Tendik Terdaftar"
    }

    Scaffold(
        containerColor = screenBg,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            ExecutiveTopBar(
                title = "Supervisi Guru & Tendik",
                subtitle = subtitleText,
                onBack = onBack,
                actions = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(innerCardBg)
                                .border(1.dp, innerCardBorder, RoundedCornerShape(10.dp))
                                .clickable { viewModel.exportSupervisionPdf(context) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.PictureAsPdf,
                                contentDescription = "Ekspor PDF GTK",
                                tint = accentBlue,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(innerCardBg)
                                .border(1.dp, innerCardBorder, RoundedCornerShape(10.dp))
                                .clickable { viewModel.load(isPullRefresh = true) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Muat Ulang",
                                tint = textSecondary,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding())
        ) {
            // Segmented Dual Tab Bar (Guru / Tutor vs Tendik)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                shape = RoundedCornerShape(14.dp),
                color = innerCardBg,
                border = BorderStroke(1.dp, innerCardBorder),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    val tab0 = uiState.selectedTab == 0
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (tab0) accentBlue.copy(alpha = 0.16f) else Color.Transparent)
                            .border(
                                width = if (tab0) 1.dp else 0.dp,
                                color = if (tab0) accentBlue.copy(alpha = 0.45f) else Color.Transparent,
                                shape = RoundedCornerShape(10.dp),
                            )
                            .clickable { viewModel.selectTab(0) }
                            .padding(vertical = 9.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.School,
                                contentDescription = null,
                                tint = if (tab0) accentBlue else textTertiary,
                                modifier = Modifier.size(15.dp),
                            )
                            Text(
                                text = if (uiState.isPkbm) "Tutor (${overview.totalTeachers})" else "Guru (${overview.totalTeachers})",
                                fontSize = 12.sp,
                                fontWeight = if (tab0) FontWeight.Bold else FontWeight.Medium,
                                color = if (tab0) accentBlue else textSecondary,
                            )
                        }
                    }

                    val tab1 = uiState.selectedTab == 1
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (tab1) TeacherNeon.copy(alpha = 0.16f) else Color.Transparent)
                            .border(
                                width = if (tab1) 1.dp else 0.dp,
                                color = if (tab1) TeacherNeon.copy(alpha = 0.45f) else Color.Transparent,
                                shape = RoundedCornerShape(10.dp),
                            )
                            .clickable { viewModel.selectTab(1) }
                            .padding(vertical = 9.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Groups,
                                contentDescription = null,
                                tint = if (tab1) TeacherNeon else textTertiary,
                                modifier = Modifier.size(15.dp),
                            )
                            Text(
                                text = "Tendik (${overview.totalStaff})",
                                fontSize = 12.sp,
                                fontWeight = if (tab1) FontWeight.Bold else FontWeight.Medium,
                                color = if (tab1) TeacherNeon else textSecondary,
                            )
                        }
                    }
                }
            }

            PullRefreshContainer(
                isRefreshing = uiState.isRefreshing,
                onRefresh = { viewModel.load(isPullRefresh = true) },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                when {
                    uiState.isLoading && overview.teachers.isEmpty() && overview.staff.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                CircularProgressIndicator(color = accentBlue, strokeWidth = 3.dp)
                                Text(
                                    text = "Mengagregasi data GTK & kinerja mengajar...",
                                    fontSize = 13.sp,
                                    color = textSecondary,
                                )
                            }
                        }
                    }

                    uiState.errorMessage != null && overview.teachers.isEmpty() && overview.staff.isEmpty() -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.WarningAmber,
                                    contentDescription = null,
                                    tint = NeonWarning,
                                    modifier = Modifier.size(40.dp),
                                )
                                Text(
                                    text = uiState.errorMessage ?: "Gagal memuat data supervisi.",
                                    fontSize = 13.sp,
                                    color = textSecondary,
                                    textAlign = TextAlign.Center,
                                )
                                Button(
                                    onClick = { viewModel.load() },
                                    colors = ButtonDefaults.buttonColors(containerColor = accentBlue),
                                ) {
                                    Text("Muat Ulang", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 36.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            // Executive KPI Summary Banner
                            item {
                                GtkExecutiveSummaryCard(
                                    overview = overview,
                                    isPkbm = uiState.isPkbm,
                                    accentBlue = accentBlue,
                                    cardBg = cardBg,
                                    cardBorder = cardBorder,
                                    innerBg = innerCardBg,
                                    innerBorder = innerCardBorder,
                                    textPrimary = textPrimary,
                                    textSecondary = textSecondary,
                                )
                            }

                            // Search Input
                            item {
                                OutlinedTextField(
                                    value = uiState.searchQuery,
                                    onValueChange = viewModel::updateSearchQuery,
                                    modifier = Modifier.fillMaxWidth(),
                                    placeholder = {
                                        Text(
                                            text = if (uiState.selectedTab == 0) {
                                                "Cari nama guru/tutor, NUPTK, NIP, atau mapel..."
                                            } else {
                                                "Cari nama tendik, jabatan, NUPTK, atau NIP..."
                                            },
                                            fontSize = 13.sp,
                                            color = textTertiary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.Search, contentDescription = null, tint = textSecondary, modifier = Modifier.size(18.dp))
                                    },
                                    trailingIcon = {
                                        if (uiState.searchQuery.isNotBlank()) {
                                            IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                                Icon(Icons.Default.Close, contentDescription = "Hapus", tint = textSecondary, modifier = Modifier.size(18.dp))
                                            }
                                        }
                                    },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                    keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = cardBg,
                                        unfocusedContainerColor = cardBg,
                                        focusedBorderColor = accentBlue,
                                        unfocusedBorderColor = cardBorder,
                                        focusedTextColor = textPrimary,
                                        unfocusedTextColor = textPrimary,
                                    ),
                                )
                            }

                            if (uiState.selectedTab == 0) {
                                // Filter & Sort Chips for Teachers
                                item {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        ) {
                                            val tierFilters = listOf(
                                                "ALL" to "Semua (${overview.totalTeachers})",
                                                "SANGAT_PRODUKTIF" to "Sangat Produktif",
                                                "AKTIF" to "Aktif",
                                                "CUKUP" to "Cukup",
                                                "PERLU_PEMBINAAN" to "Perlu Pembinaan (${overview.needsCoachingCount})",
                                            )
                                            tierFilters.forEach { (key, label) ->
                                                val selected = uiState.selectedTierFilter == key
                                                val tint = if (key == "PERLU_PEMBINAAN") NeonWarning else accentBlue
                                                SupervisionFilterChip(
                                                    label = label,
                                                    selected = selected,
                                                    accent = tint,
                                                    cardBg = cardBg,
                                                    cardBorder = cardBorder,
                                                    textSecondary = textSecondary,
                                                    onClick = { viewModel.selectTierFilter(key) },
                                                )
                                            }
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                        ) {
                                            Text(
                                                text = "Menampilkan ${uiState.filteredTeachers.size} pendidik",
                                                fontSize = 11.5.sp,
                                                color = textSecondary,
                                                fontWeight = FontWeight.Medium,
                                            )
                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                val sorts = listOf(
                                                    "SCORE_DESC" to "Skor Tertinggi",
                                                    "SCORE_ASC" to "Perlu Dorongan",
                                                    "NAME_ASC" to "A-Z",
                                                )
                                                sorts.forEach { (key, label) ->
                                                    val selected = uiState.selectedSortMode == key
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .background(if (selected) accentBlue.copy(alpha = 0.14f) else cardBg)
                                                            .border(1.dp, if (selected) accentBlue.copy(alpha = 0.4f) else cardBorder, RoundedCornerShape(8.dp))
                                                            .clickable { viewModel.selectSortMode(key) }
                                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                                                    ) {
                                                        Text(
                                                            text = label,
                                                            fontSize = 10.5.sp,
                                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                                            color = if (selected) accentBlue else textSecondary,
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                val teachers = uiState.filteredTeachers
                                if (teachers.isEmpty()) {
                                    item {
                                        EmptySupervisionCard(
                                            title = "Tidak Ada Data Guru/Tutor",
                                            subtitle = "Coba ubah kata kunci pencarian atau filter kategori performa.",
                                            cardBg = cardBg,
                                            cardBorder = cardBorder,
                                            textPrimary = textPrimary,
                                            textSecondary = textSecondary,
                                        )
                                    }
                                } else {
                                    items(teachers, key = { it.teacher.id }) { profile ->
                                        TeacherSupervisionCard(
                                            profile = profile,
                                            isPkbm = uiState.isPkbm,
                                            accentBlue = accentBlue,
                                            cardBg = cardBg,
                                            cardBorder = cardBorder,
                                            innerBg = innerCardBg,
                                            innerBorder = innerCardBorder,
                                            textPrimary = textPrimary,
                                            textSecondary = textSecondary,
                                            onClick = { onNavigateToTeacherDetail(profile.teacher.id) },
                                        )
                                    }
                                }
                            } else {
                                // Staff (Tendik) Filters
                                item {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        val staffFilters = listOf(
                                            "ALL" to "Semua (${overview.totalStaff})",
                                            "ACTIVE" to "Aktif (${overview.activeStaffCount})",
                                            "INACTIVE" to "Nonaktif (${(overview.totalStaff - overview.activeStaffCount).coerceAtLeast(0)})",
                                        )
                                        staffFilters.forEach { (key, label) ->
                                            SupervisionFilterChip(
                                                label = label,
                                                selected = uiState.selectedStaffFilter == key,
                                                accent = TeacherNeon,
                                                cardBg = cardBg,
                                                cardBorder = cardBorder,
                                                textSecondary = textSecondary,
                                                onClick = { viewModel.selectStaffFilter(key) },
                                            )
                                        }
                                    }
                                }

                                val staffList = uiState.filteredStaff
                                if (staffList.isEmpty()) {
                                    item {
                                        EmptySupervisionCard(
                                            title = "Belum Ada Data Tenaga Kependidikan",
                                            subtitle = "Data tendik hasil sinkronisasi Dapodik akan tampil di sini.",
                                            cardBg = cardBg,
                                            cardBorder = cardBorder,
                                            textPrimary = textPrimary,
                                            textSecondary = textSecondary,
                                        )
                                    }
                                } else {
                                    items(staffList, key = { it.id }) { member ->
                                        StaffMemberCard(
                                            member = member,
                                            cardBg = cardBg,
                                            cardBorder = cardBorder,
                                            innerBg = innerCardBg,
                                            innerBorder = innerCardBorder,
                                            textPrimary = textPrimary,
                                            textSecondary = textSecondary,
                                            onClick = { viewModel.openStaffDetail(member) },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Bottom Sheet for Tendik Detail
    val selectedStaff = uiState.selectedStaffMember
    if (selectedStaff != null) {
        ModalBottomSheet(
            onDismissRequest = viewModel::closeStaffDetail,
            containerColor = cardBg,
            contentColor = textPrimary,
        ) {
            StaffDetailSheetContent(
                member = selectedStaff,
                innerBg = innerCardBg,
                innerBorder = innerCardBorder,
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                accent = TeacherNeon,
                context = context,
                onDismiss = viewModel::closeStaffDetail,
            )
        }
    }
}

@Composable
private fun GtkExecutiveSummaryCard(
    overview: com.schoolos.android.domain.model.GtkSupervisionOverview,
    isPkbm: Boolean,
    accentBlue: Color,
    cardBg: Color,
    cardBorder: Color,
    innerBg: Color,
    innerBorder: Color,
    textPrimary: Color,
    textSecondary: Color,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "RINGKASAN PRODUKTIVITAS GTK",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentBlue,
                        letterSpacing = 0.6.sp,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "${overview.totalModulesCount} Modul • ${overview.totalAssignmentsCount} Tugas • ${overview.totalQuizzesCount} Kuis CBT",
                        fontSize = 12.sp,
                        color = textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = accentBlue.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, accentBlue.copy(alpha = 0.35f)),
                ) {
                    Text(
                        text = "Rata-rata ${overview.averagePerformanceScore}/100",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentBlue,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SummaryKpiTile(
                    label = if (isPkbm) "Total Tutor" else "Total Guru",
                    value = "${overview.totalTeachers}",
                    sub = "${overview.activeTeachersCount} aktif",
                    accent = accentBlue,
                    innerBg = innerBg,
                    innerBorder = innerBorder,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    modifier = Modifier.weight(1f),
                )
                SummaryKpiTile(
                    label = "Total Tendik",
                    value = "${overview.totalStaff}",
                    sub = "${overview.activeStaffCount} aktif",
                    accent = TeacherNeon,
                    innerBg = innerBg,
                    innerBorder = innerBorder,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    modifier = Modifier.weight(1f),
                )
                SummaryKpiTile(
                    label = "Produktif",
                    value = "${overview.productiveTeachersCount}",
                    sub = "${overview.needsCoachingCount} perlu bina",
                    accent = if (overview.needsCoachingCount > 0) NeonWarning else NeonSuccess,
                    innerBg = innerBg,
                    innerBorder = innerBorder,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun SummaryKpiTile(
    label: String,
    value: String,
    sub: String,
    accent: Color,
    innerBg: Color,
    innerBorder: Color,
    textPrimary: Color,
    textSecondary: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(innerBg)
            .border(1.dp, innerBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Text(text = label, fontSize = 10.sp, color = textSecondary, maxLines = 1)
        Spacer(Modifier.height(2.dp))
        Text(text = value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textPrimary)
        Text(text = sub, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = accent, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun SupervisionFilterChip(
    label: String,
    selected: Boolean,
    accent: Color,
    cardBg: Color,
    cardBorder: Color,
    textSecondary: Color,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) accent.copy(alpha = 0.16f) else cardBg)
            .border(1.dp, if (selected) accent.copy(alpha = 0.5f) else cardBorder, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
    ) {
        Text(
            text = label,
            fontSize = 11.5.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) accent else textSecondary,
        )
    }
}

@Composable
private fun TeacherSupervisionCard(
    profile: TeacherSupervisionProfile,
    isPkbm: Boolean,
    accentBlue: Color,
    cardBg: Color,
    cardBorder: Color,
    innerBg: Color,
    innerBorder: Color,
    textPrimary: Color,
    textSecondary: Color,
    onClick: () -> Unit,
) {
    val tierColor = when (profile.tier) {
        TeacherProductivityTier.SANGAT_PRODUKTIF -> NeonSuccess
        TeacherProductivityTier.AKTIF -> accentBlue
        TeacherProductivityTier.CUKUP -> TeacherNeon
        TeacherProductivityTier.PERLU_PEMBINAAN -> NeonWarning
    }

    val avatarUrl = resolveAvatarUrl(profile.teacher.fotoProfile)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(modifier = Modifier.size(46.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(tierColor.copy(alpha = 0.14f))
                            .border(1.dp, tierColor.copy(alpha = 0.35f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (!avatarUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(avatarUrl)
                                    .diskCachePolicy(CachePolicy.ENABLED)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = profile.teacher.fullName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize().clip(CircleShape),
                            )
                        } else {
                            Text(
                                text = profile.teacher.fullName.firstOrNull()?.uppercase() ?: "G",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = tierColor,
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(if (profile.isActive) NeonSuccess else NeonError)
                            .border(2.dp, cardBg, CircleShape),
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = profile.teacher.fullName,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = profile.displaySubject,
                        fontSize = 12.sp,
                        color = textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    val docId = profile.teacher.nuptk?.takeIf { it.isNotBlank() }?.let { "NUPTK: $it" }
                        ?: profile.teacher.nip?.takeIf { it.isNotBlank() }?.let { "NIP: $it" }
                        ?: profile.teacher.statusKepegawaian?.takeIf { it.isNotBlank() }
                    if (!docId.isNullOrBlank()) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = docId,
                            fontSize = 10.5.sp,
                            fontFamily = FontFamily.Monospace,
                            color = textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = tierColor.copy(alpha = 0.14f),
                        border = BorderStroke(0.8.dp, tierColor.copy(alpha = 0.35f)),
                    ) {
                        Text(
                            text = profile.tier.shortLabel,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = tierColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Text(
                            text = "Detail",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = accentBlue,
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = accentBlue,
                            modifier = Modifier.size(12.dp),
                        )
                    }
                }
            }

            // Performance Score Bar
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = if (!profile.homeroomClassName.isNullOrBlank()) {
                            if (isPkbm) "Skor Kinerja • Tutor Pamong ${profile.homeroomClassName}" else "Skor Kinerja • Wali ${profile.homeroomClassName}"
                        } else {
                            "Skor Kinerja Mengajar"
                        },
                        fontSize = 11.sp,
                        color = textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = "${profile.performanceScore}/100",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = tierColor,
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(innerBg)
                        .border(0.5.dp, innerBorder, CircleShape),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fraction = (profile.performanceScore / 100f).coerceIn(0.03f, 1f))
                            .clip(CircleShape)
                            .background(tierColor),
                    )
                }
            }

            // 4-Pillar Mini Activity Counters
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(innerBg)
                    .border(1.dp, innerBorder, RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MiniWorkCount(Icons.AutoMirrored.Outlined.MenuBook, "${profile.materialsCount} Modul", textPrimary, accentBlue)
                MiniWorkCount(Icons.AutoMirrored.Outlined.Assignment, "${profile.assignmentsCount} Tugas", textPrimary, TeacherNeon)
                MiniWorkCount(Icons.Outlined.Quiz, "${profile.quizzesCount} Kuis", textPrimary, NeonBlue)
                MiniWorkCount(Icons.Outlined.Schedule, "${profile.sessionsCount} Sesi", textPrimary, NeonSuccess)
            }
        }
    }
}

@Composable
private fun MiniWorkCount(
    icon: ImageVector,
    label: String,
    textPrimary: Color,
    tint: Color,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(13.dp))
        Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = textPrimary)
    }
}

@Composable
private fun StaffMemberCard(
    member: StaffMember,
    cardBg: Color,
    cardBorder: Color,
    innerBg: Color,
    innerBorder: Color,
    textPrimary: Color,
    textSecondary: Color,
    onClick: () -> Unit,
) {
    val statusColor = if (member.isActive) TeacherNeon else NeonError
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(statusColor.copy(alpha = 0.14f))
                        .border(1.dp, statusColor.copy(alpha = 0.35f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = member.fullName.firstOrNull()?.uppercase() ?: "T",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = member.fullName,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = member.jobTitle,
                        fontSize = 12.sp,
                        color = textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = statusColor.copy(alpha = 0.14f),
                    border = BorderStroke(0.8.dp, statusColor.copy(alpha = 0.35f)),
                ) {
                    Text(
                        text = if (member.isActive) "Aktif" else "Nonaktif",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(innerBg)
                    .border(1.dp, innerBorder, RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = member.statusKepegawaian?.takeIf { it.isNotBlank() } ?: member.jenisPtk ?: "Tenaga Kependidikan",
                    fontSize = 11.sp,
                    color = textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = member.nip?.takeIf { it.isNotBlank() }?.let { "NIP: $it" }
                        ?: member.nuptk?.takeIf { it.isNotBlank() }?.let { "NUPTK: $it" }
                        ?: "Lihat Biodata ->",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TeacherNeon,
                )
            }
        }
    }
}

@Composable
private fun StaffDetailSheetContent(
    member: StaffMember,
    innerBg: Color,
    innerBorder: Color,
    textPrimary: Color,
    textSecondary: Color,
    accent: Color,
    context: Context,
    onDismiss: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .navigationBarsPadding()
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.14f))
                    .border(1.dp, accent.copy(alpha = 0.35f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.Badge, contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(member.fullName, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                Text(member.jobTitle, fontSize = 12.5.sp, color = textSecondary)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(innerBg)
                .border(1.dp, innerBorder, RoundedCornerShape(14.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val ttl = listOfNotNull(
                member.tempatLahir?.takeIf { it.isNotBlank() },
                member.tanggalLahir?.takeIf { it.isNotBlank() },
            ).joinToString(", ")
            SupervisionInfoRow("Status Kepegawaian", member.statusKepegawaian ?: "—", textSecondary, textPrimary)
            SupervisionInfoRow("Jenis PTK", member.jenisPtk ?: "—", textSecondary, textPrimary)
            SupervisionInfoRow("NUPTK", member.nuptk ?: "—", textSecondary, textPrimary)
            SupervisionInfoRow("NIP", member.nip ?: "—", textSecondary, textPrimary)
            SupervisionInfoRow("Jenis Kelamin", formatGenderLabel(member.jk), textSecondary, textPrimary)
            SupervisionInfoRow("Tempat, Tgl Lahir", ttl.ifBlank { "—" }, textSecondary, textPrimary)
            SupervisionInfoRow("Agama", member.agama ?: "—", textSecondary, textPrimary)
            SupervisionInfoRow("Alamat", member.alamatJalan ?: "—", textSecondary, textPrimary)
            SupervisionInfoRow("No. HP / WA", member.noHp ?: "—", textSecondary, textPrimary)
            SupervisionInfoRow("Email", member.email ?: "—", textSecondary, textPrimary)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                onClick = { launchWhatsAppChat(context, member.noHp, "Yth. ${member.fullName}, ") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
            ) {
                Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(16.dp), tint = TeacherNeon)
                Spacer(Modifier.width(6.dp))
                Text("WhatsApp", color = textPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
            OutlinedButton(
                onClick = { launchPhoneDial(context, member.noHp) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
            ) {
                Icon(Icons.Outlined.Phone, contentDescription = null, modifier = Modifier.size(16.dp), tint = NeonBlue)
                Spacer(Modifier.width(6.dp))
                Text("Telepon", color = textPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
            Button(
                onClick = onDismiss,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accent),
            ) {
                Text("Tutup", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
internal fun EmptySupervisionCard(
    title: String,
    subtitle: String,
    cardBg: Color,
    cardBorder: Color,
    textPrimary: Color,
    textSecondary: Color,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder),
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(Icons.Outlined.SearchOff, contentDescription = null, tint = textSecondary, modifier = Modifier.size(32.dp))
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textPrimary, textAlign = TextAlign.Center)
            Text(subtitle, fontSize = 12.sp, color = textSecondary, textAlign = TextAlign.Center)
        }
    }
}

@Composable
internal fun SupervisionInfoRow(
    label: String,
    value: String,
    labelColor: Color,
    valueColor: Color,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = labelColor,
            modifier = Modifier.weight(0.42f),
        )
        Text(
            text = value.ifBlank { "—" },
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = valueColor,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(0.58f),
        )
    }
}

internal fun formatGenderLabel(jk: String?): String = when (jk?.trim()?.uppercase()) {
    "L", "LAKI-LAKI", "M", "MALE" -> "Laki-laki"
    "P", "PEREMPUAN", "F", "FEMALE" -> "Perempuan"
    else -> jk?.takeIf { it.isNotBlank() } ?: "—"
}

internal fun resolveAvatarUrl(raw: String?): String? {
    val clean = raw?.trim()?.takeIf { it.isNotBlank() } ?: return null
    return if (clean.startsWith("http://", ignoreCase = true) ||
        clean.startsWith("https://", ignoreCase = true) ||
        clean.startsWith("data:", ignoreCase = true)
    ) {
        clean
    } else {
        val host = BuildConfig.API_BASE_URL.substringBefore("/api/").trimEnd('/')
        val path = if (clean.startsWith("/")) clean else "/$clean"
        "$host$path"
    }
}

internal fun launchWhatsAppChat(context: Context, rawPhone: String?, prefillText: String) {
    val digits = rawPhone?.filter { it.isDigit() }.orEmpty()
    if (digits.length < 8) {
        Toast.makeText(context, "Nomor HP / WhatsApp GTK belum terdaftar.", Toast.LENGTH_SHORT).show()
        return
    }
    val waNumber = when {
        digits.startsWith("0") -> "62${digits.drop(1)}"
        digits.startsWith("62") -> digits
        else -> "62$digits"
    }
    val url = "https://wa.me/$waNumber?text=${Uri.encode(prefillText)}"
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
    }.onFailure {
        Toast.makeText(context, "Tidak dapat membuka WhatsApp di perangkat ini.", Toast.LENGTH_SHORT).show()
    }
}

internal fun launchPhoneDial(context: Context, rawPhone: String?) {
    val digits = rawPhone?.filter { it.isDigit() || it == '+' }.orEmpty()
    if (digits.length < 6) {
        Toast.makeText(context, "Nomor telepon GTK belum terdaftar.", Toast.LENGTH_SHORT).show()
        return
    }
    runCatching {
        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$digits")).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
    }.onFailure {
        Toast.makeText(context, "Gagal membuka aplikasi telepon.", Toast.LENGTH_SHORT).show()
    }
}
