package com.example.tossday.di

import com.example.tossday.data.local.QuickNoteDao
import com.example.tossday.data.local.TaskDao
import com.example.tossday.data.repository.BackupRepository
import com.example.tossday.data.repository.QuickNoteRepository
import com.example.tossday.data.repository.SettingsRepository
import com.example.tossday.data.repository.TaskRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideTaskRepository(taskDao: TaskDao): TaskRepository =
        TaskRepository(taskDao)

    @Provides
    @Singleton
    fun provideQuickNoteRepository(quickNoteDao: QuickNoteDao): QuickNoteRepository =
        QuickNoteRepository(quickNoteDao)

    @Provides
    @Singleton
    fun provideBackupRepository(
        taskDao: TaskDao,
        quickNoteDao: QuickNoteDao,
        quickNoteRepository: QuickNoteRepository,
        settingsRepository: SettingsRepository
    ): BackupRepository = BackupRepository(taskDao, quickNoteDao, quickNoteRepository, settingsRepository)
}
