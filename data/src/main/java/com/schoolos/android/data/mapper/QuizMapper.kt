package com.schoolos.android.data.mapper

import com.schoolos.android.data.remote.dto.AttemptAnswerDetailDto
import com.schoolos.android.data.remote.dto.QuizAttemptDto
import com.schoolos.android.data.remote.dto.QuizChoiceDto
import com.schoolos.android.data.remote.dto.QuizDto
import com.schoolos.android.data.remote.dto.QuizQuestionDto
import com.schoolos.android.domain.model.Quiz
import com.schoolos.android.domain.model.QuizAttempt
import com.schoolos.android.domain.model.QuizChoice
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
    maxAttempts = maxAttempts,
    studentAttemptStatus = studentAttemptStatus,
    studentAttemptsCount = studentAttemptsCount ?: 0,
    studentHasCompleted = studentHasCompleted ?: (studentAttemptStatus?.lowercase() in listOf("completed", "submitted", "graded")),
    studentLastScore = studentLastScore,
    studentLastAttemptId = studentLastAttemptId,
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
)

fun QuizChoiceDto.toDomain() = QuizChoice(
    id = id,
    choiceText = choiceText,
    orderIndex = orderIndex,
)
