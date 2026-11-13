package com.schoolos.android.feature.quizzes.dailyquest

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.draw.scale
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
    DailyQuestGameMode.FACT_OR_MYTH -> GameModeVisual(
        icon = Icons.Default.Bolt,
        primaryColor = Color(0xFF0EA5E9),
        secondaryColor = Color(0xFF2563EB),
        darkLipColor = Color(0xFF0369A1),
        badgeText = "Kilat 60 Detik",
    )
    DailyQuestGameMode.CASE_DETECTIVE -> GameModeVisual(
        icon = Icons.Default.Search,
        primaryColor = Color(0xFF10B981),
        secondaryColor = Color(0xFF0D9488),
        darkLipColor = Color(0xFF047857),
        badgeText = "Studi Kasus",
    )
    DailyQuestGameMode.SEQUENCE_ORDER -> GameModeVisual(
        icon = Icons.Default.AutoAwesome,
        primaryColor = Color(0xFF8B5CF6),
        secondaryColor = Color(0xFFD946EF),
        darkLipColor = Color(0xFF6D28D9),
        badgeText = "Rangkai Logika",
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
    val isDark = LocalIsDarkTheme.current
    val haptic = LocalHapticFeedback.current

    val pageBg = if (isDark) Color(0xFF0B0E14) else Color(0xFFF1F5F9)
    val surfaceColor = if (isDark) Color(0xFF131823) else Color.White
    val elevatedColor = if (isDark) Color(0xFF1C2333) else Color(0xFFF8FAFC)
    val borderColor = if (isDark) Color(0xFF273147) else Color(0xFFE2E8F0)
    val textPrimary = if (isDark) Color.White else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    val activeVisual = resolveModeVisual(state.quest?.gameMode ?: state.selectedMode ?: DailyQuestGameMode.FACT_OR_MYTH)

    Scaffold(
        containerColor = pageBg,
        topBar = {
            GameHudTopBar(
                quest = state.quest,
                hearts = state.hearts,
                soundEnabled = state.soundEnabled,
                onBack = onBack,
                onToggleSound = { viewModel.toggleSound() },
                surfaceColor = surfaceColor,
                borderColor = borderColor,
                textPrimary = textPrimary,
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
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                // ── 1. 4 MINI-GAME CARTRIDGE SELECTOR ──
                val modes = state.quest?.availableModes.orEmpty()
                if (modes.isNotEmpty()) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                modes.take(2).forEach { modeInfo ->
                                    GameModeCartridgeCard(
                                        modifier = Modifier.weight(1f),
                                        label = modeInfo.label,
                                        isSelected = state.selectedMode == modeInfo.code,
                                        isFeaturedToday = modeInfo.isFeaturedToday,
                                        isCompletedToday = modeInfo.completedToday,
                                        visual = resolveModeVisual(modeInfo.code),
                                        surfaceColor = surfaceColor,
                                        borderColor = borderColor,
                                        textPrimary = textPrimary,
                                        textSecondary = textSecondary,
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            viewModel.loadQuest(modeInfo.code)
                                        },
                                    )
                                }
                            }
                            if (modes.size > 2) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    modes.drop(2).take(2).forEach { modeInfo ->
                                        GameModeCartridgeCard(
                                            modifier = Modifier.weight(1f),
                                            label = modeInfo.label,
                                            isSelected = state.selectedMode == modeInfo.code,
                                            isFeaturedToday = modeInfo.isFeaturedToday,
                                            isCompletedToday = modeInfo.completedToday,
                                            visual = resolveModeVisual(modeInfo.code),
                                            surfaceColor = surfaceColor,
                                            borderColor = borderColor,
                                            textPrimary = textPrimary,
                                            textSecondary = textSecondary,
                                            onClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                viewModel.loadQuest(modeInfo.code)
                                            },
                                        )
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
                                    text = "Menyiapkan tantangan dinamis kelasmu hari ini",
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

                    // ── 3. HERO GRADIENT MISSION BANNER ──
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            color = activeVisual.darkLipColor,
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(bottom = 4.dp)
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(activeVisual.primaryColor, activeVisual.secondaryColor)
                                        )
                                    )
                                    .padding(16.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Surface(
                                                shape = RoundedCornerShape(50),
                                                color = Color.Black.copy(alpha = 0.25f),
                                            ) {
                                                Text(
                                                    text = quest.dayLabel.uppercase(),
                                                    color = Color.White,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Black,
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                                )
                                            }
                                            Surface(
                                                shape = RoundedCornerShape(50),
                                                color = Color.White.copy(alpha = 0.22f),
                                            ) {
                                                Text(
                                                    text = quest.subjectName,
                                                    color = Color.White,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                )
                                            }
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(50),
                                            color = Color(0xFFFACC15),
                                        ) {
                                            Text(
                                                text = "+${quest.xpReward} XP",
                                                color = Color(0xFF0F172A),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Black,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                            )
                                        }
                                    }

                                    Text(
                                        text = quest.title,
                                        color = Color.White,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Black,
                                        lineHeight = 22.sp,
                                    )
                                    Text(
                                        text = quest.subtitle,
                                        color = Color.White.copy(alpha = 0.92f),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        lineHeight = 17.sp,
                                    )
                                }
                            }
                        }
                    }

                    // ── 4. VICTORY / SUBMITTING / ACTIVE STAGE ARENA ──
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
                                ArcadePanel(surfaceColor = surfaceColor, borderColor = borderColor) {
                                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                        // Stage Progress + Combo + Timer
                                        StageProgressHeader(
                                            currentIdx = state.currentIndex,
                                            total = quest.challenges.size,
                                            combo = state.combo,
                                            hearts = state.hearts,
                                            elapsedSeconds = state.elapsedSeconds,
                                            accentColor = activeVisual.primaryColor,
                                            elevatedColor = elevatedColor,
                                            borderColor = borderColor,
                                            textPrimary = textPrimary,
                                            textSecondary = textSecondary,
                                        )

                                        // Mini-Game Stage Content
                                        when (currentChallenge) {
                                            is DailyQuestChallenge.FactOrMyth -> {
                                                FactOrMythArena(
                                                    challenge = currentChallenge,
                                                    feedback = state.feedback,
                                                    elevatedColor = elevatedColor,
                                                    borderColor = borderColor,
                                                    textPrimary = textPrimary,
                                                    textSecondary = textSecondary,
                                                    onChoose = { isFact ->
                                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                        viewModel.answerFactOrMyth(isFact)
                                                    },
                                                )
                                            }
                                            is DailyQuestChallenge.CaseDetective -> {
                                                CaseDetectiveArena(
                                                    challenge = currentChallenge,
                                                    feedback = state.feedback,
                                                    surfaceColor = surfaceColor,
                                                    elevatedColor = elevatedColor,
                                                    borderColor = borderColor,
                                                    textPrimary = textPrimary,
                                                    textSecondary = textSecondary,
                                                    onChooseOption = { idx ->
                                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                        viewModel.answerCaseDetective(idx)
                                                    },
                                                )
                                            }
                                            is DailyQuestChallenge.SequenceOrder -> {
                                                SequenceOrderArena(
                                                    challenge = currentChallenge,
                                                    steps = state.orderedSteps,
                                                    swapSourceIndex = state.swapSourceIndex,
                                                    feedback = state.feedback,
                                                    surfaceColor = surfaceColor,
                                                    elevatedColor = elevatedColor,
                                                    borderColor = borderColor,
                                                    textPrimary = textPrimary,
                                                    textSecondary = textSecondary,
                                                    onTapCard = { idx ->
                                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                        viewModel.tapSequenceCard(idx)
                                                    },
                                                    onMove = { idx, delta ->
                                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                        viewModel.moveSequenceStep(idx, delta)
                                                    },
                                                    onSubmitSequence = {
                                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                        viewModel.checkSequenceOrder()
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
                                                                if (!fb.explanation.isNullOrBlank()) {
                                                                    Text(
                                                                        text = fb.explanation,
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

            // Native Confetti Burst on Quest Victory!
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
private fun GameHudTopBar(
    quest: DailyQuest?,
    hearts: Int,
    soundEnabled: Boolean,
    onBack: () -> Unit,
    onToggleSound: () -> Unit,
    surfaceColor: Color,
    borderColor: Color,
    textPrimary: Color,
) {
    Surface(
        color = surfaceColor,
        border = BorderStroke(1.dp, borderColor),
        shadowElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = borderColor.copy(alpha = 0.35f),
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onBack() },
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Kembali",
                        tint = textPrimary,
                        modifier = Modifier.padding(8.dp).size(18.dp),
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = borderColor.copy(alpha = 0.35f),
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onToggleSound() },
                ) {
                    Icon(
                        imageVector = if (soundEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                        contentDescription = if (soundEnabled) "Suara Aktif" else "Suara Mati",
                        tint = if (soundEnabled) Color(0xFF0EA5E9) else Color(0xFFF43F5E),
                        modifier = Modifier.padding(8.dp).size(18.dp),
                    )
                }
            }

            if (quest != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    // 3 Hearts
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF43F5E).copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, Color(0xFFF43F5E).copy(alpha = 0.3f)),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            for (i in 0 until 3) {
                                Icon(
                                    imageVector = if (i < hearts) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = null,
                                    tint = if (i < hearts) Color(0xFFF43F5E) else Color(0xFF94A3B8),
                                    modifier = Modifier.size(14.dp),
                                )
                            }
                        }
                    }

                    // Streak Flame
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF59E0B).copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.35f)),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalFireDepartment,
                                contentDescription = null,
                                tint = Color(0xFFF59E0B),
                                modifier = Modifier.size(14.dp),
                            )
                            Text(
                                text = "${quest.streakDays}d",
                                color = Color(0xFFF59E0B),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                            )
                        }
                    }

                    // Level + XP
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF0EA5E9).copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, Color(0xFF0EA5E9).copy(alpha = 0.35f)),
                    ) {
                        Text(
                            text = "L${quest.level} • ${quest.totalXp} XP",
                            color = Color(0xFF0EA5E9),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GameModeCartridgeCard(
    modifier: Modifier = Modifier,
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
        modifier = modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick,
        ),
        shape = RoundedCornerShape(18.dp),
        color = lipColor,
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 3.dp),
            shape = RoundedCornerShape(18.dp),
            color = surfaceColor,
            border = BorderStroke(1.5.dp, activeBorder),
        ) {
            Column(
                modifier = Modifier.padding(11.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(visual.primaryColor.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = visual.icon,
                            contentDescription = null,
                            tint = visual.primaryColor,
                            modifier = Modifier.size(16.dp),
                        )
                    }

                    if (isCompletedToday) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF10B981).copy(alpha = 0.16f),
                        ) {
                            Text(
                                text = "DONE",
                                color = Color(0xFF10B981),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    } else if (isFeaturedToday) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF59E0B),
                        ) {
                            Text(
                                text = "★ BONUS",
                                color = Color(0xFF0F172A),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }
                }

                Text(
                    text = label,
                    color = textPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = visual.badgeText,
                    color = textSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun StageProgressHeader(
    currentIdx: Int,
    total: Int,
    combo: Int,
    hearts: Int,
    elapsedSeconds: Int,
    accentColor: Color,
    elevatedColor: Color,
    borderColor: Color,
    textPrimary: Color,
    textSecondary: Color,
) {
    val pulseTransition = rememberInfiniteTransition(label = "combo")
    val pulseScale by pulseTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.07f,
        animationSpec = infiniteRepeatable(
            animation = tween(550),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse",
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = elevatedColor,
                    border = BorderStroke(1.dp, borderColor),
                ) {
                    Text(
                        text = "STAGE ${currentIdx + 1} / $total",
                        color = textPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    )
                }

                if (combo >= 2) {
                    Surface(
                        modifier = Modifier.scale(pulseScale),
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFF59E0B),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalFireDepartment,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(13.dp),
                            )
                            Text(
                                text = "${combo}x COMBO!",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                            )
                        }
                    }
                } else if (hearts == 0) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFF59E0B).copy(alpha = 0.16f),
                    ) {
                        Text(
                            text = "Mode Pemulihan",
                            color = Color(0xFFF59E0B),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        )
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = elevatedColor,
                border = BorderStroke(1.dp, borderColor),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(Icons.Default.Timer, contentDescription = null, tint = accentColor, modifier = Modifier.size(13.dp))
                    Text(
                        text = formatQuestTimer(elapsedSeconds),
                        color = textSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                    )
                }
            }
        }

        // Segmented stage progress bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            for (idx in 0 until total) {
                val segColor by animateColorAsState(
                    targetValue = when {
                        idx < currentIdx -> Color(0xFF10B981)
                        idx == currentIdx -> accentColor
                        else -> borderColor
                    },
                    label = "seg",
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp)
                        .clip(RoundedCornerShape(50))
                        .background(segColor)
                )
            }
        }
    }
}

