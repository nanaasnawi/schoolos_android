package com.schoolos.android.feature.quizzes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.schoolos.android.domain.model.AcademicClass
import com.schoolos.android.domain.model.AcademicSubject
import com.schoolos.android.domain.repository.AcademicRepository
import com.schoolos.android.domain.repository.ChoiceInput
import com.schoolos.android.domain.repository.QuizRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class QuestionSummary(
    val number: Int,
    val text: String,
    val type: String, // "MULTIPLE_CHOICE" or "ESSAY"
    val points: Int,
    val choicesCount: Int,
    val choices: List<String> = emptyList(),
    val correctIndex: Int = 0,
)

data class QuizBuilderUiState(
    val isLoading: Boolean = false,
    val isGeneratingAi: Boolean = false,
    val quizCreated: Boolean = false,
    val createdQuizId: String? = null,
    val createdQuizTitle: String = "",
    val className: String = "",
    val subjectName: String = "",
    val timeLimitMinutes: Int = 30,
    val passingScore: Int = 70,
    val error: String? = null,
    val currentStep: Int = 1, // 1: Info, 2: Questions
    val questionsList: List<QuestionSummary> = emptyList(),
    val totalPoints: Int = 0,
    val availableClasses: List<AcademicClass> = emptyList(),
    val availableSubjects: List<AcademicSubject> = emptyList(),
    val isLoadingAcademicData: Boolean = false,
)

