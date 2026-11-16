package com.schoolos.android.feature.quizzes.dailyquest

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.schoolos.android.domain.model.DailyQuestGameMode
import com.schoolos.android.domain.model.ShibaPetStatus
import com.schoolos.android.feature.quizzes.R
import kotlin.math.min

internal fun resolvePuzzleDrawableRes(themeId: String): Int = when (themeId) {
    "MORNING_STUDY", "KOTA_HIJAU" -> R.drawable.dq_morning_room
    "NUSANTARA_MAP", "NUSANTARA" -> R.drawable.dq_cirebon_map
    "STUDENT_QUEST", "LAB_SAINS" -> R.drawable.dq_student_tablet
    else -> R.drawable.game_hub_hero
}

@Composable
internal fun ArcadeMascotHubBannerCompose(
    completedCount: Int,
    totalGames: Int,
    streakDays: Int,
    pet: ShibaPetStatus?,
    chestClaimedToday: Boolean,
    feedingPet: Boolean,
    openingChest: Boolean,
    feedPulse: Int,
    evolvePulse: Int,
    petNotice: String?,
    onFeedPet: () -> Unit,
    onOpenChest: () -> Unit,
    onPetTap: () -> Unit,
    surfaceColor: Color,
    elevatedColor: Color,
    borderColor: Color,
    textPrimary: Color,
    textSecondary: Color,
) {
    val stage = pet?.petStage ?: 1
    val maxStage = pet?.maxStage ?: 6
    val stageTitle = pet?.stageTitle ?: "Anak Kucing Penasaran"
    val biscuits = pet?.biscuits ?: 0

    val energyCur = pet?.foodPoints ?: 0
    val energyReq = (pet?.energyRequired ?: 120).coerceAtLeast(1)
    val energyRatio = (energyCur.toFloat() / energyReq.toFloat()).coerceIn(0f, 1f)

    val studentXp = pet?.studentXp ?: 0L
    val xpReq = (pet?.xpRequiredNext ?: 250L).coerceAtLeast(1L)
    val xpRemaining = pet?.xpRemainingNext ?: (xpReq - studentXp).coerceAtLeast(0L)
    val xpRatio = if (stage >= maxStage) 1f else (studentXp.toFloat() / xpReq.toFloat()).coerceIn(0f, 1f)
    val evoPct = pet?.evolutionProgressPercent ?: (((energyRatio + xpRatio) / 2f) * 100f).toInt()
    val readyToEvolve = pet?.readyToEvolve == true
    val hint = pet?.evolveRequirementHint?.takeIf { it.isNotBlank() }
        ?: if (stage >= maxStage) {
            "Wujud Puncak Kucing Cyber AI tercapai!"
        } else {
            "Kumpulkan Energi Makan & EXP Belajar untuk Evolusi Lv.${stage + 1}"
        }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = surfaceColor,
        border = BorderStroke(1.5.dp, borderColor),
    ) {
        Column {
            // Interactive 3D Pixar Living Stage
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Color(0xFF1E293B), Color(0xFF0F172A), Color(0xFF020617)),
                        )
                    ),
            ) {
                Image(
                    painter = painterResource(id = R.drawable.game_hub_hero),
                    contentDescription = "Studio Ciko",
                    contentScale = ContentScale.Crop,
                    alpha = 0.22f,
                    modifier = Modifier.matchParentSize(),
                )

                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFF020617).copy(alpha = 0.9f),
                                    Color(0xFF0F172A).copy(alpha = 0.72f),
                                    Color(0xFF0F172A).copy(alpha = 0.52f),
                                )
                            )
                        ),
                )

                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // Top floating HUD Badges
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color(0xFFB45309),
                        ) {
                            Surface(
                                modifier = Modifier.padding(bottom = 2.5.dp),
                                shape = RoundedCornerShape(50),
                                color = Color(0xFFF59E0B),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.7f)),
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                    Text(
                                        text = "${pet?.petName ?: "Ciko Pintar"} • Lv.$stage/$maxStage",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black,
                                        maxLines = 1,
                                        softWrap = false,
                                    )
                                }
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = Color(0xFF0F172A).copy(alpha = 0.78f),
                                border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.55f)),
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(12.dp))
                                    Text(
                                        text = "$biscuits Koin",
                                        color = Color(0xFF38BDF8),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black,
                                        maxLines = 1,
                                        softWrap = false,
                                    )
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = Color(0xFF0F172A).copy(alpha = 0.78f),
                                border = BorderStroke(1.dp, Color(0xFFFBBF24).copy(alpha = 0.5f)),
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                                ) {
                                    Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = Color(0xFFFBBF24), modifier = Modifier.size(12.dp))
                                    Text(
                                        text = "${streakDays}H",
                                        color = Color(0xFFFBBF24),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black,
                                        maxLines = 1,
                                        softWrap = false,
                                    )
                                }
                            }
                        }
                    }

                    // Split Stage: Stage Info + Living 3D Pixar Ciko
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            if (readyToEvolve) listOf(Color(0xFF10B981), Color(0xFF059669))
                                            else listOf(Color(0xFFEF4444), Color(0xFFF97316))
                                        )
                                    )
                                    .padding(horizontal = 8.dp, vertical = 2.dp),
                            ) {
                                Text(
                                    text = if (readyToEvolve) "SIAP EVOLUSI" else "EVOLUSI TAHAP $stage",
                                    color = Color.White,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Black,
                                    maxLines = 1,
                                    softWrap = false,
                                )
                            }

                            Text(
                                text = stageTitle,
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                lineHeight = 19.sp,
                            )

                            Text(
                                text = "Sentuh Ciko untuk bermain! Selesaikan tugas & beri makan agar Ciko berevolusi.",
                                color = Color(0xFFCBD5E1),
                                fontSize = 10.5.sp,
                                lineHeight = 14.sp,
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = Color(0xFF10B981).copy(alpha = 0.16f),
                                    border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.35f)),
                                ) {
                                    Text(
                                        text = "Misi: $completedCount/$totalGames",
                                        color = Color(0xFFA7F3D0),
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        maxLines = 1,
                                        softWrap = false,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = Color(0xFFF59E0B).copy(alpha = 0.16f),
                                    border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.35f)),
                                ) {
                                    Text(
                                        text = "Evolusi $evoPct%",
                                        color = Color(0xFFFDE68A),
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        maxLines = 1,
                                        softWrap = false,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                    )
                                }
                            }
                        }

                        CikoLivingAvatarCompose(
                            stage = stage,
                            stageTitle = stageTitle,
                            readyToEvolve = readyToEvolve,
                            biscuits = biscuits,
                            feedPulse = feedPulse,
                            evolvePulse = evolvePulse,
                            onPetTap = onPetTap,
                        )
                    }
                }
            }

            // Bottom Dual-Gate Evolution Bars + 3D Beveled Buttons
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    // Pillar 1: Energi Makan Ciko
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                Icon(Icons.Default.Bolt, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(13.dp))
                                Text("Energi", color = textPrimary, fontSize = 10.5.sp, fontWeight = FontWeight.Black, maxLines = 1, softWrap = false)
                            }
                            Text(
                                text = "$energyCur/$energyReq",
                                color = Color(0xFFF59E0B),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                maxLines = 1,
                                softWrap = false,
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(11.dp)
                                .clip(CircleShape)
                                .background(elevatedColor)
                                .border(1.dp, borderColor, CircleShape)
                                .padding(1.5.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(energyRatio.coerceAtLeast(0.05f))
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(Brush.verticalGradient(listOf(Color(0xFFFBBF24), Color(0xFFEA580C)))),
                            )
                        }
                    }

                    // Pillar 2: Syarat EXP Belajar
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                Icon(Icons.Default.School, contentDescription = null, tint = Color(0xFF0EA5E9), modifier = Modifier.size(13.dp))
                                Text("EXP Belajar", color = textPrimary, fontSize = 10.5.sp, fontWeight = FontWeight.Black, maxLines = 1, softWrap = false)
                            }
                            Text(
                                text = if (stage >= maxStage) "MAX ✓" else "$studentXp/$xpReq",
                                color = if (xpRemaining == 0L) Color(0xFF10B981) else Color(0xFF0EA5E9),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                maxLines = 1,
                                softWrap = false,
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(11.dp)
                                .clip(CircleShape)
                                .background(elevatedColor)
                                .border(1.dp, borderColor, CircleShape)
                                .padding(1.5.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(xpRatio.coerceAtLeast(0.05f))
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(
                                        Brush.verticalGradient(
                                            if (xpRemaining == 0L) listOf(Color(0xFF34D399), Color(0xFF059669))
                                            else listOf(Color(0xFF38BDF8), Color(0xFF6366F1))
                                        )
                                    ),
                            )
                        }
                    }
                }

                // Evolution Requirement Hint
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = elevatedColor,
                    border = BorderStroke(1.dp, borderColor),
                ) {
                    Text(
                        text = hint,
                        color = if (readyToEvolve) Color(0xFF10B981) else textSecondary,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }

                // Chunky 3D Beveled Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    val canFeed = !feedingPet && biscuits > 0
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (canFeed) Color(0xFFB45309) else borderColor,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable(enabled = canFeed) { onFeedPet() },
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(bottom = if (canFeed) 4.dp else 0.dp)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (canFeed) Brush.verticalGradient(listOf(Color(0xFFFBBF24), Color(0xFFF59E0B)))
                                    else Brush.verticalGradient(listOf(elevatedColor, elevatedColor))
                                )
                                .padding(vertical = 10.dp, horizontal = 8.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = if (canFeed) Color.White else textSecondary, modifier = Modifier.size(14.dp))
                                Text(
                                    text = if (feedingPet) "Memberi..." else "Beri Makan ($biscuits)",
                                    color = if (canFeed) Color.White else textSecondary,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Black,
                                    maxLines = 1,
                                    softWrap = false,
                                )
                            }
                        }
                    }

                    val canChest = !openingChest && !chestClaimedToday
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (canChest) Color(0xFF047857) else borderColor,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable(enabled = canChest) { onOpenChest() },
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(bottom = if (canChest) 4.dp else 0.dp)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (canChest) Brush.verticalGradient(listOf(Color(0xFF34D399), Color(0xFF10B981)))
                                    else Brush.verticalGradient(listOf(elevatedColor, elevatedColor))
                                )
                                .padding(vertical = 10.dp, horizontal = 8.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                Icon(Icons.Default.CardGiftcard, contentDescription = null, tint = if (canChest) Color.White else textSecondary, modifier = Modifier.size(14.dp))
                                Text(
                                    text = if (chestClaimedToday) "Diklaim ✓" else if (openingChest) "Membuka..." else "Peti Harian",
                                    color = if (canChest) Color.White else textSecondary,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Black,
                                    maxLines = 1,
                                    softWrap = false,
                                )
                            }
                        }
                    }
                }

                if (!petNotice.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(11.dp),
                        color = Color(0xFFF59E0B).copy(alpha = 0.14f),
                    ) {
                        Text(
                            text = petNotice,
                            color = Color(0xFFF59E0B),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 7.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun GamePosterArtworkCompose(
    mode: DailyQuestGameMode,
    modifier: Modifier = Modifier,
) {
    when (mode) {
        DailyQuestGameMode.CROSSWORD -> {
            Canvas(modifier = modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF38BDF8), Color(0xFF0284C7), Color(0xFF1E3A8A)),
                        center = Offset(w * 0.5f, h * 0.45f),
                        radius = w * 0.7f,
                    ),
                    size = Size(w, h),
                )

                // Sunburst Rays
                val center = Offset(w * 0.5f, h * 0.5f)
                for (i in 0 until 8) {
                    val angle = i * 45f
                    rotate(degrees = angle, pivot = center) {
                        val ray = Path().apply {
                            moveTo(center.x, center.y)
                            lineTo(center.x - w * 0.14f, -h * 0.2f)
                            lineTo(center.x + w * 0.14f, -h * 0.2f)
                            close()
                        }
                        drawPath(ray, Color.White.copy(alpha = 0.13f))
                    }
                }

                // Tilted 3D Game Letter Tiles
                rotate(degrees = -7f, pivot = center) {
                    val tileSize = min(w, h) * 0.24f
                    val gap = tileSize * 0.14f
                    val rowY = center.y - tileSize * 0.5f
                    val startX = center.x - tileSize * 1.9f
                    val specs = listOf(true, false, true, false)
                    specs.forEachIndexed { idx, isGold ->
                        val x = startX + idx * (tileSize + gap)
                        drawRoundRect(
                            color = if (isGold) Color(0xFFD97706) else Color(0xFF0369A1),
                            topLeft = Offset(x, rowY + tileSize * 0.12f),
                            size = Size(tileSize, tileSize),
                            cornerRadius = CornerRadius(tileSize * 0.22f),
                        )
                        drawRoundRect(
                            color = if (isGold) Color(0xFFFEF3C7) else Color(0xFF7DD3FC),
                            topLeft = Offset(x, rowY),
                            size = Size(tileSize, tileSize),
                            cornerRadius = CornerRadius(tileSize * 0.22f),
                        )
                        drawRoundRect(
                            color = Color.White,
                            topLeft = Offset(x, rowY),
                            size = Size(tileSize, tileSize),
                            cornerRadius = CornerRadius(tileSize * 0.22f),
                            style = Stroke(width = 2.5f),
                        )
                    }
                    // Vertical intersecting column tiles on index 2
                    val colX = startX + 2 * (tileSize + gap)
                    listOf(rowY - (tileSize + gap), rowY + (tileSize + gap)).forEachIndexed { i, y ->
                        val isGold = i == 1
                        drawRoundRect(
                            color = if (isGold) Color(0xFFD97706) else Color(0xFF0369A1),
                            topLeft = Offset(colX, y + tileSize * 0.12f),
                            size = Size(tileSize, tileSize),
                            cornerRadius = CornerRadius(tileSize * 0.22f),
                        )
                        drawRoundRect(
                            color = if (isGold) Color(0xFFFEF3C7) else Color(0xFF38BDF8),
                            topLeft = Offset(colX, y),
                            size = Size(tileSize, tileSize),
                            cornerRadius = CornerRadius(tileSize * 0.22f),
                        )
                        drawRoundRect(
                            color = Color.White,
                            topLeft = Offset(colX, y),
                            size = Size(tileSize, tileSize),
                            cornerRadius = CornerRadius(tileSize * 0.22f),
                            style = Stroke(width = 2.5f),
                        )
                    }
                }

                // Golden sparkles
                drawCircle(Color(0xFFFDE047), radius = 6f, center = Offset(w * 0.16f, h * 0.25f))
                drawCircle(Color.White, radius = 4f, center = Offset(w * 0.85f, h * 0.24f))
                drawCircle(Color(0xFFFDE047), radius = 5f, center = Offset(w * 0.82f, h * 0.72f))
            }
        }

        DailyQuestGameMode.SLIDING_PUZZLE -> {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Color(0xFF34D399), Color(0xFF059669), Color(0xFF064E3B)),
                        )
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF022C22),
                    modifier = Modifier
                        .size(106.dp)
                        .rotate(-6f),
                ) {
                    Column(
                        modifier = Modifier
                            .padding(bottom = 4.dp)
                            .fillMaxSize()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF064E3B))
                            .border(1.5.dp, Color(0xFFA7F3D0).copy(alpha = 0.7f), RoundedCornerShape(14.dp))
                            .padding(5.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        for (r in 0..2) {
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                            ) {
                                for (c in 0..2) {
                                    if (r == 2 && c == 2) {
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxSize()
                                                .clip(RoundedCornerShape(5.dp))
                                                .background(Color(0xFF10B981).copy(alpha = 0.2f)),
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxSize()
                                                .clip(RoundedCornerShape(5.dp))
                                                .border(1.dp, Color.White.copy(alpha = 0.85f), RoundedCornerShape(5.dp)),
                                        ) {
                                            SlidingPuzzleArtworkSlice(
                                                themeId = "PET_TREASURE",
                                                gridSize = 3,
                                                goalRow = r,
                                                goalCol = c,
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

        DailyQuestGameMode.BLOCK_PUZZLE -> {
            Image(
                painter = painterResource(id = R.drawable.game_cover_block),
                contentDescription = "Block Blast",
                contentScale = ContentScale.Crop,
                modifier = modifier.fillMaxSize(),
            )
        }

        DailyQuestGameMode.NUMBER_MERGE -> {
            Box(modifier = modifier.fillMaxSize()) {
                Image(
                    painter = painterResource(id = R.drawable.game_cover_2048),
                    contentDescription = "2048 Angka",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .scale(1.18f),
                )
            }
        }

        DailyQuestGameMode.KAHOOT_BLITZ -> {
            Canvas(modifier = modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFA855F7), Color(0xFF6D28D9), Color(0xFF3B0764)),
                        center = Offset(w * 0.5f, h * 0.45f),
                        radius = w * 0.7f,
                    ),
                    size = Size(w, h),
                )
                val bw = w * 0.28f
                val bh = h * 0.28f
                val gx = w * 0.2f
                val gy = h * 0.18f
                val specs = listOf(
                    Triple(gx, gy, Color(0xFFF43F5E) to Color(0xFF9F1239)),
                    Triple(gx + bw + 14f, gy, Color(0xFF0EA5E9) to Color(0xFF0369A1)),
                    Triple(gx, gy + bh + 12f, Color(0xFFF59E0B) to Color(0xFFB45309)),
                    Triple(gx + bw + 14f, gy + bh + 12f, Color(0xFF10B981) to Color(0xFF047857)),
                )
                specs.forEach { (x, y, pair) ->
                    drawRoundRect(pair.second, topLeft = Offset(x, y + 6f), size = Size(bw, bh), cornerRadius = CornerRadius(18f))
                    drawRoundRect(pair.first, topLeft = Offset(x, y), size = Size(bw, bh), cornerRadius = CornerRadius(18f))
                    drawRoundRect(Color.White.copy(alpha = 0.55f), topLeft = Offset(x, y), size = Size(bw, bh), cornerRadius = CornerRadius(18f), style = Stroke(2f))
                }
                drawCircle(Color(0xFFFDE047), radius = 14f, center = Offset(w * 0.5f, h * 0.48f))
            }
        }

        DailyQuestGameMode.WORD_SCRAMBLE -> {
            Canvas(modifier = modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFFB7185), Color(0xFFE11D48), Color(0xFF881337)),
                        center = Offset(w * 0.5f, h * 0.45f),
                        radius = w * 0.7f,
                    ),
                    size = Size(w, h),
                )
                val ts = min(w, h) * 0.24f
                val gap = ts * 0.14f
                val row1X = w * 0.5f - ts * 2.1f
                val row1Y = h * 0.22f
                for (i in 0..3) {
                    val x = row1X + i * (ts + gap)
                    drawRoundRect(Color(0xFFB45309), Offset(x, row1Y + 5f), Size(ts, ts), CornerRadius(14f))
                    drawRoundRect(Color(0xFFFEF3C7), Offset(x, row1Y), Size(ts, ts), CornerRadius(14f))
                    drawRoundRect(Color.White, Offset(x, row1Y), Size(ts, ts), CornerRadius(14f), style = Stroke(2f))
                }
                val row2X = w * 0.5f - ts * 1.9f
                val row2Y = row1Y + ts + 14f
                for (i in 0..3) {
                    val x = row2X + i * (ts + gap)
                    drawRoundRect(Color(0xFF0369A1), Offset(x, row2Y + 5f), Size(ts, ts), CornerRadius(14f))
                    drawRoundRect(Color(0xFF38BDF8), Offset(x, row2Y), Size(ts, ts), CornerRadius(14f))
                    drawRoundRect(Color.White, Offset(x, row2Y), Size(ts, ts), CornerRadius(14f), style = Stroke(2f))
                }
            }
        }

        DailyQuestGameMode.COGNITIVE_TRAINER -> {
            Canvas(modifier = modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF2DD4BF), Color(0xFF0D9488), Color(0xFF134E4A)),
                        center = Offset(w * 0.5f, h * 0.45f),
                        radius = w * 0.7f,
                    ),
                    size = Size(w, h),
                )
                val boxSize = min(w, h) * 0.66f
                val left = (w - boxSize) / 2f
                val top = (h - boxSize) / 2f
                drawRoundRect(Color(0xFF0F172A).copy(alpha = 0.45f), Offset(left, top + 6f), Size(boxSize, boxSize), CornerRadius(24f))
                drawRoundRect(Color(0xFF134E4A), Offset(left, top), Size(boxSize, boxSize), CornerRadius(24f))
                drawRoundRect(Color(0xFF5EEAD4), Offset(left, top), Size(boxSize, boxSize), CornerRadius(24f), style = Stroke(2.5f))
                val cell = (boxSize - 28f) / 3f
                val litMap = mapOf(
                    0 to Color(0xFF38BDF8),
                    2 to Color(0xFFFACC15),
                    4 to Color(0xFFF43F5E),
                    6 to Color(0xFFA78BFA),
                    7 to Color(0xFF34D399),
                )
                for (r in 0..2) for (c in 0..2) {
                    val idx = r * 3 + c
                    val x = left + 10f + c * (cell + 4f)
                    val y = top + 10f + r * (cell + 4f)
                    val litCol = litMap[idx]
                    drawRoundRect(
                        color = litCol ?: Color.White.copy(alpha = 0.14f),
                        topLeft = Offset(x, y),
                        size = Size(cell, cell),
                        cornerRadius = CornerRadius(10f),
                    )
                    if (litCol != null) {
                        drawRoundRect(Color.White, Offset(x, y), Size(cell, cell), CornerRadius(10f), style = Stroke(2f))
                    }
                }
            }
        }
    }
}

