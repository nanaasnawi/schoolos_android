package com.schoolos.android.domain.model

data class AssignmentChoice(
    val id: String?,
    val choiceText: String,
    val isCorrect: Boolean?,
    val orderIndex: Int?,
)

data class AssignmentQuestion(
    val id: String?,
    val questionText: String,
    val questionType: String, // "MULTIPLE_CHOICE" or "ESSAY"
    val points: Int?,
    val orderIndex: Int?,
    val choices: List<AssignmentChoice> = emptyList(),
)

data class SubmissionAnswer(
    val questionId: String,
    val questionText: String,
    val questionType: String,
    val maxPoints: Int,
    val chosenChoiceId: String?,
    val chosenChoiceText: String?,
    val isCorrect: Boolean?,
    val textAnswer: String?,
    val pointsEarned: Int,
    val teacherFeedback: String?,
)
