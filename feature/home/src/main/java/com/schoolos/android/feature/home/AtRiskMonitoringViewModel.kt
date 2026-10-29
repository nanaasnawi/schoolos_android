package com.schoolos.android.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.schoolos.android.core.common.toHumanReadableError
import com.schoolos.android.domain.model.AcademicClass
import com.schoolos.android.domain.model.AtRiskStats
import com.schoolos.android.domain.model.AtRiskStudent
import com.schoolos.android.domain.repository.AcademicRepository
import com.schoolos.android.domain.repository.AtRiskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class AtRiskUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isSendingBatch: Boolean = false,
    val sendingStudentId: String? = null,
    val error: String? = null,
    val userMessage: String? = null,
    val allStudents: List<AtRiskStudent> = emptyList(),
    val availableClasses: List<AcademicClass> = emptyList(),
    val selectedClassFilter: String = "ALL", // "ALL" or rombel name
    val selectedCategoryFilter: String = "ALL", // "ALL" | "UNREAD_MATERIAL" | "OVERDUE_ASSIGNMENT" | "LOW_SCORE"
    val searchQuery: String = "",
) {
    val stats: AtRiskStats
        get() {
            val scoped = allStudents.filter { s ->
                if (selectedClassFilter == "ALL" || selectedClassFilter.isBlank()) true
                else s.className.equals(selectedClassFilter, ignoreCase = true)
            }
            var unread = 0
            var overdue = 0
            var lowScore = 0
            for (s in scoped) {
                when (s.category) {
                    "UNREAD_MATERIAL" -> unread++
                    "OVERDUE_ASSIGNMENT" -> overdue++
                    "LOW_SCORE" -> lowScore++
                }
            }
            return AtRiskStats(
                total = scoped.size,
                unreadMaterials = unread,
                overdueAssignments = overdue,
                lowScores = lowScore,
            )
        }

    val filteredStudents: List<AtRiskStudent>
        get() {
            return allStudents.filter { s ->
                // 1. Class filter
                if (selectedClassFilter != "ALL" && selectedClassFilter.isNotBlank()) {
                    if (!s.className.equals(selectedClassFilter, ignoreCase = true)) return@filter false
                }
                // 2. Category tab filter
                if (selectedCategoryFilter != "ALL" && s.category != selectedCategoryFilter) {
                    return@filter false
                }
                // 3. Search query
                if (searchQuery.isNotBlank()) {
                    val q = searchQuery.trim().lowercase()
                    val matchesName = s.studentName.lowercase().contains(q)
                    val matchesNisn = s.nisn.lowercase().contains(q)
                    val matchesClass = s.className.lowercase().contains(q)
                    val matchesTitle = s.title.lowercase().contains(q)
                    if (!matchesName && !matchesNisn && !matchesClass && !matchesTitle) {
                        return@filter false
                    }
                }
                true
            }
        }
}

@HiltViewModel
class AtRiskMonitoringViewModel @Inject constructor(
    private val atRiskRepository: AtRiskRepository,
    private val academicRepository: AcademicRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(AtRiskUiState())
    val state = _state.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val classes = academicRepository.getClasses().getOrDefault(emptyList())
                val studentsResult = atRiskRepository.getAtRiskStudents()
                studentsResult.fold(
                    onSuccess = { list ->
                        _state.update {
                            it.copy(
                                isLoading = false,
                                isRefreshing = false,
                                allStudents = list,
                                availableClasses = classes,
                            )
                        }
                    },
                    onFailure = { err ->
                        _state.update {
                            it.copy(
                                isLoading = false,
                                isRefreshing = false,
                                error = err.toHumanReadableError("Gagal memuat data siswa perlu perhatian. Periksa koneksi internet Anda."),
                                availableClasses = classes,
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                Timber.w(e, "Error loading at risk students")
                _state.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        error = e.toHumanReadableError("Gagal memuat data siswa perlu perhatian. Periksa koneksi internet Anda.")
                    )
                }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true) }
            val studentsResult = atRiskRepository.getAtRiskStudents()
            studentsResult.fold(
                onSuccess = { list ->
                    _state.update { it.copy(isRefreshing = false, allStudents = list) }
                },
                onFailure = {
                    _state.update { it.copy(isRefreshing = false) }
                }
            )
        }
    }

    fun onSelectClassFilter(className: String) {
        _state.update { it.copy(selectedClassFilter = className) }
    }

    fun onSelectCategoryFilter(category: String) {
        _state.update { it.copy(selectedCategoryFilter = category) }
    }

    fun onSearchQueryChanged(q: String) {
        _state.update { it.copy(searchQuery = q) }
    }

    fun clearUserMessage() {
        _state.update { it.copy(userMessage = null) }
    }

    fun sendSingleReminder(student: AtRiskStudent) {
        viewModelScope.launch {
            _state.update { it.copy(sendingStudentId = student.studentId) }
            val result = atRiskRepository.sendSingleReminder(student)
            result.fold(
                onSuccess = {
                    // Update state to marked as reminded
                    val updated = _state.value.allStudents.map {
                        if (it.studentId == student.studentId && it.materialId == student.materialId) {
                            it.copy(isReminded = true, remindedAt = "Baru saja")
                        } else it
                    }
                    _state.update {
                        it.copy(
                            sendingStudentId = null,
                            allStudents = updated,
                            userMessage = "Pengingat berhasil dikirim ke ${student.studentName} secara realtime."
                        )
                    }
                },
                onFailure = { err ->
                    _state.update {
                        it.copy(
                            sendingStudentId = null,
                            userMessage = err.toHumanReadableError("Gagal mengirim pengingat ke siswa.")
                        )
                    }
                }
            )
        }
    }

    fun sendBatchReminder() {
        viewModelScope.launch {
            val targets = _state.value.filteredStudents
            if (targets.isEmpty() || _state.value.isSendingBatch) return@launch
            _state.update { it.copy(isSendingBatch = true) }
            val category = _state.value.selectedCategoryFilter.let { if (it == "ALL") "UNREAD_MATERIAL" else it }
            val result = atRiskRepository.sendBatchReminders(targets, category)
            result.fold(
                onSuccess = { count ->
                    val targetIds = targets.map { it.studentId }.toSet()
                    val updated = _state.value.allStudents.map {
                        if (targetIds.contains(it.studentId)) {
                            it.copy(isReminded = true, remindedAt = "Baru saja")
                        } else it
                    }
                    _state.update {
                        it.copy(
                            isSendingBatch = false,
                            allStudents = updated,
                            userMessage = "Pengingat serentak berhasil dikirim ke $count siswa."
                        )
                    }
                },
                onFailure = { err ->
                    _state.update {
                        it.copy(
                            isSendingBatch = false,
                            userMessage = err.toHumanReadableError("Gagal mengirim pengingat serentak.")
                        )
                    }
                }
            )
        }
    }
}
