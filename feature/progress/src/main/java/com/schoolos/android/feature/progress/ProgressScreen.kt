package com.schoolos.android.feature.progress

import com.schoolos.android.core.R as CoreR
import androidx.compose.ui.res.painterResource
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.AssignmentLate
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.schoolos.android.core.designsystem.CosmicBlack
import com.schoolos.android.core.designsystem.CosmicNavy
import com.schoolos.android.core.designsystem.CosmicSurface2
import com.schoolos.android.core.designsystem.ErrorState
import com.schoolos.android.core.designsystem.ExecutiveTopBar
import com.schoolos.android.core.designsystem.GlassBorder
import com.schoolos.android.core.designsystem.GlassBorder2
import com.schoolos.android.core.designsystem.LoadingState
import com.schoolos.android.core.designsystem.NeonBlue
import com.schoolos.android.core.designsystem.LocalIsDarkTheme
import com.schoolos.android.core.designsystem.NeonError
import com.schoolos.android.core.designsystem.NeonSuccess
import com.schoolos.android.core.designsystem.NeonWarning
import com.schoolos.android.core.designsystem.PullRefreshContainer
import com.schoolos.android.core.designsystem.StudentNeon
import com.schoolos.android.core.designsystem.TextPrimary
import com.schoolos.android.core.designsystem.TextSecondary
import com.schoolos.android.core.designsystem.TextTertiary
import com.schoolos.android.domain.model.Progress

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressScreen(
    onBack: (() -> Unit)? = null,
    onNavigateToAssignments: (() -> Unit)? = null,
    onNavigateToMaterials: (() -> Unit)? = null,
    onNavigateToQuizzes: (() -> Unit)? = null,
    viewModel: ProgressViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        containerColor = CosmicBlack,
        topBar = {
            ExecutiveTopBar(
                title = "Progres Belajar",
                subtitle = if (state.isParent) "Laporan perkembangan ${state.childName}" else "Statistik & pencapaian belajar",
                onBack = onBack,
            )
        }
    ) { padding ->
        PullRefreshContainer(
            isRefreshing = state.isRefreshing,
            onRefresh = viewModel::refresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when {
                state.isLoading -> LoadingState()
                state.error != null -> {
                    ErrorState(message = state.error!!, onRetry = viewModel::refresh)
                }
                state.progress != null -> {
                    ProgressContent(
                        progress = state.progress!!,
                        onNavigateToAssignments = onNavigateToAssignments,
                        onNavigateToMaterials = onNavigateToMaterials,
                        onNavigateToQuizzes = onNavigateToQuizzes,
                        isParent = state.isParent,
                        childName = state.childName,
                    )
                }
                else -> {
                    LoadingState()
                }
            }
        }
    }
}

