package com.example.tossday.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.tossday.domain.model.Status
import com.example.tossday.domain.model.Task
import com.example.tossday.ui.theme.SwipeGreen
import com.example.tossday.ui.theme.SwipeRed
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun TaskItem(
    task: Task,
    onDone: (Task) -> Unit,
    onDelete: (Task) -> Unit,
    onClick: () -> Unit = {},
    isHapticEnabled: Boolean = true,
    isEditMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val isDone = task.status == Status.DONE
    val scope = rememberCoroutineScope()
    val offsetX = remember(task.id) { Animatable(0f) }
    val density = LocalDensity.current
    val thresholdPx = with(density) { 120.dp.toPx() }

    var isVisible by remember(task.id) { mutableStateOf(true) }
    var pendingAction by remember(task.id) { mutableStateOf<(() -> Unit)?>(null) }

    // ФІКС: Створюємо типізовані пружини з однаковими параметрами
    val springFloat = spring<Float>(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)
    val springColor = spring<Color>(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)
    val springIntSize = spring<IntSize>(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)

    val contentAlpha by animateFloatAsState(
        targetValue = if (isDone) 0.45f else 1f,
        animationSpec = springFloat,
        label = "contentAlpha"
    )

    val textColor by animateColorAsState(
        targetValue = if (isDone) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        else MaterialTheme.colorScheme.onSurface,
        animationSpec = springColor,
        label = "textColor"
    )

    val timeColor by animateColorAsState(
        targetValue = if (isDone) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        else MaterialTheme.colorScheme.primary,
        animationSpec = springColor,
        label = "timeColor"
    )

    val surfaceColor by animateColorAsState(
        targetValue = if (isEditMode) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        else MaterialTheme.colorScheme.surface,
        animationSpec = springColor,
        label = "surfaceColor"
    )

    LaunchedEffect(isVisible) {
        if (!isVisible) {
            delay(130)
            pendingAction?.invoke()
            pendingAction = null
            isVisible = true
            offsetX.snapTo(0f)
        }
    }

    LaunchedEffect(task.id, task.status) {
        offsetX.snapTo(0f)
    }

    LaunchedEffect(isEditMode) {
        if (isEditMode && offsetX.value != 0f) {
            offsetX.animateTo(0f, springFloat)
        }
    }

    AnimatedVisibility(
        visible = isVisible,
        exit = shrinkVertically(animationSpec = tween(180)) + fadeOut(animationSpec = tween(120)),
        modifier = modifier
    ) {
        val currentOffset = offsetX.value
        val progress = (abs(currentOffset) / thresholdPx).coerceIn(0f, 1.5f)
        val easedProgress = 1f - (1f - progress.coerceIn(0f, 1f)).let { it * it }

        val elevation = lerp(0f, 8f, easedProgress)
        val scale = lerp(1f, 1.01f, easedProgress)
        val cornerRadius = lerp(0f, 16f, easedProgress)

        Box(modifier = Modifier.fillMaxWidth()) {
            // Шар підкладки для свайпу
            Row(
                modifier = Modifier
                    .matchParentSize()
                    .drawBehind {
                        val bgColor = when {
                            currentOffset > 0 && !isDone -> SwipeGreen.copy(alpha = (easedProgress * 0.85f).coerceIn(0f, 0.85f))
                            currentOffset < 0 -> SwipeRed.copy(alpha = (easedProgress * 0.85f).coerceIn(0f, 0.85f))
                            else -> Color.Transparent
                        }
                        drawRoundRect(
                            color = bgColor,
                            cornerRadius = CornerRadius(cornerRadius * density.density),
                            topLeft = Offset.Zero,
                            size = Size(size.width, size.height)
                        )
                    }
                    .padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = if (currentOffset > 0) Arrangement.Start else Arrangement.End
            ) {
                val iconAlpha = ((progress - 0.15f) / 0.85f).coerceIn(0f, 1f)
                if (currentOffset > 0 && !isDone) {
                    Icon(Icons.Default.Check, null, tint = Color.White.copy(alpha = iconAlpha), modifier = Modifier.padding(end = 8.dp))
                    Text("Виконано", color = Color.White.copy(alpha = iconAlpha), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                } else if (currentOffset < 0) {
                    Text("Видалити", color = Color.White.copy(alpha = iconAlpha), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, modifier = Modifier.padding(end = 8.dp))
                    Icon(Icons.Outlined.DeleteOutline, null, tint = Color.White.copy(alpha = iconAlpha))
                }
            }

            // Шар контенту
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(animationSpec = springIntSize) // ФІКС: Тип IntSize
                    .graphicsLayer {
                        translationX = currentOffset
                        shadowElevation = elevation
                        scaleX = scale
                        scaleY = scale
                        shape = RoundedCornerShape(cornerRadius.dp)
                        clip = true
                    }
                    .background(surfaceColor)
                    .pointerInput(task.id, task.status, isEditMode) {
                        if (isEditMode) return@pointerInput
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                scope.launch {
                                    val pastThreshold = abs(offsetX.value) > thresholdPx
                                    if (pastThreshold) {
                                        val target = if (offsetX.value > 0) size.width.toFloat() * 1.1f else -size.width.toFloat() * 1.1f
                                        offsetX.animateTo(target, spring(dampingRatio = 1f, stiffness = 400f))
                                        pendingAction = { if (offsetX.value > 0) onDone(task) else onDelete(task) }
                                        isVisible = false
                                    } else {
                                        offsetX.animateTo(0f, springFloat)
                                    }
                                }
                            },
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                scope.launch {
                                    val wasPast = abs(offsetX.value) > thresholdPx
                                    val res = if (offsetX.value * dragAmount < 0) 0.9f else 0.65f
                                    val next = offsetX.value + dragAmount * res
                                    offsetX.snapTo(next)
                                    if (abs(next) > thresholdPx && !wasPast && isHapticEnabled) {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    }
                                }
                            }
                        )
                    }
                    .padding(horizontal = 16.dp, vertical = 14.dp)
                    .clickable { onClick() }
                    .alpha(contentAlpha),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (task.time != null) {
                    Text(task.time.toString(), style = MaterialTheme.typography.bodySmall, color = timeColor, modifier = Modifier.padding(end = 12.dp))
                }
                Text(
                    text = task.text,
                    style = MaterialTheme.typography.bodyLarge.copy(textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None),
                    modifier = Modifier.weight(1f),
                    color = textColor
                )
                if (task.durationMinutes != null) {
                    Text(formatDuration(task.durationMinutes), style = MaterialTheme.typography.bodySmall, color = textColor.copy(alpha = 0.6f), modifier = Modifier.padding(start = 8.dp))
                }

                // Кнопка видалення
                AnimatedVisibility(
                    visible = isEditMode,
                    enter = fadeIn(springFloat) + expandHorizontally(expandFrom = Alignment.End, animationSpec = springIntSize),
                    exit = fadeOut(springFloat) + shrinkHorizontally(shrinkTowards = Alignment.End, animationSpec = springIntSize)
                ) {
                    Box(
                        modifier = Modifier
                            .padding(start = 12.dp)
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f))
                            .clickable {
                                if (isHapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                pendingAction = { onDelete(task) }
                                isVisible = false
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.DeleteOutline, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

private fun lerp(start: Float, end: Float, fraction: Float): Float = start + (end - start) * fraction
private fun formatDuration(minutes: Int): String = if (minutes < 60) "${minutes}хв" else "${minutes / 60}год"