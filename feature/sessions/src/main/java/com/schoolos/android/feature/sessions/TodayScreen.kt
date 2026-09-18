package com.schoolos.android.feature.sessions

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.schoolos.android.core.designsystem.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private data class DayTab(
    val dayName: String,
    val dateNum: String,
    val fullDate: String,
    val isToday: Boolean,
    val localDate: LocalDate
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    onBack: (() -> Unit)? = null,
    onSessionClick: (String) -> Unit = {},
    viewModel: TodayViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val idLocale = remember { Locale("id", "ID") }

    val role = state.userRole.lowercase()
    val isTeacher = role == "teacher" || role == "guru"
    val accentColor = if (isTeacher) TeacherNeon else StudentNeon

    // Generate weekly calendar days (Monday to Saturday) based on the week of today
    val days = remember {
        val today = LocalDate.now()
        val startOfWeek = today.with(java.time.DayOfWeek.MONDAY)
        (0..5).map { i ->
            val date = startOfWeek.plusDays(i.toLong())
            DayTab(
                dayName = date.format(DateTimeFormatter.ofPattern("EEE", idLocale)).uppercase(),
                dateNum = date.dayOfMonth.toString(),
                fullDate = date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", idLocale)),
                isToday = date == today,
                localDate = date
            )
        }
    }

    val selectedDay = days.find { it.localDate == state.selectedDate } ?: days.firstOrNull { it.isToday } ?: days.first()
    val isViewingToday = state.selectedDate == LocalDate.now()

    val totalDaySessions = state.active.size + state.upcoming.size + state.completed.size
    val activeCount = state.active.size
    val upcomingCount = state.upcoming.size
    val completedCount = state.completed.size

    PullRefreshContainer(
        isRefreshing = state.isRefreshing,
        onRefresh = viewModel::refresh,
        modifier = Modifier.fillMaxSize(),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (state.isLoading) {
                LoadingState()
            } else if (state.error != null) {
                ErrorState(message = state.error!!, onRetry = viewModel::refresh)
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding(),
                    contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 4.dp, bottom = 36.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {                    
                    // ── 2. SIGNATURE EXECUTIVE HERO BANNER (Single Source of Truth) ──
                    item {
                        AgendaHeroBanner(
                            fullDateTitle = selectedDay.fullDate,
                            className = state.className,
                            isTeacher = isTeacher,
                            accentColor = accentColor,
                            totalCount = totalDaySessions,
                            activeCount = activeCount,
                            upcomingCount = upcomingCount,
                            completedCount = completedCount,
                            hasLiveSession = activeCount > 0,
                        )
                    }

                    // ── 3. INTERACTIVE WEEKLY CALENDAR STRIP ─────────────────────────
                    item {
                        WeeklyCalendarStrip(
                            days = days,
                            selectedDate = state.selectedDate,
                            scheduleCounts = state.weekScheduleCounts,
                            accentColor = accentColor,
                            onDaySelected = { date -> viewModel.onDateSelected(date) }
                        )
                    }

                    // ── 4. SMART STATUS FILTER CHIPS ─────────────────────────────────
                    item {
                        ScheduleFilterChips(
                            selectedFilter = state.selectedFilter,
                            onFilterSelected = { viewModel.onFilterSelected(it) },
                            totalCount = totalDaySessions,
                            activeCount = activeCount,
                            upcomingCount = upcomingCount,
                            completedCount = completedCount,
                            accentColor = accentColor,
                        )
                    }

                    // ── 5. TIMELINE SECTION HEADER ───────────────────────────────────
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 0.dp, vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Daftar Sesi Pembelajaran",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(CosmicNavy)
                                    .border(1.dp, GlassBorder, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "${state.displayedSessions.size} Sesi Ditampilkan",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextSecondary,
                                )
                            }
                        }
                    }

                    // ── 6. SESSIONS CONTENT LIST ─────────────────────────────────────
                    if (isTeacher) {
                        teacherAgendaContent(
                            sessions = state.displayedSessions,
                            onSessionClick = onSessionClick,
                            accentColor = accentColor,
                            selectedDayName = selectedDay.fullDate.substringBefore(","),
                            onJumpToToday = { viewModel.onDateSelected(LocalDate.now()) },
                            isToday = isViewingToday,
                        )
                    } else {
                        studentAgendaContent(
                            sessions = state.displayedSessions,
                            onSessionClick = onSessionClick,
                            accentColor = accentColor,
                            selectedDayName = selectedDay.fullDate.substringBefore(","),
                            onJumpToToday = { viewModel.onDateSelected(LocalDate.now()) },
                            isToday = isViewingToday,
                        )
                    }
                }
            }
        }
    }
}

