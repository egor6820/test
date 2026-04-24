package com.example.tossday.data.repository

import com.example.tossday.data.local.TaskDao
import com.example.tossday.data.local.entities.DayStatsRow
import com.example.tossday.domain.model.Task
import com.example.tossday.domain.model.toDomain
import com.example.tossday.domain.model.toEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.LocalDate
import javax.inject.Inject

class TaskRepository @Inject constructor(
    private val taskDao: TaskDao
) {
    fun getInbox(): Flow<List<Task>> =
        taskDao.getInbox()
            .map { list -> list.map { it.toDomain() } }
            // Виконуємо конвертацію списків на фоновому потоці для обчислень
            .flowOn(Dispatchers.Default)

    fun getByDate(date: LocalDate): Flow<List<Task>> =
        taskDao.getByDate(date.toString())
            .map { list -> list.map { it.toDomain() } }
            .flowOn(Dispatchers.Default)

    fun getDayStats(): Flow<List<DayStatsRow>> =
        taskDao.getDayStats()
            .flowOn(Dispatchers.IO)

    suspend fun getAllFutureTasksWithTime(): List<Task> =
        withContext(Dispatchers.Default) {
            // Мапінг важкий, тому робимо його на Default
            taskDao.getAllFutureTasksWithTime().map { it.toDomain() }
        }

    suspend fun save(task: Task) = withContext(Dispatchers.IO) {
        taskDao.upsert(task.toEntity())
    }

    suspend fun update(task: Task) = withContext(Dispatchers.IO) {
        taskDao.update(task.toEntity())
    }

    suspend fun delete(task: Task) = withContext(Dispatchers.IO) {
        taskDao.delete(task.toEntity())
    }

    suspend fun deleteOldTasks(cutoffDate: LocalDate) = withContext(Dispatchers.IO) {
        taskDao.deleteOldTasks(cutoffDate.toString())
    }

    suspend fun deleteForDate(date: LocalDate) = withContext(Dispatchers.IO) {
        taskDao.deleteByDate(date.toString())
    }

    suspend fun deleteAll() = withContext(Dispatchers.IO) {
        taskDao.deleteAll()
    }
}