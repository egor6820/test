package com.example.tossday.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "quick_notes")
data class QuickNoteEntity(
    @PrimaryKey val id: Long = 1,
    val text: String,
    val updatedAt: Long
)
