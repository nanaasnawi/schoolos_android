@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.schoolos.android.feature.auth

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.schoolos.android.core.designsystem.CosmicBlack
import com.schoolos.android.core.designsystem.TextTertiary
import com.schoolos.android.feature.auth.components.LoginBackgroundCanvas
import com.schoolos.android.feature.auth.components.LoginErrorToast
import com.schoolos.android.feature.auth.components.LoginFormCard
import com.schoolos.android.feature.auth.components.LoginHeaderSection
import com.schoolos.android.feature.auth.components.LoginHelpDialog
import com.schoolos.android.feature.auth.components.RoleTabsBar
import com.schoolos.android.feature.auth.components.rememberRoleTabs
import kotlinx.coroutines.delay

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit = {},
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    var showHelpDialog by remember { mutableStateOf(false) }

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

    val roleTabs = rememberRoleTabs()
    val currentTab = roleTabs.getOrElse(state.selectedRoleTab) { roleTabs[0] }
    val animatedAccentColor by animateColorAsState(
        targetValue = currentTab.accentColor,
        animationSpec = tween(400),
        label = "accentColorAnim",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CosmicBlack),
    ) {
        // High-end ambient canvas
        LoginBackgroundCanvas(
            accentColor = animatedAccentColor,
            secondaryColor = currentTab.secondaryColor,
        )

        // Scrollable content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
                    .padding(top = 48.dp, bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                // 1. School Crest & Typography Header
                LoginHeaderSection(
                    schoolName = state.schoolName,
                    schoolLogoUrl = state.schoolLogoUrl,
                    accentColor = animatedAccentColor,
                    secondaryColor = currentTab.secondaryColor,
                )

                Spacer(Modifier.height(22.dp))

                // 2. Role Selector Tabs
                RoleTabsBar(
                    roleTabs = roleTabs,
                    selectedTabIndex = state.selectedRoleTab,
                    onTabSelected = viewModel::onRoleTabChanged,
                )

                Spacer(Modifier.height(18.dp))

                // 3. Main Authentication Form Card
                LoginFormCard(
                    state = state,
                    currentTab = currentTab,
                    accentColor = animatedAccentColor,
                    onUsernameChange = viewModel::onUsernameChanged,
                    onPasswordChange = viewModel::onPasswordChanged,
                    onRememberMeChange = viewModel::onRememberMeChanged,
                    onLoginClick = viewModel::login,
                    onOpenQrScanner = viewModel::openQrScanner,
                    onForgotPasswordClick = { showHelpDialog = true },
                )

                Spacer(Modifier.height(24.dp))

                // 4. Trust & Security Footer
                Text(
                    text = "© 2026 ${state.schoolName ?: "SCHOOL OS"} • v2.0",
                    color = TextTertiary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    letterSpacing = 0.4.sp,
                )
            }
        }

        // 5. Solid Opaque Error Toast Floating Overlay
        LoginErrorToast(
            errorMessage = state.error,
            onDismiss = viewModel::clearError,
            modifier = Modifier.align(Alignment.BottomCenter),
        )

        // 6. Help & Support Dialog
        if (showHelpDialog) {
            LoginHelpDialog(
                accentColor = animatedAccentColor,
                onDismissRequest = { showHelpDialog = false },
            )
        }

        // 7. QR Scanner Modal Bottom Sheet
        if (state.showQrScanner) {
            QrScannerModalBottomSheet(
                onDismissRequest = viewModel::closeQrScanner,
                onQrCodeDetected = viewModel::loginWithQr,
            )
        }
    }
}