/**
 * Slices crisp illustrated high-resolution game art for Sliding Puzzle tiles.
 */
@Composable
internal fun SlidingPuzzleArtworkSlice(
    themeId: String,
    gridSize: Int,
    goalRow: Int, // Pass -1 to render full preview artwork!
    goalCol: Int,
    modifier: Modifier = Modifier,
) {
    val resId = resolvePuzzleDrawableRes(themeId)
    val bitmap: ImageBitmap = ImageBitmap.imageResource(id = resId)

    Canvas(modifier = modifier.fillMaxSize()) {
        val bw = bitmap.width
        val bh = bitmap.height
        val sq = min(bw, bh)
        val originX = (bw - sq) / 2
        val originY = (bh - sq) / 2

        if (goalRow < 0 || goalCol < 0) {
            drawImage(
                image = bitmap,
                srcOffset = IntOffset(originX, originY),
                srcSize = IntSize(sq, sq),
                dstOffset = IntOffset.Zero,
                dstSize = IntSize(size.width.toInt().coerceAtLeast(1), size.height.toInt().coerceAtLeast(1)),
            )
        } else {
            val cellSrc = (sq / gridSize).coerceAtLeast(1)
            val srcX = originX + goalCol * cellSrc
            val srcY = originY + goalRow * cellSrc
            drawImage(
                image = bitmap,
                srcOffset = IntOffset(srcX, srcY),
                srcSize = IntSize(cellSrc, cellSrc),
                dstOffset = IntOffset.Zero,
                dstSize = IntSize(size.width.toInt().coerceAtLeast(1), size.height.toInt().coerceAtLeast(1)),
            )
        }
    }
}
