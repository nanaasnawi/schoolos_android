package com.schoolos.android.core.network

import okhttp3.Interceptor
import okhttp3.Response
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Singleton
class RetryInterceptor : Interceptor {

    companion object {
        private const val MAX_RETRIES = 1

        fun shouldRetryRequest(method: String, hasIdempotencyKey: Boolean): Boolean {
            return method.equals("GET", ignoreCase = true) ||
                method.equals("HEAD", ignoreCase = true) ||
                method.equals("OPTIONS", ignoreCase = true) ||
                hasIdempotencyKey
        }
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        // Never blindly retry non-idempotent mutations (POST/PATCH/DELETE) unless caller supplied an idempotency key.
        val hasIdempotencyKey = !request.header("X-Idempotency-Key").isNullOrBlank() ||
            !request.header("Idempotency-Key").isNullOrBlank()
        val isSafeToRetry = shouldRetryRequest(request.method, hasIdempotencyKey)

        if (!isSafeToRetry) {
            return chain.proceed(request)
        }

        var response: Response? = null
        var attempt = 0

        while (attempt <= MAX_RETRIES) {
            try {
                response = chain.proceed(request)
                // Never retry successful requests or client errors (4xx, e.g. 401 Unauthorized, 400, 403, 404)
                if (response.isSuccessful || response.code in 400..499 || attempt == MAX_RETRIES) {
                    return response
                }
                response.close()
            } catch (e: Exception) {
                if (attempt == MAX_RETRIES) {
                    throw e
                }
            }
            attempt++
            if (attempt <= MAX_RETRIES) {
                TimeUnit.MILLISECONDS.sleep(500L)
            }
        }

        return response ?: chain.proceed(request)
    }
}
