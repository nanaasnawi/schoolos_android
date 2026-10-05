package com.schoolos.android.feature.profile

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.core.content.ContextCompat
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
import com.schoolos.android.feature.profile.components.EditProfileFieldDialog
import com.schoolos.android.feature.profile.components.ParentStudentCard
import com.schoolos.android.feature.profile.components.ProfileAvatarActionDialog
import com.schoolos.android.feature.profile.components.ProfileAvatarPreviewDialog
import com.schoolos.android.feature.profile.components.ProfileHeaderCard
import com.schoolos.android.feature.profile.components.ProfileLogoutDialog
import com.schoolos.android.feature.profile.components.ProfileQrCard
import com.schoolos.android.feature.profile.components.ProfileSettingsGroup
import com.schoolos.android.feature.profile.components.SchoolAffiliationCard
import com.schoolos.android.feature.profile.components.WhatsAppProfileCard
import java.io.ByteArrayOutputStream

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
    val isUploadingPhoto by viewModel.isUploadingPhoto.collectAsState()

    var showAvatarActionDialog by remember { mutableStateOf(false) }
    var showAvatarPreviewDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    var activeEditFieldKey by remember { mutableStateOf<String?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val rawBytes = inputStream?.readBytes()
                inputStream?.close()
                if (rawBytes != null && rawBytes.isNotEmpty()) {
                    val compressedBytes = compressAndScaleImage(rawBytes, maxDimension = 1024, quality = 80)
                    val filename = "photo_${System.currentTimeMillis()}.jpg"
                    viewModel.uploadProfilePhoto(compressedBytes, filename, "image/jpeg") { success, err ->
                        if (success) {
                            Toast.makeText(context, "✅ Foto profil berhasil disimpan ke database!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, err ?: "Gagal mengunggah foto", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Gagal membaca berkas gambar: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            try {
                val compressedBytes = compressAndScaleBitmap(bitmap, maxDimension = 1024, quality = 80)
                val filename = "photo_cam_${System.currentTimeMillis()}.jpg"
                viewModel.uploadProfilePhoto(compressedBytes, filename, "image/jpeg") { success, err ->
                    if (success) {
                        Toast.makeText(context, "✅ Foto profil berhasil disimpan ke database!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, err ?: "Gagal mengunggah foto", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Gagal mengambil foto dari kamera: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            try {
                cameraLauncher.launch(null)
            } catch (e: Exception) {
                Toast.makeText(context, "Gagal membuka kamera: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(context, "Izin kamera diperlukan untuk mengambil foto profil.", Toast.LENGTH_LONG).show()
        }
    }

    val launchCameraSafely = {
        val hasCameraPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (hasCameraPermission) {
            try {
                cameraLauncher.launch(null)
            } catch (e: Exception) {
                Toast.makeText(context, "Gagal membuka aplikasi kamera: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val isDarkTheme = LocalIsDarkTheme.current
    val toggleTheme = LocalThemeToggle.current

    val isTeacher = isTeacherRole(user?.role)
    val isParent = isParentRole(user?.role)
    val roleNeon = when {
        isTeacher -> TeacherNeon
        isParent -> ParentNeon
        else -> StudentNeon
    }

    val roleLabel = when {
        isTeacher -> "GURU"
        isParent -> "WALI MURID"
        else -> "SISWA"
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
                title = "Profil Saya",
                subtitle = "Identitas Pengguna & Akses QR",
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

            // 1. Hero Profile Header Card (Tapping photo opens WhatsApp-style full screen preview)
            ProfileHeaderCard(
                user = user,
                roleNeon = roleNeon,
                isTeacher = isTeacher,
                isParent = isParent,
                displayContact = displayContact,
                className = state.className,
                isUploading = isUploadingPhoto,
                onAvatarClick = { showAvatarPreviewDialog = true },
            )

            // 2. WhatsApp-Style Profile Details Card
            WhatsAppProfileCard(
                userName = user?.name ?: "",
                about = state.about,
                username = state.username,
                email = user?.email ?: "",
                phone = if (state.phone.isNotBlank()) state.phone else state.identifier,
                roleNeon = roleNeon,
                onEditField = { fieldKey ->
                    activeEditFieldKey = fieldKey
                },
            )

            // 3. Downloadable QR Code Login Card
            ProfileQrCard(
                userName = user?.name ?: "Pengguna SchoolOS",
                roleLabel = roleLabel,
                schoolName = state.schoolName.ifBlank { "SchoolOS Platform" },
                qrToken = state.qrToken,
                roleNeon = roleNeon,
            )

            // 4. Parent-specific Connected Child Card
            if (isParent) {
                ParentStudentCard(
                    studentName = state.childName,
                    studentClass = state.className,
                )
            }

            // 5. School Affiliation Card
            SchoolAffiliationCard(
                schoolName = state.schoolName,
                schoolLogoUrl = state.schoolLogoUrl,
            )

            // 6. Settings & Account Operations
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

        // Fullscreen Avatar Preview Dialog with Floating Camera Button
        if (showAvatarPreviewDialog) {
            ProfileAvatarPreviewDialog(
                avatarUrl = user?.avatarUrl,
                userName = user?.name,
                onChangePhotoClick = { showAvatarActionDialog = true },
                onDismissRequest = { showAvatarPreviewDialog = false },
            )
        }

        // Action Dialog for Camera or Gallery Photo Pick
        if (showAvatarActionDialog) {
            ProfileAvatarActionDialog(
                hasCustomPhoto = !user?.avatarUrl.isNullOrBlank(),
                onViewPhoto = { showAvatarPreviewDialog = true },
                onTakePhoto = { launchCameraSafely() },
                onPickGallery = {
                    try {
                        galleryLauncher.launch("image/*")
                    } catch (e: Exception) {
                        Toast.makeText(context, "Gagal membuka galeri: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                    }
                },
                onDismissRequest = { showAvatarActionDialog = false },
            )
        }

        if (activeEditFieldKey != null) {
            val key = activeEditFieldKey!!
            val initialVal = when (key) {
                "username" -> state.username
                "email" -> user?.email ?: ""
                "phone" -> if (state.phone.isNotBlank()) state.phone else state.identifier
                "about" -> state.about
                else -> ""
            }

            EditProfileFieldDialog(
                fieldKey = key,
                currentValue = initialVal,
                roleNeon = roleNeon,
                onSave = { newValue ->
                    viewModel.updateEditableField(key, newValue) { success, err ->
                        if (success) {
                            Toast.makeText(context, "✅ Data berhasil diperbarui di server!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, err ?: "Gagal memperbarui data", Toast.LENGTH_LONG).show()
                        }
                    }
                },
                onDismissRequest = { activeEditFieldKey = null },
            )
        }

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

private fun compressAndScaleImage(
    rawBytes: ByteArray,
    maxDimension: Int = 1024,
    quality: Int = 80
): ByteArray {
    return try {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, options)

        var sampleSize = 1
        val origWidth = options.outWidth
        val origHeight = options.outHeight

        if (origWidth > maxDimension || origHeight > maxDimension) {
            val halfWidth = origWidth / 2
            val halfHeight = origHeight / 2
            while ((halfWidth / sampleSize) >= maxDimension && (halfHeight / sampleSize) >= maxDimension) {
                sampleSize *= 2
            }
        }

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
        }
        val sampledBitmap = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, decodeOptions)
            ?: return rawBytes

        val width = sampledBitmap.width
        val height = sampledBitmap.height
        val scaledBitmap = if (width > maxDimension || height > maxDimension) {
            val ratio = Math.min(maxDimension.toFloat() / width, maxDimension.toFloat() / height)
            val newW = (width * ratio).toInt()
            val newH = (height * ratio).toInt()
            Bitmap.createScaledBitmap(sampledBitmap, newW, newH, true)
        } else {
            sampledBitmap
        }

        val outputStream = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        outputStream.toByteArray()
    } catch (_: Exception) {
        rawBytes
    }
}

private fun compressAndScaleBitmap(
    bitmap: Bitmap,
    maxDimension: Int = 1024,
    quality: Int = 80
): ByteArray {
    return try {
        val width = bitmap.width
        val height = bitmap.height
        val scaledBitmap = if (width > maxDimension || height > maxDimension) {
            val ratio = Math.min(maxDimension.toFloat() / width, maxDimension.toFloat() / height)
            val newW = (width * ratio).toInt()
            val newH = (height * ratio).toInt()
            Bitmap.createScaledBitmap(bitmap, newW, newH, true)
        } else {
            bitmap
        }

        val outputStream = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        outputStream.toByteArray()
    } catch (_: Exception) {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
        stream.toByteArray()
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
