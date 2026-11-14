package com.schoolos.android.feature.quizzes.dailyquest

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.schoolos.android.core.common.toHumanReadableError
import com.schoolos.android.domain.model.DailyChestResult
import com.schoolos.android.domain.model.DailyQuest
import com.schoolos.android.domain.model.DailyQuestChallenge
import com.schoolos.android.domain.model.DailyQuestGameMode
import com.schoolos.android.domain.model.DailyQuestSubmitResult
import com.schoolos.android.domain.model.ShibaPetStatus
import com.schoolos.android.domain.repository.GamificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SequenceStepItem(
    val origIdx: Int,
    val text: String,
)

data class QuestFeedback(
    val isCorrect: Boolean,
    val message: String,
    val explanation: String? = null,
    val selectedOptionIndex: Int? = null,
    val selectedBoolAnswer: Boolean? = null,
)

data class DailyQuestUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val quest: DailyQuest? = null,
    val selectedMode: DailyQuestGameMode? = null,
    val roundNumber: Int = 0,
    val currentIndex: Int = 0,
    val correctCount: Int = 0,
    val combo: Int = 0,
    val maxCombo: Int = 0,
    val hearts: Int = 3,
    val liveScore: Int = 0,
    val hadStageMistake: Boolean = false,
    val elapsedSeconds: Int = 0,
    val isPlaying: Boolean = true,
    val isSubmitting: Boolean = false,
    val submitResult: DailyQuestSubmitResult? = null,
    val feedback: QuestFeedback? = null,
    val orderedSteps: List<SequenceStepItem> = emptyList(),
    val swapSourceIndex: Int? = null,
    val revealedClues: Int = 1,
    val guessInput: String = "",
    val soundEnabled: Boolean = true,
    val petStatus: ShibaPetStatus? = null,
    val chestClaimedToday: Boolean = false,
    val chestResult: DailyChestResult? = null,
    val petNotice: String? = null,
    val isFeedingPet: Boolean = false,
    val isOpeningChest: Boolean = false,
    val hiddenOptionIndices: Set<Int> = emptySet(),
)

