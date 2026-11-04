package com.schoolos.android.feature.profile.components

import androidx.annotation.DrawableRes
import androidx.compose.ui.res.painterResource
import com.schoolos.android.core.R as CoreR
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.schoolos.android.core.designsystem.*

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
    val isDark = LocalIsDarkTheme.current
    val cardBg = if (isDark) Color(0xFF111113) else Color.White
    val cardBorder = if (isDark) Color(0xFF1F1F23) else Color(0xFFE2E8F0)
    val textPrimary = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFFA1A1AA) else Color(0xFF64748B)
    val textTertiary = if (isDark) Color(0xFF71717A) else Color(0xFF94A3B8)
    val innerCardBorder = if (isDark) Color(0xFF27272A) else Color(0xFFE2E8F0)

    val neonBlue = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)
    val neonAmber = if (isDark) Color(0xFFF59E0B) else Color(0xFFD97706)
    val neonSuccess = if (isDark) Color(0xFF10B981) else Color(0xFF059669)
    val neonError = if (isDark) Color(0xFFEF4444) else Color(0xFFDC2626)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            text = "PENGATURAN & INFORMASI",
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            color = textTertiary,
            letterSpacing = 1.2.sp,
            modifier = Modifier.padding(horizontal = 4.dp),
        )

        Surface(
            shape = RoundedCornerShape(22.dp),
            color = cardBg,
            border = BorderStroke(1.dp, cardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                ProfileSettingRow(
                    iconRes = CoreR.drawable.ic_modern_lock,
                    color = neonBlue,
                    title = "Keamanan & Kata Sandi",
                    sub = "Perbarui kata sandi akun Akselerasi-Edu",
                    onClick = onSecurityClick,
                )

                HorizontalDivider(color = innerCardBorder, thickness = 0.5.dp, modifier = Modifier.fillMaxWidth())

                // Dark/Light Theme Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onToggleTheme)
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(11.dp))
                            .background(neonAmber.copy(alpha = if (isDark) 0.15f else 0.10f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = if (isDarkTheme) Icons.Default.DarkMode else Icons.Default.LightMode,
                            contentDescription = null,
                            tint = neonAmber,
                            modifier = Modifier.size(20.dp),
                        )
                    }

                    Spacer(Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isDarkTheme) "Mode Gelap (Aktif)" else "Mode Terang (Aktif)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = textPrimary,
                        )
                        Text(
                            text = if (isDarkTheme) "Tampilan gelap nyaman untuk mata" else "Tampilan terang dengan kontras jernih",
                            fontSize = 11.5.sp,
                            color = textSecondary,
                        )
                    }

                    Switch(
                        checked = isDarkTheme,
                        onCheckedChange = { onToggleTheme() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = neonAmber,
                            uncheckedThumbColor = textTertiary,
                            uncheckedTrackColor = if (isDark) Color(0xFF27272A) else Color(0xFFE2E8F0),
                        ),
                    )
                }

                HorizontalDivider(color = innerCardBorder, thickness = 0.5.dp, modifier = Modifier.fillMaxWidth())

                ProfileSettingRow(
                    iconRes = CoreR.drawable.ic_modern_bell,
                    color = neonBlue,
                    title = "Preferensi Notifikasi",
                    sub = "Atur peringatan jadwal, tugas & materi",
                    onClick = onNotificationsClick,
                )

                HorizontalDivider(color = innerCardBorder, thickness = 0.5.dp, modifier = Modifier.fillMaxWidth())

                ProfileSettingRow(
                    iconRes = CoreR.drawable.ic_modern_chat,
                    color = neonSuccess,
                    title = "Pusat Bantuan & Kontak",
                    sub = "Kontak resmi admin & layanan sekolah",
                    onClick = onHelpClick,
                )

                HorizontalDivider(color = innerCardBorder, thickness = 0.5.dp, modifier = Modifier.fillMaxWidth())

                ProfileSettingRow(
                    iconRes = CoreR.drawable.ic_modern_book,
                    color = neonBlue,
                    title = "Tentang Akselerasi-Edu",
                    sub = "Versi aplikasi, lisensi & info sistem",
                    onClick = onAboutClick,
                )

                HorizontalDivider(color = innerCardBorder, thickness = 0.5.dp, modifier = Modifier.fillMaxWidth())

                ProfileSettingRow(
                    iconRes = CoreR.drawable.ic_modern_logout,
                    color = neonError,
                    title = "Keluar dari Akun",
                    sub = "Akhiri sesi login perangkat ini",
                    onClick = onLogoutClick,
                    isDestructive = true,
                )
            }
        }
    }
}

@Composable
private fun ProfileSettingRow(
    @DrawableRes iconRes: Int,
    color: Color,
    title: String,
    sub: String,
    onClick: () -> Unit,
    isDestructive: Boolean = false,
) {
    val isDark = LocalIsDarkTheme.current
    val textPrimary = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFFA1A1AA) else Color(0xFF64748B)
    val textTertiary = if (isDark) Color(0xFF71717A) else Color(0xFF94A3B8)
    val neonError = if (isDark) Color(0xFFEF4444) else Color(0xFFDC2626)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(color.copy(alpha = if (isDark) 0.15f else 0.10f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp),
            )
        }

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isDestructive) neonError else textPrimary,
            )
            Text(
                text = sub,
                fontSize = 11.5.sp,
                color = if (isDestructive) neonError.copy(alpha = 0.8f) else textSecondary,
            )
        }

        Icon(
            painter = painterResource(id = CoreR.drawable.ic_modern_chevron_right),
            contentDescription = null,
            tint = if (isDestructive) neonError.copy(alpha = 0.7f) else textTertiary,
            modifier = Modifier.size(16.dp),
        )
    }
}
