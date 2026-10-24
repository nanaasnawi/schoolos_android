package com.schoolos.android.feature.home

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.schoolos.android.core.auth.AuthManager
import com.schoolos.android.core.auth.isPrincipalRole
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
    val isPrincipal: Boolean = false,
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

    val hadirCount: Int get() = filteredStudents.count { (attendanceMap[it.id] ?: "present").lowercase() in listOf("present", "hadir") }
    val sakitCount: Int get() = filteredStudents.count { attendanceMap[it.id]?.lowercase() in listOf("sick", "sakit") }
    val izinCount: Int get() = filteredStudents.count { attendanceMap[it.id]?.lowercase() in listOf("excused", "izin") }
    val alpaCount: Int get() = filteredStudents.count { attendanceMap[it.id]?.lowercase() in listOf("absent", "alpa") }
}

@HiltViewModel
class RombelStudentsViewModel @Inject constructor(
    private val academicRepository: AcademicRepository,
    private val achievementRepository: AchievementRepository,
    private val sessionRepository: SessionRepository,
    private val authManager: AuthManager,
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
        viewModelScope.launch {
            authManager.authState.collect { auth ->
                val isPrin = auth.isPrincipal || isPrincipalRole(auth.role)
                _uiState.update { it.copy(isPrincipal = isPrin) }
            }
        }
        loadStudents(navClassName)
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun selectClassFilter(filter: String) {
        _uiState.update { it.copy(selectedClassFilter = filter) }
    }

    fun setStudentAttendance(studentId: String, status: String) {
        val norm = when (status.lowercase()) {
            "present", "hadir" -> "present"
            "sick", "sakit" -> "sick"
            "excused", "izin" -> "excused"
            else -> "absent"
        }
        _uiState.update {
            val updated = it.attendanceMap.toMutableMap()
            updated[studentId] = norm
            it.copy(attendanceMap = updated)
        }
    }

    fun markAllPresent() {
        val current = _uiState.value.filteredStudents
        _uiState.update {
            val updated = it.attendanceMap.toMutableMap()
            current.forEach { s -> updated[s.id] = "present" }
            it.copy(attendanceMap = updated)
        }
    }

    fun saveAttendance(onSuccess: (String) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSavingAttendance = true) }
            val currentFiltered = _uiState.value.filteredStudents
            val currentAttendance = _uiState.value.attendanceMap
            val classes = _uiState.value.availableClasses
            val selectedFilter = _uiState.value.selectedClassFilter

            val studentsByClass = currentFiltered.groupBy { student ->
                student.classId.ifBlank {
                    classes.firstOrNull { it.name.equals(student.className, ignoreCase = true) }?.id ?: ""
                }
            }

            var totalSaved = 0
            var saveError: String? = null

            for ((classId, classStudents) in studentsByClass) {
                if (classId.isBlank()) continue
                val items = classStudents.map { s ->
                    val st = currentAttendance[s.id] ?: "present"
                    val validStatus = when (st.lowercase()) {
                        "present", "hadir" -> "present"
                        "sick", "sakit" -> "sick"
                        "excused", "izin" -> "excused"
                        else -> "absent"
                    }
                    RecordAttendanceItem(
                        studentId = s.id,
                        status = validStatus,
                        checkedInAt = if (validStatus == "present") Instant.now().toString() else null
                    )
                }
                val res = sessionRepository.recordAttendanceBulk(classId, items)
                if (res.isSuccess) {
                    totalSaved += items.size
                } else {
                    saveError = res.exceptionOrNull()?.message
                    Timber.e(res.exceptionOrNull(), "Error saving attendance for class $classId")
                }
            }

            delay(200)
            _uiState.update { it.copy(isSavingAttendance = false) }

            if (totalSaved > 0) {
                val targetLabel = if (selectedFilter == "ALL") "seluruh rombel diampu" else "rombel $selectedFilter"
                onSuccess("Presensi $totalSaved warga belajar ($targetLabel) berhasil disimpan ke database!")
                val refreshed = fetchAttendanceForClasses(classes)
                if (refreshed.isNotEmpty()) {
                    _uiState.update { it.copy(attendanceMap = refreshed) }
                }
            } else {
                onSuccess("Gagal menyimpan presensi: ${saveError ?: "Kelas tidak ditemukan"}")
            }
        }
    }

    private suspend fun fetchAttendanceForClasses(classes: List<AcademicClass>): Map<String, String> {
        val map = mutableMapOf<String, String>()
        for (cls in classes) {
            sessionRepository.getAttendance(cls.id).onSuccess { records ->
                for (r in records) {
                    val norm = when (r.status.lowercase()) {
                        "present", "hadir", "h" -> "present"
                        "sick", "sakit", "s" -> "sick"
                        "excused", "izin", "i" -> "excused"
                        else -> "absent"
                    }
                    map[r.studentId] = norm
                }
            }
        }
        return map
    }

    fun loadStudents(classNameOverride: String? = null) {
        val targetClass = (classNameOverride ?: _uiState.value.className).trim()
        val filter = if (targetClass.isBlank() || targetClass.equals("ALL", ignoreCase = true)) "ALL" else targetClass
        _uiState.update { it.copy(className = targetClass, selectedClassFilter = filter, isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            val classesResult = academicRepository.getClasses()
            val classes = classesResult.getOrDefault(emptyList())

            val realAttendance = fetchAttendanceForClasses(classes)

            val studentsResult = academicRepository.getClassStudents("")
            studentsResult.onSuccess { studentList ->
                val mergedAttendance = studentList.associate { it.id to (realAttendance[it.id] ?: "present") }
                _uiState.update { current ->
                    current.copy(
                        availableClasses = classes,
                        allStudents = studentList,
                        attendanceMap = mergedAttendance,
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
                    val mergedAttendance = distinct.associate { it.id to (realAttendance[it.id] ?: "present") }
                    _uiState.update { current ->
                        current.copy(
                            availableClasses = classes,
                            allStudents = distinct,
                            attendanceMap = mergedAttendance,
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
