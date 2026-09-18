package com.schoolos.android.core.network

import okhttp3.Dns
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.concurrent.ConcurrentHashMap

/**
 * Resilient DNS resolver for School OS Android client.
 *
 * Prevents "Unable to resolve host: No address associated with hostname" errors
 * caused by intermittent mobile connectivity, DNS timeouts, or Indonesian ISP
 * DNS filtering (TrustPositif / Nawala) on *.up.railway.app domains.
 *
 * 1. Queries standard Android System DNS.
 * 2. Caches successful lookups in memory.
 * 3. Falls back to known IP (Railway Edge 69.46.46.64) or cached addresses if System DNS fails.
 */
class ResilientDns : Dns {
    private val addressCache = ConcurrentHashMap<String, List<InetAddress>>().apply {
        try {
            // Known Anycast IP for Railway Edge (Singapore / Global)
            put(
                "schoolosbackend-production.up.railway.app",
                listOf(InetAddress.getByName("69.46.46.64"))
            )
        } catch (_: Exception) {
            // Ignored if IP parsing fails
        }
    }

    override fun lookup(hostname: String): List<InetAddress> {
        // 1. Try Android System DNS
        try {
            val addresses = Dns.SYSTEM.lookup(hostname)
            if (addresses.isNotEmpty()) {
                addressCache[hostname] = addresses
                return addresses
            }
        } catch (_: UnknownHostException) {
            // System DNS failed, check fallback below
        }

        // 2. Use cached or hardcoded fallback
        val fallback = addressCache[hostname]
        if (!fallback.isNullOrEmpty()) {
            return fallback
        }

        // 3. Fallback to default behavior to throw standard UnknownHostException
        return Dns.SYSTEM.lookup(hostname)
    }
}
