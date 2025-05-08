package com.example.notaflow.ui.screens.notelist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.notaflow.data.local.entity.Note
import com.example.notaflow.data.preferences.UserPreferencesRepository
import com.example.notaflow.domain.usecase.DeleteNoteUseCase
import com.example.notaflow.domain.usecase.GetNotesUseCase
import com.example.notaflow.domain.usecase.SearchNotesUseCase
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
    private val deleteNoteUseCase: DeleteNoteUseCase,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    // Track when a note is being deleted to show confirmation dialog
    private val _noteToDelete = MutableStateFlow<Note?>(null)
    val noteToDelete: StateFlow<Note?> = _noteToDelete

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

    fun onDeleteNoteClick(note: Note) {
        _noteToDelete.value = note
    }

    fun confirmDeleteNote() {
        val noteToDelete = _noteToDelete.value ?: return
        viewModelScope.launch {
            deleteNoteUseCase(noteToDelete)
            _noteToDelete.value = null
        }
    }

    fun dismissDeleteDialog() {
        _noteToDelete.value = null
    }

    fun changeSortOrder(sortOrder: UserPreferencesRepository.NoteSortOrder) {
        viewModelScope.launch {
            userPreferencesRepository.saveNoteSortOrder(sortOrder)
        }
    }
}