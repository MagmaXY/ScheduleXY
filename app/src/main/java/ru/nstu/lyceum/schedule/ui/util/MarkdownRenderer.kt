package ru.nstu.lyceum.schedule.ui.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration

object MarkdownRenderer {

    fun parseMarkdown(text: String): AnnotatedString {
        if (text.isEmpty()) return AnnotatedString("")

        val lines = text.lines()
        return buildAnnotatedString {
            lines.forEachIndexed { lineIdx, line ->
                var trimmedLine = line
                var isBullet = false
                if (trimmedLine.startsWith("- ") || trimmedLine.startsWith("* ")) {
                    trimmedLine = "• " + trimmedLine.substring(2)
                    isBullet = true
                }

                var i = 0
                val len = trimmedLine.length
                while (i < len) {
                    when {
                        // Bold: **text**
                        trimmedLine.startsWith("**", i) -> {
                            val end = trimmedLine.indexOf("**", i + 2)
                            if (end != -1) {
                                val boldText = trimmedLine.substring(i + 2, end)
                                pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                                append(boldText)
                                pop()
                                i = end + 2
                            } else {
                                append(trimmedLine[i])
                                i++
                            }
                        }
                        // Strikethrough: ~~text~~
                        trimmedLine.startsWith("~~", i) -> {
                            val end = trimmedLine.indexOf("~~", i + 2)
                            if (end != -1) {
                                val strikeText = trimmedLine.substring(i + 2, end)
                                pushStyle(SpanStyle(textDecoration = TextDecoration.LineThrough))
                                append(strikeText)
                                pop()
                                i = end + 2
                            } else {
                                append(trimmedLine[i])
                                i++
                            }
                        }
                        // Code: `text`
                        trimmedLine.startsWith("`", i) -> {
                            val end = trimmedLine.indexOf("`", i + 1)
                            if (end != -1) {
                                val codeText = trimmedLine.substring(i + 1, end)
                                pushStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = Color.Gray.copy(alpha = 0.15f)))
                                append(codeText)
                                pop()
                                i = end + 1
                            } else {
                                append(trimmedLine[i])
                                i++
                            }
                        }
                        // Italic: *text* (single asterisk, not preceded or followed by space)
                        trimmedLine.startsWith("*", i) -> {
                            val end = trimmedLine.indexOf("*", i + 1)
                            if (end != -1 && end > i + 1) {
                                val italicText = trimmedLine.substring(i + 1, end)
                                pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                                append(italicText)
                                pop()
                                i = end + 1
                            } else {
                                append(trimmedLine[i])
                                i++
                            }
                        }
                        else -> {
                            append(trimmedLine[i])
                            i++
                        }
                    }
                }

                if (lineIdx < lines.size - 1) {
                    append("\n")
                }
            }
        }
    }
}
