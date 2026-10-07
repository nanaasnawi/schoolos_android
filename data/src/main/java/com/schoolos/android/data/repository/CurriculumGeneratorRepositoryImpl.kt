package com.schoolos.android.data.repository

import com.schoolos.android.core.auth.AuthManager
import com.schoolos.android.core.network.ApiClient
import com.schoolos.android.domain.model.GeneratedCurriculumChoice
import com.schoolos.android.domain.model.GeneratedCurriculumQuestion
import com.schoolos.android.domain.model.GeneratedCurriculumResult
import com.schoolos.android.domain.repository.CurriculumGeneratorRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import timber.log.Timber
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
    ): Result<GeneratedCurriculumResult> = withContext(Dispatchers.IO) {
        try {
            val payload = JSONObject().apply {
                put("type", type)
                put("subject_id", subjectId)
                put("subject_name", subjectName)
                put("source_mode", sourceMode)
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = payload.toString().toRequestBody(mediaType)

            // Primary endpoint is production Next.js AI Engine
            val candidateUrls = listOf(
                "https://www.akselerasi-edu.id/api/learning/auto-generate",
                "https://akselerasi-edu.id/api/learning/auto-generate",
            )

            var lastException: Exception? = null

            for (url in candidateUrls) {
                try {
                    val requestBuilder = Request.Builder()
                        .url(url)
                        .post(requestBody)

                    val token = authManager.getAccessToken()
                    if (!token.isNullOrBlank()) {
                        requestBuilder.addHeader("Authorization", "Bearer $token")
                    }

                    val response = apiClient.httpClient.newCall(requestBuilder.build()).execute()
                    val responseBody = response.body?.string() ?: ""

                    if (!response.isSuccessful) {
                        Timber.w("Auto-generate HTTP error from $url: ${response.code} $responseBody")
                        lastException = RuntimeException("Server mengembalikan kode status ${response.code}")
                        continue
                    }

                    val json = JSONObject(responseBody)
                    if (!json.optBoolean("success", false)) {
                        val errMsg = json.optString("error", "Gagal memproses otomatisasi kurikulum.")
                        return@withContext Result.failure(RuntimeException(errMsg))
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
                    lastException = e
                }
            }

            Result.failure(lastException ?: RuntimeException("Gagal menghubungi server generator kurikulum."))
        } catch (e: Exception) {
            Timber.e(e, "Curriculum generation unhandled exception")
            Result.failure(e)
        }
    }
}
