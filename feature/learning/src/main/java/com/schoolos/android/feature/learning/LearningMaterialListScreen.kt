package com.schoolos.android.feature.learning

import com.schoolos.android.core.R as CoreR
import androidx.compose.ui.res.painterResource
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.schoolos.android.core.designsystem.*

@Composable
fun LearningMaterialListScreen(
    onBack: () -> Unit = {},
    onMaterialClick: (String) -> Unit = {},
    onCreateMaterial: () -> Unit = {},
    subjectId: String = "",
    viewModel: LearningViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val materials = state.materials
    val role = state.userRole.lowercase()
    val isTeacher = role in listOf("teacher", "guru")

    Scaffold(
        containerColor = CosmicBlack,
        topBar = {
            ExecutiveTopBar(
                title = "Modul Pembelajaran",
                subtitle = if (isTeacher) "Bahan ajar & modul kelas" else "Bahan bacaan & video",
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
                    text = { Text("Buat Materi", fontWeight = FontWeight.Bold) }
                )
            }
        }
    ) { padding ->
        PullRefreshContainer(
            isRefreshing = state.isRefreshing,
            onRefresh = viewModel::refresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {

            // ── ACTIVE SUBJECT FILTER (passed from schedule/session) ─────────
            if (state.subjectFilter != null) {
                item {
                    SubjectFilterChip(
                        subject = state.subjectFilter!!,
                        onClear = viewModel::clearSubjectFilter,
                    )
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
                        .shadow(2.dp, RoundedCornerShape(16.dp), spotColor = GlassOverlay, ambientColor = GlassOverlay)
                        .clip(RoundedCornerShape(16.dp))
                        .background(CosmicNavy)
                        .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
                        .padding(horizontal = 14.dp, vertical = 11.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Search, null, tint = TextTertiary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        BasicTextField(
                            value = state.searchQuery,
                            onValueChange = { viewModel.onSearchQueryChanged(it) },
                            modifier = Modifier.weight(1f),
                            textStyle = TextStyle(color = TextPrimary, fontSize = 14.sp),
                            singleLine = true,
                            decorationBox = { innerTextField ->
                                if (state.searchQuery.isEmpty()) {
                                    Text("Cari judul materi, bab, topik...", color = TextTertiary, fontSize = 13.sp)
                                }
                                innerTextField()
                            }
                        )
                        if (state.searchQuery.isNotEmpty()) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Hapus",
                                tint = TextTertiary,
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
                    pendingCount = state.pendingCount,
                    completedCount = state.totalCompleted,
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
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(CosmicNavy)
                            .border(0.5.dp, GlassBorder, RoundedCornerShape(12.dp))
                            .padding(28.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(com.schoolos.android.core.designsystem.CosmicSurface2)
                                    .border(0.5.dp, GlassBorder, RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                    contentDescription = null,
                                    tint = TextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = "Materi Tidak Ditemukan",
                                color = TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = if (state.searchQuery.isNotEmpty()) "Coba kata kunci pencarian yang lain" else "Belum ada materi ajar pada kategori ini",
                                color = TextTertiary,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
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
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CosmicNavy)
            .border(0.5.dp, GlassBorder, RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ShimmerBox(modifier = Modifier.width(64.dp).height(16.dp), shape = RoundedCornerShape(4.dp))
                ShimmerBox(modifier = Modifier.width(52.dp).height(16.dp), shape = RoundedCornerShape(4.dp))
            }
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                ShimmerBox(modifier = Modifier.size(38.dp), shape = RoundedCornerShape(8.dp))
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    ShimmerBox(modifier = Modifier.fillMaxWidth(0.72f).height(15.dp))
                    Spacer(Modifier.height(6.dp))
                    ShimmerBox(modifier = Modifier.fillMaxWidth(0.48f).height(12.dp))
                    Spacer(Modifier.height(6.dp))
                    ShimmerBox(modifier = Modifier.fillMaxWidth(0.30f).height(10.dp))
                }
            }
        }
    }
}

// ── STUDENT MATERIAL HERO BANNER ──────────────────────────────────────────────

@Composable
private fun StudentMaterialHeroBanner(
    totalMaterials: Int,
    completedCount: Int,
    pendingCount: Int,
) {
    val progressFraction = if (totalMaterials > 0) completedCount.toFloat() / totalMaterials.toFloat() else 0f
    val animatedProgress by animateFloatAsState(targetValue = progressFraction, label = "materialProgress")
    val completionPercent = (progressFraction * 100).toInt()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CosmicNavy)
            .border(0.5.dp, GlassBorder, RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Top Tag Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "MODUL BELAJAR",
                    color = TextTertiary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.5.sp
                )

                Text(
                    text = "$completionPercent% Selesai",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Bahan Bacaan & Materi",
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "Pelajari modul mandiri dan video pembelajaran.",
                color = TextTertiary,
                fontSize = 12.sp,
            )

            Spacer(Modifier.height(12.dp))

            // Quick Stats Cards Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatMiniCard(
                    title = "Total",
                    value = "$totalMaterials",
                    modifier = Modifier.weight(1f)
                )
                StatMiniCard(
                    title = "Selesai",
                    value = "$completedCount",
                    modifier = Modifier.weight(1f)
                )
                StatMiniCard(
                    title = "Belum",
                    value = "$pendingCount",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(10.dp))

            // Progress Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(com.schoolos.android.core.designsystem.CosmicSurface2)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(2.dp))
                        .background(NeonBlue)
                )
            }
        }
    }
}

