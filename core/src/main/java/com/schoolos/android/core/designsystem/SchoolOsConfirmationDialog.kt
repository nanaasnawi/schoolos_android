package com.schoolos.android.core.designsystem

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * Custom SchoolOS Executive Confirmation Dialog.
 * Premium rounded card, custom accent iconography, and adaptive dark/light surfaces.
 */
@Composable
fun SchoolOsConfirmationDialog(
    title: String,
    message: String,
    confirmText: String = "Ya, Lanjutkan",
    dismissText: String = "Batal",
    icon: ImageVector = Icons.Outlined.WarningAmber,
    accentColor: Color = Color(0xFFEF4444),
    confirmButtonColor: Color = accentColor,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val isDark = LocalIsDarkTheme.current
    val dialogBg = if (isDark) Color(0xFF141417) else Color.White
    val dialogBorder = if (isDark) Color(0xFF26262B) else Color(0xFFE2E8F0)
    val textPrimary = if (isDark) Color(0xFFF1F5F9) else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    val cancelBg = if (isDark) Color(0xFF1C1C20) else Color(0xFFF1F5F9)
    val cancelBorder = if (isDark) Color(0xFF2E2E33) else Color(0xFFE2E8F0)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = dialogBg,
            border = BorderStroke(1.dp, dialogBorder),
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Icon Badge
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = if (isDark) 0.18f else 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(Modifier.height(16.dp))

                // Title
                Text(
                    text = title,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(8.dp))

                // Message
                Text(
                    text = message,
                    fontSize = 13.sp,
                    color = textSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 19.sp
                )

                Spacer(Modifier.height(22.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Cancel / Dismiss
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, cancelBorder),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = cancelBg,
                            contentColor = textSecondary
                        )
                    ) {
                        Text(
                            text = dismissText,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Confirm
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = confirmButtonColor,
                            contentColor = Color.White
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                    ) {
                        Text(
                            text = confirmText,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