@HiltViewModel
class QuizBuilderViewModel @Inject constructor(
    private val repository: QuizRepository,
    private val academicRepository: AcademicRepository,
    private val generatorRepository: com.schoolos.android.domain.repository.CurriculumGeneratorRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(QuizBuilderUiState())
    val state = _state.asStateFlow()

    init {
        loadAcademicData()
    }

    fun generateQuizWithAi(
        type: String,
        subjectId: String,
        subjectName: String,
        classId: String?,
        className: String,
        topic: String = "",
        onSuccessCallback: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isGeneratingAi = true, error = null)
            generatorRepository.generateCurriculum(
                type = type,
                subjectId = subjectId,
                subjectName = subjectName,
                sourceMode = if (type == "EXAM_MONTHLY") "PAST_MONTH" else "LATEST_PUBLISHED",
                topic = topic.trim().ifBlank { null }
            ).onSuccess { generated ->
                val totalComputedScore = generated.questions.sumOf { it.points }.let { if (it > 0) it else 100 }
                repository.createQuiz(
                    title = generated.title,
                    description = generated.description ?: "Kuis Otomatis • $subjectName • $className",
                    classId = classId ?: "",
                    timeLimitMinutes = generated.timeLimitMinutes,
                    passingScore = generated.passingScore,
                    maxScore = totalComputedScore
                ).onSuccess { createdQuiz ->
                    val summaries = ArrayList<QuestionSummary>()
                    for (q in generated.questions) {
                        val choicesInputs = if (q.questionType == "MULTIPLE_CHOICE") {
                            q.choices.mapIndexed { idx, c ->
                                ChoiceInput(
                                    choiceText = c.choiceText,
                                    orderIndex = idx + 1,
                                    isCorrect = c.isCorrect
                                )
                            }
                        } else {
                            emptyList()
                        }
                        val correctIdx = q.choices.indexOfFirst { it.isCorrect }.let { if (it >= 0) it else 0 }

                        repository.addQuestion(
                            quizId = createdQuiz.id,
                            questionText = q.questionText,
                            questionType = if (q.questionType == "MULTIPLE_CHOICE") "multiple_choice" else "essay",
                            points = q.points,
                            imageUrl = null,
                            choices = choicesInputs
                        )

                        summaries.add(
                            QuestionSummary(
                                number = summaries.size + 1,
                                text = q.questionText,
                                type = q.questionType,
                                points = q.points,
                                choicesCount = q.choices.size,
                                choices = q.choices.map { it.choiceText },
                                correctIndex = correctIdx
                            )
                        )
                    }

                    _state.value = _state.value.copy(
                        isGeneratingAi = false,
                        quizCreated = true,
                        createdQuizId = createdQuiz.id,
                        createdQuizTitle = generated.title,
                        className = className,
                        subjectName = subjectName,
                        timeLimitMinutes = generated.timeLimitMinutes,
                        passingScore = generated.passingScore,
                        questionsList = summaries,
                        totalPoints = summaries.sumOf { it.points },
                        currentStep = 2
                    )
                    onSuccessCallback()
                }.onFailure { err ->
                    _state.value = _state.value.copy(isGeneratingAi = false, error = err.message)
                    onError(err.message ?: "Gagal membuat draft kuis di server")
                }
            }.onFailure { err ->
                _state.value = _state.value.copy(isGeneratingAi = false, error = err.message)
                onError(err.message ?: "Gagal memproses soal otomatis")
            }
        }
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

    fun createQuiz(
        title: String,
        description: String,
        timeLimit: Int?,
        passingScore: Int,
        maxScore: Int,
        classId: String?,
        className: String = "",
        subjectName: String = "",
    ) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            repository.createQuiz(
                title = title,
                description = description,
                classId = classId ?: "",
                timeLimitMinutes = timeLimit,
                passingScore = passingScore,
                maxScore = maxScore
            ).onSuccess { quiz ->
                _state.value = _state.value.copy(
                    isLoading = false,
                    quizCreated = true,
                    createdQuizId = quiz.id,
                    createdQuizTitle = title,
                    className = className,
                    subjectName = subjectName,
                    timeLimitMinutes = timeLimit ?: 30,
                    passingScore = passingScore,
                    currentStep = 2
                )
            }.onFailure { e ->
                _state.value = _state.value.copy(isLoading = false, error = e.message ?: "Failed to create quiz")
            }
        }
    }

    fun addQuestion(
        questionText: String,
        questionType: String = "MULTIPLE_CHOICE", // "MULTIPLE_CHOICE" or "ESSAY"
        choices: List<String> = emptyList(),
        correctIndex: Int = 0,
        points: Int = 10,
        onSuccessCallback: () -> Unit = {}
    ) {
        val quizId = _state.value.createdQuizId ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            val choiceInputs = if (questionType == "MULTIPLE_CHOICE") {
                choices.mapIndexed { index, text ->
                    ChoiceInput(
                        choiceText = text,
                        orderIndex = index + 1,
                        isCorrect = (index == correctIndex)
                    )
                }
            } else {
                emptyList()
            }
            repository.addQuestion(
                quizId = quizId,
                questionText = questionText,
                questionType = if (questionType == "MULTIPLE_CHOICE") "multiple_choice" else "essay",
                points = points,
                imageUrl = null,
                choices = choiceInputs
            ).onSuccess {
                val newSummary = QuestionSummary(
                    number = _state.value.questionsList.size + 1,
                    text = questionText,
                    type = questionType,
                    points = points,
                    choicesCount = if (questionType == "MULTIPLE_CHOICE") choices.size else 0,
                    choices = if (questionType == "MULTIPLE_CHOICE") choices else emptyList(),
                    correctIndex = correctOptionIndex(correctIndex, choices.size)
                )
                val updatedList = _state.value.questionsList + newSummary
                val newTotalPoints = updatedList.sumOf { it.points }
                _state.value = _state.value.copy(
                    isLoading = false,
                    questionsList = updatedList,
                    totalPoints = newTotalPoints
                )
                onSuccessCallback()
            }.onFailure { e ->
                _state.value = _state.value.copy(isLoading = false, error = e.message ?: "Gagal menambahkan butir soal kuis.")
            }
        }
    }

    private fun correctOptionIndex(index: Int, size: Int): Int {
        return if (size > 0 && index in 0 until size) index else 0
    }

    fun publishCreatedQuiz(onComplete: () -> Unit = {}) {
        val quizId = _state.value.createdQuizId
        if (quizId.isNullOrBlank()) {
            onComplete()
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            repository.publishQuiz(quizId)
                .onSuccess {
                    _state.value = _state.value.copy(isLoading = false)
                    onComplete()
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = e.message ?: "Gagal menerbitkan kuis"
                    )
                }
        }
    }
}
