package com.example.tossday.domain.util

/**
 * Markdown-розмітка, яку розуміє додаток у заголовках завдань і нотатках:
 *   **жирний**   → bold
 *   *курсив*     → italic
 *   ==маркер==   → highlight
 *
 * Альтернація йде в порядку bold → marker → italic — щоб `**X**` не з'їлось як два italic.
 * Контент не дозволяє `\n` чи того ж символу — навмисне обмеження проти "розповзання"
 * стилю через увесь текст, якщо користувач забув закрити пару.
 *
 * Регекс модульного рівня (стискуваний з підказкою про спільний кеш JVM регекс-движка).
 */
val MarkdownPattern: Regex = Regex(
    """\*\*(?<bold>[^*\n]+?)\*\*|==(?<mark>[^=\n]+?)==|\*(?<italic>[^*\n]+?)\*"""
)

/**
 * Прибирає markdown-маркери, лишаючи тільки контент. Використовується ParseChipsUseCase,
 * щоб у списку чипів і в БД лежав чистий текст ("купити хліб"), а не "**купити** хліб" —
 * пошук, дедуплікація і нагадування мають працювати по семантиці, а не по розмітці.
 */
fun stripMarkdown(text: String): String {
    // 1. Прибираємо парні маркери: **bold**, ==mark==, *italic*
    var result = MarkdownPattern.replace(text) { match ->
        match.groups["bold"]?.value
            ?: match.groups["mark"]?.value
            ?: match.groups["italic"]?.value
            ?: match.value
    }
    // 2. Прибираємо цитату: "> " на початку рядка
    result = result.replace(Regex("""^>\s?""", RegexOption.MULTILINE), "")
    // 3. Прибираємо залишкові непарні маркери (**  *  ==) які не підібрались у пари
    result = result.replace(Regex("""\*{1,2}|={2}"""), "")
    // 4. Нормалізуємо пробіли (після вирізання маркерів може з'явитись подвійний пробіл)
    result = result.replace(Regex(""" {2,}"""), " ").trim()
    return result
}
