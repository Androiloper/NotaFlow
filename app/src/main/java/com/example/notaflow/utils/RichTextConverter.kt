package com.example.notaflow.utils

import android.text.Html
import android.text.Spanned
import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.unit.sp

/**
 * Simple utility object for converting between AnnotatedString and HTML.
 * Avoids all BigInteger operations that cause compilation issues.
 */
object RichTextConverter {

    private const val TAG = "RichTextConverter"

    /**
     * Convert HTML to AnnotatedString with basic formatting support
     */
    fun fromHtml(html: String): AnnotatedString {
        if (html.isEmpty()) return AnnotatedString("")

        return try {
            // Clean up HTML
            val cleanHtml = html
                .replace("<br>", "\n", ignoreCase = true)
                .replace("<br/>", "\n", ignoreCase = true)
                .replace("<br />", "\n", ignoreCase = true)

            // Get plain text using Android's Html.fromHtml
            val spanned: Spanned = Html.fromHtml(cleanHtml, Html.FROM_HTML_MODE_LEGACY)
            val plainText = spanned.toString()

            buildAnnotatedString {
                append(plainText)

                // Apply bold formatting
                applySimpleFormatting(this, cleanHtml, plainText, "<b>", "</b>") {
                    SpanStyle(fontWeight = FontWeight.Bold)
                }

                // Apply italic formatting
                applySimpleFormatting(this, cleanHtml, plainText, "<i>", "</i>") {
                    SpanStyle(fontStyle = FontStyle.Italic)
                }

                // Apply underline formatting
                applySimpleFormatting(this, cleanHtml, plainText, "<u>", "</u>") {
                    SpanStyle(textDecoration = TextDecoration.Underline)
                }

                // Apply strikethrough formatting
                applySimpleFormatting(this, cleanHtml, plainText, "<s>", "</s>") {
                    SpanStyle(textDecoration = TextDecoration.LineThrough)
                }

                // Apply basic color formatting
                applyColorFormatting(this, cleanHtml, plainText)

                // Apply link formatting
                applyLinkFormatting(this, cleanHtml, plainText)

                // Apply blockquote formatting
                applyBlockquoteFormatting(this, cleanHtml, plainText)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error converting HTML to AnnotatedString", e)
            AnnotatedString(Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY).toString())
        }
    }

    /**
     * Convert AnnotatedString to HTML with basic formatting support
     */
    fun toHtml(annotatedString: AnnotatedString): String {
        if (annotatedString.text.isEmpty()) return ""

        return try {
            val text = annotatedString.text
            val result = StringBuilder()
            var currentIndex = 0

            // Simple approach: process character by character
            while (currentIndex < text.length) {
                val char = text[currentIndex]

                // Get styles at current position
                val styles = getStylesAtPosition(annotatedString, currentIndex)

                // Apply opening tags based on styles
                if (styles.bold) result.append("<b>")
                if (styles.italic) result.append("<i>")
                if (styles.underline) result.append("<u>")
                if (styles.strikethrough) result.append("<s>")
                if (styles.color != null) {
                    result.append("<font color=\"${styles.color}\">")
                }

                // Add the character
                when (char) {
                    '\n' -> result.append("<br>")
                    '<' -> result.append("&lt;")
                    '>' -> result.append("&gt;")
                    '&' -> result.append("&amp;")
                    else -> result.append(char)
                }

                // Apply closing tags (in reverse order)
                if (styles.color != null) result.append("</font>")
                if (styles.strikethrough) result.append("</s>")
                if (styles.underline) result.append("</u>")
                if (styles.italic) result.append("</i>")
                if (styles.bold) result.append("</b>")

                currentIndex++
            }

            result.toString()
        } catch (e: Exception) {
            Log.e(TAG, "Error converting AnnotatedString to HTML", e)
            annotatedString.text.replace("\n", "<br>")
        }
    }

