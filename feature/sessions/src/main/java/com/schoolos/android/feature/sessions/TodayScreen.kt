package com.schoolos.android.feature.sessions

import com.schoolos.android.core.R as CoreR
import androidx.compose.ui.res.painterResource
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.schoolos.android.core.designsystem.CosmicBlack
import com.schoolos.android.core.designsystem.ErrorState
import com.schoolos.android.core.designsystem.LocalIsDarkTheme
import com.schoolos.android.core.designsystem.PullRefreshContainer
import com.schoolos.android.core.designsystem.ShimmerBox
import com.schoolos.android.core.designsystem.StudentNeon
import com.schoolos.android.core.designsystem.TeacherNeon
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
    val idLocale = remember { Locale.Builder().setLanguage("id").setRegion("ID").build() }
    val isDark = LocalIsDarkTheme.current

    val textPrimary = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFFA1A1AA) else Color(0xFF64748B)
    val innerCardBg = if (isDark) Color(0xFF161618) else Color(0xFFF8FAFC)
    val innerCardBorder = if (isDark) Color(0xFF27272A) else Color(0xFFE2E8F0)

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
        modifier = Modifier
            .fillMaxSize()
            .background(CosmicBlack),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(CosmicBlack)
        ) {
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
                                color = textPrimary,
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(innerCardBg)
                                    .border(1.dp, innerCardBorder, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "${state.displayedSessions.size} Sesi Ditampilkan",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = textSecondary,
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
    val isDark = LocalIsDarkTheme.current
    val textPrimary = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFFA1A1AA) else Color(0xFF64748B)

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
                        painter = painterResource(id = CoreR.drawable.ic_modern_chevron_right),
                        contentDescription = "Kembali",
                        tint = textPrimary,
                        modifier = Modifier
                            .size(18.dp)
                            .padding(end = 2.dp)
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
                        fontWeight = FontWeight.Normal,
                        color = textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        if (!isViewingToday) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(accentColor.copy(alpha = if (isDark) 0.15f else 0.10f))
                    .border(1.dp, accentColor.copy(alpha = if (isDark) 0.35f else 0.30f), RoundedCornerShape(10.dp))
                    .clickable(onClick = onJumpToToday)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(id = CoreR.drawable.ic_modern_calendar),
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

// ── SIGNATURE EXECUTIVE HERO BANNER (MATCHES HOME STUDENT GREETING) ──────────

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
    val isDark = LocalIsDarkTheme.current
    val cardBg = if (isDark) Color(0xFF111113) else Color.White
    val cardBorder = if (isDark) Color(0xFF1F1F23) else Color(0xFFE2E8F0)
    val textPrimary = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFFA1A1AA) else Color(0xFF64748B)
    val innerCardBg = if (isDark) Color(0xFF161618) else Color(0xFFF8FAFC)
    val innerCardBorder = if (isDark) Color(0xFF27272A) else Color(0xFFE2E8F0)
    val successColor = if (isDark) Color(0xFF10B981) else Color(0xFF16A34A)
    val studentNameColor = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)

    // Separate day name and date: "Rabu, 4 November 2026" -> day="Rabu", date="4 November 2026"
    val dayPart = fullDateTitle.substringBefore(",").trim()
    val datePart = fullDateTitle.substringAfter(",").trim()

    val progress = if (totalCount > 0) completedCount.toFloat() / totalCount.toFloat() else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "progress"
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder),
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // ── TOP PILL ROW: [KELAS/ROLE] [LIVE/TOTAL SESI] ──────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Class or Role Tag
                val classLabel = if (className.isNotBlank()) "KELAS $className".uppercase() else if (isTeacher) "TUTOR / PAMONG" else "WARGA BELAJAR"
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isDark) Color(0xFF1E293B) else Color(0xFFE0F2FE),
                    border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFBAE6FD))
                ) {
                    Text(
                        text = classLabel,
                        color = studentNameColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                // Live status or session count pill
                if (hasLiveSession) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isDark) successColor.copy(alpha = 0.15f) else Color(0xFFDCFCE7),
                        border = BorderStroke(1.dp, if (isDark) successColor.copy(alpha = 0.35f) else Color(0xFF86EFAC))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(successColor)
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(
                                text = "SESI AKTIF",
                                color = if (isDark) successColor else Color(0xFF15803D),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = innerCardBg,
                        border = BorderStroke(1.dp, innerCardBorder)
                    ) {
                        Text(
                            text = "$totalCount Sesi Terjadwal",
                            color = textSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            // ── PROMINENT DATE HEADING (COLOR MATCHES STUDENT GREETING) ───────
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
                        painter = painterResource(id = CoreR.drawable.ic_modern_calendar),
                        contentDescription = null,
                        tint = studentNameColor,
                        modifier = Modifier.size(19.dp)
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(color = studentNameColor)) {
                                append(dayPart)
                            }
                            append(", ")
                            append(datePart)
                        },
                        fontSize = 15.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary,
                        letterSpacing = (-0.2).sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "Agenda pembelajaran dan sesi kelas terjadwal.",
                        fontSize = 11.sp,
                        color = textSecondary,
                        lineHeight = 14.sp
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            // ── MAIN STATS ROW (3 MINI CARDS) ─────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatMiniCard(
                    title = "Total Sesi",
                    value = "$totalCount",
                    cardBg = innerCardBg,
                    cardBorder = innerCardBorder,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    modifier = Modifier.weight(1f)
                )
                StatMiniCard(
                    title = "Aktif / Nanti",
                    value = "${activeCount + upcomingCount}",
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
            }

            // ── COMPLETION PROGRESS BAR ───────────────────────────────────────
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Progres Sesi Hari Ini",
                        fontSize = 11.5.sp,
                        color = textSecondary,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = if (totalCount > 0) "$completedCount/$totalCount (${(progress * 100).toInt()}%)" else "0 Sesi",
                        fontSize = 11.5.sp,
                        color = if (progress >= 1f && totalCount > 0) successColor else studentNameColor,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(6.dp))
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
                            .background(if (progress >= 1f && totalCount > 0) successColor else studentNameColor)
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

