package com.schoolos.android.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AssignmentChoiceDto(
    val id: String? = null,
    @SerialName("choice_text") val choiceText: String,
    @SerialName("is_correct") val isCorrect: Boolean? = null,
    @SerialName("order_index") val orderIndex: Int? = null,
)

@Serializable
data class AssignmentQuestionDto(
    val id: String? = null,
    @SerialName("question_text") val questionText: String,
    @SerialName("question_type") val questionType: String = "MULTIPLE_CHOICE",
    val points: Int? = null,
    @SerialName("order_index") val orderIndex: Int? = null,
    val choices: List<AssignmentChoiceDto> = emptyList(),
)

@Serializable
data class SubmitAnswerDto(
    @SerialName("question_id") val questionId: String,
    @SerialName("chosen_choice_id") val chosenChoiceId: String? = null,
    @SerialName("text_answer") val textAnswer: String? = null,
)

@Serializable
data class SubmissionAnswerDetailDto(
    @SerialName("question_id") val questionId: String,
    @SerialName("question_text") val questionText: String,
    @SerialName("question_type") val questionType: String,
    @SerialName("max_points") val maxPoints: Int,
    @SerialName("chosen_choice_id") val chosenChoiceId: String? = null,
    @SerialName("chosen_choice_text") val chosenChoiceText: String? = null,
    @SerialName("is_correct") val isCorrect: Boolean? = null,
    @SerialName("text_answer") val textAnswer: String? = null,
    @SerialName("points_earned") val pointsEarned: Int = 0,
    @SerialName("teacher_feedback") val teacherFeedback: String? = null,
)
