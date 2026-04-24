package com.example.tossday.domain.model

data class Chip(
    val text: String,
    val durationMinutes: Int?,
    val sourceStart: Int,
    val sourceEnd: Int
)
