package com.schoolos.android.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val username: String,
    val password: String,
)

@Serializable
data class ChangePasswordRequestDto(
    @SerialName("current_password") val currentPassword: String,
    @SerialName("new_password") val newPassword: String,
)

@Serializable
data class QrLoginRequest(
    val token: String,
)

@Serializable
data class LoginResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("token_type") val tokenType: String = "Bearer",
    @SerialName("expires_in") val expiresIn: Long = 86400,
    @SerialName("refresh_token") val refreshToken: String = "",
    @SerialName("user_id") val userId: String = "",
    @SerialName("tenant_id") val tenantId: String = "",
    val name: String = "",
    val email: String = "",
    val role: String = "",
    @SerialName("school_name") val schoolName: String? = null,
    @SerialName("school_logo_url") val schoolLogoUrl: String? = null,
    val identifier: String? = null,
    @SerialName("class_name") val className: String? = null,
    @SerialName("child_name") val childName: String? = null,
    @SerialName("child_id") val childId: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
)

@Serializable
data class RefreshTokenRequest(
    val refreshToken: String,
)

@Serializable
data class RefreshTokenResponse(
    val accessToken: String,
    val refreshToken: String,
)

@Serializable
data class SchoolProfileResponse(
    val id: String,
    val name: String,
    @SerialName("logo_url") val logoUrl: String? = null,
    val npsn: String? = null,
    val address: String? = null,
    @SerialName("phone_number") val phoneNumber: String? = null,
    val email: String? = null,
    val accreditation: String? = null,
    @SerialName("dapodik_url") val dapodikUrl: String? = null,
    val status: String? = null,
)

@Serializable
data class SchoolPublicInfoResponse(
    val name: String,
    @SerialName("logo_url") val logoUrl: String? = null,
    val npsn: String? = null,
)

@Serializable
data class UserDto(
    val id: String = "",
    val email: String = "",
    val phone: String? = null,
    val about: String? = null,
    @SerialName("full_name") val fullName: String = "",
    val role: String = "",
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("school_name") val schoolName: String? = null,
    @SerialName("school_logo_url") val schoolLogoUrl: String? = null,
    val identifier: String? = null,
    @SerialName("class_name") val className: String? = null,
    @SerialName("child_name") val childName: String? = null,
    @SerialName("child_id") val childId: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
)

@Serializable
data class UpdateProfileRequestDto(
    @SerialName("full_name") val fullName: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val identifier: String? = null,
    val about: String? = null,
)

@Serializable
data class UploadAvatarResponse(
    @SerialName("avatar_url") val avatarUrl: String = "",
    val message: String = "",
)

@Serializable
data class QrBadgeDto(
    val id: String = "",
    @SerialName("raw_token") val rawToken: String = "",
    @SerialName("user_id") val userId: String = "",
    @SerialName("tenant_id") val tenantId: String = "",
    @SerialName("token_type") val tokenType: String = "BADGE",
    val label: String = "",
    @SerialName("expires_at") val expiresAt: String? = null,
    @SerialName("created_at") val createdAt: String = "",
)
