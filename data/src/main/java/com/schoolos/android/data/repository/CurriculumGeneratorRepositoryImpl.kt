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
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CurriculumGeneratorRepositoryImpl @Inject constructor(
    private val apiClient: ApiClient,
    private val authManager: AuthManager,
) : CurriculumGeneratorRepository {

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

        val backendBase = (authManager.getCustomServerUrl() ?: com.schoolos.android.core.common.BuildConfig.API_BASE_URL).trimEnd('/')
        val token = authManager.getAccessToken()
        val mediaType = "application/json; charset=utf-8".toMediaType()

        // 1. Direct call to Axum Backend (Strict NVIDIA NIM AI endpoint)
        try {
            val directAiUrl = "$backendBase/api/v1/ai/generate-content"
            val aiMode = if (isQuiz) "QUIZ" else "ASSIGNMENT"
            val directPayload = JSONObject().apply {
                put("mode", aiMode)
                put("topic", effectiveTopic)
                put("subject_name", subjectName)
                put("grade_level", effectiveGrade)
                put("num_questions", if (isExam) 10 else 5)
                put("difficulty", if (isExam) "HOTS" else "Sedang")
            }

            val requestBuilder = Request.Builder()
                .url(directAiUrl)
                .post(directPayload.toString().toRequestBody(mediaType))
            if (!token.isNullOrBlank()) {
                requestBuilder.addHeader("Authorization", "Bearer $token")
            }

            val response = apiClient.httpClient.newCall(requestBuilder.build()).execute()
            val responseBody = response.body?.string() ?: ""

            if (response.isSuccessful && responseBody.isNotBlank()) {
                val json = JSONObject(responseBody)
                if (json.optBoolean("success", false)) {
                    val data = json.getJSONObject("data")

                    // Handle Direct AI Quiz Response
                    if (data.has("quiz") && !data.isNull("quiz")) {
                        val quizObj = data.getJSONObject("quiz")
                        val questionsArray = quizObj.getJSONArray("questions")
                        val questionsList = ArrayList<GeneratedCurriculumQuestion>()

                        for (i in 0 until questionsArray.length()) {
                            val qObj = questionsArray.getJSONObject(i)
                            val choicesArray = qObj.optJSONArray("choices")
                            val choicesList = ArrayList<GeneratedCurriculumChoice>()
                            if (choicesArray != null) {
                                for (j in 0 until choicesArray.length()) {
                                    val cObj = choicesArray.getJSONObject(j)
                                    choicesList.add(
                                        GeneratedCurriculumChoice(
                                            choiceText = cObj.optString("choice_text", ""),
                                            isCorrect = cObj.optBoolean("is_correct", false),
                                        )
                                    )
                                }
                            }

                            questionsList.add(
                                GeneratedCurriculumQuestion(
                                    id = qObj.optString("id", "q-$i"),
                                    questionText = qObj.optString("question_text", ""),
                                    questionType = "MULTIPLE_CHOICE",
                                    points = qObj.optInt("points", 20),
                                    choices = choicesList,
                                    explanation = qObj.optString("explanation", "Disusun oleh AI NVIDIA NIM"),
                                    rubric = null,
                                )
                            )
                        }

                        val result = GeneratedCurriculumResult(
                            title = quizObj.optString("title", "Paket Soal CBT: $effectiveTopic"),
                            instructions = "Kerjakan seluruh butir soal ujian pilihan ganda berikut dengan teliti.",
                            description = quizObj.optString("description", "Paket soal CBT resmi disusun otomatis oleh AI NVIDIA NIM."),
                            format = type,
                            questions = questionsList,
                            timeLimitMinutes = if (isExam) 90 else 45,
                            passingScore = 75,
                            subjectName = subjectName,
                        )
                        return@withContext Result.success(result)
                    }

                    // Handle Direct AI Assignment Response
                    if (data.has("assignment") && !data.isNull("assignment")) {
                        val assignObj = data.getJSONObject("assignment")
                        val tasksArray = assignObj.optJSONArray("tasks")
                        val rubric = assignObj.optString("rubric", "Rubrik Penilaian AI NVIDIA NIM")
                        val questionsList = ArrayList<GeneratedCurriculumQuestion>()

                        if (tasksArray != null) {
                            val count = maxOf(1, tasksArray.length())
                            for (i in 0 until tasksArray.length()) {
                                questionsList.add(
                                    GeneratedCurriculumQuestion(
                                        id = "task-${i + 1}",
                                        questionText = tasksArray.getString(i),
                                        questionType = "ESSAY",
                                        points = 100 / count,
                                        choices = emptyList(),
                                        explanation = rubric,
                                        rubric = rubric,
                                    )
                                )
                            }
                        }

                        val result = GeneratedCurriculumResult(
                            title = assignObj.optString("title", "Tugas Siswa: $effectiveTopic"),
                            instructions = "${assignObj.optString("instructions", "")}\n\nRubrik Penilaian Objektif (AI NVIDIA NIM):\n$rubric",
                            description = "Lembar tugas terstruktur disusun otomatis oleh AI NVIDIA NIM.",
                            format = type,
                            questions = questionsList,
                            timeLimitMinutes = 45,
                            passingScore = 70,
                            subjectName = subjectName,
                        )
                        return@withContext Result.success(result)
                    }
                }
            }
        } catch (e: Exception) {
            Timber.w(e, "Direct Axum AI endpoint call failed, trying proxy route")
        }

        // 2. Fallback to Web API Route (/api/v1/learning/auto-generate)
        val proxyUrls = listOf(
            "$backendBase/api/v1/learning/auto-generate",
            "$backendBase/api/learning/auto-generate",
            "https://www.akselerasi-edu.id/api/v1/learning/auto-generate",
            "https://akselerasi-edu.id/api/v1/learning/auto-generate",
        )

        val proxyPayload = JSONObject().apply {
            put("type", type)
            put("subject_id", subjectId)
            put("subject_name", subjectName)
            put("topic", effectiveTopic)
            put("grade_level", effectiveGrade)
            put("source_mode", sourceMode)
        }
        val requestBody = proxyPayload.toString().toRequestBody(mediaType)

        for (url in proxyUrls) {
            try {
                val requestBuilder = Request.Builder()
                    .url(url)
                    .post(requestBody)

                if (!token.isNullOrBlank()) {
                    requestBuilder.addHeader("Authorization", "Bearer $token")
                }

                val response = apiClient.httpClient.newCall(requestBuilder.build()).execute()
                val responseBody = response.body?.string() ?: ""

                if (!response.isSuccessful) {
                    Timber.w("Auto-generate HTTP error from $url: ${response.code} $responseBody")
                    continue
                }

                val json = JSONObject(responseBody)
                if (!json.optBoolean("success", false)) {
                    continue
                }

                val data = json.getJSONObject("data")
                val questionsArray = data.getJSONArray("questions")
                val questionsList = ArrayList<GeneratedCurriculumQuestion>()

                for (i in 0 until questionsArray.length()) {
                    val qObj = questionsArray.getJSONObject(i)
                    val choicesArray = qObj.optJSONArray("choices")
                    val choicesList = ArrayList<GeneratedCurriculumChoice>()

                    if (choicesArray != null) {
                        for (j in 0 until choicesArray.length()) {
                            val cObj = choicesArray.getJSONObject(j)
                            choicesList.add(
                                GeneratedCurriculumChoice(
                                    choiceText = cObj.optString("choice_text", cObj.optString("text", "")),
                                    isCorrect = cObj.optBoolean("is_correct", cObj.optBoolean("isCorrect", false)),
                                )
                            )
                        }
                    }

                    questionsList.add(
                        GeneratedCurriculumQuestion(
                            id = qObj.optString("id", "q-$i"),
                            questionText = qObj.optString("question_text", ""),
                            questionType = qObj.optString("question_type", "MULTIPLE_CHOICE"),
                            points = qObj.optInt("points", 10),
                            choices = choicesList,
                            explanation = qObj.optString("explanation", null),
                            rubric = qObj.optString("rubric", null),
                        )
                    )
                }

                val result = GeneratedCurriculumResult(
                    title = data.optString("title", "Tugas Otomatis"),
                    instructions = data.optString("instructions", null),
                    description = data.optString("description", null),
                    format = data.optString("assignment_type", data.optString("format", type)),
                    questions = questionsList,
                    timeLimitMinutes = data.optInt("time_limit_minutes", 30),
                    passingScore = data.optInt("passing_score", 70),
                    subjectName = subjectName,
                )

                return@withContext Result.success(result)
            } catch (e: Exception) {
                Timber.w(e, "Attempt failed for $url")
            }
        }

        // NO LOCAL SYNTHESIS / DUMMY MOCK ENGINE: Strict NVIDIA NIM only
        Result.failure(Exception("Gagal menghubungi server AI NVIDIA NIM. Pastikan server aktif dan koneksi internet stabil."))
    }

    override suspend fun generateMaterial(
        mode: String,
        topic: String,
        gradeLevel: String,
        subjectName: String,
    ): Result<GeneratedMaterialResult> = withContext(Dispatchers.IO) {
        val backendBase = (authManager.getCustomServerUrl() ?: com.schoolos.android.core.common.BuildConfig.API_BASE_URL).trimEnd('/')
        val token = authManager.getAccessToken()
        val mediaType = "application/json; charset=utf-8".toMediaType()

        val candidateUrls = listOf(
            "$backendBase/api/v1/ai/generate-content",
            "https://www.akselerasi-edu.id/api/v1/ai/generate-content",
            "https://akselerasi-edu.id/api/v1/ai/generate-content",
        )

        val payload = JSONObject().apply {
            put("mode", mode.uppercase())
            put("topic", topic.trim())
            put("grade_level", gradeLevel)
            put("subject_name", subjectName)
        }

        for (url in candidateUrls) {
            try {
                val requestBuilder = Request.Builder()
                    .url(url)
                    .post(payload.toString().toRequestBody(mediaType))

                if (!token.isNullOrBlank()) {
                    requestBuilder.addHeader("Authorization", "Bearer $token")
                }

                val response = apiClient.httpClient.newCall(requestBuilder.build()).execute()
                val responseBody = response.body?.string() ?: ""

                if (!response.isSuccessful) {
                    Timber.w("AI material error from $url: ${response.code} $responseBody")
                    continue
                }

                val json = JSONObject(responseBody)
                if (!json.optBoolean("success", false)) continue

                val data = json.getJSONObject("data")

                if (mode.equals("INFOGRAPHIC", ignoreCase = true) && data.has("infographic")) {
                    val infoObj = data.getJSONObject("infographic")
                    val blocksArr = infoObj.getJSONArray("blocks")
                    val blocksList = ArrayList<GeneratedMaterialBlock>()

                    for (i in 0 until blocksArr.length()) {
                        val bObj = blocksArr.getJSONObject(i)
                        val bType = bObj.optString("type", bObj.optString("block_type", "TEXT"))
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
                            title = "Infografis: ${topic.trim()}",
                            description = "Modul infografis interaktif Kurikulum Merdeka disusun otomatis oleh AI NVIDIA NIM.",
                            mode = "INFOGRAPHIC",
                            blocks = blocksList
                        )
                    )
                }

                if (mode.equals("ARTICLE", ignoreCase = true) && data.has("article")) {
                    val artObj = data.getJSONObject("article")
                    val artTitle = artObj.optString("title", "Artikel: ${topic.trim()}")
                    val artContent = artObj.optString("content", "")

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
                            description = "Artikel komprehensif kurikulum resmi disusun otomatis oleh AI NVIDIA NIM.",
                            mode = "ARTICLE",
                            blocks = blocksList
                        )
                    )
                }
            } catch (e: Exception) {
                Timber.w(e, "Generate material failed for $url")
            }
        }

        Result.failure(Exception("Gagal menyusun materi dengan AI NVIDIA NIM. Pastikan server aktif."))
    }
}
