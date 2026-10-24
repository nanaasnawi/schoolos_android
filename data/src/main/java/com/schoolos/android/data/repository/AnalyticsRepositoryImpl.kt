package com.schoolos.android.data.repository

import com.schoolos.android.data.remote.SchoolOsApi
import com.schoolos.android.domain.model.*
import com.schoolos.android.domain.repository.AcademicRepository
import com.schoolos.android.domain.repository.AnalyticsRepository
import com.schoolos.android.domain.repository.NotificationRepository
import com.schoolos.android.domain.repository.SessionRepository
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

            val complianceRate = complianceDto?.complianceRate ?: 0.0
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
                SchoolStabilityLevel.CRITICAL -> "Operasional PKBM Memerlukan Intervensi Cepat (${anomalies.size} Catatan Terdeteksi)"
                SchoolStabilityLevel.WARNING -> "Kondisi Waspada: Diperlukan Perhatian Kepala PKBM (${anomalies.size} Catatan)"
                SchoolStabilityLevel.STABLE -> "Operasional PKBM Berjalan Kondusif & Terkelola Baik"
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
                        title = "[INSTRUKSI KEPALA PKBM] Penugasan Tutor Pendamping Tutorial",
                        body = "Kepala PKBM menginstruksikan penugasan tutor pendamping untuk $targetName: $instructions",
                        targetRoles = listOf("teacher", "staff")
                    )
                    "Penugasan tutor pendamping tutorial berhasil diterbitkan ke dewan tutor pamong."
                }
                ExecutiveDecisionType.ISSUE_KKTP_GUIDANCE -> {
                    notificationRepository.broadcastNotification(
                        classId = "",
                        title = "[INSTRUKSI KEPALA PKBM] Penguatan Modul & Pendampingan KKTP",
                        body = "Instruksi Kepala PKBM untuk mata pelajaran/modul $targetName: $instructions. Harap segera berikan bimbingan ketercapaian KKTP.",
                        targetRoles = listOf("teacher")
                    )
                    "Instruksi penguatan ketercapaian KKTP berhasil disampaikan ke tutor pengampu modul."
                }
                ExecutiveDecisionType.ORDER_HOMEROOM_FOLLOW_UP -> {
                    notificationRepository.broadcastNotification(
                        classId = "",
                        title = "[INSTRUKSI KEPALA PKBM] Tindak Lanjut Tutor Pamong",
                        body = "Instruksi Pimpinan PKBM terkait $targetName: $instructions. Lakukan verifikasi dan pendampingan warga belajar.",
                        targetRoles = listOf("teacher", "staff")
                    )
                    "Instruksi pendampingan warga belajar berhasil disampaikan ke tutor pamong."
                }
                ExecutiveDecisionType.BROADCAST_PRINCIPAL_MEMO -> {
                    notificationRepository.broadcastNotification(
                        classId = "",
                        title = "[MAKLUMAT KEPALA PKBM] $title",
                        body = instructions,
                        targetRoles = listOf("teacher", "student", "parent")
                    )
                    "Maklumat resmi Kepala PKBM berhasil disiarkan ke seluruh tutor, warga belajar, dan wali."
                }
                ExecutiveDecisionType.INSPECT_ROMBEL -> {
                    "Navigasi ke rombel $targetName untuk supervisi modul dan presensi warga belajar."
                }
            }
        }
    }
}
