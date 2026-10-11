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
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CurriculumGeneratorRepositoryImpl @Inject constructor(
    private val apiClient: ApiClient,
    private val authManager: AuthManager,
) : CurriculumGeneratorRepository {

    // Dedicated high-capacity client for AI LLM generations (prevents 20s socket timeout)
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

    private fun extractCleanJson(rawText: String): String {
        var clean = rawText.trim()
        if (clean.startsWith("```json")) clean = clean.substring(7)
        else if (clean.startsWith("```")) clean = clean.substring(3)
        if (clean.endsWith("```")) clean = clean.substring(0, clean.length - 3)
        clean = clean.trim()

        val firstBrace = clean.indexOf('{')
        val firstBracket = clean.indexOf('[')
        val first = when {
            firstBrace != -1 && firstBracket != -1 -> minOf(firstBrace, firstBracket)
            firstBrace != -1 -> firstBrace
            else -> firstBracket
        }

        val lastBrace = clean.lastIndexOf('}')
        val lastBracket = clean.lastIndexOf(']')
        val last = maxOf(lastBrace, lastBracket)

        return if (first != -1 && last > first) {
            clean.substring(first, last + 1)
        } else {
            clean
        }
    }

    private fun callDirectNvidiaNim(prompt: String, maxTokens: Int = 2200): String? {
        val nimUrl = "https://integrate.api.nvidia.com/v1/chat/completions"
        val nimKey = "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"
        val candidateModels = listOf(
            "meta/llama-3.2-11b-vision-instruct",
            "nvidia/llama-3.1-nemotron-70b-instruct"
        )
        val mediaType = "application/json; charset=utf-8".toMediaType()

        for (model in candidateModels) {
            try {
                val reqPayload = JSONObject().apply {
                    put("model", model)
                    put("messages", JSONArray().apply {
                        put(JSONObject().apply {
                            put("role", "user")
                            put("content", prompt)
                        })
                    })
                    put("max_tokens", maxTokens)
                    put("temperature", 0.2)
                }
                val request = Request.Builder()
                    .url(nimUrl)
                    .addHeader("Authorization", "Bearer $nimKey")
                    .addHeader("Content-Type", "application/json")
                    .post(reqPayload.toString().toRequestBody(mediaType))
                    .build()

                val response = aiHttpClient.newCall(request).execute()
                val body = response.body?.string() ?: ""
                if (response.isSuccessful && body.isNotBlank()) {
                    val json = JSONObject(body)
                    val choices = json.optJSONArray("choices")
                    if (choices != null && choices.length() > 0) {
                        val choice = choices.getJSONObject(0)
                        val message = choice.optJSONObject("message")
                        val content = message?.optString("content", "") ?: ""
                        if (content.isNotBlank()) {
                            return content
                        }
                    }
                } else {
                    Timber.w("Direct NIM $model HTTP ${response.code}: $body")
                }
            } catch (e: Exception) {
                Timber.w(e, "Direct NIM $model invocation failed")
            }
        }
        return null
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
                    explanation = qObj.optString("explanation", "Disusun otomatis oleh AI NVIDIA NIM"),
                    rubric = if (qObj.isNull("rubric")) null else qObj.optString("rubric"),
                )
            )
        }

        return GeneratedCurriculumResult(
            title = quizObj.optString("title", "Paket Soal CBT: $effectiveTopic"),
            instructions = quizObj.optString("instructions", "Kerjakan seluruh butir soal ujian pilihan ganda berikut dengan teliti."),
            description = quizObj.optString("description", "Paket soal CBT resmi disusun otomatis oleh AI NVIDIA NIM."),
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
        val rubric = assignObj.optString("rubric", "Rubrik Penilaian AI NVIDIA NIM")
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
            instructions = "${assignObj.optString("instructions", "Selesaikan tugas berikut sesuai instruksi guru.")}\n\nRubrik Penilaian Objektif (AI NVIDIA NIM):\n$rubric",
            description = assignObj.optString("description", "Lembar tugas terstruktur disusun otomatis oleh AI NVIDIA NIM."),
            format = type,
            questions = questionsList,
            timeLimitMinutes = assignObj.optInt("time_limit_minutes", 45),
            passingScore = assignObj.optInt("passing_score", 70),
            subjectName = subjectName,
        )
    }

    private fun parseProxyAutoGenerate(
        data: JSONObject,
        type: String,
        subjectName: String
    ): GeneratedCurriculumResult {
        val questionsArray = data.getJSONArray("questions")
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
                    questionText = qObj.optString("question_text", ""),
                    questionType = qObj.optString("question_type", "MULTIPLE_CHOICE"),
                    points = qObj.optInt("points", 10),
                    choices = choicesList,
                    explanation = if (qObj.isNull("explanation") || !qObj.has("explanation")) null else qObj.optString("explanation"),
                    rubric = if (qObj.isNull("rubric") || !qObj.has("rubric")) null else qObj.optString("rubric"),
                )
            )
        }

        return GeneratedCurriculumResult(
            title = data.optString("title", "Tugas Otomatis"),
            instructions = if (data.isNull("instructions") || !data.has("instructions")) null else data.optString("instructions"),
            description = if (data.isNull("description") || !data.has("description")) null else data.optString("description"),
            format = data.optString("assignment_type", data.optString("format", type)),
            questions = questionsList,
            timeLimitMinutes = data.optInt("time_limit_minutes", 30),
            passingScore = data.optInt("passing_score", 70),
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

        // 1. Candidate Primary AI Endpoints (Direct Axum / Web Gateway)
        val candidateAiEndpoints = listOf(
            "$apiV1/ai/generate-content",
            "https://www.akselerasi-edu.id/api/v1/ai/generate-content",
            "https://akselerasi-edu.id/api/v1/ai/generate-content",
        )

        val aiMode = if (isQuiz) "QUIZ" else "ASSIGNMENT"
        val directPayload = JSONObject().apply {
            put("mode", aiMode)
            put("topic", effectiveTopic)
            put("subject_name", subjectName)
            put("grade_level", effectiveGrade)
            put("num_questions", if (isExam) 10 else 5)
            put("difficulty", if (isExam) "HOTS" else "Sedang")
        }

        for (url in candidateAiEndpoints) {
            try {
                val requestBuilder = Request.Builder()
                    .url(url)
                    .post(directPayload.toString().toRequestBody(mediaType))
                if (!token.isNullOrBlank()) {
                    requestBuilder.addHeader("Authorization", "Bearer $token")
                }

                val response = aiHttpClient.newCall(requestBuilder.build()).execute()
                val responseBody = response.body?.string() ?: ""

                if (response.isSuccessful && responseBody.isNotBlank()) {
                    val json = JSONObject(responseBody)
                    if (json.optBoolean("success", false)) {
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
                    }
                }
            } catch (e: Exception) {
                Timber.w(e, "Endpoint attempt failed for $url")
            }
        }

        // 2. Candidate Proxy Routes (/learning/auto-generate)
        val proxyUrls = listOf(
            "$apiV1/learning/auto-generate",
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

                val response = aiHttpClient.newCall(requestBuilder.build()).execute()
                val responseBody = response.body?.string() ?: ""

                if (!response.isSuccessful) {
                    Timber.w("Auto-generate HTTP error from $url: ${response.code}")
                    continue
                }

                val json = JSONObject(responseBody)
                if (!json.optBoolean("success", false)) continue

                val data = json.getJSONObject("data")
                if (data.has("questions")) {
                    return@withContext Result.success(parseProxyAutoGenerate(data, type, subjectName))
                }
            } catch (e: Exception) {
                Timber.w(e, "Proxy attempt failed for $url")
            }
        }

        // 3. Resilient Direct NVIDIA NIM AI Fallback (Guaranteed to work even if server is sleeping)
        try {
            val numQuestions = if (isExam) 10 else 5
            val difficulty = if (isExam) "HOTS" else "Sedang"

            val directNimPrompt = if (isQuiz) {
                """
Anda adalah AI Spesialis Kurikulum Pendidikan Sekolah Indonesia (NVIDIA NIM).
Buatlah paket soal kuis/ujian pilihan ganda CBT berstandar Kurikulum Merdeka dalam format JSON murni.
Topik: $effectiveTopic
Mata Pelajaran: $subjectName
Tingkat Kelas: $effectiveGrade
Jumlah Soal: $numQuestions
Tingkat Kesulitan: $difficulty

Format respon HARUS berupa JSON valid tanpa teks pembuka atau markdown:
{
  "quiz": {
    "title": "Paket Soal CBT: $effectiveTopic",
    "description": "Ujian pilihan ganda materi $effectiveTopic mata pelajaran $subjectName.",
    "questions": [
      {
        "id": "q-1",
        "question_text": "Isi butir soal pertanyaan...",
        "points": ${if (isExam) 10 else 20},
        "explanation": "Penjelasan jawaban benar...",
        "choices": [
          {"choice_text": "Pilihan A", "is_correct": true},
          {"choice_text": "Pilihan B", "is_correct": false},
          {"choice_text": "Pilihan C", "is_correct": false},
          {"choice_text": "Pilihan D", "is_correct": false}
        ]
      }
    ]
  }
}
                """.trimIndent()
            } else {
                """
Anda adalah AI Spesialis Kurikulum Pendidikan Sekolah Indonesia (NVIDIA NIM).
Buatlah lembar penugasan siswa terstruktur berstandar Kurikulum Merdeka dalam format JSON murni.
Topik: $effectiveTopic
Mata Pelajaran: $subjectName
Tingkat Kelas: $effectiveGrade

Format respon HARUS berupa JSON valid tanpa teks pembuka atau markdown:
{
  "assignment": {
    "title": "Tugas Siswa: $effectiveTopic",
    "instructions": "Petunjuk pengerjaan tugas...",
    "tasks": [
      "1. Pertanyaan atau instruksi tugas bagian pertama...",
      "2. Pertanyaan atau instruksi tugas bagian kedua...",
      "3. Pertanyaan analisis atau studi kasus..."
    ],
    "rubric": "Kriteria penilaian: Ketepatan konsep (40%), Kelengkapan analisis (40%), Kerapian (20%)."
  }
}
                """.trimIndent()
            }

            val nimContent = callDirectNvidiaNim(directNimPrompt, maxTokens = if (isExam) 3200 else 2200)
            if (!nimContent.isNullOrBlank()) {
                val cleanJsonStr = extractCleanJson(nimContent)
                val json = JSONObject(cleanJsonStr)

                if (isQuiz) {
                    val quizObj = if (json.has("quiz")) json.getJSONObject("quiz") else json
                    if (quizObj.has("questions")) {
                        return@withContext Result.success(
                            parseQuizFromJson(quizObj, type, effectiveTopic, subjectName, isExam)
                        )
                    }
                } else {
                    val assignObj = if (json.has("assignment")) json.getJSONObject("assignment") else json
                    return@withContext Result.success(
                        parseAssignmentFromJson(assignObj, type, effectiveTopic, subjectName)
                    )
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Direct NVIDIA NIM fallback error")
        }

        Result.failure(Exception("Gagal menghubungi server AI NVIDIA NIM. Pastikan server aktif dan koneksi internet stabil."))
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

        val candidateUrls = listOf(
            "$apiV1/ai/generate-content",
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

                val response = aiHttpClient.newCall(requestBuilder.build()).execute()
                val responseBody = response.body?.string() ?: ""

                if (!response.isSuccessful) {
                    Timber.w("AI material error from $url: ${response.code}")
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

        // Resilient Direct NVIDIA NIM AI Fallback for Materials
        try {
            val encodedPrompt = try {
                URLEncoder.encode("${topic.trim()} educational diagram infographic clean vector", StandardCharsets.UTF_8.toString())
            } catch (_: Exception) {
                "educational"
            }
            val diagramImageUrl = "https://image.pollinations.ai/prompt/$encodedPrompt?width=800&height=500&nologo=true"

            val prompt = if (mode.equals("INFOGRAPHIC", ignoreCase = true)) {
                """
Anda adalah AI Spesialis Modul Infografis Pembelajaran Sekolah Indonesia (NVIDIA NIM).
Buatlah struktur infografis materi ajar interaktif Kurikulum Merdeka dalam format JSON murni.
Topik: ${topic.trim()}
Mata Pelajaran: $subjectName
Tingkat Kelas: $gradeLevel

Format respon HARUS berupa JSON valid tanpa markdown atau teks pengantar:
{
  "infographic": {
    "title": "Infografis: ${topic.trim()}",
    "blocks": [
      {
        "type": "TEXT",
        "content": "Pengantar konsep materi ${topic.trim()} secara ringkas, jelas, dan edukatif."
      },
      {
        "type": "IMAGE",
        "content": "$diagramImageUrl"
      },
      {
        "type": "TEXT",
        "content": "Poin-poin penting, penerapan nyata, dan rangkuman materi ${topic.trim()}."
      }
    ]
  }
}
                """.trimIndent()
            } else {
                """
Anda adalah AI Spesialis Modul Pembelajaran Sekolah Indonesia (NVIDIA NIM).
Susunlah artikel pembelajaran komprehensif berstandar Kurikulum Merdeka dalam format JSON murni.
Topik: ${topic.trim()}
Mata Pelajaran: $subjectName
Tingkat Kelas: $gradeLevel

Format respon HARUS berupa JSON valid tanpa markdown atau teks pengantar:
{
  "article": {
    "title": "Artikel Pembelajaran: ${topic.trim()}",
    "content": "Penjelasan mendalam mengenai ${topic.trim()} mencakup latar belakang, prinsip utama, contoh aplikatif di kehidupan sehari-hari, dan kesimpulan pembelajaran."
  }
}
                """.trimIndent()
            }

            val nimContent = callDirectNvidiaNim(prompt, maxTokens = 2200)
            if (!nimContent.isNullOrBlank()) {
                val cleanJsonStr = extractCleanJson(nimContent)
                val json = JSONObject(cleanJsonStr)

                if (mode.equals("INFOGRAPHIC", ignoreCase = true)) {
                    val infoObj = if (json.has("infographic")) json.getJSONObject("infographic") else json
                    if (infoObj.has("blocks")) {
                        val blocksArr = infoObj.getJSONArray("blocks")
                        val blocksList = ArrayList<GeneratedMaterialBlock>()
                        for (i in 0 until blocksArr.length()) {
                            val bObj = blocksArr.getJSONObject(i)
                            blocksList.add(
                                GeneratedMaterialBlock(
                                    id = bObj.optString("id", UUID.randomUUID().toString()),
                                    type = bObj.optString("type", "TEXT"),
                                    content = bObj.optString("content", "")
                                )
                            )
                        }
                        return@withContext Result.success(
                            GeneratedMaterialResult(
                                title = infoObj.optString("title", "Infografis: ${topic.trim()}"),
                                description = "Modul infografis interaktif Kurikulum Merdeka disusun otomatis oleh AI NVIDIA NIM.",
                                mode = "INFOGRAPHIC",
                                blocks = blocksList
                            )
                        )
                    }
                } else {
                    val artObj = if (json.has("article")) json.getJSONObject("article") else json
                    val artTitle = artObj.optString("title", "Artikel: ${topic.trim()}")
                    val artContent = artObj.optString("content", "")
                    if (artContent.isNotBlank()) {
                        return@withContext Result.success(
                            GeneratedMaterialResult(
                                title = artTitle,
                                description = "Artikel komprehensif kurikulum resmi disusun otomatis oleh AI NVIDIA NIM.",
                                mode = "ARTICLE",
                                blocks = listOf(
                                    GeneratedMaterialBlock(
                                        id = UUID.randomUUID().toString(),
                                        type = "TEXT",
                                        content = artContent
                                    )
                                )
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Direct NVIDIA NIM fallback material generation error")
        }

        Result.failure(Exception("Gagal menyusun materi dengan AI NVIDIA NIM. Pastikan server aktif."))
    }
}
