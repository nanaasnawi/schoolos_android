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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.schoolos.android.core.auth.isParentRole
import com.schoolos.android.core.auth.isTeacherRole
import com.schoolos.android.core.designsystem.CosmicBlack
import com.schoolos.android.core.designsystem.CosmicDark
import com.schoolos.android.core.designsystem.CosmicNavy
import com.schoolos.android.core.designsystem.ExecutiveTopBar
import com.schoolos.android.core.designsystem.GlassBorder
import com.schoolos.android.core.designsystem.LocalIsDarkTheme
import com.schoolos.android.core.designsystem.NeonBlue
import com.schoolos.android.core.designsystem.NeonError
import com.schoolos.android.core.designsystem.NeonSuccess
import com.schoolos.android.core.designsystem.NeonWarning
import com.schoolos.android.core.designsystem.ParentNeon
import com.schoolos.android.core.designsystem.StudentNeon
import com.schoolos.android.core.designsystem.TeacherNeon
import com.schoolos.android.core.designsystem.TextPrimary
import com.schoolos.android.core.designsystem.TextSecondary
import com.schoolos.android.core.designsystem.TextTertiary
import kotlinx.coroutines.launch

@Composable
fun SecuritySettingsScreen(
    onBack: () -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val state by viewModel.state.collectAsState()
    val isDark = LocalIsDarkTheme.current

    val isTeacher = isTeacherRole(state.user?.role)
    val isParent = isParentRole(state.user?.role)
    val roleNeon = when {
        isTeacher -> TeacherNeon
        isParent -> ParentNeon
        else -> StudentNeon
    }
    val roleLabel = when {
        isTeacher -> "Guru Pengampu"
        isParent -> "Wali Murid"
        else -> "Siswa Aktif"
    }

    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showCurrentPassword by remember { mutableStateOf(false) }
    var showNewPassword by remember { mutableStateOf(false) }
    var showConfirmPassword by remember { mutableStateOf(false) }
    var isBiometricEnabled by remember { mutableStateOf(true) }
    var isSubmitting by remember { mutableStateOf(false) }

    // Password strength computation
    val hasMinLength = newPassword.length >= 8
    val hasNumber = newPassword.any { it.isDigit() }
    val hasUpper = newPassword.any { it.isUpperCase() }
    val hasSpecial = newPassword.any { !it.isLetterOrDigit() }
    val strengthScore = listOf(hasMinLength, hasNumber, hasUpper, hasSpecial).count { it }
    val strengthLabel = when (strengthScore) {
        4 -> "Sangat Kuat"
        3 -> "Kuat"
        2 -> "Sedang"
        1 -> "Lemah"
        else -> "Belum Memenuhi"
    }
    val strengthColor = when (strengthScore) {
        4, 3 -> NeonSuccess
        2 -> NeonWarning
        else -> NeonError
    }

    Scaffold(
        containerColor = CosmicBlack,
        topBar = {
            ExecutiveTopBar(
                title = "Keamanan & Kata Sandi",
                subtitle = "Proteksi Akun & Autentikasi",
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
            // ── 1. HERO SECURITY STATUS ──────────────────────────────
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
                                    .background(NeonSuccess.copy(alpha = 0.12f))
                                    .border(1.dp, NeonSuccess.copy(alpha = 0.25f), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = NeonSuccess,
                                    modifier = Modifier.size(22.dp),
                                )
                            }
                            Column {
                                Text(
                                    text = "STATUS SISTEM KEAMANAN",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextTertiary,
                                    letterSpacing = 0.8.sp,
                                )
                                Text(
                                    text = "Akun Terproteksi Penuh",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(NeonSuccess.copy(alpha = 0.14f))
                                .border(1.dp, NeonSuccess.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                        ) {
                            Text(
                                text = "● AKTIF",
                                color = NeonSuccess,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp,
                            )
                        }
                    }

                    Text(
                        text = "Akses Anda diamankan dengan autentikasi berbasis token JWT terenkripsi dan protokol transmisi HTTPS/TLS.",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        lineHeight = 18.sp,
                    )

                    HorizontalDivider(color = GlassBorder, thickness = 0.5.dp)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        SecurityMetaPill(
                            icon = Icons.Default.Lock,
                            label = "Enkripsi",
                            value = "TLS 1.3 / AES",
                            accentColor = NeonBlue,
                            modifier = Modifier.weight(1f),
                        )
                        SecurityMetaPill(
                            icon = Icons.Default.Key,
                            label = "Peran Akun",
                            value = roleLabel,
                            accentColor = roleNeon,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            // ── 2. UBAH KATA SANDI CARD ──────────────────────────────
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
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(NeonBlue.copy(alpha = 0.12f))
                                .border(1.dp, NeonBlue.copy(alpha = 0.25f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = null,
                                tint = NeonBlue,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        Column {
                            Text(
                                text = "Perbarui Kata Sandi",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                            )
                            Text(
                                text = "Gunakan kombinasi sandi yang aman dan tidak mudah ditebak",
                                fontSize = 11.sp,
                                color = TextSecondary,
                            )
                        }
                    }

                    HorizontalDivider(color = GlassBorder, thickness = 0.5.dp)

                    // Current password
                    SecurityInputField(
                        value = currentPassword,
                        onValueChange = { currentPassword = it },
                        label = "Kata Sandi Saat Ini",
                        showPassword = showCurrentPassword,
                        onToggleVisibility = { showCurrentPassword = !showCurrentPassword },
                    )

                    // New password
                    SecurityInputField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = "Kata Sandi Baru",
                        showPassword = showNewPassword,
                        onToggleVisibility = { showNewPassword = !showNewPassword },
                    )

                    // Password strength indicator
                    if (newPassword.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    text = "Kekuatan Kata Sandi",
                                    fontSize = 11.sp,
                                    color = TextSecondary,
                                )
                                Text(
                                    text = strengthLabel,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = strengthColor,
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                repeat(4) { index ->
                                    val barColor = if (index < strengthScore) strengthColor else GlassBorder
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(4.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(barColor),
                                    )
                                }
                            }
                            Column(
                                modifier = Modifier.padding(top = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                SecurityRequirementRow(label = "Minimal 8 karakter", satisfied = hasMinLength)
                                SecurityRequirementRow(label = "Mengandung angka (0-9)", satisfied = hasNumber)
                                SecurityRequirementRow(label = "Mengandung huruf kapital", satisfied = hasUpper)
                                SecurityRequirementRow(label = "Mengandung simbol (!@#$)", satisfied = hasSpecial)
                            }
                        }
                    }

                    // Confirm new password
                    SecurityInputField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = "Konfirmasi Kata Sandi Baru",
                        showPassword = showConfirmPassword,
                        onToggleVisibility = { showConfirmPassword = !showConfirmPassword },
                        isError = confirmPassword.isNotEmpty() && confirmPassword != newPassword,
                        errorMessage = if (confirmPassword.isNotEmpty() && confirmPassword != newPassword) "Kata sandi konfirmasi tidak cocok" else null,
                    )

                    // Submit button
                    val isFormValid = currentPassword.isNotBlank() &&
                        newPassword.length >= 8 &&
                        newPassword == confirmPassword &&
                        !isSubmitting

                    Button(
                        onClick = {
                            scope.launch {
                                isSubmitting = true
                                val res = viewModel.changePassword(currentPassword, newPassword)
                                isSubmitting = false
                                if (res.isSuccess) {
                                    Toast.makeText(context, "Kata sandi berhasil diperbarui!", Toast.LENGTH_SHORT).show()
                                    currentPassword = ""
                                    newPassword = ""
                                    confirmPassword = ""
                                } else {
                                    Toast.makeText(
                                        context,
                                        res.exceptionOrNull()?.message ?: "Gagal memperbarui kata sandi",
                                        Toast.LENGTH_LONG,
                                    ).show()
                                }
                            }
                        },
                        enabled = isFormValid,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonBlue,
                            disabledContainerColor = GlassBorder,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Simpan Kata Sandi Baru",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }

            // ── 3. AUTENTIKASI & PERANGKAT ───────────────────────────
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
                        text = "AUTENTIKASI & SESI PERANGKAT",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextTertiary,
                        letterSpacing = 0.8.sp,
                    )

                    // Biometric toggle
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
                                    .background(NeonSuccess.copy(alpha = 0.12f))
                                    .border(1.dp, NeonSuccess.copy(alpha = 0.25f), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Fingerprint,
                                    contentDescription = null,
                                    tint = NeonSuccess,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                            Column {
                                Text(
                                    text = "Masuk Cepat Biometrik",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary,
                                )
                                Text(
                                    text = "Sidik jari & pemindai wajah perangkat",
                                    fontSize = 11.sp,
                                    color = TextSecondary,
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
                                uncheckedTrackColor = CosmicDark,
                            ),
                        )
                    }

                    HorizontalDivider(color = GlassBorder, thickness = 0.5.dp)

                    // Active device row
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
                                    .background(roleNeon.copy(alpha = 0.12f))
                                    .border(1.dp, roleNeon.copy(alpha = 0.25f), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Smartphone,
                                    contentDescription = null,
                                    tint = roleNeon,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                            Column {
                                Text(
                                    text = "Perangkat Ini (Aktif)",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary,
                                )
                                Text(
                                    text = "Android • Sesi terotentikasi & aman",
                                    fontSize = 11.sp,
                                    color = TextSecondary,
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(NeonSuccess.copy(alpha = 0.14f))
                                .padding(horizontal = 7.dp, vertical = 3.dp),
                        ) {
                            Text(
                                text = "SESI AKTIF",
                                color = NeonSuccess,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }

                    HorizontalDivider(color = GlassBorder, thickness = 0.5.dp)

                    // Revoke sessions button
                    OutlinedButton(
                        onClick = {
                            Toast.makeText(context, "Sesi perangkat lain berhasil dicabut", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NeonError.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = null,
                            tint = NeonError,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Keluar dari Sesi Perangkat Lain",
                            color = NeonError,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SecurityMetaPill(
    icon: ImageVector,
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
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(14.dp),
                )
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

@Composable
private fun SecurityRequirementRow(
    label: String,
    satisfied: Boolean,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            imageVector = if (satisfied) Icons.Default.Check else Icons.Default.Close,
            contentDescription = null,
            tint = if (satisfied) NeonSuccess else TextTertiary,
            modifier = Modifier.size(13.dp),
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = if (satisfied) TextPrimary else TextTertiary,
            fontWeight = if (satisfied) FontWeight.Medium else FontWeight.Normal,
        )
    }
}

@Composable
private fun SecurityInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    showPassword: Boolean,
    onToggleVisibility: () -> Unit,
    isError: Boolean = false,
    errorMessage: String? = null,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label, fontSize = 12.sp) },
            singleLine = true,
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                IconButton(onClick = onToggleVisibility) {
                    Icon(
                        imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = null,
                        tint = TextTertiary,
                        modifier = Modifier.size(18.dp),
                    )
                }
            },
            isError = isError,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = NeonBlue,
                unfocusedBorderColor = GlassBorder,
                errorBorderColor = NeonError,
                focusedContainerColor = CosmicDark,
                unfocusedContainerColor = CosmicDark,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                focusedLabelColor = NeonBlue,
                unfocusedLabelColor = TextTertiary,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        if (errorMessage != null) {
            Text(
                text = errorMessage,
                color = NeonError,
                fontSize = 10.sp,
                modifier = Modifier.padding(start = 4.dp),
            )
        }
    }
}
