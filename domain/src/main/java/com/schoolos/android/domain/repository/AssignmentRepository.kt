package com.schoolos.android.domain.repository

import com.schoolos.android.domain.model.Assignment
import com.schoolos.android.domain.model.AssignmentSubmission

interface AssignmentRepository {
    suspend fun getAssignments(classId: String): Result<List<Assignment>>
    suspend fun getAssignment(id: String): Result<Assignment>
    suspend fun createAssignment(
        title: String,
        description: String?,
        instructions: String?,
        maxScore: Int,
        dueAt: String?,
        classId: String,
        assignmentType: String,
        questions: List<com.schoolos.android.domain.model.AssignmentQuestion> = emptyList(),
    ): Result<Assignment>
    // answers: list of Pair(questionId, chosenChoiceId or textAnswer)
    suspend fun submitAssignment(
        assignmentId: String,
        content: String?,
        fileUrl: String?,
        answers: List<Pair<String, String?>> = emptyList(), // (questionId to chosenChoiceId), or (questionId to null) for essay
        textAnswers: Map<String, String> = emptyMap(), // questionId -> text answer for ESSAY
    ): Result<AssignmentSubmission>
    suspend fun getSubmissions(assignmentId: String): Result<List<AssignmentSubmission>>
    suspend fun gradeSubmission(assignmentId: String, submissionId: String, score: Int, feedback: String?): Result<AssignmentSubmission>
}
