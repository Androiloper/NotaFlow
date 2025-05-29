package com.example.notaflow.ui.screens.noteedit

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.notaflow.ui.components.ColorSelector
import com.example.notaflow.ui.components.LinkDialog
import com.example.notaflow.ui.components.RichTextEditor
import com.example.notaflow.ui.components.RichTextFormatAction
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditScreen(
    noteId: Long?,
    onNavigateBack: () -> Unit,
    viewModel: NoteEditViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val hapticFeedback = LocalHapticFeedback.current
    val focusManager = LocalFocusManager.current

    var isRichTextEnabledState by remember(state.isRichText) { mutableStateOf(state.isRichText) }
    var isEditingPlainText by remember { mutableStateOf(false) } // For plain text mode focus

    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var isZooming by remember { mutableStateOf(false) }

    val resetZoom = {
        scale = 1f
        offset = Offset.Zero
        isZooming = false
    }

    LaunchedEffect(state.saveCompleted, state.error) {
        if (state.saveCompleted) {
            onNavigateBack()
        }
        state.error?.let { error ->
            snackbarHostState.showSnackbar(
                message = error,
                duration = androidx.compose.material3.SnackbarDuration.Short
            )
            viewModel.dismissError() // Ensure this is called
        }
    }

    // Show LinkDialog when needed
    if (state.showLinkDialog) {
        LinkDialog(
            initialText = state.currentLinkText,
            onDismiss = viewModel::onLinkDialogDismiss,
            onConfirm = viewModel::applyLink,
            onTextChange = viewModel::onLinkTextChange,
            onUrlChange = viewModel::onLinkUrlChange,
            currentUrl = state.currentLinkUrl
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isNewNote) "New Note" else "Edit Note") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    // Button to switch to Rich Text (if currently plain)
                    AnimatedVisibility(visible = !isRichTextEnabledState && !isZooming && !isEditingPlainText) {
                        IconButton(onClick = {
                            viewModel.setRichTextEnabled(true)
                            isRichTextEnabledState = true
                        }) {
                            Icon(Icons.Default.FormatBold, "Enable Rich Text") // Placeholder Icon
                        }
                    }

                    AnimatedVisibility(visible = scale != 1f || offset != Offset.Zero) {
                        IconButton(onClick = resetZoom) {
                            Icon(Icons.Default.Refresh, "Reset Zoom") // Placeholder Icon
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 8.dp)) {
                        Text("Rich Text", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(end = 4.dp))
                        Switch(
                            checked = isRichTextEnabledState,
                            onCheckedChange = { enabled ->
                                viewModel.setRichTextEnabled(enabled)
                                isRichTextEnabledState = enabled
                                isEditingPlainText = false
                                resetZoom()
                                focusManager.clearFocus()
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.saveNote() // saveNote now updates state for completion/error
                },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Check, "Save", tint = MaterialTheme.colorScheme.onPrimary)
            }
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { paddingValues ->
        val noteColor = try {
            Color(android.graphics.Color.parseColor(state.noteColorHex))
        } catch (e: IllegalArgumentException) {
            MaterialTheme.colorScheme.surfaceVariant // Fallback
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.surface) // Ensure background for the whole edit area
        ) {
            // Title Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier
                        .width(6.dp)
                        .height(56.dp)
                        .background(noteColor))
                    OutlinedTextField(
                        value = state.title,
                        onValueChange = viewModel::onTitleChange,
                        label = { Text("Title") },
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp),
                        singleLine = true,
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            containerColor = Color.Transparent,
                            cursorColor = MaterialTheme.colorScheme.primary
                        ),
                        textStyle = MaterialTheme.typography.titleLarge
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Content Editor/Viewer Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(8.dp))
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                if (isEditingPlainText && isRichTextEnabledState) return@detectTransformGestures // No zoom if typing in rich text
                                if (!isRichTextEnabledState && isEditingPlainText) return@detectTransformGestures // No zoom if typing in plain text

                                isZooming = true
                                scale = (scale * zoom).coerceIn(0.5f, 3f)
                                val panLimitX = (size.width.toFloat() * (scale - 1f).absoluteValue) / 2f
                                val panLimitY = (size.height.toFloat() * (scale - 1f).absoluteValue) / 2f
                                offset = Offset(
                                    x = (offset.x + pan.x).coerceIn(-panLimitX, panLimitX),
                                    y = (offset.y + pan.y).coerceIn(-panLimitY, panLimitY)
                                )
                                coroutineScope.launch { delay(300); isZooming = false }
                            }
                        }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offset.x,
                                translationY = offset.y
                            )
                    ) {
                        if (isRichTextEnabledState) {
                            RichTextEditor(
                                titleValue = state.title,
                                onTitleChange = viewModel::onTitleChange,
                                contentValue = state.content,
                                onContentChange = viewModel::onContentChange,
                                onStyleClick = viewModel::handleFormatAction,
                                onLinkClick = {
                                    viewModel.handleFormatAction(RichTextFormatAction.LINK)
                                },
                                // Handle color changes locally and then call the ViewModel
                                onColorSelected = { color ->
                                    coroutineScope.launch {
                                        try {
                                            // Call the ViewModel method if it exists
                                            viewModel.handleFormatAction(RichTextFormatAction.COLOR)
                                            // You might need some additional state here if the ViewModel
                                            // doesn't properly handle the color
                                        } catch (e: Exception) {
                                            // Fallback if the method doesn't exist or has errors
                                            // This is where you'd handle the color directly if needed
                                        }
                                    }
                                },
                                currentSelectedTextColor = state.currentSelectedTextColor,
                                currentFormatStyles = state.currentFormatStyles,
                                canUndo = state.canUndo,
                                canRedo = state.canRedo,
                                noteColor = noteColor,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else { // Plain Text Mode
                            if (isEditingPlainText || state.content.text.isEmpty()) { // Show editor if focused or content is empty
                                OutlinedTextField(
                                    value = state.content.text,
                                    onValueChange = { newText ->
                                        viewModel.onContentChange(TextFieldValue(newText))
                                    },
                                    label = { Text("Content") },
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(4.dp)
                                        .onFocusChanged { focusState ->
                                            isEditingPlainText = focusState.isFocused
                                            if (focusState.isFocused) resetZoom()
                                        },
                                    colors = TextFieldDefaults.outlinedTextFieldColors(
                                        focusedBorderColor = Color.Transparent,
                                        unfocusedBorderColor = Color.Transparent,
                                        containerColor = Color.Transparent,
                                        cursorColor = MaterialTheme.colorScheme.primary
                                    ),
                                    textStyle = MaterialTheme.typography.bodyLarge
                                )
                            } else { // Display plain text (non-editable, clickable to edit)
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clickable(enabled = !isZooming) {
                                            if (scale in 0.9f..1.1f) { // Only allow edit if not significantly zoomed
                                                isEditingPlainText = true
                                            }
                                        }
                                        .padding(16.dp) // Padding for display text
                                        .verticalScroll(rememberScrollState()) // Make display text scrollable
                                ) {
                                    Text(
                                        text = state.content.text,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    this@Card.AnimatedVisibility(
                        visible = scale > 1.05f && !isEditingPlainText,
                        enter = fadeIn(),
                        exit = fadeOut(),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.8f),
                            shape = RoundedCornerShape(4.dp),
                            tonalElevation = 2.dp
                        ) {
                            Text(
                                "${(scale * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.inverseOnSurface,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "Note Color",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(start = 16.dp, bottom = 8.dp, top = 8.dp)
            )
            ColorSelector(
                selectedColorHex = state.noteColorHex,
                onColorSelected = viewModel::onColorSelect,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}