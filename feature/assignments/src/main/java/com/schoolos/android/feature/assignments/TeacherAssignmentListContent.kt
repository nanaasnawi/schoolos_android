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
    selectedTab: String,
    onAssignmentClick: (String) -> Unit
) {
    val showAll = selectedTab == "Semua"
    val showScheduled = showAll || selectedTab == "Terjadwal"
    val showDueSoon = showAll || selectedTab == "Perlu Dinilai"
    val showActive = showAll || selectedTab == "Aktif"

    val hasAnyItems = (showScheduled && scheduledItems.isNotEmpty()) ||
            (showDueSoon && dueSoonItems.isNotEmpty()) ||
            (showActive && activeItems.isNotEmpty())

    if (!hasAnyItems) {
        item {
            TeacherAssignmentEmptyState(selectedTab = selectedTab)
        }
    } else {
        if (showScheduled && scheduledItems.isNotEmpty()) {
            renderTeacherSection("Draf Terjadwal", scheduledItems, onAssignmentClick)
        }
        if (showDueSoon && dueSoonItems.isNotEmpty()) {
            renderTeacherSection("Perlu Dinilai", dueSoonItems, onAssignmentClick)
        }
        if (showActive && activeItems.isNotEmpty()) {
            renderTeacherSection("Tugas Berjalan", activeItems, onAssignmentClick)
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
        "Aktif" -> "Tidak Ada Tugas Berjalan" to "Belum ada penugasan yang sedang aktif saat ini."
        "Perlu Dinilai" -> "Semua Tugas Sudah Dinilai" to "Tidak ada tugas siswa yang menunggu penilaian."
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isDark) Color(0xFF064E3B).copy(alpha = 0.35f) else Color(0xFFECFDF5))
                        .border(1.dp, if (isDark) Color(0xFF10B981).copy(alpha = 0.4f) else Color(0xFFA7F3D0), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(id = CoreR.drawable.ic_modern_tasks),
                        contentDescription = null,
                        tint = teacherAccentColor,
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

                    val isScheduled = assignment.isScheduled || assignment.status == "draft"
                    val targetDate = if (isScheduled) assignment.releaseAt ?: assignment.createdAt else assignment.createdAt
                    if (targetDate.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = CoreR.drawable.ic_modern_clock),
                                contentDescription = null,
                                tint = if (isScheduled) NeonWarning else textTertiary,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = if (isScheduled) {
                                    "Terjadwal: ${formatPublishTimestamp(targetDate)}"
                                } else {
                                    "Diterbitkan: ${formatPublishTimestamp(targetDate)}"
                                },
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Normal,
                                color = if (isScheduled) NeonWarning else textTertiary,
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
