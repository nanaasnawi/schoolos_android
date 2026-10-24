package com.schoolos.android.domain.model

enum class SchoolStabilityLevel {
    CRITICAL, // Kritis / Perlu Tindakan Segera
    WARNING,  // Waspada / Perlu Perhatian Pimpinan
    STABLE    // Stabil & Kondusif
}

enum class AnomalyCategory {
    TEACHING_COMPLIANCE, // Sesi Pembelajaran / Tutorial Terganggu (Tutor Belum Hadir)
    ACADEMIC_KKTP_GAP,   // Capaian KKTP Modul Belum Optimal / Perlu Bimbingan
    ATTENDANCE_DROP,     // Keaktifan Warga Belajar Menurun
    STUDENT_AT_RISK,     // Warga Belajar Rentan Putus Belajar (Drop Out)
}

enum class AnomalySeverity {
    HIGH,
    MEDIUM,
    LOW
}

enum class ExecutiveDecisionType {
    DISPATCH_SUBSTITUTE_TUTOR, // Tugaskan Tutor Pendamping / Pengganti
    ISSUE_KKTP_GUIDANCE,       // Instruksikan Penguatan Modul & Pendampingan KKTP
    ORDER_HOMEROOM_FOLLOW_UP,  // Instruksikan Tindak Lanjut Tutor Pamong
    BROADCAST_PRINCIPAL_MEMO,  // Terbitkan Maklumat Resmi Kepala PKBM
    INSPECT_ROMBEL,            // Supervisi Rombel Belajar Mandiri / Tutorial
}

data class SchoolAnomaly(
    val id: String,
    val category: AnomalyCategory,
    val severity: AnomalySeverity,
    val title: String,
    val description: String,
    val affectedTarget: String, // e.g. "Paket C • Modul Matematika" atau "Seluruh Rombel"
    val targetId: String = "",
    val recommendedDecision: ExecutiveDecisionType,
    val recommendedActionLabel: String,
)

data class AcademicPerformanceSummary(
    val subjectId: String,
    val subjectName: String,
    val subjectCode: String,
    val averageScore: Double,
    val minScore: Double,
    val maxScore: Double,
    val totalGraded: Int,
    val passedCount: Int,
    val remedialCount: Int,
    val passRate: Double,
)

data class SchoolHealthMetrics(
    val stabilityScore: Int, // 0 - 100
    val stabilityLevel: SchoolStabilityLevel,
    val statusSummary: String,
    val attendanceRate: Double,
    val complianceRate: Double,
    val academicMasteryRate: Double,
    val atRiskStudentsCount: Int,
    val overdueSessionsCount: Int,
    val highRemedialSubjectsCount: Int,
    val totalActiveStudents: Int,
    val totalActiveTeachers: Int,
    val totalClasses: Int,
    val anomalies: List<SchoolAnomaly> = emptyList(),
    val subjectPerformances: List<AcademicPerformanceSummary> = emptyList(),
    val hasRealData: Boolean = true,
)

