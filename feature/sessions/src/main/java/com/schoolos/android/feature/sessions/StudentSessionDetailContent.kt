package com.schoolos.android.feature.sessions

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import com.schoolos.android.core.designsystem.*
import com.schoolos.android.domain.model.LearningSession
import com.schoolos.android.domain.model.SessionAttendance

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentSessionDetailContent(
    session: LearningSession,
    attendance: List<SessionAttendance>,
    myAttendance: SessionAttendance? = null,
    onOpenAssignments: (String) -> Unit,
    onOpenQuizzes: (String) -> Unit,
    onOpenMaterials: (String) -> Unit,
    todayMaterials: List<com.schoolos.android.domain.model.LearningMaterial> = emptyList(),
    todayAssignments: List<com.schoolos.android.domain.model.Assignment> = emptyList(),
    accentColor: Color,
    onSubmitExcuse: ((status: String, notes: String) -> Unit)? = null,
) {
    var showExcuseDialog by remember { mutableStateOf(false) }
    var selectedExcuseType by remember { mutableStateOf("sick") }
    var excuseReason by remember { mutableStateOf("") }

    if (showExcuseDialog && onSubmitExcuse != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showExcuseDialog = false },
            title = {
                Text(
                    "Pengajuan Keterangan Ketidakhadiran",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = TextPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Pemberitahuan: Keterangan harus diajukan dalam kurun waktu maksimal 24 jam dari jadwal sesi. Setelah 24 jam tanpa keterangan atau membaca materi, status otomatis ditutup menjadi Alpa.",
                        fontSize = 11.sp,
                        color = TextTertiary,
                        lineHeight = 16.sp,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (selectedExcuseType == "sick") NeonBlue.copy(alpha = 0.2f) else CosmicSurface)
                                .border(1.dp, if (selectedExcuseType == "sick") NeonBlue else GlassBorder, RoundedCornerShape(10.dp))
                                .clickable { selectedExcuseType = "sick" }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Sakit",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (selectedExcuseType == "sick") NeonBlue else TextSecondary
                            )
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (selectedExcuseType == "excused") NeonWarning.copy(alpha = 0.2f) else CosmicSurface)
                                .border(1.dp, if (selectedExcuseType == "excused") NeonWarning else GlassBorder, RoundedCornerShape(10.dp))
                                .clickable { selectedExcuseType = "excused" }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Izin",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (selectedExcuseType == "excused") NeonWarning else TextSecondary
                            )
                        }
                    }

                    androidx.compose.material3.OutlinedTextField(
                        value = excuseReason,
                        onValueChange = { excuseReason = it },
                        label = { Text("Alasan / Catatan (contoh: Surat Dokter/Demam)") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3,
                    )
                }
            },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        val notes = excuseReason.ifBlank { if (selectedExcuseType == "sick") "Sakit" else "Izin" }
                        onSubmitExcuse(selectedExcuseType, notes)
                        showExcuseDialog = false
                    }
                ) {
                    Text("Kirim Keterangan", fontWeight = FontWeight.Bold, color = NeonSuccess)
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showExcuseDialog = false }) {
                    Text("Batal", color = TextTertiary)
                }
            },
            containerColor = CosmicNavy,
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        // Info Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CosmicNavy)
                .border(0.5.dp, GlassBorder, RoundedCornerShape(16.dp))
                .padding(14.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(40.dp).clip(CircleShape).background(accentColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Default.Person, null, tint = accentColor, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Guru Pengampu", fontSize = 10.sp, color = TextTertiary, fontWeight = FontWeight.Bold)
                        val teacher = session.teacherName?.takeIf { it.isNotBlank() && !it.equals("Guru Pengampu", ignoreCase = true) } ?: "Guru Pengajar"
                        Text(teacher, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                    }
                }

                HorizontalDivider(color = accentColor.copy(alpha = 0.2f), thickness = 1.dp)

                val startTime = session.scheduledAt?.let {
                    try {
                        val parser = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault()).apply {
                            timeZone = java.util.TimeZone.getTimeZone("UTC")
                        }
                        val outFormatter = java.text.SimpleDateFormat("HH.mm", java.util.Locale.getDefault()).apply {
                            timeZone = java.util.TimeZone.getTimeZone("Asia/Jakarta")
                        }
                        parser.parse(it.substringBefore("."))?.let { d -> outFormatter.format(d) + " WIB" }
                    } catch (e: Exception) { null }
                } ?: "07.30 WIB"

                Row(modifier = Modifier.fillMaxWidth()) {
                    DetailMetricItem(
                        label = "Mulai",
                        value = startTime,
                        icon = Icons.Default.Schedule,
                        modifier = Modifier.weight(1f),
                    )
                    DetailMetricItem(
                        label = "Durasi",
                        value = session.durationText() ?: "90 Menit",
                        icon = Icons.Default.PlayArrow,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        // Resource Hub
        SectionTitle("SUMBER DAYA PELAJARAN")
        GlassCard(cornerRadius = 16.dp) {
            Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                ResourceItem(
                    label = "Tugas",
                    iconRes = com.schoolos.android.core.R.drawable.ic_modern_tasks,
                    color = StudentNeon,
                    modifier = Modifier.weight(1f)
                ) { onOpenAssignments(session.subjectName?.trim().orEmpty()) }
                ResourceItem(
                    label = "Kuis",
                    iconRes = com.schoolos.android.core.R.drawable.ic_modern_quiz,
                    color = NeonSuccess,
                    modifier = Modifier.weight(1f)
                ) { onOpenQuizzes(session.subjectName?.trim().orEmpty()) }
                ResourceItem(
                    label = "Materi",
                    iconRes = com.schoolos.android.core.R.drawable.ic_modern_book,
                    color = NeonBlue,
                    modifier = Modifier.weight(1f)
                ) { onOpenMaterials(session.subjectName?.trim().orEmpty()) }
            }
        }

        // ── REAL-TIME TODAY'S SESSION MATERIALS & ASSIGNMENTS ──
        if (todayMaterials.isNotEmpty() || todayAssignments.isNotEmpty()) {
            GlassCard(cornerRadius = 16.dp) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Bahan Ajar & Tugas Sesi Ini",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        androidx.compose.material3.Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = NeonSuccess.copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, NeonSuccess.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = "Real-Time Hari Ini",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = NeonSuccess,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    todayMaterials.forEach { mat ->
                        androidx.compose.material3.Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = CosmicSurface2,
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, GlassBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenMaterials(session.subjectName?.trim().orEmpty()) }
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
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Book,
                                        contentDescription = null,
                                        tint = NeonBlue,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Column {
                                        Text(
                                            text = mat.title,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary,
                                            maxLines = 1,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "Tayang: Hari Ini • ${mat.materialType}",
                                            fontSize = 10.sp,
                                            color = NeonWarning
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = TextTertiary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }

                    todayAssignments.forEach { assign ->
                        androidx.compose.material3.Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = CosmicSurface2,
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, GlassBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenAssignments(session.subjectName?.trim().orEmpty()) }
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
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Assignment,
                                        contentDescription = null,
                                        tint = StudentNeon,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Column {
                                        Text(
                                            text = assign.title,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary,
                                            maxLines = 1,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "Tugas Sesi Hari Ini • Maks ${assign.maxScore} Poin",
                                            fontSize = 10.sp,
                                            color = StudentNeon
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = TextTertiary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Personal Attendance
        GlassCard(cornerRadius = 16.dp) {
            Column(modifier = Modifier.padding(12.dp)) {
                val myAtt = myAttendance
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Presensi Saya", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                    if (myAtt != null) {
                        StatusChip(label = myAtt.status)
                    }
                }
                Spacer(Modifier.height(14.dp))

                if (myAtt == null) {
                    EmptyStateContent(
                        message = "Belum Ada Presensi Tercatat",
                        subtitle = "Presensi otomatis tercatat 'Hadir Daring' saat Anda membaca materi pelajaran ini hari ini. Jika berhalangan hadir, ajukan Izin/Sakit dalam kurun waktu 24 jam sebelum dinyatakan Alpa.",
                        accentColor = accentColor,
                    )
                } else {
                    val statusColor = when (myAtt.status.lowercase()) {
                        "present" -> NeonSuccess
                        "excused" -> NeonWarning
                        "sick" -> NeonBlue
                        else -> NeonError
                    }
                    val statusIcon = when (myAtt.status.lowercase()) {
                        "present" -> Icons.Default.CheckCircle
                        else -> Icons.Default.Schedule
                    }
                    val statusLabel = when (myAtt.status.lowercase()) {
                        "present" -> "Hadir (Daring / Luring)"
                        "excused" -> "Izin"
                        "sick" -> "Sakit"
                        "absent" -> "Alpa (Tidak Hadir)"
                        else -> myAtt.status.replaceFirstChar { it.uppercase() }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(statusColor.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                statusIcon,
                                null,
                                tint = statusColor,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                statusLabel,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                            )
                            val excuseNotes = myAtt.notes
                            if (!excuseNotes.isNullOrBlank()) {
                                Text(
                                    excuseNotes,
                                    fontSize = 11.sp,
                                    color = TextTertiary,
                                )
                            }
                        }
                    }
                }

                // Tombol ajukan izin / sakit jika siswa belum hadir atau alpa
                val myStatus = myAtt?.status?.lowercase()
                val canSubmitExcuse = myStatus == null || myStatus == "absent"
                if (onSubmitExcuse != null && canSubmitExcuse) {
                    Spacer(Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(CosmicSurface2)
                            .border(0.5.dp, GlassBorder, RoundedCornerShape(12.dp))
                            .clickable { showExcuseDialog = true }
                            .padding(vertical = 10.dp, horizontal = 14.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.EditNote, contentDescription = null, tint = NeonWarning, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Ajukan Keterangan Izin / Sakit (Maks. 24 Jam)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        title,
        fontSize = 10.sp,
        fontWeight = FontWeight.ExtraBold,
        color = TextTertiary,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
    )
}

@Composable
private fun EmptyStateContent(message: String, subtitle: String, accentColor: Color) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(accentColor.copy(alpha = 0.1f))
                .border(1.dp, accentColor.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.EventAvailable, contentDescription = null,
                modifier = Modifier.size(24.dp), tint = accentColor)
        }
        Text(message, fontSize = 13.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
        Text(subtitle, fontSize = 11.sp, color = TextTertiary)
    }
}

// Remove duplicate definitions - use SessionDetailComponents.kt versions
// DetailMetricItem and ResourceItem are defined in SessionDetailComponents.kt