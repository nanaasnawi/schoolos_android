package com.schoolos.android.data.repository

import com.schoolos.android.core.auth.AuthManager
import com.schoolos.android.core.database.dao.QuizDao
import com.schoolos.android.core.database.mapper.toDomain
import com.schoolos.android.core.database.mapper.toEntity
import com.schoolos.android.core.network.NetworkMonitor
import com.schoolos.android.data.mapper.toDomain as dtoToDomain
import com.schoolos.android.data.remote.SchoolOsApi
import com.schoolos.android.data.remote.StartAttemptRequest
import com.schoolos.android.data.remote.CreateQuizQuestionRequestDto
import com.schoolos.android.data.remote.CreateQuizOptionRequestDto
import com.schoolos.android.data.remote.dto.SubmitAnswerRequest
import com.schoolos.android.data.remote.dto.SubmitAttemptRequest
import com.schoolos.android.domain.model.Quiz
import com.schoolos.android.domain.model.QuizAttempt
import com.schoolos.android.domain.model.QuizQuestion
import com.schoolos.android.domain.repository.AnswerInput
import com.schoolos.android.domain.repository.ChoiceInput
import com.schoolos.android.domain.repository.QuizRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QuizRepositoryImpl @Inject constructor(
    private val api: SchoolOsApi,
    private val authManager: AuthManager,
    private val quizDao: QuizDao,
    private val networkMonitor: NetworkMonitor,
) : QuizRepository {

    override suspend fun getQuizzes(classId: String): Result<List<Quiz>> = runCatching {
        try {
            val response = api.getQuizzes(classId)
            val quizzes = response.data?.map { it.dtoToDomain() }
            if (quizzes != null) {
                if (classId.isBlank() && quizzes.isNotEmpty()) {
                    quizDao.clearAll()
                }
                quizDao.insertAll(quizzes.map { it.toEntity() })
                return@runCatching quizzes
            }
        } catch (e: Exception) {
            android.util.Log.w("QuizRepo", "Remote fetch quizzes failed, falling back to cache: ${e.message}")
        }
        val cached = try { quizDao.getQuizzes().first() } catch (_: Exception) { emptyList() }
        cached.map { it.toDomain() }
    }

    fun getCachedQuizzes(): Flow<List<Quiz>> {
        return quizDao.getQuizzes().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getQuiz(id: String): Result<Quiz> = runCatching {
        try {
            val response = api.getQuiz(id)
            val domain = response.data?.dtoToDomain()
            if (domain != null) {
                quizDao.insert(domain.toEntity())
                return@runCatching domain
            }
        } catch (e: Exception) {
            android.util.Log.w("QuizRepo", "Remote getQuiz failed, checking cache: ${e.message}")
        }
        val cached = quizDao.getQuizById(id)
        cached?.toDomain() ?: throw Exception("Kuis tidak ditemukan atau perangkat sedang offline.")
    }

    override suspend fun createQuiz(
        title: String,
        description: String?,
        classId: String,
        timeLimitMinutes: Int?,
        passingScore: Int,
        maxScore: Int
    ): Result<Quiz> = runCatching {
        val targetClassId = classId.ifBlank { null }
        val request = com.schoolos.android.data.remote.CreateQuizRequestDto(
            lessonId = null,
            title = title,
            description = description,
            durationMinutes = timeLimitMinutes ?: 30,
            passingScore = passingScore,
            maxAttempts = 1,
            classId = targetClassId
        )
        val response = api.createQuiz(request)
        response.data?.dtoToDomain() ?: throw Exception(response.error?.message ?: "Gagal membuat kuis.")
    }

    override suspend fun getQuestions(quizId: String): Result<List<QuizQuestion>> = runCatching {
        val response = api.getQuizQuestions(quizId)
        response.data?.map { it.dtoToDomain() } ?: throw Exception(response.error?.message ?: "Gagal memuat soal kuis.")
    }

    override suspend fun addQuestion(
        quizId: String,
        questionText: String,
        questionType: String,
        points: Int,
        imageUrl: String?,
        choices: List<ChoiceInput>
    ): Result<QuizQuestion> = runCatching {
        val request = CreateQuizQuestionRequestDto(
            questionText = questionText,
            questionType = questionType,
            points = points,
            orderIndex = 1,
            imageUrl = imageUrl,
            choices = choices.mapIndexed { index, c ->
                CreateQuizOptionRequestDto(
                    choiceText = c.choiceText,
                    isCorrect = c.isCorrect,
                    orderIndex = if (c.orderIndex > 0) c.orderIndex else index + 1
                )
            }
        )
        val response = api.addQuizQuestion(quizId, request)
        response.data?.dtoToDomain() ?: throw Exception(response.error?.message ?: "Gagal menambahkan butir soal kuis.")
    }

    override suspend fun publishQuiz(id: String): Result<Quiz> = runCatching {
        val response = api.publishQuiz(id)
        response.data?.dtoToDomain() ?: throw Exception(response.error?.message ?: "Gagal menerbitkan kuis.")
    }

    override suspend fun startAttempt(quizId: String): Result<QuizAttempt> = runCatching {
        val studentId = authManager.getStudentId() ?: throw Exception("Sesi pengguna tidak valid.")
        val response = api.startAttempt(quizId, StartAttemptRequest(studentId = studentId))
        response.data?.dtoToDomain() ?: throw Exception(response.error?.message ?: "Gagal memulai pengerjaan kuis CBT.")
    }

    override suspend fun submitAttempt(
        quizId: String,
        attemptId: String,
        answers: List<AnswerInput>
    ): Result<QuizAttempt> = runCatching {
        val request = SubmitAttemptRequest(
            answers = answers.map {
                SubmitAnswerRequest(
                    questionId = it.questionId,
                    chosenChoiceId = it.chosenChoiceId,
                    textAnswer = it.textAnswer
                )
            }
        )
        val idempotencyKey = java.util.UUID.randomUUID().toString()
        val response = api.submitAttempt(
            quizId = quizId,
            attemptId = attemptId,
            request = request,
            idempotencyKey = idempotencyKey,
        )
        response.data?.dtoToDomain() ?: throw Exception(response.error?.message ?: "Gagal mengumpulkan jawaban kuis.")
    }
}
