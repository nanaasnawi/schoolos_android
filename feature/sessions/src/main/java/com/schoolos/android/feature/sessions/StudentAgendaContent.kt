package com.schoolos.android.feature.sessions

import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.schoolos.android.core.designsystem.CosmicNavy
import com.schoolos.android.core.designsystem.GlassBorder
import com.schoolos.android.core.designsystem.GlassOverlay
import com.schoolos.android.core.designsystem.NeonBlue
import com.schoolos.android.core.designsystem.NeonError
import com.schoolos.android.core.designsystem.NeonSuccess
import com.schoolos.android.core.designsystem.NeonWarning
import com.schoolos.android.core.designsystem.StudentNeon
import com.schoolos.android.core.designsystem.TextPrimary
import com.schoolos.android.core.designsystem.TextSecondary
import com.schoolos.android.core.designsystem.TextTertiary
import com.schoolos.android.core.designsystem.subjectIcon
import com.schoolos.android.domain.model.LearningSession
import java.time.Instant
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
fun LazyListScope.studentAgendaContent(
    sessions: List<LearningSession>,
    onSessionClick: (String) -> Unit,
    accentColor: Color,
    selectedDayName: String = "",
    onJumpToToday: () -> Unit = {},
    isToday: Boolean = true,
) {
    if (sessions.isEmpty()) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 24.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(CosmicNavy)
                    .border(0.5.dp, GlassBorder, RoundedCornerShape(12.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(com.schoolos.android.core.designsystem.CosmicSurface2)
                            .border(0.5.dp, GlassBorder, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = if (selectedDayName.isNotBlank()) "Tidak Ada Jadwal di $selectedDayName" else "Tidak Ada Jadwal Pelajaran",
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Tidak ada jadwal kelas atau sesi belajar pada hari yang dipilih.",
                        color = TextTertiary,
                        fontSize = 12.sp,
                    )
                    if (!isToday) {
                        Spacer(Modifier.height(14.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(com.schoolos.android.core.designsystem.CosmicSurface2)
                                .border(0.5.dp, GlassBorder, RoundedCornerShape(8.dp))
                                .clickable(onClick = onJumpToToday)
                                .padding(horizontal = 14.dp, vertical = 7.dp),
                        ) {
                            Text(
                                text = "Lihat Jadwal Hari Ini",
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                }
            }
        }
    } else {
        itemsIndexed(sessions, key = { _, s -> s.id }) { index, session ->
            StudentTimelineItem(
                session = session,
                isLast = index == sessions.size - 1,
                accentColor = accentColor,
                onClick = { onSessionClick(session.id) },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StudentTimelineItem(
    session: LearningSession,
    isLast: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
) {
    val title = session.subjectName ?: session.notes ?: "Pelajaran"
    val subjectName = title.substringBefore(" • ").substringBefore(" (Ruang").trim()
    val isActive = session.status.equals("active", ignoreCase = true)
    val isCompleted = session.status.equals("completed", ignoreCase = true)

    val startHour = formatHourMinute(session.scheduledAt) ?: "07.30"
    val endHour = formatHourMinute(session.endedAt) ?: "09.00"
    val room = session.room ?: "Ruang Kelas"
    val teacherName = session.teacherName?.takeIf { it.isNotBlank() && !it.equals("Guru Pengampu", ignoreCase = true) } ?: "Guru Pengajar"
    val isOnline = room.contains("Online", ignoreCase = true) || room.contains("Meet", ignoreCase = true)

    // Pulse animation for active node
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_active_node")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse_node_alpha",
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
    ) {
        // ── 1. LEFT TIME RAIL ────────────────────────────────────────────────────
        Column(
            modifier = Modifier.width(48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = startHour,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isActive) NeonSuccess else if (isCompleted) TextSecondary else TextPrimary,
            )

            Spacer(Modifier.height(4.dp))

            // Status Node
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isActive -> NeonSuccess.copy(alpha = 0.20f)
                            isCompleted -> NeonSuccess.copy(alpha = 0.15f)
                            else -> com.schoolos.android.core.designsystem.CosmicSurface2
                        }
                    )
                    .border(
                        width = 1.dp,
                        color = when {
                            isActive -> NeonSuccess.copy(alpha = pulseAlpha)
                            isCompleted -> NeonSuccess
                            else -> GlassBorder
                        },
                        shape = CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (isCompleted) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = NeonSuccess,
                        modifier = Modifier.size(10.dp),
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(if (isActive) NeonSuccess else TextTertiary),
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            Text(
                text = endHour,
                fontSize = 10.sp,
                color = TextTertiary,
                fontWeight = FontWeight.Normal,
            )

            // Connecting Track Line
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .weight(1f)
                        .background(
                            if (isCompleted) NeonSuccess.copy(alpha = 0.3f)
                            else GlassBorder
                        ),
                )
            }
        }

        Spacer(Modifier.width(10.dp))

        // ── 2. RIGHT OBSIDIAN SESSION CARD ───────────────────────────────────────
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 10.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(CosmicNavy)
                .border(
                    width = 0.5.dp,
                    color = if (isActive) NeonSuccess.copy(alpha = 0.5f) else GlassBorder,
                    shape = RoundedCornerShape(12.dp),
                )
                .clickable(onClick = onClick)
                .padding(14.dp)
        ) {
            Column {
                // Top Row: Subject + Status Pill
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(com.schoolos.android.core.designsystem.CosmicSurface2)
                                .border(0.5.dp, GlassBorder, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = if (isOnline) Icons.Default.Videocam else subjectIcon(subjectName),
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(16.dp),
                            )
                        }

                        Spacer(Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = subjectName,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Spacer(Modifier.height(1.dp))
                            Text(
                                text = teacherName,
                                fontSize = 11.sp,
                                color = TextTertiary,
                                fontWeight = FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }

                    Spacer(Modifier.width(8.dp))

                    // Status Badge Pill
                    val (statusLabel, statusColor) = when {
                        isActive -> Pair("LIVE", NeonSuccess)
                        isCompleted -> Pair("SELESAI", TextTertiary)
                        else -> Pair("MENDATANG", TextSecondary)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isActive) NeonSuccess.copy(alpha = 0.12f) else com.schoolos.android.core.designsystem.CosmicSurface2)
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = statusLabel,
                            color = statusColor,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Location & Room Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f, fill = false),
                    ) {
                        Icon(
                            imageVector = if (isOnline) Icons.Default.Videocam else Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = TextTertiary,
                            modifier = Modifier.size(12.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = room,
                            fontSize = 11.sp,
                            color = TextSecondary,
                            fontWeight = FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    Spacer(Modifier.width(8.dp))

                    // CTA Button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(com.schoolos.android.core.designsystem.CosmicSurface2)
                            .border(0.5.dp, GlassBorder, RoundedCornerShape(6.dp))
                            .clickable(onClick = onClick)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (isActive) "Masuk" else "Detail",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isActive) NeonSuccess else TextSecondary,
                            )
                            Spacer(Modifier.width(3.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = if (isActive) NeonSuccess else TextSecondary,
                                modifier = Modifier.size(10.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatHourMinute(iso: String?): String? {
    if (iso.isNullOrBlank()) return null
    return try {
        val instant = Instant.parse(iso)
        val zdt = instant.atZone(ZoneId.of("Asia/Jakarta"))
        String.format(java.util.Locale.US, "%02d.%02d", zdt.hour, zdt.minute)
    } catch (_: Exception) {
        null
    }
}