package com.schoolos.android.data.repository

import com.schoolos.android.core.auth.AuthManager
import com.schoolos.android.core.network.ApiClient
import com.schoolos.android.domain.model.GeneratedCurriculumChoice
import com.schoolos.android.domain.model.GeneratedCurriculumQuestion
import com.schoolos.android.domain.model.GeneratedCurriculumResult
import com.schoolos.android.domain.model.GeneratedMaterialBlock
import com.schoolos.android.domain.model.GeneratedMaterialResult
import com.schoolos.android.domain.repository.CurriculumGeneratorRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository implementasi pembuatan materi dan kurikulum otomatis berbasis AI.
 * Sesuai arsitektur sistem SchoolOS, seluruh business logic, prompt engineering,
 * dan kredensial AI berada secara terpusat di backend Rust (Axum API Server).
 * Klien Android murni mendelegasikan permintaan ke endpoint `/api/v1/ai/generate-content`.
 */
@Singleton
class CurriculumGeneratorRepositoryImpl @Inject constructor(
    private val apiClient: ApiClient,
    private val authManager: AuthManager,
) : CurriculumGeneratorRepository {

    // Klien HTTP dengan batas waktu baca 90s untuk menunggu generasi AI di backend
    private val aiHttpClient: OkHttpClient by lazy {
        apiClient.httpClient.newBuilder()
            .followRedirects(true)
            .followSslRedirects(true)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .writeTimeout(90, TimeUnit.SECONDS)
            .build()
    }

    private suspend fun getApiV1Base(): String {
        val rawServerUrl = (authManager.getCustomServerUrl() ?: com.schoolos.android.core.common.BuildConfig.API_BASE_URL).trimEnd('/')
        val rootBase = when {
            rawServerUrl.endsWith("/api/v1") -> rawServerUrl.removeSuffix("/api/v1").trimEnd('/')
            rawServerUrl.endsWith("/api") -> rawServerUrl.removeSuffix("/api").trimEnd('/')
            else -> rawServerUrl
        }
        return "$rootBase/api/v1"
    }

    private fun parseQuizFromJson(
        quizObj: JSONObject,
        type: String,
        effectiveTopic: String,
        subjectName: String,
        isExam: Boolean
    ): GeneratedCurriculumResult {
        val questionsArray = quizObj.getJSONArray("questions")
        val questionsList = ArrayList<GeneratedCurriculumQuestion>()

        for (i in 0 until questionsArray.length()) {
            val qObj = questionsArray.getJSONObject(i)
            val choicesArray = qObj.optJSONArray("choices")
            val choicesList = ArrayList<GeneratedCurriculumChoice>()

            if (choicesArray != null) {
                for (j in 0 until choicesArray.length()) {
                    val choiceItem = choicesArray.get(j)
                    if (choiceItem is JSONObject) {
                        choicesList.add(
                            GeneratedCurriculumChoice(
                                choiceText = choiceItem.optString("choice_text", choiceItem.optString("text", "")),
                                isCorrect = choiceItem.optBoolean("is_correct", choiceItem.optBoolean("isCorrect", false)),
                            )
                        )
                    } else if (choiceItem is String) {
                        choicesList.add(
                            GeneratedCurriculumChoice(
                                choiceText = choiceItem,
                                isCorrect = (j == 0)
                            )
                        )
                    }
                }
            }

            questionsList.add(
                GeneratedCurriculumQuestion(
                    id = qObj.optString("id", "q-${i + 1}"),
                    questionText = qObj.optString("question_text", qObj.optString("text", "")),
                    questionType = qObj.optString("question_type", "MULTIPLE_CHOICE"),
                    points = qObj.optInt("points", if (isExam) 10 else 20),
                    choices = choicesList,
                    explanation = qObj.optString("explanation", "Disusun otomatis oleh AI backend"),
                    rubric = if (qObj.isNull("rubric")) null else qObj.optString("rubric"),
                )
            )
        }

        return GeneratedCurriculumResult(
            title = quizObj.optString("title", "Paket Soal CBT: $effectiveTopic"),
            instructions = quizObj.optString("instructions", "Kerjakan seluruh butir soal ujian pilihan ganda berikut dengan teliti."),
            description = quizObj.optString("description", "Paket soal CBT resmi Kurikulum Merdeka."),
            format = type,
            questions = questionsList,
            timeLimitMinutes = quizObj.optInt("time_limit_minutes", if (isExam) 90 else 45),
            passingScore = quizObj.optInt("passing_score", 75),
            subjectName = subjectName,
        )
    }

    private fun parseAssignmentFromJson(
        assignObj: JSONObject,
        type: String,
        effectiveTopic: String,
        subjectName: String
    ): GeneratedCurriculumResult {
        val tasksArray = assignObj.optJSONArray("tasks")
        val rubric = assignObj.optString("rubric", "Rubrik Penilaian Objektif")
        val questionsList = ArrayList<GeneratedCurriculumQuestion>()

        if (tasksArray != null && tasksArray.length() > 0) {
            val count = maxOf(1, tasksArray.length())
            for (i in 0 until tasksArray.length()) {
                val taskItem = tasksArray.get(i)
                val taskText = if (taskItem is JSONObject) {
                    taskItem.optString("task_text", taskItem.optString("text", ""))
                } else {
                    taskItem.toString()
                }

                questionsList.add(
                    GeneratedCurriculumQuestion(
                        id = "task-${i + 1}",
                        questionText = taskText,
                        questionType = "ESSAY",
                        points = 100 / count,
                        choices = emptyList(),
                        explanation = rubric,
                        rubric = rubric,
                    )
                )
            }
        } else if (assignObj.has("questions")) {
            val questionsArray = assignObj.getJSONArray("questions")
            val count = maxOf(1, questionsArray.length())
            for (i in 0 until questionsArray.length()) {
                val qObj = questionsArray.getJSONObject(i)
                questionsList.add(
                    GeneratedCurriculumQuestion(
                        id = qObj.optString("id", "task-${i + 1}"),
                        questionText = qObj.optString("question_text", ""),
                        questionType = qObj.optString("question_type", "ESSAY"),
                        points = qObj.optInt("points", 100 / count),
                        choices = emptyList(),
                        explanation = rubric,
                        rubric = rubric,
                    )
                )
            }
        }

        return GeneratedCurriculumResult(
            title = assignObj.optString("title", "Tugas Siswa: $effectiveTopic"),
            instructions = "${assignObj.optString("instructions", "Selesaikan tugas berikut sesuai instruksi guru.")}\n\nRubrik Penilaian:\n$rubric",
            description = assignObj.optString("description", "Lembar tugas terstruktur Kurikulum Merdeka."),
            format = type,
            questions = questionsList,
            timeLimitMinutes = assignObj.optInt("time_limit_minutes", 45),
            passingScore = assignObj.optInt("passing_score", 70),
            subjectName = subjectName,
        )
    }

    override suspend fun generateCurriculum(
        type: String,
        subjectId: String,
        subjectName: String,
        sourceMode: String,
        topic: String?,
        gradeLevel: String?,
    ): Result<GeneratedCurriculumResult> = withContext(Dispatchers.IO) {
        val effectiveTopic = topic?.trim()?.ifBlank { null } ?: subjectName
        val effectiveGrade = gradeLevel ?: "Kelas 5 SD"
        val isQuiz = type.startsWith("QUIZ") || type == "EXAM_MONTHLY"
        val isExam = type == "EXAM_MONTHLY"

        val apiV1 = getApiV1Base()
        val token = authManager.getAccessToken()
        val mediaType = "application/json; charset=utf-8".toMediaType()

        val aiMode = if (isQuiz) "QUIZ" else "ASSIGNMENT"
        val payload = JSONObject().apply {
            put("mode", aiMode)
            put("topic", effectiveTopic)
            put("subject_name", subjectName)
            put("grade_level", effectiveGrade)
            put("num_questions", if (isExam) 10 else 5)
            put("difficulty", if (isExam) "HOTS" else "Sedang")
        }

        try {
            val url = "$apiV1/ai/generate-content"
            val requestBuilder = Request.Builder()
                .url(url)
                .post(payload.toString().toRequestBody(mediaType))

            if (!token.isNullOrBlank()) {
                requestBuilder.addHeader("Authorization", "Bearer $token")
            }

            val response = aiHttpClient.newCall(requestBuilder.build()).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful || responseBody.isBlank()) {
                val errorMsg = try {
                    val errJson = JSONObject(responseBody)
                    errJson.optJSONObject("error")?.optString("message")
                        ?: "Gagal menghubungi backend AI (HTTP ${response.code})"
                } catch (_: Exception) {
                    "Gagal menghubungi backend AI (HTTP ${response.code})"
                }
                Timber.w("AI curriculum generation failed with HTTP ${response.code}: $responseBody")
                return@withContext Result.failure(Exception(errorMsg))
            }

            val json = JSONObject(responseBody)
            if (!json.optBoolean("success", false)) {
                val errorMsg = json.optJSONObject("error")?.optString("message") ?: "Gagal memproses AI kurikulum"
                return@withContext Result.failure(Exception(errorMsg))
            }

            val data = json.getJSONObject("data")
            if (data.has("quiz") && !data.isNull("quiz")) {
                val quizObj = data.getJSONObject("quiz")
                return@withContext Result.success(
                    parseQuizFromJson(quizObj, type, effectiveTopic, subjectName, isExam)
                )
            }

            if (data.has("assignment") && !data.isNull("assignment")) {
                val assignObj = data.getJSONObject("assignment")
                return@withContext Result.success(
                    parseAssignmentFromJson(assignObj, type, effectiveTopic, subjectName)
                )
            }

            Result.failure(Exception("Format respon kurikulum dari backend tidak valid."))
        } catch (e: Exception) {
            Timber.e(e, "Curriculum generation exception")
            Result.failure(e)
        }
    }

    override suspend fun generateMaterial(
        mode: String,
        topic: String,
        gradeLevel: String,
        subjectName: String,
    ): Result<GeneratedMaterialResult> = withContext(Dispatchers.IO) {
        val apiV1 = getApiV1Base()
        val token = authManager.getAccessToken()
        val mediaType = "application/json; charset=utf-8".toMediaType()

        val payload = JSONObject().apply {
            put("mode", mode.uppercase())
            put("topic", topic.trim())
            put("grade_level", gradeLevel)
            put("subject_name", subjectName)
        }

        try {
            val url = "$apiV1/ai/generate-content"
            val requestBuilder = Request.Builder()
                .url(url)
                .post(payload.toString().toRequestBody(mediaType))

            if (!token.isNullOrBlank()) {
                requestBuilder.addHeader("Authorization", "Bearer $token")
            }

            val response = aiHttpClient.newCall(requestBuilder.build()).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful || responseBody.isBlank()) {
                val errorMsg = try {
                    val errJson = JSONObject(responseBody)
                    errJson.optJSONObject("error")?.optString("message")
                        ?: "Gagal menghubungi backend AI (HTTP ${response.code})"
                } catch (_: Exception) {
                    "Gagal menghubungi backend AI (HTTP ${response.code})"
                }
                Timber.w("AI material generation failed with HTTP ${response.code}: $responseBody")
                return@withContext Result.failure(Exception(errorMsg))
            }

            val json = JSONObject(responseBody)
            if (!json.optBoolean("success", false)) {
                val errorMsg = json.optJSONObject("error")?.optString("message") ?: "Gagal memproses materi pembelajaran AI"
                return@withContext Result.failure(Exception(errorMsg))
            }

            val data = json.getJSONObject("data")

            if (mode.equals("INFOGRAPHIC", ignoreCase = true) && data.has("infographic") && !data.isNull("infographic")) {
                val infoObj = data.getJSONObject("infographic")
                val blocksArr = infoObj.optJSONArray("blocks") ?: JSONArray()
                val blocksList = ArrayList<GeneratedMaterialBlock>()

                for (i in 0 until blocksArr.length()) {
                    val bObj = blocksArr.getJSONObject(i)
                    val bType = bObj.optString("block_type", bObj.optString("type", "TEXT"))
                    blocksList.add(
                        GeneratedMaterialBlock(
                            id = bObj.optString("id", UUID.randomUUID().toString()),
                            type = bType,
                            content = bObj.optString("content", "")
                        )
                    )
                }

                return@withContext Result.success(
                    GeneratedMaterialResult(
                        title = infoObj.optString("title", "Infografis: ${topic.trim()}"),
                        description = infoObj.optString("description", "Modul infografis interaktif Kurikulum Merdeka."),
                        mode = "INFOGRAPHIC",
                        blocks = blocksList
                    )
                )
            }

            if (mode.equals("ARTICLE", ignoreCase = true) && data.has("article") && !data.isNull("article")) {
                val artObj = data.getJSONObject("article")
                val artTitle = artObj.optString("title", "Artikel: ${topic.trim()}")
                val artDesc = artObj.optString("description", "Artikel materi pembelajaran resmi Kurikulum Merdeka.")
                val artContent = artObj.optString("article_content", artObj.optString("content", ""))

                val blocksList = listOf(
                    GeneratedMaterialBlock(
                        id = UUID.randomUUID().toString(),
                        type = "TEXT",
                        content = artContent
                    )
                )

                return@withContext Result.success(
                    GeneratedMaterialResult(
                        title = artTitle,
                        description = artDesc,
                        mode = "ARTICLE",
                        blocks = blocksList
                    )
                )
            }

            Result.failure(Exception("Format materi pembelajaran AI tidak valid."))
        } catch (e: Exception) {
            Timber.e(e, "Generate material exception")
            Result.failure(e)
        }
    }
}
