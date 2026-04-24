package com.example.tossday.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.tossday.data.local.entities.QuickNoteEntity
import com.example.tossday.data.local.entities.TaskEntity

@Database(
    entities = [TaskEntity::class, QuickNoteEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun taskDao(): TaskDao
    abstract fun quickNoteDao(): QuickNoteDao

    companion object {
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "task_planner.db")
                .build()
    }
}
