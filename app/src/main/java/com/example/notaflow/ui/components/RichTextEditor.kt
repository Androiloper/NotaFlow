package com.example.notaflow.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * A rich text editor component that supports formatting
 */
@Composable
fun RichTextEditor(
    initialContent: String,
    onContentChanged: (String, String) -> Unit, // (plainText, markdownText)
    modifier: Modifier = Modifier,
    showToolbar: Boolean = true,
    readOnly: Boolean = false
) {
    val hapticFeedback = LocalHapticFeedback.current
    val focusRequester = remember { FocusRequester() }
    val scrollState = rememberScrollState()

    // Text selection colors
    val customTextSelectionColors = TextSelectionColors(
        handleColor = MaterialTheme.colorScheme.primary,
        backgroundColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
    )

    // State for tracking the text and selection
    var textFieldValue by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(initialContent))
    }

    // State for tracking the formatted content as Markdown
    var markdownContent by rememberSaveable { mutableStateOf("") }

    // Remember the last selection position
    var lastSelectionRange by remember { mutableStateOf(TextRange.Zero) }

    // Update last selection when text field selection changes
    if (textFieldValue.selection != lastSelectionRange) {
        lastSelectionRange = textFieldValue.selection
    }

    Column(modifier = modifier) {
        // Format toolbar
        if (showToolbar) {
            RichTextFormatToolbar(
                onStyleSelected = { style ->
                    textFieldValue = applyStyleToSelection(textFieldValue, style)
                    markdownContent = convertToMarkdown(textFieldValue.text)
                    onContentChanged(textFieldValue.text, markdownContent)
                },
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Text editor
        CompositionLocalProvider(
            LocalTextSelectionColors provides customTextSelectionColors
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(16.dp)
            ) {
                if (textFieldValue.text.isEmpty()) {
                    Text(
                        text = "Start typing here...",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        ),
                        modifier = Modifier.padding(1.dp) // To match BasicTextField's intrinsic padding
                    )
                }

                BasicTextField(
                    value = textFieldValue,
                    onValueChange = { newValue ->
                        textFieldValue = newValue
                        markdownContent = convertToMarkdown(newValue.text)
                        onContentChanged(newValue.text, markdownContent)
                    },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    readOnly = readOnly
                )
            }
        }
    }
}

/**
 * Apply formatting to the selected text
 */
private fun applyStyleToSelection(
    textFieldValue: TextFieldValue,
    style: TextStyle
): TextFieldValue {
    val selection = textFieldValue.selection
    if (selection.collapsed) {
        // If no text is selected, just return the original value
        return textFieldValue
    }

    val selectedText = textFieldValue.text.substring(selection.start, selection.end)
    val prefix = textFieldValue.text.substring(0, selection.start)
    val suffix = textFieldValue.text.substring(selection.end)

    // Apply the style
    val styledText = when (style) {
        TextStyle.BOLD -> "**$selectedText**"
        TextStyle.ITALIC -> "_${selectedText}_"
        TextStyle.UNDERLINE -> "<u>$selectedText</u>"
        TextStyle.STRIKETHROUGH -> "~~$selectedText~~"
        TextStyle.HEADING1 -> "# $selectedText"
        TextStyle.HEADING2 -> "## $selectedText"
        TextStyle.HEADING3 -> "### $selectedText"
        TextStyle.BULLET_LIST -> "* $selectedText"
        TextStyle.NUMBERED_LIST -> "1. $selectedText"
        TextStyle.QUOTE -> "> $selectedText"
        TextStyle.CODE -> "`$selectedText`"
        TextStyle.LINK -> "[$selectedText](url)"
        TextStyle.NORMAL -> selectedText // Remove formatting
    }

    val newText = prefix + styledText + suffix
    val newCursorPosition = prefix.length + styledText.length

    return TextFieldValue(
        text = newText,
        selection = TextRange(newCursorPosition)
    )
}

/**
 * Basic conversion to Markdown format
 * (Note: For a production app, you'd want a more robust Markdown converter)
 */
private fun convertToMarkdown(text: String): String {
    // This is a simple implementation. In production, you'd use a proper
    // library to handle the conversion with more complexity
    return text
}

/**
 * For displaying rich text (read-only) with appropriate styling
 */
@Composable
fun RichTextDisplay(
    markdownContent: String,
    modifier: Modifier = Modifier
) {
    val annotatedString = convertMarkdownToAnnotatedString(markdownContent)

    Text(
        text = annotatedString,
        style = MaterialTheme.typography.bodyLarge,
        modifier = modifier.padding(16.dp)
    )
}

