package com.example.tossday.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.tossday.domain.model.Chip

// Виносимо специ анімацій на верхній рівень — щоб не виділяти об'єкти на кожній рекомпозиції.
private val ChipPlacementSpec = spring<androidx.compose.ui.unit.IntOffset>(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = Spring.StiffnessMediumLow
)
private val ChipFadeSpec = spring<Float>(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = Spring.StiffnessMediumLow
)
private val ChipResizeSpec = spring<androidx.compose.ui.unit.IntSize>(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = Spring.StiffnessMediumLow
)
private val ChipPressSpec = spring<Float>(
    dampingRatio = Spring.DampingRatioMediumBouncy,
    stiffness = Spring.StiffnessLow
)

@Composable
fun ChipsRow(
    chips: List<Chip>,
    onChipTap: (Chip) -> Unit,
    onChipDragStart: (Chip) -> Unit,
    isHapticEnabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    // Зберігаємо id вже-проаніміваних чипів, щоб після ротації / повторної композиції
    // не програвати enter-анімацію для тих же елементів.
    val animatedIds = rememberSaveable(
        saver = listSaver<MutableSet<Long>, Long>(
            save = { it.toList() },
            restore = { it.toMutableSet() }
        )
    ) { mutableSetOf<Long>() }

    val listState = rememberLazyListState()

    // Якщо з'явився справді новий чип (id, якого ще не бачили) — миттєво приклеюємо
    // прокрутку до 0. Снап замість animateScrollToItem прибирає ефект "виїжджає з-за екрана",
    // бо чип одразу опиняється на своїй позиції, а enter-анімація грає в місці.
    // Дописування символів у вже існуючий чип id не змінює, тому жодного скролу не буде.
    LaunchedEffect(chips) {
        val incomingIds = chips.mapTo(HashSet(chips.size)) { it.id }
        val hasNew = incomingIds.any { it !in animatedIds }
        animatedIds.retainAll(incomingIds)

        if (hasNew && chips.isNotEmpty() && listState.firstVisibleItemIndex != 0) {
            listState.scrollToItem(0)
        }
    }

    LazyRow(
        state = listState,
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            items = chips,
            key = { it.id }, // СТАБІЛЬНИЙ ключ: не змінюється поки користувач дописує символи
            contentType = { "chipItem" }
        ) { chip ->
            val id = chip.id
            val transitionState = remember(id) {
                val already = id in animatedIds
                MutableTransitionState(initialState = already).apply { targetState = true }
            }
            SideEffect { animatedIds.add(id) }

            AnimatedVisibility(
                visibleState = transitionState,
                modifier = Modifier.animateItem(
                    fadeInSpec = ChipFadeSpec,
                    fadeOutSpec = ChipFadeSpec,
                    placementSpec = ChipPlacementSpec
                ),
                // Чип "виринає" в своїй фінальній позиції, без зсуву збоку:
                // старі чипи самі плавно з'їжджають вправо завдяки animateItem placement.
                enter = fadeIn(tween(180)) + scaleIn(initialScale = 0.7f, animationSpec = tween(220)),
                exit = fadeOut(tween(140)) + scaleOut(targetScale = 0.7f, animationSpec = tween(140))
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
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = ChipPressSpec,
        label = "chipScale"
    )

    Card(
        onClick = onTap,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
            .widthIn(max = 200.dp)
            .scale(scale)
            // Плавно нарощуємо ширину, коли всередині чипа з'являються нові символи —
            // щоб текст "доповнювався", а не штампував чип кожен раз.
            .animateContentSize(animationSpec = ChipResizeSpec)
            .pointerInput(chip.id) {
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
                fontWeight = FontWeight.Medium,
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
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}

private fun formatDuration(minutes: Int): String =
    if (minutes < 60) "${minutes}хв" else "${minutes / 60}год"
