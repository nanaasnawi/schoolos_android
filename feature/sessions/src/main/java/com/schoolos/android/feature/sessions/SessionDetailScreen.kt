package com.schoolos.android.feature.sessions

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.schoolos.android.core.auth.isPrincipalRole
import com.schoolos.android.core.auth.isTeacherRole
import com.schoolos.android.core.designsystem.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionDetailScreen(
    onBack: UnitCallback = {},
    onOpenAssignments: (String, String?) -> Unit = { _, _ -> },
    onOpenQuizzes: (String, String?) -> Unit = { _, _ -> },
    onOpenMaterials: (String, String?) -> Unit = { _, _ -> },
    viewModel: SessionDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.snackbarMessage) {
        state.snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    val session = state.session
    val rawSubject = session?.subjectName ?: session?.notes ?: "Pelajaran"
    val subject = rawSubject.substringBefore(" • ").substringBefore(" (Ruang").trim()
    val roomText = session?.let { listOfNotNull(it.room ?: "Ruang Kelas", it.className).joinToString(" • ") } ?: "Ruang Belajar"
    val isPrincipal = isPrincipalRole(state.userRole)

    Scaffold(
        containerColor = CosmicBlack,
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = CosmicNavy,
                    contentColor = TextPrimary,
                )
            }
        },
        topBar = {
            // Clean TopBar: "Detail Jadwal" with no redundant duplicate subject title
            ExecutiveTopBar(
                title = "Detail Jadwal",
                subtitle = if (isPrincipal) "Supervisi Kelas • $roomText" else "Jadwal Pembelajaran • $roomText",
                onBack = onBack,
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
                session != null -> {
                    val isTeacher = isTeacherRole(state.userRole) || isPrincipal
                    val accentColor = if (isTeacher) TeacherNeon else StudentNeon

                    // Wrap callbacks to pass both the subject name and class name for strict scoping
                    val sessionClassName = session.className?.trim()?.takeIf { it.isNotBlank() }
                    val openAssignments: (String) -> Unit = { onOpenAssignments(subject, sessionClassName) }
                    val openQuizzes: (String) -> Unit = { onOpenQuizzes(subject, sessionClassName) }
                    val openMaterials: (String) -> Unit = { onOpenMaterials(subject, sessionClassName) }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // ── ROLE-BASED SESSION CONTENT ─────────────────────
                        if (isTeacher) {
                            TeacherSessionDetailContent(
                                session = session,
                                attendance = state.attendance,
                                students = state.studentItems,
                                onUpdateAttendance = viewModel::updateAttendance,
                                onMarkAllPresent = viewModel::markAllPresent,
                                onSaveAll = viewModel::saveAllAttendance,
                                isBulkSaving = state.isBulkSaving,
                                onOpenAssignments = openAssignments,
                                onOpenQuizzes = openQuizzes,
                                onOpenMaterials = openMaterials,
                                accentColor = accentColor,
                            )
                        } else {
                            StudentSessionDetailContent(
                                session = session,
                                attendance = state.attendance,
                                myAttendance = state.myAttendance,
                                onOpenAssignments = openAssignments,
                                onOpenQuizzes = openQuizzes,
                                onOpenMaterials = openMaterials,
                                accentColor = accentColor,
                                onSubmitExcuse = viewModel::submitExcuse,
                            )
                        }

                        Spacer(Modifier.height(40.dp))
                    }
                }
            }
        }
    }
}

typealias UnitCallback = () -> Unit