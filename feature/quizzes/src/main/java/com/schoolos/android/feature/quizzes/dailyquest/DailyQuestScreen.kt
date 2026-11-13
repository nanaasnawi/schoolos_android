package com.schoolos.android.feature.quizzes.dailyquest

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.schoolos.android.core.designsystem.ConfettiParticleEffect
import com.schoolos.android.core.designsystem.LocalIsDarkTheme
import com.schoolos.android.domain.model.DailyQuest
import com.schoolos.android.domain.model.DailyQuestChallenge
import com.schoolos.android.domain.model.DailyQuestGameMode
import com.schoolos.android.domain.model.DailyQuestLeaderboardEntry
import com.schoolos.android.domain.model.DailyQuestSubmitResult

private data class GameModeVisual(
    val icon: ImageVector,
    val primaryColor: Color,
    val secondaryColor: Color,
    val darkLipColor: Color,
    val badgeText: String,
)

private fun resolveModeVisual(mode: DailyQuestGameMode): GameModeVisual = when (mode) {
    DailyQuestGameMode.MAP_PIN_HUNT -> GameModeVisual(
        icon = Icons.Default.Map,
        primaryColor = Color(0xFF10B981),
        secondaryColor = Color(0xFF0D9488),
        darkLipColor = Color(0xFF047857),
        badgeText = "Peta & Hotspot",
    )
    DailyQuestGameMode.FACT_OR_MYTH -> GameModeVisual(
        icon = Icons.Default.Bolt,
        primaryColor = Color(0xFF0EA5E9),
        secondaryColor = Color(0xFF2563EB),
        darkLipColor = Color(0xFF0369A1),
        badgeText = "Geser Kiri/Kanan",
    )
    DailyQuestGameMode.SEQUENCE_ORDER -> GameModeVisual(
        icon = Icons.Default.AutoAwesome,
        primaryColor = Color(0xFF8B5CF6),
        secondaryColor = Color(0xFFD946EF),
        darkLipColor = Color(0xFF6D28D9),
        badgeText = "Susun Langkah",
    )
    DailyQuestGameMode.CASE_DETECTIVE -> GameModeVisual(
        icon = Icons.Default.Search,
        primaryColor = Color(0xFFF43F5E),
        secondaryColor = Color(0xFFEA580C),
        darkLipColor = Color(0xFFBE123C),
        badgeText = "Komik Cerita",
    )
    DailyQuestGameMode.CONCEPT_GUESS -> GameModeVisual(
        icon = Icons.Default.Key,
        primaryColor = Color(0xFFF59E0B),
        secondaryColor = Color(0xFFF43F5E),
        darkLipColor = Color(0xFFB45309),
        badgeText = "Tebak Istilah",
    )
}

private fun formatQuestTimer(sec: Int): String {
    val mins = sec / 60
    val rem = sec % 60
    return "%02d:%02d".format(mins, rem)
}

