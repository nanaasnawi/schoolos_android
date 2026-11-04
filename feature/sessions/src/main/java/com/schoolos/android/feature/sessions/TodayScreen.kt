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
import com.schoolos.android.core.designsystem.LocalIsDarkTheme
import com.schoolos.android.core.designsystem.NeonBlue
import com.schoolos.android.core.designsystem.NeonBlueDark
import com.schoolos.android.core.designsystem.NeonError
import com.schoolos.android.core.designsystem.NeonSuccess
import com.schoolos.android.core.designsystem.PullRefreshContainer
import com.schoolos.android.core.designsystem.ShimmerBox
import com.schoolos.android.core.designsystem.StudentNeon
import com.schoolos.android.core.designsystem.TeacherNeon
import com.schoolos.android.core.designsystem.TextPrimary
import com.schoolos.android.core.designsystem.TextSecondary
import com.schoolos.android.core.designsystem.TextTertiary
import com.schoolos.android.core.auth.isPrincipalRole
import com.schoolos.android.core.auth.isTeacherRole
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

    val isPrincipal = isPrincipalRole(state.userRole)
    val isTeacher = isTeacherRole(state.userRole) || isPrincipal
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
        onRefresh = { viewModel.refresh(isPullRefresh = true) },
        modifier = Modifier.fillMaxSize(),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (state.isLoading) {
                TodayScreenShimmer()
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
                            title = if (isPrincipal) "Monitoring Agenda Sekolah" else if (isTeacher) "Agenda Mengajar" else "Jadwal Pelajaran",
                            subtitle = if (isPrincipal) "Pengawasan sesi & tatap muka seluruh kelas" else if (isTeacher) "Jadwal tatap muka & agenda mengajar" else if (state.className.isNotBlank()) "Kelas ${state.className}" else null,
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (onBack != null) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Kembali",
                        tint = TextPrimary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            Column(verticalArrangement = Arrangement.Center) {
                Text(
                    text = title,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        if (!isViewingToday) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(accentColor.copy(alpha = 0.15f))
                    .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
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
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF111113))
            .border(1.dp, Color(0xFF1F1F23), RoundedCornerShape(20.dp))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Top Tag Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF161618))
                            .border(1.dp, Color(0xFF27272A), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    Text(
                        text = fullDateTitle,
                        color = TextSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                // Live or Status Badge
                if (hasLiveSession) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(NeonSuccess.copy(alpha = 0.15f))
                            .border(1.dp, NeonSuccess.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
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
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                } else if (className.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF161618))
                            .border(1.dp, Color(0xFF27272A), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Kelas $className",
                            color = TextTertiary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // Main Stats Row (Obsidian Cards)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
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

            Spacer(Modifier.height(14.dp))

            // Completion Progress Bar
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Progres Hari Ini",
                        fontSize = 12.sp,
                        color = TextTertiary,
                        fontWeight = FontWeight.Normal
                    )
                    Text(
                        text = if (totalCount > 0) "$completedCount/$totalCount (${(progress * 100).toInt()}%)" else "0 Sesi",
                        fontSize = 12.sp,
                        color = if (progress >= 1f && totalCount > 0) NeonSuccess else TextSecondary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color(0xFF27272A))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedProgress)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(3.dp))
                            .background(if (progress >= 1f && totalCount > 0) NeonSuccess else NeonBlue)
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
            .height(66.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF161618))
            .border(1.dp, Color(0xFF27272A), RoundedCornerShape(14.dp))
            .padding(vertical = 8.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = value,
            color = Color.White,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = title,
            color = TextTertiary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// ── INTERACTIVE WEEKLY CALENDAR STRIP (Obsidian Style) ────────────

@Composable
private fun WeeklyCalendarStrip(
    days: List<DayTab>,
    selectedDate: LocalDate,
    scheduleCounts: Map<LocalDate, Int>,
    accentColor: Color,
    onDaySelected: (LocalDate) -> Unit,
) {
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
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onDaySelected(day.localDate) }
                    .padding(vertical = 2.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = day.dayName.uppercase(),
                        fontSize = 11.sp,
                        fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Medium,
                        letterSpacing = 0.5.sp,
                        color = when {
                            isSelected -> Color.White
                            isToday -> Color(0xFF60A5FA)
                            else -> TextTertiary
                        },
                    )

                    Spacer(Modifier.height(6.dp))

                    // Date number box (Obsidian style)
                    Box(
                        modifier = Modifier
                            .defaultMinSize(minWidth = 38.dp)
                            .height(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                when {
                                    isSelected -> Color(0xFF2563EB)
                                    isToday -> Color(0xFF2563EB).copy(alpha = 0.18f)
                                    else -> Color(0xFF161618)
                                }
                            )
                            .border(
                                width = 1.dp,
                                color = when {
                                    isSelected -> Color(0xFF3B82F6)
                                    isToday -> Color(0xFF2563EB).copy(alpha = 0.6f)
                                    else -> Color(0xFF27272A)
                                },
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = day.dateNum,
                            fontSize = 15.sp,
                            fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.SemiBold,
                            color = when {
                                isSelected -> Color.White
                                isToday -> Color(0xFF93C5FD)
                                else -> TextSecondary
                            },
                        )
                    }

                    Spacer(Modifier.height(5.dp))

                    if (classCount > 0) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isSelected -> Color(0xFF60A5FA)
                                        isToday -> NeonSuccess
                                        else -> Color(0xFF71717A)
                                    }
                                )
                        )
                    } else {
                        Spacer(Modifier.height(5.dp))
                    }
                }
            }
        }
    }
}

