package com.schoolos.android.feature.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.schoolos.android.core.auth.isParentRole
import com.schoolos.android.core.auth.isTeacherRole
import com.schoolos.android.core.designsystem.CosmicBlack
import com.schoolos.android.core.designsystem.ExecutiveTopBar
import com.schoolos.android.core.designsystem.LocalIsDarkTheme
import com.schoolos.android.core.designsystem.LocalThemeToggle
import com.schoolos.android.core.designsystem.ParentNeon
import com.schoolos.android.core.designsystem.StudentNeon
import com.schoolos.android.core.designsystem.TeacherNeon
import com.schoolos.android.feature.profile.components.ParentStudentCard
import com.schoolos.android.feature.profile.components.ProfileAboutDialog
import com.schoolos.android.feature.profile.components.ProfileChangePasswordDialog
import com.schoolos.android.feature.profile.components.ProfileHeaderCard
import com.schoolos.android.feature.profile.components.ProfileHelpDialog
import com.schoolos.android.feature.profile.components.ProfileLogoutDialog
import com.schoolos.android.feature.profile.components.ProfileSettingsGroup
import com.schoolos.android.feature.profile.components.SchoolAffiliationCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onLogout: () -> Unit = {},
    onNavigateToSecurity: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToHelp: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    val user = state.user

    var showLogoutDialog by remember { mutableStateOf(false) }

    val isDarkTheme = LocalIsDarkTheme.current
    val toggleTheme = LocalThemeToggle.current

    val isTeacher = isTeacherRole(user?.role)
    val isParent = isParentRole(user?.role)
    val roleNeon = when {
        isTeacher -> TeacherNeon
        isParent -> ParentNeon
        else -> StudentNeon
    }

    val displayContact: String = when {
        isParent -> {
            val phone = state.identifier.ifBlank { "" }
            if (phone.isNotBlank()) formatPhoneNumber(phone)
            else if (user?.email?.contains("@wali.schoolos.id") == false) user?.email ?: ""
            else user?.name?.ifBlank { "Akun Wali Murid" } ?: "Akun Wali Murid"
        }
        else -> user?.email?.ifBlank { state.identifier.ifBlank { "Akun Terverifikasi" } } ?: "Akun Terverifikasi"
    }

    Scaffold(
        containerColor = CosmicBlack,
        topBar = {
            ExecutiveTopBar(
                title = "Pengaturan Akun",
                subtitle = "Identitas Pengguna & Sekolah",
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(Modifier.height(2.dp))

            // 1. Hero Profile Card
            ProfileHeaderCard(
                user = user,
                roleNeon = roleNeon,
                isTeacher = isTeacher,
                isParent = isParent,
                displayContact = displayContact,
                className = state.className,
            )

            // 2. Parent-specific Connected Child Card
            if (isParent) {
                ParentStudentCard(
                    studentName = state.childName,
                    studentClass = state.className,
                )
            }

            // 3. School Affiliation Card
            SchoolAffiliationCard(
                schoolName = state.schoolName,
                schoolLogoUrl = state.schoolLogoUrl,
            )

            // 4. Settings & Account Operations
            ProfileSettingsGroup(
                isDarkTheme = isDarkTheme,
                onToggleTheme = toggleTheme,
                onSecurityClick = onNavigateToSecurity,
                onNotificationsClick = onNavigateToNotifications,
                onHelpClick = onNavigateToHelp,
                onAboutClick = onNavigateToAbout,
                onLogoutClick = { showLogoutDialog = true },
            )

            Spacer(Modifier.height(24.dp))
        }

        // Dialogs
        if (showLogoutDialog) {
            ProfileLogoutDialog(
                userName = user?.name,
                onConfirmLogout = {
                    showLogoutDialog = false
                    onLogout()
                },
                onDismissRequest = { showLogoutDialog = false },
            )
        }
    }
}

private fun formatPhoneNumber(phone: String): String {
    val clean = phone.filter { it.isDigit() }
    return when {
        clean.startsWith("08") && clean.length >= 10 -> {
            "${clean.substring(0, 4)}-${clean.substring(4, 8)}-${clean.substring(8)}"
        }
        clean.startsWith("628") && clean.length >= 11 -> {
            "+62 ${clean.substring(2, 5)}-${clean.substring(5, 9)}-${clean.substring(9)}"
        }
        else -> phone
    }
}
