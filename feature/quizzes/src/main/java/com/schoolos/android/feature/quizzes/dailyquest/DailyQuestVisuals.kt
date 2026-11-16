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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
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
    petNotice: String?,
    onFeedPet: () -> Unit,
    onOpenChest: () -> Unit,
    surfaceColor: Color,
    elevatedColor: Color,
    borderColor: Color,
    textPrimary: Color,
    textSecondary: Color,
) {
    val stage = pet?.petStage ?: 1
    val biscuits = pet?.biscuits ?: 0
    val ratio = if (totalGames > 0) (completedCount.toFloat() / totalGames.toFloat()).coerceIn(0f, 1f) else 0f
    val pctText = "${(ratio * 100f).toInt()}%"

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = surfaceColor,
        border = BorderStroke(1.5.dp, borderColor),
    ) {
        Column {
            // Full-bleed illustrated Game Hub Hero Stage
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(186.dp),
            ) {
                Image(
                    painter = painterResource(id = R.drawable.game_hub_hero),
                    contentDescription = "Zona Game Edukasi",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF0F172A).copy(alpha = 0.45f),
                                    Color.Transparent,
                                    Color(0xFF0F172A).copy(alpha = 0.88f),
                                )
                            )
                        ),
                )

                // Top floating HUD Badges
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
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
                                    text = "${pet?.petName ?: "Ciko Pintar"} • Lv.$stage",
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
                            color = Color(0xFF0F172A).copy(alpha = 0.74f),
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
                            color = Color(0xFF0F172A).copy(alpha = 0.74f),
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

                // Bottom Ribbon & Headline
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Brush.horizontalGradient(listOf(Color(0xFFEF4444), Color(0xFFF97316))))
                            .padding(horizontal = 9.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = "ZONA GAME AKSELERASI",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp,
                        )
                    }
                    Text(
                        text = "Main & Koleksi Koin + XP Harian!",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                    )
                }
            }

            // Bottom Glossy Progress & 3D Beveled Buttons
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "Misi Game Hari Ini: $completedCount/$totalGames Tuntas",
                        color = textPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                    )
                    Text(
                        text = "$pctText Selesai",
                        color = Color(0xFFF59E0B),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                    )
                }

                // Glossy 3D Progress Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(14.dp)
                        .clip(CircleShape)
                        .background(elevatedColor)
                        .border(1.5.dp, borderColor, CircleShape)
                        .padding(2.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(ratio.coerceAtLeast(0.06f))
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(
                                Brush.verticalGradient(
                                    if (completedCount >= totalGames) {
                                        listOf(Color(0xFF34D399), Color(0xFF059669))
                                    } else {
                                        listOf(Color(0xFFFBBF24), Color(0xFFEA580C))
                                    }
                                )
                            ),
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
