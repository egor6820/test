package com.example.tossday.ui.main

import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tossday.data.repository.QuickNoteRepository
import com.example.tossday.data.repository.TaskRepository
import com.example.tossday.domain.model.Chip
import com.example.tossday.domain.model.Status
import com.example.tossday.domain.model.Task
import com.example.tossday.domain.usecase.AssignChipToDayUseCase
import com.example.tossday.domain.usecase.GetDayLoadUseCase
import com.example.tossday.domain.usecase.ParseChipsUseCase
import com.example.tossday.notifications.AlarmScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.example.tossday.data.repository.SettingsRepository
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val quickNoteRepository: QuickNoteRepository,
    private val parseChips: ParseChipsUseCase,
    private val assignChipToDay: AssignChipToDayUseCase,
    private val getDayLoad: GetDayLoadUseCase,
    private val alarmScheduler: AlarmScheduler,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        MainUiState(
            dayLoads = (-7..30).map { offset ->
                val date = LocalDate.now().plusDays(offset.toLong())
                com.example.tossday.domain.model.DayLoad(
                    date = date, taskCount = 0, totalMinutes = 0, percent = 0f
                )
            }
        )
    )
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            quickNoteRepository.getNote().collect { savedText ->
                _uiState.update { it.copy(quickNoteText = savedText, chips = parseChips(savedText)) }
            }
        }

        viewModelScope.launch {
            settingsRepository.isHapticEnabled.collect { isEnabled ->
                _uiState.update { it.copy(isHapticEnabled = isEnabled) }
            }
        }

        viewModelScope.launch {
            getDayLoad().collect { loadsFromDb ->
                val today = LocalDate.now()
                val window = (-7..30).map { offset ->
                    val date = today.plusDays(offset.toLong())
                    loadsFromDb.find { it.date == date } ?: com.example.tossday.domain.model.DayLoad(
                        date = date,
                        taskCount = 0,
                        totalMinutes = 0,
                        percent = 0f
                    )
                }
                _uiState.update { it.copy(dayLoads = window) }
            }
        }

        viewModelScope.launch {
            _uiState
                .map { it.selectedDate }
                .distinctUntilChanged()
                .flatMapLatest { date -> taskRepository.getByDate(date) }
                .collect { tasks -> _uiState.update { it.copy(selectedDayTasks = tasks) } }
        }

        // Clean up tasks older than 60 days automatically
        viewModelScope.launch {
            try {
                taskRepository.deleteOldTasks(LocalDate.now().minusDays(7))
            } catch (e: Exception) {
                // Ignore cleanup errors to ensure app stability
            }
        }
    }

    fun onQuickNoteChanged(text: String) {
        _uiState.update { it.copy(quickNoteText = text, chips = parseChips(text)) }
        viewModelScope.launch { quickNoteRepository.saveNote(text) }
    }

    fun onChipAssigned(chip: Chip, date: LocalDate, time: LocalTime? = null) {
        viewModelScope.launch {
            assignChipToDay(chip, date, time)
            // Schedule reminder if time is set
            if (time != null) {
                val tasks = taskRepository.getByDate(date).first()
                tasks.firstOrNull { it.text == chip.text && it.time == time }
                    ?.let { alarmScheduler.schedule(it) }
            }
            _uiState.update {
                it.copy(dragState = DragState())
            }
        }
    }

    fun onDaySelected(date: LocalDate) {
        _uiState.update { it.copy(selectedDate = date) }
    }

    fun onTaskDone(task: Task) {
        viewModelScope.launch {
            alarmScheduler.cancel(task)
            taskRepository.update(task.copy(status = Status.DONE))
            _uiState.update {
                it.copy(
                    lastAction = UndoableAction.TaskDone(task),
                    showUndoSnackbar = true
                )
            }
        }
    }

    fun onTaskReturnToInbox(task: Task) {
        viewModelScope.launch {
            alarmScheduler.cancel(task)
            taskRepository.update(task.copy(date = null, time = null))
        }
    }

    fun onTaskDeleted(task: Task) {
        viewModelScope.launch {
            alarmScheduler.cancel(task)
            taskRepository.delete(task)
            _uiState.update {
                it.copy(lastAction = UndoableAction.TaskDeleted(task), showUndoSnackbar = true)
            }
        }
    }

    fun onTaskTimeUpdated(task: Task, time: LocalTime?, durationMinutes: Int?) {
        viewModelScope.launch {
            val updatedTask = task.copy(time = time, durationMinutes = durationMinutes)
            taskRepository.update(updatedTask)
            if (time != null) {
                alarmScheduler.schedule(updatedTask)
            } else {
                alarmScheduler.cancel(task)
            }
        }
    }

    fun onUndoLastAction() {
        val action = _uiState.value.lastAction ?: return
        viewModelScope.launch {
            when (action) {
                is UndoableAction.ChipAssigned -> {
                    val tasks = taskRepository.getByDate(action.date).first()
                    tasks.firstOrNull { it.text == action.chip.text }
                        ?.let { taskRepository.delete(it) }
                }
                is UndoableAction.TaskDone -> {
                    taskRepository.update(action.task.copy(status = Status.TODO))
                    alarmScheduler.schedule(action.task)
                }
                is UndoableAction.TaskDeleted -> {
                    taskRepository.save(action.task)
                    alarmScheduler.schedule(action.task)
                }
            }
            _uiState.update { it.copy(lastAction = null, showUndoSnackbar = false) }
        }
    }

    fun onDismissUndo() {
        _uiState.update { it.copy(showUndoSnackbar = false) }
    }

    fun onDragStarted(chip: Chip) {
        _uiState.update { it.copy(dragState = DragState(isDragging = true, draggingChip = chip)) }
    }

    fun onDragMoved(offset: Offset, hoveredDate: LocalDate?) {
        _uiState.update {
            it.copy(dragState = it.dragState.copy(dragOffset = offset, hoveredDate = hoveredDate))
        }
    }

    fun onDragEnded() {
        val state = _uiState.value.dragState
        val chip = state.draggingChip
        val date = state.hoveredDate
        if (chip != null && date != null) {
            onChipAssigned(chip, date)
        } else {
            _uiState.update { it.copy(dragState = DragState()) }
        }
    }

    fun toggleHaptics() {
        val current = _uiState.value.isHapticEnabled
        settingsRepository.setHapticEnabled(!current)
    }

    fun setEditMode(enabled: Boolean) {
        _uiState.update { it.copy(isEditMode = enabled) }
    }

    fun clearDraft() {
        viewModelScope.launch { quickNoteRepository.clearNote() }
    }

    fun deleteTasksForDay(date: LocalDate) {
        viewModelScope.launch { taskRepository.deleteForDate(date) }
    }

    fun deleteAllTasks() {
        viewModelScope.launch { taskRepository.deleteAll() }
    }

    fun resetEverything() {
        viewModelScope.launch {
            taskRepository.deleteAll()
            quickNoteRepository.clearNote()
        }
    }
}
