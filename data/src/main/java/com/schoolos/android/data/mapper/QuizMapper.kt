package com.schoolos.android.data.mapper

import com.schoolos.android.data.remote.dto.AttemptAnswerDetailDto
import com.schoolos.android.data.remote.dto.ProctorEventDto
import com.schoolos.android.data.remote.dto.ProctorMonitorDto
import com.schoolos.android.data.remote.dto.ProctorMonitorSummaryDto
import com.schoolos.android.data.remote.dto.ProctorParticipantRowDto
import com.schoolos.android.data.remote.dto.ProctorScheduleConflictDto
import com.schoolos.android.data.remote.dto.ProctorScheduleDashboardDto
import com.schoolos.android.data.remote.dto.ProctorScheduleSummaryDto
import com.schoolos.android.data.remote.dto.ProctorSessionOverviewDto
import com.schoolos.android.data.remote.dto.QuizAttemptDto
import com.schoolos.android.data.remote.dto.QuizChoiceDto
import com.schoolos.android.data.remote.dto.QuizDto
import com.schoolos.android.data.remote.dto.QuizQuestionDto
import com.schoolos.android.domain.model.Quiz
import com.schoolos.android.domain.model.QuizAttempt
import com.schoolos.android.domain.model.QuizChoice
import com.schoolos.android.domain.model.QuizProctorEventLog
import com.schoolos.android.domain.model.QuizProctorMonitor
import com.schoolos.android.domain.model.QuizProctorMonitorSummary
import com.schoolos.android.domain.model.QuizProctorParticipantRow
import com.schoolos.android.domain.model.QuizProctorScheduleConflict
import com.schoolos.android.domain.model.QuizProctorScheduleDashboard
import com.schoolos.android.domain.model.QuizProctorScheduleSummary
import com.schoolos.android.domain.model.QuizProctorSessionOverview
import com.schoolos.android.domain.model.QuizQuestion

fun QuizDto.toDomain() = Quiz(
    id = id,
    title = title,
    description = description,
    timeLimitMinutes = timeLimitMinutes ?: durationMinutes,
    passingScore = passingScore,
    maxScore = maxScore,
    questionsCount = questionsCount,
    status = status,
    isActive = isActive,
    createdAt = createdAt,
    updatedAt = updatedAt,
    subjectName = subjectName,
    classId = classId,
    className = className,
    teacherId = teacherId,
    teacherName = teacherName,
    sessionId = sessionId,
    examMode = examMode,
    assessmentCategory = assessmentCategory,
    examToken = examToken,
    requiresToken = requiresToken || examMode == "PROCTORED_CBT",
    tokenExpiresAt = tokenExpiresAt,
    maxTokenAttempts = maxTokenAttempts,
    maxAttempts = maxAttempts,
    startAt = startAt,
    endAt = endAt,
    studentAttemptStatus = studentAttemptStatus,
    studentAttemptsCount = studentAttemptsCount ?: 0,
    studentHasCompleted = studentHasCompleted ?: (studentAttemptStatus?.lowercase() in listOf("completed", "submitted", "graded")),
    studentLastScore = studentLastScore,
    studentLastAttemptId = studentLastAttemptId,
    releaseAt = releaseAt,
    examPhase = examPhase,
    isOpenForStudent = isOpenForStudent,
    secondsUntilStart = secondsUntilStart,
    secondsRemaining = secondsRemaining,
)

fun AttemptAnswerDetailDto.toDomain() = com.schoolos.android.domain.model.AttemptAnswerDetail(
    questionId = questionId,
    questionText = questionText,
    questionType = questionType,
    maxPoints = maxPoints,
    chosenChoiceId = chosenChoiceId,
    chosenChoiceText = chosenChoiceText,
    isCorrect = isCorrect,
    textAnswer = textAnswer,
    pointsEarned = pointsEarned,
    teacherFeedback = teacherFeedback,
)

fun QuizAttemptDto.toDomain() = QuizAttempt(
    id = id,
    quizId = quizId,
    studentId = studentId,
    startedAt = startedAt,
    completedAt = completedAt,
    score = score,
    totalPoints = totalPoints,
    percentage = percentage,
    passed = passed,
    status = status,
    studentName = studentName,
    studentNisn = studentNisn,
    createdAt = createdAt ?: "",
    updatedAt = updatedAt ?: "",
    answers = answers.map { it.toDomain() },
)

