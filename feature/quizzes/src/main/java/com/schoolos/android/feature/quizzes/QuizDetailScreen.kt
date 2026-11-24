package com.schoolos.android.feature.quizzes

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.EditCalendar
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.MoreTime
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.schoolos.android.core.R as CoreR
import com.schoolos.android.core.auth.isPrincipalRole
import com.schoolos.android.core.auth.isTeacherRole
import com.schoolos.android.core.common.formatPublishTimestamp
import com.schoolos.android.core.designsystem.CosmicBlack
import com.schoolos.android.core.designsystem.ErrorState
import com.schoolos.android.core.designsystem.ExecutiveTopBar
import com.schoolos.android.core.designsystem.LoadingState
import com.schoolos.android.core.designsystem.LocalIsDarkTheme
import com.schoolos.android.core.designsystem.NeonBlue
import com.schoolos.android.core.designsystem.NeonError
import com.schoolos.android.core.designsystem.NeonSuccess
import com.schoolos.android.core.designsystem.NeonWarning
import com.schoolos.android.core.designsystem.SchoolOsConfirmationDialog
import com.schoolos.android.core.designsystem.StudentNeon
import com.schoolos.android.core.designsystem.TeacherNeon
import com.schoolos.android.core.designsystem.TextPrimary
import com.schoolos.android.core.designsystem.TextSecondary
import com.schoolos.android.core.designsystem.TextTertiary
import com.schoolos.android.domain.model.Quiz
import com.schoolos.android.domain.model.QuizAttempt
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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

    var staffCompactTab by rememberSaveable { mutableIntStateOf(0) } // 0: Ringkasan, 1: Pantau, 2: Soal
    var tabletRightTab by rememberSaveable { mutableIntStateOf(0) } // 0: Pantau, 1: Log Event, 2: Bank Soal
    var attemptToGrade by remember { mutableStateOf<QuizAttempt?>(null) }
    var tokenInput by remember { mutableStateOf("") }
    var showTokenBottomSheet by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showRotateTokenDialog by remember { mutableStateOf(false) }
    var showEditScheduleSheet by remember { mutableStateOf(false) }

    val nowMs = rememberTickerMillis(1000L)

    // Periodic 10-second live refresh when proctoring room is open
    LaunchedEffect(isTeacher, state.isAutoSyncMonitor, state.quiz?.id) {
        if (!isTeacher || !state.isAutoSyncMonitor) return@LaunchedEffect
        while (true) {
            delay(10_000L)
            viewModel.load(silent = true)
        }
    }

    val isDark = LocalIsDarkTheme.current
    val cardBg = if (isDark) Color(0xFF111113) else Color.White
    val cardBorder = if (isDark) Color(0xFF1F1F23) else Color(0xFFE2E8F0)
    val innerCardBg = if (isDark) Color(0xFF161618) else Color(0xFFF8FAFC)
    val innerCardBorder = if (isDark) Color(0xFF27272A) else Color(0xFFE2E8F0)
    val textPrimary = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFFA1A1AA) else Color(0xFF64748B)
    val textTertiary = if (isDark) Color(0xFF71717A) else Color(0xFF94A3B8)
    val studentAccent = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)
    val warningColor = if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706)
    val successColor = if (isDark) Color(0xFF34D399) else Color(0xFF16A34A)

    val context = LocalContext.current
    LaunchedEffect(state.successMessage) {
        state.successMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.dismissSuccessMessage()
        }
    }

    LaunchedEffect(state.attempt) {
        state.attempt?.let { attempt ->
            showTokenBottomSheet = false
            viewModel.dismissAttempt()
            onAttemptStarted(attempt.id)
        }
    }

    LaunchedEffect(state.hasCompleted) {
        if (state.hasCompleted) showTokenBottomSheet = false
    }

    if (showTokenBottomSheet && state.quiz != null && !isTeacher) {
        CbtTokenInputBottomSheet(
            quizTitle = state.quiz!!.title,
            tokenInput = tokenInput,
            onTokenChange = { raw ->
                tokenInput = raw.uppercase().filter { !it.isWhitespace() }.take(16)
                viewModel.clearStartError()
            },
            isStarting = state.isStarting,
            errorMessage = state.startError,
            onSubmit = {
                if (tokenInput.isNotBlank() && !state.isStarting) {
                    viewModel.startAttempt(tokenInput.trim())
                }
            },
            onDismiss = {
                if (!state.isStarting) {
                    showTokenBottomSheet = false
                    viewModel.clearStartError()
                }
            },
        )
    }

    if (showRotateTokenDialog && state.quiz != null && isTeacher) {
        RotateTokenDialog(
            currentToken = state.quiz!!.examToken,
            isProcessing = state.isProcessingProctorAction,
            onConfirmAuto = {
                showRotateTokenDialog = false
                viewModel.rotateToken(customToken = null)
            },
            onConfirmCustom = { custom ->
                showRotateTokenDialog = false
                viewModel.rotateToken(customToken = custom)
            },
            onDismiss = { showRotateTokenDialog = false },
        )
    }

    if (showEditScheduleSheet && state.quiz != null && isTeacher) {
        EditQuizScheduleBottomSheet(
            quiz = state.quiz!!,
            isProcessing = state.isProcessingProctorAction,
            onSave = { startIso, endIso, dur, mode, tok ->
                showEditScheduleSheet = false
                viewModel.updateSchedule(
                    startAtIso = startIso,
                    endAtIso = endIso,
                    durationMinutes = dur,
                    examMode = mode,
                    examToken = tok,
                )
            },
            onDismiss = { showEditScheduleSheet = false },
        )
    }

    if (showDeleteDialog) {
        SchoolOsConfirmationDialog(
            title = "Hapus Paket Kuis CBT?",
            message = "Apakah Anda yakin ingin menghapus kuis \"${state.quiz?.title}\"? Seluruh butir soal dan data pengerjaan terkait akan diarsipkan.",
            confirmText = "Ya, Hapus Kuis",
            dismissText = "Batal",
            icon = Icons.Default.DeleteOutline,
            accentColor = Color(0xFFEF4444),
            confirmButtonColor = Color(0xFFEF4444),
            onConfirm = {
                showDeleteDialog = false
                viewModel.deleteQuiz(onSuccess = onBack)
            },
            onDismiss = { showDeleteDialog = false },
        )
    }

    if (attemptToGrade != null) {
        val attempt = attemptToGrade!!
        val effMax = if (attempt.totalPoints > 0) attempt.totalPoints else (state.quiz?.maxScore ?: 100)
        TeacherGradeQuizDialog(
            attempt = attempt,
            maxScore = effMax,
            onDismiss = { attemptToGrade = null },
            onConfirm = { score, feedback ->
                viewModel.gradeAttempt(attempt.id, score, feedback)
                attemptToGrade = null
            },
            onViewAttemptDetail = if (attempt.status != "unsubmitted") {
                {
                    val curScore = attempt.score ?: 0
                    attemptToGrade = null
                    onViewResult(attempt.id, curScore, effMax)
                }
            } else null,
        )
    }

    Scaffold(
        containerColor = CosmicBlack,
        topBar = {
            ExecutiveTopBar(
                title = state.quiz?.title ?: "Detail Ujian CBT",
                subtitle = when {
                    isPrincipal -> "Mode Kepala Sekolah • Token, Jadwal & Pantau Proktor"
                    isTeacher -> "Mode Guru • Ruang Kendali & Pantau Ujian"
                    else -> state.quiz?.subjectName ?: "Persiapan Ujian CBT"
                },
                onBack = onBack,
                actions = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        state.quiz?.let { q ->
                            ExamPhasePill(phase = q.effectivePhase)
                            if (isTeacher) {
                                IconButton(onClick = { showDeleteDialog = true }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Hapus Kuis", tint = NeonError, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                },
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                state.isLoading -> LoadingState()
                state.error != null -> ErrorState(message = state.error!!, onRetry = { viewModel.load() })
                state.quiz != null -> {
                    val q = state.quiz!!
                    AdaptiveQuizWidthContainer(maxWidth = if (isTeacher) 1200.dp else 760.dp) { widthClass ->
                        val hPadding = widthClass.contentHorizontalPadding()
                        val useTwoPane = isTeacher && widthClass == QuizWindowWidthClass.EXPANDED

                        val openGradeDialogForParticipant: (String, String, String, String?, String, Int?, Int) -> Unit =
                            { attId, stId, stName, stNisn, rawStatus, sc, tot ->
                                attemptToGrade = QuizAttempt(
                                    id = attId,
                                    quizId = q.id,
                                    studentId = stId,
                                    studentName = stName,
                                    studentNisn = stNisn,
                                    startedAt = "",
                                    completedAt = null,
                                    score = sc,
                                    totalPoints = tot,
                                    status = rawStatus,
                                    createdAt = "",
                                    updatedAt = "",
                                )
                            }

                        if (!isTeacher) {
                            StudentDetailLayout(
                                quiz = q,
                                state = state,
                                nowMs = nowMs,
                                tokenInput = tokenInput,
                                hPadding = hPadding,
                                cardBg = cardBg,
                                cardBorder = cardBorder,
                                innerCardBg = innerCardBg,
                                innerCardBorder = innerCardBorder,
                                textPrimary = textPrimary,
                                textSecondary = textSecondary,
                                textTertiary = textTertiary,
                                studentAccent = studentAccent,
                                warningColor = warningColor,
                                successColor = successColor,
                                onOpenTokenSheet = {
                                    viewModel.clearStartError()
                                    showTokenBottomSheet = true
                                },
                                onStartExamWithoutToken = { viewModel.startAttempt(null) },
                                onViewResult = { attId, sc, tot -> onViewResult(attId, sc, tot) },
                            )
                        } else if (useTwoPane) {
                            // Tablet / Large Screen Master-Detail Split View
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = hPadding, vertical = 14.dp),
                                horizontalArrangement = Arrangement.spacedBy(18.dp),
                            ) {
                                // Left Pane: Hero, Token Vault, Schedule, Actions
                                Column(
                                    modifier = Modifier
                                        .weight(0.43f)
                                        .verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.spacedBy(14.dp),
                                ) {
                                    StaffOverviewColumn(
                                        quiz = q,
                                        isPrincipal = isPrincipal,
                                        state = state,
                                        nowMs = nowMs,
                                        onRotateToken = { showRotateTokenDialog = true },
                                        onEditSchedule = { showEditScheduleSheet = true },
                                        onExtendTime15 = { viewModel.extendTime(15) },
                                        onPublish = viewModel::publishQuiz,
                                        onCreateQuiz = onCreateQuiz,
                                        onDelete = { showDeleteDialog = true },
                                    )
                                    Spacer(Modifier.height(20.dp))
                                }

                                // Right Pane: Live Proctor Monitor / Event Logs / Bank Soal
                                Column(
                                    modifier = Modifier
                                        .weight(0.57f)
                                        .verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    DetailPanelTabRow(
                                        labels = listOf(
                                            "Pantau Live (${state.proctorMonitor?.summary?.totalEnrolled ?: state.allAttempts.size})",
                                            "Log Aktivitas (${state.proctorMonitor?.recentEvents?.size ?: 0})",
                                            "Bank Soal (${q.questionsCount})",
                                        ),
                                        selectedIndex = tabletRightTab,
                                        onSelect = { idx ->
                                            tabletRightTab = idx
                                            if (idx == 2) viewModel.loadQuestionPreview()
                                        },
                                    )

                                    when (tabletRightTab) {
                                        0 -> ProctorLiveMonitorPanel(
                                            monitor = state.proctorMonitor,
                                            fallbackAttempts = state.allAttempts,
                                            maxScore = q.maxScore,
                                            isRefreshing = state.isRefreshingMonitor,
                                            isAutoSync = state.isAutoSyncMonitor,
                                            onToggleAutoSync = viewModel::toggleAutoSyncMonitor,
                                            onRefresh = { viewModel.load(silent = true) },
                                            onExtendTime15Min = { viewModel.extendTime(15) },
                                            onGradeAttempt = openGradeDialogForParticipant,
                                            onViewAttemptDetail = onViewResult,
                                            onForceSubmitAttempt = viewModel::forceSubmitStudentAttempt,
                                            onReopenAttempt = viewModel::reopenStudentAttempt,
                                        )
                                        1 -> ProctorEventsFeedSection(events = state.proctorMonitor?.recentEvents.orEmpty())
                                        else -> QuestionPreviewListSection(
                                            questions = state.previewQuestions,
                                            isLoading = state.isLoadingQuestions,
                                            error = state.questionsError,
                                            onRetry = { viewModel.loadQuestionPreview(force = true) },
                                        )
                                    }
                                    Spacer(Modifier.height(20.dp))
                                }
                            }
                        } else {
                            // Phone / Compact / Medium Tabbed Layout
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .padding(horizontal = hPadding, vertical = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp),
                            ) {
                                CosmicQuizHeroCard(
                                    quiz = q,
                                    isPrincipal = isPrincipal,
                                    isTeacher = true,
                                    nowMs = nowMs,
                                )

                                DetailPanelTabRow(
                                    labels = listOf(
                                        "Ringkasan & Token",
                                        "Pantau (${state.proctorMonitor?.summary?.totalEnrolled ?: state.allAttempts.size})",
                                        "Soal (${q.questionsCount})",
                                    ),
                                    selectedIndex = staffCompactTab,
                                    onSelect = { idx ->
                                        staffCompactTab = idx
                                        if (idx == 2) viewModel.loadQuestionPreview()
                                    },
                                )

                                when (staffCompactTab) {
                                    0 -> StaffOverviewDetailsOnly(
                                        quiz = q,
                                        state = state,
                                        nowMs = nowMs,
                                        onRotateToken = { showRotateTokenDialog = true },
                                        onEditSchedule = { showEditScheduleSheet = true },
                                        onExtendTime15 = { viewModel.extendTime(15) },
                                        onPublish = viewModel::publishQuiz,
                                        onCreateQuiz = onCreateQuiz,
                                        onOpenMonitor = { staffCompactTab = 1 },
                                        onDelete = { showDeleteDialog = true },
                                    )
                                    1 -> {
                                        ProctorLiveMonitorPanel(
                                            monitor = state.proctorMonitor,
                                            fallbackAttempts = state.allAttempts,
                                            maxScore = q.maxScore,
                                            isRefreshing = state.isRefreshingMonitor,
                                            isAutoSync = state.isAutoSyncMonitor,
                                            onToggleAutoSync = viewModel::toggleAutoSyncMonitor,
                                            onRefresh = { viewModel.load(silent = true) },
                                            onExtendTime15Min = { viewModel.extendTime(15) },
                                            onGradeAttempt = openGradeDialogForParticipant,
                                            onViewAttemptDetail = onViewResult,
                                            onForceSubmitAttempt = viewModel::forceSubmitStudentAttempt,
                                            onReopenAttempt = viewModel::reopenStudentAttempt,
                                        )
                                        if (!state.proctorMonitor?.recentEvents.isNullOrEmpty()) {
                                            Text(
                                                text = "Log Pengawasan Terbaru",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = textPrimary,
                                                modifier = Modifier.padding(top = 6.dp),
                                            )
                                            ProctorEventsFeedSection(events = state.proctorMonitor!!.recentEvents.take(15))
                                        }
                                    }
                                    else -> QuestionPreviewListSection(
                                        questions = state.previewQuestions,
                                        isLoading = state.isLoadingQuestions,
                                        error = state.questionsError,
                                        onRetry = { viewModel.loadQuestionPreview(force = true) },
                                    )
                                }
                                Spacer(Modifier.height(24.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailPanelTabRow(
    labels: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
) {
    val isDark = LocalIsDarkTheme.current
    val trackBg = if (isDark) Color(0xFF141416) else Color(0xFFF1F5F9)
    val trackBorder = if (isDark) Color(0xFF27272A) else Color(0xFFE2E8F0)
    val activeBg = if (isDark) Color(0xFF064E3B) else Color.White

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = trackBg,
        border = BorderStroke(1.dp, trackBorder),
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            labels.forEachIndexed { idx, label ->
                val isSel = selectedIndex == idx
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(9.dp))
                        .clickable { onSelect(idx) },
                    shape = RoundedCornerShape(9.dp),
                    color = if (isSel) activeBg else Color.Transparent,
                    border = if (isSel) BorderStroke(1.dp, TeacherNeon.copy(alpha = 0.35f)) else null,
                ) {
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.SemiBold,
                        color = if (isSel) TeacherNeon else TextSecondary,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun StaffOverviewColumn(
    quiz: Quiz,
    isPrincipal: Boolean,
    state: QuizDetailUiState,
    nowMs: Long,
    onRotateToken: () -> Unit,
    onEditSchedule: () -> Unit,
    onExtendTime15: () -> Unit,
    onPublish: () -> Unit,
    onCreateQuiz: () -> Unit,
    onDelete: () -> Unit,
) {
    CosmicQuizHeroCard(quiz = quiz, isPrincipal = isPrincipal, isTeacher = true, nowMs = nowMs)
    StaffOverviewDetailsOnly(
        quiz = quiz,
        state = state,
        nowMs = nowMs,
        onRotateToken = onRotateToken,
        onEditSchedule = onEditSchedule,
        onExtendTime15 = onExtendTime15,
        onPublish = onPublish,
        onCreateQuiz = onCreateQuiz,
        onOpenMonitor = null,
        onDelete = onDelete,
    )
}

@Composable
private fun StaffOverviewDetailsOnly(
    quiz: Quiz,
    state: QuizDetailUiState,
    nowMs: Long,
    onRotateToken: () -> Unit,
    onEditSchedule: () -> Unit,
    onExtendTime15: () -> Unit,
    onPublish: () -> Unit,
    onCreateQuiz: () -> Unit,
    onOpenMonitor: (() -> Unit)?,
    onDelete: () -> Unit,
) {
    val isDark = LocalIsDarkTheme.current
    val cardBg = if (isDark) Color(0xFF111113) else Color.White
    val cardBorder = if (isDark) Color(0xFF1F1F23) else Color(0xFFE2E8F0)
    val textPrimary = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFFA1A1AA) else Color(0xFF64748B)

    // 1. Token Vault Card (Prominently Visible for Kepsek & Guru!)
    ExecutiveTokenVaultCard(
        quiz = quiz,
        isProcessing = state.isProcessingProctorAction,
        onRotateClick = onRotateToken,
    )

    // 2. Metrics Row
    val timeLimit = quiz.timeLimitMinutes
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        CosmicMetricCard("Total Soal", "${quiz.questionsCount}", "Soal", Icons.AutoMirrored.Filled.List, NeonBlue, Modifier.weight(1f))
        CosmicMetricCard("Batas Waktu", if (timeLimit != null && timeLimit > 0) "$timeLimit" else "∞", if (timeLimit != null && timeLimit > 0) "Menit" else "Bebas", Icons.Default.Timer, NeonWarning, Modifier.weight(1f))
        CosmicMetricCard("Poin Maks", "${quiz.maxScore}", "Poin", Icons.Default.EmojiEvents, NeonSuccess, Modifier.weight(1f))
    }

    // 3. Schedule Control Card (With Edit Schedule & +15m Quick Action!)
    ExamScheduleControlCard(
        quiz = quiz,
        isTeacher = true,
        nowMs = nowMs,
        onEditSchedule = onEditSchedule,
        onExtendTime15 = onExtendTime15,
    )

    // 4. Error Banner if any proctor/publish action failed
    if (!state.startError.isNullOrBlank()) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = NeonError.copy(alpha = 0.12f),
            border = BorderStroke(1.dp, NeonError.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.ErrorOutline, null, tint = NeonError, modifier = Modifier.size(18.dp))
                Text(state.startError!!, color = NeonError, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }

    // 5. Description
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Deskripsi & Petunjuk Ujian", fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = textPrimary)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = cardBg,
            border = BorderStroke(1.dp, cardBorder),
        ) {
            Text(
                text = quiz.description?.takeIf { it.isNotBlank() }
                    ?: "Ujian evaluasi pembelajaran CBT. Pastikan token dan jadwal sesi sudah sesuai sebelum peserta masuk.",
                fontSize = 13.sp,
                color = textSecondary,
                lineHeight = 20.sp,
                modifier = Modifier.padding(14.dp),
            )
        }
    }

    // 6. Compact Operational Actions
    val isPureDraft = quiz.effectivePhase == "DRAFT" || quiz.status.equals("draft", ignoreCase = true)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (onOpenMonitor != null) {
            Button(
                onClick = onOpenMonitor,
                shape = RoundedCornerShape(13.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TeacherNeon, contentColor = Color.White),
                modifier = Modifier.fillMaxWidth().height(48.dp),
            ) {
                Text("BUKA MODE PANTAU LIVE PROKTOR", fontSize = 13.sp, fontWeight = FontWeight.Black, letterSpacing = 0.4.sp)
            }
        }
        if (isPureDraft) {
            Button(
                onClick = onPublish,
                enabled = !state.isPublishing,
                shape = RoundedCornerShape(13.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NeonBlue, contentColor = Color.White),
                modifier = Modifier.fillMaxWidth().height(46.dp),
            ) {
                if (state.isPublishing) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                } else {
                    Icon(Icons.Default.Check, null, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("AKTIFKAN / PUBLIKASIKAN SEKARANG", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = onCreateQuiz,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, cardBorder),
                modifier = Modifier.weight(1f).height(44.dp),
            ) {
                Text("+ Buat Ujian", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textPrimary)
            }
            OutlinedButton(
                onClick = onDelete,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, NeonError.copy(alpha = 0.45f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonError),
                modifier = Modifier.weight(1f).height(44.dp),
            ) {
                Icon(Icons.Default.DeleteOutline, null, tint = NeonError, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Hapus Ujian", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun StudentDetailLayout(
    quiz: Quiz,
    state: QuizDetailUiState,
    nowMs: Long,
    tokenInput: String,
    hPadding: androidx.compose.ui.unit.Dp,
    cardBg: Color,
    cardBorder: Color,
    innerCardBg: Color,
    innerCardBorder: Color,
    textPrimary: Color,
    textSecondary: Color,
    textTertiary: Color,
    studentAccent: Color,
    warningColor: Color,
    successColor: Color,
    onOpenTokenSheet: () -> Unit,
    onStartExamWithoutToken: () -> Unit,
    onViewResult: (String, Int, Int) -> Unit,
) {
    val phase = quiz.effectivePhase
    val isAvailable = quiz.isCurrentlyOpenForExam && phase != "ENDED" && phase != "UPCOMING"
    val timeLimit = quiz.timeLimitMinutes
    val isAlreadyDone = state.hasCompleted ||
        (quiz.studentHasCompleted) ||
        (state.startError?.contains("batas maksimal", ignoreCase = true) == true) ||
        (state.startError?.contains("Maximum attempt limit", ignoreCase = true) == true)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = hPadding, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        CosmicQuizHeroCard(quiz = quiz, isPrincipal = false, isTeacher = false, nowMs = nowMs)

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CosmicMetricCard("Total Soal", "${quiz.questionsCount}", "Soal", Icons.AutoMirrored.Filled.List, NeonBlue, Modifier.weight(1f))
            CosmicMetricCard("Batas Waktu", if (timeLimit != null && timeLimit > 0) "$timeLimit" else "∞", if (timeLimit != null && timeLimit > 0) "Menit" else "Bebas", Icons.Default.Timer, NeonWarning, Modifier.weight(1f))
            CosmicMetricCard("Poin Maks", "${quiz.maxScore}", "Poin", Icons.Default.EmojiEvents, NeonSuccess, Modifier.weight(1f))
        }

        ExamScheduleControlCard(
            quiz = quiz,
            isTeacher = false,
            nowMs = nowMs,
            onEditSchedule = {},
            onExtendTime15 = {},
        )

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Deskripsi & Instruksi", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = cardBg,
                border = BorderStroke(1.dp, cardBorder),
            ) {
                Text(
                    text = quiz.description?.takeIf { it.isNotBlank() }
                        ?: "Kuis evaluasi pemahaman materi. Kerjakan dengan teliti dan jujur untuk mengukur kompetensi pembelajaran Anda.",
                    fontSize = 13.5.sp,
                    color = textSecondary,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }

        CbtRulesCard()

        if (isAlreadyDone) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(NeonSuccess.copy(alpha = 0.12f))
                    .border(1.dp, NeonSuccess.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(Icons.Default.CheckCircle, null, tint = NeonSuccess, modifier = Modifier.size(22.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Ujian Telah Selesai Dikerjakan", color = NeonSuccess, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    val scoreVal = state.completedAttempt?.score ?: quiz.studentLastScore
                    Text(
                        text = if (scoreVal != null) "Skor Anda: $scoreVal poin." else "Jawaban Anda tersimpan & menunggu koreksi.",
                        color = textPrimary,
                        fontSize = 12.sp,
                    )
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
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(Icons.Default.ErrorOutline, null, tint = NeonError, modifier = Modifier.size(20.dp))
                Text(state.startError!!, color = NeonError, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            }
        }

        val resultAttemptId = state.completedAttempt?.id ?: quiz.studentLastAttemptId
        val canViewResult = isAlreadyDone && resultAttemptId != null

        if (isAlreadyDone) {
            Button(
                onClick = {
                    resultAttemptId?.let { attId ->
                        onViewResult(attId, state.completedAttempt?.score ?: (quiz.studentLastScore ?: 0), state.completedAttempt?.totalPoints ?: quiz.maxScore)
                    }
                },
                enabled = canViewResult,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (canViewResult) cardBg else innerCardBg,
                    disabledContainerColor = innerCardBg.copy(alpha = 0.6f),
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .border(1.dp, if (canViewResult) NeonSuccess.copy(alpha = 0.55f) else innerCardBorder, RoundedCornerShape(14.dp)),
            ) {
                Icon(Icons.Default.CheckCircle, null, tint = if (canViewResult) successColor else textTertiary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (canViewResult) "LIHAT HASIL EVALUASI" else "SUDAH DIKERJAKAN",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    color = if (canViewResult) textPrimary else textTertiary,
                )
            }
        } else {
            val needsExamToken = quiz.requiresToken || quiz.examMode.equals("PROCTORED_CBT", ignoreCase = true)
            if (needsExamToken) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = cardBg,
                    border = BorderStroke(1.dp, cardBorder),
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Lock, null, tint = warningColor, modifier = Modifier.size(15.dp))
                            Text("TOKEN UJIAN CBT PROCTORED", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = warningColor)
                        }
                        Text(
                            text = "Minta token resmi dari pengawas sebelum membuka lembar soal ujian.",
                            fontSize = 12.sp,
                            color = textSecondary,
                        )
                        Surface(
                            onClick = { if (isAvailable && !state.isStarting) onOpenTokenSheet() },
                            enabled = isAvailable && !state.isStarting,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = innerCardBg,
                            border = BorderStroke(1.dp, if (tokenInput.isNotBlank()) warningColor.copy(alpha = 0.65f) else innerCardBorder),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    text = when {
                                        !isAvailable && phase == "UPCOMING" -> "Menunggu jadwal ujian dimulai..."
                                        !isAvailable && phase == "ENDED" -> "Sesi ujian telah berakhir"
                                        tokenInput.isBlank() -> "Ketuk untuk memasukkan token..."
                                        else -> "Token siap: $tokenInput"
                                    },
                                    fontSize = 13.sp,
                                    fontWeight = if (tokenInput.isBlank()) FontWeight.Medium else FontWeight.Bold,
                                    color = if (tokenInput.isBlank()) textTertiary else textPrimary,
                                )
                                Icon(Icons.Default.Edit, null, tint = warningColor, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            val canStartAttempt = !state.isStarting && isAvailable
            val isResuming = quiz.studentAttemptStatus.equals("in_progress", ignoreCase = true)
            Button(
                onClick = {
                    if (needsExamToken) onOpenTokenSheet() else onStartExamWithoutToken()
                },
                enabled = canStartAttempt,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, disabledContainerColor = Color.Transparent),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (canStartAttempt) Brush.horizontalGradient(listOf(StudentNeon, Color(0xFF00B4D8)))
                        else Brush.horizontalGradient(listOf(innerCardBg, innerCardBg))
                    )
                    .border(1.dp, if (canStartAttempt) studentAccent else innerCardBorder, RoundedCornerShape(14.dp)),
            ) {
                if (state.isStarting) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Menyiapkan Soal...", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                } else {
                    Text(
                        text = when {
                            isAvailable && isResuming -> "LANJUTKAN UJIAN"
                            isAvailable && needsExamToken -> "MASUKKAN TOKEN & MULAI"
                            isAvailable -> "MULAI KERJAKAN"
                            phase == "UPCOMING" -> "BELUM MEMASUKI JAM MULAI"
                            phase == "ENDED" -> "SESI UJIAN BERAKHIR"
                            else -> "BELUM DIBUKA"
                        },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        color = if (canStartAttempt) Color.White else textTertiary,
                        letterSpacing = 0.5.sp,
                    )
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ExamScheduleControlCard(
    quiz: Quiz,
    isTeacher: Boolean,
    nowMs: Long,
    onEditSchedule: () -> Unit,
    onExtendTime15: () -> Unit,
) {
    val isDark = LocalIsDarkTheme.current
    val cardBg = if (isDark) Color(0xFF111113) else Color.White
    val textPrimary = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFFA1A1AA) else Color(0xFF64748B)
    val phase = quiz.effectivePhase
    val phaseColor = when (phase) {
        "LIVE" -> TeacherNeon
        "UPCOMING" -> NeonWarning
        "ENDED" -> NeonError
        else -> NeonBlue
    }

    val window = formatExamWindowWib(quiz.startAt, quiz.endAt, quiz.releaseAt)
    val countdown = when (phase) {
        "UPCOMING" -> computeSecondsUntilStart(quiz.startAt, quiz.releaseAt, quiz.secondsUntilStart, nowMs)?.let { "Dimulai dalam ${formatCountdownHuman(it)}" }
        "LIVE" -> computeSecondsRemaining(quiz.endAt, quiz.secondsRemaining, nowMs)?.let { "Sisa waktu: ${formatCountdownHuman(it)}" }
        "ENDED" -> "Waktu pelaksanaan telah selesai"
        else -> null
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = cardBg,
        border = BorderStroke(1.dp, phaseColor.copy(alpha = 0.35f)),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Rounded.CalendarMonth, null, tint = phaseColor, modifier = Modifier.size(16.dp))
                    Text("JADWAL PELAKSANAAN CBT", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = textPrimary, letterSpacing = 0.4.sp)
                }
                ExamPhasePill(phase = phase)
            }

            if (window.hasWindow) {
                Text("Tanggal: ${window.dateLabel}", fontSize = 12.5.sp, color = textPrimary, fontWeight = FontWeight.SemiBold)
                Text("Jam Sesi: ${window.timeRangeLabel}", fontSize = 12.sp, color = textSecondary)
                quiz.startAt?.takeIf { it.isNotBlank() }?.let { start ->
                    Text("Dibuka: ${formatPublishTimestamp(start)}", fontSize = 11.sp, color = textSecondary)
                }
                quiz.endAt?.takeIf { it.isNotBlank() }?.let { end ->
                    Text("Ditutup: ${formatPublishTimestamp(end)}", fontSize = 11.sp, color = textSecondary)
                }
            } else {
                Text("Jadwal belum dikunci (mode fleksibel).", fontSize = 12.sp, color = textSecondary)
            }

            if (!countdown.isNullOrBlank()) {
                Surface(shape = RoundedCornerShape(8.dp), color = phaseColor.copy(alpha = 0.12f)) {
                    Text(
                        text = countdown,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = phaseColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    )
                }
            }

            if (isTeacher) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onEditSchedule,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, TeacherNeon.copy(alpha = 0.45f)),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.weight(1f).height(38.dp),
                    ) {
                        Icon(Icons.Rounded.EditCalendar, null, tint = TeacherNeon, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("Atur Jadwal", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = TeacherNeon)
                    }
                    OutlinedButton(
                        onClick = onExtendTime15,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, NeonBlue.copy(alpha = 0.45f)),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.weight(1f).height(38.dp),
                    ) {
                        Icon(Icons.Rounded.MoreTime, null, tint = NeonBlue, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("+15 Menit", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = NeonBlue)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CosmicQuizHeroCard(
    quiz: Quiz,
    isPrincipal: Boolean,
    isTeacher: Boolean,
    nowMs: Long,
) {
    val isDark = LocalIsDarkTheme.current
    val cardBg = if (isDark) Color(0xFF111113) else Color.White
    val cardBorder = if (isDark) Color(0xFF1F1F23) else Color(0xFFE2E8F0)
    val innerCardBg = if (isDark) Color(0xFF161618) else Color(0xFFF8FAFC)
    val innerCardBorder = if (isDark) Color(0xFF27272A) else Color(0xFFE2E8F0)
    val textPrimary = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFFA1A1AA) else Color(0xFF64748B)
    val teacherAccent = if (isDark) Color(0xFF10B981) else Color(0xFF059669)
    val studentAccent = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)

    val roleAccent = if (isTeacher) teacherAccent else studentAccent
    val isProctored = quiz.requiresToken || quiz.examMode.equals("PROCTORED_CBT", ignoreCase = true)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = roleAccent.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, roleAccent.copy(alpha = 0.35f)),
                ) {
                    val potentialXp = if (quiz.maxScore > 0) quiz.maxScore * 5 else 100
                    Text(
                        text = when {
                            isPrincipal -> "Kepala Sekolah • Proktor Utama"
                            isTeacher -> "Pendidik / Tutor Pengampu"
                            else -> "Potensi $potentialXp XP"
                        },
                        color = roleAccent,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                    )
                }

                Surface(
                    modifier = Modifier.weight(1f, fill = false).padding(start = 8.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = innerCardBg,
                    border = BorderStroke(1.dp, innerCardBorder),
                ) {
                    Text(
                        text = quiz.subjectName ?: "CBT Evaluasi",
                        color = textSecondary,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                    )
                }
            }

            Text(
                text = quiz.title,
                fontSize = 17.5.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary,
                lineHeight = 23.sp,
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                quiz.effectiveDisplayClass?.let { cls ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isDark) Color(0xFF1E293B) else Color(0xFFE0F2FE),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(Icons.Rounded.Groups, null, tint = NeonBlue, modifier = Modifier.size(13.dp))
                            Text(cls, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NeonBlue)
                        }
                    }
                }
                quiz.teacherName?.takeIf { it.isNotBlank() }?.let { tName ->
                    Surface(shape = RoundedCornerShape(8.dp), color = innerCardBg, border = BorderStroke(1.dp, innerCardBorder)) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(Icons.Rounded.Person, null, tint = textSecondary, modifier = Modifier.size(13.dp))
                            Text(tName, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = textPrimary)
                        }
                    }
                }
                formatAssessmentCategoryLabel(quiz.assessmentCategory)?.let { cat ->
                    Surface(shape = RoundedCornerShape(8.dp), color = innerCardBg, border = BorderStroke(1.dp, innerCardBorder)) {
                        Text(cat, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, color = textSecondary, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                    }
                }
                ExamProctoredModeChip(isProctored = isProctored)
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
    val isDark = LocalIsDarkTheme.current
    val innerCardBg = if (isDark) Color(0xFF161618) else Color(0xFFF8FAFC)
    val innerCardBorder = if (isDark) Color(0xFF27272A) else Color(0xFFE2E8F0)
    val textPrimary = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A)
    val textTertiary = if (isDark) Color(0xFF71717A) else Color(0xFF94A3B8)

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = innerCardBg,
        border = BorderStroke(1.dp, innerCardBorder),
    ) {
        Column(
            modifier = Modifier.padding(vertical = 11.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Box(
                modifier = Modifier.size(30.dp).clip(CircleShape).background(accentColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(15.dp))
            }
            Text(value, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = textPrimary)
            Text("$unit • $label", fontSize = 9.5.sp, fontWeight = FontWeight.SemiBold, color = textTertiary, maxLines = 1)
        }
    }
}

@Composable
private fun CbtRulesCard() {
    val isDark = LocalIsDarkTheme.current
    val cardBg = if (isDark) Color(0xFF111113) else Color.White
    val cardBorder = if (isDark) Color(0xFF1F1F23) else Color(0xFFE2E8F0)
    val warningColor = if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.Shield, contentDescription = null, tint = warningColor, modifier = Modifier.size(16.dp))
                Text("Tata Tertib & Pengawasan CBT", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = warningColor)
            }
            RuleBulletItem("Pastikan koneksi internet stabil sebelum menekan tombol Mulai.")
            RuleBulletItem("Dilarang keluar aplikasi atau membuka jendela lain saat ujian berlangsung.")
            RuleBulletItem("Timer berjalan otomatis dan jawaban langsung tersimpan secara aman.")
        }
    }
}

