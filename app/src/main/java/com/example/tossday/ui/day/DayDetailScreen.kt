package com.example.tossday.ui.day


import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.tossday.ui.components.TaskItem
import com.example.tossday.ui.main.MainViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayDetailScreen(
    date: LocalDate,
    onBack: () -> Unit,
    viewModel: MainViewModel = hiltViewModel()  // overridden by AppNavGraph with shared instance
) {
    val uiState by viewModel.uiState.collectAsState()

    val dayLoad = uiState.dayLoads.find { it.date == date }
    val tasks = if (uiState.selectedDate == date) {
        uiState.selectedDayTasks
    } else {
        viewModel.onDaySelected(date)
        emptyList()
    }

    val formatter = DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale("uk"))

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = date.format(formatter).replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (dayLoad != null) {
                LinearProgressIndicator(
                    progress = { dayLoad.percent.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    color = dayLoad.loadColor(),
                    trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                    strokeCap = StrokeCap.Round
                )
                Text(
                    text = "${(dayLoad.percent * 100).toInt()}%  •  ${dayLoad.taskCount} задач",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
                )
            }

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(tasks, key = { it.id }) { task ->
                    TaskItem(
                        task = task,
                        onDone = viewModel::onTaskDone,
                        onDelete = viewModel::onTaskDeleted,
                        isEditMode = uiState.isEditMode
                    )
                }
            }
        }
    }
}