fun QuizQuestionDto.toDomain() = QuizQuestion(
    id = id,
    questionText = questionText,
    questionType = questionType,
    points = points,
    orderIndex = orderIndex,
    imageUrl = imageUrl,
    choices = choices.map { it.toDomain() },
    interactionConfigJson = interactionConfig?.toString(),
)

fun QuizChoiceDto.toDomain() = QuizChoice(
    id = id,
    choiceText = choiceText,
    orderIndex = orderIndex,
    isCorrect = isCorrect,
)

fun ProctorScheduleSummaryDto.toDomain() = QuizProctorScheduleSummary(
    totalSessions = totalSessions,
    liveCount = liveCount,
    upcomingCount = upcomingCount,
    endedCount = endedCount,
    draftCount = draftCount,
    conflictCount = conflictCount,
    flaggedStudentsTotal = flaggedStudentsTotal,
)

fun ProctorScheduleConflictDto.toDomain() = QuizProctorScheduleConflict(
    conflictType = conflictType,
    primaryQuizId = primaryQuizId,
    primaryTitle = primaryTitle,
    conflictingQuizId = conflictingQuizId,
    conflictingTitle = conflictingTitle,
    scopeLabel = scopeLabel,
    overlapStart = overlapStart,
    overlapEnd = overlapEnd,
)

fun ProctorSessionOverviewDto.toDomain() = QuizProctorSessionOverview(
    id = id,
    title = title,
    subjectName = subjectName,
    classId = classId,
    className = className,
    teacherId = teacherId,
    teacherName = teacherName,
    examMode = examMode,
    assessmentCategory = assessmentCategory,
    status = status,
    examPhase = examPhase,
    durationMinutes = durationMinutes,
    questionsCount = questionsCount,
    passingScore = passingScore,
    examToken = examToken,
    requiresToken = requiresToken,
    tokenExpiresAt = tokenExpiresAt,
    startAt = startAt,
    endAt = endAt,
    releaseAt = releaseAt,
    secondsUntilStart = secondsUntilStart,
    secondsRemaining = secondsRemaining,
    totalEnrolled = totalEnrolled,
    startedCount = startedCount,
    inProgressCount = inProgressCount,
    needGradingCount = needGradingCount,
    completedCount = completedCount,
    notStartedCount = notStartedCount,
    totalViolations = totalViolations,
    flaggedStudentsCount = flaggedStudentsCount,
    hasConflict = hasConflict,
)

fun ProctorScheduleDashboardDto.toDomain() = QuizProctorScheduleDashboard(
    serverNow = serverNow,
    summary = summary.toDomain(),
    conflicts = conflicts.map { it.toDomain() },
    sessions = sessions.map { it.toDomain() },
)

fun ProctorMonitorSummaryDto.toDomain() = QuizProctorMonitorSummary(
    totalEnrolled = totalEnrolled,
    notStartedCount = notStartedCount,
    inProgressCount = inProgressCount,
    needGradingCount = needGradingCount,
    completedCount = completedCount,
    flaggedCount = flaggedCount,
    totalViolations = totalViolations,
)

fun ProctorParticipantRowDto.toDomain() = QuizProctorParticipantRow(
    studentId = studentId,
    userId = userId,
    studentName = studentName,
    studentNisn = studentNisn,
    className = className,
    attemptId = attemptId,
    rawAttemptStatus = rawAttemptStatus,
    proctorStatus = proctorStatus,
    startedAt = startedAt,
    completedAt = completedAt,
    elapsedSeconds = elapsedSeconds,
    answeredCount = answeredCount,
    totalQuestions = totalQuestions,
    score = score,
    totalPoints = totalPoints,
    percentage = percentage,
    passed = passed,
    leaveViolationsCount = leaveViolationsCount,
    lastViolationAt = lastViolationAt,
    lastHeartbeatAt = lastHeartbeatAt,
    isOnline = isOnline,
    lastDevicePlatform = lastDevicePlatform,
)

fun ProctorEventDto.toDomain() = QuizProctorEventLog(
    id = id,
    studentId = studentId,
    studentName = studentName,
    attemptId = attemptId,
    eventType = eventType,
    detail = detail,
    devicePlatform = devicePlatform,
    createdAt = createdAt,
)

fun ProctorMonitorDto.toDomain() = QuizProctorMonitor(
    serverNow = serverNow,
    quiz = quiz.toDomain(),
    summary = summary.toDomain(),
    participants = participants.map { it.toDomain() },
    recentEvents = recentEvents.map { it.toDomain() },
)
