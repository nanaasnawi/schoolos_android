package com.schoolos.android.feature.sessions

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.schoolos.android.core.auth.AuthManager
import com.schoolos.android.domain.model.LearningSession
import com.schoolos.android.domain.model.SessionAttendance
import com.schoolos.android.domain.repository.AcademicRepository
import com.schoolos.android.domain.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StudentAttendanceUiItem(
    val studentId: String,
    val studentName: String,
    val nisn: String? = null,
    val status: String = "present",
    val checkedInAt: String? = null,
    val notes: String? = null,
)

data class SessionDetailUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val userRole: String = "student",
    val session: LearningSession? = null,
    val attendance: List<SessionAttendance> = emptyList(),
    val studentItems: List<StudentAttendanceUiItem> = emptyList(),
)

@HiltViewModel
class SessionDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: SessionRepository,
    private val academicRepository: AcademicRepository,
    private val authManager: AuthManager,
) : ViewModel() {

    private val sessionId: String = savedStateHandle["id"] ?: ""

    private val _state = MutableStateFlow(SessionDetailUiState())
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
            repository.getSession(sessionId)
                .onSuccess { session ->
                    _state.value = _state.value.copy(isLoading = false, session = session)
                    loadAttendanceAndStudents(session)
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(isLoading = false, error = e.message ?: "Session not found")
                }
        }
    }

    private suspend fun loadAttendanceAndStudents(session: LearningSession) {
        val attendanceList = repository.getAttendance(sessionId).getOrDefault(emptyList())
        val attendanceMap = attendanceList.associateBy { it.studentId }

        val targetClass = session.classId.takeIf { it.isNotBlank() }
            ?: session.className?.takeIf { it.isNotBlank() }
            ?: ""
        val classStudents = if (targetClass.isNotBlank()) {
            academicRepository.getClassStudents(targetClass).getOrDefault(emptyList())
        } else {
            emptyList()
        }

        val items = if (classStudents.isNotEmpty()) {
            classStudents.map { student ->
                val att = attendanceMap[student.id]
                StudentAttendanceUiItem(
                    studentId = student.id,
                    studentName = student.fullName,
                    nisn = student.nisn,
                    status = att?.status ?: "present",
                    checkedInAt = att?.checkedInAt,
                    notes = att?.notes,
                )
            }
        } else {
            attendanceList.mapIndexed { idx, att ->
                StudentAttendanceUiItem(
                    studentId = att.studentId,
                    studentName = "Siswa ${idx + 1}",
                    nisn = null,
                    status = att.status,
                    checkedInAt = att.checkedInAt,
                    notes = att.notes,
                )
            }
        }

        _state.value = _state.value.copy(
            attendance = attendanceList,
            studentItems = items,
        )
    }

    fun updateAttendance(studentId: String, newStatus: String) {
        viewModelScope.launch {
            val currentList = _state.value.studentItems
            val updated = currentList.map { item ->
                if (item.studentId == studentId) item.copy(status = newStatus) else item
            }
            _state.value = _state.value.copy(studentItems = updated)

            repository.recordAttendance(
                sessionId = sessionId,
                studentId = studentId,
                status = newStatus,
            ).onFailure { e ->
                _state.value = _state.value.copy(studentItems = currentList, error = e.message)
            }
        }
    }
}

