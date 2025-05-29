package com.example.notaflow.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.dp

enum class RichTextFormatAction {
    BOLD, ITALIC, UNDERLINE, STRIKETHROUGH, LINK, COLOR, BLOCKQUOTE,
    UNDO, REDO
}

@Composable
fun RichTextFormatToolbar(
    modifier: Modifier = Modifier,
    currentStyles: Set<String>,
    onStyleClick: (RichTextFormatAction) -> Unit,
    onLinkClick: () -> Unit,
    onColorClick: (Color) -> Unit,
    selectedColor: Color, // This is the color that typed text will have, or selected text's color
    canUndo: Boolean,
    canRedo: Boolean
) {
    var showColorPicker by remember { mutableStateOf(false) }

    val actions = listOf(
        RichTextFormatAction.UNDO, RichTextFormatAction.REDO,
        RichTextFormatAction.BOLD, RichTextFormatAction.ITALIC,
        RichTextFormatAction.UNDERLINE, RichTextFormatAction.STRIKETHROUGH,
        RichTextFormatAction.LINK, RichTextFormatAction.BLOCKQUOTE,
        RichTextFormatAction.COLOR
    )

    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f))
            .padding(vertical = 4.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(actions) { action ->
            when (action) {
                RichTextFormatAction.UNDO -> ToolbarIconButton(
                    icon = Icons.Filled.Undo,
                    contentDescription = "Undo",
                    onClick = { onStyleClick(RichTextFormatAction.UNDO) },
                    isSelected = false,
                    enabled = canUndo
                )
                RichTextFormatAction.REDO -> ToolbarIconButton(
                    icon = Icons.Filled.Redo,
                    contentDescription = "Redo",
                    onClick = { onStyleClick(RichTextFormatAction.REDO) },
                    isSelected = false,
                    enabled = canRedo
                )
                RichTextFormatAction.BOLD -> ToolbarIconButton(
                    icon = Icons.Filled.FormatBold,
                    contentDescription = "Bold",
                    onClick = { onStyleClick(RichTextFormatAction.BOLD) },
                    isSelected = currentStyles.contains("BOLD")
                )
                RichTextFormatAction.ITALIC -> ToolbarIconButton(
                    icon = Icons.Filled.FormatItalic,
                    contentDescription = "Italic",
                    onClick = { onStyleClick(RichTextFormatAction.ITALIC) },
                    isSelected = currentStyles.contains("ITALIC")
                )
                RichTextFormatAction.UNDERLINE -> ToolbarIconButton(
                    icon = Icons.Filled.FormatUnderlined,
                    contentDescription = "Underline",
                    onClick = { onStyleClick(RichTextFormatAction.UNDERLINE) },
                    isSelected = currentStyles.contains("UNDERLINE")
                )
                RichTextFormatAction.STRIKETHROUGH -> ToolbarIconButton(
                    icon = Icons.Filled.FormatStrikethrough,
                    contentDescription = "Strikethrough",
                    onClick = { onStyleClick(RichTextFormatAction.STRIKETHROUGH) },
                    isSelected = currentStyles.contains("STRIKETHROUGH")
                )
                RichTextFormatAction.LINK -> ToolbarIconButton(
                    icon = Icons.Filled.Link,
                    contentDescription = "Add Link",
                    onClick = { onLinkClick() }, // Special handler via onLinkClick
                    isSelected = currentStyles.contains("LINK")
                )
                RichTextFormatAction.BLOCKQUOTE -> ToolbarIconButton(
                    icon = Icons.Filled.FormatQuote,
                    contentDescription = "Blockquote",
                    onClick = { onStyleClick(RichTextFormatAction.BLOCKQUOTE) },
                    isSelected = currentStyles.contains("BLOCKQUOTE")
                )
                RichTextFormatAction.COLOR -> Box { // Color picker is a dropdown
                    ToolbarIconButton(
                        icon = Icons.Filled.FormatColorText,
                        contentDescription = "Text Color",
                        onClick = { showColorPicker = !showColorPicker },
                        isSelected = showColorPicker,
                        tint = if (selectedColor != Color.Unspecified && selectedColor != MaterialTheme.colorScheme.onSurface) selectedColor else LocalContentColor.current
                    )
                    DropdownMenu(
                        expanded = showColorPicker,
                        onDismissRequest = { showColorPicker = false }
                    ) {
                        val defaultColors = listOf(
                            MaterialTheme.colorScheme.onSurface, // Default text color
                            Color.Black, Color.DarkGray, Color.Gray, Color.LightGray,
                            Color.Red, Color.Green, Color.Blue, Color.Yellow, Color.Cyan, Color.Magenta,
                            Color(0xFFE57373), Color(0xFF81C784), Color(0xFF64B5F6), Color(0xFFFFD54F), Color(0xFFBA68C8) // Some Pastel colors
                        )
                        defaultColors.forEach { color ->
                            DropdownMenuItem(
                                text = { Box(modifier = Modifier.size(20.dp).background(color)) },
                                onClick = {
                                    onColorClick(color)
                                    showColorPicker = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolbarIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    isSelected: Boolean,
    enabled: Boolean = true,
    tint: Color = if (isSelected && enabled) MaterialTheme.colorScheme.primary
    else LocalContentColor.current.copy(alpha = if(enabled) ContentAlpha.high else ContentAlpha.disabled) // Use standard alphas
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected && enabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent),
        colors = IconButtonDefaults.iconButtonColors(
            contentColor = tint, // Use tint for the icon color directly
            disabledContentColor = LocalContentColor.current.copy(alpha = ContentAlpha.disabled)
        )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            // Tint is handled by IconButton colors now for better M3 behavior
            // tint = tint,
            modifier = Modifier.size(20.dp)
        )
    }
}

// ContentAlpha if not available in your M3 version, you can define it or use direct float values
object ContentAlpha {
    val high: Float @Composable get() = 1.0f // LocalContentAlpha.current - but can be 1.0f for enabled
    val medium: Float @Composable get() = 0.74f // MaterialTheme.colorScheme.onSurface.copy(alpha=0.74f)
    val disabled: Float @Composable get() = 0.38f // MaterialTheme.colorScheme.onSurface.copy(alpha=0.38f)
}
