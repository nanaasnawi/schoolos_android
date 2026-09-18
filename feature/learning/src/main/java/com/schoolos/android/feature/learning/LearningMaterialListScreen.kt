package com.schoolos.android.feature.learning

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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 0.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // ── TOP APP BAR ──────────────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CustomBackButton(onClick = onBack)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Modul Pembelajaran",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                color = TextPrimary,
                                letterSpacing = (-0.3).sp
                            )
                            Text(
                                text = if (isTeacher) "Kelola bahan ajar & modul kelas" else "Bahan bacaan & video materi",
                                fontSize = 11.sp,
                                color = TextTertiary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Role pill badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isTeacher) TeacherNeon.copy(alpha = 0.12f) else StudentNeon.copy(alpha = 0.12f))
                            .border(1.dp, if (isTeacher) TeacherNeon.copy(alpha = 0.25f) else StudentNeon.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 9.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (isTeacher) "GURU" else "SISWA",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isTeacher) TeacherNeon else StudentNeon,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }

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

            // ── LOADING STATE ────────────────────────────────────────────────
            if (state.isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = NeonBlue, modifier = Modifier.size(32.dp))
                    }
                }
            }

            // ── EMPTY STATE ──────────────────────────────────────────────────
            if (!state.isLoading && materials.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(3.dp, RoundedCornerShape(20.dp), spotColor = GlassOverlay)
                            .clip(RoundedCornerShape(20.dp))
                            .background(CosmicNavy)
                            .border(1.dp, GlassBorder, RoundedCornerShape(20.dp))
                            .padding(36.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("📚", fontSize = 44.sp)
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = "Materi Tidak Ditemukan",
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
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
            .shadow(8.dp, RoundedCornerShape(24.dp), spotColor = Color(0x302563EB), ambientColor = Color(0x152563EB))
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        Color(0xFF1E40AF), // Deep Royal Blue
                        Color(0xFF2563EB), // Vibrant Electric Blue
                        Color(0xFF6D28D9), // Rich Violet
                    )
                )
            )
    ) {
        // Decorative background geometry
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 24.dp, y = (-20).dp)
                .size(120.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.08f))
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = (-16).dp, y = 16.dp)
                .size(70.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.06f))
        )

        Column(modifier = Modifier.padding(16.dp)) {
            // Top Tag Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White.copy(alpha = 0.18f))
                        .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text("📚", fontSize = 12.sp)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "MODUL & BAHAN AJAR",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.1.sp
                    )
                }

                // Completion Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.18f))
                        .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        "$completionPercent% Selesai",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = "Modul Belajar Siswa",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-0.3).sp
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "Pelajari modul mandiri, bab buku kurikulum, dan video materi.",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 12.sp,
                lineHeight = 16.sp
            )

            Spacer(Modifier.height(14.dp))

            // Quick Stats Cards Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatMiniCard(
                    title = "Total Modul",
                    value = "$totalMaterials",
                    emoji = "📖",
                    modifier = Modifier.weight(1f)
                )
                StatMiniCard(
                    title = "Selesai",
                    value = "$completedCount",
                    emoji = "✅",
                    valueColor = Color(0xFF86EFAC),
                    modifier = Modifier.weight(1f)
                )
                StatMiniCard(
                    title = "Belum",
                    value = "$pendingCount",
                    emoji = "⏳",
                    valueColor = Color(0xFFFDE047),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(12.dp))

            // Animated Smooth Progress Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color.Black.copy(alpha = 0.25f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color(0xFF38BDF8),
                                    Color(0xFF34D399)
                                )
                            )
                        )
                )
            }
        }
    }
}

