package com.example.notaflow.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.notaflow.data.local.entity.Note

/**
 * A professional-looking rich text preview component
 * for displaying formatted notes
 */
@Composable
fun RichTextViewer(
    markdownContent: String,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            if (markdownContent.isBlank()) {
                Text(
                    text = "No content",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                )
            } else {
                val formattedText = parseMarkdownForDisplay(markdownContent)
                Text(text = formattedText)
            }
        }
    }
}

/**
 * Parse markdown content into an AnnotatedString with visual formatting for display
 */
@Composable
fun parseMarkdownForDisplay(markdown: String): AnnotatedString {
    // Colors from the current theme
    val codeBackground = MaterialTheme.colorScheme.surfaceVariant
    val quoteBackground = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    val onSurface = MaterialTheme.colorScheme.onSurface
    val primary = MaterialTheme.colorScheme.primary

    return buildAnnotatedString {
        val lines = markdown.lines()

        for (i in lines.indices) {
            val line = lines[i]

            // Process line by line with different styling based on content
            when {
                line.startsWith("# ") -> {
                    // Heading 1
                    withStyle(SpanStyle(
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = primary
                    )) {
                        append(line.substringAfter("# "))
                    }
                }
                line.startsWith("## ") -> {
                    // Heading 2
                    withStyle(SpanStyle(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = primary.copy(alpha = 0.9f)
                    )) {
                        append(line.substringAfter("## "))
                    }
                }
                line.startsWith("### ") -> {
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
                    // Quote with fancy styling
                    withStyle(SpanStyle(
                        fontStyle = FontStyle.Italic,
                        background = quoteBackground,
                        color = onSurface.copy(alpha = 0.8f)
                    )) {
                        append("❝ ")  // Add quotation mark for visual enhancement
                        append(line.substringAfter("> "))
                        append(" ❞")
                    }
                }
                line.startsWith("* ") -> {
                    // Bullet list with enhanced bullets
                    withStyle(SpanStyle(color = primary)) {
                        append("• ")  // Better bullet character
                    }
                    append(line.substringAfter("* "))
                }
                line.startsWith("1. ") -> {
                    // Numbered list with enhanced numbers
                    withStyle(SpanStyle(
                        color = primary,
                        fontWeight = FontWeight.Bold
                    )) {
                        append("1. ")
                    }
                    append(line.substringAfter("1. "))
                }
                else -> {
                    // Process inline formatting with enhanced appearance
                    var currentText = line
                    var currentIndex = 0

                    while (currentIndex < currentText.length) {
                        // Bold
                        val boldStart = currentText.indexOf("**", currentIndex)
                        if (boldStart >= 0) {
                            val boldEnd = currentText.indexOf("**", boldStart + 2)
                            if (boldEnd > boldStart) {
                                append(currentText.substring(currentIndex, boldStart))
                                withStyle(SpanStyle(
                                    fontWeight = FontWeight.Bold,
                                    color = onSurface
                                )) {
                                    append(currentText.substring(boldStart + 2, boldEnd))
                                }
                                currentIndex = boldEnd + 2
                                continue
                            }
                        }

                        // Italic
                        val italicStart = currentText.indexOf("_", currentIndex)
                        if (italicStart >= 0) {
                            val italicEnd = currentText.indexOf("_", italicStart + 1)
                            if (italicEnd > italicStart) {
                                append(currentText.substring(currentIndex, italicStart))
                                withStyle(SpanStyle(
                                    fontStyle = FontStyle.Italic,
                                    color = onSurface
                                )) {
                                    append(currentText.substring(italicStart + 1, italicEnd))
                                }
                                currentIndex = italicEnd + 1
                                continue
                            }
                        }

                        // Strikethrough
                        val strikeStart = currentText.indexOf("~~", currentIndex)
                        if (strikeStart >= 0) {
                            val strikeEnd = currentText.indexOf("~~", strikeStart + 2)
                            if (strikeEnd > strikeStart) {
                                append(currentText.substring(currentIndex, strikeStart))
                                withStyle(SpanStyle(
                                    textDecoration = TextDecoration.LineThrough,
                                    color = onSurface.copy(alpha = 0.7f)
                                )) {
                                    append(currentText.substring(strikeStart + 2, strikeEnd))
                                }
                                currentIndex = strikeEnd + 2
                                continue
                            }
                        }

                        // Code - with improved styling
                        val codeStart = currentText.indexOf("`", currentIndex)
                        if (codeStart >= 0) {
                            val codeEnd = currentText.indexOf("`", codeStart + 1)
                            if (codeEnd > codeStart) {
                                append(currentText.substring(currentIndex, codeStart))
                                withStyle(SpanStyle(
                                    fontFamily = FontFamily.Monospace,
                                    background = codeBackground,
                                    fontSize = 14.sp,
                                    letterSpacing = 0.5.sp
                                )) {
                                    append(" ")  // Add some padding
                                    append(currentText.substring(codeStart + 1, codeEnd))
                                    append(" ")  // Add some padding
                                }
                                currentIndex = codeEnd + 1
                                continue
                            }
                        }

                        // If no formatting found, append rest of text
                        append(currentText.substring(currentIndex))
                        break
                    }
                }
            }

            // Add line break except for last line
            if (i < lines.size - 1) {
                append("\n")
                if (line.startsWith("#") || line.startsWith("##") || line.startsWith("###")) {
                    // Add extra space after headings for better readability
                    append("\n")
                }
            }
        }
    }
}

/**
 * Preview component for displaying a note with title and content
 */
@Composable
fun NoteDetailView(
    title: String,
    content: String,
    colorIndex: Int,
    modifier: Modifier = Modifier
) {
    val noteColor = Note.getColorByIndex(colorIndex)

    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Title area with color accent
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Color indicator
                Box(
                    modifier = Modifier
                        .width(8.dp)
                        .height(48.dp)
                        .background(noteColor)
                )

                Text(
                    text = title.ifEmpty { "Untitled" },
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Content area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Show the content using RichTextViewer for HTML content
                RichTextViewer(
                    markdownContent = content,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}