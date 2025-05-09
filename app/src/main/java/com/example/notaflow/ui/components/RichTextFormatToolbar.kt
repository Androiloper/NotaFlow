package com.example.notaflow.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class TextStyle {
    HEADING1, HEADING2, HEADING3, BOLD, ITALIC, UNDERLINE, STRIKETHROUGH,
    BULLET_LIST, NUMBERED_LIST, QUOTE, CODE, LINK, NORMAL
}

/**
 * A simplified toolbar with formatting options for rich text editing
 */
@Composable
fun RichTextFormatToolbar(
    onStyleSelected: (TextStyle) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val scrollState = rememberScrollState()

    Surface(
        tonalElevation = 3.dp,
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .horizontalScroll(scrollState)
                .padding(4.dp)
        ) {
            // Headings
            SimpleFormatButton(
                text = "H1",
                tooltip = "Heading 1",
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onStyleSelected(TextStyle.HEADING1)
                }
            )

            SimpleFormatButton(
                text = "H2",
                tooltip = "Heading 2",
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onStyleSelected(TextStyle.HEADING2)
                }
            )

            SimpleFormatButton(
                text = "H3",
                tooltip = "Heading 3",
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onStyleSelected(TextStyle.HEADING3)
                }
            )

            VerticalDivider()

            // Text styling
            SimpleFormatButton(
                text = "B",
                isBold = true,
                tooltip = "Bold",
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onStyleSelected(TextStyle.BOLD)
                }
            )

            SimpleFormatButton(
                text = "I",
                isItalic = true,
                tooltip = "Italic",
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onStyleSelected(TextStyle.ITALIC)
                }
            )

            SimpleFormatButton(
                text = "U",
                isUnderlined = true,
                tooltip = "Underline",
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onStyleSelected(TextStyle.UNDERLINE)
                }
            )

            SimpleFormatButton(
                text = "S",
                isStrikethrough = true,
                tooltip = "Strikethrough",
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onStyleSelected(TextStyle.STRIKETHROUGH)
                }
            )

            VerticalDivider()

            // Lists
            SimpleFormatButton(
                text = "•",
                tooltip = "Bullet List",
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onStyleSelected(TextStyle.BULLET_LIST)
                }
            )

            SimpleFormatButton(
                text = "1.",
                tooltip = "Numbered List",
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onStyleSelected(TextStyle.NUMBERED_LIST)
                }
            )

            VerticalDivider()

            // Other formats
            SimpleFormatButton(
                text = "\"",
                tooltip = "Quote",
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onStyleSelected(TextStyle.QUOTE)
                }
            )

            SimpleFormatButton(
                text = "</>",
                tooltip = "Code",
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onStyleSelected(TextStyle.CODE)
                }
            )

            SimpleFormatButton(
                text = "🔗",
                tooltip = "Link",
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onStyleSelected(TextStyle.LINK)
                }
            )

            VerticalDivider()

            // Reset formatting
            SimpleFormatButton(
                text = "T",
                tooltip = "Normal Text",
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onStyleSelected(TextStyle.NORMAL)
                }
            )
        }
    }
}

@Composable
private fun SimpleFormatButton(
    text: String,
    tooltip: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isBold: Boolean = false,
    isItalic: Boolean = false,
    isUnderlined: Boolean = false,
    isStrikethrough: Boolean = false
) {
    var isHovered by remember { mutableStateOf(false) }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
    ) {
        // The button
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(
                    if (isHovered) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    else MaterialTheme.colorScheme.surface
                )
                .clickable {
                    onClick()
                    isHovered = false
                }
                .padding(4.dp)
        ) {
            Text(
                text = text,
                fontSize = 16.sp,
                fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // Simple hover tooltip
        if (isHovered) {
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(top = 38.dp)
            ) {
                Text(
                    text = tooltip,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun VerticalDivider() {
    Spacer(modifier = Modifier.width(2.dp))
    Divider(
        modifier = Modifier
            .padding(vertical = 4.dp)
            .size(width = 1.dp, height = 24.dp)
    )
    Spacer(modifier = Modifier.width(2.dp))
}