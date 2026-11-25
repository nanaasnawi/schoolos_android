package com.schoolos.android.feature.sessions

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.schoolos.android.core.R as CoreR

/**
 * Shared loading and empty-state card for the session materials/assignments section
 * used by both student and teacher session detail views.
 */
@Composable
internal fun SessionResourcesPlaceholderCard(
    sectionTitle: String,
    isResourcesLoading: Boolean,
    isTodaySession: Boolean,
    isTeacherView: Boolean,
    teacherName: String?,
    isDark: Boolean,
    cardBg: Color,
    cardBorder: Color,
    textPrimary: Color,
    textSecondary: Color,
    textTertiary: Color,
    accentColor: Color,
    onActionClick: () -> Unit,
) {
    val innerBg = if (isDark) Color(0xFF161618) else Color(0xFFF8FAFC)
    val innerBorder = if (isDark) Color(0xFF27272A) else Color(0xFFE2E8F0)

    val badgeBg: Color
    val badgeBorder: Color
    val badgeFg: Color
    val badgeLabel: String

    when {
        isResourcesLoading -> {
            badgeBg = if (isDark) Color(0xFF27272A) else Color(0xFFF1F5F9)
            badgeBorder = if (isDark) Color(0xFF3F3F46) else Color(0xFFCBD5E1)
            badgeFg = textSecondary
            badgeLabel = "Memuat..."
        }
        isTodaySession -> {
            badgeBg = if (isDark) Color(0xFF31220A) else Color(0xFFFEF3C7)
            badgeBorder = if (isDark) Color(0xFFFBBF24).copy(alpha = 0.40f) else Color(0xFFFCD34D)
            badgeFg = if (isDark) Color(0xFFFBBF24) else Color(0xFFB45309)
            badgeLabel = if (isTeacherView) "Belum Diisi" else "Belum Diisi Guru"
        }
        else -> {
            badgeBg = if (isDark) Color(0xFF27272A) else Color(0xFFF1F5F9)
            badgeBorder = if (isDark) Color(0xFF3F3F46) else Color(0xFFCBD5E1)
            badgeFg = textSecondary
            badgeLabel = "Belum Tersedia"
        }
    }

    val iconTint = when {
        isTodaySession -> if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706)
        else -> accentColor
    }
    val iconBg = iconTint.copy(alpha = if (isDark) 0.16f else 0.12f)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder),
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = sectionTitle,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary,
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = badgeBg,
                    border = BorderStroke(1.dp, badgeBorder),
                ) {
                    Text(
                        text = badgeLabel,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = badgeFg,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                    )
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = innerBg,
                border = BorderStroke(1.dp, innerBorder),
            ) {
                if (isResourcesLoading) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = accentColor,
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "Memeriksa materi & tugas sesi ini...",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = textPrimary,
                            )
                            Text(
                                text = "Menyelaraskan bahan ajar dan tugas terbaru.",
                                fontSize = 11.5.sp,
                                color = textTertiary,
                            )
                        }
                    }
                } else {
                    val teacherLabel = teacherName
                        ?.trim()
                        ?.takeIf { it.isNotEmpty() }
                        ?.let { "Guru pengampu ($it)" }
                        ?: "Guru pengampu"

                    val emptyTitle = when {
                        isTeacherView && isTodaySession -> "Belum Ada Materi & Tugas Hari Ini"
                        isTeacherView -> "Belum Ada Bahan Ajar & Tugas di Sesi Ini"
                        isTodaySession -> "Guru Belum Mengisi Materi & Tugas Hari Ini"
                        else -> "Belum Ada Materi & Tugas untuk Sesi Ini"
                    }
                    val emptySubtitle = when {
                        isTeacherView && isTodaySession ->
                            "Siswa belum melihat materi atau tugas baru untuk pertemuan hari ini. Tambahkan materi atau tugas melalui menu Sumber Daya Pelajaran di atas."
                        isTeacherView ->
                            "Belum ada bahan ajar maupun tugas yang tertaut ke tanggal sesi ini. Anda tetap dapat mengelola arsip materi mata pelajaran di atas."
                        isTodaySession ->
                            "$teacherLabel belum mengunggah materi atau tugas baru untuk pertemuan hari ini. Silakan periksa kembali nanti atau pelajari arsip materi mata pelajaran."
                        else ->
                            "Belum ada materi maupun tugas yang dijadwalkan untuk tanggal sesi ini. Anda tetap dapat membuka arsip materi mata pelajaran di atas."
                    }
                    val actionLabel = if (isTeacherView) "Kelola Materi Mapel" else "Lihat Semua Materi Mapel"

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(iconBg),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                painter = painterResource(CoreR.drawable.ic_modern_book),
                                contentDescription = null,
                                tint = iconTint,
                                modifier = Modifier.size(21.dp),
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = emptyTitle,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(5.dp))
                        Text(
                            text = emptySubtitle,
                            fontSize = 12.sp,
                            color = textSecondary,
                            textAlign = TextAlign.Center,
                            lineHeight = 17.5.sp,
                        )
                        Spacer(Modifier.height(12.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = accentColor.copy(alpha = if (isDark) 0.16f else 0.10f),
                            border = BorderStroke(1.dp, accentColor.copy(alpha = if (isDark) 0.35f else 0.25f)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable(onClick = onActionClick),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Icon(
                                    painter = painterResource(CoreR.drawable.ic_modern_book),
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(14.dp),
                                )
                                Text(
                                    text = actionLabel,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = accentColor,
                                )
                                Spacer(Modifier.width(2.dp))
                                Icon(
                                    painter = painterResource(CoreR.drawable.ic_modern_chevron_right),
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(13.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
