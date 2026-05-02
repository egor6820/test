package com.example.tossday.domain.usecase

import com.example.tossday.domain.model.Chip
import com.example.tossday.domain.util.stripMarkdown
import javax.inject.Inject

class ParseChipsUseCase @Inject constructor() {

    private val durationPattern = Regex("""(\d+)\s*(m|min|хв|h|год|hr)\b""", RegexOption.IGNORE_CASE)
    // bulletPattern не повинен вважати markdown-зірочку (`*курсив*`) bullet-маркером.
    // Тому замість загального `*` тут залишаємо тільки `-` і `•`. Markdown-`*` обробляється
    // пізніше через stripMarkdown.
    private val bulletPattern = Regex("""^[-•]\s+|^\d+[.)]\s+""")

    operator fun invoke(text: String): List<Chip> {
        if (text.isBlank()) return emptyList()

        val chips = mutableListOf<Chip>()
        val seen = mutableSetOf<String>()
        var cursor = 0

        val lines = text.split("\n")
        lines.forEachIndexed { lineIndex, rawLine ->
            val lineStart = cursor
            cursor += rawLine.length + 1
            val trimmed = rawLine.trim()
            // Рядки, що починаються з "//" (коментарі) або ">" (цитати), вважаються нотатками 
            // і НЕ перетворюються на завдання (овали).
            if (trimmed.startsWith("//") || trimmed.startsWith(">")) return@forEachIndexed

            val stripped = bulletPattern.replace(rawLine, "").trim()
            val segments = if (stripped.length > 100) {
                stripped.split(Regex("""(?<=\.)\s+|(?<=;)\s+"""))
            } else {
                listOf(stripped)
            }

            segments.forEachIndexed { segIdx, segment ->
                val clean = segment.trim()
                if (clean.length < 2) return@forEachIndexed // дозволяємо короткі акроніми ("ML")

                val duration = parseDuration(clean)
                // Видаляємо тривалість, потім ще й markdown-маркери — щоб у БД лежав
                // чистий "купити хліб", а не "**купити** хліб 30хв". Поле редактора
                // тримає markdown окремо, в quickNoteText.
                val withoutDuration = durationPattern.replace(clean, "").trim()
                val text = stripMarkdown(withoutDuration).trim()
                if (text.isEmpty()) return@forEachIndexed
                if (!seen.add(text.lowercase())) return@forEachIndexed

                // Стабільний id: будується з позиції рядка в тексті (lineIndex), а не з
                // байтового зсуву (lineStart). Дописування символів у поточний рядок або
                // редагування інших рядків (без додавання/видалення \n) не змінює lineIndex,
                // тож LazyRow бачить той самий чип і не програє enter-анімацію наново.
                chips.add(
                    Chip(
                        text = text,
                        durationMinutes = duration,
                        sourceStart = lineStart,
                        sourceEnd = lineStart + rawLine.length,
                        id = (lineIndex.toLong() shl 16) or (segIdx.toLong() and 0xFFFFL)
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
