package com.schoolos.android.feature.quizzes

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.schoolos.android.core.common.toHumanReadableError
import com.schoolos.android.core.designsystem.XpRewardData
import com.schoolos.android.domain.model.Quiz
import com.schoolos.android.domain.model.QuizAttempt
import com.schoolos.android.domain.model.QuizQuestion
import com.schoolos.android.domain.repository.AnswerInput
import com.schoolos.android.domain.repository.GamificationRepository
import com.schoolos.android.domain.repository.QuizRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject

private data class QuizDraftSnapshot(
    val answers: Map<String, String> = emptyMap(),
    val flaggedIds: List<String> = emptyList(),
)

data class QuizAttemptUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val quiz: Quiz? = null,
    val questions: List<QuizQuestion> = emptyList(),
    val currentIndex: Int = 0,
    val answers: Map<String, String?> = emptyMap(),
    val flaggedIds: Set<String> = emptySet(),
    val remainingSeconds: Long? = null,
    val showFiveMinuteAlert: Boolean = false,
    val isSubmitting: Boolean = false,
    val submitSuccess: Boolean = false,
    val submitError: String? = null,
    val resultAttempt: QuizAttempt? = null,
) {
    val currentQuestion: QuizQuestion? get() = questions.getOrNull(currentIndex)
    val progress: Float get() = if (questions.isEmpty()) 0f else (currentIndex + 1).toFloat() / questions.size
    val isLastQuestion: Boolean get() = currentIndex == questions.lastIndex
    val answeredCount: Int get() = questions.count { q -> isAndroidQuestionAnswerComplete(q, answers[q.id]) }
}

