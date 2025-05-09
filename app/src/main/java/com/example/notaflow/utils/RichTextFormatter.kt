package com.example.notaflow.utils

/**
 * Utility class for handling rich text format conversions
 */
object RichTextFormatter {

    /**
     * Enhances markdown text by standardizing formatting, fixing common issues,
     * and ensuring consistent structure
     */
    fun enhanceMarkdown(text: String): String {
        if (text.isBlank()) return ""

        // Split into lines for processing
        val lines = text.lines()
        val enhancedLines = mutableListOf<String>()
        var inCodeBlock = false
        var inListBlock = false
        var lastLineWasHeader = false

        for (i in lines.indices) {
            val line = lines[i].trimEnd() // Remove trailing whitespace

            // Handle code blocks
            if (line.startsWith("```")) {
                inCodeBlock = !inCodeBlock
                enhancedLines.add(line)
                continue
            }

            // Don't process markdown within code blocks
            if (inCodeBlock) {
                enhancedLines.add(line)
                continue
            }

            // Process regular markdown
            val enhancedLine = when {
                // Standardize heading format (ensure space after #)
                // Fixes the issue with headers not working by enforcing correct syntax
                line.matches(Regex("^#+[^\\s].*$")) -> {
                    val prefix = line.takeWhile { it == '#' }
                    val content = line.substring(prefix.length).trimStart()
                    "$prefix $content"
                }

                // Fix headers without proper spacing between # symbols and text
                line.matches(Regex("^#\\s+.*$")) -> line
                line.matches(Regex("^##\\s+.*$")) -> line
                line.matches(Regex("^###\\s+.*$")) -> line

                // Headers with wrong spacing
                line.matches(Regex("^#\\s{2,}.*$")) -> {
                    "# " + line.substring(line.indexOf(line.trim().first { it != '#' && it != ' ' }))
                }
                line.matches(Regex("^##\\s{2,}.*$")) -> {
                    "## " + line.substring(line.indexOf(line.trim().first { it != '#' && it != ' ' }))
                }
                line.matches(Regex("^###\\s{2,}.*$")) -> {
                    "### " + line.substring(line.indexOf(line.trim().first { it != '#' && it != ' ' }))
                }

                // Ensure bullet lists have consistent spacing
                line.matches(Regex("^\\s*[*+-]\\s.*$")) -> {
                    val listMarkerIndex = line.indexOfFirst { it in listOf('*', '+', '-') }
                    val prefix = line.substring(0, listMarkerIndex + 1)
                    val content = line.substring(listMarkerIndex + 1).trimStart()
                    "$prefix $content"
                }

                // Ensure numbered lists have consistent spacing
                line.matches(Regex("^\\s*\\d+\\.\\s.*$")) -> {
                    val dotIndex = line.indexOf('.')
                    val prefix = line.substring(0, dotIndex + 1)
                    val content = line.substring(dotIndex + 1).trimStart()
                    "$prefix $content"
                }

                // Ensure blockquotes have consistent spacing
                line.startsWith(">") -> {
                    if (line.startsWith("> ")) line else line.replaceFirst(">", "> ")
                }

                // Other content
                else -> line
            }

            // Add spacing between different blocks for better readability
            val currentLineIsList = enhancedLine.trim().matches(Regex("^[*+-]\\s.*$|^\\d+\\.\\s.*$"))
            val currentLineIsHeader = enhancedLine.trim().matches(Regex("^#+\\s.*$"))

            if (lastLineWasHeader && !enhancedLine.isBlank() && !currentLineIsHeader) {
                // No extra line needed after header
                enhancedLines.add(enhancedLine)
            } else if (inListBlock && !currentLineIsList && !enhancedLine.isBlank()) {
                // Add space after list ends
                if (enhancedLines.isNotEmpty() && enhancedLines.last().isNotBlank()) {
                    enhancedLines.add("")
                }
                enhancedLines.add(enhancedLine)
            } else {
                enhancedLines.add(enhancedLine)
            }

            inListBlock = currentLineIsList
            lastLineWasHeader = currentLineIsHeader
        }

        return enhancedLines.joinToString("\n")
    }

    /**
     * Strips markdown formatting to get plain text
     */
    fun stripMarkdown(markdown: String): String {
        if (markdown.isBlank()) return ""

        var result = markdown

        // Remove heading markers
        result = result.replace(Regex("^\\s*#+\\s*", RegexOption.MULTILINE), "")

        // Remove bold markers
        result = result.replace(Regex("\\*\\*(.*?)\\*\\*"), "$1")

        // Remove italic markers
        result = result.replace(Regex("_(.*?)_"), "$1")
        result = result.replace(Regex("\\*(.*?)\\*"), "$1")

        // Remove strike-through
        result = result.replace(Regex("~~(.*?)~~"), "$1")

        // Remove code markers
        result = result.replace(Regex("`(.*?)`"), "$1")

        // Remove list markers
        result = result.replace(Regex("^\\s*[*+-]\\s+", RegexOption.MULTILINE), "")
        result = result.replace(Regex("^\\s*\\d+\\.\\s+", RegexOption.MULTILINE), "")

        // Remove blockquotes
        result = result.replace(Regex("^\\s*>\\s*", RegexOption.MULTILINE), "")

        // Remove HTML tags (like underline)
        result = result.replace(Regex("<.*?>"), "")

        // Remove link syntax but keep text
        result = result.replace(Regex("\\[(.*?)\\]\\(.*?\\)"), "$1")

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
            Regex("\\*[^*]+\\*"),      // Italic with asterisks
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