package com.example.tossday.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.LocalTime

private val quickDurations = listOf(15 to "15 хв", 30 to "30 хв", 60 to "1 год", 120 to "2 год")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerSheet(
    isVisible: Boolean,
    initialTime: LocalTime? = null,
    initialDuration: Int? = null,
    onDismiss: () -> Unit,
    onConfirm: (time: LocalTime?, durationMinutes: Int?) -> Unit
) {
    if (!isVisible) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val startHour = initialTime?.hour ?: LocalTime.now().hour
    val startMinute = initialTime?.minute ?: LocalTime.now().minute
    val timePickerState = rememberTimePickerState(initialHour = startHour, initialMinute = startMinute)
    var selectedDuration by remember { mutableStateOf<Int?>(initialDuration) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                quickDurations.forEach { (minutes, label) ->
                    FilterChip(
                        selected = selectedDuration == minutes,
                        onClick = {
                            selectedDuration = if (selectedDuration == minutes) null else minutes
                        },
                        label = { Text(label) }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            TimePicker(
                state = timePickerState,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = { onConfirm(null, selectedDuration) }) {
                    Text("Без часу")
                }
                Button(onClick = {
                    val time = LocalTime.of(timePickerState.hour, timePickerState.minute)
                    onConfirm(time, selectedDuration)
                }) {
                    Text("Підтвердити")
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