@Composable
private fun ProgressContent(
    progress: Progress,
    onNavigateToAssignments: (() -> Unit)? = null,
    onNavigateToMaterials: (() -> Unit)? = null,
    onNavigateToQuizzes: (() -> Unit)? = null,
    isParent: Boolean = false,
    childName: String = "",
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {

        if (isParent) {
            // ── PARENT VIEW: GROWTH & MONITORING REPORT ──
            item { ParentPerformanceBanner(progress, childName) }

            item {
                SectionHeaderLabel(
                    title = "PERLU PERHATIAN",
                    color = NeonError,
                )
            }

            item { CriticalMonitoringCard(progress, onNavigateToAssignments) }

            item {
                SectionHeaderLabel(
                    title = "STATISTIK BELAJAR",
                    color = TextTertiary,
                )
            }

            val lessonInsight = if (progress.lessonCompleted == 0) {
                "Belum ada modul yang diselesaikan minggu ini."
            } else {
                "${progress.lessonCompleted} dari ${progress.lessonTotal} modul tuntas dipelajari."
            }

            val assignmentMissing = (progress.assignmentTotal - progress.assignmentCompleted).coerceAtLeast(0)
            val assignmentInsight = if (assignmentMissing > 0) {
                "$assignmentMissing tugas memerlukan penyelesaian segera."
            } else {
                "Semua penugasan dikumpulkan tepat waktu."
            }

            val sessionPct = if (progress.sessionTotal > 0) {
                (progress.sessionAttended * 100 / progress.sessionTotal)
            } else 100
            val sessionInsight = if (sessionPct >= 90) {
                "Kehadiran sangat konsisten ($sessionPct%) di kelas."
            } else {
                "Tingkat kehadiran tercatat $sessionPct% semester ini."
            }

            item {
                ParentMonitoringCard(
                    label = "Penyelesaian Materi",
                    completed = progress.lessonCompleted,
                    total = progress.lessonTotal,
                    insight = lessonInsight,
                    color = StudentNeon,
                    iconRes = CoreR.drawable.ic_modern_book,
                )
            }
            item {
                ParentMonitoringCard(
                    label = "Ketepatan Kumpul Tugas",
                    completed = progress.assignmentCompleted,
                    total = progress.assignmentTotal,
                    insight = assignmentInsight,
                    color = NeonBlue,
                    iconRes = CoreR.drawable.ic_modern_tasks,
                )
            }
            item {
                ParentMonitoringCard(
                    label = "Partisipasi Kelas",
                    completed = progress.sessionAttended,
                    total = progress.sessionTotal,
                    insight = sessionInsight,
                    color = NeonSuccess,
                    iconRes = CoreR.drawable.ic_modern_calendar,
                )
            }

            item { TeacherRemarksCard(progress) }

        } else {
            // ── STUDENT VIEW: RICH, INFORMATIVE & NON-DUPLICATE ──

            // 1. HERO KETUNTASAN BELAJAR
            item {
                StudentProgressHeroCard(progress)
            }

            // 2. 2x2 BENTO METRIC TILES
            item {
                SectionHeaderLabel(
                    title = "DISTRIBUSI CAPAIAN BELAJAR",
                    color = TextTertiary,
                )
            }

            item {
                val lessonRemaining = (progress.lessonTotal - progress.lessonCompleted).coerceAtLeast(0)
                val lessonSub = if (lessonRemaining > 0) "$lessonRemaining modul tersisa" else "100% Selesai"

                val assignmentRemaining = (progress.assignmentTotal - progress.assignmentCompleted).coerceAtLeast(0)
                val assignmentSub = if (assignmentRemaining > 0) "$assignmentRemaining belum dikirim" else "Semua diserahkan"

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    StudentBentoProgressTile(
                        title = "Materi Modul",
                        completed = progress.lessonCompleted,
                        total = progress.lessonTotal,
                        subtitle = lessonSub,
                        iconRes = CoreR.drawable.ic_modern_book,
                        color = StudentNeon,
                        onClick = onNavigateToMaterials,
                        modifier = Modifier.weight(1f),
                    )

                    StudentBentoProgressTile(
                        title = "Tugas Siswa",
                        completed = progress.assignmentCompleted,
                        total = progress.assignmentTotal,
                        subtitle = assignmentSub,
                        iconRes = CoreR.drawable.ic_modern_tasks,
                        color = NeonBlue,
                        onClick = onNavigateToAssignments,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            item {
                val quizRemaining = (progress.quizTotal - progress.quizCompleted).coerceAtLeast(0)
                val quizSub = if (quizRemaining > 0) "$quizRemaining kuis evaluasi" else "Tuntas dievaluasi"

                val sessionPct = if (progress.sessionTotal > 0) (progress.sessionAttended * 100 / progress.sessionTotal) else 100
                val sessionSub = if (sessionPct >= 90) "Kehadiran sangat aktif" else "$sessionPct% Presensi kelas"

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    StudentBentoProgressTile(
                        title = "Kuis CBT",
                        completed = progress.quizCompleted,
                        total = progress.quizTotal,
                        subtitle = quizSub,
                        iconRes = CoreR.drawable.ic_modern_quiz,
                        color = NeonSuccess,
                        onClick = onNavigateToQuizzes,
                        modifier = Modifier.weight(1f),
                    )

                    StudentBentoProgressTile(
                        title = "Presensi Sesi",
                        completed = progress.sessionAttended,
                        total = progress.sessionTotal,
                        subtitle = sessionSub,
                        iconRes = CoreR.drawable.ic_modern_calendar,
                        color = NeonWarning,
                        onClick = null,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            // 3. ANALISIS & TINDAKAN PRIORITAS
            item {
                SectionHeaderLabel(
                    title = "ANALISIS & REKOMENDASI BELAJAR",
                    color = TextTertiary,
                )
            }

            item {
                StudentActionableInsightsCard(
                    progress = progress,
                    onNavigateToAssignments = onNavigateToAssignments,
                    onNavigateToMaterials = onNavigateToMaterials,
                )
            }

            // 4. CATATAN & EVALUASI PENGAJAR
            item {
                SectionHeaderLabel(
                    title = "EVALUASI & CATATAN PENGAJAR",
                    color = TextTertiary,
                )
            }

            item {
                TeacherRemarksCard(progress)
            }
        }

        // 5. METADATA & SYNC FOOTER
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Sinkronisasi Real-Time • Terakhir: ${progress.calculatedAt}",
                    fontSize = 11.sp,
                    color = TextTertiary,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun SectionHeaderLabel(
    title: String,
    color: Color,
) {
    Text(
        text = title,
        fontSize = 11.sp,
        fontWeight = FontWeight.ExtraBold,
        color = color,
        letterSpacing = 0.8.sp,
        modifier = Modifier.padding(start = 4.dp, top = 2.dp),
    )
}

@Composable
private fun StudentProgressHeroCard(progress: Progress) {
    val isDark = LocalIsDarkTheme.current
    val cardBg = if (isDark) Color(0xFF111113) else Color.White
    val cardBorder = if (isDark) Color(0xFF1F1F23) else Color(0xFFE2E8F0)
    val textPrimary = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFFA1A1AA) else Color(0xFF64748B)
    val textTertiary = if (isDark) Color(0xFF71717A) else Color(0xFF94A3B8)
    val innerCardBg = if (isDark) Color(0xFF161618) else Color(0xFFF8FAFC)
    val innerCardBorder = if (isDark) Color(0xFF27272A) else Color(0xFFE2E8F0)

    val pct = (progress.overallProgress / 100.0).toFloat().coerceIn(0f, 1f)

    var animTarget by remember { mutableFloatStateOf(0f) }
    val animatedPct by animateFloatAsState(
        targetValue = animTarget,
        animationSpec = tween(1500, easing = FastOutSlowInEasing),
        label = "heroRing",
    )
    LaunchedEffect(pct) { animTarget = pct }

    val statusText = progress.academicStatus
        ?: if (progress.overallProgress >= 80.0) "Sangat Baik"
        else if (progress.overallProgress >= 60.0) "Baik & Konsisten"
        else "Perlu Penguatan"

    val statusColor = if (progress.overallProgress >= 80.0) {
        if (isDark) NeonSuccess else Color(0xFF047857)
    } else if (progress.overallProgress >= 60.0) {
        if (isDark) NeonBlue else Color(0xFF1D4ED8)
    } else {
        if (isDark) NeonWarning else Color(0xFFB45309)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = cardBg,
        border = BorderStroke(0.5.dp, cardBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "AKUMULASI KETUNTASAN",
                    color = textTertiary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.8.sp,
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = progress.subjectName.ifEmpty { "Kurikulum 2026" },
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    color = textPrimary,
                    lineHeight = 22.sp,
                )

                Text(
                    text = progress.className?.let { "Kelas $it • Semester Berjalan" } ?: "Akumulasi Akademik Siswa",
                    fontSize = 11.sp,
                    color = textSecondary,
                    fontWeight = FontWeight.Normal,
                )

                Spacer(Modifier.height(10.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = statusColor.copy(alpha = 0.12f),
                    border = BorderStroke(0.5.dp, statusColor.copy(alpha = 0.35f))
                ) {
                    Text(
                        text = "Status: $statusText",
                        color = statusColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }

            Spacer(Modifier.width(14.dp))

            // Animated Circular Ring with Dual Gradient
            val trackColor = innerCardBg
            val neonBlue = NeonBlue
            val studentNeon = StudentNeon
            Box(
                modifier = Modifier.size(90.dp),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val stroke = 9.dp.toPx()
                    val r = (size.minDimension - stroke) / 2
                    val cx = size.width / 2
                    val cy = size.height / 2

                    // Background track
                    drawArc(
                        color = trackColor,
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = Offset(cx - r, cy - r),
                        size = Size(r * 2, r * 2),
                        style = Stroke(stroke, cap = StrokeCap.Round),
                    )

                    // Animated sweep
                    if (animatedPct > 0f) {
                        drawArc(
                            brush = Brush.sweepGradient(
                                listOf(neonBlue, studentNeon, neonBlue)
                            ),
                            startAngle = -90f,
                            sweepAngle = 360f * animatedPct,
                            useCenter = false,
                            topLeft = Offset(cx - r, cy - r),
                            size = Size(r * 2, r * 2),
                            style = Stroke(stroke, cap = StrokeCap.Round),
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${(animatedPct * 100).toInt()}%",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = textPrimary,
                        letterSpacing = (-0.5).sp,
                    )
                    Text(
                        text = "Tuntas",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textTertiary,
                    )
                }
            }
        }
    }
}

@Composable
private fun StudentBentoProgressTile(
    title: String,
    completed: Int,
    total: Int,
    subtitle: String,
    icon: ImageVector? = null,
    iconRes: Int? = null,
    color: Color,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val safeTotal = total.coerceAtLeast(1)
    val pct = (completed.toFloat() / safeTotal).coerceIn(0f, 1f)

    var animTarget by remember { mutableFloatStateOf(0f) }
    val animatedPct by animateFloatAsState(
        targetValue = animTarget,
        animationSpec = tween(1200, easing = FastOutSlowInEasing),
        label = "tilePct",
    )
    LaunchedEffect(pct) { animTarget = pct }

    val isDark = LocalIsDarkTheme.current
    val cardBg = if (isDark) Color(0xFF111113) else Color.White
    val cardBorder = if (isDark) Color(0xFF1F1F23) else Color(0xFFE2E8F0)
    val textPrimary = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFFA1A1AA) else Color(0xFF64748B)
    val textTertiary = if (isDark) Color(0xFF71717A) else Color(0xFF94A3B8)
    val innerCardBg = if (isDark) Color(0xFF161618) else Color(0xFFF8FAFC)
    val innerCardBorder = if (isDark) Color(0xFF27272A) else Color(0xFFE2E8F0)

    val badgeTextColor = when (color) {
        NeonSuccess -> if (isDark) NeonSuccess else Color(0xFF047857)
        NeonWarning -> if (isDark) NeonWarning else Color(0xFFB45309)
        NeonBlue -> if (isDark) NeonBlue else Color(0xFF1D4ED8)
        else -> if (isDark) color else Color(0xFF4338CA)
    }

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(20.dp),
        color = cardBg,
        border = BorderStroke(0.5.dp, cardBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(innerCardBg)
                        .border(0.5.dp, innerCardBorder, RoundedCornerShape(11.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (iconRes != null) {
                        Icon(
                            painter = painterResource(iconRes),
                            contentDescription = null,
                            tint = color,
                            modifier = Modifier.size(19.dp)
                        )
                    } else if (icon != null) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = color,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = color.copy(alpha = if (isDark) 0.12f else 0.08f),
                    border = BorderStroke(0.5.dp, color.copy(alpha = 0.25f))
                ) {
                    Text(
                        text = "${(pct * 100).toInt()}%",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = badgeTextColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(Modifier.height(2.dp))

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "$completed",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = color,
                )
                Text(
                    text = " / $total",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = textTertiary,
                    modifier = Modifier.padding(bottom = 2.dp, start = 2.dp),
                )
            }

            Spacer(Modifier.height(8.dp))

            // Progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(innerCardBg),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedPct)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(color),
                )
            }

            Spacer(Modifier.height(6.dp))

            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = textSecondary,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun StudentActionableInsightsCard(
    progress: Progress,
    onNavigateToAssignments: (() -> Unit)? = null,
    onNavigateToMaterials: (() -> Unit)? = null,
) {
    val isDark = LocalIsDarkTheme.current
    val cardBg = if (isDark) Color(0xFF111113) else Color.White
    val cardBorder = if (isDark) Color(0xFF1F1F23) else Color(0xFFE2E8F0)
    val innerCardBg = if (isDark) Color(0xFF161618) else Color(0xFFF8FAFC)
    val innerCardBorder = if (isDark) Color(0xFF27272A) else Color(0xFFE2E8F0)

    val missingAssignments = (progress.assignmentTotal - progress.assignmentCompleted).coerceAtLeast(0)
    val missingLessons = (progress.lessonTotal - progress.lessonCompleted).coerceAtLeast(0)
    val sessionPct = if (progress.sessionTotal > 0) (progress.sessionAttended * 100 / progress.sessionTotal) else 100

    val errorColor = if (isDark) NeonError else Color(0xFFDC2626)
    val successColor = if (isDark) NeonSuccess else Color(0xFF047857)
    val lessonColor = if (isDark) StudentNeon else Color(0xFF4338CA)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = cardBg,
        border = BorderStroke(0.5.dp, cardBorder)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            // Item 1: Prioritas Tugas
            if (missingAssignments > 0) {
                InsightRow(
                    iconRes = CoreR.drawable.ic_modern_tasks,
                    iconColor = errorColor,
                    title = "$missingAssignments Tugas Perlu Diselesaikan",
                    description = "Ada tugas aktif yang belum dikumpulkan. Selesaikan sebelum tenggat untuk menjaga ketuntasan KKTP.",
                    actionText = "Buka Daftar Tugas →",
                    onAction = onNavigateToAssignments,
                )
            } else {
                InsightRow(
                    icon = Icons.Default.CheckCircle,
                    iconColor = successColor,
                    title = "Semua Tugas Tuntas Dikumpulkan",
                    description = "Seluruh penugasan pada semester ini telah diserahkan dengan lengkap.",
                    actionText = null,
                    onAction = null,
                )
            }

            HorizontalDivider(color = innerCardBorder, thickness = 0.5.dp)

            // Item 2: Penguatan Materi
            if (missingLessons > 0) {
                InsightRow(
                    iconRes = CoreR.drawable.ic_modern_book,
                    iconColor = lessonColor,
                    title = "$missingLessons Modul Materi Belum Dipelajari",
                    description = "Buka materi digital dan rangkuman modul untuk persiapan evaluasi KKTP.",
                    actionText = "Buka Bahan Ajar →",
                    onAction = onNavigateToMaterials,
                )
            } else {
                InsightRow(
                    icon = Icons.Default.CheckCircle,
                    iconColor = successColor,
                    title = "Modul Ajar Tuntas Dipelajari",
                    description = "Kamu telah menyelesaikan semua bahan materi digital semester ini.",
                    actionText = null,
                    onAction = null,
                )
            }

            HorizontalDivider(color = innerCardBorder, thickness = 0.5.dp)

            // Item 3: Presensi Kehadiran
            val presensiTitle = "Tingkat Presensi: $sessionPct%"
            val presensiDesc = if (sessionPct >= 85) {
                "Kehadiran sangat aktif ($sessionPct%). Konsistensi hadir adalah kunci utama ketuntasan belajar."
            } else {
                "Tingkat kehadiran tercatat $sessionPct%. Pastikan selalu hadir dan mengisi absensi pada sesi berikutnya."
            }
            val presensiColor = if (sessionPct >= 85) {
                if (isDark) NeonBlue else Color(0xFF1D4ED8)
            } else {
                if (isDark) NeonWarning else Color(0xFFB45309)
            }

            InsightRow(
                icon = Icons.AutoMirrored.Filled.TrendingUp,
                iconColor = presensiColor,
                title = presensiTitle,
                description = presensiDesc,
                actionText = null,
                onAction = null,
            )
        }
    }
}

@Composable
private fun InsightRow(
    icon: ImageVector? = null,
    iconRes: Int? = null,
    iconColor: Color,
    title: String,
    description: String,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val isDark = LocalIsDarkTheme.current
    val textPrimary = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFFA1A1AA) else Color(0xFF64748B)
    val innerCardBg = if (isDark) Color(0xFF161618) else Color(0xFFF8FAFC)
    val innerCardBorder = if (isDark) Color(0xFF27272A) else Color(0xFFE2E8F0)

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(innerCardBg)
                .border(0.5.dp, innerCardBorder, RoundedCornerShape(11.dp)),
            contentAlignment = Alignment.Center,
        ) {
            if (iconRes != null) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(18.dp)
                )
            } else if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = description,
                fontSize = 11.sp,
                color = textSecondary,
                lineHeight = 16.sp,
            )
            if (actionText != null && onAction != null) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = actionText,
                    fontSize = 11.sp,
                    color = iconColor,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable(onClick = onAction),
                )
            }
        }
    }
}

