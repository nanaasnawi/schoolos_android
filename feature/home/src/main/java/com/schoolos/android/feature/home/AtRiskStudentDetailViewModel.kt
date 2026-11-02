package com.schoolos.android.feature.home

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.schoolos.android.core.common.toHumanReadableError
import com.schoolos.android.domain.model.AtRiskStudent
import com.schoolos.android.domain.repository.AtRiskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class AtRiskStudentDetailUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val studentId: String = "",
    val studentName: String = "",
    val nisn: String = "",
    val className: String = "",
    val items: List<AtRiskStudent> = emptyList(),
    val selectedCategoryFilter: String = "ALL", // "ALL" | "UNREAD_MATERIAL" | "OVERDUE_ASSIGNMENT" | "LOW_SCORE" | "QUIZ_ALERT"
    val isSendingAll: Boolean = false,
    val sendingItemId: String? = null,
    val toastMessage: String? = null,
    val error: String? = null,
) {
    val totalIssues: Int get() = items.size
    val unreadMaterialsCount: Int get() = items.count { it.category == "UNREAD_MATERIAL" }
    val overdueAssignmentsCount: Int get() = items.count { it.category == "OVERDUE_ASSIGNMENT" }
    val lowScoresCount: Int get() = items.count { it.category == "LOW_SCORE" }
    val quizAlertsCount: Int get() = items.count { it.category == "QUIZ_ALERT" }

    val filteredItems: List<AtRiskStudent>
        get() = if (selectedCategoryFilter == "ALL") items
        else items.filter { it.category == selectedCategoryFilter }
}

@HiltViewModel
class AtRiskStudentDetailViewModel @Inject constructor(
    private val atRiskRepository: AtRiskRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val studentId: String = savedStateHandle.get<String>("studentId").orEmpty()
    private val studentNameParam: String = savedStateHandle.get<String>("studentName").orEmpty()
    private val nisnParam: String = savedStateHandle.get<String>("nisn").orEmpty()
    private val classNameParam: String = savedStateHandle.get<String>("className").orEmpty()

    private val _state = MutableStateFlow(
        AtRiskStudentDetailUiState(
            studentId = studentId,
            studentName = studentNameParam,
            nisn = nisnParam,
            className = classNameParam,
        )
    )
    val state = _state.asStateFlow()

    init {
        loadDetails()
    }

    fun loadDetails(isRefresh: Boolean = false) {
        viewModelScope.launch {
            if (isRefresh) {
                _state.update { it.copy(isRefreshing = true, error = null) }
            } else {
                _state.update { it.copy(isLoading = true, error = null) }
            }

            atRiskRepository.getAtRiskStudentDetail(studentId)
                .onSuccess { list ->
                    val resolvedName = list.firstOrNull()?.studentName?.takeIf { it.isNotBlank() } ?: studentNameParam
                    val resolvedNisn = list.firstOrNull()?.nisn?.takeIf { it.isNotBlank() } ?: nisnParam
                    val resolvedClass = list.firstOrNull()?.className?.takeIf { it.isNotBlank() } ?: classNameParam

                    _state.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            items = list,
                            studentName = resolvedName,
                            nisn = resolvedNisn,
                            className = resolvedClass,
                            error = null,
                        )
                    }
                }
                .onFailure { err ->
                    Timber.e(err, "Failed to load at-risk details for student $studentId")
                    _state.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            error = err.toHumanReadableError("Gagal memuat rincian kendala siswa"),
                        )
                    }
                }
        }
    }

    fun onSelectCategoryFilter(category: String) {
        _state.update { it.copy(selectedCategoryFilter = category) }
    }

    fun sendSingleReminder(item: AtRiskStudent) {
        val itemKey = "${item.studentId}_${item.category}_${item.materialId ?: item.assignmentId ?: item.quizId}"
        viewModelScope.launch {
            _state.update { it.copy(sendingItemId = itemKey) }
            atRiskRepository.sendSingleReminder(item)
                .onSuccess {
                    _state.update { s ->
                        val updatedItems = s.items.map { cur ->
                            if (cur == item) cur.copy(isReminded = true) else cur
                        }
                        s.copy(
                            sendingItemId = null,
                            items = updatedItems,
                            toastMessage = "Pengingat untuk ${item.categoryLabel} berhasil dikirim ke ${s.studentName}"
                        )
                    }
                }
                .onFailure { err ->
                    Timber.w(err, "Failed to send single reminder")
                    _state.update {
                        it.copy(
                            sendingItemId = null,
                            toastMessage = "Gagal mengirim pengingat: ${err.message ?: "Kendala jaringan"}"
                        )
                    }
                }
        }
    }

    fun sendAllReminders() {
        val currentItems = _state.value.items
        if (currentItems.isEmpty()) return

        viewModelScope.launch {
            _state.update { it.copy(isSendingAll = true) }
            // Send batch reminder for all active issues of this student
            atRiskRepository.sendBatchReminders(currentItems, "ALL")
                .onSuccess {
                    _state.update { s ->
                        val updatedItems = s.items.map { it.copy(isReminded = true) }
                        s.copy(
                            isSendingAll = false,
                            items = updatedItems,
                            toastMessage = "Seluruh pengingat kendala belajar berhasil dikirim ke ${s.studentName}"
                        )
                    }
                }
                .onFailure { err ->
                    Timber.w(err, "Failed to send batch reminder for student")
                    _state.update {
                        it.copy(
                            isSendingAll = false,
                            toastMessage = "Gagal mengirim pengingat terpadu: ${err.message ?: "Kendala server"}"
                        )
                    }
                }
        }
    }

    fun clearToast() {
        _state.update { it.copy(toastMessage = null) }
    }
}

