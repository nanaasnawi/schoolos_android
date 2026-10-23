package com.schoolos.android.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AnalyticsOverviewDto(
    @SerialName("total_students") val totalStudents: Long = 0,
    @SerialName("active_students") val activeStudents: Long = 0,
    @SerialName("total_teachers") val totalTeachers: Long = 0,
    @SerialName("total_tendik") val totalTendik: Long = 0,
    @SerialName("total_classes") val totalClasses: Long = 0,
    @SerialName("active_classes") val activeClasses: Long = 0,
    @SerialName("total_guardians") val totalGuardians: Long = 0,
    @SerialName("attendance_rate") val attendanceRate: Double = 0.0,
    @SerialName("at_risk_students") val atRiskStudents: Long = 0,
)

@Serializable
data class DashboardDataResponseDto(
    val metrics: DashboardMetricsDto? = null,
    @SerialName("rombel_distribution") val rombelDistribution: List<RombelDistributionDto> = emptyList(),
    @SerialName("academic_performance") val academicPerformance: List<AcademicPerformanceDto> = emptyList(),
)

@Serializable
data class DashboardMetricsDto(
    @SerialName("total_students") val totalStudents: Long = 0,
    @SerialName("active_students") val activeStudents: Long = 0,
    @SerialName("transferred_students") val transferredStudents: Long = 0,
    @SerialName("total_teachers") val totalTeachers: Long = 0,
    @SerialName("active_teachers") val activeTeachers: Long = 0,
    @SerialName("total_tendik") val totalTendik: Long = 0,
    @SerialName("total_classes") val totalClasses: Long = 0,
    @SerialName("active_classes") val activeClasses: Long = 0,
    @SerialName("total_guardians") val totalGuardians: Long = 0,
    @SerialName("total_learning_materials") val totalLearningMaterials: Long = 0,
    @SerialName("total_quizzes") val totalQuizzes: Long = 0,
    @SerialName("total_assignments") val totalAssignments: Long = 0,
    @SerialName("total_submissions") val totalSubmissions: Long = 0,
    @SerialName("dapodik_sync_records") val dapodikSyncRecords: Long = 0,
    @SerialName("total_notifications") val totalNotifications: Long = 0,
)

@Serializable
data class AcademicPerformanceDto(
    @SerialName("subject_id") val subjectId: String,
    @SerialName("subject_name") val subjectName: String,
    @SerialName("subject_code") val subjectCode: String = "",
    @SerialName("total_graded") val totalGraded: Long = 0,
    @SerialName("average_score") val averageScore: Double = 0.0,
    @SerialName("min_score") val minScore: Double = 0.0,
    @SerialName("max_score") val maxScore: Double = 0.0,
    @SerialName("passed_count") val passedCount: Long = 0,
    @SerialName("remedial_count") val remedialCount: Long = 0,
)

@Serializable
data class RombelDistributionDto(
    val id: String,
    val name: String,
    @SerialName("jenis_rombel") val jenisRombel: String? = null,
    @SerialName("student_count") val studentCount: Long = 0,
)
