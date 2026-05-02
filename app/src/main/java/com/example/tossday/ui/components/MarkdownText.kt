package com.example.tossday.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import com.example.tossday.domain.util.MarkdownPattern

/**
 * VisualTransformation, що рендерить markdown прямо в полі вводу:
 *  • вміст усередині пар `**`/`*`/`==` отримує жирний/курсив/підсвітку;
 *  • самі маркери ХОВАЮТЬСЯ візуально (не рендеряться зовсім) — як в iOS Notes;
 *  • OffsetMapping перерахований під приховані маркери, щоб курсор/виділення працювали
 *    в "візуальній" системі координат, а в БД лежав raw markdown.
 *
 * Параметр `highlightBackground` — колір підсвітки для `==marker==`. Залежить від теми, тож
 * новий інстанс створюється лише при зміні теми.
 */
class MarkdownVisualTransformation(
    private val highlightBackground: Color,
    private val selection: TextRange
) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        if (raw.isEmpty()) return TransformedText(text, OffsetMapping.Identity)

        // [(displayStartInclusive, displayEndExclusive, style)] — спани, які треба накласти
        // у display-просторі. displayEnd рахуємо ПІСЛЯ обчислення origToTrans.
        data class Span(val origStart: Int, val origEnd: Int, val style: SpanStyle)

        val spans = ArrayList<Span>()

        val n = raw.length
        val isMarker = BooleanArray(n)
        val synthesized = HashMap<Int, String>()
        
        // Зберігаємо оригінальні межі цитат, щоб потім додати анотації в display-просторі
        data class QuoteBlock(val origStart: Int, val origEnd: Int)
        val quotes = ArrayList<QuoteBlock>()

        // Pass 0: Quote Blocks (> )
        var i = 0
        while (i < n) {
            if ((i == 0 || raw[i-1] == '\n') && raw[i] == '>') {
                val blockStart = i
                var blockEnd = i
                val lineStarts = ArrayList<Int>()
                lineStarts.add(i)
                
                while (blockEnd < n) {
                    var lineEnd = blockEnd
                    while (lineEnd < n && raw[lineEnd] != '\n') {
                        lineEnd++
                    }
                    blockEnd = lineEnd
                    if (lineEnd + 1 < n && raw[lineEnd + 1] == '>') {
                        blockEnd = lineEnd + 1
                        lineStarts.add(blockEnd)
                    } else {
                        break
                    }
                }
                
                val isCursorInside = selection.start in blockStart..blockEnd || 
                                     selection.end in blockStart..blockEnd ||
                                     (selection.start <= blockStart && selection.end >= blockEnd)
                                     
                val isCollapsed = !isCursorInside && lineStarts.size > 1
                
                // Ховаємо ВСІ маркери "> " на початку рядків
                for (ls in lineStarts) {
                    isMarker[ls] = true
                    if (ls + 1 < n && raw[ls + 1] == ' ') {
                        isMarker[ls + 1] = true
                    }
                }
                
                quotes.add(QuoteBlock(blockStart, blockEnd))
                
                // Стиль тексту всередині цитати
                spans.add(Span(blockStart, blockEnd, SpanStyle(color = Color.Gray, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)))
                
                if (isCollapsed) {
                    var firstLineEnd = lineStarts[0]
                    while (firstLineEnd < n && raw[firstLineEnd] != '\n') {
                        firstLineEnd++
                    }
                    
                    for (k in firstLineEnd until blockEnd) {
                        isMarker[k] = true
                    }
                    
                    synthesized[firstLineEnd] = " … [Розгорнути]"
                }
                
                i = blockEnd
            }
            i++
        }

        // Pass 1: Highlight ==
        i = 0
        while (i < n - 1) {
            if (raw[i] == '=' && raw[i+1] == '=') {
                val start = i
                var end = -1
                for (j in i + 2 until n - 1) {
                    if (raw[j] == '=' && raw[j+1] == '=' && raw[j-1] != '\n') {
                        end = j + 1
                        break
                    }
                }
                if (end != -1) {
                    for (k in start..start+1) isMarker[k] = true
                    for (k in end-1..end) isMarker[k] = true
                    spans.add(Span(start + 2, end - 1, highlightStyle()))
                    i = end + 1
                    continue
                }
            }
            i++
        }

        // Pass 2: Bold **
        i = 0
        while (i < n - 1) {
            if (!isMarker[i] && !isMarker[i+1] && raw[i] == '*' && raw[i+1] == '*') {
                val start = i
                var end = -1
                for (j in i + 2 until n - 1) {
                    if (!isMarker[j] && !isMarker[j+1] && raw[j] == '*' && raw[j+1] == '*' && raw[j-1] != '\n') {
                        end = j + 1
                        break
                    }
                }
                if (end != -1) {
                    for (k in start..start+1) isMarker[k] = true
                    for (k in end-1..end) isMarker[k] = true
                    spans.add(Span(start + 2, end - 1, boldStyle))
                    i = end + 1
                    continue
                }
            }
            i++
        }

        // Pass 3: Italic *
        i = 0
        while (i < n) {
            if (!isMarker[i] && raw[i] == '*') {
                val start = i
                var end = -1
                for (j in i + 1 until n) {
                    if (!isMarker[j] && raw[j] == '*' && raw[j-1] != '\n') {
                        end = j
                        break
                    }
                }
                // Перевіряємо, щоб це не було порожнє ** (воно обробляється вище)
                if (end != -1 && end > start + 1) {
                    isMarker[start] = true
                    isMarker[end] = true
                    spans.add(Span(start + 1, end, italicStyle))
                    i = end + 1
                    continue
                }
            }
            i++
        }

        // Будуємо display-рядок і двосторонній OffsetMapping одним проходом.
        val displayBuilder = StringBuilder(n + 50)
        val origToTrans = IntArray(n + 1)
        val transToOrig = ArrayList<Int>(n + 50)
        
        var t = 0
        for (idx in 0..n) {
            origToTrans[idx] = t
            
            val insertStr = synthesized[idx]
            if (insertStr != null) {
                for (char in insertStr) {
                    displayBuilder.append(char)
                    transToOrig.add(idx)
                    t++
                }
            }
            
            if (idx < n && !isMarker[idx]) {
                displayBuilder.append(raw[idx])
                transToOrig.add(idx)
                t++
            }
        }
        
        // Кінцева позиція display = n у original.
        transToOrig.add(n)
        val transToOrigArr = transToOrig.toIntArray()

        val annotated = buildAnnotatedString {
            append(displayBuilder.toString())
            for (span in spans) {
                val ds = origToTrans[span.origStart]
                val de = origToTrans[span.origEnd]
                if (ds < de) addStyle(span.style, ds, de)
            }
            
            // Додаємо відступи (ParagraphStyle) та Анотації для фону цитат
            for (q in quotes) {
                val ds = origToTrans[q.origStart]
                val de = origToTrans[q.origEnd]
                if (ds < de) {
                    addStyle(ParagraphStyle(textIndent = TextIndent(firstLine = 12.sp, restLine = 12.sp)), ds, de)
                    addStringAnnotation("QUOTE", "quote", ds, de)
                }
            }
        }

        val mapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int =
                origToTrans[offset.coerceIn(0, n)]

            override fun transformedToOriginal(offset: Int): Int =
                transToOrigArr[offset.coerceIn(0, transToOrigArr.size - 1)]
        }

        return TransformedText(annotated, mapping)
    }

    private val boldStyle = SpanStyle(fontWeight = FontWeight.Bold)
    private val italicStyle = SpanStyle(fontStyle = FontStyle.Italic)
    private fun highlightStyle() = SpanStyle(background = highlightBackground)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MarkdownVisualTransformation) return false
        if (highlightBackground != other.highlightBackground) return false
        if (selection != other.selection) return false
        return true
    }

    override fun hashCode(): Int {
        var result = highlightBackground.hashCode()
        result = 31 * result + selection.hashCode()
        return result
    }
}

