package com.example.tossday.ui.day

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle // ОПТИМІЗАЦІЯ БАТАРЕЇ
import com.example.tossday.ui.components.TaskItem
import com.example.tossday.ui.main.MainViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

// Кешуємо форматер, щоб не створювати його на кожному кадрі
private val formatter = DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale("uk"))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayDetailScreen(
    date: LocalDate,
    onBack: () -> Unit,
    viewModel: MainViewModel = hiltViewModel()
) {
    // 1. БЕЗПЕЧНИЙ ЗБІР СТАНУ
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // 2. ПРАВИЛЬНА РОБОТА З ПОБІЧНИМИ ЕФЕКТАМИ
    // Замість виклику функції прямо в UI, робимо це через LaunchedEffect
    LaunchedEffect(date) {
        if (uiState.selectedDate != date) {
            viewModel.onDaySelected(date)
        }
    }

    val dayLoad = uiState.dayLoads.find { it.date == date }
    val tasks = if (uiState.selectedDate == date) uiState.selectedDayTasks else emptyList()

    // Кешуємо відформатований рядок дати
    val dateText = remember(date) {
        date.format(formatter).replaceFirstChar { it.uppercase() }
    }

    // 3. ПЛАВНА АНІМАЦІЯ ПРОГРЕСУ (Spring)
    val animatedProgress by animateFloatAsState(
        targetValue = dayLoad?.percent?.coerceIn(0f, 1f) ?: 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "progressAnimation"
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = dateText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // БЛОК СТАТИСТИКИ ДНЯ
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.fillMaxWidth(),
                    color = dayLoad?.loadColor() ?: MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                    strokeCap = StrokeCap.Round
                )
                Text(
                    text = "${((dayLoad?.percent ?: 0f) * 100).toInt()}%  •  ${dayLoad?.taskCount ?: 0} задач",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // БЛОК СПИСКУ ЗАВДАНЬ
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 8.dp, bottom = 40.dp)
            ) {
                items(
                    items = tasks,
                    key = { it.id },
                    contentType = { "taskItem" } // 4. ОПТИМІЗАЦІЯ ПЕРЕМАЛЬОВУВАННЯ
                ) { task ->
                    TaskItem(
                        task = task,
                        onDone = viewModel::onTaskDone,
                        onDelete = viewModel::onTaskDeleted,
                        isEditMode = uiState.isEditMode,
                        isHapticEnabled = uiState.isHapticEnabled,
                        modifier = Modifier.animateItem( // 5. ПРЕМІАЛЬНЕ РОЗСУВАННЯ
                            fadeInSpec = spring(stiffness = Spring.StiffnessLow),
                            fadeOutSpec = spring(stiffness = Spring.StiffnessLow),
                            placementSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessLow
                            )
                        )
                    )
                }
            }
        }
    }
}