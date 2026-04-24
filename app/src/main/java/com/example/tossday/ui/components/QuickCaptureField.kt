package com.example.tossday.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
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

    // Track whether we've done the initial cursor placement
    var initialCursorSet by remember { mutableStateOf(false) }
    var showCleanupDialog by remember { mutableStateOf(false) }
    // Survives nav pop/push + activity recreation so keyboard doesn't re-pop after
    // returning from Settings or a config change.
    var hasAutoFocused by rememberSaveable { mutableStateOf(false) }

    // Internal TextFieldValue to control cursor position
    var textFieldValue by remember { mutableStateOf(TextFieldValue(text)) }

    // Sync external text changes (e.g. initial load from Room)
    LaunchedEffect(text) {
        if (textFieldValue.text != text) {
            if (!initialCursorSet && text.isNotEmpty()) {
                // First load with content — ensure empty last line and place cursor at end
                val displayText = if (!text.endsWith("\n")) "$text\n" else text
                textFieldValue = TextFieldValue(displayText, TextRange(displayText.length))
                initialCursorSet = true
                if (displayText != text) onTextChange(displayText) // Save the prepended newline
            } else {
                // Subsequent external changes — keep current cursor position safely
                textFieldValue = textFieldValue.copy(text = text)
            }
        }
    }

    LaunchedEffect(Unit) {
        if (!hasAutoFocused) {
            delay(250) // Let UI render first, then show keyboard
            focusRequester.requestFocus()
            hasAutoFocused = true
        }
    }

    Box(modifier = modifier) {
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
                    text = "Пиши завдання — кожне з нового рядка\n// такий рядок ігнорується",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
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

        // Smart cleanup button: appears only when there are excessive newlines
        val hasExtraNewlines = text.contains("\n\n\n") || text.trim().isEmpty() && text.length > 2
        
        androidx.compose.animation.AnimatedVisibility(
            visible = hasExtraNewlines,
            enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.scaleIn(),
            exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.scaleOut(),
            modifier = Modifier
                .align(androidx.compose.ui.Alignment.BottomEnd)
                .padding(8.dp)
        ) {
            androidx.compose.material3.IconButton(
                onClick = {
                    showCleanupDialog = true 
                },
                colors = androidx.compose.material3.IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            ) {
                androidx.compose.material3.Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = "Очистити пусті рядки",
                    modifier = Modifier.padding(4.dp)
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
                            // Replace 3+ newlines with exactly 2 newlines (1 empty line)
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
