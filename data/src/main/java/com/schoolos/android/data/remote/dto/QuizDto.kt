package com.schoolos.android.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class QuizDto(
    val id: String,
    val title: String,
    val description: String? = null,
    @SerialName("time_limit_minutes") val timeLimitMinutes: Int? = null,
    @SerialName("duration_minutes") val durationMinutes: Int? = null,
    @SerialName("passing_score") val passingScore: Int = 0,
    @SerialName("max_score") val maxScore: Int = 0,
    @SerialName("questions_count") val questionsCount: Int = 0,
    val status: String = "draft",
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
    @SerialName("max_attempts") val maxAttempts: Int = 1,
    @SerialName("class_id") val classId: String? = null,
    @SerialName("class_name") val className: String? = null,
    @SerialName("subject_name") val subjectName: String? = null,
    @SerialName("teacher_id") val teacherId: String? = null,
    @SerialName("teacher_name") val teacherName: String? = null,
    @SerialName("session_id") val sessionId: String? = null,
    @SerialName("exam_mode") val examMode: String = "HOMEWORK_QUIZ",
    @SerialName("assessment_category") val assessmentCategory: String? = null,
    @SerialName("exam_token") val examToken: String? = null,
    @SerialName("requires_token") val requiresToken: Boolean = false,
    @SerialName("token_expires_at") val tokenExpiresAt: String? = null,
    @SerialName("max_token_attempts") val maxTokenAttempts: Int = 5,
    @SerialName("start_at") val startAt: String? = null,
    @SerialName("end_at") val endAt: String? = null,
    @SerialName("student_attempt_status") val studentAttemptStatus: String? = null,
    @SerialName("student_attempts_count") val studentAttemptsCount: Int? = null,
    @SerialName("student_has_completed") val studentHasCompleted: Boolean? = null,
    @SerialName("student_last_score") val studentLastScore: Int? = null,
    @SerialName("student_last_attempt_id") val studentLastAttemptId: String? = null,
    @SerialName("release_at") val releaseAt: String? = null,
    @SerialName("exam_phase") val examPhase: String? = null,
    @SerialName("is_open_for_student") val isOpenForStudent: Boolean? = null,
    @SerialName("seconds_until_start") val secondsUntilStart: Long? = null,
    @SerialName("seconds_remaining") val secondsRemaining: Long? = null,
)

@Serializable
data class VerifyQuizTokenRequestDto(
    val token: String,
)

@Serializable
data class VerifyQuizTokenResponseDto(
    val valid: Boolean,
    val message: String,
)

@Serializable
data class AttemptAnswerDetailDto(
    @SerialName("question_id") val questionId: String,
    @SerialName("question_text") val questionText: String? = null,
    @SerialName("question_type") val questionType: String? = null,
    @SerialName("max_points") val maxPoints: Int = 0,
    @SerialName("chosen_choice_id") val chosenChoiceId: String? = null,
    @SerialName("chosen_choice_text") val chosenChoiceText: String? = null,
    @SerialName("is_correct") val isCorrect: Boolean? = null,
    @SerialName("text_answer") val textAnswer: String? = null,
    @SerialName("points_earned") val pointsEarned: Int = 0,
    @SerialName("teacher_feedback") val teacherFeedback: String? = null,
    @SerialName("interaction_config") val interactionConfig: JsonElement? = null,
)

@Serializable
data class QuizAttemptDto(
    val id: String,
    @SerialName("quiz_id") val quizId: String,
    @SerialName("student_id") val studentId: String,
    @SerialName("started_at") val startedAt: String,
    @SerialName("completed_at") val completedAt: String? = null,
    val score: Int? = null,
    @SerialName("total_points") val totalPoints: Int = 0,
    val percentage: Int? = null,
    val passed: Boolean? = null,
    val status: String = "in_progress",
    @SerialName("student_name") val studentName: String? = null,
    @SerialName("student_nisn") val studentNisn: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    val answers: List<AttemptAnswerDetailDto> = emptyList(),
)

