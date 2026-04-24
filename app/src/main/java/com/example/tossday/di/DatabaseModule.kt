package com.example.tossday.di

import android.content.Context
import com.example.tossday.data.local.AppDatabase
import com.example.tossday.data.local.QuickNoteDao
import com.example.tossday.data.local.TaskDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        AppDatabase.create(context)

    @Provides
    fun provideTaskDao(db: AppDatabase): TaskDao = db.taskDao()

    @Provides
    fun provideQuickNoteDao(db: AppDatabase): QuickNoteDao = db.quickNoteDao()
}
