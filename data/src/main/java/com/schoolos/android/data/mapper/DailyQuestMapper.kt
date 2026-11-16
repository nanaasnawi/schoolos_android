package com.schoolos.android.data.mapper

import com.schoolos.android.data.remote.dto.DailyChestResponseDto
import com.schoolos.android.data.remote.dto.DailyQuestAttemptSummaryDto
import com.schoolos.android.data.remote.dto.DailyQuestLeaderboardEntryDto
import com.schoolos.android.data.remote.dto.DailyQuestModeInfoDto
import com.schoolos.android.data.remote.dto.DailyQuestResponseDto
import com.schoolos.android.data.remote.dto.FeedPetResponseDto
import com.schoolos.android.data.remote.dto.ShibaPetStatusDto
import com.schoolos.android.data.remote.dto.SubmitDailyQuestResponseDto
import com.schoolos.android.domain.model.CognitiveStageData
import com.schoolos.android.domain.model.CrosswordClueItem
import com.schoolos.android.domain.model.CrosswordSubjectOption
import com.schoolos.android.domain.model.DailyChestResult
import com.schoolos.android.domain.model.DailyQuest
import com.schoolos.android.domain.model.DailyQuestAttemptSummary
import com.schoolos.android.domain.model.DailyQuestChallenge
import com.schoolos.android.domain.model.DailyQuestGameMode
import com.schoolos.android.domain.model.DailyQuestLeaderboardEntry
import com.schoolos.android.domain.model.DailyQuestModeInfo
import com.schoolos.android.domain.model.DailyQuestSubmitResult
import com.schoolos.android.domain.model.FeedPetResult
import com.schoolos.android.domain.model.KahootQuestionItem
import com.schoolos.android.domain.model.NumberMergeSeedTile
import com.schoolos.android.domain.model.ShibaPetStatus
import com.schoolos.android.domain.model.WordScrambleItemData
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

private fun JsonObject.str(key: String): String =
    runCatching { this[key]?.jsonPrimitive?.contentOrNull }.getOrNull().orEmpty()

private fun JsonObject.strOrNull(key: String): String? =
    runCatching { this[key]?.jsonPrimitive?.contentOrNull }.getOrNull()?.takeIf { it.isNotBlank() }

private fun JsonObject.intVal(key: String, default: Int = 0): Int =
    runCatching { this[key]?.jsonPrimitive?.intOrNull }.getOrNull() ?: default

private fun JsonObject.longVal(key: String, default: Long = 0L): Long =
    runCatching { this[key]?.jsonPrimitive?.longOrNull }.getOrNull() ?: default

private fun JsonObject.strList(key: String): List<String> =
    runCatching {
        this[key]?.jsonArray?.mapNotNull { runCatching { it.jsonPrimitive.contentOrNull }.getOrNull() }.orEmpty()
    }.getOrDefault(emptyList())

private fun JsonObject.intList(key: String): List<Int> =
    runCatching {
        this[key]?.jsonArray?.mapNotNull { runCatching { it.jsonPrimitive.intOrNull }.getOrNull() }.orEmpty()
    }.getOrDefault(emptyList())

