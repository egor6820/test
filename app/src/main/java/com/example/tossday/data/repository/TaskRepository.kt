package com.example.tossday.data.repository

import com.example.tossday.data.local.TaskDao
import com.example.tossday.data.local.entities.DayStatsRow
import com.example.tossday.domain.model.Task
import com.example.tossday.domain.model.toDomain
import com.example.tossday.domain.model.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

class TaskRepository @Inject constructor(
    private val taskDao: TaskDao
) {
    fun getInbox(): Flow<List<Task>> =
        taskDao.getInbox().map { list -> list.map { it.toDomain() } }

    fun getByDate(date: LocalDate): Flow<List<Task>> =
        taskDao.getByDate(date.toString()).map { list -> list.map { it.toDomain() } }

    fun getDayStats(): Flow<List<DayStatsRow>> =
        taskDao.getDayStats()

    suspend fun getAllFutureTasksWithTime(): List<Task> =
        taskDao.getAllFutureTasksWithTime().map { it.toDomain() }

    suspend fun save(task: Task) {
        taskDao.upsert(task.toEntity())
    }

    suspend fun update(task: Task) {
        taskDao.update(task.toEntity())
    }

    suspend fun delete(task: Task) {
        taskDao.delete(task.toEntity())
    }

    suspend fun deleteOldTasks(cutoffDate: LocalDate) {
        taskDao.deleteOldTasks(cutoffDate.toString())
    }

    suspend fun deleteForDate(date: LocalDate) {
        taskDao.deleteByDate(date.toString())
    }

    suspend fun deleteAll() {
        taskDao.deleteAll()
    }
}
