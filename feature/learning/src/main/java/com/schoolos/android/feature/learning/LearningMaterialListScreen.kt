package com.schoolos.android.feature.learning

import com.schoolos.android.core.R as CoreR
import androidx.compose.ui.res.painterResource
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.schoolos.android.core.common.formatPublishTimestamp
import com.schoolos.android.core.designsystem.*

@Composable
fun LearningMaterialListScreen(
    onBack: () -> Unit = {},
    onMaterialClick: (String) -> Unit = {},
    onCreateMaterial: () -> Unit = {},
    subjectId: String = "",
    className: String? = null,
    viewModel: LearningViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val materials = state.materials
    val isPrincipal = com.schoolos.android.core.auth.isPrincipalRole(state.userRole)
    val isTeacher = com.schoolos.android.core.auth.isTeacherRole(state.userRole) || isPrincipal
    val isDark = LocalIsDarkTheme.current
    val innerCardBg = if (isDark) Color(0xFF161618) else Color(0xFFF8FAFC)
    val innerCardBorder = if (isDark) Color(0xFF27272A) else Color(0xFFE2E8F0)
    val textPrimary = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A)
    val textTertiary = if (isDark) Color(0xFF71717A) else Color(0xFF94A3B8)

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.refresh(isPullRefresh = false)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        containerColor = CosmicBlack,
        topBar = {
            ExecutiveTopBar(
                title = if (isPrincipal) "Modul Pembelajaran Sekolah" else "Modul Pembelajaran",
                subtitle = when {
                    isPrincipal -> "Pemantauan bahan ajar seluruh guru & rombel"
                    isTeacher -> "Bahan ajar & modul kelas"
                    else -> "Bahan bacaan & video"
                },
                onBack = onBack,
            )
        },
        floatingActionButton = {
            if (isTeacher) {
                ExtendedFloatingActionButton(
                    onClick = onCreateMaterial,
                    containerColor = TeacherNeon,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(16.dp),
                    icon = { Icon(Icons.Default.Add, null) },
                    text = { Text("Buat Materi", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.navigationBarsPadding()
                )
            }
        }
    ) { padding ->
        PullRefreshContainer(
            isRefreshing = state.isRefreshing,
            onRefresh = { viewModel.refresh(isPullRefresh = true) },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // ── ACTIVE SUBJECT / CLASS / DATE FILTER ─────────
                if (state.subjectFilter != null || state.classFilter != null || state.dateFilter != null) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (state.subjectFilter != null || state.classFilter != null) {
                                SubjectFilterChip(
                                    subject = state.subjectFilter.orEmpty(),
                                    className = state.classFilter,
                                    canClear = false,
                                )
                            }
                            if (state.dateFilter != null) {
                                SessionDateFilterBadge(
                                    dateStr = state.dateFilter!!,
                                    onShowAllArchive = { viewModel.clearDateFilter() }
                                )
                            }
                        }
                    }
                }

                // ── HERO BANNER ──────────────────────────────────────────────────
                item {
                    if (isTeacher) {
                        TeacherLearningHeroBanner(
                            totalMaterials = state.totalMaterials,
                            completedCount = state.totalCompleted
                        )
                    } else {
                        StudentMaterialHeroBanner(
                            totalMaterials = state.totalMaterials,
                            completedCount = state.totalCompleted,
                            pendingCount = state.pendingCount
                        )
                    }
                }

                // ── SEARCH BAR ──────────────────────────────────────────────────
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(innerCardBg)
                            .border(1.dp, innerCardBorder, RoundedCornerShape(16.dp))
                            .padding(horizontal = 14.dp, vertical = 11.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Search, null, tint = textTertiary, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(10.dp))
                            BasicTextField(
                                value = state.searchQuery,
                                onValueChange = { viewModel.onSearchQueryChanged(it) },
                                modifier = Modifier.weight(1f),
                                textStyle = TextStyle(color = textPrimary, fontSize = 14.sp),
                                singleLine = true,
                                decorationBox = { innerTextField ->
                                    if (state.searchQuery.isEmpty()) {
                                        Text("Cari judul materi, bab, topik...", color = textTertiary, fontSize = 13.sp)
                                    }
                                    innerTextField()
                                }
                            )
                            if (state.searchQuery.isNotEmpty()) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Hapus",
                                    tint = textTertiary,
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clickable { viewModel.onSearchQueryChanged("") }
                                )
                            }
                        }
                    }
                }

                // ── CATEGORY / FORMAT FILTER TABS ────────────────────────────────
                item {
                    MaterialCategoryTabs(
                        selectedCategory = state.selectedCategory,
                        onSelect = { viewModel.onCategorySelected(it) },
                        totalCount = state.totalMaterials,
                        bookPdfCount = state.bookPdfCount,
                        videoCount = state.videoCount,
                        infographicCount = state.infographicCount,
                        articleCount = state.articleCount,
                        pendingCount = state.pendingCount,
                        completedCount = state.totalCompleted,
                        scheduledCount = state.scheduledCount,
                        isTeacher = isTeacher
                    )
                }

                // ── SKELETON SHIMMER LOADING STATE ───────────────────────────────
                if (state.isLoading) {
                    items(4) {
                        LearningMaterialShimmerItem()
                    }
                }

                // ── EMPTY STATE ──────────────────────────────────────────────────
                if (!state.isLoading && materials.isEmpty()) {
                    item {
                        EmptyMaterialState(searchQuery = state.searchQuery)
                    }
                }

                // ── MATERIAL CARDS ───────────────────────────────────────────────
                items(materials, key = { it.id }) { item ->
                    ModernMaterialCard(
                        item = item,
                        isTeacher = isTeacher,
                        onClick = { onMaterialClick(item.id) }
                    )
                }
            }
        }
    }
}

