package com.schoolos.android.core.common

import org.json.JSONObject
import retrofit2.HttpException
import java.io.IOException
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeoutException
import javax.net.ssl.SSLException

/**
 * Utility for converting raw system errors, network socket traces, HTTP status codes,
 * and technical exceptions into polite, clear, and human-friendly Indonesian messages.
 */
object ErrorHumanizer {

    private val IP_OR_PORT_REGEX = Regex("""(?:\d{1,3}\.){3}\d{1,3}(?::\d+)?|/69\.|:\d{2,5}""")
    private val HTTP_CODE_REGEX = Regex("""HTTP\s*(\d{3})""", RegexOption.IGNORE_CASE)
    private val EXCEPTION_NAME_REGEX = Regex("""[a-zA-Z0-9_.]*(?:Exception|Error):""")

    /**
     * Converts a [Throwable] into a human-friendly Indonesian error message.
     */
    fun humanize(throwable: Throwable?, fallback: String = "Terjadi kendala saat memproses data. Silakan coba beberapa saat lagi."): String {
        if (throwable == null) return fallback

        // 1. Direct Typed Exception Checking
        when (throwable) {
            is ConnectException, is NoRouteToHostException -> {
                return "Tidak dapat terhubung ke server sekolah. Pastikan internet aktif dan stabil, lalu coba kembali."
            }
            is SocketTimeoutException, is TimeoutException -> {
                return "Koneksi ke server terputus karena waktu tunggu habis. Mohon periksa sinyal internet Anda lalu coba lagi."
            }
            is UnknownHostException -> {
                return "Gagal menjangkau server sekolah. Periksa koneksi internet atau pengaturan jaringan perangkat Anda."
            }
            is SSLException -> {
                return "Koneksi aman ke server terganggu. Pastikan tanggal dan waktu pada perangkat Anda sudah akurat."
            }
            is HttpException -> {
                // Try extracting server-sent message from JSON body first
                val serverMsg = extractHttpErrorMessage(throwable)
                if (!serverMsg.isNullOrBlank()) {
                    val humanizedServerMsg = humanize(serverMsg, fallback = "")
                    if (humanizedServerMsg.isNotBlank()) return humanizedServerMsg
                }
                return humanizeHttpCode(throwable.code())
            }
        }

        // 2. Fall back to string message analysis
        val rawMessage = throwable.message.orEmpty()
        return humanize(rawMessage, fallback)
    }