@Composable
private fun RuleBulletItem(text: String) {
    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("•", fontSize = 12.sp, color = TextTertiary, fontWeight = FontWeight.Bold)
        Text(text, fontSize = 11.5.sp, color = TextSecondary, lineHeight = 16.sp)
    }
}

@Composable
fun TeacherGradeQuizDialog(
    attempt: QuizAttempt,
    maxScore: Int,
    onDismiss: () -> Unit,
    onConfirm: (score: Int, feedback: String?) -> Unit,
    onViewAttemptDetail: (() -> Unit)? = null,
) {
    val isDark = LocalIsDarkTheme.current
    val dialogBg = if (isDark) Color(0xFF111113) else Color.White
    val textPrimary = if (isDark) Color(0xFFF1F5F9) else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    val textTertiary = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)
    val inputBg = if (isDark) Color(0xFF1B1B1E) else Color(0xFFF8FAFC)
    val inputBorder = if (isDark) Color(0xFF2E2E32) else Color(0xFFE2E8F0)

    val effectiveMax = if (maxScore > 0) maxScore else (attempt.totalPoints.takeIf { it > 0 } ?: 100)
    var scoreText by remember { mutableStateOf(attempt.score?.toString() ?: "") }
    var feedbackText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = dialogBg,
        shape = RoundedCornerShape(22.dp),
        title = {
            Column {
                Text("Koreksi Hasil Ujian CBT", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                Text("${attempt.studentName ?: "Siswa"} (NISN: ${attempt.studentNisn ?: "-"})", fontSize = 12.sp, color = textSecondary)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (onViewAttemptDetail != null) {
                    OutlinedButton(
                        onClick = onViewAttemptDetail,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonBlue),
                        modifier = Modifier.fillMaxWidth().height(36.dp),
                    ) {
                        Icon(Icons.AutoMirrored.Filled.List, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Buka Lembar Jawaban Siswa", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                OutlinedTextField(
                    value = scoreText,
                    onValueChange = { input ->
                        if (input.isEmpty() || input.all { it.isDigit() }) {
                            scoreText = input
                            errorMessage = null
                        }
                    },
                    label = { Text("Nilai Skor (0 - $effectiveMax)", fontSize = 12.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = inputBg,
                        unfocusedContainerColor = inputBg,
                        focusedBorderColor = TeacherNeon,
                        unfocusedBorderColor = inputBorder,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val kktpScore = (effectiveMax * 0.75).toInt()
                    val halfScore = effectiveMax / 2
                    listOf("Penuh" to effectiveMax, "KKTP" to kktpScore, "50%" to halfScore, "0" to 0).forEach { (label, value) ->
                        val isSel = scoreText == value.toString()
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) TeacherNeon.copy(alpha = 0.16f) else inputBg)
                                .border(1.dp, if (isSel) TeacherNeon else inputBorder, RoundedCornerShape(8.dp))
                                .clickable { scoreText = value.toString(); errorMessage = null }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isSel) TeacherNeon else textSecondary)
                        }
                    }
                }
                if (errorMessage != null) {
                    Text(errorMessage!!, fontSize = 11.sp, color = NeonError)
                }
                OutlinedTextField(
                    value = feedbackText,
                    onValueChange = { feedbackText = it },
                    label = { Text("Umpan Balik Guru (Opsional)", fontSize = 12.sp) },
                    minLines = 2,
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = inputBg,
                        unfocusedContainerColor = inputBg,
                        focusedBorderColor = TeacherNeon,
                        unfocusedBorderColor = inputBorder,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsed = scoreText.toIntOrNull()
                    if (parsed == null || parsed < 0 || parsed > effectiveMax) {
                        errorMessage = "Masukkan skor valid antara 0 dan $effectiveMax"
                    } else {
                        onConfirm(parsed, feedbackText.takeIf { it.isNotBlank() })
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = TeacherNeon, contentColor = Color.White),
                shape = RoundedCornerShape(10.dp),
            ) {
                Text("Simpan Nilai", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal", color = textTertiary, fontSize = 12.sp)
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CbtTokenInputBottomSheet(
    quizTitle: String,
    tokenInput: String,
    onTokenChange: (String) -> Unit,
    isStarting: Boolean,
    errorMessage: String?,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit,
) {
    val isDark = LocalIsDarkTheme.current
    val sheetBg = if (isDark) Color(0xFF111113) else Color.White
    val innerBg = if (isDark) Color(0xFF161618) else Color(0xFFF8FAFC)
    val innerBorder = if (isDark) Color(0xFF27272A) else Color(0xFFE2E8F0)
    val textPrimary = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFFA1A1AA) else Color(0xFF64748B)
    val textTertiary = if (isDark) Color(0xFF71717A) else Color(0xFF94A3B8)
    val warningColor = if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706)

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        delay(120)
        try {
            focusRequester.requestFocus()
            keyboardController?.show()
        } catch (_: Exception) {}
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = sheetBg,
        contentColor = textPrimary,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = warningColor.copy(alpha = 0.14f),
                border = BorderStroke(1.dp, warningColor.copy(alpha = 0.35f)),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Icon(Icons.Default.Lock, null, tint = warningColor, modifier = Modifier.size(13.dp))
                    Text("TOKEN UJIAN CBT PROCTORED", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = warningColor)
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Masukkan Token Ujian", fontSize = 17.sp, fontWeight = FontWeight.Black, color = textPrimary)
                Text(quizTitle, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = textSecondary)
                Text("Minta token resmi dari pengawas untuk membuka soal.", fontSize = 12.sp, color = textTertiary)
            }

            OutlinedTextField(
                value = tokenInput,
                onValueChange = onTokenChange,
                placeholder = { Text("Contoh: GSJHYP", color = textTertiary, fontSize = 14.sp) },
                singleLine = true,
                enabled = !isStarting,
                textStyle = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Black, letterSpacing = 2.5.sp, color = textPrimary),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                    keyboardType = KeyboardType.Ascii,
                    imeAction = ImeAction.Done,
                    autoCorrectEnabled = false,
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (tokenInput.isNotBlank() && !isStarting) {
                            keyboardController?.hide()
                            onSubmit()
                        }
                    },
                ),
                leadingIcon = { Icon(Icons.Default.Lock, null, tint = warningColor, modifier = Modifier.size(18.dp)) },
                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = innerBg,
                    unfocusedContainerColor = innerBg,
                    focusedBorderColor = warningColor,
                    unfocusedBorderColor = innerBorder,
                ),
                shape = RoundedCornerShape(14.dp),
            )

            if (!errorMessage.isNullOrBlank()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(NeonError.copy(alpha = 0.12f))
                        .border(1.dp, NeonError.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(Icons.Default.ErrorOutline, null, tint = NeonError, modifier = Modifier.size(18.dp))
                    Text(errorMessage, color = NeonError, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                }
            }

            val canSubmit = tokenInput.isNotBlank() && !isStarting
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    enabled = !isStarting,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("Batal", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = { keyboardController?.hide(); onSubmit() },
                    enabled = canSubmit,
                    modifier = Modifier.weight(1.6f).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = StudentNeon, contentColor = Color.White),
                ) {
                    if (isStarting) {
                        CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                    } else {
                        Text("Verifikasi & Mulai", fontSize = 13.5.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}
