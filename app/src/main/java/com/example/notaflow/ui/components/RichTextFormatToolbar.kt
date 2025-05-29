package com.example.notaflow.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.* // Ensure this import is present and correct
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.ContentAlpha

// Removed: import com.example.notaflow.ui.theme.NotaFlowTheme // Access theme via MaterialTheme

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
    selectedColor: Color,
    canUndo: Boolean,
    canRedo: Boolean
) {
    var showColorPicker by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f)) // Corrected
            .padding(vertical = 4.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        ToolbarIconButton(
            icon = Icons.Filled.Undo, // Standard Material Icon
            contentDescription = "Undo",
            onClick = { onStyleClick(RichTextFormatAction.UNDO) },
            isSelected = false,
            enabled = canUndo
        )
        ToolbarIconButton(
            icon = Icons.Filled.Redo, // Standard Material Icon
            contentDescription = "Redo",
            onClick = { onStyleClick(RichTextFormatAction.REDO) },
            isSelected = false,
            enabled = canRedo
        )
        ToolbarIconButton(
            icon = Icons.Filled.FormatBold, // Standard Material Icon
            contentDescription = "Bold",
            onClick = { onStyleClick(RichTextFormatAction.BOLD) },
            isSelected = currentStyles.contains("BOLD")
        )
        ToolbarIconButton(
            icon = Icons.Filled.FormatItalic, // Standard Material Icon
            contentDescription = "Italic",
            onClick = { onStyleClick(RichTextFormatAction.ITALIC) },
            isSelected = currentStyles.contains("ITALIC")
        )
        ToolbarIconButton(
            icon = Icons.Filled.FormatUnderlined, // Standard Material Icon
            contentDescription = "Underline",
            onClick = { onStyleClick(RichTextFormatAction.UNDERLINE) },
            isSelected = currentStyles.contains("UNDERLINE")
        )
        ToolbarIconButton(
            icon = Icons.Filled.FormatStrikethrough, // Standard Material Icon
            contentDescription = "Strikethrough",
            onClick = { onStyleClick(RichTextFormatAction.STRIKETHROUGH) },
            isSelected = currentStyles.contains("STRIKETHROUGH")
        )
        ToolbarIconButton(
            icon = Icons.Filled.Link, // Standard Material Icon
            contentDescription = "Add Link",
            onClick = { onLinkClick() },
            isSelected = currentStyles.contains("LINK")
        )
        ToolbarIconButton(
            icon = Icons.Filled.FormatQuote, // Standard Material Icon
            contentDescription = "Blockquote",
            onClick = { onStyleClick(RichTextFormatAction.BLOCKQUOTE) },
            isSelected = currentStyles.contains("BLOCKQUOTE")
        )

        Box {
            ToolbarIconButton(
                icon = Icons.Filled.FormatColorText, // Standard Material Icon
                contentDescription = "Text Color",
                onClick = { showColorPicker = !showColorPicker },
                isSelected = showColorPicker,
                tint = if (selectedColor != Color.Unspecified && selectedColor != MaterialTheme.colorScheme.onSurface) selectedColor else LocalContentColor.current // Corrected
            )
            DropdownMenu(
                expanded = showColorPicker,
                onDismissRequest = { showColorPicker = false }
            ) {
                val colors = listOf(
                    MaterialTheme.colorScheme.onSurface, // Corrected
                    Color.Red, Color.Blue, Color.Green, Color.Black, Color.DarkGray, Color.Magenta, Color.Yellow, Color.Cyan
                )
                colors.forEach { color ->
                    DropdownMenuItem(
                        text = {
                            Box(modifier = Modifier.size(20.dp).background(color))
                        },
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

@Composable
private fun ToolbarIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    isSelected: Boolean,
    enabled: Boolean = true,
    // Corrected: Use MaterialTheme for primary color if selected
    tint: Color = if (isSelected && enabled) MaterialTheme.colorScheme.primary
    else LocalContentColor.current.copy(alpha = if (enabled) ContentAlpha.high else ContentAlpha.disabled) // Using ContentAlpha for standard disabled/enabled states
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            // Corrected: Use MaterialTheme for background if selected
            .background(if (isSelected && enabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
    }
}
