package com.schoolos.android.data.mapper

import com.schoolos.android.data.remote.dto.DailyQuestAttemptSummaryDto
import com.schoolos.android.data.remote.dto.DailyQuestLeaderboardEntryDto
import com.schoolos.android.data.remote.dto.DailyQuestModeInfoDto
import com.schoolos.android.data.remote.dto.DailyQuestResponseDto
import com.schoolos.android.data.remote.dto.SubmitDailyQuestResponseDto
import com.schoolos.android.domain.model.DailyQuest
import com.schoolos.android.domain.model.DailyQuestAttemptSummary
import com.schoolos.android.domain.model.DailyQuestChallenge
import com.schoolos.android.domain.model.DailyQuestGameMode
import com.schoolos.android.domain.model.DailyQuestLeaderboardEntry
import com.schoolos.android.domain.model.DailyQuestModeInfo
import com.schoolos.android.domain.model.DailyQuestSubmitResult
import com.schoolos.android.domain.model.MapPinItem
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

private fun JsonObject.str(key: String): String =
    runCatching { this[key]?.jsonPrimitive?.contentOrNull }.getOrNull().orEmpty()

private fun JsonObject.strOrNull(key: String): String? =
    runCatching { this[key]?.jsonPrimitive?.contentOrNull }.getOrNull()?.takeIf { it.isNotBlank() }

private fun JsonObject.bool(key: String, default: Boolean = false): Boolean =
    runCatching { this[key]?.jsonPrimitive?.booleanOrNull }.getOrNull() ?: default

private fun JsonObject.intVal(key: String, default: Int = 0): Int =
    runCatching { this[key]?.jsonPrimitive?.intOrNull }.getOrNull() ?: default

private fun JsonObject.floatVal(key: String, default: Float = 0.5f): Float =
    runCatching { this[key]?.jsonPrimitive?.floatOrNull }.getOrNull() ?: default

private fun JsonObject.strList(key: String): List<String> =
    runCatching {
        this[key]?.jsonArray?.mapNotNull { runCatching { it.jsonPrimitive.contentOrNull }.getOrNull() }.orEmpty()
    }.getOrDefault(emptyList())

private fun JsonObject.pinList(key: String): List<MapPinItem> =
    runCatching {
        this[key]?.jsonArray?.mapIndexedNotNull { idx, elem ->
            val obj = elem.jsonObject
            val label = obj.str("label")
            if (label.isBlank()) null
            else MapPinItem(
                id = obj.str("id").ifBlank { "pin_$idx" },
                label = label,
                stepLabel = obj.strOrNull("step_label"),
                x = obj.floatVal("x", 0.5f).coerceIn(0.08f, 0.92f),
                y = obj.floatVal("y", 0.5f).coerceIn(0.10f, 0.90f),
                isTarget = obj.bool("is_target", false),
            )
        }.orEmpty()
    }.getOrDefault(emptyList())

