package com.example.tossday.domain.usecase

import com.example.tossday.data.repository.TaskRepository
import com.example.tossday.domain.model.DayLoad
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

class GetDayLoadUseCase @Inject constructor(
    private val taskRepository: TaskRepository
) {
    operator fun invoke(capacityMinutes: Int = 480): Flow<List<DayLoad>> =
        taskRepository.getDayStats().map { rows ->
            rows.map { row ->
                DayLoad(
                    date = LocalDate.parse(row.date),
                    taskCount = row.taskCount,
                    totalMinutes = row.totalMinutes,
                    capacityMinutes = capacityMinutes,
                    percent = (row.totalMinutes.toFloat() / capacityMinutes).coerceIn(0f, 1.5f)
                )
            }
        }
}
