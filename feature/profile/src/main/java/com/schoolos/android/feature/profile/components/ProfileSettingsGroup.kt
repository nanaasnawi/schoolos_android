package com.schoolos.android.feature.profile.components

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.schoolos.android.core.designsystem.CosmicNavy
import com.schoolos.android.core.designsystem.GlassBorder
import com.schoolos.android.core.designsystem.GlassBorder2
import com.schoolos.android.core.designsystem.NeonBlue
import com.schoolos.android.core.designsystem.NeonError
import com.schoolos.android.core.designsystem.NeonWarning
import com.schoolos.android.core.designsystem.TextPrimary
import com.schoolos.android.core.designsystem.TextSecondary
import com.schoolos.android.core.designsystem.TextTertiary

@Composable
fun ProfileSettingsGroup(
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onSecurityClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onHelpClick: () -> Unit,
    onAboutClick: () -> Unit,
    onLogoutClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            text = "PENGATURAN & INFORMASI",
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            color = TextTertiary,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(horizontal = 4.dp),
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(CosmicNavy)
                .border(1.dp, GlassBorder, RoundedCornerShape(18.dp)),
        ) {
            Column {
                ProfileSettingRow(
                    icon = Icons.Default.Lock,
                    color = NeonBlue,
                    title = "Keamanan & Kata Sandi",
                    sub = "Perbarui kata sandi akun SchoolOS",
                    onClick = onSecurityClick,
                )

                Box(Modifier.fillMaxWidth().height(1.dp).background(GlassBorder2))

                // Dark/Light Theme Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background((if (isDarkTheme) NeonBlue else NeonWarning).copy(alpha = 0.08f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = if (isDarkTheme) Icons.Default.DarkMode else Icons.Default.LightMode,
                            contentDescription = null,
                            tint = if (isDarkTheme) NeonBlue else NeonWarning,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Tema Tampilan", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                        Text(if (isDarkTheme) "Mode Gelap (Cosmic)" else "Mode Terang", fontSize = 10.sp, color = TextTertiary)
                    }
                    Switch(
                        checked = isDarkTheme,
                        onCheckedChange = { onToggleTheme() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = NeonBlue,
                        ),
                    )
                }

                Box(Modifier.fillMaxWidth().height(1.dp).background(GlassBorder2))

                ProfileSettingRow(
                    icon = Icons.Default.Notifications,
                    color = NeonWarning,
                    title = "Notifikasi Akademik",
                    sub = "Pengingat jadwal, tugas & pengumuman",
                    onClick = onNotificationsClick,
                )

                Box(Modifier.fillMaxWidth().height(1.dp).background(GlassBorder2))

                ProfileSettingRow(
                    icon = Icons.AutoMirrored.Filled.HelpOutline,
                    color = Color(0xFF10B981),
                    title = "Bantuan & Kontak",
                    sub = "Pusat bantuan akademik dan kontak sekolah",
                    onClick = onHelpClick,
                )

                Box(Modifier.fillMaxWidth().height(1.dp).background(GlassBorder2))

                ProfileSettingRow(
                    icon = Icons.Default.Info,
                    color = NeonBlue,
                    title = "Tentang SchoolOS",
                    sub = "Informasi versi aplikasi dan lisensi",
                    onClick = onAboutClick,
                )
            }
        }

        // Logout CTA Button
        Button(
            onClick = onLogoutClick,
            colors = ButtonDefaults.buttonColors(containerColor = NeonError.copy(alpha = 0.12f)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, NeonError.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                .height(48.dp),
        ) {
            Icon(Icons.AutoMirrored.Filled.ExitToApp, null, tint = NeonError, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Keluar dari Akun", color = NeonError, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }
}

@Composable
private fun ProfileSettingRow(
    icon: ImageVector,
    color: Color,
    title: String,
    sub: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(color.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
            Text(sub, fontSize = 10.sp, color = TextTertiary)
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = TextTertiary.copy(alpha = 0.4f),
            modifier = Modifier.size(16.dp),
        )
    }
}
