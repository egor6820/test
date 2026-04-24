package com.example.tossday.domain.model

import androidx.compose.ui.graphics.Color
import com.example.tossday.ui.theme.LoadGreen
import com.example.tossday.ui.theme.LoadOrange
import com.example.tossday.ui.theme.LoadRed
import com.example.tossday.ui.theme.LoadYellow
import java.time.LocalDate

data class DayLoad(
    val date: LocalDate,
    val taskCount: Int,
    val totalMinutes: Int,
    val capacityMinutes: Int = 480,
    val percent: Float
) {
    fun loadColor(): Color = when {
        percent < 0.60f -> LoadGreen
        percent < 0.85f -> LoadYellow
        percent < 1.00f -> LoadOrange
        else -> LoadRed
    }
}
