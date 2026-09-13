package com.schoolos.android.feature.grades

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.schoolos.android.domain.model.SubjectGradeSummary
import com.schoolos.android.domain.model.toSubjectSummary
import com.schoolos.android.domain.repository.AcademicRepository
import com.schoolos.android.domain.repository.GradeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GradebookUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val userRole: String = "student",
    val className: String = "",
    val teacherName: String = "",
    val subjects: List<SubjectGradeSummary> = emptyList(),
)

@HiltViewModel
class GradebookListViewModel @Inject constructor(
    private val repository: GradeRepository,
    private val academicRepository: AcademicRepository,
    private val authManager: com.schoolos.android.core.auth.AuthManager,
) : ViewModel() {

    private val _state = MutableStateFlow(GradebookUiState())
    val state = _state.asStateFlow()

    private var currentClassId: String = ""

    init {
        viewModelScope.launch {
            authManager.authState.collect { auth ->
                currentClassId = auth.classId ?: ""
                val homeroom = auth.className?.takeIf { it.isNotBlank() } ?: ""
                val name = auth.name?.takeIf { it.isNotBlank() } ?: "Guru Pengampu"
                _state.value = _state.value.copy(
                    userRole = auth.role ?: "student",
                    className = homeroom,
                    teacherName = name,
                )
                load(homeroom, currentClassId)
            }
        }
    }

    fun refresh() {
        _state.value = _state.value.copy(isRefreshing = true)
        viewModelScope.launch {
            val auth = authManager.authState.first()
            load(auth.className.orEmpty(), auth.classId.orEmpty())
        }
    }

    private fun load(className: String, classId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)

            // Resolve target class ID if not yet available in AuthState
            var targetClassId = classId
            if (targetClassId.isBlank() && className.isNotBlank()) {
                val classesResult = academicRepository.getClasses()
                val matched = classesResult.getOrNull()?.find {
                    it.name.equals(className, ignoreCase = true)
                }
                if (matched != null) {
                    targetClassId = matched.id
                    currentClassId = matched.id
                }
            }

            val subjectsResult = academicRepository.getSubjects()
            val allSubjects = subjectsResult.getOrDefault(emptyList())
            val subjectMap = allSubjects.associate { it.id to it.name }

            repository.getGradebook(targetClassId.ifBlank { null })
                .onSuccess { entries ->
                    val subjectIds = entries.map { it.subjectId }.distinct()
                    val entrySummaries = subjectIds.mapNotNull { id ->
                        val realName = subjectMap[id] ?: return@mapNotNull null
                        entries.toSubjectSummary(id, realName)
                    }

                    // For curriculum subjects without grade entries yet, represent real un-graded state
                    val existingIds = entrySummaries.map { it.subjectId }.toSet()
                    val unGradedSummaries = allSubjects
                        .filter { it.id !in existingIds }
                        .map { subj ->
                            SubjectGradeSummary(
                                subjectId = subj.id,
                                subjectName = subj.name,
                                finalScore = 0.0,
                                letterGrade = "-",
                                completionPercentage = 0.0,
                                lastCalculated = "",
                                componentCount = 0,
                                gradedComponentCount = 0,
                            )
                        }

                    val combined = (entrySummaries + unGradedSummaries).distinctBy { it.subjectId }
                    val finalSubjects = combined.sortedWith(
                        compareByDescending<SubjectGradeSummary> { it.finalScore > 0 }
                            .thenByDescending { it.finalScore }
                            .thenBy { it.subjectName }
                    )

                    _state.value = _state.value.copy(
                        isLoading = false,
                        isRefreshing = false,
                        subjects = finalSubjects,
                        error = null,
                    )
                }
                .onFailure { e ->
                    // Fallback to subjects with real un-graded status (no fake dummy scores)
                    val emptySummaries = allSubjects.map { subj ->
                        SubjectGradeSummary(
                            subjectId = subj.id,
                            subjectName = subj.name,
                            finalScore = 0.0,
                            letterGrade = "-",
                            completionPercentage = 0.0,
                            lastCalculated = "",
                            componentCount = 0,
                            gradedComponentCount = 0,
                        )
                    }
                    _state.value = _state.value.copy(
                        isLoading = false,
                        isRefreshing = false,
                        subjects = emptySummaries,
                        error = if (emptySummaries.isEmpty()) e.message ?: "Gagal memuat buku nilai" else null,
                    )
                }
        }
    }
}
