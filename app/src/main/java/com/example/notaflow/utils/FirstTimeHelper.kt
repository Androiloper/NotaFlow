// utils/FirstTimeHelper.kt
package com.example.notaflow.utils

import android.content.Context
import android.content.SharedPreferences
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext

/**
 * Helper class to show first-time user guidance.
 */
object FirstTimeHelper {

    private const val PREFS_NAME = "NotaFlowPrefs"
    private const val KEY_LONG_PRESS_HELPER_SHOWN = "long_press_helper_shown"

    /**
     * Shows a helper toast for the long press feature if it's the first time the user is using the app.
     */
    @Composable
    fun ShowLongPressHelper(notes: List<Any>) {
        val context = LocalContext.current

        LaunchedEffect(notes.isNotEmpty()) {
            if (notes.isNotEmpty() && !isLongPressHelperShown(context)) {
                Toast.makeText(
                    context,
                    "Tip: Long press on a note to see more options",
                    Toast.LENGTH_LONG
                ).show()
                markLongPressHelperAsShown(context)
            }
        }
    }

    /**
     * Checks if the long press helper has already been shown
     */
    private fun isLongPressHelperShown(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_LONG_PRESS_HELPER_SHOWN, false)
    }

    /**
     * Marks the long press helper as shown so it won't be displayed again
     */
    private fun markLongPressHelperAsShown(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_LONG_PRESS_HELPER_SHOWN, true).apply()
    }

    /**
     * Resets the helper flag for testing purposes
     */
    fun resetLongPressHelper(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_LONG_PRESS_HELPER_SHOWN, false).apply()
    }
}