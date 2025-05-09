package com.example.notaflow.ui.screens.noteedit

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.notaflow.ui.components.ColorSelector
import com.example.notaflow.ui.components.RichTextEditor
import com.example.notaflow.ui.components.EnhancedRichTextDisplay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditScreen(
    noteId: Long?,
    onNavigateBack: () -> Unit,
    viewModel: NoteEditViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // State for toggling rich text editing, initialized from ViewModel state
    var isRichTextEnabled by remember(state.isRichText) { mutableStateOf(state.isRichText) }

    // NEW: State to track if user is actively editing in non-rich text mode
    var isEditing by remember { mutableStateOf(false) }

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
                                // Reset editing state when switching modes
                                isEditing = false
                            }
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    viewModel.saveNote()
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
            // Title card - no changes needed here
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
                            .height(56.dp)
                            .background(noteColor)
                    )
                    OutlinedTextField(
                        value = state.title,
                        onValueChange = viewModel::onTitleChange,
                        label = { Text("Title") },
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 8.dp, end = 8.dp),
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

            // Content editor/viewer - THIS IS THE MAIN SECTION THAT CHANGES
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
                        .clip(RoundedCornerShape(8.dp))
                ) {
                    if (isRichTextEnabled) {
                        // Rich text editor mode - remains the same
                        RichTextEditor(
                            initialContent = if (state.richTextContent.isNotEmpty()) {
                                state.richTextContent
                            } else {
                                state.content
                            },
                            onMarkdownChanged = { markdownText ->
                                viewModel.onContentChange(markdownText)
                                viewModel.onRichTextContentChange(markdownText)
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        // CHANGED: Display rich text when not editing, show editor when actively editing
                        if (isEditing) {
                            // Plain text editor for editing
                            OutlinedTextField(
                                value = state.content,
                                onValueChange = { newContent ->
                                    viewModel.onContentChange(newContent)
                                    // If switching from rich text to plain, and user edits,
                                    // keep the rich text content updated as well to preserve formatting
                                    if (state.isRichText) {
                                        viewModel.onRichTextContentChange(newContent)
                                    }
                                },
                                label = { Text("Content") },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(4.dp)
                                    .onFocusChanged {
                                        // Only update editing state when focus changes to ensure
                                        // we don't constantly reset the state
                                        if (!it.isFocused && isEditing) {
                                            isEditing = false
                                        }
                                    },
                                colors = TextFieldDefaults.outlinedTextFieldColors(
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedBorderColor = Color.Transparent,
                                    cursorColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        } else {
                            // Rich text display for viewing - clickable to edit
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clickable { isEditing = true }
                                    .padding(16.dp)
                            ) {
                                // Display formatted content
                                EnhancedRichTextDisplay(
                                    markdownContent = if (state.richTextContent.isNotEmpty()) {
                                        state.richTextContent
                                    } else {
                                        state.content
                                    },
                                    modifier = Modifier.fillMaxSize(),
                                    backgroundColor = MaterialTheme.colorScheme.surface
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Color selector - no changes needed
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