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

            val complianceRate = complianceDto?.complianceRate ?: 100.0
            val attendanceRate = overviewDto?.attendanceRate ?: 95.0
            val atRiskStudents = overviewDto?.atRiskStudents?.toInt() ?: 0
            val overdueSessionsCount = complianceDto?.overdueUnrecordedCount?.toInt() ?: 0

            // Subject performance summaries
            val rawSubjects = dashboardDto?.academicPerformance ?: emptyList()
            val subjectSummaries = rawSubjects.map { raw ->
                val passRate = if (raw.totalGraded > 0) {
                    ((raw.passedCount.toDouble() / raw.totalGraded.toDouble()) * 100.0 * 10.0).let { Math.round(it) / 10.0 }
                } else {
                    100.0
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

            // ── ANOMALY DETECTION ENGINE (EARLY WARNING SYSTEM) ───────────────
            val anomalies = mutableListOf<SchoolAnomaly>()

            // A. Teaching Compliance & Jam Kosong
            if (overdueSessionsCount > 0) {
                val affectedSession = complianceDto?.items?.firstOrNull { it.state.equals("OVERDUE_UNRECORDED", ignoreCase = true) }
                val targetDesc = if (affectedSession != null) {
                    "${affectedSession.className} • ${affectedSession.subjectName} (${affectedSession.teacherName})"
                } else {
                    "$overdueSessionsCount Sesi Terjadwal"
                }

                anomalies.add(
                    SchoolAnomaly(
                        id = "anomaly-kbm-overdue",
                        category = AnomalyCategory.TEACHING_COMPLIANCE,
                        severity = AnomalySeverity.HIGH,
                        title = "KBM Terganggu: $overdueSessionsCount Sesi Jam Pelajaran Tanpa Presensi / Guru",
                        description = "Sesi pembelajaran terjadwal telah melewati batas waktu mulai tanpa kehadiran terkonfirmasi. Segera tentukan guru pengganti (inval) agar KBM tidak terlantar.",
                        affectedTarget = targetDesc,
                        targetId = affectedSession?.sessionId ?: affectedSession?.scheduleId ?: "",
                        recommendedDecision = ExecutiveDecisionType.DISPATCH_SUBSTITUTE_TEACHER,
                        recommendedActionLabel = "Tugaskan Guru Pengganti (Inval)"
                    )
                )
            }

            // B. Academic KKM & Remedial Spike
            for (sub in highRemedialSubjects.take(2)) {
                anomalies.add(
                    SchoolAnomaly(
                        id = "anomaly-kkm-${sub.subjectId}",
                        category = AnomalyCategory.ACADEMIC_REMEDIAL,
                        severity = if (sub.passRate < 60.0) AnomalySeverity.HIGH else AnomalySeverity.MEDIUM,
                        title = "Ketidaktuntasan KKM: ${sub.subjectName} (${sub.remedialCount} Remedial)",
                        description = "Tingkat ketuntasan hanya ${sub.passRate}% dengan rata-rata kelas ${sub.averageScore}. Diperlukan intervensi remedial terstruktur dan review instrumen asesmen.",
                        affectedTarget = sub.subjectName,
                        targetId = sub.subjectId,
                        recommendedDecision = ExecutiveDecisionType.ISSUE_REMEDIAL_MANDATE,
                        recommendedActionLabel = "Terbitkan Mandat Remedial"
                    )
                )
            }

            // C. Low School-Wide Attendance
            if (attendanceRate > 0.0 && attendanceRate < 85.0) {
                anomalies.add(
                    SchoolAnomaly(
                        id = "anomaly-attendance-drop",
                        category = AnomalyCategory.ATTENDANCE_DROP,
                        severity = if (attendanceRate < 75.0) AnomalySeverity.HIGH else AnomalySeverity.MEDIUM,
                        title = "Tingkat Kehadiran Sekolah Di Bawah Standar (${attendanceRate}%)",
                        description = "Persentase kehadiran harian berada di bawah target institusi (90%). Berpotensi mengindikasikan lonjakan absensi tanpa keterangan atau wabah sakit rombel.",
                        affectedTarget = "Seluruh Rombel Sekolah",
                        recommendedDecision = ExecutiveDecisionType.ORDER_HOMEROOM_FOLLOW_UP,
                        recommendedActionLabel = "Instruksi Tindak Lanjut Wali Kelas"
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
                        title = "$atRiskStudents Siswa Terindikasi Memerlukan Intervensi Khusus",
                        description = "Siswa teridentifikasi tidak tuntas pada kriteria ketuntasan minimal berkelanjutan. Diperlukan konseling wali kelas & bimbingan konseling (BK).",
                        affectedTarget = "Daftar Siswa Evaluasi",
                        recommendedDecision = ExecutiveDecisionType.ORDER_HOMEROOM_FOLLOW_UP,
                        recommendedActionLabel = "Pemanggilan Wali Murid & BK"
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
                SchoolStabilityLevel.CRITICAL -> "Kondisi Sekolah Memerlukan Intervensi Cepat (${anomalies.size} Masalah Terdeteksi)"
                SchoolStabilityLevel.WARNING -> "Kondisi Waspada: Diperlukan Perhatian Pimpinan (${anomalies.size} Catatan)"
                SchoolStabilityLevel.STABLE -> "Kondisi Sekolah Stabil, Kondusif, dan Berjalan Optimal"
            }

            val academicMasteryRate = if (subjectSummaries.isNotEmpty()) {
                val totalP = subjectSummaries.sumOf { it.passedCount }
                val totalG = subjectSummaries.sumOf { it.totalGraded }
                if (totalG > 0) ((totalP.toDouble() / totalG.toDouble()) * 100.0 * 10.0).let { Math.round(it) / 10.0 }
                else 100.0
            } else {
                100.0
            }

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
                ExecutiveDecisionType.DISPATCH_SUBSTITUTE_TEACHER -> {
                    notificationRepository.broadcastNotification(
                        classId = targetId,
                        title = "[INSTRUKSI KEPSEK] Penugasan Guru Pengganti / Inval KBM",
                        body = "Kepala Sekolah menginstruksikan penugasan guru pengganti untuk $targetName: $instructions",
                        targetRoles = listOf("teacher", "staff")
                    )
                    "✓ Penugasan guru pengganti dan instruksi darurat KBM berhasil diterbitkan ke seluruh dewan guru."
                }
                ExecutiveDecisionType.ISSUE_REMEDIAL_MANDATE -> {
                    notificationRepository.broadcastNotification(
                        classId = "",
                        title = "[INSTRUKSI KEPSEK] Mandat Remedial & Intervensi KKM",
                        body = "Instruksi Kepala Sekolah untuk mata pelajaran $targetName: $instructions. Harap segera susun jadwal remedial.",
                        targetRoles = listOf("teacher")
                    )
                    "✓ Mandat remedial dan intervensi kurikulum berhasil dikirimkan ke guru mata pelajaran."
                }
                ExecutiveDecisionType.ORDER_HOMEROOM_FOLLOW_UP -> {
                    notificationRepository.broadcastNotification(
                        classId = "",
                        title = "[INSTRUKSI KEPSEK] Tindak Lanjut Wali Kelas & BP/BK",
                        body = "Instruksi Pimpinan terkait $targetName: $instructions. Lakukan verifikasi dan pemanggilan orang tua bila diperlukan.",
                        targetRoles = listOf("teacher", "staff")
                    )
                    "✓ Instruksi tindak lanjut berhasil disampaikan ke seluruh wali kelas dan staf bimbingan konseling (BK)."
                }
                ExecutiveDecisionType.BROADCAST_PRINCIPAL_MEMO -> {
                    notificationRepository.broadcastNotification(
                        classId = "",
                        title = "[MAKLUMAT KEPALA SEKOLAH] $title",
                        body = instructions,
                        targetRoles = listOf("teacher", "student", "parent")
                    )
                    "✓ Maklumat resmi Kepala Sekolah berhasil disiarkan ke seluruh civitas akademika sekolah."
                }
                ExecutiveDecisionType.INSPECT_ROMBEL -> {
                    "✓ Navigasi ke rombel $targetName untuk verifikasi data Dapodik dan presensi."
                }
            }
        }
    }
}
