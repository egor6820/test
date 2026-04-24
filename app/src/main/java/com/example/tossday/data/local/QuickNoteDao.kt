package com.example.tossday.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.tossday.data.local.entities.QuickNoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QuickNoteDao {

    @Query("SELECT * FROM quick_notes WHERE id = 1")
    fun getNote(): Flow<QuickNoteEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(note: QuickNoteEntity)

    @Query("DELETE FROM quick_notes")
    suspend fun clearNote()
}
