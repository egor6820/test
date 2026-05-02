package com.example.tossday.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.tossday.domain.model.DayLoad
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

private val ukLocale = Locale("uk")

@Composable
fun DayTile(
    dayLoad: DayLoad,
    isSelected: Boolean,
    isDragHovered: Boolean,
    isPinned: Boolean = false,
    isHapticEnabled: Boolean = true,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    // 1. КЕШУВАННЯ КОНСТАНТ (Виконується лише при зміні дати)
    val dayOfWeekStr = remember(dayLoad.date) {
        dayLoad.date.dayOfWeek.getDisplayName(TextStyle.SHORT, ukLocale).uppercase()
    }
    val dayOfMonthStr = remember(dayLoad.date) {
        dayLoad.date.dayOfMonth.toString()
    }
    val isToday = remember(dayLoad.date) {
        dayLoad.date == LocalDate.now()
    }

    // 2. ОПТИМІЗОВАНІ АНІМАЦІЇ
    val scale by animateFloatAsState(
        targetValue = when {
            isDragHovered -> 1.06f
            isSelected -> 1.02f
            else -> 1f
        },
        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow),
        label = "scale"
    )

    val animatedPercent by animateFloatAsState(
        targetValue = dayLoad.percent.coerceIn(0f, 1f),
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "percent"
    )

    val containerColor by animateColorAsState(
        targetValue = when {
            isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
            isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            else -> MaterialTheme.colorScheme.surfaceContainerLow
        },
        animationSpec = tween(250), label = "bg"
    )

    val textColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
    else MaterialTheme.colorScheme.onSurface

    val progressColor = dayLoad.loadColor()

    // 3. ЛОГІКА МАЛЮВАННЯ ТА ШАРІВ
    Box(
        modifier = modifier
            .width(74.dp)
            .graphicsLayer {
                // Використовуємо відеокарту для трансформацій
                scaleX = scale
                scaleY = scale
                // shadowElevation робимо невеликим, щоб не перевантажувати GPU
                shadowElevation = if (isSelected) 6f else 0f
                shape = RoundedCornerShape(20.dp)
                clip = false
                // Магічний параметр для стабільності прозорості
                compositingStrategy = CompositingStrategy.ModulateAlpha
            }
            .clip(RoundedCornerShape(20.dp))
            .background(containerColor)
            .border(
                width = when {
                    isSelected || isDragHovered -> 1.5.dp
                    isPinned -> 1.dp
                    else -> 0.5.dp
                },
                color = when {
                    isSelected || isDragHovered -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    isPinned -> MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                    else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                },
                shape = RoundedCornerShape(20.dp)
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick?.let {
                    {
                        if (isHapticEnabled) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        it()
                    }
                }
            )
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = dayOfWeekStr,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = textColor.copy(alpha = 0.5f)
            )

            Text(
                text = dayOfMonthStr,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = if (isToday && !isSelected) MaterialTheme.colorScheme.primary else textColor
            )

            // 4. НАДШВИДКИЙ ПРОГРЕС БАР (Замість LinearProgressIndicator)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .padding(horizontal = 10.dp)
                    .drawBehind {
                        val trackColor = textColor.copy(alpha = 0.1f)
                        val radius = size.height / 2

                        // Малюємо фон (трек)
                        drawRoundRect(
                            color = trackColor,
                            size = size,
                            cornerRadius = CornerRadius(radius, radius)
                        )

                        // Малюємо заповнення (прогрес)
                        drawRoundRect(
                            color = progressColor,
                            size = Size(width = size.width * animatedPercent, height = size.height),
                            cornerRadius = CornerRadius(radius, radius)
                        )
                    }
            )

            Text(
                text = dayLoad.taskCount.toString(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = progressColor.copy(alpha = 0.8f)
            )
        }

        if (isPinned) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 4.dp, end = 4.dp)
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PushPin,
                    contentDescription = "Закріплено",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(10.dp)
                )
            }
        }
    }
}