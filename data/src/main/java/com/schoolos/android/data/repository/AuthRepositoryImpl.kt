package com.schoolos.android.data.repository

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import com.schoolos.android.core.auth.AuthManager
import com.schoolos.android.data.remote.SchoolOsApi
import com.schoolos.android.data.remote.dto.LoginRequest
import com.schoolos.android.domain.model.SchoolContactInfo
import com.schoolos.android.domain.model.User
import com.schoolos.android.domain.repository.AuthRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val api: SchoolOsApi,
    private val authManager: AuthManager,
) : AuthRepository {

    override suspend fun login(username: String, password: String): Result<User> = runCatching {
        val response = api.login(LoginRequest(username = username, password = password))
        val data = response.data ?: throw Exception(
            response.error?.message ?: "Login gagal. Silakan periksa kredensial email/username & password Anda."
        )

        authManager.saveSession(
            accessToken = data.accessToken,
            refreshToken = data.refreshToken,
            userId = data.userId,
            tenantId = data.tenantId,
            name = data.name,
            email = data.email,
            role = data.role,
            identifier = data.identifier,
            className = data.className,
            childName = data.childName,
            childId = data.childId,
            avatarUrl = data.avatarUrl,
        )

        if (!data.schoolName.isNullOrBlank()) {
            authManager.saveSchoolProfile(name = data.schoolName, logoUrl = data.schoolLogoUrl)
        } else {
            try {
                val profileResponse = api.getSchoolProfile()
                profileResponse.data?.let { profile ->
                    authManager.saveSchoolProfile(name = profile.name, logoUrl = profile.logoUrl)
                }
            } catch (e: Exception) {
                // Log or ignore profile fetch failure so it doesn't break login
            }
        }

        User(id = data.userId, name = data.name, email = data.email, role = data.role, avatarUrl = data.avatarUrl)
    }

    override suspend fun loginWithQr(token: String): Result<User> = runCatching {
        val response = api.loginWithQr(com.schoolos.android.data.remote.dto.QrLoginRequest(token = token))
        val data = response.data ?: throw Exception(
            response.error?.message ?: "QR Login gagal. Pastikan QR Code valid dan masih aktif."
        )

        authManager.saveSession(
            accessToken = data.accessToken,
            refreshToken = data.refreshToken,
            userId = data.userId,
            tenantId = data.tenantId,
            name = data.name,
            email = data.email,
            role = data.role,
            identifier = data.identifier,
            className = data.className,
            childName = data.childName,
            childId = data.childId,
            avatarUrl = data.avatarUrl,
        )

        if (!data.schoolName.isNullOrBlank()) {
            authManager.saveSchoolProfile(name = data.schoolName, logoUrl = data.schoolLogoUrl)
        } else {
            try {
                val profileResponse = api.getSchoolProfile()
                profileResponse.data?.let { profile ->
                    authManager.saveSchoolProfile(name = profile.name, logoUrl = profile.logoUrl)
                }
            } catch (e: Exception) {
                // Log or ignore profile fetch failure so it doesn't break login
            }
        }

        User(id = data.userId, name = data.name, email = data.email, role = data.role, avatarUrl = data.avatarUrl)
    }

    override suspend fun logout() {

        authManager.clearSession()
    }

    override suspend fun refreshToken(): Result<String> = runCatching {
        val refreshToken = authManager.getRefreshToken() ?: throw Exception("No refresh token stored")
        val response = api.refreshToken(
            com.schoolos.android.data.remote.dto.RefreshTokenRequest(refreshToken)
        )
        val data = response.data ?: throw Exception("Token refresh failed")
        authManager.updateTokens(
            accessToken = data.accessToken,
            refreshToken = data.refreshToken,
        )
        data.accessToken
    }

    override suspend fun isLoggedIn(): Boolean = authManager.isLoggedIn

    override suspend fun getCurrentUser(): Result<User> = runCatching {
        val response = api.getCurrentUser()
        val data = response.data ?: throw Exception(
            response.error?.message ?: "Gagal memuat profil pengguna."
        )

        authManager.updateUserProfile(
            name = data.fullName,
            email = data.email,
            role = data.role,
            identifier = data.identifier,
            className = data.className,
            childName = data.childName,
            childId = data.childId,
            avatarUrl = data.avatarUrl,
        )
        authManager.updateUserEditableDetails(
            email = data.email.takeIf { it.isNotBlank() },
            phone = data.phone?.takeIf { it.isNotBlank() },
            identifier = data.identifier?.takeIf { it.isNotBlank() },
            about = data.about?.takeIf { it.isNotBlank() },
        )

        var schoolName = data.schoolName
        var schoolLogo = data.schoolLogoUrl

        // Also fetch latest school profile from /api/v1/schools/profile to ensure dynamic tenant logo changes from Next.js are instantly captured!
        try {
            val schoolProfile = api.getSchoolProfile().data
            if (schoolProfile != null) {
                if (!schoolProfile.name.isNullOrBlank()) schoolName = schoolProfile.name
                if (!schoolProfile.logoUrl.isNullOrBlank()) schoolLogo = schoolProfile.logoUrl
            }
        } catch (_: Exception) {}

        if (!schoolName.isNullOrBlank()) {
            authManager.saveSchoolProfile(name = schoolName, logoUrl = schoolLogo)
        }

        User(id = data.id, name = data.fullName, email = data.email, role = data.role, avatarUrl = data.avatarUrl)
    }

    override suspend fun changePassword(currentPassword: String, newPassword: String): Result<Unit> = runCatching {
        val response = api.changePassword(
            com.schoolos.android.data.remote.dto.ChangePasswordRequestDto(
                currentPassword = currentPassword,
                newPassword = newPassword,
            )
        )
        if (!response.success) {
            throw Exception(response.error?.message ?: "Gagal memperbarui kata sandi.")
        }
    }

    override suspend fun getSchoolContactInfo(): Result<SchoolContactInfo> = runCatching {
        val response = api.getSchoolProfile()
        val data = response.data ?: throw Exception(
            response.error?.message ?: "Gagal memuat informasi kontak sekolah."
        )
        SchoolContactInfo(
            name = data.name,
            npsn = data.npsn?.takeIf { it.isNotBlank() },
            phone = data.phoneNumber?.takeIf { it.isNotBlank() },
            email = data.email?.takeIf { it.isNotBlank() },
            address = data.address?.takeIf { it.isNotBlank() },
            accreditation = data.accreditation?.takeIf { it.isNotBlank() },
            website = data.dapodikUrl?.takeIf { it.isNotBlank() },
        )
    }

    override suspend fun uploadAvatar(
        bytes: ByteArray,
        filename: String,
        mimeType: String
    ): Result<String> = runCatching {
        val mediaType = mimeType.toMediaTypeOrNull()
        val requestBody = bytes.toRequestBody(mediaType)
        val part = MultipartBody.Part.createFormData("avatar", filename, requestBody)
        val response = api.uploadAvatar(part)
        val data = response.data ?: throw Exception(response.error?.message ?: "Gagal mengunggah foto profil.")
        authManager.updateUserAvatar(data.avatarUrl)
        data.avatarUrl
    }

    override suspend fun updateProfile(
        fullName: String?,
        avatarUrl: String?,
        email: String?,
        phone: String?,
        identifier: String?,
        about: String?,
    ): Result<Unit> = runCatching {
        val response = api.updateProfile(
            com.schoolos.android.data.remote.dto.UpdateProfileRequestDto(
                fullName = fullName,
                avatarUrl = avatarUrl,
                email = email,
                phone = phone,
                identifier = identifier,
                about = about,
            )
        )
        if (!response.success) {
            throw Exception(response.error?.message ?: "Gagal memperbarui profil.")
        }
        authManager.updateUserEditableDetails(
            email = email,
            phone = phone,
            identifier = identifier,
            about = about,
        )
        if (!avatarUrl.isNullOrBlank()) {
            authManager.updateUserAvatar(avatarUrl)
        }
        try {
            getCurrentUser()
        } catch (_: Exception) {}
    }

    override suspend fun getMyQrBadge(): Result<String> = runCatching {
        val response = api.getMyQrBadge()
        val data = response.data ?: throw Exception(
            response.error?.message ?: "Gagal memuat QR Badge login."
        )
        if (data.rawToken.isBlank()) {
            throw Exception("QR Badge token kosong.")
        }
        data.rawToken
    }
}
