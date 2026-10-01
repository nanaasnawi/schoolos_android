package com.schoolos.android.domain.model

data class AttemptAnswerDetail(
    val questionId: String,
    val questionText: String? = null,
    val questionType: String? = null,
    val maxPoints: Int = 0,
    val chosenChoiceId: String? = null,
    val chosenChoiceText: String? = null,
    val isCorrect: Boolean? = null,
    val textAnswer: String? = null,
    val pointsEarned: Int = 0,
    val teacherFeedback: String? = null,
)

data class QuizAttempt(
    val id: String,
    val quizId: String,
    val studentId: String,
    val startedAt: String,
    val completedAt: String?,
    val score: Int?,
    val totalPoints: Int,
    val percentage: Int? = null,
    val passed: Boolean? = null,
    val status: String,
    val studentName: String? = null,
    val studentNisn: String? = null,
    val createdAt: String,
    val updatedAt: String,
    val answers: List<AttemptAnswerDetail> = emptyList(),
) {
    val correctCount: Int get() = answers.count { it.isCorrect == true }
    val incorrectCount: Int get() = answers.count { it.isCorrect == false }
    val unansweredCount: Int get() = answers.count { it.chosenChoiceId == null && it.textAnswer.isNullOrBlank() }
}
