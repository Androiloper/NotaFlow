package com.example.notaflow.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.notaflow.data.preferences.UserPreferencesRepository

/**
 * ViewModel to handle theme preferences
 */
class ThemeViewModel(
    private val userPreferencesRepository: UserPreferencesRepository
) : androidx.lifecycle.ViewModel() {
    val themeMode = userPreferencesRepository.themeMode
}

/**
 * Provider that applies the user's theme preference
 */
@Composable
fun ThemeProvider(
    viewModel: ThemeViewModel,
    content: @Composable () -> Unit
) {
    // Get the current theme mode from preferences
    val themeMode by viewModel.themeMode.collectAsState(initial = UserPreferencesRepository.ThemeMode.SYSTEM)

    // Determine if dark theme should be used
    val isDarkTheme = when (themeMode) {
        UserPreferencesRepository.ThemeMode.LIGHT -> false
        UserPreferencesRepository.ThemeMode.DARK -> true
        UserPreferencesRepository.ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    // Apply the theme
    NotaFlowTheme(darkTheme = isDarkTheme) {
        content()
    }
}