package com.example.tossday.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
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
    isHapticEnabled: Boolean = true,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dayOfWeekStr = remember(dayLoad.date) {
        dayLoad.date.dayOfWeek.getDisplayName(TextStyle.SHORT, ukLocale).uppercase()
    }
    val dayOfMonthStr = remember(dayLoad.date) {
        dayLoad.date.dayOfMonth.toString()
    }
    val isToday = remember(dayLoad.date) {
        dayLoad.date == LocalDate.now()
    }

    val scale by animateFloatAsState(
        targetValue = when {
            isDragHovered -> 1.05f
            isSelected -> 1.02f
            else -> 1f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "tileScale"
    )

    val animatedPercent by animateFloatAsState(
        targetValue = dayLoad.percent.coerceIn(0f, 1f),
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "loadPercent"
    )

    val borderColor by animateColorAsState(
        targetValue = when {
            isDragHovered -> MaterialTheme.colorScheme.primary
            isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
            else -> Color.Transparent
        },
        animationSpec = tween(300), label = "borderColor"
    )

    val containerColor by animateColorAsState(
        targetValue = when {
            isSelected -> MaterialTheme.colorScheme.surfaceVariant
            isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
            else -> MaterialTheme.colorScheme.surfaceContainerLow
        },
        animationSpec = tween(300), label = "containerColor"
    )

    val textColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.onSurfaceVariant
        else MaterialTheme.colorScheme.onSurface,
        animationSpec = tween(300), label = "textColor"
    )

    // ЗАМІНА CARD НА ЛЕГКИЙ BOX
    Box(
        modifier = modifier
            .width(74.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                shadowElevation = if (isSelected) 8f else 0f
                shape = RoundedCornerShape(18.dp)
                clip = false
            }
            .clip(RoundedCornerShape(18.dp)) // Обрізка для правильного ripple-ефекту натискання
            .background(containerColor)
            .border(
                width = if (isSelected || isDragHovered) 1.5.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(18.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
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
            LinearProgressIndicator(
                progress = { animatedPercent },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp),
                color = dayLoad.loadColor().copy(alpha = 0.9f),
                trackColor = textColor.copy(alpha = 0.1f),
                strokeCap = StrokeCap.Round
            )
            Text(
                text = dayLoad.taskCount.toString(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = textColor.copy(alpha = 0.4f)
            )
        }
    }
}