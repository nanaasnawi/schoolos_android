package com.schoolos.android.core.network

import com.schoolos.android.core.auth.AuthManager
import com.schoolos.android.core.common.BuildConfig
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

class DynamicHostInterceptor(
    private val authManager: AuthManager,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val customUrl = authManager.getCustomServerUrlSync()

        var primaryRequest = originalRequest
        if (!customUrl.isNullOrBlank()) {
            val targetHttpUrl = customUrl.toHttpUrlOrNull()
            if (targetHttpUrl != null) {
                val isLocalIp = targetHttpUrl.host.startsWith("192.168.")
                    || targetHttpUrl.host.startsWith("10.")
                    || targetHttpUrl.host == "127.0.0.1"
                    || targetHttpUrl.host == "10.0.2.2"
                val originalIsHttps = originalRequest.url.isHttps

                // Only apply customUrl if original is not HTTPS, or customUrl is also a remote host
                if (!(originalIsHttps && isLocalIp)) {
                    val newUrl = originalRequest.url.newBuilder()
                        .scheme(targetHttpUrl.scheme)
                        .host(targetHttpUrl.host)
                        .port(targetHttpUrl.port)
                        .build()
                    primaryRequest = originalRequest.newBuilder().url(newUrl).build()
                }
            }
        }

        return chain.proceed(primaryRequest)
    }
}

@Singleton
class ApiClient @Inject constructor(
    private val authManager: AuthManager,
    private val maintenanceManager: MaintenanceManager,
) {
    val json: Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        encodeDefaults = true
    }

    val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .dns(ResilientDns())
            .addInterceptor(DynamicHostInterceptor(authManager))
            .addInterceptor(MaintenanceInterceptor(maintenanceManager))
            .addInterceptor(AuthInterceptor(authManager))
            .addInterceptor(RetryInterceptor())
            .authenticator(TokenRefreshInterceptor(authManager))
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = if (BuildConfig.DEBUG) {
                        HttpLoggingInterceptor.Level.BODY
                    } else {
                        HttpLoggingInterceptor.Level.NONE
                    }
                }
            )
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .certificatePinner(CertificatePinnerFactory.create())
            .build()
    }

    val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(httpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    inline fun <reified T> create(): T = retrofit.create(T::class.java)
}
