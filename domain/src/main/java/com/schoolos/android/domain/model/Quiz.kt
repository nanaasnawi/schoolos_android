package com.schoolos.android.domain.model

data class Quiz(
    val id: String,
    val title: String,
    val description: String?,
    val timeLimitMinutes: Int?,
    val passingScore: Int,
    val maxScore: Int,
    val questionsCount: Int,
    val status: String,
    val isActive: Boolean,
    val createdAt: String,
    val updatedAt: String,
    val subjectName: String? = null,
    val classId: String? = null,
    val className: String? = null,
    val maxAttempts: Int = 1,
    val studentAttemptStatus: String? = null,
    val studentAttemptsCount: Int = 0,
    val studentHasCompleted: Boolean = false,
    val studentLastScore: Int? = null,
    val studentLastAttemptId: String? = null,
)