// ── SKELETON SHIMMER ITEM ─────────────────────────────────────────────────────

@Composable
private fun LearningMaterialShimmerItem(modifier: Modifier = Modifier) {
    val isDark = LocalIsDarkTheme.current
    val cardBg = if (isDark) Color(0xFF111113) else Color.White
    val cardBorder = if (isDark) Color(0xFF1F1F23) else Color(0xFFE2E8F0)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp)),
        shape = RoundedCornerShape(22.dp),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder),
        shadowElevation = if (isDark) 0.dp else 1.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ShimmerBox(modifier = Modifier.width(64.dp).height(16.dp), shape = RoundedCornerShape(6.dp))
                ShimmerBox(modifier = Modifier.width(52.dp).height(16.dp), shape = RoundedCornerShape(6.dp))
            }
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                ShimmerBox(modifier = Modifier.size(40.dp), shape = RoundedCornerShape(12.dp))
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    ShimmerBox(modifier = Modifier.fillMaxWidth(0.72f).height(15.dp), shape = RoundedCornerShape(4.dp))
                    Spacer(Modifier.height(6.dp))
                    ShimmerBox(modifier = Modifier.fillMaxWidth(0.48f).height(12.dp), shape = RoundedCornerShape(4.dp))
                    Spacer(Modifier.height(6.dp))
                    ShimmerBox(modifier = Modifier.fillMaxWidth(0.30f).height(10.dp), shape = RoundedCornerShape(4.dp))
                }
            }
        }
    }
}

// ── STUDENT MATERIAL HERO BANNER (22.dp ROUNDED, ADAPTIVE) ────────────────────

