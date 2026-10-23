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
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
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
import com.schoolos.android.domain.model.AcademicClass
import com.schoolos.android.domain.model.AcademicPerformanceSummary
import com.schoolos.android.domain.model.Assignment
import com.schoolos.android.domain.model.ExecutiveDecisionType
import com.schoolos.android.domain.model.LearningSession
import com.schoolos.android.domain.model.Notification
import com.schoolos.android.domain.model.SchoolAnomaly
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
    onTriggerDecision: (SchoolAnomaly) -> Unit = {},
    onQuickDecisionAction: (ExecutiveDecisionType) -> Unit = {},
) {
    // ── 1. EXECUTIVE SCHOOL STABILITY & HEALTH RADAR (HERO) ───────────────────
    item {
        val level = schoolHealthMetrics?.stabilityLevel ?: SchoolStabilityLevel.STABLE
        val score = schoolHealthMetrics?.stabilityScore ?: 95

        val heroGradient = when (level) {
            SchoolStabilityLevel.CRITICAL -> listOf(Color(0xFF380808), Color(0xFF5E1313), Color(0xFF7F1D1D))
            SchoolStabilityLevel.WARNING -> listOf(Color(0xFF381504), Color(0xFF5E2407), Color(0xFF78350F))
            SchoolStabilityLevel.STABLE -> listOf(Color(0xFF032B20), Color(0xFF064E3B), Color(0xFF047857))
        }

        val accentBorder = when (level) {
            SchoolStabilityLevel.CRITICAL -> Color(0xFFEF4444)
            SchoolStabilityLevel.WARNING -> Color(0xFFF59E0B)
            SchoolStabilityLevel.STABLE -> NeonSuccess
        }

        val badgeText = when (level) {
            SchoolStabilityLevel.CRITICAL -> "STATUS: PERLU INTERVENSI SEGERA"
            SchoolStabilityLevel.WARNING -> "STATUS: WASPADA / PERHATIAN PIMPINAN"
            SchoolStabilityLevel.STABLE -> "STATUS: SEKOLAH STABIL & KONDUSIF"
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Brush.verticalGradient(heroGradient))
                .border(1.dp, accentBorder.copy(alpha = 0.65f), RoundedCornerShape(16.dp))
                .padding(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Header: Badge Status & Live Indicator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(accentBorder.copy(alpha = 0.2f))
                            .border(0.5.dp, accentBorder.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(accentBorder)
                            )
                            Text(
                                text = badgeText,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                letterSpacing = 0.5.sp,
                            )
                        }
                    }

                    Text(
                        text = "COCKPIT EKSEKUTIF",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        letterSpacing = 1.sp,
                    )
                }

                // Core Score & Summary Narrative
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "$score",
                                fontSize = 38.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextPrimary,
                                lineHeight = 40.sp,
                            )
                            Text(
                                text = " / 100",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextSecondary,
                                modifier = Modifier.padding(bottom = 6.dp, start = 2.dp),
                            )
                        }
                        Text(
                            text = "Indeks Stabilitas & Kesehatan Sekolah",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary,
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = schoolHealthMetrics?.statusSummary
                                ?: "Operasional sekolah berjalan kondusif dengan kepatuhan tinggi.",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                // 4 Pillars Micro-Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    val compRate = schoolHealthMetrics?.complianceRate?.toInt() ?: 100
                    val attRate = schoolHealthMetrics?.attendanceRate ?: 95.0
                    val mastRate = schoolHealthMetrics?.academicMasteryRate ?: 90.0
                    val atRisk = schoolHealthMetrics?.atRiskStudentsCount ?: 0

                    HealthPillarTile(
                        label = "Kepatuhan KBM",
                        value = "$compRate%",
                        isWarning = compRate < 85,
                        modifier = Modifier.weight(1f),
                    )
                    HealthPillarTile(
                        label = "Kehadiran Siswa",
                        value = "${attRate.toInt()}%",
                        isWarning = attRate < 85.0,
                        modifier = Modifier.weight(1f),
                    )
                    HealthPillarTile(
                        label = "Tuntas KKM",
                        value = "${mastRate.toInt()}%",
                        isWarning = mastRate < 75.0,
                        modifier = Modifier.weight(1f),
                    )
                    HealthPillarTile(
                        label = "Siswa Rawan",
                        value = "$atRisk Murid",
                        isWarning = atRisk > 0,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }

    // ── 2. EARLY WARNING SYSTEM (EWS) - ANOMALI & KRISIS TERDETEKSI ───────────
    item {
        val anomalies = schoolHealthMetrics?.anomalies ?: emptyList()
        PrincipalSectionHeader(
            title = "Early Warning System (EWS) Sekolah",
            subtitle = if (anomalies.isNotEmpty()) "${anomalies.size} Masalah terdeteksi memerlukan keputusan pimpinan"
            else "Seluruh parameter operasional KBM berada dalam koridor optimal",
            count = anomalies.size,
        )
    }

    val activeAnomalies = schoolHealthMetrics?.anomalies ?: emptyList()
    if (activeAnomalies.isNotEmpty()) {
        items(activeAnomalies, key = { it.id }) { anomaly ->
            AnomalyAlertCard(
                anomaly = anomaly,
                onTakeAction = { onTriggerDecision(anomaly) },
            )
        }
    } else {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CosmicNavy)
                    .border(0.5.dp, NeonSuccess.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                    .padding(14.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(NeonSuccess.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = NeonSuccess,
                            modifier = Modifier.size(20.dp),
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Tidak Ada Anomali Kritis Terdeteksi",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "KBM berjalan terjadwal, angka kehadiran stabil, dan ketuntasan KKM siswa terpantau optimal.",
                            fontSize = 11.sp,
                            color = TextSecondary,
                        )
                    }
                }
            }
        }
    }

    // ── 3. PUSAT PENGAMBILAN KEPUTUSAN EKSEKUTIF (DECISION SUPPORT HUB) ─────────
    item {
        PrincipalSectionHeader(
            title = "Pusat Keputusan Eksekutif Pimpinan",
            subtitle = "Aksi taktis terarah kepala sekolah untuk menstabilkan institusi",
        )
    }

    item {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ExecutiveActionTile(
                    title = "Instruksi Wali & BK",
                    subtitle = "Intervensi absensi & siswa",
                    badgeText = "Siswa Rawan",
                    icon = Icons.Default.SupervisorAccount,
                    accentColor = NeonBlue,
                    onClick = { onQuickDecisionAction(ExecutiveDecisionType.ORDER_HOMEROOM_FOLLOW_UP) },
                    modifier = Modifier.weight(1f),
                )

                ExecutiveActionTile(
                    title = "Mandat Remedial",
                    subtitle = "Perbaikan ketuntasan KKM",
                    badgeText = "Kurikulum",
                    icon = Icons.Default.Book,
                    accentColor = TeacherNeon,
                    onClick = { onQuickDecisionAction(ExecutiveDecisionType.ISSUE_REMEDIAL_MANDATE) },
                    modifier = Modifier.weight(1f),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ExecutiveActionTile(
                    title = "Tugaskan Inval / Piket",
                    subtitle = "Tangani jam KBM kosong",
                    badgeText = "Darurat KBM",
                    icon = Icons.Default.CalendarMonth,
                    accentColor = NeonWarning,
                    onClick = { onQuickDecisionAction(ExecutiveDecisionType.DISPATCH_SUBSTITUTE_TEACHER) },
                    modifier = Modifier.weight(1f),
                )

                ExecutiveActionTile(
                    title = "Maklumat Resmi Kepsek",
                    subtitle = "Edaran & instruksi masal",
                    badgeText = "Disposisi",
                    icon = Icons.Default.Campaign,
                    accentColor = Color(0xFFA855F7),
                    onClick = { onQuickDecisionAction(ExecutiveDecisionType.BROADCAST_PRINCIPAL_MEMO) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }

    // ── 4. AUDIT KETUNTASAN KURIKULUM & KKM MAPEL (ACADEMIC MASTERY RADAR) ─────
    val performances = schoolHealthMetrics?.subjectPerformances ?: emptyList()
    if (performances.isNotEmpty()) {
        item {
            PrincipalSectionHeader(
                title = "Audit Ketuntasan Kurikulum & KKM Mapel",
                subtitle = "Analitik ketercapaian nilai dan sebaran siswa remedial per mata pelajaran",
                count = performances.size,
                actionLabel = "Buku Nilai",
                onActionClick = onNavigateToGrades,
            )
        }

        items(performances.take(4), key = { "perf-${it.subjectId}" }) { perf ->
            SubjectMasteryAuditCard(
                performance = perf,
                onRemedialAction = {
                    onTriggerDecision(
                        SchoolAnomaly(
                            id = "remedial-${perf.subjectId}",
                            category = com.schoolos.android.domain.model.AnomalyCategory.ACADEMIC_REMEDIAL,
                            severity = if (perf.passRate < 65.0) com.schoolos.android.domain.model.AnomalySeverity.HIGH else com.schoolos.android.domain.model.AnomalySeverity.MEDIUM,
                            title = "Mandat Remedial: ${perf.subjectName}",
                            description = "${perf.remedialCount} Siswa tercatat belum tuntas pada mata pelajaran ${perf.subjectName} dengan rata-rata ${perf.averageScore}.",
                            affectedTarget = perf.subjectName,
                            targetId = perf.subjectId,
                            recommendedDecision = ExecutiveDecisionType.ISSUE_REMEDIAL_MANDATE,
                            recommendedActionLabel = "Terbitkan Mandat Remedial"
                        )
                    )
                }
            )
        }
    }

    // ── 5. PENGAWASAN SELURUH ROMBEL DAPODIK (COMMAND SHORTCUT) ────────────────
    item {
        PrincipalSectionHeader(
            title = "Akses Manajemen & Pengawasan Rombel",
            subtitle = "Satu sentuhan audit seluruh rombel, leger nilai, dan aktivitas KBM",
        )
    }

    item {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(CosmicNavy)
                .border(0.5.dp, GlassBorder, RoundedCornerShape(14.dp))
                .clickable { onNavigateToRombelStudents("ALL") }
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
                            text = "PENGAWASAN ROMBEL EKSEKUTIF",
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
                            text = "Buka Pengawasan",
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
                            text = "Audit Seluruh Rombel & Data Dapodik",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(2.dp))
                        val totalClassCount = if (schoolClasses.isNotEmpty()) schoolClasses.size else schoolHealthMetrics?.totalClasses ?: 0
                        Text(
                            text = "Memantau ${totalClassCount} rombel aktif & ${totalStudentsCount} siswa terdaftar tanpa memotong rincian Dapodik.",
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

    item {
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun HealthPillarTile(
    label: String,
    value: String,
    isWarning: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(CosmicBlack.copy(alpha = 0.5f))
            .border(
                0.5.dp,
                if (isWarning) Color(0xFFEF4444).copy(alpha = 0.6f) else GlassBorder,
                RoundedCornerShape(8.dp)
            )
            .padding(vertical = 8.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = value,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (isWarning) Color(0xFFEF4444) else TextPrimary,
                maxLines = 1,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = label,
                fontSize = 9.sp,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun AnomalyAlertCard(
    anomaly: SchoolAnomaly,
    onTakeAction: () -> Unit,
) {
    val isHigh = anomaly.severity == com.schoolos.android.domain.model.AnomalySeverity.HIGH
    val cardBorder = if (isHigh) Color(0xFFEF4444) else Color(0xFFF59E0B)
    val cardBg = if (isHigh) Color(0xFF2B0A0A) else Color(0xFF291505)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(cardBg)
            .border(1.dp, cardBorder.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
            .padding(12.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                            .clip(RoundedCornerShape(4.dp))
                            .background(cardBorder.copy(alpha = 0.25f))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = if (isHigh) "KRITIS" else "PERHATIAN",
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = cardBorder,
                            letterSpacing = 0.5.sp,
                        )
                    }

                    Text(
                        text = anomaly.affectedTarget,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = cardBorder,
                    modifier = Modifier.size(14.dp),
                )
            }

            Text(
                text = anomaly.title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
            )

            Text(
                text = anomaly.description,
                fontSize = 11.sp,
                color = TextSecondary,
                lineHeight = 15.sp,
            )

            // Direct Decision CTA Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                Button(
                    onClick = onTakeAction,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = cardBorder,
                        contentColor = Color.White,
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(32.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp),
                        )
                        Text(
                            text = anomaly.recommendedActionLabel,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExecutiveActionTile(
    title: String,
    subtitle: String,
    badgeText: String,
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

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(CosmicSurface2)
                        .padding(horizontal = 5.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = badgeText,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor,
                    )
                }
            }

            Column {
                Text(
                    text = title,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(1.dp))
                Text(
                    text = subtitle,
                    fontSize = 10.5.sp,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun SubjectMasteryAuditCard(
    performance: AcademicPerformanceSummary,
    onRemedialAction: () -> Unit,
) {
    val isAtRisk = performance.remedialCount > 0 || performance.passRate < 75.0

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CosmicNavy)
            .border(
                0.5.dp,
                if (isAtRisk) Color(0xFFEF4444).copy(alpha = 0.4f) else GlassBorder,
                RoundedCornerShape(12.dp)
            )
            .padding(12.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = performance.subjectName,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(1.dp))
                    Text(
                        text = "Rata-rata: ${performance.averageScore} • Rentang: ${performance.minScore.toInt()} - ${performance.maxScore.toInt()}",
                        fontSize = 11.sp,
                        color = TextSecondary,
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (performance.remedialCount > 0) Color(0xFFEF4444).copy(alpha = 0.15f)
                            else NeonSuccess.copy(alpha = 0.15f)
                        )
                        .border(
                            0.5.dp,
                            if (performance.remedialCount > 0) Color(0xFFEF4444).copy(alpha = 0.4f)
                            else NeonSuccess.copy(alpha = 0.4f),
                            RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 7.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = if (performance.remedialCount > 0) "${performance.remedialCount} Remedial" else "100% Tuntas",
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (performance.remedialCount > 0) Color(0xFFEF4444) else NeonSuccess,
                    )
                }
            }

            // Linear Progress Bar for Pass Rate
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "Ketuntasan KKM",
                        fontSize = 10.sp,
                        color = TextTertiary,
                    )
                    Text(
                        text = "${performance.passRate}% (${performance.passedCount}/${performance.totalGraded})",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isAtRisk) Color(0xFFEF4444) else TextSecondary,
                    )
                }
                LinearProgressIndicator(
                    progress = { (performance.passRate / 100.0).toFloat().coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = if (isAtRisk) Color(0xFFEF4444) else NeonSuccess,
                    trackColor = CosmicSurface2,
                )
            }

            if (performance.remedialCount > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    Text(
                        text = "Mandat Remedial →",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TeacherNeon,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable(onClick = onRemedialAction)
                            .padding(horizontal = 6.dp, vertical = 3.dp),
                    )
                }
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
