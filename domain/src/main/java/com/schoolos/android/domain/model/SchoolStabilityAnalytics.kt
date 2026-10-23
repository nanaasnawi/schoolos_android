package com.schoolos.android.domain.model

enum class SchoolStabilityLevel {
    CRITICAL, // Kritis / Perlu Tindakan Segera
    WARNING,  // Waspada / Perlu Perhatian
    STABLE    // Stabil & Kondusif
}

enum class AnomalyCategory {
    TEACHING_COMPLIANCE, // KBM Terganggu / Guru Belum Hadir / Jam Kosong
    ACADEMIC_REMEDIAL,   // Ketidaktuntasan KKM / Remedial Tinggi
    ATTENDANCE_DROP,     // Lonjakan Ketidakhadiran Rombel
    STUDENT_AT_RISK,     // Siswa Rawan / Rentan Putus Sekolah
}

enum class AnomalySeverity {
    HIGH,
    MEDIUM,
    LOW
}

enum class ExecutiveDecisionType {
    DISPATCH_SUBSTITUTE_TEACHER, // Tugaskan Guru Pengganti / Inval
    ISSUE_REMEDIAL_MANDATE,      // Perintahkan Remedial & Pengayaan KKM
    ORDER_HOMEROOM_FOLLOW_UP,    // Instruksikan Tindak Lanjut Wali Kelas & BP/BK
    BROADCAST_PRINCIPAL_MEMO,    // Terbitkan Instruksi Resmi / Maklumat Kepala Sekolah
    INSPECT_ROMBEL,              // Audit Rombel Lapangan
}

data class SchoolAnomaly(
    val id: String,
    val category: AnomalyCategory,
    val severity: AnomalySeverity,
    val title: String,
    val description: String,
    val affectedTarget: String, // e.g. "Kelas 4 • Matematika" atau "Seluruh Rombel"
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
)
