package com.schoolos.android.domain.model

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
)