@Composable
private fun StatMiniCard(
    title: String,
    value: String,
    emoji: String,
    valueColor: Color = Color.White,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.12f))
            .border(1.dp, Color.White.copy(alpha = 0.20f), RoundedCornerShape(12.dp))
            .padding(vertical = 8.dp, horizontal = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(emoji, fontSize = 11.sp)
            Spacer(Modifier.width(4.dp))
            Text(
                text = value,
                color = valueColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black
            )
        }
        Spacer(Modifier.height(2.dp))
        Text(
            text = title,
            color = Color.White.copy(alpha = 0.75f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
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
    data class TabDef(val id: String, val label: String, val count: Int, val emoji: String)

    val tabs = if (isTeacher) {
        listOf(
            TabDef("Semua", "Semua", totalCount, "🌐"),
            TabDef("Buku & PDF", "Buku & PDF", bookPdfCount, "📖"),
            TabDef("Video", "Video", videoCount, "🎥"),
        )
    } else {
        listOf(
            TabDef("Semua", "Semua", totalCount, "🌐"),
            TabDef("Buku & PDF", "Buku & PDF", bookPdfCount, "📖"),
            TabDef("Video", "Video", videoCount, "🎥"),
            TabDef("Belum Selesai", "Belum", pendingCount, "⏳"),
            TabDef("Selesai", "Selesai", completedCount, "✅"),
        )
    }

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(tabs) { tab ->
            val isSelected = tab.id == selectedCategory
            val activeColor = when (tab.id) {
                "Selesai" -> NeonSuccess
                "Belum Selesai" -> NeonWarning
                "Video" -> StudentNeon
                else -> NeonBlue
            }

            Box(
                modifier = Modifier
                    .shadow(if (isSelected) 3.dp else 1.dp, RoundedCornerShape(12.dp), spotColor = if (isSelected) activeColor.copy(alpha = 0.35f) else GlassOverlay)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSelected) activeColor else CosmicNavy)
                    .border(1.dp, if (isSelected) activeColor else GlassBorder, RoundedCornerShape(12.dp))
                    .clickable { onSelect(tab.id) }
                    .padding(horizontal = 12.dp, vertical = 7.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(tab.emoji, fontSize = 12.sp)
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = tab.label,
                        color = if (isSelected) Color.White else TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                    )
                    Spacer(Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) Color.White.copy(alpha = 0.25f) else CosmicDark)
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "${tab.count}",
                            color = if (isSelected) Color.White else TextTertiary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
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
    // Determine card accent
    val cardAccent = if (item.isCompleted) NeonSuccess else item.color
    val isLibraryBook = item.startPage != null

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(3.dp, RoundedCornerShape(20.dp), spotColor = GlassOverlay, ambientColor = GlassOverlay)
            .clip(RoundedCornerShape(20.dp))
            .background(CosmicNavy)
            .border(
                width = if (item.isCompleted) 1.5.dp else 1.dp,
                color = if (item.isCompleted) NeonSuccess.copy(alpha = 0.4f) else GlassBorder,
                shape = RoundedCornerShape(20.dp)
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
                // Genuine Subject Tag with Dot Indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .clip(RoundedCornerShape(8.dp))
                        .background(cardAccent.copy(alpha = 0.10f))
                        .border(1.dp, cardAccent.copy(alpha = 0.22f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(cardAccent)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = item.subject.uppercase(),
                        color = cardAccent,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.4.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(Modifier.width(8.dp))

                // Status Badge
                if (isTeacher) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(TeacherNeon.copy(alpha = 0.12f))
                            .border(1.dp, TeacherNeon.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.People, null, tint = TeacherNeon, modifier = Modifier.size(12.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("${item.completedCount} selesai", color = TeacherNeon, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                } else if (item.isCompleted) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(NeonSuccess.copy(alpha = 0.14f))
                            .border(1.dp, NeonSuccess.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, null, tint = NeonSuccess, modifier = Modifier.size(12.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Selesai", color = NeonSuccess, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(CosmicDark)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Schedule, null, tint = TextTertiary, modifier = Modifier.size(11.dp))
                            Spacer(Modifier.width(3.dp))
                            Text("Belum Selesai", color = TextTertiary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── MAIN ROW: MEDIA ICON + TITLE & METADATA ─────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // Media Icon Container
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    cardAccent.copy(alpha = 0.18f),
                                    cardAccent.copy(alpha = 0.08f)
                                )
                            )
                        )
                        .border(1.dp, cardAccent.copy(alpha = 0.25f), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when {
                            item.type == "VIDEO" -> Icons.Default.PlayCircleFilled
                            isLibraryBook -> Icons.AutoMirrored.Filled.MenuBook
                            item.type == "PDF" -> Icons.Default.Description
                            else -> Icons.Default.Article
                        },
                        contentDescription = null,
                        tint = cardAccent,
                        modifier = Modifier.size(24.dp)
                    )
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
                        Spacer(Modifier.height(6.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFFEF3C7))
                                .border(1.dp, Color(0xFFFDE68A), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("📖", fontSize = 10.sp)
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "Halaman ${item.startPage} — ${item.endPage}",
                                color = Color(0xFF92400E),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Teacher & Class Info Row
                    val teacher = item.teacherName?.takeIf { it.isNotBlank() }
                    val rombel = item.className?.takeIf { it.isNotBlank() }
                    if (teacher != null || rombel != null) {
                        Spacer(Modifier.height(5.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = listOfNotNull(
                                    teacher?.let { "👨‍🏫 $it" },
                                    rombel?.let { "🏫 $it" }
                                ).joinToString(" • "),
                                color = TextTertiary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Description / Excerpt (Clean excerpt if not just duplicate instructions)
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

            Spacer(Modifier.height(12.dp))

            // ── BOTTOM META ROW: FORMAT CHIP + TIME + ACTION BUTTON ─────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Format Chip
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(CosmicDark)
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = when {
                                item.type == "VIDEO" -> "🎥 Video Pelajaran"
                                isLibraryBook -> "📖 Buku Siswa"
                                else -> "📄 Modul PDF"
                            },
                            color = TextSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Reading / Watch Time
                    Text(
                        text = "⏱️ ${item.readTimeMinutes} mnt baca",
                        color = TextTertiary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Action Forward Button
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(if (item.isCompleted) NeonSuccess.copy(alpha = 0.12f) else NeonBlue.copy(alpha = 0.10f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Buka",
                        tint = if (item.isCompleted) NeonSuccess else NeonBlue,
                        modifier = Modifier.size(15.dp)
                    )
                }
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
            .shadow(8.dp, RoundedCornerShape(24.dp), spotColor = Color(0x30059669), ambientColor = Color(0x15059669))
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        Color(0xFF047857), // Deep Emerald
                        Color(0xFF059669), // Emerald
                        Color(0xFF0D9488), // Teal
                    )
                )
            )
    ) {
        // Decorative accent circle
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 20.dp, y = (-16).dp)
                .size(100.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.08f))
        )

        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White.copy(alpha = 0.18f))
                        .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text("👨‍🏫", fontSize = 12.sp)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "PORTAL MATERI GURU",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.1.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.18f))
                        .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        "$totalMaterials Materi Aktif",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = "Manajemen Bahan Ajar",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-0.3).sp
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "Pantau modul belajar siswa dan distribusikan bahan bacaan baru.",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 12.sp,
                lineHeight = 16.sp
            )

            Spacer(Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Total materi card
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.14f))
                        .border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(12.dp))
                        .padding(vertical = 8.dp, horizontal = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "$totalMaterials",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        "Total Modul",
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Avg completion card
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.14f))
                        .border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(12.dp))
                        .padding(vertical = 8.dp, horizontal = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "$avgCompletion%",
                        color = if (avgCompletion >= 70) Color(0xFF86EFAC) else Color(0xFFFDE047),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        "Rata-rata Selesai",
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
