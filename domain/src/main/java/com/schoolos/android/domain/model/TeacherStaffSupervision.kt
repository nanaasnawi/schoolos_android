package com.schoolos.android.domain.model

enum class TeacherProductivityTier(
    val label: String,
    val shortLabel: String,
) {
    SANGAT_PRODUKTIF("Sangat Produktif", "Sangat Aktif"),
    AKTIF("Aktif Berkarya", "Aktif"),
    CUKUP("Cukup Aktif", "Cukup"),
    PERLU_PEMBINAAN("Perlu Pembinaan", "Perlu Dorongan"),
}

data class TeacherSupervisionProfile(
    val teacher: OfficialTeacherBiodata,
    val isActive: Boolean,
    val displaySubject: String,
    val homeroomClassName: String? = null,
    val assignedClasses: List<String> = emptyList(),
    val materialsCount: Int = 0,
    val assignmentsCount: Int = 0,
    val quizzesCount: Int = 0,
    val sessionsCount: Int = 0,
    val completedSessionsCount: Int = 0,
    val overdueSessionsCount: Int = 0,
    val totalReadingsCompleted: Long = 0L,
    val performanceScore: Int = 0,
    val materialsScore: Int = 0,
    val assignmentsScore: Int = 0,
    val quizzesScore: Int = 0,
    val sessionsScore: Int = 0,
    val tier: TeacherProductivityTier = TeacherProductivityTier.PERLU_PEMBINAAN,
    val latestActivityAt: String? = null,
    val coachingInsights: List<String> = emptyList(),
    val materials: List<LearningMaterial> = emptyList(),
    val assignments: List<Assignment> = emptyList(),
    val quizzes: List<Quiz> = emptyList(),
    val sessions: List<LearningSession> = emptyList(),
) {
    val totalWorksCount: Int
        get() = materialsCount + assignmentsCount + quizzesCount
}

data class StaffMember(
    val id: String,
    val userId: String? = null,
    val fullName: String,
    val nuptk: String? = null,
    val nip: String? = null,
    val jk: String? = null,
    val tempatLahir: String? = null,
    val tanggalLahir: String? = null,
    val statusKepegawaian: String? = null,
    val jenisPtk: String? = null,
    val agama: String? = null,
    val alamatJalan: String? = null,
    val noHp: String? = null,
    val email: String? = null,
    val jobTitle: String,
    val isActive: Boolean = true,
    val updatedAt: String? = null,
)

data class GtkSupervisionOverview(
    val teachers: List<TeacherSupervisionProfile> = emptyList(),
    val staff: List<StaffMember> = emptyList(),
    val totalTeachers: Int = 0,
    val activeTeachersCount: Int = 0,
    val totalStaff: Int = 0,
    val activeStaffCount: Int = 0,
    val productiveTeachersCount: Int = 0,
    val needsCoachingCount: Int = 0,
    val averagePerformanceScore: Int = 0,
    val totalModulesCount: Int = 0,
    val totalAssignmentsCount: Int = 0,
    val totalQuizzesCount: Int = 0,
)
