package com.schoolos.android.feature.profile.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Icon
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
import com.schoolos.android.core.designsystem.TextPrimary
import com.schoolos.android.core.designsystem.TextSecondary
import com.schoolos.android.core.designsystem.TextTertiary

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
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CosmicNavy)
            .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
            .padding(16.dp),
    ) {
        // 1. Nama (Read-Only)
        ProfileInfoRow(
            icon = Icons.Default.Person,
            label = "Nama",
            value = userName.ifBlank { "Pengguna SchoolOS" },
            roleNeon = roleNeon,
            isEditable = false,
            footerNote = "Nama ini diatur oleh administrator sekolah dan tidak dapat diubah.",
        )

        ProfileDivider()

        // 2. Tentang / Status (Editable)
        ProfileInfoRow(
            icon = Icons.Default.Info,
            label = "Tentang",
            value = about.ifBlank { "Ada di SchoolOS" },
            roleNeon = roleNeon,
            isEditable = true,
            onClick = { onEditField("about") },
        )

        ProfileDivider()

        // 3. Pengguna / Username (Editable)
        ProfileInfoRow(
            icon = Icons.Default.AccountCircle,
            label = "Pengguna",
            value = if (username.isNotBlank()) "@$username" else "@user",
            roleNeon = roleNeon,
            isEditable = true,
            onClick = { onEditField("username") },
        )

        ProfileDivider()

        // 4. Email (Editable)
        ProfileInfoRow(
            icon = Icons.Default.Email,
            label = "Email",
            value = email.ifBlank { "Belum diatur" },
            roleNeon = roleNeon,
            isEditable = true,
            onClick = { onEditField("email") },
        )

        ProfileDivider()

        // 5. Telepon (Editable)
        ProfileInfoRow(
            icon = Icons.Default.Phone,
            label = "Telepon",
            value = phone.ifBlank { "Belum diatur" },
            roleNeon = roleNeon,
            isEditable = true,
            onClick = { onEditField("phone") },
        )
    }
}

@Composable
private fun ProfileInfoRow(
    icon: ImageVector,
    label: String,
    value: String,
    roleNeon: Color,
    isEditable: Boolean,
    footerNote: String? = null,
    onClick: () -> Unit = {},
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = isEditable) { onClick() }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = TextTertiary,
            modifier = Modifier.size(22.dp),
        )

        Spacer(Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = 11.sp,
                color = TextTertiary,
                fontWeight = FontWeight.Medium,
            )

            Spacer(Modifier.height(2.dp))

            Text(
                text = value,
                fontSize = 14.sp,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold,
            )

            if (!footerNote.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = footerNote,
                    fontSize = 10.sp,
                    color = TextSecondary,
                    lineHeight = 14.sp,
                )
            }
        }

        Spacer(Modifier.width(8.dp))

        if (isEditable) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "Edit $label",
                tint = roleNeon,
                modifier = Modifier.size(18.dp),
            )
        } else {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Terkunci",
                tint = TextTertiary.copy(alpha = 0.6f),
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun ProfileDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(0.5.dp)
            .background(GlassBorder2),
    )
}
