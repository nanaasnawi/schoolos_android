package com.schoolos.android.feature.home

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.schoolos.android.core.designsystem.CosmicNavy
import com.schoolos.android.core.designsystem.GlassBorder
import com.schoolos.android.core.designsystem.GlassOverlay
import com.schoolos.android.core.designsystem.NeonBlue
import com.schoolos.android.core.designsystem.NeonError
import com.schoolos.android.core.designsystem.NeonSuccess
import com.schoolos.android.core.designsystem.StudentNeon
import com.schoolos.android.core.designsystem.TextPrimary
import com.schoolos.android.core.designsystem.TextSecondary
import com.schoolos.android.core.designsystem.TextTertiary

// ─── Data Models ─────────────────────────────────────────────────────────────
data class ScheduleItem(
    val time: String,
    val subject: String,
    val room: String,
    val icon: ImageVector,
    val accentColor: Color,
    val bgTint: Color,
)

data class QuickAction(
    val label: String,
    val icon: ImageVector,
    val accentColor: Color,
    val onClick: () -> Unit = {},
)

// ─── Modern Bento Action Card ────────────────────────────────────────────────
@Composable
fun BentoActionCard(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    badgeCount: Int = 0,
    badgeText: String = "",
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(22.dp),
                spotColor = accentColor.copy(alpha = 0.18f),
                ambientColor = GlassOverlay,
            )
            .clip(RoundedCornerShape(22.dp))
            .background(CosmicNavy)
            .background(
                Brush.linearGradient(
                    listOf(
                        accentColor.copy(alpha = 0.08f),
                        Color.Transparent,
                    )
                )
            )
            .border(1.dp, accentColor.copy(alpha = 0.22f), RoundedCornerShape(22.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                // Icon Box with rich gradient tint
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    accentColor.copy(alpha = 0.24f),
                                    accentColor.copy(alpha = 0.10f),
                                )
                            )
                        )
                        .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = accentColor,
                        modifier = Modifier.size(24.dp),
                    )
                }

                // Dynamic Badge
                if (badgeCount > 0) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(NeonError)
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                    ) {
                        Text(
                            text = "$badgeCount",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                        )
                    }
                } else if (badgeText.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(accentColor.copy(alpha = 0.15f))
                            .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 7.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = badgeText,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentColor,
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            Column {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Normal,
                    color = TextTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

// ─── Pulsing Live Status Indicator ───────────────────────────────────────────
@Composable
fun LiveStatusBadge(
    isLive: Boolean = true,
    text: String = if (isLive) "LIVE SEKARANG" else "AKAN DATANG",
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_live")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse_alpha",
    )

    val color = if (isLive) NeonSuccess else NeonBlue

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.20f))
            .border(1.dp, color.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
            .padding(horizontal = 9.dp, vertical = 4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = if (isLive) pulseAlpha else 1f)),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = text,
                color = color,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp,
            )
        }
    }
}

