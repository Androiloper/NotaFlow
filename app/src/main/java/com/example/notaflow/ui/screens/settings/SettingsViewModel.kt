package com.example.notaflow.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.notaflow.data.preferences.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    val themeMode: StateFlow<UserPreferencesRepository.ThemeMode> =
        userPreferencesRepository.themeMode.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserPreferencesRepository.ThemeMode.SYSTEM
        )

    val sortOrder: StateFlow<UserPreferencesRepository.NoteSortOrder> =
        userPreferencesRepository.noteSortOrder.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserPreferencesRepository.NoteSortOrder.MODIFIED_DESC
        )

    fun setThemeMode(themeMode: UserPreferencesRepository.ThemeMode) {
        viewModelScope.launch {
            userPreferencesRepository.saveThemeMode(themeMode)
        }
    }

    fun setSortOrder(sortOrder: UserPreferencesRepository.NoteSortOrder) {
        viewModelScope.launch {
            userPreferencesRepository.saveNoteSortOrder(sortOrder)
        }
    }
}