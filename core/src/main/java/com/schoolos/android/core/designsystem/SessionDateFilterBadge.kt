package com.schoolos.android.core.designsystem

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun SessionDateFilterBadge(
    dateStr: String,
    onShowAllArchive: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val formattedDate = remember(dateStr) {
        val targetDate = if (dateStr.equals("today", ignoreCase = true)) {
            LocalDate.now(ZoneId.of("Asia/Jakarta"))
        } else {
            try { LocalDate.parse(dateStr.trim().substringBefore(" ").substringBefore("T")) }
            catch (_: Exception) { LocalDate.now(ZoneId.of("Asia/Jakarta")) }
        }
        val isToday = targetDate == LocalDate.now(ZoneId.of("Asia/Jakarta"))
        val dateText = targetDate.format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale("id", "ID")))
        if (isToday) "Sesi Hari Ini • $dateText" else "Sesi Jadwal • $dateText"
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = CosmicNavy,
        border = BorderStroke(0.5.dp, GlassBorder),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(NeonBlue.copy(alpha = 0.15f))
                        .border(0.5.dp, NeonBlue.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = NeonBlue,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = formattedDate,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = NeonSuccess.copy(alpha = 0.15f),
                            border = BorderStroke(0.5.dp, NeonSuccess.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = "Real-Time",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = NeonSuccess,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = "Hanya materi/tugas yang tayang pada jadwal sesi ini",
                        fontSize = 11.sp,
                        color = TextTertiary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = CosmicSurface2,
                border = BorderStroke(0.5.dp, GlassBorder),
                modifier = Modifier.clickable(onClick = onShowAllArchive)
            ) {
                Text(
                    text = "Semua Arsip",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = NeonBlue,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                )
            }
        }
    }
}