private fun JsonObject.toChallenge(mode: DailyQuestGameMode, fallbackIndex: Int): DailyQuestChallenge? {
    val id = str("id").ifBlank { "ch_$fallbackIndex" }
    val explanation = strOrNull("explanation")
    val topicTag = strOrNull("topic_tag")

    return when (mode) {
        DailyQuestGameMode.MAP_PIN_HUNT -> {
            val prompt = str("prompt")
            val pins = pinList("pins")
            if (prompt.isBlank() || pins.isEmpty()) null
            else DailyQuestChallenge.MapPinHunt(
                id = id,
                sceneId = str("scene_id").ifBlank { "cirebon_keraton_map" },
                interactionType = str("interaction_type").ifBlank { "MAP_TAGS" },
                prompt = prompt,
                pins = pins,
                explanation = explanation,
                topicTag = topicTag,
            )
        }
        DailyQuestGameMode.FACT_OR_MYTH -> {
            val statement = str("statement")
            if (statement.isBlank()) null
            else DailyQuestChallenge.FactOrMyth(
                id = id,
                statement = statement,
                isFact = bool("is_fact", true),
                explanation = explanation,
                topicTag = topicTag,
            )
        }
        DailyQuestGameMode.CASE_DETECTIVE -> {
            val question = str("question")
            val options = strList("options")
            if (question.isBlank() || options.isEmpty()) null
            else DailyQuestChallenge.CaseDetective(
                id = id,
                scenario = str("scenario"),
                question = question,
                options = options,
                correctIndex = intVal("correct_index", 0).coerceIn(0, (options.size - 1).coerceAtLeast(0)),
                explanation = explanation,
                topicTag = topicTag,
            )
        }
        DailyQuestGameMode.SEQUENCE_ORDER -> {
            val steps = strList("steps")
            if (steps.isEmpty()) null
            else DailyQuestChallenge.SequenceOrder(
                id = id,
                prompt = str("prompt"),
                steps = steps,
                explanation = explanation,
                topicTag = topicTag,
            )
        }
        DailyQuestGameMode.CONCEPT_GUESS -> {
            val answer = str("answer")
            if (answer.isBlank()) null
            else DailyQuestChallenge.ConceptGuess(
                id = id,
                answer = answer,
                clues = strList("clues"),
                acceptableAliases = strList("acceptable_aliases"),
                explanation = explanation,
                topicTag = topicTag,
            )
        }
    }
}

fun DailyQuestModeInfoDto.toDomain() = DailyQuestModeInfo(
    code = DailyQuestGameMode.fromCode(code),
    label = label,
    isFeaturedToday = isFeaturedToday,
    completedToday = completedToday,
)

fun DailyQuestAttemptSummaryDto.toDomain() = DailyQuestAttemptSummary(
    scorePercent = scorePercent,
    correctCount = correctCount,
    totalCount = totalCount,
    maxCombo = maxCombo,
    durationSeconds = durationSeconds,
    xpEarned = xpEarned,
)

fun DailyQuestLeaderboardEntryDto.toDomain() = DailyQuestLeaderboardEntry(
    studentName = studentName,
    className = className,
    scorePercent = scorePercent,
    durationSeconds = durationSeconds,
    xpEarned = xpEarned,
    isMe = isMe,
)

fun DailyQuestResponseDto.toDomain(): DailyQuest {
    val parsedMode = DailyQuestGameMode.fromCode(gameMode)
    val parsedChallenges = challenges.mapIndexedNotNull { index, obj ->
        obj.toChallenge(parsedMode, index)
    }
    return DailyQuest(
        questId = questId,
        questDate = questDate,
        dayLabel = dayLabel,
        gradeBand = gradeBand,
        className = className,
        gameMode = parsedMode,
        featuredModeToday = DailyQuestGameMode.fromCode(featuredModeToday),
        isFeaturedToday = isFeaturedToday,
        title = title,
        subtitle = subtitle,
        subjectName = subjectName,
        sourceLabel = sourceLabel,
        xpReward = xpReward,
        challenges = parsedChallenges,
        alreadyCompleted = alreadyCompleted,
        myAttempt = myAttempt?.toDomain(),
        streakDays = streakDays,
        totalXp = totalXp,
        level = level.coerceAtLeast(1),
        availableModes = availableModes.map { it.toDomain() },
        leaderboard = leaderboard.map { it.toDomain() },
    )
}

fun SubmitDailyQuestResponseDto.toDomain() = DailyQuestSubmitResult(
    awarded = awarded,
    alreadyCompleted = alreadyCompleted,
    xpEarned = xpEarned,
    scorePercent = scorePercent,
    totalXp = totalXp,
    level = level.coerceAtLeast(1),
    streakDays = streakDays.coerceAtLeast(1),
    leveledUp = leveledUp,
    isPreviewAccount = isPreviewAccount,
    message = message,
)
