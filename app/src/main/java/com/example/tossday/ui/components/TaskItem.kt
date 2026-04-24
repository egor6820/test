package com.example.tossday.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextDecoration
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

    val contentAlpha by animateFloatAsState(
        targetValue = if (isDone) 0.45f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "contentAlpha"
    )

    val textColor by animateColorAsState(
        targetValue = if (isDone) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        else MaterialTheme.colorScheme.onSurface,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "textColor"
    )

    val timeColor by animateColorAsState(
        targetValue = if (isDone) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        else MaterialTheme.colorScheme.primary,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "timeColor"
    )

    // Плавна зміна фону в режимі редагування
    val surfaceColor by animateColorAsState(
        targetValue = if (isEditMode) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        else MaterialTheme.colorScheme.surface,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
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

    // Якщо раптом увімкнули режим редагування, коли картка була трохи зсунута — повертаємо її на місце
    LaunchedEffect(isEditMode) {
        if (isEditMode && offsetX.value != 0f) {
            offsetX.animateTo(0f, spring(stiffness = Spring.StiffnessLow))
        }
    }

    AnimatedVisibility(
        visible = isVisible,
        exit = shrinkVertically(animationSpec = tween(140)) + fadeOut(animationSpec = tween(110)),
        modifier = modifier
    ) {
        val currentOffset = offsetX.value
        val progress = (abs(currentOffset) / thresholdPx).coerceIn(0f, 1.5f)
        val isRight = currentOffset > 0
        val isLeft = currentOffset < 0

        val easedProgress = 1f - (1f - progress.coerceIn(0f, 1f)).let { it * it }
        val elevation = lerp(0f, 8f, easedProgress)
        val scale = lerp(1f, 1.02f, easedProgress)
        val cornerRadius = lerp(0f, 16f, easedProgress)

        val bgAlpha = (easedProgress * 0.85f).coerceIn(0f, 0.85f)
        val bgColor: Color = when {
            isRight && !isDone -> SwipeGreen.copy(alpha = bgAlpha)
            isLeft -> SwipeRed.copy(alpha = bgAlpha)
            else -> Color.Transparent
        }
        val iconAlpha = ((progress - 0.15f) / 0.85f).coerceIn(0f, 1f)

        Box(modifier = Modifier.fillMaxWidth()) {
            // ФОНОВИЙ ШАР (Свайп)
            Row(
                modifier = Modifier
                    .matchParentSize()
                    .drawBehind {
                        drawRoundRect(
                            color = bgColor,
                            cornerRadius = CornerRadius(cornerRadius * density.density),
                            topLeft = Offset.Zero,
                            size = Size(size.width, size.height)
                        )
                    }
                    .padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = if (isRight) Arrangement.Start else Arrangement.End
            ) {
                if (isRight && !isDone) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Виконано",
                        tint = Color.White.copy(alpha = iconAlpha),
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = "Виконано",
                        color = Color.White.copy(alpha = iconAlpha),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                    )
                } else if (isLeft) {
                    Text(
                        text = "Видалити",
                        color = Color.White.copy(alpha = iconAlpha),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Icon(
                        Icons.Outlined.DeleteOutline,
                        contentDescription = "Видалити",
                        tint = Color.White.copy(alpha = iconAlpha)
                    )
                }
            }

            // ШАР З КОНТЕНТОМ
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        translationX = currentOffset
                        shadowElevation = elevation
                        scaleX = scale
                        scaleY = scale
                        shape = RoundedCornerShape(cornerRadius.dp)
                        clip = true
                    }
                    .background(surfaceColor)
                    // Блокуємо свайп, якщо увімкнено режим редагування
                    .pointerInput(task.id, task.status, isEditMode) {
                        if (isEditMode) return@pointerInput

                        val width = size.width.toFloat()
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                scope.launch {
                                    val cur = offsetX.value
                                    val pastThreshold = abs(cur) > thresholdPx
                                    val right = cur > 0
                                    val left = cur < 0

                                    if (pastThreshold) {
                                        val target = if (right) width * 1.1f else -width * 1.1f
                                        offsetX.animateTo(
                                            target,
                                            spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 300f)
                                        )
                                        pendingAction = {
                                            if (right && !isDone) onDone(task)
                                            else if (left) onDelete(task)
                                        }
                                        isVisible = false
                                    } else {
                                        offsetX.animateTo(0f, spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessMediumLow))
                                    }
                                }
                            },
                            onDragCancel = {
                                scope.launch { offsetX.animateTo(0f, spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMedium)) }
                            },
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                scope.launch {
                                    val wasPastThreshold = abs(offsetX.value) > thresholdPx
                                    val goingRight = (offsetX.value + dragAmount) > 0
                                    val resistance = when {
                                        goingRight && isDone -> 0.12f
                                        else -> {
                                            val p = (abs(offsetX.value) / (thresholdPx * 1.5f)).coerceIn(0f, 1f)
                                            lerp(0.65f, 0.92f, p)
                                        }
                                    }
                                    val newOffset = offsetX.value + dragAmount * resistance
                                    offsetX.snapTo(newOffset)

                                    val isPastThreshold = abs(newOffset) > thresholdPx
                                    if (isPastThreshold && !wasPastThreshold && isHapticEnabled) {
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
                    Text(
                        text = task.time.toString(),
                        style = MaterialTheme.typography.bodySmall,
                        color = timeColor,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }

                Text(
                    text = task.text,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None
                    ),
                    modifier = Modifier.weight(1f),
                    color = textColor
                )

                if (task.durationMinutes != null) {
                    Text(
                        text = formatDuration(task.durationMinutes),
                        style = MaterialTheme.typography.bodySmall,
                        color = textColor.copy(alpha = 0.6f),
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                // ПРЕМІАЛЬНА КНОПКА ВИДАЛЕННЯ: Плавно виїжджає справа
                AnimatedVisibility(
                    visible = isEditMode,
                    enter = fadeIn(spring(stiffness = Spring.StiffnessLow)) +
                            expandHorizontally(expandFrom = Alignment.End, animationSpec = spring(stiffness = Spring.StiffnessLow)),
                    exit = fadeOut(spring(stiffness = Spring.StiffnessLow)) +
                            shrinkHorizontally(shrinkTowards = Alignment.End, animationSpec = spring(stiffness = Spring.StiffnessLow))
                ) {
                    Box(
                        modifier = Modifier
                            .padding(start = 12.dp)
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.errorContainer)
                            .clickable {
                                if (isHapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                // Використовуємо ту ж систему pendingAction для красивого згортання картки
                                pendingAction = { onDelete(task) }
                                isVisible = false
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteOutline,
                            contentDescription = "Видалити",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun lerp(start: Float, end: Float, fraction: Float): Float = start + (end - start) * fraction

private fun formatDuration(minutes: Int): String = if (minutes < 60) "${minutes}хв" else "${minutes / 60}год"