@Composable
private fun StudentMaterialHeroBanner(
    totalMaterials: Int,
    completedCount: Int,
    pendingCount: Int,
) {
    val isDark = LocalIsDarkTheme.current
    val cardBg = if (isDark) Color(0xFF111113) else Color.White
    val cardBorder = if (isDark) Color(0xFF1F1F23) else Color(0xFFE2E8F0)
    val textPrimary = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFFA1A1AA) else Color(0xFF64748B)
    val textTertiary = if (isDark) Color(0xFF71717A) else Color(0xFF94A3B8)
    val innerCardBg = if (isDark) Color(0xFF161618) else Color(0xFFF8FAFC)
    val innerCardBorder = if (isDark) Color(0xFF27272A) else Color(0xFFE2E8F0)
    val studentAccentColor = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)
    val successColor = if (isDark) Color(0xFF10B981) else Color(0xFF16A34A)

    val progressFraction = if (totalMaterials > 0) completedCount.toFloat() / totalMaterials.toFloat() else 0f
    val animatedProgress by animateFloatAsState(targetValue = progressFraction, label = "materialProgress")
    val completionPercent = (progressFraction * 100).toInt()

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder),
        shadowElevation = if (isDark) 0.dp else 1.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top Tag Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isDark) Color(0xFF1E293B) else Color(0xFFE0F2FE),
                    border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFBAE6FD))
                ) {
                    Text(
                        text = "MODUL BELAJAR",
                        color = studentAccentColor,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = innerCardBg,
                    border = BorderStroke(1.dp, innerCardBorder)
                ) {
                    Text(
                        text = "$completionPercent% Selesai",
                        color = if (completionPercent >= 100) successColor else textSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            // Title & Subtitle with Outline Book Icon
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isDark) Color(0xFF1E293B) else Color(0xFFE0F2FE))
                        .border(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFBAE6FD), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = CoreR.drawable.ic_modern_book),
                        contentDescription = null,
                        tint = studentAccentColor,
                        modifier = Modifier.size(19.dp)
                    )
                }

                Column {
                    Text(
                        text = "Bahan Bacaan & Materi",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary,
                        letterSpacing = (-0.3).sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(1.dp))
                    Text(
                        text = "Pelajari modul mandiri dan video pembelajaran.",
                        fontSize = 11.5.sp,
                        color = textSecondary,
                        lineHeight = 15.sp
                    )
                }
            }

            // Quick Stats Cards Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatMiniCard(
                    title = "Total",
                    value = "$totalMaterials",
                    cardBg = innerCardBg,
                    cardBorder = innerCardBorder,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    modifier = Modifier.weight(1f)
                )
                StatMiniCard(
                    title = "Selesai",
                    value = "$completedCount",
                    cardBg = innerCardBg,
                    cardBorder = innerCardBorder,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    modifier = Modifier.weight(1f)
                )
                StatMiniCard(
                    title = "Belum",
                    value = "$pendingCount",
                    cardBg = innerCardBg,
                    cardBorder = innerCardBorder,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    modifier = Modifier.weight(1f)
                )
            }

            // Progress Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(if (isDark) Color(0xFF27272A) else Color(0xFFE2E8F0))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (completionPercent >= 100) successColor else studentAccentColor)
                )
            }
        }
    }
}

