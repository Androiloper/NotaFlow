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
    private val fontColorRegex = Regex("""<font color="#([0-9a-fA-F]{6,8})">(.*?)</font>""", RegexOption.DOT_MATCHES_ALL)
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
                val colorValue = style.color.value
                // Format as RRGGBB, ignoring alpha for standard HTML color
                val colorHex = String.format("%06X", colorValue and 16777215u)
                tagMap.getOrPut(range.start) { mutableListOf() }.add(fontColorTagOpen("#$colorHex"))
                tagMap.getOrPut(range.end) { mutableListOf() }.add(FONT_COLOR_TAG_CLOSE)
            }
        }
        annotatedString.getStringAnnotations("URL", 0, text.length).forEach { range ->
            tagMap.getOrPut(range.start) { mutableListOf() }.add(linkTagOpen(range.item))
            tagMap.getOrPut(range.end) { mutableListOf() }.add(LINK_TAG_CLOSE)
        }
        paragraphs.forEach { range ->
            // Example for blockquote based on textIndent
            if (range.item.textIndent == TextIndent(16.sp, 16.sp)) {
                tagMap.getOrPut(range.start) { mutableListOf() }.add(BLOCKQUOTE_TAG_OPEN)
                tagMap.getOrPut(range.end) { mutableListOf() }.add(BLOCKQUOTE_TAG_CLOSE)
            }
        }

        // Iterate through text, applying tags at boundaries
        // This simplified logic handles basic cases but might not perfectly reconstruct complex nested HTML.
        for (i in text.indices) {
            // Append closing tags first (in reverse order of typical opening)
            // A proper stack-based approach would be more robust for perfect nesting.
            tagMap[i]?.filter { it.startsWith("</") }?.reversed()?.forEach { stringBuilder.append(it) }
            tagMap[i]?.filterNot { it.startsWith("</") }?.forEach { stringBuilder.append(it) }
            stringBuilder.append(text[i])
        }
        // Append any closing tags at the very end of the text
        tagMap[text.length]?.filter { it.startsWith("</") }?.reversed()?.forEach { stringBuilder.append(it) }
        // Opening tags at text.length should ideally not happen if spans are correctly bounded.

        Log.d(TAG, "toHtml Output: ${stringBuilder.toString()}")
        return stringBuilder.toString().replace("\n", "<br>") // Basic newline handling
    }

    /**
     * Converts an HTML-like string to an AnnotatedString.
     */
    fun fromHtml(html: String): AnnotatedString {
        if (html.isEmpty()) return AnnotatedString("")
        Log.d(TAG, "fromHtml Input: $html")

        val textWithNewlines = html.replace("<br>", "\n", ignoreCase = true).replace("<br/>", "\n", ignoreCase = true)

        // Corrected: Use Html.fromHtml with explicit flags
        val spanned: Spanned = Html.fromHtml(textWithNewlines, Html.FROM_HTML_MODE_LEGACY)

        return buildAnnotatedString {
            append(spanned.toString())
            val plainText = spanned.toString()

            // Apply styles based on regex matches on the original HTML (textWithNewlines)
            // This approach has limitations with accurately mapping indices if tags are heavily nested or overlapping.
            // A sequential parser would be more robust.

            // Bold
            boldRegex.findAll(textWithNewlines).forEach { matchResult ->
                val (content) = matchResult.destructured
                // Corrected: Use Html.fromHtml with explicit flags
                findAndApplyStyle(plainText, textWithNewlines, matchResult, SpanStyle(fontWeight = FontWeight.Bold))
            }
            // Italic
            italicRegex.findAll(textWithNewlines).forEach { matchResult ->
                val (content) = matchResult.destructured
                findAndApplyStyle(plainText, textWithNewlines, matchResult, SpanStyle(fontStyle = FontStyle.Italic))
            }
            // Underline
            underlineRegex.findAll(textWithNewlines).forEach { matchResult ->
                val (content) = matchResult.destructured
                findAndApplyStyle(plainText, textWithNewlines, matchResult, SpanStyle(textDecoration = TextDecoration.Underline))
            }
            // Strikethrough
            strikethroughRegex.findAll(textWithNewlines).forEach { matchResult ->
                val (content) = matchResult.destructured
                findAndApplyStyle(plainText, textWithNewlines, matchResult, SpanStyle(textDecoration = TextDecoration.LineThrough))
            }

            // Font Color
            fontColorRegex.findAll(textWithNewlines).forEach { matchResult ->
                val (colorHexWithHash, content) = matchResult.destructured
                try {
                    // Ensure colorHexWithHash includes '#' for parsing
                    val colorString = if (colorHexWithHash.startsWith("#")) colorHexWithHash else "#$colorHexWithHash"
                    val color = Color(android.graphics.Color.parseColor(colorString))
                    findAndApplyStyle(plainText, textWithNewlines, matchResult, SpanStyle(color = color))
                } catch (e: IllegalArgumentException) {
                    Log.e(TAG, "Invalid color hex: $colorHexWithHash", e)
                }
            }

            // Links
            linkRegex.findAll(textWithNewlines).forEach { matchResult ->
                val (url, linkTextHtml) = matchResult.destructured
                // Corrected: Use Html.fromHtml with explicit flags
                val cleanLinkText = Html.fromHtml(linkTextHtml, Html.FROM_HTML_MODE_LEGACY).toString()

                var searchStartIndexInPlainText = 0
                val originalMatchStartIndex = matchResult.range.first

                // Try to find the plain text version of linkTextHtml starting from a similar position
                // This is an approximation.
                val estimatedPlainTextStartIndex = plainText.length * originalMatchStartIndex / textWithNewlines.length

                val foundPlainTextIndex = plainText.indexOf(cleanLinkText, startIndex = maxOf(0, estimatedPlainTextStartIndex - cleanLinkText.length - 10))
                    .takeIf { it != -1} ?: plainText.indexOf(cleanLinkText) // Fallback to search from start


                if (foundPlainTextIndex != -1) {
                    val start = foundPlainTextIndex
                    val end = foundPlainTextIndex + cleanLinkText.length
                    try {
                        addStyle(SpanStyle(color = Color.Blue, textDecoration = TextDecoration.Underline), start, end)
                        addStringAnnotation("URL", url, start, end)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error applying link style for '$cleanLinkText': ${e.message}")
                    }
                } else {
                    Log.w(TAG, "Could not accurately map link text '$cleanLinkText' from HTML to plain text.")
                }
            }

            // Blockquotes (ParagraphStyle)
            blockquoteRegex.findAll(textWithNewlines).forEach { matchResult ->
                val (contentHtml) = matchResult.destructured
                // Corrected: Use Html.fromHtml with explicit flags
                val cleanContent = Html.fromHtml(contentHtml, Html.FROM_HTML_MODE_LEGACY).toString()

                // Similar logic to links for finding the plain text content
                var searchStartIndexInPlainText = 0
                val originalMatchStartIndex = matchResult.range.first
                val estimatedPlainTextStartIndex = plainText.length * originalMatchStartIndex / textWithNewlines.length

                val foundPlainTextIndex = plainText.indexOf(cleanContent, startIndex = maxOf(0, estimatedPlainTextStartIndex - cleanContent.length - 10))
                    .takeIf { it != -1} ?: plainText.indexOf(cleanContent)


                if (foundPlainTextIndex != -1) {
                    val start = foundPlainTextIndex
                    val end = foundPlainTextIndex + cleanContent.length
                    // Basic check for paragraph boundaries (start of text or after newline)
                    // This is a simplification.
                    if ((start == 0 || plainText.getOrNull(start - 1) == '\n') &&
                        (end == plainText.length || plainText.getOrNull(end) == '\n' || plainText.getOrNull(end-1) == '\n')) {
                        try {
                            addStyle(ParagraphStyle(textIndent = TextIndent(16.sp, 16.sp)), start, end)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error applying blockquote style for '$cleanContent': ${e.message}")
                        }
                    } else {
                        Log.w(TAG, "Blockquote content '$cleanContent' not on clear paragraph boundary.")
                    }
                } else {
                    Log.w(TAG, "Could not accurately map blockquote content '$cleanContent' from HTML to plain text.")
                }
            }
        }
    }

    // Helper to find content in plainText based on original HTML match, and apply style
    private fun AnnotatedString.Builder.findAndApplyStyle(
        plainText: String,
        originalHtml: String,
        htmlMatchResult: MatchResult,
        styleToApply: SpanStyle
    ) {
        val htmlContent = htmlMatchResult.groupValues[1] // The content within the tags
        // Corrected: Use Html.fromHtml with explicit flags
        val plainContent = Html.fromHtml(htmlContent, Html.FROM_HTML_MODE_LEGACY).toString()
        if (plainContent.isEmpty()) return

        // Attempt to locate plainContent in plainText, using original match position as a hint
        val originalMatchStartIndex = htmlMatchResult.range.first

        // Estimate where the plainContent might start in plainText
        // This is a heuristic and not perfectly accurate for complex HTML.
        var searchStartIndex = 0
        if (originalHtml.isNotEmpty()) { // Avoid division by zero
            searchStartIndex = (plainText.length * originalMatchStartIndex) / originalHtml.length
            searchStartIndex = maxOf(0, searchStartIndex - plainContent.length) // Search a bit before the estimate
        }


        var foundIndex = plainText.indexOf(plainContent, startIndex = searchStartIndex)
        if (foundIndex == -1) { // If not found near estimate, search from beginning
            foundIndex = plainText.indexOf(plainContent)
        }

        if (foundIndex != -1) {
            try {
                addStyle(styleToApply, foundIndex, foundIndex + plainContent.length)
            } catch (e: Exception) {
                Log.e(TAG, "Error in findAndApplyStyle for '$plainContent': ${e.message} at index $foundIndex")
            }
        } else {
            Log.w(TAG, "Could not accurately map HTML content '$htmlContent' (plain: '$plainContent') to plain text.")
        }
    }
}
