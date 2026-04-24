package com.example.tossday.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,
    val date: String?,
    val time: String?,
    val durationMinutes: Int?,
    val status: String,
    val energy: String,
    val itemOrder: Int,
    val createdAt: Long
)