private fun JsonObject.toChallenge(mode: DailyQuestGameMode, fallbackIndex: Int): DailyQuestChallenge? {
    val id = str("id").ifBlank { "ch_$fallbackIndex" }
    val explanation = strOrNull("explanation")
    val topicTag = strOrNull("topic_tag")

    return when (mode) {
        DailyQuestGameMode.CROSSWORD -> {
            val rows = intVal("rows", 0)
            val cols = intVal("cols", 0)
            val grid = strList("grid")
            val cluesArray = runCatching { this["clues"]?.jsonArray }.getOrNull().orEmpty()
            val clues = cluesArray.mapNotNull { elem ->
                val obj = runCatching { elem.jsonObject }.getOrNull() ?: return@mapNotNull null
                val ans = obj.str("answer")
                val clueTxt = obj.str("clue")
                if (ans.isBlank() || clueTxt.isBlank()) null
                else CrosswordClueItem(
                    number = obj.intVal("number", 1),
                    direction = obj.str("direction").ifBlank { "ACROSS" },
                    row = obj.intVal("row", 0),
                    col = obj.intVal("col", 0),
                    length = obj.intVal("length", ans.length),
                    answer = ans,
                    clue = clueTxt,
                    topicTag = obj.strOrNull("topic_tag"),
                )
            }
            val subjArr = runCatching { this["available_subjects"]?.jsonArray }.getOrNull().orEmpty()
            val availableSubjects = subjArr.mapNotNull { elem ->
                val obj = runCatching { elem.jsonObject }.getOrNull() ?: return@mapNotNull null
                val code = obj.str("code")
                val label = obj.str("label")
                if (code.isBlank() || label.isBlank()) null
                else CrosswordSubjectOption(code = code, label = label)
            }
            val levelVal = intVal("level", 1).coerceAtLeast(1)
            val maxHintsVal = intVal("max_hints", if (levelVal >= 5) 2 else 3).coerceAtLeast(1)
            val diffLabel = str("difficulty_label").ifBlank {
                when {
                    levelVal >= 5 -> "Master"
                    levelVal >= 3 -> "Menengah"
                    else -> "Pemula"
                }
            }
            val aiGen = runCatching { this["ai_generated"]?.jsonPrimitive?.booleanOrNull }.getOrNull() ?: false
            if (rows <= 0 || cols <= 0 || grid.isEmpty() || clues.isEmpty()) null
            else DailyQuestChallenge.Crossword(
                id = id,
                rows = rows,
                cols = cols,
                grid = grid,
                clues = clues,
                maxHints = maxHintsVal,
                level = levelVal,
                difficultyLabel = diffLabel,
                subject = str("subject").ifBlank { "UMUM" },
                subjectLabel = str("subject_label").ifBlank { "Semua Mapel" },
                aiGenerated = aiGen,
                availableSubjects = availableSubjects,
                topicTag = topicTag,
                explanation = explanation,
            )
        }

        DailyQuestGameMode.SLIDING_PUZZLE -> {
            val gridSize = intVal("grid_size", 3).coerceIn(3, 4)
            val tiles = intList("initial_tiles")
            if (tiles.isEmpty()) null
            else DailyQuestChallenge.SlidingPuzzle(
                id = id,
                gridSize = gridSize,
                themeId = str("theme_id").ifBlank { "TATA_SURYA" },
                themeTitle = str("theme_title").ifBlank { "Puzzle Geser Visual" },
                themeSubtitle = str("theme_subtitle"),
                initialTiles = tiles,
                parMoves = intVal("par_moves", 35),
                seed = longVal("seed", 12345L),
                topicTag = topicTag,
                explanation = explanation,
            )
        }

        DailyQuestGameMode.BLOCK_PUZZLE -> {
            val prefilledArr = runCatching { this["prefilled_cells"]?.jsonArray }.getOrNull().orEmpty()
            val prefilled = prefilledArr.mapNotNull { elem ->
                val arr = runCatching { elem.jsonArray }.getOrNull() ?: return@mapNotNull null
                if (arr.size >= 2) {
                    val r = runCatching { arr[0].jsonPrimitive.intOrNull }.getOrNull()
                    val c = runCatching { arr[1].jsonPrimitive.intOrNull }.getOrNull()
                    if (r != null && c != null) Pair(r, c) else null
                } else null
            }
            DailyQuestChallenge.BlockPuzzle(
                id = id,
                boardSize = intVal("board_size", 8),
                targetScore = intVal("target_score", 350),
                targetLines = intVal("target_lines", 4),
                seed = longVal("seed", 123456L),
                prefilledCells = prefilled,
                themeTitle = str("theme_title").ifBlank { "Arena Block Blast Harian" },
                topicTag = topicTag,
                explanation = explanation,
            )
        }

        DailyQuestGameMode.NUMBER_MERGE -> {
            val seedTilesArr = runCatching { this["initial_tiles"]?.jsonArray }.getOrNull().orEmpty()
            val initialTiles = seedTilesArr.mapNotNull { elem ->
                val obj = runCatching { elem.jsonObject }.getOrNull() ?: return@mapNotNull null
                NumberMergeSeedTile(
                    row = obj.intVal("row", 0),
                    col = obj.intVal("col", 0),
                    value = obj.intVal("value", 2),
                )
            }
            DailyQuestChallenge.NumberMerge(
                id = id,
                boardSize = intVal("board_size", 4),
                targetTile = intVal("target_tile", 256),
                targetScore = intVal("target_score", 1500),
                seed = longVal("seed", 777123L),
                initialTiles = initialTiles,
                themeTitle = str("theme_title").ifBlank { "2048 Gabung Angka" },
                topicTag = topicTag,
                explanation = explanation,
            )
        }

        DailyQuestGameMode.KAHOOT_BLITZ -> {
            val qArr = runCatching { this["questions"]?.jsonArray }.getOrNull().orEmpty()
            val questions = qArr.mapIndexedNotNull { qIdx, elem ->
                val obj = runCatching { elem.jsonObject }.getOrNull() ?: return@mapIndexedNotNull null
                val qText = obj.str("question")
                val opts = obj.strList("options")
                if (qText.isBlank() || opts.isEmpty()) null
                else KahootQuestionItem(
                    id = obj.str("id").ifBlank { "kq_$qIdx" },
                    question = qText,
                    options = opts,
                    correctIndex = obj.intVal("correct_index", 0).coerceIn(0, (opts.size - 1).coerceAtLeast(0)),
                    topicTag = obj.str("topic_tag").ifBlank { "Umum" },
                    explanation = obj.strOrNull("explanation"),
                )
            }
            if (questions.isEmpty()) null
            else DailyQuestChallenge.KahootBlitz(
                id = id,
                gradeBand = str("grade_band").ifBlank { "PAKET_B" },
                timePerQuestion = intVal("time_per_question", 15),
                questions = questions,
                topicTag = topicTag,
                explanation = explanation,
            )
        }

        DailyQuestGameMode.WORD_SCRAMBLE -> {
            val arr = runCatching { this["items"]?.jsonArray }.getOrNull().orEmpty()
            val items = arr.mapIndexedNotNull { wIdx, elem ->
                val obj = runCatching { elem.jsonObject }.getOrNull() ?: return@mapIndexedNotNull null
                val word = obj.str("word")
                val letters = obj.strList("scrambled_letters")
                if (word.isBlank() || letters.isEmpty()) null
                else WordScrambleItemData(
                    id = obj.str("id").ifBlank { "ws_$wIdx" },
                    word = word,
                    scrambledLetters = letters,
                    language = obj.str("language").ifBlank { "ID" },
                    category = obj.str("category").ifBlank { "Kosakata" },
                    clue = obj.str("clue"),
                    explanation = obj.strOrNull("explanation"),
                )
            }
            if (items.isEmpty()) null
            else DailyQuestChallenge.WordScramble(
                id = id,
                gradeBand = str("grade_band").ifBlank { "PAKET_B" },
                items = items,
                topicTag = topicTag,
                explanation = explanation,
            )
        }

        DailyQuestGameMode.COGNITIVE_TRAINER -> {
            val arr = runCatching { this["stages"]?.jsonArray }.getOrNull().orEmpty()
            val stages = arr.mapIndexedNotNull { sIdx, elem ->
                val obj = runCatching { elem.jsonObject }.getOrNull() ?: return@mapIndexedNotNull null
                val kind = obj.str("kind").ifBlank { "PATTERN_SEQUENCE" }
                CognitiveStageData(
                    id = obj.str("id").ifBlank { "cs_$sIdx" },
                    kind = kind,
                    prompt = obj.str("prompt"),
                    gridSize = obj.intVal("grid_size", 3),
                    targets = obj.intList("targets"),
                    viewMs = obj.intVal("view_ms", 2500),
                    displayWord = obj.strOrNull("display_word"),
                    inkColor = obj.strOrNull("ink_color"),
                    sequence = obj.strOrNull("sequence"),
                    leftExpr = obj.strOrNull("left_expr"),
                    rightExpr = obj.strOrNull("right_expr"),
                    options = obj.strList("options"),
                    correctIndex = obj.intVal("correct_index", 0),
                    explanation = obj.strOrNull("explanation"),
                )
            }
            if (stages.isEmpty()) null
            else DailyQuestChallenge.CognitiveTrainer(
                id = id,
                gradeBand = str("grade_band").ifBlank { "PAKET_B" },
                stages = stages,
                topicTag = topicTag,
                explanation = explanation,
            )
        }
    }
}

