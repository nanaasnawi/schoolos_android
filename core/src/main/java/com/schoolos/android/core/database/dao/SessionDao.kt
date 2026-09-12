package com.schoolos.android.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.schoolos.android.core.database.entity.SessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Query("SELECT * FROM learning_sessions ORDER BY scheduledAt ASC")
    fun getSessions(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM learning_sessions WHERE classId = :classId ORDER BY scheduledAt ASC")
    fun getSessionsByClass(classId: String): Flow<List<SessionEntity>>

    @Query("SELECT * FROM learning_sessions WHERE id = :id LIMIT 1")
    suspend fun getSessionById(id: String): SessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: SessionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(sessions: List<SessionEntity>)

    @Query("DELETE FROM learning_sessions")
    suspend fun clearAll()
}
