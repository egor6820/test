package com.example.tossday.ui.main

import androidx.compose.ui.geometry.Offset
import com.example.tossday.domain.model.Chip
import com.example.tossday.domain.model.DayLoad
import com.example.tossday.domain.model.Task
import java.time.LocalDate
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class MainUiState(
    val quickNoteText: String = "",
    val chips: List<Chip> = emptyList(),
    val dayLoads: ImmutableList<DayLoad> = persistentListOf(),
    val selectedDate: LocalDate = LocalDate.now(),
    val selectedDayTasks: List<Task> = emptyList(),
    val dragState: DragState = DragState(),
    val lastAction: UndoableAction? = null,
    val showUndoSnackbar: Boolean = false,
    val isHapticEnabled: Boolean = true,
    val isEditMode: Boolean = false
)

data class DragState(
    val isDragging: Boolean = false,
    val draggingChip: Chip? = null,
    val dragOffset: Offset = Offset.Zero,
    val hoveredDate: LocalDate? = null
)

sealed class UndoableAction {
    data class ChipAssigned(val chip: Chip, val date: LocalDate) : UndoableAction()
    data class TaskDone(val task: Task) : UndoableAction()
    data class TaskDeleted(val task: Task) : UndoableAction()
}
