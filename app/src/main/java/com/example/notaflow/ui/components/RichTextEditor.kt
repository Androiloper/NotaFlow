package com.example.notaflow.ui.components

import androidx.compose.foundation.layout.Column
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
    titleValue: TextFieldValue, // Assuming title is part of this editor
    contentValue: TextFieldValue,
    onTitleChange: (TextFieldValue) -> Unit, // Assuming title is part of this editor
    onContentChange: (TextFieldValue) -> Unit,
    onStyleClick: (RichTextFormatAction) -> Unit,
    onLinkClick: () -> Unit,
    onColorSelected: (Color) -> Unit,
    currentSelectedTextColor: Color,
    currentFormatStyles: Set<String>,
    canUndo: Boolean,
    canRedo: Boolean,
    noteColor: Color, // Background for the note area, or use MaterialTheme.colorScheme.surface
    focusRequester: FocusRequester = remember { FocusRequester() }
) {
    val titleScrollState = rememberScrollState() // Separate scroll for title if it can be long
    val contentScrollState = rememberScrollState() // Separate scroll for content

    Column(modifier = modifier) { // Removed .verticalScroll from Column, let TextFields handle scroll
        // Title TextField (if title is edited here)
        TextField(
            value = titleValue,
            onValueChange = onTitleChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
            // .verticalScroll(titleScrollState) // If title can be multiline and very long
            ,
            placeholder = { Text("Title", style = MaterialTheme.typography.headlineSmall) },
            textStyle = MaterialTheme.typography.headlineSmall.copy(
                color = MaterialTheme.colorScheme.onSurface
            ),
            colors = TextFieldDefaults.colors( // Use new colors for M3
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
            singleLine = true // Assuming title is single line
        )

        // Rich Text Content TextField
        TextField(
            value = contentValue,
            onValueChange = onContentChange,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f) // Takes available space
                .padding(horizontal = 16.dp)
                .focusRequester(focusRequester)
                .verticalScroll(contentScrollState), // Allow content TextField to scroll
            placeholder = { Text("Note content...") },
            textStyle = TextStyle(
                fontSize = 18.sp,
                color = currentSelectedTextColor.takeIf { it != Color.Unspecified }
                    ?: MaterialTheme.colorScheme.onSurface
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
                capitalization = KeyboardCapitalization.Sentences
            )
            // imeAction = ImeAction.Default // Default for multiline
        )

        RichTextFormatToolbar(
            modifier = Modifier.padding(bottom = 8.dp, start = 8.dp, end = 8.dp),
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
