package com.schoolos.android.domain.repository

import com.schoolos.android.domain.model.Notification
import kotlinx.coroutines.flow.Flow

interface NotificationRepository {
    suspend fun getNotifications(page: Int = 1): Result<List<Notification>>
    suspend fun getUnreadCount(): Result<Int>
    fun getUnreadCountFlow(): Flow<Int>
    suspend fun markRead(id: String)
    suspend fun markAllRead()
    suspend fun broadcastNotification(
        classId: String,
        title: String,
        body: String,
        targetRoles: List<String>
    ): Result<Unit>
}
