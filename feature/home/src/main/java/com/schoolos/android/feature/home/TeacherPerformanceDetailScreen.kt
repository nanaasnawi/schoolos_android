package com.schoolos.android.feature.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.schoolos.android.core.designsystem.*
import com.schoolos.android.domain.model.Assignment
import com.schoolos.android.domain.model.LearningMaterial
import com.schoolos.android.domain.model.LearningSession
import com.schoolos.android.domain.model.Quiz
import com.schoolos.android.domain.model.TeacherProductivityTier
import com.schoolos.android.domain.model.TeacherSupervisionProfile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherPerformanceDetailScreen(
    onBack: () -> Unit,
    onNavigateToChatThread: (threadId: String, recipientName: String) -> Unit = { _, _ -> },
    onNavigateToAiChat: () -> Unit = {},
    onNavigateToMaterialDetail: (String) -> Unit = {},
    onNavigateToAssignmentDetail: (String) -> Unit = {},
    onNavigateToQuizDetail: (String) -> Unit = {},
    onNavigateToSessionDetail: (String) -> Unit = {},
    onNavigateToRombelStudents: (String) -> Unit = {},
    viewModel: TeacherPerformanceDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.state.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val isDark = LocalIsDarkTheme.current
    val screenBg = if (isDark) Color(0xFF09090B) else Color(0xFFF8FAFC)
    val cardBg = if (isDark) Color(0xFF111113) else Color.White
    val cardBorder = if (isDark) Color(0xFF1F1F23) else Color(0xFFE2E8F0)
    val innerBg = if (isDark) Color(0xFF161618) else Color(0xFFF8FAFC)
    val innerBorder = if (isDark) Color(0xFF27272A) else Color(0xFFE2E8F0)
    val textPrimary = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFFA1A1AA) else Color(0xFF64748B)
    val accentBlue = if (isDark) NeonBlue else Color(0xFF0284C7)

    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearUserMessage()
        }
    }

    val profile = uiState.profile

    Scaffold(
        containerColor = screenBg,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            ExecutiveTopBar(
                title = "Detail & Performa Guru",
                subtitle = profile?.teacher?.fullName ?: if (uiState.isPkbm) "Profil & Kinerja Tutor" else "Profil & Kinerja Guru",
                onBack = onBack,
                actions = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(innerBg)
                                .border(1.dp, innerBorder, RoundedCornerShape(10.dp))
                                .clickable(enabled = profile != null) { viewModel.exportTeacherPdf(context) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.PictureAsPdf,
                                contentDescription = "Ekspor Rapor PDF",
                                tint = accentBlue,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(innerBg)
                                .border(1.dp, innerBorder, RoundedCornerShape(10.dp))
                                .clickable { viewModel.load(isPullRefresh = true) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Muat Ulang",
                                tint = textSecondary,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        PullRefreshContainer(
            isRefreshing = uiState.isRefreshing,
            onRefresh = { viewModel.load(isPullRefresh = true) },
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding()),
        ) {
            when {
                uiState.isLoading && profile == null -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            CircularProgressIndicator(color = accentBlue, strokeWidth = 3.dp)
                            Text("Menghitung metrik kinerja guru...", fontSize = 13.sp, color = textSecondary)
                        }
                    }
                }

                uiState.errorMessage != null && profile == null -> {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Icon(Icons.Outlined.WarningAmber, contentDescription = null, tint = NeonWarning, modifier = Modifier.size(40.dp))
                            Text(
                                text = uiState.errorMessage ?: "Gagal memuat detail guru.",
                                fontSize = 13.sp,
                                color = textSecondary,
                                textAlign = TextAlign.Center,
                            )
                            Button(
                                onClick = { viewModel.load() },
                                colors = ButtonDefaults.buttonColors(containerColor = accentBlue),
                            ) {
                                Text("Coba Lagi", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                profile != null -> {
                    val tierColor = when (profile.tier) {
                        TeacherProductivityTier.SANGAT_PRODUKTIF -> NeonSuccess
                        TeacherProductivityTier.AKTIF -> accentBlue
                        TeacherProductivityTier.CUKUP -> TeacherNeon
                        TeacherProductivityTier.PERLU_PEMBINAAN -> NeonWarning
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 36.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        // 1. Hero Card + Performance Gauge
                        item {
                            TeacherHeroPerformanceCard(
                                profile = profile,
                                schoolAverageScore = uiState.schoolAverageScore,
                                isPkbm = uiState.isPkbm,
                                tierColor = tierColor,
                                accentBlue = accentBlue,
                                cardBg = cardBg,
                                cardBorder = cardBorder,
                                innerBg = innerBg,
                                innerBorder = innerBorder,
                                textPrimary = textPrimary,
                                textSecondary = textSecondary,
                            )
                        }

                        // 2. Quick Principal Actions
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                QuickActionPill(
                                    icon = Icons.Default.Chat,
                                    label = "WhatsApp",
                                    tint = TeacherNeon,
                                    cardBg = cardBg,
                                    cardBorder = cardBorder,
                                    textPrimary = textPrimary,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        launchWhatsAppChat(
                                            context,
                                            profile.teacher.noHp,
                                            "Yth. Bapak/Ibu ${profile.teacher.fullName}, "
                                        )
                                    },
                                )
                                QuickActionPill(
                                    icon = Icons.Outlined.Forum,
                                    label = "Chat App",
                                    tint = accentBlue,
                                    cardBg = cardBg,
                                    cardBorder = cardBorder,
                                    textPrimary = textPrimary,
                                    modifier = Modifier.weight(1f),
                                    onClick = { viewModel.openDirectChat(onNavigateToChatThread) },
                                )
                                QuickActionPill(
                                    icon = Icons.Outlined.AutoAwesome,
                                    label = "Tanya AI",
                                    tint = Color(0xFF8B5CF6),
                                    cardBg = cardBg,
                                    cardBorder = cardBorder,
                                    textPrimary = textPrimary,
                                    modifier = Modifier.weight(1f),
                                    onClick = onNavigateToAiChat,
                                )
                                QuickActionPill(
                                    icon = Icons.Outlined.PictureAsPdf,
                                    label = "Rapor PDF",
                                    tint = NeonWarning,
                                    cardBg = cardBg,
                                    cardBorder = cardBorder,
                                    textPrimary = textPrimary,
                                    modifier = Modifier.weight(1f),
                                    onClick = { viewModel.exportTeacherPdf(context) },
                                )
                            }
                        }

                        // 3. 4-Pillar Performance Breakdown
                        item {
                            FourPillarBreakdownCard(
                                profile = profile,
                                accentBlue = accentBlue,
                                cardBg = cardBg,
                                cardBorder = cardBorder,
                                innerBg = innerBg,
                                innerBorder = innerBorder,
                                textPrimary = textPrimary,
                                textSecondary = textSecondary,
                            )
                        }

                        // 4. Coaching Insights & Recommendations
                        if (profile.coachingInsights.isNotEmpty()) {
                            item {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(18.dp),
                                    color = cardBg,
                                    border = BorderStroke(1.dp, cardBorder),
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        ) {
                                            Icon(Icons.Outlined.Insights, contentDescription = null, tint = tierColor, modifier = Modifier.size(18.dp))
                                            Text(
                                                text = "Catatan Supervisi & Tindak Lanjut",
                                                fontSize = 13.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = textPrimary,
                                            )
                                        }
                                        profile.coachingInsights.forEach { note ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(innerBg)
                                                    .border(1.dp, innerBorder, RoundedCornerShape(10.dp))
                                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.Top,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .padding(top = 5.dp)
                                                        .size(6.dp)
                                                        .clip(CircleShape)
                                                        .background(tierColor),
                                                )
                                                Text(
                                                    text = note,
                                                    fontSize = 12.sp,
                                                    color = textPrimary,
                                                    lineHeight = 17.sp,
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // 5. Segmented Detail Tabs
                        item {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = innerBg,
                                border = BorderStroke(1.dp, innerBorder),
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    val tabs = listOf(
                                        "Karya (${profile.totalWorksCount})",
                                        "Rombel & Sesi (${profile.sessionsCount})",
                                        "Biodata GTK",
                                    )
                                    tabs.forEachIndexed { idx, label ->
                                        val selected = uiState.selectedTab == idx
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(if (selected) accentBlue.copy(alpha = 0.16f) else Color.Transparent)
                                                .border(
                                                    width = if (selected) 1.dp else 0.dp,
                                                    color = if (selected) accentBlue.copy(alpha = 0.45f) else Color.Transparent,
                                                    shape = RoundedCornerShape(10.dp),
                                                )
                                                .clickable { viewModel.selectTab(idx) }
                                                .padding(vertical = 8.dp),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Text(
                                                text = label,
                                                fontSize = 11.5.sp,
                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (selected) accentBlue else textSecondary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 6. Active Tab Body
                        when (uiState.selectedTab) {
                            0 -> {
                                if (profile.materials.isEmpty() && profile.assignments.isEmpty() && profile.quizzes.isEmpty()) {
                                    item {
                                        EmptySupervisionCard(
                                            title = "Belum Ada Karya Pembelajaran",
                                            subtitle = "Guru ini belum menerbitkan modul ajar, tugas siswa, maupun kuis CBT.",
                                            cardBg = cardBg,
                                            cardBorder = cardBorder,
                                            textPrimary = textPrimary,
                                            textSecondary = textSecondary,
                                        )
                                    }
                                } else {
                                    if (profile.materials.isNotEmpty()) {
                                        item {
                                            SectionMiniHeader("Modul & Bahan Ajar (${profile.materialsCount})", Icons.AutoMirrored.Outlined.MenuBook, accentBlue, textPrimary)
                                        }
                                        items(profile.materials.take(10), key = { "mat-${it.id}" }) { mat ->
                                            TeacherMaterialRow(mat, cardBg, cardBorder, textPrimary, textSecondary, accentBlue) {
                                                onNavigateToMaterialDetail(mat.id)
                                            }
                                        }
                                    }
                                    if (profile.assignments.isNotEmpty()) {
                                        item {
                                            SectionMiniHeader("Tugas Siswa (${profile.assignmentsCount})", Icons.AutoMirrored.Outlined.Assignment, TeacherNeon, textPrimary)
                                        }
                                        items(profile.assignments.take(10), key = { "asg-${it.id}" }) { asg ->
                                            TeacherAssignmentRow(asg, cardBg, cardBorder, textPrimary, textSecondary) {
                                                onNavigateToAssignmentDetail(asg.id)
                                            }
                                        }
                                    }
                                    if (profile.quizzes.isNotEmpty()) {
                                        item {
                                            SectionMiniHeader("Kuis & Ujian CBT (${profile.quizzesCount})", Icons.Outlined.Quiz, NeonBlue, textPrimary)
                                        }
                                        items(profile.quizzes.take(10), key = { "quiz-${it.id}" }) { quiz ->
                                            TeacherQuizRow(quiz, cardBg, cardBorder, textPrimary, textSecondary) {
                                                onNavigateToQuizDetail(quiz.id)
                                            }
                                        }
                                    }
                                }
                            }

                            1 -> {
                                if (profile.assignedClasses.isNotEmpty()) {
                                    item {
                                        Surface(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(16.dp),
                                            color = cardBg,
                                            border = BorderStroke(1.dp, cardBorder),
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(14.dp),
                                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                            ) {
                                                Text(
                                                    text = "Rombongan Belajar Diampu (${profile.assignedClasses.size})",
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = textPrimary,
                                                )
                                                profile.assignedClasses.forEach { cls ->
                                                    val isWali = profile.homeroomClassName.equals(cls, ignoreCase = true)
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .clip(RoundedCornerShape(10.dp))
                                                            .background(innerBg)
                                                            .border(1.dp, innerBorder, RoundedCornerShape(10.dp))
                                                            .clickable { onNavigateToRombelStudents(cls) }
                                                            .padding(horizontal = 12.dp, vertical = 9.dp),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically,
                                                    ) {
                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Text(cls, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = textPrimary)
                                                            if (isWali) {
                                                                Text(
                                                                    text = if (uiState.isPkbm) "Tutor Pamong / Wali Rombel" else "Wali Kelas",
                                                                    fontSize = 11.sp,
                                                                    fontWeight = FontWeight.Medium,
                                                                    color = TeacherNeon,
                                                                )
                                                            }
                                                        }
                                                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = accentBlue, modifier = Modifier.size(16.dp))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                if (profile.sessions.isEmpty()) {
                                    item {
                                        EmptySupervisionCard(
                                            title = "Belum Ada Sesi Mengajar Terdata",
                                            subtitle = "Jadwal dan riwayat sesi pembelajaran untuk guru ini belum tersedia.",
                                            cardBg = cardBg,
                                            cardBorder = cardBorder,
                                            textPrimary = textPrimary,
                                            textSecondary = textSecondary,
                                        )
                                    }
                                } else {
                                    items(profile.sessions.take(15), key = { "ses-${it.id}" }) { session ->
                                        TeacherSessionRow(session, cardBg, cardBorder, textPrimary, textSecondary, accentBlue) {
                                            onNavigateToSessionDetail(session.id)
                                        }
                                    }
                                }
                            }

                            2 -> {
                                item {
                                    TeacherOfficialBiodataCard(
                                        profile = profile,
                                        cardBg = cardBg,
                                        cardBorder = cardBorder,
                                        textPrimary = textPrimary,
                                        textSecondary = textSecondary,
                                        accentBlue = accentBlue,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TeacherHeroPerformanceCard(
    profile: TeacherSupervisionProfile,
    schoolAverageScore: Int,
    isPkbm: Boolean,
    tierColor: Color,
    accentBlue: Color,
    cardBg: Color,
    cardBorder: Color,
    innerBg: Color,
    innerBorder: Color,
    textPrimary: Color,
    textSecondary: Color,
) {
    val avatarUrl = resolveAvatarUrl(profile.teacher.fotoProfile)
    val delta = profile.performanceScore - schoolAverageScore
    val comparisonLabel = when {
        schoolAverageScore <= 0 -> "Berdasarkan metrik karya & pengajaran aktual"
        delta >= 10 -> "+$delta poin di atas rata-rata sekolah ($schoolAverageScore)"
        delta >= 0 -> "Setara/di atas rata-rata sekolah ($schoolAverageScore)"
        else -> "$delta poin dibanding rata-rata sekolah ($schoolAverageScore)"
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .clip(CircleShape)
                        .background(tierColor.copy(alpha = 0.14f))
                        .border(1.5.dp, tierColor.copy(alpha = 0.45f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    if (!avatarUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(avatarUrl)
                                .diskCachePolicy(CachePolicy.ENABLED)
                                .crossfade(true)
                                .build(),
                            contentDescription = profile.teacher.fullName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                        )
                    } else {
                        Text(
                            text = profile.teacher.fullName.firstOrNull()?.uppercase() ?: "G",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = tierColor,
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = profile.teacher.fullName,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary,
                    )
                    Text(
                        text = profile.displaySubject,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = accentBlue,
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        val activeColor = if (profile.isActive) NeonSuccess else NeonError
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(activeColor.copy(alpha = 0.14f))
                                .padding(horizontal = 7.dp, vertical = 2.dp),
                        ) {
                            Text(
                                text = if (profile.isActive) "GTK Aktif" else "Nonaktif",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = activeColor,
                            )
                        }
                        if (!profile.homeroomClassName.isNullOrBlank()) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(TeacherNeon.copy(alpha = 0.14f))
                                    .padding(horizontal = 7.dp, vertical = 2.dp),
                            ) {
                                Text(
                                    text = if (isPkbm) "Pamong ${profile.homeroomClassName}" else "Wali ${profile.homeroomClassName}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TeacherNeon,
                                )
                            }
                        }
                    }
                }
            }

            // Overall Score Box
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(innerBg)
                    .border(1.dp, innerBorder, RoundedCornerShape(14.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "SKOR PERFORMA SUPERVISI",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = textSecondary,
                            letterSpacing = 0.6.sp,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = comparisonLabel,
                            fontSize = 11.5.sp,
                            color = textSecondary,
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${profile.performanceScore}/100",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = tierColor,
                        )
                        Text(
                            text = profile.tier.label,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = tierColor,
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape)
                        .background(cardBg)
                        .border(0.5.dp, innerBorder, CircleShape),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth((profile.performanceScore / 100f).coerceIn(0.03f, 1f))
                            .clip(CircleShape)
                            .background(tierColor),
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickActionPill(
    icon: ImageVector,
    label: String,
    tint: Color,
    cardBg: Color,
    cardBorder: Color,
    textPrimary: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder),
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(imageVector = icon, contentDescription = label, tint = tint, modifier = Modifier.size(16.dp))
            }
            Text(
                text = label,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun FourPillarBreakdownCard(
    profile: TeacherSupervisionProfile,
    accentBlue: Color,
    cardBg: Color,
    cardBorder: Color,
    innerBg: Color,
    innerBorder: Color,
    textPrimary: Color,
    textSecondary: Color,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "Evaluasi Kinerja 4 Pilar",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary,
            )

            PillarProgressRow(
                title = "Modul & Bahan Ajar",
                detail = "${profile.materialsCount} modul terbit • ${profile.totalReadingsCompleted} pembaca selesai",
                score = profile.materialsScore,
                maxScore = 30,
                tint = accentBlue,
                innerBg = innerBg,
                innerBorder = innerBorder,
                textPrimary = textPrimary,
                textSecondary = textSecondary,
            )
            PillarProgressRow(
                title = "Penugasan Siswa / WB",
                detail = "${profile.assignmentsCount} tugas diterbitkan",
                score = profile.assignmentsScore,
                maxScore = 25,
                tint = TeacherNeon,
                innerBg = innerBg,
                innerBorder = innerBorder,
                textPrimary = textPrimary,
                textSecondary = textSecondary,
            )
            PillarProgressRow(
                title = "Evaluasi & Kuis CBT",
                detail = "${profile.quizzesCount} kuis CBT dibuat",
                score = profile.quizzesScore,
                maxScore = 25,
                tint = Color(0xFF8B5CF6),
                innerBg = innerBg,
                innerBorder = innerBorder,
                textPrimary = textPrimary,
                textSecondary = textSecondary,
            )
            PillarProgressRow(
                title = "Kehadiran Sesi & Rombel",
                detail = "${profile.completedSessionsCount}/${profile.sessionsCount} sesi • ${profile.assignedClasses.size} rombel",
                score = profile.sessionsScore,
                maxScore = 20,
                tint = NeonSuccess,
                innerBg = innerBg,
                innerBorder = innerBorder,
                textPrimary = textPrimary,
                textSecondary = textSecondary,
            )
        }
    }
}

@Composable
private fun PillarProgressRow(
    title: String,
    detail: String,
    score: Int,
    maxScore: Int,
    tint: Color,
    innerBg: Color,
    innerBorder: Color,
    textPrimary: Color,
    textSecondary: Color,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = textPrimary)
                Text(detail, fontSize = 11.sp, color = textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text("$score / $maxScore", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = tint)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape)
                .background(innerBg)
                .border(0.5.dp, innerBorder, CircleShape),
        ) {
            val fraction = if (maxScore > 0) (score.toFloat() / maxScore.toFloat()).coerceIn(0f, 1f) else 0f
            if (fraction > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fraction.coerceAtLeast(0.03f))
                        .clip(CircleShape)
                        .background(tint),
                )
            }
        }
    }
}

@Composable
private fun SectionMiniHeader(title: String, icon: ImageVector, tint: Color, textPrimary: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.padding(top = 4.dp),
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
        Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = textPrimary)
    }
}

@Composable
private fun TeacherMaterialRow(
    item: LearningMaterial,
    cardBg: Color,
    cardBorder: Color,
    textPrimary: Color,
    textSecondary: Color,
    accent: Color,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(2.dp))
                Text(
                    text = listOfNotNull(item.subject.takeIf { it.isNotBlank() }, item.className?.takeIf { it.isNotBlank() }, "${item.completedCount} selesai").joinToString(" • "),
                    fontSize = 11.sp,
                    color = textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = accent, modifier = Modifier.size(15.dp))
        }
    }
}

@Composable
private fun TeacherAssignmentRow(
    item: Assignment,
    cardBg: Color,
    cardBorder: Color,
    textPrimary: Color,
    textSecondary: Color,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(2.dp))
                Text(
                    text = listOfNotNull(item.subjectName?.takeIf { it.isNotBlank() }, item.className?.takeIf { it.isNotBlank() }, item.status.uppercase()).joinToString(" • "),
                    fontSize = 11.sp,
                    color = textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = TeacherNeon, modifier = Modifier.size(15.dp))
        }
    }
}

