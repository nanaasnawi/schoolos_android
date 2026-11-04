package com.schoolos.android.feature.sessions

import com.schoolos.android.core.R as CoreR
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.schoolos.android.core.designsystem.*
import com.schoolos.android.domain.model.LearningSession
import com.schoolos.android.domain.model.MaterialType
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
    val isDark = LocalIsDarkTheme.current
    val cardBg = if (isDark) Color(0xFF111113) else Color.White
    val cardBorder = if (isDark) Color(0xFF1F1F23) else Color(0xFFE2E8F0)
    val innerCardBg = if (isDark) Color(0xFF161618) else Color(0xFFF8FAFC)
    val innerCardBorder = if (isDark) Color(0xFF27272A) else Color(0xFFE2E8F0)
    val textPrimary = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFFA1A1AA) else Color(0xFF64748B)
    val textTertiary = if (isDark) Color(0xFF71717A) else Color(0xFF94A3B8)
    val studentAccent = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)
    val successColor = if (isDark) Color(0xFF10B981) else Color(0xFF16A34A)
    val warningColor = if (isDark) Color(0xFFF59E0B) else Color(0xFFD97706)

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
                    color = textPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Pemberitahuan: Keterangan harus diajukan dalam kurun waktu maksimal 24 jam dari jadwal sesi. Setelah 24 jam tanpa keterangan atau membaca materi, status otomatis ditutup menjadi Alpa.",
                        fontSize = 11.5.sp,
                        color = textTertiary,
                        lineHeight = 16.sp,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (selectedExcuseType == "sick") studentAccent.copy(alpha = if (isDark) 0.2f else 0.12f) else innerCardBg,
                            border = BorderStroke(1.dp, if (selectedExcuseType == "sick") studentAccent else innerCardBorder),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedExcuseType = "sick" }
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "Sakit",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (selectedExcuseType == "sick") studentAccent else textSecondary
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (selectedExcuseType == "excused") warningColor.copy(alpha = if (isDark) 0.2f else 0.12f) else innerCardBg,
                            border = BorderStroke(1.dp, if (selectedExcuseType == "excused") warningColor else innerCardBorder),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedExcuseType = "excused" }
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "Izin",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (selectedExcuseType == "excused") warningColor else textSecondary
                                )
                            }
                        }
                    }

                    androidx.compose.material3.OutlinedTextField(
                        value = excuseReason,
                        onValueChange = { excuseReason = it },
                        label = { Text("Alasan / Catatan (contoh: Surat Dokter/Demam)") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3,
                        shape = RoundedCornerShape(12.dp),
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
                    Text("Kirim Keterangan", fontWeight = FontWeight.Bold, color = successColor)
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showExcuseDialog = false }) {
                    Text("Batal", color = textTertiary)
                }
            },
            containerColor = cardBg,
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // ── 1. SESSION HERO INFO CARD (22.dp ROUNDED, ADAPTIVE) ───────────────
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            color = cardBg,
            border = BorderStroke(1.dp, cardBorder),
            shadowElevation = if (isDark) 0.dp else 1.dp
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Teacher Row
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = if (isDark) 0.20f else 0.12f))
                            .border(1.dp, accentColor.copy(alpha = if (isDark) 0.40f else 0.25f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(id = CoreR.drawable.ic_modern_profile),
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Guru Pengampu",
                            fontSize = 11.sp,
                            color = textTertiary,
                            fontWeight = FontWeight.SemiBold
                        )
                        val teacher = session.teacherName?.takeIf { it.isNotBlank() && !it.equals("Guru Pengampu", ignoreCase = true) } ?: "Guru Pengajar"
                        Text(
                            text = teacher,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                    }
                }

                HorizontalDivider(color = cardBorder, thickness = 1.dp)

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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DetailMetricItem(
                        label = "Waktu Mulai",
                        value = startTime,
                        iconRes = CoreR.drawable.ic_modern_clock,
                        modifier = Modifier.weight(1f),
                    )
                    DetailMetricItem(
                        label = "Durasi Sesi",
                        value = session.durationText() ?: "90 Menit",
                        iconRes = CoreR.drawable.ic_modern_calendar,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        // ── 2. SUMBER DAYA PELAJARAN (22.dp ROUNDED, ADAPTIVE) ─────────────────
        SectionTitle("SUMBER DAYA PELAJARAN")
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            color = cardBg,
            border = BorderStroke(1.dp, cardBorder),
            shadowElevation = if (isDark) 0.dp else 1.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ResourceItem(
                    label = "Tugas",
                    iconRes = CoreR.drawable.ic_modern_tasks,
                    color = studentAccent,
                    modifier = Modifier.weight(1f)
                ) { onOpenAssignments(session.subjectName?.trim().orEmpty()) }

                ResourceItem(
                    label = "Kuis CBT",
                    iconRes = CoreR.drawable.ic_modern_quiz,
                    color = successColor,
                    modifier = Modifier.weight(1f)
                ) { onOpenQuizzes(session.subjectName?.trim().orEmpty()) }

                ResourceItem(
                    label = "Materi Modul",
                    iconRes = CoreR.drawable.ic_modern_book,
                    color = Color(0xFF8B5CF6),
                    modifier = Modifier.weight(1f)
                ) { onOpenMaterials(session.subjectName?.trim().orEmpty()) }
            }
        }

        // ── 3. REAL-TIME TODAY'S SESSION MATERIALS & ASSIGNMENTS ───────────────
        if (todayMaterials.isNotEmpty() || todayAssignments.isNotEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                color = cardBg,
                border = BorderStroke(1.dp, cardBorder),
                shadowElevation = if (isDark) 0.dp else 1.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Bahan Ajar & Tugas Sesi Ini",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isDark) successColor.copy(alpha = 0.15f) else Color(0xFFDCFCE7),
                            border = BorderStroke(1.dp, if (isDark) successColor.copy(alpha = 0.40f) else Color(0xFF86EFAC))
                        ) {
                            Text(
                                text = "Real-Time Hari Ini",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) successColor else Color(0xFF15803D),
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                    }

                    todayMaterials.forEach { mat ->
                        val isVideo = mat.materialType == MaterialType.VIDEO ||
                            mat.mediaUrl?.contains("youtube", ignoreCase = true) == true ||
                            mat.mediaUrl?.contains("youtu.be", ignoreCase = true) == true ||
                            mat.title.contains("video", ignoreCase = true)

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = innerCardBg,
                            border = BorderStroke(1.dp, innerCardBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenMaterials(session.subjectName?.trim().orEmpty()) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isVideo) Color(0xFFEF4444).copy(alpha = if (isDark) 0.15f else 0.10f) else studentAccent.copy(alpha = if (isDark) 0.15f else 0.10f))
                                            .border(1.dp, if (isVideo) Color(0xFFEF4444).copy(alpha = 0.35f) else studentAccent.copy(alpha = 0.30f), RoundedCornerShape(10.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            painter = painterResource(
                                                id = if (isVideo) CoreR.drawable.ic_modern_video else CoreR.drawable.ic_modern_book
                                            ),
                                            contentDescription = null,
                                            tint = if (isVideo) Color(0xFFEF4444) else studentAccent,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = mat.title,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = textPrimary,
                                            maxLines = 1,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                        )
                                        Spacer(Modifier.height(1.dp))
                                        Text(
                                            text = if (isVideo) "Tayang: Hari Ini • Video Pembelajaran" else "Tayang: Hari Ini • Modul Bacaan",
                                            fontSize = 10.5.sp,
                                            color = if (isVideo) Color(0xFFEF4444) else warningColor,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                                Icon(
                                    painter = painterResource(id = CoreR.drawable.ic_modern_chevron_right),
                                    contentDescription = null,
                                    tint = textTertiary,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                    }

                    todayAssignments.forEach { assign ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = innerCardBg,
                            border = BorderStroke(1.dp, innerCardBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenAssignments(session.subjectName?.trim().orEmpty()) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(studentAccent.copy(alpha = if (isDark) 0.15f else 0.10f))
                                            .border(1.dp, studentAccent.copy(alpha = 0.30f), RoundedCornerShape(10.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            painter = painterResource(id = CoreR.drawable.ic_modern_tasks),
                                            contentDescription = null,
                                            tint = studentAccent,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = assign.title,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = textPrimary,
                                            maxLines = 1,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                        )
                                        Spacer(Modifier.height(1.dp))
                                        Text(
                                            text = "Tugas Sesi Hari Ini • Maks ${assign.maxScore} Poin",
                                            fontSize = 10.5.sp,
                                            color = studentAccent,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                                Icon(
                                    painter = painterResource(id = CoreR.drawable.ic_modern_chevron_right),
                                    contentDescription = null,
                                    tint = textTertiary,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── 4. PRESENSI SAYA (22.dp ROUNDED, ADAPTIVE) ─────────────────────────
        SectionTitle("STATUS PRESENSI")
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            color = cardBg,
            border = BorderStroke(1.dp, cardBorder),
            shadowElevation = if (isDark) 0.dp else 1.dp
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                val myAtt = myAttendance
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Presensi Saya",
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                    if (myAtt != null) {
                        StatusChip(label = myAtt.status)
                    }
                }
                Spacer(Modifier.height(14.dp))

                if (myAtt == null) {
                    EmptyStateContent(
                        message = "Belum Ada Presensi Tercatat",
                        subtitle = "Presensi otomatis tercatat 'Hadir Daring' saat Anda membaca materi pelajaran ini hari ini. Jika berhalangan hadir, ajukan Izin/Sakit dalam kurun waktu 24 jam sebelum dinyatakan Alpa.",
                        accentColor = studentAccent,
                        isDark = isDark,
                        textSecondary = textSecondary,
                        textTertiary = textTertiary,
                    )
                } else {
                    val statusColor = when (myAtt.status.lowercase()) {
                        "present" -> successColor
                        "excused" -> warningColor
                        "sick" -> studentAccent
                        else -> Color(0xFFEF4444)
                    }
                    val statusLabel = when (myAtt.status.lowercase()) {
                        "present" -> "Hadir (Daring / Luring)"
                        "excused" -> "Izin Resmi"
                        "sick" -> "Sakit"
                        "absent" -> "Alpa (Tidak Hadir)"
                        else -> myAtt.status.replaceFirstChar { it.uppercase() }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(statusColor.copy(alpha = if (isDark) 0.18f else 0.12f))
                                .border(1.dp, statusColor.copy(alpha = if (isDark) 0.40f else 0.25f), CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                painter = painterResource(id = CoreR.drawable.ic_modern_check_circle),
                                contentDescription = null,
                                tint = statusColor,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = statusLabel,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = textPrimary,
                            )
                            val excuseNotes = myAtt.notes
                            if (!excuseNotes.isNullOrBlank()) {
                                Spacer(Modifier.height(1.dp))
                                Text(
                                    text = excuseNotes,
                                    fontSize = 11.5.sp,
                                    color = textTertiary,
                                )
                            }
                        }
                    }
                }

                // Tombol ajukan izin / sakit jika siswa belum hadir atau alpa
                val myStatus = myAtt?.status?.lowercase()
                val canSubmitExcuse = myStatus == null || myStatus == "absent"
                if (onSubmitExcuse != null && canSubmitExcuse) {
                    Spacer(Modifier.height(12.dp))
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { showExcuseDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        color = innerCardBg,
                        border = BorderStroke(1.dp, innerCardBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 12.dp, horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.EditNote,
                                contentDescription = null,
                                tint = warningColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Ajukan Keterangan Izin / Sakit (Maks. 24 Jam)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = textPrimary,
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
    val isDark = LocalIsDarkTheme.current
    val textTertiary = if (isDark) Color(0xFF71717A) else Color(0xFF94A3B8)
    Text(
        text = title,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = textTertiary,
        letterSpacing = 0.8.sp,
        modifier = Modifier.padding(start = 4.dp, top = 2.dp),
    )
}

@Composable
private fun EmptyStateContent(
    message: String,
    subtitle: String,
    accentColor: Color,
    isDark: Boolean,
    textSecondary: Color,
    textTertiary: Color,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(accentColor.copy(alpha = if (isDark) 0.15f else 0.10f))
                .border(1.dp, accentColor.copy(alpha = if (isDark) 0.35f else 0.25f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(id = CoreR.drawable.ic_modern_attendance),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = accentColor
            )
        }
        Text(
            text = message,
            fontSize = 13.5.sp,
            color = textSecondary,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = subtitle,
            fontSize = 11.5.sp,
            color = textTertiary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            lineHeight = 16.sp
        )
    }
}
