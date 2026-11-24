package com.schoolos.android.data.repository

import com.schoolos.android.data.remote.SchoolOsApi
import com.schoolos.android.data.remote.dto.ComplianceItemDto
import com.schoolos.android.data.remote.dto.OfficialTeacherBiodataDto
import com.schoolos.android.domain.model.*
import com.schoolos.android.domain.repository.AcademicRepository
import com.schoolos.android.domain.repository.AnalyticsRepository
import com.schoolos.android.domain.repository.AssignmentRepository
import com.schoolos.android.domain.repository.LearningMaterialRepository
import com.schoolos.android.domain.repository.NotificationRepository
import com.schoolos.android.domain.repository.QuizRepository
import com.schoolos.android.domain.repository.SessionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnalyticsRepositoryImpl @Inject constructor(
    private val api: SchoolOsApi,
    private val notificationRepository: NotificationRepository,
    private val sessionRepository: SessionRepository,
    private val academicRepository: AcademicRepository,
    private val learningMaterialRepository: LearningMaterialRepository,
    private val assignmentRepository: AssignmentRepository,
    private val quizRepository: QuizRepository,
) : AnalyticsRepository {

    override suspend fun getSchoolStabilityMetrics(): Result<SchoolHealthMetrics> {
        return runCatching {
            val todayDate = LocalDate.now().toString()

            // 1. Fetch compliance
            val complianceDto = runCatching {
                val res = api.getScheduleCompliance(todayDate)
                res.data
            }.getOrNull()

            // 2. Fetch dashboard
            val dashboardDto = runCatching {
                val res = api.getAnalyticsDashboard()
                res.data
            }.getOrNull()

            // 3. Fetch overview
            val overviewDto = runCatching {
                val res = api.getAnalyticsOverview()
                res.data
            }.getOrNull()

            // 4. Fetch classes for fallback counts
            val classes = academicRepository.getClasses().getOrDefault(emptyList())

            val totalStudents = overviewDto?.totalStudents?.toInt()
                ?: dashboardDto?.metrics?.totalStudents?.toInt()
                ?: 0
            val totalTeachers = overviewDto?.totalTeachers?.toInt()
                ?: dashboardDto?.metrics?.totalTeachers?.toInt()
                ?: 0
            val totalClasses = overviewDto?.totalClasses?.toInt()
                ?: classes.size

            val complianceRate = if ((complianceDto?.totalScheduled ?: 0) > 0) {
                complianceDto?.complianceRate ?: 0.0
            } else {
                0.0
            }
            val attendanceRate = overviewDto?.attendanceRate ?: 0.0
            val atRiskStudents = overviewDto?.atRiskStudents?.toInt() ?: 0
            val overdueSessionsCount = complianceDto?.overdueUnrecordedCount?.toInt() ?: 0

            // Subject performance summaries
            val rawSubjects = dashboardDto?.academicPerformance ?: emptyList()
            val subjectSummaries = rawSubjects.map { raw ->
                val passRate = if (raw.totalGraded > 0) {
                    ((raw.passedCount.toDouble() / raw.totalGraded.toDouble()) * 100.0 * 10.0).let { Math.round(it) / 10.0 }
                } else {
                    0.0
                }
                AcademicPerformanceSummary(
                    subjectId = raw.subjectId,
                    subjectName = raw.subjectName,
                    subjectCode = raw.subjectCode,
                    averageScore = raw.averageScore,
                    minScore = raw.minScore,
                    maxScore = raw.maxScore,
                    totalGraded = raw.totalGraded.toInt(),
                    passedCount = raw.passedCount.toInt(),
                    remedialCount = raw.remedialCount.toInt(),
                    passRate = passRate,
                )
            }

            val highRemedialSubjects = subjectSummaries.filter { it.remedialCount > 0 && it.passRate < 80.0 }

            // ── ANOMALY DETECTION ENGINE (EARLY WARNING SYSTEM UNTUK KEPALA PKBM) ───────────────
            val anomalies = mutableListOf<SchoolAnomaly>()

            // A. Teaching Compliance & Tutorial Kosong
            if (overdueSessionsCount > 0) {
                val affectedSession = complianceDto?.items?.firstOrNull { it.state.equals("OVERDUE_UNRECORDED", ignoreCase = true) }
                val targetDesc = if (affectedSession != null) {
                    "${affectedSession.className} • ${affectedSession.subjectName} (${affectedSession.teacherName})"
                } else {
                    "$overdueSessionsCount Sesi Tutorial Terjadwal"
                }

                anomalies.add(
                    SchoolAnomaly(
                        id = "anomaly-kbm-overdue",
                        category = AnomalyCategory.TEACHING_COMPLIANCE,
                        severity = AnomalySeverity.HIGH,
                        title = "Sesi Belajar Terganggu: $overdueSessionsCount Sesi Tutorial Tanpa Presensi Tutor",
                        description = "Sesi pembelajaran / tutorial terjadwal telah melewati batas waktu mulai tanpa presensi tutor terkonfirmasi. Segera tentukan tutor pendamping agar kegiatan belajar warga belajar tidak terhenti.",
                        affectedTarget = targetDesc,
                        targetId = affectedSession?.sessionId ?: affectedSession?.scheduleId ?: "",
                        recommendedDecision = ExecutiveDecisionType.DISPATCH_SUBSTITUTE_TUTOR,
                        recommendedActionLabel = "Tugaskan Tutor Pendamping"
                    )
                )
            }

            // B. Academic KKTP Gap (Kurikulum Merdeka - Non Formal)
            for (sub in highRemedialSubjects.take(2)) {
                anomalies.add(
                    SchoolAnomaly(
                        id = "anomaly-kktp-${sub.subjectId}",
                        category = AnomalyCategory.ACADEMIC_KKTP_GAP,
                        severity = if (sub.passRate < 60.0) AnomalySeverity.HIGH else AnomalySeverity.MEDIUM,
                        title = "Capaian KKTP Belum Optimal: ${sub.subjectName} (${sub.remedialCount} Perlu Bimbingan)",
                        description = "Tingkat ketercapaian KKTP hanya ${sub.passRate}% dengan rata-rata modul ${sub.averageScore}. Diperlukan penguatan modul dan bimbingan belajar mandiri terstruktur.",
                        affectedTarget = sub.subjectName,
                        targetId = sub.subjectId,
                        recommendedDecision = ExecutiveDecisionType.ISSUE_KKTP_GUIDANCE,
                        recommendedActionLabel = "Instruksi Penguatan KKTP"
                    )
                )
            }

            // C. Low PKBM-Wide Attendance (Risiko Keterikatan WB)
            if (attendanceRate in 1.0..84.9) {
                anomalies.add(
                    SchoolAnomaly(
                        id = "anomaly-attendance-drop",
                        category = AnomalyCategory.ATTENDANCE_DROP,
                        severity = if (attendanceRate < 75.0) AnomalySeverity.HIGH else AnomalySeverity.MEDIUM,
                        title = "Keaktifan Warga Belajar PKBM Di Bawah Standar (${attendanceRate}%)",
                        description = "Persentase kehadiran tutorial/tatap muka berada di bawah target keaktifan warga belajar. Perlu pendampingan tutor pamong untuk mencegah risiko putus belajar (drop out).",
                        affectedTarget = "Seluruh Rombel PKBM",
                        recommendedDecision = ExecutiveDecisionType.ORDER_HOMEROOM_FOLLOW_UP,
                        recommendedActionLabel = "Instruksi Tindak Lanjut Tutor Pamong"
                    )
                )
            }

            // D. At-Risk / Drop-Out Students
            if (atRiskStudents > 0) {
                anomalies.add(
                    SchoolAnomaly(
                        id = "anomaly-at-risk-students",
                        category = AnomalyCategory.STUDENT_AT_RISK,
                        severity = if (atRiskStudents >= 5) AnomalySeverity.HIGH else AnomalySeverity.MEDIUM,
                        title = "$atRiskStudents Warga Belajar Memerlukan Pendampingan Khusus",
                        description = "Warga belajar teridentifikasi mengalami ketertinggalan modul secara berkelanjutan. Diperlukan konseling dan kunjungan pendampingan oleh tutor pamong.",
                        affectedTarget = "Daftar Warga Belajar Rentan",
                        recommendedDecision = ExecutiveDecisionType.ORDER_HOMEROOM_FOLLOW_UP,
                        recommendedActionLabel = "Pendampingan Tutor Pamong"
                    )
                )
            }

            // ── COMPUTE SCHOOL STABILITY SCORE ───────────────────────────────
            var score = 100
            val highCount = anomalies.count { it.severity == AnomalySeverity.HIGH }
            val medCount = anomalies.count { it.severity == AnomalySeverity.MEDIUM }
            score -= (highCount * 18)
            score -= (medCount * 8)
            if (attendanceRate in 1.0..89.0) {
                score -= ((90.0 - attendanceRate) * 0.8).toInt()
            }
            score = score.coerceIn(20, 100)

            val level = when {
                score < 70 || highCount >= 2 -> SchoolStabilityLevel.CRITICAL
                score < 88 || anomalies.isNotEmpty() -> SchoolStabilityLevel.WARNING
                else -> SchoolStabilityLevel.STABLE
            }

            val summaryText = when (level) {
                SchoolStabilityLevel.CRITICAL -> "Operasional Sekolah Memerlukan Intervensi Cepat (${anomalies.size} Catatan Terdeteksi)"
                SchoolStabilityLevel.WARNING -> "Kondisi Waspada: Diperlukan Perhatian Kepala Sekolah (${anomalies.size} Catatan)"
                SchoolStabilityLevel.STABLE -> "Operasional Sekolah Berjalan Kondusif & Terkelola Baik"
            }

            val academicMasteryRate = if (subjectSummaries.isNotEmpty()) {
                val totalP = subjectSummaries.sumOf { it.passedCount }
                val totalG = subjectSummaries.sumOf { it.totalGraded }
                if (totalG > 0) ((totalP.toDouble() / totalG.toDouble()) * 100.0 * 10.0).let { Math.round(it) / 10.0 }
                else 0.0
            } else {
                0.0
            }

            val hasRealData = overviewDto != null || complianceDto != null || dashboardDto != null

            val teacherWeeklyTrend = (dashboardDto?.teacherUploadTrend ?: emptyList()).map { trend ->
                val total = (trend.materi + trend.tugas).toInt()
                ActivityTrendPoint(
                    day = trend.day,
                    value = total.toFloat(),
                    rawCount = total,
                )
            }

            val studentWeeklyTrend = (dashboardDto?.studentEngagementTrend ?: emptyList()).map { trend ->
                val total = (trend.pembaca + trend.submisi).toInt()
                ActivityTrendPoint(
                    day = trend.day,
                    value = total.toFloat(),
                    rawCount = total,
                )
            }

            val completedSessions = (complianceDto?.completedCount ?: 0).toInt()
            val totalScheduled = (complianceDto?.totalScheduled ?: 0).toInt()

            SchoolHealthMetrics(
                stabilityScore = score,
                stabilityLevel = level,
                statusSummary = summaryText,
                attendanceRate = attendanceRate,
                complianceRate = complianceRate,
                academicMasteryRate = academicMasteryRate,
                atRiskStudentsCount = atRiskStudents,
                overdueSessionsCount = overdueSessionsCount,
                highRemedialSubjectsCount = highRemedialSubjects.size,
                totalActiveStudents = totalStudents,
                totalActiveTeachers = totalTeachers,
                totalClasses = totalClasses,
                anomalies = anomalies,
                subjectPerformances = subjectSummaries,
                hasRealData = hasRealData,
                teacherWeeklyTrend = teacherWeeklyTrend,
                studentWeeklyTrend = studentWeeklyTrend,
                completedSessionsCount = completedSessions,
                totalScheduledSessions = totalScheduled,
            )
        }.onFailure { error ->
            Timber.e(error, "Failed to compute school stability metrics")
        }
    }

    override suspend fun executePrincipalDecision(
        decisionType: ExecutiveDecisionType,
        targetId: String,
        targetName: String,
        title: String,
        instructions: String,
    ): Result<String> {
        return runCatching {
            when (decisionType) {
                ExecutiveDecisionType.DISPATCH_SUBSTITUTE_TUTOR -> {
                    notificationRepository.broadcastNotification(
                        classId = targetId,
                        title = "[INSTRUKSI KEPALA SEKOLAH] Penugasan Guru Pendamping",
                        body = "Kepala Sekolah menginstruksikan penugasan guru pendamping untuk $targetName: $instructions",
                        targetRoles = listOf("teacher", "staff")
                    )
                    "Penugasan guru pendamping berhasil diterbitkan ke dewan guru."
                }
                ExecutiveDecisionType.ISSUE_KKTP_GUIDANCE -> {
                    notificationRepository.broadcastNotification(
                        classId = "",
                        title = "[INSTRUKSI KEPALA SEKOLAH] Penguatan Materi & Pendampingan KKTP",
                        body = "Instruksi Kepala Sekolah untuk mata pelajaran/modul $targetName: $instructions. Harap segera berikan bimbingan ketercapaian KKTP.",
                        targetRoles = listOf("teacher")
                    )
                    "Instruksi penguatan ketercapaian KKTP berhasil disampaikan ke guru pengampu."
                }
                ExecutiveDecisionType.ORDER_HOMEROOM_FOLLOW_UP -> {
                    notificationRepository.broadcastNotification(
                        classId = "",
                        title = "[INSTRUKSI KEPALA SEKOLAH] Tindak Lanjut Wali Kelas",
                        body = "Instruksi Pimpinan Sekolah terkait $targetName: $instructions. Lakukan verifikasi dan pendampingan siswa.",
                        targetRoles = listOf("teacher", "staff")
                    )
                    "Instruksi pendampingan siswa berhasil disampaikan ke wali kelas & guru pamong."
                }
                ExecutiveDecisionType.BROADCAST_PRINCIPAL_MEMO -> {
                    notificationRepository.broadcastNotification(
                        classId = "",
                        title = "[MAKLUMAT KEPALA SEKOLAH] $title",
                        body = instructions,
                        targetRoles = listOf("teacher", "student", "parent")
                    )
                    "Maklumat resmi Kepala Sekolah berhasil disiarkan ke seluruh guru, siswa, dan wali."
                }
                ExecutiveDecisionType.INSPECT_ROMBEL -> {
                    "Navigasi ke rombel $targetName untuk supervisi modul dan presensi warga belajar."
                }
            }
        }
    }

    override suspend fun getGtkSupervisionOverview(): Result<GtkSupervisionOverview> = runCatching {
        withContext(Dispatchers.IO) {
            coroutineScope {
                val teachersDeferred = async {
                    runCatching { api.getTeachers(page = 1, pageSize = 200).data ?: emptyList() }.getOrDefault(emptyList())
                }
                val staffDeferred = async {
                    runCatching { api.getStaff(page = 1, pageSize = 200).data ?: emptyList() }.getOrDefault(emptyList())
                }
                val materialsDeferred = async {
                    learningMaterialRepository.getMaterials().getOrDefault(emptyList())
                }
                val assignmentsDeferred = async {
                    assignmentRepository.getAssignments(classId = "").getOrDefault(emptyList())
                }
                val quizzesDeferred = async {
                    quizRepository.getQuizzes(classId = "").getOrDefault(emptyList())
                }
                val sessionsDeferred = async {
                    sessionRepository.getSessions(classId = null).getOrDefault(emptyList())
                }
                val classesDeferred = async {
                    academicRepository.getClasses().getOrDefault(emptyList())
                }
                val complianceDeferred = async {
                    runCatching { api.getScheduleCompliance(LocalDate.now().toString()).data?.items ?: emptyList() }
                        .getOrDefault(emptyList())
                }

                val rawTeachers = teachersDeferred.await()
                val rawStaff = staffDeferred.await()
                val materials = materialsDeferred.await()
                val assignments = assignmentsDeferred.await()
                val quizzes = quizzesDeferred.await()
                val sessions = sessionsDeferred.await()
                val classes = classesDeferred.await()
                val complianceItems = complianceDeferred.await()

                val profiles = buildTeacherProfiles(
                    teacherDtos = rawTeachers,
                    materials = materials,
                    assignments = assignments,
                    quizzes = quizzes,
                    sessions = sessions,
                    classes = classes,
                    complianceItems = complianceItems,
                )

                val staffMembers = rawStaff
                    .filter { it.fullName.isNotBlank() }
                    .map { s ->
                        StaffMember(
                            id = s.id,
                            userId = s.userId,
                            fullName = s.fullName.trim(),
                            nuptk = s.nuptk,
                            nip = s.nip,
                            jk = s.jk,
                            tempatLahir = s.tempatLahir,
                            tanggalLahir = s.tanggalLahir,
                            statusKepegawaian = s.statusKepegawaian,
                            jenisPtk = s.jenisPtk,
                            agama = s.agama,
                            alamatJalan = s.alamatJalan,
                            noHp = s.noHp,
                            email = s.email,
                            jobTitle = s.jobTitle.ifBlank { s.jenisPtk ?: "Tenaga Kependidikan" },
                            isActive = s.isActive,
                            updatedAt = s.updatedAt,
                        )
                    }
                    .sortedWith(
                        compareByDescending<StaffMember> { it.isActive }
                            .thenBy { it.fullName.lowercase() }
                    )

                val activeTeachersCount = profiles.count { it.isActive }
                val activeStaffCount = staffMembers.count { it.isActive }
                val productiveCount = profiles.count {
                    it.tier == TeacherProductivityTier.SANGAT_PRODUKTIF ||
                        it.tier == TeacherProductivityTier.AKTIF
                }
                val needsCoachingCount = profiles.count { it.tier == TeacherProductivityTier.PERLU_PEMBINAAN }
                val avgScore = if (profiles.isNotEmpty()) {
                    profiles.map { it.performanceScore }.average().toInt()
                } else 0

                GtkSupervisionOverview(
                    teachers = profiles,
                    staff = staffMembers,
                    totalTeachers = profiles.size,
                    activeTeachersCount = activeTeachersCount,
                    totalStaff = staffMembers.size,
                    activeStaffCount = activeStaffCount,
                    productiveTeachersCount = productiveCount,
                    needsCoachingCount = needsCoachingCount,
                    averagePerformanceScore = avgScore,
                    totalModulesCount = materials.size,
                    totalAssignmentsCount = assignments.size,
                    totalQuizzesCount = quizzes.size,
                )
            }
        }
    }

    override suspend fun getTeacherSupervisionDetail(teacherId: String): Result<TeacherSupervisionProfile> = runCatching {
        withContext(Dispatchers.IO) {
            coroutineScope {
                val detailDtoDeferred = async {
                    runCatching { api.getTeacherById(teacherId).data }.getOrNull()
                }
                val listDtosDeferred = async {
                    runCatching { api.getTeachers(page = 1, pageSize = 200).data ?: emptyList() }.getOrDefault(emptyList())
                }
                val materialsDeferred = async {
                    learningMaterialRepository.getMaterials().getOrDefault(emptyList())
                }
                val assignmentsDeferred = async {
                    assignmentRepository.getAssignments(classId = "").getOrDefault(emptyList())
                }
                val quizzesDeferred = async {
                    quizRepository.getQuizzes(classId = "").getOrDefault(emptyList())
                }
                val sessionsDeferred = async {
                    sessionRepository.getSessions(classId = null).getOrDefault(emptyList())
                }
                val classesDeferred = async {
                    academicRepository.getClasses().getOrDefault(emptyList())
                }
                val complianceDeferred = async {
                    runCatching { api.getScheduleCompliance(LocalDate.now().toString()).data?.items ?: emptyList() }
                        .getOrDefault(emptyList())
                }

                val detailDto = detailDtoDeferred.await()
                val listDtos = listDtosDeferred.await()
                val mergedDtos = if (detailDto != null) {
                    val existingIdx = listDtos.indexOfFirst { it.id == detailDto.id }
                    if (existingIdx >= 0) {
                        listDtos.toMutableList().apply { set(existingIdx, detailDto) }
                    } else {
                        listDtos + detailDto
                    }
                } else {
                    listDtos
                }

                val profiles = buildTeacherProfiles(
                    teacherDtos = mergedDtos,
                    materials = materialsDeferred.await(),
                    assignments = assignmentsDeferred.await(),
                    quizzes = quizzesDeferred.await(),
                    sessions = sessionsDeferred.await(),
                    classes = classesDeferred.await(),
                    complianceItems = complianceDeferred.await(),
                )

                profiles.firstOrNull { it.teacher.id == teacherId }
                    ?: detailDto?.let {
                        buildTeacherProfiles(
                            teacherDtos = listOf(it),
                            materials = materialsDeferred.await(),
                            assignments = assignmentsDeferred.await(),
                            quizzes = quizzesDeferred.await(),
                            sessions = sessionsDeferred.await(),
                            classes = classesDeferred.await(),
                            complianceItems = complianceDeferred.await(),
                        ).firstOrNull()
                    }
                    ?: throw IllegalStateException("Data guru tidak ditemukan.")
            }
        }
    }

    private fun buildTeacherProfiles(
        teacherDtos: List<OfficialTeacherBiodataDto>,
        materials: List<LearningMaterial>,
        assignments: List<Assignment>,
        quizzes: List<Quiz>,
        sessions: List<LearningSession>,
        classes: List<AcademicClass>,
        complianceItems: List<ComplianceItemDto>,
    ): List<TeacherSupervisionProfile> {
        val teachers = teacherDtos
            .filter { it.fullName.isNotBlank() }
            .distinctBy { it.id }

        if (teachers.isEmpty()) return emptyList()

        // Match each activity item deterministically to the single best matching teacher
        val materialsByTeacher = HashMap<String, MutableList<LearningMaterial>>()
        val assignmentsByTeacher = HashMap<String, MutableList<Assignment>>()
        val quizzesByTeacher = HashMap<String, MutableList<Quiz>>()
        val sessionsByTeacher = HashMap<String, MutableList<LearningSession>>()
        val complianceByTeacher = HashMap<String, MutableList<ComplianceItemDto>>()

        val strippedNamesByTeacher = teachers.associate { it.id to normalizeTeacherName(it.fullName) }

        fun findBestTeacherId(candidateId: String?, candidateName: String?): String? {
            if (!candidateId.isNullOrBlank()) {
                val idMatch = teachers.firstOrNull { t ->
                    t.id.equals(candidateId, ignoreCase = true) ||
                        (!t.userId.isNullOrBlank() && t.userId.equals(candidateId, ignoreCase = true))
                }
                if (idMatch != null) return idMatch.id
            }
            val normCand = normalizeTeacherName(candidateName)
            if (normCand.length < 3) return null
            if (normCand in setOf("guru", "guru pengampu", "guru mata pelajaran", "tutor", "pihak sekolah", "admin")) {
                return null
            }
            teachers.firstOrNull { strippedNamesByTeacher[it.id] == normCand }?.let { return it.id }
            return teachers.firstOrNull { t ->
                val tName = strippedNamesByTeacher[t.id].orEmpty()
                tName.length >= 6 && (tName.startsWith(normCand) || normCand.startsWith(tName))
            }?.id
        }

        for (m in materials) {
            findBestTeacherId(m.teacherId, m.teacherName)?.let { tid ->
                materialsByTeacher.getOrPut(tid) { mutableListOf() }.add(m)
            }
        }
        for (a in assignments) {
            findBestTeacherId(null, a.teacherName)?.let { tid ->
                assignmentsByTeacher.getOrPut(tid) { mutableListOf() }.add(a)
            }
        }
        for (q in quizzes) {
            findBestTeacherId(q.teacherId, q.teacherName)?.let { tid ->
                quizzesByTeacher.getOrPut(tid) { mutableListOf() }.add(q)
            }
        }
        for (s in sessions) {
            val primaryTid = findBestTeacherId(s.teacherId, s.teacherName)
            if (primaryTid != null) {
                sessionsByTeacher.getOrPut(primaryTid) { mutableListOf() }.add(s)
            }
            if (!s.substituteTeacherId.isNullOrBlank() && s.substituteTeacherId != primaryTid) {
                findBestTeacherId(s.substituteTeacherId, s.substituteTeacherName)?.let { subTid ->
                    sessionsByTeacher.getOrPut(subTid) { mutableListOf() }.add(s)
                }
            }
        }
        for (c in complianceItems) {
            findBestTeacherId(c.teacherId, c.teacherName)?.let { tid ->
                complianceByTeacher.getOrPut(tid) { mutableListOf() }.add(c)
            }
        }

        val homeroomByTeacher = classes
            .filter { !it.homeroomTeacherId.isNullOrBlank() && !isUuidString(it.name) }
            .associateBy { it.homeroomTeacherId!! }

        return teachers.map { dto ->
            val active = dto.isActive &&
                !dto.status.equals("Inactive", ignoreCase = true) &&
                !dto.status.equals("Nonaktif", ignoreCase = true)
            val homeroomClass = homeroomByTeacher[dto.id]?.name
                ?: classes.firstOrNull { !it.homeroomTeacherId.isNullOrBlank() && it.homeroomTeacherId == dto.userId && !isUuidString(it.name) }?.name

            val tMaterials = (materialsByTeacher[dto.id] ?: emptyList())
                .sortedByDescending { it.createdAt ?: "" }
            val tAssignments = (assignmentsByTeacher[dto.id] ?: emptyList())
                .sortedByDescending { it.updatedAt.ifBlank { it.createdAt } }
            val tQuizzes = (quizzesByTeacher[dto.id] ?: emptyList())
                .sortedByDescending { it.updatedAt.ifBlank { it.createdAt } }
            val tSessions = (sessionsByTeacher[dto.id] ?: emptyList())
                .distinctBy { it.id }
                .sortedByDescending { it.sessionDate ?: it.scheduledAt ?: "" }
            val tCompliance = complianceByTeacher[dto.id] ?: emptyList()

            val assignedClasses = buildList {
                homeroomClass?.let { add(it) }
                dto.classesAssigned?.split(",", ";")?.forEach { part ->
                    val cleaned = part.trim()
                    if (cleaned.isNotBlank() && !isUuidString(cleaned)) add(cleaned)
                }
                tSessions.mapNotNull { it.className }.forEach { if (it.isNotBlank() && !isUuidString(it)) add(it) }
                tCompliance.map { it.className }.forEach { if (it.isNotBlank() && !isUuidString(it)) add(it) }
                tMaterials.mapNotNull { it.className }.forEach { if (it.isNotBlank() && !isUuidString(it)) add(it) }
                tAssignments.mapNotNull { it.className }.forEach { if (it.isNotBlank() && !isUuidString(it)) add(it) }
                tQuizzes.mapNotNull { it.effectiveDisplayClass }.forEach { if (it.isNotBlank() && !isUuidString(it)) add(it) }
            }.distinct()

            val inferredSubjects = buildList {
                tSessions.mapNotNull { it.subjectName }.forEach { add(it) }
                tCompliance.map { it.subjectName }.forEach { add(it) }
                tAssignments.mapNotNull { it.subjectName }.forEach { add(it) }
                tQuizzes.mapNotNull { it.subjectName }.forEach { add(it) }
                tMaterials.map { it.subject }.forEach { add(it) }
            }.map { it.trim() }
                .filter { it.isNotBlank() && !it.equals("Mata Pelajaran", ignoreCase = true) && !it.equals("Umum", ignoreCase = true) && !isUuidString(it) }
                .distinct()

            val displaySubject = dto.subject?.trim()?.takeIf { it.isNotBlank() && it != "-" }
                ?: inferredSubjects.take(2).joinToString(", ").ifBlank {
                    dto.jenisPtk?.takeIf { it.isNotBlank() } ?: "Guru Mata Pelajaran"
                }

            val totalReadings = tMaterials.sumOf { it.completedCount }
            val completedSessions = tSessions.count {
                it.status.equals("completed", ignoreCase = true) ||
                    it.status.equals("selesai", ignoreCase = true) ||
                    it.status.equals("in_progress", ignoreCase = true)
            }.coerceAtLeast(tCompliance.count { it.state == "COMPLETED" || it.state == "IN_PROGRESS" })
            val overdueCount = tCompliance.count { it.state == "OVERDUE_UNRECORDED" }

            // Transparent 4-Pillar Performance Score (0..100)
            val materialsScore = ((tMaterials.size * 6).coerceAtMost(24) +
                when {
                    totalReadings >= 10L -> 6
                    totalReadings >= 5L -> 4
                    totalReadings >= 1L -> 2
                    else -> 0
                }).coerceIn(0, 30)
            val assignmentsScore = (tAssignments.size * 6).coerceIn(0, 25)
            val quizzesScore = (tQuizzes.size * 8).coerceIn(0, 25)
            val sessionsScore = ((tSessions.size * 3).coerceAtMost(9) +
                (completedSessions * 3).coerceAtMost(7) +
                when {
                    homeroomClass != null -> 4
                    assignedClasses.isNotEmpty() -> 2
                    else -> 0
                } -
                (overdueCount * 3).coerceAtMost(6)).coerceIn(0, 20)

            val totalScore = (materialsScore + assignmentsScore + quizzesScore + sessionsScore).coerceIn(0, 100)
            val totalWorks = tMaterials.size + tAssignments.size + tQuizzes.size

            val tier = when {
                totalScore >= 70 || totalWorks >= 8 -> TeacherProductivityTier.SANGAT_PRODUKTIF
                totalScore >= 40 || totalWorks >= 3 -> TeacherProductivityTier.AKTIF
                totalScore >= 15 || totalWorks >= 1 || tSessions.isNotEmpty() || homeroomClass != null -> TeacherProductivityTier.CUKUP
                else -> TeacherProductivityTier.PERLU_PEMBINAAN
            }

            val latestDate = listOfNotNull(
                tMaterials.firstOrNull()?.createdAt,
                tAssignments.firstOrNull()?.updatedAt?.ifBlank { tAssignments.firstOrNull()?.createdAt },
                tQuizzes.firstOrNull()?.updatedAt?.ifBlank { tQuizzes.firstOrNull()?.createdAt },
                tSessions.firstOrNull()?.sessionDate ?: tSessions.firstOrNull()?.scheduledAt,
            ).maxOrNull()

            val insights = buildList {
                if (tMaterials.size >= 3) {
                    add("Aktif menyusun ${tMaterials.size} modul/bahan ajar ($totalReadings pembaca tuntas).")
                } else if (tMaterials.isNotEmpty()) {
                    add("Sudah menerbitkan ${tMaterials.size} modul ajar untuk peserta didik.")
                } else {
                    add("Belum ada modul atau bahan ajar terbit pada akun guru ini.")
                }

                if (tAssignments.size + tQuizzes.size >= 3) {
                    add("Konsisten melaksanakan asesmen (${tAssignments.size} tugas, ${tQuizzes.size} kuis CBT).")
                } else if (tAssignments.isEmpty() && tQuizzes.isEmpty()) {
                    add("Belum terdapat penugasan maupun evaluasi kuis CBT yang diterbitkan.")
                }

                if (overdueCount > 0) {
                    add("Perhatian: $overdueCount jadwal mengajar hari ini melewati waktu mulai tanpa presensi.")
                } else if (homeroomClass != null) {
                    add("Mengemban tugas tambahan sebagai Wali Rombel $homeroomClass.")
                } else if (assignedClasses.isNotEmpty()) {
                    add("Mengampu pembelajaran pada ${assignedClasses.size} rombongan belajar.")
                }
            }

            val biodata = OfficialTeacherBiodata(
                id = dto.id,
                tenantId = dto.tenantId,
                userId = dto.userId,
                nip = dto.nip,
                fullName = dto.fullName.trim(),
                nuptk = dto.nuptk,
                jk = dto.jk,
                tempatLahir = dto.tempatLahir,
                tanggalLahir = dto.tanggalLahir,
                statusKepegawaian = dto.statusKepegawaian,
                jenisPtk = dto.jenisPtk,
                agama = dto.agama,
                alamatJalan = dto.alamatJalan,
                rt = dto.rt,
                rw = dto.rw,
                dusun = dto.dusun,
                kelurahan = dto.kelurahan,
                kecamatan = dto.kecamatan,
                kodePos = dto.kodePos,
                noHp = dto.noHp,
                email = dto.email,
                subject = dto.subject ?: displaySubject,
                classesAssigned = assignedClasses.joinToString(", ").ifBlank { dto.classesAssigned },
                status = dto.status ?: if (dto.isActive) "Active" else "Inactive",
                fotoProfile = dto.fotoProfile,
            )

            TeacherSupervisionProfile(
                teacher = biodata,
                isActive = active,
                displaySubject = displaySubject,
                homeroomClassName = homeroomClass,
                assignedClasses = assignedClasses,
                materialsCount = tMaterials.size,
                assignmentsCount = tAssignments.size,
                quizzesCount = tQuizzes.size,
                sessionsCount = tSessions.size.coerceAtLeast(tCompliance.size),
                completedSessionsCount = completedSessions,
                overdueSessionsCount = overdueCount,
                totalReadingsCompleted = totalReadings,
                performanceScore = totalScore,
                materialsScore = materialsScore,
                assignmentsScore = assignmentsScore,
                quizzesScore = quizzesScore,
                sessionsScore = sessionsScore,
                tier = tier,
                latestActivityAt = latestDate,
                coachingInsights = insights,
                materials = tMaterials,
                assignments = tAssignments,
                quizzes = tQuizzes,
                sessions = tSessions,
            )
        }.sortedWith(
            compareByDescending<TeacherSupervisionProfile> { it.isActive }
                .thenByDescending { it.performanceScore }
                .thenByDescending { it.totalWorksCount }
                .thenBy { it.teacher.fullName.lowercase() }
        )
    }

    private fun normalizeTeacherName(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        val cleaned = raw.trim().lowercase().replace(Regex("[,.()]"), " ")
        val ignoreTokens = setOf(
            "spd", "mpd", "spdsd", "spdi", "sag", "skom", "se", "st", "msi", "mm",
            "ssi", "ssos", "sh", "shum", "lc", "ma", "phd", "psi", "drs", "dra",
            "dr", "prof", "h", "hj"
        )
        return cleaned.split(Regex("\\s+"))
            .filter { it.isNotBlank() && it !in ignoreTokens }
            .joinToString(" ")
    }

    private fun isUuidString(value: String): Boolean {
        val clean = value.trim()
        return clean.length >= 32 && clean.count { it == '-' } >= 4
    }
}
