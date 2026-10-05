package com.schoolos.android.feature.home

import com.schoolos.android.core.R as CoreR
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Class
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SupervisorAccount
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
    todaySessions: List<LearningSession> = emptyList(),
    teacherAssignments: List<Assignment> = emptyList(),
    teacherAnnouncements: List<Notification> = emptyList(),
    pendingAssignmentsCount: String = "0",
    materialsCount: String = "0",
    scheduleCount: String = "0",
    quizzesCount: String = "0",
) {
    // ── 1. EXECUTIVE PRINCIPAL COCKPIT METRICS (2x2 Grid) ─────────────────────
    item {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PrincipalKpiCard(
                    title = "Data Rombel",
                    value = if (schoolClasses.isNotEmpty()) "${schoolClasses.size} Kelas" else "13 Rombel",
                    subtitle = "Seluruh rombel sekolah",
                    icon = Icons.Default.Class,
                    accentColor = TeacherNeon,
                    onClick = { onNavigateToRombelStudents("ALL") },
                    modifier = Modifier.weight(1f),
                )

                PrincipalKpiCard(
                    title = "Sesi Kuis & CBT",
                    value = if (quizzesCount.isNotBlank()) "$quizzesCount Kuis" else "0 Kuis",
                    subtitle = "Evaluasi sekolah",
                    icon = Icons.AutoMirrored.Filled.Assignment,
                    accentColor = NeonSuccess,
                    onClick = onNavigateToQuizzes,
                    modifier = Modifier.weight(1f),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PrincipalKpiCard(
                    title = "Agenda Mengajar",
                    value = if (todaySessions.isNotEmpty()) "${todaySessions.size} Sesi" else if (scheduleCount.isNotBlank()) "$scheduleCount Sesi" else "0 Sesi",
                    subtitle = "Jadwal harian sekolah",
                    icon = Icons.Default.CalendarMonth,
                    accentColor = NeonBlue,
                    onClick = onNavigateToSessions,
                    modifier = Modifier.weight(1f),
                )

                PrincipalKpiCard(
                    title = "Modul Ajar",
                    value = if (materialsCount.isNotBlank()) "$materialsCount Modul" else "0 Modul",
                    subtitle = "Materi & dokumen",
                    icon = Icons.Default.Book,
                    accentColor = NeonWarning,
                    onClick = onNavigateToLearning,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }

    // ── 2. PRINCIPAL COMMAND HERO (PEMANTAUAN SELURUH ROMBEL SEKOLAH) ─────────────
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
                            text = "DASHBOARD KEPALA SEKOLAH",
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
                            text = "Kelola Rombel",
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
                            text = "Pengawasan Seluruh Rombel Sekolah",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "Akses rekapitulasi data siswa, jadwal mengajar, dan aktivitas akademik seluruh kelas",
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

    // ── 3. AKSI KEPALA SEKOLAH (Pusat Pengumuman & Leger Nilai) ────────────────
    item {
        PrincipalSectionHeader(
            title = "Aksi & Manajemen Sekolah",
            subtitle = "Pusat broadcast pengumuman, leger nilai, dan dokumen ajar",
        )
    }

    item {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                BentoActionCard(
                    title = "Broadcast Sekolah",
                    subtitle = "Kirim pesan masal",
                    iconRes = CoreR.drawable.ic_modern_chat,
                    accentColor = TeacherNeon,
                    badgeText = "Pesan",
                    onClick = onNavigateToBroadcastCenter,
                    modifier = Modifier.weight(1f),
                )

                BentoActionCard(
                    title = "Buku Nilai Sekolah",
                    subtitle = "Leger & capaian KKM",
                    iconRes = CoreR.drawable.ic_modern_grades,
                    accentColor = NeonBlue,
                    badgeText = "Nilai",
                    onClick = onNavigateToGrades,
                    modifier = Modifier.weight(1f),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                BentoActionCard(
                    title = "Bank Kuis & CBT",
                    subtitle = "Ujian & evaluasi",
                    iconRes = CoreR.drawable.ic_modern_quiz,
                    accentColor = NeonWarning,
                    badgeText = "Kuis",
                    onClick = onNavigateToQuizzes,
                    modifier = Modifier.weight(1f),
                )

                BentoActionCard(
                    title = "Perpustakaan Ajar",
                    subtitle = "Modul & bahan ajar",
                    iconRes = CoreR.drawable.ic_modern_book,
                    accentColor = NeonSuccess,
                    badgeText = "Modul",
                    onClick = onNavigateToLearning,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }

    // ── 4. ANUMERASI JADWAL AKADEMIK SEKOLAH ──────────────────────────────────
    item {
        PrincipalSectionHeader(
            title = "Jadwal Pembelajaran Sekolah Hari Ini",
            subtitle = "Agenda tatap muka & sesi mengajar terdaftar",
            count = todaySessions.size,
            actionLabel = "Lihat Semua",
            onActionClick = onNavigateToSessions,
        )
    }

    if (todaySessions.isNotEmpty()) {
        items(todaySessions.take(3), key = { it.id }) { session ->
            val isLive = session.status.equals("active", ignoreCase = true)
            val sessionClass = formatClassName(session.className ?: session.room)

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
                        Text(
                            text = session.subjectName?.ifBlank { "Mata Pelajaran" } ?: "Mata Pelajaran",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "$sessionClass • ${session.teacherName ?: "Pengajar"}",
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

    item {
        Spacer(Modifier.height(16.dp))
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
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(1.dp))
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
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
                    fontSize = 15.sp,
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
                    fontSize = 11.sp,
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
