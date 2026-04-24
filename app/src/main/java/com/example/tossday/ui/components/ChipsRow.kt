package com.example.tossday.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.tossday.domain.model.Chip

@Composable
fun ChipsRow(
    chips: List<Chip>,
    onChipTap: (Chip) -> Unit,
    onChipDragStart: (Chip) -> Unit,
    isHapticEnabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    // Persistent record of keys that have already played their enter animation.
    // Survives nav pop (Settings → back) and activity recreation.
    val animatedKeys = rememberSaveable(
        saver = listSaver(
            save = { it.toList() },
            restore = { it.toMutableSet() }
        )
    ) { mutableSetOf<String>() }

    val listState = rememberLazyListState()
    val size = chips.size

    // When keys disappear entirely, drop them from the record so a re-add animates fresh.
    // Triggered on size change only — typing letters within a line doesn't re-run this.
    LaunchedEffect(size) {
        val currentSet = (0 until size).map { i -> "chip_${size - 1 - i}" }.toSet()
        val hasNew = currentSet.any { it !in animatedKeys }
        animatedKeys.retainAll(currentSet)
        if (hasNew) listState.scrollToItem(0)
    }

    LazyRow(
        state = listState,
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(
            items = chips,
            key = { index, _ -> "chip_${size - 1 - index}" }
        ) { index, chip ->
            val key = "chip_${size - 1 - index}"
            // Each chip owns a MutableTransitionState tied to its key. If this key was
            // previously seen, start visible — no enter. If it's new, start hidden and
            // let AnimatedVisibility flip it on, triggering the enter animation once.
            // This avoids the one-frame-invisible flicker that the old visibleKeys
            // state could cause during fast typing.
            val transitionState = remember(key) {
                val already = key in animatedKeys
                MutableTransitionState(initialState = already).apply { targetState = true }
            }
            SideEffect { animatedKeys.add(key) }

            AnimatedVisibility(
                visibleState = transitionState,
                enter = fadeIn(tween(160)) + slideInVertically(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    ),
                    initialOffsetY = { it / 2 }
                ) + scaleIn(
                    initialScale = 0.85f,
                    animationSpec = tween(160)
                ),
                exit = fadeOut(tween(120)) + scaleOut(
                    targetScale = 0.85f,
                    animationSpec = tween(120)
                )
            ) {
                ChipItem(
                    chip = chip,
                    onTap = { onChipTap(chip) },
                    onDragStart = { onChipDragStart(chip) },
                    isHapticEnabled = isHapticEnabled
                )
            }
        }
    }
}

@Composable
private fun ChipItem(
    chip: Chip,
    onTap: () -> Unit,
    onDragStart: () -> Unit,
    isHapticEnabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "chipScale"
    )

    Card(
        onClick = onTap,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = modifier
            .widthIn(max = 200.dp)
            .scale(scale)
            .pointerInput(chip.text) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { onDragStart() },
                    onDrag = { _, _ -> }
                )
            }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = chip.text,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            if (chip.durationMinutes != null) {
                Icon(
                    imageVector = Icons.Default.AccessTime,
                    contentDescription = null,
                    modifier = Modifier.padding(start = 2.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                Text(
                    text = formatDuration(chip.durationMinutes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

private fun formatDuration(minutes: Int): String =
    if (minutes < 60) "${minutes}хв" else "${minutes / 60}год"
