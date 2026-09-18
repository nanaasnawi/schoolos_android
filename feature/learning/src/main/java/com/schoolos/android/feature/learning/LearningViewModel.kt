package com.schoolos.android.feature.learning

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.schoolos.android.core.designsystem.NeonBlue
import com.schoolos.android.core.designsystem.TeacherNeon
import com.schoolos.android.core.designsystem.StudentNeon
import com.schoolos.android.core.designsystem.AccentNeonPurple
import com.schoolos.android.domain.model.MaterialType
import com.schoolos.android.domain.repository.LearningMaterialRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MaterialItem(
    val id: String,
    val title: String,
    val type: String, // PDF, VIDEO, MODULE, ARTICLE
    val size: String,
    val subject: String,
    val color: Color,
    val isCompleted: Boolean = false,
    val completedCount: Long = 0L,
    val readTimeMinutes: Int = 8,
    val description: String? = null,
    val teacherName: String? = null,
    val className: String? = null,
    val startPage: Int? = null,
    val endPage: Int? = null,
    val mediaUrl: String? = null,
    val thumbnailUrl: String? = null,
)

data class LearningUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val searchQuery: String = "",
    val selectedCategory: String = "Semua",
    val materials: List<MaterialItem> = emptyList(),
    val totalCompleted: Int = 0,
    val totalMaterials: Int = 0,
    val userRole: String = "student",
    val subjectFilter: String? = null,
    val bookPdfCount: Int = 0,
    val videoCount: Int = 0,
    val pendingCount: Int = 0,
)