@Composable
private fun FactOrMythArena(
    challenge: DailyQuestChallenge.FactOrMyth,
    feedback: QuestFeedback?,
    elevatedColor: Color,
    borderColor: Color,
    textPrimary: Color,
    textSecondary: Color,
    onChoose: (Boolean) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = elevatedColor,
            border = BorderStroke(1.5.dp, borderColor),
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                val topicTag = challenge.topicTag
                if (!topicTag.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF0EA5E9).copy(alpha = 0.15f),
                    ) {
                        Text(
                            text = topicTag.uppercase(),
                            color = Color(0xFF0EA5E9),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                }
                Text(
                    text = "\"${challenge.statement}\"",
                    color = textPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    lineHeight = 23.sp,
                )
                Text(
                    text = "Apakah pernyataan di atas Fakta Ilmiah atau Mitos?",
                    color = textSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            val mythCorrect = feedback != null && !challenge.isFact
            val mythWrongSelected = feedback != null && feedback.selectedBoolAnswer == false && challenge.isFact
            ArcadeDualChoiceSlab(
                modifier = Modifier.weight(1f),
                label = "MITOS / SALAH",
                icon = Icons.Default.Cancel,
                accent = when {
                    mythCorrect -> Color(0xFF10B981)
                    mythWrongSelected -> Color(0xFFF43F5E)
                    else -> Color(0xFFF43F5E)
                },
                dimmed = feedback != null && !mythCorrect && !mythWrongSelected,
                enabled = feedback == null,
                onClick = { onChoose(false) },
            )

            val factCorrect = feedback != null && challenge.isFact
            val factWrongSelected = feedback != null && feedback.selectedBoolAnswer == true && !challenge.isFact
            ArcadeDualChoiceSlab(
                modifier = Modifier.weight(1f),
                label = "FAKTA / BENAR",
                icon = Icons.Default.CheckCircle,
                accent = when {
                    factCorrect -> Color(0xFF10B981)
                    factWrongSelected -> Color(0xFFF43F5E)
                    else -> Color(0xFF10B981)
                },
                dimmed = feedback != null && !factCorrect && !factWrongSelected,
                enabled = feedback == null,
                onClick = { onChoose(true) },
            )
        }
    }
}