/**
 * Convert markdown text to an AnnotatedString for display
 * This is a simplified implementation. In a production app,
 * you'd want to use a proper markdown parser.
 */
@Composable
private fun convertMarkdownToAnnotatedString(markdown: String): AnnotatedString {
    if (markdown.isBlank()) return AnnotatedString("")

    return buildAnnotatedString {
        val lines = markdown.lines()

        for (i in lines.indices) {
            val line = lines[i]

            // Handle headings
            when {
                line.startsWith("# ") -> {
                    append(
                        AnnotatedString(
                            line.substringAfter("# "),
                            SpanStyle(
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    )
                }
                line.startsWith("## ") -> {
                    append(
                        AnnotatedString(
                            line.substringAfter("## "),
                            SpanStyle(
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    )
                }
                line.startsWith("### ") -> {
                    append(
                        AnnotatedString(
                            line.substringAfter("### "),
                            SpanStyle(
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    )
                }
                line.startsWith("> ") -> {
                    append(
                        AnnotatedString(
                            line.substringAfter("> "),
                            SpanStyle(
                                fontStyle = FontStyle.Italic,
                                fontSize = 16.sp
                            )
                        )
                    )
                }
                line.startsWith("* ") -> {
                    append("• ")
                    append(line.substringAfter("* "))
                }
                line.startsWith("1. ") -> {
                    append("1. ")
                    append(line.substringAfter("1. "))
                }
                else -> {
                    // Process inline styles for regular text
                    var currentText = line

                    // Bold
                    while (currentText.contains("**")) {
                        val startIndex = currentText.indexOf("**")
                        val endIndex = currentText.indexOf("**", startIndex + 2)

                        if (endIndex > startIndex) {
                            // Text before the bold section
                            append(currentText.substring(0, startIndex))

                            // Bold text
                            append(
                                AnnotatedString(
                                    currentText.substring(startIndex + 2, endIndex),
                                    SpanStyle(fontWeight = FontWeight.Bold)
                                )
                            )

                            // Update remaining text
                            currentText = currentText.substring(endIndex + 2)
                        } else {
                            // No matching closing tag, just append the rest
                            append(currentText)
                            break
                        }
                    }

                    // Italic
                    while (currentText.contains("_")) {
                        val startIndex = currentText.indexOf("_")
                        val endIndex = currentText.indexOf("_", startIndex + 1)

                        if (endIndex > startIndex) {
                            // Text before the italic section
                            append(currentText.substring(0, startIndex))

                            // Italic text
                            append(
                                AnnotatedString(
                                    currentText.substring(startIndex + 1, endIndex),
                                    SpanStyle(fontStyle = FontStyle.Italic)
                                )
                            )

                            // Update remaining text
                            currentText = currentText.substring(endIndex + 1)
                        } else {
                            // No matching closing tag, just append the rest
                            append(currentText)
                            break
                        }
                    }

                    // Strikethrough
                    while (currentText.contains("~~")) {
                        val startIndex = currentText.indexOf("~~")
                        val endIndex = currentText.indexOf("~~", startIndex + 2)

                        if (endIndex > startIndex) {
                            // Text before the strikethrough section
                            append(currentText.substring(0, startIndex))

                            // Strikethrough text
                            append(
                                AnnotatedString(
                                    currentText.substring(startIndex + 2, endIndex),
                                    SpanStyle(textDecoration = TextDecoration.LineThrough)
                                )
                            )

                            // Update remaining text
                            currentText = currentText.substring(endIndex + 2)
                        } else {
                            // No matching closing tag, just append the rest
                            append(currentText)
                            break
                        }
                    }

                    // Code
                    while (currentText.contains("`")) {
                        val startIndex = currentText.indexOf("`")
                        val endIndex = currentText.indexOf("`", startIndex + 1)

                        if (endIndex > startIndex) {
                            // Text before the code section
                            append(currentText.substring(0, startIndex))

                            // Code text
                            append(
                                AnnotatedString(
                                    currentText.substring(startIndex + 1, endIndex),
                                    SpanStyle(
                                        fontFamily = FontFamily.Monospace,
                                        background = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                )
                            )

                            // Update remaining text
                            currentText = currentText.substring(endIndex + 1)
                        } else {
                            // No matching closing tag, just append the rest
                            append(currentText)
                            break
                        }
                    }

                    // Any remaining text
                    if (currentText.isNotEmpty()) {
                        append(currentText)
                    }
                }
            }

            // Add a line break after each line except the last one
            if (i < lines.size - 1) {
                append("\n")
            }
        }
    }
}