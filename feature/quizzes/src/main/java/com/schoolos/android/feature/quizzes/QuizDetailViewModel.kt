package com.schoolos.android.feature.quizzes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
)

@HiltViewModel
class QuizDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: QuizRepository,
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

                    val role = _state.value.userRole.lowercase()
                    val isStudent = role != "teacher" && role != "guru"

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
                    }

                    _state.value = _state.value.copy(
                        isLoading = false,
                        quiz = quiz,
                        hasCompleted = hasCompleted,
                        completedAttempt = completedAttempt,
                    )
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(isLoading = false, error = e.message ?: "Quiz not found")
                }
        }
    }

    fun startAttempt() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isStarting = true, startError = null)
            repository.startAttempt(quizId)
                .onSuccess { attempt ->
                    _state.value = _state.value.copy(isStarting = false, attempt = attempt)
                }
                .onFailure { e ->
                    val errorMsg = e.message ?: "Gagal memulai kuis"
                    val isMaxLimit = errorMsg.contains("batas maksimal", ignoreCase = true) ||
                            errorMsg.contains("Maximum attempt limit", ignoreCase = true)
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
                    _state.value = _state.value.copy(isPublishing = false, startError = e.message ?: "Gagal mempublikasikan kuis")
                }
        }
    }

    fun dismissAttempt() {
        _state.value = _state.value.copy(attempt = null)
    }
}
