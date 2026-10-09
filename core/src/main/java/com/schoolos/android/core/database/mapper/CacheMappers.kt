package com.schoolos.android.core.database.mapper

import com.schoolos.android.core.database.entity.AssignmentEntity
import com.schoolos.android.core.database.entity.NotificationEntity
import com.schoolos.android.core.database.entity.QuizEntity
import com.schoolos.android.domain.model.Assignment
import com.schoolos.android.domain.model.Notification
import com.schoolos.android.domain.model.Quiz

fun Assignment.toEntity() = AssignmentEntity(
    id = id,
    title = title,
    description = description,
    instructions = instructions,
    maxScore = maxScore,
    dueAt = dueAt,
    assignmentType = assignmentType,
    status = status,
    isActive = isActive,
    createdAt = createdAt,
    updatedAt = updatedAt,
    classId = classId,
    className = className,
    subjectName = subjectName,
    teacherName = teacherName,
)

fun AssignmentEntity.toDomain() = Assignment(
    id = id,
    title = title,
    description = description,
    instructions = instructions,
    maxScore = maxScore,
    dueAt = dueAt,
    assignmentType = assignmentType,
    status = status,
    isActive = isActive,
    createdAt = createdAt,
    updatedAt = updatedAt,
    classId = classId,
    className = className,
    subjectName = subjectName,
    teacherName = teacherName,
)

fun Quiz.toEntity() = QuizEntity(
    id = id,
    title = title,
    description = description,
    timeLimitMinutes = timeLimitMinutes,
    passingScore = passingScore,
    maxScore = maxScore,
    questionsCount = questionsCount,
    status = status,
    isActive = isActive,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun QuizEntity.toDomain() = Quiz(
    id = id,
    title = title,
    description = description,
    timeLimitMinutes = timeLimitMinutes,
    passingScore = passingScore,
    maxScore = maxScore,
    questionsCount = questionsCount,
    status = status,
    isActive = isActive,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun Notification.toEntity(userId: String) = NotificationEntity(
    id = id,
    userId = userId,
    title = title,
    body = body,
    notificationType = notificationType,
    channel = channel,
    referenceType = referenceType,
    referenceId = referenceId,
    isRead = isRead,
    readAt = readAt,
    createdAt = createdAt,
)

fun NotificationEntity.toDomain() = Notification(
    id = id,
    userId = userId,
    title = title,
    body = body,
    notificationType = notificationType,
    channel = channel,
    referenceType = referenceType,
    referenceId = referenceId,
    isRead = isRead,
    readAt = readAt,
    createdAt = createdAt,
)

fun com.schoolos.android.domain.model.LearningSession.toEntity() = com.schoolos.android.core.database.entity.SessionEntity(
    id = id,
    lessonId = lessonId,
    sessionType = sessionType,
    scheduleId = scheduleId,
    subjectId = subjectId,
    classId = classId,
    teacherId = teacherId,
    substituteTeacherId = substituteTeacherId,
    substituteTeacherName = substituteTeacherName,
    sessionDate = sessionDate,
    sessionNumber = sessionNumber,
    startTime = startTime,
    endTime = endTime,
    scheduledAt = scheduledAt,
    startedAt = startedAt,
    endedAt = endedAt,
    status = status,
    notes = notes,
    cancellationReason = cancellationReason,
    subjectName = subjectName,
    teacherName = teacherName,
    className = className,
    room = room,
)

fun com.schoolos.android.core.database.entity.SessionEntity.toDomain() = com.schoolos.android.domain.model.LearningSession(
    id = id,
    lessonId = lessonId,
    sessionType = sessionType,
    scheduleId = scheduleId,
    subjectId = subjectId,
    classId = classId,
    teacherId = teacherId,
    substituteTeacherId = substituteTeacherId,
    substituteTeacherName = substituteTeacherName,
    sessionDate = sessionDate,
    sessionNumber = sessionNumber,
    startTime = startTime,
    endTime = endTime,
    scheduledAt = scheduledAt,
    startedAt = startedAt,
    endedAt = endedAt,
    status = status,
    notes = notes,
    cancellationReason = cancellationReason,
    subjectName = subjectName,
    teacherName = teacherName,
    className = className,
    room = room,
)

fun com.schoolos.android.domain.model.GradeEntry.toEntity() = com.schoolos.android.core.database.entity.GradeEntity(
    id = id,
    studentId = studentId,
    classId = classId,
    subjectId = subjectId,
    componentName = componentName,
    sourceType = sourceType,
    rawScore = rawScore,
    maxRawScore = maxRawScore,
    weightedScore = weightedScore,
    weightPercentage = weightPercentage,
    calculatedAt = calculatedAt,
)

fun com.schoolos.android.core.database.entity.GradeEntity.toDomain() = com.schoolos.android.domain.model.GradeEntry(
    id = id,
    studentId = studentId,
    classId = classId,
    subjectId = subjectId,
    componentName = componentName,
    sourceType = sourceType,
    rawScore = rawScore,
    maxRawScore = maxRawScore,
    weightedScore = weightedScore,
    weightPercentage = weightPercentage,
    calculatedAt = calculatedAt,
)

fun com.schoolos.android.domain.model.SessionAttendance.toEntity() = com.schoolos.android.core.database.entity.SessionAttendanceEntity(
    id = id,
    sessionId = sessionId,
    studentId = studentId,
    status = status,
    checkedInAt = checkedInAt,
    notes = notes,
)

fun com.schoolos.android.core.database.entity.SessionAttendanceEntity.toDomain() = com.schoolos.android.domain.model.SessionAttendance(
    id = id,
    sessionId = sessionId,
    studentId = studentId,
    status = status,
    checkedInAt = checkedInAt,
    notes = notes,
)

