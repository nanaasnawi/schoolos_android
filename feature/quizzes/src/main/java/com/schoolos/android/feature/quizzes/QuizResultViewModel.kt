package com.schoolos.android.feature.quizzes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.schoolos.android.domain.model.QuizAttempt
import com.schoolos.android.domain.repository.QuizRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class QuizResultUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val attempt: QuizAttempt? = null,
)

@HiltViewModel
class QuizResultViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: QuizRepository,
) : ViewModel() {

    private val attemptId: String = savedStateHandle.get<String>("attemptId") ?: ""
    private val quizId: String = savedStateHandle.get<String>("quizId") ?: ""
    private val score: Int = savedStateHandle.get<Int>("score")
        ?: (savedStateHandle.get<String>("score")?.toIntOrNull() ?: 0)
    private val totalPoints: Int = savedStateHandle.get<Int>("totalPoints")
        ?: (savedStateHandle.get<String>("totalPoints")?.toIntOrNull() ?: 100)

    private val _state = MutableStateFlow(
        QuizResultUiState(
            attempt = QuizAttempt(
                id = attemptId,
                quizId = quizId,
                studentId = "",
                startedAt = "",
                completedAt = null,
                score = score,
                totalPoints = if (totalPoints > 0) totalPoints else 100,
                percentage = if (totalPoints > 0) (score * 100) / totalPoints else 0,
                status = "completed",
                createdAt = "",
                updatedAt = "",
            )
        )
    )
    val state = _state.asStateFlow()

    init {
        loadResult()
    }

    private fun loadResult() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            if (quizId.isNotBlank() && attemptId.isNotBlank()) {
                repository.getQuizAttempt(quizId, attemptId)
                    .onSuccess { fullAttempt ->
                        _state.value = _state.value.copy(
                            isLoading = false,
                            attempt = fullAttempt,
                        )
                    }
                    .onFailure {
                        // Keep initial attempt from arguments if remote load fails
                        _state.value = _state.value.copy(isLoading = false)
                    }
            } else {
                _state.value = _state.value.copy(isLoading = false)
            }
        }
    }
}
