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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.schoolos.android.core.designsystem.CosmicBlack
import com.schoolos.android.core.designsystem.CosmicDark
import com.schoolos.android.core.designsystem.CosmicNavy
import com.schoolos.android.core.designsystem.ErrorState
import com.schoolos.android.core.designsystem.GlassBorder
import com.schoolos.android.core.designsystem.GlassOverlay
import com.schoolos.android.core.designsystem.LoadingState
import com.schoolos.android.core.designsystem.NeonError
import com.schoolos.android.core.designsystem.NeonSuccess
import com.schoolos.android.core.designsystem.PullRefreshContainer
import com.schoolos.android.core.designsystem.StudentNeon
import com.schoolos.android.core.designsystem.TeacherNeon
import com.schoolos.android.core.designsystem.TextPrimary
import com.schoolos.android.core.designsystem.TextSecondary
import com.schoolos.android.core.designsystem.TextTertiary
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
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 36.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // ── 1. MODERN TOP BAR ─────────────────────────────────────────
                    item {
                        ScreenTopNavigation(
                            title = if (isTeacher) "Agenda Mengajar" else "Jadwal Pelajaran",
                            isViewingToday = isViewingToday,
                            onBack = onBack,
                            onJumpToToday = { viewModel.onDateSelected(LocalDate.now()) },
                            accentColor = accentColor,
                        )
                    }

                    // ── 2. EXECUTIVE HERO SUMMARY CARD ────────────────────────────
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

                    // ── 3. INTERACTIVE WEEKLY CALENDAR STRIP ─────────────────────
                    item {
                        WeeklyCalendarStrip(
                            days = days,
                            selectedDate = state.selectedDate,
                            scheduleCounts = state.weekScheduleCounts,
                            accentColor = accentColor,
                            onDaySelected = { date -> viewModel.onDateSelected(date) }
                        )
                    }

                    // ── 4. SMART STATUS FILTER CHIPS ─────────────────────────────
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

                    // ── 5. TIMELINE SECTION HEADER ───────────────────────────────
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 2.dp, vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Daftar Sesi Pembelajaran",
                                fontSize = 15.sp,
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

                    // ── 6. SESSIONS CONTENT LIST (VERTICAL TIMELINE) ──────────────
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
                IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Kembali",
                        tint = TextPrimary,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                color = TextPrimary,
                letterSpacing = (-0.5).sp,
            )
        }

        if (!isViewingToday) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(accentColor.copy(alpha = 0.15f))
                    .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                    .clickable(onClick = onJumpToToday)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Today,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(14.dp)
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
                Color(0xFF064E3B),
                Color(0xFF047857),
                Color(0xFF0F766E),
            )
        )
    } else {
        Brush.linearGradient(
            listOf(
                Color(0xFF1E1B4B),
                Color(0xFF3730A3),
                Color(0xFF6366F1),
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
            animation = tween(600),
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
                spotColor = accentColor.copy(alpha = 0.40f)
            )
            .clip(RoundedCornerShape(24.dp))
            .background(bannerBrush)
            .border(1.dp, Color.White.copy(alpha = 0.20f), RoundedCornerShape(24.dp))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Top Tag Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Class or Date Pill
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.Black.copy(alpha = 0.25f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(12.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = fullDateTitle,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                // Live or Status Badge
                if (hasLiveSession) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(NeonSuccess.copy(alpha = 0.25f))
                            .border(1.dp, NeonSuccess.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(NeonSuccess.copy(alpha = pulseAlpha))
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(
                                text = "SESI AKTIF",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.16f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (className.isNotBlank()) "Kelas $className" else "$totalCount Sesi",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // Main Stats Row
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

            Spacer(Modifier.height(14.dp))

            // Completion Progress Bar
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Progres Sesi Hari Ini",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.82f),
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = if (totalCount > 0) "$completedCount dari $totalCount (${(progress * 100).toInt()}%)" else "0 Sesi",
                        fontSize = 11.sp,
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                Spacer(Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.28f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedProgress)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(4.dp))
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
            .clip(RoundedCornerShape(14.dp))
            .background(Color.Black.copy(alpha = 0.22f))
            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
            .padding(vertical = 10.dp, horizontal = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(emoji, fontSize = 14.sp)
        Spacer(Modifier.height(3.dp))
        Text(
            text = value,
            color = valueColor,
            fontSize = 19.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = (-0.5).sp,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = title,
            color = Color.White.copy(alpha = 0.82f),
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
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
                isSelected -> Brush.verticalGradient(listOf(accentColor, accentColor.copy(alpha = 0.80f)))
                isToday -> Brush.verticalGradient(listOf(accentColor.copy(alpha = 0.18f), accentColor.copy(alpha = 0.06f)))
                else -> Brush.verticalGradient(listOf(CosmicNavy, CosmicNavy))
            }

            val borderColor = when {
                isSelected -> Color.White.copy(alpha = 0.5f)
                isToday -> accentColor.copy(alpha = 0.55f)
                else -> GlassBorder
            }

            Column(
                modifier = Modifier
                    .width(56.dp)
                    .then(
                        if (isSelected) Modifier.shadow(8.dp, RoundedCornerShape(18.dp), spotColor = accentColor.copy(alpha = 0.60f))
                        else Modifier
                    )
                    .clip(RoundedCornerShape(18.dp))
                    .background(cardBg)
                    .border(1.dp, borderColor, RoundedCornerShape(18.dp))
                    .clickable { onDaySelected(day.localDate) }
                    .padding(vertical = 12.dp, horizontal = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = day.dayName,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = when {
                        isSelected -> Color.White
                        isToday -> accentColor
                        else -> TextTertiary
                    },
                    letterSpacing = 0.8.sp
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = day.dateNum,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = when {
                        isSelected -> Color.White
                        isToday -> accentColor
                        else -> TextPrimary
                    },
                    letterSpacing = (-0.5).sp,
                )

                Spacer(Modifier.height(5.dp))

                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .width(16.dp)
                            .height(3.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                } else if (classCount > 0) {
                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        repeat(minOf(classCount, 3)) {
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(if (isToday) accentColor else Color(0xFF38BDF8))
                            )
                        }
                    }
                } else {
                    Spacer(Modifier.height(5.dp))
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
                    .then(
                        if (isSelected) Modifier.shadow(5.dp, RoundedCornerShape(12.dp), spotColor = accentColor.copy(alpha = 0.35f))
                        else Modifier
                    )
                    .clip(RoundedCornerShape(12.dp))
                    .background(bg)
                    .border(1.dp, border, RoundedCornerShape(12.dp))
                    .clickable { onFilterSelected(tab.id) }
                    .padding(horizontal = 13.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (tab.dotColor != null) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) Color.White else tab.dotColor)
                        )
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(
                        text = tab.label,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                        color = contentColor,
                    )
                    Spacer(Modifier.width(7.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) Color.White.copy(alpha = 0.26f) else CosmicDark)
                            .padding(horizontal = 7.dp, vertical = 2.dp)
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