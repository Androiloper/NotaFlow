package com.example.notaflow.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * Extracts headers from markdown content and displays them as a clickable table of contents
 */
@Composable
fun TableOfContents(
    markdownContent: String,
    onHeaderClick: (position: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val headers = remember(markdownContent) {
        extractHeaders(markdownContent)
    }

    Surface(
        modifier = modifier,
        tonalElevation = 1.dp,
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            Text(
                text = "Table of Contents",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (headers.isEmpty()) {
                Text(
                    text = "No headers found in document",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            } else {
                headers.forEach { (level, text, position) ->
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onHeaderClick(position) }
                            .padding(
                                start = (level * 12).dp,
                                top = 6.dp,
                                bottom = 6.dp
                            )
                    )
                }
            }
        }
    }
}

/**
 * Helper function to extract headers with their positions from markdown content
 */
private fun extractHeaders(markdown: String): List<Triple<Int, String, Int>> {
    val headers = mutableListOf<Triple<Int, String, Int>>()
    val lines = markdown.lines()
    var currentPosition = 0

    for (line in lines) {
        // Match header patterns (# Header, ## Header, ### Header)
        if (line.matches(Regex("^#{1,3}\\s+.*$"))) {
            val level = line.takeWhile { it == '#' }.length
            val text = line.substring(level).trimStart()
            headers.add(Triple(level, text, currentPosition))
        }
        // Add line length + 1 for newline character
        currentPosition += line.length + 1
    }

    return headers
}