// ── INTERACTIVE WEEKLY CALENDAR STRIP (UNIFIED CAPSULE DESIGN) ────────────

@Composable
private fun WeeklyCalendarStrip(
    days: List<DayTab>,
    selectedDate: LocalDate,
    scheduleCounts: Map<LocalDate, Int>,
    accentColor: Color,
    onDaySelected: (LocalDate) -> Unit,
) {
    val isDark = LocalIsDarkTheme.current
    val textPrimary = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFFA1A1AA) else Color(0xFF64748B)
    val studentNameColor = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)
    val successColor = if (isDark) Color(0xFF10B981) else Color(0xFF16A34A)
    val cardBorder = if (isDark) Color(0xFF1F1F23) else Color(0xFFE2E8F0)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        days.take(6).forEach { day ->
            val isSelected = day.localDate == selectedDate
            val isToday = day.isToday
            val classCount = scheduleCounts[day.localDate] ?: 0

            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onDaySelected(day.localDate) },
                shape = RoundedCornerShape(14.dp),
                color = when {
                    isSelected -> studentNameColor
                    isToday -> if (isDark) Color(0xFF1E293B) else Color(0xFFE0F2FE)
                    else -> if (isDark) Color(0xFF161618) else Color.White
                },
                border = when {
                    isSelected -> null
                    isToday -> BorderStroke(1.dp, if (isDark) studentNameColor.copy(alpha = 0.5f) else Color(0xFFBAE6FD))
                    else -> BorderStroke(1.dp, cardBorder)
                },
                shadowElevation = 0.dp
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = day.dayName.take(3),
                        fontSize = 10.5.sp,
                        fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Medium,
                        color = when {
                            isSelected -> Color.White
                            isToday -> studentNameColor
                            else -> textSecondary
                        }
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = day.dateNum,
                        fontSize = 15.sp,
                        fontWeight = if (isSelected || isToday) FontWeight.ExtraBold else FontWeight.SemiBold,
                        color = when {
                            isSelected -> Color.White
                            isToday -> studentNameColor
                            else -> textPrimary
                        }
                    )
                    Spacer(Modifier.height(4.dp))
                    if (classCount > 0) {
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isSelected -> Color.White
                                        isToday -> successColor
                                        else -> studentNameColor
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

// ── SMART STATUS FILTER CHIPS (ADAPTIVE LIGHT / DARK) ─────────────────────────

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
    val isDark = LocalIsDarkTheme.current
    val studentNameColor = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)
    val successColor = if (isDark) Color(0xFF10B981) else Color(0xFF16A34A)
    val textSecondary = if (isDark) Color(0xFFA1A1AA) else Color(0xFF64748B)
    val textTertiary = if (isDark) Color(0xFF71717A) else Color(0xFF94A3B8)

    data class TabItem(val id: String, val label: String, val count: Int, val dotColor: Color?)

    val tabs = listOf(
        TabItem("ALL", "Semua", totalCount, null),
        TabItem("ACTIVE", "Berlangsung", activeCount, successColor),
        TabItem("UPCOMING", "Mendatang", upcomingCount, studentNameColor),
        TabItem("COMPLETED", "Selesai", completedCount, textTertiary),
    )

    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        itemsIndexed(tabs) { _, tab ->
            val isSelected = selectedFilter == tab.id
            val tabAccent = tab.dotColor ?: accentColor

            val chipBg = when {
                isSelected -> tabAccent.copy(alpha = if (isDark) 0.16f else 0.10f)
                else -> if (isDark) Color(0xFF141417) else Color.White
            }
            val chipBorder = when {
                isSelected -> tabAccent.copy(alpha = if (isDark) 0.55f else 0.45f)
                else -> if (isDark) Color(0xFF232326) else Color(0xFFE2E8F0)
            }
            val chipTextColor = when {
                isSelected -> tabAccent
                else -> textSecondary
            }
            val badgeBg = when {
                isSelected -> tabAccent.copy(alpha = if (isDark) 0.24f else 0.16f)
                else -> if (isDark) Color(0xFF1F1F23) else Color(0xFFF1F5F9)
            }
            val badgeTextColor = when {
                isSelected -> tabAccent
                else -> textSecondary
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(chipBg)
                    .border(1.dp, chipBorder, RoundedCornerShape(12.dp))
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
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = chipTextColor,
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
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = badgeTextColor,
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── SKELETON SHIMMER FOR JADWAL PELAJARAN ─────────────────────────────────────

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
        SkeletonSurface(cornerRadius = 22.dp) {
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
                            Modifier.weight(1f).height(62.dp),
                            RoundedCornerShape(14.dp)
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
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(6) {
                ShimmerBox(
                    modifier = Modifier.weight(1f).height(58.dp),
                    shape = RoundedCornerShape(14.dp)
                )
            }
        }

        // ── 4. FILTER CHIPS ─────────────────────────────────────────
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(64.dp, 96.dp, 88.dp, 70.dp).forEach { w ->
                ShimmerBox(Modifier.width(w).height(32.dp), RoundedCornerShape(12.dp))
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
                // Time rail
                Column(
                    modifier = Modifier.width(48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ShimmerBox(Modifier.width(34.dp).height(12.dp), RoundedCornerShape(3.dp))
                    ShimmerBox(Modifier.size(16.dp), CircleShape)
                    ShimmerBox(Modifier.width(28.dp).height(10.dp), RoundedCornerShape(3.dp))
                }
                Spacer(Modifier.width(10.dp))
                SkeletonSurface(cornerRadius = 22.dp, modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ShimmerBox(Modifier.size(40.dp), RoundedCornerShape(12.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            ShimmerBox(Modifier.fillMaxWidth(0.75f).height(14.dp), RoundedCornerShape(4.dp))
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
    val isDark = LocalIsDarkTheme.current
    val cardBg = if (isDark) Color(0xFF111113) else Color.White
    val cardBorder = if (isDark) Color(0xFF1F1F23) else Color(0xFFE2E8F0)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(cornerRadius)),
        shape = RoundedCornerShape(cornerRadius),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder),
        shadowElevation = 0.dp
    ) {
        Box(modifier = Modifier.padding(14.dp)) {
            content()
        }
    }
}
