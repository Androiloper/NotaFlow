package com.example.notaflow.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

// Extension property for Context to access DataStore
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

@Singleton
class UserPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // Keys
    private object PreferencesKeys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val NOTE_SORT_ORDER = stringPreferencesKey("note_sort_order")
    }

    // Theme settings
    enum class ThemeMode {
        LIGHT, DARK, SYSTEM
    }

    // Sort order options
    enum class NoteSortOrder {
        MODIFIED_DESC, CREATED_DESC, TITLE_ASC
    }

    // Get theme mode flow
    val themeMode: Flow<ThemeMode> = context.dataStore.data
        .map { preferences ->
            val themeModeString = preferences[PreferencesKeys.THEME_MODE] ?: ThemeMode.SYSTEM.name
            try {
                ThemeMode.valueOf(themeModeString)
            } catch (e: IllegalArgumentException) {
                ThemeMode.SYSTEM
            }
        }

    // Get sort order flow
    val noteSortOrder: Flow<NoteSortOrder> = context.dataStore.data
        .map { preferences ->
            val sortOrderString = preferences[PreferencesKeys.NOTE_SORT_ORDER] ?: NoteSortOrder.MODIFIED_DESC.name
            try {
                NoteSortOrder.valueOf(sortOrderString)
            } catch (e: IllegalArgumentException) {
                NoteSortOrder.MODIFIED_DESC
            }
        }

    // Save theme mode
    suspend fun saveThemeMode(themeMode: ThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = themeMode.name
        }
    }

    // Save sort order
    suspend fun saveNoteSortOrder(sortOrder: NoteSortOrder) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.NOTE_SORT_ORDER] = sortOrder.name
        }
    }
}