fun ShibaPetStatusDto.toDomain() = ShibaPetStatus(
    petName = petName,
    petStage = petStage.coerceIn(1, maxStage.coerceAtLeast(6)),
    stageTitle = stageTitle,
    foodPoints = foodPoints.coerceAtLeast(0),
    pointsToNextStage = pointsToNextStage.coerceAtLeast(0),
    energyRequired = energyRequired.coerceAtLeast(1),
    studentXp = studentXp.coerceAtLeast(0L),
    xpRequiredNext = xpRequiredNext.coerceAtLeast(1L),
    xpRemainingNext = xpRemainingNext.coerceAtLeast(0L),
    evolutionProgressPercent = evolutionProgressPercent.coerceIn(0, 100),
    maxStage = maxStage.coerceAtLeast(6),
    readyToEvolve = readyToEvolve,
    evolveRequirementHint = evolveRequirementHint,
    totalFed = totalFed.coerceAtLeast(0),
    biscuits = biscuits.coerceAtLeast(0),
    fiftyFiftyTokens = fiftyFiftyTokens.coerceAtLeast(0),
)

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
        roundNumber = roundNumber,
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
        petStatus = petStatus?.toDomain(),
        chestClaimedToday = chestClaimedToday,
        availableModes = availableModes.map { it.toDomain() },
        leaderboard = leaderboard.map { it.toDomain() },
    )
}

fun SubmitDailyQuestResponseDto.toDomain() = DailyQuestSubmitResult(
    awarded = awarded,
    alreadyCompleted = alreadyCompleted,
    xpEarned = xpEarned,
    biscuitsEarned = biscuitsEarned,
    scorePercent = scorePercent,
    totalXp = totalXp,
    level = level.coerceAtLeast(1),
    streakDays = streakDays.coerceAtLeast(1),
    leveledUp = leveledUp,
    isPreviewAccount = isPreviewAccount,
    petStatus = petStatus?.toDomain(),
    message = message,
)

fun FeedPetResponseDto.toDomain() = FeedPetResult(
    fed = fed,
    evolved = evolved,
    bonusXpEarned = bonusXpEarned,
    totalXp = totalXp,
    level = level.coerceAtLeast(1),
    petStatus = petStatus?.toDomain(),
    message = message,
)

fun DailyChestResponseDto.toDomain() = DailyChestResult(
    opened = opened,
    alreadyClaimed = alreadyClaimed,
    rewardType = rewardType,
    rewardAmount = rewardAmount,
    rewardLabel = rewardLabel,
    totalXp = totalXp,
    level = level.coerceAtLeast(1),
    petStatus = petStatus?.toDomain(),
    message = message,
)
