package com.example.notaflow.ui.screens.noteedit

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Add  // Replace ZoomIn with Add
import androidx.compose.material.icons.filled.ArrowForward // Replace ZoomOut with Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
//import androidx.glance.layout.height
//import androidx.glance.layout.width
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.notaflow.ui.components.ColorSelector
import com.example.notaflow.ui.components.RichTextEditor
import com.example.notaflow.ui.components.EnhancedRichTextDisplay
import kotlin.math.absoluteValue
import kotlin.text.toFloat

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

    // State to track if user is actively editing in non-rich text mode
    var isEditing by remember { mutableStateOf(false) }

    // Zoom-related state variables
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val density = LocalDensity.current

    // Reset zoom function
    val resetZoom = {
        scale = 1f
        offset = Offset.Zero
    }

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
                    // Zoom controls with available icons
                    IconButton(onClick = { scale = (scale * 0.9f).coerceAtLeast(0.5f) }) {
                        Icon(
                            imageVector = Icons.Default.ArrowForward,  // Using Remove instead of ZoomOut
                            contentDescription = "Zoom Out"
                        )
                    }
                    IconButton(onClick = { scale = (scale * 1.1f).coerceAtMost(3f) }) {
                        Icon(
                            imageVector = Icons.Default.Add,  // Using Add instead of ZoomIn
                            contentDescription = "Zoom In"
                        )
                    }

                    // Rich text toggle
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
                                isEditing = false
                                // Reset zoom when changing modes
                                resetZoom()
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
            // Title card
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

            // Content editor/viewer with zoom functionality
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                // Zoomable container
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(8.dp))
                        .pointerInput(Unit) {
                            detectTransformGestures { centroid, pan, zoom, rotation ->
                                // Only scale, ignore rotation
                                scale = (scale * zoom).coerceIn(0.5f, 3f)

                                // Update offset with pan
                                // Calculate the absolute panning limit based on the difference from normal scale (1f)
                                val panLimitX = (size.width.toFloat() * (scale - 1f).absoluteValue) / 2f
                                val panLimitY = (size.height.toFloat() * (scale - 1f).absoluteValue) / 2f

                                offset = Offset(
                                    x = (offset.x + pan.x).coerceIn(-panLimitX, panLimitX),
                                    y = (offset.y + pan.y).coerceIn(-panLimitY, panLimitY)
                                )
                            }
                        }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            // Apply scaling and offset transformations
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offset.x,
                                translationY = offset.y
                            )
                    ) {
                        if (isRichTextEnabled) {
                            // Rich text editor mode
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
                            // Display/Edit mode
                            if (isEditing) {
                                // Plain text editor for editing
                                OutlinedTextField(
                                    value = state.content,
                                    onValueChange = { newContent ->
                                        viewModel.onContentChange(newContent)
                                        if (state.isRichText) {
                                            viewModel.onRichTextContentChange(newContent)
                                        }
                                    },
                                    label = { Text("Content") },
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(4.dp)
                                        .onFocusChanged {
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
                                // Rich text display for viewing
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clickable {
                                            // Only enable editing when scale is normal
                                            // to prevent accidental edits during zoom
                                            if (scale in 0.9f..1.1f) {
                                                isEditing = true
                                            }
                                        }
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

                    // Zoom indicator overlay (when zoomed)
                    if (scale > 1.05f) {
                        Text(
                            text = "${(scale * 100).toInt()}%",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .background(
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .padding(4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Color selector
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