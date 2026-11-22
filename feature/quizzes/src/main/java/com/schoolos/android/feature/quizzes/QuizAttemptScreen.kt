package com.schoolos.android.feature.quizzes

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.CheckCircle
import com.schoolos.android.core.designsystem.QuestionPromptWithDiagram
import com.schoolos.android.core.designsystem.SchoolOsConfirmationDialog
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.schoolos.android.core.R as CoreR
import com.schoolos.android.core.designsystem.*
import kotlinx.coroutines.delay

private val ChoiceLabels = listOf("A", "B", "C", "D", "E")

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun tryEngageExamLockTask(activity: Activity?) {
    if (activity == null) return
    try {
        val am = activity.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        if (am?.lockTaskModeState == ActivityManager.LOCK_TASK_MODE_NONE) {
            activity.startLockTask()
        }
    } catch (_: Throwable) {
    }
}

private fun tryReleaseExamLockTask(activity: Activity?) {
    if (activity == null) return
    try {
        activity.stopLockTask()
    } catch (_: Throwable) {
    }
}

@Composable
fun QuizAttemptScreen(
    onBack: () -> Unit = {},
    onSubmitted: (attemptId: String, score: Int, totalPoints: Int) -> Unit = { _, _, _ -> },
    viewModel: QuizAttemptViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    var showConfirm by remember { mutableStateOf(false) }

    val xpReward by viewModel.xpRewardEvent.collectAsState()
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val lifecycleOwner = LocalLifecycleOwner.current

    val isExamLocked = !state.isLoading && state.questions.isNotEmpty() && !state.submitSuccess
    var hasShownStartToast by remember { mutableStateOf(false) }
    var examLockStartedAtMs by remember { mutableLongStateOf(0L) }
    var leaveViolationCount by remember { mutableIntStateOf(0) }
    var showLeaveViolationDialog by remember { mutableStateOf(false) }

    val showBlockedExitToast = remember(context) {
        {
            Toast.makeText(
                context,
                "Ujian sedang dikerjakan! Dilarang keluar dari aplikasi sebelum jawaban dikumpulkan.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // Immediate notice toast when the exam session starts + periodic proctor heartbeat
    LaunchedEffect(isExamLocked) {
        if (!isExamLocked) return@LaunchedEffect
        if (!hasShownStartToast) {
            hasShownStartToast = true
            Toast.makeText(
                context,
                "Ujian dimulai! Selama mengerjakan dilarang keluar dari aplikasi sebelum mengumpulkan jawaban.",
                Toast.LENGTH_LONG
            ).show()
        }
        viewModel.sendProctorEvent("HEARTBEAT", "Siswa masuk layar ujian Android CBT")
        while (true) {
            delay(25_000L)
            viewModel.sendProctorEvent("HEARTBEAT", "Siswa aktif di aplikasi Android CBT")
        }
    }

    // Hard-block Android Back button & swipe gesture while exam is active
    BackHandler(enabled = isExamLocked) {
        showBlockedExitToast()
    }

    // Enforce FLAG_SECURE + Screen Pinning while exam is active; release cleanly on finish/dispose
    DisposableEffect(isExamLocked, activity) {
        if (isExamLocked && activity != null) {
            examLockStartedAtMs = System.currentTimeMillis()
            activity.window?.addFlags(
                WindowManager.LayoutParams.FLAG_SECURE or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
            tryEngageExamLockTask(activity)
        }
        onDispose {
            activity?.window?.clearFlags(
                WindowManager.LayoutParams.FLAG_SECURE or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
            tryReleaseExamLockTask(activity)
        }
    }

    // Detect & warn immediately if student switches away from or backgrounds the exam app
    DisposableEffect(lifecycleOwner, isExamLocked, state.isSubmitting) {
        val observer = LifecycleEventObserver { _, event ->
            if (!isExamLocked) return@LifecycleEventObserver
            when (event) {
                Lifecycle.Event.ON_STOP -> {
                    val elapsedSinceLock = System.currentTimeMillis() - examLockStartedAtMs
                    if (!state.isSubmitting && elapsedSinceLock > 2500L) {
                        leaveViolationCount += 1
                        showLeaveViolationDialog = true
                        viewModel.sendProctorEvent(
                            "APP_BACKGROUND",
                            "Siswa keluar / meminimalkan aplikasi Android (${leaveViolationCount}x)"
                        )
                        Toast.makeText(
                            context,
                            "PERINGATAN UJIAN ($leaveViolationCount x): Dilarang keluar dari aplikasi saat ujian berlangsung!",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
                Lifecycle.Event.ON_RESUME -> {
                    if (leaveViolationCount > 0) {
                        Toast.makeText(
                            context,
                            "Dilarang meninggalkan layar ujian! Pelanggaran tercatat: ${leaveViolationCount}x.",
                            Toast.LENGTH_LONG
                        ).show()
                        tryEngageExamLockTask(activity)
                    }
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
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

    LaunchedEffect(state.submitSuccess) {
        if (state.submitSuccess && state.resultAttempt != null && xpReward == null) {
            val attempt = state.resultAttempt!!
            onSubmitted(attempt.id, attempt.score ?: 0, attempt.totalPoints)
        }
    }

    when {
        state.isLoading -> LoadingState()
        state.questions.isEmpty() && !state.isLoading -> {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(CosmicBlack),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        painter = painterResource(id = CoreR.drawable.ic_modern_quiz),
                        contentDescription = null,
                        tint = textTertiary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text("Tidak ada soal tersedia pada kuis ini", color = textSecondary, fontSize = 14.sp)
                }
            }
        }
        else -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(CosmicBlack)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {

                    // ── 1. TOP BAR ──────────────────────────────────────────
                    QuizTopBar(
                        currentIndex = state.currentIndex,
                        totalQuestions = state.questions.size,
                        progress = state.progress,
                        answeredCount = state.answeredCount,
                        isExamLocked = isExamLocked,
                        leaveViolationCount = leaveViolationCount,
                        onClose = {
                            if (isExamLocked) {
                                showBlockedExitToast()
                            } else {
                                onBack()
                            }
                        },
                        isDark = isDark,
                        cardBg = cardBg,
                        cardBorder = cardBorder,
                        innerCardBg = innerCardBg,
                        innerCardBorder = innerCardBorder,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        studentAccent = studentAccent,
                    )

                    // ── 2. QUESTION NAVIGATOR PILLS ──────────────────────────
                    LazyRow(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        itemsIndexed(state.questions) { index, question ->
                            val isAnswered = isAndroidQuestionAnswerComplete(question, state.answers[question.id])
                            val isCurrent = index == state.currentIndex
                            val pillBg = when {
                                isCurrent  -> studentAccent
                                isAnswered -> if (isDark) Color(0xFF064E3B) else Color(0xFFDCFCE7)
                                else       -> innerCardBg
                            }
                            val pillBorder = when {
                                isCurrent  -> studentAccent
                                isAnswered -> if (isDark) Color(0xFF10B981).copy(alpha = 0.5f) else Color(0xFF86EFAC)
                                else       -> innerCardBorder
                            }
                            val pillTextColor = when {
                                isCurrent  -> Color.White
                                isAnswered -> if (isDark) Color(0xFF34D399) else Color(0xFF15803D)
                                else       -> textTertiary
                            }

                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(pillBg)
                                    .border(1.dp, pillBorder, RoundedCornerShape(10.dp))
                                    .clickable { viewModel.goToQuestion(index) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    "${index + 1}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = pillTextColor,
                                )
                            }
                        }
                    }

                    // ── 3. QUESTION CONTENT (animated slide) ────────────────
                    val question = state.currentQuestion
                    if (question != null) {
                        AnimatedContent(
                            targetState = state.currentIndex,
                            transitionSpec = {
                                if (targetState > initialState) {
                                    (slideInHorizontally { it } + fadeIn()).togetherWith(slideOutHorizontally { -it } + fadeOut())
                                } else {
                                    (slideInHorizontally { -it } + fadeIn()).togetherWith(slideOutHorizontally { it } + fadeOut())
                                }
                            },
                            label = "questionSlide",
                            modifier = Modifier.weight(1f),
                        ) { _ ->
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                            ) {
                                // Question Card
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(22.dp),
                                    color = cardBg,
                                    border = BorderStroke(1.dp, cardBorder),
                                    shadowElevation = 0.dp,
                                ) {
                                    Column(
                                        modifier = Modifier.padding(18.dp),
                                        verticalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        // Header Row: Soal Number + Type Badge + Points Pill
                                        val qVariatifType = normalizeAndroidQuestionType(question.questionType)
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = studentAccent.copy(alpha = 0.12f),
                                                    border = BorderStroke(1.dp, studentAccent.copy(alpha = 0.35f))
                                                ) {
                                                    Text(
                                                        text = "SOAL NO. ${state.currentIndex + 1}",
                                                        color = studentAccent,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        letterSpacing = 0.5.sp,
                                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                                    )
                                                }
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = innerCardBg,
                                                    border = BorderStroke(1.dp, innerCardBorder)
                                                ) {
                                                    Text(
                                                        text = androidQuestionTypeLabel(qVariatifType),
                                                        color = textSecondary,
                                                        fontSize = 10.5.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                    )
                                                }
                                            }

                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = innerCardBg,
                                                border = BorderStroke(1.dp, innerCardBorder)
                                            ) {
                                                Text(
                                                    text = "+${question.points} Poin",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isDark) Color(0xFF34D399) else Color(0xFF16A34A),
                                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                                )
                                            }
                                        }

                                        // Diagram / Image + Question stem text
                                        QuestionPromptWithDiagram(
                                            rawText = question.questionText,
                                            explicitImageUrl = question.imageUrl,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = textPrimary,
                                            lineHeight = 22.sp,
                                        )
                                    }
                                }

                                Spacer(Modifier.height(14.dp))

                                // Answer Options (All 7 Variatif Formats)
                                when (normalizeAndroidQuestionType(question.questionType)) {
                                    AndroidVariatifQuestionType.MULTIPLE_CHOICE -> {
                                        question.choices.forEachIndexed { idx, choice ->
                                            val choiceKey = "choice_${choice.id}"
                                            val isSelected = state.answers[question.id] == choiceKey
                                            val label = ChoiceLabels.getOrElse(idx) { "${idx + 1}" }

                                            ModernAnswerBubble(
                                                label = label,
                                                text = choice.choiceText,
                                                isSelected = isSelected,
                                                onClick = { viewModel.selectAnswer(question.id, choiceKey) },
                                                isDark = isDark,
                                                cardBg = cardBg,
                                                cardBorder = cardBorder,
                                                innerCardBg = innerCardBg,
                                                innerCardBorder = innerCardBorder,
                                                textPrimary = textPrimary,
                                                studentAccent = studentAccent,
                                            )
                                            Spacer(Modifier.height(10.dp))
                                        }
                                    }
                                    AndroidVariatifQuestionType.TRUE_FALSE_MATRIX -> {
                                        TrueFalseMatrixSection(
                                            question = question,
                                            rawAnswer = state.answers[question.id],
                                            onAnswerChange = { viewModel.setAnswer(question.id, it) },
                                            isDark = isDark,
                                            cardBg = cardBg,
                                            cardBorder = cardBorder,
                                            innerCardBg = innerCardBg,
                                            textPrimary = textPrimary,
                                            textSecondary = textSecondary,
                                            studentAccent = studentAccent,
                                        )
                                    }
                                    AndroidVariatifQuestionType.MULTIPLE_SELECT -> {
                                        MultipleSelectSection(
                                            question = question,
                                            rawAnswer = state.answers[question.id],
                                            onAnswerChange = { viewModel.setAnswer(question.id, it) },
                                            isDark = isDark,
                                            cardBg = cardBg,
                                            cardBorder = cardBorder,
                                            textPrimary = textPrimary,
                                            textSecondary = textSecondary,
                                        )
                                    }
                                    AndroidVariatifQuestionType.SHORT_ANSWER -> {
                                        ShortAnswerSection(
                                            question = question,
                                            rawAnswer = state.answers[question.id],
                                            onAnswerChange = { viewModel.setAnswer(question.id, it.ifBlank { null }) },
                                            cardBg = cardBg,
                                            cardBorder = cardBorder,
                                            textPrimary = textPrimary,
                                            textTertiary = textTertiary,
                                            studentAccent = studentAccent,
                                        )
                                    }
                                    AndroidVariatifQuestionType.MATCHING -> {
                                        MatchingSection(
                                            question = question,
                                            rawAnswer = state.answers[question.id],
                                            onAnswerChange = { viewModel.setAnswer(question.id, it) },
                                            isDark = isDark,
                                            cardBg = cardBg,
                                            cardBorder = cardBorder,
                                            innerCardBg = innerCardBg,
                                            textPrimary = textPrimary,
                                            textSecondary = textSecondary,
                                            studentAccent = studentAccent,
                                        )
                                    }
                                    AndroidVariatifQuestionType.ORDERING -> {
                                        OrderingSection(
                                            question = question,
                                            rawAnswer = state.answers[question.id],
                                            onAnswerChange = { viewModel.setAnswer(question.id, it) },
                                            isDark = isDark,
                                            cardBg = cardBg,
                                            cardBorder = cardBorder,
                                            textPrimary = textPrimary,
                                            textSecondary = textSecondary,
                                        )
                                    }
                                    AndroidVariatifQuestionType.ESSAY -> {
                                        val text = state.answers[question.id] ?: ""
                                        OutlinedTextField(
                                            value = text,
                                            onValueChange = { viewModel.setEssayAnswer(question.id, it) },
                                            placeholder = { Text("Tulis jawaban lengkap kamu di sini...", color = textTertiary, fontSize = 13.sp) },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(180.dp),
                                            maxLines = 8,
                                            shape = RoundedCornerShape(18.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedContainerColor = cardBg,
                                                unfocusedContainerColor = cardBg,
                                                focusedBorderColor = studentAccent,
                                                unfocusedBorderColor = cardBorder,
                                                focusedTextColor = textPrimary,
                                                unfocusedTextColor = textPrimary,
                                            ),
                                        )
                                    }
                                }

                                Spacer(Modifier.height(24.dp))
                            }
                        }
                    }

                    // ── 4. BOTTOM NAV DOCK ────────────────────────────────────
                    QuizBottomBar(
                        currentIndex = state.currentIndex,
                        isLastQuestion = state.isLastQuestion,
                        isSubmitting = state.isSubmitting,
                        submitError = state.submitError,
                        onPrevious = viewModel::previousQuestion,
                        onNext = viewModel::nextQuestion,
                        onShowConfirm = { showConfirm = true },
                        isDark = isDark,
                        cardBg = cardBg,
                        cardBorder = cardBorder,
                        innerCardBg = innerCardBg,
                        innerCardBorder = innerCardBorder,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        studentAccent = studentAccent,
                    )
                }
            }
        }
    }

    // Confirmation Dialog before final submit
    if (showConfirm) {
        val unanswered = state.questions.size - state.answeredCount
        val confirmMsg = if (unanswered > 0) {
            "Kamu telah menjawab ${state.answeredCount} dari ${state.questions.size} soal. Masih ada $unanswered soal yang belum dijawab. Yakin ingin mengumpulkan sekarang?"
        } else {
            "Seluruh ${state.questions.size} soal telah terjawab. Pastikan kembali jawabanmu sebelum mengakhiri sesi ujian CBT."
        }
        SchoolOsConfirmationDialog(
            title = "Selesai & Kumpulkan Ujian?",
            message = confirmMsg,
            confirmText = "Ya, Kumpulkan",
            dismissText = "Periksa Lagi",
            icon = Icons.Outlined.CheckCircle,
            accentColor = if (unanswered > 0) Color(0xFFF59E0B) else Color(0xFF10B981),
            confirmButtonColor = if (isDark) Color(0xFF10B981) else Color(0xFF059669),
            onConfirm = {
                showConfirm = false
                viewModel.submit()
            },
            onDismiss = { showConfirm = false }
        )
    }

    if (state.submitError != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearSubmitError() },
            containerColor = cardBg,
            shape = RoundedCornerShape(22.dp),
            title = {
                Text("Gagal Mengumpulkan Ujian", color = if (isDark) Color(0xFFF87171) else Color(0xFFDC2626), fontWeight = FontWeight.Bold)
            },
            text = {
                Text(state.submitError ?: "", color = textSecondary, fontSize = 13.sp)
            },
            confirmButton = {
                TextButton(onClick = { viewModel.clearSubmitError() }) {
                    Text("Tutup", color = studentAccent, fontWeight = FontWeight.Bold)
                }
            },
        )
    }

    if (showLeaveViolationDialog && isExamLocked) {
        AlertDialog(
            onDismissRequest = {
                showLeaveViolationDialog = false
                tryEngageExamLockTask(activity)
            },
            containerColor = cardBg,
            shape = RoundedCornerShape(22.dp),
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFF59E0B),
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    "Dilarang Keluar Aplikasi!",
                    color = if (isDark) Color(0xFFFBBF24) else Color(0xFFB45309),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    "Anda terdeteksi meninggalkan layar aplikasi sebanyak ${leaveViolationCount}x saat ujian sedang berlangsung. Tetap berada di dalam aplikasi hingga jawaban dikumpulkan.",
                    color = textSecondary,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLeaveViolationDialog = false
                        tryEngageExamLockTask(activity)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = studentAccent, contentColor = Color.White),
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Text("Kembali ke Ujian", fontWeight = FontWeight.Bold)
                }
            },
        )
    }

    com.schoolos.android.core.designsystem.XpCelebrationDialog(
        reward = xpReward,
        onDismiss = {
            viewModel.clearXpReward()
            val attempt = state.resultAttempt
            if (attempt != null) {
                onSubmitted(attempt.id, attempt.score ?: 0, attempt.totalPoints)
            }
        },
    )
}

