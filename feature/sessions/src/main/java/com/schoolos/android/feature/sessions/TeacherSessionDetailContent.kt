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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.schoolos.android.core.designsystem.*
import com.schoolos.android.domain.model.LearningSession
import com.schoolos.android.domain.model.MaterialType
import com.schoolos.android.domain.model.SessionAttendance

/**
 * Detail Jadwal (portal guru) — gaya identik dengan beranda siswa & kepala sekolah:
 * kartu rounded 22dp, pill pastel, header seksi kapital kecil, tanpa gradien.
 */
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
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    val textTertiary = if (isDark) Color(0xFF71717A) else Color(0xFF94A3B8)
    val innerBg = if (isDark) Color(0xFF161618) else Color(0xFFF8FAFC)
    val innerBorder = if (isDark) Color(0xFF27272A) else Color(0xFFE2E8F0)

    // Pastel palette identik beranda
    val skyBg = if (isDark) Color(0xFF0C243B) else Color(0xFFE0F2FE)
    val skyFg = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)
    val greenBg = if (isDark) Color(0xFF0A2B1D) else Color(0xFFDCFCE7)
    val greenFg = if (isDark) Color(0xFF34D399) else Color(0xFF16A34A)
    val purpleBg = if (isDark) Color(0xFF261447) else Color(0xFFF3E8FF)
    val purpleFg = if (isDark) Color(0xFFA78BFA) else Color(0xFF9333EA)
    val amberBg = if (isDark) Color(0xFF31220A) else Color(0xFFFEF3C7)
    val amberFg = if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706)
    val redBg = if (isDark) Color(0xFF3A0F14) else Color(0xFFFEE2E2)
    val redFg = if (isDark) Color(0xFFF87171) else Color(0xFFDC2626)

    val effectiveStudents = if (students.isNotEmpty()) {
        students
    } else {
        attendance.map {
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
    val absentCount = effectiveStudents.count { it.status.lowercase() in listOf("absent", "sick", "excused") }
    val totalCount = effectiveStudents.size
    val attendanceRate = if (totalCount > 0) (presentCount * 100 / totalCount) else 0
    val sessionPeriod = formatSessionPeriod(session.scheduledAt)

    val rawSubject = session.subjectName ?: session.notes ?: "Pelajaran"
    val subject = rawSubject.substringBefore(" • ").substringBefore(" (Ruang").trim()
    val roomText = session.room?.takeIf { it.isNotBlank() } ?: "Ruang Kelas"
    val className = session.className?.trim().orEmpty()
    val icon = subjectIcon(subject)

    val timeText = session.scheduledAt?.let {
        try {
            val zone = java.time.ZoneId.of("Asia/Jakarta")
            val zdt = java.time.Instant.parse(it).atZone(zone)
            val endStr = session.endedAt?.let { end ->
                val e = java.time.Instant.parse(end).atZone(zone)
                String.format(java.util.Locale.US, "%02d.%02d", e.hour, e.minute)
            }
            if (!endStr.isNullOrBlank()) {
                String.format(java.util.Locale.US, "%02d.%02d – %s WIB", zdt.hour, zdt.minute, endStr)
            } else {
                String.format(java.util.Locale.US, "%02d.%02d WIB", zdt.hour, zdt.minute)
            }
        } catch (_: Exception) { "08.00 WIB" }
    } ?: "08.00 WIB"

    val subjectArg = session.subjectName?.trim().orEmpty()

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

        // ── 1. HERO SESI ───────────────────────────────────────────────
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            color = cardBg,
            border = BorderStroke(1.dp, cardBorder),
            shadowElevation = 1.dp,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (className.isNotBlank()) {
                        SessionPill(text = className.uppercase(), bg = skyBg, fg = skyFg, border = skyFg.copy(alpha = 0.35f))
                    }
                    SessionPill(
                        text = "$sessionPeriod • $timeText",
                        bg = if (isDark) Color(0xFF27272A) else Color(0xFFF1F5F9),
                        fg = textSecondary,
                        border = null,
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(skyBg),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(icon, contentDescription = null, tint = skyFg, modifier = Modifier.size(24.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = subject,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary,
                            letterSpacing = (-0.3).sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = roomText,
                            fontSize = 12.sp,
                            color = textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    StatusChip(label = session.status)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    AttendanceMetricBox("HADIR", presentCount.toString(), greenFg, greenBg, Modifier.weight(1f))
                    AttendanceMetricBox("TIDAK HADIR", absentCount.toString(), redFg, redBg, Modifier.weight(1f))
                    AttendanceMetricBox("KEHADIRAN", "$attendanceRate%", skyFg, skyBg, Modifier.weight(1f))
                }

                if (totalCount > 0) {
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        LinearProgressIndicator(
                            progress = { (presentCount.toFloat() / totalCount).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = greenFg,
                            trackColor = innerBg,
                        )
                        Text(
                            text = "$presentCount dari $totalCount siswa hadir",
                            fontSize = 11.sp,
                            color = textTertiary,
                        )
                    }
                }
            }
        }

        // ── 2. AKSES KELAS ─────────────────────────────────────────────
        SessionSectionHeader("AKSES KELAS", textTertiary)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ResourceActionCard(
                label = "Beri Tugas",
                iconRes = CoreR.drawable.ic_modern_tasks,
                tint = skyFg, tintBg = skyBg,
                cardBg = cardBg, cardBorder = cardBorder,
                textPrimary = textPrimary,
                modifier = Modifier.weight(1f),
                onClick = { onOpenAssignments(subjectArg) },
            )
            ResourceActionCard(
                label = "Buat Kuis",
                iconRes = CoreR.drawable.ic_modern_quiz,
                tint = greenFg, tintBg = greenBg,
                cardBg = cardBg, cardBorder = cardBorder,
                textPrimary = textPrimary,
                modifier = Modifier.weight(1f),
                onClick = { onOpenQuizzes(subjectArg) },
            )
            ResourceActionCard(
                label = "Unggah Materi",
                iconRes = CoreR.drawable.ic_modern_book,
                tint = purpleFg, tintBg = purpleBg,
                cardBg = cardBg, cardBorder = cardBorder,
                textPrimary = textPrimary,
                modifier = Modifier.weight(1f),
                onClick = { onOpenMaterials(subjectArg) },
            )
        }

        // ── 3. AKTIVITAS SESI INI ──────────────────────────────────────
        if (todayMaterials.isNotEmpty() || todayAssignments.isNotEmpty()) {
            SessionSectionHeader("AKTIVITAS SESI INI", textTertiary)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                color = cardBg,
                border = BorderStroke(1.dp, cardBorder),
                shadowElevation = 1.dp,
            ) {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    val rows = todayMaterials.size + todayAssignments.size
                    var index = 0
                    todayMaterials.forEach { mat ->
                        val isVideo = mat.materialType == MaterialType.VIDEO ||
                            mat.mediaUrl?.contains("youtube", ignoreCase = true) == true ||
                            mat.mediaUrl?.contains("youtu.be", ignoreCase = true) == true
                        ActivityRow(
                            title = mat.title,
                            caption = if (isVideo) "Video pelajaran" else "Bahan bacaan",
                            iconRes = if (isVideo) CoreR.drawable.ic_modern_video else CoreR.drawable.ic_modern_book,
                            tint = if (isVideo) amberFg else purpleFg,
                            tintBg = if (isVideo) amberBg else purpleBg,
                            textPrimary = textPrimary, textSecondary = textSecondary, textTertiary = textTertiary,
                            onClick = { onOpenMaterials(subjectArg) },
                        )
                        index++
                        if (index < rows) HorizontalDivider(Modifier.padding(horizontal = 14.dp), thickness = 0.5.dp, color = cardBorder)
                    }
                    todayAssignments.forEach { assign ->
                        ActivityRow(
                            title = assign.title,
                            caption = "Tugas • Maks ${assign.maxScore} poin",
                            iconRes = CoreR.drawable.ic_modern_tasks,
                            tint = skyFg, tintBg = skyBg,
                            textPrimary = textPrimary, textSecondary = textSecondary, textTertiary = textTertiary,
                            onClick = { onOpenAssignments(subjectArg) },
                        )
                        index++
                        if (index < rows) HorizontalDivider(Modifier.padding(horizontal = 14.dp), thickness = 0.5.dp, color = cardBorder)
                    }
                }
            }
        }

        // ── 4. PRESENSI SISWA ──────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "PRESENSI SISWA",
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.5.sp,
                color = textTertiary,
            )
            if (totalCount > 0) {
                SessionPill(text = "$totalCount Siswa", bg = skyBg, fg = skyFg, border = null)
            }
        }

        if (effectiveStudents.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clickable(enabled = !isBulkSaving, onClick = onMarkAllPresent),
                    shape = RoundedCornerShape(14.dp),
                    color = greenBg,
                    border = BorderStroke(1.dp, greenFg.copy(alpha = 0.35f)),
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Icon(Icons.Default.DoneAll, contentDescription = null, tint = greenFg, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Hadir Semua", color = greenFg, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Button(
                    onClick = onSaveAll,
                    enabled = !isBulkSaving,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = skyFg, contentColor = Color.White),
                ) {
                    if (isBulkSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), color = Color.White, strokeWidth = 2.dp)
                        Spacer(Modifier.width(6.dp))
                        Text("Menyimpan...", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Simpan Presensi", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            color = cardBg,
            border = BorderStroke(1.dp, cardBorder),
            shadowElevation = 1.dp,
        ) {
            if (effectiveStudents.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(skyBg),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Default.EventAvailable, contentDescription = null, tint = skyFg, modifier = Modifier.size(24.dp))
                    }
                    Text("Data presensi belum tersedia", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = textPrimary)
                    Text(
                        "Presensi akan muncul setelah sesi dimulai.",
                        fontSize = 11.5.sp,
                        color = textTertiary,
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                    effectiveStudents.forEachIndexed { idx, student ->
                        if (idx > 0) HorizontalDivider(thickness = 0.5.dp, color = cardBorder)
                        val current = student.status.lowercase()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(skyBg),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = student.studentName.firstOrNull()?.uppercase() ?: "${idx + 1}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = skyFg,
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
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = student.nisn?.let { "NISN: $it" } ?: "ID: ${student.studentId.take(8)}",
                                    fontSize = 10.5.sp,
                                    color = textTertiary,
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                AttendanceStatusButton("H", current == "present", greenFg, innerBg, innerBorder, textSecondary) {
                                    onUpdateAttendance(student.studentId, "present")
                                }
                                AttendanceStatusButton("S", current == "sick", skyFg, innerBg, innerBorder, textSecondary) {
                                    onUpdateAttendance(student.studentId, "sick")
                                }
                                AttendanceStatusButton("I", current == "excused", amberFg, innerBg, innerBorder, textSecondary) {
                                    onUpdateAttendance(student.studentId, "excused")
                                }
                                AttendanceStatusButton("A", current == "absent", redFg, innerBg, innerBorder, textSecondary) {
                                    onUpdateAttendance(student.studentId, "absent")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionSectionHeader(text: String, color: Color) {
    Text(
        text = text,
        fontSize = 12.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = 0.5.sp,
        color = color,
        modifier = Modifier.padding(horizontal = 2.dp),
    )
}

@Composable
private fun SessionPill(text: String, bg: Color, fg: Color, border: Color?) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = bg,
        border = border?.let { BorderStroke(0.5.dp, it) },
    ) {
        Text(
            text = text,
            color = fg,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}

@Composable
private fun AttendanceMetricBox(
    label: String,
    value: String,
    color: Color,
    bg: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = value, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, color = color)
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.3.sp,
            color = color.copy(alpha = 0.8f),
            maxLines = 1,
        )
    }
}

@Composable
private fun ResourceActionCard(
    label: String,
    iconRes: Int,
    tint: Color,
    tintBg: Color,
    cardBg: Color,
    cardBorder: Color,
    textPrimary: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder),
        shadowElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(tintBg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(iconRes), contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
            }
            Text(
                text = label,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun ActivityRow(
    title: String,
    caption: String,
    iconRes: Int,
    tint: Color,
    tintBg: Color,
    textPrimary: Color,
    textSecondary: Color,
    textTertiary: Color,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(tintBg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(iconRes), contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(text = caption, fontSize = 11.sp, color = textSecondary)
        }
        Icon(
            painter = painterResource(CoreR.drawable.ic_modern_chevron_right),
            contentDescription = null,
            tint = textTertiary,
            modifier = Modifier.size(14.dp),
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
        shape = RoundedCornerShape(10.dp),
        color = if (selected) activeColor else inactiveBg,
        border = BorderStroke(0.5.dp, if (selected) activeColor else inactiveBorder),
        modifier = Modifier.size(32.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (selected) Color.White else inactiveText,
            )
        }
    }
}
