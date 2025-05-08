package com.example.notaflow.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.notaflow.data.preferences.UserPreferencesRepository
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Alignment

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val themeMode by viewModel.themeMode.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(scrollState)
        ) {
            // Theme Settings
            SettingsSection(title = "Appearance") {
                ThemeOption(
                    title = "System Default",
                    selected = themeMode == UserPreferencesRepository.ThemeMode.SYSTEM,
                    onClick = { viewModel.setThemeMode(UserPreferencesRepository.ThemeMode.SYSTEM) }
                )

                ThemeOption(
                    title = "Light",
                    selected = themeMode == UserPreferencesRepository.ThemeMode.LIGHT,
                    onClick = { viewModel.setThemeMode(UserPreferencesRepository.ThemeMode.LIGHT) }
                )

                ThemeOption(
                    title = "Dark",
                    selected = themeMode == UserPreferencesRepository.ThemeMode.DARK,
                    onClick = { viewModel.setThemeMode(UserPreferencesRepository.ThemeMode.DARK) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sort Order Settings
            SettingsSection(title = "Default Sort Order") {
                SortOption(
                    title = "Last Modified",
                    selected = sortOrder == UserPreferencesRepository.NoteSortOrder.MODIFIED_DESC,
                    onClick = { viewModel.setSortOrder(UserPreferencesRepository.NoteSortOrder.MODIFIED_DESC) }
                )

                SortOption(
                    title = "Creation Date",
                    selected = sortOrder == UserPreferencesRepository.NoteSortOrder.CREATED_DESC,
                    onClick = { viewModel.setSortOrder(UserPreferencesRepository.NoteSortOrder.CREATED_DESC) }
                )

                SortOption(
                    title = "Title",
                    selected = sortOrder == UserPreferencesRepository.NoteSortOrder.TITLE_ASC,
                    onClick = { viewModel.setSortOrder(UserPreferencesRepository.NoteSortOrder.TITLE_ASC) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // About Section
            SettingsSection(title = "About") {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "NotaFlow",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Version 1.0.0",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "A modern, professional note-taking app for Android.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(16.dp)
            )

            Divider()

            content()
        }
    }
}

@Composable
fun ThemeOption(
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    SettingsOption(
        title = title,
        selected = selected,
        onClick = onClick
    )
}

@Composable
fun SortOption(
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    SettingsOption(
        title = title,
        selected = selected,
        onClick = onClick
    )
}

@Composable
fun SettingsOption(
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick
        )

        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = 16.dp)
        )
    }
}