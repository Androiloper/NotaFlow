package com.example.notaflow.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
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
    val annotatedString = convertToAnnotatedString(markdownContent)

    // Display the formatted text
    Text(
        text = annotatedString,
        style = MaterialTheme.typography.bodyLarge,
        modifier = modifier.padding(4.dp)
    )
}

/**
 * Convert markdown to annotated string for simple display
 */
private fun convertToAnnotatedString(markdown: String): AnnotatedString {
    if (markdown.isBlank()) return AnnotatedString("")

    return buildAnnotatedString {
        val lines = markdown.lines()

        for (i in lines.indices) {
            val line = lines[i]

            // Process different markdown formatting
            when {
                line.startsWith("# ") -> {
                    // Heading 1
                    append(line.substringAfter("# "))
                }
                line.startsWith("## ") -> {
                    // Heading 2
                    append(line.substringAfter("## "))
                }
                line.startsWith("### ") -> {
                    // Heading 3
                    append(line.substringAfter("### "))
                }
                line.startsWith("> ") -> {
                    // Blockquote
                    append("\"${line.substringAfter("> ")}\"")
                }
                line.startsWith("* ") -> {
                    // Bullet list
                    append("• ${line.substringAfter("* ")}")
                }
                line.startsWith("1. ") -> {
                    // Numbered list
                    append("1. ${line.substringAfter("1. ")}")
                }
                else -> {
                    // Regular text with inline formatting
                    val formattedLine = processInlineFormatting(line)
                    append(formattedLine)
                }
            }

            // Add line break except for last line
            if (i < lines.size - 1) {
                append("\n")
            }
        }
    }
}

/**
 * Process inline formatting in a text line
 */
private fun processInlineFormatting(text: String): String {
    var result = text

    // Bold
    result = result.replace(Regex("\\*\\*(.*?)\\*\\*"), "$1")

    // Italic
    result = result.replace(Regex("_(.*?)_"), "$1")

    // Strikethrough
    result = result.replace(Regex("~~(.*?)~~"), "$1")

    // Code
    result = result.replace(Regex("`(.*?)`"), "$1")

    // Links [text](url)
    result = result.replace(Regex("\\[(.*?)\\]\\(.*?\\)"), "$1")

    return result
}