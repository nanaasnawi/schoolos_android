package com.schoolos.android.feature.assignments.habits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.schoolos.android.core.auth.AuthManager
import com.schoolos.android.core.common.toHumanReadableError
import com.schoolos.android.domain.model.CanonicalHabitsList
import com.schoolos.android.domain.model.HabitEntry
import com.schoolos.android.domain.repository.HabitRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDate
import javax.inject.Inject

data class HabitsUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isVerifying: Boolean = false,
    val isRefreshing: Boolean = false,
    val userRole: String = "student",
    val studentName: String = "",
    val childName: String = "",
    val effectiveStudentId: String = "",
    val selectedDate: String = LocalDate.now().toString(), // YYYY-MM-DD
    val currentEntry: HabitEntry? = null,
    val habitChecks: Map<String, Boolean> = emptyMap(),
    val habitNotes: Map<String, String> = emptyMap(),
    val weeklyHistory: List<HabitEntry> = emptyList(),
    val completedCount: Int = 0,
    val complianceRate: Double = 0.0,
    val streakDays: Int = 0,
    val verificationStatus: String = "NOT_RECORDED", // NOT_RECORDED, PENDING, PARENT_VERIFIED, TEACHER_OVERRIDE, SYSTEM_EXPIRED
    val parentFeedback: String? = null,
    val parentFeedbackInput: String = "",
    val showParentVerifyDialog: Boolean = false,
    val expandedNoteKey: String? = null,
    val successSnackbar: String? = null,
    val errorMessage: String? = null,
) {
    val isParent: Boolean get() = com.schoolos.android.core.auth.isParentRole(userRole)
    val isTeacher: Boolean get() = com.schoolos.android.core.auth.isTeacherRole(userRole)
}

