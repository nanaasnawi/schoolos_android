package com.schoolos.android.feature.sessions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.schoolos.android.core.auth.AuthManager
import com.schoolos.android.domain.model.LearningSession
import com.schoolos.android.domain.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import javax.inject.Inject

data class TodayUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val userRole: String = "student",
    val className: String = "",
    val classId: String? = null,
    val selectedDate: LocalDate = LocalDate.now(),
    val selectedFilter: String = "ALL", // "ALL", "ACTIVE", "UPCOMING", "COMPLETED"
    val weekScheduleCounts: Map<LocalDate, Int> = emptyMap(),
    val active: List<LearningSession> = emptyList(),
    val upcoming: List<LearningSession> = emptyList(),
    val completed: List<LearningSession> = emptyList(),
    val displayedSessions: List<LearningSession> = emptyList(),
)

@HiltViewModel
class TodayViewModel @Inject constructor(
    private val repository: SessionRepository,
    private val authManager: AuthManager,
) : ViewModel() {

    private val _state = MutableStateFlow(TodayUiState())
    val state = _state.asStateFlow()

    private var allSessionsCache: List<LearningSession> = emptyList()

    init {
        viewModelScope.launch {
            authManager.authState.collect { auth ->
                _state.value = _state.value.copy(
                    userRole = auth.role ?: "student",
                    className = auth.className ?: "",
                    classId = auth.classId,
                )
            }
        }
        load(_state.value.selectedDate)
    }

    fun refresh() {
        _state.value = _state.value.copy(isRefreshing = true)
        load(_state.value.selectedDate)
    }

    fun onDateSelected(date: LocalDate) {
        _state.value = _state.value.copy(selectedDate = date)
        if (allSessionsCache.isNotEmpty()) {
            filterAndGroupSessions(allSessionsCache, date, _state.value.selectedFilter)
        } else {
            load(date)
        }
    }

    fun onFilterSelected(filter: String) {
        _state.value = _state.value.copy(selectedFilter = filter)
        filterAndGroupSessions(allSessionsCache, _state.value.selectedDate, filter)
    }

    private fun load(targetDate: LocalDate = LocalDate.now()) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            repository.getSessions(_state.value.classId)
                .onSuccess { sessions ->
                    allSessionsCache = sessions
                    filterAndGroupSessions(sessions, targetDate, _state.value.selectedFilter)
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        isRefreshing = false,
                        error = e.message ?: "Gagal memuat jadwal pelajaran",
                    )
                }
        }
    }

    private fun filterAndGroupSessions(
        allSessions: List<LearningSession>,
        targetDate: LocalDate,
        filter: String,
    ) {
        // Compute schedule count per date for weekly calendar indicator dots
        val dateCounts = mutableMapOf<LocalDate, Int>()
        allSessions.forEach { s ->
            val d = s.scheduledAt?.let { parseDate(it) } ?: s.startedAt?.let { parseDate(it) }
            if (d != null) {
                dateCounts[d] = (dateCounts[d] ?: 0) + 1
            }
        }

        // Filter sessions that belong to targetDate
        val targetSessions = allSessions.filter { s ->
            val scheduled = s.scheduledAt?.let { parseDate(it) }
            val started = s.startedAt?.let { parseDate(it) }
            scheduled == targetDate || started == targetDate
        }

        val grouped = groupSessions(targetSessions)

        // Apply tab filter
        val displayed = when (filter) {
            "ACTIVE" -> grouped.active
            "UPCOMING" -> grouped.upcoming
            "COMPLETED" -> grouped.completed
            else -> targetSessions.sortedWith(
                compareBy<LearningSession> {
                    when (it.status.lowercase()) {
                        "active" -> 0
                        "scheduled" -> 1
                        "completed" -> 2
                        else -> 3
                    }
                }.thenBy { it.scheduledAt ?: it.startedAt ?: "" }
            )
        }

        _state.value = _state.value.copy(
            isLoading = false,
            isRefreshing = false,
            selectedDate = targetDate,
            weekScheduleCounts = dateCounts,
            active = grouped.active,
            upcoming = grouped.upcoming,
            completed = grouped.completed,
            displayedSessions = displayed,
        )
    }

    private fun groupSessions(sessions: List<LearningSession>): Grouped {
        val active = mutableListOf<LearningSession>()
        val upcoming = mutableListOf<LearningSession>()
        val completed = mutableListOf<LearningSession>()

        for (s in sessions) {
            when (s.status.lowercase()) {
                "active" -> active.add(s)
                "completed" -> completed.add(s)
                else -> upcoming.add(s)
            }
        }

        return Grouped(
            active = active.sortedBy { it.scheduledAt },
            upcoming = upcoming.sortedBy { it.scheduledAt },
            completed = completed.sortedByDescending { it.endedAt ?: it.scheduledAt ?: "" },
        )
    }

    private fun parseDate(iso: String): LocalDate? {
        return try {
            Instant.parse(iso).atZone(ZoneId.systemDefault()).toLocalDate()
        } catch (_: Exception) {
            try {
                ZonedDateTime.parse(iso).withZoneSameInstant(ZoneId.systemDefault()).toLocalDate()
            } catch (_: Exception) {
                try {
                    LocalDate.parse(iso.substringBefore("T"))
                } catch (_: Exception) { null }
            }
        }
    }

    private data class Grouped(
        val active: List<LearningSession>,
        val upcoming: List<LearningSession>,
        val completed: List<LearningSession>,
    )
}
