package com.example.notaflow.utils

import android.text.Html
import android.text.Spanned
import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.unit.sp
import java.util.Stack // Keep if complex toHtml logic is re-enabled

/**
 * Utility object for converting between AnnotatedString and HTML-like representation.
 * This version adds support for underline, strikethrough, and blockquotes.
 */
object RichTextConverter {

    private const val TAG = "RichTextConverter"

    // Define HTML-like tags
    private const val BOLD_TAG_OPEN = "<b>"
    private const val BOLD_TAG_CLOSE = "</b>"
    private const val ITALIC_TAG_OPEN = "<i>"
    private const val ITALIC_TAG_CLOSE = "</i>"
    private const val UNDERLINE_TAG_OPEN = "<u>"
    private const val UNDERLINE_TAG_CLOSE = "</u>"
    private const val STRIKETHROUGH_TAG_OPEN = "<s>"
    private const val STRIKETHROUGH_TAG_CLOSE = "</s>"
    private const val BLOCKQUOTE_TAG_OPEN = "<blockquote>"
    private const val BLOCKQUOTE_TAG_CLOSE = "</blockquote>"
    private fun fontColorTagOpen(colorHex: String) = "<font color=\"$colorHex\">"
    private const val FONT_COLOR_TAG_CLOSE = "</font>"
    private fun linkTagOpen(url: String) = "<a href=\"$url\">"
    private const val LINK_TAG_CLOSE = "</a>"

    // Regex for parsing
    // Corrected: Use RegexOption.DOT_MATCHES_ALL
    private val boldRegex = Regex("""<b>(.*?)</b>""", RegexOption.DOT_MATCHES_ALL)
    private val italicRegex = Regex("""<i>(.*?)</i>""", RegexOption.DOT_MATCHES_ALL)
    private val underlineRegex = Regex("""<u>(.*?)</u>""", RegexOption.DOT_MATCHES_ALL)
    private val strikethroughRegex = Regex("""<s>(.*?)</s>""", RegexOption.DOT_MATCHES_ALL)
    private val blockquoteRegex = Regex("""<blockquote>(.*?)</blockquote>""", RegexOption.DOT_MATCHES_ALL)
    private val fontColorRegex = Regex("""<font color="(#[0-9a-fA-F]{6,8})">(.*?)</font>""", RegexOption.DOT_MATCHES_ALL) // Adjusted to capture #
    private val linkRegex = Regex("""<a href="(.*?)">(.*?)</a>""", RegexOption.DOT_MATCHES_ALL)