/**
 * Обгортає виділений діапазон у `marker` з обох боків — або знімає обгортку, якщо вона вже
 * є (toggle). Покриває кілька варіантів того, як виглядає selection в underlying string,
 * бо з прихованими маркерами OffsetMapping може мапити display-вибір "hi" в original-вибір
 * "hi**" (включно з трейлинговими маркерами):
 *
 *   A. Selection САМ містить маркери з обох боків: `**hi**` → знімаємо.
 *   B. Selection — чистий контент, маркери — сусіди: `«**»hi«**»` → знімаємо.
 *   C. Selection починається з маркера, а трейл — сусід: `**hi`+`«**»` → знімаємо.
 *   D. Selection закінчується маркером, а лідер — сусід: `«**»`+`hi**` → знімаємо.
 *   E. Інакше — обгортаємо.
 *
 * Виділення після операції лишається на тому самому видимому контенті.
 *
 * Згорнуте виділення (sel.collapsed) — нічого не робимо.
 */
fun wrapSelectionInMarker(tfv: TextFieldValue, marker: String): TextFieldValue {
    val sel = tfv.selection
    if (sel.collapsed) return tfv

    val text = tfv.text
    var start = sel.min
    var end = sel.max
    var selectedText = text.substring(start, end)

    // Android's word selection often captures leading/trailing spaces. 
    // We trim them from the selection bounds so they don't interfere with marker toggling.
    while (selectedText.endsWith(" ") || selectedText.endsWith("\n")) {
        end--
        selectedText = text.substring(start, end)
    }
    while (selectedText.startsWith(" ") || selectedText.startsWith("\n")) {
        start++
        selectedText = text.substring(start, end)
    }
    
    if (start >= end) return tfv

    val markerLen = marker.length

    val beforeStart = (start - markerLen).coerceAtLeast(0)
    val afterEnd = (end + markerLen).coerceAtMost(text.length)
    val before = text.substring(beforeStart, start)
    val after = text.substring(end, afterEnd)

    val selStartsWithMarker = selectedText.startsWith(marker)
    val selEndsWithMarker = selectedText.endsWith(marker)
    val selHasRoomForBoth = selectedText.length >= 2 * markerLen

    // A. selection покриває маркери з обох боків.
    if (selHasRoomForBoth && selStartsWithMarker && selEndsWithMarker) {
        val content = selectedText.substring(markerLen, selectedText.length - markerLen)
        val newText = text.substring(0, start) + content + text.substring(end)
        return tfv.copy(
            text = newText,
            selection = TextRange(start, start + content.length)
        )
    }

    // B. Маркери — сусіди по обидва боки selection.
    if (before == marker && after == marker) {
        val newText = text.substring(0, beforeStart) + selectedText + text.substring(afterEnd)
        return tfv.copy(
            text = newText,
            selection = TextRange(beforeStart, beforeStart + selectedText.length)
        )
    }

    // C. Лідируючий маркер у selection, трейлинговий — сусід після.
    if (selStartsWithMarker && after == marker) {
        val content = selectedText.substring(markerLen)
        val newText = text.substring(0, start) + content + text.substring(afterEnd)
        return tfv.copy(
            text = newText,
            selection = TextRange(start, start + content.length)
        )
    }

    // D. Лідируючий маркер — сусід перед, трейлинговий у selection.
    if (before == marker && selEndsWithMarker) {
        val content = selectedText.substring(0, selectedText.length - markerLen)
        val newText = text.substring(0, beforeStart) + content + text.substring(end)
        return tfv.copy(
            text = newText,
            selection = TextRange(beforeStart, beforeStart + content.length)
        )
    }

    // E. Обгортаємо.
    val newText = text.substring(0, start) + marker + selectedText + marker + text.substring(end)
    return tfv.copy(
        text = newText,
        selection = TextRange(start + markerLen, end + markerLen)
    )
}
