package com.schoolos.android.domain.model

data class QuizQuestion(
    val id: String,
    val questionText: String,
    val questionType: String,
    val points: Int,
    val orderIndex: Int,
    val imageUrl: String? = null,
    val choices: List<QuizChoice>,
    val interactionConfigJson: String? = null,
)

data class QuizChoice(
    val id: String,
    val choiceText: String,
    val orderIndex: Int,
    val isCorrect: Boolean? = null,
)
