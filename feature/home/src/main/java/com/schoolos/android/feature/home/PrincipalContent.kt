package com.schoolos.android.feature.home

import com.schoolos.android.core.R as CoreR
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
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.schoolos.android.core.designsystem.*
import com.schoolos.android.domain.model.AcademicClass
import com.schoolos.android.domain.model.Assignment
import com.schoolos.android.domain.model.LearningSession
import com.schoolos.android.domain.model.Notification
import com.schoolos.android.domain.model.SchoolHealthMetrics
import com.schoolos.android.domain.model.SchoolStabilityLevel

private fun isUuid(str: String?): Boolean {
    if (str.isNullOrBlank()) return false
    val clean = str.trim()
    return clean.length >= 32 && clean.contains("-")
}

private fun formatClassName(raw: String?, fallback: String = "Kelas"): String {
    if (raw.isNullOrBlank() || raw == "-" || isUuid(raw)) {
        return fallback
    }
    val clean = raw.trim()
    return if (clean.startsWith("Kelas", ignoreCase = true) ||
        clean.startsWith("Paket", ignoreCase = true) ||
        clean.startsWith("Ruang", ignoreCase = true)
    ) {
        clean
    } else {
        "Kelas $clean"
    }
}

private fun formatHourMinute(raw: String?): String? {
    if (raw.isNullOrBlank()) return null
    val trimmed = raw.trim()
    if (trimmed.matches(Regex("""^\d{1,2}:\d{2}(:\d{2})?$"""))) {
        val parts = trimmed.split(":")
        val h = parts[0].toIntOrNull() ?: return null
        val m = parts[1].toIntOrNull() ?: return null
        return String.format(java.util.Locale.US, "%02d.%02d", h, m)
    }
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
    return try {
        val instant = java.time.Instant.parse(trimmed)
        val zdt = instant.atZone(java.time.ZoneId.of("Asia/Jakarta"))
        String.format(java.util.Locale.US, "%02d.%02d", zdt.hour, zdt.minute)
    } catch (_: Exception) {
        try {
            val zdt = java.time.ZonedDateTime.parse(trimmed).withZoneSameInstant(java.time.ZoneId.of("Asia/Jakarta"))
            String.format(java.util.Locale.US, "%02d.%02d", zdt.hour, zdt.minute)
        } catch (_: Exception) {
            null
        }
    }
}