@Composable
private fun ParentPerformanceBanner(progress: Progress, childName: String = "") {
    val isDark = LocalIsDarkTheme.current
    val cardBg = if (isDark) Color(0xFF111113) else Color.White
    val cardBorder = if (isDark) Color(0xFF1F1F23) else Color(0xFFE2E8F0)
    val textPrimary = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFFA1A1AA) else Color(0xFF64748B)
    val textTertiary = if (isDark) Color(0xFF71717A) else Color(0xFF94A3B8)

    val statusText = progress.academicStatus
        ?: if (progress.overallProgress >= 80.0) "Sangat Baik"
        else if (progress.overallProgress >= 60.0) "Baik"
        else "Perlu Perhatian"

    val displayName = if (childName.isNotBlank()) childName
        else if (progress.subjectName.isNotBlank() && progress.subjectName != "Pelajaran") progress.subjectName
        else "Anak Anda"
    val className = progress.className.orEmpty()

    val statusColor = if (progress.overallProgress >= 80.0) {
        if (isDark) NeonSuccess else Color(0xFF047857)
    } else if (progress.overallProgress >= 60.0) {
        if (isDark) NeonBlue else Color(0xFF1D4ED8)
    } else {
        if (isDark) NeonWarning else Color(0xFFB45309)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = cardBg,
        border = BorderStroke(0.5.dp, cardBorder)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "PERFORMA AKADEMIK",
                        color = textTertiary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        displayName,
                        fontWeight = FontWeight.Black,
                        fontSize = 19.sp,
                        color = textPrimary,
                        lineHeight = 24.sp,
                    )
                    Text(
                        if (className.isNotBlank()) "Kelas $className" else "Siswa Aktif",
                        fontSize = 12.sp,
                        color = textSecondary,
                        fontWeight = FontWeight.Medium,
                    )
                }
                Spacer(Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = statusColor.copy(alpha = 0.12f),
                    border = BorderStroke(0.5.dp, statusColor.copy(alpha = 0.35f))
                ) {
                    Text(
                        text = statusText,
                        color = statusColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        String.format(java.util.Locale.US, "%.1f", progress.overallProgress),
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Black,
                        color = textPrimary,
                        letterSpacing = (-1).sp,
                    )
                    Text(
                        "/ 100",
                        fontSize = 14.sp,
                        color = textTertiary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 6.dp, start = 4.dp),
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = NeonBlue.copy(alpha = 0.12f),
                    border = BorderStroke(0.5.dp, NeonBlue.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Download, null, tint = NeonBlue, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("PDF RAPOR", color = NeonBlue, fontSize = 11.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}

@Composable
private fun CriticalMonitoringCard(
    progress: Progress,
    onNavigateToAssignments: (() -> Unit)? = null,
) {
    val isDark = LocalIsDarkTheme.current
    val cardBg = if (isDark) Color(0xFF111113) else Color.White
    val textPrimary = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A)
    val textTertiary = if (isDark) Color(0xFF71717A) else Color(0xFF94A3B8)

    val missingCount = (progress.assignmentTotal - progress.assignmentCompleted).coerceAtLeast(0)
    val absenceCount = (progress.sessionTotal - progress.sessionAttended).coerceAtLeast(0)
    val isAlert = missingCount > 0 || absenceCount > 0
    val cardBorder = if (isAlert) NeonError.copy(alpha = 0.3f) else NeonSuccess.copy(alpha = 0.3f)
    val iconBg = if (isAlert) NeonError.copy(alpha = 0.12f) else NeonSuccess.copy(alpha = 0.12f)
    val iconColor = if (isAlert) NeonError else NeonSuccess

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .clickable { onNavigateToAssignments?.invoke() },
        shape = RoundedCornerShape(22.dp),
        color = cardBg,
        border = BorderStroke(0.5.dp, cardBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (isAlert) Icons.Default.Warning else Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    if (missingCount > 0) "$missingCount Tugas Belum Terkumpul" else "Semua Tugas Tuntas Dikumpulkan",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = if (missingCount > 0) NeonError else textPrimary,
                )
                Spacer(Modifier.height(2.dp))
                val sessionPct = if (progress.sessionTotal > 0) (progress.sessionAttended * 100 / progress.sessionTotal) else 100
                Text(
                    if (missingCount > 0) "Kehadiran $sessionPct% • Ketuk untuk lihat rincian tugas →" else "Kehadiran $sessionPct% aktif mengikuti kegiatan kelas",
                    fontSize = 12.sp,
                    color = if (missingCount > 0) NeonWarning else textTertiary,
                    fontWeight = if (missingCount > 0) FontWeight.Medium else FontWeight.Normal,
                )
            }
        }
    }
}

