package com.example.tossday

import org.junit.Test
import org.junit.Assert.*
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.graphics.Color

class MarkdownTest2 {
    
    data class Span(val origStart: Int, val origEnd: Int, val style: String)

    @Test
    fun testRegex() {
        val raw = "***word***"
        
        val spans = ArrayList<Span>()
        val n = raw.length
        val isMarker = BooleanArray(n)
        
        // Pass 1: Highlight ==
        var i = 0
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
                    spans.add(Span(start + 2, end - 1, "highlight"))
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
                    spans.add(Span(start + 2, end - 1, "bold"))
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
                if (end != -1 && end > start + 1) {
                    isMarker[start] = true
                    isMarker[end] = true
                    spans.add(Span(start + 1, end, "italic"))
                    i = end + 1
                    continue
                }
            }
            i++
        }
        
        val displayBuilder = StringBuilder(n)
        for (idx in 0 until n) {
            if (!isMarker[idx]) {
                displayBuilder.append(raw[idx])
            }
        }
        println("Display: '${displayBuilder.toString()}'")
        println("Spans: $spans")
        assertEquals("word", displayBuilder.toString())
        assertTrue(spans.any { it.style == "bold" })
        assertTrue(spans.any { it.style == "italic" })
    }
}