@Composable
private fun StatMiniCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .height(58.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(com.schoolos.android.core.designsystem.CosmicSurface2)
            .border(0.5.dp, GlassBorder, RoundedCornerShape(8.dp))
            .padding(vertical = 6.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = value,
            color = TextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = title,
            color = TextTertiary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// ── CATEGORY & FORMAT TABS ────────────────────────────────────────────────────

@Composable
private fun MaterialCategoryTabs(
    selectedCategory: String,
    onSelect: (String) -> Unit,
    totalCount: Int,
    bookPdfCount: Int,
    videoCount: Int,
    pendingCount: Int,
    completedCount: Int,
    isTeacher: Boolean,
) {
    data class TabDef(val id: String, val label: String, val count: Int)

    val tabs = if (isTeacher) {
        listOf(
            TabDef("Semua", "Semua", totalCount),
            TabDef("Buku & PDF", "Buku & PDF", bookPdfCount),
            TabDef("Video", "Video", videoCount),
        )
    } else {
        listOf(
            TabDef("Semua", "Semua", totalCount),
            TabDef("Buku & PDF", "Buku & PDF", bookPdfCount),
            TabDef("Video", "Video", videoCount),
            TabDef("Belum Selesai", "Belum", pendingCount),
            TabDef("Selesai", "Selesai", completedCount),
        )
    }

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(tabs) { tab ->
            val isSelected = tab.id == selectedCategory

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) com.schoolos.android.core.designsystem.CosmicSurface2 else CosmicNavy)
                    .border(
                        0.5.dp,
                        if (isSelected) com.schoolos.android.core.designsystem.GlassBorder2 else GlassBorder,
                        RoundedCornerShape(8.dp)
                    )
                    .clickable { onSelect(tab.id) }
                    .padding(horizontal = 12.dp, vertical = 7.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = tab.label,
                        color = if (isSelected) TextPrimary else TextTertiary,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                    if (tab.count > 0) {
                        Spacer(Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isSelected) com.schoolos.android.core.designsystem.CosmicSurface3 else com.schoolos.android.core.designsystem.CosmicSurface2)
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "${tab.count}",
                                color = if (isSelected) TextPrimary else TextTertiary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── MODERN MATERIAL CARD ─────────────────────────────────────────────────────

@Composable
private fun ModernMaterialCard(
    item: MaterialItem,
    isTeacher: Boolean = false,
    onClick: () -> Unit
) {
    val cardAccent = if (item.isCompleted) NeonSuccess else item.color
    val isLibraryBook = item.startPage != null

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CosmicNavy)
            .border(
                width = 0.5.dp,
                color = if (item.isCompleted) NeonSuccess.copy(alpha = 0.35f) else GlassBorder,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Column {
            // ── TOP ROW: SUBJECT TAG & STATUS BADGE ──────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(com.schoolos.android.core.designsystem.CosmicSurface2)
                        .border(0.5.dp, GlassBorder, RoundedCornerShape(4.dp))
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = item.subject.uppercase(),
                        color = TextSecondary,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(Modifier.width(8.dp))

                // Status Badge
                if (isTeacher) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(com.schoolos.android.core.designsystem.CosmicSurface2)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("${item.completedCount} selesai", color = TextTertiary, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                    }
                } else if (item.isCompleted) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(NeonSuccess.copy(alpha = 0.12f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("Selesai", color = NeonSuccess, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(com.schoolos.android.core.designsystem.CosmicSurface2)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("Belum Selesai", color = TextTertiary, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // ── MAIN ROW: MEDIA ICON + TITLE & METADATA ─────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // Media Icon Container
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(com.schoolos.android.core.designsystem.CosmicSurface2)
                        .border(0.5.dp, GlassBorder, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLibraryBook || item.type != "VIDEO") {
                        Icon(
                            painter = painterResource(id = CoreR.drawable.ic_modern_book),
                            contentDescription = null,
                            tint = NeonBlue,
                            modifier = Modifier.size(19.dp),
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.PlayCircleFilled,
                            contentDescription = null,
                            tint = NeonSuccess,
                            modifier = Modifier.size(19.dp),
                        )
                    }
                }

                Spacer(Modifier.width(12.dp))

                // Title & Subtitle Info
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        color = TextPrimary,
                        lineHeight = 20.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Library Book Page Range Pill
                    if (item.startPage != null && item.endPage != null) {
                        Spacer(Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(com.schoolos.android.core.designsystem.CosmicSurface2)
                                .border(0.5.dp, GlassBorder, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Hal. ${item.startPage} — ${item.endPage}",
                                color = TextSecondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Teacher & Class Info Row
                    val teacher = item.teacherName?.takeIf { it.isNotBlank() }
                    val rombel = item.className?.takeIf { it.isNotBlank() }
                    if (teacher != null || rombel != null) {
                        Spacer(Modifier.height(3.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = listOfNotNull(
                                    teacher,
                                    rombel?.let { "Kelas $it" }
                                ).joinToString(" • "),
                                color = TextTertiary,
                                fontSize = 11.sp,
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
                    color = TextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.height(10.dp))

            // ── BOTTOM META ROW: FORMAT CHIP + TIME + ACTION BUTTON ─────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(com.schoolos.android.core.designsystem.CosmicSurface2)
                    .border(0.5.dp, GlassBorder, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
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
                            item.type == "VIDEO" -> "Video Pelajaran"
                            isLibraryBook -> "Buku Siswa"
                            else -> "Modul PDF"
                        },
                        color = TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = "•",
                        color = TextTertiary,
                        fontSize = 10.sp,
                    )

                    Text(
                        text = "${item.readTimeMinutes} mnt baca",
                        color = TextTertiary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Icon(
                    painter = painterResource(id = CoreR.drawable.ic_modern_chevron_right),
                    contentDescription = "Buka",
                    tint = TextTertiary,
                    modifier = Modifier.size(14.dp)
                )
            }

            // Bottom completion bar
            if (item.isCompleted) {
                Spacer(Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(1.5.dp))
                        .background(NeonSuccess)
                )
            }
        }
    }
}

// ── TEACHER LEARNING HERO BANNER ──────────────────────────────────────────────

@Composable
private fun TeacherLearningHeroBanner(
    totalMaterials: Int,
    completedCount: Int,
) {
    val avgCompletion = if (totalMaterials > 0) (completedCount * 100) / totalMaterials else 0

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CosmicNavy)
            .border(0.5.dp, GlassBorder, RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "MANAJEMEN BAHAN AJAR",
                    color = TextTertiary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.5.sp
                )

                Text(
                    text = "$totalMaterials Materi Aktif",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Modul & Bahan Bacaan",
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "Kelola dan distribusikan bahan ajar untuk siswa.",
                color = TextTertiary,
                fontSize = 12.sp,
            )

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Total materi card
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(com.schoolos.android.core.designsystem.CosmicSurface2)
                        .border(0.5.dp, GlassBorder, RoundedCornerShape(8.dp))
                        .padding(vertical = 8.dp, horizontal = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "$totalMaterials",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "Total Modul",
                        color = TextTertiary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Normal
                    )
                }

                // Avg completion card
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(com.schoolos.android.core.designsystem.CosmicSurface2)
                        .border(0.5.dp, GlassBorder, RoundedCornerShape(8.dp))
                        .padding(vertical = 8.dp, horizontal = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "$avgCompletion%",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "Rata-rata Selesai",
                        color = TextTertiary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
            }
        }
    }
}
