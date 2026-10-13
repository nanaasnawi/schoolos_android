package com.schoolos.android.data.mapper

import com.schoolos.android.data.remote.dto.GamificationStatusDto
import com.schoolos.android.data.remote.dto.XpTransactionDto
import com.schoolos.android.domain.model.GamificationStatus
import com.schoolos.android.domain.model.XpTransaction

fun XpTransactionDto.toDomain(): XpTransaction = XpTransaction(
    id = id,
    actionType = actionType,
    referenceId = referenceId,
    xpAmount = xpAmount,
    description = description,
    createdAt = createdAt,
)

fun GamificationStatusDto.toDomain(): GamificationStatus = GamificationStatus(
    studentId = studentId,
    totalXp = totalXp,
    level = level.coerceAtLeast(1),
    streakDays = streakDays.coerceAtLeast(1),
    lastActivityDate = lastActivityDate,
    progressToNextLevel = progressToNextLevel.coerceIn(0, 100),
    nextLevelTargetXp = nextLevelTargetXp,
    levelTitle = GamificationStatus.resolveTitle(level),
    rankBadge = GamificationStatus.resolveBadge(level),
    transactions = transactions.map { it.toDomain() },
)
