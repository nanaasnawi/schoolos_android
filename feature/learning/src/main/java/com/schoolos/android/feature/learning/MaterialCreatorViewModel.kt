package com.schoolos.android.feature.learning

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.schoolos.android.domain.model.AcademicClass
import com.schoolos.android.domain.model.AcademicSubject
import com.schoolos.android.domain.model.MaterialType
import com.schoolos.android.domain.repository.AcademicRepository
import com.schoolos.android.domain.repository.LearningMaterialRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MaterialCreatorUiState(
    val isLoading: Boolean = false,
    val isUploadingFile: Boolean = false,
    val uploadedFileName: String? = null,
    val uploadedFileUrl: String? = null,
    val success: Boolean = false,
    val error: String? = null,
    val availableClasses: List<AcademicClass> = emptyList(),
    val availableSubjects: List<AcademicSubject> = emptyList(),
    val availableBooks: List<com.schoolos.android.domain.model.LibraryBook> = emptyList(),
    val isLoadingAcademicData: Boolean = false,
    val isLoadingBooks: Boolean = false,
)

@HiltViewModel
class MaterialCreatorViewModel @Inject constructor(
    private val repository: LearningMaterialRepository,
    private val academicRepository: AcademicRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(MaterialCreatorUiState())
    val state = _state.asStateFlow()

    init {
        loadAcademicData()
        loadLibraryBooks()
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

    fun uploadFile(bytes: ByteArray, fileName: String, mimeType: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isUploadingFile = true, error = null)
            repository.uploadMaterialFile(bytes, fileName, mimeType)
                .onSuccess { url ->
                    _state.value = _state.value.copy(
                        isUploadingFile = false,
                        uploadedFileName = fileName,
                        uploadedFileUrl = url,
                    )
                }
                .onFailure { err ->
                    _state.value = _state.value.copy(
                        isUploadingFile = false,
                        error = err.message ?: "Gagal mengunggah file materi: ${err.localizedMessage}"
                    )
                }
        }
    }

    fun clearUploadedFile() {
        _state.value = _state.value.copy(
            uploadedFileName = null,
            uploadedFileUrl = null,
        )
    }

    fun createMaterial(
        title: String,
        description: String?,
        materialType: MaterialType,
        contentBody: String?,
        mediaUrl: String?,
        subject: String,
        classId: String?,
    ) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            repository.createMaterial(
                title = title,
                description = description,
                materialType = materialType,
                contentBody = contentBody,
                mediaUrl = mediaUrl,
                subject = subject,
                classId = classId,
            ).onSuccess {
                _state.value = _state.value.copy(isLoading = false, success = true)
            }.onFailure { err ->
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = err.message ?: "Gagal membuat materi ajar."
                )
            }
        }
    }

    fun loadLibraryBooks(search: String? = null) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoadingBooks = true)
            repository.getLibraryBooks(search = search)
                .onSuccess { books ->
                    _state.value = _state.value.copy(
                        isLoadingBooks = false,
                        availableBooks = books
                    )
                }
                .onFailure {
                    _state.value = _state.value.copy(isLoadingBooks = false)
                }
        }
    }

    fun assignReadingBook(
        book: com.schoolos.android.domain.model.LibraryBook,
        startPage: Int,
        endPage: Int,
        instructions: String?,
        classId: String,
        subjectId: String?
    ) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            val title = "Materi Bacaan: ${book.title} (Hal. $startPage–$endPage)"
            repository.assignReadingMaterial(
                bookId = book.id,
                title = title,
                instructions = instructions,
                classId = classId,
                subjectId = subjectId,
                startPage = startPage,
                endPage = endPage
            ).onSuccess {
                _state.value = _state.value.copy(isLoading = false, success = true)
            }.onFailure { err ->
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = err.message ?: "Gagal menugaskan materi buku perpustakaan."
                )
            }
        }
    }

    fun resetState() {
        _state.value = _state.value.copy(
            isLoading = false,
            isUploadingFile = false,
            uploadedFileName = null,
            uploadedFileUrl = null,
            success = false,
            error = null,
        )
    }
}
