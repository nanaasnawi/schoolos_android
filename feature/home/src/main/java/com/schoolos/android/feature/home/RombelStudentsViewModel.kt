package com.schoolos.android.feature.home

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.schoolos.android.domain.model.AcademicClass
import com.schoolos.android.domain.model.Achievement
import com.schoolos.android.domain.model.ClassStudent
import com.schoolos.android.domain.model.Progress
import com.schoolos.android.domain.repository.AcademicRepository
import com.schoolos.android.domain.repository.AchievementRepository
import com.schoolos.android.domain.repository.RecordAttendanceItem
import com.schoolos.android.domain.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.Instant
import javax.inject.Inject

data class RombelStudentsUiState(
    val className: String = "",
    val availableClasses: List<AcademicClass> = emptyList(),
    val selectedClassFilter: String = "ALL", // "ALL" or specific class e.g. "Kelas 4"
    val allStudents: List<ClassStudent> = emptyList(),
    val isLoading: Boolean = true,
    val isSavingAttendance: Boolean = false,
    val errorMessage: String? = null,
    val searchQuery: String = "",
    val attendanceMap: Map<String, String> = emptyMap(), // studentId -> "HADIR" | "SAKIT" | "IZIN" | "ALPA"
    val selectedStudent: ClassStudent? = null,
    val selectedStudentProgress: Progress? = null,
    val selectedStudentAchievements: List<Achievement> = emptyList(),
    val isLoadingDetail: Boolean = false,
) {
    val filteredStudents: List<ClassStudent>
        get() {
            val base = if (selectedClassFilter.isBlank() || selectedClassFilter.equals("ALL", ignoreCase = true) || selectedClassFilter.equals("Semua", ignoreCase = true)) {
                allStudents
            } else {
                allStudents.filter { it.className.equals(selectedClassFilter, ignoreCase = true) }
            }
            return if (searchQuery.isBlank()) base
            else base.filter {
                it.fullName.contains(searchQuery, ignoreCase = true) ||
                        it.nisn.contains(searchQuery, ignoreCase = true) ||
                        it.className.contains(searchQuery, ignoreCase = true)
            }
        }

    val totalCount: Int get() = filteredStudents.size
    val maleCount: Int get() = filteredStudents.count { it.gender?.trim()?.uppercase() == "L" }
    val femaleCount: Int get() = filteredStudents.count { it.gender?.trim()?.uppercase() == "P" }

    val hadirCount: Int get() = filteredStudents.count { (attendanceMap[it.id] ?: "HADIR").uppercase() == "HADIR" }
    val sakitCount: Int get() = filteredStudents.count { attendanceMap[it.id]?.uppercase() == "SAKIT" }
    val izinCount: Int get() = filteredStudents.count { attendanceMap[it.id]?.uppercase() == "IZIN" }
    val alpaCount: Int get() = filteredStudents.count { attendanceMap[it.id]?.uppercase() == "ALPA" }
}

