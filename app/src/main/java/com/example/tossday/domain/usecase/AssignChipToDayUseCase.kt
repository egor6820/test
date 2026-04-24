package com.example.tossday.domain.usecase

import com.example.tossday.data.repository.TaskRepository
import com.example.tossday.domain.model.Chip
import com.example.tossday.domain.model.EnergyLevel
import com.example.tossday.domain.model.Status
import com.example.tossday.domain.model.Task
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

class AssignChipToDayUseCase @Inject constructor(
    private val taskRepository: TaskRepository
) {
    suspend operator fun invoke(chip: Chip, date: LocalDate, time: LocalTime? = null) {
        val task = Task(
            text = chip.text,
            date = date,
            time = time,
            durationMinutes = chip.durationMinutes,
            status = Status.TODO,
            energy = EnergyLevel.NONE
        )
        taskRepository.save(task)
    }
}
