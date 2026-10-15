package com.schoolos.android.feature.auth.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.schoolos.android.core.designsystem.CosmicSurface
import com.schoolos.android.core.designsystem.GlassBorder
import com.schoolos.android.core.designsystem.NeonBlue
import com.schoolos.android.core.designsystem.TextPrimary
import com.schoolos.android.core.designsystem.TextSecondary
import com.schoolos.android.core.designsystem.TextTertiary
import com.schoolos.android.feature.auth.LoginUiState

@Composable
fun LoginFormCard(
    state: LoginUiState,
    accentColor: Color = NeonBlue,
    secondaryColor: Color = Color(0xFF6366F1),
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

    // Seamless form layout directly on page background (NO CARD CONTAINER)
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // 1. "Login" Section Title (Left aligned matching mockup)
        Text(
            text = "Login",
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            color = TextPrimary,
            letterSpacing = (-0.5).sp,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(16.dp))

        // 2. Username / Email Field (Pill Shape)
        OutlinedTextField(
            value = state.username,
            onValueChange = onUsernameChange,
            placeholder = {
                Text(
                    text = "Username atau Email",
                    fontSize = 14.sp,
                    color = TextTertiary,
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = TextTertiary,
                    modifier = Modifier
                        .padding(start = 6.dp)
                        .size(20.dp),
                )
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
            shape = CircleShape,
            modifier = Modifier
                .fillMaxWidth()
                .shadow(4.dp, CircleShape, spotColor = Color.Black.copy(alpha = 0.06f)),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next,
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = CosmicSurface,
                unfocusedContainerColor = CosmicSurface,
                focusedBorderColor = accentColor,
                unfocusedBorderColor = GlassBorder,
                cursorColor = accentColor,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextSecondary,
            ),
        )

        Spacer(Modifier.height(14.dp))

        // 3. Password Field (Pill Shape)
        OutlinedTextField(
            value = state.password,
            onValueChange = onPasswordChange,
            placeholder = {
                Text(
                    text = "Password",
                    fontSize = 14.sp,
                    color = TextTertiary,
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = TextTertiary,
                    modifier = Modifier
                        .padding(start = 6.dp)
                        .size(20.dp),
                )
            },
            singleLine = true,
            shape = CircleShape,
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
            modifier = Modifier
                .fillMaxWidth()
                .shadow(4.dp, CircleShape, spotColor = Color.Black.copy(alpha = 0.06f)),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = CosmicSurface,
                unfocusedContainerColor = CosmicSurface,
                focusedBorderColor = accentColor,
                unfocusedBorderColor = GlassBorder,
                cursorColor = accentColor,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextSecondary,
            ),
        )

        Spacer(Modifier.height(8.dp))

        // 4. Remember Me & Lupa Kata Sandi Row
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
                    .padding(vertical = 4.dp),
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
                Spacer(Modifier.width(6.dp))
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

        Spacer(Modifier.height(18.dp))

        // 5. Primary Login Action Button ("Login →")
        Button(
            onClick = {
                focusManager.clearFocus()
                onLoginClick()
            },
            enabled = !state.isLoading,
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .shadow(
                    elevation = if (state.isLoading) 0.dp else 10.dp,
                    shape = CircleShape,
                    spotColor = accentColor.copy(alpha = 0.45f),
                )
                .clip(CircleShape)
                .background(
                    if (state.isLoading) {
                        Brush.horizontalGradient(
                            listOf(
                                accentColor.copy(alpha = 0.6f),
                                secondaryColor.copy(alpha = 0.4f),
                            ),
                        )
                    } else {
                        Brush.horizontalGradient(
                            listOf(
                                accentColor,
                                secondaryColor,
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
                            text = "Memverifikasi...",
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
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            letterSpacing = 0.5.sp,
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

        Spacer(Modifier.height(20.dp))

        // 6. Divider: "Atau Masuk Dengan"
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
                text = "Atau Masuk Dengan",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextTertiary,
                modifier = Modifier.padding(horizontal = 14.dp),
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(1.dp)
                    .background(GlassBorder),
            )
        }

        Spacer(Modifier.height(16.dp))

        // 7. Dedicated QR Code Login Action Button
        Surface(
            onClick = onOpenQrScanner,
            shape = CircleShape,
            color = CosmicSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder),
            shadowElevation = 3.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = "Pindai QR",
                    tint = accentColor,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "Login dengan QR Code",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                )
            }
        }

        Spacer(Modifier.height(22.dp))

        // 8. Bottom Assistance / School Help Footer (Do you have an account removed)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Pusat Bantuan Sekolah",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = accentColor,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onForgotPasswordClick() }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            )
        }
    }
}
