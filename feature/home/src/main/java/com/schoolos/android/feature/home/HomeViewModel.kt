package com.schoolos.android.feature.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.schoolos.android.core.auth.AuthManager
import com.schoolos.android.core.auth.AuthState
import com.schoolos.android.core.notification.TeacherScheduleReminderManager
import com.schoolos.android.core.reading.ReadingHistoryManager
import com.schoolos.android.domain.model.AcademicClass
import com.schoolos.android.domain.model.AcademicSubject
import com.schoolos.android.domain.model.Assignment
import com.schoolos.android.domain.model.BookReadingItem
import com.schoolos.android.domain.model.LearningSession
import com.schoolos.android.domain.model.LibraryBook
import com.schoolos.android.domain.model.MaterialType
import com.schoolos.android.domain.model.Notification
import com.schoolos.android.domain.model.Progress
import com.schoolos.android.domain.model.SubjectGradeSummary
import com.schoolos.android.domain.model.toSubjectSummary
import com.schoolos.android.domain.repository.AcademicRepository
import com.schoolos.android.domain.repository.AchievementRepository
import com.schoolos.android.domain.repository.AssignmentRepository
import com.schoolos.android.domain.repository.AuthRepository
import com.schoolos.android.domain.repository.GradeRepository
import com.schoolos.android.domain.repository.LearningMaterialRepository
import com.schoolos.android.domain.repository.NotificationRepository
import com.schoolos.android.domain.repository.ProgressRepository
import com.schoolos.android.domain.repository.QuizRepository
import com.schoolos.android.domain.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Locale
import javax.inject.Inject

data class UrgentTeachingSession(
    val sessionId: String,
    val subjectName: String,
    val className: String,
    val scheduledTimeStr: String,
    val minutesUntilStart: Long,
    val isLive: Boolean,
    val isOneHourWarning: Boolean,
)

