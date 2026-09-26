package com.schoolos.android.feature.progress

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
                    "Penyelesaian Materi",
                    progress.lessonCompleted,
                    progress.lessonTotal,
                    lessonInsight,
                    StudentNeon,
                )
            }
            item {
                ParentMonitoringCard(
                    "Ketepatan Kumpul Tugas",
                    progress.assignmentCompleted,
                    progress.assignmentTotal,
                    assignmentInsight,
                    NeonBlue,
                )
            }
            item {
                ParentMonitoringCard(
                    "Partisipasi Kelas",
                    progress.sessionAttended,
                    progress.sessionTotal,
                    sessionInsight,
                    NeonSuccess,
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
                val lessonSub = if (lessonRemaining > 0) "$lessonRemaining modul tersisa" else "✓ 100% Selesai"

                val assignmentRemaining = (progress.assignmentTotal - progress.assignmentCompleted).coerceAtLeast(0)
                val assignmentSub = if (assignmentRemaining > 0) "⚠️ $assignmentRemaining belum dikirim" else "✓ Semua diserahkan"

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    StudentBentoProgressTile(
                        title = "Materi Modul",
                        completed = progress.lessonCompleted,
                        total = progress.lessonTotal,
                        subtitle = lessonSub,
                        icon = Icons.Default.Book,
                        color = StudentNeon,
                        onClick = onNavigateToMaterials,
                        modifier = Modifier.weight(1f),
                    )

                    StudentBentoProgressTile(
                        title = "Tugas Siswa",
                        completed = progress.assignmentCompleted,
                        total = progress.assignmentTotal,
                        subtitle = assignmentSub,
                        icon = Icons.AutoMirrored.Filled.Assignment,
                        color = NeonBlue,
                        onClick = onNavigateToAssignments,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            item {
                val quizRemaining = (progress.quizTotal - progress.quizCompleted).coerceAtLeast(0)
                val quizSub = if (quizRemaining > 0) "$quizRemaining kuis evaluasi" else "✓ Tuntas dievaluasi"

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
                        icon = Icons.Default.Quiz,
                        color = NeonSuccess,
                        onClick = onNavigateToQuizzes,
                        modifier = Modifier.weight(1f),
                    )

                    StudentBentoProgressTile(
                        title = "Presensi Sesi",
                        completed = progress.sessionAttended,
                        total = progress.sessionTotal,
                        subtitle = sessionSub,
                        icon = Icons.Default.CalendarMonth,
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

    val statusColor = if (progress.overallProgress >= 80.0) NeonSuccess
        else if (progress.overallProgress >= 60.0) NeonBlue
        else NeonWarning

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        CosmicNavy,
                        CosmicSurface2.copy(alpha = 0.85f),
                    )
                )
            )
            .border(1.dp, GlassBorder, RoundedCornerShape(18.dp))
            .padding(18.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "AKUMULASI KETUNTASAN",
                    color = TextTertiary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.8.sp,
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = progress.subjectName.ifEmpty { "Kurikulum 2026" },
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    color = Color.White,
                    lineHeight = 22.sp,
                )

                Text(
                    text = progress.className?.let { "Kelas $it • Semester Berjalan" } ?: "Akumulasi Akademik Siswa",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.Normal,
                )

                Spacer(Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(statusColor.copy(alpha = 0.12f))
                        .border(1.dp, statusColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = "Status: $statusText",
                        color = statusColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            Spacer(Modifier.width(14.dp))

            // Animated Circular Ring with Dual Gradient
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
                        color = Color.White.copy(alpha = 0.08f),
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
                                listOf(NeonBlue, StudentNeon, NeonBlue)
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
                        color = Color.White,
                        letterSpacing = (-0.5).sp,
                    )
                    Text(
                        text = "Tuntas",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextTertiary,
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
    icon: ImageVector,
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

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(CosmicNavy)
            .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(14.dp),
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(color.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(color.copy(alpha = 0.1f))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = "${(pct * 100).toInt()}%",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = color,
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
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
                    color = TextTertiary,
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
                    .background(color.copy(alpha = 0.12f)),
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
                color = TextSecondary,
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
    val missingAssignments = (progress.assignmentTotal - progress.assignmentCompleted).coerceAtLeast(0)
    val missingLessons = (progress.lessonTotal - progress.lessonCompleted).coerceAtLeast(0)
    val sessionPct = if (progress.sessionTotal > 0) (progress.sessionAttended * 100 / progress.sessionTotal) else 100

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(CosmicNavy)
            .border(1.dp, GlassBorder, RoundedCornerShape(18.dp))
            .padding(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {

            // Item 1: Prioritas Tugas
            if (missingAssignments > 0) {
                InsightRow(
                    icon = Icons.Default.AssignmentLate,
                    iconColor = NeonError,
                    title = "$missingAssignments Tugas Perlu Diselesaikan",
                    description = "Ada tugas aktif yang belum dikumpulkan. Selesaikan sebelum tenggat untuk menjaga nilai semester.",
                    actionText = "Buka Daftar Tugas →",
                    onAction = onNavigateToAssignments,
                )
            } else {
                InsightRow(
                    icon = Icons.Default.CheckCircle,
                    iconColor = NeonSuccess,
                    title = "Semua Tugas Tuntas Dikumpulkan",
                    description = "Seluruh penugasan pada semester ini telah diserahkan dengan lengkap.",
                    actionText = null,
                    onAction = null,
                )
            }

            HorizontalDivider(color = GlassBorder.copy(alpha = 0.4f), thickness = 0.6.dp)

            // Item 2: Penguatan Materi
            if (missingLessons > 0) {
                InsightRow(
                    icon = Icons.Default.Book,
                    iconColor = StudentNeon,
                    title = "$missingLessons Modul Materi Belum Dipelajari",
                    description = "Buka materi digital dan rangkuman modul untuk persiapan menghadapi kuis dan ujian.",
                    actionText = "Buka Bahan Ajar →",
                    onAction = onNavigateToMaterials,
                )
            } else {
                InsightRow(
                    icon = Icons.Default.CheckCircle,
                    iconColor = NeonSuccess,
                    title = "Modul Ajar Tuntas Dipelajari",
                    description = "Kamu telah menyelesaikan semua bahan materi digital semester ini.",
                    actionText = null,
                    onAction = null,
                )
            }

            HorizontalDivider(color = GlassBorder.copy(alpha = 0.4f), thickness = 0.6.dp)

            // Item 3: Presensi Kehadiran
            val presensiTitle = "Tingkat Presensi: $sessionPct%"
            val presensiDesc = if (sessionPct >= 85) {
                "Kehadiran sangat aktif ($sessionPct%). Konsistensi hadir adalah kunci utama ketuntasan belajar."
            } else {
                "Tingkat kehadiran tercatat $sessionPct%. Pastikan selalu hadir dan mengisi absensi pada sesi berikutnya."
            }
            val presensiColor = if (sessionPct >= 85) NeonBlue else NeonWarning

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
    icon: ImageVector,
    iconColor: Color,
    title: String,
    description: String,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(iconColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = iconColor, modifier = Modifier.size(18.dp))
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = description,
                fontSize = 11.sp,
                color = TextSecondary,
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
    val statusText = progress.academicStatus
        ?: if (progress.overallProgress >= 80.0) "Sangat Baik"
        else if (progress.overallProgress >= 60.0) "Baik"
        else "Perlu Perhatian"

    val displayName = if (childName.isNotBlank()) childName
        else if (progress.subjectName.isNotBlank() && progress.subjectName != "Pelajaran") progress.subjectName
        else "Anak Anda"
    val className = progress.className.orEmpty()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CosmicNavy)
            .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
            .padding(16.dp),
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "PERFORMA AKADEMIK",
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        displayName,
                        fontWeight = FontWeight.Black,
                        fontSize = 19.sp,
                        color = Color.White,
                        lineHeight = 24.sp,
                    )
                    Text(
                        if (className.isNotBlank()) "Kelas $className" else "Siswa Aktif",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.85f),
                        fontWeight = FontWeight.Medium,
                    )
                }
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.2f))
                        .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                ) {
                    Text(statusText, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
                        color = Color.White,
                        letterSpacing = (-1).sp,
                    )
                    Text(
                        "/ 100",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.75f),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 6.dp, start = 4.dp),
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.18f))
                        .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Download, null, tint = Color.White, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("PDF RAPOR", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black)
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
    val missingCount = (progress.assignmentTotal - progress.assignmentCompleted).coerceAtLeast(0)
    val absenceCount = (progress.sessionTotal - progress.sessionAttended).coerceAtLeast(0)
    val isAlert = missingCount > 0 || absenceCount > 0
    val cardBorder = if (isAlert) NeonError.copy(alpha = 0.3f) else NeonSuccess.copy(alpha = 0.3f)
    val iconBg = if (isAlert) NeonError.copy(alpha = 0.12f) else NeonSuccess.copy(alpha = 0.12f)
    val iconColor = if (isAlert) NeonError else NeonSuccess

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(CosmicNavy)
            .border(1.dp, cardBorder, RoundedCornerShape(18.dp))
            .clickable { onNavigateToAssignments?.invoke() }
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.AssignmentLate, null, tint = iconColor, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    if (missingCount > 0) "$missingCount Tugas Belum Terkumpul" else "Semua Tugas Tuntas Dikumpulkan",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = if (missingCount > 0) NeonError else TextPrimary,
                )
                Spacer(Modifier.height(2.dp))
                val sessionPct = if (progress.sessionTotal > 0) (progress.sessionAttended * 100 / progress.sessionTotal) else 100
                Text(
                    if (missingCount > 0) "Kehadiran $sessionPct% • Ketuk untuk lihat rincian tugas →" else "Kehadiran $sessionPct% aktif mengikuti kegiatan kelas",
                    fontSize = 12.sp,
                    color = if (missingCount > 0) NeonWarning else TextTertiary,
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
) {
    val pct = (completed.toFloat() / total.coerceAtLeast(1)).coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CosmicNavy)
            .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
            .padding(16.dp),
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(label, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                Text("${(pct * 100).toInt()}%", fontWeight = FontWeight.Black, fontSize = 15.sp, color = color)
            }
            Spacer(Modifier.height(10.dp))
            Box(
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape).background(color.copy(alpha = 0.08f)),
            ) {
                Box(modifier = Modifier.fillMaxWidth(pct).height(6.dp).clip(CircleShape).background(color))
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Filled.TrendingUp, null, tint = color, modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(6.dp))
                Text(insight, fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun TeacherRemarksCard(progress: Progress) {
    val teacherName = progress.teacherName?.ifBlank { null } ?: "Wali Kelas"
    val className = progress.className?.ifBlank { null } ?: "Kelas Aktif"
    val noteText = progress.teacherNotes?.ifBlank { null }
        ?: "Evaluasi perkembangan belajar siswa berjalan teratur dan sesuai rencana kurikulum. Terus pertahankan ritme belajar."

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(CosmicNavy)
            .border(1.dp, NeonBlue.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
            .padding(14.dp),
    ) {
        Column {
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
                            .border(1.dp, NeonBlue.copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Default.Person, null, tint = NeonBlue, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(teacherName, fontWeight = FontWeight.Black, fontSize = 13.sp, color = TextPrimary)
                            Spacer(Modifier.width(4.dp))
                            Text("✓", color = NeonBlue, fontSize = 11.sp, fontWeight = FontWeight.Black)
                        }
                        Text("Pengampu • $className", fontSize = 11.sp, color = TextTertiary, fontWeight = FontWeight.Medium)
                    }
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(NeonBlue.copy(alpha = 0.1f))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Text("Catatan Resmi", fontSize = 10.sp, color = NeonBlue, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CosmicBlack.copy(alpha = 0.5f))
                    .padding(12.dp),
            ) {
                Row(verticalAlignment = Alignment.Top) {
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
                        color = TextPrimary,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.Normal,
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Text(
                "— Sistem Informasi Akademik • Data Terverifikasi",
                fontSize = 10.sp,
                color = TextTertiary,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
            )
        }
    }
}
