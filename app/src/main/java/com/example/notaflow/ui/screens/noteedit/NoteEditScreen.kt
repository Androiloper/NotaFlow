package com.example.notaflow.ui.screens.noteedit

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TextFieldDefaults // Explicit import for TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.notaflow.ui.components.ColorSelector
import com.example.notaflow.ui.components.RichTextEditor
// import com.example.notaflow.utils.RichTextFormatter // Not used in this snippet, can be removed if not used elsewhere

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditScreen(
    noteId: Long?, // noteId is passed but not directly used in this Composable's logic, assuming ViewModel handles it
    onNavigateBack: () -> Unit,
    viewModel: NoteEditViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // State for toggling rich text editing, initialized from ViewModel state
    var isRichTextEnabled by remember(state.isRichText) { mutableStateOf(state.isRichText) }

    /*
    // Effect for navigation and error handling
    LaunchedEffect(state.saveCompleted, state.error) {
        if (state.saveCompleted) {
            onNavigateBack()
            viewModel.resetSaveCompleted() // Reset the flag after navigation
        }

        state.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.dismissError()
        }
    }

     */

    // Effect for navigation and error handling
    LaunchedEffect(state.saveCompleted, state.error) {
        if (state.saveCompleted) {
            onNavigateBack()
        }

        state.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.dismissError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isNewNote) "New Note" else "Edit Note") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(
                            text = "Rich Text",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                        Switch(
                            checked = isRichTextEnabled,
                            onCheckedChange = { enabled ->
                                isRichTextEnabled = enabled
                                viewModel.setRichTextEnabled(enabled)
                                // When switching from plain to rich, ensure richTextContent might need initialization
                                // or from rich to plain, content should reflect the (potentially markdown) text.
                                // This is handled by how initialContent and onContentChange are set up.
                            }
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    viewModel.saveNote() // saveNote should ideally return a signal for completion if needed here,
                    // but navigation is handled by LaunchedEffect on state.saveCompleted
                },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Save",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        val noteColor = try {
            Color(android.graphics.Color.parseColor(state.colorHex))
        } catch (e: IllegalArgumentException) {
            MaterialTheme.colorScheme.surfaceVariant // Fallback color
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .width(6.dp)
                            .height(56.dp) // Ensure height matches TextField
                            .background(noteColor)
                    )
                    OutlinedTextField(
                        value = state.title,
                        onValueChange = viewModel::onTitleChange,
                        label = { Text("Title") },
                        modifier = Modifier
                            .weight(1f) // Allow TextField to take remaining space
                            .padding(start = 8.dp, end = 8.dp), // Padding around text field
                        singleLine = true,
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            cursorColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(8.dp)) // Ensure content respects rounded corners
                ) {
                    if (isRichTextEnabled) {
                        RichTextEditor(
                            initialContent = if (state.richTextContent.isNotEmpty()) {
                                state.richTextContent
                            } else {
                                // If switching from plain to rich, state.content might be plain.
                                // RichTextEditor will treat it as initial markdown.
                                state.content
                            },
                            // The RichTextEditor now only provides the markdown text.
                            onMarkdownChanged = { markdownText ->
                                // When rich text is enabled, update both general content and specific rich text content
                                // to be the markdown.
                                viewModel.onContentChange(markdownText)
                                viewModel.onRichTextContentChange(markdownText)
                            },
                            modifier = Modifier.fillMaxSize()
                            // placeholderText can be added if desired
                        )
                    } else {
                        OutlinedTextField(
                            value = state.content, // When rich text is disabled, this shows plain text (or raw markdown if switched from rich)
                            onValueChange = { newContent ->
                                viewModel.onContentChange(newContent)
                                // If switching from rich text to plain, and user edits,
                                // you might want to clear richTextContent or keep it as last known markdown.
                                // For simplicity, we only update general content here.
                                // If viewModel.onContentChange also updates richTextContent conditionally, that's fine.
                            },
                            label = { Text("Content") },
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(4.dp), // Inner padding for the TextField content
                            // maxLines = Int.MAX_VALUE, // For truly multiline
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                cursorColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Note Color",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )

            ColorSelector(
                selectedColorHex = state.colorHex,
                onColorSelected = viewModel::onColorSelect,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}