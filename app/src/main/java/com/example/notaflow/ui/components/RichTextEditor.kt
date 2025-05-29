package com.example.notaflow.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RichTextEditor(
    modifier: Modifier = Modifier,
    titleValue: TextFieldValue,
    contentValue: TextFieldValue,
    onTitleChange: (TextFieldValue) -> Unit,
    onContentChange: (TextFieldValue) -> Unit,
    onStyleClick: (RichTextFormatAction) -> Unit,
    onLinkClick: () -> Unit,
    onColorSelected: (Color) -> Unit,
    currentSelectedTextColor: Color,
    currentFormatStyles: Set<String>,
    canUndo: Boolean,
    canRedo: Boolean,
    noteColor: Color,
    focusRequester: FocusRequester = remember { FocusRequester() }
) {
    val contentScrollState = rememberScrollState()
    var isContentFocused by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        // Title TextField
        TextField(
            value = titleValue,
            onValueChange = onTitleChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = {
                Text(
                    "Title",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                )
            },
            textStyle = MaterialTheme.typography.headlineSmall.copy(
                color = MaterialTheme.colorScheme.onSurface
            ),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent
            ),
            keyboardOptions = KeyboardOptions.Default.copy(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Next
            ),
            singleLine = true
        )

        // Rich Text Content TextField with improved handling
        TextField(
            value = contentValue,
            onValueChange = { newValue ->
                // Ensure we maintain the rich text formatting during changes
                onContentChange(newValue)
            },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp)
                .focusRequester(focusRequester)
                .onFocusChanged { focusState ->
                    isContentFocused = focusState.isFocused
                }
                .verticalScroll(contentScrollState),
            placeholder = {
                Text(
                    "Start typing your note...",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                )
            },
            textStyle = TextStyle(
                fontSize = 18.sp,
                color = if (currentSelectedTextColor != Color.Unspecified) {
                    currentSelectedTextColor
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                lineHeight = 24.sp
            ),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                cursorColor = MaterialTheme.colorScheme.primary
            ),
            keyboardOptions = KeyboardOptions.Default.copy(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Default
            ),
            // Enable multiline
            maxLines = Int.MAX_VALUE,
            minLines = 3
        )

        // Format Toolbar - always show when content is focused or has focus
        if (isContentFocused || contentValue.text.isNotEmpty()) {
            RichTextFormatToolbar(
                modifier = Modifier
                    .padding(bottom = 8.dp, start = 8.dp, end = 8.dp, top = 8.dp)
                    .fillMaxWidth(),
                currentStyles = currentFormatStyles,
                onStyleClick = onStyleClick,
                onLinkClick = onLinkClick,
                onColorClick = onColorSelected,
                selectedColor = currentSelectedTextColor,
                canUndo = canUndo,
                canRedo = canRedo
            )
        }
    }
}

/**
 * Enhanced Rich Text Display component for read-only viewing
 */
@Composable
fun RichTextViewer(
    content: TextFieldValue,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.surface
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            if (content.text.isBlank()) {
                Text(
                    text = "No content",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                )
            } else {
                // Display the rich text with proper formatting
                Text(
                    text = content.annotatedString,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        lineHeight = 24.sp
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * Preview mode component that shows formatted text
 */
@Composable
fun RichTextPreview(
    content: TextFieldValue,
    modifier: Modifier = Modifier,
    onEditClick: () -> Unit = {}
) {
    Card(
        modifier = modifier
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Preview header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Text(
                    text = "Preview",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )

                TextButton(onClick = onEditClick) {
                    Text("Edit")
                }
            }

            Divider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            )

            // Preview content
            if (content.text.isBlank()) {
                Text(
                    text = "No content to preview",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                )
            } else {
                Text(
                    text = content.annotatedString,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        lineHeight = 24.sp
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                )
            }
        }
    }
}

/**
 * Utility composable for handling text selection changes
 */
@Composable
fun rememberRichTextState(
    initialValue: TextFieldValue = TextFieldValue("")
): MutableState<TextFieldValue> {
    return remember { mutableStateOf(initialValue) }
}

/**
 * Extension function to check if text field has rich formatting
 */
fun TextFieldValue.hasRichFormatting(): Boolean {
    return annotatedString.spanStyles.isNotEmpty() ||
            annotatedString.paragraphStyles.isNotEmpty() ||
            annotatedString.getStringAnnotations(0, text.length).isNotEmpty()
}

/**
 * Extension function to get plain text from rich text
 */
fun TextFieldValue.toPlainText(): String {
    return annotatedString.text
}

/**
 * Helper function to create a TextFieldValue with rich formatting preserved
 */
fun createRichTextFieldValue(
    text: String,
    selection: androidx.compose.ui.text.TextRange = androidx.compose.ui.text.TextRange.Zero,
    annotatedString: androidx.compose.ui.text.AnnotatedString? = null
): TextFieldValue {
    return if (annotatedString != null) {
        TextFieldValue(
            annotatedString = annotatedString,
            selection = selection
        )
    } else {
        TextFieldValue(
            text = text,
            selection = selection
        )
    }
}