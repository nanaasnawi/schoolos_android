package com.schoolos.android.domain.model

data class XpTransaction(
    val id: String,
    val actionType: String,
    val referenceId: String?,
    val xpAmount: Int,
    val description: String?,
    val createdAt: String,
)

data class GamificationStatus(
    val studentId: String,
    val totalXp: Long,
    val level: Int,
    val streakDays: Int,
    val lastActivityDate: String?,
    val progressToNextLevel: Int,
    val nextLevelTargetXp: Long,
    val levelTitle: String,
    val rankBadge: String,
    val transactions: List<XpTransaction>,
) {
    companion object {
        fun resolveTitle(level: Int): String = when (level) {
            1 -> "Pelajar Pemula"
            2 -> "Pembelajar Aktif"
            3 -> "Penjelajah Ilmu"
            4 -> "Cendekiawan Muda"
            5 -> "Teladan Berprestasi"
            6 -> "Bintang Kehormatan"
            7 -> "Legenda Juara"
            else -> "Pujangga Kesatria"
        }

        fun resolveBadge(level: Int): String = when (level) {
            1 -> "Novice"
            2 -> "Apprentice"
            3 -> "Explorer"
            4 -> "Scholar"
            5 -> "Master"
            6 -> "Grandmaster"
            7 -> "Champion"
            else -> "Mythic"
        }
    }
}

data class AwardXpResult(
    val awarded: Boolean,
    val alreadyAwarded: Boolean,
    val xpEarned: Int,
    val totalXp: Long,
    val level: Int,
    val streakDays: Int,
    val leveledUp: Boolean,
    val message: String,
)
