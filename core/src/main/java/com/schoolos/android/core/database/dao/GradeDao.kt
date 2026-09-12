package com.schoolos.android.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.schoolos.android.core.database.entity.GradeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GradeDao {
    @Query("SELECT * FROM grades ORDER BY calculatedAt DESC")
    fun getGrades(): Flow<List<GradeEntity>>

    @Query("SELECT * FROM grades WHERE studentId = :studentId ORDER BY calculatedAt DESC")
    fun getGradesByStudent(studentId: String): Flow<List<GradeEntity>>

    @Query("SELECT * FROM grades WHERE classId = :classId AND subjectId = :subjectId")
    fun getGradesBySubject(classId: String, subjectId: String): Flow<List<GradeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(grade: GradeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(grades: List<GradeEntity>)

    @Query("DELETE FROM grades")
    suspend fun clearAll()
}
