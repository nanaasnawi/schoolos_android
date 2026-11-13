package com.schoolos.android.feature.home

import com.schoolos.android.core.R as CoreR
import androidx.compose.ui.res.painterResource
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import coil.request.CachePolicy
import coil.request.ImageRequest
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.schoolos.android.core.designsystem.BookReaderDialog
import com.schoolos.android.core.designsystem.CosmicBlack
import com.schoolos.android.core.designsystem.CosmicNavy
import com.schoolos.android.core.designsystem.CosmicSurface2
import com.schoolos.android.domain.model.BookReadingItem
import com.schoolos.android.core.designsystem.GlassBorder
import com.schoolos.android.core.designsystem.HomeShimmerSkeleton
import com.schoolos.android.core.designsystem.LoadingState
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
    onNavigateToAssignmentDetail: (String) -> Unit = {},
    onNavigateToQuizzes: () -> Unit = {},
    onNavigateToQuizDetail: (String) -> Unit = {},
    onNavigateToGrades: () -> Unit = {},
    onNavigateToProgress: () -> Unit = {},
    onNavigateToAchievements: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToLearning: () -> Unit = {},
    onNavigateToMaterialDetail: (String) -> Unit = {},
    onNavigateToAssignmentCreator: () -> Unit = {},
    onNavigateToQuizBuilder: () -> Unit = {},
    onNavigateToBroadcastCenter: () -> Unit = {},
    onNavigateToAssignmentsWithSubject: (String) -> Unit = {},
    onNavigateToQuizzesWithSubject: (String) -> Unit = {},
    onNavigateToLearningWithSubject: (String) -> Unit = {},
    onNavigateToRombelStudents: (String) -> Unit = {},
    onNavigateToAiChat: () -> Unit = {},
    onNavigateToDailyQuest: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var activeReadingBook by remember { mutableStateOf<BookReadingItem?>(null) }

    val isPrincipal = com.schoolos.android.core.auth.isPrincipalRole(state.userRole)
    val isParent    = com.schoolos.android.core.auth.isParentRole(state.userRole)
    val isTeacher   = com.schoolos.android.core.auth.isTeacherRole(state.userRole) && !isPrincipal

    val isPkbm      = com.schoolos.android.core.auth.isPkbmInstitution(state.schoolName)
    val userName = if (state.userName.isNotBlank()) state.userName
                   else when {
                       isPrincipal -> "Kepala Sekolah"
                       isTeacher   -> if (isPkbm) "Tutor / Pamong Belajar" else "Guru"
                       isParent    -> if (isPkbm) "Wali Warga Belajar" else "Wali Murid"
                       else        -> if (isPkbm) "Warga Belajar" else "Siswa"
                   }

    val roleAccent = when {
        isPrincipal -> Color(0xFF8B5CF6)
        isTeacher   -> TeacherNeon
        isParent    -> ParentNeon
        else        -> StudentNeon
    }

    val avatarIcon = when {
        isTeacher -> Icons.Default.Person
        isParent  -> Icons.Default.Face
        else      -> Icons.Default.School
    }

    val roleLabel = com.schoolos.android.core.auth.resolveRoleDisplay(
        role = state.userRole,
        schoolName = state.schoolName,
        isFormalUpper = true
    )

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshUnreadCount()
                viewModel.refresh(isPullRefresh = false)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.refresh(isPullRefresh = false)
    }

    val isDark = com.schoolos.android.core.designsystem.LocalIsDarkTheme.current

    Scaffold(
        containerColor = CosmicBlack,
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            Image(
                painter = painterResource(id = CoreR.drawable.mascot_akselerasi_edu),
                contentDescription = if (isPrincipal) "AI Analitik Kepala Sekolah" else "Tanya Guru AI",
                modifier = Modifier
                    .offset(y = 10.dp)
                    .size(70.dp)
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null,
                        onClick = onNavigateToAiChat,
                    ),
                contentScale = androidx.compose.ui.layout.ContentScale.Fit,
            )
        }
    ) { padding ->
        PullRefreshContainer(
            isRefreshing = state.isRefreshing,
            onRefresh = { viewModel.refresh(isPullRefresh = true) },
            includeStatusBarPadding = true,
            modifier = Modifier.fillMaxSize(),
        ) {
            if (state.isLoading) {
                HomeShimmerSkeleton()
            } else {
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
                verticalArrangement = Arrangement.spacedBy(10.dp),
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
                                    text = if (state.schoolName.isNotBlank()) state.schoolName else "Akselerasi-Edu",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    letterSpacing = (-0.2).sp,
                                    lineHeight = 16.sp,
                                )
                                Spacer(Modifier.height(0.dp))
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
                                modifier = Modifier.size(42.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                        .background(CosmicNavy)
                                        .border(1.dp, GlassBorder, CircleShape)
                                        .clickable(onClick = onNavigateToNotifications),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        painter = painterResource(id = CoreR.drawable.ic_modern_bell),
                                        contentDescription = "Notifikasi",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }

                                if (state.unreadCount > 0) {
                                    val badgeText = if (state.unreadCount > 99) "99+" else "${state.unreadCount}"
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .offset(x = 3.dp, y = (-2).dp)
                                            .defaultMinSize(minWidth = 18.dp, minHeight = 18.dp)
                                            .clip(CircleShape)
                                            .background(NeonError)
                                            .border(1.5.dp, CosmicBlack, CircleShape)
                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                            .clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = null,
                                                onClick = onNavigateToNotifications,
                                            ),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            text = badgeText,
                                            color = Color.White,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center,
                                            lineHeight = 11.sp,
                                        )
                                    }
                                }
                            }

                            val fullAvatarUrl = if (!state.userAvatarUrl.isNullOrBlank()) {
                                val raw = state.userAvatarUrl!!
                                if (raw.startsWith("http://", ignoreCase = true) || raw.startsWith("https://", ignoreCase = true) || raw.startsWith("data:", ignoreCase = true)) {
                                    raw
                                } else {
                                    val host = com.schoolos.android.core.common.BuildConfig.API_BASE_URL.substringBefore("/api/").trimEnd('/')
                                    val path = if (raw.startsWith("/")) raw else "/$raw"
                                    "$host$path"
                                }
                            } else null

                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(CosmicNavy)
                                    .border(1.dp, roleAccent.copy(alpha = 0.4f), CircleShape)
                                    .clickable(onClick = onNavigateToProfile),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (!fullAvatarUrl.isNullOrBlank()) {
                                    val context = LocalContext.current
                                    val imageRequest = remember(fullAvatarUrl) {
                                        ImageRequest.Builder(context)
                                            .data(fullAvatarUrl)
                                            .diskCachePolicy(CachePolicy.ENABLED)
                                            .memoryCachePolicy(CachePolicy.ENABLED)
                                            .crossfade(true)
                                            .build()
                                    }
                                    coil.compose.AsyncImage(
                                        model = imageRequest,
                                        contentDescription = "Profil",
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape),
                                    )
                                } else {
                                    Icon(
                                        painter = painterResource(id = CoreR.drawable.ic_modern_profile),
                                        contentDescription = "Profil",
                                        tint = roleAccent,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            }
                        }
                    }
                }

                // ── 3. ROLE-BASED DYNAMIC CONTENT ────────────────────────────────
                when {
                    isPrincipal -> {
                        principalContent(
                            onNavigateToSessions        = onNavigateToSessions,
                            onNavigateToSessionDetail   = onNavigateToSessionDetail,
                            onNavigateToAssignments     = onNavigateToAssignments,
                            onNavigateToQuizzes         = onNavigateToQuizzes,
                            onNavigateToGrades          = onNavigateToGrades,
                            onNavigateToNotifications   = onNavigateToNotifications,
                            onNavigateToBroadcastCenter = onNavigateToBroadcastCenter,
                            onNavigateToLearning        = onNavigateToLearning,
                            onNavigateToRombelStudents  = onNavigateToRombelStudents,
                            onNavigateToAiChat          = onNavigateToAiChat,
                            principalName               = userName,
                            schoolClasses               = state.teacherClasses,
                            totalStudentsCount          = state.totalStudentsCount,
                            todaySessions               = state.todaySessions,
                            teacherAssignments          = state.teacherAssignments,
                            teacherAnnouncements        = state.teacherAnnouncements,
                            pendingAssignmentsCount     = state.teacherPendingCount,
                            materialsCount              = state.teacherMaterialsCount,
                            scheduleCount               = state.teacherScheduleCount,
                            quizzesCount                = state.teacherQuizzesCount,
                            schoolHealthMetrics         = state.schoolHealthMetrics,
                            schoolName                  = state.schoolName,
                        )
                    }
                    isTeacher -> {
                        val isHomeroom = state.homeroomClass.isNotBlank() && !isUuid(state.homeroomClass)
                        val teacherClass = if (isHomeroom) state.homeroomClass else state.activeSessionClass
                        teacherContent(
                            teacherName              = userName,
                            onNavigateToSessions     = onNavigateToSessions,
                            onNavigateToSessionDetail = onNavigateToSessionDetail,
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
                            quizzesCount             = state.teacherQuizzesCount,
                            urgentTeachingSession    = state.urgentTeachingSession,
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
                        activities               = state.parentActivities,
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
                        onNavigateToAssignmentDetail = onNavigateToAssignmentDetail,
                        onNavigateToQuizzes      = onNavigateToQuizzes,
                        onNavigateToQuizDetail   = onNavigateToQuizDetail,
                        onNavigateToGrades       = onNavigateToGrades,
                        onNavigateToProgress     = onNavigateToProgress,
                        onNavigateToAchievements = onNavigateToAchievements,
                        onNavigateToLearning     = onNavigateToLearning,
                        onNavigateToDailyQuest   = onNavigateToDailyQuest,
                        onNavigateToMaterialDetail = onNavigateToMaterialDetail,
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
                        xpCount                  = state.xpCount,
                        dailyQuestSummary        = state.dailyQuestSummary,
                        assignmentsCount         = state.assignmentsCount,
                        materialsCount           = state.teacherMaterialsCount,
                        weeklyActivity           = state.weeklyActivity,
                        studentName              = state.userName,
                        studentClass             = state.homeroomClass,
                        studentNisn              = state.userIdentifier,
                        streakDays               = state.streakDays,
                        studentAssignments       = state.studentAssignments,
                    )
                }
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
    return "$greeting, $name!"
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
