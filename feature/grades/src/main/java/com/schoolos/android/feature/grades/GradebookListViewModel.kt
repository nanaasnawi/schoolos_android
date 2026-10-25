package com.schoolos.android.feature.grades

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.schoolos.android.domain.model.AcademicClass
import com.schoolos.android.domain.model.AcademicSubject
import com.schoolos.android.domain.model.ClassStudent
import com.schoolos.android.domain.model.GradebookSummaryItem
import com.schoolos.android.domain.model.StudentGradeInputItem
import com.schoolos.android.domain.model.SubjectGradeSummary
import com.schoolos.android.domain.model.computeKurikulumMerdekaGrade
import com.schoolos.android.domain.model.toSubjectSummary
import com.schoolos.android.domain.repository.AcademicRepository
import com.schoolos.android.domain.repository.GradeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.Locale
import javax.inject.Inject

data class StudentGradebookRow(
    val studentId: String,
    val nisn: String,
    val name: String,
    val className: String,
    val sumatifM1: Double = 0.0,
    val sumatifM2: Double = 0.0,
    val sumatifM3: Double = 0.0,
    val sumatifM4: Double = 0.0,
    val avgSumatif: Double = 0.0,
    val ats: Double = 0.0,
    val sas: Double = 0.0,
    val finalScore: Double = 0.0,
    val letterGrade: String = "-",
    val statusKktp: String = "Belum Diinput", // "Tuntas KKTP" | "Perlu Remedial" | "Belum Diinput"
    val highestCompetencyDesc: String = "",
    val lowestCompetencyDesc: String = "",
    val formatifNotes: String = "",
)

data class GradebookUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val userMessage: String? = null,
    val userRole: String = "student",
    val className: String = "",
    val teacherName: String = "",
    val teacherSubject: String? = null,
    val subjects: List<SubjectGradeSummary> = emptyList(),
    // ── KURIKULUM MERDEKA TEACHER/PRINCIPAL GRADEBOOK SSOT ───────────
    val allActiveStudents: List<ClassStudent> = emptyList(), // 304 active students
    val availableClasses: List<AcademicClass> = emptyList(), // 13 classes
    val availableSubjects: List<AcademicSubject> = emptyList(), // 18 subjects
    val selectedSubjectId: String = "",
    val selectedSubjectName: String = "",
    val selectedClassFilter: String = "ALL", // "ALL" or specific rombel
    val activeScheme: String = "A", // "A" (60/20/20) or "B" (100% Modul)
    val searchQuery: String = "",
    val savedSummaries: Map<String, GradebookSummaryItem> = emptyMap(), // studentId -> GradebookSummaryItem
    val activeTab: Int = 0, // 0 = Buku Nilai Siswa (Gradebook Table), 1 = Rekap 18 Modul
) {
    val filteredGradebookRows: List<StudentGradebookRow>
        get() {
            val base = allActiveStudents.map { s ->
                val saved = savedSummaries[s.id]
                val m1 = saved?.sumatifM1 ?: 0.0
                val m2 = saved?.sumatifM2 ?: 0.0
                val m3 = saved?.sumatifM3 ?: 0.0
                val a = saved?.ats ?: 0.0
                val sa = saved?.sas ?: 0.0
                val eval = if (activeScheme == "B") {
                    computeKurikulumMerdekaGrade(m1, m2, m3, atsVal = 0.0, sasVal = 0.0, scheme = "B")
                } else {
                    computeKurikulumMerdekaGrade(m1, m2, m3, atsVal = a, sasVal = sa, scheme = "A")
                }

                val savedFs = saved?.finalScore
                val savedAvg = saved?.avgSumatif
                val fs: Double = if (activeScheme == "B") eval.finalScore else (if (savedFs != null && savedFs > 0.0) savedFs else eval.finalScore)
                val lg: String = if (activeScheme == "B") eval.letterGrade else (if (!saved?.letterGrade.isNullOrBlank() && saved?.letterGrade != "-") saved!!.letterGrade!! else eval.letterGrade)
                val status: String = if (activeScheme == "B") eval.statusKktp else (if (saved?.passed == true) "Tuntas KKTP"
                else if (savedFs != null && savedFs > 0.0) "Perlu Remedial"
                else eval.statusKktp)
                val avgM: Double = if (savedAvg != null && savedAvg > 0.0) savedAvg else eval.avgSumatif

                StudentGradebookRow(
                    studentId = s.id,
                    nisn = s.nisn,
                    name = s.fullName,
                    className = s.className,
                    sumatifM1 = m1,
                    sumatifM2 = m2,
                    sumatifM3 = m3,
                    avgSumatif = avgM,
                    ats = if (activeScheme == "B") 0.0 else a,
                    sas = if (activeScheme == "B") 0.0 else sa,
                    finalScore = fs,
                    letterGrade = lg,
                    statusKktp = status,
                    highestCompetencyDesc = saved?.highestCompetencyDesc.orEmpty(),
                    lowestCompetencyDesc = saved?.lowestCompetencyDesc.orEmpty(),
                    formatifNotes = saved?.formatifNotes.orEmpty(),
                )
            }
            val byClass = if (selectedClassFilter.isBlank() || selectedClassFilter.equals("ALL", ignoreCase = true) || selectedClassFilter.equals("Semua", ignoreCase = true)) {
                base
            } else {
                base.filter { it.className.equals(selectedClassFilter, ignoreCase = true) }
            }
            return if (searchQuery.isBlank()) byClass
            else byClass.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                        it.nisn.contains(searchQuery, ignoreCase = true) ||
                        it.className.contains(searchQuery, ignoreCase = true)
            }
        }

    val totalStudentsCount: Int get() = filteredGradebookRows.size
    val gradedRows: List<StudentGradebookRow> get() = filteredGradebookRows.filter { it.finalScore > 0 }
    val averageScore: Double get() = if (gradedRows.isNotEmpty()) gradedRows.map { it.finalScore }.average() else 0.0
    val passedCount: Int get() = gradedRows.count { it.finalScore >= 75.0 }
    val remedialCount: Int get() = gradedRows.count { it.finalScore < 75.0 }
    val passingPercentage: Int get() = if (gradedRows.isNotEmpty()) ((passedCount.toDouble() / gradedRows.size) * 100).toInt() else 0
}

