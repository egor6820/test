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
import androidx.compose.foundation.ExperimentalFoundationApi
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

@OptIn(ExperimentalFoundationApi::class) // Необхідно для анімації переміщення елементів списку
@Composable
fun ChipsRow(
    chips: List<Chip>,
    onChipTap: (Chip) -> Unit,
    onChipDragStart: (Chip) -> Unit,
    isHapticEnabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val animatedKeys = rememberSaveable(
        saver = listSaver(
            save = { it.toList() },
            restore = { it.toMutableSet() }
        )
    ) { mutableSetOf<String>() }

    val listState = rememberLazyListState()
    val size = chips.size

    LaunchedEffect(size) {
        val currentSet = (0 until size).map { i -> "chip_${size - 1 - i}" }.toSet()
        val hasNew = currentSet.any { it !in animatedKeys }
        animatedKeys.retainAll(currentSet)
        // Плавний скрол на початок при додаванні нового чипа
        if (hasNew) listState.animateScrollToItem(0)
    }

    LazyRow(
        state = listState,
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(
            items = chips,
            key = { index, _ -> "chip_${size - 1 - index}" },
            contentType = { _, _ -> "chipItem" } // Оптимізація рендерингу
        ) { index, chip ->
            val key = "chip_${size - 1 - index}"
            val transitionState = remember(key) {
                val already = key in animatedKeys
                MutableTransitionState(initialState = already).apply { targetState = true }
            }
            SideEffect { animatedKeys.add(key) }

            AnimatedVisibility(
                visibleState = transitionState,
                // ВИПРАВЛЕНО: Використовуємо animateItem() замість animateItemPlacement()
                modifier = Modifier.animateItem(
                    fadeInSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessLow
                    ),
                    fadeOutSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessLow
                    ),
                    placementSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                ),
                // Преміальна анімація появи: знизу-вгору + збільшення + проявлення
                enter = fadeIn(tween(250)) +
                        slideInVertically(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            ),
                            initialOffsetY = { it } // Виїжджає повністю знизу
                        ) +
                        scaleIn(
                            initialScale = 0.8f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            )
                        ),
                exit = fadeOut(tween(150)) + scaleOut(
                    targetScale = 0.8f,
                    animationSpec = tween(150)
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

    // Пружинна реакція на натискання
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
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
        // Вимикаємо стандартну тінь для чистішого вигляду
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
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
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = chip.text,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f, fill = false)
            )
            if (chip.durationMinutes != null) {
                Icon(
                    imageVector = Icons.Default.AccessTime,
                    contentDescription = null,
                    modifier = Modifier.padding(start = 2.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
                Text(
                    text = formatDuration(chip.durationMinutes),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}

private fun formatDuration(minutes: Int): String =
    if (minutes < 60) "${minutes}хв" else "${minutes / 60}год"