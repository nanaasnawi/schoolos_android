package com.schoolos.android.feature.quizzes.dailyquest

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.schoolos.android.domain.model.ShibaPetStatus

@Composable
internal fun ArcadeCatMascotCanvas(
    stage: Int,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        fun px(v: Float) = v * (w / 96f)
        fun py(v: Float) = v * (h / 96f)

        // Cardboard Box
        drawRoundRect(
            color = Color(0xFFD97706),
            topLeft = Offset(px(18f), py(60f)),
            size = Size(px(60f), py(26f)),
            cornerRadius = CornerRadius(px(6f), px(6f)),
        )
        drawRect(
            color = Color(0xFFFBBF24),
            topLeft = Offset(px(14f), py(55f)),
            size = Size(px(68f), py(9f)),
        )

        // Ears
        val leftEar = Path().apply {
            moveTo(px(27f), py(38f))
            lineTo(px(23f), py(19f))
            lineTo(px(40f), py(28f))
            close()
        }
        val rightEar = Path().apply {
            moveTo(px(69f), py(38f))
            lineTo(px(73f), py(19f))
            lineTo(px(56f), py(28f))
            close()
        }
        drawPath(leftEar, Color(0xFFF8FAFC))
        drawPath(rightEar, Color(0xFFF8FAFC))

        // Head
        drawOval(
            color = Color.White,
            topLeft = Offset(px(25f), py(25f)),
            size = Size(px(46f), py(38f)),
        )
        drawOval(
            color = Color(0xFFCBD5E1),
            topLeft = Offset(px(25f), py(25f)),
            size = Size(px(46f), py(38f)),
            style = Stroke(width = px(1.5f)),
        )

        // Forehead stripes
        drawLine(Color(0xFF38BDF8), Offset(px(44f), py(26f)), Offset(px(44f), py(32f)), strokeWidth = px(2.5f))
        drawLine(Color(0xFF38BDF8), Offset(px(48f), py(25f)), Offset(px(48f), py(33f)), strokeWidth = px(2.5f))
        drawLine(Color(0xFF38BDF8), Offset(px(52f), py(26f)), Offset(px(52f), py(32f)), strokeWidth = px(2.5f))

        // Eyes
        drawCircle(Color(0xFF0F172A), radius = px(4.8f), center = Offset(px(38f), py(44f)))
        drawCircle(Color(0xFF0F172A), radius = px(4.8f), center = Offset(px(58f), py(44f)))
        drawCircle(Color.White, radius = px(1.8f), center = Offset(px(36.5f), py(42.5f)))
        drawCircle(Color.White, radius = px(1.8f), center = Offset(px(56.5f), py(42.5f)))

        if (stage >= 2) {
            drawRoundRect(
                color = Color(0xFF0F172A),
                topLeft = Offset(px(30f), py(38f)),
                size = Size(px(16f), py(12f)),
                cornerRadius = CornerRadius(px(4f), px(4f)),
                style = Stroke(width = px(2f)),
            )
            drawRoundRect(
                color = Color(0xFF0F172A),
                topLeft = Offset(px(50f), py(38f)),
                size = Size(px(16f), py(12f)),
                cornerRadius = CornerRadius(px(4f), px(4f)),
                style = Stroke(width = px(2f)),
            )
        }

        // Nose & Paws
        drawCircle(Color(0xFFF43F5E), radius = px(2.6f), center = Offset(px(48f), py(49f)))
        drawOval(Color.White, topLeft = Offset(px(26f), py(55f)), size = Size(px(12f), py(8f)))
        drawOval(Color.White, topLeft = Offset(px(58f), py(55f)), size = Size(px(12f), py(8f)))

        if (stage >= 3) {
            val crown = Path().apply {
                moveTo(px(38f), py(23f))
                lineTo(px(41f), py(13f))
                lineTo(px(48f), py(19f))
                lineTo(px(55f), py(13f))
                lineTo(px(58f), py(23f))
                close()
            }
            drawPath(crown, Color(0xFFFACC15))
        }
    }
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
    val energy = pet?.foodPoints ?: 25
    val ratio = if (totalGames > 0) (completedCount.toFloat() / totalGames.toFloat()).coerceIn(0f, 1f) else 0f

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = surfaceColor,
        border = BorderStroke(1.dp, borderColor),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(elevatedColor),
                    contentAlignment = Alignment.Center,
                ) {
                    ArcadeCatMascotCanvas(stage = stage, modifier = Modifier.size(68.dp))
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(shape = RoundedCornerShape(50), color = Color(0xFFF59E0B)) {
                            Text(
                                text = "${pet?.petName ?: "Ciko Pintar"} • Lv.$stage",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color(0xFF0EA5E9).copy(alpha = 0.14f),
                        ) {
                            Text(
                                text = "$biscuits Koin",
                                color = Color(0xFF0EA5E9),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            )
                        }
                    }

                    Text(
                        text = "Pilih Game Favoritmu Hari Ini!",
                        color = textPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                    )
                    Text(
                        text = "Mainkan 4 game logika & kata dinamis, raih +XP & Streak ($streakDays hari).",
                        color = textSecondary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = elevatedColor,
                border = BorderStroke(1.dp, borderColor),
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "Progres Harian: $completedCount/$totalGames Selesai",
                            color = textPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                        )
                        Text(
                            text = "Energi $energy%",
                            color = Color(0xFF0EA5E9),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(CircleShape)
                            .background(borderColor),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(ratio.coerceAtLeast(0.03f))
                                .height(8.dp)
                                .clip(CircleShape)
                                .background(if (completedCount >= totalGames) Color(0xFF10B981) else Color(0xFF0EA5E9)),
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Button(
                            onClick = onFeedPet,
                            enabled = !feedingPet && biscuits > 0,
                            shape = RoundedCornerShape(11.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFF59E0B),
                                contentColor = Color.White,
                                disabledContainerColor = surfaceColor,
                                disabledContentColor = textSecondary,
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
                            Text(
                                text = " Beri Makan ($biscuits)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                maxLines = 1,
                                softWrap = false,
                            )
                        }

                        Button(
                            onClick = onOpenChest,
                            enabled = !openingChest && !chestClaimedToday,
                            shape = RoundedCornerShape(11.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF10B981),
                                contentColor = Color.White,
                                disabledContainerColor = surfaceColor,
                                disabledContentColor = textSecondary,
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Default.CardGiftcard, contentDescription = null, modifier = Modifier.size(14.dp))
                            Text(
                                text = if (chestClaimedToday) " Diklaim ✓" else " Peti Harian",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                maxLines = 1,
                                softWrap = false,
                            )
                        }
                    }

                    if (!petNotice.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFF59E0B).copy(alpha = 0.14f),
                        ) {
                            Text(
                                text = petNotice,
                                color = Color(0xFFF59E0B),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Draws full or tile-cropped artwork for Sliding Puzzle so each tile renders its crisp piece.
 */
@Composable
internal fun SlidingPuzzleArtworkSlice(
    themeId: String,
    gridSize: Int,
    goalRow: Int, // Pass -1 to render full preview image!
    goalCol: Int,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        clipRect {
            if (goalRow < 0 || goalCol < 0) {
                drawPuzzleScene(themeId, size.width, size.height)
            } else {
                val fullW = size.width * gridSize
                val fullH = size.height * gridSize
                translate(left = -goalCol * size.width, top = -goalRow * size.height) {
                    drawPuzzleScene(themeId, fullW, fullH)
                }
            }
        }
    }
}

private fun DrawScope.drawPuzzleScene(themeId: String, w: Float, h: Float) {
    fun px(x: Float) = x * (w / 300f)
    fun py(y: Float) = y * (h / 300f)

    when (themeId) {
        "TATA_SURYA" -> {
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFF0F172A), Color(0xFF1E1B4B), Color(0xFF312E81)),
                ),
                size = Size(w, h),
            )
            // Stars
            listOf(32f to 40f, 88f to 24f, 240f to 42f, 272f to 110f, 44f to 250f, 255f to 260f).forEach { (sx, sy) ->
                drawCircle(Color(0xFFFDE68A), radius = px(2.5f), center = Offset(px(sx), py(sy)))
            }
            // Orbits
            drawCircle(Color(0x5594A3B8), radius = px(68f), center = Offset(px(150f), py(150f)), style = Stroke(width = px(2f)))
            drawCircle(Color(0x4494A3B8), radius = px(108f), center = Offset(px(150f), py(150f)), style = Stroke(width = px(2f)))
            // Sun
            drawCircle(Color(0xFFF59E0B), radius = px(35f), center = Offset(px(150f), py(150f)))
            drawCircle(Color(0xFFFEF08A), radius = px(24f), center = Offset(px(150f), py(150f)))
            // Earth
            drawCircle(Color(0xFF0EA5E9), radius = px(18f), center = Offset(px(90f), py(94f)))
            drawCircle(Color(0xFF10B981), radius = px(7f), center = Offset(px(92f), py(92f)))
            // Saturn
            drawOval(Color(0xFFFDE68A), topLeft = Offset(px(196f), py(188f)), size = Size(px(62f), py(22f)), style = Stroke(width = px(4f)))
            drawCircle(Color(0xFFF97316), radius = px(18f), center = Offset(px(227f), py(199f)))
            // Rocket
            drawCircle(Color(0xFFEF4444), radius = px(14f), center = Offset(px(72f), py(224f)))
            drawCircle(Color(0xFF38BDF8), radius = px(6f), center = Offset(px(72f), py(224f)))
        }

        "NUSANTARA" -> {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF38BDF8), Color(0xFFBAE6FD), Color(0xFFFDE68A)),
                ),
                size = Size(w, h),
            )
            drawCircle(Color(0xFFFBBF24), radius = px(32f), center = Offset(px(225f), py(70f)))
            val m1 = Path().apply {
                moveTo(0f, py(215f))
                lineTo(px(95f), py(105f))
                lineTo(px(195f), py(215f))
                close()
            }
            val m2 = Path().apply {
                moveTo(px(120f), py(215f))
                lineTo(px(210f), py(120f))
                lineTo(px(300f), py(215f))
                close()
            }
            drawPath(m1, Color(0xFF0F766E))
            drawPath(m2, Color(0xFF047857))
            drawRect(Color(0xFF0284C7), topLeft = Offset(0f, py(212f)), size = Size(w, py(88f)))
            drawRect(Color(0xFF78350F), topLeft = Offset(px(70f), py(232f)), size = Size(px(65f), py(18f)))
            val sail = Path().apply {
                moveTo(px(102f), py(155f))
                lineTo(px(102f), py(230f))
                lineTo(px(140f), py(226f))
                close()
            }
            drawPath(sail, Color.White)
        }

        "SAMUDRA" -> {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF0EA5E9), Color(0xFF0369A1), Color(0xFF0F172A)),
                ),
                size = Size(w, h),
            )
            drawRect(Color(0xFFFDE68A), topLeft = Offset(0f, py(242f)), size = Size(w, py(58f)))
            drawLine(Color(0xFFF43F5E), Offset(px(42f), py(250f)), Offset(px(42f), py(188f)), strokeWidth = px(12f))
            drawLine(Color(0xFF10B981), Offset(px(250f), py(250f)), Offset(px(250f), py(184f)), strokeWidth = px(12f))
            // Submarine
            drawOval(Color(0xFFFACC15), topLeft = Offset(px(92f), py(108f)), size = Size(px(114f), py(56f)))
            drawRect(Color(0xFFFACC15), topLeft = Offset(px(134f), py(90f)), size = Size(px(26f), py(22f)))
            drawCircle(Color(0xFF38BDF8), radius = px(10f), center = Offset(px(122f), py(136f)))
            drawCircle(Color(0xFF38BDF8), radius = px(10f), center = Offset(px(152f), py(136f)))
            drawCircle(Color(0xFF38BDF8), radius = px(10f), center = Offset(px(182f), py(136f)))
            // Clownfish
            drawOval(Color(0xFFF97316), topLeft = Offset(px(200f), py(64f)), size = Size(px(42f), py(24f)))
        }

        "KOTA_HIJAU" -> {
            drawRect(Color(0xFFE0F2FE), size = Size(w, h))
            drawCircle(Color(0xFFFBBF24), radius = px(28f), center = Offset(px(66f), py(64f)))
            drawRoundRect(Color(0xFF3B82F6), topLeft = Offset(px(35f), py(135f)), size = Size(px(52f), py(115f)), cornerRadius = CornerRadius(px(6f)))
            drawRoundRect(Color(0xFF0EA5E9), topLeft = Offset(px(100f), py(95f)), size = Size(px(64f), py(155f)), cornerRadius = CornerRadius(px(8f)))
            drawRoundRect(Color(0xFF8B5CF6), topLeft = Offset(px(178f), py(122f)), size = Size(px(56f), py(128f)), cornerRadius = CornerRadius(px(6f)))
            drawRect(Color(0xFFFEF08A), topLeft = Offset(px(116f), py(116f)), size = Size(px(14f), py(16f)))
            drawRect(Color(0xFFFEF08A), topLeft = Offset(px(138f), py(116f)), size = Size(px(14f), py(16f)))
            drawRect(Color(0xFF10B981), topLeft = Offset(0f, py(238f)), size = Size(w, py(62f)))
        }

        else -> { // LAB_SAINS
            drawRect(Color(0xFF1E293B), size = Size(w, h))
            drawRect(Color(0xFF334155), topLeft = Offset(0f, py(218f)), size = Size(w, py(82f)))
            drawOval(Color(0xFF38BDF8), topLeft = Offset(px(36f), py(58f)), size = Size(px(84f), py(36f)), style = Stroke(width = px(3f)))
            drawCircle(Color(0xFFFACC15), radius = px(10f), center = Offset(px(78f), py(76f)))
            val flask = Path().apply {
                moveTo(px(128f), py(85f))
                lineTo(px(156f), py(85f))
                lineTo(px(192f), py(205f))
                lineTo(px(90f), py(205f))
                close()
            }
            drawPath(flask, Color(0xFFE0F2FE))
            val liquid = Path().apply {
                moveTo(px(104f), py(172f))
                lineTo(px(178f), py(172f))
                lineTo(px(190f), py(205f))
                lineTo(px(90f), py(205f))
                close()
            }
            drawPath(liquid, Color(0xFF10B981))
            drawRoundRect(Color(0xFFFCE7F3), topLeft = Offset(px(208f), py(130f)), size = Size(px(52f), py(76f)), cornerRadius = CornerRadius(px(6f)))
            drawRect(Color(0xFFF43F5E), topLeft = Offset(px(210f), py(166f)), size = Size(px(48f), py(38f)))
        }
    }
}
