// ui/screens/notelist/NoteListScreen.kt
package com.example.notaflow.ui.screens.notelist

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.notaflow.data.local.entity.Note
import com.example.notaflow.data.preferences.UserPreferencesRepository
import com.example.notaflow.utils.FirstTimeHelper
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteListScreen(
    onNoteClick: (Long) -> Unit,
    onNewNoteClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onNavigateToHome: () -> Unit = {},
    viewModel: NoteListViewModel = hiltViewModel()
) {
    val notes by viewModel.notes.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val lastDeletedNoteId by viewModel.lastDeletedNoteId.collectAsState()
    val context = LocalContext.current
    //val coroutineScope = rememberCoroutineScope()

    var showSortMenu by remember { mutableStateOf(false) }
    var isSearchActive by remember { mutableStateOf(false) }

    // Show helper tooltip for long press the first time
    FirstTimeHelper.ShowLongPressHelper(notes)

    // Snackbar host state
    val snackbarHostState = remember { SnackbarHostState() }

    // Show snackbar when a note is deleted
    LaunchedEffect(lastDeletedNoteId) {
        lastDeletedNoteId?.let { noteId ->
            Log.d("NotaFlow", "NoteListScreen: Showing undo snackbar for note ID: $noteId")

            // Show the snackbar
            val result = snackbarHostState.showSnackbar(
                message = "Note moved to trash",
                actionLabel = "UNDO",
                duration = SnackbarDuration.Long
            )

            // Process the result
            when (result) {
                SnackbarResult.ActionPerformed -> {
                    // User clicked UNDO
                    Log.d("NotaFlow", "NoteListScreen: User clicked UNDO for note ID: $noteId")
                    viewModel.undoDelete()
                    Toast.makeText(context, "Note restored", Toast.LENGTH_SHORT).show()
                }
                SnackbarResult.Dismissed -> {
                    // User dismissed or timeout
                    Log.d("NotaFlow", "NoteListScreen: Snackbar dismissed without undo")
                    viewModel.clearLastDeletedNoteId()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (!isSearchActive) {
                        Text("NotaFlow")
                    }
                },
                navigationIcon = {
                    // Home button
                    IconButton(onClick = onNavigateToHome) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = "Go to Home"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { isSearchActive = true }) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search"
                        )
                    }

                    IconButton(onClick = { showSortMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More options"
                        )
                    }

                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Sort by last modified") },
                            onClick = {
                                viewModel.changeSortOrder(UserPreferencesRepository.NoteSortOrder.MODIFIED_DESC)
                                showSortMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Sort by created date") },
                            onClick = {
                                viewModel.changeSortOrder(UserPreferencesRepository.NoteSortOrder.CREATED_DESC)
                                showSortMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Sort by title") },
                            onClick = {
                                viewModel.changeSortOrder(UserPreferencesRepository.NoteSortOrder.TITLE_ASC)
                                showSortMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Settings") },
                            onClick = {
                                onSettingsClick()
                                showSortMenu = false
                            }
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNewNoteClick,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Note",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search Bar
            SearchBar(
                query = searchQuery,
                onQueryChange = viewModel::onSearchQueryChanged,
                onSearch = { isSearchActive = false },
                active = isSearchActive,
                onActiveChange = { isSearchActive = it },
                placeholder = { Text("Search notes") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search Icon"
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear search"
                            )
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                // Search suggestions could be added here
            }

            if (notes.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (searchQuery.isBlank()) {
                            "No notes yet.\nTap + to create a new note."
                        } else {
                            "No notes found for \"$searchQuery\"."
                        },
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        items = notes,
                        key = { note -> note.id } // Important for animations
                    ) { note ->
                        LongPressNoteItem(
                            note = note,
                            onNoteClick = { onNoteClick(note.id) },
                            onDeleteConfirm = {
                                // Delete the note and show immediate feedback
                                viewModel.deleteNote(note)
                                Toast.makeText(context, "Note moved to trash", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
        }
    }
}