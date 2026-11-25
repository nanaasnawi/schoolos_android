package com.schoolos.android.domain.model

data class Assignment(
    val id: String,
    val title: String,
    val description: String?,
    val instructions: String?,
    val maxScore: Int,
    val dueAt: String?,
    val assignmentType: String,
    val status: String,
    val isActive: Boolean,
    val createdAt: String,
    val updatedAt: String,
    val materials: List<LearningMaterial> = emptyList(),
    val classId: String? = null,
    val className: String? = null,
    val subjectName: String? = null,
    val teacherName: String? = null,
    val questions: List<AssignmentQuestion> = emptyList(),
    val releaseAt: String? = null,
    val hasGameQuestions: Boolean = false,
    val totalStudents: Int = 0,
    val submittedCount: Int = 0,
    val gradedCount: Int = 0,
    val ungradedCount: Int = 0,
    val isPastDue: Boolean = false,
) {
    val hasAnyGame: Boolean
        get() = hasGameQuestions || questions.any {
            it.questionType.uppercase() in setOf("MATCHING", "ORDERING", "FILL_BLANK")
        }

    val isScheduled: Boolean
        get() = status.equals("draft", ignoreCase = true) || (!releaseAt.isNullOrBlank() && try {
            java.time.Instant.parse(releaseAt).isAfter(java.time.Instant.now())
        } catch (_: Exception) { false })

    val isExpiredOrClosed: Boolean
        get() = isPastDue ||
            status.equals("closed", ignoreCase = true) ||
            status.equals("archived", ignoreCase = true) ||
            (!dueAt.isNullOrBlank() && try {
                java.time.Instant.parse(dueAt).isBefore(java.time.Instant.now())
            } catch (_: Exception) {
                try {
                    java.time.ZonedDateTime.parse(dueAt).toInstant().isBefore(java.time.Instant.now())
                } catch (_: Exception) {
                    try {
                        java.time.LocalDate.parse(dueAt.substringBefore("T").substringBefore(" "))
                            .isBefore(java.time.LocalDate.now(java.time.ZoneId.of("Asia/Jakarta")))
                    } catch (_: Exception) { false }
                }
            })

    val effectiveUngradedCount: Int
        get() = if (ungradedCount > 0) ungradedCount else (submittedCount - gradedCount).coerceAtLeast(0)

    val isReadyToGrade: Boolean
        get() = !isScheduled && effectiveUngradedCount > 0
}
