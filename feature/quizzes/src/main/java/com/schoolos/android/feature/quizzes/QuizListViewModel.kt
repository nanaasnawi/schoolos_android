package com.schoolos.android.feature.quizzes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.schoolos.android.domain.model.Quiz
import com.schoolos.android.domain.repository.QuizRepository
import dagger.hilt.android.lifecycle.HiltViewModel
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
    val subjectFilter: String? = null,
    val classFilter: String? = null,
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

    /** Optional subject filter passed via navigation (e.g. from a session detail). */
    private var subjectFilter: String? = savedStateHandle.get<String>("subjectId")?.takeIf { it.isNotBlank() }

    /** Optional class/rombel filter passed via navigation (e.g. from a session detail). */
    private var classFilter: String? = savedStateHandle.get<String>("className")?.takeIf { it.isNotBlank() }

    init {
        viewModelScope.launch {
            authManager.authState.collect { auth ->
                _state.value = _state.value.copy(userRole = auth.role ?: "student")
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

    private fun load() {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                isLoading = !hasLoadedInitialData && _state.value.quizzes.isEmpty(),
                error = null
            )
            repository.getQuizzes(classId)
                .onSuccess { quizzes ->
                    hasLoadedInitialData = true
                    val isTeacher = com.schoolos.android.core.auth.isTeacherRole(_state.value.userRole) || com.schoolos.android.core.auth.isPrincipalRole(_state.value.userRole)
                    val isStudent = !isTeacher

                    val updatedQuizzes = if (isStudent) {
                        quizzes.map { quiz ->
                            if (quiz.studentHasCompleted || quiz.studentAttemptStatus?.lowercase() in listOf("completed", "submitted", "graded")) {
                                quiz.copy(studentHasCompleted = true)
                            } else {
                                val attemptsResult = repository.getQuizAttempts(quiz.id).getOrNull()
                                val hasCompleted = attemptsResult?.any {
                                    it.status.lowercase() in listOf("completed", "submitted", "graded")
                                } == true || (attemptsResult != null && attemptsResult.isNotEmpty() && attemptsResult.size >= quiz.maxAttempts)
                                if (hasCompleted) {
                                    quiz.copy(
                                        studentHasCompleted = true,
                                        studentAttemptStatus = "completed",
                                        studentAttemptsCount = attemptsResult?.size ?: 1,
                                        studentLastAttemptId = attemptsResult?.firstOrNull()?.id,
                                        studentLastScore = attemptsResult?.firstOrNull()?.score,
                                    )
                                } else {
                                    quiz
                                }
                            }
                        }
                    } else {
                        quizzes
                    }

                    var filtered = updatedQuizzes
                    subjectFilter?.let { filter ->
                        filtered = filtered.filter { q ->
                            com.schoolos.android.core.designsystem.matchesSubjectFilter(q.subjectName, filter) ||
                            (q.subjectName.isNullOrBlank() && com.schoolos.android.core.designsystem.matchesSubjectFilter(q.title, filter))
                        }
                    }
                    classFilter?.let { filter ->
                        filtered = filtered.filter { q ->
                            com.schoolos.android.core.designsystem.matchesClassFilter(q.className, filter) ||
                            (q.className.isNullOrBlank() && com.schoolos.android.core.designsystem.matchesClassFilter(q.title, filter))
                        }
                    }
                    _state.value = _state.value.copy(
                        isLoading = false,
                        isRefreshing = false,
                        quizzes = filtered.sortedByDescending { it.createdAt },
                        subjectFilter = subjectFilter,
                        classFilter = classFilter,
                    )
                }
                .onFailure { e ->
                    hasLoadedInitialData = true
                    _state.value = _state.value.copy(
                        isLoading = false,
                        isRefreshing = false,
                        error = e.message ?: "Failed to load quizzes",
                    )
                }
        }
    }

    fun clearSubjectFilter() {
        subjectFilter = null
        _state.value = _state.value.copy(subjectFilter = null)
        refresh()
    }
}
