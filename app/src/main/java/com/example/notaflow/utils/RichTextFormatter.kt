package com.example.notaflow.utils

import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.unit.sp

object RichTextFormatter {

    enum class StyleType {
        BOLD, ITALIC, UNDERLINE, STRIKETHROUGH
    }

    enum class ParagraphStyleType {
        BLOCKQUOTE
    }

    fun toggleStyle(textFieldValue: TextFieldValue, styleType: StyleType): TextFieldValue {
        val selection = textFieldValue.selection
        if (selection.collapsed && textFieldValue.composition == null) return textFieldValue

        val currentAnnotatedString = textFieldValue.annotatedString
        val newAnnotatedString = buildAnnotatedString {
            append(currentAnnotatedString)

            val stylesInSelection = currentAnnotatedString.spanStyles.filter { range ->
                maxOf(range.start, selection.min) < minOf(range.end, selection.max)
            }

            var isCurrentlyApplied = false
            when (styleType) {
                StyleType.BOLD -> isCurrentlyApplied = stylesInSelection.any { it.item.fontWeight == FontWeight.Bold }
                StyleType.ITALIC -> isCurrentlyApplied = stylesInSelection.any { it.item.fontStyle == FontStyle.Italic }
                StyleType.UNDERLINE -> isCurrentlyApplied = stylesInSelection.any {
                    val decoration = it.item.textDecoration
                    decoration != null && decoration != TextDecoration.None &&
                            (decoration == TextDecoration.Underline ||
                                    (decoration.contains(TextDecoration.Underline) && decoration.contains(TextDecoration.LineThrough)))
                }
                StyleType.STRIKETHROUGH -> isCurrentlyApplied = stylesInSelection.any {
                    val decoration = it.item.textDecoration
                    decoration != null && decoration != TextDecoration.None &&
                            (decoration == TextDecoration.LineThrough ||
                                    (decoration.contains(TextDecoration.Underline) && decoration.contains(TextDecoration.LineThrough)))
                }
            }

            val targetStyle: SpanStyle = when (styleType) {
                StyleType.BOLD -> if (isCurrentlyApplied) SpanStyle(fontWeight = FontWeight.Normal) else SpanStyle(fontWeight = FontWeight.Bold)
                StyleType.ITALIC -> if (isCurrentlyApplied) SpanStyle(fontStyle = FontStyle.Normal) else SpanStyle(fontStyle = FontStyle.Italic)
                StyleType.UNDERLINE -> {
                    if (isCurrentlyApplied) {
                        // Remove underline from the existing decoration
                        val currentDecoration = stylesInSelection
                            .mapNotNull { it.item.textDecoration }
                            .firstOrNull()

                        if (currentDecoration != null) {
                            // If we have LineThrough + Underline, keep only LineThrough
                            if (currentDecoration == TextDecoration.combine(listOf(TextDecoration.Underline, TextDecoration.LineThrough))) {
                                SpanStyle(textDecoration = TextDecoration.LineThrough)
                            } else {
                                // If we only have Underline, remove it
                                SpanStyle(textDecoration = TextDecoration.None)
                            }
                        } else {
                            // No decoration to remove
                            SpanStyle(textDecoration = TextDecoration.None)
                        }
                    } else {
                        // Add underline to existing decoration
                        val currentDecoration = stylesInSelection
                            .mapNotNull { it.item.textDecoration }
                            .firstOrNull()

                        if (currentDecoration != null && currentDecoration != TextDecoration.None) {
                            // If we already have LineThrough, add Underline to it
                            if (currentDecoration == TextDecoration.LineThrough) {
                                SpanStyle(textDecoration = TextDecoration.combine(listOf(TextDecoration.Underline, TextDecoration.LineThrough)))
                            } else {
                                // Otherwise just add Underline
                                SpanStyle(textDecoration = TextDecoration.Underline)
                            }
                        } else {
                            // No existing decoration, just apply underline
                            SpanStyle(textDecoration = TextDecoration.Underline)
                        }
                    }
                }
                StyleType.STRIKETHROUGH -> {
                    if (isCurrentlyApplied) {
                        // Remove strikethrough from the existing decoration
                        val currentDecoration = stylesInSelection
                            .mapNotNull { it.item.textDecoration }
                            .firstOrNull()

                        if (currentDecoration != null) {
                            // If we have LineThrough + Underline, keep only Underline
                            if (currentDecoration == TextDecoration.combine(listOf(TextDecoration.Underline, TextDecoration.LineThrough))) {
                                SpanStyle(textDecoration = TextDecoration.Underline)
                            } else {
                                // If we only have LineThrough, remove it
                                SpanStyle(textDecoration = TextDecoration.None)
                            }
                        } else {
                            // No decoration to remove
                            SpanStyle(textDecoration = TextDecoration.None)
                        }
                    } else {
                        // Add strikethrough to existing decoration
                        val currentDecoration = stylesInSelection
                            .mapNotNull { it.item.textDecoration }
                            .firstOrNull()

                        if (currentDecoration != null && currentDecoration != TextDecoration.None) {
                            // If we already have Underline, add LineThrough to it
                            if (currentDecoration == TextDecoration.Underline) {
                                SpanStyle(textDecoration = TextDecoration.combine(listOf(TextDecoration.Underline, TextDecoration.LineThrough)))
                            } else {
                                // Otherwise just add LineThrough
                                SpanStyle(textDecoration = TextDecoration.LineThrough)
                            }
                        } else {
                            // No existing decoration, just apply strikethrough
                            SpanStyle(textDecoration = TextDecoration.LineThrough)
                        }
                    }
                }
            }
            addStyle(targetStyle, selection.min, selection.max)
        }
        return textFieldValue.copy(annotatedString = newAnnotatedString, selection = selection)
    }

