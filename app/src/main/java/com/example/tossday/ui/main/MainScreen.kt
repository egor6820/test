package com.example.tossday.ui.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.tossday.domain.model.Task
import com.example.tossday.ui.components.ChipsRow
import com.example.tossday.ui.components.DayTile
import com.example.tossday.ui.components.QuickCaptureField
import com.example.tossday.ui.components.TaskItem
import com.example.tossday.ui.components.TimePickerSheet
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

// Pre-created formatters — avoid re-allocation on every call
private val dateFormatter = DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale("uk"))
private val shortDateFormatter = DateTimeFormatter.ofPattern("d MMMM", Locale("uk"))

@Composable
fun MainScreen(
    onDayClick: (LocalDate) -> Unit,
    onSettingsClick: () -> Unit,
    viewModel: MainViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val rowState = rememberLazyListState()
    var editingTimeForTask by remember { mutableStateOf<Task?>(null) }

    // Scroll to today (index 7 in the -7..+30 window) on first load
    LaunchedEffect(Unit) {
        rowState.scrollToItem(7)
    }

    // Snackbar: reset flag immediately, then show.
    // Undo is handled via SnackbarResult instead of a custom button.
    LaunchedEffect(uiState.showUndoSnackbar) {
        if (uiState.showUndoSnackbar) {
            val message = when (uiState.lastAction) {
                is UndoableAction.TaskDone -> "Виконано"
                is UndoableAction.TaskDeleted -> "Видалено"
                else -> "Готово"
            }
            // Reset flag first so it doesn't get stuck
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
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                // Quick capture — capped at 250dp so it doesn't push everything off screen
                QuickCaptureField(
                    text = uiState.quickNoteText,
                    onTextChange = viewModel::onQuickNoteChanged,
                    isHapticEnabled = uiState.isHapticEnabled,
                    modifier = Modifier.heightIn(max = 250.dp)
                )

                // Floating settings button
                IconButton(
                    onClick = onSettingsClick,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f), CircleShape)
                        .size(36.dp)
                ) {
                    Icon(Icons.Default.Settings, contentDescription = "Налаштування", modifier = Modifier.size(20.dp))
                }
            }

            // Chips row — always composed so rapid typing can't bounce it in/out of
            // the tree and lose per-chip animation state. Empty list = LazyRow with
            // no items (just contentPadding of ~16dp as breathing room).
            ChipsRow(
                chips = uiState.chips,
                onChipTap = { chip ->
                    viewModel.onChipAssigned(chip, uiState.selectedDate)
                },
                onChipDragStart = viewModel::onDragStarted,
                isHapticEnabled = uiState.isHapticEnabled
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 4.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            )

            // Day tiles — always 14 days visible
            LazyRow(
                state = rowState,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.dayLoads, key = { it.date.toString() }) { dayLoad ->
                    DayTile(
                        dayLoad = dayLoad,
                        isSelected = dayLoad.date == uiState.selectedDate,
                        isDragHovered = dayLoad.date == uiState.dragState.hoveredDate,
                        isHapticEnabled = uiState.isHapticEnabled,
                        onClick = { viewModel.onDaySelected(dayLoad.date) }
                    )
                }
            }

            // Day header
            Text(
                text = formatSelectedDate(uiState.selectedDate),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Task list — weight(1f) ensures it fills remaining space and scrolls
            LazyColumn(modifier = Modifier.weight(1f)) {
                if (uiState.isEditMode) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Режим редагування",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            androidx.compose.material3.TextButton(
                                onClick = { viewModel.setEditMode(false) }
                            ) {
                                Text(
                                    "Вийти",
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }
                items(uiState.selectedDayTasks, key = { it.id }) { task ->
                    TaskItem(
                        task = task,
                        onDone = viewModel::onTaskDone,
                        onDelete = viewModel::onTaskDeleted,
                        onClick = { editingTimeForTask = task },
                        isHapticEnabled = uiState.isHapticEnabled,
                        isEditMode = uiState.isEditMode
                    )
                }
            }
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

private fun formatSelectedDate(date: LocalDate): String {
    val today = LocalDate.now()
    return when (date) {
        today -> "Сьогодні, ${date.format(shortDateFormatter)}"
        today.plusDays(1) -> "Завтра, ${date.format(shortDateFormatter)}"
        today.minusDays(1) -> "Вчора, ${date.format(shortDateFormatter)}"
        else -> date.format(dateFormatter).replaceFirstChar { it.uppercase() }
    }
}

