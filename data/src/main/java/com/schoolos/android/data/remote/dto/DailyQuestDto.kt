package com.schoolos.android.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class DailyQuestModeInfoDto(
    val code: String = "CROSSWORD",
    val label: String = "",
    @SerialName("is_featured_today") val isFeaturedToday: Boolean = false,
    @SerialName("completed_today") val completedToday: Boolean = false,
)

@Serializable
data class DailyQuestAttemptSummaryDto(
    @SerialName("score_percent") val scorePercent: Int = 0,
    @SerialName("correct_count") val correctCount: Int = 0,
    @SerialName("total_count") val totalCount: Int = 0,
    @SerialName("max_combo") val maxCombo: Int = 0,
    @SerialName("duration_seconds") val durationSeconds: Int = 0,
    @SerialName("xp_earned") val xpEarned: Int = 0,
)

@Serializable
data class DailyQuestLeaderboardEntryDto(
    @SerialName("student_name") val studentName: String = "",
    @SerialName("class_name") val className: String = "",
    @SerialName("score_percent") val scorePercent: Int = 0,
    @SerialName("duration_seconds") val durationSeconds: Int = 0,
    @SerialName("xp_earned") val xpEarned: Int = 0,
    @SerialName("is_me") val isMe: Boolean = false,
)

@Serializable
data class ShibaPetStatusDto(
    @SerialName("pet_name") val petName: String = "Ciko Pintar",
    @SerialName("pet_stage") val petStage: Int = 1,
    @SerialName("stage_title") val stageTitle: String = "Anak Kucing Penasaran",
    @SerialName("food_points") val foodPoints: Int = 0,
    @SerialName("points_to_next_stage") val pointsToNextStage: Int = 100,
    @SerialName("energy_required") val energyRequired: Int = 120,
    @SerialName("student_xp") val studentXp: Long = 0L,
    @SerialName("xp_required_next") val xpRequiredNext: Long = 250L,
    @SerialName("xp_remaining_next") val xpRemainingNext: Long = 250L,
    @SerialName("evolution_progress_percent") val evolutionProgressPercent: Int = 0,
    @SerialName("max_stage") val maxStage: Int = 6,
    @SerialName("ready_to_evolve") val readyToEvolve: Boolean = false,
    @SerialName("evolve_requirement_hint") val evolveRequirementHint: String = "",
    @SerialName("total_fed") val totalFed: Int = 0,
    val biscuits: Int = 0,
    @SerialName("fifty_fifty_tokens") val fiftyFiftyTokens: Int = 1,
)

@Serializable
data class DailyQuestResponseDto(
    @SerialName("quest_id") val questId: String = "",
    @SerialName("quest_date") val questDate: String = "",
    @SerialName("day_label") val dayLabel: String = "",
    @SerialName("grade_band") val gradeBand: String = "PAKET_B",
    @SerialName("class_name") val className: String = "",
    @SerialName("game_mode") val gameMode: String = "CROSSWORD",
    @SerialName("featured_mode_today") val featuredModeToday: String = "CROSSWORD",
    @SerialName("is_featured_today") val isFeaturedToday: Boolean = false,
    @SerialName("round_number") val roundNumber: Int = 0,
    val title: String = "",
    val subtitle: String = "",
    @SerialName("subject_name") val subjectName: String = "",
    @SerialName("source_label") val sourceLabel: String = "",
    @SerialName("xp_reward") val xpReward: Int = 80,
    val challenges: List<JsonObject> = emptyList(),
    @SerialName("already_completed") val alreadyCompleted: Boolean = false,
    @SerialName("my_attempt") val myAttempt: DailyQuestAttemptSummaryDto? = null,
    @SerialName("streak_days") val streakDays: Int = 1,
    @SerialName("total_xp") val totalXp: Long = 0,
    val level: Int = 1,
    @SerialName("pet_status") val petStatus: ShibaPetStatusDto? = null,
    @SerialName("chest_claimed_today") val chestClaimedToday: Boolean = false,
    @SerialName("available_modes") val availableModes: List<DailyQuestModeInfoDto> = emptyList(),
    val leaderboard: List<DailyQuestLeaderboardEntryDto> = emptyList(),
)

@Serializable
data class SubmitDailyQuestRequestDto(
    @SerialName("quest_id") val questId: String,
    @SerialName("correct_count") val correctCount: Int,
    @SerialName("total_count") val totalCount: Int,
    @SerialName("max_combo") val maxCombo: Int,
    @SerialName("duration_seconds") val durationSeconds: Int,
    @SerialName("round_number") val roundNumber: Int = 0,
)

@Serializable
data class SubmitDailyQuestResponseDto(
    val awarded: Boolean = false,
    @SerialName("already_completed") val alreadyCompleted: Boolean = false,
    @SerialName("xp_earned") val xpEarned: Int = 0,
    @SerialName("biscuits_earned") val biscuitsEarned: Int = 0,
    @SerialName("score_percent") val scorePercent: Int = 0,
    @SerialName("total_xp") val totalXp: Long = 0,
    val level: Int = 1,
    @SerialName("streak_days") val streakDays: Int = 1,
    @SerialName("leveled_up") val leveledUp: Boolean = false,
    @SerialName("is_preview_account") val isPreviewAccount: Boolean = false,
    @SerialName("pet_status") val petStatus: ShibaPetStatusDto? = null,
    val message: String = "",
)

@Serializable
data class FeedPetResponseDto(
    val fed: Boolean = false,
    val evolved: Boolean = false,
    @SerialName("bonus_xp_earned") val bonusXpEarned: Int = 0,
    @SerialName("total_xp") val totalXp: Long = 0,
    val level: Int = 1,
    @SerialName("pet_status") val petStatus: ShibaPetStatusDto? = null,
    val message: String = "",
)

@Serializable
data class DailyChestResponseDto(
    val opened: Boolean = false,
    @SerialName("already_claimed") val alreadyClaimed: Boolean = false,
    @SerialName("reward_type") val rewardType: String = "XP",
    @SerialName("reward_amount") val rewardAmount: Int = 0,
    @SerialName("reward_label") val rewardLabel: String = "",
    @SerialName("total_xp") val totalXp: Long = 0,
    val level: Int = 1,
    @SerialName("pet_status") val petStatus: ShibaPetStatusDto? = null,
    val message: String = "",
)