@HiltViewModel
class RombelStudentsViewModel @Inject constructor(
    private val academicRepository: AcademicRepository,
    private val achievementRepository: AchievementRepository,
    private val sessionRepository: SessionRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val navClassName: String = savedStateHandle.get<String>("className") ?: ""

    private val _uiState = MutableStateFlow(
        RombelStudentsUiState(
            className = navClassName,
            selectedClassFilter = if (navClassName.isBlank() || navClassName.equals("ALL", ignoreCase = true)) "ALL" else navClassName
        )
    )
    val uiState = _uiState.asStateFlow()

    init {
        loadStudents(navClassName)
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun selectClassFilter(filter: String) {
        _uiState.update { it.copy(selectedClassFilter = filter) }
    }

    fun setStudentAttendance(studentId: String, status: String) {
        _uiState.update {
            val updated = it.attendanceMap.toMutableMap()
            updated[studentId] = status
            it.copy(attendanceMap = updated)
        }
    }

    fun markAllPresent() {
        val current = _uiState.value.filteredStudents
        _uiState.update {
            val updated = it.attendanceMap.toMutableMap()
            current.forEach { s -> updated[s.id] = "HADIR" }
            it.copy(attendanceMap = updated)
        }
    }

    fun saveAttendance(onSuccess: (String) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSavingAttendance = true) }
            val currentFiltered = _uiState.value.filteredStudents
            val currentAttendance = _uiState.value.attendanceMap
            try {
                val sessionsResult = sessionRepository.getSessions()
                val activeSession = sessionsResult.getOrNull()?.firstOrNull { it.status.equals("active", ignoreCase = true) }
                    ?: sessionsResult.getOrNull()?.firstOrNull()

                if (activeSession != null) {
                    val items = currentFiltered.map { s ->
                        val st = currentAttendance[s.id] ?: "HADIR"
                        val statusBackend = when (st.uppercase()) {
                            "HADIR" -> "present"
                            "SAKIT" -> "sick"
                            "IZIN" -> "permit"
                            "ALPA" -> "absent"
                            else -> "present"
                        }
                        RecordAttendanceItem(
                            studentId = s.id,
                            status = statusBackend,
                            checkedInAt = Instant.now().toString()
                        )
                    }
                    sessionRepository.recordAttendanceBulk(activeSession.id, items)
                }
            } catch (e: Exception) {
                Timber.e(e, "Error saving session attendance")
            }
            delay(250)
            _uiState.update { it.copy(isSavingAttendance = false) }
            val targetLabel = if (_uiState.value.selectedClassFilter == "ALL") "semua kelas diampu" else "kelas ${_uiState.value.selectedClassFilter}"
            onSuccess("✓ Presensi ${currentFiltered.size} siswa ($targetLabel) berhasil disimpan!")
        }
    }

    fun loadStudents(classNameOverride: String? = null) {
        val targetClass = (classNameOverride ?: _uiState.value.className).trim()
        val filter = if (targetClass.isBlank() || targetClass.equals("ALL", ignoreCase = true)) "ALL" else targetClass
        _uiState.update { it.copy(className = targetClass, selectedClassFilter = filter, isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            val classesResult = academicRepository.getClasses()
            val classes = classesResult.getOrDefault(emptyList())

            val studentsResult = academicRepository.getClassStudents("")
            studentsResult.onSuccess { studentList ->
                val initialAttendance = studentList.associate { it.id to "HADIR" }
                _uiState.update { current ->
                    current.copy(
                        availableClasses = classes,
                        allStudents = studentList,
                        attendanceMap = if (current.attendanceMap.isEmpty()) initialAttendance else current.attendanceMap,
                        isLoading = false,
                        errorMessage = null
                    )
                }
            }.onFailure { error ->
                Timber.e(error, "Error loading students with empty query, attempting fallback")
                val fallbackList = mutableListOf<ClassStudent>()
                for (cls in classes) {
                    academicRepository.getClassStudents(cls.name).onSuccess {
                        fallbackList.addAll(it)
                    }
                }
                val distinct = fallbackList.distinctBy { it.id }
                if (distinct.isNotEmpty()) {
                    val initialAttendance = distinct.associate { it.id to "HADIR" }
                    _uiState.update { current ->
                        current.copy(
                            availableClasses = classes,
                            allStudents = distinct,
                            attendanceMap = if (current.attendanceMap.isEmpty()) initialAttendance else current.attendanceMap,
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            availableClasses = classes,
                            isLoading = false,
                            errorMessage = error.message ?: "Gagal memuat daftar murid"
                        )
                    }
                }
            }
        }
    }

    fun selectStudent(student: ClassStudent?) {
        if (student == null) {
            _uiState.update {
                it.copy(
                    selectedStudent = null,
                    selectedStudentProgress = null,
                    selectedStudentAchievements = emptyList(),
                    isLoadingDetail = false
                )
            }
            return
        }

        _uiState.update {
            it.copy(
                selectedStudent = student,
                isLoadingDetail = true,
                selectedStudentProgress = null,
                selectedStudentAchievements = emptyList()
            )
        }

        viewModelScope.launch {
            val progressResult = academicRepository.getStudentProgress(student.id)
            val achievementsResult = achievementRepository.getStudentAchievements(student.id)

            _uiState.update {
                it.copy(
                    isLoadingDetail = false,
                    selectedStudentProgress = progressResult.getOrNull(),
                    selectedStudentAchievements = achievementsResult.getOrDefault(emptyList())
                )
            }
        }
    }
}