@HiltViewModel
class HabitsViewModel @Inject constructor(
    private val repository: HabitRepository,
    private val authManager: AuthManager,
) : ViewModel() {

    private val _state = MutableStateFlow(HabitsUiState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            authManager.authState.collect { auth ->
                val role = auth.role ?: "student"
                val isParent = com.schoolos.android.core.auth.isParentRole(role)
                val targetStudentId = auth.childId?.takeIf { it.isNotBlank() } ?: auth.userId.orEmpty()

                _state.update {
                    it.copy(
                        userRole = role,
                        studentName = auth.name ?: "Siswa",
                        childName = auth.childName ?: auth.name ?: "Ananda",
                        effectiveStudentId = targetStudentId
                    )
                }
                loadHabits()
            }
        }
    }

    fun refresh() {
        _state.update { it.copy(isRefreshing = true) }
        loadHabits(isRefresh = true)
    }

    fun loadHabits(isRefresh: Boolean = false) {
        val studentId = _state.value.effectiveStudentId
        viewModelScope.launch {
            if (!isRefresh) {
                _state.update { it.copy(isLoading = true, errorMessage = null) }
            }

            repository.getHabitsCalendar(studentId = studentId.ifBlank { null })
                .onSuccess { list ->
                    val selected = _state.value.selectedDate
                    val activeEntry = list.find { it.entryDate == selected }
                    
                    val initialChecks = mutableMapOf<String, Boolean>()
                    val initialNotes = mutableMapOf<String, String>()

                    CanonicalHabitsList.items.forEach { habit ->
                        initialChecks[habit.key] = activeEntry?.habits?.get(habit.key) ?: false
                        val note = activeEntry?.habitsNotes?.get(habit.key)
                        if (!note.isNullOrBlank()) {
                            initialNotes[habit.key] = note
                        }
                    }

                    val count = initialChecks.count { it.value }
                    val rate = if (activeEntry != null) activeEntry.complianceRate else (count.toDouble() / 7.0 * 100.0)
                    val status = activeEntry?.verificationStatus ?: if (count > 0) "PENDING" else "NOT_RECORDED"

                    // Calculate streak: consecutive days recorded with compliance >= 50%
                    val streak = calculateStreak(list)

                    _state.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            weeklyHistory = list,
                            currentEntry = activeEntry,
                            habitChecks = initialChecks,
                            habitNotes = initialNotes,
                            completedCount = count,
                            complianceRate = rate,
                            verificationStatus = status,
                            parentFeedback = activeEntry?.parentFeedback,
                            streakDays = streak,
                            errorMessage = null
                        )
                    }
                }
                .onFailure { err ->
                    Timber.w(err, "Failed to load habits data")
                    // In offline/initial state, populate default uncompleted habits
                    val defaultChecks = CanonicalHabitsList.items.associate { it.key to false }
                    _state.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            habitChecks = it.habitChecks.ifEmpty { defaultChecks },
                            errorMessage = null // Avoid blocking banner if network is temporarily slow
                        )
                    }
                }
        }
    }

    private fun calculateStreak(entries: List<HabitEntry>): Int {
        if (entries.isEmpty()) return 0
        val sortedDates = entries.filter { it.completedCount >= 4 }
            .mapNotNull { 
                runCatching { LocalDate.parse(it.entryDate) }.getOrNull() 
            }
            .sortedDescending()

        if (sortedDates.isEmpty()) return 0

        var streak = 0
        var checkDate = LocalDate.now()
        
        // If today not yet completed, start checking from yesterday
        if (!sortedDates.contains(checkDate)) {
            checkDate = checkDate.minusDays(1)
        }

        while (sortedDates.contains(checkDate)) {
            streak++
            checkDate = checkDate.minusDays(1)
        }

        return streak
    }

    fun toggleHabit(key: String) {
        if (_state.value.isParent) return // Parent views and verifies, student checks

        _state.update { current ->
            val updatedChecks = current.habitChecks.toMutableMap()
            val currentVal = updatedChecks[key] ?: false
            updatedChecks[key] = !currentVal

            val newCount = updatedChecks.count { it.value }
            val newRate = (newCount.toDouble() / 7.0) * 100.0

            current.copy(
                habitChecks = updatedChecks,
                completedCount = newCount,
                complianceRate = newRate
            )
        }
    }

    fun setNote(key: String, note: String) {
        _state.update { current ->
            val updatedNotes = current.habitNotes.toMutableMap()
            if (note.isBlank()) {
                updatedNotes.remove(key)
            } else {
                updatedNotes[key] = note
            }
            current.copy(habitNotes = updatedNotes)
        }
    }

    fun toggleNoteExpanded(key: String) {
        _state.update { current ->
            val newKey = if (current.expandedNoteKey == key) null else key
            current.copy(expandedNoteKey = newKey)
        }
    }

    fun saveHabits() {
        val stateVal = _state.value
        val studentId = stateVal.effectiveStudentId
        val date = stateVal.selectedDate

        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }

            repository.recordHabitEntry(
                studentId = studentId,
                entryDate = date,
                habits = stateVal.habitChecks,
                habitsNotes = stateVal.habitNotes.filterValues { it.isNotBlank() },
            ).onSuccess { savedEntry ->
                _state.update {
                    it.copy(
                        isSaving = false,
                        currentEntry = savedEntry,
                        verificationStatus = savedEntry.verificationStatus,
                        successSnackbar = "Jurnal 7 Kebiasaan Anak Indonesia Hebat berhasil dicatat! Menunggu verifikasi orang tua."
                    )
                }
                loadHabits(isRefresh = true)
            }.onFailure { err ->
                _state.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = err.toHumanReadableError("Gagal menyimpan jurnal kebiasaan. Periksa koneksi internet Anda.")
                    )
                }
            }
        }
    }

    fun openParentVerifyDialog() {
        _state.update { it.copy(showParentVerifyDialog = true, parentFeedbackInput = it.parentFeedback ?: "") }
    }

    fun closeParentVerifyDialog() {
        _state.update { it.copy(showParentVerifyDialog = false) }
    }

    fun setParentFeedbackInput(text: String) {
        _state.update { it.copy(parentFeedbackInput = text) }
    }

    fun submitParentVerification() {
        val entry = _state.value.currentEntry
        if (entry == null) {
            _state.update { it.copy(errorMessage = "Belum ada catatan jurnal untuk diverifikasi hari ini.") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isVerifying = true, showParentVerifyDialog = false) }

            val feedback = _state.value.parentFeedbackInput.trim().ifBlank {
                "Alhamdulillah ananda telah membiasakan 7 Karakter Hebat hari ini dengan baik."
            }

            repository.verifyHabitParent(
                entryId = entry.id,
                parentFeedback = feedback
            ).onSuccess {
                _state.update {
                    it.copy(
                        isVerifying = false,
                        verificationStatus = "PARENT_VERIFIED",
                        parentFeedback = feedback,
                        successSnackbar = "Verifikasi Orang Tua / Wali berhasil disimpan! Terima kasih atas pendampingan ananda."
                    )
                }
                loadHabits(isRefresh = true)
            }.onFailure { err ->
                _state.update {
                    it.copy(
                        isVerifying = false,
                        errorMessage = err.toHumanReadableError("Gagal memverifikasi jurnal ananda. Periksa koneksi internet Anda.")
                    )
                }
            }
        }
    }

    fun selectDate(date: String) {
        _state.update { it.copy(selectedDate = date) }
        loadHabits()
    }

    fun dismissSnackbar() {
        _state.update { it.copy(successSnackbar = null, errorMessage = null) }
    }
}
