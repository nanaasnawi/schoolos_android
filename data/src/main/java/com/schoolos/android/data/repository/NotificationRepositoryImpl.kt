package com.schoolos.android.data.repository

import com.schoolos.android.core.auth.AuthManager
import com.schoolos.android.core.database.dao.NotificationDao
import com.schoolos.android.core.database.mapper.toDomain as entityToDomain
import com.schoolos.android.core.database.mapper.toEntity
import com.schoolos.android.core.network.NetworkMonitor
import com.schoolos.android.data.mapper.toDomain as dtoToDomain
import com.schoolos.android.data.remote.SchoolOsApi
import com.schoolos.android.domain.model.Notification
import com.schoolos.android.domain.repository.NotificationRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepositoryImpl @Inject constructor(
    private val api: SchoolOsApi,
    private val authManager: AuthManager,
    private val notificationDao: NotificationDao,
    private val networkMonitor: NetworkMonitor,
) : NotificationRepository {

    override suspend fun getNotifications(page: Int): Result<List<Notification>> = runCatching {
        val userId = authManager.getStudentId() ?: ""
        val isOnline = try { networkMonitor.isOnline.first() } catch (_: Exception) { true }
        if (isOnline) {
            val response = api.getNotifications(page)
            val notifications = response.data?.items?.map { it.dtoToDomain() } ?: emptyList()
            if (userId.isNotEmpty()) {
                if (page == 1) {
                    try { notificationDao.clearAll() } catch (_: Exception) {}
                }
                if (notifications.isNotEmpty()) {
                    notificationDao.insertAll(notifications.map { it.toEntity(userId) })
                }
            }
            notifications
        } else {
            val cached = try {
                if (userId.isNotEmpty()) {
                    notificationDao.getNotifications(userId).first()
                } else {
                    emptyList()
                }
            } catch (_: Exception) { emptyList() }
            cached.map { it.entityToDomain() }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getCachedNotifications(): Flow<List<Notification>> {
        return authManager.authState.flatMapLatest { auth ->
            val uid = auth.userId ?: ""
            if (uid.isEmpty()) flowOf(emptyList())
            else notificationDao.getNotifications(uid)
        }.map { list -> list.map { it.entityToDomain() } }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getUnreadCountFlow(): Flow<Int> {
        return authManager.authState.flatMapLatest { auth ->
            val uid = auth.userId ?: ""
            if (uid.isEmpty()) flowOf(0)
            else notificationDao.getUnreadCount(uid)
        }
    }

    override suspend fun getUnreadCount(): Result<Int> = runCatching {
        val isOnline = try { networkMonitor.isOnline.first() } catch (_: Exception) { true }
        if (isOnline) {
            try {
                val apiCount = api.getUnreadCount().data?.count
                if (apiCount != null) {
                    return@runCatching apiCount
                }
            } catch (e: Exception) {
                timber.log.Timber.w(e, "Remote unread count fetch failed, falling back to local DB")
            }
        }
        val userId = authManager.getStudentId() ?: ""
        try {
            if (userId.isNotEmpty()) {
                notificationDao.getUnreadCount(userId).first()
            } else {
                0
            }
        } catch (_: Exception) { 0 }
    }

    override suspend fun markRead(id: String) {
        try { api.markNotificationRead(id) } catch (_: Exception) {}
        try { notificationDao.markRead(id, Instant.now().toString()) } catch (_: Exception) {}
    }

    override suspend fun markAllRead() {
        try { api.markAllNotificationsRead() } catch (_: Exception) {}
    }

    override suspend fun broadcastNotification(
        classId: String,
        title: String,
        body: String,
        targetRoles: List<String>
    ): Result<Unit> = runCatching {
        val targets = mutableListOf<String>()
        if (targetRoles.contains("student")) targets.add("STUDENT")
        if (targetRoles.contains("teacher")) targets.add("TEACHER")
        if (targetRoles.contains("parent")) targets.add("GUARDIAN")

        val target = if (targets.size == 3 || targets.isEmpty()) {
            "TARGET_ALL"
        } else {
            targets.joinToString(",")
        }

        val auth = authManager.authState.first()
        val authorName = auth.name?.takeIf { it.isNotBlank() }
        val authorRole = when {
            auth.isPrincipal -> "Kepala Sekolah"
            auth.isTeacher -> "Guru Pengampu"
            else -> "Pihak Sekolah"
        }
        val authorDisplay = if (authorName != null) "$authorName ($authorRole)" else authorRole

        api.createAnnouncement(
            com.schoolos.android.data.remote.CreateAnnouncementRequest(
                title = title,
                content = body,
                category = "PENGUMUMAN",
                target = target,
                author = authorDisplay,
                isPinned = false,
                sendPush = true
            )
        )
    }

    override suspend fun getNotificationById(id: String): Result<Notification?> = runCatching {
        val entity = notificationDao.getNotificationById(id)
        entity?.entityToDomain()
    }

    override suspend fun getAnnouncementDetail(id: String): Result<com.schoolos.android.domain.model.AnnouncementDetail?> = runCatching {
        try {
            val res = api.getAnnouncementById(id)
            res.data?.let {
                com.schoolos.android.domain.model.AnnouncementDetail(
                    id = it.id,
                    title = it.title,
                    content = it.content,
                    category = it.category,
                    author = it.author,
                    date = it.date.ifBlank { it.createdAt }
                )
            }
        } catch (_: Exception) {
            null
        }
    }
}