@Serializable
data class QuizQuestionDto(
    val id: String,
    @SerialName("question_text") val questionText: String,
    @SerialName("question_type") val questionType: String = "multiple_choice",
    val points: Int = 1,
    @SerialName("order_index") val orderIndex: Int = 0,
    @SerialName("image_url") val imageUrl: String? = null,
    val choices: List<QuizChoiceDto> = emptyList(),
    @SerialName("interaction_config") val interactionConfig: JsonElement? = null,
)

@Serializable
data class QuizChoiceDto(
    val id: String,
    @SerialName("choice_text") val choiceText: String,
    @SerialName("order_index") val orderIndex: Int = 0,
    @SerialName("is_correct") val isCorrect: Boolean? = null,
)

@Serializable
data class UpdateQuizRequestDto(
    val title: String? = null,
    val description: String? = null,
    @SerialName("duration_minutes") val durationMinutes: Int? = null,
    @SerialName("start_at") val startAt: String? = null,
    @SerialName("end_at") val endAt: String? = null,
    @SerialName("exam_mode") val examMode: String? = null,
    @SerialName("assessment_category") val assessmentCategory: String? = null,
    @SerialName("exam_token") val examToken: String? = null,
    val status: String? = null,
)

@Serializable
data class RotateTokenRequestDto(
    @SerialName("custom_token") val customToken: String? = null,
    @SerialName("expires_in_minutes") val expiresInMinutes: Int? = null,
)

@Serializable
data class ExtendTimeRequestDto(
    @SerialName("extra_minutes") val extraMinutes: Int,
)

@Serializable
data class ProctorSessionOverviewDto(
    val id: String,
    val title: String,
    @SerialName("subject_name") val subjectName: String? = null,
    @SerialName("class_id") val classId: String? = null,
    @SerialName("class_name") val className: String? = null,
    @SerialName("teacher_id") val teacherId: String? = null,
    @SerialName("teacher_name") val teacherName: String? = null,
    @SerialName("exam_mode") val examMode: String = "HOMEWORK_QUIZ",
    @SerialName("assessment_category") val assessmentCategory: String = "FORMATIVE",
    val status: String = "draft",
    @SerialName("exam_phase") val examPhase: String = "DRAFT",
    @SerialName("duration_minutes") val durationMinutes: Int = 30,
    @SerialName("questions_count") val questionsCount: Int = 0,
    @SerialName("passing_score") val passingScore: Int = 70,
    @SerialName("exam_token") val examToken: String? = null,
    @SerialName("requires_token") val requiresToken: Boolean = false,
    @SerialName("token_expires_at") val tokenExpiresAt: String? = null,
    @SerialName("start_at") val startAt: String? = null,
    @SerialName("end_at") val endAt: String? = null,
    @SerialName("release_at") val releaseAt: String? = null,
    @SerialName("seconds_until_start") val secondsUntilStart: Long? = null,
    @SerialName("seconds_remaining") val secondsRemaining: Long? = null,
    @SerialName("total_enrolled") val totalEnrolled: Long = 0,
    @SerialName("started_count") val startedCount: Long = 0,
    @SerialName("in_progress_count") val inProgressCount: Long = 0,
    @SerialName("need_grading_count") val needGradingCount: Long = 0,
    @SerialName("completed_count") val completedCount: Long = 0,
    @SerialName("not_started_count") val notStartedCount: Long = 0,
    @SerialName("total_violations") val totalViolations: Long = 0,
    @SerialName("flagged_students_count") val flaggedStudentsCount: Long = 0,
    @SerialName("has_conflict") val hasConflict: Boolean = false,
)

@Serializable
data class ProctorScheduleSummaryDto(
    @SerialName("total_sessions") val totalSessions: Int = 0,
    @SerialName("live_count") val liveCount: Int = 0,
    @SerialName("upcoming_count") val upcomingCount: Int = 0,
    @SerialName("ended_count") val endedCount: Int = 0,
    @SerialName("draft_count") val draftCount: Int = 0,
    @SerialName("conflict_count") val conflictCount: Int = 0,
    @SerialName("flagged_students_total") val flaggedStudentsTotal: Long = 0,
)

