package com.schoolos.android.feature.assignments

import com.schoolos.android.core.R as CoreR
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.schoolos.android.core.common.formatPublishTimestamp
import com.schoolos.android.core.designsystem.*
import com.schoolos.android.domain.model.Assignment

fun LazyListScope.teacherAssignmentListContent(
    activeItems: List<Assignment>,
    dueSoonItems: List<Assignment>,
    scheduledItems: List<Assignment> = emptyList(),
    closedItems: List<Assignment> = emptyList(),
    selectedTab: String,
    onAssignmentClick: (String) -> Unit
) {
    val showAll = selectedTab == "Semua"
    val showScheduled = showAll || selectedTab == "Terjadwal"
    val showDueSoon = showAll || selectedTab == "Perlu Dinilai"
    val showActive = showAll || selectedTab == "Aktif"
    val showClosed = showAll || selectedTab == "Berakhir"

    val hasAnyItems = (showScheduled && scheduledItems.isNotEmpty()) ||
            (showDueSoon && dueSoonItems.isNotEmpty()) ||
            (showActive && activeItems.isNotEmpty()) ||
            (showClosed && closedItems.isNotEmpty())

    if (!hasAnyItems) {
        item {
            TeacherAssignmentEmptyState(selectedTab = selectedTab)
        }
    } else {
        if ((showAll || selectedTab == "Perlu Dinilai") && dueSoonItems.isNotEmpty()) {
            val totalUngraded = dueSoonItems.sumOf { it.effectiveUngradedCount }
            item {
                val isDark = LocalIsDarkTheme.current
                val warningBg = if (isDark) Color(0xFF31220A) else Color(0xFFFFFBEB)
                val warningBorder = if (isDark) Color(0xFFFBBF24).copy(alpha = 0.45f) else Color(0xFFFCD34D)
                val warningFg = if (isDark) Color(0xFFFBBF24) else Color(0xFFB45309)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = warningBg,
                    border = BorderStroke(1.dp, warningBorder),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            painter = painterResource(id = CoreR.drawable.ic_modern_tasks),
                            contentDescription = null,
                            tint = warningFg,
                            modifier = Modifier.size(18.dp),
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Peringatan Penilaian: ${dueSoonItems.size} Tugas Siap Dinilai",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = warningFg,
                            )
                            Text(
                                text = "$totalUngraded jawaban siswa menunggu pemeriksaan & input nilai.",
                                fontSize = 11.sp,
                                color = warningFg.copy(alpha = 0.88f),
                            )
                        }
                    }
                }
            }
        }
        if (showDueSoon && dueSoonItems.isNotEmpty()) {
            renderTeacherSection("Perlu Dinilai", dueSoonItems, onAssignmentClick)
        }
        if (showActive && activeItems.isNotEmpty()) {
            renderTeacherSection("Tugas Berjalan", activeItems, onAssignmentClick)
        }
        if (showScheduled && scheduledItems.isNotEmpty()) {
            renderTeacherSection("Draf Terjadwal", scheduledItems, onAssignmentClick)
        }
        if (showClosed && closedItems.isNotEmpty()) {
            renderTeacherSection("Tenggat Berakhir", closedItems, onAssignmentClick)
        }
    }
}

