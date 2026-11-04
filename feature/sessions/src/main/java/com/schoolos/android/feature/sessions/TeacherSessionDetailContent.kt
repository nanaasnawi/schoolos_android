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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.schoolos.android.core.designsystem.*
import com.schoolos.android.domain.model.LearningSession
import com.schoolos.android.domain.model.MaterialType
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
    todayMaterials: List<com.schoolos.android.domain.model.LearningMaterial> = emptyList(),
    todayAssignments: List<com.schoolos.android.domain.model.Assignment> = emptyList(),
    accentColor: Color,
) {
    val isDark = LocalIsDarkTheme.current
    val cardBg = if (isDark) Color(0xFF111113) else Color.White
    val cardBorder = if (isDark) Color(0xFF1F1F23) else Color(0xFFE2E8F0)
    val textPrimary = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFFA1A1AA) else Color(0xFF64748B)
    val textTertiary = if (isDark) Color(0xFF71717A) else Color(0xFF94A3B8)
    val innerCardBg = if (isDark) Color(0xFF161618) else Color(0xFFF8FAFC)
    val innerCardBorder = if (isDark) Color(0xFF27272A) else Color(0xFFE2E8F0)

    val effectiveStudents = if (students.isNotEmpty()) {
        students
    } else {
        attendance.mapIndexed { idx, it ->
            StudentAttendanceUiItem(
                studentId = it.studentId,
                studentName = "Peserta Didik",
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
            shape = RoundedCornerShape(22.dp),
            color = cardBg,
            border = BorderStroke(0.5.dp, cardBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
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
                                .background(innerCardBg)
                                .border(0.5.dp, innerCardBorder, RoundedCornerShape(12.dp)),
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
                                color = textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = roomText,
                                fontSize = 12.sp,
                                color = textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(Modifier.width(8.dp))

                    // Period & Time Badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = innerCardBg,
                        border = BorderStroke(0.5.dp, innerCardBorder)
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
                                color = textTertiary
                            )
                        }
                    }
                }

                HorizontalDivider(color = innerCardBorder, thickness = 0.5.dp)

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
                            trackColor = innerCardBg
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "$presentCount dari $totalCount siswa hadir",
                                fontSize = 11.sp,
                                color = textTertiary
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
                    color = textPrimary
                )
            }
            Text(
                text = "Pintasan Aksi",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = textTertiary
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ResourceActionButton(
                label = "Beri Tugas",
                iconRes = CoreR.drawable.ic_modern_tasks,
                accentColor = StudentNeon,
                modifier = Modifier.weight(1f),
                onClick = { onOpenAssignments(session.subjectName?.trim().orEmpty()) }
            )
            ResourceActionButton(
                label = "Buat Kuis",
                iconRes = CoreR.drawable.ic_modern_quiz,
                accentColor = NeonSuccess,
                modifier = Modifier.weight(1f),
                onClick = { onOpenQuizzes(session.subjectName?.trim().orEmpty()) }
            )
            ResourceActionButton(
                label = "Unggah Materi",
                iconRes = CoreR.drawable.ic_modern_book,
                accentColor = NeonBlue,
                modifier = Modifier.weight(1f),
                onClick = { onOpenMaterials(session.subjectName?.trim().orEmpty()) }
            )
        }

        // ── 2.5 REAL-TIME TODAY'S SESSION MATERIALS & ASSIGNMENTS ──
        if (todayMaterials.isNotEmpty() || todayAssignments.isNotEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                color = cardBg,
                border = BorderStroke(0.5.dp, cardBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
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
                                    .background(NeonSuccess)
                            )
                            Text(
                                text = "Bahan Ajar & Tugas Sesi Ini",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = textPrimary
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = NeonSuccess.copy(alpha = 0.15f),
                            border = BorderStroke(0.5.dp, NeonSuccess.copy(alpha = 0.3f))
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
                        val isVideo = mat.materialType == MaterialType.VIDEO ||
                            mat.mediaUrl?.contains("youtube", ignoreCase = true) == true ||
                            mat.mediaUrl?.contains("youtu.be", ignoreCase = true) == true ||
                            mat.title.contains("video", ignoreCase = true)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = innerCardBg,
                            border = BorderStroke(0.5.dp, innerCardBorder),
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
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(if (isVideo) CoreR.drawable.ic_modern_video else CoreR.drawable.ic_modern_book),
                                        contentDescription = null,
                                        tint = if (isVideo) NeonWarning else NeonBlue,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Column {
                                        Text(
                                            text = mat.title,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = textPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = if (isVideo) "Video Pelajaran • Real-Time Sesi" else "Bahan Bacaan • Real-Time Sesi",
                                            fontSize = 10.sp,
                                            color = if (isVideo) NeonWarning else NeonBlue
                                        )
                                    }
                                }
                                Icon(
                                    painter = painterResource(CoreR.drawable.ic_modern_chevron_right),
                                    contentDescription = null,
                                    tint = textTertiary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }

                    todayAssignments.forEach { assign ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = innerCardBg,
                            border = BorderStroke(0.5.dp, innerCardBorder),
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
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(CoreR.drawable.ic_modern_tasks),
                                        contentDescription = null,
                                        tint = StudentNeon,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Column {
                                        Text(
                                            text = assign.title,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = textPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "Tugas Sesi Hari Ini • Maks ${assign.maxScore} Poin",
                                            fontSize = 10.sp,
                                            color = StudentNeon
                                        )
                                    }
                                }
                                Icon(
                                    painter = painterResource(CoreR.drawable.ic_modern_chevron_right),
                                    contentDescription = null,
                                    tint = textTertiary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
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
                    color = textPrimary
                )
                if (totalCount > 0) {
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = innerCardBg,
                        border = BorderStroke(0.5.dp, innerCardBorder)
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
                    color = innerCardBg,
                    border = BorderStroke(0.5.dp, innerCardBorder)
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
            shape = RoundedCornerShape(22.dp),
            color = cardBg,
            border = BorderStroke(0.5.dp, cardBorder)
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
                                color = cardBorder,
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
                                    .background(innerCardBg)
                                    .border(0.5.dp, innerCardBorder, RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = initial,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textPrimary
                                )
                            }

                            Spacer(Modifier.width(10.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = student.studentName,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                val subText = student.nisn?.let { "NISN: $it" } ?: "ID: ${student.studentId.take(8)}"
                                Text(
                                    text = subText,
                                    fontSize = 10.5.sp,
                                    color = textTertiary
                                )
                            }

                            Spacer(Modifier.width(8.dp))

                            // 4 Segmented Attendance Buttons: H, S, I, A (High Contrast, Bold, Zero Gradient)
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                AttendanceStatusButton(
                                    label = "H",
                                    selected = currentStatus == "present",
                                    activeColor = NeonSuccess,
                                    inactiveBg = innerCardBg,
                                    inactiveBorder = innerCardBorder,
                                    inactiveText = textSecondary,
                                    onClick = { onUpdateAttendance(student.studentId, "present") }
                                )
                                AttendanceStatusButton(
                                    label = "S",
                                    selected = currentStatus == "sick",
                                    activeColor = NeonBlue,
                                    inactiveBg = innerCardBg,
                                    inactiveBorder = innerCardBorder,
                                    inactiveText = textSecondary,
                                    onClick = { onUpdateAttendance(student.studentId, "sick") }
                                )
                                AttendanceStatusButton(
                                    label = "I",
                                    selected = currentStatus == "excused",
                                    activeColor = NeonWarning,
                                    inactiveBg = innerCardBg,
                                    inactiveBorder = innerCardBorder,
                                    inactiveText = textSecondary,
                                    onClick = { onUpdateAttendance(student.studentId, "excused") }
                                )
                                AttendanceStatusButton(
                                    label = "A",
                                    selected = currentStatus == "absent",
                                    activeColor = NeonError,
                                    inactiveBg = innerCardBg,
                                    inactiveBorder = innerCardBorder,
                                    inactiveText = textSecondary,
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
    val isDark = LocalIsDarkTheme.current
    val bg = if (isDark) Color(0xFF161618) else Color(0xFFF8FAFC)
    val border = if (isDark) Color(0xFF27272A) else Color(0xFFE2E8F0)
    val textTertiary = if (isDark) Color(0xFF71717A) else Color(0xFF94A3B8)

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = bg,
        border = BorderStroke(0.5.dp, border)
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
                color = textTertiary
            )
        }
    }
}

@Composable
private fun ResourceActionButton(
    label: String,
    icon: ImageVector? = null,
    iconRes: Int? = null,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val isDark = LocalIsDarkTheme.current
    val textPrimary = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A)

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(accentColor.copy(alpha = 0.14f))
                .border(1.dp, accentColor.copy(alpha = 0.28f), RoundedCornerShape(13.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (iconRes != null) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )
            } else if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun AttendanceStatusButton(
    label: String,
    selected: Boolean,
    activeColor: Color,
    inactiveBg: Color,
    inactiveBorder: Color,
    inactiveText: Color,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (selected) activeColor else inactiveBg,
        border = BorderStroke(
            0.5.dp,
            if (selected) activeColor else inactiveBorder
        ),
        modifier = Modifier.size(32.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (selected) Color.White else inactiveText
            )
        }
    }
}
