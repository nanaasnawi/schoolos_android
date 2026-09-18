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
                    .clip(RoundedCornerShape(22.dp))
                    .background(CosmicNavy)
                    .border(1.dp, GlassBorder, RoundedCornerShape(22.dp))
                    .padding(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🏖️", fontSize = 42.sp)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = if (selectedDayName.isNotBlank()) "Tidak Ada Jadwal di $selectedDayName" else "Tidak Ada Jadwal Pelajaran",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Tidak ada jadwal kelas atau sesi belajar pada hari yang dipilih.",
                        color = TextTertiary,
                        fontSize = 12.sp,
                    )
                    if (!isToday) {
                        Spacer(Modifier.height(16.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(accentColor.copy(alpha = 0.15f))
                                .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                                .clickable(onClick = onJumpToToday)
                                .padding(horizontal = 16.dp, vertical = 9.dp),
                        ) {
                            Text(
                                text = "Lihat Jadwal Hari Ini",
                                color = accentColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
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

    val (emoji, iconBg) = when {
        subjectName.contains("Matematika", ignoreCase = true) -> Pair("🧮", Color(0xFF6366F1))
        subjectName.contains("IPA", ignoreCase = true) || subjectName.contains("Fisika", ignoreCase = true) || subjectName.contains("Biologi", ignoreCase = true) || subjectName.contains("Kimia", ignoreCase = true) -> Pair("🔬", Color(0xFF0EA5E9))
        subjectName.contains("Bahasa", ignoreCase = true) -> Pair("📚", Color(0xFF10B981))
        subjectName.contains("Penjaskes", ignoreCase = true) || subjectName.contains("Olahraga", ignoreCase = true) -> Pair("⚽", Color(0xFFF97316))
        subjectName.contains("IPS", ignoreCase = true) || subjectName.contains("Sejarah", ignoreCase = true) || subjectName.contains("Geografi", ignoreCase = true) -> Pair("🌍", Color(0xFFEAB308))
        subjectName.contains("Agama", ignoreCase = true) || subjectName.contains("PAI", ignoreCase = true) -> Pair("🕌", Color(0xFF14B8A6))
        subjectName.contains("Seni", ignoreCase = true) || subjectName.contains("Budaya", ignoreCase = true) -> Pair("🎨", Color(0xFFA855F7))
        subjectName.contains("Komputer", ignoreCase = true) || subjectName.contains("Informatika", ignoreCase = true) -> Pair("💻", Color(0xFF06B6D4))
        else -> Pair("📖", Color(0xFF64748B))
    }

    val startHour = formatHourMinute(session.scheduledAt) ?: "07.30"
    val endHour = formatHourMinute(session.endedAt) ?: "09.00"
    val room = session.room ?: "Ruang Kelas"
    val teacherName = session.teacherName ?: "Guru Pengampu"
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
            modifier = Modifier.width(52.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = startHour,
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (isActive) NeonSuccess else if (isCompleted) TextSecondary else TextPrimary,
            )

            Spacer(Modifier.height(4.dp))

            // Dynamic Status Node
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isActive -> NeonSuccess.copy(alpha = 0.20f)
                            isCompleted -> NeonSuccess.copy(alpha = 0.15f)
                            else -> accentColor.copy(alpha = 0.12f)
                        }
                    )
                    .border(
                        width = 1.5.dp,
                        color = when {
                            isActive -> NeonSuccess.copy(alpha = pulseAlpha)
                            isCompleted -> NeonSuccess
                            else -> accentColor.copy(alpha = 0.5f)
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
                        modifier = Modifier.size(11.dp),
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (isActive) NeonSuccess else accentColor),
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            Text(
                text = endHour,
                fontSize = 10.sp,
                color = TextTertiary,
                fontWeight = FontWeight.Medium,
            )

            // Connecting Track Line
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .weight(1f)
                        .background(
                            if (isCompleted) NeonSuccess.copy(alpha = 0.35f)
                            else GlassBorder
                        ),
                )
            }
        }

        Spacer(Modifier.width(10.dp))

        // ── 2. RIGHT RICH SESSION CARD ───────────────────────────────────────────
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 12.dp)
                .shadow(
                    elevation = if (isActive) 8.dp else 3.dp,
                    shape = RoundedCornerShape(20.dp),
                    spotColor = if (isActive) accentColor.copy(alpha = 0.35f) else GlassOverlay,
                )
                .clip(RoundedCornerShape(20.dp))
                .background(CosmicNavy)
                .background(
                    if (isActive) Brush.linearGradient(listOf(accentColor.copy(alpha = 0.09f), Color.Transparent))
                    else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                )
                .border(
                    width = 1.dp,
                    color = when {
                        isActive -> accentColor.copy(alpha = 0.65f)
                        isCompleted -> NeonSuccess.copy(alpha = 0.25f)
                        else -> GlassBorder
                    },
                    shape = RoundedCornerShape(20.dp),
                )
                .clickable(onClick = onClick)
                .animateContentSize(),
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                // Color Accent Stripe on left
                Box(
                    modifier = Modifier
                        .width(5.dp)
                        .fillMaxHeight()
                        .background(if (isActive) NeonSuccess else iconBg),
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(14.dp),
                ) {
                    // Top Row: Emoji Avatar + Subject + Status Pill
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
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(iconBg.copy(alpha = 0.15f))
                                    .border(1.dp, iconBg.copy(alpha = 0.35f), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(emoji, fontSize = 18.sp)
                            }

                            Spacer(Modifier.width(10.dp))

                            Column {
                                Text(
                                    text = subjectName,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Spacer(Modifier.height(1.dp))
                                Text(
                                    text = teacherName,
                                    fontSize = 11.sp,
                                    color = TextTertiary,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }

                        // Status Badge Pill
                        when {
                            isActive -> {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(NeonSuccess.copy(alpha = 0.18f))
                                        .border(1.dp, NeonSuccess.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                ) {
                                    Text(
                                        text = "LIVE",
                                        color = NeonSuccess,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black,
                                    )
                                }
                            }
                            isCompleted -> {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(TextTertiary.copy(alpha = 0.12f))
                                        .padding(horizontal = 7.dp, vertical = 3.dp),
                                ) {
                                    Text(
                                        text = "SELESAI",
                                        color = TextTertiary,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                            else -> {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(accentColor.copy(alpha = 0.12f))
                                        .padding(horizontal = 7.dp, vertical = 3.dp),
                                ) {
                                    Text(
                                        text = "SEGERA",
                                        color = accentColor,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    // Location & Room Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isOnline) Icons.Default.Videocam else Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = TextTertiary,
                                modifier = Modifier.size(13.dp),
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = room,
                                fontSize = 11.sp,
                                color = TextSecondary,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }

                        // CTA Button
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isActive) accentColor
                                    else accentColor.copy(alpha = 0.12f)
                                )
                                .clickable(onClick = onClick)
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (isActive) "Masuk Kelas" else "Detail",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isActive) Color.White else accentColor,
                                )
                                Spacer(Modifier.width(3.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = if (isActive) Color.White else accentColor,
                                    modifier = Modifier.size(11.dp),
                                )
                            }
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