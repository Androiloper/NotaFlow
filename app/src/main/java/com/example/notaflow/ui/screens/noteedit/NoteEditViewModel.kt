package com.example.notaflow.ui.screens.noteedit

import androidx.compose.ui.graphics.Color
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
import com.example.notaflow.ui.components.RichTextFormatAction
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

    // Ensure the key "noteId" matches what you use in your Navigation graph
    private val noteIdFromNav: Long? = savedStateHandle.get<Long>("noteId")?.takeIf { it != -1L && it != 0L }


    private val contentHistory = mutableListOf<TextFieldValue>()
    private var currentHistoryIndex = -1
    private val maxHistorySize = 50

    init {
        if (noteIdFromNav != null) {
            _state.update { it.copy(isNewNote = false) }
            loadNote(noteIdFromNav)
        } else {
            val initialContent = TextFieldValue("")
            addToHistory(initialContent)
            _state.update {
                it.copy(
                    content = initialContent,
                    isNewNote = true
                    // noteColorHex is already initialized in NoteEditState
                )
            }
        }
    }

    private fun loadNote(id: Long) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                // Assuming GetNoteByIdUseCase takes Long, or Int if Note ID is Int
                // If Note ID is Int, getNoteByIdUseCase param should be Int, and id should be cast
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
                            isRichText = true
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

    // This is for cases where an external component might feed raw HTML/Markdown
    // If your RichTextEditor directly uses TextFieldValue, this might be less used.
    fun onRichTextContentChangeViaRaw(rawContent: String) {
        val currentContentTfv = _state.value.content
        val newAnnotatedString = RichTextConverter.fromHtml(rawContent)

        if (currentContentTfv.annotatedString.text != newAnnotatedString.text ||
            currentContentTfv.annotatedString.spanStyles != newAnnotatedString.spanStyles ||
            currentContentTfv.annotatedString.paragraphStyles != newAnnotatedString.paragraphStyles) {

            val newContentTfvUpdated = currentContentTfv.copy(
                annotatedString = newAnnotatedString,
                selection = TextRange(newAnnotatedString.length)
            )
            onContentChange(newContentTfvUpdated) // Use the main onContentChange
        }
    }

    fun onColorSelect(colorHex: String) {
        _state.update { it.copy(noteColorHex = colorHex) }
    }

    fun setRichTextEnabled(enabled: Boolean) {
        _state.update { it.copy(isRichText = enabled) }
        if (!enabled) {
            val plainText = _state.value.content.annotatedString.text
            val newContent = TextFieldValue(plainText, selection = TextRange(plainText.length))
            _state.update { it.copy(content = newContent) }
            contentHistory.clear()
            addToHistory(newContent)
            updateCurrentFormatStyles(newContent)
        } else {
            addToHistory(_state.value.content) // Re-add to history if switching back to rich
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
                Color(android.graphics.Color.parseColor(currentState.noteColorHex))
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
                isBookmarked = currentState.isBookmarked,
                isDeleted = currentState.note.isDeleted, // Preserve existing delete state
                deletedTimestamp = currentState.note.deletedTimestamp // Preserve existing delete timestamp
            ) ?: Note(
                title = title,
                content = contentHtml,
                createdTimestamp = System.currentTimeMillis(),
                timestamp = System.currentTimeMillis(),
                color = colorIndex,
                isPinned = currentState.isPinned,
                isBookmarked = currentState.isBookmarked,
                isDeleted = false, // New notes are not deleted
                deletedTimestamp = null
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

        val activeRange = if (selection.collapsed) TextRange(selection.start, selection.start +1) else selection

        annotatedString.spanStyles.filter { it.intersect(activeRange).length > 0 }.forEach {
            if (it.item.fontWeight == FontWeight.Bold) currentStyles.add("BOLD")
            if (it.item.fontStyle == FontStyle.Italic) currentStyles.add("ITALIC")
            it.item.textDecoration?.let { deco ->
                if (deco.contains(TextDecoration.Underline)) currentStyles.add("UNDERLINE")
                if (deco.contains(TextDecoration.LineThrough)) currentStyles.add("STRIKETHROUGH")
            }
            if (annotatedString.getStringAnnotations("URL", it.start, it.end).any { ann -> ann.intersect(activeRange).length > 0}) currentStyles.add("LINK")
        }
        annotatedString.paragraphStyles.filter { it.intersect(activeRange).length > 0 }.forEach {
            if (it.item.textIndent == TextIndent(16.sp, 16.sp)) currentStyles.add("BLOCKQUOTE")
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
            RichTextFormatAction.COLOR -> { /* Handled by color picker directly */ }
            RichTextFormatAction.UNDO -> undo()
            RichTextFormatAction.REDO -> redo()
        }
    }

    fun applyTextColor(color: Color) {
        val currentContent = _state.value.content
        val selection = currentContent.selection
        if (selection.collapsed && currentContent.composition == null) {
            _state.update { it.copy(currentSelectedTextColor = color) }
            return
        }
        val newTextFieldValue = RichTextFormatter.formatColor(currentContent, color)
        onContentChange(newTextFieldValue)
        _state.update { it.copy(currentSelectedTextColor = color) }
    }

    fun applyLink(url: String, text: String?) {
        val currentContent = _state.value.content
        val selection = currentContent.selection
        val linkTextToShow = if (text.isNullOrEmpty()) url else text
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
        onContentChange(RichTextFormatter.toggleStyle(_state.value.content, styleType))
    }

    private fun toggleParagraphStyle(styleType: RichTextFormatter.ParagraphStyleType) {
        onContentChange(RichTextFormatter.toggleParagraphStyle(_state.value.content, styleType))
    }

    private fun addToHistory(value: TextFieldValue) {
        if (currentHistoryIndex < contentHistory.size - 1) {
            contentHistory.subList(currentHistoryIndex + 1, contentHistory.size).clear()
        }
        contentHistory.add(value)
        if (contentHistory.size > maxHistorySize) {
            contentHistory.removeAt(0)
        }
        currentHistoryIndex = contentHistory.size - 1
        updateUndoRedoState()
    }

    private fun undo() {
        if (canUndo()) {
            currentHistoryIndex--
            val previousStateContent = contentHistory[currentHistoryIndex]
            _state.update { it.copy(content = previousStateContent) }
            updateUndoRedoState()
            updateCurrentFormatStyles(previousStateContent)
        }
    }

    private fun redo() {
        if (canRedo()) {
            currentHistoryIndex++
            val nextStateContent = contentHistory[currentHistoryIndex]
            _state.update { it.copy(content = nextStateContent) }
            updateUndoRedoState()
            updateCurrentFormatStyles(nextStateContent)
        }
    }

    private fun canUndo(): Boolean = currentHistoryIndex > 0
    private fun canRedo(): Boolean = currentHistoryIndex < contentHistory.size - 1

    private fun updateUndoRedoState() {
        _state.update { it.copy(canUndo = canUndo(), canRedo = canRedo()) }
    }
}
