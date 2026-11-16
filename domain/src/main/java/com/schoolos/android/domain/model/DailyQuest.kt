package com.schoolos.android.domain.model

enum class DailyQuestGameMode(val code: String, val displayLabel: String) {
    CROSSWORD("CROSSWORD", "Teka-Teki Silang"),
    SLIDING_PUZZLE("SLIDING_PUZZLE", "Puzzle Geser"),
    BLOCK_PUZZLE("BLOCK_PUZZLE", "Block Blast"),
    NUMBER_MERGE("NUMBER_MERGE", "2048 Angka");

    companion object {
        fun fromCode(raw: String?): DailyQuestGameMode = when (raw?.trim()?.uppercase()) {
            "SLIDING_PUZZLE" -> SLIDING_PUZZLE
            "BLOCK_PUZZLE" -> BLOCK_PUZZLE
            "NUMBER_MERGE" -> NUMBER_MERGE
            else -> CROSSWORD
        }
    }
}

data class CrosswordClueItem(
    val number: Int,
    val direction: String, // "ACROSS" or "DOWN"
    val row: Int,
    val col: Int,
    val length: Int,
    val answer: String,
    val clue: String,
    val topicTag: String? = null,
)

data class NumberMergeSeedTile(
    val row: Int,
    val col: Int,
    val value: Int,
)

data class ShibaPetStatus(
    val petName: String = "Ciko Pintar",
    val petStage: Int = 1,
    val stageTitle: String = "Anak Kucing Penasaran",
    val foodPoints: Int = 25,
    val pointsToNextStage: Int = 75,
    val totalFed: Int = 0,
    val biscuits: Int = 0,
    val fiftyFiftyTokens: Int = 1,
)

data class FeedPetResult(
    val fed: Boolean,
    val evolved: Boolean,
    val bonusXpEarned: Int,
    val totalXp: Long,
    val level: Int,
    val petStatus: ShibaPetStatus? = null,
    val message: String,
)

data class DailyChestResult(
    val opened: Boolean,
    val alreadyClaimed: Boolean,
    val rewardType: String,
    val rewardAmount: Int,
    val rewardLabel: String,
    val totalXp: Long,
    val level: Int,
    val petStatus: ShibaPetStatus? = null,
    val message: String,
)

sealed class DailyQuestChallenge {
    abstract val id: String
    abstract val topicTag: String?
    abstract val explanation: String?

    data class Crossword(
        override val id: String,
        val rows: Int,
        val cols: Int,
        val grid: List<String>,
        val clues: List<CrosswordClueItem>,
        override val topicTag: String? = null,
        override val explanation: String? = null,
    ) : DailyQuestChallenge()

    data class SlidingPuzzle(
        override val id: String,
        val gridSize: Int,
        val themeId: String,
        val themeTitle: String,
        val themeSubtitle: String,
        val initialTiles: List<Int>,
        val parMoves: Int,
        val seed: Long,
        override val topicTag: String? = null,
        override val explanation: String? = null,
    ) : DailyQuestChallenge()

    data class BlockPuzzle(
        override val id: String,
        val boardSize: Int,
        val targetScore: Int,
        val targetLines: Int,
        val seed: Long,
        val prefilledCells: List<Pair<Int, Int>>,
        val themeTitle: String,
        override val topicTag: String? = null,
        override val explanation: String? = null,
    ) : DailyQuestChallenge()

    data class NumberMerge(
        override val id: String,
        val boardSize: Int,
        val targetTile: Int,
        val targetScore: Int,
        val seed: Long,
        val initialTiles: List<NumberMergeSeedTile>,
        val themeTitle: String,
        override val topicTag: String? = null,
        override val explanation: String? = null,
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
    val roundNumber: Int = 0,
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
    val petStatus: ShibaPetStatus? = null,
    val chestClaimedToday: Boolean = false,
    val availableModes: List<DailyQuestModeInfo> = emptyList(),
    val leaderboard: List<DailyQuestLeaderboardEntry> = emptyList(),
)

data class DailyQuestSubmitResult(
    val awarded: Boolean,
    val alreadyCompleted: Boolean,
    val xpEarned: Int,
    val biscuitsEarned: Int = 0,
    val scorePercent: Int,
    val totalXp: Long,
    val level: Int,
    val streakDays: Int,
    val leveledUp: Boolean,
    val isPreviewAccount: Boolean,
    val petStatus: ShibaPetStatus? = null,
    val message: String,
)
