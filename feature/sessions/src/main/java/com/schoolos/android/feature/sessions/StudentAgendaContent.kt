package com.schoolos.android.feature.sessions

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
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
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.schoolos.android.core.designsystem.*
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
                    .padding(top = 16.dp, bottom = 24.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(CosmicNavy)
                    .border(1.dp, GlassBorder, RoundedCornerShape(20.dp))
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🏖️", fontSize = 40.sp)
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = if (selectedDayName.isNotBlank()) "Tidak Ada Jadwal di $selectedDayName" else "Tidak Ada Jadwal Pelajaran",
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
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
                                .clip(RoundedCornerShape(12.dp))
                                .background(accentColor.copy(alpha = 0.15f))
                                .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                                .clickable(onClick = onJumpToToday)
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "Lihat Jadwal Hari Ini",
                                color = accentColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    } else {
        items(sessions, key = { it.id }) { session ->
            StudentSessionCard(
                session = session,
                accentColor = accentColor,
                onClick = { onSessionClick(session.id) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StudentSessionCard(
    session: LearningSession,
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

    val period = formatSessionPeriod(session.scheduledAt)
    val timeText = formatStudentTime(session.scheduledAt, session.endedAt)
    val room = session.room ?: "Ruang Kelas"
    val teacherName = session.teacherName ?: "Guru Pengampu"
    val isOnline = room.contains("Online", ignoreCase = true) || room.contains("Meet", ignoreCase = true)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (isActive) 6.dp else 2.dp,
                shape = RoundedCornerShape(20.dp),
                spotColor = if (isActive) accentColor.copy(alpha = 0.35f) else GlassOverlay,
                ambientColor = if (isActive) accentColor.copy(alpha = 0.15f) else Color.Transparent
            )
            .clip(RoundedCornerShape(20.dp))
            .background(CosmicNavy)
            .background(
                if (isActive) Brush.linearGradient(listOf(accentColor.copy(alpha = 0.08f), Color.Transparent))
                else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
            )
            .border(
                width = 1.dp,
                color = when {
                    isActive -> accentColor.copy(alpha = 0.6f)
                    isCompleted -> NeonSuccess.copy(alpha = 0.25f)
                    else -> GlassBorder
                },
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick)
            .animateContentSize(),
    ) {
        Column {
            // Main Info Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Emoji / Subject Avatar
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(iconBg.copy(alpha = 0.14f))
                        .border(1.dp, iconBg.copy(alpha = 0.3f), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(emoji, fontSize = 22.sp)
                }

                Spacer(Modifier.width(12.dp))

                // Subject Title & Teacher
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = subjectName,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = if (isCompleted) TextSecondary else TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(3.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Oleh $teacherName",
                            fontSize = 11.sp,
                            color = TextTertiary,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "• $period",
                            fontSize = 11.sp,
                            color = TextTertiary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(Modifier.width(8.dp))

                // Status Badge
                when {
                    isActive -> {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(NeonSuccess.copy(alpha = 0.15f))
                                .border(1.dp, NeonSuccess.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(NeonSuccess)
                                        .animateInfinitePulse(NeonSuccess)
                                )
                                Spacer(Modifier.width(5.dp))
                                Text(
                                    text = "LIVE",
                                    fontSize = 10.sp,
                                    color = NeonSuccess,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    }
                    isCompleted -> {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.06f))
                                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, null, tint = NeonSuccess, modifier = Modifier.size(12.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "Selesai",
                                    fontSize = 10.sp,
                                    color = TextSecondary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    else -> {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(CosmicDark)
                                .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                        ) {
                            Text(
                                text = "Terjadwal",
                                fontSize = 10.sp,
                                color = TextTertiary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = GlassBorder, thickness = 1.dp)

            // Metadata Bottom Row (Time, Room, Arrow Action)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Time
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = if (isActive) accentColor else TextTertiary,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = timeText,
                            fontSize = 11.sp,
                            color = if (isActive) TextPrimary else TextSecondary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Room
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isOnline) Icons.Default.Videocam else Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = TextTertiary,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = room.substringBefore(" / ").take(22),
                            fontSize = 11.sp,
                            color = TextTertiary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Quick Action Pill
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Buka Sesi",
                    tint = if (isActive) accentColor else TextTertiary,
                    modifier = Modifier.size(14.dp)
                )
            }

            // Active Progress Indicator Bar
            if (isActive) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(accentColor)
                )
            }
        }
    }
}

@Composable
private fun Modifier.animateInfinitePulse(color: Color): Modifier = composed {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "pulseAlpha",
    )
    this.drawBehind {
        drawCircle(
            color = color.copy(alpha = alpha),
            radius = (4.dp).toPx(),
            center = androidx.compose.ui.geometry.Offset((3.dp).toPx(), (3.dp).toPx()),
        )
    }
}

private fun formatStudentTime(startIso: String?, endIso: String?): String {
    val start = formatHourMinute(startIso) ?: "08.00"
    val end = formatHourMinute(endIso) ?: "09.30"
    return "$start - $end WIB"
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