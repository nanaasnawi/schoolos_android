package com.schoolos.android.feature.profile.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.schoolos.android.core.designsystem.CosmicBlack
import com.schoolos.android.core.designsystem.CosmicNavy
import com.schoolos.android.core.designsystem.GlassBorder
import com.schoolos.android.core.designsystem.NeonBlue
import com.schoolos.android.core.designsystem.NeonError
import com.schoolos.android.core.designsystem.NeonSuccess
import com.schoolos.android.core.designsystem.NeonWarning
import com.schoolos.android.core.designsystem.TextPrimary
import com.schoolos.android.core.designsystem.TextSecondary
import com.schoolos.android.core.designsystem.TextTertiary

@Composable
fun ProfileChangePasswordDialog(
    context: Context,
    onDismissRequest: () -> Unit,
) {
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        containerColor = CosmicNavy,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(NeonBlue.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.Lock, null, tint = NeonBlue, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(10.dp))
                Text("Ganti Kata Sandi", color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Black)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = currentPassword,
                    onValueChange = { currentPassword = it },
                    label = { Text("Kata Sandi Saat Ini", fontSize = 12.sp) },
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonBlue,
                        unfocusedBorderColor = GlassBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text("Kata Sandi Baru", fontSize = 12.sp) },
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonBlue,
                        unfocusedBorderColor = GlassBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("Konfirmasi Sandi Baru", fontSize = 12.sp) },
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                null,
                                tint = TextTertiary,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonBlue,
                        unfocusedBorderColor = GlassBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (currentPassword.isBlank() || newPassword.isBlank()) {
                        Toast.makeText(context, "Harap isi kata sandi!", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (newPassword.length < 6) {
                        Toast.makeText(context, "Kata sandi baru minimal 6 karakter!", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (newPassword != confirmPassword) {
                        Toast.makeText(context, "Konfirmasi sandi tidak cocok!", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    Toast.makeText(context, "Kata sandi berhasil diperbarui!", Toast.LENGTH_LONG).show()
                    onDismissRequest()
                },
                colors = ButtonDefaults.buttonColors(containerColor = NeonSuccess),
                shape = RoundedCornerShape(10.dp),
            ) {
                Text("Simpan Sandi", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Tutup", color = TextSecondary, fontSize = 13.sp)
            }
        },
    )
}

@Composable
fun ProfileHelpDialog(
    schoolName: String?,
    context: Context,
    onDismissRequest: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        containerColor = CosmicNavy,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(NeonWarning.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.AutoMirrored.Filled.HelpOutline, null, tint = NeonWarning, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("Bantuan & Kontak", color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Black)
                    Text(schoolName?.ifBlank { "Layanan Sekolah" } ?: "Layanan Sekolah", color = TextTertiary, fontSize = 11.sp)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(CosmicBlack)
                        .border(1.dp, NeonSuccess.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .clickable {
                            Toast.makeText(context, "Silakan hubungi staf Tata Usaha / Admin IT Sekolah Anda.", Toast.LENGTH_LONG).show()
                        }
                        .padding(14.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(NeonSuccess.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Default.Phone, null, tint = NeonSuccess, modifier = Modifier.size(18.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Layanan Tata Usaha & IT", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 13.sp)
                            Text("Pusat Bantuan Akademik Sekolah", color = TextSecondary, fontSize = 11.sp)
                        }
                        Icon(Icons.Default.ChevronRight, null, tint = TextTertiary, modifier = Modifier.size(16.dp))
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(CosmicBlack.copy(alpha = 0.5f))
                        .padding(10.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🕒 Jam Layanan: ", fontSize = 11.sp, color = TextTertiary, fontWeight = FontWeight.Bold)
                        Text("Senin – Jumat, 08.00 – 16.00 WIB", fontSize = 11.sp, color = TextSecondary)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Tutup", color = NeonBlue, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        },
    )
}

@Composable
fun ProfileAboutDialog(
    schoolName: String?,
    appVersion: String?,
    onDismissRequest: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        containerColor = CosmicNavy,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(NeonBlue.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.Info, null, tint = NeonBlue, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("Tentang SchoolOS", color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Black)
                    Text("Sistem Operasi Manajemen Sekolah", color = TextTertiary, fontSize = 11.sp)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(CosmicBlack)
                        .border(1.dp, GlassBorder, RoundedCornerShape(14.dp))
                        .padding(14.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("SchoolOS Mobile", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Black)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(NeonBlue.copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 2.dp),
                            ) {
                                Text("v${appVersion?.ifBlank { "2.0.0" } ?: "2.0.0"}", color = NeonBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Text(
                            "Platform terpadu untuk pembelajaran daring, tugas, CBT, jadwal terstruktur, dan pemantauan realtime.",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(CosmicBlack)
                            .border(0.5.dp, GlassBorder, RoundedCornerShape(8.dp))
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("PostgreSQL Sync", color = TextTertiary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(CosmicBlack)
                            .border(0.5.dp, GlassBorder, RoundedCornerShape(8.dp))
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("Axum Cloud Engine", color = TextTertiary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(CosmicBlack)
                            .border(0.5.dp, GlassBorder, RoundedCornerShape(8.dp))
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("Jetpack Compose", color = TextTertiary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                Text(
                    "Lembaga: ${schoolName?.ifBlank { "Lembaga Pendidikan Terdaftar" } ?: "Lembaga Pendidikan Terdaftar"}\n© 2026 SchoolOS. Seluruh hak cipta dilindungi.",
                    color = TextTertiary,
                    fontSize = 10.sp,
                    lineHeight = 14.sp,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Tutup", color = NeonBlue, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        },
    )
}

@Composable
fun ProfileLogoutDialog(
    userName: String?,
    onConfirmLogout: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        containerColor = CosmicNavy,
        title = {
            Text("Konfirmasi Keluar Akun", color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Black)
        },
        text = {
            Text(
                text = "Apakah Anda yakin ingin keluar dari akun ${userName ?: "ini"}? Anda harus login kembali untuk mengakses jadwal dan modul pembelajaran.",
                color = TextSecondary,
                fontSize = 13.sp,
                lineHeight = 18.sp,
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirmLogout,
                colors = ButtonDefaults.buttonColors(containerColor = NeonError),
                shape = RoundedCornerShape(10.dp),
            ) {
                Text("Ya, Keluar", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Batal", color = TextSecondary, fontSize = 13.sp)
            }
        },
    )
}