@Composable
fun DailyQuestScreen(
    onBack: () -> Unit,
    viewModel: DailyQuestViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current

    // Dedicated crisp sunlit Game Palette matching Mockup References 1-4
    val pageBg = Color(0xFFEDF3FA)
    val surfaceColor = Color.White
    val elevatedColor = Color(0xFFF7FAFE)
    val borderColor = Color(0xFFE2E8F0)
    val textPrimary = Color(0xFF0F172A)
    val textSecondary = Color(0xFF475569)

    val activeVisual = resolveModeVisual(state.quest?.gameMode ?: state.selectedMode ?: DailyQuestGameMode.MAP_PIN_HUNT)

    Scaffold(
        containerColor = pageBg,
        topBar = {
            MockupArcadeHudTopBar(
                quest = state.quest,
                stars = state.hearts,
                liveScore = state.liveScore,
                currentIdx = state.currentIndex,
                totalChallenges = state.quest?.challenges?.size ?: 0,
                hasFeedback = state.feedback != null,
                soundEnabled = state.soundEnabled,
                onBack = onBack,
                onToggleSound = { viewModel.toggleSound() },
                surfaceColor = surfaceColor,
                elevatedColor = elevatedColor,
                borderColor = borderColor,
                textPrimary = textPrimary,
                textSecondary = textSecondary,
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // ── 1. CLEAN MODE PILL SWITCHER ──
                val modes = state.quest?.availableModes.orEmpty()
                if (modes.isNotEmpty()) {
                    item {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            items(modes) { modeInfo ->
                                val isSel = state.selectedMode == modeInfo.code
                                val modeVis = resolveModeVisual(modeInfo.code)
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = if (isSel) Color(0xFF0EA5E9) else Color.White,
                                    border = BorderStroke(1.dp, if (isSel) Color(0xFF0284C7) else borderColor),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            viewModel.loadQuest(modeInfo.code)
                                        },
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                                    ) {
                                        Icon(
                                            imageVector = modeVis.icon,
                                            contentDescription = null,
                                            tint = if (isSel) Color.White else modeVis.primaryColor,
                                            modifier = Modifier.size(13.dp),
                                        )
                                        Text(
                                            text = modeInfo.label,
                                            color = if (isSel) Color.White else textPrimary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                        )
                                        if (modeInfo.completedToday) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = if (isSel) Color.White else Color(0xFF10B981),
                                                modifier = Modifier.size(12.dp),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ── 2. LOADING / ERROR STATE ──
                if (state.isLoading) {
                    item {
                        ArcadePanel(surfaceColor = surfaceColor, borderColor = borderColor) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                CircularProgressIndicator(color = activeVisual.primaryColor, strokeWidth = 3.dp)
                                Text(
                                    text = "LOADING GAME STAGE...",
                                    color = textPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                )
                                Text(
                                    text = "Menyiapkan peta & tantangan interaktif hari ini",
                                    color = textSecondary,
                                    fontSize = 12.sp,
                                )
                            }
                        }
                    }
                } else if (state.errorMessage != null) {
                    item {
                        ArcadePanel(surfaceColor = surfaceColor, borderColor = Color(0xFFF43F5E).copy(alpha = 0.5f)) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Icon(Icons.Default.Cancel, contentDescription = null, tint = Color(0xFFF43F5E), modifier = Modifier.size(38.dp))
                                Text(
                                    text = state.errorMessage ?: "Gagal memuat game",
                                    color = textPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                )
                                ArcadePushButton(
                                    text = "COBA LAGI",
                                    topColor = Color(0xFF0EA5E9),
                                    lipColor = Color(0xFF0369A1),
                                    onClick = { viewModel.loadQuest(state.selectedMode) },
                                )
                            }
                        }
                    }
                } else if (state.quest != null) {
                    val quest = state.quest!!

                    // ── 3. VICTORY / SUBMITTING / VISUAL STAGE ARENA ──
                    when {
                        state.submitResult != null -> {
                            item {
                                VictoryLootCard(
                                    result = state.submitResult!!,
                                    correctCount = state.correctCount,
                                    totalCount = quest.challenges.size.coerceAtLeast(1),
                                    maxCombo = state.maxCombo,
                                    surfaceColor = surfaceColor,
                                    elevatedColor = elevatedColor,
                                    borderColor = borderColor,
                                    textPrimary = textPrimary,
                                    textSecondary = textSecondary,
                                    onReplay = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        viewModel.replayQuest()
                                    },
                                    onSwitchMode = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        viewModel.switchToNextMode()
                                    },
                                )
                            }
                        }
                        state.isSubmitting -> {
                            item {
                                ArcadePanel(surfaceColor = surfaceColor, borderColor = borderColor) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(12.dp),
                                    ) {
                                        CircularProgressIndicator(color = Color(0xFFF59E0B))
                                        Text(
                                            text = "Mengklaim XP Loot & Menyimpan Skor...",
                                            color = textPrimary,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Black,
                                        )
                                    }
                                }
                            }
                        }
                        quest.challenges.isEmpty() -> {
                            item {
                                ArcadePanel(surfaceColor = surfaceColor, borderColor = borderColor) {
                                    Text(
                                        text = "Belum ada tantangan tersedia untuk mode ini.",
                                        color = textSecondary,
                                        fontSize = 13.sp,
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.Center,
                                    )
                                }
                            }
                        }
                        else -> {
                            val currentChallenge = quest.challenges.getOrNull(state.currentIndex)
                            item {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(22.dp),
                                    color = elevatedColor,
                                    border = BorderStroke(1.5.dp, Color.White),
                                    shadowElevation = 4.dp,
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp),
                                    ) {
                                        // Mini-Game Stage Content (Direct Clean Mockup Stage!)
                                        when (currentChallenge) {
                                            is DailyQuestChallenge.MapPinHunt -> {
                                                MapPinHuntArenaCompose(
                                                    challenge = currentChallenge,
                                                    disabled = state.feedback != null,
                                                    onWrongTap = {
                                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                        viewModel.onWrongStageTap()
                                                    },
                                                    onCompleted = { msg ->
                                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                        viewModel.completeInteractiveStage(msg, currentChallenge.explanation)
                                                    },
                                                )
                                            }
                                            is DailyQuestChallenge.FactOrMyth -> {
                                                SwipeCardArenaCompose(
                                                    challenge = currentChallenge,
                                                    feedback = state.feedback,
                                                    onSwipeAnswer = { isFact ->
                                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                        viewModel.answerFactOrMyth(isFact)
                                                    },
                                                )
                                            }
                                            is DailyQuestChallenge.SequenceOrder -> {
                                                DualColumnStepBuilderCompose(
                                                    challenge = currentChallenge,
                                                    shuffledPool = state.orderedSteps,
                                                    disabled = state.feedback != null,
                                                    onWrongTap = {
                                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                        viewModel.onWrongStageTap()
                                                    },
                                                    onAllCompleted = {
                                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                        viewModel.completeInteractiveStage(
                                                            "Urutan langkah 100% tepat! Logikamu tajam.",
                                                            currentChallenge.explanation,
                                                        )
                                                    },
                                                )
                                            }
                                            is DailyQuestChallenge.CaseDetective -> {
                                                StoryComicArenaCompose(
                                                    challenge = currentChallenge,
                                                    feedback = state.feedback,
                                                    onChooseOption = { idx ->
                                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                        viewModel.answerCaseDetective(idx)
                                                    },
                                                )
                                            }
                                            is DailyQuestChallenge.ConceptGuess -> {
                                                ConceptGuessArena(
                                                    challenge = currentChallenge,
                                                    revealedClues = state.revealedClues,
                                                    guessInput = state.guessInput,
                                                    feedback = state.feedback,
                                                    surfaceColor = surfaceColor,
                                                    elevatedColor = elevatedColor,
                                                    borderColor = borderColor,
                                                    textPrimary = textPrimary,
                                                    textSecondary = textSecondary,
                                                    onRevealNextClue = { viewModel.revealNextClue() },
                                                    onGuessChange = { viewModel.updateGuessInput(it) },
                                                    onSubmitGuess = {
                                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                        viewModel.submitConceptGuess()
                                                    },
                                                    onSkipGuess = { viewModel.skipConceptGuess() },
                                                )
                                            }
                                            null -> Unit
                                        }

                                        // Feedback Explanation + Next Stage CTA
                                        val fb = state.feedback
                                        AnimatedVisibility(
                                            visible = fb != null,
                                            enter = fadeIn() + scaleIn(initialScale = 0.96f),
                                        ) {
                                            if (fb != null) {
                                                val fbColor = if (fb.isCorrect) Color(0xFF10B981) else Color(0xFFF43F5E)
                                                val nextColor = if (fb.isCorrect) Color(0xFF10B981) else Color(0xFF0EA5E9)
                                                val nextLip = if (fb.isCorrect) Color(0xFF047857) else Color(0xFF0369A1)

                                                Surface(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    shape = RoundedCornerShape(18.dp),
                                                    color = fbColor.copy(alpha = 0.12f),
                                                    border = BorderStroke(1.5.dp, fbColor.copy(alpha = 0.45f)),
                                                ) {
                                                    Column(
                                                        modifier = Modifier.padding(14.dp),
                                                        verticalArrangement = Arrangement.spacedBy(10.dp),
                                                    ) {
                                                        Row(
                                                            verticalAlignment = Alignment.Top,
                                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                        ) {
                                                            Icon(
                                                                imageVector = if (fb.isCorrect) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                                                contentDescription = null,
                                                                tint = fbColor,
                                                                modifier = Modifier.size(22.dp),
                                                            )
                                                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                                Text(
                                                                    text = fb.message,
                                                                    color = fbColor,
                                                                    fontSize = 14.sp,
                                                                    fontWeight = FontWeight.Black,
                                                                )
                                                                val expl = fb.explanation
                                                                if (!expl.isNullOrBlank()) {
                                                                    Text(
                                                                        text = expl,
                                                                        color = textSecondary,
                                                                        fontSize = 12.sp,
                                                                        lineHeight = 17.sp,
                                                                    )
                                                                }
                                                            }
                                                        }

                                                        ArcadePushButton(
                                                            text = if (state.currentIndex + 1 >= quest.challenges.size) {
                                                                "LIHAT HASIL & HADIAH"
                                                            } else {
                                                                "STAGE BERIKUTNYA"
                                                            },
                                                            icon = Icons.AutoMirrored.Filled.ArrowForward,
                                                            topColor = nextColor,
                                                            lipColor = nextLip,
                                                            onClick = {
                                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                                viewModel.nextStep()
                                                            },
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ── 5. TOP-10 ARENA PODIUM LEADERBOARD ──
                    item {
                        LeaderboardArenaPanel(
                            entries = quest.leaderboard,
                            surfaceColor = surfaceColor,
                            elevatedColor = elevatedColor,
                            borderColor = borderColor,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary,
                        )
                    }
                }
            }

            if (state.submitResult != null) {
                ConfettiParticleEffect(
                    modifier = Modifier.fillMaxSize(),
                    particleCount = 100,
                    durationMs = 2800,
                )
            }
        }
    }
}

