package com.example.tossday.data.repository

import com.example.tossday.data.local.QuickNoteDao
import com.example.tossday.data.local.entities.QuickNoteEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class QuickNoteRepository @Inject constructor(
    private val quickNoteDao: QuickNoteDao
) {
    fun getNote(): Flow<String> =
        quickNoteDao.getNote().map { it?.text ?: "" }

    suspend fun saveNote(text: String) {
        quickNoteDao.upsert(
            QuickNoteEntity(
                id = 1,
                text = text,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun clearNote() {
        quickNoteDao.clearNote()
    }
}
