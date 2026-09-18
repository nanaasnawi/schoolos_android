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
import com.schoolos.android.core.designsystem.CosmicNavy
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
                    start = 12.dp,
                    end = 12.dp,
                    top = 6.dp,
                    bottom = padding.calculateBottomPadding() + 8.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                // ── 1. MODERN TOP BAR ────────────────────────────────────────────
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // User Profile Identity
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .weight(1f)
                                .clickable(onClick = onNavigateToProfile),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .shadow(6.dp, CircleShape, spotColor = roleAccent.copy(alpha = 0.35f))
                                    .clip(CircleShape)
                                    .background(CosmicNavy)
                                    .border(2.dp, roleAccent.copy(alpha = 0.6f), CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = avatarIcon,
                                    contentDescription = "Profil",
                                    tint = roleAccent,
                                    modifier = Modifier.size(26.dp),
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = getContextualGreeting(userName),
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Black,
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    letterSpacing = (-0.3).sp,
                                )
                                Spacer(Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(roleAccent.copy(alpha = 0.15f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp),
                                    ) {
                                        Text(
                                            text = roleLabel,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black,
                                            color = roleAccent,
                                            letterSpacing = 0.5.sp,
                                        )
                                    }
                                    if (state.schoolName.isNotBlank()) {
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = state.schoolName,
                                            fontSize = 11.sp,
                                            color = TextTertiary,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }
                            }
                        }

                        // Notification Bell with Pulsing Badge
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .shadow(4.dp, CircleShape, spotColor = GlassBorder)
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
                                                fontSize = 9.sp,
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
                    }
                }

                // ── 2. DYNAMIC LIVE SPOTLIGHT HERO ───────────────────────────────
                item {
                    val hasLiveSession = state.nextSessionIsLive || (isTeacher && state.activeSessionSubject != "-" && state.activeSessionSubject.isNotBlank())
                    val spotlightSubject = if (state.nextSessionSubject != "-") state.nextSessionSubject 
                        else if (state.activeSessionSubject != "-") state.activeSessionSubject 
                        else "Tidak Ada Sesi Aktif"
                    val spotlightRoom = if (state.nextSessionRoom != "-") state.nextSessionRoom 
                        else if (state.activeSessionClass != "-") "Kelas ${state.activeSessionClass}" 
                        else "Sekolah"

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(
                                elevation = 14.dp,
                                shape = RoundedCornerShape(26.dp),
                                spotColor = heroGradient.last().copy(alpha = 0.45f),
                            )
                            .clip(RoundedCornerShape(26.dp))
                            .background(Brush.linearGradient(heroGradient))
                            .border(1.dp, Color.White.copy(alpha = 0.20f), RoundedCornerShape(26.dp)),
                    ) {
                        // Subtle curved background accents
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 24.dp, y = (-24).dp)
                                .size(140.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.08f)),
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .offset(x = (-20).dp, y = 20.dp)
                                .size(90.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.06f)),
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                        ) {
                            // Top Row: Date Pill & Live Status
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color.Black.copy(alpha = 0.22f))
                                        .padding(horizontal = 10.dp, vertical = 4.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarMonth,
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.85f),
                                        modifier = Modifier.size(12.dp),
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = formatRealTimeToday(),
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }

                                if (hasLiveSession) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(NeonSuccess.copy(alpha = 0.25f))
                                            .border(1.dp, NeonSuccess.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                                    ) {
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
                                                color = Color.White,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Black,
                                            )
                                        }
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color.White.copy(alpha = 0.15f))
                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                                    ) {
                                        Text(
                                            text = if (isTeacher) "JADWAL MENGAJAR" else "AGENDA BELAJAR",
                                            color = Color.White.copy(alpha = 0.9f),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.height(14.dp))

                            // Spotlight Subject Title & Room Info
                            Text(
                                text = if (hasLiveSession) "Sedang Berlangsung:" else "Sesi Pembelajaran:",
                                color = Color.White.copy(alpha = 0.75f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.3.sp,
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = spotlightSubject,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                letterSpacing = (-0.5).sp,
                            )

                            Spacer(Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.8f),
                                        modifier = Modifier.size(14.dp),
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = spotlightRoom,
                                        color = Color.White.copy(alpha = 0.9f),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                    )
                                    if (state.nextSessionTime != "-") {
                                        Spacer(Modifier.width(8.dp))
                                        Text("•", color = Color.White.copy(alpha = 0.5f))
                                        Spacer(Modifier.width(8.dp))
                                        Icon(
                                            imageVector = Icons.Default.Schedule,
                                            contentDescription = null,
                                            tint = Color.White.copy(alpha = 0.8f),
                                            modifier = Modifier.size(14.dp),
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            text = state.nextSessionTime,
                                            color = Color.White.copy(alpha = 0.9f),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                        )
                                    }
                                }

                                // Quick CTA
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.White)
                                        .clickable(onClick = onNavigateToSessions)
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = if (hasLiveSession) "Masuk" else "Jadwal",
                                            color = heroGradient.first(),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Black,
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                            contentDescription = null,
                                            tint = heroGradient.first(),
                                            modifier = Modifier.size(12.dp),
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.height(16.dp))

                            // ── Integrated Floating Quick Stats Bar ─────────────
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color.Black.copy(alpha = 0.22f))
                                    .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(16.dp))
                                    .padding(vertical = 10.dp, horizontal = 4.dp),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    when {
                                        isTeacher -> {
                                            HeroStatItem("JADWAL", state.teacherScheduleCount, onNavigateToSessions)
                                            HeroStatDivider()
                                            HeroStatItem("HADIR", state.teacherAttendanceRate, onNavigateToSessions)
                                            HeroStatDivider()
                                            HeroStatItem("PENDING", state.teacherPendingCount, onNavigateToAssignments)
                                            HeroStatDivider()
                                            HeroStatItem("MATERI", state.teacherMaterialsCount, onNavigateToLearning)
                                        }
                                        isParent -> {
                                            HeroStatItem("HADIR", state.parentAttendanceRate, onNavigateToProgress)
                                            HeroStatDivider()
                                            HeroStatItem("NILAI", state.gradeAverage, onNavigateToGrades)
                                            HeroStatDivider()
                                            HeroStatItem("TUGAS", state.parentAssignmentsCount, onNavigateToAssignments)
                                            HeroStatDivider()
                                            HeroStatItem("STATUS", "Aktif", onNavigateToProgress)
                                        }
                                        else -> {
                                            HeroStatItem("RERATA", state.gradeAverage, onNavigateToGrades)
                                            HeroStatDivider()
                                            HeroStatItem("TUGAS", state.assignmentsCount, onNavigateToAssignments)
                                            HeroStatDivider()
                                            HeroStatItem("XP", state.xpCount, onNavigateToAchievements)
                                            HeroStatDivider()
                                            HeroStatItem("BADGE", state.badgeCount, onNavigateToAchievements)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ── 3. ROLE-BASED DYNAMIC CONTENT ────────────────────────────────
                when {
                    isTeacher -> {
                        val isHomeroom = state.homeroomClass.isNotBlank()
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
                    )
                }
            }
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
