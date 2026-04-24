package com.example.tossday.ui.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle // НОВИЙ ІМПОРТ
import com.example.tossday.ui.main.MainViewModel
import com.example.tossday.ui.theme.AppTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    // ОПТИМІЗАЦІЯ БАТАРЕЇ: Безпечний збір стану
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var pendingAction by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(pendingAction) {
        if (pendingAction != null) {
            delay(2500)
            pendingAction = null
        }
    }

    fun confirmOrExecute(key: String, action: () -> Unit, feedback: String) {
        if (pendingAction == key) {
            pendingAction = null
            action()
            scope.launch {
                snackbarHostState.showSnackbar(feedback, duration = SnackbarDuration.Short)
            }
        } else {
            pendingAction = key
        }
    }

    val settingsViewModel: SettingsHolderViewModel = hiltViewModel()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Налаштування", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.surface),
            contentPadding = PaddingValues(bottom = 40.dp, top = 8.dp)
        ) {
            item {
                SettingsGroup(title = "Вигляд та поведінка") {
                    // ОПТИМІЗАЦІЯ БАТАРЕЇ: Безпечний збір теми
                    val currentTheme by settingsViewModel.appTheme.collectAsStateWithLifecycle()

                    ThemePickerItem(
                        currentTheme = currentTheme,
                        onThemeSelected = { settingsViewModel.setTheme(it) }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    SettingsItem(
                        icon = Icons.Outlined.Refresh,
                        title = "Тактильний відгук",
                        subtitle = "Вібрація при взаємодії",
                        trailing = {
                            Switch(
                                checked = uiState.isHapticEnabled,
                                onCheckedChange = { viewModel.toggleHaptics() }
                            )
                        },
                        onClick = { viewModel.toggleHaptics() }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    val isEdit = uiState.isEditMode
                    SettingsItem(
                        icon = Icons.Default.Edit,
                        title = if (isEdit) "Вимкнути режим редагування" else "Режим редагування",
                        subtitle = "Видалення задач кнопкою замість свайпу",
                        onClick = {
                            viewModel.setEditMode(!isEdit)
                            if (!isEdit) onBack()
                        }
                    )
                }
            }

            item {
                val selectedDate = uiState.selectedDate
                val dateStr = remember(selectedDate) {
                    java.time.format.DateTimeFormatter
                        .ofPattern("d MMMM", java.util.Locale("uk"))
                        .format(selectedDate)
                }

                SettingsGroup(title = "Дії з даними") {
                    DestructiveItem(
                        title = "Видалити завдання за $dateStr",
                        icon = Icons.Outlined.DeleteOutline,
                        isPending = pendingAction == "deleteDay",
                        onClick = {
                            confirmOrExecute("deleteDay", { viewModel.deleteTasksForDay(selectedDate) }, "Завдання за $dateStr видалено")
                        }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    DestructiveItem(
                        title = "Видалити всі завдання",
                        icon = Icons.Outlined.DeleteOutline,
                        isPending = pendingAction == "deleteAll",
                        onClick = {
                            confirmOrExecute("deleteAll", { viewModel.deleteAllTasks() }, "Всі завдання видалено")
                        }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    DestructiveItem(
                        title = "Очистити чернетку",
                        icon = Icons.Outlined.DeleteOutline,
                        isPending = pendingAction == "clearDraft",
                        onClick = {
                            confirmOrExecute("clearDraft", { viewModel.clearDraft() }, "Чернетку очищено")
                        }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    DestructiveItem(
                        title = "Скинути все",
                        subtitle = "Безповоротне видалення всіх даних",
                        icon = Icons.Outlined.Refresh,
                        isPending = pendingAction == "resetAll",
                        onClick = {
                            confirmOrExecute("resetAll", { viewModel.resetEverything() }, "Все скинуто")
                        }
                    )
                }
            }

            item {
                SettingsGroup(title = "Про застосунок") {
                    SettingsItem(
                        icon = Icons.Outlined.Info,
                        title = "TossDay",
                        subtitle = "Версія 1.0.0 • Планувальник завдань",
                        onClick = null
                    )
                }
            }
        }
    }
}

// ── Преміальні компоненти (Групи та Елементи) ──

@Composable
private fun SettingsGroup(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 32.dp, top = 24.dp, bottom = 8.dp)
        )
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(content = content)
        }
    }
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    trailing: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(16.dp))
            trailing()
        }
    }
}

@Composable
private fun DestructiveItem(
    title: String,
    subtitle: String? = null,
    icon: ImageVector,
    isPending: Boolean,
    onClick: () -> Unit
) {
    val contentColor by animateColorAsState(
        targetValue = if (isPending) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.error,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "contentColor"
    )
    val bgColor by animateColorAsState(
        targetValue = if (isPending) MaterialTheme.colorScheme.errorContainer else Color.Transparent,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "bgColor"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (isPending) "Натисніть ще раз для підтвердження" else title,
                style = MaterialTheme.typography.bodyLarge,
                color = contentColor
            )
            if (!isPending && subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = contentColor.copy(alpha = 0.8f)
                )
            }
        }
    }
}

private data class ThemeOption(val theme: AppTheme, val color: Color, val label: String)

@Composable
private fun ThemePickerItem(
    currentTheme: AppTheme,
    onThemeSelected: (AppTheme) -> Unit
) {
    val options = listOf(
        ThemeOption(AppTheme.OCEAN, Color(0xFF1565C0), "Синій"),
        ThemeOption(AppTheme.FOREST, Color(0xFF2E7D32), "Зелений"),
        ThemeOption(AppTheme.LAVENDER, Color(0xFF6A1B9A), "Фіолетовий"),
        ThemeOption(AppTheme.NEUTRAL, Color(0xFF37474F), "Нейтральний"),
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Text(
            text = "Тема оформлення",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            options.forEach { option ->
                val isSelected = currentTheme == option.theme

                val scale by animateFloatAsState(
                    targetValue = if (isSelected) 1.15f else 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    ),
                    label = "scale"
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onThemeSelected(option.theme) }
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .scale(scale)
                            .clip(CircleShape)
                            .background(option.color)
                            .then(
                                if (isSelected) Modifier.border(
                                    width = 3.dp,
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = CircleShape
                                ) else Modifier
                            )
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = option.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}