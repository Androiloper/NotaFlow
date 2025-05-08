// ui/screens/notelist/NoteListViewModel.kt
package com.example.notaflow.ui.screens.notelist

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.notaflow.data.local.entity.Note
import com.example.notaflow.data.preferences.UserPreferencesRepository
import com.example.notaflow.domain.usecase.GetNotesUseCase
import com.example.notaflow.domain.usecase.RestoreNoteUseCase
import com.example.notaflow.domain.usecase.SearchNotesUseCase
import com.example.notaflow.domain.usecase.SoftDeleteNoteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NoteListViewModel @Inject constructor(
    private val getNotesUseCase: GetNotesUseCase,
    private val searchNotesUseCase: SearchNotesUseCase,
    private val softDeleteNoteUseCase: SoftDeleteNoteUseCase,
    private val restoreNoteUseCase: RestoreNoteUseCase,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    // Latest deleted note ID for undo functionality
    private val _lastDeletedNoteId = MutableStateFlow<Long?>(null)
    val lastDeletedNoteId: StateFlow<Long?> = _lastDeletedNoteId

    // Get sort order from preferences
    private val sortOrder = userPreferencesRepository.noteSortOrder

    @OptIn(ExperimentalCoroutinesApi::class)
    val notes: StateFlow<List<Note>> = _searchQuery
        .debounce(300)
        .flatMapLatest { query ->
            if (query.isBlank()) {
                getNotesUseCase()
            } else {
                searchNotesUseCase(query)
            }
        }
        .combine(sortOrder) { notes, sortOrder ->
            when (sortOrder) {
                UserPreferencesRepository.NoteSortOrder.MODIFIED_DESC -> notes.sortedByDescending { it.modifiedAt }
                UserPreferencesRepository.NoteSortOrder.CREATED_DESC -> notes.sortedByDescending { it.createdAt }
                UserPreferencesRepository.NoteSortOrder.TITLE_ASC -> notes.sortedBy { it.title }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun changeSortOrder(sortOrder: UserPreferencesRepository.NoteSortOrder) {
        viewModelScope.launch {
            userPreferencesRepository.saveNoteSortOrder(sortOrder)
        }
    }

    /**
     * Soft deletes a note (marks it as deleted but keeps it in the database).
     */
    fun deleteNote(note: Note) {
        viewModelScope.launch {
            try {
                Log.d("NotaFlow", "ViewModel: Soft deleting note ID ${note.id}")
                softDeleteNoteUseCase(note)
                // Store the ID for potential undo
                _lastDeletedNoteId.value = note.id
                Log.d("NotaFlow", "ViewModel: Note soft deleted, ID stored for undo: ${note.id}")
            } catch (e: Exception) {
                Log.e("NotaFlow", "ViewModel: Error soft deleting note", e)
            }
        }
    }

    /**
     * Restores a soft-deleted note.
     */
    fun undoDelete() {
        val noteId = _lastDeletedNoteId.value
        if (noteId != null) {
            viewModelScope.launch {
                try {
                    Log.d("NotaFlow", "ViewModel: Undoing delete for note ID $noteId")
                    restoreNoteUseCase(noteId)
                    _lastDeletedNoteId.value = null
                    Log.d("NotaFlow", "ViewModel: Note ID $noteId restored successfully")
                } catch (e: Exception) {
                    Log.e("NotaFlow", "ViewModel: Error restoring note", e)
                }
            }
        } else {
            Log.w("NotaFlow", "ViewModel: No note ID to restore")
        }
    }

    /**
     * Clears the last deleted note ID.
     */
    fun clearLastDeletedNoteId() {
        _lastDeletedNoteId.value = null
    }
}