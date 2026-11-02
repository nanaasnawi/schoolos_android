package com.schoolos.android.feature.sessions

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.schoolos.android.core.auth.AuthManager
import com.schoolos.android.core.common.toHumanReadableError
import com.schoolos.android.domain.model.LearningSession
import com.schoolos.android.domain.model.SessionAttendance
import com.schoolos.android.domain.repository.AcademicRepository
import com.schoolos.android.domain.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StudentAttendanceUiItem(
    val studentId: String,
    val studentName: String,
    val nisn: String? = null,
    val status: String = "present",
    val checkedInAt: String? = null,
    val notes: String? = null,
)

data class SessionDetailUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val userRole: String = "student",
    val session: LearningSession? = null,
    val attendance: List<SessionAttendance> = emptyList(),
    val myAttendance: SessionAttendance? = null,
    val studentItems: List<StudentAttendanceUiItem> = emptyList(),
    val updatingStudentId: String? = null,
    val isBulkSaving: Boolean = false,
    val snackbarMessage: String? = null,
    val todayMaterials: List<com.schoolos.android.domain.model.LearningMaterial> = emptyList(),
    val todayAssignments: List<com.schoolos.android.domain.model.Assignment> = emptyList(),
)

@HiltViewModel
class SessionDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: SessionRepository,
    private val academicRepository: AcademicRepository,
    private val authManager: AuthManager,
    private val materialRepository: com.schoolos.android.domain.repository.LearningMaterialRepository,
    private val assignmentRepository: com.schoolos.android.domain.repository.AssignmentRepository,
) : ViewModel() {

    private val sessionId: String = savedStateHandle["id"] ?: ""

    private val _state = MutableStateFlow(SessionDetailUiState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            authManager.authState.collect { auth ->
                _state.value = _state.value.copy(userRole = auth.role ?: "student")
            }
        }
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            repository.getSession(sessionId)
                .onSuccess { session ->
                    _state.value = _state.value.copy(isLoading = false, session = session)
                    loadAttendanceAndStudents(session)
                    loadSessionResources(session)
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = e.toHumanReadableError("Sesi pelajaran tidak ditemukan atau telah berakhir.")
                    )
                }
        }
    }

    private suspend fun loadAttendanceAndStudents(session: LearningSession) {
        val attendanceList = repository.getAttendance(sessionId).getOrDefault(emptyList())
        val attendanceMap = attendanceList.associateBy { it.studentId }

        val targetClass = session.classId.takeIf { it.isNotBlank() }
            ?: session.className?.takeIf { it.isNotBlank() }
            ?: ""
        val classStudents = if (targetClass.isNotBlank()) {
            academicRepository.getClassStudents(targetClass).getOrDefault(emptyList())
        } else {
            emptyList()
        }

        val items = if (classStudents.isNotEmpty()) {
            classStudents.map { student ->
                val att = attendanceMap[student.id]
                StudentAttendanceUiItem(
                    studentId = student.id,
                    studentName = student.fullName,
                    nisn = student.nisn,
                    // Default "absent" — guru harus secara aktif menandai hadir,
                    // bukan sebaliknya. Mencegah siswa alpa tercatat hadir.
                    status = att?.status ?: "absent",
                    checkedInAt = att?.checkedInAt,
                    notes = att?.notes,
                )
            }
        } else {
            attendanceList.mapIndexed { idx, att ->
                StudentAttendanceUiItem(
                    studentId = att.studentId,
                    studentName = "Siswa ${idx + 1}",
                    nisn = null,
                    status = att.status,
                    checkedInAt = att.checkedInAt,
                    notes = att.notes,
                )
            }
        }

        val auth = authManager.authState.first()
        val myAtt = attendanceList.find { att ->
            (!auth.identifier.isNullOrBlank() && classStudents.any { it.nisn.equals(auth.identifier, ignoreCase = true) && it.id == att.studentId }) ||
            (!auth.userId.isNullOrBlank() && (att.studentId == auth.userId || classStudents.any { it.id == auth.userId && it.id == att.studentId })) ||
            (!auth.name.isNullOrBlank() && classStudents.any { it.fullName.equals(auth.name, ignoreCase = true) && it.id == att.studentId })
        }

        _state.value = _state.value.copy(
            attendance = attendanceList,
            studentItems = items,
            myAttendance = myAtt,
        )
    }

    fun updateAttendance(studentId: String, newStatus: String) {
        viewModelScope.launch {
            val currentList = _state.value.studentItems
            val updated = currentList.map { item ->
                if (item.studentId == studentId) item.copy(status = newStatus) else item
            }
            // Optimistic update + tandai siswa sedang diproses
            _state.value = _state.value.copy(
                studentItems = updated,
                updatingStudentId = studentId,
            )

            repository.recordAttendance(
                sessionId = sessionId,
                studentId = studentId,
                status = newStatus,
            ).onSuccess {
                _state.value = _state.value.copy(
                    updatingStudentId = null,
                    snackbarMessage = "Presensi disimpan",
                )
            }.onFailure { e ->
                // Revert optimistic update
                _state.value = _state.value.copy(
                    studentItems = currentList,
                    updatingStudentId = null,
                    snackbarMessage = e.toHumanReadableError("Gagal menyimpan data presensi."),
                )
            }
        }
    }

    fun markAllPresent() {
        val currentList = _state.value.studentItems
        if (currentList.isEmpty()) return
        val updated = currentList.map { it.copy(status = "present") }
        _state.value = _state.value.copy(studentItems = updated, isBulkSaving = true)
        viewModelScope.launch {
            val bulkItems = updated.map { item ->
                com.schoolos.android.domain.repository.RecordAttendanceItem(
                    studentId = item.studentId,
                    status = "present",
                    notes = item.notes,
                )
            }
            repository.recordAttendanceBulk(sessionId, bulkItems)
                .onSuccess {
                    _state.value = _state.value.copy(
                        isBulkSaving = false,
                        snackbarMessage = "Semua siswa (${bulkItems.size}) ditandai Hadir",
                    )
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(
                        studentItems = currentList,
                        isBulkSaving = false,
                        snackbarMessage = e.toHumanReadableError("Gagal menyimpan presensi semua siswa."),
                    )
                }
        }
    }

    fun saveAllAttendance() {
        val currentList = _state.value.studentItems
        if (currentList.isEmpty()) return
        _state.value = _state.value.copy(isBulkSaving = true)
        viewModelScope.launch {
            val bulkItems = currentList.map { item ->
                com.schoolos.android.domain.repository.RecordAttendanceItem(
                    studentId = item.studentId,
                    status = item.status,
                    notes = item.notes,
                )
            }
            repository.recordAttendanceBulk(sessionId, bulkItems)
                .onSuccess {
                    _state.value = _state.value.copy(
                        isBulkSaving = false,
                        snackbarMessage = "Berhasil menyimpan presensi ${bulkItems.size} siswa",
                    )
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(
                        isBulkSaving = false,
                        snackbarMessage = e.toHumanReadableError("Gagal menyimpan presensi siswa."),
                    )
                }
        }
    }

    fun submitExcuse(status: String, notes: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            repository.submitExcuse(sessionId, status, notes)
                .onSuccess {
                    val label = if (status.equals("sick", ignoreCase = true)) "Sakit" else "Izin"
                    _state.value = _state.value.copy(
                        isLoading = false,
                        snackbarMessage = "Keterangan $label berhasil diajukan",
                    )
                    _state.value.session?.let { loadAttendanceAndStudents(it) }
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        snackbarMessage = e.toHumanReadableError("Gagal mengajukan keterangan izin/sakit."),
                    )
                }
        }
    }

    fun clearSnackbar() {
        _state.value = _state.value.copy(snackbarMessage = null)
    }

    private suspend fun loadSessionResources(session: LearningSession) {
        val targetDate = extractSessionLocalDate(session) ?: java.time.LocalDate.now(java.time.ZoneId.of("Asia/Jakarta"))
        val targetSubject = session.subjectName?.trim().orEmpty()
        val targetClass = session.className?.trim().orEmpty()

        val materials = materialRepository.getMaterials().getOrDefault(emptyList())
        val scopedMaterials = materials.filter { m ->
            (targetSubject.isBlank() || com.schoolos.android.core.designsystem.matchesSubjectFilter(m.subject, targetSubject)) &&
            (targetClass.isBlank() || com.schoolos.android.core.designsystem.matchesClassFilter(m.className, targetClass)) &&
            isMaterialMatchingDate(m, targetDate)
        }

        val assignments = assignmentRepository.getAssignments(session.classId).getOrDefault(emptyList())
        val scopedAssignments = assignments.filter { a ->
            (targetSubject.isBlank() || com.schoolos.android.core.designsystem.matchesSubjectFilter(a.subjectName, targetSubject)) &&
            (targetClass.isBlank() || com.schoolos.android.core.designsystem.matchesClassFilter(a.className, targetClass)) &&
            isAssignmentMatchingDate(a, targetDate)
        }

        _state.value = _state.value.copy(
            todayMaterials = scopedMaterials,
            todayAssignments = scopedAssignments,
        )
    }

    private fun isMaterialMatchingDate(m: com.schoolos.android.domain.model.LearningMaterial, targetDate: java.time.LocalDate): Boolean {
        val iso = m.releaseAt?.takeIf { it.isNotBlank() } ?: m.createdAt?.takeIf { it.isNotBlank() } ?: return false
        val d = parseDate(iso)
        return d != null && d == targetDate
    }

    private fun isAssignmentMatchingDate(a: com.schoolos.android.domain.model.Assignment, targetDate: java.time.LocalDate): Boolean {
        val iso = a.releaseAt?.takeIf { it.isNotBlank() } ?: a.createdAt.takeIf { it.isNotBlank() } ?: return false
        val d = parseDate(iso)
        return d != null && d == targetDate
    }

    private fun extractSessionLocalDate(s: LearningSession): java.time.LocalDate? {
        val fromSessionDate = s.sessionDate?.let { dateStr ->
            val clean = dateStr.trim().substringBefore(" ").substringBefore("T")
            try { java.time.LocalDate.parse(clean) } catch (_: Exception) { null }
        }
        return fromSessionDate ?: s.scheduledAt?.let { parseDate(it) } ?: s.startedAt?.let { parseDate(it) }
    }

    private fun parseDate(iso: String): java.time.LocalDate? {
        val clean = iso.trim()
        val zone = java.time.ZoneId.of("Asia/Jakarta")
        return try {
            java.time.Instant.parse(clean).atZone(zone).toLocalDate()
        } catch (_: Exception) {
            try {
                java.time.ZonedDateTime.parse(clean).withZoneSameInstant(zone).toLocalDate()
            } catch (_: Exception) {
                try {
                    val datePart = clean.substringBefore(" ").substringBefore("T")
                    java.time.LocalDate.parse(datePart)
                } catch (_: Exception) { null }
            }
        }
    }

}