@Composable
private fun TeacherQuizRow(
    item: Quiz,
    cardBg: Color,
    cardBorder: Color,
    textPrimary: Color,
    textSecondary: Color,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(2.dp))
                Text(
                    text = listOfNotNull(item.subjectName?.takeIf { it.isNotBlank() }, item.effectiveDisplayClass?.takeIf { it.isNotBlank() }, "${item.questionsCount} soal").joinToString(" • "),
                    fontSize = 11.sp,
                    color = textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = NeonBlue, modifier = Modifier.size(15.dp))
        }
    }
}

@Composable
private fun TeacherSessionRow(
    session: LearningSession,
    cardBg: Color,
    cardBorder: Color,
    textPrimary: Color,
    textSecondary: Color,
    accent: Color,
    onClick: () -> Unit,
) {
    val statusUpper = session.status.trim().uppercase()
    val statusColor = when {
        statusUpper in listOf("COMPLETED", "SELESAI") -> NeonSuccess
        statusUpper in listOf("IN_PROGRESS", "LIVE", "BERLANGSUNG") -> accent
        statusUpper in listOf("CANCELLED", "DIBATALKAN") -> NeonError
        else -> textSecondary
    }
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = session.subjectName?.takeIf { it.isNotBlank() } ?: "Sesi Pembelajaran",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                val meta = listOfNotNull(
                    session.className?.takeIf { it.isNotBlank() },
                    session.sessionDate?.takeIf { it.isNotBlank() },
                    listOfNotNull(session.startTime?.take(5), session.endTime?.take(5)).joinToString("-").takeIf { it.isNotBlank() },
                ).joinToString(" • ")
                Text(meta.ifBlank { "Jadwal Pembelajaran" }, fontSize = 11.sp, color = textSecondary)
            }
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = statusColor.copy(alpha = 0.14f),
            ) {
                Text(
                    text = session.status.ifBlank { "Scheduled" },
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = statusColor,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                )
            }
        }
    }
}

