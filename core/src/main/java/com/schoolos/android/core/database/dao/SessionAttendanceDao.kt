package com.schoolos.android.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.schoolos.android.core.database.entity.SessionAttendanceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionAttendanceDao {
    @Query("SELECT * FROM session_attendances WHERE sessionId = :sessionId")
    suspend fun getAttendanceBySession(sessionId: String): List<SessionAttendanceEntity>

    @Query("SELECT * FROM session_attendances WHERE sessionId = :sessionId")
    fun observeAttendanceBySession(sessionId: String): Flow<List<SessionAttendanceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(attendance: SessionAttendanceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(attendances: List<SessionAttendanceEntity>)

    @Query("DELETE FROM session_attendances WHERE sessionId = :sessionId")
    suspend fun clearSessionAttendance(sessionId: String)

    @Query("DELETE FROM session_attendances")
    suspend fun clearAll()

    @Transaction
    suspend fun replaceSessionAttendance(sessionId: String, attendances: List<SessionAttendanceEntity>) {
        clearSessionAttendance(sessionId)
        insertAll(attendances)
    }
}