@Composable
private fun MockupArcadeHudTopBar(
    quest: DailyQuest?,
    stars: Int,
    liveScore: Int,
    currentIdx: Int,
    totalChallenges: Int,
    hasFeedback: Boolean,
    soundEnabled: Boolean,
    onBack: () -> Unit,
    onToggleSound: () -> Unit,
    surfaceColor: Color,
    elevatedColor: Color,
    borderColor: Color,
    textPrimary: Color,
    textSecondary: Color,
) {
    val progressFrac = if (totalChallenges > 0) {
        ((currentIdx + if (hasFeedback) 1 else 0).toFloat() / totalChallenges.toFloat()).coerceIn(0.10f, 1f)
    } else 0.15f

    val headerSubject = when (quest?.gameMode) {
        DailyQuestGameMode.SEQUENCE_ORDER -> "Matematika"
        DailyQuestGameMode.MAP_PIN_HUNT -> "Geografi & Sejarah"
        else -> quest?.subjectName ?: "Game Belajar"
    }

    Surface(
        color = Color.White,
        border = BorderStroke(1.dp, borderColor),
        shadowElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Top Utility Row: Back + "Akselerasi Edu CBT - <Mapel>" + Streak + Sound
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = elevatedColor,
                        border = BorderStroke(1.dp, borderColor),
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable { onBack() },
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = textPrimary,
                            modifier = Modifier.padding(6.dp).size(16.dp),
                        )
                    }
                    Text(
                        text = "Akselerasi Edu CBT - $headerSubject",
                        color = textPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (quest != null) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFF59E0B).copy(alpha = 0.14f),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                            ) {
                                Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(13.dp))
                                Text("${quest.streakDays}d", color = Color(0xFFF59E0B), fontSize = 10.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = borderColor.copy(alpha = 0.35f),
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onToggleSound() },
                    ) {
                        Icon(
                            imageVector = if (soundEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                            contentDescription = "Sound",
                            tint = if (soundEnabled) Color(0xFF0EA5E9) else Color(0xFFF43F5E),
                            modifier = Modifier.padding(7.dp).size(17.dp),
                        )
                    }
                }
            }

            // Mockup Game HUD Row: [⭐⭐⭐]  [Green Capsule Progress]  [SCORE: XX + Shiba]
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                    for (i in 0 until 3) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = if (i < stars) Color(0xFFFACC15) else borderColor,
                            modifier = Modifier.size(19.dp),
                        )
                    }
                }

                // Green Capsule Progress Bar
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(14.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFFDCE4EE))
                        .padding(2.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progressFrac)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(50))
                            .background(Brush.horizontalGradient(listOf(Color(0xFF4ADE80), Color(0xFF16A34A)))),
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "SCORE: $liveScore",
                        color = textPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                    )
                    ShibaMascotAvatar(size = 34.dp, happy = stars > 0)
                }
            }
        }
    }
}

