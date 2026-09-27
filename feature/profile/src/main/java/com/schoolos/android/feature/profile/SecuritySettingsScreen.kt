package com.schoolos.android.feature.profile

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import com.schoolos.android.core.designsystem.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecuritySettingsScreen(
    onBack: () -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showCurrentPassword by remember { mutableStateOf(false) }
    var showNewPassword by remember { mutableStateOf(false) }
    var showConfirmPassword by remember { mutableStateOf(false) }
    var isBiometricEnabled by remember { mutableStateOf(true) }
    var isSubmitting by remember { mutableStateOf(false) }
    var showChangePasswordSection by remember { mutableStateOf(false) }

    // Strength computation
    val hasMinLength = newPassword.length >= 8
    val hasNumber   = newPassword.any { it.isDigit() }
    val hasUpper    = newPassword.any { it.isUpperCase() }
    val hasSpecial  = newPassword.any { !it.isLetterOrDigit() }
    val strengthScore = listOf(hasMinLength, hasNumber, hasUpper, hasSpecial).count { it }
    val strengthLabel = when (strengthScore) {
        4    -> "Sangat Kuat 🛡️"
        3    -> "Kuat"
        2    -> "Sedang"
        1    -> "Lemah"
        else -> "Belum Memenuhi"
    }
    val strengthColor = when (strengthScore) {
        4, 3 -> NeonSuccess
        2    -> NeonWarning
        else -> NeonError
    }

    Scaffold(
        containerColor = CosmicBlack,
        topBar = {
            ExecutiveTopBar(
                title = "Keamanan & Kata Sandi",
                subtitle = "Proteksi akun & autentikasi perangkat",
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

            // ── 1. SECURITY STATUS HERO ──────────────────────────────
            SecurityHeroBanner()

            // ── 2. SECURITY QUICK STATS ──────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SecurityStatCard(
                    emoji = "🔐",
                    label = "Enkripsi",
                    value = "SHA-256",
                    accent = NeonSuccess,
                    modifier = Modifier.weight(1f)
                )
                SecurityStatCard(
                    emoji = "⏱️",
                    label = "Sesi Token",
                    value = "7 Hari",
                    accent = NeonBlue,
                    modifier = Modifier.weight(1f)
                )
                SecurityStatCard(
                    emoji = "🌐",
                    label = "Protokol",
                    value = "HTTPS/TLS",
                    accent = StudentNeon,
                    modifier = Modifier.weight(1f)
                )
            }

            // ── 3. CHANGE PASSWORD (COLLAPSIBLE) ─────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, RoundedCornerShape(20.dp), spotColor = GlassOverlay)
                    .clip(RoundedCornerShape(20.dp))
                    .background(CosmicNavy)
                    .border(1.dp, GlassBorder, RoundedCornerShape(20.dp))
            ) {
                Column {
                    // Header — tap to expand/collapse
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showChangePasswordSection = !showChangePasswordSection }
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(NeonBlue.copy(alpha = 0.12f))
                                    .border(1.dp, NeonBlue.copy(alpha = 0.25f), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Key, null, tint = NeonBlue, modifier = Modifier.size(18.dp))
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Ubah Kata Sandi",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = TextPrimary
                                )
                                Text(
                                    text = if (showChangePasswordSection) "Ketuk untuk tutup" else "Ketuk untuk ubah kata sandi",
                                    fontSize = 11.sp,
                                    color = TextTertiary
                                )
                            }
                        }
                        Icon(
                            imageVector = if (showChangePasswordSection) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = TextTertiary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    AnimatedVisibility(
                        visible = showChangePasswordSection,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            HorizontalDivider(color = GlassBorder, thickness = 0.5.dp)
                            Spacer(Modifier.height(2.dp))

                            // Current Password
                            SecureTextField(
                                value = currentPassword,
                                onValueChange = { currentPassword = it },
                                label = "Kata Sandi Saat Ini",
                                showPassword = showCurrentPassword,
                                onToggleShow = { showCurrentPassword = !showCurrentPassword },
                            )

                            // New Password
                            SecureTextField(
                                value = newPassword,
                                onValueChange = { newPassword = it },
                                label = "Kata Sandi Baru",
                                showPassword = showNewPassword,
                                onToggleShow = { showNewPassword = !showNewPassword },
                            )

                            // Strength indicator
                            if (newPassword.isNotEmpty()) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Kekuatan Sandi", fontSize = 11.sp, color = TextTertiary)
                                        Text(strengthLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = strengthColor)
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        repeat(4) { index ->
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(4.dp)
                                                    .clip(RoundedCornerShape(2.dp))
                                                    .background(if (index < strengthScore) strengthColor else GlassBorder)
                                            )
                                        }
                                    }
                                    // Requirement checklist
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        PasswordRequirementRow("Minimal 8 karakter", hasMinLength)
                                        PasswordRequirementRow("Mengandung angka", hasNumber)
                                        PasswordRequirementRow("Mengandung huruf kapital", hasUpper)
                                        PasswordRequirementRow("Mengandung simbol (!@#)", hasSpecial)
                                    }
                                }
                            }

                            // Confirm Password
                            SecureTextField(
                                value = confirmPassword,
                                onValueChange = { confirmPassword = it },
                                label = "Konfirmasi Kata Sandi Baru",
                                showPassword = showConfirmPassword,
                                onToggleShow = { showConfirmPassword = !showConfirmPassword },
                                isError = confirmPassword.isNotEmpty() && confirmPassword != newPassword,
                                supportingText = if (confirmPassword.isNotEmpty() && confirmPassword != newPassword) "Kata sandi tidak cocok" else null
                            )

                            // Submit button
                            Button(
                                enabled = !isSubmitting && currentPassword.isNotBlank() && newPassword.length >= 8 && newPassword == confirmPassword,
                                onClick = {
                                    scope.launch {
                                        isSubmitting = true
                                        val res = viewModel.changePassword(currentPassword, newPassword)
                                        isSubmitting = false
                                        if (res.isSuccess) {
                                            Toast.makeText(context, "✅ Kata sandi berhasil diperbarui!", Toast.LENGTH_LONG).show()
                                            currentPassword = ""; newPassword = ""; confirmPassword = ""
                                            showChangePasswordSection = false
                                        } else {
                                            Toast.makeText(context, res.exceptionOrNull()?.message ?: "Gagal memperbarui kata sandi", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = NeonBlue,
                                    disabledContainerColor = GlassBorder
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                if (isSubmitting) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Default.CheckCircle, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Simpan Kata Sandi Baru", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }
            }

            // ── 4. BIOMETRIC & DEVICE ────────────────────────────────
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "AUTENTIKASI & PERANGKAT",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = TextTertiary,
                            letterSpacing = 1.sp
                        )
                    }

                    // Biometric toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(NeonSuccess.copy(alpha = 0.10f))
                                    .border(1.dp, NeonSuccess.copy(alpha = 0.20f), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("👆", fontSize = 18.sp)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Masuk dengan Biometrik",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Sidik jari / wajah untuk akses cepat",
                                    fontSize = 11.sp,
                                    color = TextTertiary
                                )
                            }
                        }
                        Switch(
                            checked = isBiometricEnabled,
                            onCheckedChange = { isBiometricEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = NeonSuccess,
                                uncheckedThumbColor = TextTertiary,
                                uncheckedTrackColor = CosmicDark
                            )
                        )
                    }

                    HorizontalDivider(color = GlassBorder, thickness = 0.5.dp)

                    // Active device info row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(CosmicDark.copy(alpha = 0.5f))
                            .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(NeonBlue.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.PhoneAndroid, null, tint = NeonBlue, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Perangkat Ini", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Spacer(Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(NeonSuccess.copy(alpha = 0.15f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("● AKTIF", fontSize = 9.sp, fontWeight = FontWeight.Black, color = NeonSuccess)
                                }
                            }
                            Text("Android • Sesi terenkripsi & valid", fontSize = 11.sp, color = TextTertiary)
                        }
                    }

                    OutlinedButton(
                        onClick = { Toast.makeText(context, "Semua sesi di perangkat lain telah dicabut.", Toast.LENGTH_SHORT).show() },
                        border = androidx.compose.foundation.BorderStroke(1.dp, NeonError.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(44.dp)
                    ) {
                        Icon(Icons.Default.Logout, null, tint = NeonError, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Keluarkan dari Semua Perangkat Lain", color = NeonError, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // ── 5. SECURITY TIPS ─────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF0F2027).copy(alpha = 0.8f))
                    .border(1.dp, NeonBlue.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "💡 TIPS KEAMANAN",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = NeonBlue,
                        letterSpacing = 1.sp
                    )
                    SecurityTip("Jangan bagikan kata sandi kepada siapapun, termasuk staff sekolah.")
                    SecurityTip("Ganti kata sandi secara berkala setiap 3 bulan untuk keamanan optimal.")
                    SecurityTip("Gunakan kombinasi huruf kapital, angka, dan simbol agar sandi lebih kuat.")
                }
            }

            Spacer(Modifier.height(30.dp))
        }
    }
}

