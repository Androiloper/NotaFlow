package com.example.notaflow.ui.screens.noteedit

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.notaflow.data.local.entity.Note
import com.example.notaflow.domain.usecase.GetNoteByIdUseCase
import com.example.notaflow.domain.usecase.SaveNoteUseCase
import com.example.notaflow.ui.theme.NoteColors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class NoteEditViewModel @Inject constructor(
    private val getNoteByIdUseCase: GetNoteByIdUseCase,
    private val saveNoteUseCase: SaveNoteUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // UI state
    private val _state = MutableStateFlow(NoteEditState())
    val state: StateFlow<NoteEditState> = _state.asStateFlow()

    // Note ID from navigation
    private val noteId: Long? = savedStateHandle.get<Long>("noteId")?.takeIf { it != -1L }

    init {
        // If editing existing note, load it
        noteId?.let { id ->
            loadNote(id)
        }
    }

    private fun loadNote(id: Long) {
        viewModelScope.launch {
            getNoteByIdUseCase(id)?.let { note ->
                _state.value = _state.value.copy(
                    noteId = note.id,
                    title = note.title,
                    content = note.content,
                    colorHex = note.colorHex,
                    isNewNote = false
                )
            }
        }
    }

    fun onTitleChange(title: String) {
        _state.value = _state.value.copy(title = title)
    }

    fun onContentChange(content: String) {
        _state.value = _state.value.copy(content = content)
    }

    fun onColorSelect(colorHex: String) {
        _state.value = _state.value.copy(colorHex = colorHex)
    }

    fun saveNote(): Boolean {
        val currentState = _state.value

        // Basic validation
        if (currentState.title.isBlank() && currentState.content.isBlank()) {
            _state.value = currentState.copy(
                error = "Note cannot be empty",
                isSaving = false
            )
            return false
        }

        viewModelScope.launch {
            _state.value = currentState.copy(isSaving = true, error = null)

            try {
                val note = Note(
                    id = currentState.noteId,
                    title = currentState.title,
                    content = currentState.content,
                    colorHex = currentState.colorHex,
                    modifiedAt = Date()
                    // createdAt will default to now for new notes or keep existing for updates
                )

                saveNoteUseCase(note)

                // Reset UI state after successful save
                _state.value = currentState.copy(
                    isSaving = false,
                    saveCompleted = true
                )
            } catch (e: Exception) {
                _state.value = currentState.copy(
                    isSaving = false,
                    error = "Failed to save note: ${e.message}"
                )
            }
        }

        return true
    }

    fun dismissError() {
        _state.value = _state.value.copy(error = null)
    }
}