@Composable
private fun StatMiniCard(
    title: String,
    value: String,
    cardBg: Color,
    cardBorder: Color,
    textPrimary: Color,
    textSecondary: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .height(66.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(cardBg)
            .border(1.dp, cardBorder, RoundedCornerShape(14.dp))
            .padding(vertical = 8.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = value,
            color = textPrimary,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = title,
            color = textSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// ── TEACHER LEARNING HERO BANNER (22.dp ROUNDED, ADAPTIVE) ────────────────────

@Composable
private fun TeacherLearningHeroBanner(
    totalMaterials: Int,
    completedCount: Int,
) {
    val isDark = LocalIsDarkTheme.current
    val cardBg = if (isDark) Color(0xFF111113) else Color.White
    val cardBorder = if (isDark) Color(0xFF1F1F23) else Color(0xFFE2E8F0)
    val textPrimary = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFFA1A1AA) else Color(0xFF64748B)
    val innerCardBg = if (isDark) Color(0xFF161618) else Color(0xFFF8FAFC)
    val innerCardBorder = if (isDark) Color(0xFF27272A) else Color(0xFFE2E8F0)
    val teacherAccentColor = if (isDark) Color(0xFF10B981) else Color(0xFF059669)

    val avgCompletion = if (totalMaterials > 0) (completedCount * 100) / totalMaterials else 0

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder),
        shadowElevation = if (isDark) 0.dp else 1.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isDark) Color(0xFF064E3B).copy(alpha = 0.35f) else Color(0xFFECFDF5),
                    border = BorderStroke(1.dp, if (isDark) Color(0xFF10B981).copy(alpha = 0.4f) else Color(0xFFA7F3D0))
                ) {
                    Text(
                        text = "MANAJEMEN BAHAN AJAR",
                        color = teacherAccentColor,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = innerCardBg,
                    border = BorderStroke(1.dp, innerCardBorder)
                ) {
                    Text(
                        text = "$totalMaterials Materi Aktif",
                        color = textSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isDark) Color(0xFF064E3B).copy(alpha = 0.35f) else Color(0xFFECFDF5))
                        .border(1.dp, if (isDark) Color(0xFF10B981).copy(alpha = 0.4f) else Color(0xFFA7F3D0), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = CoreR.drawable.ic_modern_book),
                        contentDescription = null,
                        tint = teacherAccentColor,
                        modifier = Modifier.size(19.dp)
                    )
                }

                Column {
                    Text(
                        text = "Modul & Bahan Bacaan",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary,
                        letterSpacing = (-0.3).sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(1.dp))
                    Text(
                        text = "Kelola dan distribusikan bahan ajar untuk siswa.",
                        fontSize = 11.5.sp,
                        color = textSecondary,
                        lineHeight = 15.sp
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatMiniCard(
                    title = "Total Modul",
                    value = "$totalMaterials",
                    cardBg = innerCardBg,
                    cardBorder = innerCardBorder,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    modifier = Modifier.weight(1f)
                )
                StatMiniCard(
                    title = "Rata-rata Selesai",
                    value = "$avgCompletion%",
                    cardBg = innerCardBg,
                    cardBorder = innerCardBorder,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

// ── CATEGORY & FORMAT TABS (12.dp ROUNDED, ADAPTIVE) ──────────────────────────

@Composable
private fun MaterialCategoryTabs(
    selectedCategory: String,
    onSelect: (String) -> Unit,
    totalCount: Int,
    bookPdfCount: Int,
    videoCount: Int,
    infographicCount: Int = 0,
    articleCount: Int = 0,
    pendingCount: Int = 0,
    completedCount: Int = 0,
    scheduledCount: Int = 0,
    isTeacher: Boolean,
) {
    val isDark = LocalIsDarkTheme.current
    val textSecondary = if (isDark) Color(0xFFA1A1AA) else Color(0xFF64748B)

    data class TabDef(val id: String, val label: String, val count: Int)

    val tabs = if (isTeacher) {
        listOf(
            TabDef("Semua", "Semua", totalCount),
            TabDef("Terjadwal", "Terjadwal (Draft)", scheduledCount),
            TabDef("Buku & PDF", "Buku & PDF", bookPdfCount),
            TabDef("Video", "Video", videoCount),
            TabDef("Infografis", "Infografis", infographicCount),
            TabDef("Artikel", "Artikel", articleCount),
        )
    } else {
        listOf(
            TabDef("Semua", "Semua", totalCount),
            TabDef("Buku & PDF", "Buku & PDF", bookPdfCount),
            TabDef("Video", "Video", videoCount),
            TabDef("Infografis", "Infografis", infographicCount),
            TabDef("Artikel", "Artikel", articleCount),
            TabDef("Belum Selesai", "Belum", pendingCount),
            TabDef("Selesai", "Selesai", completedCount),
        )
    }

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(tabs) { tab ->
            val isSelected = tab.id == selectedCategory

            val chipBg = when {
                isSelected -> if (isDark) Color(0xFF27272A) else Color(0xFF0F172A)
                else -> if (isDark) Color(0xFF161618) else Color.White
            }
            val chipBorder = when {
                isSelected -> if (isDark) Color(0xFF3F3F46) else Color(0xFF0F172A)
                else -> if (isDark) Color(0xFF1F1F23) else Color(0xFFE2E8F0)
            }
            val chipTextColor = when {
                isSelected -> Color.White
                else -> textSecondary
            }
            val badgeBg = when {
                isSelected -> if (isDark) Color(0xFF3F3F46) else Color(0xFF334155)
                else -> if (isDark) Color(0xFF222226) else Color(0xFFF1F5F9)
            }
            val badgeTextColor = when {
                isSelected -> Color.White
                else -> textSecondary
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(chipBg)
                    .border(1.dp, chipBorder, RoundedCornerShape(12.dp))
                    .clickable { onSelect(tab.id) }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = tab.label,
                        color = chipTextColor,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                    )
                    if (tab.count > 0) {
                        Spacer(Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(badgeBg)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${tab.count}",
                                color = badgeTextColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── MODERN MATERIAL CARD (22.dp ROUNDED, ADAPTIVE) ───────────────────────────

@Composable
private fun ModernMaterialCard(
    item: MaterialItem,
    isTeacher: Boolean = false,
    onClick: () -> Unit
) {
    val isDark = LocalIsDarkTheme.current
    val cardBg = if (isDark) Color(0xFF111113) else Color.White
    val cardBorder = if (isDark) Color(0xFF1F1F23) else Color(0xFFE2E8F0)
    val textPrimary = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFFA1A1AA) else Color(0xFF64748B)
    val textTertiary = if (isDark) Color(0xFF71717A) else Color(0xFF94A3B8)
    val innerCardBg = if (isDark) Color(0xFF161618) else Color(0xFFF8FAFC)
    val innerCardBorder = if (isDark) Color(0xFF27272A) else Color(0xFFE2E8F0)
    val studentAccentColor = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)
    val successColor = if (isDark) Color(0xFF10B981) else Color(0xFF16A34A)

    val isLibraryBook = item.startPage != null
    val isVideo = item.type.equals("VIDEO", ignoreCase = true) ||
        item.mediaUrl?.contains("youtube", ignoreCase = true) == true ||
        item.mediaUrl?.contains("youtu.be", ignoreCase = true) == true ||
        item.mediaUrl?.endsWith(".mp4", ignoreCase = true) == true ||
        item.title.contains("video", ignoreCase = true)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        color = cardBg,
        border = BorderStroke(
            width = if (item.isCompleted) 1.5.dp else 1.dp,
            color = if (item.isCompleted) (if (isDark) successColor.copy(alpha = 0.7f) else successColor) else cardBorder
        ),
        shadowElevation = if (isDark) 0.dp else 1.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // ── TOP ROW: SUBJECT TAG & STATUS BADGE ──────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isDark) Color(0xFF1E293B) else Color(0xFFE0F2FE),
                    border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFBAE6FD))
                ) {
                    Text(
                        text = item.subject.uppercase(),
                        color = studentAccentColor,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Spacer(Modifier.width(8.dp))

                // Status Badge
                if (isTeacher) {
                    if (item.isScheduled) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isDark) NeonWarning.copy(alpha = 0.15f) else Color(0xFFFFFBEB),
                            border = BorderStroke(1.dp, if (isDark) NeonWarning.copy(alpha = 0.4f) else Color(0xFFFDE68A))
                        ) {
                            Text(
                                text = "Terjadwal (Draft)",
                                color = if (isDark) NeonWarning else Color(0xFFB45309),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isDark) successColor.copy(alpha = 0.12f) else Color(0xFFDCFCE7),
                            border = BorderStroke(1.dp, if (isDark) successColor.copy(alpha = 0.35f) else Color(0xFF86EFAC))
                        ) {
                            Text(
                                text = "Aktif",
                                color = if (isDark) successColor else Color(0xFF15803D),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                    }
                } else if (item.isCompleted) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isDark) successColor.copy(alpha = 0.12f) else Color(0xFFDCFCE7),
                        border = BorderStroke(1.dp, if (isDark) successColor.copy(alpha = 0.35f) else Color(0xFF86EFAC))
                    ) {
                        Text(
                            text = "Selesai",
                            color = if (isDark) successColor else Color(0xFF15803D),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = innerCardBg,
                        border = BorderStroke(1.dp, innerCardBorder)
                    ) {
                        Text(
                            text = "Belum Selesai",
                            color = textTertiary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── MAIN ROW: OUTLINE ICON + TITLE & METADATA ────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(innerCardBg)
                        .border(1.dp, innerCardBorder, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(
                            id = if (isVideo) CoreR.drawable.ic_modern_video else CoreR.drawable.ic_modern_book
                        ),
                        contentDescription = null,
                        tint = if (item.isCompleted) successColor else studentAccentColor,
                        modifier = Modifier.size(20.dp),
                    )
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = textPrimary,
                        lineHeight = 20.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Library Book Page Range Pill
                    if (item.startPage != null && item.endPage != null) {
                        Spacer(Modifier.height(4.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = innerCardBg,
                            border = BorderStroke(1.dp, innerCardBorder)
                        ) {
                            Text(
                                text = "Hal. ${item.startPage} — ${item.endPage}",
                                color = textSecondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // Teacher & Class Info Row
                    val teacher = item.teacherName?.takeIf { it.isNotBlank() }
                    val rombel = item.className?.takeIf { it.isNotBlank() }
                    if (teacher != null || rombel != null) {
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = listOfNotNull(
                                teacher,
                                rombel?.let { "Kelas $it" }
                            ).joinToString(" • "),
                            color = textSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    val targetDate = if (item.isScheduled) item.releaseAt ?: item.createdAt else item.createdAt
                    if (!targetDate.isNullOrBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = CoreR.drawable.ic_modern_clock),
                                contentDescription = null,
                                tint = if (item.isScheduled) NeonWarning else textTertiary,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = if (item.isScheduled) {
                                    "Tayang: ${formatPublishTimestamp(targetDate)}"
                                } else {
                                    "Diterbitkan: ${formatPublishTimestamp(targetDate)}"
                                },
                                color = if (item.isScheduled) NeonWarning else textTertiary,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Description / Excerpt
            val descExcerpt = item.description?.replace(" • ", " - ")
            if (!descExcerpt.isNullOrBlank() && !descExcerpt.startsWith("Materi Bacaan Buku (Halaman")) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = descExcerpt,
                    color = textSecondary,
                    fontSize = 11.5.sp,
                    lineHeight = 16.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.height(12.dp))

            // ── BOTTOM META ROW: FORMAT CHIP + TIME + ACTION BUTTON ─────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(innerCardBg)
                    .border(1.dp, innerCardBorder, RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Text(
                        text = when {
                            isVideo -> "Video Pelajaran"
                            item.type == "ARTICLE" -> "Artikel Naskah"
                            item.type == "INFOGRAPHIC" || item.type == "IMAGE" -> "Infografis Digital"
                            isLibraryBook -> "Buku SIBI (Hal. ${item.startPage ?: 1} — ${item.endPage ?: ""})"
                            else -> "Buku / PDF"
                        },
                        color = textSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = "•",
                        color = textTertiary,
                        fontSize = 11.sp,
                    )

                    Text(
                        text = if (isVideo) "${item.readTimeMinutes} mnt tonton" else "${item.readTimeMinutes} mnt baca",
                        color = textTertiary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (isTeacher && item.completedCount >= 0) {
                        Text(
                            text = "•",
                            color = textTertiary,
                            fontSize = 11.sp,
                        )
                        Text(
                            text = "${item.completedCount} selesai",
                            color = TeacherNeon,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Icon(
                    painter = painterResource(id = CoreR.drawable.ic_modern_chevron_right),
                    contentDescription = "Buka",
                    tint = textSecondary,
                    modifier = Modifier.size(12.dp)
                )
            }

            // Bottom completion bar
            if (item.isCompleted) {
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(1.5.dp))
                        .background(successColor)
                )
            }
        }
    }
}

// ── EMPTY STATE (22.dp ROUNDED, ADAPTIVE) ─────────────────────────────────────

@Composable
private fun EmptyMaterialState(searchQuery: String) {
    val isDark = LocalIsDarkTheme.current
    val cardBg = if (isDark) Color(0xFF111113) else Color.White
    val cardBorder = if (isDark) Color(0xFF1F1F23) else Color(0xFFE2E8F0)
    val textPrimary = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A)
    val textTertiary = if (isDark) Color(0xFF71717A) else Color(0xFF94A3B8)
    val innerCardBg = if (isDark) Color(0xFF161618) else Color(0xFFF8FAFC)
    val innerCardBorder = if (isDark) Color(0xFF27272A) else Color(0xFFE2E8F0)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp)),
        shape = RoundedCornerShape(22.dp),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder),
        shadowElevation = if (isDark) 0.dp else 1.dp
    ) {
        Column(
            modifier = Modifier.padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(innerCardBg)
                    .border(1.dp, innerCardBorder, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = CoreR.drawable.ic_modern_book),
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(Modifier.height(14.dp))
            Text(
                text = "Materi Tidak Ditemukan",
                color = textPrimary,
                fontSize = 15.5.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (searchQuery.isNotEmpty()) "Coba kata kunci pencarian yang lain" else "Belum ada materi ajar pada kategori ini.",
                color = textTertiary,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}