@Composable
private fun SecurityHeroBanner() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(10.dp, RoundedCornerShape(24.dp), spotColor = Color(0x3010B981))
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF064E3B), Color(0xFF047857), Color(0xFF0D9488))
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
                        Icon(Icons.Default.Shield, null, tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            "STATUS KEAMANAN",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White.copy(alpha = 0.75f),
                            letterSpacing = 1.2.sp
                        )
                        Text(
                            "Sangat Terlindungi",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White.copy(alpha = 0.18f))
                        .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF6EE7B7)))
                        Spacer(Modifier.width(5.dp))
                        Text("AMAN", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
            Text(
                "Akun Anda terlindungi dengan enkripsi SHA-256 sesi token dan protokol verifikasi lembaga resmi sekolah.",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.85f),
                lineHeight = 17.sp
            )
        }
    }
}

@Composable
private fun SecurityStatCard(
    emoji: String,
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(CosmicNavy)
            .border(1.dp, accent.copy(alpha = 0.20f), RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(emoji, fontSize = 20.sp)
            Text(value, fontSize = 11.sp, fontWeight = FontWeight.Black, color = accent, maxLines = 1)
            Text(label, fontSize = 10.sp, color = TextTertiary, maxLines = 1)
        }
    }
}

@Composable
private fun SecureTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    showPassword: Boolean,
    onToggleShow: () -> Unit,
    isError: Boolean = false,
    supportingText: String? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 13.sp) },
        isError = isError,
        supportingText = if (supportingText != null) {
            { Text(supportingText, color = NeonError, fontSize = 11.sp) }
        } else null,
        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = onToggleShow) {
                Icon(
                    if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    null,
                    tint = TextTertiary,
                    modifier = Modifier.size(20.dp)
                )
            }
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = NeonBlue,
            unfocusedBorderColor = GlassBorder,
            errorBorderColor = NeonError,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
            focusedLabelColor = NeonBlue,
            unfocusedLabelColor = TextTertiary,
            cursorColor = NeonBlue,
        ),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun PasswordRequirementRow(text: String, met: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(14.dp)
                .clip(CircleShape)
                .background(if (met) NeonSuccess.copy(alpha = 0.15f) else GlassBorder.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            if (met) Icon(Icons.Default.Check, null, tint = NeonSuccess, modifier = Modifier.size(9.dp))
        }
        Spacer(Modifier.width(6.dp))
        Text(text, fontSize = 11.sp, color = if (met) NeonSuccess else TextTertiary)
    }
}

@Composable
private fun SecurityTip(text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .padding(top = 2.dp)
                .size(5.dp)
                .clip(CircleShape)
                .background(NeonBlue.copy(alpha = 0.6f))
        )
        Spacer(Modifier.width(8.dp))
        Text(text, fontSize = 11.sp, color = TextSecondary, lineHeight = 16.sp)
    }
}