    /**
     * Converts an AnnotatedString to an HTML-like string.
     */
    fun toHtml(annotatedString: AnnotatedString): String {
        val text = annotatedString.text
        val spans = annotatedString.spanStyles
        val paragraphs = annotatedString.paragraphStyles
        val stringBuilder = StringBuilder()

        val tagMap = mutableMapOf<Int, MutableList<String>>()

        spans.forEach { range ->
            val style = range.item
            if (style.fontWeight == FontWeight.Bold) {
                tagMap.getOrPut(range.start) { mutableListOf() }.add(BOLD_TAG_OPEN)
                tagMap.getOrPut(range.end) { mutableListOf() }.add(BOLD_TAG_CLOSE)
            }
            if (style.fontStyle == FontStyle.Italic) {
                tagMap.getOrPut(range.start) { mutableListOf() }.add(ITALIC_TAG_OPEN)
                tagMap.getOrPut(range.end) { mutableListOf() }.add(ITALIC_TAG_CLOSE)
            }
            if (style.textDecoration?.contains(TextDecoration.Underline) == true) {
                tagMap.getOrPut(range.start) { mutableListOf() }.add(UNDERLINE_TAG_OPEN)
                tagMap.getOrPut(range.end) { mutableListOf() }.add(UNDERLINE_TAG_CLOSE)
            }
            if (style.textDecoration?.contains(TextDecoration.LineThrough) == true) {
                tagMap.getOrPut(range.start) { mutableListOf() }.add(STRIKETHROUGH_TAG_OPEN)
                tagMap.getOrPut(range.end) { mutableListOf() }.add(STRIKETHROUGH_TAG_CLOSE)
            }
            if (style.color != Color.Unspecified) {
                // Convert Compose Color to hex string #RRGGBB
                val red = (style.color.red * 255).toInt()
                val green = (style.color.green * 255).toInt()
                val blue = (style.color.blue * 255).toInt()
                val colorHex = String.format("#%02X%02X%02X", red, green, blue)
                tagMap.getOrPut(range.start) { mutableListOf() }.add(fontColorTagOpen(colorHex))
                tagMap.getOrPut(range.end) { mutableListOf() }.add(FONT_COLOR_TAG_CLOSE)
            }
        }
        annotatedString.getStringAnnotations("URL", 0, text.length).forEach { range ->
            tagMap.getOrPut(range.start) { mutableListOf() }.add(linkTagOpen(range.item))
            tagMap.getOrPut(range.end) { mutableListOf() }.add(LINK_TAG_CLOSE)
        }
        paragraphs.forEach { range ->
            // Example for blockquote based on textIndent
            if (range.item.textIndent == TextIndent(16.sp, 16.sp)) { // Ensure this matches how blockquotes are styled
                tagMap.getOrPut(range.start) { mutableListOf() }.add(BLOCKQUOTE_TAG_OPEN)
                tagMap.getOrPut(range.end) { mutableListOf() }.add(BLOCKQUOTE_TAG_CLOSE)
            }
        }

        val sortedIndices = tagMap.keys.sorted()
        var lastProcessedCharIndex = -1

        for (i in text.indices) {
            // Process tags for index i
            tagMap[i]?.filter { it.startsWith("</") }?.reversed()?.forEach { stringBuilder.append(it) }
            tagMap[i]?.filterNot { it.startsWith("</") }?.forEach { stringBuilder.append(it) }
            stringBuilder.append(text[i])
            lastProcessedCharIndex = i
        }

        // Append any tags that are at the very end of the string (after the last character)
        tagMap[text.length]?.filter { it.startsWith("</") }?.reversed()?.forEach { stringBuilder.append(it) }
        tagMap[text.length]?.filterNot { it.startsWith("</") }?.forEach { stringBuilder.append(it) }


        // Log.d(TAG, "toHtml Output: ${stringBuilder.toString()}")
        return stringBuilder.toString().replace("\n", "<br>")
    }

