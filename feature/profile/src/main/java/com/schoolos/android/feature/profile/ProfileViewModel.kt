package com.schoolos.android.feature.profile

import android.content.Context
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.schoolos.android.core.auth.AuthManager
import com.schoolos.android.core.common.Settings
import com.schoolos.android.core.common.SettingsManager
import com.schoolos.android.domain.model.User
import com.schoolos.android.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val user: User? = null,
    val username: String = "",
    val phone: String = "",
    val about: String = "Ada di SchoolOS",
    val schoolName: String = "",
    val schoolLogoUrl: String? = null,
    val className: String = "",
    val identifier: String = "",
    val childName: String = "",
    val isDarkMode: Boolean = false,
    val currentLanguage: String = "en",
    val appVersion: String = "",
    val loggingOut: Boolean = false,
    val qrToken: String = "",
    // Dynamic school contact info fetched from /schools/profile
    val schoolPhone: String? = null,
    val schoolEmail: String? = null,
    val schoolAddress: String? = null,
    val schoolNpsn: String? = null,
    val schoolAccreditation: String? = null,
    val schoolWebsite: String? = null,
    val schoolContactLoading: Boolean = false,
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authManager: AuthManager,
    private val settingsManager: SettingsManager,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    val settings: StateFlow<Settings> = settingsManager.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Settings())

    init {
        viewModelScope.launch {
            val pkg = try {
                context.packageManager.getPackageInfo(context.packageName, 0)
            } catch (_: PackageManager.NameNotFoundException) { null }
            _state.value = _state.value.copy(appVersion = pkg?.versionName ?: "1.0.0")
        }
        viewModelScope.launch {
            authManager.authState.collect { authState ->
                val userId = authState.userId ?: ""
                val tenantId = authState.tenantId ?: ""
                val identifier = authState.identifier ?: ""
                val token = if (userId.isNotBlank()) "sch_qr_v1_${userId}_${tenantId.ifBlank { "tenant" }}" else "sch_qr_v1_guest"

                _state.value = _state.value.copy(
                    user = if (authState.isLoggedIn) User(
                        id = userId,
                        name = authState.name ?: "",
                        email = authState.email ?: "",
                        role = authState.role ?: "",
                        avatarUrl = authState.avatarUrl,
                    ) else null,
                    username = identifier.ifBlank { authState.name?.lowercase()?.replace(" ", "_") ?: "user" },
                    phone = authState.phone ?: "",
                    about = authState.about?.ifBlank { null } ?: "Ada di SchoolOS",
                    schoolName = authState.schoolName ?: "",
                    schoolLogoUrl = authState.schoolLogoUrl,
                    className = authState.className ?: "",
                    identifier = identifier,
                    childName = authState.childName ?: "",
                    qrToken = if (_state.value.qrToken.startsWith("sch_qr_v1_") && !_state.value.qrToken.contains("_tenant")) _state.value.qrToken else token,
                )
            }
        }
        viewModelScope.launch {
            if (authManager.isLoggedIn) {
                authRepository.getCurrentUser()
                loadQrBadgeToken()
            }
        }
        loadSchoolContactInfo()
    }

    private fun loadQrBadgeToken() {
        viewModelScope.launch {
            if (authManager.isLoggedIn) {
                authRepository.getMyQrBadge()
                    .onSuccess { rawToken ->
                        if (rawToken.isNotBlank()) {
                            _state.value = _state.value.copy(qrToken = rawToken)
                        }
                    }
            }
        }
    }

    private fun loadSchoolContactInfo() {
        viewModelScope.launch {
            _state.value = _state.value.copy(schoolContactLoading = true)
            authRepository.getSchoolContactInfo()
                .onSuccess { info ->
                    _state.value = _state.value.copy(
                        schoolPhone = info.phone,
                        schoolEmail = info.email,
                        schoolAddress = info.address,
                        schoolNpsn = info.npsn,
                        schoolAccreditation = info.accreditation,
                        schoolWebsite = info.website,
                    )
                }
            // Fail silently — contact info is non-critical
            _state.value = _state.value.copy(schoolContactLoading = false)
        }
    }

    fun setDarkMode(enabled: Boolean) {
        viewModelScope.launch { settingsManager.setDarkMode(enabled) }
    }

    fun setLanguage(language: String) {
        viewModelScope.launch { settingsManager.setLanguage(language) }
    }

    suspend fun changePassword(current: String, newPass: String): Result<Unit> {
        return authRepository.changePassword(current, newPass)
    }

    val isUploadingPhoto = MutableStateFlow(false)

    fun uploadProfilePhoto(bytes: ByteArray, filename: String, mimeType: String, onComplete: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            isUploadingPhoto.value = true
            authRepository.uploadAvatar(bytes, filename, mimeType)
                .onSuccess { newAvatarUrl ->
                    isUploadingPhoto.value = false
                    _state.value = _state.value.copy(
                        user = _state.value.user?.copy(avatarUrl = newAvatarUrl)
                    )
                    onComplete(true, null)
                }
                .onFailure { error ->
                    isUploadingPhoto.value = false
                    onComplete(false, error.localizedMessage ?: "Gagal mengunggah foto profil.")
                }
        }
    }

    fun updateEditableField(
        fieldKey: String, // "username", "email", "phone", "about"
        newValue: String,
        onComplete: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val clean = newValue.trim()
                val currentName = _state.value.user?.name

                val res = when (fieldKey) {
                    "username" -> authRepository.updateProfile(fullName = currentName, identifier = clean)
                    "email" -> authRepository.updateProfile(fullName = currentName, email = clean)
                    "phone" -> authRepository.updateProfile(fullName = currentName, phone = clean)
                    "about" -> authRepository.updateProfile(fullName = currentName, about = clean)
                    else -> Result.success(Unit)
                }

                res.onSuccess {
                    try {
                        authRepository.getCurrentUser()
                    } catch (_: Exception) {}
                    onComplete(true, null)
                }.onFailure { err ->
                    onComplete(false, err.localizedMessage ?: "Gagal memperbarui data di server.")
                }
            } catch (e: Exception) {
                onComplete(false, e.localizedMessage ?: "Gagal memperbarui data.")
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loggingOut = true)
            authRepository.logout()
        }
    }
}
