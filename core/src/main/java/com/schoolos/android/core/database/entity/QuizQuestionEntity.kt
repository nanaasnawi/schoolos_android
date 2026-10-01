package com.schoolos.android.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "quiz_questions")
data class QuizQuestionEntity(
    @PrimaryKey val id: String,
    val quizId: String,
    val questionText: String,
    val questionType: String,
    val points: Int,
    val orderIndex: Int,
    val imageUrl: String?,
    val choicesJson: String,
)
