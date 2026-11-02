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

    private fun isJwtExpired(token: String?): Boolean {
        if (token.isNullOrBlank()) return true
        val parts = token.split(".")
        if (parts.size < 2) return true
        return try {
            val payloadBytes = Base64.decode(
                parts[1],
                Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP
            )
            val json = JSONObject(String(payloadBytes, Charsets.UTF_8))
            val exp = json.optLong("exp", 0L)
            if (exp > 0L) {
                val nowSeconds = System.currentTimeMillis() / 1000
                exp <= nowSeconds
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val path = original.url.encodedPath

        // Skip auth handling for public login / qr login endpoints
        if (path.contains("/auth/login") || path.contains("/auth/qr-login")) {
            return chain.proceed(original)
        }

        val token = authManager.getAccessTokenSync()

        // 1. Fail-fast: If access token is present but already expired,
        // and refresh token is also empty or expired, force-clear session immediately
        // to avoid sending zombie requests that trigger gateway timeouts.
        if (!token.isNullOrBlank() && isJwtExpired(token)) {
            val refreshToken = authManager.getRefreshTokenSync()
            if (refreshToken.isNullOrBlank() || isJwtExpired(refreshToken)) {
                authManager.clearSessionSync()
            }
        }

        val request = if (token != null) {
            original.newBuilder()
                .header("Authorization", "Bearer $token")
                .header("X-Request-ID", java.util.UUID.randomUUID().toString())
                .build()
        } else {
            original
        }

        val response = chain.proceed(request)

        // 2. Intercept HTTP 401 Unauthorized:
        // When backend returns 401 Unauthorized (and Authenticator either cannot refresh or refresh failed),
        // immediately clear local session and DataStore to trigger navigation to Screen.Auth/Login
        // and stop zombie requests from overwhelming the server.
        if (response.code == 401 && !path.contains("/auth/refresh")) {
            authManager.clearSessionSync()
        }

        return response
    }
}
