// ui/components/SnackbarState.kt
package com.example.notaflow.ui.components

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * State holder for snackbar functionality with undo support.
 */
class SnackbarState(
    val snackbarHostState: SnackbarHostState,
    private val coroutineScope: CoroutineScope
) {
    private var currentSnackbarJob: Job? = null

    /**
     * Shows a snackbar with an undo action.
     *
     * @param message The message to display
     * @param actionLabel The label for the action button (typically "UNDO")
     * @param onAction The callback to invoke when the action is clicked
     * @param onDismiss The callback to invoke when the snackbar is dismissed without action
     */
    fun showUndoSnackbar(
        message: String,
        actionLabel: String,
        onAction: () -> Unit,
        onDismiss: () -> Unit
    ) {
        // Cancel any existing snackbar to prevent multiple snackbars from queueing
        currentSnackbarJob?.cancel()

        currentSnackbarJob = coroutineScope.launch {
            try {
                // Show the snackbar and get the result
                val result = snackbarHostState.showSnackbar(
                    message = message,
                    actionLabel = actionLabel,
                    duration = SnackbarDuration.Short
                )

                // Handle the result
                when (result) {
                    SnackbarResult.ActionPerformed -> {
                        onAction()
                    }
                    SnackbarResult.Dismissed -> {
                        onDismiss()
                    }
                }
            } catch (e: Exception) {
                // Log any errors that occur during snackbar display
                Timber.e(e, "Error showing undo snackbar")
            }
        }
    }

    /**
     * Shows a simple snackbar with a message.
     *
     * @param message The message to display
     */
    fun showSnackbar(message: String) {
        // Cancel any existing snackbar
        currentSnackbarJob?.cancel()

        currentSnackbarJob = coroutineScope.launch {
            try {
                snackbarHostState.showSnackbar(message)
            } catch (e: Exception) {
                Timber.e(e, "Error showing snackbar")
            }
        }
    }
}

/**
 * Creates and remembers a SnackbarState.
 */
@Composable
fun rememberSnackbarState(): SnackbarState {
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    return remember {
        SnackbarState(snackbarHostState, coroutineScope)
    }
}