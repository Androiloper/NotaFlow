package com.example.notaflow.utils

/**
 * Utility class for handling rich text format conversions
 */
object RichTextFormatter {

    /**
     * Converts plain text with markdown syntax to a fully-formed markdown document
     */
    fun enhanceMarkdown(text: String): String {
        if (text.isBlank()) return ""

        // This would be where you'd enhance the markdown for better rendering
        // For simplicity's sake, we're just returning the original text
        // In a production app, you might want to clean up the markdown, ensure proper
        // whitespace, etc.
        return text
    }

    /**
     * Strips markdown formatting to get plain text
     */
    fun stripMarkdown(markdown: String): String {
        if (markdown.isBlank()) return ""

        var result = markdown

        // Remove heading markers
        result = result.replace(Regex("^\\s*#+\\s*"), "")

        // Remove bold markers
        result = result.replace(Regex("\\*\\*(.*?)\\*\\*"), "$1")

        // Remove italic markers
        result = result.replace(Regex("_(.*?)_"), "$1")

        // Remove strike-through
        result = result.replace(Regex("~~(.*?)~~"), "$1")

        // Remove code markers
        result = result.replace(Regex("`(.*?)`"), "$1")

        // Remove list markers
        result = result.replace(Regex("^\\s*[*+-]\\s+"), "")
        result = result.replace(Regex("^\\s*\\d+\\.\\s+"), "")

        // Remove blockquotes
        result = result.replace(Regex("^\\s*>\\s*"), "")

        // Remove HTML tags (like underline)
        result = result.replace(Regex("<.*?>"), "")

        return result
    }

    /**
     * Determines if a text contains markdown formatting
     */
    fun containsMarkdown(text: String): Boolean {
        if (text.isBlank()) return false

        // Check for markdown formatting patterns
        val markdownPatterns = listOf(
            Regex("\\*\\*.*?\\*\\*"),  // Bold
            Regex("_.*?_"),            // Italic
            Regex("~~.*?~~"),          // Strikethrough
            Regex("`.*?`"),            // Code
            Regex("^\\s*#+\\s+.*$", RegexOption.MULTILINE),  // Headings
            Regex("^\\s*[*+-]\\s+.*$", RegexOption.MULTILINE),  // Bullet lists
            Regex("^\\s*\\d+\\.\\s+.*$", RegexOption.MULTILINE),  // Numbered lists
            Regex("^\\s*>\\s+.*$", RegexOption.MULTILINE),  // Blockquotes
            Regex("\\[.*?\\]\\(.*?\\)")  // Links
        )

        for (pattern in markdownPatterns) {
            if (pattern.containsMatchIn(text)) {
                return true
            }
        }

        return false
    }
}