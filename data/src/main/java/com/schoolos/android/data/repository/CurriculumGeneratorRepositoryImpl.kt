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

    private fun cleanChoiceText(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.length >= 2) {
            val first = trimmed[0]
            if (first.isLetter()) {
                val punct = trimmed[1]
                if (punct == '.' || punct == ')' || punct == ':' || punct == '-') {
                    return trimmed.substring(2).trimStart()
                }
            }
        }
        return trimmed
    }

    private fun cleanQuestionStem(raw: String): String {
        val lines = raw.lines()
        if (lines.size <= 1) return raw.trim()

        var cutIdx = lines.size
        for (i in lines.indices.reversed()) {
            val line = lines[i].trim()
            if (line.isEmpty()) continue
            val isChoiceLine = if (line.length >= 2) {
                val first = line[0]
                if (first.isLetter()) {
                    val punct = line[1]
                    punct == '.' || punct == ')' || punct == ':' || punct == '-'
                } else false
            } else false

            if (isChoiceLine) {
                cutIdx = i
            } else {
                break
            }
        }

        return if (cutIdx in 1 until lines.size) {
            lines.subList(0, cutIdx).joinToString("\n").trim()
        } else {
            raw.trim()
        }
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
                                choiceText = cleanChoiceText(choiceItem.optString("choice_text", choiceItem.optString("text", ""))),
                                isCorrect = choiceItem.optBoolean("is_correct", choiceItem.optBoolean("isCorrect", false)),
                            )
                        )
                    } else if (choiceItem is String) {
                        choicesList.add(
                            GeneratedCurriculumChoice(
                                choiceText = cleanChoiceText(choiceItem),
                                isCorrect = (j == 0)
                            )
                        )
                    }
                }
            }

            val explicitType = qObj.optString("question_type", "")
            val qType = if (explicitType.isNotBlank()) {
                explicitType.uppercase()
            } else if (choicesList.isNotEmpty()) {
                "MULTIPLE_CHOICE"
            } else {
                "ESSAY"
            }

            val rawQText = qObj.optString("question_text", qObj.optString("text", ""))
            val cleanQText = if (qType == "MULTIPLE_CHOICE") cleanQuestionStem(rawQText) else rawQText.trim()

            questionsList.add(
                GeneratedCurriculumQuestion(
                    id = qObj.optString("id", "q-${i + 1}"),
                    questionText = cleanQText,
                    questionType = qType,
                    points = qObj.optInt("points", if (qType == "ESSAY") 20 else (if (isExam) 10 else 10)),
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
        val questionsArray = assignObj.optJSONArray("questions")
        val tasksArray = assignObj.optJSONArray("tasks")
        val rubric = assignObj.optString("rubric", "Rubrik Penilaian Objektif: Kriteria ketepatan konsep dan kelengkapan analisis (Skor 0 - 100)")
        val questionsList = ArrayList<GeneratedCurriculumQuestion>()

        if (questionsArray != null && questionsArray.length() > 0) {
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
                                    choiceText = cleanChoiceText(choiceItem.optString("choice_text", choiceItem.optString("text", ""))),
                                    isCorrect = choiceItem.optBoolean("is_correct", choiceItem.optBoolean("isCorrect", false)),
                                )
                            )
                        } else if (choiceItem is String) {
                            choicesList.add(
                                GeneratedCurriculumChoice(
                                    choiceText = cleanChoiceText(choiceItem),
                                    isCorrect = (j == 0)
                                )
                            )
                        }
                    }
                }

                val explicitType = qObj.optString("question_type", "")
                val qType = if (explicitType.isNotBlank()) {
                    explicitType.uppercase()
                } else if (choicesList.isNotEmpty()) {
                    "MULTIPLE_CHOICE"
                } else {
                    "ESSAY"
                }

                val defaultPoints = if (qType == "MULTIPLE_CHOICE") 10 else 20
                val rawQText = qObj.optString("question_text", qObj.optString("text", ""))
                val cleanQText = if (qType == "MULTIPLE_CHOICE") cleanQuestionStem(rawQText) else rawQText.trim()

                questionsList.add(
                    GeneratedCurriculumQuestion(
                        id = qObj.optString("id", "asg-q-${i + 1}"),
                        questionText = cleanQText,
                        questionType = qType,
                        points = qObj.optInt("points", defaultPoints),
                        choices = choicesList,
                        explanation = qObj.optString("explanation", rubric),
                        rubric = if (qObj.isNull("rubric")) rubric else qObj.optString("rubric", rubric),
                    )
                )
            }
        } else if (tasksArray != null && tasksArray.length() > 0) {
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
        numQuestions: Int?,
        numMcq: Int?,
        numEssay: Int?,
        difficulty: String?,
        includeImages: Boolean?,
    ): Result<GeneratedCurriculumResult> = withContext(Dispatchers.IO) {
        val effectiveTopic = topic?.trim()?.ifBlank { null } ?: subjectName
        val effectiveGrade = gradeLevel ?: "Kelas 5 SD"
        val isQuiz = type.startsWith("QUIZ") || type == "EXAM_MONTHLY"
        val isExam = type == "EXAM_MONTHLY"
        val isHomework = type == "ASSIGNMENT_HOMEWORK"
        val isQuizMixed = type == "QUIZ_MCQ_ESSAY" || (isQuiz && (numEssay ?: 0) > 0)

        val effectiveDifficulty = difficulty ?: if (isExam) "HOTS" else "Sedang"
        val effectiveNumMcq = numMcq ?: if (isHomework) 0 else if (isQuizMixed) 10 else 5
        val effectiveNumEssay = numEssay ?: if (isQuizMixed) 5 else (if (isHomework) 5 else 2)
        val effectiveNumQuestions = numQuestions ?: if (isQuiz) {
            effectiveNumMcq + (if (isQuizMixed) effectiveNumEssay else 0)
        } else if (isHomework) {
            effectiveNumEssay
        } else {
            effectiveNumMcq + effectiveNumEssay
        }

        val apiV1 = getApiV1Base()
        val token = authManager.getAccessToken()
        val mediaType = "application/json; charset=utf-8".toMediaType()

        val aiMode = if (isQuiz) "QUIZ" else "ASSIGNMENT"
        val payload = JSONObject().apply {
            put("mode", aiMode)
            put("topic", effectiveTopic)
            put("subject_name", subjectName)
            put("grade_level", effectiveGrade)
            put("difficulty", effectiveDifficulty)
            put("num_questions", effectiveNumQuestions)
            put("num_mcq", effectiveNumMcq)
            put("num_essay", effectiveNumEssay)
            put("quiz_format", if (isQuizMixed) "MIXED" else "MCQ_ONLY")
            put("assignment_format", if (isHomework) "HOMEWORK_PR" else "STRUCTURED_QUESTIONS")
            if (includeImages != null) {
                put("include_images", includeImages)
            }
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
