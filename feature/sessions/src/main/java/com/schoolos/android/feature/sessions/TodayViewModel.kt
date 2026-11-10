package com.schoolos.android.feature.sessions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.schoolos.android.core.auth.AuthManager
import com.schoolos.android.core.common.toHumanReadableError
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
    private var hasLoadedInitialData: Boolean = false
    private var currentAuth: com.schoolos.android.core.auth.AuthState = com.schoolos.android.core.auth.AuthState()

    init {
        viewModelScope.launch {
            authManager.authState.collect { auth ->
                currentAuth = auth
                _state.value = _state.value.copy(
                    userRole = auth.role ?: "student",
                    className = auth.className ?: "",
                    classId = auth.classId,
                )
                load(_state.value.selectedDate)
            }
        }
    }

    fun refresh(isPullRefresh: Boolean = false) {
        if (!isPullRefresh && hasLoadedInitialData) return
        if (isPullRefresh) {
            _state.value = _state.value.copy(isRefreshing = true)
        }
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
            _state.value = _state.value.copy(
                isLoading = !hasLoadedInitialData && allSessionsCache.isEmpty(),
                error = null
            )
            val role = _state.value.userRole.lowercase()
            val isTeacherOrStaff = role.contains("teacher") || role.contains("guru") || role.contains("principal") || role.contains("kepala") || role.contains("kepsek")
            val targetClassId = if (isTeacherOrStaff) null else _state.value.classId
            repository.getSessions(targetClassId)
                .onSuccess { sessions ->
                    hasLoadedInitialData = true
                    allSessionsCache = sessions
                    filterAndGroupSessions(sessions, targetDate, _state.value.selectedFilter)
                }
                .onFailure { e ->
                    hasLoadedInitialData = true
                    _state.value = _state.value.copy(
                        isLoading = false,
                        isRefreshing = false,
                        error = e.toHumanReadableError("Gagal memuat jadwal pelajaran. Periksa koneksi internet Anda."),
                    )
                }
        }
    }

    private fun filterAndGroupSessions(
        allSessions: List<LearningSession>,
        targetDate: LocalDate,
        filter: String,
    ) {
        val role = _state.value.userRole.lowercase()
        val isPrincipal = role.contains("principal") || role.contains("kepala") || role.contains("kepsek")
        val isTeacher = (role.contains("teacher") || role.contains("guru")) && !isPrincipal
        val teacherUserId = currentAuth.userId ?: ""
        val teacherName = currentAuth.name ?: ""

        // For teachers: strictly filter sessions to those taught by this teacher
        // For principals: keep all sessions for executive bird's-eye view across all classes
        val cleanTeacherName = teacherName.trim().replace(Regex("\\s+"), " ").lowercase()
        val scopedSessions = if (isPrincipal) {
            allSessions
        } else if (isTeacher && (teacherUserId.isNotBlank() || cleanTeacherName.isNotBlank())) {
            allSessions.filter { s ->
                val uidMatch = teacherUserId.isNotBlank() && (
                    s.teacherId.equals(teacherUserId, ignoreCase = true) ||
                    s.substituteTeacherId?.equals(teacherUserId, ignoreCase = true) == true
                )
                val sTeacher = s.teacherName?.trim()?.replace(Regex("\\s+"), " ")?.lowercase()
                val sSubTeacher = s.substituteTeacherName?.trim()?.replace(Regex("\\s+"), " ")?.lowercase()
                val nameMatch = cleanTeacherName.isNotBlank() && (
                    (!sTeacher.isNullOrBlank() && sTeacher == cleanTeacherName) ||
                    (!sSubTeacher.isNullOrBlank() && sSubTeacher == cleanTeacherName)
                )
                uidMatch || nameMatch
            }
        } else {
            allSessions
        }

        val isTeacherOrPrincipal = isTeacher || isPrincipal

        // Compute schedule count per date for weekly calendar indicator dots
        val dateCounts = mutableMapOf<LocalDate, Int>()
        scopedSessions.forEach { s ->
            val d = extractSessionDate(s)
            if (d != null) {
                dateCounts[d] = (dateCounts[d] ?: 0) + 1
            }
        }

        // Filter sessions that belong to targetDate
        val targetSessions = scopedSessions.filter { s ->
            extractSessionDate(s) == targetDate
        }

        val grouped = groupSessions(targetSessions, isTeacherOrPrincipal)

        // Apply tab filter
        val displayed = when (filter) {
            "ACTIVE" -> grouped.active
            "UPCOMING" -> grouped.upcoming
            "COMPLETED" -> grouped.completed
            else -> if (isTeacherOrPrincipal) {
                targetSessions.sortedWith { a, b -> compareSessionsForTeacherOrPrincipal(a, b) }
            } else {
                targetSessions.sortedWith(
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

    private fun groupSessions(sessions: List<LearningSession>, isTeacherOrPrincipal: Boolean = false): Grouped {
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

        return if (isTeacherOrPrincipal) {
            Grouped(
                active = active.sortedWith { a, b -> compareSessionsForTeacherOrPrincipal(a, b) },
                upcoming = upcoming.sortedWith { a, b -> compareSessionsForTeacherOrPrincipal(a, b) },
                completed = completed.sortedWith { a, b -> compareSessionsForTeacherOrPrincipal(a, b) },
            )
        } else {
            Grouped(
                active = active.sortedBy { it.scheduledAt },
                upcoming = upcoming.sortedBy { it.scheduledAt },
                completed = completed.sortedByDescending { it.endedAt ?: it.scheduledAt ?: "" },
            )
        }
    }

    private fun compareSessionsForTeacherOrPrincipal(a: LearningSession, b: LearningSession): Int {
        val classCmp = compareClassNames(a.className, b.className)
        if (classCmp != 0) return classCmp

        val timeA = extractSessionTime(a)
        val timeB = extractSessionTime(b)
        val timeCmp = timeA.compareTo(timeB)
        if (timeCmp != 0) return timeCmp

        return a.id.compareTo(b.id)
    }

    private fun compareClassNames(a: String?, b: String?): Int {
        if (a.isNullOrBlank() && b.isNullOrBlank()) return 0
        if (a.isNullOrBlank()) return 1
        if (b.isNullOrBlank()) return -1

        fun rankCategory(name: String): Int {
            val upper = name.uppercase()
            return when {
                upper.contains("PAKET A") || upper.contains("SD") -> 10
                upper.contains("PAKET B") || upper.contains("SMP") -> 20
                upper.contains("PAKET C") || upper.contains("SMA") || upper.contains("SMK") -> 30
                else -> 50
            }
        }

        val rankA = rankCategory(a)
        val rankB = rankCategory(b)
        if (rankA != rankB) return rankA.compareTo(rankB)

        val numRegex = Regex("(\\d+)")
        val numA = numRegex.find(a)?.value?.toIntOrNull() ?: 0
        val numB = numRegex.find(b)?.value?.toIntOrNull() ?: 0
        if (numA != numB) return numA.compareTo(numB)

        return a.compareTo(b, ignoreCase = true)
    }

    private fun extractSessionTime(s: LearningSession): String {
        val rawTime = s.startTime?.trim()
        if (!rawTime.isNullOrBlank()) {
            return rawTime.take(5)
        }
        val iso = s.scheduledAt ?: s.startedAt ?: ""
        if (iso.isBlank()) return "00:00"
        val clean = iso.trim()
        val zone = ZoneId.of("Asia/Jakarta")
        return try {
            val zdt = Instant.parse(clean).atZone(zone)
            String.format(java.util.Locale.US, "%02d:%02d", zdt.hour, zdt.minute)
        } catch (_: Exception) {
            try {
                val zdt = ZonedDateTime.parse(clean).withZoneSameInstant(zone)
                String.format(java.util.Locale.US, "%02d:%02d", zdt.hour, zdt.minute)
            } catch (_: Exception) {
                if (clean.length >= 16 && clean.contains("T")) {
                    clean.substringAfter("T").take(5)
                } else {
                    "00:00"
                }
            }
        }
    }

    private fun extractSessionDate(s: LearningSession): LocalDate? {
        val fromSessionDate = s.sessionDate?.let { dateStr ->
            val clean = dateStr.trim().substringBefore(" ").substringBefore("T")
            try {
                LocalDate.parse(clean)
            } catch (_: Exception) {
                null
            }
        }
        return fromSessionDate ?: s.scheduledAt?.let { parseDate(it) } ?: s.startedAt?.let { parseDate(it) }
    }

    private fun parseDate(iso: String): LocalDate? {
        val clean = iso.trim()
        val zone = ZoneId.of("Asia/Jakarta")
        return try {
            Instant.parse(clean).atZone(zone).toLocalDate()
        } catch (_: Exception) {
            try {
                ZonedDateTime.parse(clean).withZoneSameInstant(zone).toLocalDate()
            } catch (_: Exception) {
                try {
                    val datePart = clean.substringBefore(" ").substringBefore("T")
                    LocalDate.parse(datePart)
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