@HiltViewModel
class DailyQuestViewModel @Inject constructor(
    private val gamificationRepository: GamificationRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(DailyQuestUiState())
    val state = _state.asStateFlow()

    private var timerJob: Job? = null
    private var toneGenerator: ToneGenerator? = null

    init {
        loadQuest(null)
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        runCatching { toneGenerator?.release() }
    }

    fun toggleSound() {
        _state.update { it.copy(soundEnabled = !it.soundEnabled) }
    }

    fun loadQuest(modeOverride: DailyQuestGameMode? = null, roundOverride: Int = 0) {
        viewModelScope.launch {
            timerJob?.cancel()
            _state.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null,
                    submitResult = null,
                    feedback = null,
                    selectedMode = modeOverride ?: it.selectedMode,
                    chestResult = null,
                    petNotice = null,
                    hiddenOptionIndices = emptySet(),
                )
            }

            val result = gamificationRepository.getDailyQuest(modeOverride?.code, roundOverride)
            result.onSuccess { quest ->
                val firstSteps = prepareStepsForChallenge(quest, 0)
                _state.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = null,
                        quest = quest,
                        selectedMode = quest.gameMode,
                        roundNumber = quest.roundNumber,
                        currentIndex = 0,
                        correctCount = 0,
                        combo = 0,
                        maxCombo = 0,
                        hearts = 3,
                        liveScore = 0,
                        hadStageMistake = false,
                        elapsedSeconds = 0,
                        isPlaying = true,
                        isSubmitting = false,
                        submitResult = null,
                        feedback = null,
                        orderedSteps = firstSteps,
                        swapSourceIndex = null,
                        revealedClues = 1,
                        guessInput = "",
                        petStatus = quest.petStatus ?: it.petStatus,
                        chestClaimedToday = quest.chestClaimedToday,
                        hiddenOptionIndices = emptySet(),
                    )
                }
                startTimer()
            }.onFailure { err ->
                _state.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = err.toHumanReadableError(),
                    )
                }
            }
        }
    }

    fun loadNextRound() {
        val current = _state.value
        val mode = current.selectedMode ?: current.quest?.gameMode
        playSfx(SfxKind.TAP)
        loadQuest(modeOverride = mode, roundOverride = current.roundNumber + 1)
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive) {
                delay(1_000L)
                val current = _state.value
                if (!current.isLoading && current.isPlaying && !current.isSubmitting && current.submitResult == null && current.quest != null) {
                    _state.update { it.copy(elapsedSeconds = it.elapsedSeconds + 1) }
                }
            }
        }
    }

    fun answerFactOrMyth(chosenIsFact: Boolean) {
        val current = _state.value
        if (current.feedback != null) return
        val challenge = current.quest?.challenges?.getOrNull(current.currentIndex) as? DailyQuestChallenge.FactOrMyth ?: return
        val ok = challenge.isFact == chosenIsFact
        val msg = when {
            ok && chosenIsFact -> "Tepat! Itu memang Fakta."
            ok && !chosenIsFact -> "Tepat! Itu memang Mitos."
            !ok && challenge.isFact -> "Kurang tepat, itu sebenarnya Fakta!"
            else -> "Kurang tepat, itu sebenarnya Mitos!"
        }
        registerAnswer(
            isCorrect = ok,
            message = msg,
            explanation = challenge.explanation,
            selectedBoolAnswer = chosenIsFact,
        )
    }

    fun answerCaseDetective(optionIndex: Int) {
        val current = _state.value
        if (current.feedback != null) return
        val challenge = current.quest?.challenges?.getOrNull(current.currentIndex) as? DailyQuestChallenge.CaseDetective ?: return
        val ok = optionIndex == challenge.correctIndex
        val letter = ('A' + challenge.correctIndex.coerceAtLeast(0))
        val msg = if (ok) {
            "Keputusan tepat! Analisis ceritamu cerdas."
        } else {
            "Kurang tepat. Jawaban benar adalah opsi $letter."
        }
        registerAnswer(
            isCorrect = ok,
            message = msg,
            explanation = challenge.explanation,
            selectedOptionIndex = optionIndex,
        )
    }

    fun onWrongStageTap() {
        playSfx(SfxKind.WRONG)
        _state.update {
            it.copy(
                hadStageMistake = true,
                hearts = (it.hearts - 1).coerceAtLeast(0),
                combo = 0,
            )
        }
    }

    fun completeInteractiveStage(message: String, explanation: String?) {
        val current = _state.value
        if (current.feedback != null) return
        registerAnswer(
            isCorrect = true,
            message = message,
            explanation = explanation,
        )
    }

    fun revealNextClue() {
        val current = _state.value
        if (current.feedback != null) return
        val challenge = current.quest?.challenges?.getOrNull(current.currentIndex) as? DailyQuestChallenge.ConceptGuess ?: return
        playSfx(SfxKind.TAP)
        _state.update {
            it.copy(revealedClues = (it.revealedClues + 1).coerceAtMost(challenge.clues.size.coerceAtLeast(1)))
        }
    }

    fun updateGuessInput(text: String) {
        _state.update { it.copy(guessInput = text) }
    }

    fun submitConceptGuess() {
        val current = _state.value
        if (current.feedback != null) return
        val challenge = current.quest?.challenges?.getOrNull(current.currentIndex) as? DailyQuestChallenge.ConceptGuess ?: return
        val cleaned = current.guessInput.trim().uppercase().replace("\\s+".toRegex(), "")
        if (cleaned.isEmpty()) return

        val validTargets = (listOf(challenge.answer) + challenge.acceptableAliases)
            .map { it.trim().uppercase().replace("\\s+".toRegex(), "") }
        val ok = cleaned in validTargets
        val msg = if (ok) {
            "Benar sekali! Jawabannya adalah ${challenge.answer}."
        } else {
            "Belum tepat. Jawaban yang benar: ${challenge.answer}."
        }
        registerAnswer(
            isCorrect = ok,
            message = msg,
            explanation = challenge.explanation,
        )
    }

    fun skipConceptGuess() {
        val current = _state.value
        if (current.feedback != null) return
        val challenge = current.quest?.challenges?.getOrNull(current.currentIndex) as? DailyQuestChallenge.ConceptGuess ?: return
        registerAnswer(
            isCorrect = false,
            message = "Dilewati. Jawabannya adalah ${challenge.answer}.",
            explanation = challenge.explanation,
        )
    }

    private fun registerAnswer(
        isCorrect: Boolean,
        message: String,
        explanation: String?,
        selectedOptionIndex: Int? = null,
        selectedBoolAnswer: Boolean? = null,
    ) {
        val current = _state.value
        if (current.feedback != null) return

        val cleanSolve = isCorrect && !current.hadStageMistake
        val nextCorrect = if (cleanSolve) current.correctCount + 1 else current.correctCount
        val nextCombo = if (cleanSolve) current.combo + 1 else 0
        val nextMaxCombo = maxOf(current.maxCombo, nextCombo)
        val nextHearts = if (isCorrect) current.hearts else (current.hearts - 1).coerceAtLeast(0)
        val addedScore = if (isCorrect) {
            if (current.hadStageMistake) 15 else (25 + nextCombo * 5)
        } else 0

        playSfx(if (isCorrect) SfxKind.CORRECT else SfxKind.WRONG)

        _state.update {
            it.copy(
                correctCount = nextCorrect,
                combo = nextCombo,
                maxCombo = nextMaxCombo,
                hearts = nextHearts,
                liveScore = it.liveScore + addedScore,
                feedback = QuestFeedback(
                    isCorrect = isCorrect,
                    message = message,
                    explanation = explanation,
                    selectedOptionIndex = selectedOptionIndex,
                    selectedBoolAnswer = selectedBoolAnswer,
                ),
            )
        }
    }

    fun nextStep() {
        val current = _state.value
        val quest = current.quest ?: return
        playSfx(SfxKind.TAP)
        val nextIndex = current.currentIndex + 1
        if (nextIndex >= quest.challenges.size) {
            finishQuest()
        } else {
            val nextSteps = prepareStepsForChallenge(quest, nextIndex)
            _state.update {
                it.copy(
                    currentIndex = nextIndex,
                    feedback = null,
                    hadStageMistake = false,
                    orderedSteps = nextSteps,
                    swapSourceIndex = null,
                    revealedClues = 1,
                    guessInput = "",
                    hiddenOptionIndices = emptySet(),
                )
            }
        }
    }

    fun useFiftyFiftyPowerUp() {
        val current = _state.value
        val pet = current.petStatus ?: return
        if (current.feedback != null || pet.fiftyFiftyTokens <= 0 || current.hiddenOptionIndices.isNotEmpty()) return
        val ch = current.quest?.challenges?.getOrNull(current.currentIndex) as? DailyQuestChallenge.CaseDetective ?: return
        val wrong = ch.options.indices.filter { it != ch.correctIndex }
        if (wrong.size <= 1) return
        playSfx(SfxKind.TAP)
        _state.update {
            it.copy(
                hiddenOptionIndices = wrong.take(2).toSet(),
                petStatus = pet.copy(fiftyFiftyTokens = (pet.fiftyFiftyTokens - 1).coerceAtLeast(0)),
            )
        }
    }

    fun feedShibaPet() {
        if (_state.value.isFeedingPet) return
        _state.update { it.copy(isFeedingPet = true, petNotice = null) }
        viewModelScope.launch {
            gamificationRepository.feedShibaPet().onSuccess { res ->
                playSfx(if (res.evolved) SfxKind.VICTORY else SfxKind.CORRECT)
                _state.update {
                    it.copy(
                        isFeedingPet = false,
                        petStatus = res.petStatus ?: it.petStatus,
                        petNotice = res.message,
                    )
                }
            }.onFailure { err ->
                _state.update {
                    it.copy(
                        isFeedingPet = false,
                        petNotice = err.toHumanReadableError(),
                    )
                }
            }
        }
    }

    fun openDailyChest() {
        val cur = _state.value
        if (cur.isOpeningChest || cur.chestClaimedToday) return
        _state.update { it.copy(isOpeningChest = true) }
        viewModelScope.launch {
            gamificationRepository.openDailyChest().onSuccess { res ->
                playSfx(SfxKind.VICTORY)
                _state.update {
                    it.copy(
                        isOpeningChest = false,
                        chestClaimedToday = true,
                        chestResult = res,
                        petStatus = res.petStatus ?: it.petStatus,
                    )
                }
            }.onFailure {
                _state.update { it.copy(isOpeningChest = false) }
            }
        }
    }

    private fun finishQuest() {
        val current = _state.value
        val quest = current.quest ?: return
        timerJob?.cancel()
        playSfx(SfxKind.VICTORY)

        _state.update { it.copy(isPlaying = false, isSubmitting = true) }

        viewModelScope.launch {
            val total = quest.challenges.size.coerceAtLeast(1)
            val result = gamificationRepository.submitDailyQuest(
                questId = quest.questId,
                correctCount = current.correctCount,
                totalCount = total,
                maxCombo = current.maxCombo,
                durationSeconds = current.elapsedSeconds.coerceAtLeast(3),
                roundNumber = current.roundNumber,
            )

            result.onSuccess { submitRes ->
                _state.update {
                    it.copy(
                        isSubmitting = false,
                        submitResult = submitRes,
                        petStatus = submitRes.petStatus ?: it.petStatus,
                    )
                }
                if (current.roundNumber == 0) {
                    gamificationRepository.getDailyQuest(quest.gameMode.code, 0).onSuccess { refreshed ->
                        _state.update { it.copy(quest = refreshed, petStatus = refreshed.petStatus ?: it.petStatus) }
                    }
                }
            }.onFailure {
                val pct = ((current.correctCount.toFloat() / total) * 100f).toInt().coerceIn(0, 100)
                _state.update {
                    it.copy(
                        isSubmitting = false,
                        submitResult = DailyQuestSubmitResult(
                            awarded = false,
                            alreadyCompleted = false,
                            xpEarned = 0,
                            scorePercent = pct,
                            totalXp = quest.totalXp,
                            level = quest.level,
                            streakDays = quest.streakDays,
                            leveledUp = false,
                            isPreviewAccount = false,
                            message = "Skor selesai dicatat secara lokal. Periksa koneksi untuk sinkronisasi XP.",
                        ),
                    )
                }
            }
        }
    }

    fun replayQuest() {
        val current = _state.value
        val quest = current.quest ?: return
        playSfx(SfxKind.TAP)
        val firstSteps = prepareStepsForChallenge(quest, 0)
        _state.update {
            it.copy(
                currentIndex = 0,
                correctCount = 0,
                combo = 0,
                maxCombo = 0,
                hearts = 3,
                liveScore = 0,
                hadStageMistake = false,
                elapsedSeconds = 0,
                isPlaying = true,
                isSubmitting = false,
                submitResult = null,
                feedback = null,
                orderedSteps = firstSteps,
                swapSourceIndex = null,
                revealedClues = 1,
                guessInput = "",
                hiddenOptionIndices = emptySet(),
            )
        }
        startTimer()
    }

    fun switchToNextMode() {
        val quest = _state.value.quest ?: return
        val modes = quest.availableModes
        if (modes.isEmpty()) return
        val idx = modes.indexOfFirst { it.code == quest.gameMode }.coerceAtLeast(0)
        val next = modes[(idx + 1) % modes.size].code
        playSfx(SfxKind.TAP)
        loadQuest(next, 0)
    }

    private fun prepareStepsForChallenge(quest: DailyQuest, index: Int): List<SequenceStepItem> {
        val ch = quest.challenges.getOrNull(index) as? DailyQuestChallenge.SequenceOrder ?: return emptyList()
        return deterministicShuffleSteps(ch.steps, "${quest.questId}-${ch.id}")
    }

    private fun deterministicShuffleSteps(steps: List<String>, seedStr: String): List<SequenceStepItem> {
        val indexed = steps.mapIndexed { origIdx, text -> SequenceStepItem(origIdx, text) }.toMutableList()
        if (indexed.size <= 1) return indexed

        var hash = 0L
        for (c in seedStr) {
            hash = (hash * 31L + c.code.toLong()) and 0xFFFFFFFFL
        }

        for (i in indexed.size - 1 downTo 1) {
            hash = (hash * 1664525L + 1013904223L) and 0xFFFFFFFFL
            val j = (hash % (i + 1)).toInt()
            val tmp = indexed[i]
            indexed[i] = indexed[j]
            indexed[j] = tmp
        }

        val alreadySorted = indexed.indices.all { idx -> indexed[idx].origIdx == idx }
        if (alreadySorted && indexed.size >= 2) {
            val tmp = indexed[0]
            indexed[0] = indexed[indexed.lastIndex]
            indexed[indexed.lastIndex] = tmp
        }
        return indexed
    }

    private enum class SfxKind { TAP, CORRECT, WRONG, VICTORY }

    private fun playSfx(kind: SfxKind) {
        if (!_state.value.soundEnabled) return
        viewModelScope.launch {
            runCatching {
                val tg = toneGenerator ?: ToneGenerator(AudioManager.STREAM_MUSIC, 65).also {
                    toneGenerator = it
                }
                when (kind) {
                    SfxKind.TAP -> tg.startTone(ToneGenerator.TONE_PROP_BEEP, 45)
                    SfxKind.CORRECT -> tg.startTone(ToneGenerator.TONE_PROP_ACK, 120)
                    SfxKind.WRONG -> tg.startTone(ToneGenerator.TONE_PROP_NACK, 160)
                    SfxKind.VICTORY -> tg.startTone(ToneGenerator.TONE_PROP_BEEP2, 260)
                }
            }
        }
    }
}
