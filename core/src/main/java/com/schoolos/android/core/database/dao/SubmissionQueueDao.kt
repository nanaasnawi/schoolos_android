package com.schoolos.android.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.schoolos.android.core.database.entity.SubmissionQueueEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SubmissionQueueDao {
    @Query("SELECT * FROM submission_queue WHERE tenantId = :tenantId AND userId = :userId AND (status = 'PENDING' OR status = 'FAILED') ORDER BY createdAt ASC")
    suspend fun getPendingSubmissionsForUser(tenantId: String, userId: String): List<SubmissionQueueEntity>

    @Query("SELECT COUNT(*) FROM submission_queue WHERE tenantId = :tenantId AND userId = :userId AND (status = 'PENDING' OR status = 'FAILED')")
    fun observePendingCountForUser(tenantId: String, userId: String): Flow<Int>

    @Query("SELECT * FROM submission_queue WHERE tenantId = :tenantId AND userId = :userId ORDER BY createdAt DESC")
    fun getAllSubmissionsForUser(tenantId: String, userId: String): Flow<List<SubmissionQueueEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: SubmissionQueueEntity)

    @Query("UPDATE submission_queue SET status = :status, lastError = :error WHERE id = :id")
    suspend fun updateStatus(id: String, status: String, error: String? = null)

    @Query("UPDATE submission_queue SET retryCount = retryCount + 1, lastError = :error WHERE id = :id")
    suspend fun incrementRetry(id: String, error: String)

    @Query("DELETE FROM submission_queue WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM submission_queue WHERE status = 'SYNCED'")
    suspend fun clearSynced()

    @Query("DELETE FROM submission_queue")
    suspend fun clearAll()
}
