package com.example.notaflow.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp

enum class RichTextFormatAction {
    BOLD, ITALIC, UNDERLINE, STRIKETHROUGH, LINK, COLOR, BLOCKQUOTE,
    UNDO, REDO, HEADER1, HEADER2, LIST_BULLET, LIST_NUMBERED
}

@Composable
fun RichTextFormatToolbar(
    modifier: Modifier = Modifier,
    currentStyles: Set<String>,
    onStyleClick: (RichTextFormatAction) -> Unit,
    onLinkClick: () -> Unit,
    onColorClick: (Color) -> Unit,
    selectedColor: Color,
    canUndo: Boolean,
    canRedo: Boolean,
    isVisible: Boolean = true
) {
    val hapticFeedback = LocalHapticFeedback.current
    var showColorPicker by remember { mutableStateOf(false) }

    if (!isVisible) return

    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column {
            // Main toolbar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Undo/Redo
                ToolbarButton(
                    icon = Icons.Filled.Undo,
                    contentDescription = "Undo",
                    onClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onStyleClick(RichTextFormatAction.UNDO)
                    },
                    isSelected = false,
                    enabled = canUndo
                )

                ToolbarButton(
                    icon = Icons.Filled.Redo,
                    contentDescription = "Redo",
                    onClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onStyleClick(RichTextFormatAction.REDO)
                    },
                    isSelected = false,
                    enabled = canRedo
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Basic formatting
                ToolbarButton(
                    icon = Icons.Filled.FormatBold,
                    contentDescription = "Bold",
                    onClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        onStyleClick(RichTextFormatAction.BOLD)
                    },
                    isSelected = currentStyles.contains("BOLD")
                )

                ToolbarButton(
                    icon = Icons.Filled.FormatItalic,
                    contentDescription = "Italic",
                    onClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        onStyleClick(RichTextFormatAction.ITALIC)
                    },
                    isSelected = currentStyles.contains("ITALIC")
                )

                ToolbarButton(
                    icon = Icons.Filled.FormatUnderlined,
                    contentDescription = "Underline",
                    onClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        onStyleClick(RichTextFormatAction.UNDERLINE)
                    },
                    isSelected = currentStyles.contains("UNDERLINE")
                )

                ToolbarButton(
                    icon = Icons.Filled.FormatStrikethrough,
                    contentDescription = "Strikethrough",
                    onClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        onStyleClick(RichTextFormatAction.STRIKETHROUGH)
                    },
                    isSelected = currentStyles.contains("STRIKETHROUGH")
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Color picker
                Box {
                    ToolbarButton(
                        icon = Icons.Filled.FormatColorText,
                        contentDescription = "Text Color",
                        onClick = {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            showColorPicker = !showColorPicker
                        },
                        isSelected = showColorPicker,
                        iconTint = if (selectedColor != Color.Unspecified) selectedColor else null
                    )

                    DropdownMenu(
                        expanded = showColorPicker,
                        onDismissRequest = { showColorPicker = false }
                    ) {
                        ColorPickerMenu(
                            selectedColor = selectedColor,
                            onColorSelected = { color ->
                                onColorClick(color)
                                showColorPicker = false
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                        )
                    }
                }

                // Link
                ToolbarButton(
                    icon = Icons.Filled.Link,
                    contentDescription = "Add Link",
                    onClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        onLinkClick()
                    },
                    isSelected = currentStyles.contains("LINK")
                )

                // Blockquote
                ToolbarButton(
                    icon = Icons.Filled.FormatQuote,
                    contentDescription = "Blockquote",
                    onClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        onStyleClick(RichTextFormatAction.BLOCKQUOTE)
                    },
                    isSelected = currentStyles.contains("BLOCKQUOTE")
                )
            }

            // Active styles indicator
            if (currentStyles.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Active:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    currentStyles.forEach { style ->
                        Surface(
                            modifier = Modifier.height(20.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = when (style) {
                                    "BOLD" -> "B"
                                    "ITALIC" -> "I"
                                    "UNDERLINE" -> "U"
                                    "STRIKETHROUGH" -> "S"
                                    "LINK" -> "🔗"
                                    "BLOCKQUOTE" -> "❝"
                                    else -> style.take(1)
                                },
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolbarButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    isSelected: Boolean,
    enabled: Boolean = true,
    iconTint: Color? = null
) {
    val buttonColor = if (isSelected && enabled) {
        MaterialTheme.colorScheme.primary
    } else if (enabled) {
        LocalContentColor.current
    } else {
        LocalContentColor.current.copy(alpha = 0.38f)
    }

    val actualTint = iconTint ?: buttonColor

    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isSelected && enabled) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                } else {
                    Color.Transparent
                }
            )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = actualTint,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun ColorPickerMenu(
    selectedColor: Color,
    onColorSelected: (Color) -> Unit
) {
    Column(
        modifier = Modifier
            .width(200.dp)
            .padding(16.dp)
    ) {
        Text(
            text = "Text Color",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Default color
        ColorButton(
            color = MaterialTheme.colorScheme.onSurface,
            isSelected = selectedColor == MaterialTheme.colorScheme.onSurface,
            onClick = { onColorSelected(MaterialTheme.colorScheme.onSurface) },
            label = "Default"
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Basic colors
        val basicColors = listOf(
            Color.Black to "Black",
            Color.Red to "Red",
            Color.Blue to "Blue",
            Color.Green to "Green",
            Color(0xFFFF6B35) to "Orange",
            Color(0xFF7209B7) to "Purple"
        )

        basicColors.forEach { (color, label) ->
            ColorButton(
                color = color,
                isSelected = selectedColor == color,
                onClick = { onColorSelected(color) },
                label = label
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Theme colors
        val themeColors = listOf(
            MaterialTheme.colorScheme.primary to "Primary",
            MaterialTheme.colorScheme.secondary to "Secondary",
            MaterialTheme.colorScheme.tertiary to "Tertiary",
            MaterialTheme.colorScheme.error to "Error"
        )

        themeColors.forEach { (color, label) ->
            ColorButton(
                color = color,
                isSelected = selectedColor == color,
                onClick = { onColorSelected(color) },
                label = label
            )
        }
    }
}

@Composable
private fun ColorButton(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    label: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(color)
        ) {
            if (isSelected) {
                val iconColor = if (isLightColor(color)) Color.Black else Color.White
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = iconColor,
                    modifier = Modifier.size(16.dp).align(Alignment.Center)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

private fun isLightColor(color: Color): Boolean {
    val luminance = 0.299f * color.red + 0.587f * color.green + 0.114f * color.blue
    return luminance > 0.5f
}