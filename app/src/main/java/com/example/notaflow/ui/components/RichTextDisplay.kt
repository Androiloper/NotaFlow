package com.example.notaflow.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Simple rich text display component for showing formatted text
 * in list views and other places where a full preview isn't needed
 */
@Composable
fun RichTextDisplay(
    markdownContent: String,
    modifier: Modifier = Modifier
) {
    // Convert markdown to annotated string
    val annotatedString = convertMarkdownToAnnotatedString(markdownContent)

    // Display the formatted text
    Text(
        text = annotatedString,
        style = MaterialTheme.typography.bodyLarge,
        modifier = modifier.padding(4.dp)
    )
}

/**
 * Convert markdown to annotated string for simple display
 * Fixed to properly handle headers and other markdown elements
 */
@Composable
private fun convertMarkdownToAnnotatedString(markdown: String): AnnotatedString {
    if (markdown.isBlank()) return AnnotatedString("")

    // Colors from the current theme for styling
    val primary = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface
    val background = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)

    return buildAnnotatedString {
        val lines = markdown.lines()

        for (i in lines.indices) {
            val line = lines[i]

            // Process different markdown formatting
            when {
                // Proper regex matching for headers to fix the issue
                line.matches(Regex("^#\\s+.*$")) -> {
                    // Heading 1
                    withStyle(SpanStyle(
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = primary
                    )) {
                        append(line.substringAfter("# "))
                    }
                }
                line.matches(Regex("^##\\s+.*$")) -> {
                    // Heading 2
                    withStyle(SpanStyle(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = primary.copy(alpha = 0.9f)
                    )) {
                        append(line.substringAfter("## "))
                    }
                }
                line.matches(Regex("^###\\s+.*$")) -> {
                    // Heading 3
                    withStyle(SpanStyle(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = primary.copy(alpha = 0.8f)
                    )) {
                        append(line.substringAfter("### "))
                    }
                }
                line.startsWith("> ") -> {
                    // Blockquote with improved styling
                    withStyle(SpanStyle(
                        fontStyle = FontStyle.Italic,
                        background = background,
                        color = onSurface.copy(alpha = 0.8f)
                    )) {
                        append("❝ ${line.substringAfter("> ")} ❞")
                    }
                }
                line.matches(Regex("^[*+-]\\s+.*$")) -> {
                    // Bullet list with robust pattern matching
                    val bulletMatch = Regex("^([*+-])\\s+(.*)$").find(line)
                    if (bulletMatch != null) {
                        withStyle(SpanStyle(color = primary)) {
                            append("• ")
                        }
                        append(bulletMatch.groupValues[2])
                    } else {
                        append(line)
                    }
                }
                line.matches(Regex("^\\d+\\.\\s+.*$")) -> {
                    // Numbered list
                    val numberMatch = Regex("^(\\d+\\.)\\s+(.*)$").find(line)
                    if (numberMatch != null) {
                        withStyle(SpanStyle(
                            color = primary,
                            fontWeight = FontWeight.Bold
                        )) {
                            append(numberMatch.groupValues[1] + " ")
                        }
                        append(numberMatch.groupValues[2])
                    } else {
                        append(line)
                    }
                }
                else -> {
                    // Inline formatting with improved patterns
                    processInlineFormatting(line, this)
                }
            }

            // Add line break except for last line
            if (i < lines.size - 1) {
                append("\n")
                // Add extra space after headings for better readability
                if (line.matches(Regex("^#+\\s+.*$"))) {
                    append("\n")
                }
            }
        }
    }
}

/**
 * Process inline markdown formatting in a text line
 * Enhanced to properly handle all formatting types
 */
private fun processInlineFormatting(text: String, builder: AnnotatedString.Builder) {
    var currentIndex = 0

    while (currentIndex < text.length) {
        // Bold: **text**
        val boldMatch = Regex("\\*\\*(.+?)\\*\\*").find(text, currentIndex)
        if (boldMatch != null && boldMatch.range.first == currentIndex) {
            builder.withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                append(boldMatch.groupValues[1])
            }
            currentIndex = boldMatch.range.last + 1
            continue
        }

        // Italic with underscores: _text_
        val italicMatch = Regex("_(.+?)_").find(text, currentIndex)
        if (italicMatch != null && italicMatch.range.first == currentIndex) {
            builder.withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                append(italicMatch.groupValues[1])
            }
            currentIndex = italicMatch.range.last + 1
            continue
        }

        // Italic with asterisks: *text*
        val italicAsteriskMatch = Regex("\\*(.+?)\\*").find(text, currentIndex)
        if (italicAsteriskMatch != null && italicAsteriskMatch.range.first == currentIndex) {
            builder.withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                append(italicAsteriskMatch.groupValues[1])
            }
            currentIndex = italicAsteriskMatch.range.last + 1
            continue
        }

        // Strikethrough: ~~text~~
        val strikethroughMatch = Regex("~~(.+?)~~").find(text, currentIndex)
        if (strikethroughMatch != null && strikethroughMatch.range.first == currentIndex) {
            builder.withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                append(strikethroughMatch.groupValues[1])
            }
            currentIndex = strikethroughMatch.range.last + 1
            continue
        }

        // Underline: <u>text</u>
        val underlineMatch = Regex("<u>(.+?)</u>").find(text, currentIndex)
        if (underlineMatch != null && underlineMatch.range.first == currentIndex) {
            builder.withStyle(SpanStyle(textDecoration = TextDecoration.Underline)) {
                append(underlineMatch.groupValues[1])
            }
            currentIndex = underlineMatch.range.last + 1
            continue
        }

        // Code: `text`
        val codeMatch = Regex("`(.+?)`").find(text, currentIndex)
        if (codeMatch != null && codeMatch.range.first == currentIndex) {
            builder.withStyle(SpanStyle(
                fontFamily = FontFamily.Monospace,
                background = Color.LightGray.copy(alpha = 0.2f)
            )) {
                append(codeMatch.groupValues[1])
            }
            currentIndex = codeMatch.range.last + 1
            continue
        }

        // Link: [text](url)
        val linkMatch = Regex("\\[(.+?)\\]\\((.+?)\\)").find(text, currentIndex)
        if (linkMatch != null && linkMatch.range.first == currentIndex) {
            builder.withStyle(SpanStyle(
                color = Color.Blue,
                textDecoration = TextDecoration.Underline
            )) {
                append(linkMatch.groupValues[1])
            }
            currentIndex = linkMatch.range.last + 1
            continue
        }

        // If no formatting found at current position, append the character and move on
        builder.append(text[currentIndex].toString())
        currentIndex++
    }
}