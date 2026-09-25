package com.schoolos.android.feature.assignments

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import android.widget.Toast
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
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
import com.schoolos.android.core.designsystem.LoadingState
import com.schoolos.android.core.designsystem.NeonSuccess
import com.schoolos.android.core.designsystem.NeonWarning
import com.schoolos.android.core.designsystem.NeonError
import com.schoolos.android.core.designsystem.StatusChip
import com.schoolos.android.core.designsystem.TextPrimary
import com.schoolos.android.core.designsystem.TextSecondary
import com.schoolos.android.core.designsystem.TextTertiary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignmentDetailScreen(
    onBack: (() -> Unit)? = null,
    onOpenMaterial: (String) -> Unit = {},
    onAskTeacher: ((assignmentTitle: String, assignmentId: String, subjectName: String, teacherName: String) -> Unit)? = null,
    viewModel: AssignmentDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    var content by remember { mutableStateOf("") }
    var showConfirm by remember { mutableStateOf(false) }
    var pgAnswers by remember { mutableStateOf(emptyMap<String, String>()) }
    var essayAnswers by remember { mutableStateOf(emptyMap<String, String>()) }

    LaunchedEffect(state.submitSuccess) {
        if (state.submitSuccess) {
            Toast.makeText(context, "✓ Tugas berhasil dikumpulkan!", Toast.LENGTH_SHORT).show()
            content = ""
            pgAnswers = emptyMap()
            essayAnswers = emptyMap()
            viewModel.dismissSubmitSuccess()
        }
    }

    LaunchedEffect(state.submitError) {
        state.submitError?.let { err ->
            Toast.makeText(context, "Gagal mengumpulkan tugas: $err", Toast.LENGTH_LONG).show()
            viewModel.dismissSubmitError()
        }
    }

    Scaffold(
        containerColor = CosmicBlack,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            when {
                state.assignment != null -> {
                    val a = state.assignment!!
                    val isTeacher = com.schoolos.android.core.auth.isTeacherRole(state.userRole)
                    ExecutiveTopBar(
                        title = a.title,
                        subtitle = when {
                            isTeacher -> "Tugas & Penilaian • ${a.subjectName ?: "Mata Pelajaran"}"
                            else -> "Detail Tugas • ${a.subjectName ?: "Mata Pelajaran"}"
                        },
                        onBack = onBack,
                        actions = {
                            // Status chip in top bar actions area
                            StatusChip(label = a.status)
                        },
                    )
                }
                else -> ExecutiveTopBar(
                    title = "Detail Tugas",
                    onBack = onBack,
                )
            }
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                state.isLoading -> LoadingState()
                state.error != null -> ErrorState(message = state.error!!, onRetry = viewModel::load)
                state.assignment != null -> {
                    val a = state.assignment!!
                    val isTeacher = com.schoolos.android.core.auth.isTeacherRole(state.userRole)
                    val isParent = com.schoolos.android.core.auth.isParentRole(state.userRole)

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 12.dp,
                            bottom = 80.dp,
                        ),
                    ) {
                        // ── INFO STRIP ────────────────────────────────────
                        item {
                            AssignmentInfoStrip(
                                dueAt = a.dueAt,
                                subjectName = a.subjectName,
                                teacherName = a.teacherName,
                            )
                            Spacer(Modifier.height(14.dp))
                        }

                        // ── ROLE-SPECIFIC CONTENT ─────────────────────────
                        item {
                            if (isTeacher) {
                                TeacherAssignmentDetailContent(
                                    assignment = a,
                                    allSubmissions = state.allSubmissions,
                                    isGrading = state.isGrading,
                                    gradeSuccess = state.gradeSuccess,
                                    gradeError = state.gradeError,
                                    onGrade = { submissionId, score, feedback ->
                                        viewModel.gradeSubmission(submissionId, score, feedback)
                                    },
                                    onDismissGradeSuccess = viewModel::dismissGradeSuccess,
                                    onDismissGradeError = viewModel::dismissGradeError,
                                )
                            } else {
                                StudentAssignmentDetailContent(
                                    assignment = a,
                                    submission = state.submission,
                                    isParent = isParent,
                                    isSubmitting = state.isSubmitting,
                                    content = content,
                                    onContentChange = { content = it },
                                    onOpenMaterial = onOpenMaterial,
                                    onSubmitClick = { showConfirm = true },
                                    childName = state.childName,
                                    onAskTeacher = {
                                        onAskTeacher?.invoke(
                                            a.title,
                                            a.id,
                                            a.subjectName ?: "Tugas",
                                            a.teacherName ?: "Guru Pengampu",
                                        )
                                    },
                                    pgAnswers = pgAnswers,
                                    essayAnswers = essayAnswers,
                                    onPgAnswerSelected = { qId, choiceId ->
                                        pgAnswers = pgAnswers + (qId to choiceId)
                                    },
                                    onEssayAnswerChanged = { qId, text ->
                                        essayAnswers = essayAnswers + (qId to text)
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("Kumpulkan Tugas?") },
            text = { Text("Pastikan jawaban kamu sudah benar sebelum dikirimkan.") },
            confirmButton = {
                TextButton(onClick = {
                    showConfirm = false
                    viewModel.submit(
                        content = content,
                        pgAnswers = pgAnswers,
                        essayAnswers = essayAnswers,
                    )
                }) { Text("Kumpulkan") }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) { Text("Batal") }
            },
        )
    }
}

// ── ASSIGNMENT INFO STRIP ─────────────────────────────────────────────────────

@Composable
private fun AssignmentInfoStrip(
    dueAt: String?,
    subjectName: String?,
    teacherName: String?,
) {
    val dueInfo = dueDateInfo(dueAt)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CosmicNavy)
            .border(0.5.dp, GlassBorder, RoundedCornerShape(12.dp))
            .padding(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Subject + teacher column
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = subjectName?.ifBlank { "Mata Pelajaran" } ?: "Mata Pelajaran",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!teacherName.isNullOrBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = teacherName,
                        fontSize = 11.sp,
                        color = TextTertiary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            // Due date pill
            if (dueInfo != null) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(dueInfo.color.copy(alpha = 0.12f))
                        .border(0.5.dp, dueInfo.color.copy(alpha = 0.30f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = dueInfo.color,
                        modifier = Modifier.size(12.dp),
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = dueInfo.label,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = dueInfo.color,
                    )
                }
            }
        }
    }
}