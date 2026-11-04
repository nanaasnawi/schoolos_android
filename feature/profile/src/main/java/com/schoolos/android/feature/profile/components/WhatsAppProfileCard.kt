package com.schoolos.android.feature.profile.components

import com.schoolos.android.core.R as CoreR
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.schoolos.android.core.designsystem.*

@Composable
fun WhatsAppProfileCard(
    userName: String,
    about: String,
    username: String,
    email: String,
    phone: String,
    roleNeon: Color,
    onEditField: (fieldKey: String) -> Unit,
    modifier: Modifier = Modifier,
    isStudent: Boolean = false,
) {
    val isDark = LocalIsDarkTheme.current
    val cardBg = if (isDark) Color(0xFF111113) else Color.White
    val cardBorder = if (isDark) Color(0xFF1F1F23) else Color(0xFFE2E8F0)
    val innerCardBorder = if (isDark) Color(0xFF27272A) else Color(0xFFE2E8F0)

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // 1. Nama (Read-Only)
            ProfileInfoRow(
                iconRes = CoreR.drawable.ic_modern_profile,
                label = "Nama",
                value = userName.ifBlank { "Pengguna Akselerasi-Edu" },
                roleNeon = roleNeon,
                isEditable = false,
                footerNote = "Nama ini sinkron dengan biodata pokok resmi dan tidak dapat diubah.",
            )

            HorizontalDivider(color = innerCardBorder, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 12.dp))

            // 2. Tentang / Status
            ProfileInfoRow(
                iconRes = CoreR.drawable.ic_modern_location,
                label = "Tentang / Domisili",
                value = about.ifBlank { "Ada di Akselerasi-Edu" },
                roleNeon = roleNeon,
                isEditable = !isStudent,
                footerNote = if (isStudent) "Alamat domisili terdaftar pada biodata pokok resmi." else null,
                onClick = { if (!isStudent) onEditField("about") },
            )

            HorizontalDivider(color = innerCardBorder, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 12.dp))

            // 3. Pengguna / Username
            ProfileInfoRow(
                iconRes = CoreR.drawable.ic_modern_profile,
                label = "Pengguna",
                value = if (username.isNotBlank()) "@$username" else "@user",
                roleNeon = roleNeon,
                isEditable = !isStudent,
                footerNote = if (isStudent) "Username resmi ditetapkan oleh sistem satuan pendidikan." else null,
                onClick = { if (!isStudent) onEditField("username") },
            )

            HorizontalDivider(color = innerCardBorder, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 12.dp))

            // 4. Email
            ProfileInfoRow(
                iconRes = CoreR.drawable.ic_modern_chat,
                label = "Email",
                value = email.ifBlank { "Belum diatur" },
                roleNeon = roleNeon,
                isEditable = true,
                footerNote = "Ketuk untuk memperbarui alamat email aktif Anda.",
                onClick = { onEditField("email") },
            )

            HorizontalDivider(color = innerCardBorder, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 12.dp))

            // 5. Telepon / WhatsApp
            ProfileInfoRow(
                iconRes = CoreR.drawable.ic_modern_attendance,
                label = "Nomor Telepon",
                value = phone.ifBlank { "Belum diatur" },
                roleNeon = roleNeon,
                isEditable = true,
                footerNote = "Ketuk untuk memperbarui nomor telepon / WhatsApp aktif.",
                onClick = { onEditField("phone") },
            )
        }
    }
}

@Composable
private fun ProfileInfoRow(
    iconRes: Int,
    label: String,
    value: String,
    roleNeon: Color,
    isEditable: Boolean,
    footerNote: String? = null,
    onClick: (() -> Unit)? = null,
) {
    val isDark = LocalIsDarkTheme.current
    val textPrimary = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFFA1A1AA) else Color(0xFF64748B)
    val textTertiary = if (isDark) Color(0xFF71717A) else Color(0xFF94A3B8)

    val rowModifier = if (isEditable && onClick != null) {
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp)
    } else {
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    }

    Row(
        modifier = rowModifier,
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            tint = textTertiary,
            modifier = Modifier
                .size(20.dp)
                .padding(top = 2.dp),
        )

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = textTertiary,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = textPrimary,
            )
            if (!footerNote.isNullOrBlank()) {
                Spacer(Modifier.height(3.dp))
                Text(
                    text = footerNote,
                    fontSize = 11.sp,
                    color = textSecondary,
                    lineHeight = 15.sp,
                )
            }
        }

        if (isEditable) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "Ubah $label",
                tint = roleNeon,
                modifier = Modifier
                    .size(16.dp)
                    .padding(top = 4.dp),
            )
        } else {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Terkunci",
                tint = textTertiary.copy(alpha = 0.6f),
                modifier = Modifier
                    .size(15.dp)
                    .padding(top = 4.dp),
            )
        }
    }
}
