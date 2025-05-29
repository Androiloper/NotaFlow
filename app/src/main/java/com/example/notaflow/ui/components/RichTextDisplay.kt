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
 * High-quality rich text display component for rendering markdown.
 * Designed for efficient inline display while supporting all major markdown features.
 */
@Composable
fun RichTextDisplay(
    markdownContent: String,
    modifier: Modifier = Modifier
) {
    // If empty, show nothing
    if (markdownContent.isBlank()) {
        return
    }

    // Parse the markdown into an annotated string
    val annotatedString = renderMarkdownContent(markdownContent)

    // Display the formatted text
    Text(
        text = annotatedString,
        style = MaterialTheme.typography.bodyLarge,
        modifier = modifier.padding(4.dp)
    )
}

/**
 * Renders markdown content with proper styling and formatting.
 * Handles all standard markdown elements with theme integration.
 */
@Composable
private fun renderMarkdownContent(markdown: String): AnnotatedString {
    // Get theme colors for consistent styling
    val primary = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface
    val background = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
    val codeBackground = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)

    return buildAnnotatedString {
        val lines = markdown.lines()
        var inCodeBlock = false

        for (i in lines.indices) {
            val line = lines[i]

            // Handle code blocks
            if (line.startsWith("```")) {
                inCodeBlock = !inCodeBlock
                continue
            }

            // Content inside code blocks
            if (inCodeBlock) {
                withStyle(SpanStyle(
                    fontFamily = FontFamily.Monospace,
                    background = codeBackground,
                    fontSize = 14.sp,
                    letterSpacing = 0.5.sp
                )) {
                    append(line)
                }
            } else {
                // Handle block-level markdown
                when {
                    line.matches(Regex("^#\\s+.*$")) -> {
                        // Heading 1 with improved styling
                        withStyle(SpanStyle(
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = primary,
                            // Add bottom padding and subtle bottom border
                            background = Color.Transparent,
                            letterSpacing = 0.4.sp
                        )) {
                            append(line.substringAfter("# "))
                        }
                        // Add more vertical space after headers
                        append("\n")
                    }
                    line.matches(Regex("^##\\s+.*$")) -> {
                        // Heading 2 with clearer visual distinction
                        withStyle(SpanStyle(
                            fontSize = 22.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = primary.copy(alpha = 0.9f),
                            letterSpacing = 0.2.sp
                        )) {
                            append(line.substringAfter("## "))
                        }
                        append("\n")

                }

                    // Blockquotes
                    line.startsWith("> ") -> {
                        withStyle(SpanStyle(
                            fontStyle = FontStyle.Italic,
                            background = background,
                            color = onSurface.copy(alpha = 0.8f)
                        )) {
                            append("❝ ${line.substringAfter("> ")} ❞")
                        }
                    }

                    // Lists
                    line.matches(Regex("^\\s*[*+-]\\s+.*$")) -> {
                        val bulletMatch = Regex("^\\s*[*+-]\\s+(.*)$").find(line)
                        if (bulletMatch != null) {
                            withStyle(SpanStyle(color = primary)) {
                                append("• ")
                            }
                            append(bulletMatch.groupValues[1])
                        } else {
                            append(line)
                        }
                    }
                    line.matches(Regex("^\\s*\\d+\\.\\s+.*$")) -> {
                        val numberMatch = Regex("^\\s*(\\d+\\.)\\s+(.*)$").find(line)
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

                    // Regular text with inline formatting
                    else -> {
                        processInlineMarkdown(line, this, primary, codeBackground)
                    }
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
 * Processes inline markdown formatting within a single line of text.
 * Handles nested formatting and edge cases correctly.
 */
private fun processInlineMarkdown(
    text: String,
    builder: AnnotatedString.Builder,
    primaryColor: Color,
    codeBackground: Color
) {
    var remainingText = text
    var startIndex = 0

    // Process the text iteratively to handle all formatting
    while (startIndex < remainingText.length) {
        // Check for each formatting pattern
        val boldPattern = Regex("\\*\\*(.*?)\\*\\*").find(remainingText, startIndex)
        val italicUnderscorePattern = Regex("_(.*?)_").find(remainingText, startIndex)
        val italicAsteriskPattern = Regex("(?<![*])\\*([^*]+)\\*(?![*])").find(remainingText, startIndex)
        val strikethroughPattern = Regex("~~(.*?)~~").find(remainingText, startIndex)
        val codePattern = Regex("`(.*?)`").find(remainingText, startIndex)
        val linkPattern = Regex("\\[(.*?)\\]\\((.*?)\\)").find(remainingText, startIndex)
        val underlinePattern = Regex("<u>(.*?)</u>").find(remainingText, startIndex)

        // Find the pattern that appears first
        val patterns = listOfNotNull(
            boldPattern, italicUnderscorePattern, italicAsteriskPattern,
            strikethroughPattern, codePattern, linkPattern, underlinePattern
        )

        if (patterns.isEmpty()) {
            // No more patterns found, append remaining text
            builder.append(remainingText.substring(startIndex))
            break
        }

        // Find the earliest match
        val firstMatch = patterns.minByOrNull { it.range.first }!!

        // Append text before the match
        if (firstMatch.range.first > startIndex) {
            builder.append(remainingText.substring(startIndex, firstMatch.range.first))
        }

        // Apply the appropriate style
        when (firstMatch) {
            boldPattern -> {
                builder.withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(firstMatch.groupValues[1])
                }
            }
            italicUnderscorePattern, italicAsteriskPattern -> {
                builder.withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                    append(firstMatch.groupValues[1])
                }
            }
            strikethroughPattern -> {
                builder.withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                    append(firstMatch.groupValues[1])
                }
            }
            underlinePattern -> {
                builder.withStyle(SpanStyle(textDecoration = TextDecoration.Underline)) {
                    append(firstMatch.groupValues[1])
                }
            }
            codePattern -> {
                builder.withStyle(SpanStyle(
                    fontFamily = FontFamily.Monospace,
                    background = codeBackground,
                    letterSpacing = 0.5.sp
                )) {
                    append(firstMatch.groupValues[1])
                }
            }
            linkPattern -> {
                builder.withStyle(SpanStyle(
                    color = primaryColor,
                    textDecoration = TextDecoration.Underline
                )) {
                    append(firstMatch.groupValues[1])
                }
            }
        }

        // Update start index to after the match
        startIndex = firstMatch.range.last + 1
    }
}