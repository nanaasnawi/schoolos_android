package com.schoolos.android.feature.home

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.schoolos.android.domain.model.Achievement
import com.schoolos.android.domain.model.ClassStudent
import com.schoolos.android.domain.model.Progress
import com.schoolos.android.domain.repository.AcademicRepository
import com.schoolos.android.domain.repository.AchievementRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class StudentDetailUiState(
    val student: ClassStudent,
    val progress: Progress? = null,
    val achievements: List<Achievement> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val selectedTab: Int = 0,
)

@HiltViewModel
class StudentDetailViewModel @Inject constructor(
    private val academicRepository: AcademicRepository,
    private val achievementRepository: AchievementRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val studentId: String = savedStateHandle.get<String>("studentId") ?: ""
    private val studentName: String = savedStateHandle.get<String>("studentName") ?: ""
    private val nisn: String = savedStateHandle.get<String>("nisn") ?: ""
    private val gender: String? = savedStateHandle.get<String>("gender")?.ifBlank { null }
    private val className: String = savedStateHandle.get<String>("className") ?: ""
    private val phone: String? = savedStateHandle.get<String>("phone")?.ifBlank { null }
    private val email: String? = savedStateHandle.get<String>("email")?.ifBlank { null }

    private val initialStudent = ClassStudent(
        id = studentId,
        fullName = studentName.ifBlank { "Detail Siswa" },
        nisn = nisn,
        gender = gender,
        status = "ACTIVE",
        noHp = phone,
        email = email,
        classId = "",
        className = className
    )

    private val _uiState = MutableStateFlow(StudentDetailUiState(student = initialStudent))
    val uiState = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun selectTab(index: Int) {
        _uiState.update { it.copy(selectedTab = index) }
    }

    fun refresh() {
        loadData()
    }

    private fun loadData() {
        if (studentId.isBlank()) {
            _uiState.update { it.copy(isLoading = false) }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                val progressResult = academicRepository.getStudentProgress(studentId)
                val achievementsResult = achievementRepository.getStudentAchievements(studentId)

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        progress = progressResult.getOrNull(),
                        achievements = achievementsResult.getOrDefault(emptyList()),
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading student detail data for student $studentId")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Gagal memuat rincian data siswa"
                    )
                }
            }
        }
    }
}
