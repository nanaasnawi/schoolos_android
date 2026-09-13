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
