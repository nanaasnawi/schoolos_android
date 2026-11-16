package com.schoolos.android.feature.quizzes.dailyquest

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.schoolos.android.core.common.toHumanReadableError
import com.schoolos.android.domain.model.DailyQuest
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

data class DailyQuestUiState(
    val isLoadingHub: Boolean = true,
    val isLoadingGame: Boolean = false,
    val errorMessage: String? = null,
    val hubQuest: DailyQuest? = null,
    val activeGame: DailyQuestGameMode? = null, // null = show 4-Game Selection Menu Hub first!
    val activeQuest: DailyQuest? = null,
    val roundNumber: Int = 0,
    val elapsedSeconds: Int = 0,
    val isSubmitting: Boolean = false,
    val submitResult: DailyQuestSubmitResult? = null,
    val soundEnabled: Boolean = true,
    val petStatus: ShibaPetStatus? = null,
    val chestClaimedToday: Boolean = false,
    val petNotice: String? = null,
    val isFeedingPet: Boolean = false,
    val isOpeningChest: Boolean = false,
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
        loadHubSummary()
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        runCatching { toneGenerator?.release() }
    }

    fun toggleSound() {
        _state.update { it.copy(soundEnabled = !it.soundEnabled) }
    }

    fun loadHubSummary() {
        viewModelScope.launch {
            _state.update { it.copy(isLoadingHub = true, errorMessage = null) }
            gamificationRepository.getDailyQuest(mode = null, round = 0)
                .onSuccess { quest ->
                    _state.update {
                        it.copy(
                            isLoadingHub = false,
                            errorMessage = null,
                            hubQuest = quest,
                            petStatus = quest.petStatus ?: it.petStatus,
                            chestClaimedToday = quest.chestClaimedToday,
                        )
                    }
                }
                .onFailure { err ->
                    _state.update {
                        it.copy(
                            isLoadingHub = false,
                            errorMessage = err.toHumanReadableError(),
                        )
                    }
                }
        }
    }

    fun openGame(mode: DailyQuestGameMode, round: Int = 0) {
        viewModelScope.launch {
            timerJob?.cancel()
            playSfx(SfxKind.TAP)
            _state.update {
                it.copy(
                    activeGame = mode,
                    roundNumber = round,
                    isLoadingGame = true,
                    errorMessage = null,
                    submitResult = null,
                    elapsedSeconds = 0,
                )
            }
            gamificationRepository.getDailyQuest(mode = mode.code, round = round)
                .onSuccess { quest ->
                    _state.update {
                        it.copy(
                            isLoadingGame = false,
                            errorMessage = null,
                            activeQuest = quest,
                            roundNumber = quest.roundNumber,
                            elapsedSeconds = 0,
                            submitResult = null,
                            petStatus = quest.petStatus ?: it.petStatus,
                            chestClaimedToday = quest.chestClaimedToday,
                        )
                    }
                    startTimer()
                }
                .onFailure { err ->
                    _state.update {
                        it.copy(
                            isLoadingGame = false,
                            errorMessage = err.toHumanReadableError(),
                        )
                    }
                }
        }
    }

    fun loadNextLevel() {
        val mode = _state.value.activeGame ?: return
        openGame(mode, _state.value.roundNumber + 1)
    }

    fun backToMenu() {
        timerJob?.cancel()
        playSfx(SfxKind.TAP)
        _state.update {
            it.copy(
                activeGame = null,
                activeQuest = null,
                submitResult = null,
                errorMessage = null,
                elapsedSeconds = 0,
            )
        }
        loadHubSummary()
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive) {
                delay(1_000L)
                val current = _state.value
                if (current.activeGame != null && !current.isLoadingGame && !current.isSubmitting && current.submitResult == null) {
                    _state.update { it.copy(elapsedSeconds = it.elapsedSeconds + 1) }
                }
            }
        }
    }

    fun submitGameScore(
        correctCount: Int,
        totalCount: Int,
        maxCombo: Int,
        durationSeconds: Int? = null,
    ) {
        val current = _state.value
        val quest = current.activeQuest ?: return
        if (current.isSubmitting || current.submitResult != null) return

        timerJob?.cancel()
        _state.update { it.copy(isSubmitting = true) }

        val duration = (durationSeconds ?: current.elapsedSeconds).coerceAtLeast(1)

        viewModelScope.launch {
            gamificationRepository.submitDailyQuest(
                questId = quest.questId,
                correctCount = correctCount,
                totalCount = totalCount.coerceAtLeast(1),
                maxCombo = maxCombo.coerceAtLeast(1),
                durationSeconds = duration,
                roundNumber = quest.roundNumber,
            ).onSuccess { res ->
                playSfx(SfxKind.WIN)
                _state.update {
                    it.copy(
                        isSubmitting = false,
                        submitResult = res,
                        petStatus = res.petStatus ?: it.petStatus,
                    )
                }
                loadHubSummary()
            }.onFailure { err ->
                playSfx(SfxKind.WIN)
                _state.update {
                    it.copy(
                        isSubmitting = false,
                        submitResult = DailyQuestSubmitResult(
                            awarded = false,
                            alreadyCompleted = false,
                            xpEarned = 0,
                            scorePercent = ((correctCount.toFloat() / totalCount.coerceAtLeast(1)) * 100f).toInt(),
                            totalXp = it.hubQuest?.totalXp ?: 0L,
                            level = it.hubQuest?.level ?: 1,
                            streakDays = it.hubQuest?.streakDays ?: 1,
                            leveledUp = false,
                            isPreviewAccount = false,
                            message = "Permainan selesai! (${err.toHumanReadableError()})",
                        ),
                    )
                }
            }
        }
    }

    fun feedShibaPet() {
        val current = _state.value
        if (current.isFeedingPet) return
        _state.update { it.copy(isFeedingPet = true, petNotice = null) }
        viewModelScope.launch {
            gamificationRepository.feedShibaPet()
                .onSuccess { res ->
                    playSfx(if (res.evolved) SfxKind.WIN else SfxKind.CORRECT)
                    _state.update {
                        it.copy(
                            isFeedingPet = false,
                            petStatus = res.petStatus ?: it.petStatus,
                            hubQuest = it.hubQuest?.copy(
                                totalXp = res.totalXp,
                                level = res.level,
                                petStatus = res.petStatus ?: it.hubQuest.petStatus,
                            ),
                            petNotice = res.message,
                        )
                    }
                }
                .onFailure { err ->
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
        val current = _state.value
        if (current.isOpeningChest || current.chestClaimedToday) return
        _state.update { it.copy(isOpeningChest = true, petNotice = null) }
        viewModelScope.launch {
            gamificationRepository.openDailyChest()
                .onSuccess { res ->
                    playSfx(SfxKind.WIN)
                    _state.update {
                        it.copy(
                            isOpeningChest = false,
                            chestClaimedToday = true,
                            petStatus = res.petStatus ?: it.petStatus,
                            hubQuest = it.hubQuest?.copy(
                                totalXp = res.totalXp,
                                level = res.level,
                                petStatus = res.petStatus ?: it.hubQuest.petStatus,
                            ),
                            petNotice = res.message,
                        )
                    }
                }
                .onFailure { err ->
                    _state.update {
                        it.copy(
                            isOpeningChest = false,
                            petNotice = err.toHumanReadableError(),
                        )
                    }
                }
        }
    }

    fun playTapSfx() = playSfx(SfxKind.TAP)
    fun playGoodSfx() = playSfx(SfxKind.CORRECT)
    fun playErrorSfx() = playSfx(SfxKind.WRONG)

    enum class SfxKind { TAP, CORRECT, WRONG, WIN }

    private fun playSfx(kind: SfxKind) {
        if (!_state.value.soundEnabled) return
        runCatching {
            val tg = toneGenerator ?: ToneGenerator(AudioManager.STREAM_MUSIC, 65).also { toneGenerator = it }
            when (kind) {
                SfxKind.TAP -> tg.startTone(ToneGenerator.TONE_PROP_BEEP, 40)
                SfxKind.CORRECT -> tg.startTone(ToneGenerator.TONE_PROP_ACK, 100)
                SfxKind.WRONG -> tg.startTone(ToneGenerator.TONE_PROP_NACK, 130)
                SfxKind.WIN -> tg.startTone(ToneGenerator.TONE_PROP_BEEP2, 240)
            }
        }
    }
}
