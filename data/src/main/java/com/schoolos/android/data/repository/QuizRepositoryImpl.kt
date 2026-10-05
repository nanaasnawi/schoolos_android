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
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
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
            val response = api.getQuizzes(classId.ifBlank { null })
            val quizzes = response.data?.map { it.dtoToDomain() } ?: emptyList()
            try {
                if (classId.isBlank()) {
                    quizDao.clearAll()
                }
                if (quizzes.isNotEmpty()) {
                    quizDao.insertAll(quizzes.map { it.toEntity() })
                }
            } catch (_: Exception) {}
            return@runCatching quizzes
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
        val isOnline = try { networkMonitor.isOnline.first() } catch (_: Exception) { true }
        if (isOnline) {
            try {
                val response = api.getQuizQuestions(quizId)
                val questions = response.data?.map { it.dtoToDomain() }
                if (questions != null) {
                    val entities = questions.map { q ->
                        val choicesDtos = q.choices.map {
                            com.schoolos.android.data.remote.dto.QuizChoiceDto(
                                id = it.id,
                                choiceText = it.choiceText,
                                orderIndex = it.orderIndex,
                            )
                        }
                        com.schoolos.android.core.database.entity.QuizQuestionEntity(
                            id = q.id,
                            quizId = quizId,
                            questionText = q.questionText,
                            questionType = q.questionType,
                            points = q.points,
                            orderIndex = q.orderIndex,
                            imageUrl = q.imageUrl,
                            choicesJson = Json.encodeToString(choicesDtos),
                        )
                    }
                    try {
                        quizDao.deleteQuestionsForQuiz(quizId)
                        quizDao.insertQuestions(entities)
                    } catch (_: Exception) {}
                    return@runCatching questions
                }
            } catch (_: Exception) {
                // fall through to cache
            }
        }
        val cached = quizDao.getQuestionsForQuiz(quizId)
        if (cached.isNotEmpty()) {
            cached.map { entity ->
                val choicesDtos: List<com.schoolos.android.data.remote.dto.QuizChoiceDto> = try {
                    Json.decodeFromString<List<com.schoolos.android.data.remote.dto.QuizChoiceDto>>(entity.choicesJson)
                } catch (_: Exception) {
                    emptyList()
                }
                QuizQuestion(
                    id = entity.id,
                    questionText = entity.questionText,
                    questionType = entity.questionType,
                    points = entity.points,
                    orderIndex = entity.orderIndex,
                    imageUrl = entity.imageUrl,
                    choices = choicesDtos.map { it.dtoToDomain() },
                )
            }
        } else {
            throw Exception("Gagal memuat butir soal kuis. Periksa koneksi internet Anda.")
        }
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

    private fun parseHttpError(e: Throwable, defaultMessage: String): Exception {
        if (e is retrofit2.HttpException) {
            val errorBody = try { e.response()?.errorBody()?.string() } catch (_: Exception) { null }
            val parsedMsg = try {
                if (!errorBody.isNullOrBlank()) {
                    val json = Json { ignoreUnknownKeys = true }
                    val apiResp = json.decodeFromString<com.schoolos.android.data.remote.dto.ApiResponse<kotlinx.serialization.json.JsonElement>>(errorBody)
                    apiResp.error?.message
                } else null
            } catch (_: Exception) {
                null
            }
            val message = when {
                parsedMsg?.contains("status 'draft'", ignoreCase = true) == true ->
                    "Kuis ini masih berupa draf dan belum dipublikasikan oleh guru."
                parsedMsg?.contains("Maximum attempt limit", ignoreCase = true) == true ->
                    "Anda telah mencapai batas maksimal pengerjaan untuk kuis ini."
                parsedMsg?.contains("not opened yet", ignoreCase = true) == true ->
                    "Kuis belum dibuka untuk dikerjakan."
                parsedMsg?.contains("expired", ignoreCase = true) == true ->
                    "Batas waktu pengerjaan kuis telah berakhir."
                parsedMsg?.contains("bukan untuk kelas Anda", ignoreCase = true) == true ||
                parsedMsg?.contains("Unauthorized", ignoreCase = true) == true ||
                e.code() == 401 || e.code() == 403 ->
                    parsedMsg ?: "Anda tidak memiliki izin untuk mengakses kuis ini."
                !parsedMsg.isNullOrBlank() -> parsedMsg
                e.code() == 400 -> "Permintaan tidak valid (Bad Request)."
                e.code() == 404 -> "Kuis tidak ditemukan."
                else -> defaultMessage
            }
            return Exception(message, e)
        }
        return Exception(e.message ?: defaultMessage, e)
    }

    override suspend fun publishQuiz(id: String): Result<Quiz> = try {
        val response = api.publishQuiz(id)
        val domain = response.data?.dtoToDomain() ?: throw Exception(response.error?.message ?: "Gagal menerbitkan kuis.")
        Result.success(domain)
    } catch (e: Throwable) {
        Result.failure(parseHttpError(e, "Gagal menerbitkan kuis."))
    }

    override suspend fun startAttempt(quizId: String): Result<QuizAttempt> = try {
        val studentId = authManager.getStudentId() ?: throw Exception("Sesi pengguna tidak valid.")
        val response = api.startAttempt(quizId, StartAttemptRequest(studentId = studentId))
        val domain = response.data?.dtoToDomain() ?: throw Exception(response.error?.message ?: "Gagal memulai pengerjaan kuis CBT.")
        Result.success(domain)
    } catch (e: Throwable) {
        Result.failure(parseHttpError(e, "Gagal memulai pengerjaan kuis CBT."))
    }

    override suspend fun submitAttempt(
        quizId: String,
        attemptId: String,
        answers: List<AnswerInput>
    ): Result<QuizAttempt> = try {
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
        val domain = response.data?.dtoToDomain() ?: throw Exception(response.error?.message ?: "Gagal mengumpulkan jawaban kuis.")
        Result.success(domain)
    } catch (e: Throwable) {
        Result.failure(parseHttpError(e, "Gagal mengumpulkan jawaban kuis."))
    }

    override suspend fun getQuizAttempts(quizId: String): Result<List<QuizAttempt>> = runCatching {
        val response = api.getQuizAttempts(quizId)
        response.data?.map { it.dtoToDomain() } ?: emptyList()
    }

    override suspend fun getQuizAttempt(quizId: String, attemptId: String): Result<QuizAttempt> = runCatching {
        val response = api.getQuizAttempt(quizId, attemptId)
        response.data?.dtoToDomain() ?: throw Exception(response.error?.message ?: "Gagal memuat hasil kuis.")
    }
}
