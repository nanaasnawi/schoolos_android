package com.schoolos.android.feature.quizzes.dailyquest

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.schoolos.android.core.designsystem.ConfettiParticleEffect
import com.schoolos.android.core.designsystem.LocalIsDarkTheme
import com.schoolos.android.domain.model.DailyQuestChallenge
import com.schoolos.android.domain.model.DailyQuestGameMode

private data class GameCatalogMeta(
    val mode: DailyQuestGameMode,
    val title: String,
    val badge: String,
    val tagline: String,
    val accent: Color,
    val gradTop: Color,
    val gradBottom: Color,
    val lipColor: Color,
    val icon: ImageVector,
)

private val ANDROID_GAME_CATALOG = listOf(
    GameCatalogMeta(
        mode = DailyQuestGameMode.KAHOOT_BLITZ,
        title = "Kuis Kilat Arena",
        badge = "KUIS CEPAT",
        tagline = "Gaya Kahoot! Jawab cepat sebelum waktu habis untuk bonus kombo",
        accent = Color(0xFFA855F7),
        gradTop = Color(0xFFC084FC),
        gradBottom = Color(0xFF9333EA),
        lipColor = Color(0xFF6D28D9),
        icon = Icons.Default.Bolt,
    ),
    GameCatalogMeta(
        mode = DailyQuestGameMode.WORD_SCRAMBLE,
        title = "Susun Kata ID & EN",
        badge = "BILINGUAL ID•EN",
        tagline = "Rangkai huruf acak kosakata Bahasa Indonesia & Bahasa Inggris",
        accent = Color(0xFFF43F5E),
        gradTop = Color(0xFFFB7185),
        gradBottom = Color(0xFFE11D48),
        lipColor = Color(0xFFBE123C),
        icon = Icons.Default.Translate,
    ),
    GameCatalogMeta(
        mode = DailyQuestGameMode.COGNITIVE_TRAINER,
        title = "Latih Kognitif",
        badge = "ASAH OTAK",
        tagline = "Latih memori visual, fokus Stroop, pola & hitung Paket A/B/C",
        accent = Color(0xFF14B8A6),
        gradTop = Color(0xFF2DD4BF),
        gradBottom = Color(0xFF0D9488),
        lipColor = Color(0xFF0F766E),
        icon = Icons.Default.Psychology,
    ),
    GameCatalogMeta(
        mode = DailyQuestGameMode.BLOCK_PUZZLE,
        title = "Block Blast 8×8",
        badge = "STRATEGI BALOK",
        tagline = "Pasang balok permata & bersihkan baris penuh untuk kombo",
        accent = Color(0xFFF59E0B),
        gradTop = Color(0xFFFBBF24),
        gradBottom = Color(0xFFD97706),
        lipColor = Color(0xFFB45309),
        icon = Icons.Default.ViewModule,
    ),
    GameCatalogMeta(
        mode = DailyQuestGameMode.CROSSWORD,
        title = "Teka-Teki Silang",
        badge = "KATA & WAWASAN",
        tagline = "Susun kata di papan silang interaktif dengan petunjuk dinamis",
        accent = Color(0xFF0EA5E9),
        gradTop = Color(0xFF38BDF8),
        gradBottom = Color(0xFF0284C7),
        lipColor = Color(0xFF0369A1),
        icon = Icons.Default.GridOn,
    ),
    GameCatalogMeta(
        mode = DailyQuestGameMode.SLIDING_PUZZLE,
        title = "Puzzle Geser",
        badge = "VISUAL & LOGIKA",
        tagline = "Susun potongan gambar Nusantara & petualangan 3×3 atau 4×4",
        accent = Color(0xFF10B981),
        gradTop = Color(0xFF34D399),
        gradBottom = Color(0xFF059669),
        lipColor = Color(0xFF047857),
        icon = Icons.Default.Extension,
    ),
    GameCatalogMeta(
        mode = DailyQuestGameMode.NUMBER_MERGE,
        title = "2048 Angka",
        badge = "NUMERASI CEPAT",
        tagline = "Gabungkan angka kembar hingga mencapai target ubin 2048",
        accent = Color(0xFF8B5CF6),
        gradTop = Color(0xFFA78BFA),
        gradBottom = Color(0xFF7C3AED),
        lipColor = Color(0xFF5B21B6),
        icon = Icons.Default.Calculate,
    ),
)

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
    val isDark = LocalIsDarkTheme.current

    // Adaptive Light & OLED Dark Palette
    val pageBg = if (isDark) Color(0xFF000000) else Color(0xFFF1F5F9)
    val surfaceColor = if (isDark) Color(0xFF111113) else Color.White
    val elevatedColor = if (isDark) Color(0xFF161618) else Color(0xFFF8FAFC)
    val borderColor = if (isDark) Color(0xFF1F1F23) else Color(0xFFE2E8F0)
    val textPrimary = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFFA1A1AA) else Color(0xFF64748B)

    val hubQuest = state.hubQuest
    val availableModes = hubQuest?.availableModes.orEmpty()
    val completedCount = availableModes.count { it.completedToday }
    val totalGames = availableModes.size.coerceAtLeast(7)

    Scaffold(
        containerColor = pageBg,
        topBar = {
            Surface(color = surfaceColor, border = BorderStroke(1.dp, borderColor)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = elevatedColor,
                        border = BorderStroke(1.dp, borderColor),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                if (state.activeGame != null) viewModel.backToMenu() else onBack()
                            },
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Kembali",
                                tint = textPrimary,
                                modifier = Modifier.size(16.dp),
                            )
                            Text(
                                text = if (state.activeGame != null) "Menu Game" else "Beranda",
                                color = textPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                softWrap = false,
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        if (state.activeGame != null) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = elevatedColor,
                                border = BorderStroke(1.dp, borderColor),
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Icon(Icons.Default.Timer, contentDescription = null, tint = Color(0xFF0EA5E9), modifier = Modifier.size(13.dp))
                                    Text(
                                        text = formatQuestTimer(state.elapsedSeconds),
                                        color = textPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        maxLines = 1,
                                        softWrap = false,
                                    )
                                }
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color(0xFFF59E0B).copy(alpha = 0.16f),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(13.dp))
                                Text(
                                    text = "Lv.${hubQuest?.level ?: 1} • ${hubQuest?.totalXp ?: 0} XP",
                                    color = Color(0xFFF59E0B),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    maxLines = 1,
                                    softWrap = false,
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(11.dp),
                            color = elevatedColor,
                            border = BorderStroke(1.dp, borderColor),
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(11.dp))
                                .clickable { viewModel.toggleSound() },
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (state.soundEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                                    contentDescription = "Suara",
                                    tint = textPrimary,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                    }
                }
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (state.errorMessage != null) {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFFF43F5E).copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, Color(0xFFF43F5E).copy(alpha = 0.4f)),
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    text = state.errorMessage!!,
                                    color = Color(0xFFF43F5E),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                )
                                Button(
                                    onClick = {
                                        val m = state.activeGame
                                        if (m != null) viewModel.openGame(m, state.roundNumber) else viewModel.loadHubSummary()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0EA5E9)),
                                    shape = RoundedCornerShape(10.dp),
                                ) {
                                    Text("Coba Lagi", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // ── VIEW A: 4-GAME MENU HUB (ALWAYS SHOWN FIRST) ─────────────
                if (state.activeGame == null) {
                    item {
                        ArcadeMascotHubBannerCompose(
                            completedCount = completedCount,
                            totalGames = totalGames,
                            streakDays = hubQuest?.streakDays ?: 1,
                            pet = state.petStatus ?: hubQuest?.petStatus,
                            chestClaimedToday = state.chestClaimedToday,
                            feedingPet = state.isFeedingPet,
                            openingChest = state.isOpeningChest,
                            feedPulse = state.cikoFeedPulse,
                            evolvePulse = state.cikoEvolvePulse,
                            petNotice = state.petNotice,
                            onFeedPet = { viewModel.feedShibaPet() },
                            onOpenChest = { viewModel.openDailyChest() },
                            onPetTap = { viewModel.playGoodSfx() },
                            surfaceColor = surfaceColor,
                            elevatedColor = elevatedColor,
                            borderColor = borderColor,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary,
                        )
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.SportsEsports, contentDescription = null, tint = Color(0xFF0EA5E9), modifier = Modifier.size(18.dp))
                                Text(
                                    text = "PILIH JENIS PERMAINAN",
                                    color = textPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                )
                            }
                            Text(
                                text = hubQuest?.dayLabel ?: "Dinamis Harian",
                                color = textSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }

                    if (state.isLoadingHub) {
                        item {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(18.dp),
                                color = surfaceColor,
                                border = BorderStroke(1.dp, borderColor),
                            ) {
                                Column(
                                    modifier = Modifier.padding(30.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    CircularProgressIndicator(color = Color(0xFF0EA5E9), strokeWidth = 3.dp)
                                    Text("Menyiapkan Zona Game Edukasi...", color = textSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    } else {
                        ANDROID_GAME_CATALOG.forEach { itemMeta ->
                            val serverMode = availableModes.find { it.code == itemMeta.mode }
                            val isDone = serverMode?.completedToday == true
                            val isFeatured = serverMode?.isFeaturedToday == true

                            item(key = itemMeta.mode.code) {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(22.dp))
                                        .clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            viewModel.openGame(itemMeta.mode, 0)
                                        },
                                    shape = RoundedCornerShape(22.dp),
                                    color = surfaceColor,
                                    border = BorderStroke(
                                        width = 1.5.dp,
                                        color = if (isDone) Color(0xFF10B981).copy(alpha = 0.55f) else borderColor,
                                    ),
                                ) {
                                    Column {
                                        // Full-Bleed Illustrated Game Cover Header
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(142.dp),
                                        ) {
                                            GamePosterArtworkCompose(mode = itemMeta.mode)

                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(
                                                        Brush.verticalGradient(
                                                            colors = listOf(
                                                                Color(0xFF0F172A).copy(alpha = 0.22f),
                                                                Color.Transparent,
                                                                Color(0xFF0F172A).copy(alpha = 0.78f),
                                                            )
                                                        )
                                                    ),
                                            )

                                            // Top-Left Category Badge
                                            Surface(
                                                shape = RoundedCornerShape(50),
                                                color = Color(0xFF0F172A).copy(alpha = 0.72f),
                                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                                                modifier = Modifier
                                                    .align(Alignment.TopStart)
                                                    .padding(10.dp),
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                ) {
                                                    Icon(itemMeta.icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                                    Text(
                                                        text = itemMeta.badge,
                                                        color = Color.White,
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Black,
                                                        maxLines = 1,
                                                        softWrap = false,
                                                    )
                                                }
                                            }

                                            // Top-Right Reward / Status Pill
                                            Surface(
                                                shape = RoundedCornerShape(50),
                                                color = when {
                                                    isDone -> Color(0xFF10B981)
                                                    isFeatured -> Color(0xFFF59E0B)
                                                    else -> Color(0xFF0F172A).copy(alpha = 0.78f)
                                                },
                                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.45f)),
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .padding(10.dp),
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                                                ) {
                                                    if (isDone) {
                                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(11.dp))
                                                    }
                                                    Text(
                                                        text = if (isDone) "TUNTAS" else if (isFeatured) "★ +80 XP" else "+60 XP",
                                                        color = Color.White,
                                                        fontSize = 9.5.sp,
                                                        fontWeight = FontWeight.Black,
                                                        maxLines = 1,
                                                        softWrap = false,
                                                    )
                                                }
                                            }

                                            // Title Over Cover Scrim
                                            Text(
                                                text = itemMeta.title,
                                                color = Color.White,
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Black,
                                                modifier = Modifier
                                                    .align(Alignment.BottomStart)
                                                    .padding(horizontal = 13.dp, vertical = 10.dp),
                                            )
                                        }

                                        // Card Body + Chunky 3D Play Button
                                        Column(
                                            modifier = Modifier.padding(horizontal = 13.dp, vertical = 12.dp),
                                            verticalArrangement = Arrangement.spacedBy(10.dp),
                                        ) {
                                            Text(
                                                text = itemMeta.tagline,
                                                color = textSecondary,
                                                fontSize = 12.sp,
                                                lineHeight = 16.sp,
                                            )

                                            Surface(
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(14.dp),
                                                color = itemMeta.lipColor,
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .padding(bottom = 4.dp)
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(14.dp))
                                                        .background(Brush.verticalGradient(listOf(itemMeta.gradTop, itemMeta.gradBottom)))
                                                        .padding(vertical = 10.dp),
                                                    contentAlignment = Alignment.Center,
                                                ) {
                                                    Text(
                                                        text = if (isDone) "▶ MAIN LEVEL BARU" else "▶ MAIN SEKARANG",
                                                        color = Color.White,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Black,
                                                        maxLines = 1,
                                                        softWrap = false,
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    val lb = hubQuest?.leaderboard.orEmpty()
                    if (lb.isNotEmpty()) {
                        item {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(18.dp),
                                color = surfaceColor,
                                border = BorderStroke(1.dp, borderColor),
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(16.dp))
                                        Text("Papan Skor Sekolah Hari Ini", color = textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Black)
                                    }
                                    lb.take(5).forEachIndexed { idx, entry ->
                                        Surface(
                                            shape = RoundedCornerShape(11.dp),
                                            color = if (entry.isMe) Color(0xFF0EA5E9).copy(alpha = 0.14f) else elevatedColor,
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                Row(
                                                    modifier = Modifier.weight(1f),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(22.dp)
                                                            .clip(RoundedCornerShape(7.dp))
                                                            .background(if (idx == 0) Color(0xFFF59E0B) else surfaceColor),
                                                        contentAlignment = Alignment.Center,
                                                    ) {
                                                        if (idx == 0) {
                                                            Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                                        } else {
                                                            Text("${idx + 1}", color = textPrimary, fontSize = 11.sp, fontWeight = FontWeight.Black)
                                                        }
                                                    }
                                                    Column {
                                                        Text(
                                                            text = entry.studentName,
                                                            color = textPrimary,
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.ExtraBold,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis,
                                                        )
                                                        Text("${entry.className} • ${entry.durationSeconds} dtk", color = textSecondary, fontSize = 10.sp)
                                                    }
                                                }
                                                Text(
                                                    text = "${entry.scorePercent}% (+${entry.xpEarned} XP)",
                                                    color = Color(0xFF10B981),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Black,
                                                    maxLines = 1,
                                                    softWrap = false,
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // ── VIEW B: ACTIVE PLAYABLE MINI-GAME ────────────────────────
                    if (state.isLoadingGame) {
                        item {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                color = surfaceColor,
                                border = BorderStroke(1.dp, borderColor),
                            ) {
                                Column(
                                    modifier = Modifier.padding(36.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    CircularProgressIndicator(color = Color(0xFF0EA5E9), strokeWidth = 3.dp)
                                    Text("Membuat papan game baru...", color = textPrimary, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                                }
                            }
                        }
                    } else if (state.submitResult != null) {
                        val res = state.submitResult!!
                        item {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(22.dp),
                                color = surfaceColor,
                                border = BorderStroke(1.5.dp, Color(0xFF10B981).copy(alpha = 0.5f)),
                            ) {
                                Column(
                                    modifier = Modifier.padding(22.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(60.dp)
                                            .clip(RoundedCornerShape(18.dp))
                                            .background(Color(0xFF10B981)),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
                                    }

                                    Text("Permainan Tuntas!", color = textPrimary, fontSize = 20.sp, fontWeight = FontWeight.Black)
                                    Text(res.message, color = textSecondary, fontSize = 13.sp, textAlign = TextAlign.Center)

                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Surface(shape = RoundedCornerShape(50), color = Color(0xFF10B981).copy(alpha = 0.16f)) {
                                            Text(
                                                text = "+${res.xpEarned} XP",
                                                color = Color(0xFF10B981),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Black,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                            )
                                        }
                                        Surface(shape = RoundedCornerShape(50), color = Color(0xFFF59E0B).copy(alpha = 0.16f)) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            ) {
                                                Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(13.dp))
                                                Text(
                                                    text = "Streak ${res.streakDays} Hari",
                                                    color = Color(0xFFF59E0B),
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Black,
                                                )
                                            }
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    ) {
                                        Button(
                                            onClick = { viewModel.loadNextLevel() },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0EA5E9)),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.weight(1f),
                                        ) {
                                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(15.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text(
                                                text = if (state.activeGame == DailyQuestGameMode.CROSSWORD && state.ttsLevel < 10) {
                                                    "Naik Level ${state.ttsLevel + 1}"
                                                } else {
                                                    "Level Baru"
                                                },
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Black,
                                                maxLines = 1,
                                                softWrap = false,
                                            )
                                        }
                                        Button(
                                            onClick = { viewModel.backToMenu() },
                                            colors = ButtonDefaults.buttonColors(containerColor = elevatedColor, contentColor = textPrimary),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.weight(1f),
                                        ) {
                                            Text("Menu Game", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        val firstChallenge = state.activeQuest?.challenges?.firstOrNull()
                        if (firstChallenge != null) {
                            item {
                                when (firstChallenge) {
                                    is DailyQuestChallenge.Crossword -> CrosswordGameCompose(
                                        challenge = firstChallenge,
                                        disabled = state.isSubmitting,
                                        selectedSubject = state.ttsSubject,
                                        selectedLevel = state.ttsLevel,
                                        unlockedLevel = state.unlockedLevelFor(state.ttsSubject),
                                        surfaceColor = surfaceColor,
                                        elevatedColor = elevatedColor,
                                        borderColor = borderColor,
                                        textPrimary = textPrimary,
                                        textSecondary = textSecondary,
                                        onChangeSubject = { viewModel.selectTtsSubject(it) },
                                        onChangeLevel = { viewModel.selectTtsLevel(it) },
                                        onPlayTap = { viewModel.playTapSfx() },
                                        onPlayGood = { viewModel.playGoodSfx() },
                                        onFinish = { c, t, m -> viewModel.submitGameScore(c, t, m) },
                                        onNewLevel = { viewModel.openGame(DailyQuestGameMode.CROSSWORD, state.roundNumber + 1) },
                                    )

                                    is DailyQuestChallenge.SlidingPuzzle -> SlidingPuzzleGameCompose(
                                        challenge = firstChallenge,
                                        disabled = state.isSubmitting,
                                        surfaceColor = surfaceColor,
                                        elevatedColor = elevatedColor,
                                        borderColor = borderColor,
                                        textPrimary = textPrimary,
                                        textSecondary = textSecondary,
                                        onPlayTap = { viewModel.playTapSfx() },
                                        onPlayGood = { viewModel.playGoodSfx() },
                                        onFinish = { c, t, m -> viewModel.submitGameScore(c, t, m) },
                                        onNewLevel = { viewModel.loadNextLevel() },
                                    )

                                    is DailyQuestChallenge.BlockPuzzle -> BlockBlastGameCompose(
                                        challenge = firstChallenge,
                                        disabled = state.isSubmitting,
                                        surfaceColor = surfaceColor,
                                        elevatedColor = elevatedColor,
                                        borderColor = borderColor,
                                        textPrimary = textPrimary,
                                        textSecondary = textSecondary,
                                        onPlayTap = { viewModel.playTapSfx() },
                                        onPlayGood = { viewModel.playGoodSfx() },
                                        onPlayError = { viewModel.playErrorSfx() },
                                        onFinish = { c, t, m -> viewModel.submitGameScore(c, t, m) },
                                        onNewLevel = { viewModel.loadNextLevel() },
                                    )

                                    is DailyQuestChallenge.NumberMerge -> NumberMergeGameCompose(
                                        challenge = firstChallenge,
                                        disabled = state.isSubmitting,
                                        surfaceColor = surfaceColor,
                                        elevatedColor = elevatedColor,
                                        borderColor = borderColor,
                                        textPrimary = textPrimary,
                                        textSecondary = textSecondary,
                                        onPlayTap = { viewModel.playTapSfx() },
                                        onPlayGood = { viewModel.playGoodSfx() },
                                        onFinish = { c, t, m -> viewModel.submitGameScore(c, t, m) },
                                        onNewLevel = { viewModel.loadNextLevel() },
                                    )

                                    is DailyQuestChallenge.KahootBlitz -> KahootBlitzGameCompose(
                                        challenge = firstChallenge,
                                        disabled = state.isSubmitting,
                                        surfaceColor = surfaceColor,
                                        elevatedColor = elevatedColor,
                                        borderColor = borderColor,
                                        textPrimary = textPrimary,
                                        textSecondary = textSecondary,
                                        onPlayTap = { viewModel.playTapSfx() },
                                        onPlayGood = { viewModel.playGoodSfx() },
                                        onPlayError = { viewModel.playErrorSfx() },
                                        onFinish = { c, t, m -> viewModel.submitGameScore(c, t, m) },
                                    )

                                    is DailyQuestChallenge.WordScramble -> WordScrambleGameCompose(
                                        challenge = firstChallenge,
                                        disabled = state.isSubmitting,
                                        surfaceColor = surfaceColor,
                                        elevatedColor = elevatedColor,
                                        borderColor = borderColor,
                                        textPrimary = textPrimary,
                                        textSecondary = textSecondary,
                                        onPlayTap = { viewModel.playTapSfx() },
                                        onPlayGood = { viewModel.playGoodSfx() },
                                        onPlayError = { viewModel.playErrorSfx() },
                                        onFinish = { c, t, m -> viewModel.submitGameScore(c, t, m) },
                                    )

                                    is DailyQuestChallenge.CognitiveTrainer -> CognitiveTrainerGameCompose(
                                        challenge = firstChallenge,
                                        disabled = state.isSubmitting,
                                        surfaceColor = surfaceColor,
                                        elevatedColor = elevatedColor,
                                        borderColor = borderColor,
                                        textPrimary = textPrimary,
                                        textSecondary = textSecondary,
                                        onPlayTap = { viewModel.playTapSfx() },
                                        onPlayGood = { viewModel.playGoodSfx() },
                                        onPlayError = { viewModel.playErrorSfx() },
                                        onFinish = { c, t, m -> viewModel.submitGameScore(c, t, m) },
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (state.submitResult != null) {
                ConfettiParticleEffect()
            }
        }
    }
}
