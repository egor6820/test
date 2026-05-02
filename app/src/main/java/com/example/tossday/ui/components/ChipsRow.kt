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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ContextualFlowRow
import androidx.compose.foundation.layout.ContextualFlowRowOverflow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.tossday.data.repository.ChipsLayout
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

/**
 * Перемикач між горизонтальним рядом (default, як було) і wrap-розкладкою з кнопкою
 * "+N / Згорнути". `layout` приходить із SettingsRepository → MainUiState. Дефолт
 * LINEAR — щоб старі користувачі побачили те ж саме, що й до апдейту.
 */
@Composable
fun ChipsRow(
    chips: List<Chip>,
    onChipTap: (Chip) -> Unit,
    onChipDragStart: (Chip) -> Unit,
    isHapticEnabled: Boolean = true,
    layout: ChipsLayout = ChipsLayout.LINEAR,
    modifier: Modifier = Modifier
) {
    // Спільне сховище id вже-проаніміваних чипів — переживає перемикання layout,
    // щоб при зміні режиму не програвати enter-анімацію всім чипам наново.
    val animatedIds = rememberSaveable(
        saver = listSaver<MutableSet<Long>, Long>(
            save = { it.toList() },
            restore = { it.toMutableSet() }
        )
    ) { mutableSetOf<Long>() }

    when (layout) {
        ChipsLayout.LINEAR -> LinearChipsRow(
            chips = chips,
            onChipTap = onChipTap,
            onChipDragStart = onChipDragStart,
            isHapticEnabled = isHapticEnabled,
            animatedIds = animatedIds,
            modifier = modifier
        )
        ChipsLayout.WRAP -> WrapChipsRow(
            chips = chips,
            onChipTap = onChipTap,
            onChipDragStart = onChipDragStart,
            isHapticEnabled = isHapticEnabled,
            animatedIds = animatedIds,
            modifier = modifier
        )
    }
}

@Composable
private fun LinearChipsRow(
    chips: List<Chip>,
    onChipTap: (Chip) -> Unit,
    onChipDragStart: (Chip) -> Unit,
    isHapticEnabled: Boolean,
    animatedIds: MutableSet<Long>,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    // Якщо з'явився справді новий чип (id, якого ще не бачили) — миттєво приклеюємо
    // прокрутку до самого початку (індекс 0, offset 0). Снап замість animateScrollToItem
    // прибирає ефект "виїжджає з-за екрана": чип одразу опиняється на своїй позиції,
    // а enter-анімація (fade+scale) грає в місці. Перевіряємо ще й scrollOffset, бо
    // користувач міг трохи прокрутити вправо — без цього новий чип лишався б за лівим краєм.
    // Дописування символів у вже існуючий чип id не змінює, тож жодного скролу не буде.
    LaunchedEffect(chips) {
        val incomingIds = chips.mapTo(HashSet(chips.size)) { it.id }
        val hasNew = incomingIds.any { it !in animatedIds }
        animatedIds.retainAll(incomingIds)

        if (hasNew && chips.isNotEmpty()) {
            val notAtStart = listState.firstVisibleItemIndex != 0 ||
                    listState.firstVisibleItemScrollOffset != 0
            if (notAtStart) listState.scrollToItem(0)
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

// ── Wrap layout (ContextualFlowRow + collapse/expand) ──
//
// ContextualFlowRow (а не FlowRow) — бо саме він дозволяє читати totalItemCount/shownItemCount
// під час composition phase в lambda overflow-індикатора. У FlowRow ці значення доступні
// тільки в draw phase — спроба прочитати їх для рендеру тексту "+N" викликає
// IllegalStateException.
//
// Згорнутий режим — рівно 2 рядки. "+N ↓" автоматично з'являється в кінці другого ряду
// (Compose резервує місце під нього на етапі вимірювання — через це решта чипів не
// дриґаються коли індикатор зʼявляється/зникає).
// Розгорнутий режим — всі чипи + кнопка "↑ Згорнути" в природному кінці потоку.

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WrapChipsRow(
    chips: List<Chip>,
    onChipTap: (Chip) -> Unit,
    onChipDragStart: (Chip) -> Unit,
    isHapticEnabled: Boolean,
    animatedIds: MutableSet<Long>,
    modifier: Modifier = Modifier
) {
    if (chips.isEmpty()) return

    val haptic = LocalHapticFeedback.current
    var isExpanded by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(chips) {
        if (animatedIds.isNotEmpty()) {
            val present = chips.mapTo(HashSet(chips.size)) { it.id }
            animatedIds.retainAll(present)
        }
    }

    val onToggle: () -> Unit = onToggle@{
        if (isHapticEnabled) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
        isExpanded = !isExpanded
    }

    val overflow = if (isExpanded) {
        ContextualFlowRowOverflow.Visible
    } else {
        ContextualFlowRowOverflow.expandIndicator {
            // ContextualFlowRowOverflowScope ініціалізує ці значення під час composition phase,
            // тому читання тут безпечне (на відміну від FlowRowOverflowScope).
            val hidden = totalItemCount - shownItemCount
            if (hidden > 0) {
                ToggleChip(
                    isExpanded = false,
                    hiddenCount = hidden,
                    onClick = onToggle
                )
            }
        }
    }

    // У expanded режимі додаємо "Згорнути" як +1 item у кінці потоку.
    val totalCount = if (isExpanded) chips.size + 1 else chips.size

    ContextualFlowRow(
        itemCount = totalCount,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            // Плавна зміна висоти при expand/collapse. Spring без баунсу — щоб не пружинило.
            .animateContentSize(animationSpec = ChipResizeSpec),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        maxLines = if (isExpanded) Int.MAX_VALUE else 2,
        overflow = overflow
    ) { index ->
        if (index < chips.size) {
            val chip = chips[index]
            // key(chip.id) — стабільна слот-ідентичність. Без неї додавання нового чипа
            // на початок списку зміщує позиційні слоти Compose, і ВСІ чипи отримують
            // нові MutableTransitionState → enter-анімація грається у всіх → джанк.
            key(chip.id) {
                val transitionState = remember {
                    MutableTransitionState(initialState = chip.id in animatedIds)
                        .apply { targetState = true }
                }
                SideEffect { animatedIds.add(chip.id) }

                AnimatedVisibility(
                    visibleState = transitionState,
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
        } else {
            // index == chips.size: чип "Згорнути" в кінці потоку (тільки коли isExpanded).
            ToggleChip(
                isExpanded = true,
                hiddenCount = 0,
                onClick = onToggle
            )
        }
    }
}

@Composable
private fun ToggleChip(
    isExpanded: Boolean,
    hiddenCount: Int,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1f,
        animationSpec = ChipPressSpec,
        label = "toggleChipScale"
    )

    val container = if (isExpanded) {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    } else {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
    }
    val border = if (isExpanded) {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
    } else {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
    }
    val tint = if (isExpanded) {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
    } else {
        MaterialTheme.colorScheme.primary
    }

    Card(
        onClick = onClick,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = container),
        border = BorderStroke(1.dp, border),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .scale(scale)
            .animateContentSize(animationSpec = ChipResizeSpec)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            if (isExpanded) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowUp,
                    contentDescription = "Згорнути",
                    tint = tint
                )
                Text(
                    text = "Згорнути",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = tint
                )
            } else {
                Text(
                    text = "+$hiddenCount",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = tint
                )
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Показати ще",
                    tint = tint.copy(alpha = 0.85f)
                )
            }
        }
    }
}
