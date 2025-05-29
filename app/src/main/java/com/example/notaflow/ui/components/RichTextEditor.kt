package com.example.notaflow.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
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
// Removed: com.example.notaflow.ui.theme.NotaFlowTheme // Direct access not needed here
// It's assumed NotaFlowTheme is applied at a higher level in the composition tree

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
    noteColor: Color, // Background color for the note editor area
    focusRequester: FocusRequester = remember { FocusRequester() }
) {
    val scrollState = rememberScrollState()

    Column(modifier = modifier.verticalScroll(scrollState)) {
        // Title TextField
        TextField(
            value = titleValue,
            onValueChange = onTitleChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = { Text("Title", style = MaterialTheme.typography.headlineSmall) },
            textStyle = MaterialTheme.typography.headlineSmall.copy(
                color = MaterialTheme.colorScheme.onSurface // Corrected: Use MaterialTheme
            ),
            colors = TextFieldDefaults.textFieldColors(
                containerColor = Color.Transparent,
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

        // Rich Text Content TextField
        TextField(
            value = contentValue,
            onValueChange = onContentChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .weight(1f) // Takes available space
                .focusRequester(focusRequester),
            placeholder = { Text("Note content...") },
            textStyle = TextStyle(
                fontSize = 18.sp, // Default text size for content
                color = currentSelectedTextColor.takeIf { it != Color.Unspecified }
                    ?: MaterialTheme.colorScheme.onSurface // Corrected: Use MaterialTheme
            ),
            colors = TextFieldDefaults.textFieldColors(
                containerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent
            ),
            keyboardOptions = KeyboardOptions.Default.copy(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Default // No specific action, allows multiline
            )
        )

        // Formatting Toolbar
        RichTextFormatToolbar(
            modifier = Modifier.padding(bottom = 8.dp),
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
