package com.schoolos.android.domain.model

data class GeneratedCurriculumChoice(
    val choiceText: String,
    val isCorrect: Boolean,
)

data class GeneratedCurriculumQuestion(
    val id: String,
    val questionText: String,
    val questionType: String, // "MULTIPLE_CHOICE" or "ESSAY"
    val points: Int,
    val choices: List<GeneratedCurriculumChoice> = emptyList(),
    val explanation: String? = null,
    val rubric: String? = null,
)

data class GeneratedCurriculumResult(
    val title: String,
    val instructions: String? = null,
    val description: String? = null,
    val format: String,
    val questions: List<GeneratedCurriculumQuestion>,
    val timeLimitMinutes: Int = 30,
    val passingScore: Int = 70,
    val subjectName: String = "",
)

data class GeneratedMaterialBlock(
    val id: String,
    val type: String, // "TEXT" or "IMAGE"
    val content: String,
)

data class GeneratedMaterialResult(
    val title: String,
    val description: String,
    val mode: String, // "INFOGRAPHIC" or "ARTICLE"
    val blocks: List<GeneratedMaterialBlock> = emptyList(),
)