@Composable
private fun TeacherAssignmentEmptyState(selectedTab: String) {
    val isDark = LocalIsDarkTheme.current
    val cardBg = if (isDark) Color(0xFF111113) else Color.White
    val cardBorder = if (isDark) Color(0xFF1F1F23) else Color(0xFFE2E8F0)
    val textPrimary = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A)
    val textTertiary = if (isDark) Color(0xFF71717A) else Color(0xFF94A3B8)
    val innerCardBg = if (isDark) Color(0xFF161618) else Color(0xFFF8FAFC)
    val innerCardBorder = if (isDark) Color(0xFF27272A) else Color(0xFFE2E8F0)

    val (title, subtitle) = when (selectedTab) {
        "Terjadwal" -> "Tidak Ada Tugas Terjadwal" to "Belum ada draf penugasan terjadwal untuk rombel ini."
        "Aktif" -> "Tidak Ada Tugas Berjalan" to "Belum ada penugasan yang sedang aktif sebelum tenggat waktu."
        "Perlu Dinilai" -> "Semua Tugas Sudah Dinilai" to "Tidak ada jawaban siswa yang menunggu penilaian."
        "Berakhir" -> "Belum Ada Tugas Berakhir" to "Belum ada tugas yang melewati tenggat waktu."
        else -> "Tugas Tidak Ditemukan" to "Belum ada penugasan pada kategori ini."
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder),
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(innerCardBg)
                    .border(1.dp, innerCardBorder, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = CoreR.drawable.ic_modern_tasks),
                    contentDescription = null,
                    tint = textTertiary,
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(Modifier.height(14.dp))
            Text(
                text = title,
                color = textPrimary,
                fontSize = 15.5.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = subtitle,
                color = textTertiary,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun LazyListScope.renderTeacherSection(
    title: String,
    items: List<Assignment>,
    onAssignmentClick: (String) -> Unit,
) {
    if (items.isEmpty()) return

    item {
        TeacherSectionHeader(
            title = title,
            count = items.size,
        )
    }

    items(items, key = { it.id }) { assignment ->
        TeacherAssignmentCard(assignment = assignment, onClick = { onAssignmentClick(assignment.id) })
    }
}

@Composable
private fun TeacherSectionHeader(
    title: String,
    count: Int,
) {
    val isDark = LocalIsDarkTheme.current
    val textPrimary = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFFA1A1AA) else Color(0xFF64748B)
    val innerCardBg = if (isDark) Color(0xFF161618) else Color(0xFFF8FAFC)
    val innerCardBorder = if (isDark) Color(0xFF27272A) else Color(0xFFE2E8F0)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp, vertical = 4.dp)
    ) {
        Text(
            text = title,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = textPrimary,
        )
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(innerCardBg)
                .border(1.dp, innerCardBorder, RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 3.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "$count Tugas",
                color = textSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun TeacherAssignmentCard(
    assignment: Assignment,
    onClick: () -> Unit,
) {
    val isDark = LocalIsDarkTheme.current
    val cardBg = if (isDark) Color(0xFF111113) else Color.White
    val cardBorder = if (isDark) Color(0xFF1F1F23) else Color(0xFFE2E8F0)
    val textPrimary = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFFA1A1AA) else Color(0xFF64748B)
    val textTertiary = if (isDark) Color(0xFF71717A) else Color(0xFF94A3B8)
    val innerCardBg = if (isDark) Color(0xFF161618) else Color(0xFFF8FAFC)
    val innerCardBorder = if (isDark) Color(0xFF27272A) else Color(0xFFE2E8F0)
    val teacherAccentColor = if (isDark) Color(0xFF10B981) else Color(0xFF059669)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder),
        shadowElevation = 0.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            val isScheduled = assignment.isScheduled || assignment.status.equals("draft", ignoreCase = true)
            val isExpired = !isScheduled && assignment.isExpiredOrClosed
            val ungraded = assignment.effectiveUngradedCount
            val isReadyToGrade = assignment.isReadyToGrade
            val statusLabel = when {
                isScheduled -> "Terjadwal"
                isReadyToGrade && isExpired -> "Siap Dinilai ($ungraded)"
                isReadyToGrade -> "Perlu Dinilai ($ungraded)"
                isExpired && assignment.submittedCount > 0 -> "Selesai Dinilai"
                isExpired -> "Tenggat Berakhir"
                else -> "Aktif"
            }
            val statusColor = when {
                isReadyToGrade -> NeonWarning
                isExpired && assignment.submittedCount > 0 -> teacherAccentColor
                isExpired -> Color(0xFFEF4444)
                isScheduled -> NeonWarning
                else -> teacherAccentColor
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(statusColor.copy(alpha = if (isDark) 0.18f else 0.12f))
                        .border(1.dp, statusColor.copy(alpha = 0.38f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(id = CoreR.drawable.ic_modern_tasks),
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        assignment.title,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = textPrimary,
                        lineHeight = 20.sp,
                    )
                    Spacer(Modifier.height(3.dp))
                    val subtitle = listOfNotNull(
                        assignment.subjectName,
                        assignment.className?.let { "Kelas $it" }
                    ).joinToString(" • ").ifBlank {
                        assignment.assignmentType.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }
                    }
                    Text(
                        subtitle,
                        fontSize = 12.sp,
                        color = textSecondary,
                        fontWeight = FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(statusColor.copy(alpha = if (isDark) 0.18f else 0.12f))
                                .border(0.8.dp, statusColor.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = statusLabel,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = statusColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (assignment.submittedCount > 0 || assignment.totalStudents > 0) {
                            val subText = if (assignment.totalStudents > 0) {
                                "${assignment.submittedCount}/${assignment.totalStudents} Kumpul"
                            } else {
                                "${assignment.submittedCount} Kumpul"
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(innerCardBg)
                                    .border(0.8.dp, innerCardBorder, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 7.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = subText,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = textSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    val targetDate = when {
                        isScheduled -> assignment.releaseAt ?: assignment.createdAt
                        !assignment.dueAt.isNullOrBlank() -> assignment.dueAt!!
                        else -> assignment.createdAt
                    }
                    if (targetDate.isNotBlank()) {
                        Spacer(Modifier.height(5.dp))
                        val dateTint = when {
                            isScheduled -> NeonWarning
                            isReadyToGrade -> NeonWarning
                            isExpired -> Color(0xFFEF4444)
                            else -> textTertiary
                        }
                        val datePrefix = when {
                            isScheduled -> "Terjadwal"
                            !assignment.dueAt.isNullOrBlank() -> if (isExpired) "Lewat tenggat" else "Tenggat"
                            else -> "Diterbitkan"
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = CoreR.drawable.ic_modern_clock),
                                contentDescription = null,
                                tint = dateTint,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "$datePrefix: ${formatPublishTimestamp(targetDate)}",
                                fontSize = 10.5.sp,
                                fontWeight = if (isReadyToGrade || isExpired) FontWeight.SemiBold else FontWeight.Normal,
                                color = dateTint,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(Modifier.width(8.dp))
                Icon(
                    painter = painterResource(id = CoreR.drawable.ic_modern_chevron_right),
                    contentDescription = null,
                    tint = textSecondary,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}
