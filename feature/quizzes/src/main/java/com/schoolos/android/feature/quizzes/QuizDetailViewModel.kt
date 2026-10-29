package com.schoolos.android.feature.quizzes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.schoolos.android.core.common.toHumanReadableError
import com.schoolos.android.domain.model.Quiz
import com.schoolos.android.domain.model.QuizAttempt
import com.schoolos.android.domain.repository.QuizRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class QuizDetailUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val quiz: Quiz? = null,
    val isStarting: Boolean = false,
    val isPublishing: Boolean = false,
    val attempt: QuizAttempt? = null,
    val startError: String? = null,
    val userRole: String = "student",
    val hasCompleted: Boolean = false,
    val completedAttempt: QuizAttempt? = null,
    val allAttempts: List<QuizAttempt> = emptyList(),
)

@HiltViewModel
class QuizDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: QuizRepository,
    private val academicRepository: com.schoolos.android.domain.repository.AcademicRepository,
    private val authManager: com.schoolos.android.core.auth.AuthManager,
) : ViewModel() {

    private val quizId: String = savedStateHandle["id"] ?: ""

    private val _state = MutableStateFlow(QuizDetailUiState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            authManager.authState.collect { auth ->
                _state.value = _state.value.copy(userRole = auth.role ?: "student")
            }
        }
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            repository.getQuiz(quizId)
                .onSuccess { quiz ->
                    var hasCompleted = quiz.studentHasCompleted ||
                            quiz.studentAttemptStatus?.lowercase() in listOf("completed", "submitted", "graded")
                    var completedAttempt: QuizAttempt? = null

                    val isTeacher = com.schoolos.android.core.auth.isTeacherRole(_state.value.userRole) || com.schoolos.android.core.auth.isPrincipalRole(_state.value.userRole)
                    val isStudent = !isTeacher

                    var allAttempts: List<QuizAttempt> = emptyList()

                    if (isStudent) {
                        val attemptsResult = repository.getQuizAttempts(quizId).getOrNull()
                        if (!attemptsResult.isNullOrEmpty()) {
                            completedAttempt = attemptsResult.firstOrNull {
                                it.status.lowercase() in listOf("completed", "submitted", "graded")
                            } ?: attemptsResult.firstOrNull()
                            if (completedAttempt != null || attemptsResult.size >= quiz.maxAttempts) {
                                hasCompleted = true
                            }
                        }
                    } else if (isTeacher) {
                        val attemptsResult = repository.getQuizAttempts(quizId).getOrNull() ?: emptyList()
                        var mergedAttempts = attemptsResult
                        val className = quiz.className ?: ""
                        val classId = quiz.classId ?: ""
                        val classStudents = if (classId.isNotBlank()) {
                            academicRepository.getClassStudents(classId).getOrNull() ?: emptyList()
                        } else if (className.isNotBlank() && !className.equals("Semua Rombel", ignoreCase = true)) {
                            academicRepository.getClassStudents(className).getOrNull() ?: emptyList()
                        } else {
                            academicRepository.getClassStudents("ALL").getOrNull() ?: emptyList()
                        }

                        if (classStudents.isNotEmpty()) {
                            val submittedStudentIds = attemptsResult.filter { it.status != "unsubmitted" }.flatMap { 
                                listOfNotNull(it.studentId, it.studentNisn)
                            }.toSet()
                            val unsubmittedList = classStudents.filter { s -> s.id !in submittedStudentIds && s.nisn !in submittedStudentIds }.map { s ->
                                QuizAttempt(
                                    id = "unsub-${s.id}",
                                    quizId = quizId,
                                    studentId = s.id,
                                    studentName = s.fullName,
                                    studentNisn = s.nisn,
                                    startedAt = "",
                                    completedAt = null,
                                    status = "unsubmitted",
                                    score = 0,
                                    totalPoints = quiz.maxScore,
                                    createdAt = "",
                                    updatedAt = "",
                                    answers = emptyList(),
                                )
                            }
                            mergedAttempts = attemptsResult + unsubmittedList
                        }
                        allAttempts = mergedAttempts
                    }

                    _state.value = _state.value.copy(
                        isLoading = false,
                        quiz = quiz,
                        hasCompleted = hasCompleted,
                        completedAttempt = completedAttempt,
                        allAttempts = allAttempts,
                    )
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = e.toHumanReadableError("Kuis CBT tidak ditemukan atau telah dihapus.")
                    )
                }
        }
    }

    fun gradeAttempt(attemptId: String, score: Int, feedback: String? = null) {
        viewModelScope.launch {
            repository.gradeAttempt(quizId, attemptId, score, feedback)
                .onSuccess {
                    load()
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(
                        error = e.toHumanReadableError("Gagal menilai pengerjaan kuis.")
                    )
                }
        }
    }

    fun startAttempt(token: String? = null) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isStarting = true, startError = null)
            val quiz = _state.value.quiz
            if (quiz?.examMode == "PROCTORED_CBT" && !token.isNullOrBlank()) {
                val verifyRes = repository.verifyToken(quizId, token)
                if (verifyRes.isFailure) {
                    _state.value = _state.value.copy(
                        isStarting = false,
                        startError = verifyRes.exceptionOrNull().toHumanReadableError("Token ujian CBT tidak valid."),
                    )
                    return@launch
                }
            }
            repository.startAttempt(quizId)
                .onSuccess { attempt ->
                    _state.value = _state.value.copy(isStarting = false, attempt = attempt)
                }
                .onFailure { e ->
                    val rawMsg = e?.message ?: ""
                    val isMaxLimit = rawMsg.contains("batas maksimal", ignoreCase = true) ||
                            rawMsg.contains("Maximum attempt limit", ignoreCase = true)
                    val errorMsg = e.toHumanReadableError("Gagal memulai kuis CBT.")
                    _state.value = _state.value.copy(
                        isStarting = false,
                        startError = errorMsg,
                        hasCompleted = if (isMaxLimit) true else _state.value.hasCompleted,
                    )
                }
        }
    }

    fun publishQuiz() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isPublishing = true, startError = null)
            repository.publishQuiz(quizId)
                .onSuccess {
                    _state.value = _state.value.copy(isPublishing = false)
                    load()
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(
                        isPublishing = false,
                        startError = e.toHumanReadableError("Gagal mempublikasikan kuis.")
                    )
                }
        }
    }

    fun deleteQuiz(onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, startError = null)
            repository.deleteQuiz(quizId)
                .onSuccess {
                    _state.value = _state.value.copy(isLoading = false)
                    onSuccess()
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        startError = e.toHumanReadableError("Gagal menghapus kuis.")
                    )
                }
        }
    }

    fun dismissAttempt() {
        _state.value = _state.value.copy(attempt = null)
    }
}
