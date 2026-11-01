package com.schoolos.android.core.designsystem

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun PublishingScheduleSelector(
    isScheduled: Boolean,
    onScheduleModeChange: (Boolean) -> Unit,
    scheduledDate: LocalDate,
    onDateChange: (LocalDate) -> Unit,
    scheduledTime: LocalTime,
    onTimeChange: (LocalTime) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val idLocale = remember { Locale("id", "ID") }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("EEE, d MMM yyyy", idLocale) }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm 'WIB'", idLocale) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = CosmicSurface2,
        border = BorderStroke(0.5.dp, GlassBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = NeonBlue,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = "Jadwal Tayang & Status Rilis",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(Modifier.width(8.dp))

                if (isScheduled) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(AccentNeonAmber.copy(alpha = 0.15f))
                            .border(0.5.dp, AccentNeonAmber.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "Terjadwal (Draft)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentNeonAmber,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(NeonSuccess.copy(alpha = 0.15f))
                            .border(0.5.dp, NeonSuccess.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "Tayang Langsung",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonSuccess,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            // Mode Selector Buttons (Tayang Hari Ini vs Jadwalkan Tayang)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Button 1: Tayang Hari Ini (Aktif)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (!isScheduled) NeonSuccess.copy(alpha = 0.14f) else CosmicNavy)
                        .border(
                            width = if (!isScheduled) 1.dp else 0.5.dp,
                            color = if (!isScheduled) NeonSuccess else GlassBorder,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clickable { onScheduleModeChange(false) }
                        .padding(vertical = 10.dp, horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = if (!isScheduled) NeonSuccess else TextTertiary,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "Tayang Hari Ini",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (!isScheduled) NeonSuccess else TextSecondary,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                // Button 2: Jadwalkan Tayang
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isScheduled) AccentNeonAmber.copy(alpha = 0.14f) else CosmicNavy)
                        .border(
                            width = if (isScheduled) 1.dp else 0.5.dp,
                            color = if (isScheduled) AccentNeonAmber else GlassBorder,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clickable { onScheduleModeChange(true) }
                        .padding(vertical = 10.dp, horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = if (isScheduled) AccentNeonAmber else TextTertiary,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "Jadwalkan Tayang",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isScheduled) AccentNeonAmber else TextSecondary,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            // Date & Time Pickers (if isScheduled is true)
            if (isScheduled) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(CosmicNavy)
                        .border(0.5.dp, GlassBorder, RoundedCornerShape(8.dp))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Date picker button
                        Box(
                            modifier = Modifier
                                .weight(1.3f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(CosmicSurface2)
                                .border(0.5.dp, GlassBorder, RoundedCornerShape(8.dp))
                                .clickable {
                                    val dpd = DatePickerDialog(
                                        context,
                                        { _, y, m, d ->
                                            onDateChange(LocalDate.of(y, m + 1, d))
                                        },
                                        scheduledDate.year,
                                        scheduledDate.monthValue - 1,
                                        scheduledDate.dayOfMonth
                                    )
                                    dpd.datePicker.minDate = System.currentTimeMillis()
                                    dpd.show()
                                }
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarToday,
                                    contentDescription = null,
                                    tint = AccentNeonAmber,
                                    modifier = Modifier.size(14.dp)
                                )
                                Column {
                                    Text(
                                        text = "Tanggal Tayang",
                                        fontSize = 9.sp,
                                        color = TextTertiary
                                    )
                                    Text(
                                        text = scheduledDate.format(dateFormatter),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                }
                            }
                        }

                        // Time picker button
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(CosmicSurface2)
                                .border(0.5.dp, GlassBorder, RoundedCornerShape(8.dp))
                                .clickable {
                                    TimePickerDialog(
                                        context,
                                        { _, hour, minute ->
                                            onTimeChange(LocalTime.of(hour, minute))
                                        },
                                        scheduledTime.hour,
                                        scheduledTime.minute,
                                        true
                                    ).show()
                                }
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccessTime,
                                    contentDescription = null,
                                    tint = AccentNeonAmber,
                                    modifier = Modifier.size(14.dp)
                                )
                                Column {
                                    Text(
                                        text = "Jam Tayang (WIB)",
                                        fontSize = 9.sp,
                                        color = TextTertiary
                                    )
                                    Text(
                                        text = scheduledTime.format(timeFormatter),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = TextTertiary,
                            modifier = Modifier.size(13.dp).padding(top = 1.dp)
                        )
                        Text(
                            text = "Siswa baru dapat mengakses materi/tugas secara otomatis pada ${scheduledDate.format(dateFormatter)} pukul ${scheduledTime.format(timeFormatter)}.",
                            fontSize = 10.5.sp,
                            color = TextTertiary,
                            lineHeight = 14.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * Helper to compute an ISO-8601 UTC timestamp string from a local date and time in Asia/Jakarta timezone.
 */
fun buildScheduledUtcIso(date: LocalDate, time: LocalTime): String {
    val zoneJakarta = ZoneId.of("Asia/Jakarta")
    val zdt = ZonedDateTime.of(date, time, zoneJakarta)
    return zdt.toInstant().toString()
}

