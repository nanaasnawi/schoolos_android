package com.schoolos.android.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.schoolos.android.core.database.entity.QuizEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QuizDao {
    @Query("SELECT * FROM quizzes ORDER BY createdAt DESC")
    fun getQuizzes(): Flow<List<QuizEntity>>

    @Query("SELECT * FROM quizzes WHERE id = :id LIMIT 1")
    suspend fun getQuizById(id: String): QuizEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(quiz: QuizEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(quizzes: List<QuizEntity>)

    @Query("DELETE FROM quizzes")
    suspend fun clearAll()

    @Query("SELECT * FROM quiz_questions WHERE quizId = :quizId ORDER BY orderIndex ASC")
    suspend fun getQuestionsForQuiz(quizId: String): List<com.schoolos.android.core.database.entity.QuizQuestionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestions(questions: List<com.schoolos.android.core.database.entity.QuizQuestionEntity>)

    @Query("DELETE FROM quiz_questions WHERE quizId = :quizId")
    suspend fun deleteQuestionsForQuiz(quizId: String)
}
