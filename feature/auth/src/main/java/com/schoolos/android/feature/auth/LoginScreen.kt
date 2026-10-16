@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.schoolos.android.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.schoolos.android.core.designsystem.CosmicBlack
import com.schoolos.android.core.designsystem.NeonBlue
import com.schoolos.android.core.designsystem.TextTertiary
import com.schoolos.android.feature.auth.components.LoginBackgroundCanvas
import com.schoolos.android.feature.auth.components.LoginErrorToast
import com.schoolos.android.feature.auth.components.LoginFormCard
import com.schoolos.android.feature.auth.components.LoginHeaderSection
import com.schoolos.android.feature.auth.components.LoginHelpDialog
import kotlinx.coroutines.delay

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit = {},
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    var showHelpDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Resolve real dynamic app version name from Android package manager
    val realAppVersion = remember(context) {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.2.1"
        } catch (_: Exception) {
            "1.2.1"
        }
    }

    LaunchedEffect(state.isLoggedIn) {
        if (state.isLoggedIn) onLoginSuccess()
    }

    // Auto-dismiss error toast after 4 seconds
    LaunchedEffect(state.error) {
        if (state.error != null) {
            delay(4000L)
            viewModel.clearError()
        }
    }

    val accentColor = NeonBlue
    val secondaryColor = Color(0xFF6366F1)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CosmicBlack),
    ) {
        // High-end ambient canvas
        LoginBackgroundCanvas(
            accentColor = accentColor,
            secondaryColor = secondaryColor,
        )

        // Scrollable content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp)
                    .padding(top = 20.dp, bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top,
            ) {
                // 1. School Crest & Typography Header
                LoginHeaderSection(
                    schoolName = state.schoolName,
                    schoolLogoUrl = state.schoolLogoUrl,
                    accentColor = accentColor,
                    secondaryColor = secondaryColor,
                )

                Spacer(Modifier.height(2.dp))

                // 2. Main Authentication Form (Flat on canvas, no enclosing card container, no role tabs)
                LoginFormCard(
                    state = state,
                    accentColor = accentColor,
                    secondaryColor = secondaryColor,
                    onUsernameChange = viewModel::onUsernameChanged,
                    onPasswordChange = viewModel::onPasswordChanged,
                    onRememberMeChange = viewModel::onRememberMeChanged,
                    onLoginClick = viewModel::login,
                    onOpenQrScanner = viewModel::openQrScanner,
                    onForgotPasswordClick = { showHelpDialog = true },
                )

                Spacer(Modifier.height(20.dp))

                // 3. Trust & Security Footer with real app version
                Text(
                    text = "© 2026 ${state.schoolName ?: "PKBM AS-SALAFIYAH"} • v$realAppVersion",
                    color = TextTertiary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    letterSpacing = 0.4.sp,
                )
            }
        }

        // 4. Solid Opaque Error Toast Floating Overlay
        LoginErrorToast(
            errorMessage = state.error,
            onDismiss = viewModel::clearError,
            modifier = Modifier.align(Alignment.BottomCenter),
        )

        // 5. Help & Support Dialog
        if (showHelpDialog) {
            LoginHelpDialog(
                accentColor = accentColor,
                onDismissRequest = { showHelpDialog = false },
            )
        }

        // 6. QR Scanner Modal Bottom Sheet
        if (state.showQrScanner) {
            QrScannerModalBottomSheet(
                onDismissRequest = viewModel::closeQrScanner,
                onQrCodeDetected = viewModel::loginWithQr,
            )
        }
    }
}