// ── TOP NAVIGATION ────────────────────────────────────────────────────────────

@Composable
private fun ScreenTopNavigation(
    title: String,
    isViewingToday: Boolean,
    onBack: (() -> Unit)?,
    onJumpToToday: () -> Unit,
    accentColor: Color,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBack != null) {
                CustomBackButton(onClick = onBack)
                Spacer(Modifier.width(12.dp))
            }
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = TextPrimary,
                letterSpacing = (-0.3).sp,
            )
        }

        if (!isViewingToday) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(accentColor.copy(alpha = 0.15f))
                    .border(1.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                    .clickable(onClick = onJumpToToday)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Today,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = "Hari Ini",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor,
                    )
                }
            }
        }
    }
}

// ── SIGNATURE EXECUTIVE HERO BANNER ───────────────────────────────────────────

@Composable
private fun AgendaHeroBanner(
    fullDateTitle: String,
    className: String,
    isTeacher: Boolean,
    accentColor: Color,
    totalCount: Int,
    activeCount: Int,
    upcomingCount: Int,
    completedCount: Int,
    hasLiveSession: Boolean,
) {
    val bannerBrush = if (isTeacher) {
        Brush.linearGradient(
            listOf(
                Color(0xFF0F766E), // Deep Teal
                Color(0xFF0D9488), // Teal
                Color(0xFF047857), // Deep Emerald
            )
        )
    } else {
        Brush.linearGradient(
            listOf(
                Color(0xFF1E40AF), // Deep Royal Blue
                Color(0xFF2563EB), // Vibrant Electric Blue
                Color(0xFF6D28D9), // Rich Violet
            )
        )
    }

    val progress = if (totalCount > 0) completedCount.toFloat() / totalCount.toFloat() else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "progress"
    )

    // Pulse animation for live indicator
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(750),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(24.dp),
                spotColor = accentColor.copy(alpha = 0.45f)
            )
            .clip(RoundedCornerShape(24.dp))
            .background(bannerBrush)
            .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(24.dp))
    ) {
        // Decorative background geometry
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 28.dp, y = (-24).dp)
                .size(130.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.08f))
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = (-20).dp, y = 20.dp)
                .size(80.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.06f))
        )

        Column(modifier = Modifier.padding(18.dp)) {
            // Top Tag Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Role Tag Pill
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White.copy(alpha = 0.18f))
                        .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (isTeacher) "👨‍🏫" else "🎒",
                        fontSize = 11.sp
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = if (isTeacher) "AGENDA MENGAJAR" else "JADWAL PELAJARAN",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.8.sp
                    )
                }

                // Live or Status Badge
                if (hasLiveSession) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.20f))
                            .border(1.dp, Color.White.copy(alpha = 0.40f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 9.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(NeonSuccess.copy(alpha = pulseAlpha))
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(
                                text = "LIVE BERJALAN",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                } else if (completedCount == totalCount && totalCount > 0) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.18f))
                            .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 9.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "✅ SEMUA SELESAI",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.16f))
                            .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 9.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "$totalCount Sesi Terjadwal",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Main Date Title
            Text(
                text = fullDateTitle,
                color = Color.White,
                fontSize = 21.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-0.4).sp
            )

            Spacer(Modifier.height(2.dp))

            // Subtitle
            val subtitleText = when {
                isTeacher && className.isNotBlank() -> "Wali Kelas $className • Pantau sesi mengajar, absensi, dan materi."
                className.isNotBlank() -> "Rombel Kelas $className • Tatap muka kurikulum & sesi daring aktif."
                isTeacher -> "Pantau sesi mengajar harian, kehadiran siswa, dan materi."
                else -> "Jadwal tatap muka harian, modul mandiri, dan tugas kelas."
            }
            Text(
                text = subtitleText,
                color = Color.White.copy(alpha = 0.88f),
                fontSize = 12.sp,
                lineHeight = 16.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(14.dp))

            // Single Embedded Stats Row (No duplicate cards anywhere else)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatMiniCard(
                    title = "Total Sesi",
                    value = "$totalCount",
                    emoji = "📋",
                    modifier = Modifier.weight(1f)
                )
                StatMiniCard(
                    title = "Aktif / Nanti",
                    value = "${activeCount + upcomingCount}",
                    emoji = "⏳",
                    valueColor = Color(0xFF93C5FD),
                    modifier = Modifier.weight(1f)
                )
                StatMiniCard(
                    title = "Selesai",
                    value = "$completedCount",
                    emoji = "✅",
                    valueColor = Color(0xFF86EFAC),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(12.dp))

            // Completion Progress Bar
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Progres Hari Ini",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.82f),
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = if (totalCount > 0) "$completedCount dari $totalCount selesai (${(progress * 100).toInt()}%)" else "0 Sesi",
                        fontSize = 11.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(5.dp))
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
            .background(Color.White.copy(alpha = 0.14f))
            .border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(12.dp))
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
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ── INTERACTIVE WEEKLY CALENDAR STRIP ─────────────────────────────────────────

