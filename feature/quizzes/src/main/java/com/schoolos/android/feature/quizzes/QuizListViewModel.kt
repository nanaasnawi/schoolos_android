package com.schoolos.android.feature.quizzes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.schoolos.android.core.common.toHumanReadableError
import com.schoolos.android.domain.model.Quiz
import com.schoolos.android.domain.model.QuizProctorScheduleDashboard
import com.schoolos.android.domain.repository.QuizRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class QuizListUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val userRole: String = "student",
    val quizzes: List<Quiz> = emptyList(),
    val proctorSchedule: QuizProctorScheduleDashboard? = null,
    val subjectFilter: String? = null,
    val classFilter: String? = null,
    val dateFilter: String? = null,
)

@HiltViewModel
class QuizListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: QuizRepository,
    private val authManager: com.schoolos.android.core.auth.AuthManager,
) : ViewModel() {

    private val _state = MutableStateFlow(QuizListUiState())
    val state = _state.asStateFlow()

    private val classId = ""
    private var hasLoadedInitialData: Boolean = false

    private var subjectFilter: String? = savedStateHandle.get<String>("subjectId")?.takeIf { it.isNotBlank() }
    private var classFilter: String? = savedStateHandle.get<String>("className")?.takeIf { it.isNotBlank() }
    private var dateFilter: String? = savedStateHandle.get<String>("date")?.takeIf { it.isNotBlank() }

    private var studentEnrolledClass: String? = null
    private var teacherSubject: String? = null
    private var isTeacher: Boolean = false
    private var isPrincipal: Boolean = false

    init {
        viewModelScope.launch {
            authManager.authState.collect { auth ->
                val role = auth.role ?: "student"
                isPrincipal = auth.isPrincipal || com.schoolos.android.core.auth.isPrincipalRole(role)
                isTeacher = !isPrincipal && com.schoolos.android.core.auth.isTeacherRole(role)
                val isStudent = !isTeacher && !isPrincipal
                studentEnrolledClass = if (isStudent) {
                    auth.className?.trim()?.takeIf { it.isNotBlank() && !it.equals("ALL", ignoreCase = true) }
                } else {
                    null
                }
                teacherSubject = if (isTeacher) auth.subject?.trim()?.takeIf { it.isNotBlank() } else null
                _state.value = _state.value.copy(userRole = role)
                if (hasLoadedInitialData) {
                    load()
                }
            }
        }
        load()
    }

    fun refresh(isPullRefresh: Boolean = false) {
        if (!isPullRefresh && hasLoadedInitialData) return
        if (isPullRefresh) {
            _state.value = _state.value.copy(isRefreshing = true)
        }
        load()
    }

    fun forceRefresh() {
        _state.value = _state.value.copy(isRefreshing = true)
        load()
    }

    private fun load() {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                isLoading = !hasLoadedInitialData && _state.value.quizzes.isEmpty(),
                error = null,
            )
            val isStaff = com.schoolos.android.core.auth.isTeacherRole(_state.value.userRole) ||
                com.schoolos.android.core.auth.isPrincipalRole(_state.value.userRole)

            val (quizzesResult, scheduleResult) = coroutineScope {
                val quizzesDeferred = async { repository.getQuizzes(classId) }
                val scheduleDeferred = if (isStaff) {
                    async { repository.getProctorSchedule() }
                } else {
                    null
                }
                quizzesDeferred.await() to scheduleDeferred?.await()
            }

            val scheduleDashboard = scheduleResult?.getOrNull()

            quizzesResult
                .onSuccess { rawQuizzes ->
                    hasLoadedInitialData = true
                    val isStudent = !isStaff

                    // Merge teacher/token/phase fields from proctor schedule if available
                    val sessionById = scheduleDashboard?.sessions?.associateBy { it.id }.orEmpty()
                    val enrichedQuizzes = rawQuizzes.map { q ->
                        val sess = sessionById[q.id]
                        if (sess != null) {
                            q.copy(
                                examToken = q.examToken ?: sess.examToken,
                                requiresToken = q.requiresToken || sess.requiresToken,
                                teacherName = q.teacherName ?: sess.teacherName,
                                className = q.className ?: sess.className,
                                subjectName = q.subjectName ?: sess.subjectName,
                                assessmentCategory = q.assessmentCategory ?: sess.assessmentCategory,
                                examPhase = q.examPhase ?: sess.examPhase,
                            )
                        } else {
                            q
                        }
                    }

                    val updatedQuizzes = if (isStudent) {
                        enrichedQuizzes.map { quiz ->
                            if (quiz.studentHasCompleted || quiz.studentAttemptStatus?.lowercase() in listOf("completed", "submitted", "graded")) {
                                quiz.copy(studentHasCompleted = true)
                            } else {
                                val attemptsResult = repository.getQuizAttempts(quiz.id).getOrNull()
                                val finishedAttempts = attemptsResult?.filter {
                                    it.status.lowercase() in listOf("completed", "submitted", "graded")
                                } ?: emptyList()
                                val hasCompleted = finishedAttempts.isNotEmpty() && finishedAttempts.size >= quiz.maxAttempts
                                if (hasCompleted) {
                                    quiz.copy(
                                        studentHasCompleted = true,
                                        studentAttemptStatus = finishedAttempts.firstOrNull()?.status ?: "completed",
                                        studentAttemptsCount = finishedAttempts.size,
                                        studentLastAttemptId = finishedAttempts.firstOrNull()?.id,
                                        studentLastScore = finishedAttempts.firstOrNull()?.score,
                                    )
                                } else {
                                    quiz
                                }
                            }
                        }
                    } else {
                        enrichedQuizzes
                    }

                    var filtered = updatedQuizzes

                    val effectiveClass = classFilter ?: studentEnrolledClass
                    if (effectiveClass != null) {
                        filtered = filtered.filter { q ->
                            com.schoolos.android.core.designsystem.matchesClassFilter(q.className, effectiveClass) ||
                                (q.className.isNullOrBlank() && com.schoolos.android.core.designsystem.matchesClassFilter(q.title, effectiveClass))
                        }
                    }

                    if (subjectFilter != null) {
                        filtered = filtered.filter { q ->
                            com.schoolos.android.core.designsystem.matchesSubjectFilter(q.subjectName, subjectFilter) ||
                                (q.subjectName.isNullOrBlank() && com.schoolos.android.core.designsystem.matchesSubjectFilter(q.title, subjectFilter))
                        }
                    } else if (isTeacher && !isPrincipal && !teacherSubject.isNullOrBlank()) {
                        filtered = filtered.filter { q ->
                            com.schoolos.android.core.designsystem.matchesTeacherTaughtSubjects(q.subjectName ?: q.title, teacherSubject)
                        }
                    }

                    dateFilter?.let { dFilter ->
                        val targetDate = if (dFilter.equals("today", ignoreCase = true)) {
                            java.time.LocalDate.now(java.time.ZoneId.of("Asia/Jakarta"))
                        } else {
                            try {
                                java.time.LocalDate.parse(dFilter.trim().substringBefore(" ").substringBefore("T"))
                            } catch (_: Exception) {
                                java.time.LocalDate.now(java.time.ZoneId.of("Asia/Jakarta"))
                            }
                        }
                        filtered = filtered.filter { q ->
                            val qDate = extractQuizDate(q)
                            qDate != null && qDate == targetDate
                        }
                    }

                    val sortedQuizzes = filtered.sortedWith(
                        compareBy<Quiz>(
                            { phasePriority(it.effectivePhase) },
                            {
                                if (it.effectivePhase == "UPCOMING" || it.effectivePhase == "LIVE") {
                                    parseQuizInstant(it.startAt ?: it.releaseAt)?.toEpochMilli() ?: Long.MAX_VALUE
                                } else {
                                    -(parseQuizInstant(it.startAt ?: it.createdAt)?.toEpochMilli() ?: 0L)
                                }
                            },
                            { it.effectiveDisplayClass ?: "" },
                        )
                    )

                    _state.value = _state.value.copy(
                        isLoading = false,
                        isRefreshing = false,
                        quizzes = sortedQuizzes,
                        proctorSchedule = scheduleDashboard,
                        subjectFilter = subjectFilter,
                        classFilter = classFilter,
                        dateFilter = dateFilter,
                    )
                }
                .onFailure { e ->
                    hasLoadedInitialData = true
                    _state.value = _state.value.copy(
                        isLoading = false,
                        isRefreshing = false,
                        proctorSchedule = scheduleDashboard ?: _state.value.proctorSchedule,
                        error = e.toHumanReadableError("Gagal memuat daftar kuis CBT. Periksa koneksi internet Anda."),
                    )
                }
        }
    }

    private fun phasePriority(phase: String): Int = when (phase.uppercase()) {
        "LIVE" -> 0
        "UPCOMING" -> 1
        "DRAFT" -> 2
        else -> 3
    }

    fun clearDateFilter() {
        dateFilter = null
        _state.value = _state.value.copy(dateFilter = null)
        load()
    }

    fun clearSubjectFilter() {
        subjectFilter = null
        _state.value = _state.value.copy(subjectFilter = null)
        forceRefresh()
    }

    private fun extractQuizDate(quiz: Quiz): java.time.LocalDate? {
        val targetIso = (quiz.startAt ?: quiz.releaseAt)?.takeIf { it.isNotBlank() }
            ?: quiz.createdAt.takeIf { it.isNotBlank() }
            ?: return null
        return parseQuizInstant(targetIso)?.atZone(java.time.ZoneId.of("Asia/Jakarta"))?.toLocalDate()
    }
}
