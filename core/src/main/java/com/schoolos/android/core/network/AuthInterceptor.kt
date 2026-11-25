package com.schoolos.android.core.network

import android.util.Base64
import com.schoolos.android.core.auth.AuthManager
import okhttp3.Interceptor
import okhttp3.Response
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthInterceptor @Inject constructor(
    private val authManager: AuthManager,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val path = original.url.encodedPath

        // Skip auth handling for public login / qr login / refresh endpoints
        if (path.contains("/auth/login") || path.contains("/auth/qr-login") || path.contains("/auth/refresh")) {
            return chain.proceed(original)
        }

        val token = authManager.getAccessTokenSync()

        // Only clear session fail-fast if BOTH access token and 90-day refresh token are invalid/expired
        if (!token.isNullOrBlank() && authManager.isJwtExpired(token)) {
            val refreshToken = authManager.getRefreshTokenSync()
            if (refreshToken.isNullOrBlank() || authManager.isJwtExpired(refreshToken)) {
                authManager.clearSessionSync()
            }
        }

        val tenantId = authManager.getTenantIdSync()
        val request = if (token != null) {
            original.newBuilder()
                .header("Authorization", "Bearer $token")
                .apply {
                    if (!tenantId.isNullOrBlank()) {
                        header("X-Tenant-ID", tenantId)
                    }
                }
                .header("X-Request-ID", java.util.UUID.randomUUID().toString())
                .build()
        } else {
            original
        }

        val response = chain.proceed(request)

        // If 401 reaches back here (after OkHttp Authenticator tried refreshing or had no refresh token), clear session
        if (response.code == 401 && !path.contains("/auth/")) {
            authManager.clearSessionSync()
        }

        return response
    }
}