@Composable
private fun GameModeCartridgeCard(
    label: String,
    isSelected: Boolean,
    isFeaturedToday: Boolean,
    isCompletedToday: Boolean,
    visual: GameModeVisual,
    surfaceColor: Color,
    borderColor: Color,
    textPrimary: Color,
    textSecondary: Color,
    onClick: () -> Unit,
) {
    val activeBorder = if (isSelected) visual.primaryColor else borderColor
    val lipColor = if (isSelected) visual.darkLipColor else borderColor

    Surface(
        modifier = Modifier
            .width(136.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        shape = RoundedCornerShape(16.dp),
        color = lipColor,
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 3.dp),
            shape = RoundedCornerShape(16.dp),
            color = surfaceColor,
            border = BorderStroke(1.5.dp, activeBorder),
        ) {
            Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(visual.primaryColor.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = visual.icon,
                            contentDescription = null,
                            tint = visual.primaryColor,
                            modifier = Modifier.size(15.dp),
                        )
                    }

                    if (isCompletedToday) {
                        Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFF10B981).copy(alpha = 0.16f)) {
                            Text("DONE", color = Color(0xFF10B981), fontSize = 8.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
                        }
                    } else if (isFeaturedToday) {
                        Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFFF59E0B)) {
                            Text("★ UTAMA", color = Color(0xFF0F172A), fontSize = 8.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
                        }
                    }
                }

                Text(
                    text = label,
                    color = textPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = visual.badgeText,
                    color = textSecondary,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun ConceptGuessArena(
    challenge: DailyQuestChallenge.ConceptGuess,
    revealedClues: Int,
    guessInput: String,
    feedback: QuestFeedback?,
    surfaceColor: Color,
    elevatedColor: Color,
    borderColor: Color,
    textPrimary: Color,
    textSecondary: Color,
    onRevealNextClue: () -> Unit,
    onGuessChange: (String) -> Unit,
    onSubmitGuess: () -> Unit,
    onSkipGuess: () -> Unit,
) {
    val shownCount = revealedClues.coerceIn(1, challenge.clues.size.coerceAtLeast(1))
    val tag = challenge.topicTag
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = elevatedColor,
            border = BorderStroke(1.5.dp, borderColor),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFFF59E0B).copy(alpha = 0.16f)) {
                        Text(
                            text = (tag ?: "TEBAK ISTILAH").uppercase(),
                            color = Color(0xFFF59E0B),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                    Text(
                        text = "$shownCount/${challenge.clues.size} Petunjuk",
                        color = textSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    challenge.clues.take(shownCount).forEach { clue ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            color = surfaceColor,
                            border = BorderStroke(1.dp, borderColor),
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.Top,
                            ) {
                                Icon(Icons.Default.Lightbulb, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(16.dp))
                                Text(clue, color = textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold, lineHeight = 18.sp)
                            }
                        }
                    }
                }

                if (feedback == null && shownCount < challenge.clues.size) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFF59E0B).copy(alpha = 0.16f),
                        border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.4f)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onRevealNextClue() },
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(14.dp))
                            Text("Buka Petunjuk Berikutnya", color = Color(0xFFF59E0B), fontSize = 11.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }

        if (feedback == null) {
            OutlinedTextField(
                value = guessInput,
                onValueChange = onGuessChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("Ketik 1 kata / istilah jawaban...") },
                shape = RoundedCornerShape(16.dp),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onSubmitGuess() }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFF59E0B),
                    unfocusedBorderColor = borderColor,
                    focusedTextColor = textPrimary,
                    unfocusedTextColor = textPrimary,
                    focusedContainerColor = elevatedColor,
                    unfocusedContainerColor = elevatedColor,
                ),
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    ArcadePushButton(
                        text = "BUKA BRANKAS ⚡",
                        topColor = Color(0xFFF59E0B),
                        lipColor = Color(0xFFB45309),
                        textColor = Color(0xFF0F172A),
                        onClick = onSubmitGuess,
                    )
                }
                ArcadePushButton(
                    text = "Lewati",
                    topColor = elevatedColor,
                    lipColor = borderColor,
                    textColor = textSecondary,
                    onClick = onSkipGuess,
                )
            }
        }
    }
}

