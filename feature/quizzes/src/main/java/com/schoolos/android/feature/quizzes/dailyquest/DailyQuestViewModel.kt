package com.schoolos.android.feature.quizzes.dailyquest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.schoolos.android.core.auth.AuthManager
import com.schoolos.android.core.common.toHumanReadableError
import com.schoolos.android.core.designsystem.matchesClassFilter
import com.schoolos.android.domain.model.Assignment
import com.schoolos.android.domain.repository.AssignmentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

data class DailyQuestUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val teacherGameMissions: List<Assignment> = emptyList(),
)

@HiltViewModel
class DailyQuestViewModel @Inject constructor(
    private val assignmentRepository: AssignmentRepository,
    private val authManager: AuthManager,
) : ViewModel() {

    private val _state = MutableStateFlow(DailyQuestUiState())
    val state = _state.asStateFlow()

    init {
        loadTeacherGameMissions()
    }

    fun loadTeacherGameMissions() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            val auth = authManager.authState.first()
            val classFilter = if (!auth.isTeacher && !auth.isPrincipal) {
                auth.className?.takeIf { it.isNotBlank() }
            } else {
                null
            }
            assignmentRepository.getAssignments("")
                .onSuccess { list ->
                    val now = Instant.now()
                    val activeMissions = list.filter { assignment ->
                        val classOk = classFilter == null || matchesClassFilter(assignment.className, classFilter)
                        val isCompleted = assignment.status.equals("submitted", ignoreCase = true) ||
                            assignment.status.equals("graded", ignoreCase = true)
                        val isExpired = !assignment.dueAt.isNullOrBlank() && try {
                            Instant.parse(assignment.dueAt).isBefore(now)
                        } catch (_: Exception) {
                            false
                        }
                        classOk && assignment.hasAnyGame && !assignment.isScheduled && !isCompleted && !isExpired
                    }
                    _state.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = null,
                            teacherGameMissions = activeMissions,
                        )
                    }
                }
                .onFailure { err ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            teacherGameMissions = emptyList(),
                            errorMessage = err.toHumanReadableError(),
                        )
                    }
                }
        }
    }
}
