package com.schoolos.android.core.network

import com.schoolos.android.core.common.toHumanReadableError
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.CancellationException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Interceptor that intercepts network connection failures (socket errors, timeouts, host resolution, SSL errors)
 * and ensures any thrown [IOException] contains a polite, humanized Indonesian error message instead of raw
 * socket addresses, ports, or technical stack traces.
 */
@Singleton
class NetworkErrorInterceptor @Inject constructor() : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        try {
            return chain.proceed(request)
        } catch (e: Exception) {
            if (e is CancellationException || e.message?.contains("Canceled", ignoreCase = true) == true) {
                throw e
            }
            val humanMessage = e.toHumanReadableError()
            throw IOException(humanMessage, e)
        }
    }
}