@Composable
private fun QuizTopBar(
    currentIndex: Int,
    totalQuestions: Int,
    progress: Float,
    answeredCount: Int,
    isExamLocked: Boolean,
    leaveViolationCount: Int,
    onClose: () -> Unit,
    isDark: Boolean,
    cardBg: Color,
    cardBorder: Color,
    innerCardBg: Color,
    innerCardBorder: Color,
    textPrimary: Color,
    textSecondary: Color,
    studentAccent: Color,
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(400, easing = FastOutSlowInEasing),
        label = "progressAnim",
    )
    val lockAmber = if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(cardBg)
            .statusBarsPadding()
            .border(width = 1.dp, color = cardBorder, shape = RoundedCornerShape(0.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onClose, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = if (isExamLocked) Icons.Default.Lock else Icons.Default.Close,
                    contentDescription = if (isExamLocked) "Ujian Terkunci" else "Keluar",
                    tint = if (isExamLocked) lockAmber else textSecondary,
                    modifier = Modifier.size(19.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Soal ${currentIndex + 1} / $totalQuestions",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary,
                )
                Text(
                    text = "$answeredCount terjawab",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = studentAccent,
                )
            }

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (isExamLocked) lockAmber.copy(alpha = 0.14f) else studentAccent.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, if (isExamLocked) lockAmber.copy(alpha = 0.45f) else studentAccent.copy(alpha = 0.35f))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                ) {
                    if (isExamLocked) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = lockAmber,
                            modifier = Modifier.size(11.dp),
                        )
                    }
                    Text(
                        text = if (isExamLocked) "TERKUNCI" else "CBT",
                        fontSize = 10.5.sp,
                        color = if (isExamLocked) lockAmber else studentAccent,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }

        if (isExamLocked) {
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(lockAmber.copy(alpha = 0.10f))
                    .border(0.5.dp, lockAmber.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = if (leaveViolationCount > 0) {
                        "Dilarang keluar aplikasi • Peringatan keluar: ${leaveViolationCount}x"
                    } else {
                        "Mode Anti-Keluar Aktif • Selesaikan & kumpulkan ujian sebelum keluar"
                    },
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = lockAmber,
                    textAlign = TextAlign.Center,
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // Progress bar track
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(CircleShape)
                .background(innerCardBorder)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(studentAccent)
            )
        }
    }
}