@Composable
private fun WeeklyCalendarStrip(
    days: List<DayTab>,
    selectedDate: LocalDate,
    scheduleCounts: Map<LocalDate, Int>,
    accentColor: Color,
    onDaySelected: (LocalDate) -> Unit,
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        itemsIndexed(days) { _, day ->
            val isSelected = day.localDate == selectedDate
            val isToday = day.isToday
            val classCount = scheduleCounts[day.localDate] ?: 0

            val cardBg = when {
                isSelected -> Brush.verticalGradient(listOf(accentColor, accentColor.copy(alpha = 0.85f)))
                isToday -> Brush.verticalGradient(listOf(accentColor.copy(alpha = 0.16f), accentColor.copy(alpha = 0.06f)))
                else -> Brush.verticalGradient(listOf(CosmicNavy, CosmicNavy))
            }

            val borderColor = when {
                isSelected -> Color.White.copy(alpha = 0.45f)
                isToday -> accentColor.copy(alpha = 0.5f)
                else -> GlassBorder
            }

            Column(
                modifier = Modifier
                    .width(55.dp)
                    .then(
                        if (isSelected) Modifier.shadow(6.dp, RoundedCornerShape(16.dp), spotColor = accentColor.copy(alpha = 0.55f))
                        else Modifier
                    )
                    .clip(RoundedCornerShape(16.dp))
                    .background(cardBg)
                    .border(1.dp, borderColor, RoundedCornerShape(16.dp))
                    .clickable { onDaySelected(day.localDate) }
                    .padding(vertical = 10.dp, horizontal = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Day name (e.g. SEN, SEL)
                Text(
                    text = day.dayName,
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                    color = when {
                        isSelected -> Color.White
                        isToday -> accentColor
                        else -> TextTertiary
                    },
                    letterSpacing = 0.5.sp
                )

                Spacer(Modifier.height(4.dp))

                // Date number (e.g. 18)
                Text(
                    text = day.dateNum,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = when {
                        isSelected -> Color.White
                        isToday -> accentColor
                        else -> TextPrimary
                    },
                )

                Spacer(Modifier.height(5.dp))

                // Bottom Indicator: active bar for selected, dot for class count, or empty
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .width(14.dp)
                            .height(3.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                } else if (classCount > 0) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (isToday) accentColor else Color(0xFF38BDF8))
                    )
                } else {
                    Spacer(Modifier.height(6.dp))
                }
            }
        }
    }
}

// ── SMART STATUS FILTER CHIPS ─────────────────────────────────────────────────

@Composable
private fun ScheduleFilterChips(
    selectedFilter: String,
    onFilterSelected: (String) -> Unit,
    totalCount: Int,
    activeCount: Int,
    upcomingCount: Int,
    completedCount: Int,
    accentColor: Color,
) {
    data class TabItem(val id: String, val label: String, val count: Int, val dotColor: Color?)

    val tabs = listOf(
        TabItem("ALL", "Semua", totalCount, null),
        TabItem("ACTIVE", "Berlangsung", activeCount, NeonSuccess),
        TabItem("UPCOMING", "Mendatang", upcomingCount, Color(0xFF38BDF8)),
        TabItem("COMPLETED", "Selesai", completedCount, TextTertiary),
    )

    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        itemsIndexed(tabs) { _, tab ->
            val isSelected = selectedFilter == tab.id
            val bg = if (isSelected) accentColor else CosmicNavy
            val contentColor = if (isSelected) Color.White else TextSecondary
            val border = if (isSelected) accentColor else GlassBorder

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(bg)
                    .border(1.dp, border, RoundedCornerShape(12.dp))
                    .clickable { onFilterSelected(tab.id) }
                    .padding(horizontal = 12.dp, vertical = 7.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (tab.dotColor != null) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) Color.White else tab.dotColor)
                        )
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(
                        text = tab.label,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = contentColor,
                    )
                    Spacer(Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) Color.White.copy(alpha = 0.24f) else CosmicDark)
                            .padding(horizontal = 6.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "${tab.count}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = contentColor,
                        )
                    }
                }
            }
        }
    }
}