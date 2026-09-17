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
    val recommendedBooks: List<com.schoolos.android.domain.model.LibraryBook> = emptyList(),
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

    fun updateRecommendation(className: String?, subjectName: String?) {
        val books = _state.value.availableBooks
        if (books.isEmpty() || (className.isNullOrBlank() && subjectName.isNullOrBlank())) {
            _state.value = _state.value.copy(recommendedBooks = emptyList())
            return
        }

        val s = (subjectName ?: "").lowercase()
        val c = (className ?: "").lowercase()

        val matches = books.filter { b ->
            val title = b.title.lowercase()
            val bookSubj = (b.subjectName ?: "").lowercase()

            val subjMatch = s.isNotBlank() && (
                title.contains(s) || bookSubj.contains(s) ||
                (s.contains("matematika") && (title.contains("matematika") || bookSubj.contains("matematika"))) ||
                (s.contains("indonesia") && (title.contains("indonesia") || bookSubj.contains("indonesia"))) ||
                (s.contains("inggris") && (title.contains("inggris") || bookSubj.contains("inggris"))) ||
                (s.contains("fisika") && (title.contains("fisika") || bookSubj.contains("fisika"))) ||
                (s.contains("biologi") && (title.contains("biologi") || bookSubj.contains("biologi"))) ||
                (s.contains("kimia") && (title.contains("kimia") || bookSubj.contains("kimia"))) ||
                (s.contains("komputer") && (title.contains("informatika") || title.contains("koding"))) ||
                (s.contains("pancasila") && (title.contains("pancasila") || title.contains("ppkn")))
            )

            val classMatch = if (c.contains("10") || c.contains(" x") || c.contains("x ")) {
                title.contains("kelas x") || title.contains("kelas 10") || (b.gradeLevelName?.contains("10") == true)
            } else if (c.contains("11") || c.contains(" xi") || c.contains("xi ")) {
                title.contains("kelas xi") || title.contains("kelas 11") || (b.gradeLevelName?.contains("11") == true)
            } else if (c.contains("12") || c.contains(" xii") || c.contains("xii ")) {
                title.contains("kelas xii") || title.contains("kelas 12") || (b.gradeLevelName?.contains("12") == true)
            } else if (c.contains("7") || c.contains("vii")) {
                title.contains("kelas vii") || title.contains("kelas 7") || (b.gradeLevelName?.contains("7") == true)
            } else if (c.contains("8") || c.contains("viii")) {
                title.contains("kelas viii") || title.contains("kelas 8") || (b.gradeLevelName?.contains("8") == true)
            } else if (c.contains("9") || c.contains("ix")) {
                title.contains("kelas ix") || title.contains("kelas 9") || (b.gradeLevelName?.contains("9") == true)
            } else true

            subjMatch && classMatch
        }.sortedByDescending { b ->
            val title = b.title.lowercase()
            if (!title.contains("panduan guru") && !title.contains("buku guru")) 2 else 1
        }

        _state.value = _state.value.copy(recommendedBooks = matches)
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
