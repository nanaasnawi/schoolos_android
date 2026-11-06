package com.schoolos.android.feature.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.schoolos.android.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TeacherDirectoryState(
    val isLoading: Boolean = true,
    val teachers: List<TeacherContactItem> = emptyList(),
)

/** Loads the real teacher directory (name + subject) from the backend; no hardcoded contacts. */
@HiltViewModel
class TeacherDirectoryViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(TeacherDirectoryState())
    val state: StateFlow<TeacherDirectoryState> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            authRepository.getTeacherDirectory()
                .onSuccess { list ->
                    val contacts = list.map { t ->
                        val subject = t.subject?.trim().orEmpty()
                        TeacherContactItem(
                            id = t.id,
                            userId = t.userId?.takeIf { it.isNotBlank() } ?: t.id,
                            name = t.fullName.trim(),
                            subject = subject.ifBlank { "Guru Mata Pelajaran" },
                            roleTitle = if (subject.isBlank()) "Tutor Pengampu" else "Tutor $subject",
                            isPrincipal = false,
                        )
                    }.sortedBy { it.name.lowercase() }

                    // Principal contact item for student inquiry / direct chat with school management
                    val principalContact = TeacherContactItem(
                        id = "principal-user",
                        userId = "principal-user",
                        name = "Kepala Sekolah PKBM",
                        subject = "Konsultasi Kebijakan, Akademik & Pengaduan Warga Belajar",
                        roleTitle = "Kepala Sekolah / Pimpinan PKBM",
                        isPrincipal = true,
                    )

                    _state.update {
                        it.copy(
                            isLoading = false,
                            teachers = listOf(principalContact) + contacts
                        )
                    }
                }
                .onFailure { _state.update { it.copy(isLoading = false) } }
        }
    }
}
