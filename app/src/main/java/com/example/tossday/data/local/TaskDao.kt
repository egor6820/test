package com.example.tossday.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.tossday.data.local.entities.DayStatsRow
import com.example.tossday.data.local.entities.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    @Query("SELECT * FROM tasks WHERE date IS NULL AND status = 'TODO' ORDER BY createdAt DESC")
    fun getInbox(): Flow<List<TaskEntity>>

    @Query("""
        SELECT * FROM tasks WHERE date = :date
        ORDER BY
            CASE status WHEN 'TODO' THEN 0 ELSE 1 END,
            CASE WHEN time IS NULL THEN 1 ELSE 0 END,
            time,
            itemOrder
    """)
    fun getByDate(date: String): Flow<List<TaskEntity>>

    @Query("""
        SELECT
            date,
            COUNT(*) as taskCount,
            COALESCE(SUM(durationMinutes), 0) + (COUNT(*) - COUNT(durationMinutes)) * 30 as totalMinutes
        FROM tasks
        WHERE date IS NOT NULL AND status = 'TODO'
        GROUP BY date
    """)
    fun getDayStats(): Flow<List<DayStatsRow>>

    @Query("SELECT * FROM tasks WHERE status = 'TODO' AND date IS NOT NULL AND time IS NOT NULL")
    suspend fun getAllFutureTasksWithTime(): List<TaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(task: TaskEntity): Long

    @Update
    suspend fun update(task: TaskEntity)

    @Delete
    suspend fun delete(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE date < :cutoffDate")
    suspend fun deleteOldTasks(cutoffDate: String)

    @Query("DELETE FROM tasks WHERE date = :date")
    suspend fun deleteByDate(date: String)

    @Query("DELETE FROM tasks")
    suspend fun deleteAll()
}