// ── SMART STATUS FILTER CHIPS (Obsidian Segmented Style) ─────────────────────────

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
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        itemsIndexed(tabs) { _, tab ->
            val isSelected = selectedFilter == tab.id

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSelected) Color(0xFF27272A) else Color(0xFF161618))
                    .border(
                        1.dp,
                        if (isSelected) Color(0xFF3F3F46) else Color(0xFF1F1F23),
                        RoundedCornerShape(12.dp)
                    )
                    .clickable { onFilterSelected(tab.id) }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
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
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (isSelected) Color.White else TextTertiary,
                    )
                    if (tab.count > 0) {
                        Spacer(Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) Color(0xFF3F3F46) else Color(0xFF222226))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${tab.count}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isSelected) Color.White else TextSecondary,
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── SKELETON SHIMMER FOR JADWAL PELAJARAN ─────────────────────────────────────
// Mirrors the real layout (top bar → hero banner → calendar strip → filter chips →
// timeline cards) so content swaps in without layout jump. Uses the design-system
// ShimmerBox, whose highlight is a single screen-space sweep across all placeholders.

@Composable
private fun TodayScreenShimmer() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
            .padding(top = 6.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ── 1. TOP BAR ───────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ShimmerBox(Modifier.width(150.dp).height(18.dp), RoundedCornerShape(5.dp))
                ShimmerBox(Modifier.width(90.dp).height(12.dp), RoundedCornerShape(4.dp))
            }
        }

        // ── 2. HERO BANNER ───────────────────────────────────────────
        SkeletonSurface(cornerRadius = 12.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ShimmerBox(Modifier.width(150.dp).height(11.dp), RoundedCornerShape(4.dp))
                    ShimmerBox(Modifier.width(64.dp).height(11.dp), RoundedCornerShape(4.dp))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    repeat(3) {
                        ShimmerBox(
                            Modifier.weight(1f).height(58.dp),
                            RoundedCornerShape(8.dp)
                        )
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    ShimmerBox(Modifier.width(90.dp).height(10.dp), RoundedCornerShape(3.dp))
                    ShimmerBox(Modifier.width(64.dp).height(10.dp), RoundedCornerShape(3.dp))
                }
                ShimmerBox(Modifier.fillMaxWidth().height(4.dp), RoundedCornerShape(2.dp))
            }
        }

        // ── 3. WEEKLY CALENDAR STRIP ────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(6) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    ShimmerBox(Modifier.width(24.dp).height(9.dp), RoundedCornerShape(3.dp))
                    ShimmerBox(Modifier.width(36.dp).height(32.dp), RoundedCornerShape(9.dp))
                    Spacer(Modifier.height(4.dp))
                }
            }
        }

        // ── 4. FILTER CHIPS ─────────────────────────────────────────
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(64.dp, 96.dp, 88.dp, 70.dp).forEach { w ->
                ShimmerBox(Modifier.width(w).height(30.dp), RoundedCornerShape(8.dp))
            }
        }

        // ── 5. SECTION HEADER ───────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ShimmerBox(Modifier.width(170.dp).height(15.dp), RoundedCornerShape(4.dp))
            ShimmerBox(Modifier.width(96.dp).height(20.dp), RoundedCornerShape(8.dp))
        }

        // ── 6. TIMELINE SESSION CARDS ───────────────────────────────
        repeat(3) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // Time rail (start hour · status node · end hour)
                Column(
                    modifier = Modifier.width(48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ShimmerBox(Modifier.width(34.dp).height(12.dp), RoundedCornerShape(3.dp))
                    ShimmerBox(Modifier.size(16.dp), CircleShape)
                    ShimmerBox(Modifier.width(28.dp).height(10.dp), RoundedCornerShape(3.dp))
                }
                Spacer(Modifier.width(8.dp))
                SkeletonSurface(cornerRadius = 12.dp, modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ShimmerBox(Modifier.size(40.dp), RoundedCornerShape(10.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            ShimmerBox(Modifier.fillMaxWidth(0.75f).height(13.dp), RoundedCornerShape(4.dp))
                            ShimmerBox(Modifier.fillMaxWidth(0.5f).height(10.dp), RoundedCornerShape(3.dp))
                            ShimmerBox(Modifier.fillMaxWidth(0.35f).height(10.dp), RoundedCornerShape(3.dp))
                        }
                        Spacer(Modifier.width(10.dp))
                        ShimmerBox(Modifier.width(52.dp).height(20.dp), RoundedCornerShape(6.dp))
                    }
                }
            }
        }
    }
}

/** Static card container used behind skeleton placeholders (matches real card chrome). */
@Composable
private fun SkeletonSurface(
    cornerRadius: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(cornerRadius))
            .background(CosmicNavy)
            .border(0.5.dp, GlassBorder, RoundedCornerShape(cornerRadius))
            .padding(14.dp)
    ) { content() }
}
