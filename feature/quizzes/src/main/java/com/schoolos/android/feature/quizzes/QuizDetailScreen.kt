package com.schoolos.android.feature.quizzes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Timer
import com.schoolos.android.core.common.formatPublishTimestamp
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.schoolos.android.core.designsystem.AccentNeonAmber
import com.schoolos.android.core.designsystem.CosmicBlack
import com.schoolos.android.core.designsystem.CosmicNavy
import com.schoolos.android.core.designsystem.CosmicSurface
import com.schoolos.android.core.designsystem.CosmicSurface2
import com.schoolos.android.core.designsystem.ErrorState
import com.schoolos.android.core.designsystem.ExecutiveTopBar
import com.schoolos.android.core.designsystem.GlassBorder
import com.schoolos.android.core.designsystem.GlassBorder2
import com.schoolos.android.core.designsystem.GlassCard
import com.schoolos.android.core.designsystem.LoadingState
import com.schoolos.android.core.designsystem.NeonBlue
import com.schoolos.android.core.designsystem.NeonError
import com.schoolos.android.core.designsystem.NeonSuccess
import com.schoolos.android.core.designsystem.NeonWarning
import com.schoolos.android.core.designsystem.StatusChip
import com.schoolos.android.core.auth.isPrincipalRole
import com.schoolos.android.core.auth.isTeacherRole
import com.schoolos.android.core.designsystem.StudentNeon
import com.schoolos.android.core.designsystem.TeacherNeon
import com.schoolos.android.core.designsystem.TextPrimary
import com.schoolos.android.core.designsystem.TextSecondary
import com.schoolos.android.core.designsystem.TextTertiary