data class HomeUiState(
    val isLoading: Boolean = true,
    val userName: String = "",
    val userRole: String = "student",
    val userEmail: String = "",
    val userAvatarUrl: String? = null,
    val schoolName: String = "",
    val schoolLogoUrl: String? = null,
    val unreadCount: Int = 0,
    val gradeAverage: String = "-",
    val gradeStatus: String = "Belum ada data nilai",
    val gradeTrendPoints: List<Float> = emptyList(),
    val topGradeSubjects: List<SubjectGradeSummary> = emptyList(),
    val studentProgress: Progress? = null,
    val progressPercentage: Float = 0f,
    val assignmentsCount: String = "0",
    val xpCount: String = "0",
    val badgeCount: String = "0",
    val teacherScheduleCount: String = "0",
    val teacherAttendanceRate: String = "-",
    val teacherPendingCount: String = "0",
    val teacherQuizzesCount: String = "0",
    val teacherMaterialsCount: String = "0",
    val teacherAssignments: List<Assignment> = emptyList(),
    val teacherAnnouncements: List<Notification> = emptyList(),
    val activeSessionSubject: String = "-",
    val activeSessionClass: String = "-",
    val homeroomClass: String = "",
    val todaySessions: List<LearningSession> = emptyList(),
    val urgentTeachingSession: UrgentTeachingSession? = null,
    val nextSessionSubject: String = "-",
    val nextSessionRoom: String = "-",
    val nextSessionTime: String = "-",
    val nextSessionIsLive: Boolean = false,
    val childName: String = "",
    val childId: String = "",
    val parentAttendanceRate: String = "-",
    val parentPresentDays: String = "-",
    val parentPermitDays: String = "-",
    val parentAbsentDays: String = "-",
    val parentAssignmentsCount: String = "0",
    val teacherClasses: List<AcademicClass> = emptyList(),
    val teacherSubjects: List<AcademicSubject> = emptyList(),
    val activeReadingHistory: List<BookReadingItem> = emptyList(),
    val recommendedSibiBook: LibraryBook? = null,
    val totalStudentsCount: String = "0",
    val isRefreshing: Boolean = false,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authRepository: AuthRepository,
    private val authManager: AuthManager,
    private val notificationRepository: NotificationRepository,
    private val gradeRepository: GradeRepository,
    private val progressRepository: ProgressRepository,
    private val academicRepository: AcademicRepository,
    private val assignmentRepository: AssignmentRepository,
    private val achievementRepository: AchievementRepository,
    private val sessionRepository: SessionRepository,
    private val quizRepository: QuizRepository,
    private val learningMaterialRepository: LearningMaterialRepository,
    private val readingHistoryManager: ReadingHistoryManager,
    private val firebaseRtdbManager: com.schoolos.android.core.firebase.FirebaseRealtimeDatabaseManager,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state = _state.asStateFlow()

    private var lastSyncTimestamp: Long = 0L
    private var isSyncInProgress: Boolean = false
    private var hasLoadedInitialData: Boolean = false
    private var currentAuth = AuthState()
    private var cachedClassMap: Map<String, String> = emptyMap()
    private var cachedSubjectMap: Map<String, String> = emptyMap()
    private var teacherScheduledClassIds: Set<String> = emptySet()
    private var teacherScheduledClassNames: Set<String> = emptySet()

    init {
        observeAuthState()
        observeNotifications()
        observeRealtimeData()
        startTeacherUrgentTicker()
    }

    private fun observeRealtimeData() {
        viewModelScope.launch {
            firebaseRtdbManager.observeAllInquiries().collect {
                refreshUnreadCount()
            }
        }
    }

    private fun startTeacherUrgentTicker() {
        viewModelScope.launch {
            while (isActive) {
                delay(30_000L)
                if (currentAuth.isTeacher && _state.value.todaySessions.isNotEmpty()) {
                    TeacherScheduleReminderManager.scheduleReminders(context, _state.value.todaySessions)
                    val updatedUrgent = evaluateUrgentSession(
                        _state.value.todaySessions,
                        cachedClassMap,
                        cachedSubjectMap,
                        currentAuth.className ?: ""
                    )
                    if (_state.value.urgentTeachingSession != updatedUrgent) {
                        _state.update { it.copy(urgentTeachingSession = updatedUrgent) }
                    }
                }
            }
        }
    }

    private fun observeNotifications() {
        viewModelScope.launch {
            notificationRepository.getUnreadCountFlow().collect { localCount ->
                _state.update { current ->
                    if (localCount > 0 || current.unreadCount == 0) {
                        current.copy(unreadCount = localCount)
                    } else {
                        current
                    }
                }
            }
        }
        refreshUnreadCount()
    }

    fun refreshUnreadCount() {
        viewModelScope.launch {
            notificationRepository.getUnreadCount().onSuccess { count ->
                _state.update { it.copy(unreadCount = count) }
            }
        }
    }

    private fun observeAuthState() {
        viewModelScope.launch {
            authManager.authState.collect { auth ->
                val authChanged = currentAuth.userId != auth.userId || currentAuth.role != auth.role || currentAuth.tenantId != auth.tenantId
                currentAuth = auth
                val name = if (!auth.name.isNullOrBlank()) auth.name!! else "Pengguna School OS"
                val role = if (!auth.role.isNullOrBlank()) auth.role!! else "student"
                val homeroom = auth.className ?: ""
                val child = auth.childName ?: ""
                val cId = auth.childId ?: ""

                _state.update { current ->
                    val effectiveClass = current.activeSessionClass.takeIf { it.isNotBlank() && it != "-" && !isUuid(it) }
                        ?: homeroom.takeIf { it.isNotBlank() && !isUuid(it) }
                        ?: "-"
                    current.copy(
                        userName = name,
                        userRole = role,
                        userEmail = auth.email ?: "",
                        userAvatarUrl = auth.avatarUrl,
                        schoolName = auth.schoolName ?: "",
                        schoolLogoUrl = auth.schoolLogoUrl,
                        homeroomClass = homeroom,
                        activeSessionClass = effectiveClass,
                        childName = child,
                        childId = cId,
                    )
                }

                if (auth.isLoggedIn) {
                    syncData(silent = hasLoadedInitialData && !isDataEmpty(), force = authChanged)
                }
            }
        }
        viewModelScope.launch {
            if (authManager.isLoggedIn) {
                authRepository.getCurrentUser()
            }
        }
    }

    private fun isDataEmpty(): Boolean {
        val current = _state.value
        return current.todaySessions.isEmpty() &&
            current.teacherAssignments.isEmpty() &&
            current.topGradeSubjects.isEmpty() &&
            current.studentProgress == null &&
            current.activeReadingHistory.isEmpty() &&
            current.recommendedSibiBook == null
    }

    /**
     * Public manual or swipe-to-refresh method called by HomeScreen.
     */
    fun refresh(isPullRefresh: Boolean = false) {
        if (isPullRefresh) {
            _state.update { it.copy(isRefreshing = true) }
        } else if (!hasLoadedInitialData || isDataEmpty()) {
            _state.update { it.copy(isLoading = true) }
        }
        refreshUnreadCount()
        viewModelScope.launch {
            try {
                withTimeoutOrNull(15000L) {
                    syncData(
                        silent = !isPullRefresh && hasLoadedInitialData && !isDataEmpty(),
                        force = isPullRefresh || isDataEmpty()
                    )
                }
            } catch (e: Exception) {
                timber.log.Timber.e(e, "Error refreshing home data")
            } finally {
                _state.update { it.copy(isRefreshing = false, isLoading = false) }
            }
        }
    }

    private fun isUuid(str: String?): Boolean {
        if (str.isNullOrBlank()) return false
        val clean = str.trim()
        return clean.length >= 32 && clean.contains("-")
    }

    private fun parseTime(iso: String): String {
        return try {
            val dt = Instant.parse(iso).atZone(ZoneId.systemDefault())
            val formatter = java.time.format.DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
            "${dt.format(formatter)} WIB"
        } catch (_: Exception) {
            "Hari ini"
        }
    }

    private suspend fun syncData(silent: Boolean, force: Boolean = false) {
        if (isSyncInProgress && !force) return
        val emptyData = isDataEmpty()
        if (!force && hasLoadedInitialData && !emptyData && System.currentTimeMillis() - lastSyncTimestamp < 15_000L) return
        isSyncInProgress = true
        if (!silent || emptyData) {
            _state.update { it.copy(isLoading = true) }
        }
        try {
            lastSyncTimestamp = System.currentTimeMillis()
            val auth = currentAuth
            val isParent = auth.isParent
            val isTeacher = auth.isTeacher
            val isStudent = auth.isStudent
            val homeroom = auth.className ?: ""

            val targetClassId = when {
                isStudent || isParent -> auth.classId?.takeIf { it.isNotBlank() }
                else -> null
            }

            // 1. Real-time Learning Sessions Sync (Filtered to Today's Agenda)
            sessionRepository.getSessions(targetClassId).onSuccess { sessions ->
            val today = LocalDate.now()
            val todaySessions = sessions.filter { s ->
                // Active sessions are always relevant today
                if (s.status.equals("active", ignoreCase = true)) return@filter true

                val scheduled = s.scheduledAt?.let { parseDate(it) }
                val started = s.startedAt?.let { parseDate(it) }
                scheduled == today || started == today
            }.sortedWith(
                compareBy<LearningSession> {
                    if (it.status.equals("active", ignoreCase = true)) 0 else 1
                }.thenBy { it.scheduledAt ?: it.startedAt ?: "" }
            )

            teacherScheduledClassIds = sessions.mapNotNull { it.classId }.filter { it.isNotBlank() }.toSet()
            teacherScheduledClassNames = sessions.mapNotNull { it.className }.filter { it.isNotBlank() && !isUuid(it) }.toSet()

            // Resolve class & subject name maps so UUIDs are NEVER passed to UI
            val classList = academicRepository.getClasses().getOrNull() ?: emptyList()
            val classMap = classList.associate { it.id to it.name }
            val subjectList = academicRepository.getSubjects().getOrNull() ?: emptyList()
            val subjectMap = subjectList.associate { it.id to it.name }
            cachedClassMap = classMap
            cachedSubjectMap = subjectMap

            if (isTeacher && todaySessions.isNotEmpty()) {
                TeacherScheduleReminderManager.scheduleReminders(context, todaySessions)
            }

            val urgentSession = if (isTeacher) {
                evaluateUrgentSession(todaySessions, classMap, subjectMap, homeroom)
            } else null

            // Real-time active session check: must actually have status == "active"
            val liveSession = todaySessions.firstOrNull { it.status.equals("active", ignoreCase = true) }
                ?: sessions.firstOrNull { it.status.equals("active", ignoreCase = true) }
            val isLive = liveSession != null

            val upcomingSession = todaySessions.firstOrNull { it.status.equals("scheduled", ignoreCase = true) }
                ?: todaySessions.firstOrNull()

            val displaySession = liveSession ?: upcomingSession

            val nextSubj = if (displaySession != null) {
                cleanSubjectName(displaySession.lessonId, displaySession.subjectName, displaySession.notes, subjectMap)
            } else {
                "-"
            }

            val nextRm = if (displaySession != null) {
                val cls = cleanClassName(displaySession.classId, displaySession.className, classMap, homeroom)
                if (cls.isNotBlank()) cls else (displaySession.room ?: "Ruang Kelas")
            } else {
                "-"
            }

            val timeText = if (isLive) {
                "Sedang Berlangsung"
            } else if (upcomingSession != null) {
                upcomingSession.scheduledAt?.let { parseTime(it) } ?: "Hari ini"
            } else {
                "-"
            }

            val activeTeacherSession = liveSession ?: todaySessions.firstOrNull { it.status.equals("active", ignoreCase = true) }
            val activeSubj = if (activeTeacherSession != null) {
                cleanSubjectName(activeTeacherSession.lessonId, activeTeacherSession.subjectName, activeTeacherSession.notes, subjectMap)
            } else {
                "-"
            }

            val activeClass = if (activeTeacherSession != null) {
                cleanClassName(activeTeacherSession.classId, activeTeacherSession.className, classMap, homeroom)
            } else if (homeroom.isNotBlank() && !isUuid(homeroom)) {
                homeroom
            } else {
                "-"
            }

            val scheduleCount = if (todaySessions.isNotEmpty()) todaySessions.size else sessions.size
            val attendanceRate = if (todaySessions.isNotEmpty()) {
                val completedOrActive = todaySessions.count {
                    it.status.equals("completed", ignoreCase = true) || it.status.equals("active", ignoreCase = true)
                }
                if (completedOrActive > 0) {
                    val pct = (completedOrActive.toFloat() / todaySessions.size.toFloat()) * 100f
                    "${pct.toInt()}%"
                } else {
                    "100%"
                }
            } else if (sessions.isNotEmpty()) {
                "100%"
            } else {
                "0%"
            }

            _state.update { current ->
                current.copy(
                    todaySessions = todaySessions,
                    urgentTeachingSession = urgentSession,
                    nextSessionSubject = nextSubj,
                    nextSessionRoom = nextRm,
                    nextSessionTime = timeText,
                    nextSessionIsLive = isLive,
                    teacherScheduleCount = scheduleCount.toString(),
                    teacherAttendanceRate = attendanceRate,
                    activeSessionSubject = activeSubj,
                    activeSessionClass = activeClass,
                )
            }
        }

        // 2. Unread Notifications Count
        notificationRepository.getUnreadCount().onSuccess { count ->
            _state.update { it.copy(unreadCount = count) }
        }

        // 3. Role-specific real-time metrics
        if (isStudent) {
            // Live Grades & Academic Performance
            gradeRepository.getGradebook().onSuccess { entries ->
                val validScores = entries.mapNotNull { it.rawScore ?: it.weightedScore }
                val trendPoints = validScores.map { it.toFloat() }
                val avg = if (validScores.isNotEmpty()) validScores.average() else 0.0
                val avgStr = if (validScores.isNotEmpty()) String.format(Locale.US, "%.1f", avg) else "-"
                val statusStr = when {
                    avg >= 85.0 -> "Melampaui KKM • Predikat A"
                    avg >= 75.0 -> "Tuntas KKM • Predikat B"
                    avg > 0.0 -> "Perlu Remedial • Predikat C"
                    else -> "Belum ada data nilai"
                }

                val subjectMap = academicRepository.getSubjects().getOrDefault(emptyList()).associate { it.id to it.name }
                val subjectIds = entries.map { it.subjectId }.distinct()
                val summaries = subjectIds.mapNotNull { id ->
                    val realName = subjectMap[id] ?: return@mapNotNull null
                    entries.toSubjectSummary(id, realName)
                }.sortedByDescending { summary -> summary.finalScore }

                _state.update { current ->
                    current.copy(
                        gradeAverage = avgStr,
                        gradeStatus = statusStr,
                        gradeTrendPoints = trendPoints,
                        topGradeSubjects = summaries,
                    )
                }
            }

            // Live Learning Progress
            progressRepository.getProgress("").onSuccess { progress ->
                val pct = (progress.overallProgress / 100.0).toFloat().coerceIn(0f, 1f)
                _state.update { current ->
                    current.copy(
                        studentProgress = progress,
                        progressPercentage = pct,
                    )
                }
            }

            assignmentRepository.getAssignments(classId = "").onSuccess { assignments ->
                _state.update { it.copy(assignmentsCount = assignments.size.toString()) }
            }

            achievementRepository.getAchievements().onSuccess { achievements ->
                val earned = achievements.filter { !it.earnedAt.isNullOrBlank() }
                val badges = earned.size
                val xp = badges * 50
                _state.update { current ->
                    current.copy(
                        badgeCount = badges.toString(),
                        xpCount = xp.toString(),
                    )
                }
            }

            // 4. Reading History & SIBI Book Recommendations (Kemendikdasmen)
            val studentId = auth.userId ?: ""
            val localHistory = readingHistoryManager.getReadingHistory(studentId).filter { !it.isCompleted }

            // Check assigned school learning materials for in-progress assigned books
            val assignedMaterialsResult = learningMaterialRepository.getMaterials()
            val assignedBookItems = assignedMaterialsResult.getOrNull()?.filter { mat ->
                (mat.startPage != null || mat.materialType == MaterialType.DOCUMENT) && !mat.isCompleted
            }?.map { mat ->
                BookReadingItem(
                    id = mat.id,
                    title = mat.title,
                    author = mat.teacherName?.takeIf { it.isNotBlank() && !it.equals("Guru Pengampu", ignoreCase = true) } ?: "Guru Mata Pelajaran",
                    publisher = "Materi Sekolah",
                    subjectName = mat.subject,
                    gradeLevelName = mat.className,
                    coverUrl = mat.thumbnailUrl,
                    fileUrl = mat.mediaUrl,
                    currentPage = mat.startPage ?: 1,
                    totalPages = mat.endPage ?: 50,
                    startPage = mat.startPage,
                    endPage = mat.endPage,
                    isCompleted = mat.isCompleted,
                    materialId = mat.id,
                )
            } ?: emptyList()

            // Merge local reading history with active assigned reading materials
            val combinedHistory = (localHistory + assignedBookItems.filter { assigned ->
                localHistory.none { it.id == assigned.id || (assigned.fileUrl != null && it.fileUrl == assigned.fileUrl) }
            }).sortedByDescending { it.lastReadAt ?: "" }

            if (combinedHistory.isNotEmpty()) {
                _state.update { it.copy(activeReadingHistory = combinedHistory, recommendedSibiBook = null) }
            } else {
                // If student has NO reading history, fetch integrated SIBI Kemendikdasmen books scoped to student's rombel
                val studentClassName = (currentAuth.className ?: auth.className ?: "").lowercase()
                val studentClassNum = Regex("\\d+").find(studentClassName)?.value?.toIntOrNull()

                learningMaterialRepository.getLibraryBooks(recommendations = true).onSuccess { books ->
                    // 1. Strictly match student's enrolled rombel/grade level
                    val gradeMatchedBooks = books.filter { book ->
                        val t = book.title.lowercase()
                        val isNotTeacherGuide = !t.contains("panduan guru") && !t.contains("buku guru")
                        if (!isNotTeacherGuide) return@filter false

                        if (studentClassNum != null) {
                            book.classLevel == studentClassNum ||
                            book.gradeLevelName?.contains(studentClassNum.toString()) == true ||
                            t.contains("kelas $studentClassNum") ||
                            (studentClassNum == 5 && (t.contains("kelas v") || t.contains("kelas 5"))) ||
                            (studentClassNum == 4 && (t.contains("kelas iv") || t.contains("kelas 4"))) ||
                            (studentClassNum == 6 && (t.contains("kelas vi") || t.contains("kelas 6"))) ||
                            (studentClassNum == 7 && (t.contains("kelas vii") || t.contains("kelas 7"))) ||
                            (studentClassNum == 8 && (t.contains("kelas viii") || t.contains("kelas 8"))) ||
                            (studentClassNum == 9 && (t.contains("kelas ix") || t.contains("kelas 9"))) ||
                            (studentClassNum == 10 && (t.contains("kelas x") || t.contains("kelas 10"))) ||
                            (studentClassNum == 11 && (t.contains("kelas xi") || t.contains("kelas 11"))) ||
                            (studentClassNum == 12 && (t.contains("kelas xii") || t.contains("kelas 12")))
                        } else true
                    }

                    val candidateBooks = if (gradeMatchedBooks.isNotEmpty()) gradeMatchedBooks else books.filter {
                        val t = it.title.lowercase()
                        !t.contains("panduan guru") && !t.contains("buku guru")
                    }

                    val recommended = candidateBooks.firstOrNull { book ->
                        val t = book.title.lowercase()
                        t.contains("koding") || t.contains("informatika") || t.contains("bahasa") || t.contains("matematika") || t.contains("ilmu pengetahuan") || t.contains("pancasila")
                    } ?: candidateBooks.firstOrNull() ?: books.firstOrNull()

                    _state.update { it.copy(activeReadingHistory = emptyList(), recommendedSibiBook = recommended) }
                }
            }
        } else if (isTeacher) {
            // Fetch total registered students across school from Railway PostgreSQL
            academicRepository.getClassStudents("ALL").onSuccess { students ->
                _state.update { it.copy(totalStudentsCount = students.size.toString()) }
            }
            assignmentRepository.getAssignments(classId = "").onSuccess { assignments ->
                _state.update { it.copy(
                    teacherPendingCount = assignments.size.toString(),
                    teacherAssignments = assignments.take(4),
                ) }
            }
            notificationRepository.getNotifications(page = 1).onSuccess { notifs ->
                _state.update { it.copy(
                    teacherAnnouncements = notifs.take(3),
                ) }
            }
            learningMaterialRepository.getMaterials().onSuccess { materials ->
                _state.update { it.copy(teacherMaterialsCount = materials.size.toString()) }
            }
            quizRepository.getQuizzes(classId = "").onSuccess { quizzes ->
                _state.update { it.copy(teacherQuizzesCount = quizzes.size.toString()) }
            }
            academicRepository.getClasses().onSuccess { classes ->
                val validClasses = classes.filter { !isUuid(it.name) }
                val teacherUserId = currentAuth.userId ?: ""

                // Find explicit homeroom class matching the teacher's userId or auth homeroom name
                val matchedHomeroomClass = validClasses.find { c ->
                    (teacherUserId.isNotBlank() && c.homeroomTeacherId == teacherUserId) ||
                    (homeroom.isNotBlank() && c.name.equals(homeroom, ignoreCase = true))
                }

                val resolvedHomeroom = matchedHomeroomClass?.name ?: homeroom

                _state.update { current ->
                    val activeCls = resolvedHomeroom.takeIf { it.isNotBlank() && !isUuid(it) }
                        ?: current.activeSessionClass.takeIf { it.isNotBlank() && it != "-" && !isUuid(it) }
                        ?: validClasses.firstOrNull()?.name
                        ?: "-"
                    current.copy(
                        teacherClasses = validClasses,
                        homeroomClass = resolvedHomeroom,
                        activeSessionClass = activeCls
                    )
                }
            }
            academicRepository.getSubjects().onSuccess { subjects ->
                _state.update { current ->
                    val activeSubj = if (current.activeSessionSubject.isBlank() || current.activeSessionSubject == "-" || isUuid(current.activeSessionSubject)) {
                        subjects.firstOrNull()?.name ?: current.activeSessionSubject
                    } else current.activeSessionSubject
                    current.copy(teacherSubjects = subjects, activeSessionSubject = activeSubj)
                }
            }
        } else if (isParent) {
            assignmentRepository.getAssignments(classId = "").onSuccess { assignments ->
                _state.update { current ->
                    current.copy(
                        parentAssignmentsCount = assignments.size.toString(),
                        assignmentsCount = assignments.size.toString()
                    )
                }
            }
        }
    } finally {
        hasLoadedInitialData = true
        _state.update { it.copy(isLoading = false) }
        isSyncInProgress = false
    }
}

    private fun cleanClassName(cid: String?, cname: String?, classMap: Map<String, String>, homeroom: String): String {
        if (!cname.isNullOrBlank() && !isUuid(cname)) return cname
        val mapped = cid?.let { classMap[it] }
        if (!mapped.isNullOrBlank() && !isUuid(mapped)) return mapped
        if (homeroom.isNotBlank() && !isUuid(homeroom)) return homeroom
        return ""
    }

    private fun cleanSubjectName(lid: String?, sname: String?, notes: String?, subjectMap: Map<String, String>): String {
        if (!sname.isNullOrBlank() && !isUuid(sname)) return sname
        val mapped = lid?.let { subjectMap[it] }
        if (!mapped.isNullOrBlank()) return mapped
        val notePart = notes?.substringBefore(" • ")?.trim()
        if (!notePart.isNullOrBlank() && !isUuid(notePart)) return notePart
        return "Pelajaran"
    }

    private fun evaluateUrgentSession(
        sessions: List<LearningSession>,
        classMap: Map<String, String>,
        subjectMap: Map<String, String>,
        homeroom: String
    ): UrgentTeachingSession? {
        if (sessions.isEmpty()) return null
        val now = System.currentTimeMillis()

        // 1. Live or current session (status == "active" or now is within startMs until startMs + 2 hours)
        val liveOrCurrent = sessions.firstOrNull { s ->
            if (s.status.equals("active", ignoreCase = true)) return@firstOrNull true
            val startMs = TeacherScheduleReminderManager.parseEpochMs(s.scheduledAt ?: s.startedAt)
            if (startMs != null && now >= startMs && now < (startMs + 2 * 3600_000L) && !s.status.equals("completed", ignoreCase = true)) {
                return@firstOrNull true
            }
            false
        }

        if (liveOrCurrent != null) {
            val startMs = TeacherScheduleReminderManager.parseEpochMs(liveOrCurrent.scheduledAt ?: liveOrCurrent.startedAt) ?: now
            val subj = cleanSubjectName(liveOrCurrent.lessonId, liveOrCurrent.subjectName, liveOrCurrent.notes, subjectMap)
            val cls = cleanClassName(liveOrCurrent.classId, liveOrCurrent.className, classMap, homeroom).ifBlank { liveOrCurrent.room ?: "Kelas" }
            val timeStr = TeacherScheduleReminderManager.formatHourMinute(startMs)
            return UrgentTeachingSession(
                sessionId = liveOrCurrent.id,
                subjectName = subj,
                className = cls,
                scheduledTimeStr = timeStr,
                minutesUntilStart = 0L,
                isLive = true,
                isOneHourWarning = false,
            )
        }

        // 2. Upcoming session starting within 60 minutes
        val upcoming1h = sessions.filter { s ->
            !s.status.equals("completed", ignoreCase = true)
        }.mapNotNull { s ->
            val startMs = TeacherScheduleReminderManager.parseEpochMs(s.scheduledAt ?: s.startedAt) ?: return@mapNotNull null
            val diffMs = startMs - now
            val mins = diffMs / (60 * 1000L)
            if (mins in 0..60) Pair(s, mins) else null
        }.minByOrNull { it.second }

        if (upcoming1h != null) {
            val session = upcoming1h.first
            val mins = upcoming1h.second
            val startMs = TeacherScheduleReminderManager.parseEpochMs(session.scheduledAt ?: session.startedAt) ?: now
            val subj = cleanSubjectName(session.lessonId, session.subjectName, session.notes, subjectMap)
            val cls = cleanClassName(session.classId, session.className, classMap, homeroom).ifBlank { session.room ?: "Kelas" }
            val timeStr = TeacherScheduleReminderManager.formatHourMinute(startMs)
            return UrgentTeachingSession(
                sessionId = session.id,
                subjectName = subj,
                className = cls,
                scheduledTimeStr = timeStr,
                minutesUntilStart = mins,
                isLive = false,
                isOneHourWarning = true,
            )
        }

        return null
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

    fun recordReadingProgress(item: BookReadingItem) {
        val studentId = currentAuth.userId ?: ""
        readingHistoryManager.saveReadingProgress(studentId, item)
        val updatedHistory = readingHistoryManager.getReadingHistory(studentId).filter { !it.isCompleted }
        _state.update { it.copy(activeReadingHistory = updatedHistory) }
    }

    fun startReadingSibiBook(book: LibraryBook): BookReadingItem {
        val item = BookReadingItem(
            id = book.id,
            title = book.title,
            author = book.author ?: "Pusat Perbukuan",
            publisher = book.publisher ?: "Kemendikdasmen",
            subjectName = book.subjectName ?: "Mata Pelajaran",
            gradeLevelName = book.gradeLevelName,
            coverUrl = book.coverUrl,
            fileUrl = book.fileUrl,
            currentPage = 1,
            totalPages = book.totalPages,
        )
        recordReadingProgress(item)
        return item
    }

    fun markBookCompleted(bookId: String) {
        val studentId = currentAuth.userId ?: ""
        readingHistoryManager.markBookCompleted(studentId, bookId)
        val updatedHistory = readingHistoryManager.getReadingHistory(studentId).filter { !it.isCompleted }
        _state.update { it.copy(activeReadingHistory = updatedHistory) }
    }

    fun logout() {
        viewModelScope.launch { authRepository.logout() }
    }
}
