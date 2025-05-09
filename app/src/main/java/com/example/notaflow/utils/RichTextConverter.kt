package com.example.notaflow.utils

/**
 * Utility class for handling rich text format conversions
 * This simplified version doesn't rely on external libraries
 */
class RichTextConverter {

    /**
     * Converts markdown text to HTML for display
     */
    fun markdownToHtml(markdown: String): String {
        if (markdown.isBlank()) return ""

        // Basic markdown-to-HTML conversion
        // For a production app, you would use a proper library for this
        var html = markdown

        // Headers
        html = html.replace(Regex("^# (.*)$", RegexOption.MULTILINE), "<h1>$1</h1>")
        html = html.replace(Regex("^## (.*)$", RegexOption.MULTILINE), "<h2>$1</h2>")
        html = html.replace(Regex("^### (.*)$", RegexOption.MULTILINE), "<h3>$1</h3>")

        // Bold
        html = html.replace(Regex("\\*\\*(.*?)\\*\\*"), "<strong>$1</strong>")

        // Italic
        html = html.replace(Regex("_(.*?)_"), "<em>$1</em>")

        // Strikethrough
        html = html.replace(Regex("~~(.*?)~~"), "<del>$1</del>")

        // Code
        html = html.replace(Regex("`(.*?)`"), "<code>$1</code>")

        // Lists
        html = html.replace(Regex("^\\* (.*)$", RegexOption.MULTILINE), "<li>$1</li>")
        html = html.replace(Regex("^\\d+\\. (.*)$", RegexOption.MULTILINE), "<li>$1</li>")

        // Blockquotes
        html = html.replace(Regex("^> (.*)$", RegexOption.MULTILINE), "<blockquote>$1</blockquote>")

        return html
    }

    /**
     * Simple check to determine if text has markdown formatting
     */
    fun hasMarkdownFormatting(text: String): Boolean {
        if (text.isBlank()) return false

        // Check for common markdown patterns
        val markdownPatterns = listOf(
            Regex("^#+\\s.*$", RegexOption.MULTILINE),    // Headers
            Regex("\\*\\*(.*?)\\*\\*"),                   // Bold
            Regex("_(.*?)_"),                             // Italic
            Regex("~~(.*?)~~"),                          // Strikethrough
            Regex("`(.*?)`"),                            // Code
            Regex("^\\*\\s.*$", RegexOption.MULTILINE),   // Bullet lists
            Regex("^\\d+\\.\\s.*$", RegexOption.MULTILINE), // Numbered lists
            Regex("^>\\s.*$", RegexOption.MULTILINE)      // Blockquotes
        )

        return markdownPatterns.any { it.containsMatchIn(text) }
    }
}