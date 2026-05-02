package com.example.tossday.ui.main

import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tossday.data.repository.BackupRepository
import com.example.tossday.data.repository.ImportResult
import com.example.tossday.data.repository.QuickNoteRepository
import com.example.tossday.data.repository.SettingsRepository
import com.example.tossday.data.repository.TaskRepository
import com.example.tossday.domain.model.toDomain
import com.example.tossday.domain.model.Chip
import com.example.tossday.domain.model.DayLoad
import com.example.tossday.domain.model.Status
import com.example.tossday.domain.model.Task
import com.example.tossday.domain.usecase.AssignChipToDayUseCase
import com.example.tossday.domain.usecase.GetDayLoadUseCase
import com.example.tossday.domain.usecase.ParseChipsUseCase
import com.example.tossday.notifications.AlarmScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toImmutableList // ПОТРІБЕН ЦЕЙ ІМПОРТ
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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
    private val settingsRepository: SettingsRepository,
    private val backupRepository: BackupRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        MainUiState(
            // ФІКС 1: Додано .toImmutableList()
            dayLoads = (-7..30).map { offset ->
                val date = LocalDate.now().plusDays(offset.toLong())
                DayLoad(date = date, taskCount = 0, totalMinutes = 0, percent = 0f)
            }.toImmutableList()
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
            settingsRepository.noteBackground.collect { bg ->
                _uiState.update { it.copy(noteBackground = bg) }
            }
        }

        // Об'єднуємо вантаження днів і закріплений день, щоб обчислити фінальний список тайлів
        // одним проходом. Якщо закріплений день поза вікном [-7..+30] — додаємо його в кінець
        // (з реальною статистикою з БД, або порожнім DayLoad якщо завдань немає).
        viewModelScope.launch {
            combine(getDayLoad(), settingsRepository.pinnedDate) { loadsFromDb, pinned ->
                val today = LocalDate.now()
                val windowStart = today.minusDays(7)
                val windowEnd = today.plusDays(30)

                val window = (-7..30).map { offset ->
                    val date = today.plusDays(offset.toLong())
                    loadsFromDb.find { it.date == date }
                        ?: DayLoad(date = date, taskCount = 0, totalMinutes = 0, percent = 0f)
                }

                val pinnedOutside = pinned != null &&
                        (pinned.isBefore(windowStart) || pinned.isAfter(windowEnd))

                val final = if (pinnedOutside && pinned != null) {
                    val pinnedLoad = loadsFromDb.find { it.date == pinned }
                        ?: DayLoad(date = pinned, taskCount = 0, totalMinutes = 0, percent = 0f)
                    (window + pinnedLoad).toImmutableList()
                } else {
                    window.toImmutableList()
                }
                final to pinned
            }.collect { (loads, pinned) ->
                _uiState.update { it.copy(dayLoads = loads, pinnedDate = pinned) }
            }
        }

        viewModelScope.launch {
            _uiState
                .map { it.selectedDate }
                .distinctUntilChanged()
                .flatMapLatest { date -> taskRepository.getByDate(date) }
                .collect { tasks -> _uiState.update { it.copy(selectedDayTasks = tasks) } }
        }

        viewModelScope.launch {
            try {
                // Чистимо старі завдання, але БЕРЕЖЕМО завдання закріпленого дня —
                // навіть якщо він давно вийшов із вікна 7 днів.
                val pinned = settingsRepository.pinnedDate.value
                val cutoff = LocalDate.now().minusDays(7)
                // Перед фізичним видаленням рядків з БД — скасовуємо їхні alarms у
                // AlarmManager, інакше PendingIntent-и залишаються в системному кеші
                // (orphan-и, які накопичуються місяцями).
                val staleAlarmTasks = taskRepository.getOldTasksWithAlarms(cutoff, pinned)
                staleAlarmTasks.forEach { alarmScheduler.cancel(it) }
                taskRepository.deleteOldTasksKeeping(cutoff, pinned)
            } catch (e: Exception) {
                // Ignore cleanup errors
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

    /**
     * Закріпити/відкріпити день. Якщо натиснули на той самий день, що вже закріплений —
     * відкріплюємо. Якщо на інший — переміщуємо закріплення на нього (одночасно тільки один).
     */
    fun togglePinnedDate(date: LocalDate) {
        val current = _uiState.value.pinnedDate
        settingsRepository.setPinnedDate(if (current == date) null else date)
    }

    fun clearDraft() {
        viewModelScope.launch { quickNoteRepository.clearNote() }
    }

    fun deleteTasksForDay(date: LocalDate) {
        viewModelScope.launch {
            val tasks = taskRepository.getByDate(date).first()
            tasks.forEach { alarmScheduler.cancel(it) }
            taskRepository.deleteForDate(date)
        }
    }

    fun deleteAllTasks() {
        viewModelScope.launch {
            val tasksWithAlarms = taskRepository.getAllFutureTasksWithTime()
            tasksWithAlarms.forEach { alarmScheduler.cancel(it) }
            taskRepository.deleteAll()
        }
    }

    fun resetEverything() {
        viewModelScope.launch {
            val tasksWithAlarms = taskRepository.getAllFutureTasksWithTime()
            tasksWithAlarms.forEach { alarmScheduler.cancel(it) }
            taskRepository.deleteAll()
            quickNoteRepository.clearNote()
        }
    }

    /** Експорт у JSON-рядок. Викликається з UI, який потім записує його у файл через SAF. */
    suspend fun exportBackupJson(): String = backupRepository.exportToJson()

    /**
     * Імпорт з JSON. Перед заміною даних скасовуємо ВСІ існуючі alarms (бо їхні id зникнуть),
     * після успішного імпорту пере-плануємо alarms для нових завдань з призначеним часом.
     * Повертає текст для snackbar (успіх або помилка).
     */
    suspend fun importBackupJson(json: String): String {
        // Скасовуємо всі активні alarms (поточні task id скоро зникнуть з БД).
        val existingAlarmTasks = taskRepository.getAllFutureTasksWithTime()
        existingAlarmTasks.forEach { alarmScheduler.cancel(it) }

        return when (val result = backupRepository.importFromJson(json)) {
            is ImportResult.Success -> {
                // Пере-плануємо alarms для імпортованих завдань з часом.
                result.tasksWithAlarms.forEach { entity ->
                    alarmScheduler.schedule(entity.toDomain())
                }
                "Імпортовано: ${result.taskCount} завдань"
            }
            is ImportResult.Error -> "Помилка імпорту: ${result.reason}"
        }
    }
}