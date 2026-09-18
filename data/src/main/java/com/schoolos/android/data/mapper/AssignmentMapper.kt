package com.schoolos.android.data.mapper

import com.schoolos.android.data.remote.dto.AssignmentChoiceDto
import com.schoolos.android.data.remote.dto.AssignmentDto
import com.schoolos.android.data.remote.dto.AssignmentQuestionDto
import com.schoolos.android.data.remote.dto.SubmissionAnswerDetailDto
import com.schoolos.android.data.remote.dto.SubmissionDto
import com.schoolos.android.domain.model.Assignment
import com.schoolos.android.domain.model.AssignmentChoice
import com.schoolos.android.domain.model.AssignmentQuestion
import com.schoolos.android.domain.model.AssignmentSubmission
import com.schoolos.android.domain.model.SubmissionAnswer

fun AssignmentChoiceDto.toDomain() = AssignmentChoice(
    id = id,
    choiceText = choiceText,
    isCorrect = isCorrect,
    orderIndex = orderIndex,
)

fun AssignmentQuestionDto.toDomain() = AssignmentQuestion(
    id = id,
    questionText = questionText,
    questionType = questionType,
    points = points,
    orderIndex = orderIndex,
    choices = choices.map { it.toDomain() },
)

fun SubmissionAnswerDetailDto.toDomain() = SubmissionAnswer(
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

fun AssignmentDto.toDomain() = Assignment(
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
    questions = questions.map { it.toDomain() },
)

fun SubmissionDto.toDomain() = AssignmentSubmission(
    id = id,
    assignmentId = assignmentId,
    studentId = studentId,
    content = content,
    fileUrl = fileUrl,
    submittedAt = submittedAt,
    status = status,
    score = score,
    feedback = feedback,
    gradedAt = gradedAt,
    gradedBy = gradedBy,
    studentName = studentName,
    studentNisn = studentNisn,
    answers = answers.map { it.toDomain() },
)
