package com.example.notaflow.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Enhanced rich text display component for showing formatted text
 * that matches rendering in the editor
 */
@Composable
fun EnhancedRichTextDisplay(
    markdownContent: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    // Add this parameter
    highlightedPosition: Int? = null
) {
    val scrollState = rememberScrollState()

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            if (markdownContent.isBlank()) {
                Text(
                    text = "No content",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                )
            } else {
                // Display headers with potential highlighting
                var currentPosition = 0
                val lines = markdownContent.lines()

                for (line in lines) {
                    val isHighlighted = highlightedPosition != null &&
                            currentPosition <= highlightedPosition &&
                            (currentPosition + line.length) >= highlightedPosition

                    // Add special highlight for headers
                    if (isHighlighted && line.matches(Regex("^#{1,3}\\s+.*$"))) {
                        // Header level (1-3)
                        val level = line.takeWhile { it == '#' }.length
                        val headerText = line.substring(level).trimStart()

                        // Display highlighted header
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(vertical = 8.dp, horizontal = 12.dp)
                        ) {
                            Text(
                                text = headerText,
                                style = when (level) {
                                    1 -> MaterialTheme.typography.headlineLarge
                                    2 -> MaterialTheme.typography.headlineMedium
                                    else -> MaterialTheme.typography.headlineSmall
                                },
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    currentPosition += line.length + 1 // +1 for newline
                }

                // Use existing RichTextDisplay
                RichTextDisplay(
                    markdownContent = markdownContent,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}