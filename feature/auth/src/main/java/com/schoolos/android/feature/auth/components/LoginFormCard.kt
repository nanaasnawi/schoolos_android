package com.schoolos.android.feature.auth.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.schoolos.android.core.designsystem.CosmicNavy
import com.schoolos.android.core.designsystem.CosmicSurface
import com.schoolos.android.core.designsystem.GlassBorder
import com.schoolos.android.core.designsystem.TextPrimary
import com.schoolos.android.core.designsystem.TextSecondary
import com.schoolos.android.core.designsystem.TextTertiary
import com.schoolos.android.feature.auth.LoginUiState

@Composable
fun LoginFormCard(
    state: LoginUiState,
    currentTab: RoleTabConfig,
    accentColor: Color,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onRememberMeChange: (Boolean) -> Unit,
    onLoginClick: () -> Unit,
    onOpenQrScanner: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var passwordVisible by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(16.dp, RoundedCornerShape(28.dp), spotColor = accentColor.copy(alpha = 0.16f))
            .clip(RoundedCornerShape(28.dp))
            .background(CosmicNavy.copy(alpha = 0.98f))
            .border(1.2.dp, GlassBorder, RoundedCornerShape(28.dp))
            .padding(horizontal = 20.dp, vertical = 22.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // 1. Card Header with Role Indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "Login",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 22.sp,
                        color = TextPrimary,
                        letterSpacing = (-0.4).sp,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Masuk ke akun pembelajaran Anda",
                        fontSize = 12.sp,
                        color = TextTertiary,
                    )
                }

                // Active Role Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(accentColor.copy(alpha = 0.12f))
                        .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = currentTab.title.uppercase(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor,
                        letterSpacing = 0.5.sp,
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // 2. Username / Identifier Field (Pill Shape)
            OutlinedTextField(
                value = state.username,
                onValueChange = onUsernameChange,
                label = {
                    Text(
                        text = currentTab.inputLabel,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                    )
                },
                placeholder = {
                    Text(
                        text = currentTab.inputPlaceholder,
                        fontSize = 12.sp,
                        color = TextTertiary,
                    )
                },
                leadingIcon = {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = currentTab.icon,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(19.dp),
                        )
                    }
                },
                trailingIcon = {
                    if (state.username.isNotEmpty()) {
                        IconButton(onClick = { onUsernameChange("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Hapus",
                                tint = TextTertiary,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = if (state.selectedRoleTab == 1) KeyboardType.Number else KeyboardType.Text,
                    imeAction = ImeAction.Next,
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = CosmicSurface,
                    unfocusedContainerColor = CosmicSurface,
                    focusedBorderColor = accentColor,
                    unfocusedBorderColor = GlassBorder,
                    focusedLabelColor = accentColor,
                    unfocusedLabelColor = TextTertiary,
                    cursorColor = accentColor,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextSecondary,
                ),
            )

            // Contextual Hint Banner (e.g. 10 digit NISN)
            if (currentTab.hintExample.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(CosmicSurface)
                        .border(0.8.dp, GlassBorder, RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = currentTab.hintBadge,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor,
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "• ${currentTab.hintExample}",
                        fontSize = 10.sp,
                        color = TextTertiary,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // 3. Password Field (Pill Shape)
            OutlinedTextField(
                value = state.password,
                onValueChange = onPasswordChange,
                label = {
                    Text(
                        text = "Kata Sandi",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                    )
                },
                placeholder = {
                    Text(
                        text = "Masukkan kata sandi Anda",
                        fontSize = 12.sp,
                        color = TextTertiary,
                    )
                },
                leadingIcon = {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(19.dp),
                        )
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(20.dp),
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        onLoginClick()
                    },
                ),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (passwordVisible) "Sembunyikan" else "Tampilkan",
                            tint = if (passwordVisible) accentColor else TextTertiary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = CosmicSurface,
                    unfocusedContainerColor = CosmicSurface,
                    focusedBorderColor = accentColor,
                    unfocusedBorderColor = GlassBorder,
                    focusedLabelColor = accentColor,
                    unfocusedLabelColor = TextTertiary,
                    cursorColor = accentColor,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextSecondary,
                ),
            )

            Spacer(Modifier.height(12.dp))

            // 4. Remember Me & Forgot Password Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onRememberMeChange(!state.rememberMe) }
                        .padding(vertical = 4.dp, horizontal = 2.dp),
                ) {
                    Checkbox(
                        checked = state.rememberMe,
                        onCheckedChange = onRememberMeChange,
                        colors = CheckboxDefaults.colors(
                            checkedColor = accentColor,
                            uncheckedColor = TextTertiary,
                        ),
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Ingat Akun",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium,
                    )
                }

                TextButton(
                    onClick = onForgotPasswordClick,
                    modifier = Modifier.height(34.dp),
                ) {
                    Text(
                        text = "Lupa Kata Sandi?",
                        fontSize = 12.sp,
                        color = accentColor,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // 5. Primary Login Action Button ("Login ->")
            Button(
                onClick = {
                    focusManager.clearFocus()
                    onLoginClick()
                },
                enabled = !state.isLoading,
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .shadow(
                        elevation = if (state.isLoading) 0.dp else 12.dp,
                        shape = RoundedCornerShape(26.dp),
                        spotColor = accentColor.copy(alpha = 0.5f),
                    )
                    .clip(RoundedCornerShape(26.dp))
                    .background(
                        if (state.isLoading) {
                            Brush.horizontalGradient(
                                listOf(
                                    accentColor.copy(alpha = 0.6f),
                                    currentTab.secondaryColor.copy(alpha = 0.4f),
                                ),
                            )
                        } else {
                            Brush.horizontalGradient(
                                listOf(
                                    accentColor,
                                    currentTab.secondaryColor,
                                ),
                            )
                        },
                    ),
            ) {
                AnimatedContent(
                    targetState = state.isLoading,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "buttonContent",
                ) { loading ->
                    if (loading) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 2.5.dp,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = "Memverifikasi Kredensial...",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                            )
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Text(
                                text = "Login",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                letterSpacing = 0.4.sp,
                            )
                            Spacer(Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(19.dp),
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            // 6. Divider: "Login With"
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(1.dp)
                        .background(GlassBorder),
                )
                Text(
                    text = "Login With",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextTertiary,
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(1.dp)
                        .background(GlassBorder),
                )
            }

            Spacer(Modifier.height(16.dp))

            // 7. Dedicated QR Code Login Action Button (Replaces Google & Facebook)
            Surface(
                onClick = onOpenQrScanner,
                shape = RoundedCornerShape(24.dp),
                color = CosmicSurface,
                border = androidx.compose.foundation.BorderStroke(1.2.dp, accentColor.copy(alpha = 0.45f)),
                shadowElevation = 3.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(accentColor.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = "Pindai QR",
                                tint = accentColor,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Login dengan QR Code",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                            )
                            Text(
                                text = "Pindai Kartu Siswa / ID Card",
                                fontSize = 10.sp,
                                color = TextTertiary,
                            )
                        }
                    }

                    // Instant Badge Pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(accentColor.copy(alpha = 0.12f))
                            .border(0.8.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = "⚡ Instan",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentColor,
                        )
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            // 8. Bottom Assistance / Registration Footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Belum memiliki akun? ",
                    fontSize = 12.sp,
                    color = TextTertiary,
                )
                Text(
                    text = "Bantuan Sekolah",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onForgotPasswordClick() }
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                )
            }
        }
    }
}
