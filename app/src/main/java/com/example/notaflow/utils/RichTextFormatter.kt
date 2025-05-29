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
        BLOCKQUOTE // Add more like lists later
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

            val styleAlreadyApplied = stylesInSelection.any { range ->
                when (styleType) {
                    StyleType.BOLD -> range.item.fontWeight == FontWeight.Bold
                    StyleType.ITALIC -> range.item.fontStyle == FontStyle.Italic
                    StyleType.UNDERLINE -> range.item.textDecoration?.contains(TextDecoration.Underline) == true
                    StyleType.STRIKETHROUGH -> range.item.textDecoration?.contains(TextDecoration.LineThrough) == true
                }
            }

            if (styleAlreadyApplied) {
                val neutralizingStyle = when (styleType) {
                    StyleType.BOLD -> SpanStyle(fontWeight = FontWeight.Normal)
                    StyleType.ITALIC -> SpanStyle(fontStyle = FontStyle.Normal)
                    StyleType.UNDERLINE -> {
                        val combinedCurrentDecorations = stylesInSelection
                            .mapNotNull { it.item.textDecoration }
                            .reduceOrNull { acc, deco -> acc + deco }
                        val resultingDecoration = combinedCurrentDecorations?.minus(TextDecoration.Underline)
                        SpanStyle(textDecoration = if (resultingDecoration == TextDecoration.None || resultingDecoration?.mask == 0) null else resultingDecoration)
                    }
                    StyleType.STRIKETHROUGH -> {
                        val combinedCurrentDecorations = stylesInSelection
                            .mapNotNull { it.item.textDecoration }
                            .reduceOrNull { acc, deco -> acc + deco }
                        val resultingDecoration = combinedCurrentDecorations?.minus(TextDecoration.LineThrough)
                        SpanStyle(textDecoration = if (resultingDecoration == TextDecoration.None || resultingDecoration?.mask == 0) null else resultingDecoration)
                    }
                }
                addStyle(neutralizingStyle, selection.min, selection.max)
            } else {
                // Add the style
                val styleToAdd = when (styleType) {
                    StyleType.BOLD -> SpanStyle(fontWeight = FontWeight.Bold)
                    StyleType.ITALIC -> SpanStyle(fontStyle = FontStyle.Italic)
                    StyleType.UNDERLINE -> {
                        val combinedCurrentDecorations = stylesInSelection
                            .mapNotNull { it.item.textDecoration }
                            .reduceOrNull { acc, deco -> acc + deco }
                        val finalDecoration = combinedCurrentDecorations?.plus(TextDecoration.Underline) ?: TextDecoration.Underline
                        SpanStyle(textDecoration = finalDecoration)
                    }
                    StyleType.STRIKETHROUGH -> {
                        val combinedCurrentDecorations = stylesInSelection
                            .mapNotNull { it.item.textDecoration }
                            .reduceOrNull { acc, deco -> acc + deco }
                        val finalDecoration = combinedCurrentDecorations?.plus(TextDecoration.LineThrough) ?: TextDecoration.LineThrough
                        SpanStyle(textDecoration = finalDecoration)
                    }
                }
                addStyle(styleToAdd, selection.min, selection.max)
            }
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
        val newAnnotatedString = buildAnnotatedString {
            // It's important to qualify 'this' if there's any ambiguity,
            // but usually it's inferred correctly in the buildAnnotatedString lambda.
            val builder: AnnotatedString.Builder = this

            builder.append(textFieldValue.annotatedString)
            val start: Int
            val end: Int
            if (selection.collapsed) {
                builder.insert(selection.start, text)
                start = selection.start
                end = selection.start + text.length
            } else {
                builder.replace(selection.min, selection.max, text)
                start = selection.min
                end = selection.min + text.length
            }
            builder.addStringAnnotation("URL", url, start, end)
            builder.addStyle(SpanStyle(color = Color.Blue, textDecoration = TextDecoration.Underline), start, end)
        }
        return textFieldValue.copy(annotatedString = newAnnotatedString, selection = TextRange(end))
    }


    fun toggleParagraphStyle(textFieldValue: TextFieldValue, styleType: ParagraphStyleType): TextFieldValue {
        val selection = textFieldValue.selection
        val currentAnnotatedString = textFieldValue.annotatedString

        var paraStart = selection.min
        while (paraStart > 0 && currentAnnotatedString.text[paraStart - 1] != '\n') {
            paraStart--
        }
        var paraEnd = selection.max
        while (paraEnd < currentAnnotatedString.text.length && currentAnnotatedString.text[paraEnd] != '\n') {
            paraEnd++
        }

        val newAnnotatedString = buildAnnotatedString {
            append(currentAnnotatedString)
            val styleToToggle = when (styleType) {
                ParagraphStyleType.BLOCKQUOTE -> ParagraphStyle(textIndent = TextIndent(16.sp, 16.sp))
            }
            val existingStyles = currentAnnotatedString.paragraphStyles.filter { range ->
                maxOf(range.start, paraStart) < minOf(range.end, paraEnd)
            }
            val styleAlreadyApplied = existingStyles.any { range ->
                range.item.textIndent == styleToToggle.textIndent
            }

            if (styleAlreadyApplied) {
                addStyle(ParagraphStyle(), paraStart, paraEnd)
            } else {
                addStyle(styleToToggle, paraStart, paraEnd)
            }
        }
        return textFieldValue.copy(annotatedString = newAnnotatedString, selection = TextRange(paraStart, paraEnd))
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
