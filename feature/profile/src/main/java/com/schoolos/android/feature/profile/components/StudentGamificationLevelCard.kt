package com.schoolos.android.feature.profile.components

import com.schoolos.android.core.R as CoreR
import androidx.compose.ui.res.painterResource
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.schoolos.android.core.designsystem.*
import com.schoolos.android.domain.model.GamificationStatus

@Composable
fun StudentGamificationLevelCard(
    gamification: GamificationStatus?,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
) {
    val isDark = LocalIsDarkTheme.current
    val cardBg = if (isDark) Color(0xFF111113) else Color.White
    val cardBorder = if (isDark) Color(0xFF1F1F23) else Color(0xFFE2E8F0)
    val textPrimary = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFFA1A1AA) else Color(0xFF64748B)
    val textTertiary = if (isDark) Color(0xFF71717A) else Color(0xFF94A3B8)
    val innerCardBg = if (isDark) Color(0xFF161618) else Color(0xFFF8FAFC)
    val innerCardBorder = if (isDark) Color(0xFF27272A) else Color(0xFFE2E8F0)

    val studentPrimary = if (isDark) Color(0xFF818CF8) else Color(0xFF4F46E5)
    val successColor = if (isDark) Color(0xFF10B981) else Color(0xFF059669)
    val amberColor = if (isDark) Color(0xFFF59E0B) else Color(0xFFD97706)

    val level = gamification?.level ?: 1
    val totalXp = gamification?.totalXp ?: 0L
    val streakDays = gamification?.streakDays ?: 1
    val progressPercent = (gamification?.progressToNextLevel ?: 0).coerceIn(0, 100)
    val targetXp = gamification?.nextLevelTargetXp ?: 50L
    val levelTitle = gamification?.levelTitle ?: GamificationStatus.resolveTitle(level)
    val rankBadge = gamification?.rankBadge ?: GamificationStatus.resolveBadge(level)
    val remainingXp = (targetXp - totalXp).coerceAtLeast(0L)
    val transactions = gamification?.transactions ?: emptyList()

    var showHistory by remember { mutableStateOf(false) }

    val animatedProgress by animateFloatAsState(
        targetValue = progressPercent / 100f,
        animationSpec = tween(durationMillis = 800),
        label = "XpProgressAnimation"
    )

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ── Top Header Row ─────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Stars,
                        contentDescription = null,
                        tint = studentPrimary,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = "PENCAPAIAN LEVEL & XP",
                        color = textTertiary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.5.sp,
                        maxLines = 1,
                    )
                }

                // Streak Badge Pill
                val streakBg = if (isDark) Color(0xFF311C08) else Color(0xFFFFFBEB)
                val streakBorder = if (isDark) Color(0xFFEA580C).copy(alpha = 0.45f) else Color(0xFFFDE68A)
                val streakText = if (isDark) Color(0xFFF97316) else Color(0xFFEA580C)

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = streakBg,
                    border = BorderStroke(0.5.dp, streakBorder)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = null,
                            tint = streakText,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "$streakDays Hari Aktif",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = streakText
                        )
                    }
                }
            }

            // ── Hero Level & Title Row ─────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Sleek Level Badge
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    Color(0xFF4F46E5),
                                    Color(0xFF6366F1)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "LVL",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.85f),
                            letterSpacing = 0.5.sp,
                            lineHeight = 10.sp
                        )
                        Text(
                            text = "$level",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            lineHeight = 21.sp
                        )
                    }
                }

                // Title, Rank Badge, and XP info
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = levelTitle,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = successColor.copy(alpha = if (isDark) 0.15f else 0.10f),
                            border = BorderStroke(0.5.dp, successColor.copy(alpha = if (isDark) 0.40f else 0.25f))
                        ) {
                            Text(
                                text = rankBadge.uppercase(),
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Black,
                                color = successColor,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            text = "$totalXp XP",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = amberColor
                        )
                        Text(
                            text = if (remainingXp > 0) "• Kurang $remainingXp XP lagi" else "• Level Maksimal",
                            fontSize = 11.5.sp,
                            color = textTertiary
                        )
                    }
                }
            }

            // ── Clean Linear Progress Bar ──────────────────────────────
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Menuju Level ${level + 1}",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = textSecondary
                    )
                    Text(
                        text = "$progressPercent%",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = studentPrimary
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(7.dp)
                        .clip(CircleShape)
                        .background(innerCardBg)
                        .border(0.5.dp, innerCardBorder, CircleShape)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedProgress)
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color(0xFF6366F1),
                                        Color(0xFF818CF8)
                                    )
                                )
                            )
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Basis: ${level * 50 - 50} XP",
                        fontSize = 9.5.sp,
                        color = textTertiary
                    )
                    Text(
                        text = "Target: $targetXp XP",
                        fontSize = 9.5.sp,
                        color = textTertiary
                    )
                }
            }

            // ── 3-Column Key Metric Grid ───────────────────────────────
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = innerCardBg,
                border = BorderStroke(1.dp, innerCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 11.dp, horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "LEVEL",
                            color = textTertiary,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "Lvl $level",
                            color = studentPrimary,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .width(0.5.dp)
                            .height(22.dp)
                            .background(innerCardBorder)
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "TOTAL XP",
                            color = textTertiary,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "$totalXp",
                            color = amberColor,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .width(0.5.dp)
                            .height(22.dp)
                            .background(innerCardBorder)
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "TARGET LVL ${level + 1}",
                            color = textTertiary,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "$targetXp XP",
                            color = textPrimary,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // ── XP Activity History (Optional Expandable) ──────────────
            if (transactions.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showHistory = !showHistory }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = amberColor,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "Riwayat Perolehan XP (${transactions.size})",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = textSecondary
                        )
                    }
                    Icon(
                        imageVector = if (showHistory) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = textTertiary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                AnimatedVisibility(visible = showHistory) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        transactions.take(4).forEach { tx ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = innerCardBg,
                                border = BorderStroke(0.5.dp, innerCardBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 7.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(id = CoreR.drawable.ic_modern_check_circle),
                                            contentDescription = null,
                                            tint = successColor,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text(
                                            text = tx.description ?: tx.actionType,
                                            fontSize = 11.5.sp,
                                            color = textSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Text(
                                        text = "+${tx.xpAmount} XP",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = amberColor
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
