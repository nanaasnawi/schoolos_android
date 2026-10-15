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
import com.schoolos.android.core.auth.AuthManager
import kotlinx.coroutines.flow.first

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
    val classId: String? = null,
    val className: String = "",
    val subjectName: String = "",
    val teacherSubject: String? = null,
    val isSubjectLocked: Boolean = false,
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
    private val authManager: AuthManager,
) : ViewModel() {

    private val _state = MutableStateFlow(QuizBuilderUiState())
    val state = _state.asStateFlow()

    init {
        loadAcademicData()
    }

    fun goToStep(step: Int) {
        _state.value = _state.value.copy(currentStep = step)
    }

    fun generateQuizWithAi(
        type: String,
        subjectId: String,
        subjectName: String,
        classId: String?,
        className: String,
        topic: String = "",
        numQuestions: Int = 5,
        difficulty: String = "Sedang",
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
                topic = topic.trim().ifBlank { null },
                gradeLevel = className.ifBlank { "Kelas 5 SD" },
                numQuestions = numQuestions,
                difficulty = difficulty
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

    suspend fun generateQuizAsync(
        type: String,
        subjectId: String,
        subjectName: String,
        classId: String?,
        className: String,
        topic: String = "",
        numQuestions: Int = 10,
        numMcq: Int = 10,
        numEssay: Int = 0,
        difficulty: String = "Sedang",
        includeImages: Boolean = false,
    ): Result<com.schoolos.android.domain.model.GeneratedCurriculumResult> {
        _state.value = _state.value.copy(isGeneratingAi = true, error = null)
        val res = generatorRepository.generateCurriculum(
            type = type,
            subjectId = subjectId,
            subjectName = subjectName,
            sourceMode = if (type == "EXAM_MONTHLY") "PAST_MONTH" else "LATEST_PUBLISHED",
            topic = topic.trim().ifBlank { null },
            gradeLevel = className.ifBlank { "Kelas 5 SD" },
            numQuestions = numQuestions,
            numMcq = numMcq,
            numEssay = numEssay,
            difficulty = difficulty,
            includeImages = includeImages,
        )
        _state.value = _state.value.copy(isGeneratingAi = false)
        return res
    }

    fun applyGeneratedQuiz(
        classId: String?,
        className: String,
        generated: com.schoolos.android.domain.model.GeneratedCurriculumResult,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val summaries = generated.questions.mapIndexed { idx, q ->
            val correctIdx = q.choices.indexOfFirst { it.isCorrect }.let { if (it >= 0) it else 0 }
            QuestionSummary(
                number = idx + 1,
                text = q.questionText,
                type = q.questionType,
                points = q.points,
                choicesCount = q.choices.size,
                choices = q.choices.map { it.choiceText },
                correctIndex = correctIdx
            )
        }

        // 1. Instantly update UI state so all generated questions appear immediately in Bank Soal
        _state.value = _state.value.copy(
            isLoading = false,
            quizCreated = true,
            createdQuizId = null,
            createdQuizTitle = generated.title,
            className = className,
            subjectName = generated.subjectName,
            timeLimitMinutes = generated.timeLimitMinutes,
            passingScore = generated.passingScore,
            questionsList = summaries,
            totalPoints = summaries.sumOf { it.points },
            currentStep = 2,
            error = null
        )
        onSuccess()

        // 2. Persist draft quiz and questions to server in the background
        viewModelScope.launch {
            val totalComputedScore = summaries.sumOf { it.points }.let { if (it > 0) it else 100 }
            val targetClassId = classId ?: _state.value.availableClasses.find { it.name == className }?.id
                ?: _state.value.availableClasses.firstOrNull()?.id ?: ""

            repository.createQuiz(
                title = generated.title,
                description = generated.description ?: "Kuis Otomatis • ${generated.subjectName} • $className",
                classId = targetClassId,
                timeLimitMinutes = generated.timeLimitMinutes,
                passingScore = generated.passingScore,
                maxScore = totalComputedScore
            ).onSuccess { createdQuiz ->
                _state.value = _state.value.copy(createdQuizId = createdQuiz.id)
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
                    repository.addQuestion(
                        quizId = createdQuiz.id,
                        questionText = q.questionText,
                        questionType = if (q.questionType == "MULTIPLE_CHOICE") "multiple_choice" else "essay",
                        points = q.points,
                        imageUrl = null,
                        choices = choicesInputs
                    )
                }
            }.onFailure { err ->
                android.util.Log.w("QuizBuilderVM", "Background draft quiz creation failed: ${err.message}")
            }
        }
    }

    fun loadAcademicData() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoadingAcademicData = true)
            val auth = authManager.authState.first()
            val classesResult = academicRepository.getClasses()
            val subjectsResult = academicRepository.getSubjects()
            val rawSubjects = subjectsResult.getOrDefault(emptyList())

            val isTeacher = auth.isSubjectLockedTeacher
            val teacherSubj = auth.subject?.trim()
            val isLocked = isTeacher && !teacherSubj.isNullOrBlank()

            val finalSubjects = if (isLocked && teacherSubj != null) {
                val tokens = teacherSubj.split(Regex("[/,]"))
                    .map { it.trim().lowercase() }
                    .filter { it.isNotEmpty() }
                val matched = rawSubjects.filter { s ->
                    val sName = s.name.lowercase()
                    tokens.any { t -> sName == t || sName.contains(t) || t.contains(sName) }
                }
                if (matched.isNotEmpty()) {
                    matched
                } else {
                    listOf(AcademicSubject(id = "teacher_subj", name = teacherSubj, code = "GURU"))
                }
            } else {
                rawSubjects
            }

            _state.value = _state.value.copy(
                isLoadingAcademicData = false,
                availableClasses = classesResult.getOrDefault(emptyList()),
                availableSubjects = finalSubjects,
                teacherSubject = if (isLocked) teacherSubj else null,
                isSubjectLocked = isLocked,
                subjectName = if (isLocked && teacherSubj != null && _state.value.subjectName.isBlank()) {
                    finalSubjects.firstOrNull()?.name ?: teacherSubj
                } else {
                    _state.value.subjectName
                }
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
                    classId = classId,
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

    fun updateQuestionInState(
        index: Int,
        text: String,
        type: String,
        choices: List<String>,
        correctIndex: Int,
        points: Int
    ) {
        val currentList = _state.value.questionsList.toMutableList()
        if (index in currentList.indices) {
            val old = currentList[index]
            currentList[index] = old.copy(
                text = text,
                type = type,
                choices = choices,
                choicesCount = choices.size,
                correctIndex = correctIndex,
                points = points
            )
            _state.value = _state.value.copy(
                questionsList = currentList,
                totalPoints = currentList.sumOf { it.points }
            )
        }
    }

    fun deleteQuestion(index: Int) {
        val current = _state.value.questionsList.toMutableList()
        if (index in current.indices) {
            current.removeAt(index)
            val renumbered = current.mapIndexed { idx, q -> q.copy(number = idx + 1) }
            _state.value = _state.value.copy(
                questionsList = renumbered,
                totalPoints = renumbered.sumOf { it.points }
            )
        }
    }

    fun duplicateQuestion(index: Int) {
        val current = _state.value.questionsList.toMutableList()
        if (index in current.indices) {
            val target = current[index]
            val duplicated = target.copy(
                number = current.size + 1,
                text = "${target.text} (Salinan)"
            )
            current.add(duplicated)
            _state.value = _state.value.copy(
                questionsList = current,
                totalPoints = current.sumOf { it.points }
            )
        }
    }

    fun balancePoints(targetTotal: Int = 100) {
        val current = _state.value.questionsList
        if (current.isEmpty()) return
        val count = current.size
        val basePts = targetTotal / count
        val remainder = targetTotal % count
        val rebalanced = current.mapIndexed { idx, q ->
            val pts = basePts + if (idx < remainder) 1 else 0
            q.copy(points = pts)
        }
        _state.value = _state.value.copy(
            questionsList = rebalanced,
            totalPoints = targetTotal
        )
    }

    private fun correctOptionIndex(index: Int, size: Int): Int {
        return if (size > 0 && index in 0 until size) index else 0
    }

    fun publishCreatedQuiz(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            val existingQuizId = _state.value.createdQuizId
            val finalQuizId = if (existingQuizId.isNullOrBlank()) {
                val targetClassId = _state.value.availableClasses.find { it.name == _state.value.className }?.id
                    ?: _state.value.availableClasses.firstOrNull()?.id ?: ""
                val totalComputedScore = _state.value.questionsList.sumOf { it.points }.let { if (it > 0) it else 100 }
                val title = _state.value.createdQuizTitle.ifBlank { "Paket Soal CBT: ${_state.value.subjectName}" }
                val desc = "Kuis Otomatis • ${_state.value.subjectName} • ${_state.value.className}"
                val createRes = repository.createQuiz(
                    title = title,
                    description = desc,
                    classId = targetClassId,
                    timeLimitMinutes = _state.value.timeLimitMinutes,
                    passingScore = _state.value.passingScore,
                    maxScore = totalComputedScore
                )
                val newQuiz = createRes.getOrNull()
                if (newQuiz != null) {
                    _state.value = _state.value.copy(createdQuizId = newQuiz.id)
                    for (q in _state.value.questionsList) {
                        val choicesInputs = if (q.type == "MULTIPLE_CHOICE") {
                            q.choices.mapIndexed { idx, cText ->
                                ChoiceInput(choiceText = cText, orderIndex = idx + 1, isCorrect = idx == q.correctIndex)
                            }
                        } else emptyList()
                        repository.addQuestion(
                            quizId = newQuiz.id,
                            questionText = q.text,
                            questionType = if (q.type == "MULTIPLE_CHOICE") "multiple_choice" else "essay",
                            points = q.points,
                            imageUrl = null,
                            choices = choicesInputs
                        )
                    }
                    newQuiz.id
                } else {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = createRes.exceptionOrNull()?.message ?: "Gagal membuat kuis di server"
                    )
                    return@launch
                }
            } else {
                existingQuizId
            }

            repository.publishQuiz(finalQuizId)
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
