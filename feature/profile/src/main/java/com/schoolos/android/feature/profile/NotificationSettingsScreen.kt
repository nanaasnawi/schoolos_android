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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.schoolos.android.core.designsystem.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(
    onBack: () -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()

    var masterPush        by remember { mutableStateOf(true) }
    var assignmentReminders by remember { mutableStateOf(true) }
    var announcementAlerts  by remember { mutableStateOf(true) }
    var scheduleReminders   by remember { mutableStateOf(true) }
    var gradeAlerts         by remember { mutableStateOf(true) }
    var soundEnabled        by remember { mutableStateOf(true) }
    var vibrateEnabled      by remember { mutableStateOf(true) }
    var quietHours          by remember { mutableStateOf(false) }

    // How many categories active
    val activeCount = listOf(assignmentReminders, announcementAlerts, scheduleReminders, gradeAlerts)
        .count { it && masterPush }

    Scaffold(
        containerColor = CosmicBlack,
        topBar = {
            ExecutiveTopBar(
                title = "Notifikasi Akademik",
                subtitle = "Pengingat tugas, jadwal & pembaruan nilai",
                onBack = onBack,
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            // ── 1. HERO BANNER ───────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(10.dp, RoundedCornerShape(24.dp), spotColor = Color(0x306366F1))
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF312E81), Color(0xFF4F46E5), Color(0xFF7C3AED))
                        )
                    )
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(13.dp))
                                    .background(Color.White.copy(alpha = 0.18f))
                                    .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(13.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Notifications, null, tint = Color.White, modifier = Modifier.size(24.dp))
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    "PUSAT PENGINGAT",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White.copy(alpha = 0.75f),
                                    letterSpacing = 1.2.sp
                                )
                                Text(
                                    "Pengingat Tepat Waktu",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                            }
                        }
                        val statusBg by animateColorAsState(
                            targetValue = if (masterPush) Color.White.copy(alpha = 0.20f) else Color(0xFFDC2626).copy(alpha = 0.30f),
                            animationSpec = tween(300), label = "badge"
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(statusBg)
                                .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(20.dp))
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                if (masterPush) "● AKTIF" else "○ MATI",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                    // Stats row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        NotifStatPill(
                            label = "$activeCount aktif",
                            icon = "🔔",
                            modifier = Modifier.weight(1f)
                        )
                        NotifStatPill(
                            label = if (quietHours) "Jam Tenang ON" else "Jam Tenang OFF",
                            icon = "🌙",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Text(
                        "${state.schoolName.ifBlank { "Sistem" }} mengirimkan pemberitahuan real-time untuk tugas, kuis CBT, dan pembaruan nilai akademik.",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.82f),
                        lineHeight = 17.sp
                    )
                }
            }

            // ── 2. MASTER TOGGLE ─────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, RoundedCornerShape(20.dp), spotColor = GlassOverlay)
                    .clip(RoundedCornerShape(20.dp))
                    .background(CosmicNavy)
                    .border(
                        1.5.dp,
                        if (masterPush) StudentNeon.copy(alpha = 0.35f) else GlassBorder,
                        RoundedCornerShape(20.dp)
                    )
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (masterPush) StudentNeon.copy(alpha = 0.12f) else GlassBorder.copy(alpha = 0.08f))
                                .border(
                                    1.dp,
                                    if (masterPush) StudentNeon.copy(alpha = 0.25f) else GlassBorder,
                                    RoundedCornerShape(12.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                if (masterPush) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                                null,
                                tint = if (masterPush) StudentNeon else TextTertiary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                "Izinkan Semua Notifikasi",
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp,
                                color = TextPrimary
                            )
                            Text(
                                "Terima pemberitahuan push di perangkat ini",
                                fontSize = 11.sp,
                                color = TextTertiary
                            )
                        }
                    }
                    Switch(
                        checked = masterPush,
                        onCheckedChange = {
                            masterPush = it
                            Toast.makeText(context, if (it) "✅ Notifikasi diaktifkan" else "🔕 Notifikasi dimatikan", Toast.LENGTH_SHORT).show()
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = StudentNeon,
                            uncheckedThumbColor = TextTertiary,
                            uncheckedTrackColor = CosmicDark
                        )
                    )
                }
            }

            // ── 3. ACADEMIC CATEGORIES ───────────────────────────────
            PremiumSectionCard(title = "KATEGORI AKADEMIK") {
                NotifToggleRow(
                    emoji = "📝",
                    accentColor = NeonWarning,
                    title = "Pengingat Tugas & Deadline",
                    desc = "Pemberitahuan H-1 sebelum tenggat dan tugas baru",
                    checked = assignmentReminders && masterPush,
                    enabled = masterPush,
                    onCheckedChange = { assignmentReminders = it }
                )
                NotifDivider()
                NotifToggleRow(
                    emoji = "📢",
                    accentColor = NeonError,
                    title = "Pengumuman & Siaran Sekolah",
                    desc = "Surat edaran, libur sekolah, dan berita penting",
                    checked = announcementAlerts && masterPush,
                    enabled = masterPush,
                    onCheckedChange = { announcementAlerts = it }
                )
                NotifDivider()
                NotifToggleRow(
                    emoji = "📅",
                    accentColor = NeonBlue,
                    title = "Jadwal & Agenda Kelas",
                    desc = "Peringatan sesi belajar pagi dan agenda hari ini",
                    checked = scheduleReminders && masterPush,
                    enabled = masterPush,
                    onCheckedChange = { scheduleReminders = it }
                )
                NotifDivider()
                NotifToggleRow(
                    emoji = "📊",
                    accentColor = NeonSuccess,
                    title = "Pembaruan Nilai & Rapor",
                    desc = "Saat guru mengunggah nilai tugas atau kuis Anda",
                    checked = gradeAlerts && masterPush,
                    enabled = masterPush,
                    onCheckedChange = { gradeAlerts = it }
                )
            }

            // ── 4. SOUND & VIBRATION ─────────────────────────────────
            PremiumSectionCard(title = "SUARA & PREFERENSI") {
                NotifToggleRow(
                    emoji = "🔊",
                    accentColor = StudentNeon,
                    title = "Suara Notifikasi",
                    desc = "Bunyikan nada saat ada pesan atau pengingat masuk",
                    checked = soundEnabled && masterPush,
                    enabled = masterPush,
                    onCheckedChange = { soundEnabled = it }
                )
                NotifDivider()
                NotifToggleRow(
                    emoji = "📳",
                    accentColor = TeacherNeon,
                    title = "Getaran Perangkat",
                    desc = "Aktifkan getaran untuk pemberitahuan penting",
                    checked = vibrateEnabled && masterPush,
                    enabled = masterPush,
                    onCheckedChange = { vibrateEnabled = it }
                )
                NotifDivider()
                NotifToggleRow(
                    emoji = "🌙",
                    accentColor = Color(0xFF818CF8),
                    title = "Jam Tenang (21.00 – 06.00)",
                    desc = "Senyapkan semua pengingat selama jam istirahat malam",
                    checked = quietHours && masterPush,
                    enabled = masterPush,
                    onCheckedChange = { quietHours = it }
                )
            }

            Spacer(Modifier.height(30.dp))
        }
    }
}

// ── SHARED COMPONENTS ─────────────────────────────────────────────────────────

@Composable
private fun NotifStatPill(label: String, icon: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.12f))
            .border(1.dp, Color.White.copy(alpha = 0.20f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(icon, fontSize = 13.sp)
            Spacer(Modifier.width(6.dp))
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

@Composable
private fun PremiumSectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(20.dp), spotColor = GlassOverlay)
            .clip(RoundedCornerShape(20.dp))
            .background(CosmicNavy)
            .border(1.dp, GlassBorder, RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = TextTertiary,
                letterSpacing = 1.sp
            )
            content()
        }
    }
}

@Composable
private fun NotifToggleRow(
    emoji: String,
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
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (enabled && checked) accentColor.copy(alpha = 0.12f) else CosmicDark)
                    .border(1.dp, if (enabled && checked) accentColor.copy(alpha = 0.22f) else GlassBorder, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(emoji, fontSize = 17.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = if (enabled) TextPrimary else TextTertiary
                )
                Text(desc, fontSize = 11.sp, color = TextTertiary, lineHeight = 15.sp)
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
            )
        )
    }
}

@Composable
private fun NotifDivider() {
    HorizontalDivider(color = GlassBorder, thickness = 0.5.dp)
}
