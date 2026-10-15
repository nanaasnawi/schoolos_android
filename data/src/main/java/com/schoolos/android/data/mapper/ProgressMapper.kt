package com.schoolos.android.data.mapper

import com.schoolos.android.data.remote.dto.*
import com.schoolos.android.domain.model.*

fun ProgressDto.toDomain() = Progress(
    id = id,
    overallProgress = overallProgress,
    lessonCompleted = lessonCompleted,
    lessonTotal = lessonTotal,
    assignmentCompleted = assignmentCompleted,
    assignmentTotal = assignmentTotal,
    quizCompleted = quizCompleted,
    quizTotal = quizTotal,
    sessionAttended = sessionAttended,
    sessionTotal = sessionTotal,
    subjectName = subjectName,
    calculatedAt = calculatedAt,
    teacherNotes = teacherNotes,
    teacherName = teacherName,
    className = className,
    academicStatus = academicStatus,
)

fun WeeklyActivityDto.toDomain() = WeeklyActivity(
    studentId = studentId,
    studentName = studentName,
    weekStartDate = weekStartDate,
    weekEndDate = weekEndDate,
    totalActivities = totalActivities,
    totalMaterials = totalMaterials,
    totalAssignments = totalAssignments,
    totalQuizzes = totalQuizzes,
    peakDay = peakDay,
    peakCount = peakCount,
    activeDaysCount = activeDaysCount,
    days = days.map { it.toDomain() },
)

fun DailyActivityStatDto.toDomain() = DailyActivityStat(
    date = date,
    dayName = dayName,
    dayShort = dayShort,
    dayNumber = dayNumber,
    isToday = isToday,
    materialsCount = materialsCount,
    assignmentsCount = assignmentsCount,
    quizzesCount = quizzesCount,
    totalCount = totalCount,
    activities = activities.map { it.toDomain() },
)

fun DailyActivityItemDto.toDomain() = DailyActivityItem(
    id = id,
    title = title,
    category = category,
    categoryLabel = categoryLabel,
    subType = subType,
    subjectName = subjectName,
    time = time,
)