@Composable
private fun TeacherOfficialBiodataCard(
    profile: TeacherSupervisionProfile,
    cardBg: Color,
    cardBorder: Color,
    textPrimary: Color,
    textSecondary: Color,
    accentBlue: Color,
) {
    val b = profile.teacher
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(Icons.Outlined.Badge, contentDescription = null, tint = accentBlue, modifier = Modifier.size(18.dp))
                Text("Biodata Resmi Dapodik GTK", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textPrimary)
            }
            HorizontalDivider(color = cardBorder)

            val ttl = listOfNotNull(
                b.tempatLahir?.takeIf { it.isNotBlank() },
                b.tanggalLahir?.takeIf { it.isNotBlank() },
            ).joinToString(", ")
            val fullAddress = listOfNotNull(
                b.alamatJalan?.takeIf { it.isNotBlank() },
                b.dusun?.takeIf { it.isNotBlank() },
                b.kelurahan?.takeIf { it.isNotBlank() },
                b.kecamatan?.takeIf { it.isNotBlank() },
                b.kodePos?.takeIf { it.isNotBlank() },
            ).joinToString(", ")

            SupervisionInfoRow("Nama Lengkap", b.fullName, textSecondary, textPrimary)
            SupervisionInfoRow("NUPTK", b.nuptk ?: "—", textSecondary, textPrimary)
            SupervisionInfoRow("NIP", b.nip ?: "—", textSecondary, textPrimary)
            SupervisionInfoRow("Jenis PTK", b.jenisPtk ?: "—", textSecondary, textPrimary)
            SupervisionInfoRow("Status Kepegawaian", b.statusKepegawaian ?: "—", textSecondary, textPrimary)
            SupervisionInfoRow("Mata Pelajaran", profile.displaySubject, textSecondary, textPrimary)
            SupervisionInfoRow("Rombel Diampu", b.classesAssigned ?: "—", textSecondary, textPrimary)
            SupervisionInfoRow("Jenis Kelamin", formatGenderLabel(b.jk), textSecondary, textPrimary)
            SupervisionInfoRow("Tempat, Tgl Lahir", ttl.ifBlank { "—" }, textSecondary, textPrimary)
            SupervisionInfoRow("Agama", b.agama ?: "—", textSecondary, textPrimary)
            SupervisionInfoRow("No. HP / WhatsApp", b.noHp ?: "—", textSecondary, textPrimary)
            SupervisionInfoRow("Email", b.email ?: "—", textSecondary, textPrimary)
            SupervisionInfoRow("Alamat Domisili", fullAddress.ifBlank { "—" }, textSecondary, textPrimary)
        }
    }
}
