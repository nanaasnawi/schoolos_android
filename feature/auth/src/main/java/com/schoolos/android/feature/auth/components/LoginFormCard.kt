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
import androidx.compose.material.icons.filled.Shield
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
            .shadow(18.dp, RoundedCornerShape(28.dp), spotColor = accentColor.copy(alpha = 0.15f))
            .clip(RoundedCornerShape(28.dp))
            .background(CosmicNavy.copy(alpha = 0.98f))
            .border(1.2.dp, GlassBorder, RoundedCornerShape(28.dp))
            .padding(16.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Card Sub-Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(accentColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "AUTENTIKASI",
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp,
                        color = TextTertiary,
                        letterSpacing = 1.2.sp,
                    )
                }

                // Role badge pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(accentColor.copy(alpha = 0.12f))
                        .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                ) {
                    Text(
                        text = currentTab.title.uppercase(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor,
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            // Username / Identity Field
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
                shape = RoundedCornerShape(16.dp),
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

            // Contextual Hint Banner
            if (currentTab.hintExample.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(CosmicSurface)
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

            // Password Field
            OutlinedTextField(
                value = state.password,
                onValueChange = onPasswordChange,
                label = {
                    Text(
                        "Kata Sandi",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                    )
                },
                placeholder = {
                    Text(
                        "Masukkan kata sandi akun Anda",
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
                shape = RoundedCornerShape(16.dp),
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

            // Remember Me & Help Row
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

            // Login CTA Button
            Button(
                onClick = {
                    focusManager.clearFocus()
                    onLoginClick()
                },
                enabled = !state.isLoading,
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .shadow(
                        elevation = if (state.isLoading) 0.dp else 12.dp,
                        shape = RoundedCornerShape(18.dp),
                        spotColor = accentColor.copy(alpha = 0.5f),
                    )
                    .clip(RoundedCornerShape(18.dp))
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
                                text = "Masuk",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                letterSpacing = 0.3.sp,
                            )
                            Spacer(Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Divider: "ATAU MASUK PRAKTIS"
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
                    text = "ATAU MASUK PRAKTIS",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextTertiary,
                    modifier = Modifier.padding(horizontal = 10.dp),
                    letterSpacing = 1.sp,
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(1.dp)
                        .background(GlassBorder),
                )
            }

            Spacer(Modifier.height(14.dp))

            // QR Code Action Button
            Surface(
                onClick = onOpenQrScanner,
                shape = RoundedCornerShape(18.dp),
                color = CosmicSurface,
                border = androidx.compose.foundation.BorderStroke(1.2.dp, accentColor.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "Pindai QR Code",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                    )
                }
            }
        }
    }
}
