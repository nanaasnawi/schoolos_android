package com.schoolos.android.core.designsystem

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

data class XpRewardData(
    val xpEarned: Int,
    val newTotalXp: Int,
    val level: Int,
    val title: String = "Luar Biasa!",
    val description: String = "Poin pengalaman belajar Anda bertambah.",
    val actionType: String = "ACTIVITY", // "READ_MATERIAL", "SUBMIT_ASSIGNMENT", "COMPLETE_QUIZ"
)

@Composable
fun XpCelebrationDialog(
    reward: XpRewardData?,
    onDismiss: () -> Unit,
) {
    if (reward == null) return

    var startAnimation by remember { mutableStateOf(false) }

    LaunchedEffect(reward) {
        startAnimation = true
    }

    val prevXp = (reward.newTotalXp - reward.xpEarned).coerceAtLeast(0)
    val animatedTotalXp by animateIntAsState(
        targetValue = if (startAnimation) reward.newTotalXp else prevXp,
        animationSpec = tween(durationMillis = 1400, delayMillis = 350, easing = FastOutSlowInEasing),
        label = "totalXpCounter",
    )

    val level = reward.level.coerceAtLeast(1)
    val currentLevelBase = ((level - 1) * (level - 1) * 50).toFloat()
    val nextLevelTarget = (level * level * 50).toFloat()
    val progressFraction = ((animatedTotalXp - currentLevelBase) / (nextLevelTarget - currentLevelBase).coerceAtLeast(1f))
        .coerceIn(0f, 1f)

    val animatedProgress by animateFloatAsState(
        targetValue = progressFraction,
        animationSpec = tween(durationMillis = 1400, delayMillis = 400, easing = FastOutSlowInEasing),
        label = "levelProgress",
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.78f)),
            contentAlignment = Alignment.Center,
        ) {
            // Confetti explosion behind the card
            ConfettiParticleEffect(
                modifier = Modifier.fillMaxSize(),
                particleCount = 100,
                durationMs = 2600,
            )

            // Celebration Card
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.88f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(CosmicNavy)
                    .border(1.dp, GlassBorder2, RoundedCornerShape(24.dp))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Glowing Trophy Badge
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .shadow(16.dp, CircleShape, spotColor = Color(0xFFF59E0B))
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFFF59E0B), Color(0xFFEA580C))
                            )
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = "⭐", fontSize = 34.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))

                val defaultHeading = when (reward.actionType) {
                    "READ_MATERIAL" -> "Materi Selesai Dibaca! 📖"
                    "SUBMIT_ASSIGNMENT" -> "Tugas Berhasil Dikumpulkan! 📝"
                    "COMPLETE_QUIZ" -> "Ujian CBT Selesai! 🏆"
                    else -> reward.title
                }

                Text(
                    text = defaultHeading,
                    color = TextPrimary,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = reward.description,
                    color = TextSecondary,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp,
                )

                Spacer(modifier = Modifier.height(20.dp))

                // XP Earned & Current Total Summary Card
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(CosmicSurface2)
                        .border(1.dp, Color(0xFFF59E0B).copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            text = "XP Diraih",
                            color = TextTertiary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "+${reward.xpEarned} XP",
                            color = Color(0xFFFBBF24),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Total XP Sekarang",
                            color = TextTertiary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "$animatedTotalXp XP",
                            color = Color(0xFF38BDF8),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Level Progress Bar
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "🎖️ Level $level",
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "${(progressFraction * 100).toInt()}% ke Level ${level + 1}",
                            color = TextSecondary,
                            fontSize = 11.sp,
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(CircleShape)
                            .background(CosmicSurface3),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(animatedProgress)
                                .height(8.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(Color(0xFF3B82F6), Color(0xFF8B5CF6), Color(0xFFEC4899))
                                    )
                                ),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Continue Button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2563EB),
                    ),
                ) {
                    Text(
                        text = "Keren, Lanjutkan! 🚀",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                    )
                }
            }
        }
    }
}
