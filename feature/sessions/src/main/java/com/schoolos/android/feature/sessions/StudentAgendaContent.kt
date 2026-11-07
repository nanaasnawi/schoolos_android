package com.schoolos.android.feature.sessions

import com.schoolos.android.core.R as CoreR
import androidx.compose.ui.res.painterResource
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.schoolos.android.core.designsystem.LocalIsDarkTheme
import com.schoolos.android.core.designsystem.subjectIcon
import com.schoolos.android.domain.model.LearningSession
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

private data class StatusPillStyle(
    val label: String,
    val bg: Color,
    val border: Color,
    val textColor: Color,
)

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
            StudentAgendaEmptyCard(
                selectedDayName = selectedDayName,
                isToday = isToday,
                onJumpToToday = onJumpToToday,
            )
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

@Composable
private fun StudentAgendaEmptyCard(
    selectedDayName: String,
    isToday: Boolean,
    onJumpToToday: () -> Unit,
) {
    val isDark = LocalIsDarkTheme.current
    val cardBg = if (isDark) Color(0xFF111113) else Color.White
    val cardBorder = if (isDark) Color(0xFF1F1F23) else Color(0xFFE2E8F0)
    val textPrimary = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFFA1A1AA) else Color(0xFF64748B)
    val textTertiary = if (isDark) Color(0xFF71717A) else Color(0xFF94A3B8)
    val innerCardBg = if (isDark) Color(0xFF161618) else Color(0xFFF8FAFC)
    val innerCardBorder = if (isDark) Color(0xFF27272A) else Color(0xFFE2E8F0)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 24.dp),
        shape = RoundedCornerShape(22.dp),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder),
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(innerCardBg)
                    .border(1.dp, innerCardBorder, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = CoreR.drawable.ic_modern_calendar),
                    contentDescription = null,
                    tint = textSecondary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(Modifier.height(14.dp))
            Text(
                text = if (selectedDayName.isNotBlank()) "Tidak Ada Jadwal di $selectedDayName" else "Tidak Ada Jadwal Pelajaran",
                color = textPrimary,
                fontSize = 15.5.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Tidak ada jadwal kelas atau sesi belajar pada hari yang dipilih.",
                color = textTertiary,
                fontSize = 12.sp,
            )
            if (!isToday) {
                Spacer(Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(innerCardBg)
                        .border(1.dp, innerCardBorder, RoundedCornerShape(10.dp))
                        .clickable(onClick = onJumpToToday)
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                ) {
                    Text(
                        text = "Lihat Jadwal Hari Ini",
                        color = textPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
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
    val isDark = LocalIsDarkTheme.current
    val cardBg = if (isDark) Color(0xFF111113) else Color.White
    val cardBorder = if (isDark) Color(0xFF1F1F23) else Color(0xFFE2E8F0)
    val textPrimary = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFFA1A1AA) else Color(0xFF64748B)
    val textTertiary = if (isDark) Color(0xFF71717A) else Color(0xFF94A3B8)
    val innerCardBg = if (isDark) Color(0xFF161618) else Color(0xFFF8FAFC)
    val innerCardBorder = if (isDark) Color(0xFF27272A) else Color(0xFFE2E8F0)
    val studentNameColor = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)
    val successColor = if (isDark) Color(0xFF10B981) else Color(0xFF16A34A)
    val primaryBlue = studentNameColor

    val title = session.subjectName ?: session.notes ?: "Pelajaran"
    val subjectName = title.substringBefore(" • ").substringBefore(" (Ruang").trim()
    val isActive = session.status.equals("active", ignoreCase = true)
    val isCompleted = session.status.equals("completed", ignoreCase = true)

    val startHour = formatHourMinute(session.startTime)
        ?: formatHourMinute(session.scheduledAt)
        ?: formatHourMinute(session.startedAt)
        ?: "07.30"
    val endHour = formatHourMinute(session.endTime)
        ?: formatHourMinute(session.endedAt)
        ?: "09.00"
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
                fontWeight = FontWeight.Bold,
                color = if (isActive) successColor else if (isCompleted) textSecondary else textPrimary,
            )

            Spacer(Modifier.height(4.dp))

            // Status Node
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isActive -> successColor.copy(alpha = 0.20f)
                            isCompleted -> if (isDark) successColor.copy(alpha = 0.15f) else Color(0xFFDCFCE7)
                            else -> if (isDark) Color(0xFF161618) else Color(0xFFF1F5F9)
                        }
                    )
                    .border(
                        width = 1.dp,
                        color = when {
                            isActive -> successColor.copy(alpha = pulseAlpha)
                            isCompleted -> successColor
                            else -> if (isDark) Color(0xFF27272A) else Color(0xFFCBD5E1)
                        },
                        shape = CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (isCompleted) {
                    Icon(
                        painter = painterResource(id = CoreR.drawable.ic_modern_check_circle),
                        contentDescription = null,
                        tint = successColor,
                        modifier = Modifier.size(11.dp),
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(if (isActive) successColor else textTertiary),
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            Text(
                text = endHour,
                fontSize = 10.sp,
                color = textTertiary,
                fontWeight = FontWeight.Normal,
            )

            // Connecting Track Line
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(1.5.dp)
                        .weight(1f)
                        .background(
                            if (isCompleted) successColor.copy(alpha = 0.35f)
                            else if (isDark) Color(0xFF27272A) else Color(0xFFE2E8F0)
                        ),
                )
            }
        }

        Spacer(Modifier.width(10.dp))

        // ── 2. RIGHT ADAPTIVE SESSION CARD ───────────────────────────────────────
        Surface(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 12.dp)
                .clip(RoundedCornerShape(22.dp))
                .clickable(onClick = onClick),
            shape = RoundedCornerShape(22.dp),
            color = cardBg,
            border = BorderStroke(
                width = if (isActive) 1.5.dp else 1.dp,
                color = if (isActive) (if (isDark) successColor.copy(alpha = 0.7f) else successColor) else cardBorder
            ),
            shadowElevation = 0.dp,
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Top Row: Subject + Status Pill
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Row(
                        verticalAlignment = Alignment.Top,
                        modifier = Modifier.weight(1f),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isActive) (if (isDark) successColor.copy(alpha = 0.15f) else Color(0xFFDCFCE7)) else innerCardBg)
                                .border(1.dp, if (isActive) (if (isDark) successColor.copy(alpha = 0.4f) else Color(0xFF86EFAC)) else innerCardBorder, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (isOnline) {
                                Icon(
                                    painter = painterResource(id = CoreR.drawable.ic_modern_classes),
                                    contentDescription = null,
                                    tint = if (isActive) successColor else primaryBlue,
                                    modifier = Modifier.size(20.dp),
                                )
                            } else {
                                Icon(
                                    imageVector = subjectIcon(subjectName),
                                    contentDescription = null,
                                    tint = if (isActive) successColor else primaryBlue,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }

                        Spacer(Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = subjectName,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = textPrimary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                lineHeight = 20.sp,
                            )
                            Spacer(Modifier.height(3.dp))
                            Text(
                                text = teacherName,
                                fontSize = 12.sp,
                                color = textSecondary,
                                fontWeight = FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }

                    Spacer(Modifier.width(8.dp))

                    // Status Badge Pill
                    val statusPill = when {
                        isActive -> StatusPillStyle(
                            label = "LIVE",
                            bg = if (isDark) successColor.copy(alpha = 0.15f) else Color(0xFFDCFCE7),
                            border = if (isDark) successColor.copy(alpha = 0.4f) else Color(0xFF86EFAC),
                            textColor = if (isDark) successColor else Color(0xFF15803D),
                        )
                        isCompleted -> StatusPillStyle(
                            label = "SELESAI",
                            bg = if (isDark) Color(0xFF161618) else Color(0xFFF1F5F9),
                            border = if (isDark) Color(0xFF27272A) else Color(0xFFE2E8F0),
                            textColor = textTertiary,
                        )
                        else -> StatusPillStyle(
                            label = "MENDATANG",
                            bg = if (isDark) Color(0xFF1E293B) else Color(0xFFEFF6FF),
                            border = if (isDark) Color(0xFF3B82F6).copy(alpha = 0.4f) else Color(0xFFBFDBFE),
                            textColor = primaryBlue,
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(statusPill.bg)
                            .border(1.dp, statusPill.border, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                    ) {
                        Text(
                            text = statusPill.label,
                            color = statusPill.textColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Location & Room Row + CTA
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f, fill = false),
                    ) {
                        if (isOnline) {
                            Icon(
                                painter = painterResource(id = CoreR.drawable.ic_modern_classes),
                                contentDescription = null,
                                tint = textSecondary,
                                modifier = Modifier.size(14.dp),
                            )
                        } else {
                            Icon(
                                painter = painterResource(id = CoreR.drawable.ic_modern_location),
                                contentDescription = null,
                                tint = textSecondary,
                                modifier = Modifier.size(14.dp),
                            )
                        }
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = room,
                            fontSize = 12.sp,
                            color = textSecondary,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    Spacer(Modifier.width(8.dp))

                    // CTA Button
                    val ctaBg = if (isActive) (if (isDark) successColor.copy(alpha = 0.15f) else Color(0xFFDCFCE7)) else innerCardBg
                    val ctaBorder = if (isActive) (if (isDark) successColor.copy(alpha = 0.4f) else Color(0xFF86EFAC)) else innerCardBorder
                    val ctaTextColor = if (isActive) (if (isDark) successColor else Color(0xFF15803D)) else textSecondary

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(ctaBg)
                            .border(1.dp, ctaBorder, RoundedCornerShape(8.dp))
                            .clickable(onClick = onClick)
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (isActive) "Masuk" else "Detail",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = ctaTextColor,
                            )
                            Spacer(Modifier.width(3.dp))
                            Icon(
                                painter = painterResource(id = CoreR.drawable.ic_modern_chevron_right),
                                contentDescription = null,
                                tint = ctaTextColor,
                                modifier = Modifier.size(9.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatHourMinute(raw: String?): String? {
    if (raw.isNullOrBlank()) return null
    val trimmed = raw.trim()
    // 1. Check if it's already a time string like "07:30" or "07:30:00"
    if (trimmed.matches(Regex("""^\d{1,2}:\d{2}(:\d{2})?$"""))) {
        val parts = trimmed.split(":")
        val h = parts[0].toIntOrNull() ?: return null
        val m = parts[1].toIntOrNull() ?: return null
        return String.format(java.util.Locale.US, "%02d.%02d", h, m)
    }
    // 2. Check if it contains a space "YYYY-MM-DD HH:mm:ss"
    if (trimmed.contains(" ") && trimmed.length >= 16) {
        val timePart = trimmed.substringAfter(" ").trim()
        if (timePart.matches(Regex("""^\d{1,2}:\d{2}(:\d{2})?$"""))) {
            val parts = timePart.split(":")
            val h = parts[0].toIntOrNull()
            val m = parts[1].toIntOrNull()
            if (h != null && m != null) {
                return String.format(java.util.Locale.US, "%02d.%02d", h, m)
            }
        }
    }
    // 3. Fallback to ISO / Instant in Asia/Jakarta
    return try {
        val instant = Instant.parse(trimmed)
        val zdt = instant.atZone(ZoneId.of("Asia/Jakarta"))
        String.format(java.util.Locale.US, "%02d.%02d", zdt.hour, zdt.minute)
    } catch (_: Exception) {
        try {
            val zdt = ZonedDateTime.parse(trimmed).withZoneSameInstant(ZoneId.of("Asia/Jakarta"))
            String.format(java.util.Locale.US, "%02d.%02d", zdt.hour, zdt.minute)
        } catch (_: Exception) {
            null
        }
    }
}