@Composable
private fun ArcadeDualChoiceSlab(
    modifier: Modifier = Modifier,
    label: String,
    icon: ImageVector,
    accent: Color,
    dimmed: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier.clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = accent.copy(alpha = if (dimmed) 0.25f else 0.75f),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp),
            shape = RoundedCornerShape(18.dp),
            color = accent.copy(alpha = if (dimmed) 0.08f else 0.16f),
            border = BorderStroke(1.5.dp, accent.copy(alpha = if (dimmed) 0.25f else 0.65f)),
        ) {
            Column(
                modifier = Modifier.padding(vertical = 18.dp, horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent.copy(alpha = if (dimmed) 0.45f else 1f),
                    modifier = Modifier.size(28.dp),
                )
                Text(
                    text = label,
                    color = accent.copy(alpha = if (dimmed) 0.5f else 1f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun CaseDetectiveArena(
    challenge: DailyQuestChallenge.CaseDetective,
    feedback: QuestFeedback?,
    surfaceColor: Color,
    elevatedColor: Color,
    borderColor: Color,
    textPrimary: Color,
    textSecondary: Color,
    onChooseOption: (Int) -> Unit,
) {
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
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF10B981).copy(alpha = 0.16f),
                ) {
                    Text(
                        text = (challenge.topicTag ?: "BERKAS KASUS").uppercase(),
                        color = Color(0xFF10B981),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }

                if (challenge.scenario.isNotBlank()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = surfaceColor,
                        border = BorderStroke(1.dp, borderColor),
                    ) {
                        Text(
                            text = challenge.scenario,
                            color = textSecondary,
                            fontSize = 13.sp,
                            lineHeight = 19.sp,
                            modifier = Modifier.padding(12.dp),
                        )
                    }
                }

                Text(
                    text = challenge.question,
                    color = textPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    lineHeight = 21.sp,
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            challenge.options.forEachIndexed { idx, optText ->
                val letter = ('A' + idx).toString()
                val isSelected = feedback?.selectedOptionIndex == idx
                val isAnswer = idx == challenge.correctIndex

                val tileBorder = when {
                    feedback == null -> borderColor
                    isAnswer -> Color(0xFF10B981)
                    isSelected -> Color(0xFFF43F5E)
                    else -> borderColor.copy(alpha = 0.5f)
                }
                val tileBg = when {
                    feedback == null -> elevatedColor
                    isAnswer -> Color(0xFF10B981).copy(alpha = 0.14f)
                    isSelected -> Color(0xFFF43F5E).copy(alpha = 0.14f)
                    else -> elevatedColor.copy(alpha = 0.55f)
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = feedback == null) { onChooseOption(idx) },
                    shape = RoundedCornerShape(16.dp),
                    color = tileBorder,
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 3.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = tileBg,
                        border = BorderStroke(1.5.dp, tileBorder),
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        when {
                                            feedback != null && isAnswer -> Color(0xFF10B981)
                                            feedback != null && isSelected -> Color(0xFFF43F5E)
                                            else -> surfaceColor
                                        }
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = letter,
                                    color = if (feedback != null && (isAnswer || isSelected)) Color.White else textPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black,
                                )
                            }
                            Text(
                                text = optText,
                                color = textPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                lineHeight = 18.sp,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SequenceOrderArena(
    challenge: DailyQuestChallenge.SequenceOrder,
    steps: List<SequenceStepItem>,
    swapSourceIndex: Int?,
    feedback: QuestFeedback?,
    surfaceColor: Color,
    elevatedColor: Color,
    borderColor: Color,
    textPrimary: Color,
    textSecondary: Color,
    onTapCard: (Int) -> Unit,
    onMove: (Int, Int) -> Unit,
    onSubmitSequence: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = elevatedColor,
            border = BorderStroke(1.5.dp, borderColor),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF8B5CF6).copy(alpha = 0.16f),
                ) {
                    Text(
                        text = (challenge.topicTag ?: "RANGKAI LOGIKA").uppercase(),
                        color = Color(0xFF8B5CF6),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
                Text(
                    text = challenge.prompt,
                    color = textPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    lineHeight = 21.sp,
                )
                Text(
                    text = "Tap 2 kartu untuk menukar posisi, atau tekan tombol panah ▲ ▼.",
                    color = textSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            steps.forEachIndexed { idx, stepItem ->
                val isSelected = swapSourceIndex == idx
                val isPosCorrect = feedback != null && stepItem.origIdx == idx
                val cardBorder = when {
                    feedback != null -> if (isPosCorrect) Color(0xFF10B981) else Color(0xFFF43F5E)
                    isSelected -> Color(0xFF8B5CF6)
                    else -> borderColor
                }
                val cardBg = when {
                    feedback != null -> if (isPosCorrect) Color(0xFF10B981).copy(alpha = 0.14f) else Color(0xFFF43F5E).copy(alpha = 0.14f)
                    isSelected -> Color(0xFF8B5CF6).copy(alpha = 0.16f)
                    else -> elevatedColor
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = feedback == null) { onTapCard(idx) },
                    shape = RoundedCornerShape(16.dp),
                    color = cardBorder,
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 3.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = cardBg,
                        border = BorderStroke(1.5.dp, cardBorder),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(9.dp))
                                    .background(
                                        when {
                                            feedback != null -> if (isPosCorrect) Color(0xFF10B981) else Color(0xFFF43F5E)
                                            isSelected -> Color(0xFF8B5CF6)
                                            else -> Color(0xFF0EA5E9).copy(alpha = 0.16f)
                                        }
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "${idx + 1}",
                                    color = if (feedback != null || isSelected) Color.White else Color(0xFF0EA5E9),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                )
                            }

                            Text(
                                text = stepItem.text,
                                color = textPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                lineHeight = 18.sp,
                                modifier = Modifier.weight(1f),
                            )

                            if (feedback == null) {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = surfaceColor,
                                        border = BorderStroke(1.dp, borderColor),
                                        modifier = Modifier
                                            .size(30.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable(enabled = idx > 0) { onMove(idx, -1) },
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.ArrowUpward,
                                                contentDescription = "Naik",
                                                tint = if (idx > 0) textPrimary else textSecondary.copy(alpha = 0.35f),
                                                modifier = Modifier.size(16.dp),
                                            )
                                        }
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = surfaceColor,
                                        border = BorderStroke(1.dp, borderColor),
                                        modifier = Modifier
                                            .size(30.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable(enabled = idx < steps.lastIndex) { onMove(idx, 1) },
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.ArrowDownward,
                                                contentDescription = "Turun",
                                                tint = if (idx < steps.lastIndex) textPrimary else textSecondary.copy(alpha = 0.35f),
                                                modifier = Modifier.size(16.dp),
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

        if (feedback == null) {
            ArcadePushButton(
                text = "KUNCI URUTAN JAWABAN ⚡",
                topColor = Color(0xFF8B5CF6),
                lipColor = Color(0xFF6D28D9),
                onClick = onSubmitSequence,
            )
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
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFF59E0B).copy(alpha = 0.16f),
                    ) {
                        Text(
                            text = (challenge.topicTag ?: "TEBAK ISTILAH").uppercase(),
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
                                Icon(
                                    imageVector = Icons.Default.Lightbulb,
                                    contentDescription = null,
                                    tint = Color(0xFFF59E0B),
                                    modifier = Modifier.size(16.dp),
                                )
                                Text(
                                    text = clue,
                                    color = textPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = 18.sp,
                                )
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
                            Icon(Icons.Default.HelpOutline, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(14.dp))
                            Text(
                                text = "Buka Petunjuk Berikutnya",
                                color = Color(0xFFF59E0B),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                            )
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
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                    imeAction = ImeAction.Done,
                ),
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
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
    val (grade, rankLabel, badgeTop, badgeBottom) = when {
        result.scorePercent >= 100 -> listOf("S", "LEGENDARY PERFECT!", Color(0xFFFACC15), Color(0xFFF97316))
        result.scorePercent >= 80 -> listOf("A", "SHARP CHALLENGER!", Color(0xFF34D399), Color(0xFF0D9488))
        result.scorePercent >= 60 -> listOf("B", "SOLID QUEST CLEAR!", Color(0xFF38BDF8), Color(0xFF2563EB))
        else -> listOf("C", "QUEST COMPLETED!", Color(0xFFA78BFA), Color(0xFF7C3AED))
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
                    .background(Brush.linearGradient(listOf(badgeTop as Color, badgeBottom as Color))),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "RANK",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                    )
                    Text(
                        text = grade as String,
                        color = Color.White,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Black,
                        lineHeight = 38.sp,
                    )
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = rankLabel as String,
                    color = badgeTop,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                )
                Text(
                    text = if (result.xpEarned > 0) "Yay! +${result.xpEarned} XP Masuk Tas Loot!" else "Latihan Misi Harian Tuntas!",
                    color = textPrimary,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = result.message,
                    color = textSecondary,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 17.sp,
                )
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
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
                        Text(
                            text = "Arena Peringkat Hari Ini",
                            color = textPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                        )
                        Text(
                            text = "Pemain tercepat & paling akurat di sekolah",
                            color = textSecondary,
                            fontSize = 11.sp,
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = elevatedColor,
                    border = BorderStroke(1.dp, borderColor),
                ) {
                    Text(
                        text = "Top ${entries.size}",
                        color = textSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
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
                                            Text(
                                                text = "${idx + 1}",
                                                color = if (idx < 3) Color.White else textPrimary,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Black,
                                            )
                                        }
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text(
                                                text = row.studentName,
                                                color = textPrimary,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                            if (row.isMe) {
                                                Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFF0EA5E9)) {
                                                    Text(
                                                        text = "KAMU",
                                                        color = Color.White,
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Black,
                                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                                    )
                                                }
                                            }
                                        }
                                        Text(
                                            text = "${row.className} • ${row.durationSeconds} detik",
                                            color = textSecondary,
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "${row.scorePercent}%",
                                        color = Color(0xFF10B981),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Black,
                                    )
                                    Text(
                                        text = "+${row.xpEarned} XP",
                                        color = Color(0xFF0EA5E9),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
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
                Text(
                    text = text,
                    color = textColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp,
                )
            }
        }
    }
}

