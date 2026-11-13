package com.schoolos.android.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class DailyQuestModeInfoDto(
    val code: String = "FACT_OR_MYTH",
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
data class DailyQuestResponseDto(
    @SerialName("quest_id") val questId: String = "",
    @SerialName("quest_date") val questDate: String = "",
    @SerialName("day_label") val dayLabel: String = "",
    @SerialName("grade_band") val gradeBand: String = "PAKET_B",
    @SerialName("class_name") val className: String = "",
    @SerialName("game_mode") val gameMode: String = "FACT_OR_MYTH",
    @SerialName("featured_mode_today") val featuredModeToday: String = "FACT_OR_MYTH",
    @SerialName("is_featured_today") val isFeaturedToday: Boolean = false,
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
)

@Serializable
data class SubmitDailyQuestResponseDto(
    val awarded: Boolean = false,
    @SerialName("already_completed") val alreadyCompleted: Boolean = false,
    @SerialName("xp_earned") val xpEarned: Int = 0,
    @SerialName("score_percent") val scorePercent: Int = 0,
    @SerialName("total_xp") val totalXp: Long = 0,
    val level: Int = 1,
    @SerialName("streak_days") val streakDays: Int = 1,
    @SerialName("leveled_up") val leveledUp: Boolean = false,
    @SerialName("is_preview_account") val isPreviewAccount: Boolean = false,
    val message: String = "",
)