    fun formatColor(textFieldValue: TextFieldValue, color: Color): TextFieldValue {
        val selection = textFieldValue.selection
        if (selection.collapsed && textFieldValue.composition == null) return textFieldValue

        val newAnnotatedString = buildAnnotatedString {
            append(textFieldValue.annotatedString)
            addStyle(SpanStyle(color = color), selection.min, selection.max)
        }
        return textFieldValue.copy(annotatedString = newAnnotatedString, selection = selection)
    }

    fun applyLink(textFieldValue: TextFieldValue, url: String, text: String, selection: TextRange): TextFieldValue {
        val currentAnnotatedString = textFieldValue.annotatedString
        val currentText = currentAnnotatedString.text

        // Create a new AnnotatedString
        val newAnnotatedString = buildAnnotatedString {
            // Handle text insertion or replacement
            if (selection.collapsed) {
                // Insert mode - insert the text at the current position
                val insertPosition = selection.start.coerceIn(0, currentText.length)

                // Split the original text at the insertion point
                val beforeText = currentText.substring(0, insertPosition)
                val afterText = currentText.substring(insertPosition)

                // Append text before the insertion, the new link text, and text after
                append(beforeText)
                append(text)
                append(afterText)

                // Apply styling and URL annotation to the inserted link text
                addStringAnnotation("URL", url, beforeText.length, beforeText.length + text.length)
                addStyle(
                    SpanStyle(color = Color.Blue, textDecoration = TextDecoration.Underline),
                    beforeText.length,
                    beforeText.length + text.length
                )

                // Copy over existing span styles, adjusting positions for text after insertion
                currentAnnotatedString.spanStyles.forEach { span ->
                    if (span.start < insertPosition) {
                        // Style is before insertion point - copy as is
                        if (span.end <= insertPosition) {
                            addStyle(span.item, span.start, span.end)
                        } else {
                            // Style spans across the insertion point
                            addStyle(span.item, span.start, span.end + text.length)
                        }
                    } else {
                        // Style is after insertion point - shift position
                        addStyle(span.item, span.start + text.length, span.end + text.length)
                    }
                }

                // Copy over paragraph styles with adjusted positions
                currentAnnotatedString.paragraphStyles.forEach { para ->
                    if (para.start < insertPosition) {
                        // Paragraph style is before insertion point
                        if (para.end <= insertPosition) {
                            addStyle(para.item, para.start, para.end)
                        } else {
                            // Paragraph style spans across the insertion point
                            addStyle(para.item, para.start, para.end + text.length)
                        }
                    } else {
                        // Paragraph style is after insertion point - shift position
                        addStyle(para.item, para.start + text.length, para.end + text.length)
                    }
                }

                // Copy URL annotations with adjusted positions
                for (annotationType in currentAnnotatedString.getStringAnnotations(0, currentText.length)
                    .map { it.tag }
                    .toSet()) {

                    currentAnnotatedString.getStringAnnotations(annotationType, 0, currentText.length).forEach { annotation ->
                        if (annotation.start < insertPosition) {
                            // Annotation is before insertion point
                            if (annotation.end <= insertPosition) {
                                addStringAnnotation(annotation.tag, annotation.item, annotation.start, annotation.end)
                            } else {
                                // Annotation spans across insertion point
                                addStringAnnotation(annotation.tag, annotation.item, annotation.start, annotation.end + text.length)
                            }
                        } else {
                            // Annotation is after insertion point - shift position
                            addStringAnnotation(annotation.tag, annotation.item, annotation.start + text.length, annotation.end + text.length)
                        }
                    }
                }
            } else {
                // Replace mode - replace the selected text with the link text
                val replaceStart = selection.min.coerceIn(0, currentText.length)
                val replaceEnd = selection.max.coerceIn(0, currentText.length)

                // Split the original text at the selection boundaries
                val beforeSelection = currentText.substring(0, replaceStart)
                val afterSelection = currentText.substring(replaceEnd)

                // Append text before the selection, the new link text, and text after
                append(beforeSelection)
                append(text)
                append(afterSelection)

                // Apply styling and URL annotation to the link text
                addStringAnnotation("URL", url, beforeSelection.length, beforeSelection.length + text.length)
                addStyle(
                    SpanStyle(color = Color.Blue, textDecoration = TextDecoration.Underline),
                    beforeSelection.length,
                    beforeSelection.length + text.length
                )

                // Copy over existing span styles, adjusting positions for the replacement
                currentAnnotatedString.spanStyles.forEach { span ->
                    if (span.end <= replaceStart) {
                        // Style is completely before the selection - copy as is
                        addStyle(span.item, span.start, span.end)
                    } else if (span.start >= replaceEnd) {
                        // Style is completely after the selection - adjust position
                        val shift = text.length - (replaceEnd - replaceStart)
                        addStyle(span.item, span.start + shift, span.end + shift)
                    } else if (span.start < replaceStart && span.end > replaceEnd) {
                        // Style spans the entire selection - adjust end position
                        val newEnd = span.end - (replaceEnd - replaceStart) + text.length
                        addStyle(span.item, span.start, newEnd)
                    }
                    // If style is partially within selection, we don't copy it
                }

                // Copy over paragraph styles with adjusted positions
                currentAnnotatedString.paragraphStyles.forEach { para ->
                    if (para.end <= replaceStart) {
                        // Paragraph style is completely before the selection
                        addStyle(para.item, para.start, para.end)
                    } else if (para.start >= replaceEnd) {
                        // Paragraph style is completely after the selection
                        val shift = text.length - (replaceEnd - replaceStart)
                        addStyle(para.item, para.start + shift, para.end + shift)
                    } else if (para.start < replaceStart && para.end > replaceEnd) {
                        // Paragraph style spans the entire selection
                        val newEnd = para.end - (replaceEnd - replaceStart) + text.length
                        addStyle(para.item, para.start, newEnd)
                    }
                    // If paragraph style is partially within selection, we don't copy it
                }

                // Copy URL annotations with adjusted positions
                for (annotationType in currentAnnotatedString.getStringAnnotations(0, currentText.length)
                    .map { it.tag }
                    .toSet()) {

                    currentAnnotatedString.getStringAnnotations(annotationType, 0, currentText.length).forEach { annotation ->
                        if (annotation.end <= replaceStart) {
                            // Annotation is completely before the selection
                            addStringAnnotation(annotation.tag, annotation.item, annotation.start, annotation.end)
                        } else if (annotation.start >= replaceEnd) {
                            // Annotation is completely after the selection
                            val shift = text.length - (replaceEnd - replaceStart)
                            addStringAnnotation(annotation.tag, annotation.item, annotation.start + shift, annotation.end + shift)
                        } else if (annotation.start < replaceStart && annotation.end > replaceEnd) {
                            // Annotation spans the entire selection
                            val newEnd = annotation.end - (replaceEnd - replaceStart) + text.length
                            addStringAnnotation(annotation.tag, annotation.item, annotation.start, newEnd)
                        }
                        // If annotation is partially within selection, we don't copy it
                    }
                }
            }
        }

        // Create a new selection at the end of the inserted link
        val newCursorPosition = if (selection.collapsed) {
            selection.start + text.length
        } else {
            selection.min + text.length
        }
        val newSelection = TextRange(newCursorPosition)

        return TextFieldValue(
            annotatedString = newAnnotatedString,
            selection = newSelection
        )
    }

