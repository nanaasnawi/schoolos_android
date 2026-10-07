package com.schoolos.android.feature.notifications

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.schoolos.android.domain.model.AnnouncementDetail
import com.schoolos.android.domain.model.Notification
import com.schoolos.android.domain.repository.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NotificationDetailUiState(
    val isLoading: Boolean = false,
    val notification: Notification? = null,
    val announcement: AnnouncementDetail? = null,
    val author: String = "Pihak Sekolah",
    val title: String = "",
    val body: String = "",
    val category: String = "PENGUMUMAN",
    val createdAt: String = "",
    val referenceType: String = "",
    val referenceId: String = "",
    val error: String? = null
)

@HiltViewModel
class NotificationDetailViewModel @Inject constructor(
    private val repository: NotificationRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val id: String = savedStateHandle.get<String>("id") ?: ""
    private val initialTitle: String = savedStateHandle.get<String>("title") ?: ""
    private val initialBody: String = savedStateHandle.get<String>("body") ?: ""
    private val initialType: String = savedStateHandle.get<String>("type") ?: "PENGUMUMAN"
    private val initialCreatedAt: String = savedStateHandle.get<String>("createdAt") ?: ""
    private val initialReferenceType: String = savedStateHandle.get<String>("referenceType") ?: ""
    private val initialReferenceId: String = savedStateHandle.get<String>("referenceId") ?: ""

    private val _state = MutableStateFlow(
        NotificationDetailUiState(
            title = initialTitle,
            body = initialBody,
            category = initialType,
            createdAt = initialCreatedAt,
            referenceType = initialReferenceType,
            referenceId = initialReferenceId
        )
    )
    val state = _state.asStateFlow()

    init {
        loadDetail()
    }

    fun loadDetail() {
        if (id.isBlank()) return
        viewModelScope.launch {
            // 1. Mark notification as read
            repository.markRead(id)

            // 2. Fetch notification from DB
            repository.getNotificationById(id).onSuccess { notif ->
                if (notif != null) {
                    _state.value = _state.value.copy(
                        notification = notif,
                        title = notif.title.ifBlank { _state.value.title },
                        body = notif.body.ifBlank { _state.value.body },
                        category = notif.notificationType.ifBlank { _state.value.category },
                        createdAt = notif.createdAt.ifBlank { _state.value.createdAt },
                        referenceType = notif.referenceType ?: _state.value.referenceType,
                        referenceId = notif.referenceId ?: _state.value.referenceId
                    )
                }
            }

            // 3. If reference is an announcement or notification is ANNOUNCEMENT, fetch announcement details (for real author name!)
            val targetRefId = _state.value.referenceId.ifBlank { id }
            if (targetRefId.isNotBlank()) {
                repository.getAnnouncementDetail(targetRefId).onSuccess { ann ->
                    if (ann != null) {
                        _state.value = _state.value.copy(
                            announcement = ann,
                            author = ann.author.ifBlank { "Pihak Sekolah" },
                            title = ann.title.ifBlank { _state.value.title },
                            body = ann.content.ifBlank { _state.value.body },
                            category = ann.category.ifBlank { _state.value.category },
                        )
                    }
                }
            }
        }
    }
}
