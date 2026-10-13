package com.schoolos.android.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class XpTransactionDto(
    val id: String = "",
    @SerialName("action_type") val actionType: String = "",
    @SerialName("reference_id") val referenceId: String? = null,
    @SerialName("xp_amount") val xpAmount: Int = 0,
    val description: String? = null,
    @SerialName("created_at") val createdAt: String = "",
)

@Serializable
data class GamificationStatusDto(
    @SerialName("student_id") val studentId: String = "",
    @SerialName("total_xp") val totalXp: Long = 0,
    val level: Int = 1,
    @SerialName("streak_days") val streakDays: Int = 1,
    @SerialName("last_activity_date") val lastActivityDate: String? = null,
    @SerialName("progress_to_next_level") val progressToNextLevel: Int = 0,
    @SerialName("next_level_target_xp") val nextLevelTargetXp: Long = 50,
    val transactions: List<XpTransactionDto> = emptyList(),
)

@Serializable
data class AwardXpRequestDto(
    @SerialName("student_id") val studentId: String? = null,
    @SerialName("action_type") val actionType: String,
    @SerialName("reference_id") val referenceId: String? = null,
    val description: String? = null,
)

@Serializable
data class AwardXpResponseDto(
    val awarded: Boolean = false,
    @SerialName("already_awarded") val alreadyAwarded: Boolean = false,
    @SerialName("xp_earned") val xpEarned: Int = 0,
    @SerialName("total_xp") val totalXp: Long = 0,
    val level: Int = 1,
    @SerialName("streak_days") val streakDays: Int = 1,
    @SerialName("leveled_up") val leveledUp: Boolean = false,
    val message: String = "",
)
