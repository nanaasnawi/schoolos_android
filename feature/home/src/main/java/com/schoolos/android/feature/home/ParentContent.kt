package com.schoolos.android.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.schoolos.android.core.designsystem.CosmicNavy
import com.schoolos.android.core.designsystem.DonutChart
import com.schoolos.android.core.designsystem.GlassBorder
import com.schoolos.android.core.designsystem.NeonBlue
import com.schoolos.android.core.designsystem.NeonError
import com.schoolos.android.core.designsystem.NeonSuccess
import com.schoolos.android.core.designsystem.NeonWarning
import com.schoolos.android.core.designsystem.ParentNeon
import com.schoolos.android.core.designsystem.TextPrimary
import com.schoolos.android.core.designsystem.TextSecondary
import com.schoolos.android.core.designsystem.TextTertiary

data class ActivityItem(
    val title: String,
    val description: String,
    val timestamp: String,
    val type: ActivityType = ActivityType.INFO,
)

enum class ActivityType {
    ATTENDANCE, ASSIGNMENT, GRADE, ACHIEVEMENT, INFO
}

fun LazyListScope.parentContent(
    childName: String = "",
    childClass: String = "",
    attendanceRate: String = "-",
    presentDays: String = "-",
    permitDays: String = "-",
    absentDays: String = "-",
    assignmentsCount: String = "0",
    activities: List<ActivityItem> = emptyList(),
    currentActivity: String = "",
    studentStatus: String = "Siswa Aktif",
    onNavigateToProgress: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToAssignments: () -> Unit,
    onNavigateToGrades: () -> Unit,
    onNavigateToAchievements: () -> Unit,
) {
    val displayChildName = if (childName.isNotBlank() && childName != "-") childName else "Anak Anda"
    val displayClass = if (childClass.isNotBlank() && childClass != "-") childClass else "Kelas Aktif"

    // ── 1. CHILD PRESENCE SPOTLIGHT CARD ─────────────────────────────────────────
    item {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CosmicNavy)
                .background(
                    Brush.linearGradient(
                        listOf(ParentNeon.copy(alpha = 0.12f), Color.Transparent)
                    )
                )
                .border(1.dp, ParentNeon.copy(alpha = 0.30f), RoundedCornerShape(16.dp))
                .clickable(onClick = onNavigateToProgress)
                .padding(18.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(ParentNeon.copy(alpha = 0.18f))
                            .border(2.dp, ParentNeon.copy(alpha = 0.40f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChildCare,
                            contentDescription = null,
                            tint = ParentNeon,
                            modifier = Modifier.size(30.dp),
                        )
                    }

                    Spacer(Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(NeonSuccess.copy(alpha = 0.18f))
                                    .padding(horizontal = 7.dp, vertical = 2.dp),
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(5.dp)
                                            .clip(CircleShape)
                                            .background(NeonSuccess),
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = studentStatus,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        color = NeonSuccess,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = displayClass,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextTertiary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = displayChildName,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "Presensi: $attendanceRate • $assignmentsCount Tugas Terdata",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }

    // ── 2. BENTO QUICK ACCESS WALI MURID ─────────────────────────────────────────
    item {
        LightSectionHeader(
            title = "Akses Pemantauan",
            sub = "Aktivitas belajar dan perkembangan",
        )
    }

    item {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                BentoActionCard(
                    title = "Perkembangan",
                    subtitle = "Kehadiran & profil",
                    icon = Icons.Default.Assessment,
                    accentColor = ParentNeon,
                    badgeText = attendanceRate,
                    onClick = onNavigateToProgress,
                    modifier = Modifier.weight(1f),
                )

                BentoActionCard(
                    title = "Tugas Rumah",
                    subtitle = "Pantau PR anak",
                    icon = Icons.AutoMirrored.Filled.Assignment,
                    accentColor = NeonBlue,
                    badgeText = "$assignmentsCount Tugas",
                    onClick = onNavigateToAssignments,
                    modifier = Modifier.weight(1f),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                BentoActionCard(
                    title = "Rapor & Nilai",
                    subtitle = "Hasil ujian berkala",
                    icon = Icons.Default.Assessment,
                    accentColor = NeonSuccess,
                    onClick = onNavigateToGrades,
                    modifier = Modifier.weight(1f),
                )

                BentoActionCard(
                    title = "Pesan Sekolah",
                    subtitle = "Pengumuman wali kelas",
                    icon = Icons.AutoMirrored.Filled.Message,
                    accentColor = NeonWarning,
                    onClick = onNavigateToNotifications,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }

    // ── 3. DETAILED ATTENDANCE CARD ──────────────────────────────────────────────
    item {
        LightSectionHeader(
            title = "Ringkasan Kehadiran",
            sub = "Semester Ganjil Berjalan",
        )
    }

    item {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CosmicNavy)
                .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
                .padding(18.dp),
        ) {
            val parsedRate = attendanceRate.removeSuffix("%").toFloatOrNull()?.let { it / 100f } ?: 0.95f
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DonutChart(
                    percentage = parsedRate,
                    activeColor = NeonSuccess,
                    backgroundColor = NeonSuccess.copy(alpha = 0.12f),
                    labelText = attendanceRate,
                    modifier = Modifier.size(105.dp),
                    strokeWidth = 18f,
                )
                Spacer(Modifier.width(24.dp))
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    ParentAttendanceLegendRow("Hadir", if (presentDays != "-") "$presentDays Hari" else "Tercatat", NeonSuccess)
                    ParentAttendanceLegendRow("Izin/Sakit", if (permitDays != "-") "$permitDays Hari" else "0 Hari", NeonBlue)
                    ParentAttendanceLegendRow("Tanpa Keterangan", if (absentDays != "-") "$absentDays Hari" else "0 Hari", NeonError)
                }
            }
        }
    }
}

@Composable
private fun ParentAttendanceLegendRow(label: String, value: String, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondary,
            )
        }
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
        )
    }
}
