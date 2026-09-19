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
import androidx.compose.foundation.layout.defaultMinSize
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
import com.schoolos.android.core.designsystem.LocalIsDarkTheme
import com.schoolos.android.core.designsystem.NeonBlue
import com.schoolos.android.core.designsystem.NeonBlueDark
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

    // Generate weekly calendar days (Monday to Saturday, or including Sunday if viewed on Sunday)
    val days = remember {
        val today = LocalDate.now()
        val isSunday = today.dayOfWeek == java.time.DayOfWeek.SUNDAY
        val startOfWeek = if (isSunday) today.minusDays(6) else today.with(java.time.DayOfWeek.MONDAY)
        val count = if (isSunday) 7 else 6
        (0 until count).map { i ->
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
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 36.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    // ── 1. QUIET TOP BAR ─────────────────────────────────────────
                    item {
                        ScreenTopNavigation(
                            title = if (isTeacher) "Agenda Mengajar" else "Jadwal Pelajaran",
                            subtitle = if (state.className.isNotBlank()) "Kelas ${state.className}" else null,
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
    subtitle: String? = null,
    isViewingToday: Boolean,
    onBack: (() -> Unit)?,
    onJumpToToday: () -> Unit,
    accentColor: Color,
) {
    com.schoolos.android.core.designsystem.ExecutiveTopBar(
        title = title,
        subtitle = subtitle,
        onBack = onBack,
        actions = {
            if (!isViewingToday) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(accentColor.copy(alpha = 0.15f))
                        .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                        .clickable(onClick = onJumpToToday)
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Today,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(Modifier.width(4.dp))
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
    )
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
    val progress = if (totalCount > 0) completedCount.toFloat() / totalCount.toFloat() else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "progress"
    )

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
                    text = fullDateTitle,
                    color = TextTertiary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Normal,
                )

                // Live or Status Badge
                if (hasLiveSession) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(NeonSuccess)
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = "SESI AKTIF",
                            color = NeonSuccess,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                } else if (className.isNotBlank()) {
                    Text(
                        text = "Kelas $className",
                        color = TextTertiary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Normal,
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Main Stats Row (Quiet Monochrome)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatMiniCard(
                    title = "Total Sesi",
                    value = "$totalCount",
                    modifier = Modifier.weight(1f)
                )
                StatMiniCard(
                    title = "Aktif / Nanti",
                    value = "${activeCount + upcomingCount}",
                    modifier = Modifier.weight(1f)
                )
                StatMiniCard(
                    title = "Selesai",
                    value = "$completedCount",
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
                        color = TextTertiary,
                        fontWeight = FontWeight.Normal
                    )
                    Text(
                        text = if (totalCount > 0) "$completedCount/$totalCount (${(progress * 100).toInt()}%)" else "0 Sesi",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(Modifier.height(6.dp))
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
            fontSize = 16.sp,
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

// ── INTERACTIVE WEEKLY CALENDAR STRIP (Apple Calendar Minimalist) ────────────

@Composable
private fun WeeklyCalendarStrip(
    days: List<DayTab>,
    selectedDate: LocalDate,
    scheduleCounts: Map<LocalDate, Int>,
    accentColor: Color,
    onDaySelected: (LocalDate) -> Unit,
) {
    val isDark = LocalIsDarkTheme.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        days.forEach { day ->
            val isSelected = day.localDate == selectedDate
            val isToday = day.isToday
            val classCount = scheduleCounts[day.localDate] ?: 0

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onDaySelected(day.localDate) }
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = day.dayName.uppercase(),
                        fontSize = 10.sp,
                        fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Medium,
                        letterSpacing = 0.5.sp,
                        color = when {
                            isToday -> NeonBlueDark
                            isSelected -> TextPrimary
                            else -> TextTertiary
                        },
                    )

                    Spacer(Modifier.height(6.dp))

                    // Bungkus angka tanggalnya saja dengan padding kanan-kiri proporsional & kontras tinggi
                    Box(
                        modifier = Modifier
                            .defaultMinSize(minWidth = 36.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(
                                when {
                                    isToday && isSelected -> NeonBlueDark
                                    isToday && !isSelected -> NeonBlueDark.copy(alpha = 0.14f)
                                    isSelected -> if (isDark) Color(0xFF27272A) else Color(0xFF0F172A)
                                    else -> if (isDark) CosmicNavy else Color.White
                                }
                            )
                            .border(
                                width = when {
                                    isToday && isSelected -> 0.dp
                                    isToday && !isSelected -> 1.5.dp
                                    isSelected -> 0.dp
                                    else -> 0.8.dp
                                },
                                color = when {
                                    isToday && isSelected -> Color.Transparent
                                    isToday && !isSelected -> NeonBlueDark
                                    isSelected -> Color.Transparent
                                    else -> GlassBorder
                                },
                                shape = RoundedCornerShape(9.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = day.dateNum,
                            fontSize = 14.sp,
                            fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = when {
                                isToday && isSelected -> Color.White
                                isToday && !isSelected -> NeonBlueDark
                                isSelected -> Color.White
                                else -> TextSecondary
                            },
                        )
                    }

                    Spacer(Modifier.height(5.dp))

                    if (classCount > 0) {
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isToday -> NeonBlueDark
                                        isSelected -> TextPrimary
                                        else -> TextTertiary
                                    }
                                )
                        )
                    } else {
                        Spacer(Modifier.height(4.dp))
                    }
                }
            }
        }
    }
}

// ── SMART STATUS FILTER CHIPS (Apple Segmented Style) ─────────────────────────

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
        TabItem("UPCOMING", "Mendatang", upcomingCount, NeonBlue),
        TabItem("COMPLETED", "Selesai", completedCount, TextTertiary),
    )

    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        itemsIndexed(tabs) { _, tab ->
            val isSelected = selectedFilter == tab.id

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) com.schoolos.android.core.designsystem.CosmicSurface2 else CosmicNavy)
                    .border(
                        0.5.dp,
                        if (isSelected) com.schoolos.android.core.designsystem.GlassBorder2 else GlassBorder,
                        RoundedCornerShape(8.dp)
                    )
                    .clickable { onFilterSelected(tab.id) }
                    .padding(horizontal = 12.dp, vertical = 7.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (tab.dotColor != null) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(tab.dotColor)
                        )
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(
                        text = tab.label,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isSelected) TextPrimary else TextTertiary,
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
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isSelected) TextPrimary else TextTertiary,
                            )
                        }
                    }
                }
            }
        }
    }
}