    fun toggleParagraphStyle(textFieldValue: TextFieldValue, styleType: ParagraphStyleType): TextFieldValue {
        val selection = textFieldValue.selection
        val currentAnnotatedString = textFieldValue.annotatedString
        val text = currentAnnotatedString.text

        // Find paragraph boundaries
        var paraStart = selection.min
        while (paraStart > 0 && text[paraStart - 1] != '\n') {
            paraStart--
        }

        var paraEnd = selection.max
        while (paraEnd < text.length && text[paraEnd] != '\n') {
            paraEnd++
        }
        if (paraEnd < text.length && text[paraEnd] == '\n') {
            paraEnd++
        }

        val newAnnotatedString = buildAnnotatedString {
            append(currentAnnotatedString)
            val styleToToggle = when (styleType) {
                ParagraphStyleType.BLOCKQUOTE -> ParagraphStyle(textIndent = TextIndent(16.sp, 16.sp))
            }

            // Check if style is already applied
            val existingStyles = currentAnnotatedString.paragraphStyles.filter { range ->
                maxOf(range.start, paraStart) < minOf(range.end, paraEnd)
            }
            val styleAlreadyApplied = existingStyles.any { range ->
                range.item.textIndent == styleToToggle.textIndent
            }

            // Toggle the style
            if (styleAlreadyApplied) {
                addStyle(ParagraphStyle(), paraStart, paraEnd)
            } else {
                addStyle(styleToToggle, paraStart, paraEnd)
            }
        }
        return textFieldValue.copy(annotatedString = newAnnotatedString, selection = selection)
    }

    fun fixMarkdownErrors(markdownContent: String): String {
        Log.d("RichTextFormatter", "Fixing markdown (stub): $markdownContent")
        return markdownContent
    }

    fun stripMarkdown(markdownContent: String): String {
        Log.d("RichTextFormatter", "Stripping markdown (stub): $markdownContent")
        var text = markdownContent
        text = text.replace(Regex("""\*\*(.*?)\*\*"""), "$1")
        text = text.replace(Regex("""\*(.*?)\*"""), "$1")
        text = text.replace(Regex("""__(.*?)__"""), "$1")
        text = text.replace(Regex("""~~(.*?)~~"""), "$1")
        text = text.replace(Regex("""^#+\s*(.*)""", RegexOption.MULTILINE), "$1")
        text = text.replace(Regex("""\[(.*?)\]\(.*?\)"""), "$1")
        text = text.replace(Regex("""\!\[(.*?)\]\(.*?\)"""), "$1")
        return text
    }
}