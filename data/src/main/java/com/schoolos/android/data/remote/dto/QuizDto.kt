package com.schoolos.android.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
    @SerialName("session_id") val sessionId: String? = null,
    @SerialName("exam_mode") val examMode: String = "HOMEWORK_QUIZ",
    @SerialName("exam_token") val examToken: String? = null,
    @SerialName("token_expires_at") val tokenExpiresAt: String? = null,
    @SerialName("max_token_attempts") val maxTokenAttempts: Int = 5,
    @SerialName("student_attempt_status") val studentAttemptStatus: String? = null,
    @SerialName("student_attempts_count") val studentAttemptsCount: Int? = null,
    @SerialName("student_has_completed") val studentHasCompleted: Boolean? = null,
    @SerialName("student_last_score") val studentLastScore: Int? = null,
    @SerialName("student_last_attempt_id") val studentLastAttemptId: String? = null,
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
)

@Serializable
data class QuizChoiceDto(
    val id: String,
    @SerialName("choice_text") val choiceText: String,
    @SerialName("order_index") val orderIndex: Int = 0,
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

