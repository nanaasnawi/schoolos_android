package com.schoolos.android.feature.assignments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.schoolos.android.domain.model.AcademicClass
import com.schoolos.android.domain.model.AcademicSubject
import com.schoolos.android.domain.model.AssignmentChoice
import com.schoolos.android.domain.model.AssignmentQuestion
import com.schoolos.android.domain.repository.AcademicRepository
import com.schoolos.android.domain.repository.AssignmentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AssignmentCreatorUiState(
    val isLoading: Boolean = false,
    val success: Boolean = false,
    val error: String? = null,
    val availableClasses: List<AcademicClass> = emptyList(),
    val availableSubjects: List<AcademicSubject> = emptyList(),
    val isLoadingAcademicData: Boolean = false,
    val assignmentFormat: String = "STRUCTURED_QUESTIONS", // "STRUCTURED_QUESTIONS", "HOMEWORK_PR", "HYBRID"
    val questions: List<AssignmentQuestion> = listOf(defaultMultipleChoiceQuestion(1)),
)

private fun defaultMultipleChoiceQuestion(order: Int) = AssignmentQuestion(
    id = null,
    questionText = "",
    questionType = "MULTIPLE_CHOICE",
    points = 10,
    orderIndex = order,
    choices = listOf(
        AssignmentChoice(id = null, choiceText = "", isCorrect = true, orderIndex = 1),
        AssignmentChoice(id = null, choiceText = "", isCorrect = false, orderIndex = 2),
        AssignmentChoice(id = null, choiceText = "", isCorrect = false, orderIndex = 3),
        AssignmentChoice(id = null, choiceText = "", isCorrect = false, orderIndex = 4),
    )
)

private fun defaultEssayQuestion(order: Int) = AssignmentQuestion(
    id = null,
    questionText = "",
    questionType = "ESSAY",
    points = 20,
    orderIndex = order,
    choices = emptyList()
)

@HiltViewModel
class AssignmentCreatorViewModel @Inject constructor(
    private val repository: AssignmentRepository,
    private val academicRepository: AcademicRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(AssignmentCreatorUiState())
    val state = _state.asStateFlow()

    init {
        loadAcademicData()
    }

    fun loadAcademicData() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoadingAcademicData = true)
            val classesResult = academicRepository.getClasses()
            val subjectsResult = academicRepository.getSubjects()

            _state.value = _state.value.copy(
                isLoadingAcademicData = false,
                availableClasses = classesResult.getOrDefault(emptyList()),
                availableSubjects = subjectsResult.getOrDefault(emptyList()),
            )
        }
    }

    fun setAssignmentFormat(format: String) {
        _state.value = _state.value.copy(assignmentFormat = format)
    }

    fun addMultipleChoiceQuestion() {
        val current = _state.value.questions
        val nextOrder = current.size + 1
        _state.value = _state.value.copy(questions = current + defaultMultipleChoiceQuestion(nextOrder))
    }

    fun addEssayQuestion() {
        val current = _state.value.questions
        val nextOrder = current.size + 1
        _state.value = _state.value.copy(questions = current + defaultEssayQuestion(nextOrder))
    }

    fun removeQuestion(index: Int) {
        val current = _state.value.questions.toMutableList()
        if (index in current.indices) {
            current.removeAt(index)
            val reindexed = current.mapIndexed { idx, q -> q.copy(orderIndex = idx + 1) }
            _state.value = _state.value.copy(questions = reindexed)
        }
    }

    fun updateQuestionText(index: Int, text: String) {
        val current = _state.value.questions.toMutableList()
        if (index in current.indices) {
            current[index] = current[index].copy(questionText = text)
            _state.value = _state.value.copy(questions = current)
        }
    }

    fun updateQuestionPoints(index: Int, points: Int) {
        val current = _state.value.questions.toMutableList()
        if (index in current.indices) {
            current[index] = current[index].copy(points = points)
            _state.value = _state.value.copy(questions = current)
        }
    }

    fun updateChoiceText(qIndex: Int, cIndex: Int, text: String) {
        val currentQuestions = _state.value.questions.toMutableList()
        if (qIndex in currentQuestions.indices) {
            val q = currentQuestions[qIndex]
            val currentChoices = q.choices.toMutableList()
            if (cIndex in currentChoices.indices) {
                currentChoices[cIndex] = currentChoices[cIndex].copy(choiceText = text)
                currentQuestions[qIndex] = q.copy(choices = currentChoices)
                _state.value = _state.value.copy(questions = currentQuestions)
            }
        }
    }

    fun setCorrectChoice(qIndex: Int, cIndex: Int) {
        val currentQuestions = _state.value.questions.toMutableList()
        if (qIndex in currentQuestions.indices) {
            val q = currentQuestions[qIndex]
            val updatedChoices = q.choices.mapIndexed { idx, c ->
                c.copy(isCorrect = (idx == cIndex))
            }
            currentQuestions[qIndex] = q.copy(choices = updatedChoices)
            _state.value = _state.value.copy(questions = currentQuestions)
        }
    }

    fun createAssignment(
        title: String,
        description: String,
        instructions: String,
        customMaxScore: Int,
        dueAt: String,
        classId: String?,
    ) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)

            val format = _state.value.assignmentFormat
            val isStructured = format == "STRUCTURED_QUESTIONS" || format == "HYBRID"

            // Filter valid questions: must have non-blank questionText
            val validQuestions = if (isStructured) {
                _state.value.questions
                    .filter { it.questionText.isNotBlank() }
                    .mapIndexed { idx, q ->
                        val validChoices = if (q.questionType == "MULTIPLE_CHOICE") {
                            q.choices.filter { it.choiceText.isNotBlank() }.mapIndexed { cIdx, c ->
                                c.copy(orderIndex = cIdx + 1)
                            }
                        } else emptyList()
                        q.copy(orderIndex = idx + 1, choices = validChoices)
                    }
            } else emptyList()

            val totalQuestionsPoints = validQuestions.sumOf { it.points ?: 10 }
            val computedMaxScore = if (isStructured && totalQuestionsPoints > 0) {
                totalQuestionsPoints
            } else {
                customMaxScore
            }

            val assignmentType = when (format) {
                "HOMEWORK_PR" -> "HOMEWORK"
                "HYBRID" -> "HYBRID"
                else -> "QUIZ"
            }

            repository.createAssignment(
                title = title,
                description = description,
                instructions = instructions,
                maxScore = computedMaxScore,
                dueAt = dueAt,
                classId = classId ?: "",
                assignmentType = assignmentType,
                questions = validQuestions,
            ).onSuccess {
                _state.value = _state.value.copy(isLoading = false, success = true)
            }.onFailure { e ->
                _state.value = _state.value.copy(isLoading = false, error = e.message ?: "Failed to create assignment")
            }
        }
    }

    fun resetState() {
        _state.value = _state.value.copy(
            isLoading = false,
            success = false,
            error = null,
            questions = listOf(defaultMultipleChoiceQuestion(1)),
        )
    }
}
