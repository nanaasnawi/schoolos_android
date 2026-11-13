package com.schoolos.android.domain.model

enum class DailyQuestGameMode(val code: String, val displayLabel: String) {
    FACT_OR_MYTH("FACT_OR_MYTH", "Fakta atau Mitos"),
    CASE_DETECTIVE("CASE_DETECTIVE", "Detektif Kasus"),
    SEQUENCE_ORDER("SEQUENCE_ORDER", "Susun Urutan"),
    CONCEPT_GUESS("CONCEPT_GUESS", "Tebak Istilah");

    companion object {
        fun fromCode(raw: String?): DailyQuestGameMode = when (raw?.trim()?.uppercase()) {
            "CASE_DETECTIVE" -> CASE_DETECTIVE
            "SEQUENCE_ORDER" -> SEQUENCE_ORDER
            "CONCEPT_GUESS" -> CONCEPT_GUESS
            else -> FACT_OR_MYTH
        }
    }
}

sealed class DailyQuestChallenge {
    abstract val id: String
    abstract val topicTag: String?
    abstract val explanation: String?

    data class FactOrMyth(
        override val id: String,
        val statement: String,
        val isFact: Boolean,
        override val explanation: String? = null,
        override val topicTag: String? = null,
    ) : DailyQuestChallenge()

    data class CaseDetective(
        override val id: String,
        val scenario: String,
        val question: String,
        val options: List<String>,
        val correctIndex: Int,
        override val explanation: String? = null,
        override val topicTag: String? = null,
    ) : DailyQuestChallenge()

    data class SequenceOrder(
        override val id: String,
        val prompt: String,
        val steps: List<String>,
        override val explanation: String? = null,
        override val topicTag: String? = null,
    ) : DailyQuestChallenge()

    data class ConceptGuess(
        override val id: String,
        val answer: String,
        val clues: List<String>,
        val acceptableAliases: List<String> = emptyList(),
        override val explanation: String? = null,
        override val topicTag: String? = null,
    ) : DailyQuestChallenge()
}

data class DailyQuestModeInfo(
    val code: DailyQuestGameMode,
    val label: String,
    val isFeaturedToday: Boolean,
    val completedToday: Boolean,
)

data class DailyQuestAttemptSummary(
    val scorePercent: Int,
    val correctCount: Int,
    val totalCount: Int,
    val maxCombo: Int,
    val durationSeconds: Int,
    val xpEarned: Int,
)

data class DailyQuestLeaderboardEntry(
    val studentName: String,
    val className: String,
    val scorePercent: Int,
    val durationSeconds: Int,
    val xpEarned: Int,
    val isMe: Boolean,
)

data class DailyQuest(
    val questId: String,
    val questDate: String,
    val dayLabel: String,
    val gradeBand: String,
    val className: String,
    val gameMode: DailyQuestGameMode,
    val featuredModeToday: DailyQuestGameMode,
    val isFeaturedToday: Boolean,
    val title: String,
    val subtitle: String,
    val subjectName: String,
    val sourceLabel: String,
    val xpReward: Int,
    val challenges: List<DailyQuestChallenge>,
    val alreadyCompleted: Boolean,
    val myAttempt: DailyQuestAttemptSummary? = null,
    val streakDays: Int = 1,
    val totalXp: Long = 0,
    val level: Int = 1,
    val availableModes: List<DailyQuestModeInfo> = emptyList(),
    val leaderboard: List<DailyQuestLeaderboardEntry> = emptyList(),
)

data class DailyQuestSubmitResult(
    val awarded: Boolean,
    val alreadyCompleted: Boolean,
    val xpEarned: Int,
    val scorePercent: Int,
    val totalXp: Long,
    val level: Int,
    val streakDays: Int,
    val leveledUp: Boolean,
    val isPreviewAccount: Boolean,
    val message: String,
)