@HiltViewModel
class QuizAttemptViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val appContext: Context,
    private val repository: QuizRepository,
    private val gamificationRepository: GamificationRepository,
) : ViewModel() {

    private val quizId: String = savedStateHandle["quizId"] ?: ""
    private val attemptId: String = savedStateHandle["attemptId"] ?: ""
    private val prefs by lazy { appContext.getSharedPreferences("cbt_attempt_drafts", Context.MODE_PRIVATE) }

    private val _state = MutableStateFlow(QuizAttemptUiState())
    val state = _state.asStateFlow()
    val xpRewardEvent = MutableStateFlow<XpRewardData?>(null)

    private var timerJob: Job? = null
    private var warnedFiveMin = false

    fun clearXpReward() {
        xpRewardEvent.value = null
    }

    fun dismissFiveMinuteAlert() {
        _state.value = _state.value.copy(showFiveMinuteAlert = false)
    }

    init {
        loadExamData()
    }

    private fun loadExamData() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            val (quizRes, questionsRes, attemptsRes) = coroutineScope {
                val qDef = async { repository.getQuiz(quizId) }
                val qsDef = async { repository.getQuestions(quizId) }
                val atDef = async { repository.getQuizAttempts(quizId) }
                Triple(qDef.await(), qsDef.await(), atDef.await())
            }

            val quiz = quizRes.getOrNull()
            val currentAttempt = attemptsRes.getOrNull()?.firstOrNull { it.id == attemptId }

            questionsRes
                .onSuccess { rawQuestions ->
                    val sorted = rawQuestions.sortedBy { it.orderIndex }
                    val draft = loadSavedDraft()
                    val mergedAnswers = sorted.associate { q -> q.id to draft?.answers?.get(q.id) }
                    val mergedFlagged = draft?.flaggedIds?.toSet().orEmpty()

                    val initialRemaining = computeAuthoritativeRemainingSeconds(quiz, currentAttempt)

                    _state.value = _state.value.copy(
                        isLoading = false,
                        quiz = quiz,
                        questions = sorted,
                        answers = mergedAnswers,
                        flaggedIds = mergedFlagged,
                        remainingSeconds = initialRemaining,
                    )

                    if (initialRemaining != null) {
                        startCountdownTimer(initialRemaining)
                    }
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = e.toHumanReadableError("Gagal memuat butir soal ujian CBT. Periksa koneksi internet Anda."),
                    )
                }
        }
    }

    private fun computeAuthoritativeRemainingSeconds(quiz: Quiz?, attempt: QuizAttempt?): Long? {
        val nowMs = System.currentTimeMillis()
        val endWindowSecs = quiz?.endAt?.let { endIso ->
            parseQuizInstant(endIso)?.let { ((it.toEpochMilli() - nowMs) / 1000L).coerceAtLeast(0L) }
        }
        val durationSecs = quiz?.timeLimitMinutes?.takeIf { it > 0 }?.let { mins ->
            val startedMs = parseQuizInstant(attempt?.startedAt)?.toEpochMilli() ?: nowMs
            val deadlineMs = startedMs + mins * 60_000L
            ((deadlineMs - nowMs) / 1000L).coerceAtLeast(0L)
        }
        return listOfNotNull(endWindowSecs, durationSecs).minOrNull()
    }

    private fun startCountdownTimer(startSeconds: Long) {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            var remaining = startSeconds
            while (remaining > 0L && !_state.value.submitSuccess) {
                delay(1000L)
                remaining = (remaining - 1L).coerceAtLeast(0L)
                val shouldWarn = !warnedFiveMin && remaining in 1L..300L
                if (shouldWarn) warnedFiveMin = true
                _state.value = _state.value.copy(
                    remainingSeconds = remaining,
                    showFiveMinuteAlert = if (shouldWarn) true else _state.value.showFiveMinuteAlert,
                )
            }
            if (remaining <= 0L && !_state.value.submitSuccess && !_state.value.isSubmitting) {
                submit()
            }
        }
    }

    private fun persistDraft() {
        if (attemptId.isBlank()) return
        try {
            val answersObj = JSONObject()
            _state.value.answers.forEach { (k, v) ->
                if (!v.isNullOrBlank()) answersObj.put(k, v)
            }
            val flaggedArr = JSONArray()
            _state.value.flaggedIds.forEach { flaggedArr.put(it) }
            val root = JSONObject().apply {
                put("answers", answersObj)
                put("flaggedIds", flaggedArr)
            }
            prefs.edit().putString("draft_$attemptId", root.toString()).apply()
        } catch (_: Exception) {}
    }

    private fun loadSavedDraft(): QuizDraftSnapshot? {
        if (attemptId.isBlank()) return null
        return try {
            val raw = prefs.getString("draft_$attemptId", null) ?: return null
            val root = JSONObject(raw)
            val ansObj = root.optJSONObject("answers")
            val ansMap = mutableMapOf<String, String>()
            if (ansObj != null) {
                val keys = ansObj.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val valStr = ansObj.optString(key, "")
                    if (valStr.isNotEmpty()) ansMap[key] = valStr
                }
            }
            val flaggedArr = root.optJSONArray("flaggedIds")
            val flaggedList = mutableListOf<String>()
            if (flaggedArr != null) {
                for (i in 0 until flaggedArr.length()) {
                    val item = flaggedArr.optString(i, "")
                    if (item.isNotBlank()) flaggedList.add(item)
                }
            }
            QuizDraftSnapshot(answers = ansMap, flaggedIds = flaggedList)
        } catch (_: Exception) {
            null
        }
    }

    private fun clearSavedDraft() {
        if (attemptId.isBlank()) return
        try { prefs.edit().remove("draft_$attemptId").apply() } catch (_: Exception) {}
    }

    fun setAnswer(questionId: String, value: String?) {
        val updated = _state.value.answers.toMutableMap()
        updated[questionId] = value
        _state.value = _state.value.copy(answers = updated)
        persistDraft()
    }

    fun selectAnswer(questionId: String, choiceId: String) = setAnswer(questionId, choiceId)

    fun setEssayAnswer(questionId: String, text: String) = setAnswer(questionId, text.ifBlank { null })

    fun toggleFlagQuestion(questionId: String) {
        val next = _state.value.flaggedIds.toMutableSet()
        if (!next.add(questionId)) next.remove(questionId)
        _state.value = _state.value.copy(flaggedIds = next)
        persistDraft()
    }

    fun nextQuestion() {
        val idx = _state.value.currentIndex
        if (idx < _state.value.questions.lastIndex) {
            _state.value = _state.value.copy(currentIndex = idx + 1)
        }
    }

    fun previousQuestion() {
        val idx = _state.value.currentIndex
        if (idx > 0) {
            _state.value = _state.value.copy(currentIndex = idx - 1)
        }
    }

    fun goToQuestion(index: Int) {
        if (index in _state.value.questions.indices) {
            _state.value = _state.value.copy(currentIndex = index)
        }
    }

    fun submit() {
        if (_state.value.isSubmitting || _state.value.submitSuccess) return
        viewModelScope.launch {
            _state.value = _state.value.copy(isSubmitting = true, submitError = null)
            val questionsMap = _state.value.questions.associateBy { it.id }
            val answerInputs = _state.value.answers.mapNotNull { (qId, value) ->
                if (value.isNullOrBlank()) return@mapNotNull null
                val question = questionsMap[qId]
                val variatifType = normalizeAndroidQuestionType(question?.questionType)

                if (variatifType == AndroidVariatifQuestionType.MULTIPLE_CHOICE) {
                    val choiceId = if (value.startsWith("choice_")) value.removePrefix("choice_") else value
                    AnswerInput(questionId = qId, chosenChoiceId = choiceId, textAnswer = null)
                } else {
                    AnswerInput(questionId = qId, chosenChoiceId = null, textAnswer = value)
                }
            }
            repository.submitAttempt(quizId, attemptId, answerInputs)
                .onSuccess { attempt ->
                    timerJob?.cancel()
                    clearSavedDraft()
                    _state.value = _state.value.copy(isSubmitting = false, submitSuccess = true, resultAttempt = attempt)

                    viewModelScope.launch {
                        gamificationRepository.awardXp(
                            actionType = "COMPLETE_QUIZ",
                            referenceId = attempt.id.ifBlank { attemptId },
                            description = "Menyelesaikan evaluasi kuis CBT",
                        ).onSuccess { res ->
                            if (res.awarded || res.alreadyAwarded) {
                                xpRewardEvent.value = XpRewardData(
                                    xpEarned = res.xpEarned,
                                    newTotalXp = res.totalXp.toInt(),
                                    level = res.level,
                                    title = if (res.alreadyAwarded) "Evaluasi Telah Diselesaikan" else "Ujian / Kuis Selesai!",
                                    description = if (res.alreadyAwarded) "Anda telah menyelesaikan evaluasi ini sebelumnya." else "Luar biasa! Menyelesaikan evaluasi CBT memberi Anda +${res.xpEarned} XP.",
                                    actionType = "COMPLETE_QUIZ",
                                )
                            }
                        }
                    }
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(
                        isSubmitting = false,
                        submitError = e.toHumanReadableError("Gagal mengirimkan lembar jawaban ujian CBT. Periksa koneksi internet Anda."),
                    )
                }
        }
    }

    fun clearSubmitError() {
        _state.value = _state.value.copy(submitError = null)
    }

    fun sendProctorEvent(eventType: String, detail: String? = null) {
        if (quizId.isBlank() || attemptId.isBlank() || _state.value.submitSuccess) return
        viewModelScope.launch {
            repository.recordProctorEvent(quizId = quizId, attemptId = attemptId, eventType = eventType, detail = detail)
        }
    }

    override fun onCleared() {
        timerJob?.cancel()
        super.onCleared()
    }
}