// ─── Modern Timeline Preview Row ─────────────────────────────────────────────
@Composable
fun ModernTimelineRow(
    time: String,
    subject: String,
    room: String,
    teacher: String = "",
    status: String = "upcoming", // "active", "completed", "upcoming"
    accentColor: Color = StudentNeon,
    onClick: () -> Unit = {},
) {
    val isLive = status.equals("active", ignoreCase = true)
    val isDone = status.equals("completed", ignoreCase = true)

    val nodeColor = when {
        isLive -> NeonSuccess
        isDone -> TextTertiary
        else -> accentColor
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Time with status node
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(52.dp),
        ) {
            Text(
                text = time,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (isLive) NeonSuccess else TextSecondary,
            )
            Spacer(Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(nodeColor),
            )
        }

        Spacer(Modifier.width(10.dp))

        // Subject content
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = subject,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (isLive) {
                    Spacer(Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(NeonSuccess.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        Text("LIVE", color = NeonSuccess, fontSize = 9.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = room,
                    fontSize = 11.sp,
                    color = TextTertiary,
                    fontWeight = FontWeight.Medium,
                )
                if (teacher.isNotBlank()) {
                    Text(
                        text = " • $teacher",
                        fontSize = 11.sp,
                        color = TextTertiary,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        Spacer(Modifier.width(8.dp))

        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = TextTertiary,
            modifier = Modifier.size(16.dp),
        )
    }
}

// ─── Legacy Compatible Shared Components ─────────────────────────────────────
@Composable
fun LightCard(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .shadow(6.dp, RoundedCornerShape(22.dp), spotColor = GlassOverlay, ambientColor = GlassOverlay)
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(CosmicNavy)
            .border(1.dp, GlassBorder, RoundedCornerShape(22.dp)),
    ) { content() }
}

@Composable
fun LightSectionHeader(title: String, sub: String = "", onSeeAll: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(
                text = title,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 17.sp,
                color = TextPrimary,
                letterSpacing = (-0.3).sp,
            )
            if (sub.isNotEmpty()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = sub,
                    fontSize = 12.sp,
                    color = TextTertiary,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
        if (onSeeAll != null) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(StudentNeon.copy(alpha = 0.10f))
                    .border(1.dp, StudentNeon.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                    .clickable(onClick = onSeeAll)
                    .padding(horizontal = 10.dp, vertical = 5.dp),
            ) {
                Text(
                    text = "Lihat Semua",
                    fontSize = 11.sp,
                    color = StudentNeon,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
fun LightScheduleRow(item: ScheduleItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.width(58.dp)) {
            Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(item.accentColor))
            Spacer(Modifier.width(6.dp))
            Text(item.time, fontSize = 12.sp, color = TextSecondary, fontWeight = FontWeight.ExtraBold)
        }

        Spacer(Modifier.width(10.dp))

        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(item.bgTint)
                .border(1.dp, item.accentColor.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(item.icon, null, tint = item.accentColor, modifier = Modifier.size(20.dp))
        }

        Spacer(Modifier.width(12.dp))

        Text(
            text = item.subject,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(GlassBorder)
                .padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            Text(
                text = item.room,
                fontSize = 10.sp,
                color = TextSecondary,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.End,
            )
        }
    }
}

@Composable
fun LightMiniStatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    sub: String,
    icon: ImageVector,
    accentColor: Color,
    badgeText: String = "",
    progress: Float? = null,
    onClick: () -> Unit = {},
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(CosmicNavy)
            .background(
                Brush.verticalGradient(
                    listOf(accentColor.copy(alpha = 0.08f), Color.Transparent),
                    startY = 0f, endY = 240f,
                ),
            )
            .border(1.dp, accentColor.copy(alpha = 0.20f), RoundedCornerShape(22.dp))
            .shadow(6.dp, RoundedCornerShape(22.dp), spotColor = accentColor.copy(alpha = 0.12f))
            .clickable(onClick = onClick)
            .padding(16.dp),
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(accentColor.copy(alpha = 0.12f))
                        .border(1.dp, accentColor.copy(alpha = 0.25f), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(icon, null, tint = accentColor, modifier = Modifier.size(20.dp))
                }

                if (badgeText.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(accentColor.copy(alpha = 0.14f))
                            .border(1.dp, accentColor.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                    ) {
                        Text(
                            text = badgeText,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = accentColor,
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = title,
                fontSize = 11.sp,
                color = TextSecondary,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.3.sp,
            )

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    fontWeight = FontWeight.Black,
                    fontSize = 32.sp,
                    color = accentColor,
                    letterSpacing = (-1).sp,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = sub,
                    fontSize = 11.sp,
                    color = TextTertiary,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 6.dp),
                )
            }

            if (progress != null) {
                Spacer(Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.12f)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress.coerceIn(0f, 1f))
                            .height(5.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.horizontalGradient(listOf(accentColor, accentColor.copy(alpha = 0.7f))),
                            ),
                    )
                }
            }
        }
    }
}

@Composable
fun LightProgressRow(subject: String, progress: Float, color: Color) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(subject, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(color.copy(alpha = 0.12f))
                    .padding(horizontal = 7.dp, vertical = 2.dp),
            ) {
                Text("${(progress * 100).toInt()}%", fontSize = 11.sp, fontWeight = FontWeight.Black, color = color)
            }
        }
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(4.dp))
                .background(color.copy(alpha = 0.1f)),
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).height(7.dp).clip(RoundedCornerShape(4.dp))
                    .background(Brush.horizontalGradient(listOf(color, color.copy(alpha = 0.75f)))),
            )
        }
    }
}

@Composable
fun LightQuickActionBtn(action: QuickAction) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = action.onClick),
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(action.accentColor.copy(alpha = 0.18f), action.accentColor.copy(alpha = 0.06f)),
                    ),
                )
                .border(1.dp, action.accentColor.copy(alpha = 0.30f), RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(action.icon, action.label, tint = action.accentColor, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = action.label,
            fontSize = 11.sp,
            color = TextSecondary,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun HeroStatItem(label: String, value: String, onClick: () -> Unit = {}) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(
            text = value,
            fontWeight = FontWeight.Black,
            fontSize = 17.sp,
            color = Color.White,
            letterSpacing = (-0.5).sp,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White.copy(alpha = 0.75f),
            letterSpacing = 0.6.sp,
        )
    }
}

@Composable
fun HeroStatDivider() {
    Box(
        modifier = Modifier
            .height(26.dp)
            .width(1.dp)
            .background(Color.White.copy(alpha = 0.20f)),
    )
}
