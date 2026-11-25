package com.schoolos.android.core

import com.schoolos.android.core.network.RefreshResult
import com.schoolos.android.core.network.RetryInterceptor
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SecurityIsolationTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun refreshResultParsesBothNestedDataAndRootCombinedFormats() {
        val nestedPayload = """
            {
              "success": true,
              "data": {
                "access_token": "acc_nested",
                "refresh_token": "ref_nested"
              }
            }
        """.trimIndent()
        val nested = json.decodeFromString<RefreshResult>(nestedPayload)
        assertEquals("acc_nested", nested.data?.accessTokenSnake)
        assertEquals("ref_nested", nested.data?.refreshTokenSnake)

        val rootPayload = """
            {
              "success": true,
              "access_token": "acc_root",
              "refresh_token": "ref_root"
            }
        """.trimIndent()
        val root = json.decodeFromString<RefreshResult>(rootPayload)
        assertEquals("acc_root", root.accessTokenSnake)
        assertEquals("ref_root", root.refreshTokenSnake)
    }

    @Test
    fun retryInterceptorBlocksUnsafeNonIdempotentMutations() {
        assertTrue(RetryInterceptor.shouldRetryRequest("GET", hasIdempotencyKey = false))
        assertTrue(RetryInterceptor.shouldRetryRequest("HEAD", hasIdempotencyKey = false))
        assertFalse(RetryInterceptor.shouldRetryRequest("POST", hasIdempotencyKey = false))
        assertFalse(RetryInterceptor.shouldRetryRequest("DELETE", hasIdempotencyKey = false))
        assertTrue(RetryInterceptor.shouldRetryRequest("POST", hasIdempotencyKey = true))
    }
}
