package com.example.tossday.domain.usecase

import com.example.tossday.domain.model.Chip
import javax.inject.Inject

class ParseChipsUseCase @Inject constructor() {

    private val durationPattern = Regex("""(\d+)\s*(m|min|хв|h|год|hr)\b""", RegexOption.IGNORE_CASE)
    private val bulletPattern = Regex("""^[-*•]\s+|^\d+[.)]\s+""")

    operator fun invoke(text: String): List<Chip> {
        if (text.isBlank()) return emptyList()

        val chips = mutableListOf<Chip>()
        val seen = mutableSetOf<String>()
        var cursor = 0

        for (rawLine in text.split("\n")) {
            val lineStart = cursor
            cursor += rawLine.length + 1
            if (rawLine.trim().startsWith("//")) continue

            val stripped = bulletPattern.replace(rawLine, "").trim()
            val segments = if (stripped.length > 100) {
                stripped.split(Regex("""(?<=\.)\s+|(?<=;)\s+"""))
            } else {
                listOf(stripped)
            }

            for (segment in segments) {
                val clean = segment.trim()
                if (clean.length < 2) continue // Changed from 3 to 2 to allow short acronyms like "ML"
                if (!seen.add(clean.lowercase())) continue

                val duration = parseDuration(clean)
                chips.add(
                    Chip(
                        text = durationPattern.replace(clean, "").trim(),
                        durationMinutes = duration,
                        sourceStart = lineStart,
                        sourceEnd = lineStart + rawLine.length
                    )
                )
            }
        }

        return chips.reversed()
    }

    private fun parseDuration(text: String): Int? {
        val match = durationPattern.find(text) ?: return null
        val value = match.groupValues[1].toIntOrNull() ?: return null
        return when (match.groupValues[2].lowercase()) {
            "h", "год", "hr" -> value * 60
            else -> value
        }
    }
}
