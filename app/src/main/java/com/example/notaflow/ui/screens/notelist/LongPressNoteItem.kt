// ui/screens/notelist/LongPressNoteItem.kt
package com.example.notaflow.ui.screens.notelist

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.notaflow.data.local.entity.Note
import com.example.notaflow.ui.components.RichTextDisplay
import com.example.notaflow.utils.HapticFeedback
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.ExperimentalFoundationApi

@Composable
fun LongPressNoteItem(
    note: Note,
    onNoteClick: () -> Unit,
    onDeleteConfirm: () -> Unit,
    modifier: Modifier = Modifier
) {
    // State to track if item is being deleted with animation
    var isDeleting by remember { mutableStateOf(false) }

    // State for context menu
    var showContextMenu by remember { mutableStateOf(false) }

    // For haptic feedback
    val hapticFeedback = LocalHapticFeedback.current
    val context = LocalContext.current

    // Handle actual deletion after animation completes
    LaunchedEffect(isDeleting) {
        if (isDeleting) {
            delay(300) // Animation duration
            onDeleteConfirm()
        }
    }

    // Wrap in AnimatedVisibility for disappearing animation
    AnimatedVisibility(
        visible = !isDeleting,
        exit = shrinkVertically(
            animationSpec = tween(durationMillis = 300)
        ) + fadeOut(animationSpec = tween(durationMillis = 300))
    ) {
        Box(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
            // The note card
            NoteCard(
                note = note,
                onTap = onNoteClick,
                onLongPress = {
                    HapticFeedback.performLongPressHaptic(hapticFeedback)
                    showContextMenu = true
                },
                modifier = modifier
            )

            // Context menu with improved styling
            DropdownMenu(
                expanded = showContextMenu,
                onDismissRequest = { showContextMenu = false },
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                // Edit option
                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Edit",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    },
                    onClick = {
                        showContextMenu = false
                        onNoteClick()
                    }
                )

                // Delete option
                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Delete",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    onClick = {
                        showContextMenu = false
                        HapticFeedback.deleteVibration(context) // Specialized delete feedback
                        isDeleting = true // Start delete animation
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NoteCard(
    note: Note,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    val noteColor = Color(android.graphics.Color.parseColor(note.colorHex))

    Card(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onTap,
                onLongClick = onLongPress
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            // Color indicator
            Box(
                modifier = Modifier
                    .width(12.dp)
                    .height(120.dp)
                    .background(noteColor)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Title
                Text(
                    text = note.title.ifEmpty { "Untitled" },
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Rich text indicator if applicable
                if (note.isRichText) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 4.dp)
                    ) {
                        // Custom rich text indicator that doesn't rely on Material icons
                        RichTextIndicator()
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Rich Text",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Content - show either plain text or rich text preview
                if (note.isRichText && note.richTextContent.isNotEmpty()) {
                    // Improved rich text preview
                    val previewContent = generateSmartPreview(note.richTextContent)

                    // Display simplified version of rich text
                    RichTextDisplay(
                        markdownContent = previewContent,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                    )
                } else {
                    // Plain text content
                    Text(
                        text = note.content.ifEmpty { "No content" },
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Last modified date
                Text(
                    text = "Last modified: ${formatDate(note.modifiedAt)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

/**
 * Generates a smart preview of markdown content that preserves markdown structure
 * and avoids breaking in the middle of elements
 */
private fun generateSmartPreview(markdown: String): String {
    if (markdown.length <= 150) return markdown

    val lines = markdown.lines()
    val previewLines = mutableListOf<String>()
    var charCount = 0
    var foundHeader = false
    var inCodeBlock = false

    // First pass: Try to include the first header as a summary
    for (line in lines) {
        if (line.matches(Regex("^#+ .*$"))) {
            previewLines.add(line)
            charCount += line.length + 1  // +1 for newline
            foundHeader = true
            break
        }
    }

    // Second pass: Add content up to reasonable size
    for (line in lines) {
        // Skip header if we already included it
        if (foundHeader && line.matches(Regex("^#+ .*$")) && previewLines.contains(line)) {
            continue
        }

        // Handle code blocks - don't break in the middle
        if (line.startsWith("```")) {
            if (!inCodeBlock && charCount < 100) {
                // Only start a code block if we have space
                inCodeBlock = true
                previewLines.add(line)
                charCount += line.length + 1
            } else if (inCodeBlock) {
                // Always close a code block if we started one
                inCodeBlock = false
                previewLines.add(line)
                charCount += line.length + 1
                if (charCount > 120) break  // Stop after closing code block if preview is long enough
            }
            continue
        }

        // If in code block, always include the line
        if (inCodeBlock) {
            previewLines.add(line)
            charCount += line.length + 1
            continue
        }

        // Add the line if we have space
        if (charCount + line.length <= 150) {
            previewLines.add(line)
            charCount += line.length + 1
        } else {
            // For the last line, try to find a good breaking point
            val remainingChars = 150 - charCount
            if (remainingChars > 10) {  // Only add partial if we can show something meaningful
                // Find a good breaking point - prefer breaking at punctuation or space
                var breakPoint = remainingChars
                while (breakPoint > 0) {
                    if (breakPoint < line.length && line[breakPoint] in listOf(' ', '.', ',', '!', '?', ';', ':')) {
                        break
                    }
                    breakPoint--
                }

                if (breakPoint > 0) {
                    previewLines.add(line.substring(0, breakPoint) + "...")
                }
            }
            break
        }
    }

    // Always close code blocks in preview
    if (inCodeBlock) {
        previewLines.add("```")
    }

    // Add ellipsis if we truncated and didn't already add it
    if (charCount > 150 && !previewLines.last().endsWith("...")) {
        previewLines.add("...")
    }

    return previewLines.joinToString("\n")
}

@Composable
private fun RichTextIndicator(modifier: Modifier = Modifier) {
    // A simple "T" in a circle to indicate rich text - no icon dependency
    Box(
        modifier = modifier
            .size(16.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
            .border(1.dp, MaterialTheme.colorScheme.primary, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "T",
            color = MaterialTheme.colorScheme.primary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun formatDate(date: Date): String {
    val formatter = SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault())
    return formatter.format(date)
}