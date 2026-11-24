package com.schoolos.android.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "quizzes")
data class QuizEntity(
    @PrimaryKey val id: String,
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
    val releaseAt: String? = null,
    val subjectName: String? = null,
    val classId: String? = null,
    val className: String? = null,
    val teacherName: String? = null,
    val examMode: String = "HOMEWORK_QUIZ",
    val assessmentCategory: String? = null,
    val examToken: String? = null,
    val requiresToken: Boolean = false,
    val startAt: String? = null,
    val endAt: String? = null,
    val examPhase: String? = null,
)
