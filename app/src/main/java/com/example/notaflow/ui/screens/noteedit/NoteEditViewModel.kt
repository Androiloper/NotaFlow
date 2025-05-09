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
import com.example.notaflow.utils.RichTextFormatter
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
                    content = if (note.isRichText) RichTextFormatter.stripMarkdown(note.richTextContent) else note.content,
                    colorHex = note.colorHex,
                    isNewNote = false,
                    isRichText = note.isRichText,
                    richTextContent = note.richTextContent
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

    fun onRichTextContentChange(richTextContent: String) {
        _state.value = _state.value.copy(richTextContent = richTextContent)
    }

    fun onColorSelect(colorHex: String) {
        _state.value = _state.value.copy(colorHex = colorHex)
    }

    fun setRichTextEnabled(enabled: Boolean) {
        val currentState = _state.value

        // If turning on rich text from plain text, convert the existing content
        // to markdown to preserve it
        val richTextContent = if (enabled && currentState.richTextContent.isBlank()) {
            // Simply use the content as-is initially
            currentState.content
        } else {
            currentState.richTextContent
        }

        _state.value = currentState.copy(
            isRichText = enabled,
            richTextContent = richTextContent
        )
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
                // Prepare content based on whether rich text is enabled
                val finalContent = if (currentState.isRichText) {
                    // For rich text, we'll store the plain content in the regular content field
                    // and the markdown in the richTextContent field
                    RichTextFormatter.stripMarkdown(currentState.richTextContent)
                } else {
                    currentState.content
                }

                val note = Note(
                    id = currentState.noteId,
                    title = currentState.title,
                    content = finalContent,
                    colorHex = currentState.colorHex,
                    modifiedAt = Date(),
                    isRichText = currentState.isRichText,
                    richTextContent = if (currentState.isRichText)
                        RichTextFormatter.enhanceMarkdown(currentState.richTextContent)
                    else ""
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