@HiltViewModel
class GradebookListViewModel @Inject constructor(
    private val repository: GradeRepository,
    private val academicRepository: AcademicRepository,
    private val authManager: com.schoolos.android.core.auth.AuthManager,
    private val authRepository: com.schoolos.android.domain.repository.AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(GradebookUiState())
    val state = _state.asStateFlow()

    private var currentClassId: String = ""
    private var loadJob: Job? = null
    private var hasLoadedOnce: Boolean = false

    private var cachedSubjects: List<AcademicSubject>? = null
    private var cachedClasses: List<AcademicClass>? = null
    private var cachedStudents: List<ClassStudent>? = null

    init {
        viewModelScope.launch {
            val initialAuth = authManager.authState.first()
            currentClassId = initialAuth.classId ?: ""
            val homeroom = initialAuth.className?.takeIf { it.isNotBlank() } ?: ""
            val name = initialAuth.name?.takeIf { it.isNotBlank() } ?: ""
            var teacherSubj = initialAuth.subject?.takeIf { it.isNotBlank() }
            if (teacherSubj == null && initialAuth.isTeacher) {
                teacherSubj = authRepository.getTeacherOfficialBiodata().getOrNull()?.subject?.takeIf { it.isNotBlank() }
            }
            _state.value = _state.value.copy(
                userRole = initialAuth.role ?: "student",
                className = homeroom,
                teacherName = name,
                teacherSubject = teacherSubj,
            )
            load(homeroom, currentClassId, isInitial = true)
        }

        viewModelScope.launch {
            authManager.authState
                .distinctUntilChangedBy { Triple(it.role, it.className, it.classId) }
                .collect { auth ->
                    val newClassId = auth.classId ?: ""
                    val newHomeroom = auth.className?.takeIf { it.isNotBlank() } ?: ""
                    val newRole = auth.role ?: "student"
                    val name = auth.name?.takeIf { it.isNotBlank() } ?: ""
                    var teacherSubj = auth.subject?.takeIf { it.isNotBlank() }
                    if (teacherSubj == null && auth.isTeacher) {
                        teacherSubj = authRepository.getTeacherOfficialBiodata().getOrNull()?.subject?.takeIf { it.isNotBlank() }
                    }

                    val classOrRoleChanged = hasLoadedOnce && (
                            _state.value.userRole != newRole ||
                                    _state.value.className != newHomeroom ||
                                    currentClassId != newClassId ||
                                    _state.value.teacherSubject != teacherSubj
                            )

                    _state.value = _state.value.copy(
                        userRole = newRole,
                        className = newHomeroom,
                        teacherName = name,
                        teacherSubject = teacherSubj,
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

    fun selectTab(tab: Int) {
        _state.update { it.copy(activeTab = tab) }
    }

    fun selectSubject(subjectId: String, subjectName: String) {
        _state.update { it.copy(selectedSubjectId = subjectId, selectedSubjectName = subjectName) }
        loadGradebookSummaries(subjectId)
    }

    fun selectClassFilter(filter: String) {
        _state.update { it.copy(selectedClassFilter = filter) }
    }

    fun selectScheme(scheme: String) {
        _state.update { it.copy(activeScheme = scheme) }
    }

    fun onSearchQueryChanged(query: String) {
        _state.update { it.copy(searchQuery = query) }
    }

    fun clearUserMessage() {
        _state.update { it.copy(userMessage = null) }
    }

    private fun loadGradebookSummaries(subjectId: String) {
        viewModelScope.launch {
            repository.getGradebookSummary(subjectId = subjectId.ifBlank { null })
                .onSuccess { list ->
                    val map = list.associateBy { it.studentId }
                    _state.update { it.copy(savedSummaries = map) }
                }
                .onFailure {
                    Timber.w("Failed to fetch gradebook summary for subject $subjectId: ${it.message}")
                }
        }
    }

    fun saveStudentGrade(
        studentId: String,
        studentName: String,
        m1: Double,
        m2: Double,
        m3: Double,
        ats: Double,
        sas: Double,
        highestDesc: String,
        lowestDesc: String,
        formatifNotes: String,
        m4: Double = 0.0,
    ) {
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            val currentSubjId = _state.value.selectedSubjectId
            val currentSubjName = _state.value.selectedSubjectName
            val scheme = _state.value.activeScheme

            val effectiveAts = if (scheme == "B") 0.0 else ats
            val effectiveSas = if (scheme == "B") 0.0 else sas
            val eval = computeKurikulumMerdekaGrade(
                m1 = m1,
                m2 = m2,
                m3 = m3,
                atsVal = effectiveAts,
                sasVal = effectiveSas,
                scheme = scheme,
                m4 = m4,
            )

            val input = StudentGradeInputItem(
                studentId = studentId,
                sumatifM1 = m1,
                sumatifM2 = m2,
                sumatifM3 = m3,
                ats = effectiveAts,
                sas = effectiveSas,
                formatifNotes = formatifNotes,
                finalScore = eval.finalScore,
                letterGrade = eval.letterGrade,
                passed = eval.statusKktp == "Tuntas KKTP",
                status = "published"
            )

            val studentObj = _state.value.allActiveStudents.find { it.id == studentId }
            val classId = studentObj?.classId

            val result = repository.saveGradebook(
                classId = classId,
                subjectId = currentSubjId.ifBlank { null },
                subjectName = currentSubjName.ifBlank { null },
                weightSumatif = if (scheme == "B") 1.0 else 0.6,
                weightAts = if (scheme == "B") 0.0 else 0.2,
                weightSas = if (scheme == "B") 0.0 else 0.2,
                kktp = 75.0,
                grades = listOf(input)
            )

            result.onSuccess {
                // Update local memory map immediately for zero lag
                val updatedMap = _state.value.savedSummaries.toMutableMap()
                updatedMap[studentId] = GradebookSummaryItem(
                    studentId = studentId,
                    classId = classId,
                    subjectId = currentSubjId,
                    subjectName = currentSubjName,
                    sumatifM1 = m1,
                    sumatifM2 = m2,
                    sumatifM3 = m3,
                    avgSumatif = eval.avgSumatif,
                    ats = ats,
                    sas = sas,
                    finalScore = eval.finalScore,
                    letterGrade = eval.letterGrade,
                    passed = eval.statusKktp == "Tuntas KKTP",
                    status = "published",
                    highestCompetencyDesc = highestDesc,
                    lowestCompetencyDesc = lowestDesc,
                    formatifNotes = formatifNotes
                )
                _state.update {
                    it.copy(
                        isSaving = false,
                        savedSummaries = updatedMap,
                        userMessage = "Nilai & capaian $studentName berhasil disimpan!"
                    )
                }
                // Reload from backend
                loadGradebookSummaries(currentSubjId)
            }.onFailure { err ->
                _state.update {
                    it.copy(
                        isSaving = false,
                        error = "Gagal menyimpan nilai: ${err.message}"
                    )
                }
            }
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
            _state.value = _state.value.copy(
                isLoading = _state.value.subjects.isEmpty() && _state.value.allActiveStudents.isEmpty(),
                error = null,
            )

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

            // Retrieve raw subjects
            val allRawSubjects = if (!forceRefresh && cachedSubjects != null) {
                cachedSubjects!!
            } else {
                val res = academicRepository.getSubjects()
                val list = res.getOrDefault(emptyList()).distinctBy { it.id }
                if (list.isNotEmpty()) cachedSubjects = list
                list
            }

            // Retrieve raw classes
            val allClasses = if (!forceRefresh && cachedClasses != null) {
                cachedClasses!!
            } else {
                val res = academicRepository.getClasses()
                val list = res.getOrDefault(emptyList()).distinctBy { it.id }
                if (list.isNotEmpty()) cachedClasses = list
                list
            }

            // Retrieve all active students (SSOT: 304 active students)
            val allStudents = if (!forceRefresh && cachedStudents != null && cachedStudents!!.isNotEmpty()) {
                cachedStudents!!
            } else {
                val res = academicRepository.getClassStudents("")
                val list = res.getOrDefault(emptyList())
                if (list.isNotEmpty()) cachedStudents = list
                list
            }

            val auth = authManager.authState.first()
            val isPrincipal = auth.isPrincipal
            val isTeacher = auth.isTeacher
            var teacherSubj = _state.value.teacherSubject ?: auth.subject?.trim()
            if (isTeacher && teacherSubj.isNullOrBlank()) {
                teacherSubj = authRepository.getTeacherOfficialBiodata().getOrNull()?.subject?.trim()
                if (!teacherSubj.isNullOrBlank()) {
                    _state.value = _state.value.copy(teacherSubject = teacherSubj)
                }
            }

            // Scope subjects strictly to teacher assigned subject(s) for guru mapel (Kemendikdasmen PKBM)
            val allSubjects = if (isTeacher && !isPrincipal && !teacherSubj.isNullOrBlank()) {
                val tokens = teacherSubj.split(Regex("[,;/|]")).map { it.trim().lowercase() }.filter { it.isNotEmpty() }
                val matched = allRawSubjects.filter { s ->
                    val sName = s.name.lowercase()
                    tokens.any { t -> sName == t || sName.contains(t) || t.contains(sName) }
                }
                if (matched.isNotEmpty()) matched else allRawSubjects
            } else {
                allRawSubjects
            }

            // Determine initial selected subject for Gradebook view
            val initialSubject = if (_state.value.selectedSubjectName.isNotBlank()) {
                allSubjects.find { it.name.equals(_state.value.selectedSubjectName, ignoreCase = true) }
                    ?: allSubjects.firstOrNull()
            } else {
                // Default to teacher's subject if available, otherwise first subject (matches web)
                if (!teacherSubj.isNullOrBlank()) {
                    allSubjects.firstOrNull { it.name.contains(teacherSubj!!, ignoreCase = true) } ?: allSubjects.firstOrNull()
                } else {
                    allSubjects.firstOrNull()
                }
            }

            val selectedSubjId = initialSubject?.id ?: ""
            val selectedSubjName = initialSubject?.name ?: if (allSubjects.isNotEmpty()) allSubjects.first().name else "Pendidikan Agama Islam dan Budi Pekerti"

            val subjectMap = allSubjects.associate { it.id to it.name }

            // Load saved gradebook summary from backend
            if (selectedSubjId.isNotBlank()) {
                repository.getGradebookSummary(subjectId = selectedSubjId)
                    .onSuccess { summaries ->
                        val map = summaries.associateBy { it.studentId }
                        _state.update { it.copy(savedSummaries = map) }
                    }
            }

            repository.getGradebook(targetClassId.ifBlank { null })
                .onSuccess { entries ->
                    val subjectIds = entries.map { it.subjectId }.distinct()
                    val entrySummaries = subjectIds.mapNotNull { id ->
                        val realName = subjectMap[id] ?: return@mapNotNull null
                        entries.toSubjectSummary(id, realName)
                    }

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
                        allActiveStudents = allStudents,
                        availableClasses = allClasses,
                        availableSubjects = allSubjects,
                        selectedSubjectId = selectedSubjId,
                        selectedSubjectName = selectedSubjName,
                        error = null,
                    )
                }
                .onFailure { e ->
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
                        allActiveStudents = allStudents,
                        availableClasses = allClasses,
                        availableSubjects = allSubjects,
                        selectedSubjectId = selectedSubjId,
                        selectedSubjectName = selectedSubjName,
                        error = if (_state.value.subjects.isEmpty() && emptySummaries.isEmpty()) e.message ?: "Gagal memuat buku nilai" else null,
                    )
                }
        }
    }
}
