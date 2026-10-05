package com.schoolos.android.domain.repository

import com.schoolos.android.domain.model.SchoolContactInfo
import com.schoolos.android.domain.model.User

interface AuthRepository {
    suspend fun login(username: String, password: String): Result<User>
    suspend fun loginWithQr(token: String): Result<User>
    suspend fun logout()
    suspend fun refreshToken(): Result<String>
    suspend fun isLoggedIn(): Boolean
    suspend fun getCurrentUser(): Result<User>
    suspend fun changePassword(currentPassword: String, newPassword: String): Result<Unit>
    suspend fun getSchoolContactInfo(): Result<SchoolContactInfo>
    suspend fun uploadAvatar(bytes: ByteArray, filename: String, mimeType: String): Result<String>
    suspend fun updateProfile(
        fullName: String? = null,
        avatarUrl: String? = null,
        email: String? = null,
        phone: String? = null,
        identifier: String? = null,
        about: String? = null,
    ): Result<Unit>
    suspend fun getMyQrBadge(): Result<String>
}