    /**
     * Converts a raw error string (e.g. from state or exception) into a human-friendly Indonesian message.
     */
    fun humanize(rawMessage: String?, fallback: String = "Terjadi kendala saat memproses data. Silakan coba beberapa saat lagi."): String {
        if (rawMessage.isNullOrBlank()) return fallback
        val msg = rawMessage.trim()

        // 1. Detect HTTP Status Codes embedded in string (e.g. "HTTP 401", "HTTP 500 Internal Server Error")
        val httpMatch = HTTP_CODE_REGEX.find(msg)
        if (httpMatch != null) {
            val code = httpMatch.groupValues[1].toIntOrNull()
            if (code != null) {
                return humanizeHttpCode(code)
            }
        }

        // 2. Network & Socket Connection Issues
        if (msg.contains("Failed to connect", ignoreCase = true) ||
            msg.contains("Connection refused", ignoreCase = true) ||
            msg.contains("ECONNREFUSED", ignoreCase = true) ||
            msg.contains("CLEARTEXT", ignoreCase = true) ||
            msg.contains("Network is unreachable", ignoreCase = true) ||
            msg.contains("No route to host", ignoreCase = true) ||
            IP_OR_PORT_REGEX.containsMatchIn(msg)
        ) {
            return "Tidak dapat terhubung ke server sekolah. Pastikan internet aktif dan stabil, lalu coba kembali."
        }

        // 3. Timeouts
        if (msg.contains("timeout", ignoreCase = true) ||
            msg.contains("timed out", ignoreCase = true)
        ) {
            return "Waktu koneksi ke server habis. Mohon periksa kestabilan jaringan internet Anda lalu coba lagi."
        }

        // 4. Host Resolution / DNS
        if (msg.contains("Unable to resolve host", ignoreCase = true) ||
            msg.contains("No address associated", ignoreCase = true) ||
            msg.contains("UnknownHost", ignoreCase = true)
        ) {
            return "Gagal menjangkau server sekolah. Periksa koneksi internet atau pengaturan jaringan perangkat Anda."
        }

        // 5. Auth & Credentials
        if (msg.contains("invalid credentials", ignoreCase = true) ||
            msg.contains("invalid_grant", ignoreCase = true) ||
            msg.contains("password salah", ignoreCase = true) ||
            msg.contains("kata sandi salah", ignoreCase = true) ||
            msg.contains("unauthorized", ignoreCase = true)
        ) {
            return "Nama pengguna atau kata sandi tidak cocok. Silakan periksa kembali akun Anda."
        }

        if (msg.contains("token expired", ignoreCase = true) ||
            msg.contains("token invalid", ignoreCase = true) ||
            msg.contains("session expired", ignoreCase = true)
        ) {
            return "Sesi masuk akun Anda telah berakhir. Silakan masuk kembali."
        }

        if (msg.contains("forbidden", ignoreCase = true) ||
            msg.contains("access denied", ignoreCase = true)
        ) {
            return "Akses tidak diizinkan. Akun Anda tidak memiliki hak akses untuk tindakan ini."
        }

        if (msg.contains("user not found", ignoreCase = true) ||
            msg.contains("account not found", ignoreCase = true)
        ) {
            return "Akun pengguna tidak ditemukan di data sekolah."
        }

        // 6. Generic English System Fallbacks
        if (msg.startsWith("Failed to load", ignoreCase = true) ||
            msg.startsWith("Failed to fetch", ignoreCase = true) ||
            msg.startsWith("Failed to get", ignoreCase = true)
        ) {
            return "Gagal memuat data dari server sekolah. Silakan coba lagi."
        }

        // 7. Technical Exceptions & Stack Traces Leakage (e.g. "java.lang.NullPointerException", "Retrofit error")
        if (EXCEPTION_NAME_REGEX.containsMatchIn(msg) ||
            msg.contains("NullPointerException", ignoreCase = true) ||
            msg.contains("IllegalStateException", ignoreCase = true) ||
            msg.contains("JsonSyntaxException", ignoreCase = true) ||
            msg.contains("SerializationException", ignoreCase = true)
        ) {
            return fallback
        }

        // 8. If message is already a natural human sentence without technical leaks, return it
        return msg
    }

    private fun humanizeHttpCode(code: Int): String = when (code) {
        400 -> "Permintaan tidak dapat diproses. Silakan periksa kembali kelengkapan data."
        401 -> "Nama pengguna atau kata sandi tidak cocok. Silakan periksa kembali akun Anda."
        403 -> "Akses tidak diizinkan. Akun Anda tidak memiliki izin untuk fitur ini."
        404 -> "Data atau layanan yang diminta tidak ditemukan di server sekolah."
        408 -> "Waktu permintaan ke server habis. Silakan coba beberapa saat lagi."
        409 -> "Terjadi bentrokan data di server. Silakan muat ulang halaman."
        422 -> "Format data tidak valid. Silakan periksa kembali isian formulir Anda."
        429 -> "Terlalu banyak permintaan dalam waktu singkat. Mohon tunggu sejenak."
        500, 502, 503, 504 -> "Layanan server sekolah sedang sibuk atau dalam pemeliharaan. Silakan coba kembali beberapa saat lagi."
        else -> "Terjadi kendala pada server (Kode: $code). Silakan coba beberapa saat lagi."
    }

    private fun extractHttpErrorMessage(httpException: HttpException): String? {
        return try {
            val body = httpException.response()?.errorBody()?.string() ?: return null
            if (body.startsWith("{") && body.endsWith("}")) {
                val json = JSONObject(body)
                val msg = json.optString("message").ifBlank {
                    json.optString("error").ifBlank {
                        json.optString("detail")
                    }
                }
                if (msg.isNotBlank()) msg else null
            } else null
        } catch (_: Exception) {
            null
        }
    }
}

/**
 * Extension function for [Throwable] to get a human-friendly Indonesian error message.
 */
fun Throwable?.toHumanReadableError(fallback: String = "Terjadi kendala saat memproses data. Silakan coba beberapa saat lagi."): String {
    return ErrorHumanizer.humanize(this, fallback)
}

/**
 * Extension function for [String] to get a human-friendly Indonesian error message.
 */
fun String?.toHumanReadableError(fallback: String = "Terjadi kendala saat memproses data. Silakan coba beberapa saat lagi."): String {
    return ErrorHumanizer.humanize(this, fallback)
}