@Composable
private fun ModernAnswerBubble(
    label: String,
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    isDark: Boolean,
    cardBg: Color,
    cardBorder: Color,
    innerCardBg: Color,
    innerCardBorder: Color,
    textPrimary: Color,
    studentAccent: Color,
) {
    val targetBg = if (isSelected) {
        if (isDark) studentAccent.copy(alpha = 0.18f) else studentAccent.copy(alpha = 0.12f)
    } else {
        cardBg
    }

    val targetBorder = if (isSelected) {
        studentAccent
    } else {
        cardBorder
    }

    val targetTextColor = if (isSelected) {
        studentAccent
    } else {
        textPrimary
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = targetBg,
        border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, targetBorder),
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Letter badge circle
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) studentAccent else innerCardBg)
                    .border(1.dp, if (isSelected) studentAccent else innerCardBorder, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.5.sp,
                    color = if (isSelected) Color.White else targetTextColor,
                )
            }

            Spacer(Modifier.width(12.dp))

            Text(
                text = text,
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = targetTextColor,
                lineHeight = 20.sp,
                modifier = Modifier.weight(1f),
            )

            if (isSelected) {
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(studentAccent),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun QuizBottomBar(
    currentIndex: Int,
    isLastQuestion: Boolean,
    isSubmitting: Boolean,
    submitError: String?,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onShowConfirm: () -> Unit,
    isDark: Boolean,
    cardBg: Color,
    cardBorder: Color,
    innerCardBg: Color,
    innerCardBorder: Color,
    textPrimary: Color,
    textSecondary: Color,
    studentAccent: Color,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(width = 1.dp, color = cardBorder, shape = RoundedCornerShape(0.dp)),
        color = cardBg,
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            if (submitError != null) {
                Text(
                    text = submitError,
                    color = if (isDark) Color(0xFFF87171) else Color(0xFFDC2626),
                    fontSize = 11.5.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Previous Button
                Surface(
                    modifier = Modifier
                        .height(48.dp)
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable(enabled = currentIndex > 0, onClick = onPrevious),
                    shape = RoundedCornerShape(14.dp),
                    color = if (currentIndex > 0) innerCardBg else innerCardBg.copy(alpha = 0.4f),
                    border = BorderStroke(1.dp, innerCardBorder),
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Sebelumnya",
                            tint = if (currentIndex > 0) textPrimary else textSecondary.copy(alpha = 0.4f),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Sebelumnya",
                            fontWeight = FontWeight.SemiBold,
                            color = if (currentIndex > 0) textPrimary else textSecondary.copy(alpha = 0.4f),
                            fontSize = 13.sp
                        )
                    }
                }

                // Next or Submit Button
                Surface(
                    modifier = Modifier
                        .height(48.dp)
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable(enabled = !isSubmitting) {
                            if (isLastQuestion) onShowConfirm() else onNext()
                        },
                    shape = RoundedCornerShape(14.dp),
                    color = if (isLastQuestion) (if (isDark) Color(0xFF10B981) else Color(0xFF059669)) else studentAccent,
                    shadowElevation = 0.dp,
                ) {
                    if (isSubmitting) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = if (isLastQuestion) "Kumpulkan" else "Berikutnya",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 13.5.sp,
                            )
                            Spacer(Modifier.width(6.dp))
                            Icon(
                                imageVector = if (isLastQuestion) Icons.AutoMirrored.Filled.Send else Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
