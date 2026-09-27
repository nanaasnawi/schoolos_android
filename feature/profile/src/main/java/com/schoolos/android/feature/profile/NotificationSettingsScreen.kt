package com.schoolos.android.feature.profile

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.schoolos.android.core.R as CoreR
import com.schoolos.android.core.designsystem.CosmicBlack
import com.schoolos.android.core.designsystem.CosmicDark
import com.schoolos.android.core.designsystem.CosmicNavy
import com.schoolos.android.core.designsystem.ExecutiveTopBar
import com.schoolos.android.core.designsystem.GlassBorder
import com.schoolos.android.core.designsystem.NeonBlue
import com.schoolos.android.core.designsystem.NeonError
import com.schoolos.android.core.designsystem.NeonSuccess
import com.schoolos.android.core.designsystem.NeonWarning
import com.schoolos.android.core.designsystem.StudentNeon
import com.schoolos.android.core.designsystem.TeacherNeon
import com.schoolos.android.core.designsystem.TextPrimary
import com.schoolos.android.core.designsystem.TextSecondary
import com.schoolos.android.core.designsystem.TextTertiary

@Composable
fun NotificationSettingsScreen(
    onBack: () -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()

    var masterPush by remember { mutableStateOf(true) }
    var assignmentReminders by remember { mutableStateOf(true) }
    var announcementAlerts by remember { mutableStateOf(true) }
    var scheduleReminders by remember { mutableStateOf(true) }
    var gradeAlerts by remember { mutableStateOf(true) }
    var soundEnabled by remember { mutableStateOf(true) }
    var vibrateEnabled by remember { mutableStateOf(true) }
    var quietHours by remember { mutableStateOf(false) }

    val activeCount = listOf(assignmentReminders, announcementAlerts, scheduleReminders, gradeAlerts)
        .count { it && masterPush }

    Scaffold(
        containerColor = CosmicBlack,
        topBar = {
            ExecutiveTopBar(
                title = "Notifikasi Akademik",
                subtitle = "Pengingat Tugas, Jadwal & Pengumuman",
                onBack = onBack,
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // ── 1. HERO STATUS & MASTER TOGGLE CARD ──────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CosmicNavy)
                    .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
                    .padding(18.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(NeonBlue.copy(alpha = 0.12f))
                                    .border(1.dp, NeonBlue.copy(alpha = 0.25f), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    painter = painterResource(id = CoreR.drawable.ic_modern_bell),
                                    contentDescription = null,
                                    tint = NeonBlue,
                                    modifier = Modifier.size(22.dp),
                                )
                            }
                            Column {
                                Text(
                                    text = "PUSAT NOTIFIKASI AKADEMIK",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextTertiary,
                                    letterSpacing = 0.8.sp,
                                )
                                Text(
                                    text = "Saluran Pemberitahuan",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                )
                            }
                        }

                        val badgeColor = if (masterPush) NeonSuccess else NeonError
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(badgeColor.copy(alpha = 0.14f))
                                .border(1.dp, badgeColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                        ) {
                            Text(
                                text = if (masterPush) "● AKTIF" else "○ DINONAKTIFKAN",
                                color = badgeColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp,
                            )
                        }
                    }

                    Text(
                        text = "Pemberitahuan push real-time untuk ${state.schoolName.ifBlank { "kegiatan akademik" }}, batas pengumpulan tugas, kuis CBT, dan pengumuman resmi sekolah.",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        lineHeight = 18.sp,
                    )

                    HorizontalDivider(color = GlassBorder, thickness = 0.5.dp)

                    // Master Push Switch Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (masterPush) NeonBlue.copy(alpha = 0.12f) else CosmicDark)
                                    .border(
                                        1.dp,
                                        if (masterPush) NeonBlue.copy(alpha = 0.25f) else GlassBorder,
                                        RoundedCornerShape(10.dp),
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = if (masterPush) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                                    contentDescription = null,
                                    tint = if (masterPush) NeonBlue else TextTertiary,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                            Column {
                                Text(
                                    text = "Terima Notifikasi Push",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary,
                                )
                                Text(
                                    text = if (masterPush) "Pemberitahuan push diaktifkan" else "Pemberitahuan disenyapkan sepenuhnya",
                                    fontSize = 11.sp,
                                    color = TextSecondary,
                                )
                            }
                        }

                        Switch(
                            checked = masterPush,
                            onCheckedChange = {
                                masterPush = it
                                Toast.makeText(
                                    context,
                                    if (it) "Notifikasi push diaktifkan" else "Notifikasi push dinonaktifkan",
                                    Toast.LENGTH_SHORT,
                                ).show()
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = NeonBlue,
                                uncheckedThumbColor = TextTertiary,
                                uncheckedTrackColor = CosmicDark,
                            ),
                        )
                    }

                    HorizontalDivider(color = GlassBorder, thickness = 0.5.dp)

                    // Quick Meta Indicators
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        NotificationMetaPill(
                            painter = painterResource(id = CoreR.drawable.ic_modern_check_circle),
                            label = "Status Saluran",
                            value = if (masterPush) "$activeCount Kategori Aktif" else "Layanan Nonaktif",
                            accentColor = if (masterPush) NeonSuccess else NeonError,
                            modifier = Modifier.weight(1f),
                        )
                        NotificationMetaPill(
                            imageVector = Icons.Default.AccessTime,
                            label = "Jam Tenang",
                            value = if (quietHours && masterPush) "Aktif (Malam)" else "Nonaktif (24 Jam)",
                            accentColor = if (quietHours && masterPush) StudentNeon else TextTertiary,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            // ── 2. ACADEMIC CATEGORIES CARD ──────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CosmicNavy)
                    .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
                    .padding(18.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = "KATEGORI PENGINGAT AKADEMIK",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextTertiary,
                        letterSpacing = 0.8.sp,
                    )

                    NotificationToggleRow(
                        painter = painterResource(id = CoreR.drawable.ic_modern_tasks),
                        accentColor = NeonWarning,
                        title = "Pengingat Tugas & Kuis CBT",
                        desc = "Pemberitahuan tugas baru dan alarm H-1 sebelum batas waktu pengumpulan.",
                        checked = assignmentReminders && masterPush,
                        enabled = masterPush,
                        onCheckedChange = { assignmentReminders = it },
                    )

                    HorizontalDivider(color = GlassBorder, thickness = 0.5.dp)

                    NotificationToggleRow(
                        painter = painterResource(id = CoreR.drawable.ic_modern_calendar),
                        accentColor = NeonBlue,
                        title = "Jadwal Pelajaran & Sesi Belajar",
                        desc = "Pemberitahuan jadwal harian dan peringatan sesi kelas yang akan dimulai.",
                        checked = scheduleReminders && masterPush,
                        enabled = masterPush,
                        onCheckedChange = { scheduleReminders = it },
                    )

                    HorizontalDivider(color = GlassBorder, thickness = 0.5.dp)

                    NotificationToggleRow(
                        painter = painterResource(id = CoreR.drawable.ic_modern_grades),
                        accentColor = NeonSuccess,
                        title = "Pembaruan Nilai & Asesmen",
                        desc = "Pemberitahuan saat guru mengunggah nilai tugas, ujian, atau lembar rapor.",
                        checked = gradeAlerts && masterPush,
                        enabled = masterPush,
                        onCheckedChange = { gradeAlerts = it },
                    )

                    HorizontalDivider(color = GlassBorder, thickness = 0.5.dp)

                    NotificationToggleRow(
                        painter = painterResource(id = CoreR.drawable.ic_modern_announcement),
                        accentColor = TeacherNeon,
                        title = "Pengumuman & Siaran Sekolah",
                        desc = "Surat edaran resmi, agenda libur, dan berita penting dari pihak sekolah.",
                        checked = announcementAlerts && masterPush,
                        enabled = masterPush,
                        onCheckedChange = { announcementAlerts = it },
                    )
                }
            }

            // ── 3. DEVICE SOUND & PREFERENCES ─────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CosmicNavy)
                    .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
                    .padding(18.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = "PREFERENSI PERANGKAT & SUARA",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextTertiary,
                        letterSpacing = 0.8.sp,
                    )

                    NotificationToggleRow(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        accentColor = StudentNeon,
                        title = "Suara Notifikasi",
                        desc = "Bunyikan nada dering saat notifikasi akademik masuk ke perangkat.",
                        checked = soundEnabled && masterPush,
                        enabled = masterPush,
                        onCheckedChange = { soundEnabled = it },
                    )

                    HorizontalDivider(color = GlassBorder, thickness = 0.5.dp)

                    NotificationToggleRow(
                        imageVector = Icons.Default.Vibration,
                        accentColor = TeacherNeon,
                        title = "Getaran Perangkat",
                        desc = "Aktifkan getaran haptik untuk pemberitahuan tugas dan jadwal penting.",
                        checked = vibrateEnabled && masterPush,
                        enabled = masterPush,
                        onCheckedChange = { vibrateEnabled = it },
                    )

                    HorizontalDivider(color = GlassBorder, thickness = 0.5.dp)

                    NotificationToggleRow(
                        imageVector = Icons.Default.AccessTime,
                        accentColor = NeonBlue,
                        title = "Jam Tenang (21.00 – 06.00)",
                        desc = "Senyapkan pengingat akademik selama jam istirahat malam hari.",
                        checked = quietHours && masterPush,
                        enabled = masterPush,
                        onCheckedChange = { quietHours = it },
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

// ── SUBCOMPONENTS ─────────────────────────────────────────────────────────────

@Composable
private fun NotificationToggleRow(
    painter: Painter? = null,
    imageVector: ImageVector? = null,
    accentColor: Color,
    title: String,
    desc: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (enabled && checked) accentColor.copy(alpha = 0.12f) else CosmicDark)
                    .border(
                        1.dp,
                        if (enabled && checked) accentColor.copy(alpha = 0.25f) else GlassBorder,
                        RoundedCornerShape(10.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (painter != null) {
                    Icon(
                        painter = painter,
                        contentDescription = null,
                        tint = if (enabled && checked) accentColor else TextTertiary,
                        modifier = Modifier.size(20.dp),
                    )
                } else if (imageVector != null) {
                    Icon(
                        imageVector = imageVector,
                        contentDescription = null,
                        tint = if (enabled && checked) accentColor else TextTertiary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = if (enabled) TextPrimary else TextTertiary,
                )
                Text(
                    text = desc,
                    fontSize = 11.sp,
                    color = TextSecondary,
                    lineHeight = 15.sp,
                )
            }
        }

        Spacer(Modifier.width(8.dp))

        Switch(
            checked = checked,
            enabled = enabled,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = accentColor,
                uncheckedThumbColor = TextTertiary,
                uncheckedTrackColor = CosmicDark,
                disabledCheckedTrackColor = GlassBorder,
                disabledUncheckedTrackColor = CosmicDark.copy(alpha = 0.3f),
            ),
        )
    }
}

@Composable
private fun NotificationMetaPill(
    painter: Painter? = null,
    imageVector: ImageVector? = null,
    label: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(CosmicDark)
            .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
            .padding(10.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (painter != null) {
                    Icon(
                        painter = painter,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(14.dp),
                    )
                } else if (imageVector != null) {
                    Icon(
                        imageVector = imageVector,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(14.dp),
                    )
                }
                Text(
                    text = label,
                    fontSize = 10.sp,
                    color = TextTertiary,
                    fontWeight = FontWeight.Medium,
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
}
