package com.example.tossday.data.local.entities

// Result of the day-stats SQL query — not a Room entity.
// totalMinutes already includes 30-min estimate for tasks without duration.
data class DayStatsRow(
    val date: String,
    val taskCount: Int,
    val totalMinutes: Int
)
