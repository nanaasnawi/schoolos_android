package com.schoolos.android.feature.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.schoolos.android.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
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
    private val authManager: com.schoolos.android.core.auth.AuthManager,
) : ViewModel() {

    private val _state = MutableStateFlow(TeacherDirectoryState())
    val state: StateFlow<TeacherDirectoryState> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            val auth = authManager.authState.first()
            val isPkbm = com.schoolos.android.core.auth.isPkbmInstitution(auth.schoolName)
            authRepository.getTeacherDirectory()
                .onSuccess { list ->
                    val contacts = list.filterNot { t ->
                        t.fullName.contains("Kepala Sekolah", ignoreCase = true) ||
                        t.subject?.contains("Kepala Sekolah", ignoreCase = true) == true
                    }.map { t ->
                        val subject = t.subject?.trim().orEmpty()
                        val canonicalName = t.fullName.trim()
                            .replace(Regex(",\\s*(S\\.?\\s*Pd(\\.?\\s*I|\\.\\s*SD)?|M\\.?\\s*Pd|S\\.?\\s*Ag|S\\.?\\s*Kom|S\\.?\\s*E|S\\.?\\s*T|S\\.?\\s*Si|S\\.?\\s*Sos|M\\.?\\s*Si|M\\.?\\s*M)\\.?$", RegexOption.IGNORE_CASE), "")
                            .trim()
                        TeacherContactItem(
                            id = t.id,
                            userId = t.userId?.takeIf { it.isNotBlank() } ?: t.id,
                            name = canonicalName.ifBlank { t.fullName.trim() },
                            subject = subject.ifBlank { if (isPkbm) "Tutor Mata Pelajaran" else "Guru Mata Pelajaran" },
                            roleTitle = if (subject.isBlank()) (if (isPkbm) "Tutor Pengampu" else "Guru Pengampu") else (if (isPkbm) "Tutor $subject" else "Guru $subject"),
                            isPrincipal = false,
                        )
                    }.distinctBy { it.name.lowercase().replace(Regex("[^a-z0-9 ]"), "").trim() }
                     .sortedBy { it.name.lowercase() }

                    // Principal contact item for student inquiry / direct chat with school management (only if present in tenant directory)
                    val principalUser = list.firstOrNull { t ->
                        t.fullName.contains("Kepala Sekolah", ignoreCase = true) ||
                        t.subject?.contains("Kepala Sekolah", ignoreCase = true) == true
                    }
                    val principalContacts = if (principalUser != null) {
                        val principalId = principalUser.userId?.takeIf { it.isNotBlank() } ?: principalUser.id
                        val principalName = principalUser.fullName.trim().ifBlank { "Kepala Sekolah" }
                        listOf(
                            TeacherContactItem(
                                id = principalId,
                                userId = principalId,
                                name = principalName,
                                subject = if (isPkbm) "Konsultasi Kebijakan, Akademik & Pengaduan Warga Belajar" else "Konsultasi Kebijakan, Akademik & Pengaduan Siswa",
                                roleTitle = "Kepala Sekolah",
                                isPrincipal = true,
                            )
                        )
                    } else {
                        emptyList()
                    }

                    _state.update {
                        it.copy(
                            isLoading = false,
                            teachers = principalContacts + contacts
                        )
                    }
                }
                .onFailure { _state.update { it.copy(isLoading = false) } }
        }
    }
}