@HiltViewModel
class LearningViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: LearningMaterialRepository,
    private val authManager: com.schoolos.android.core.auth.AuthManager,
) : ViewModel() {

    private val _state = MutableStateFlow(LearningUiState())
    val state = _state.asStateFlow()

    private var allMaterials: List<MaterialItem> = emptyList()

    /** Optional subject filter passed via navigation (e.g. from a session detail). */
    private var subjectFilter: String? = savedStateHandle.get<String>("subjectId")?.takeIf { it.isNotBlank() }

    val selectedMaterial = MutableStateFlow<com.schoolos.android.domain.model.LearningMaterial?>(null)
    val materialCompletions = MutableStateFlow<List<com.schoolos.android.domain.model.MaterialStudentCompletion>>(emptyList())
    val isLoadingCompletions = MutableStateFlow(false)

    init {
        viewModelScope.launch {
            authManager.authState.collect { auth ->
                _state.value = _state.value.copy(userRole = auth.role ?: "student")
            }
        }
        loadMaterials()
    }

    fun loadMaterials() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            repository.getMaterials()
                .onSuccess { list ->
                    val mapped = list.mapIndexed { idx, m ->
                        val color = when (idx % 4) {
                            0 -> NeonBlue
                            1 -> TeacherNeon
                            2 -> AccentNeonPurple
                            else -> StudentNeon
                        }
                        val typeLabel = when (m.materialType) {
                            MaterialType.VIDEO -> "VIDEO"
                            MaterialType.DOCUMENT -> "PDF"
                            MaterialType.IMAGE -> "IMAGE"
                            MaterialType.ARTICLE -> "ARTICLE"
                        }
                        MaterialItem(
                            id = m.id,
                            title = m.title,
                            type = typeLabel,
                            size = m.size ?: "PDF Digital",
                            subject = m.subject,
                            color = color,
                            isCompleted = m.isCompleted,
                            completedCount = m.completedCount,
                            readTimeMinutes = if (typeLabel == "VIDEO") 12 else 8,
                            description = m.description,
                            teacherName = m.teacherName,
                            className = m.className,
                            startPage = m.startPage,
                            endPage = m.endPage,
                            mediaUrl = m.mediaUrl,
                            thumbnailUrl = m.thumbnailUrl,
                        )
                    }
                    allMaterials = mapped
                    filterMaterials(_state.value.searchQuery, _state.value.selectedCategory)
                }
                .onFailure { err ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = err.message ?: "Gagal memuat modul pembelajaran."
                    )
                }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _state.value = _state.value.copy(searchQuery = query)
        filterMaterials(query, _state.value.selectedCategory)
    }

    fun onCategorySelected(category: String) {
        _state.value = _state.value.copy(selectedCategory = category)
        filterMaterials(_state.value.searchQuery, category)
    }

    fun loadMaterialCompletions(materialId: String) {
        viewModelScope.launch {
            isLoadingCompletions.value = true
            repository.getMaterialCompletions(materialId)
                .onSuccess { completions ->
                    materialCompletions.value = completions
                    isLoadingCompletions.value = false
                }
                .onFailure {
                    materialCompletions.value = emptyList()
                    isLoadingCompletions.value = false
                }
        }
    }

    fun loadMaterialDetail(id: String) {
        viewModelScope.launch {
            repository.getMaterialById(id)
                .onSuccess {
                    selectedMaterial.value = it
                    loadMaterialCompletions(id)
                }
                .onFailure { selectedMaterial.value = null }
        }
    }

    fun toggleMaterialCompletion(id: String) {
        viewModelScope.launch {
            val currentMat = selectedMaterial.value
            val currentCompleted = currentMat?.isCompleted ?: allMaterials.find { it.id == id }?.isCompleted ?: false
            val newCompleted = !currentCompleted

            // Optimistic update
            if (currentMat != null && currentMat.id == id) {
                selectedMaterial.value = currentMat.copy(isCompleted = newCompleted)
            }
            allMaterials = allMaterials.map {
                if (it.id == id) it.copy(isCompleted = newCompleted) else it
            }
            filterMaterials(_state.value.searchQuery, _state.value.selectedCategory)

            // Network sync
            repository.toggleMaterialCompletion(id)
                .onSuccess { serverIsCompleted ->
                    if (selectedMaterial.value?.id == id) {
                        selectedMaterial.value = selectedMaterial.value?.copy(isCompleted = serverIsCompleted)
                    }
                    allMaterials = allMaterials.map {
                        if (it.id == id) it.copy(isCompleted = serverIsCompleted) else it
                    }
                    filterMaterials(_state.value.searchQuery, _state.value.selectedCategory)
                    loadMaterialCompletions(id)
                }
                .onFailure {
                    // Revert on error
                    if (selectedMaterial.value?.id == id) {
                        selectedMaterial.value = currentMat?.copy(isCompleted = currentCompleted)
                    }
                    allMaterials = allMaterials.map {
                        if (it.id == id) it.copy(isCompleted = currentCompleted) else it
                    }
                    filterMaterials(_state.value.searchQuery, _state.value.selectedCategory)
                }
        }
    }

    fun clearSubjectFilter() {
        subjectFilter = null
        _state.value = _state.value.copy(subjectFilter = null)
        filterMaterials(_state.value.searchQuery, _state.value.selectedCategory)
    }

    /** Tolerant subject matching: exact, or bidirectional contains (case-insensitive). */
    private fun matchesSubject(itemSubject: String?, filter: String): Boolean {
        if (itemSubject.isNullOrBlank()) return false
        val a = itemSubject.trim()
        val b = filter.trim()
        return a.equals(b, ignoreCase = true) || a.contains(b, ignoreCase = true) || b.contains(a, ignoreCase = true)
    }

    private fun filterMaterials(query: String, category: String) {
        var filtered = allMaterials

        subjectFilter?.let { filter ->
            filtered = filtered.filter { matchesSubject(it.subject, filter) }
        }

        if (query.isNotBlank()) {
            filtered = filtered.filter {
                it.title.contains(query, ignoreCase = true) ||
                it.subject.contains(query, ignoreCase = true)
            }
        }

        when (category) {
            "Selesai" -> filtered = filtered.filter { it.isCompleted }
            "Belum Selesai" -> filtered = filtered.filter { !it.isCompleted }
            "Buku & PDF" -> filtered = filtered.filter { it.type == "PDF" || it.startPage != null }
            "Video" -> filtered = filtered.filter { it.type == "VIDEO" }
            "Semua" -> {}
            else -> {
                filtered = filtered.filter {
                    it.subject.contains(category, ignoreCase = true)
                }
            }
        }

        val completedCount = allMaterials.count { it.isCompleted }
        val pendingCount = allMaterials.size - completedCount
        val bookPdfCount = allMaterials.count { it.type == "PDF" || it.startPage != null }
        val videoCount = allMaterials.count { it.type == "VIDEO" }

        _state.value = _state.value.copy(
            isLoading = false,
            materials = filtered,
            totalCompleted = completedCount,
            totalMaterials = allMaterials.size,
            pendingCount = pendingCount,
            bookPdfCount = bookPdfCount,
            videoCount = videoCount,
            subjectFilter = subjectFilter,
        )
    }

    fun deleteMaterial(id: String, onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            repository.deleteMaterial(id)
                .onSuccess {
                    loadMaterials()
                    onResult(true, null)
                }
                .onFailure { err ->
                    _state.value = _state.value.copy(isLoading = false, error = err.message)
                    onResult(false, err.message)
                }
        }
    }

    fun updateMaterial(
        id: String,
        title: String?,
        description: String?,
        mediaUrl: String? = null,
        onResult: (Boolean, String?) -> Unit = { _, _ -> }
    ) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            repository.updateMaterial(id, title, description, mediaUrl)
                .onSuccess { updated ->
                    selectedMaterial.value = updated
                    loadMaterials()
                    onResult(true, null)
                }
                .onFailure { err ->
                    _state.value = _state.value.copy(isLoading = false, error = err.message)
                    onResult(false, err.message)
                }
        }
    }
}