fun LazyListScope.principalContent(
    onNavigateToSessions: () -> Unit,
    onNavigateToSessionDetail: (String) -> Unit = {},
    onNavigateToAssignments: () -> Unit,
    onNavigateToQuizzes: () -> Unit,
    onNavigateToGrades: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToBroadcastCenter: () -> Unit,
    onNavigateToLearning: () -> Unit = {},
    onNavigateToRombelStudents: (String) -> Unit = {},
    schoolClasses: List<AcademicClass> = emptyList(),
    totalStudentsCount: String = "0",
    todaySessions: List<LearningSession> = emptyList(),
    teacherAssignments: List<Assignment> = emptyList(),
    teacherAnnouncements: List<Notification> = emptyList(),
    pendingAssignmentsCount: String = "0",
    materialsCount: String = "0",
    scheduleCount: String = "0",
    quizzesCount: String = "0",
    schoolHealthMetrics: SchoolHealthMetrics? = null,
) {
    // ── 0. EXECUTIVE STABILITAS & KESEHATAN PKBM (SLEEK TOP COCKPIT) ─────────
    item {
        val hasData = schoolHealthMetrics != null && schoolHealthMetrics.hasRealData
        val level = schoolHealthMetrics?.stabilityLevel ?: SchoolStabilityLevel.STABLE
        val score = if (hasData) "${schoolHealthMetrics.stabilityScore}%" else "—"
        val attendance = if (hasData && schoolHealthMetrics.attendanceRate > 0) "${schoolHealthMetrics.attendanceRate.toInt()}%" else "—"
        val compliance = if (hasData && schoolHealthMetrics.complianceRate > 0) "${schoolHealthMetrics.complianceRate.toInt()}%" else "—"
        val mastery = if (hasData && schoolHealthMetrics.academicMasteryRate > 0) "${schoolHealthMetrics.academicMasteryRate.toInt()}%" else "—"

        val statusColor = when (level) {
            SchoolStabilityLevel.CRITICAL -> NeonError
            SchoolStabilityLevel.WARNING -> NeonWarning
            SchoolStabilityLevel.STABLE -> if (hasData) NeonSuccess else TextSecondary
        }

        val statusLabel = when (level) {
            SchoolStabilityLevel.CRITICAL -> "PERLU INTERVENSI SEGERA"
            SchoolStabilityLevel.WARNING -> "PERHATIAN KEPALA PKBM"
            SchoolStabilityLevel.STABLE -> if (hasData) "OPERASIONAL KONDUSIF" else "ANALITIK OPERASIONAL"
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(CosmicNavy)
                .border(0.5.dp, GlassBorder, RoundedCornerShape(14.dp))
                .padding(14.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(statusColor)
                        )
                        Text(
                            text = statusLabel,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusColor,
                            letterSpacing = 0.5.sp,
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Text(
                            text = "Indeks Stabilitas:",
                            fontSize = 11.sp,
                            color = TextSecondary,
                        )
                        Text(
                            text = score,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                        )
                    }
                }

                // 3 Pillar Micro Stats Row (Kurikulum Merdeka PKBM)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(CosmicSurface2.copy(alpha = 0.7f))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MicroHealthStat(
                        label = "Kehadiran WB",
                        value = attendance,
                        accent = NeonSuccess,
                    )

                    Box(
                        modifier = Modifier
                            .width(0.5.dp)
                            .height(20.dp)
                            .background(GlassBorder)
                    )

                    MicroHealthStat(
                        label = "Keterlaksanaan Sesi",
                        value = compliance,
                        accent = NeonBlue,
                    )

                    Box(
                        modifier = Modifier
                            .width(0.5.dp)
                            .height(20.dp)
                            .background(GlassBorder)
                    )

                    MicroHealthStat(
                        label = "Capaian KKTP",
                        value = mastery,
                        accent = TeacherNeon,
                    )
                }
            }
        }
    }

    // ── 0.5. EARLY WARNING DISPOSISI (HANYA MUNCUL JIKA ADA ANOMALI RIIL) ────────
    val topAnomaly = schoolHealthMetrics?.anomalies?.firstOrNull()
    if (topAnomaly != null) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CosmicNavy)
                    .border(0.5.dp, NeonWarning.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .clickable(onClick = onNavigateToBroadcastCenter)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(7.dp))
                                .background(NeonWarning.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = NeonWarning,
                                modifier = Modifier.size(15.dp),
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = topAnomaly.title,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = "Ketuk untuk terbitkan instruksi / maklumat Kepala PKBM",
                                fontSize = 10.5.sp,
                                color = TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = "Disposisi",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = NeonWarning,
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = NeonWarning,
                            modifier = Modifier.size(11.dp),
                        )
                    }
                }
            }
        }
    }

    // ── 1. PRINCIPAL COMMAND HERO (PEMANTAUAN SELURUH ROMBEL PKBM) ─────────
    item {
        val classCount = if (schoolClasses.isNotEmpty()) schoolClasses.size else schoolHealthMetrics?.totalClasses ?: 0
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(CosmicNavy)
                .border(0.5.dp, GlassBorder, RoundedCornerShape(14.dp))
                .clickable { onNavigateToRombelStudents("SUPERVISION") }
                .padding(14.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(TeacherNeon.copy(alpha = 0.12f))
                            .border(0.5.dp, TeacherNeon.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 7.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = "PENGAWASAN ROMBEL PKBM",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = TeacherNeon,
                            letterSpacing = 0.5.sp,
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Text(
                            text = "Buka Rombel",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TeacherNeon,
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = TeacherNeon,
                            modifier = Modifier.size(11.dp),
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(CosmicSurface2)
                            .border(0.5.dp, GlassBorder, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.SupervisorAccount,
                            contentDescription = null,
                            tint = TextPrimary,
                            modifier = Modifier.size(20.dp),
                        )
                    }

                    Spacer(Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Supervisi $classCount Rombel PKBM",
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "Akses rekapitulasi data Warga Belajar, keaktifan belajar, dan progres modul",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }

    // ── 2. EXECUTIVE ACTION & MANAGEMENT GRID (Zero Duplication) ─────────────
    item {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val actualAssignmentCount = pendingAssignmentsCount.toIntOrNull() ?: if (teacherAssignments.isNotEmpty()) teacherAssignments.size else 0
                val assignmentsCount = if (actualAssignmentCount > 0) "$actualAssignmentCount Tugas" else "0 Tugas"

                PrincipalKpiCard(
                    title = "Tugas & Modul",
                    value = assignmentsCount,
                    subtitle = "Penugasan dari tutor",
                    icon = Icons.AutoMirrored.Filled.Assignment,
                    accentColor = TeacherNeon,
                    onClick = onNavigateToAssignments,
                    modifier = Modifier.weight(1f),
                )

                val quizzesDisplay = if (quizzesCount.isNotBlank() && quizzesCount != "0") {
                    "$quizzesCount Ujian/Kuis"
                } else {
                    "Asesmen CBT"
                }

                PrincipalKpiCard(
                    title = "Ujian & Asesmen",
                    value = quizzesDisplay,
                    subtitle = "Evaluasi CBT & kuis",
                    icon = Icons.Default.Quiz,
                    accentColor = NeonBlue,
                    onClick = onNavigateToQuizzes,
                    modifier = Modifier.weight(1f),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PrincipalKpiCard(
                    title = "Maklumat & Memo",
                    value = "Pusat Broadcast",
                    subtitle = "Kirim instruksi resmi",
                    icon = Icons.Default.Campaign,
                    accentColor = NeonWarning,
                    onClick = onNavigateToBroadcastCenter,
                    modifier = Modifier.weight(1f),
                )

                PrincipalKpiCard(
                    title = "Bank Modul & CBT",
                    value = if (materialsCount.isNotBlank() && materialsCount != "0") "$materialsCount Modul" else "Perangkat",
                    subtitle = "Modul & bahan ajar",
                    icon = Icons.Default.Book,
                    accentColor = NeonSuccess,
                    onClick = onNavigateToLearning,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }

    // ── 4. JADWAL PEMBELAJARAN / TUTORIAL HARI INI ────────────────────────────
    item {
        PrincipalSectionHeader(
            title = "Jadwal Tutorial & Sesi Hari Ini",
            subtitle = "Monitoring jalannya sesi tutorial seluruh rombel",
            count = todaySessions.size,
            actionLabel = "Lihat Semua",
            onActionClick = onNavigateToSessions,
        )
    }

    if (todaySessions.isNotEmpty()) {
        items(todaySessions.take(3), key = { it.id }) { session ->
            val isLive = session.status.equals("active", ignoreCase = true)
            val sessionClass = formatClassName(session.className ?: session.room)
            val startHour = formatHourMinute(session.startTime)
                ?: formatHourMinute(session.scheduledAt)
                ?: formatHourMinute(session.startedAt)
                ?: "07.30"
            val endHour = formatHourMinute(session.endTime)
                ?: formatHourMinute(session.endedAt)
                ?: "09.00"

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CosmicNavy)
                    .border(
                        0.5.dp,
                        if (isLive) NeonSuccess.copy(alpha = 0.45f) else GlassBorder,
                        RoundedCornerShape(12.dp)
                    )
                    .clickable { onNavigateToSessionDetail(session.id) }
                    .padding(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                text = session.subjectName?.ifBlank { "Modul / Pelajaran" } ?: "Modul / Pelajaran",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false),
                            )
                            if (isLive) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(NeonSuccess.copy(alpha = 0.15f))
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = "LIVE",
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NeonSuccess,
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(3.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "$sessionClass • ${session.teacherName ?: "Tutor"} • $startHour - $endHour",
                                fontSize = 11.sp,
                                color = TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = TextTertiary,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        }
    }

    // ── 5. MONITORING PENUGASAN WARGA BELAJAR ──────────────────────────────────
    if (teacherAssignments.isNotEmpty()) {
        item {
            PrincipalSectionHeader(
                title = "Monitoring Tugas Warga Belajar",
                subtitle = "Rekapitulasi penugasan aktif dari tutor",
                count = pendingAssignmentsCount.toIntOrNull() ?: teacherAssignments.size,
                actionLabel = "Lihat Semua",
                onActionClick = onNavigateToAssignments,
            )
        }

        items(teacherAssignments.take(3), key = { "assign-${it.id}" }) { assignment ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CosmicNavy)
                    .border(0.5.dp, GlassBorder, RoundedCornerShape(12.dp))
                    .clickable { onNavigateToAssignments() }
                    .padding(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = assignment.title.ifBlank { "Tugas Warga Belajar" },
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "${assignment.subjectName ?: "Modul"} • ${formatClassName(assignment.className, "Semua Rombel")}",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = TextTertiary,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        }
    }

    item {
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun MicroHealthStat(
    label: String,
    value: String,
    accent: Color,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = accent,
        )
        Text(
            text = label,
            fontSize = 9.5.sp,
            color = TextSecondary,
        )
    }
}

@Composable
private fun PrincipalKpiCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(CosmicNavy)
            .border(0.5.dp, GlassBorder, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(accentColor.copy(alpha = 0.12f))
                        .border(0.5.dp, accentColor.copy(alpha = 0.25f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(16.dp),
                    )
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = TextTertiary,
                    modifier = Modifier.size(12.dp),
                )
            }

            Column {
                Text(
                    text = value,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(1.dp))
                Text(
                    text = subtitle,
                    fontSize = 10.5.sp,
                    color = TextTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun PrincipalSectionHeader(
    title: String,
    subtitle: String = "",
    count: Int? = null,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f, fill = false)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = title,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                )
                if (count != null && count > 0) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(CosmicSurface2)
                            .border(0.5.dp, GlassBorder, RoundedCornerShape(4.dp))
                            .padding(horizontal = 5.dp, vertical = 1.dp),
                    ) {
                        Text(
                            text = "$count",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                        )
                    }
                }
            }
            if (subtitle.isNotBlank()) {
                Spacer(Modifier.height(1.dp))
                Text(
                    text = subtitle,
                    fontSize = 10.5.sp,
                    color = TextTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        if (actionLabel != null && onActionClick != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(onClick = onActionClick)
                    .padding(horizontal = 6.dp, vertical = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = actionLabel,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = TeacherNeon,
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = TeacherNeon,
                    modifier = Modifier.size(11.dp),
                )
            }
        }
    }
}
