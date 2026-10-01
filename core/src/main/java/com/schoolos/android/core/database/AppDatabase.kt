package com.schoolos.android.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.schoolos.android.core.database.dao.AssignmentDao
import com.schoolos.android.core.database.dao.GradeDao
import com.schoolos.android.core.database.dao.LearningMaterialDao
import com.schoolos.android.core.database.dao.NotificationDao
import com.schoolos.android.core.database.dao.QuizDao
import com.schoolos.android.core.database.dao.SessionAttendanceDao
import com.schoolos.android.core.database.dao.SessionDao
import com.schoolos.android.core.database.dao.SubmissionQueueDao
import com.schoolos.android.core.database.entity.AssignmentEntity
import com.schoolos.android.core.database.entity.GradeEntity
import com.schoolos.android.core.database.entity.LearningMaterialEntity
import com.schoolos.android.core.database.entity.NotificationEntity
import com.schoolos.android.core.database.entity.QuizEntity
import com.schoolos.android.core.database.entity.SessionAttendanceEntity
import com.schoolos.android.core.database.entity.SessionEntity
import com.schoolos.android.core.database.entity.QuizQuestionEntity
import com.schoolos.android.core.database.entity.SubmissionQueueEntity

@Database(
    entities = [
        NotificationEntity::class,
        AssignmentEntity::class,
        QuizEntity::class,
        QuizQuestionEntity::class,
        LearningMaterialEntity::class,
        SubmissionQueueEntity::class,
        SessionEntity::class,
        GradeEntity::class,
        SessionAttendanceEntity::class,
    ],
    version = 8,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun notificationDao(): NotificationDao
    abstract fun assignmentDao(): AssignmentDao
    abstract fun quizDao(): QuizDao
    abstract fun learningMaterialDao(): LearningMaterialDao
    abstract fun submissionQueueDao(): SubmissionQueueDao
    abstract fun sessionDao(): SessionDao
    abstract fun gradeDao(): GradeDao
    abstract fun sessionAttendanceDao(): SessionAttendanceDao
}