    /**
     * Converts an HTML-like string to an AnnotatedString.
     */
    fun fromHtml(html: String): AnnotatedString {
        if (html.isEmpty()) return AnnotatedString("")
        // Log.d(TAG, "fromHtml Input: $html")

        val textWithNewlines = html.replace("<br>", "\n", ignoreCase = true).replace("<br/>", "\n", ignoreCase = true)

        val spanned: Spanned = Html.fromHtml(textWithNewlines, Html.FROM_HTML_MODE_LEGACY)

        return buildAnnotatedString {
            append(spanned.toString())
            val plainText = spanned.toString()

            boldRegex.findAll(textWithNewlines).forEach { matchResult ->
                findAndApplyStyle(plainText, textWithNewlines, matchResult, SpanStyle(fontWeight = FontWeight.Bold))
            }
            italicRegex.findAll(textWithNewlines).forEach { matchResult ->
                findAndApplyStyle(plainText, textWithNewlines, matchResult, SpanStyle(fontStyle = FontStyle.Italic))
            }
            underlineRegex.findAll(textWithNewlines).forEach { matchResult ->
                findAndApplyStyle(plainText, textWithNewlines, matchResult, SpanStyle(textDecoration = TextDecoration.Underline))
            }
            strikethroughRegex.findAll(textWithNewlines).forEach { matchResult ->
                findAndApplyStyle(plainText, textWithNewlines, matchResult, SpanStyle(textDecoration = TextDecoration.LineThrough))
            }
            fontColorRegex.findAll(textWithNewlines).forEach { matchResult ->
                val (colorHexWithHash, _) = matchResult.destructured // content is groupValues[2]
                try {
                    val color = Color(android.graphics.Color.parseColor(colorHexWithHash)) // colorHexWithHash already has #
                    findAndApplyStyle(plainText, textWithNewlines, matchResult, SpanStyle(color = color))
                } catch (e: IllegalArgumentException) {
                    Log.e(TAG, "Invalid color hex: $colorHexWithHash", e)
                }
            }
            linkRegex.findAll(textWithNewlines).forEach { matchResult ->
                val (url, linkTextHtml) = matchResult.destructured
                val cleanLinkText = Html.fromHtml(linkTextHtml, Html.FROM_HTML_MODE_LEGACY).toString()
                val foundIndices = findTextOccurrences(plainText, cleanLinkText) // Get all occurrences

                // This is a heuristic: try to match based on original HTML position to disambiguate
                // For simplicity, applying to all found occurrences if not easily disambiguated
                foundIndices.forEach { index ->
                    try {
                        addStyle(SpanStyle(color = Color.Blue, textDecoration = TextDecoration.Underline), index, index + cleanLinkText.length)
                        addStringAnnotation("URL", url, index, index + cleanLinkText.length)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error applying link style for '$cleanLinkText': ${e.message}")
                    }
                }
            }
            blockquoteRegex.findAll(textWithNewlines).forEach { matchResult ->
                val (contentHtml) = matchResult.destructured
                val cleanContent = Html.fromHtml(contentHtml, Html.FROM_HTML_MODE_LEGACY).toString()
                val foundIndices = findTextOccurrences(plainText, cleanContent)
                foundIndices.forEach { index ->
                    if ((index == 0 || plainText.getOrNull(index - 1) == '\n')) {
                        try {
                            // ParagraphStyle applies to [start, end), ensure end is at newline or end of text for proper paragraph
                            var paraEnd = index + cleanContent.length
                            if (paraEnd < plainText.length && plainText[paraEnd] == '\n') {
                                paraEnd++ // Include the newline for the paragraph
                            } else if (paraEnd == plainText.length) {
                                // At the end of the text
                            } else {
                                // Content doesn't end with a newline, might not be a full paragraph
                                // For simplicity, still apply if it's a block
                            }
                            addStyle(ParagraphStyle(textIndent = TextIndent(16.sp, 16.sp)), index, paraEnd)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error applying blockquote style for '$cleanContent': ${e.message}")
                        }
                    }
                }
            }
        }
    }

    // Helper to find all occurrences of a substring
    private fun findTextOccurrences(text: String, sub: String): List<Int> {
        if (sub.isEmpty()) return emptyList()
        val indices = mutableListOf<Int>()
        var startIndex = 0
        while (startIndex < text.length) {
            val index = text.indexOf(sub, startIndex)
            if (index != -1) {
                indices.add(index)
                startIndex = index + sub.length
            } else {
                break
            }
        }
        return indices
    }


    private fun AnnotatedString.Builder.findAndApplyStyle(
        plainText: String,
        originalHtml: String,
        htmlMatchResult: MatchResult,
        styleToApply: SpanStyle
    ) {
        val htmlContent = htmlMatchResult.groupValues[1]
        val plainContent = Html.fromHtml(htmlContent, Html.FROM_HTML_MODE_LEGACY).toString()
        if (plainContent.isEmpty()) return

        val foundIndices = findTextOccurrences(plainText, plainContent)
        // Basic heuristic: if only one match in plainText, use it.
        // Otherwise, this simple version might misapply styles if plainContent is common.
        // A more advanced version would use the original HTML match position as a stronger hint.
        foundIndices.forEach { index ->
            try {
                addStyle(styleToApply, index, index + plainContent.length)
            } catch (e: Exception) {
                Log.e(TAG, "Error in findAndApplyStyle for '$plainContent': ${e.message} at index $index")
            }
        }
    }
}
