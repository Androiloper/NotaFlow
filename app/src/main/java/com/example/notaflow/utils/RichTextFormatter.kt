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

    /**
     * Improved style toggling that preserves existing formatting better
     */
    fun toggleStyle(textFieldValue: TextFieldValue, styleType: StyleType): TextFieldValue {
        val selection = textFieldValue.selection
        val annotatedString = textFieldValue.annotatedString
        val text = annotatedString.text

        // Handle empty selection - apply to cursor position for future typing
        if (selection.collapsed) {
            return handleCursorPositionFormatting(textFieldValue, styleType)
        }

        val selectionStart = selection.min
        val selectionEnd = selection.max

        // Check if the entire selection has the target style
        val hasTargetStyle = checkIfSelectionHasStyle(annotatedString, selectionStart, selectionEnd, styleType)

        val newAnnotatedString = buildAnnotatedString {
            append(annotatedString)

            if (hasTargetStyle) {
                // Remove the style from the selection
                removeStyleFromRange(this, selectionStart, selectionEnd, styleType, annotatedString)
            } else {
                // Add the style to the selection
                addStyleToRange(this, selectionStart, selectionEnd, styleType)
            }
        }

        return textFieldValue.copy(
            annotatedString = newAnnotatedString,
            selection = selection
        )
    }

    /**
     * Handle formatting at cursor position (for future typing)
     */
    private fun handleCursorPositionFormatting(textFieldValue: TextFieldValue, styleType: StyleType): TextFieldValue {
        val cursorPos = textFieldValue.selection.start
        val annotatedString = textFieldValue.annotatedString

        // Check current styles at cursor position
        val stylesAtCursor = getStylesAtPosition(annotatedString, cursorPos)
        val hasStyle = when (styleType) {
            StyleType.BOLD -> stylesAtCursor.any { it.fontWeight == FontWeight.Bold }
            StyleType.ITALIC -> stylesAtCursor.any { it.fontStyle == FontStyle.Italic }
            StyleType.UNDERLINE -> stylesAtCursor.any {
                it.textDecoration?.contains(TextDecoration.Underline) == true
            }
            StyleType.STRIKETHROUGH -> stylesAtCursor.any {
                it.textDecoration?.contains(TextDecoration.LineThrough) == true
            }
        }

        // For cursor position, we'll insert a zero-width character with the style
        // This is a common technique in rich text editors
        val targetStyle = createStyleForType(styleType, !hasStyle)
        val newAnnotatedString = buildAnnotatedString {
            append(annotatedString)

            if (!hasStyle) {
                // Add invisible marker for next character
                addStyle(targetStyle, cursorPos, cursorPos)
            }
        }

        return textFieldValue.copy(annotatedString = newAnnotatedString)
    }

    /**
     * Check if the entire selection has the specified style
     */
    private fun checkIfSelectionHasStyle(
        annotatedString: AnnotatedString,
        start: Int,
        end: Int,
        styleType: StyleType
    ): Boolean {
        val relevantSpans = annotatedString.spanStyles.filter { spanStyle ->
            spanStyle.start < end && spanStyle.end > start
        }

        if (relevantSpans.isEmpty()) return false

        // Check if the entire selection is covered by spans with the target style
        for (pos in start until end) {
            val spansAtPos = relevantSpans.filter { it.start <= pos && it.end > pos }
            val hasStyleAtPos = spansAtPos.any { spanStyle ->
                when (styleType) {
                    StyleType.BOLD -> spanStyle.item.fontWeight == FontWeight.Bold
                    StyleType.ITALIC -> spanStyle.item.fontStyle == FontStyle.Italic
                    StyleType.UNDERLINE -> spanStyle.item.textDecoration?.contains(TextDecoration.Underline) == true
                    StyleType.STRIKETHROUGH -> spanStyle.item.textDecoration?.contains(TextDecoration.LineThrough) == true
                }
            }

            if (!hasStyleAtPos) return false
        }

        return true
    }

    /**
     * Remove style from a specific range while preserving other styles
     */
    private fun removeStyleFromRange(
        builder: AnnotatedString.Builder,
        start: Int,
        end: Int,
        styleType: StyleType,
        originalString: AnnotatedString
    ) {
        // Clear existing styles in the range and re-apply non-target styles
        val existingSpans = originalString.spanStyles.filter { spanStyle ->
            spanStyle.start < end && spanStyle.end > start
        }

        for (spanStyle in existingSpans) {
            val spanStart = maxOf(spanStyle.start, start)
            val spanEnd = minOf(spanStyle.end, end)

            if (spanStart < spanEnd) {
                val modifiedStyle = removeStyleFromSpanStyle(spanStyle.item, styleType)
                if (modifiedStyle != SpanStyle()) {
                    builder.addStyle(modifiedStyle, spanStart, spanEnd)
                }
            }
        }
    }

    /**
     * Add style to a specific range while preserving existing styles
     */
    private fun addStyleToRange(
        builder: AnnotatedString.Builder,
        start: Int,
        end: Int,
        styleType: StyleType
    ) {
        val targetStyle = createStyleForType(styleType, true)
        builder.addStyle(targetStyle, start, end)
    }

    /**
     * Create a SpanStyle for the given style type
     */
    private fun createStyleForType(styleType: StyleType, enabled: Boolean): SpanStyle {
        return when (styleType) {
            StyleType.BOLD -> SpanStyle(fontWeight = if (enabled) FontWeight.Bold else FontWeight.Normal)
            StyleType.ITALIC -> SpanStyle(fontStyle = if (enabled) FontStyle.Italic else FontStyle.Normal)
            StyleType.UNDERLINE -> SpanStyle(textDecoration = if (enabled) TextDecoration.Underline else TextDecoration.None)
            StyleType.STRIKETHROUGH -> SpanStyle(textDecoration = if (enabled) TextDecoration.LineThrough else TextDecoration.None)
        }
    }

    /**
     * Remove a specific style from a SpanStyle while preserving others
     */
    private fun removeStyleFromSpanStyle(spanStyle: SpanStyle, styleType: StyleType): SpanStyle {
        return when (styleType) {
            StyleType.BOLD -> spanStyle.copy(fontWeight = FontWeight.Normal)
            StyleType.ITALIC -> spanStyle.copy(fontStyle = FontStyle.Normal)
            StyleType.UNDERLINE -> {
                val currentDecoration = spanStyle.textDecoration
                val newDecoration = when {
                    currentDecoration == TextDecoration.Underline -> TextDecoration.None
                    currentDecoration == TextDecoration.combine(listOf(TextDecoration.Underline, TextDecoration.LineThrough)) ->
                        TextDecoration.LineThrough
                    else -> currentDecoration
                }
                spanStyle.copy(textDecoration = newDecoration)
            }
            StyleType.STRIKETHROUGH -> {
                val currentDecoration = spanStyle.textDecoration
                val newDecoration = when {
                    currentDecoration == TextDecoration.LineThrough -> TextDecoration.None
                    currentDecoration == TextDecoration.combine(listOf(TextDecoration.Underline, TextDecoration.LineThrough)) ->
                        TextDecoration.Underline
                    else -> currentDecoration
                }
                spanStyle.copy(textDecoration = newDecoration)
            }
        }
    }

    /**
     * Get all styles that apply at a specific position
     */
    private fun getStylesAtPosition(annotatedString: AnnotatedString, position: Int): List<SpanStyle> {
        return annotatedString.spanStyles
            .filter { it.start <= position && it.end > position }
            .map { it.item }
    }

    /**
     * Apply color formatting with better preservation of existing styles
     */
    fun formatColor(textFieldValue: TextFieldValue, color: Color): TextFieldValue {
        val selection = textFieldValue.selection
        if (selection.collapsed && textFieldValue.composition == null) {
            // Handle cursor position coloring
            return handleCursorColorFormatting(textFieldValue, color)
        }

        val newAnnotatedString = buildAnnotatedString {
            append(textFieldValue.annotatedString)
            addStyle(SpanStyle(color = color), selection.min, selection.max)
        }

        return textFieldValue.copy(annotatedString = newAnnotatedString, selection = selection)
    }

    /**
     * Handle color formatting at cursor position
     */
    private fun handleCursorColorFormatting(textFieldValue: TextFieldValue, color: Color): TextFieldValue {
        val cursorPos = textFieldValue.selection.start
        val newAnnotatedString = buildAnnotatedString {
            append(textFieldValue.annotatedString)
            // Add color style for next character typed
            addStyle(SpanStyle(color = color), cursorPos, cursorPos)
        }

        return textFieldValue.copy(annotatedString = newAnnotatedString)
    }

    /**
     * Improved link application that preserves formatting
     */
    fun applyLink(textFieldValue: TextFieldValue, url: String, text: String, selection: TextRange): TextFieldValue {
        val currentAnnotatedString = textFieldValue.annotatedString
        val currentText = currentAnnotatedString.text

        val newAnnotatedString = buildAnnotatedString {
            if (selection.collapsed) {
                // Insert link at cursor position
                val insertPosition = selection.start.coerceIn(0, currentText.length)
                val beforeText = currentText.substring(0, insertPosition)
                val afterText = currentText.substring(insertPosition)

                append(beforeText)
                append(text)
                append(afterText)

                // Apply link styling
                addStringAnnotation("URL", url, beforeText.length, beforeText.length + text.length)
                addStyle(
                    SpanStyle(color = Color.Blue, textDecoration = TextDecoration.Underline),
                    beforeText.length,
                    beforeText.length + text.length
                )

                // Preserve existing styles with position adjustments
                preserveExistingStyles(this, currentAnnotatedString, insertPosition, text.length, true)
            } else {
                // Replace selected text with link
                val replaceStart = selection.min.coerceIn(0, currentText.length)
                val replaceEnd = selection.max.coerceIn(0, currentText.length)
                val beforeSelection = currentText.substring(0, replaceStart)
                val afterSelection = currentText.substring(replaceEnd)

                append(beforeSelection)
                append(text)
                append(afterSelection)

                // Apply link styling
                addStringAnnotation("URL", url, beforeSelection.length, beforeSelection.length + text.length)
                addStyle(
                    SpanStyle(color = Color.Blue, textDecoration = TextDecoration.Underline),
                    beforeSelection.length,
                    beforeSelection.length + text.length
                )

                // Preserve existing styles with replacement adjustments
                preserveExistingStylesForReplacement(this, currentAnnotatedString, replaceStart, replaceEnd, text.length)
            }
        }

        val newCursorPosition = if (selection.collapsed) {
            selection.start + text.length
        } else {
            selection.min + text.length
        }

        return TextFieldValue(
            annotatedString = newAnnotatedString,
            selection = TextRange(newCursorPosition)
        )
    }

    /**
     * Preserve existing styles when inserting text
     */
    private fun preserveExistingStyles(
        builder: AnnotatedString.Builder,
        original: AnnotatedString,
        insertPosition: Int,
        insertLength: Int,
        isInsertion: Boolean
    ) {
        // Preserve span styles
        original.spanStyles.forEach { span ->
            when {
                span.end <= insertPosition -> {
                    // Style is before insertion - copy as is
                    builder.addStyle(span.item, span.start, span.end)
                }
                span.start >= insertPosition -> {
                    // Style is after insertion - shift position
                    builder.addStyle(span.item, span.start + insertLength, span.end + insertLength)
                }
                else -> {
                    // Style spans across insertion point
                    builder.addStyle(span.item, span.start, span.end + insertLength)
                }
            }
        }

        // Preserve paragraph styles
        original.paragraphStyles.forEach { para ->
            when {
                para.end <= insertPosition -> {
                    builder.addStyle(para.item, para.start, para.end)
                }
                para.start >= insertPosition -> {
                    builder.addStyle(para.item, para.start + insertLength, para.end + insertLength)
                }
                else -> {
                    builder.addStyle(para.item, para.start, para.end + insertLength)
                }
            }
        }

        // Preserve string annotations
        for (annotation in original.getStringAnnotations(0, original.text.length)) {
            when {
                annotation.end <= insertPosition -> {
                    builder.addStringAnnotation(annotation.tag, annotation.item, annotation.start, annotation.end)
                }
                annotation.start >= insertPosition -> {
                    builder.addStringAnnotation(annotation.tag, annotation.item,
                        annotation.start + insertLength, annotation.end + insertLength)
                }
                else -> {
                    builder.addStringAnnotation(annotation.tag, annotation.item,
                        annotation.start, annotation.end + insertLength)
                }
            }
        }
    }

    /**
     * Preserve existing styles when replacing text
     */
    private fun preserveExistingStylesForReplacement(
        builder: AnnotatedString.Builder,
        original: AnnotatedString,
        replaceStart: Int,
        replaceEnd: Int,
        newTextLength: Int
    ) {
        val lengthDiff = newTextLength - (replaceEnd - replaceStart)

        // Preserve span styles
        original.spanStyles.forEach { span ->
            when {
                span.end <= replaceStart -> {
                    // Style is completely before replacement
                    builder.addStyle(span.item, span.start, span.end)
                }
                span.start >= replaceEnd -> {
                    // Style is completely after replacement
                    builder.addStyle(span.item, span.start + lengthDiff, span.end + lengthDiff)
                }
                span.start < replaceStart && span.end > replaceEnd -> {
                    // Style spans the entire replacement
                    builder.addStyle(span.item, span.start, span.end + lengthDiff)
                }
                // Skip styles that are partially within the replacement
            }
        }

        // Similar logic for paragraph styles and annotations
        original.paragraphStyles.forEach { para ->
            when {
                para.end <= replaceStart -> {
                    builder.addStyle(para.item, para.start, para.end)
                }
                para.start >= replaceEnd -> {
                    builder.addStyle(para.item, para.start + lengthDiff, para.end + lengthDiff)
                }
                para.start < replaceStart && para.end > replaceEnd -> {
                    builder.addStyle(para.item, para.start, para.end + lengthDiff)
                }
            }
        }

        for (annotation in original.getStringAnnotations(0, original.text.length)) {
            when {
                annotation.end <= replaceStart -> {
                    builder.addStringAnnotation(annotation.tag, annotation.item, annotation.start, annotation.end)
                }
                annotation.start >= replaceEnd -> {
                    builder.addStringAnnotation(annotation.tag, annotation.item,
                        annotation.start + lengthDiff, annotation.end + lengthDiff)
                }
                annotation.start < replaceStart && annotation.end > replaceEnd -> {
                    builder.addStringAnnotation(annotation.tag, annotation.item,
                        annotation.start, annotation.end + lengthDiff)
                }
            }
        }
    }

    /**
     * Toggle paragraph style with better handling
     */
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

            // Check if style is already applied to the paragraph
            val existingStyles = currentAnnotatedString.paragraphStyles.filter { range ->
                maxOf(range.start, paraStart) < minOf(range.end, paraEnd)
            }

            val styleAlreadyApplied = existingStyles.any { range ->
                range.item.textIndent == styleToToggle.textIndent
            }

            // Toggle the style
            if (styleAlreadyApplied) {
                // Remove the style by applying empty paragraph style
                addStyle(ParagraphStyle(), paraStart, paraEnd)
            } else {
                // Apply the style
                addStyle(styleToToggle, paraStart, paraEnd)
            }
        }

        return textFieldValue.copy(annotatedString = newAnnotatedString, selection = selection)
    }

    /**
     * Utility functions for markdown processing
     */
    fun fixMarkdownErrors(markdownContent: String): String {
        Log.d("RichTextFormatter", "Fixing markdown errors in content")
        return markdownContent
            .replace(Regex("""\*\*\*\*(.*?)\*\*\*\*"""), "**$1**") // Fix quadruple asterisks
            .replace(Regex("""_____(.*?)_____"""), "_$1_") // Fix quintuple underscores
            .replace(Regex("""\n\n\n+"""), "\n\n") // Fix excessive line breaks
    }

    fun stripMarkdown(markdownContent: String): String {
        Log.d("RichTextFormatter", "Stripping markdown from content")
        return markdownContent
            .replace(Regex("""\*\*(.*?)\*\*"""), "$1") // Remove bold
            .replace(Regex("""\*(.*?)\*"""), "$1") // Remove italic
            .replace(Regex("""__(.*?)__"""), "$1") // Remove underline
            .replace(Regex("""~~(.*?)~~"""), "$1") // Remove strikethrough
            .replace(Regex("""^#+\s*(.*)""", RegexOption.MULTILINE), "$1") // Remove headers
            .replace(Regex("""\[(.*?)\]\(.*?\)"""), "$1") // Remove links, keep text
            .replace(Regex("""\!\[(.*?)\]\(.*?\)"""), "$1") // Remove images, keep alt text
            .replace(Regex("""`(.*?)`"""), "$1") // Remove inline code
            .replace(Regex("""^>\s*(.*)""", RegexOption.MULTILINE), "$1") // Remove blockquotes
    }
}