@Serializable
data class ProctorScheduleConflictDto(
    @SerialName("conflict_type") val conflictType: String = "",
    @SerialName("primary_quiz_id") val primaryQuizId: String = "",
    @SerialName("primary_title") val primaryTitle: String = "",
    @SerialName("conflicting_quiz_id") val conflictingQuizId: String = "",
    @SerialName("conflicting_title") val conflictingTitle: String = "",
    @SerialName("scope_label") val scopeLabel: String = "",
    @SerialName("overlap_start") val overlapStart: String = "",
    @SerialName("overlap_end") val overlapEnd: String = "",
)

@Serializable
data class ProctorScheduleDashboardDto(
    @SerialName("server_now") val serverNow: String = "",
    val summary: ProctorScheduleSummaryDto = ProctorScheduleSummaryDto(),
    val conflicts: List<ProctorScheduleConflictDto> = emptyList(),
    val sessions: List<ProctorSessionOverviewDto> = emptyList(),
)

@Serializable
data class ProctorParticipantRowDto(
    @SerialName("student_id") val studentId: String,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("student_name") val studentName: String = "Siswa",
    @SerialName("student_nisn") val studentNisn: String = "-",
    @SerialName("class_name") val className: String? = null,
    @SerialName("attempt_id") val attemptId: String? = null,
    @SerialName("raw_attempt_status") val rawAttemptStatus: String? = null,
    @SerialName("proctor_status") val proctorStatus: String = "NOT_STARTED",
    @SerialName("started_at") val startedAt: String? = null,
    @SerialName("completed_at") val completedAt: String? = null,
    @SerialName("elapsed_seconds") val elapsedSeconds: Long? = null,
    @SerialName("answered_count") val answeredCount: Long = 0,
    @SerialName("total_questions") val totalQuestions: Int = 0,
    val score: Int? = null,
    @SerialName("total_points") val totalPoints: Int = 0,
    val percentage: Int? = null,
    val passed: Boolean? = null,
    @SerialName("leave_violations_count") val leaveViolationsCount: Int = 0,
    @SerialName("last_violation_at") val lastViolationAt: String? = null,
    @SerialName("last_heartbeat_at") val lastHeartbeatAt: String? = null,
    @SerialName("is_online") val isOnline: Boolean = false,
    @SerialName("last_device_platform") val lastDevicePlatform: String? = null,
)

@Serializable
data class ProctorEventDto(
    val id: String,
    @SerialName("student_id") val studentId: String,
    @SerialName("student_name") val studentName: String = "Siswa",
    @SerialName("attempt_id") val attemptId: String,
    @SerialName("event_type") val eventType: String = "",
    val detail: String? = null,
    @SerialName("device_platform") val devicePlatform: String? = null,
    @SerialName("created_at") val createdAt: String = "",
)

@Serializable
data class ProctorMonitorSummaryDto(
    @SerialName("total_enrolled") val totalEnrolled: Int = 0,
    @SerialName("not_started_count") val notStartedCount: Int = 0,
    @SerialName("in_progress_count") val inProgressCount: Int = 0,
    @SerialName("need_grading_count") val needGradingCount: Int = 0,
    @SerialName("completed_count") val completedCount: Int = 0,
    @SerialName("flagged_count") val flaggedCount: Int = 0,
    @SerialName("total_violations") val totalViolations: Long = 0,
)

@Serializable
data class ProctorMonitorDto(
    @SerialName("server_now") val serverNow: String = "",
    val quiz: QuizDto,
    val summary: ProctorMonitorSummaryDto = ProctorMonitorSummaryDto(),
    val participants: List<ProctorParticipantRowDto> = emptyList(),
    @SerialName("recent_events") val recentEvents: List<ProctorEventDto> = emptyList(),
)

@Serializable
data class SubmitAttemptRequest(
    val answers: List<SubmitAnswerRequest>,
)

@Serializable
data class SubmitAnswerRequest(
    @SerialName("question_id") val questionId: String,
    @SerialName("chosen_choice_id") val chosenChoiceId: String? = null,
    @SerialName("text_answer") val textAnswer: String? = null,
)

@Serializable
data class GradeQuizAttemptRequest(
    val score: Int? = null,
    val feedback: String? = null,
)

@Serializable
data class RecordProctorEventRequestDto(
    @SerialName("event_type") val eventType: String,
    val detail: String? = null,
    @SerialName("device_platform") val devicePlatform: String = "ANDROID",
)

