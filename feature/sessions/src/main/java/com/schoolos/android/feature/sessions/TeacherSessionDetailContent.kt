package com.schoolos.android.feature.sessions

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.schoolos.android.core.designsystem.*
import com.schoolos.android.domain.model.LearningSession
import com.schoolos.android.domain.model.SessionAttendance

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherSessionDetailContent(
    session: LearningSession,
    attendance: List<SessionAttendance> = emptyList(),
    students: List<StudentAttendanceUiItem> = emptyList(),
    onUpdateAttendance: (studentId: String, status: String) -> Unit = { _, _ -> },
    onMarkAllPresent: () -> Unit = {},
    onSaveAll: () -> Unit = {},
    isBulkSaving: Boolean = false,
    onOpenAssignments: (String) -> Unit,
    onOpenQuizzes: (String) -> Unit,
    onOpenMaterials: (String) -> Unit,
    accentColor: Color,
) {
    val effectiveStudents = if (students.isNotEmpty()) {
        students
    } else {
        attendance.mapIndexed { idx, it ->
            StudentAttendanceUiItem(
                studentId = it.studentId,
                studentName = "Siswa (${it.studentId.take(8)})",
                nisn = null,
                status = it.status,
                checkedInAt = it.checkedInAt,
                notes = it.notes,
            )
        }
    }
    val presentCount = effectiveStudents.count { it.status.lowercase() == "present" }
    val absentCount = effectiveStudents.count {
        it.status.lowercase() in listOf("absent", "sick", "excused")
    }
    val totalCount = effectiveStudents.size
    val attendanceRate = if (totalCount > 0) (presentCount * 100 / totalCount) else 0
    val sessionPeriod = formatSessionPeriod(session.scheduledAt)

    val rawSubject = session.subjectName ?: session.notes ?: "Pelajaran"
    val subject = rawSubject.substringBefore(" • ").substringBefore(" (Ruang").trim()
    val roomText = listOfNotNull(session.room ?: "Ruang Kelas", session.className).joinToString(" • ")
    val icon = subjectIcon(subject)

    val timeText = session.scheduledAt?.let {
        try {
            val instant = java.time.Instant.parse(it)
            val zdt = instant.atZone(java.time.ZoneId.of("Asia/Jakarta"))
            val endTimeStr = session.endedAt?.let { end ->
                val eZdt = java.time.Instant.parse(end).atZone(java.time.ZoneId.of("Asia/Jakarta"))
                String.format(java.util.Locale.US, "%02d.%02d", eZdt.hour, eZdt.minute)
            } ?: ""
            if (endTimeStr.isNotBlank()) {
                String.format(java.util.Locale.US, "%02d.%02d — %s WIB", zdt.hour, zdt.minute, endTimeStr)
            } else {
                String.format(java.util.Locale.US, "%02d.%02d WIB", zdt.hour, zdt.minute)
            }
        } catch (_: Exception) { "08.00 WIB" }
    } ?: "08.00 WIB"

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {

        // ── 1. UNIFIED EXECUTIVE SESSION CARD (ZERO GRADIENT, ZERO DUPLICATION) ──
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = CosmicNavy,
            border = BorderStroke(0.5.dp, GlassBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Top Header Row: Icon + Subject + Class + Time Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Squircle Icon Container (Zero gradient)
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(CosmicSurface2)
                                .border(0.5.dp, GlassBorder, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = NeonBlue,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = subject,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = roomText,
                                fontSize = 12.sp,
                                color = TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(Modifier.width(8.dp))

                    // Period & Time Badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = CosmicSurface2,
                        border = BorderStroke(0.5.dp, GlassBorder)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = sessionPeriod,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonBlue
                            )
                            Text(
                                text = timeText,
                                fontSize = 10.sp,
                                color = TextTertiary
                            )
                        }
                    }
                }

                HorizontalDivider(color = GlassBorder, thickness = 0.5.dp)

                // Attendance Metrics Row (3 Clean Cards, Zero Gradients)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AttendanceMetricBox(
                        label = "HADIR",
                        value = presentCount.toString(),
                        color = NeonSuccess,
                        modifier = Modifier.weight(1f)
                    )
                    AttendanceMetricBox(
                        label = "TIDAK HADIR",
                        value = absentCount.toString(),
                        color = NeonError,
                        modifier = Modifier.weight(1f)
                    )
                    AttendanceMetricBox(
                        label = "KEHADIRAN",
                        value = "$attendanceRate%",
                        color = NeonBlue,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Solid Linear Progress Bar (No gradient)
                if (totalCount > 0) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        LinearProgressIndicator(
                            progress = { if (totalCount > 0) (presentCount.toFloat() / totalCount).coerceIn(0f, 1f) else 0f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = NeonSuccess,
                            trackColor = CosmicSurface2
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "$presentCount dari $totalCount siswa hadir",
                                fontSize = 11.sp,
                                color = TextTertiary
                            )
                            StatusChip(label = session.status)
                        }
                    }
                }
            }
        }

        // ── 2. RESOURCE MANAGEMENT ACTIONS ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(NeonBlue)
                )
                Text(
                    text = "Kelola Sumber Daya Kelas",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
            Text(
                text = "Pintasan Aksi",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = TextTertiary
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ResourceActionCard(
                label = "Beri Tugas",
                hint = "Tugas & PR",
                icon = Icons.AutoMirrored.Filled.Assignment,
                accentColor = StudentNeon,
                modifier = Modifier.weight(1f),
                onClick = { onOpenAssignments(session.subjectName?.trim().orEmpty()) }
            )
            ResourceActionCard(
                label = "Buat Kuis",
                hint = "Evaluasi CBT",
                icon = Icons.Default.Quiz,
                accentColor = NeonSuccess,
                modifier = Modifier.weight(1f),
                onClick = { onOpenQuizzes(session.subjectName?.trim().orEmpty()) }
            )
            ResourceActionCard(
                label = "Unggah Materi",
                hint = "Bahan Ajar",
                icon = Icons.AutoMirrored.Filled.MenuBook,
                accentColor = NeonBlue,
                modifier = Modifier.weight(1f),
                onClick = { onOpenMaterials(session.subjectName?.trim().orEmpty()) }
            )
        }

        // ── 3. ATTENDANCE ROSTER HEADER & ACTIONS ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, top = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Rekap Presensi Siswa",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                if (totalCount > 0) {
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = CosmicSurface2,
                        border = BorderStroke(0.5.dp, GlassBorder)
                    ) {
                        Text(
                            text = "$totalCount Siswa",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonBlue,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }

        if (effectiveStudents.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Button Tandai Hadir Semua
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clickable(enabled = !isBulkSaving, onClick = onMarkAllPresent),
                    shape = RoundedCornerShape(10.dp),
                    color = CosmicSurface2,
                    border = BorderStroke(0.5.dp, GlassBorder)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DoneAll,
                            contentDescription = null,
                            tint = NeonSuccess,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Hadir Semua",
                            color = NeonSuccess,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Button Simpan Presensi (Solid button, zero gradient)
                Button(
                    onClick = onSaveAll,
                    enabled = !isBulkSaving,
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonBlue,
                        contentColor = Color.White
                    )
                ) {
                    if (isBulkSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Menyimpan...",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Simpan Presensi",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // ── 4. STUDENT ATTENDANCE ROSTER LIST ──
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = CosmicNavy,
            border = BorderStroke(0.5.dp, GlassBorder)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (effectiveStudents.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.EventAvailable,
                            contentDescription = null,
                            tint = TextTertiary,
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = "Data presensi belum tersedia",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Presensi akan muncul setelah sesi dimulai.",
                            fontSize = 11.sp,
                            color = TextTertiary
                        )
                    }
                } else {
                    effectiveStudents.forEachIndexed { idx, student ->
                        if (idx > 0) {
                            HorizontalDivider(
                                color = GlassBorder,
                                thickness = 0.5.dp,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                        val currentStatus = student.status.lowercase()

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val initial = student.studentName.firstOrNull()?.uppercase() ?: "${idx + 1}"
                            // Avatar Box
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(CosmicSurface2)
                                    .border(0.5.dp, GlassBorder, RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = initial,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }

                            Spacer(Modifier.width(10.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = student.studentName,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                val subText = student.nisn?.let { "NISN: $it" } ?: "ID: ${student.studentId.take(8)}"
                                Text(
                                    text = subText,
                                    fontSize = 10.5.sp,
                                    color = TextTertiary
                                )
                            }

                            Spacer(Modifier.width(8.dp))

                            // 4 Segmented Attendance Buttons: H, S, I, A (High Contrast, Bold, Zero Gradient)
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                AttendanceStatusButton(
                                    label = "H",
                                    selected = currentStatus == "present",
                                    activeColor = NeonSuccess,
                                    onClick = { onUpdateAttendance(student.studentId, "present") }
                                )
                                AttendanceStatusButton(
                                    label = "S",
                                    selected = currentStatus == "sick",
                                    activeColor = NeonBlue,
                                    onClick = { onUpdateAttendance(student.studentId, "sick") }
                                )
                                AttendanceStatusButton(
                                    label = "I",
                                    selected = currentStatus == "excused",
                                    activeColor = NeonWarning,
                                    onClick = { onUpdateAttendance(student.studentId, "excused") }
                                )
                                AttendanceStatusButton(
                                    label = "A",
                                    selected = currentStatus == "absent",
                                    activeColor = NeonError,
                                    onClick = { onUpdateAttendance(student.studentId, "absent") }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AttendanceMetricBox(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = CosmicSurface2,
        border = BorderStroke(0.5.dp, GlassBorder)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = color
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = label,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = TextTertiary
            )
        }
    }
}

@Composable
private fun ResourceActionCard(
    label: String,
    hint: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = CosmicSurface,
        border = BorderStroke(1.dp, if (LocalIsDarkTheme.current) accentColor.copy(alpha = 0.28f) else GlassBorder),
        shadowElevation = 2.dp,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.08f),
                            Color.Transparent
                        )
                    )
                )
                .padding(horizontal = 8.dp, vertical = 14.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(accentColor.copy(alpha = 0.14f))
                        .border(1.dp, accentColor.copy(alpha = 0.28f), RoundedCornerShape(13.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Text(
                    text = label,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = accentColor.copy(alpha = 0.10f),
                    border = BorderStroke(0.5.dp, accentColor.copy(alpha = 0.20f))
                ) {
                    Text(
                        text = hint,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = accentColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AttendanceStatusButton(
    label: String,
    selected: Boolean,
    activeColor: Color,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (selected) activeColor else CosmicSurface2,
        border = BorderStroke(
            0.5.dp,
            if (selected) activeColor else GlassBorder
        ),
        modifier = Modifier.size(32.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (selected) Color.White else TextSecondary
            )
        }
    }
}