@Composable
private fun ParentMonitoringCard(
    label: String,
    completed: Int,
    total: Int,
    insight: String,
    color: Color,
    icon: ImageVector? = null,
    iconRes: Int? = null,
) {
    val isDark = LocalIsDarkTheme.current
    val cardBg = if (isDark) Color(0xFF111113) else Color.White
    val cardBorder = if (isDark) Color(0xFF1F1F23) else Color(0xFFE2E8F0)
    val textPrimary = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFFA1A1AA) else Color(0xFF64748B)
    val innerCardBg = if (isDark) Color(0xFF161618) else Color(0xFFF8FAFC)
    val innerCardBorder = if (isDark) Color(0xFF27272A) else Color(0xFFE2E8F0)

    val pct = (completed.toFloat() / total.coerceAtLeast(1)).coerceIn(0f, 1f)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = cardBg,
        border = BorderStroke(0.5.dp, cardBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(11.dp))
                            .background(innerCardBg)
                            .border(0.5.dp, innerCardBorder, RoundedCornerShape(11.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (iconRes != null) {
                            Icon(
                                painter = painterResource(iconRes),
                                contentDescription = null,
                                tint = color,
                                modifier = Modifier.size(19.dp)
                            )
                        } else if (icon != null) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = color,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(label, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = textPrimary)
                }
                Text("${(pct * 100).toInt()}%", fontWeight = FontWeight.Black, fontSize = 15.sp, color = color)
            }
            Spacer(Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(innerCardBg),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(pct)
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(color)
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Filled.TrendingUp, null, tint = color, modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(6.dp))
                Text(insight, fontSize = 11.sp, color = textSecondary, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun TeacherRemarksCard(progress: Progress) {
    val isDark = LocalIsDarkTheme.current
    val cardBg = if (isDark) Color(0xFF111113) else Color.White
    val cardBorder = if (isDark) Color(0xFF1F1F23) else Color(0xFFE2E8F0)
    val textPrimary = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A)
    val textTertiary = if (isDark) Color(0xFF71717A) else Color(0xFF94A3B8)
    val innerCardBg = if (isDark) Color(0xFF161618) else Color(0xFFF8FAFC)
    val innerCardBorder = if (isDark) Color(0xFF27272A) else Color(0xFFE2E8F0)

    val teacherName = progress.teacherName?.ifBlank { null } ?: "Wali Kelas"
    val className = progress.className?.ifBlank { null } ?: "Kelas Aktif"
    val noteText = progress.teacherNotes?.ifBlank { null }
        ?: "Evaluasi perkembangan belajar siswa berjalan teratur dan sesuai rencana kurikulum. Terus pertahankan ritme belajar."

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = cardBg,
        border = BorderStroke(0.5.dp, cardBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(NeonBlue.copy(alpha = 0.12f))
                            .border(0.5.dp, NeonBlue.copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Default.Person, null, tint = NeonBlue, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(teacherName, fontWeight = FontWeight.Black, fontSize = 13.sp, color = textPrimary)
                            Spacer(Modifier.width(4.dp))
                            Icon(Icons.Default.CheckCircle, null, tint = NeonBlue, modifier = Modifier.size(13.dp))
                        }
                        Text("Pengampu • $className", fontSize = 11.sp, color = textTertiary, fontWeight = FontWeight.Medium)
                    }
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = NeonBlue.copy(alpha = 0.1f),
                    border = BorderStroke(0.5.dp, NeonBlue.copy(alpha = 0.25f))
                ) {
                    Text(
                        "Catatan Resmi",
                        fontSize = 10.sp,
                        color = NeonBlue,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = innerCardBg,
                border = BorderStroke(0.5.dp, innerCardBorder)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        Icons.Default.ChatBubbleOutline,
                        null,
                        tint = NeonBlue.copy(alpha = 0.8f),
                        modifier = Modifier.size(15.dp).padding(top = 2.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        noteText,
                        fontSize = 12.sp,
                        color = textPrimary,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.Normal,
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Text(
                "— Sistem Informasi Akademik • Data Terverifikasi",
                fontSize = 10.sp,
                color = textTertiary,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
            )
        }
    }
}
