package com.schoolos.android.feature.quizzes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.schoolos.android.core.auth.AuthManager
import com.schoolos.android.core.auth.isPrincipalRole
import com.schoolos.android.core.auth.isTeacherRole
import com.schoolos.android.core.common.toHumanReadableError
import com.schoolos.android.domain.model.Quiz
import com.schoolos.android.domain.model.QuizAttempt
import com.schoolos.android.domain.model.QuizProctorMonitor
import com.schoolos.android.domain.model.QuizQuestion
import com.schoolos.android.domain.repository.AcademicRepository
import com.schoolos.android.domain.repository.QuizRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class QuizDetailUiState(
    val isLoading: Boolean = true,
    val isRefreshingMonitor: Boolean = false,
    val isAutoSyncMonitor: Boolean = true,
    val isProcessingProctorAction: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val quiz: Quiz? = null,
    val proctorMonitor: QuizProctorMonitor? = null,
    val previewQuestions: List<QuizQuestion> = emptyList(),
    val isLoadingQuestions: Boolean = false,
    val questionsError: String? = null,
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
    private val academicRepository: AcademicRepository,
    private val authManager: AuthManager,
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

    fun load(silent: Boolean = false) {
        viewModelScope.launch {
            if (!silent && _state.value.quiz == null) {
                _state.value = _state.value.copy(isLoading = true, error = null)
            } else if (silent) {
                _state.value = _state.value.copy(isRefreshingMonitor = true)
            }

            val isStaff = isTeacherRole(_state.value.userRole) || isPrincipalRole(_state.value.userRole)

            if (isStaff) {
                val (quizRes, monitorRes) = coroutineScope {
                    val quizDef = async { repository.getQuiz(quizId) }
                    val monitorDef = async { repository.getProctorMonitor(quizId) }
                    quizDef.await() to monitorDef.await()
                }
                val monitor = monitorRes.getOrNull()
                val quiz = monitor?.quiz ?: quizRes.getOrNull()

                if (quiz != null) {
                    val fallbackAttempts = if (monitor == null) {
                        buildFallbackTeacherAttempts(quiz)
                    } else {
                        emptyList()
                    }
                    _state.value = _state.value.copy(
                        isLoading = false,
                        isRefreshingMonitor = false,
                        quiz = quiz,
                        proctorMonitor = monitor ?: _state.value.proctorMonitor,
                        allAttempts = fallbackAttempts,
                    )
                } else {
                    val err = monitorRes.exceptionOrNull() ?: quizRes.exceptionOrNull()
                    _state.value = _state.value.copy(
                        isLoading = false,
                        isRefreshingMonitor = false,
                        error = if (_state.value.quiz == null) {
                            err.toHumanReadableError("Kuis CBT tidak ditemukan atau telah dihapus.")
                        } else null,
                    )
                }
            } else {
                repository.getQuiz(quizId)
                    .onSuccess { quiz ->
                        var hasCompleted = quiz.studentHasCompleted ||
                            quiz.studentAttemptStatus?.lowercase() in listOf("completed", "submitted", "graded")
                        var completedAttempt: QuizAttempt? = null

                        val attemptsResult = repository.getQuizAttempts(quizId).getOrNull()
                        if (!attemptsResult.isNullOrEmpty()) {
                            val finishedAttempts = attemptsResult.filter {
                                it.status.lowercase() in listOf("completed", "submitted", "graded")
                            }
                            completedAttempt = finishedAttempts.firstOrNull()
                            if (completedAttempt != null || finishedAttempts.size >= quiz.maxAttempts) {
                                hasCompleted = true
                            }
                        }

                        _state.value = _state.value.copy(
                            isLoading = false,
                            isRefreshingMonitor = false,
                            quiz = quiz,
                            hasCompleted = hasCompleted,
                            completedAttempt = completedAttempt,
                        )
                    }
                    .onFailure { e ->
                        _state.value = _state.value.copy(
                            isLoading = false,
                            isRefreshingMonitor = false,
                            error = e.toHumanReadableError("Kuis CBT tidak ditemukan atau telah dihapus."),
                        )
                    }
            }
        }
    }

    private suspend fun buildFallbackTeacherAttempts(quiz: Quiz): List<QuizAttempt> {
        val attemptsResult = repository.getQuizAttempts(quizId).getOrNull() ?: emptyList()
        val className = quiz.className ?: ""
        val classId = quiz.classId ?: ""
        val classStudents = when {
            classId.isNotBlank() -> academicRepository.getClassStudents(classId).getOrNull() ?: emptyList()
            className.isNotBlank() && !className.equals("Semua Rombel", ignoreCase = true) ->
                academicRepository.getClassStudents(className).getOrNull() ?: emptyList()
            else -> academicRepository.getClassStudents("ALL").getOrNull() ?: emptyList()
        }
        if (classStudents.isEmpty()) return attemptsResult
        val submittedIds = attemptsResult.filter { it.status != "unsubmitted" }
            .flatMap { listOfNotNull(it.studentId, it.studentNisn) }
            .toSet()
        val unsubmitted = classStudents
            .filter { s -> s.id !in submittedIds && s.nisn !in submittedIds }
            .map { s ->
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
        return attemptsResult + unsubmitted
    }

    fun loadQuestionPreview(force: Boolean = false) {
        if (_state.value.isLoadingQuestions) return
        if (!force && _state.value.previewQuestions.isNotEmpty()) return
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoadingQuestions = true, questionsError = null)
            repository.getQuestions(quizId)
                .onSuccess { list ->
                    _state.value = _state.value.copy(
                        isLoadingQuestions = false,
                        previewQuestions = list.sortedBy { it.orderIndex },
                    )
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(
                        isLoadingQuestions = false,
                        questionsError = e.toHumanReadableError("Gagal memuat daftar soal kuis."),
                    )
                }
        }
    }

    fun toggleAutoSyncMonitor(enabled: Boolean) {
        _state.value = _state.value.copy(isAutoSyncMonitor = enabled)
    }

    fun rotateToken(customToken: String? = null, expiresInMinutes: Int? = null) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isProcessingProctorAction = true, startError = null)
            repository.rotateProctorToken(quizId, customToken, expiresInMinutes)
                .onSuccess { updated ->
                    _state.value = _state.value.copy(
                        isProcessingProctorAction = false,
                        quiz = updated,
                        successMessage = "Token ujian diperbarui: ${updated.examToken ?: "-"}",
                    )
                    load(silent = true)
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(
                        isProcessingProctorAction = false,
                        startError = e.toHumanReadableError("Gagal memperbarui token ujian CBT."),
                    )
                }
        }
    }

    fun extendTime(extraMinutes: Int) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isProcessingProctorAction = true, startError = null)
            repository.extendProctorTime(quizId, extraMinutes)
                .onSuccess { updated ->
                    _state.value = _state.value.copy(
                        isProcessingProctorAction = false,
                        quiz = updated,
                        successMessage = "Waktu ujian diperpanjang +$extraMinutes menit.",
                    )
                    load(silent = true)
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(
                        isProcessingProctorAction = false,
                        startError = e.toHumanReadableError("Gagal memperpanjang batas waktu ujian."),
                    )
                }
        }
    }

    fun updateSchedule(
        startAtIso: String?,
        endAtIso: String?,
        durationMinutes: Int?,
        examMode: String?,
        examToken: String?,
        status: String? = null,
    ) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isProcessingProctorAction = true, startError = null)
            repository.updateQuiz(
                id = quizId,
                durationMinutes = durationMinutes,
                startAt = startAtIso,
                endAt = endAtIso,
                examMode = examMode,
                examToken = examToken,
                status = status,
            )
                .onSuccess { updated ->
                    _state.value = _state.value.copy(
                        isProcessingProctorAction = false,
                        quiz = updated,
                        successMessage = "Jadwal & pengaturan ujian CBT berhasil diperbarui.",
                    )
                    load(silent = true)
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(
                        isProcessingProctorAction = false,
                        startError = e.toHumanReadableError("Gagal menyimpan jadwal ujian CBT."),
                    )
                }
        }
    }

    fun forceSubmitStudentAttempt(attemptId: String, studentName: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isProcessingProctorAction = true, startError = null)
            repository.forceSubmitAttempt(quizId, attemptId)
                .onSuccess {
                    _state.value = _state.value.copy(
                        isProcessingProctorAction = false,
                        successMessage = "Sesi $studentName berhasil diselesaikan & dinilai.",
                    )
                    load(silent = true)
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(
                        isProcessingProctorAction = false,
                        startError = e.toHumanReadableError("Gagal memaksa pengumpulan jawaban siswa."),
                    )
                }
        }
    }

    fun reopenStudentAttempt(attemptId: String, studentName: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isProcessingProctorAction = true, startError = null)
            repository.reopenAttempt(quizId, attemptId)
                .onSuccess {
                    _state.value = _state.value.copy(
                        isProcessingProctorAction = false,
                        successMessage = "Sesi ujian $studentName dibuka kembali.",
                    )
                    load(silent = true)
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(
                        isProcessingProctorAction = false,
                        startError = e.toHumanReadableError("Gagal membuka ulang sesi ujian siswa."),
                    )
                }
        }
    }

    fun gradeAttempt(attemptId: String, score: Int, feedback: String? = null) {
        viewModelScope.launch {
            repository.gradeAttempt(quizId, attemptId, score, feedback)
                .onSuccess {
                    _state.value = _state.value.copy(
                        successMessage = "Nilai kuis berhasil disimpan.",
                    )
                    load(silent = true)
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(
                        startError = e.toHumanReadableError("Gagal menilai pengerjaan kuis."),
                    )
                }
        }
    }

    fun dismissSuccessMessage() {
        _state.value = _state.value.copy(successMessage = null)
    }

    fun clearStartError() {
        if (_state.value.startError != null) {
            _state.value = _state.value.copy(startError = null)
        }
    }

    fun startAttempt(token: String? = null) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isStarting = true, startError = null)
            val quiz = _state.value.quiz
            if ((quiz?.requiresToken == true || quiz?.examMode == "PROCTORED_CBT") && !token.isNullOrBlank()) {
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
                    val rawMsg = e.message ?: ""
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
                    _state.value = _state.value.copy(
                        isPublishing = false,
                        successMessage = "Kuis CBT berhasil diterbitkan.",
                    )
                    load(silent = true)
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(
                        isPublishing = false,
                        startError = e.toHumanReadableError("Gagal mempublikasikan kuis."),
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
                        startError = e.toHumanReadableError("Gagal menghapus kuis."),
                    )
                }
        }
    }

    fun dismissAttempt() {
        _state.value = _state.value.copy(attempt = null)
    }
}
