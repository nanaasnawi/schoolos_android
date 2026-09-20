package com.schoolos.android.feature.home

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.schoolos.android.core.designsystem.BookReaderDialog
import com.schoolos.android.core.designsystem.CosmicBlack
import com.schoolos.android.core.designsystem.CosmicNavy
import com.schoolos.android.domain.model.BookReadingItem
import com.schoolos.android.core.designsystem.GlassBorder
import com.schoolos.android.core.designsystem.NeonError
import com.schoolos.android.core.designsystem.NeonSuccess
import com.schoolos.android.core.designsystem.ParentNeon
import com.schoolos.android.core.designsystem.PullRefreshContainer
import com.schoolos.android.core.designsystem.StudentNeon
import com.schoolos.android.core.designsystem.TeacherNeon
import com.schoolos.android.core.designsystem.TextPrimary
import com.schoolos.android.core.designsystem.TextSecondary
import com.schoolos.android.core.designsystem.TextTertiary

@Composable
fun HomeScreen(
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToSessions: () -> Unit = {},
    onNavigateToSessionDetail: (String) -> Unit = {},
    onNavigateToAssignments: () -> Unit = {},
    onNavigateToQuizzes: () -> Unit = {},
    onNavigateToGrades: () -> Unit = {},
    onNavigateToProgress: () -> Unit = {},
    onNavigateToAchievements: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToLearning: () -> Unit = {},
    onNavigateToAssignmentCreator: () -> Unit = {},
    onNavigateToQuizBuilder: () -> Unit = {},
    onNavigateToBroadcastCenter: () -> Unit = {},
    onNavigateToAssignmentsWithSubject: (String) -> Unit = {},
    onNavigateToQuizzesWithSubject: (String) -> Unit = {},
    onNavigateToLearningWithSubject: (String) -> Unit = {},
    onNavigateToRombelStudents: (String) -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    var activeReadingBook by remember { mutableStateOf<BookReadingItem?>(null) }
    val isParent  = com.schoolos.android.core.auth.isParentRole(state.userRole)
    val isTeacher = com.schoolos.android.core.auth.isTeacherRole(state.userRole)

    val userName = if (state.userName.isNotBlank()) state.userName
                   else when {
                       isTeacher -> "Bapak / Ibu Guru"
                       isParent  -> "Orang Tua / Wali"
                       else      -> "Siswa"
                   }

    val heroGradient = when {
        isTeacher -> listOf(Color(0xFF064E3B), Color(0xFF047857), Color(0xFF0F766E))
        isParent  -> listOf(Color(0xFF881337), Color(0xFFBE123C), Color(0xFFE11D48))
        else      -> listOf(Color(0xFF1E1B4B), Color(0xFF3730A3), Color(0xFF6366F1))
    }

    val roleAccent = when {
        isTeacher -> TeacherNeon
        isParent  -> ParentNeon
        else      -> StudentNeon
    }

    val avatarIcon = when {
        isTeacher -> Icons.Default.Person
        isParent  -> Icons.Default.Face
        else      -> Icons.Default.School
    }

    val roleLabel = when {
        isTeacher -> "GURU"
        isParent  -> "WALI MURID"
        else      -> "SISWA"
    }

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    Scaffold(containerColor = CosmicBlack) { padding ->
        PullRefreshContainer(
            isRefreshing = state.isRefreshing,
            onRefresh = { viewModel.refresh(isPullRefresh = true) },
            modifier = Modifier.fillMaxSize(),
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 8.dp,
                    bottom = padding.calculateBottomPadding() + 16.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                // ── 1. QUIET INSTITUTIONAL TOP BAR ────────────────────────────────
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // School identity & quiet user title
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp),
                        ) {
                            com.schoolos.android.core.designsystem.DynamicSchoolLogo(
                                logoUrl = state.schoolLogoUrl,
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(10.dp)),
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Text(
                                    text = if (state.schoolName.isNotBlank()) state.schoolName else "Akselerasi Edu",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    letterSpacing = (-0.2).sp,
                                    lineHeight = 16.sp,
                                )
                                Spacer(Modifier.height(1.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text(
                                        text = userName,
                                        fontSize = 11.sp,
                                        color = TextSecondary,
                                        fontWeight = FontWeight.Normal,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        lineHeight = 14.sp,
                                        modifier = Modifier.weight(1f, fill = false),
                                    )
                                    Text(
                                        text = " • ",
                                        fontSize = 10.sp,
                                        color = TextTertiary,
                                    )
                                    Text(
                                        text = roleLabel,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = roleAccent,
                                        maxLines = 1,
                                    )
                                }
                            }
                        }

                        // Right actions: Notifications & Quick Profile Avatar
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(CosmicNavy)
                                    .border(1.dp, GlassBorder, CircleShape)
                                    .clickable(onClick = onNavigateToNotifications),
                                contentAlignment = Alignment.Center,
                            ) {
                                BadgedBox(
                                    badge = {
                                        if (state.unreadCount > 0) {
                                            Badge(
                                                containerColor = NeonError,
                                                contentColor = Color.White,
                                            ) {
                                                Text(
                                                    text = "${state.unreadCount}",
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Black,
                                                )
                                            }
                                        }
                                    },
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Notifications,
                                        contentDescription = "Notifikasi",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(22.dp),
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(CosmicNavy)
                                    .border(1.dp, roleAccent.copy(alpha = 0.4f), CircleShape)
                                    .clickable(onClick = onNavigateToProfile),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = avatarIcon,
                                    contentDescription = "Profil",
                                    tint = roleAccent,
                                    modifier = Modifier.size(22.dp),
                                )
                            }
                        }
                    }
                }

                // ── 2. STREAMLINED LIVE SPOTLIGHT HERO ───────────────────────────
                item {
                    val hasLiveSession = state.nextSessionIsLive
                    val spotlightSubject = if (state.nextSessionSubject != "-" && state.nextSessionSubject.isNotBlank() && !isUuid(state.nextSessionSubject)) state.nextSessionSubject 
                        else if (state.activeSessionSubject != "-" && state.activeSessionSubject.isNotBlank() && !isUuid(state.activeSessionSubject)) state.activeSessionSubject 
                        else if (hasLiveSession) "Sesi Pembelajaran Aktif"
                        else "Tidak Ada Sesi Aktif"

                    val spotlightRoom = when {
                        state.nextSessionRoom != "-" && state.nextSessionRoom.isNotBlank() && !isUuid(state.nextSessionRoom) ->
                            formatClassOrRoom(state.nextSessionRoom)
                        state.activeSessionClass != "-" && state.activeSessionClass.isNotBlank() && !isUuid(state.activeSessionClass) ->
                            formatClassOrRoom(state.activeSessionClass)
                        state.homeroomClass.isNotBlank() && !isUuid(state.homeroomClass) ->
                            formatClassOrRoom(state.homeroomClass)
                        else -> "Ruang Kelas"
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(CosmicNavy)
                            .border(0.5.dp, GlassBorder, RoundedCornerShape(12.dp)),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                        ) {
                            // Top Row: Date & Status
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = formatRealTimeToday(),
                                    color = TextTertiary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Normal,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )

                                if (hasLiveSession) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(NeonSuccess),
                                        )
                                        Spacer(Modifier.width(5.dp))
                                        Text(
                                            text = "SEDANG AKTIF",
                                            color = NeonSuccess,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                    }
                                } else {
                                    Text(
                                        text = if (isTeacher) "JADWAL MENGAJAR" else "AGENDA HARI INI",
                                        color = TextTertiary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        letterSpacing = 0.5.sp,
                                    )
                                }
                            }

                            Spacer(Modifier.height(10.dp))

                            // Spotlight Subject Title & Info
                            Text(
                                text = spotlightSubject,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )

                            Spacer(Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f, fill = false),
                                ) {
                                    Text(
                                        text = spotlightRoom,
                                        color = TextSecondary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Normal,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    if (state.nextSessionTime != "-") {
                                        Text(
                                            text = " • ${state.nextSessionTime}",
                                            color = TextTertiary,
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }

                                // Clean Compact CTA
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(com.schoolos.android.core.designsystem.CosmicSurface2)
                                        .border(0.5.dp, GlassBorder, RoundedCornerShape(6.dp))
                                        .clickable(onClick = onNavigateToSessions)
                                        .padding(horizontal = 10.dp, vertical = 5.dp),
                                ) {
                                    Text(
                                        text = if (hasLiveSession) "Buka Sesi" else "Jadwal",
                                        color = TextPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = null,
                                        tint = TextTertiary,
                                        modifier = Modifier.size(11.dp),
                                    )
                                }
                            }
                        }
                    }
                }

                // ── 3. ROLE-BASED DYNAMIC CONTENT ────────────────────────────────
                when {
                    isTeacher -> {
                        val isHomeroom = state.homeroomClass.isNotBlank() && !isUuid(state.homeroomClass)
                        val teacherClass = if (isHomeroom) state.homeroomClass else state.activeSessionClass
                        teacherContent(
                            onNavigateToSessions     = onNavigateToSessions,
                            onNavigateToAssignments  = onNavigateToAssignments,
                            onNavigateToQuizzes      = onNavigateToQuizzes,
                            onNavigateToGrades       = onNavigateToGrades,
                            onNavigateToNotifications= onNavigateToNotifications,
                            onNavigateToAssignmentCreator = onNavigateToAssignmentCreator,
                            onNavigateToQuizBuilder  = onNavigateToQuizBuilder,
                            onNavigateToBroadcastCenter = onNavigateToBroadcastCenter,
                            onNavigateToLearning     = onNavigateToLearning,
                            onNavigateToRombelStudents = onNavigateToRombelStudents,
                            activeSubject            = state.activeSessionSubject,
                            activeClass              = teacherClass,
                            isHomeroom               = isHomeroom,
                            teacherClasses           = state.teacherClasses,
                            teacherSubjects          = state.teacherSubjects,
                            todaySessions            = state.todaySessions,
                            teacherAssignments       = state.teacherAssignments,
                            teacherAnnouncements     = state.teacherAnnouncements,
                            pendingAssignmentsCount  = state.teacherPendingCount,
                            materialsCount           = state.teacherMaterialsCount,
                            scheduleCount            = state.teacherScheduleCount,
                            attendanceRate           = state.teacherAttendanceRate,
                        )
                    }
                    isParent -> parentContent(
                        childName                = state.childName,
                        childClass               = state.homeroomClass,
                        attendanceRate           = state.parentAttendanceRate,
                        presentDays              = state.parentPresentDays,
                        permitDays               = state.parentPermitDays,
                        absentDays               = state.parentAbsentDays,
                        assignmentsCount         = state.parentAssignmentsCount,
                        onNavigateToProgress     = onNavigateToProgress,
                        onNavigateToNotifications= onNavigateToNotifications,
                        onNavigateToAssignments  = onNavigateToAssignments,
                        onNavigateToGrades       = onNavigateToGrades,
                        onNavigateToAchievements = onNavigateToAchievements,
                    )
                    else -> studentContent(
                        onNavigateToSessions     = onNavigateToSessions,
                        onNavigateToSessionDetail = { sessionId -> onNavigateToSessionDetail(sessionId) },
                        onNavigateToAssignments  = onNavigateToAssignments,
                        onNavigateToQuizzes      = onNavigateToQuizzes,
                        onNavigateToGrades       = onNavigateToGrades,
                        onNavigateToProgress     = onNavigateToProgress,
                        onNavigateToAchievements = onNavigateToAchievements,
                        onNavigateToLearning     = onNavigateToLearning,
                        onNavigateToAssignmentWithSubject = onNavigateToAssignmentsWithSubject,
                        onNavigateToQuizWithSubject      = onNavigateToQuizzesWithSubject,
                        onNavigateToMaterialWithSubject  = onNavigateToLearningWithSubject,
                        nextSessionSubject       = state.nextSessionSubject,
                        nextSessionRoom          = state.nextSessionRoom,
                        nextSessionTime          = state.nextSessionTime,
                        nextSessionIsLive        = state.nextSessionIsLive,
                        todaySessions            = state.todaySessions,
                        gradeAverage             = state.gradeAverage,
                        gradeStatus              = state.gradeStatus,
                        gradeTrendPoints         = state.gradeTrendPoints,
                        topGradeSubjects         = state.topGradeSubjects,
                        studentProgress          = state.studentProgress,
                        progressPercentage       = state.progressPercentage,
                        activeReadingHistory     = state.activeReadingHistory,
                        recommendedSibiBook      = state.recommendedSibiBook,
                        onOpenBookReading        = { item ->
                            if (!item.materialId.isNullOrBlank() && item.fileUrl.isNullOrBlank()) {
                                onNavigateToLearning()
                            } else {
                                activeReadingBook = item
                            }
                        },
                        onStartReadingSibi       = { book ->
                            val item = viewModel.startReadingSibiBook(book)
                            activeReadingBook = item
                        },
                    )
                }
            }
        }

        if (activeReadingBook != null) {
            BookReaderDialog(
                title = activeReadingBook!!.title,
                pdfUrl = activeReadingBook!!.fileUrl ?: "",
                subject = activeReadingBook!!.subjectName ?: "Buku SIBI",
                startPage = activeReadingBook!!.startPage,
                endPage = activeReadingBook!!.endPage,
                initialPage = activeReadingBook!!.currentPage,
                onPageChanged = { newPage ->
                    viewModel.recordReadingProgress(activeReadingBook!!.copy(currentPage = newPage))
                },
                onDismiss = { activeReadingBook = null },
            )
        }
    }
}

