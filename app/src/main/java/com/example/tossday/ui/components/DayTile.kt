package com.example.tossday.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import com.example.tossday.domain.model.DayLoad
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun DayTile(
    dayLoad: DayLoad,
    isSelected: Boolean,
    isDragHovered: Boolean,
    isHapticEnabled: Boolean = true,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = when {
            isDragHovered -> 1.05f
            isSelected -> 1.0f
            else -> 1f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "tileScale"
    )

    val animatedPercent by animateFloatAsState(
        targetValue = dayLoad.percent.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 600),
        label = "loadPercent"
    )

    val borderColor = when {
        isDragHovered -> MaterialTheme.colorScheme.primary
        isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    }

    Card(
        onClick = onClick,
        modifier = modifier
            .width(72.dp)
            .scale(scale),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            width = if (isSelected || isDragHovered) 1.5.dp else 0.5.dp,
            color = borderColor
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.surfaceVariant
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = dayLoad.date.dayOfWeek
                    .getDisplayName(TextStyle.SHORT, Locale("uk")),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Text(
                text = dayLoad.date.dayOfMonth.toString(),
                style = MaterialTheme.typography.titleMedium,
                color = if (dayLoad.date == LocalDate.now())
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurface
            )
            LinearProgressIndicator(
                progress = { animatedPercent },
                modifier = Modifier.fillMaxWidth(),
                color = dayLoad.loadColor(),
                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                strokeCap = StrokeCap.Round
            )
            Text(
                text = dayLoad.taskCount.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}