@Composable
private fun VictoryLootCard(
    result: DailyQuestSubmitResult,
    correctCount: Int,
    totalCount: Int,
    maxCombo: Int,
    surfaceColor: Color,
    elevatedColor: Color,
    borderColor: Color,
    textPrimary: Color,
    textSecondary: Color,
    onReplay: () -> Unit,
    onSwitchMode: () -> Unit,
) {
    val grade = when {
        result.scorePercent >= 100 -> "S"
        result.scorePercent >= 80 -> "A"
        result.scorePercent >= 60 -> "B"
        else -> "C"
    }
    val rankLabel = when {
        result.scorePercent >= 100 -> "LEGENDARY PERFECT!"
        result.scorePercent >= 80 -> "SHARP CHALLENGER!"
        result.scorePercent >= 60 -> "SOLID QUEST CLEAR!"
        else -> "QUEST COMPLETED!"
    }
    val badgeTop = when {
        result.scorePercent >= 100 -> Color(0xFFFACC15)
        result.scorePercent >= 80 -> Color(0xFF34D399)
        result.scorePercent >= 60 -> Color(0xFF38BDF8)
        else -> Color(0xFFA78BFA)
    }
    val badgeBottom = when {
        result.scorePercent >= 100 -> Color(0xFFF97316)
        result.scorePercent >= 80 -> Color(0xFF0D9488)
        result.scorePercent >= 60 -> Color(0xFF2563EB)
        else -> Color(0xFF7C3AED)
    }

    ArcadePanel(surfaceColor = surfaceColor, borderColor = borderColor) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(84.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Brush.linearGradient(listOf(badgeTop, badgeBottom))),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("RANK", color = Color.White.copy(alpha = 0.9f), fontSize = 10.sp, fontWeight = FontWeight.Black)
                    Text(grade, color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Black, lineHeight = 38.sp)
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(rankLabel, color = badgeTop, fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                Text(
                    text = if (result.xpEarned > 0) "Yay! +${result.xpEarned} XP Masuk Tas Loot!" else "Latihan Misi Harian Tuntas!",
                    color = textPrimary,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                )
                Text(result.message, color = textSecondary, fontSize = 12.sp, textAlign = TextAlign.Center, lineHeight = 17.sp)
            }

            if (result.leveledUp) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFF59E0B).copy(alpha = 0.16f),
                    border = BorderStroke(1.5.dp, Color(0xFFF59E0B).copy(alpha = 0.45f)),
                ) {
                    Text(
                        text = "🎉 LEVEL UP! Kamu naik ke Level ${result.level}!",
                        color = Color(0xFFF59E0B),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }

            if (result.isPreviewAccount) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF0EA5E9).copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, Color(0xFF0EA5E9).copy(alpha = 0.35f)),
                ) {
                    Text(
                        text = "Mode Pratinjau Guru/Kepala Sekolah: Permainan berhasil diuji tanpa memodifikasi XP siswa.",
                        color = Color(0xFF0EA5E9),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(10.dp),
                    )
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LootMetricTile("AKURASI", "${result.scorePercent}%", Color(0xFF10B981), elevatedColor, borderColor, textSecondary, Modifier.weight(1f))
                LootMetricTile("BENAR", "$correctCount/$totalCount", textPrimary, elevatedColor, borderColor, textSecondary, Modifier.weight(1f))
                LootMetricTile("COMBO", "${maxCombo}x", Color(0xFFF59E0B), elevatedColor, borderColor, textSecondary, Modifier.weight(1f))
                LootMetricTile("XP LOOT", "+${result.xpEarned}", Color(0xFF0EA5E9), elevatedColor, borderColor, textSecondary, Modifier.weight(1f))
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                ArcadePushButton(
                    text = "MAIN LAGI (LATIHAN)",
                    icon = Icons.Default.Refresh,
                    topColor = elevatedColor,
                    lipColor = borderColor,
                    textColor = textPrimary,
                    onClick = onReplay,
                )
                ArcadePushButton(
                    text = "PINDAH MODE GAME LAIN",
                    icon = Icons.Default.SportsEsports,
                    topColor = Color(0xFF0EA5E9),
                    lipColor = Color(0xFF0369A1),
                    onClick = onSwitchMode,
                )
            }
        }
    }
}

