package com.schoolos.android.domain.model

data class Quiz(
    val id: String,
    val title: String,
    val description: String?,
    val timeLimitMinutes: Int?,
    val passingScore: Int,
    val maxScore: Int,
    val questionsCount: Int,
    val status: String,
    val isActive: Boolean,
    val createdAt: String,
    val updatedAt: String,
    val subjectName: String? = null,
    val classId: String? = null,
    val className: String? = null,
    val teacherId: String? = null,
    val teacherName: String? = null,
    val sessionId: String? = null,
    val examMode: String = "HOMEWORK_QUIZ",
    val assessmentCategory: String? = null,
    val examToken: String? = null,
    val requiresToken: Boolean = false,
    val tokenExpiresAt: String? = null,
    val maxTokenAttempts: Int = 5,
    val maxAttempts: Int = 1,
    val startAt: String? = null,
    val endAt: String? = null,
    val studentAttemptStatus: String? = null,
    val studentAttemptsCount: Int = 0,
    val studentHasCompleted: Boolean = false,
    val studentLastScore: Int? = null,
    val studentLastAttemptId: String? = null,
    val releaseAt: String? = null,
    val examPhase: String? = null,
    val isOpenForStudent: Boolean? = null,
    val secondsUntilStart: Long? = null,
    val secondsRemaining: Long? = null,
) {
    val isScheduled: Boolean
        get() = if (!examPhase.isNullOrBlank()) {
            examPhase.equals("UPCOMING", ignoreCase = true) || examPhase.equals("DRAFT", ignoreCase = true)
        } else {
            status.equals("draft", ignoreCase = true) || (!(releaseAt ?: startAt).isNullOrBlank() && try {
                java.time.Instant.parse(releaseAt ?: startAt).isAfter(java.time.Instant.now())
            } catch (_: Exception) { false })
        }

    val effectivePhase: String
        get() {
            val rawPhase = examPhase?.trim()?.uppercase()
            if (rawPhase in listOf("UPCOMING", "LIVE", "ENDED", "DRAFT")) {
                return rawPhase!!
            }
            val lowerStatus = status.trim().lowercase()
            if (lowerStatus in listOf("closed", "ended", "archived") || hasExpired) {
                return "ENDED"
            }
            val effectiveStart = releaseAt ?: startAt
            val startInFuture = !effectiveStart.isNullOrBlank() && try {
                java.time.Instant.parse(effectiveStart).isAfter(java.time.Instant.now())
            } catch (_: Exception) { false }
            return when {
                lowerStatus == "draft" && questionsCount <= 0 && !startInFuture -> "DRAFT"
                startInFuture || lowerStatus == "scheduled" -> "UPCOMING"
                lowerStatus in listOf("published", "active", "open", "live_exam", "live") -> "LIVE"
                else -> "DRAFT"
            }
        }

    val effectiveDisplayClass: String?
        get() = className?.takeIf { it.isNotBlank() }
            ?: Regex("PAKET\\s+[ABC]\\s+\\d+", RegexOption.IGNORE_CASE)
                .find("$title ${description.orEmpty()}")?.value?.uppercase()

    val hasExpired: Boolean
        get() = if (!examPhase.isNullOrBlank()) {
            examPhase.equals("ENDED", ignoreCase = true)
        } else {
            !endAt.isNullOrBlank() && try {
                java.time.Instant.parse(endAt).isBefore(java.time.Instant.now())
            } catch (_: Exception) { false }
        }

    val isCurrentlyOpenForExam: Boolean
        get() = isOpenForStudent ?: (
            isActive &&
                (status.equals("published", ignoreCase = true) ||
                    status.equals("active", ignoreCase = true) ||
                    status.equals("open", ignoreCase = true)) &&
                !isScheduled &&
                !hasExpired &&
                !studentHasCompleted &&
                studentAttemptStatus?.lowercase() !in listOf("completed", "submitted", "graded")
        )
}
