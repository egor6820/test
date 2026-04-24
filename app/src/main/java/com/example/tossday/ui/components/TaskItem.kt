package com.example.tossday.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
    // Edit mode: simple row with delete button, no swipe
    if (isEditMode) {
        EditModeTaskItem(task = task, onDelete = onDelete, modifier = modifier)
        return
    }

    val haptic = LocalHapticFeedback.current
    val isDone = task.status == Status.DONE
    val scope = rememberCoroutineScope()
    val offsetX = remember(task.id) { Animatable(0f) }
    val density = LocalDensity.current
    val thresholdPx = with(density) { 120.dp.toPx() }

    // Controls the smooth collapse after swipe-off
    var isVisible by remember(task.id) { mutableStateOf(true) }
    // Pending action to fire after collapse animation finishes
    var pendingAction by remember(task.id) { mutableStateOf<(() -> Unit)?>(null) }

    // Fire the action after collapse animation completes
    LaunchedEffect(isVisible) {
        if (!isVisible) {
            delay(130) // wait for shrink + fade to finish (matches anim durations below)
            pendingAction?.invoke()
            pendingAction = null
            // Reset state for potential recomposition (e.g. undo)
            isVisible = true
            offsetX.snapTo(0f)
        }
    }

    // Reset offset when task status changes — fixes re-swipe green bug
    LaunchedEffect(task.id, task.status) {
        offsetX.snapTo(0f)
    }

    // ── Collapse wrapper: card + background shrink together ──
    AnimatedVisibility(
        visible = isVisible,
        exit = shrinkVertically(
            animationSpec = tween(140)
        ) + fadeOut(
            animationSpec = tween(110)
        )
    ) {
        // Derived visual values
        val currentOffset = offsetX.value
        val progress = (abs(currentOffset) / thresholdPx).coerceIn(0f, 1.5f)
        val isRight = currentOffset > 0
        val isLeft = currentOffset < 0

        // Smooth easing function for visual properties (ease-out feel)
        val easedProgress = 1f - (1f - progress.coerceIn(0f, 1f)).let { it * it }

        // Sticky peel effect: gentle ramp-up of elevation, scale, corner radius
        val elevation = lerp(0f, 6f, easedProgress)
        val scale = lerp(1f, 1.01f, easedProgress)
        val cornerRadius = lerp(0f, 14f, easedProgress)

        // Background color behind the card — smooth fade-in
        val bgAlpha = (easedProgress * 0.8f).coerceIn(0f, 0.8f)
        val bgColor: Color = when {
            isRight && !isDone -> SwipeGreen.copy(alpha = bgAlpha)
            isLeft -> SwipeRed.copy(alpha = bgAlpha)
            else -> Color.Transparent
        }
        val iconAlpha = ((progress - 0.15f) / 0.85f).coerceIn(0f, 1f)

        Box(modifier = modifier.fillMaxWidth()) {
            // ── Background layer (revealed when content slides) ──
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
                        style = MaterialTheme.typography.bodyLarge
                    )
                } else if (isLeft) {
                    Text(
                        text = "Видалити",
                        color = Color.White.copy(alpha = iconAlpha),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Icon(
                        Icons.Outlined.DeleteOutline,
                        contentDescription = "Видалити",
                        tint = Color.White.copy(alpha = iconAlpha)
                    )
                }
            }

            // ── Content layer with sticky peel effect ──
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
                    .background(MaterialTheme.colorScheme.surface)
                    .pointerInput(task.id, task.status) {
                        val width = size.width.toFloat()
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                scope.launch {
                                    val cur = offsetX.value
                                    val pastThreshold = abs(cur) > thresholdPx
                                    val right = cur > 0
                                    val left = cur < 0

                                    if (pastThreshold) {
                                        // Gentle spring slide-off
                                        val target =
                                            if (right) width * 1.1f else -width * 1.1f
                                        offsetX.animateTo(
                                            target,
                                            spring(
                                                dampingRatio = Spring.DampingRatioNoBouncy,
                                                stiffness = 300f
                                            )
                                        )
                                        // Store action, then trigger collapse
                                        pendingAction = {
                                            if (right && !isDone) onDone(task)
                                            else if (left) onDelete(task)
                                        }
                                        isVisible = false
                                    } else {
                                        // Soft spring back — gentle, no harsh bounce
                                        offsetX.animateTo(
                                            0f,
                                            spring(
                                                dampingRatio = 0.65f,
                                                stiffness = Spring.StiffnessMediumLow
                                            )
                                        )
                                    }
                                }
                            },
                            onDragCancel = {
                                scope.launch {
                                    offsetX.animateTo(
                                        0f,
                                        spring(
                                            dampingRatio = 0.7f,
                                            stiffness = Spring.StiffnessMedium
                                        )
                                    )
                                }
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
                    .alpha(if (isDone) 0.45f else 1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (task.time != null) {
                    Text(
                        text = task.time.toString(),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isDone)
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        else
                            MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
                Text(
                    text = task.text,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        textDecoration = if (isDone) TextDecoration.LineThrough
                        else TextDecoration.None
                    ),
                    modifier = Modifier.weight(1f),
                    color = if (isDone)
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    else
                        MaterialTheme.colorScheme.onSurface
                )
                if (task.durationMinutes != null) {
                    Text(
                        text = formatDuration(task.durationMinutes),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(
                            alpha = if (isDone) 0.3f else 0.5f
                        ),
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun EditModeTaskItem(
    task: Task,
    onDelete: (Task) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDone = task.status == Status.DONE
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .alpha(if (isDone) 0.45f else 1f),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (task.time != null) {
            Text(
                text = task.time.toString(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(end = 8.dp)
            )
        }
        Text(
            text = task.text,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurface
        )
        if (task.durationMinutes != null) {
            Text(
                text = formatDuration(task.durationMinutes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
        androidx.compose.material3.IconButton(onClick = { onDelete(task) }) {
            Icon(
                imageVector = Icons.Outlined.DeleteOutline,
                contentDescription = "Видалити",
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}

private fun lerp(start: Float, end: Float, fraction: Float): Float =
    start + (end - start) * fraction

private fun formatDuration(minutes: Int): String =
    if (minutes < 60) "${minutes}хв" else "${minutes / 60}год"
