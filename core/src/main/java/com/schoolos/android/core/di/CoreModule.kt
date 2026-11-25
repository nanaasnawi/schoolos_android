package com.schoolos.android.core.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.schoolos.android.core.common.SettingsManager
import com.schoolos.android.core.database.AppDatabase
import com.schoolos.android.core.database.dao.AssignmentDao
import com.schoolos.android.core.database.dao.LearningMaterialDao
import com.schoolos.android.core.database.dao.NotificationDao
import com.schoolos.android.core.database.dao.QuizDao
import com.schoolos.android.core.database.dao.SubmissionQueueDao
import com.schoolos.android.core.network.NetworkMonitor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CoreModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        val migration12To13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE submission_queue ADD COLUMN tenantId TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE submission_queue ADD COLUMN userId TEXT NOT NULL DEFAULT ''")
            }
        }
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "schoolos.db",
        )
            .addMigrations(migration12To13)
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideNotificationDao(db: AppDatabase): NotificationDao = db.notificationDao()

    @Provides
    @Singleton
    fun provideAssignmentDao(db: AppDatabase): AssignmentDao = db.assignmentDao()

    @Provides
    @Singleton
    fun provideQuizDao(db: AppDatabase): QuizDao = db.quizDao()

    @Provides
    @Singleton
    fun provideLearningMaterialDao(db: AppDatabase): LearningMaterialDao = db.learningMaterialDao()

    @Provides
    @Singleton
    fun provideSubmissionQueueDao(db: AppDatabase): SubmissionQueueDao = db.submissionQueueDao()

    @Provides
    @Singleton
    fun provideSessionDao(db: AppDatabase): com.schoolos.android.core.database.dao.SessionDao = db.sessionDao()

    @Provides
    @Singleton
    fun provideGradeDao(db: AppDatabase): com.schoolos.android.core.database.dao.GradeDao = db.gradeDao()

    @Provides
    @Singleton
    fun provideSessionAttendanceDao(db: AppDatabase): com.schoolos.android.core.database.dao.SessionAttendanceDao = db.sessionAttendanceDao()

    @Provides
    @Singleton
    fun provideSettingsManager(@ApplicationContext context: Context): SettingsManager {
        return SettingsManager(context)
    }

    @Provides
    @Singleton
    fun provideNetworkMonitor(@ApplicationContext context: Context): NetworkMonitor {
        return NetworkMonitor(context)
    }
}