@Composable
private fun LootMetricTile(
    label: String,
    value: String,
    valueColor: Color,
    bgColor: Color,
    borderColor: Color,
    labelColor: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = bgColor,
        border = BorderStroke(1.dp, borderColor),
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(text = label, color = labelColor, fontSize = 9.sp, fontWeight = FontWeight.Black)
            Text(text = value, color = valueColor, fontSize = 18.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun LeaderboardArenaPanel(
    entries: List<DailyQuestLeaderboardEntry>,
    surfaceColor: Color,
    elevatedColor: Color,
    borderColor: Color,
    textPrimary: Color,
    textSecondary: Color,
) {
    ArcadePanel(surfaceColor = surfaceColor, borderColor = borderColor) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF59E0B).copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(19.dp))
                    }
                    Column {
                        Text("Arena Peringkat Hari Ini", color = textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Black)
                        Text("Pemain tercepat & paling akurat di sekolah", color = textSecondary, fontSize = 11.sp)
                    }
                }
                Surface(shape = RoundedCornerShape(10.dp), color = elevatedColor, border = BorderStroke(1.dp, borderColor)) {
                    Text("Top ${entries.size}", color = textSecondary, fontSize = 10.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                }
            }

            if (entries.isEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = elevatedColor,
                    border = BorderStroke(1.dp, borderColor),
                ) {
                    Text(
                        text = "Belum ada yang menuntaskan mode ini hari ini. Jadilah juara #1! 🏆",
                        color = textSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    entries.forEachIndexed { idx, row ->
                        val rankColor = when (idx) {
                            0 -> Color(0xFFF59E0B)
                            1 -> Color(0xFF94A3B8)
                            2 -> Color(0xFFB45309)
                            else -> borderColor
                        }
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            color = if (row.isMe) Color(0xFF0EA5E9).copy(alpha = 0.12f) else elevatedColor,
                            border = BorderStroke(
                                width = if (row.isMe) 1.5.dp else 1.dp,
                                color = if (row.isMe) Color(0xFF0EA5E9).copy(alpha = 0.55f) else borderColor,
                            ),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(RoundedCornerShape(9.dp))
                                            .background(rankColor),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        if (idx == 0) {
                                            Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                        } else {
                                            Text("${idx + 1}", color = if (idx < 3) Color.White else textPrimary, fontSize = 12.sp, fontWeight = FontWeight.Black)
                                        }
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text(row.studentName, color = textPrimary, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            if (row.isMe) {
                                                Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFF0EA5E9)) {
                                                    Text("KAMU", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
                                                }
                                            }
                                        }
                                        Text("${row.className} • ${row.durationSeconds} detik", color = textSecondary, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("${row.scorePercent}%", color = Color(0xFF10B981), fontSize = 13.sp, fontWeight = FontWeight.Black)
                                    Text("+${row.xpEarned} XP", color = Color(0xFF0EA5E9), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ArcadePanel(
    surfaceColor: Color,
    borderColor: Color,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = borderColor,
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp),
            shape = RoundedCornerShape(24.dp),
            color = surfaceColor,
            border = BorderStroke(1.5.dp, borderColor),
        ) {
            Box(modifier = Modifier.padding(16.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun ArcadePushButton(
    text: String,
    topColor: Color,
    lipColor: Color,
    textColor: Color = Color.White,
    icon: ImageVector? = null,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = lipColor,
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp),
            shape = RoundedCornerShape(16.dp),
            color = topColor,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 13.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (icon != null) {
                    Icon(imageVector = icon, contentDescription = null, tint = textColor, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(text = text, color = textColor, fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 0.5.sp)
            }
        }
    }
}
