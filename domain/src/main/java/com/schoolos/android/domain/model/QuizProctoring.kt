package com.schoolos.android.domain.model

data class QuizProctorScheduleDashboard(
    val serverNow: String,
    val summary: QuizProctorScheduleSummary,
    val conflicts: List<QuizProctorScheduleConflict>,
    val sessions: List<QuizProctorSessionOverview>,
)

data class QuizProctorScheduleSummary(
    val totalSessions: Int = 0,
    val liveCount: Int = 0,
    val upcomingCount: Int = 0,
    val endedCount: Int = 0,
    val draftCount: Int = 0,
    val conflictCount: Int = 0,
    val flaggedStudentsTotal: Long = 0,
)

data class QuizProctorScheduleConflict(
    val conflictType: String,
    val primaryQuizId: String,
    val primaryTitle: String,
    val conflictingQuizId: String,
    val conflictingTitle: String,
    val scopeLabel: String,
    val overlapStart: String,
    val overlapEnd: String,
)

data class QuizProctorSessionOverview(
    val id: String,
    val title: String,
    val subjectName: String? = null,
    val classId: String? = null,
    val className: String? = null,
    val teacherId: String? = null,
    val teacherName: String? = null,
    val examMode: String = "HOMEWORK_QUIZ",
    val assessmentCategory: String = "FORMATIVE",
    val status: String,
    val examPhase: String,
    val durationMinutes: Int,
    val questionsCount: Int,
    val passingScore: Int,
    val examToken: String? = null,
    val requiresToken: Boolean = false,
    val tokenExpiresAt: String? = null,
    val startAt: String? = null,
    val endAt: String? = null,
    val releaseAt: String? = null,
    val secondsUntilStart: Long? = null,
    val secondsRemaining: Long? = null,
    val totalEnrolled: Long = 0,
    val startedCount: Long = 0,
    val inProgressCount: Long = 0,
    val needGradingCount: Long = 0,
    val completedCount: Long = 0,
    val notStartedCount: Long = 0,
    val totalViolations: Long = 0,
    val flaggedStudentsCount: Long = 0,
    val hasConflict: Boolean = false,
)

data class QuizProctorMonitor(
    val serverNow: String,
    val quiz: Quiz,
    val summary: QuizProctorMonitorSummary,
    val participants: List<QuizProctorParticipantRow>,
    val recentEvents: List<QuizProctorEventLog>,
)

data class QuizProctorMonitorSummary(
    val totalEnrolled: Int = 0,
    val notStartedCount: Int = 0,
    val inProgressCount: Int = 0,
    val needGradingCount: Int = 0,
    val completedCount: Int = 0,
    val flaggedCount: Int = 0,
    val totalViolations: Long = 0,
)

data class QuizProctorParticipantRow(
    val studentId: String,
    val userId: String? = null,
    val studentName: String,
    val studentNisn: String,
    val className: String? = null,
    val attemptId: String? = null,
    val rawAttemptStatus: String? = null,
    val proctorStatus: String,
    val startedAt: String? = null,
    val completedAt: String? = null,
    val elapsedSeconds: Long? = null,
    val answeredCount: Long = 0,
    val totalQuestions: Int = 0,
    val score: Int? = null,
    val totalPoints: Int = 0,
    val percentage: Int? = null,
    val passed: Boolean? = null,
    val leaveViolationsCount: Int = 0,
    val lastViolationAt: String? = null,
    val lastHeartbeatAt: String? = null,
    val isOnline: Boolean = false,
    val lastDevicePlatform: String? = null,
)

data class QuizProctorEventLog(
    val id: String,
    val studentId: String,
    val studentName: String,
    val attemptId: String,
    val eventType: String,
    val detail: String? = null,
    val devicePlatform: String? = null,
    val createdAt: String,
)