private fun getContextualGreeting(fullName: String): String {
    val name = formatGreetingName(fullName)
    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    val greeting = when (hour) {
        in 4..10 -> "Selamat Pagi"
        in 11..14 -> "Selamat Siang"
        in 15..18 -> "Selamat Sore"
        else -> "Selamat Malam"
    }
    return "$greeting, $name! 👋"
}

private fun formatGreetingName(fullName: String): String {
    val clean = fullName.trim()
    if (clean.isEmpty()) return "Pengguna"
    val parts = clean.split("\\s+".toRegex()).filter { it.isNotBlank() }
    if (parts.isEmpty()) return clean
    val first = parts.first()
    if ((first.length <= 2 || first.endsWith(".")) && parts.size > 1) {
        return parts[1].lowercase().replaceFirstChar { it.uppercase() }
    }
    return first.lowercase().replaceFirstChar { it.uppercase() }
}

private fun formatRealTimeToday(): String {
    return try {
        val locale = java.util.Locale("id", "ID")
        val formatter = java.text.SimpleDateFormat("EEEE, d MMMM yyyy", locale)
        formatter.format(java.util.Date())
    } catch (e: Exception) {
        java.time.LocalDate.now().toString()
    }
}

private fun isUuid(str: String?): Boolean {
    if (str.isNullOrBlank()) return false
    val clean = str.trim()
    return clean.length >= 32 && clean.contains("-")
}

private fun formatClassOrRoom(raw: String?): String {
    if (raw.isNullOrBlank() || raw == "-" || isUuid(raw)) {
        return "Ruang Kelas"
    }
    val clean = raw.trim()
    return if (clean.startsWith("Kelas", ignoreCase = true) ||
        clean.startsWith("Ruang", ignoreCase = true) ||
        clean.startsWith("Lab", ignoreCase = true) ||
        clean.startsWith("Paket", ignoreCase = true)
    ) {
        clean
    } else {
        "Kelas $clean"
    }
}
