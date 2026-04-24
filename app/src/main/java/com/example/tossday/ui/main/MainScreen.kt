package com.example.tossday.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle // НОВИЙ ІМПОРТ
import com.example.tossday.domain.model.DayLoad
import com.example.tossday.domain.model.Task
import com.example.tossday.ui.components.ChipsRow
import com.example.tossday.ui.components.DayTile
import com.example.tossday.ui.components.QuickCaptureField
import com.example.tossday.ui.components.TaskItem
import com.example.tossday.ui.components.TimePickerSheet
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dateFormatter = DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale("uk"))
private val shortDateFormatter = DateTimeFormatter.ofPattern("d MMMM", Locale("uk"))

@Composable
fun MainScreen(
    onDayClick: (LocalDate) -> Unit,
    onSettingsClick: () -> Unit,
    viewModel: MainViewModel = hiltViewModel()
) {
    // ОПТИМІЗАЦІЯ БАТАРЕЇ: Безпечний збір стану з урахуванням життєвого циклу
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    var editingTimeForTask by remember { mutableStateOf<Task?>(null) }

    LaunchedEffect(uiState.showUndoSnackbar) {
        if (uiState.showUndoSnackbar) {
            val message = when (uiState.lastAction) {
                is UndoableAction.TaskDone -> "Виконано"
                is UndoableAction.TaskDeleted -> "Видалено"
                else -> "Готово"
            }
            viewModel.onDismissUndo()
            val result = snackbarHostState.showSnackbar(
                message = message,
                actionLabel = "Скасувати",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.onUndoLastAction()
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
        ) {
            TopBarSection(
                quickNoteText = uiState.quickNoteText,
                onNoteChanged = viewModel::onQuickNoteChanged,
                isHapticEnabled = uiState.isHapticEnabled,
                onSettingsClick = onSettingsClick
            )

            ChipsRow(
                chips = uiState.chips,
                onChipTap = { chip -> viewModel.onChipAssigned(chip, uiState.selectedDate) },
                onChipDragStart = viewModel::onDragStarted,
                isHapticEnabled = uiState.isHapticEnabled
            )

            Spacer(modifier = Modifier.height(8.dp))

            DaysRowSection(
                dayLoads = uiState.dayLoads,
                selectedDate = uiState.selectedDate,
                hoveredDate = uiState.dragState.hoveredDate,
                isHapticEnabled = uiState.isHapticEnabled,
                onDaySelected = viewModel::onDaySelected
            )

            val dateHeaderText = remember(uiState.selectedDate) {
                formatSelectedDate(uiState.selectedDate)
            }
            Text(
                text = dateHeaderText,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
            )

            TasksListSection(
                modifier = Modifier.weight(1f),
                tasks = uiState.selectedDayTasks,
                isEditMode = uiState.isEditMode,
                isHapticEnabled = uiState.isHapticEnabled,
                onDone = viewModel::onTaskDone,
                onDelete = viewModel::onTaskDeleted,
                onTaskClick = { editingTimeForTask = it },
                onExitEditMode = { viewModel.setEditMode(false) }
            )
        }

        TimePickerSheet(
            isVisible = editingTimeForTask != null,
            initialTime = editingTimeForTask?.time,
            initialDuration = editingTimeForTask?.durationMinutes,
            onDismiss = { editingTimeForTask = null },
            onConfirm = { time, duration ->
                editingTimeForTask?.let { task ->
                    viewModel.onTaskTimeUpdated(task, time, duration)
                }
                editingTimeForTask = null
            }
        )
    }
}

// === ІЗОЛЬОВАНІ КОМПОНЕНТИ ===

@Composable
private fun TopBarSection(
    quickNoteText: String,
    onNoteChanged: (String) -> Unit,
    isHapticEnabled: Boolean,
    onSettingsClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        QuickCaptureField(
            text = quickNoteText,
            onTextChange = onNoteChanged,
            isHapticEnabled = isHapticEnabled,
            modifier = Modifier.heightIn(max = 250.dp)
        )
        IconButton(
            onClick = onSettingsClick,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                .border(0.5.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f), CircleShape)
                .size(40.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Налаштування",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun DaysRowSection(
    dayLoads: List<DayLoad>,
    selectedDate: LocalDate,
    hoveredDate: LocalDate?,
    isHapticEnabled: Boolean,
    onDaySelected: (LocalDate) -> Unit
) {
    val rowState = rememberLazyListState()

    LaunchedEffect(Unit) {
        rowState.scrollToItem(7)
    }

    LazyRow(
        state = rowState,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(
            items = dayLoads,
            key = { it.date.toEpochDay() },
            contentType = { "dayTile" }
        ) { dayLoad ->
            val cachedOnClick = remember(dayLoad.date) {
                { onDaySelected(dayLoad.date) }
            }

            DayTile(
                dayLoad = dayLoad,
                isSelected = dayLoad.date == selectedDate,
                isDragHovered = dayLoad.date == hoveredDate,
                isHapticEnabled = isHapticEnabled,
                onClick = cachedOnClick
            )
        }
    }
}

@Composable
private fun TasksListSection(
    modifier: Modifier = Modifier,
    tasks: List<Task>,
    isEditMode: Boolean,
    isHapticEnabled: Boolean,
    onDone: (Task) -> Unit,
    onDelete: (Task) -> Unit,
    onTaskClick: (Task) -> Unit,
    onExitEditMode: () -> Unit
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        if (isEditMode) {
            item(key = "edit_mode_header", contentType = "header") {
                Row(
                    modifier = Modifier
                        // МАГІЯ ПЛАВНОСТІ: Заголовок м'яко виштовхує задачі вниз
                        .animateItem(
                            fadeInSpec = androidx.compose.animation.core.spring(stiffness = androidx.compose.animation.core.Spring.StiffnessLow),
                            fadeOutSpec = androidx.compose.animation.core.spring(stiffness = androidx.compose.animation.core.Spring.StiffnessLow),
                            placementSpec = androidx.compose.animation.core.spring(stiffness = androidx.compose.animation.core.Spring.StiffnessLow)
                        )
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clip(MaterialTheme.shapes.large) // Більш преміальне заокруглення
                        .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)) // Елегантніший колір
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Режим редагування",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    TextButton(onClick = onExitEditMode) {
                        Text(
                            text = "Готово",
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        items(
            items = tasks,
            key = { it.id },
            contentType = { "taskItem" }
        ) { task ->
            TaskItem(
                task = task,
                onDone = onDone,
                onDelete = onDelete,
                onClick = { onTaskClick(task) },
                isHapticEnabled = isHapticEnabled,
                isEditMode = isEditMode,
                // Додаємо animateItem сюди, якщо його раніше не було
                modifier = Modifier.animateItem(
                    fadeInSpec = androidx.compose.animation.core.spring(stiffness = androidx.compose.animation.core.Spring.StiffnessLow),
                    fadeOutSpec = androidx.compose.animation.core.spring(stiffness = androidx.compose.animation.core.Spring.StiffnessLow),
                    placementSpec = androidx.compose.animation.core.spring(
                        dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy,
                        stiffness = androidx.compose.animation.core.Spring.StiffnessLow
                    )
                )
            )
        }
    }
}

private fun formatSelectedDate(date: LocalDate): String {
    val today = LocalDate.now()
    return when (date) {
        today -> "Сьогодні, ${date.format(shortDateFormatter)}"
        today.plusDays(1) -> "Завтра, ${date.format(shortDateFormatter)}"
        today.minusDays(1) -> "Вчора, ${date.format(shortDateFormatter)}"
        else -> date.format(dateFormatter).replaceFirstChar { it.uppercase() }
    }
}