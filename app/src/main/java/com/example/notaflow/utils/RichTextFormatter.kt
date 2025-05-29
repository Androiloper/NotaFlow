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
                StyleType.UNDERLINE -> isCurrentlyApplied = stylesInSelection.any { it.item.textDecoration?.contains(TextDecoration.Underline) == true }
                StyleType.STRIKETHROUGH -> isCurrentlyApplied = stylesInSelection.any { it.item.textDecoration?.contains(TextDecoration.LineThrough) == true }
            }

            val targetStyle: SpanStyle = when (styleType) {
                StyleType.BOLD -> if (isCurrentlyApplied) SpanStyle(fontWeight = FontWeight.Normal) else SpanStyle(fontWeight = FontWeight.Bold)
                StyleType.ITALIC -> if (isCurrentlyApplied) SpanStyle(fontStyle = FontStyle.Normal) else SpanStyle(fontStyle = FontStyle.Italic)
                StyleType.UNDERLINE -> {
                    val currentGlobalDecoration = stylesInSelection.mapNotNull { it.item.textDecoration }.fold(null as TextDecoration?) { acc, current -> acc?.plus(current) ?: current }
                    if (isCurrentlyApplied) {
                        val newDecoration = currentGlobalDecoration?.minus(TextDecoration.Underline)
                        SpanStyle(textDecoration = if (newDecoration?.mask == 0) null else newDecoration)
                    } else {
                        SpanStyle(textDecoration = (currentGlobalDecoration ?: TextDecoration.None).plus(TextDecoration.Underline))
                    }
                }
                StyleType.STRIKETHROUGH -> {
                    val currentGlobalDecoration = stylesInSelection.mapNotNull { it.item.textDecoration }.fold(null as TextDecoration?) { acc, current -> acc?.plus(current) ?: current }
                    if (isCurrentlyApplied) {
                        val newDecoration = currentGlobalDecoration?.minus(TextDecoration.LineThrough)
                        SpanStyle(textDecoration = if (newDecoration?.mask == 0) null else newDecoration)
                    } else {
                        SpanStyle(textDecoration = (currentGlobalDecoration ?: TextDecoration.None).plus(TextDecoration.LineThrough))
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
        val newAnnotatedString = buildAnnotatedString {
            val builder: AnnotatedString.Builder = this
            builder.append(currentAnnotatedString)

            val actualStart: Int
            val actualEnd: Int

            if (selection.collapsed) {
                builder.insert(selection.start, text)
                actualStart = selection.start
                actualEnd = selection.start + text.length
            } else {
                builder.replace(selection.min, selection.max, text)
                actualStart = selection.min
                actualEnd = selection.min + text.length
            }

            builder.addStringAnnotation("URL", url, actualStart, actualEnd)
            builder.addStyle(SpanStyle(color = Color.Blue, textDecoration = TextDecoration.Underline), actualStart, actualEnd)
        }
        return textFieldValue.copy(annotatedString = newAnnotatedString, selection = TextRange(actualEnd))
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
        if (paraEnd < currentAnnotatedString.text.length && currentAnnotatedString.text[paraEnd] == '\n') {
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
