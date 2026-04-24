package com.example.tossday.domain.model

import com.example.tossday.data.local.entities.TaskEntity
import java.time.LocalDate
import java.time.LocalTime

data class Task(
    val id: Long = 0,
    val text: String,
    val date: LocalDate?,
    val time: LocalTime?,
    val durationMinutes: Int?,
    val status: Status,
    val energy: EnergyLevel,
    val order: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

enum class Status { TODO, DONE, ARCHIVED }
enum class EnergyLevel { NONE, LOW, MEDIUM, HIGH }

fun TaskEntity.toDomain() = Task(
    id = id,
    text = text,
    date = date?.let { LocalDate.parse(it) },
    time = time?.let { LocalTime.parse(it) },
    durationMinutes = durationMinutes,
    status = Status.valueOf(status),
    energy = EnergyLevel.valueOf(energy),
    order = itemOrder,
    createdAt = createdAt
)

fun Task.toEntity() = TaskEntity(
    id = id,
    text = text,
    date = date?.toString(),
    time = time?.toString(),
    durationMinutes = durationMinutes,
    status = status.name,
    energy = energy.name,
    itemOrder = order,
    createdAt = createdAt
)
