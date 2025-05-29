package com.example.notaflow.ui.screens.noteedit

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.unit.sp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.notaflow.data.local.entity.Note
import com.example.notaflow.domain.usecase.GetNoteByIdUseCase
import com.example.notaflow.domain.usecase.SaveNoteUseCase
import com.example.notaflow.ui.components.RichTextFormatAction // Ensure this is the correct import
import com.example.notaflow.utils.RichTextConverter
import com.example.notaflow.utils.RichTextFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.util.Log

@HiltViewModel
class NoteEditViewModel @Inject constructor(
    private val getNoteByIdUseCase: GetNoteByIdUseCase,
    private val saveNoteUseCase: SaveNoteUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _state = MutableStateFlow(NoteEditState())
    val state: StateFlow<NoteEditState> = _state.asStateFlow()

    private val noteId: Long? = savedStateHandle.get<Long>("noteId")?.takeIf { it != -1L }

    private val contentHistory = mutableListOf<TextFieldValue>()
    private var currentHistoryIndex = -1
    private val maxHistorySize = 50

    init {
        if (noteId != null && noteId != 0L) {
            _state.update { it.copy(isNewNote = false) } // Set isNewNote before loading
            loadNote(noteId)
        } else {
            val initialContent = TextFieldValue("")
            addToHistory(initialContent)
            _state.update {
                it.copy(
                    // noteColorHex is already initialized in NoteEditState with default
                    content = initialContent,
                    isNewNote = true
                )
            }
        }
    }

    private fun loadNote(id: Long) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                // Corrected: Pass id as Long if GetNoteByIdUseCase expects Long
                val note = getNoteByIdUseCase(id)
                if (note != null) {
                    val htmlContent = note.content
                    val annotatedString = RichTextConverter.fromHtml(htmlContent)
                    val contentFieldValue = TextFieldValue(
                        annotatedString = annotatedString,
                        selection = TextRange(annotatedString.text.length)
                    )
                    _state.update {
                        it.copy(
                            note = note,
                            title = TextFieldValue(note.title),
                            content = contentFieldValue,
                            noteColorHex = Note.getColorByIndex(note.color).toHexString(),
                            isPinned = note.isPinned,
                            isBookmarked = note.isBookmarked,
                            isLoading = false,
                            isRichText = true // Assume loaded notes are rich text
                        )
                    }
                    contentHistory.clear()
                    addToHistory(contentFieldValue)
                } else {
                    _state.update { it.copy(error = "Note not found", isLoading = false) }
                }
            } catch (e: Exception) {
                _state.update { it.copy(error = "Failed to load note: ${e.message}", isLoading = false) }
                Log.e("NoteEditViewModel", "Error loading note", e)
            }
        }
    }

    fun onTitleChange(newTitle: TextFieldValue) {
        _state.update { it.copy(title = newTitle) }
    }

    fun onContentChange(newContent: TextFieldValue) {
        _state.update { it.copy(content = newContent) }
        if (contentHistory.getOrNull(currentHistoryIndex) != newContent) {
            addToHistory(newContent)
        }
        updateCurrentFormatStyles(newContent)
    }

    fun onRichTextContentChange(markdownOrHtmlContent: String) {
        val currentContentTfv = _state.value.content
        val newAnnotatedString = RichTextConverter.fromHtml(markdownOrHtmlContent)

        // Only update if the content or its primary styling attributes actually changed
        // This helps prevent unnecessary recompositions or history entries.
        if (currentContentTfv.annotatedString.text != newAnnotatedString.text ||
            currentContentTfv.annotatedString.spanStyles != newAnnotatedString.spanStyles ||
            currentContentTfv.annotatedString.paragraphStyles != newAnnotatedString.paragraphStyles) {

            val newContentTfvUpdated = currentContentTfv.copy(
                annotatedString = newAnnotatedString,
                selection = TextRange(newAnnotatedString.length) // Reset selection to end
            )
            onContentChange(newContentTfvUpdated)
        }
    }

    fun onColorSelect(colorHex: String) {
        _state.update { it.copy(noteColorHex = colorHex) }
    }

    fun setRichTextEnabled(enabled: Boolean) {
        _state.update { it.copy(isRichText = enabled) }
        if (!enabled) {
            // From Rich to Plain: Convert AnnotatedString to plain text.
            val plainText = _state.value.content.annotatedString.text
            val newContent = TextFieldValue(plainText, selection = TextRange(plainText.length))
            // Update content without adding to rich text history if it's just a format switch
            _state.update { it.copy(content = newContent) }
            // Optionally, clear rich text history or handle it differently
            contentHistory.clear()
            addToHistory(newContent) // Add the plain text version as the new base
            updateCurrentFormatStyles(newContent)

        } else {
            // From Plain to Rich: The current TextFieldValue might already contain some basic
            // styling if it was loaded from HTML. If it's purely plain, it will render as such.
            // No explicit conversion needed here unless you have a separate plain text source.
            // Ensure history reflects the current state.
            addToHistory(_state.value.content)
        }
    }

    fun saveNote() {
        viewModelScope.launch {
            val currentState = _state.value
            val title = currentState.title.text.trim()
            val contentHtml = RichTextConverter.toHtml(currentState.content.annotatedString)

            if (title.isEmpty() && contentHtml.isEmpty()) {
                _state.update { it.copy(error = "Cannot save empty note", saveCompleted = false) }
                return@launch
            }
            _state.update { it.copy(isLoading = true) }

            val colorObject = try {
                val parsedColorInt = android.graphics.Color.parseColor(currentState.noteColorHex)
                Color(parsedColorInt)
            } catch (e: Exception) {
                Log.w("NoteEditViewModel", "Invalid color hex: ${currentState.noteColorHex}, using default.")
                Note.noteColors.first()
            }
            val colorIndex = Note.indexOfColor(colorObject)

            val noteToSave = currentState.note?.copy(
                title = title,
                content = contentHtml,
                timestamp = System.currentTimeMillis(),
                color = colorIndex,
                isPinned = currentState.isPinned,
                isBookmarked = currentState.isBookmarked
            ) ?: Note(
                title = title,
                content = contentHtml,
                createdTimestamp = System.currentTimeMillis(),
                timestamp = System.currentTimeMillis(),
                color = colorIndex,
                isPinned = currentState.isPinned,
                isBookmarked = currentState.isBookmarked
            )

            try {
                saveNoteUseCase(noteToSave)
                _state.update { it.copy(isLoading = false, saveCompleted = true, error = null) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = "Failed to save note: ${e.message}", saveCompleted = false) }
                Log.e("NoteEditViewModel", "Error saving note", e)
            }
        }
    }

    fun dismissError() {
        _state.update { it.copy(error = null) }
    }

    private fun updateCurrentFormatStyles(contentValue: TextFieldValue) {
        val selection = contentValue.selection
        val currentStyles = mutableSetOf<String>()
        val annotatedString = contentValue.annotatedString

        if (selection.collapsed) {
            annotatedString.spanStyles.filter { it.start <= selection.start && it.end >= selection.start }.forEach {
                if (it.item.fontWeight == FontWeight.Bold) currentStyles.add("BOLD")
                if (it.item.fontStyle == FontStyle.Italic) currentStyles.add("ITALIC")
                it.item.textDecoration?.let { deco ->
                    if (deco.contains(TextDecoration.Underline)) currentStyles.add("UNDERLINE")
                    if (deco.contains(TextDecoration.LineThrough)) currentStyles.add("STRIKETHROUGH")
                }
                if (annotatedString.getStringAnnotations("URL", it.start, it.end).isNotEmpty()) currentStyles.add("LINK")
            }
            annotatedString.paragraphStyles.filter { it.start <= selection.start && it.end >= selection.start }.forEach {
                if (it.item.textIndent == TextIndent(16.sp, 16.sp)) currentStyles.add("BLOCKQUOTE")
            }
        } else {
            // For a selection, a style is "active" if it's present anywhere in the selection.
            // More sophisticated logic could check if it's uniformly applied.
            annotatedString.spanStyles.filter { maxOf(it.start, selection.min) < minOf(it.end, selection.max) }.forEach {
                if (it.item.fontWeight == FontWeight.Bold) currentStyles.add("BOLD")
                if (it.item.fontStyle == FontStyle.Italic) currentStyles.add("ITALIC")
                it.item.textDecoration?.let { deco ->
                    if (deco.contains(TextDecoration.Underline)) currentStyles.add("UNDERLINE")
                    if (deco.contains(TextDecoration.LineThrough)) currentStyles.add("STRIKETHROUGH")
                }
                if (annotatedString.getStringAnnotations("URL", it.start, it.end).isNotEmpty()) currentStyles.add("LINK")
            }
            annotatedString.paragraphStyles.filter { maxOf(it.start, selection.min) < minOf(it.end, selection.max) }.forEach {
                if (it.item.textIndent == TextIndent(16.sp, 16.sp)) currentStyles.add("BLOCKQUOTE")
            }
        }
        _state.update { it.copy(currentFormatStyles = currentStyles) }
    }

    fun handleFormatAction(action: RichTextFormatAction) {
        when (action) {
            RichTextFormatAction.BOLD -> toggleStyle(RichTextFormatter.StyleType.BOLD)
            RichTextFormatAction.ITALIC -> toggleStyle(RichTextFormatter.StyleType.ITALIC)
            RichTextFormatAction.UNDERLINE -> toggleStyle(RichTextFormatter.StyleType.UNDERLINE)
            RichTextFormatAction.STRIKETHROUGH -> toggleStyle(RichTextFormatter.StyleType.STRIKETHROUGH)
            RichTextFormatAction.BLOCKQUOTE -> toggleParagraphStyle(RichTextFormatter.ParagraphStyleType.BLOCKQUOTE)
            RichTextFormatAction.LINK -> _state.update { it.copy(showLinkDialog = true) }
            RichTextFormatAction.COLOR -> { /* This case might be redundant if color selection directly calls applyTextColor */ }
            RichTextFormatAction.UNDO -> undo()
            RichTextFormatAction.REDO -> redo()
        }
    }

    fun applyTextColor(color: Color) {
        val currentContent = _state.value.content
        val selection = currentContent.selection
        if (selection.collapsed && currentContent.composition == null) { // Apply to typing attributes if not composing
            _state.update { it.copy(currentSelectedTextColor = color) }
            // To make future typed text have this color, you might need to adjust how TextField handles this.
            // Often, this involves pre-setting a span that new characters will inherit.
            // For simplicity, we're just updating the state for the color picker's visual feedback.
            return
        }
        val newTextFieldValue = RichTextFormatter.formatColor(currentContent, color)
        onContentChange(newTextFieldValue)
        _state.update { it.copy(currentSelectedTextColor = color) }
    }

    fun applyLink(url: String, text: String?) {
        val currentContent = _state.value.content
        val selection = currentContent.selection
        val linkTextToShow = text ?: url // Default to URL if text is null/empty
        val newTextFieldValue = RichTextFormatter.applyLink(currentContent, url, linkTextToShow, selection)
        onContentChange(newTextFieldValue)
        _state.update { it.copy(showLinkDialog = false, currentLinkUrl = "", currentLinkText = "") }
    }

    fun onLinkDialogDismiss() {
        _state.update { it.copy(showLinkDialog = false, currentLinkUrl = "", currentLinkText = "") }
    }

    fun onLinkTextChange(text: String) {
        _state.update { it.copy(currentLinkText = text) }
    }

    fun onLinkUrlChange(url: String) {
        _state.update { it.copy(currentLinkUrl = url) }
    }

    private fun toggleStyle(styleType: RichTextFormatter.StyleType) {
        val currentContent = _state.value.content
        val newTextFieldValue = RichTextFormatter.toggleStyle(currentContent, styleType)
        onContentChange(newTextFieldValue)
    }

    private fun toggleParagraphStyle(styleType: RichTextFormatter.ParagraphStyleType) {
        val currentContent = _state.value.content
        val newTextFieldValue = RichTextFormatter.toggleParagraphStyle(currentContent, styleType)
        onContentChange(newTextFieldValue)
    }

    private fun addToHistory(value: TextFieldValue) {
        // If we undo and then type, clear the "redo" future
        if (currentHistoryIndex < contentHistory.size - 1) {
            contentHistory.subList(currentHistoryIndex + 1, contentHistory.size).clear()
        }
        contentHistory.add(value)
        if (contentHistory.size > maxHistorySize) {
            contentHistory.removeAt(0) // Keep history size bounded
        }
        currentHistoryIndex = contentHistory.size - 1
        updateUndoRedoState()
    }

    private fun undo() {
        if (canUndo()) {
            currentHistoryIndex--
            val previousState = contentHistory[currentHistoryIndex]
            _state.update { it.copy(content = previousState) } // Directly update content
            updateUndoRedoState()
            updateCurrentFormatStyles(previousState) // Update toolbar based on new content
        }
    }

    private fun redo() {
        if (canRedo()) {
            currentHistoryIndex++
            val nextState = contentHistory[currentHistoryIndex]
            _state.update { it.copy(content = nextState) } // Directly update content
            updateUndoRedoState()
            updateCurrentFormatStyles(nextState) // Update toolbar based on new content
        }
    }

    private fun canUndo(): Boolean = currentHistoryIndex > 0
    private fun canRedo(): Boolean = currentHistoryIndex < contentHistory.size - 1

    private fun updateUndoRedoState() {
        _state.update { it.copy(canUndo = canUndo(), canRedo = canRedo()) }
    }
}
