package com.example.tossday.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun QuickCaptureField(
    text: String,
    onTextChange: (String) -> Unit,
    isHapticEnabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }

    var initialCursorSet by remember { mutableStateOf(false) }
    var showCleanupDialog by remember { mutableStateOf(false) }
    var hasAutoFocused by rememberSaveable { mutableStateOf(false) }

    var textFieldValue by remember { mutableStateOf(TextFieldValue(text)) }

    LaunchedEffect(text) {
        if (textFieldValue.text != text) {
            if (!initialCursorSet && text.isNotEmpty()) {
                val displayText = if (!text.endsWith("\n")) "$text\n" else text
                textFieldValue = TextFieldValue(displayText, TextRange(displayText.length))
                initialCursorSet = true
                if (displayText != text) onTextChange(displayText)
            } else {
                textFieldValue = textFieldValue.copy(text = text)
            }
        }
    }

    LaunchedEffect(Unit) {
        if (!hasAutoFocused) {
            delay(250)
            focusRequester.requestFocus()
            hasAutoFocused = true
        }
    }

    // ФІКС: Прибрано animateContentSize, щоб уникнути конфліктів з клавіатурою (IME)
    Box(modifier = modifier.fillMaxWidth()) {
        TextField(
            value = textFieldValue,
            onValueChange = { newValue ->
                textFieldValue = newValue
                initialCursorSet = true
                onTextChange(newValue.text)
            },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 140.dp)
                .focusRequester(focusRequester),
            placeholder = {
                Text(
                    text = "Що плануєш зробити?",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                )
            },
            textStyle = MaterialTheme.typography.bodyLarge,
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Default,
                capitalization = KeyboardCapitalization.Sentences
            ),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            )
        )

        AnimatedVisibility(
            visible = text.isEmpty(),
            enter = fadeIn(tween(500)),
            exit = fadeOut(tween(200)),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, bottom = 16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
                Text(
                    text = "Enter — нове завдання. Рядки з '//' ігноруються",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
            }
        }

        val hasExtraNewlines = text.contains("\n\n\n") || text.trim().isEmpty() && text.length > 2

        // Логіка: ховаємо кнопку як тільки відкривається діалог (запобігає стрибкам)
        val isButtonVisible = hasExtraNewlines && !showCleanupDialog

        AnimatedVisibility(
            visible = isButtonVisible,
            enter = fadeIn(tween(250)) +
                    slideInVertically(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        initialOffsetY = { 40 }
                    ) +
                    scaleIn(initialScale = 0.8f, animationSpec = tween(200)),
            exit = fadeOut(tween(150)) +
                    slideOutVertically(
                        targetOffsetY = { 20 },
                        animationSpec = tween(150)
                    ) +
                    scaleOut(targetScale = 0.8f, animationSpec = tween(150)),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    .border(
                        width = 0.5.dp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
                        shape = CircleShape
                    )
                    .clickable { showCleanupDialog = true },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoFixHigh,
                    contentDescription = "Оптимізувати текст",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        if (showCleanupDialog) {
            AlertDialog(
                onDismissRequest = { showCleanupDialog = false },
                title = { Text("Прибрати зайві відступи?") },
                text = { Text("Видалити великі проміжки, залишивши максимум по одному порожньому рядку між абзацами тексту?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val cleaned = "\n" + text.replace(Regex("\\n{3,}"), "\n\n").trim()
                            textFieldValue = TextFieldValue(cleaned, TextRange(0))
                            onTextChange(cleaned)
                            showCleanupDialog = false
                        }
                    ) {
                        Text("Очистити")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCleanupDialog = false }) {
                        Text("Скасувати")
                    }
                }
            )
        }
    }
}