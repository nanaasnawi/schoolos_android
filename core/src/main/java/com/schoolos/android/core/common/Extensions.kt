package com.schoolos.android.core.common

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

fun String.toFormattedDate(inputPattern: String = "yyyy-MM-dd'T'HH:mm:ss'Z'", outputPattern: String = "MMM dd, yyyy"): String {
    return try {
        val inputFormatter = DateTimeFormatter.ofPattern(inputPattern, Locale.US)
        val outputFormatter = DateTimeFormatter.ofPattern(outputPattern, Locale.US)
        val dateTime = LocalDateTime.parse(this, inputFormatter)
        dateTime.format(outputFormatter)
    } catch (e: Exception) {
        this
    }
}

fun String.toReadableTime(): String {
    return toFormattedDate(outputPattern = "hh:mm a")
}

fun Double.toPercentage(): String = "${(this * 100).toInt()}%"

/**
 * Formats timestamps into Indonesian standard date & time: "d MMM yyyy, HH:mm WIB"
 * (e.g. "3 Okt 2026, 20:30 WIB") to show exact publication time for materials, assignments, and exams.
 */
fun formatPublishTimestamp(timestamp: String?): String {
    if (timestamp.isNullOrBlank()) return ""
    val trimmed = timestamp.trim()
    return try {
        val zoneJakarta = java.time.ZoneId.of("Asia/Jakarta")
        val instant: java.time.Instant = when {
            trimmed.toLongOrNull() != null -> {
                java.time.Instant.ofEpochMilli(trimmed.toLong())
            }
            trimmed.endsWith("Z") || trimmed.contains("+") || (trimmed.contains("-") && trimmed.count { it == '-' } > 2) -> {
                try {
                    java.time.OffsetDateTime.parse(trimmed).toInstant()
                } catch (_: Exception) {
                    java.time.Instant.parse(trimmed)
                }
            }
            trimmed.contains("T") -> {
                val cleanIso = trimmed.substringBefore(".")
                java.time.LocalDateTime.parse(cleanIso).atZone(zoneJakarta).toInstant()
            }
            trimmed.contains(" ") -> {
                val clean = trimmed.replace(' ', 'T').substringBefore(".")
                java.time.LocalDateTime.parse(clean).atZone(zoneJakarta).toInstant()
            }
            trimmed.length == 10 && trimmed.count { it == '-' } == 2 -> {
                java.time.LocalDate.parse(trimmed).atStartOfDay(zoneJakarta).toInstant()
            }
            else -> {
                java.time.Instant.parse(trimmed)
            }
        }
        val zdt = instant.atZone(zoneJakarta)
        val formatter = java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm 'WIB'", java.util.Locale("id", "ID"))
        zdt.format(formatter)
    } catch (_: Exception) {
        try {
            val normalized = timestamp.replace("T", " ").substringBefore(".")
            val parser = java.text.SimpleDateFormat(
                if (normalized.contains(":")) "yyyy-MM-dd HH:mm:ss" else "yyyy-MM-dd",
                java.util.Locale.US
            ).apply {
                timeZone = java.util.TimeZone.getTimeZone("UTC")
            }
            val date = parser.parse(normalized)
            if (date != null) {
                val outFormatter = java.text.SimpleDateFormat("d MMM yyyy, HH:mm 'WIB'", java.util.Locale("id", "ID")).apply {
                    timeZone = java.util.TimeZone.getTimeZone("Asia/Jakarta")
                }
                return outFormatter.format(date)
            }
        } catch (_: Exception) {}
        timestamp
    }
}

fun String?.toPublishedDateTime(): String = formatPublishTimestamp(this)


object DapodikPeriod {
    fun getActiveSemester(): String {
        val calendar = java.util.Calendar.getInstance()
        val month = calendar.get(java.util.Calendar.MONTH) // 0-based: 0=Jan, 6=Jul, 8=Sep, 11=Dec
        return if (month in java.util.Calendar.JULY..java.util.Calendar.DECEMBER) "Semester Ganjil" else "Semester Genap"
    }

    fun getAcademicYear(): String {
        val calendar = java.util.Calendar.getInstance()
        val month = calendar.get(java.util.Calendar.MONTH)
        val year = calendar.get(java.util.Calendar.YEAR)
        return if (month in java.util.Calendar.JULY..java.util.Calendar.DECEMBER) {
            "$year/${year + 1}"
        } else {
            "${year - 1}/$year"
        }
    }

    fun getFullPeriodLabel(): String = "${getActiveSemester()} • Tahun Ajaran ${getAcademicYear()}"
}
