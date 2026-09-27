package com.schoolos.android.feature.grades

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.schoolos.android.domain.model.AcademicClass
import com.schoolos.android.domain.model.AcademicSubject
import com.schoolos.android.domain.model.SubjectGradeSummary
import com.schoolos.android.domain.model.toSubjectSummary
import com.schoolos.android.domain.repository.AcademicRepository
import com.schoolos.android.domain.repository.GradeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChangedBy
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
    private var loadJob: Job? = null
    private var hasLoadedOnce: Boolean = false

    // In-memory cache for static school academic structures
    private var cachedSubjects: List<AcademicSubject>? = null
    private var cachedClasses: List<AcademicClass>? = null

    init {
        // Initial load: fetch once cleanly without collecting continuous DataStore triggers
        viewModelScope.launch {
            val initialAuth = authManager.authState.first()
            currentClassId = initialAuth.classId ?: ""
            val homeroom = initialAuth.className?.takeIf { it.isNotBlank() } ?: ""
            val name = initialAuth.name?.takeIf { it.isNotBlank() } ?: "Guru Pengampu"
            _state.value = _state.value.copy(
                userRole = initialAuth.role ?: "student",
                className = homeroom,
                teacherName = name,
            )
            load(homeroom, currentClassId, isInitial = true)
        }

        // Only reload if the user's role or class assignment actually changed structurally
        viewModelScope.launch {
            authManager.authState
                .distinctUntilChangedBy { Triple(it.role, it.className, it.classId) }
                .collect { auth ->
                    val newClassId = auth.classId ?: ""
                    val newHomeroom = auth.className?.takeIf { it.isNotBlank() } ?: ""
                    val newRole = auth.role ?: "student"
                    val name = auth.name?.takeIf { it.isNotBlank() } ?: "Guru Pengampu"

                    val classOrRoleChanged = hasLoadedOnce && (
                        _state.value.userRole != newRole ||
                        _state.value.className != newHomeroom ||
                        currentClassId != newClassId
                    )

                    _state.value = _state.value.copy(
                        userRole = newRole,
                        className = newHomeroom,
                        teacherName = name,
                    )

                    if (classOrRoleChanged) {
                        currentClassId = newClassId
                        load(newHomeroom, newClassId, isInitial = false)
                    }
                }
        }
    }

    fun refresh() {
        _state.value = _state.value.copy(isRefreshing = true)
        viewModelScope.launch {
            val auth = authManager.authState.first()
            load(auth.className.orEmpty(), auth.classId.orEmpty(), isInitial = false, forceRefresh = true)
        }
    }

    private fun load(
        className: String,
        classId: String,
        isInitial: Boolean,
        forceRefresh: Boolean = false,
    ) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            // Keep existing UI rendered if we already have subjects to prevent flickering
            _state.value = _state.value.copy(
                isLoading = _state.value.subjects.isEmpty(),
                error = null,
            )

            // Resolve target class ID if not yet available in AuthState
            var targetClassId = classId
            if (targetClassId.isBlank() && className.isNotBlank()) {
                val classes = cachedClasses ?: run {
                    val res = academicRepository.getClasses()
                    res.getOrNull()?.also { cachedClasses = it } ?: emptyList()
                }
                val matched = classes.find { it.name.equals(className, ignoreCase = true) }
                if (matched != null) {
                    targetClassId = matched.id
                    currentClassId = matched.id
                }
            }

            // Retrieve subjects (from cache if already loaded unless forceRefresh)
            val allSubjects = if (!forceRefresh && cachedSubjects != null) {
                cachedSubjects!!
            } else {
                val res = academicRepository.getSubjects()
                val list = res.getOrDefault(emptyList()).distinctBy { it.id }
                if (list.isNotEmpty()) cachedSubjects = list
                list
            }
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

                    hasLoadedOnce = true
                    _state.value = _state.value.copy(
                        isLoading = false,
                        isRefreshing = false,
                        subjects = finalSubjects,
                        error = null,
                    )
                }
                .onFailure { e ->
                    // Fallback to subjects with real un-graded status (no fake dummy scores)
                    val emptySummaries = allSubjects.distinctBy { it.id }.map { subj ->
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
                    hasLoadedOnce = true
                    _state.value = _state.value.copy(
                        isLoading = false,
                        isRefreshing = false,
                        subjects = if (_state.value.subjects.isNotEmpty()) _state.value.subjects else emptySummaries,
                        error = if (_state.value.subjects.isEmpty() && emptySummaries.isEmpty()) e.message ?: "Gagal memuat buku nilai" else null,
                    )
                }
        }
    }
}
