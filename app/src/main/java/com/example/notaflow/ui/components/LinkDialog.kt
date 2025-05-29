package com.example.notaflow.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties

/**
 * Enhanced dialog for adding or editing a hyperlink in the rich text editor.
 * Compatible with multiple call patterns from the ViewModel.
 */
@Composable
fun LinkDialog(
    // Support for original parameters
    initialText: String = "",
    isVisible: Boolean = true,
    onDismiss: () -> Unit,
    onConfirm: (url: String, text: String) -> Unit,

    // Support for newer parameters
    currentText: String = initialText,
    currentUrl: String = "",
    onTextChange: ((String) -> Unit)? = null,
    onUrlChange: ((String) -> Unit)? = null
) {
    if (!isVisible) return

    var linkText by remember(currentText) { mutableStateOf(currentText) }
    var linkUrl by remember(currentUrl) { mutableStateOf(if (currentUrl.isNotEmpty()) currentUrl else "https://") }
    var isUrlValid by remember { mutableStateOf(true) }

    // Auto-validate URL as user types
    LaunchedEffect(linkUrl) {
        isUrlValid = linkUrl.isBlank() || linkUrl.startsWith("http")
        onUrlChange?.invoke(linkUrl)
    }

    LaunchedEffect(linkText) {
        onTextChange?.invoke(linkText)
    }

    // Focus requesters for text fields
    val textFocusRequester = remember { FocusRequester() }
    val urlFocusRequester = remember { FocusRequester() }

    // Auto-focus the text field if empty, otherwise the URL field
    LaunchedEffect(Unit) {
        if (currentText.isBlank()) {
            textFocusRequester.requestFocus()
        } else {
            urlFocusRequester.requestFocus()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Place, contentDescription = null) },
        title = { Text("Add Link") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                // Link text field
                OutlinedTextField(
                    value = linkText,
                    onValueChange = {
                        linkText = it
                        onTextChange?.invoke(it)
                    },
                    label = { Text("Link Text") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { urlFocusRequester.requestFocus() }
                    ),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(textFocusRequester)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // URL field
                OutlinedTextField(
                    value = linkUrl,
                    onValueChange = {
                        linkUrl = it
                        onUrlChange?.invoke(it)
                    },
                    label = { Text("URL") },
                    placeholder = { Text("https://example.com") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (isValidLink(linkText, linkUrl)) {
                                onConfirm(linkUrl, linkText)
                            }
                        }
                    ),
                    isError = !isUrlValid,
                    supportingText = {
                        AnimatedVisibility(
                            visible = !isUrlValid,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            Text(
                                text = "URL must start with http:// or https://",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    trailingIcon = {
                        if (isUrlValid && linkUrl.isNotBlank()) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Valid URL",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(urlFocusRequester)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(linkUrl, linkText)
                },
                enabled = isValidLink(linkText, linkUrl)
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        ),
        modifier = Modifier.fillMaxWidth(0.9f)
    )
}

// Helper function to validate link info
private fun isValidLink(text: String, url: String): Boolean {
    return text.isNotBlank() && url.isNotBlank() && url.startsWith("http")
}