    /**
     * Simple formatting application helper
     */
    private fun applySimpleFormatting(
        builder: AnnotatedString.Builder,
        html: String,
        plainText: String,
        openTag: String,
        closeTag: String,
        styleProvider: () -> SpanStyle
    ) {
        try {
            val pattern = "$openTag(.*?)$closeTag".toRegex(RegexOption.DOT_MATCHES_ALL)
            pattern.findAll(html).forEach { match ->
                val content = match.groupValues[1]
                val cleanContent = Html.fromHtml(content, Html.FROM_HTML_MODE_LEGACY).toString()

                val index = plainText.indexOf(cleanContent)
                if (index >= 0 && index + cleanContent.length <= plainText.length) {
                    builder.addStyle(styleProvider(), index, index + cleanContent.length)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error applying formatting for $openTag", e)
        }
    }

    /**
     * Apply color formatting - simplified to avoid BigInteger issues
     */
    private fun applyColorFormatting(
        builder: AnnotatedString.Builder,
        html: String,
        plainText: String
    ) {
        try {
            val colorPattern = "<font color=\"(#[0-9a-fA-F]{6})\">(.*?)</font>".toRegex(RegexOption.DOT_MATCHES_ALL)
            colorPattern.findAll(html).forEach { match ->
                val colorHex = match.groupValues[1]
                val content = match.groupValues[2]
                val cleanContent = Html.fromHtml(content, Html.FROM_HTML_MODE_LEGACY).toString()

                try {
                    val color = parseColorSafe(colorHex)
                    val index = plainText.indexOf(cleanContent)
                    if (index >= 0 && index + cleanContent.length <= plainText.length) {
                        builder.addStyle(SpanStyle(color = color), index, index + cleanContent.length)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Invalid color: $colorHex", e)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error applying color formatting", e)
        }
    }

    /**
     * Apply link formatting
     */
    private fun applyLinkFormatting(
        builder: AnnotatedString.Builder,
        html: String,
        plainText: String
    ) {
        try {
            val linkPattern = "<a href=\"(.*?)\">(.*?)</a>".toRegex(RegexOption.DOT_MATCHES_ALL)
            linkPattern.findAll(html).forEach { match ->
                val url = match.groupValues[1]
                val content = match.groupValues[2]
                val cleanContent = Html.fromHtml(content, Html.FROM_HTML_MODE_LEGACY).toString()

                val index = plainText.indexOf(cleanContent)
                if (index >= 0 && index + cleanContent.length <= plainText.length) {
                    builder.addStyle(
                        SpanStyle(color = Color.Blue, textDecoration = TextDecoration.Underline),
                        index, index + cleanContent.length
                    )
                    builder.addStringAnnotation("URL", url, index, index + cleanContent.length)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error applying link formatting", e)
        }
    }

    /**
     * Apply blockquote formatting
     */
    private fun applyBlockquoteFormatting(
        builder: AnnotatedString.Builder,
        html: String,
        plainText: String
    ) {
        try {
            val blockquotePattern = "<blockquote>(.*?)</blockquote>".toRegex(RegexOption.DOT_MATCHES_ALL)
            blockquotePattern.findAll(html).forEach { match ->
                val content = match.groupValues[1]
                val cleanContent = Html.fromHtml(content, Html.FROM_HTML_MODE_LEGACY).toString()

                val index = plainText.indexOf(cleanContent)
                if (index >= 0) {
                    var endIndex = index + cleanContent.length
                    if (endIndex < plainText.length && plainText[endIndex] == '\n') {
                        endIndex++
                    }
                    if (endIndex <= plainText.length) {
                        builder.addStyle(
                            ParagraphStyle(textIndent = TextIndent(16.sp, 16.sp)),
                            index, endIndex
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error applying blockquote formatting", e)
        }
    }

    /**
     * Get styles at a specific position - simplified
     */
    private fun getStylesAtPosition(annotatedString: AnnotatedString, position: Int): SimpleStyle {
        val styles = SimpleStyle()

        try {
            annotatedString.spanStyles.forEach { spanStyle ->
                if (position >= spanStyle.start && position < spanStyle.end) {
                    val style = spanStyle.item
                    if (style.fontWeight == FontWeight.Bold) styles.bold = true
                    if (style.fontStyle == FontStyle.Italic) styles.italic = true
                    if (style.textDecoration?.contains(TextDecoration.Underline) == true) styles.underline = true
                    if (style.textDecoration?.contains(TextDecoration.LineThrough) == true) styles.strikethrough = true
                    if (style.color != Color.Unspecified) {
                        styles.color = colorToHexSafe(style.color)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error getting styles at position", e)
        }

        return styles
    }

    /**
     * Safe color parsing that avoids BigInteger operations
     */
    private fun parseColorSafe(colorHex: String): Color {
        return try {
            // Use Android's color parsing
            val colorInt = android.graphics.Color.parseColor(colorHex)
            Color(colorInt)
        } catch (e: Exception) {
            Color.Black // Fallback
        }
    }

    /**
     * Safe color to hex conversion that avoids BigInteger operations
     */
    private fun colorToHexSafe(color: Color): String {
        return try {
            val red = (color.red * 255f).toInt().coerceIn(0, 255)
            val green = (color.green * 255f).toInt().coerceIn(0, 255)
            val blue = (color.blue * 255f).toInt().coerceIn(0, 255)

            // Build hex string manually to avoid any BigInteger operations
            val redHex = red.toString(16).padStart(2, '0')
            val greenHex = green.toString(16).padStart(2, '0')
            val blueHex = blue.toString(16).padStart(2, '0')

            "#$redHex$greenHex$blueHex"
        } catch (e: Exception) {
            "#000000" // Fallback
        }
    }

    /**
     * Simple style holder to avoid complex operations
     */
    private data class SimpleStyle(
        var bold: Boolean = false,
        var italic: Boolean = false,
        var underline: Boolean = false,
        var strikethrough: Boolean = false,
        var color: String? = null
    )

    /**
     * Extract plain text from HTML
     */
    fun htmlToPlainText(html: String): String {
        return try {
            Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY).toString()
        } catch (e: Exception) {
            html
        }
    }

    /**
     * Check if HTML contains any formatting
     */
    fun hasFormatting(html: String): Boolean {
        return try {
            val cleanHtml = html.replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "")
            cleanHtml.contains(Regex("<[^>]+>"))
        } catch (e: Exception) {
            false
        }
    }
}