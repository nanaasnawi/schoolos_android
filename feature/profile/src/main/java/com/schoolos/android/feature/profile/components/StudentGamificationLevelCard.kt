package com.schoolos.android.feature.profile.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.schoolos.android.domain.model.GamificationStatus

@Composable
fun StudentGamificationLevelCard(
    gamification: GamificationStatus?,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
) {
    val level = gamification?.level ?: 1
    val totalXp = gamification?.totalXp ?: 0L
    val streakDays = gamification?.streakDays ?: 1
    val progressPercent = gamification?.progressToNextLevel ?: 0
    val targetXp = gamification?.nextLevelTargetXp ?: 50L
    val levelTitle = gamification?.levelTitle ?: GamificationStatus.resolveTitle(level)
    val rankBadge = gamification?.rankBadge ?: GamificationStatus.resolveBadge(level)
    val remainingXp = (targetXp - totalXp).coerceAtLeast(0L)

    val neonCyan = Color(0xFF06B6D4)
    val neonGold = Color(0xFFF59E0B)
    val neonEmerald = Color(0xFF10B981)

    val infiniteTransition = rememberInfiniteTransition(label = "LevelCardShine")
    val borderPulse by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "BorderPulse"
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF0F172A),
        border = androidx.compose.foundation.BorderStroke(
            1.5.dp,
            Brush.linearGradient(
                listOf(
                    neonGold.copy(alpha = borderPulse),
                    neonEmerald.copy(alpha = 0.6f),
                    neonCyan.copy(alpha = borderPulse)
                )
            )
        ),
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Row: Badge & Streak
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                Brush.linearGradient(listOf(neonGold, Color(0xFFD97706)))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Level & XP Murid",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Text(
                            text = "Pencapaian Gamifikasi Belajar",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                // Streak Badge
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFEA580C).copy(alpha = 0.2f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEA580C).copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = null,
                            tint = Color(0xFFFB923C),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "$streakDays Hari Beruntun",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFED7AA)
                        )
                    }
                }
            }

            HorizontalDivider(color = Color(0xFF334155).copy(alpha = 0.6f))

            // Main Hero: Level Badge & Total XP
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Circular Level Shield
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.sweepGradient(
                                listOf(
                                    neonGold,
                                    Color(0xFFFBBF24),
                                    neonEmerald,
                                    neonCyan,
                                    neonGold
                                )
                            )
                        )
                        .padding(3.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(Color(0xFF0F172A)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "LVL",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = neonGold,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "$level",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                lineHeight = 30.sp
                            )
                        }
                    }
                }

                // Level Name & Total XP
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = levelTitle,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = neonEmerald.copy(alpha = 0.2f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, neonEmerald.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = rankBadge.uppercase(),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = neonEmerald,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "$totalXp",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = neonGold
                        )
                        Text(
                            text = "XP Telah Dicapai",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFCBD5E1),
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }

                    Text(
                        text = "Diperoleh dari membaca materi, tugas & ujian CBT",
                        fontSize = 10.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            }

            // Progress Bar to Next Level
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1E293B).copy(alpha = 0.8f))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                            contentDescription = null,
                            tint = neonCyan,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Menuju Level ${level + 1}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Text(
                        text = "$progressPercent%",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = neonCyan
                    )
                }

                // Custom Animated Progress Bar
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape)
                ) {
                    // Track
                    drawRoundRect(
                        color = Color(0xFF334155),
                        cornerRadius = CornerRadius(size.height / 2f, size.height / 2f)
                    )

                    // Fill
                    val fillWidth = size.width * (progressPercent / 100f).coerceIn(0f, 1f)
                    if (fillWidth > 0f) {
                        drawRoundRect(
                            brush = Brush.horizontalGradient(
                                listOf(neonCyan, neonEmerald, neonGold)
                            ),
                            topLeft = Offset.Zero,
                            size = Size(fillWidth, size.height),
                            cornerRadius = CornerRadius(size.height / 2f, size.height / 2f)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (remainingXp > 0) "Kurang $remainingXp XP lagi untuk naik level" else "Level maksimal tercapai!",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFCBD5E1)
                    )
                    Text(
                        text = "Target: $targetXp XP",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF94A3B8)
                    )
                }
            }

            // Quick Stats Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GamificationStatPill(
                    modifier = Modifier.weight(1f),
                    title = "Level",
                    value = "Lvl $level",
                    accentColor = neonEmerald
                )
                GamificationStatPill(
                    modifier = Modifier.weight(1f),
                    title = "Total XP",
                    value = "$totalXp",
                    accentColor = neonGold
                )
                GamificationStatPill(
                    modifier = Modifier.weight(1f),
                    title = "Streak",
                    value = "$streakDays Hari",
                    accentColor = Color(0xFFFB923C)
                )
                GamificationStatPill(
                    modifier = Modifier.weight(1f),
                    title = "Target",
                    value = "$targetXp",
                    accentColor = neonCyan
                )
            }

            // Recent Transactions (if available)
            val txList = gamification?.transactions ?: emptyList()
            if (txList.isNotEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Riwayat Perolehan XP Terbaru",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFCBD5E1)
                    )

                    txList.take(3).forEach { tx ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF1E293B).copy(alpha = 0.5f))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val actionLabel = when (tx.actionType) {
                                "READ_MATERIAL" -> "📖 Modul Ajar Selesai"
                                "SUBMIT_ASSIGNMENT" -> "📝 Pengumpulan Tugas"
                                "COMPLETE_QUIZ" -> "🎯 Ujian Kuis CBT"
                                "COMPLETE_EXAM" -> "👑 Penilaian Sumatif CBT"
                                else -> "⭐ Aktivitas Pembelajaran"
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = actionLabel,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                                val desc = tx.description
                                if (!desc.isNullOrBlank()) {
                                    Text(
                                        text = desc,
                                        fontSize = 9.sp,
                                        color = Color(0xFF94A3B8),
                                        maxLines = 1
                                    )
                                }
                            }

                            Text(
                                text = "+${tx.xpAmount} XP",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = neonEmerald
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GamificationStatPill(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    accentColor: Color,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF1E293B).copy(alpha = 0.6f),
        border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF94A3B8)
            )
            Text(
                text = value,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = accentColor,
                maxLines = 1
            )
        }
    }
}