@Composable
fun QuizDetailScreen(
    onBack: (() -> Unit) = {},
    onAttemptStarted: (String) -> Unit = {},
    onCreateQuiz: (() -> Unit) = {},
    onViewResult: ((attemptId: String, score: Int, totalPoints: Int) -> Unit) = { _, _, _ -> },
    viewModel: QuizDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val isPrincipal = isPrincipalRole(state.userRole)
    val isTeacher = isTeacherRole(state.userRole) || isPrincipal
    var selectedFilter by remember { mutableStateOf("Semua") }
    var attemptToGrade by remember { mutableStateOf<com.schoolos.android.domain.model.QuizAttempt?>(null) }
    var tokenInput by remember { mutableStateOf("") }

    LaunchedEffect(state.attempt) {
        state.attempt?.let { attempt ->
            viewModel.dismissAttempt()
            onAttemptStarted(attempt.id)
        }
    }

    if (attemptToGrade != null) {
        val attempt = attemptToGrade!!
        TeacherGradeQuizDialog(
            attempt = attempt,
            maxScore = state.quiz?.maxScore ?: attempt.totalPoints.takeIf { it > 0 } ?: 100,
            onDismiss = { attemptToGrade = null },
            onConfirm = { score, feedback ->
                viewModel.gradeAttempt(attempt.id, score, feedback)
                attemptToGrade = null
            }
        )
    }

    Scaffold(
        containerColor = CosmicBlack,
        topBar = {
            ExecutiveTopBar(
                title = state.quiz?.title ?: "Detail Kuis CBT",
                subtitle = state.quiz?.let {
                    if (isPrincipal) "Mode Kepala Sekolah • Monitoring Pelaksanaan CBT & Hasil Siswa"
                    else if (isTeacher) "Mode Guru • Manajemen & Monitoring Kuis"
                    else "Kuis CBT • ${it.subjectName ?: "Evaluasi Mandiri"}"
                } ?: "Evaluasi Siswa",
                onBack = onBack,
                actions = {
                    state.quiz?.let { q ->
                        StatusChip(label = q.status)
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                state.isLoading -> LoadingState()
                state.error != null -> {
                    ErrorState(message = state.error!!, onRetry = viewModel::load)
                }
                state.quiz != null -> {
                    val q = state.quiz!!
                    val isDraft = q.status.lowercase() == "draft"
                    val isAvailable = q.status.lowercase() in listOf("active", "open", "published")
                    val timeLimit = q.timeLimitMinutes

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // ── 1. COSMIC HERO BANNER CARD ────────────────────────
                        CosmicQuizHeroCard(
                            title = q.title,
                            subjectName = q.subjectName ?: "CBT Evaluasi",
                            maxScore = q.maxScore,
                            isTeacher = isTeacher,
                            createdAt = q.createdAt,
                        )

                        // ── 2. METRICS ROW (Soal, Waktu, Poin) ────────────────
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CosmicMetricCard(
                                label = "Total Soal",
                                value = "${q.questionsCount}",
                                unit = "Soal",
                                icon = Icons.AutoMirrored.Filled.List,
                                accentColor = NeonBlue,
                                modifier = Modifier.weight(1f)
                            )
                            CosmicMetricCard(
                                label = "Batas Waktu",
                                value = if (timeLimit != null && timeLimit > 0) "$timeLimit" else "∞",
                                unit = if (timeLimit != null && timeLimit > 0) "Menit" else "Bebas",
                                icon = Icons.Default.Timer,
                                accentColor = NeonWarning,
                                modifier = Modifier.weight(1f)
                            )
                            CosmicMetricCard(
                                label = "Poin Maksimal",
                                value = "${q.maxScore}",
                                unit = "Poin",
                                icon = Icons.Default.EmojiEvents,
                                accentColor = NeonSuccess,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // ── 3. DESKRIPSI KUIS ─────────────────────────────────
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Deskripsi & Instruksi",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = TextPrimary
                            )
                            GlassCard(cornerRadius = 14.dp) {
                                Text(
                                    text = q.description?.takeIf { it.isNotBlank() }
                                        ?: "Kuis evaluasi pemahaman materi. Kerjakan dengan teliti dan jujur untuk mengukur kompetensi pembelajaran Anda.",
                                    fontSize = 13.sp,
                                    color = TextSecondary,
                                    lineHeight = 20.sp,
                                    modifier = Modifier.padding(14.dp)
                                )
                            }
                        }

                        // ── 4. TATA TERTIB / PANDUAN GURU ─────────────────────
                        if (isTeacher) {
                            TeacherCbtGuideCard()
                        } else {
                            CbtRulesCard()
                        }

                        // ── 5. COMPLETION OR ERROR ALERT ──────────────────────
                        val isAlreadyDone = state.hasCompleted ||
                                (state.quiz?.studentHasCompleted == true) ||
                                (state.startError?.contains("batas maksimal", ignoreCase = true) == true) ||
                                (state.startError?.contains("Maximum attempt limit", ignoreCase = true) == true)

                        if (isAlreadyDone) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(NeonSuccess.copy(alpha = 0.12f))
                                    .border(1.dp, NeonSuccess.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = NeonSuccess,
                                    modifier = Modifier.size(22.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Kuis Telah Selesai Dikerjakan",
                                        color = NeonSuccess,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    val scoreVal = state.completedAttempt?.score ?: state.quiz?.studentLastScore
                                    if (scoreVal != null) {
                                        Text(
                                            text = "Skor Anda: $scoreVal poin.",
                                            color = TextPrimary,
                                            fontSize = 12.sp,
                                        )
                                    } else {
                                        Text(
                                            text = "Anda telah mencapai batas maksimal pengerjaan untuk kuis ini.",
                                            color = TextSecondary,
                                            fontSize = 12.sp,
                                        )
                                    }
                                }
                            }
                        } else if (state.startError != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(NeonError.copy(alpha = 0.12f))
                                    .border(1.dp, NeonError.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = NeonError,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = state.startError ?: "Terjadi kesalahan saat memulai kuis.",
                                    color = NeonError,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        // ── 6. PRIMARY ACTION BUTTON ──────────────────────────
                        if (isTeacher) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                if (isDraft) {
                                    Button(
                                        onClick = viewModel::publishQuiz,
                                        enabled = !state.isPublishing,
                                        shape = RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = TeacherNeon,
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(52.dp)
                                    ) {
                                        if (state.isPublishing) {
                                            CircularProgressIndicator(
                                                color = CosmicBlack,
                                                strokeWidth = 2.dp,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                "Menerbitkan Kuis...",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = CosmicBlack
                                            )
                                        } else {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = null,
                                                tint = CosmicBlack,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(Modifier.width(6.dp))
                                            Text(
                                                "PUBLIKASIKAN KUIS CBT",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Black,
                                                color = CosmicBlack,
                                                letterSpacing = 0.5.sp
                                            )
                                        }
                                    }
                                }

                                Button(
                                    onClick = onBack,
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = CosmicSurface2,
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                        .border(1.dp, GlassBorder, RoundedCornerShape(14.dp))
                                ) {
                                    Text(
                                        "KEMBALI KE DAFTAR KUIS",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary,
                                        letterSpacing = 0.5.sp
                                    )
                                }

                                Button(
                                    onClick = onCreateQuiz,
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isDraft) CosmicSurface2 else TeacherNeon,
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                        .then(
                                            if (isDraft) Modifier.border(1.dp, GlassBorder, RoundedCornerShape(14.dp))
                                            else Modifier
                                        )
                                ) {
                                    Text(
                                        "+ BUAT KUIS BARU",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (isDraft) TextPrimary else CosmicBlack,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                            }

                            Spacer(Modifier.height(24.dp))
                            
                            if (state.allAttempts.isNotEmpty()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            "Monitoring & Koreksi Ujian",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                        Text(
                                            "Pantau pengerjaan siswa secara realtime dan beri nilai",
                                            fontSize = 11.sp,
                                            color = TextTertiary
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(TeacherNeon.copy(alpha = 0.15f))
                                            .border(1.dp, TeacherNeon.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            "● Realtime",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = TeacherNeon
                                        )
                                    }
                                }
                                
                                val totalSiswa = state.allAttempts.size
                                val inProgressCount = state.allAttempts.count { it.status == "in_progress" }
                                val completedCount = state.allAttempts.count { it.status in listOf("completed", "submitted", "graded") }
                                val unsubmittedCount = state.allAttempts.count { it.status == "unsubmitted" }
                                val progressPct = if (totalSiswa > 0) completedCount.toFloat() / totalSiswa else 0f
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "${(progressPct * 100).toInt()}% Selesai ($completedCount/$totalSiswa)",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = TextSecondary,
                                        modifier = Modifier.weight(1f)
                                    )
                                    androidx.compose.material3.LinearProgressIndicator(
                                        progress = { progressPct },
                                        modifier = Modifier.width(120.dp).height(6.dp).clip(RoundedCornerShape(3.dp)),
                                        color = TeacherNeon,
                                        trackColor = CosmicSurface2
                                    )
                                }

                                // Segmented Filter Tabs
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf(
                                        "Semua" to totalSiswa,
                                        "Sedang Ujian" to inProgressCount,
                                        "Selesai" to completedCount,
                                        "Belum" to unsubmittedCount,
                                    ).forEach { (tab, count) ->
                                        val isSelected = selectedFilter == tab
                                        val tabColor = when (tab) {
                                            "Sedang Ujian" -> NeonBlue
                                            "Selesai" -> NeonSuccess
                                            "Belum" -> TextSecondary
                                            else -> TeacherNeon
                                        }
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(if (isSelected) tabColor.copy(alpha = 0.16f) else CosmicSurface2)
                                                .border(1.dp, if (isSelected) tabColor.copy(alpha = 0.5f) else GlassBorder, RoundedCornerShape(10.dp))
                                                .clickable { selectedFilter = tab }
                                                .padding(vertical = 7.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "$tab ($count)",
                                                fontSize = 10.sp,
                                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                                color = if (isSelected) tabColor else TextTertiary,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }

                                val filteredAttempts = state.allAttempts.filter { attempt ->
                                    when (selectedFilter) {
                                        "Sedang Ujian" -> attempt.status == "in_progress"
                                        "Selesai" -> attempt.status in listOf("completed", "submitted", "graded")
                                        "Belum" -> attempt.status == "unsubmitted"
                                        else -> true
                                    }
                                }.sortedWith(
                                    compareByDescending<com.schoolos.android.domain.model.QuizAttempt> { it.status == "in_progress" }
                                        .thenByDescending { it.status in listOf("completed", "submitted", "graded") }
                                        .thenByDescending { it.score }
                                )
                                
                                if (filteredAttempts.isEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(CosmicSurface2)
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "Tidak ada siswa dalam kategori ini.",
                                            fontSize = 12.sp,
                                            color = TextTertiary
                                        )
                                    }
                                } else {
                                    filteredAttempts.forEach { attempt ->
                                        val isDone = attempt.status in listOf("completed", "submitted", "graded")
                                        val isInProgress = attempt.status == "in_progress"
                                        val canGrade = attempt.status != "unsubmitted"
                                        
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(bottom = 8.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(CosmicSurface2)
                                                .border(
                                                    1.dp,
                                                    if (isInProgress) NeonBlue.copy(alpha = 0.4f) else GlassBorder,
                                                    RoundedCornerShape(12.dp)
                                                )
                                                .clickable(enabled = canGrade) {
                                                    attemptToGrade = attempt
                                                }
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Avatar circle
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        if (isInProgress) Brush.linearGradient(listOf(NeonBlue, TeacherNeon))
                                                        else if (isDone) Brush.linearGradient(listOf(NeonSuccess, NeonBlue))
                                                        else Brush.linearGradient(listOf(CosmicSurface, CosmicSurface2))
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = attempt.studentName?.firstOrNull()?.toString()?.uppercase() ?: "?",
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = Color.White
                                                )
                                            }

                                            Spacer(Modifier.width(10.dp))

                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    attempt.studentName ?: "Siswa Tidak Diketahui",
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = TextPrimary
                                                )
                                                Text(
                                                    "NISN: ${attempt.studentNisn ?: "-"}",
                                                    fontSize = 10.sp,
                                                    color = TextTertiary
                                                )
                                            }

                                            Spacer(Modifier.width(8.dp))

                                            // Status / Action
                                            when {
                                                isInProgress -> {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Box(
                                                            modifier = Modifier
                                                                .clip(RoundedCornerShape(8.dp))
                                                                .background(NeonBlue.copy(alpha = 0.15f))
                                                                .border(1.dp, NeonBlue.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                                        ) {
                                                            Text(
                                                                "Sedang Ujian ⏳",
                                                                fontSize = 10.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = NeonBlue
                                                            )
                                                        }
                                                        Spacer(Modifier.width(6.dp))
                                                        Button(
                                                            onClick = { attemptToGrade = attempt },
                                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                            shape = RoundedCornerShape(8.dp),
                                                            colors = ButtonDefaults.buttonColors(containerColor = TeacherNeon.copy(alpha = 0.2f)),
                                                            modifier = Modifier.height(28.dp)
                                                        ) {
                                                            Text("Koreksi", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TeacherNeon)
                                                        }
                                                    }
                                                }
                                                isDone -> {
                                                    Column(horizontalAlignment = Alignment.End) {
                                                        Text(
                                                            "${attempt.score} / ${attempt.totalPoints}",
                                                            fontSize = 14.sp,
                                                            fontWeight = FontWeight.ExtraBold,
                                                            color = NeonSuccess
                                                        )
                                                        Text(
                                                            if (attempt.status == "graded") "Sudah Dinilai" else "Perlu Dinilai",
                                                            fontSize = 9.sp,
                                                            color = if (attempt.status == "graded") NeonSuccess else NeonWarning
                                                        )
                                                    }
                                                }
                                                else -> {
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .background(CosmicSurface)
                                                            .border(0.5.dp, GlassBorder, RoundedCornerShape(8.dp))
                                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                                    ) {
                                                        Text(
                                                            "Belum Mulai",
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Medium,
                                                            color = TextTertiary
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            val resultAttemptId = state.completedAttempt?.id ?: state.quiz?.studentLastAttemptId
                            val canViewResult = isAlreadyDone && resultAttemptId != null

                            if (isAlreadyDone) {
                                Button(
                                    onClick = {
                                        if (resultAttemptId != null) {
                                            val score = state.completedAttempt?.score ?: state.quiz?.studentLastScore ?: 0
                                            val totalPoints = state.completedAttempt?.totalPoints ?: state.quiz?.maxScore ?: 100
                                            onViewResult(resultAttemptId, score, totalPoints)
                                        }
                                    },
                                    enabled = canViewResult,
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (canViewResult) CosmicSurface2 else CosmicSurface,
                                        disabledContainerColor = CosmicSurface2.copy(alpha = 0.6f)
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .border(
                                            width = 1.dp,
                                            color = if (canViewResult) NeonSuccess.copy(alpha = 0.45f) else GlassBorder,
                                            shape = RoundedCornerShape(14.dp)
                                        )
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = if (canViewResult) NeonSuccess else TextTertiary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = if (canViewResult) "LIHAT HASIL EVALUASI" else "SUDAH DIKERJAKAN",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Black,
                                            color = if (canViewResult) Color.White else TextTertiary,
                                            letterSpacing = 0.5.sp
                                        )
                                    }
                                }
                            } else {
                                if (q.examMode == "PROCTORED_CBT") {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(CosmicNavy)
                                            .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
                                            .padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Lock,
                                                contentDescription = null,
                                                tint = com.schoolos.android.core.designsystem.NeonWarning,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = "TOKEN UJIAN CBT PROCTORED",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = com.schoolos.android.core.designsystem.NeonWarning,
                                                letterSpacing = 0.5.sp,
                                            )
                                        }
                                        Text(
                                            text = "Ujian ini diawasi ketat. Masukkan token resmi yang diberikan oleh guru pengawas.",
                                            fontSize = 11.sp,
                                            color = TextTertiary,
                                        )
                                        OutlinedTextField(
                                            value = tokenInput,
                                            onValueChange = { if (it.length <= 10) tokenInput = it.uppercase() },
                                            placeholder = { Text("Contoh: TOKEN6", color = TextTertiary, fontSize = 13.sp) },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = com.schoolos.android.core.designsystem.NeonWarning,
                                                unfocusedBorderColor = GlassBorder,
                                                focusedTextColor = TextPrimary,
                                                unfocusedTextColor = TextPrimary,
                                            ),
                                            shape = RoundedCornerShape(10.dp),
                                        )
                                    }
                                    Spacer(Modifier.height(10.dp))
                                }

                                val canStartAttempt = !state.isStarting && isAvailable && (q.examMode != "PROCTORED_CBT" || tokenInput.isNotBlank())

                                Button(
                                    onClick = { viewModel.startAttempt(tokenInput.takeIf { q.examMode == "PROCTORED_CBT" }) },
                                    enabled = canStartAttempt,
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.Transparent,
                                        disabledContainerColor = Color.Transparent
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(
                                            if (canStartAttempt) {
                                                Brush.horizontalGradient(
                                                    listOf(StudentNeon, Color(0xFF00B4D8))
                                                )
                                            } else {
                                                Brush.horizontalGradient(
                                                    listOf(CosmicSurface2, CosmicSurface)
                                                )
                                            }
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = if (canStartAttempt) GlassBorder2 else GlassBorder,
                                            shape = RoundedCornerShape(14.dp)
                                        )
                                ) {
                                    if (state.isStarting) {
                                        CircularProgressIndicator(
                                            color = Color.White,
                                            strokeWidth = 2.dp,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Spacer(Modifier.width(10.dp))
                                        Text(
                                            "Menyiapkan Soal...",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    } else {
                                        Text(
                                            text = when {
                                                isAvailable -> "MULAI KERJAKAN"
                                                isDraft -> "KUIS MASIH DRAF"
                                                else -> "KUIS BELUM TERSEDIA"
                                            },
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Black,
                                            color = if (isAvailable) Color.White else TextTertiary,
                                            letterSpacing = 0.5.sp
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun TeacherCbtGuideCard() {
    GlassCard(cornerRadius = 14.dp) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = TeacherNeon,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Mode Pendidik / Guru Pengampu",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TeacherNeon
                )
            }
            Text(
                text = "Kuis ini diterbitkan untuk dikerjakan secara mandiri oleh peserta didik di kelas. Guru memantau kehadiran, rekonsiliasi nilai, dan analisis butir soal secara komprehensif melalui Web Portal CBT.",
                fontSize = 12.sp,
                color = TextSecondary,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
private fun CosmicQuizHeroCard(
    title: String,
    subjectName: String,
    maxScore: Int,
    isTeacher: Boolean = false,
    createdAt: String = "",
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(CosmicNavy, CosmicSurface)
                )
            )
            .border(0.5.dp, GlassBorder, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Potensi XP / Role Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isTeacher) TeacherNeon.copy(alpha = 0.15f) else AccentNeonAmber.copy(alpha = 0.15f))
                        .border(0.5.dp, if (isTeacher) TeacherNeon.copy(alpha = 0.4f) else AccentNeonAmber.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    val potentialXp = if (maxScore > 0) maxScore * 5 else 100
                    Text(
                        text = if (isTeacher) "Pendidik / Guru Pengampu" else "⭐ Potensi $potentialXp XP",
                        color = if (isTeacher) TeacherNeon else AccentNeonAmber,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Mapel Pill
                Text(
                    text = subjectName,
                    color = TextTertiary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary,
                lineHeight = 24.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(NeonSuccess)
                    )
                    Text(
                        text = "CBT Online Terstandarisasi",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }

                if (createdAt.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = TextTertiary,
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = "Diterbitkan: ${formatPublishTimestamp(createdAt)}",
                            fontSize = 10.sp,
                            color = TextTertiary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CosmicMetricCard(
    label: String,
    value: String,
    unit: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(CosmicNavy)
            .border(0.5.dp, GlassBorder, RoundedCornerShape(14.dp))
            .padding(vertical = 12.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = value,
                fontWeight = FontWeight.Black,
                fontSize = 16.sp,
                color = TextPrimary
            )
            Text(
                text = "$unit • $label",
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextTertiary,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun CbtRulesCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CosmicNavy.copy(alpha = 0.7f))
            .border(0.5.dp, NeonWarning.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = NeonWarning,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Panduan Pengerjaan CBT",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonWarning
                )
            }

            RuleBulletItem(text = "Pastikan koneksi internet stabil sebelum menekan tombol Mulai.")
            RuleBulletItem(text = "Jawaban otomatis tersimpan setiap kali Anda berpindah butir soal.")
            RuleBulletItem(text = "Pengatur waktu (timer) berjalan otomatis dan kuis akan dikumpulkan saat waktu habis.")
        }
    }
}

@Composable
private fun RuleBulletItem(text: String) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(start = 2.dp)
    ) {
        Text("•", fontSize = 12.sp, color = TextTertiary, fontWeight = FontWeight.Bold)
        Text(
            text = text,
            fontSize = 11.sp,
            color = TextSecondary,
            lineHeight = 16.sp
        )
    }
}

@Composable
fun TeacherGradeQuizDialog(
    attempt: com.schoolos.android.domain.model.QuizAttempt,
    maxScore: Int,
    onDismiss: () -> Unit,
    onConfirm: (score: Int, feedback: String?) -> Unit,
) {
    var scoreText by remember { mutableStateOf(attempt.score.toString()) }
    var feedbackText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CosmicNavy,
        shape = RoundedCornerShape(18.dp),
        title = {
            Column {
                Text(
                    text = "Koreksi Hasil Ujian",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "${attempt.studentName ?: "Siswa"} (NISN: ${attempt.studentNisn ?: "-"})",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (attempt.status == "in_progress") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(NeonBlue.copy(alpha = 0.12f))
                            .border(1.dp, NeonBlue.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        Text(
                            "Status: Siswa sedang mengerjakan ujian. Anda dapat menetapkan nilai akhir secara langsung.",
                            fontSize = 11.sp,
                            color = NeonBlue
                        )
                    }
                }

                Column {
                    Text(
                        text = "Nilai Skor (Maksimal: $maxScore)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary
                    )
                    Spacer(Modifier.height(4.dp))
                    OutlinedTextField(
                        value = scoreText,
                        onValueChange = {
                            if (it.all { ch -> ch.isDigit() }) {
                                scoreText = it
                                errorMessage = null
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = CosmicSurface2,
                            unfocusedContainerColor = CosmicSurface2,
                            focusedBorderColor = TeacherNeon,
                            unfocusedBorderColor = GlassBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (errorMessage != null) {
                        Text(
                            text = errorMessage!!,
                            fontSize = 10.sp,
                            color = NeonError,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                Column {
                    Text(
                        text = "Catatan / Umpan Balik Guru (Opsional)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary
                    )
                    Spacer(Modifier.height(4.dp))
                    OutlinedTextField(
                        value = feedbackText,
                        onValueChange = { feedbackText = it },
                        placeholder = { Text("Tulis umpan balik untuk siswa...", fontSize = 12.sp, color = TextTertiary) },
                        minLines = 2,
                        maxLines = 4,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = CosmicSurface2,
                            unfocusedContainerColor = CosmicSurface2,
                            focusedBorderColor = TeacherNeon,
                            unfocusedBorderColor = GlassBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsed = scoreText.toIntOrNull()
                    if (parsed == null || parsed < 0 || parsed > maxScore) {
                        errorMessage = "Masukkan skor valid antara 0 dan $maxScore"
                    } else {
                        onConfirm(parsed, feedbackText.takeIf { it.isNotBlank() })
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = TeacherNeon),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Simpan Nilai", color = CosmicBlack, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal", color = TextTertiary, fontSize = 12.sp)
            }
        }
    )
}

