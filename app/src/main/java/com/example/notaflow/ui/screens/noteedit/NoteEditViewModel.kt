package com.example.notaflow.ui.screens.noteedit

import android.util.Log
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

@HiltViewModel
class NoteEditViewModel @Inject constructor(
    private val getNoteByIdUseCase: GetNoteByIdUseCase,
    private val saveNoteUseCase: SaveNoteUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _state = MutableStateFlow(NoteEditState())
    val state: StateFlow<NoteEditState> = _state.asStateFlow()

    private val noteIdFromNav: Long? = savedStateHandle.get<Long>("noteId")?.takeIf { it != -1L && it != 0L }

    // Enhanced history management for undo/redo
    private val contentHistory = mutableListOf<TextFieldValue>()
    private var currentHistoryIndex = -1
    private val maxHistorySize = 100
    private var isUpdatingFromHistory = false

    // Track pending formatting for cursor position - this is key to fixing the disappearing formatting
    private var pendingCursorStyles = mutableSetOf<String>()
    private var pendingCursorColor: Color? = null
    private var lastKnownCursorPosition = 0

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
                )
            }
        }
    }

    private fun loadNote(id: Long) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                val note = getNoteByIdUseCase(id)
                if (note != null) {
                    val htmlContent = note.content
                    val annotatedString = RichTextConverter.fromHtml(htmlContent)
                    val contentFieldValue = TextFieldValue(
                        annotatedString = annotatedString,
                        selection = TextRange(annotatedString.text.length)
                    )

                    val colorHex = Note.getColorHexByIndex(note.color)

                    _state.update {
                        it.copy(
                            note = note,
                            title = TextFieldValue(note.title),
                            content = contentFieldValue,
                            noteColorHex = colorHex,
                            isPinned = note.isPinned,
                            isBookmarked = note.isBookmarked,
                            isLoading = false,
                            isRichText = true
                        )
                    }
                    contentHistory.clear()
                    addToHistory(contentFieldValue)
                    updateCurrentFormatStyles(contentFieldValue)
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

    /**
     * Enhanced content change handling - KEY FIX for disappearing formatting
     */
    fun onContentChange(newContent: TextFieldValue) {
        if (isUpdatingFromHistory) return

        val currentContent = _state.value.content
        lastKnownCursorPosition = newContent.selection.start

        // Check if we should apply pending styles to newly typed content
        val processedContent = if (shouldApplyPendingStyles(currentContent, newContent)) {
            applyPendingStylesToNewContent(currentContent, newContent)
        } else {
            newContent
        }

        _state.update { it.copy(content = processedContent) }

        // Add to history only for significant changes (not just cursor movements)
        if (isSignificantChange(currentContent, processedContent)) {
            addToHistory(processedContent)
        }

        updateCurrentFormatStyles(processedContent)

        // Clear pending styles if they were applied
        if (processedContent != newContent) {
            clearPendingStylesIfApplied(currentContent, processedContent)
        }

        Log.d("NoteEditViewModel", "Content changed. Pending styles: $pendingCursorStyles")
    }

    /**
     * Check if we should apply pending cursor styles to new content
     */
    private fun shouldApplyPendingStyles(oldContent: TextFieldValue, newContent: TextFieldValue): Boolean {
        val hasNewText = newContent.text.length > oldContent.text.length
        val hasPendingStyles = pendingCursorStyles.isNotEmpty() || pendingCursorColor != null
        val atCursor = newContent.selection.collapsed

        return hasNewText && hasPendingStyles && atCursor
    }

    /**
     * Apply pending styles to newly typed text - CORE FIX
     */
    private fun applyPendingStylesToNewContent(oldContent: TextFieldValue, newContent: TextFieldValue): TextFieldValue {
        if (pendingCursorStyles.isEmpty() && pendingCursorColor == null) {
            return newContent
        }

        val insertionStart = oldContent.selection.start
        val insertionEnd = newContent.selection.start

        if (insertionStart >= insertionEnd) return newContent

        var processedContent = newContent
        Log.d("NoteEditViewModel", "Applying pending styles from $insertionStart to $insertionEnd")

        // Apply each pending style
        pendingCursorStyles.forEach { styleString ->
            val styleType = when (styleString) {
                "BOLD" -> RichTextFormatter.StyleType.BOLD
                "ITALIC" -> RichTextFormatter.StyleType.ITALIC
                "UNDERLINE" -> RichTextFormatter.StyleType.UNDERLINE
                "STRIKETHROUGH" -> RichTextFormatter.StyleType.STRIKETHROUGH
                else -> return@forEach
            }

            // Create temporary selection for the new text
            val tempSelection = TextRange(insertionStart, insertionEnd)
            val tempContent = processedContent.copy(selection = tempSelection)
            processedContent = RichTextFormatter.toggleStyle(tempContent, styleType)
                .copy(selection = newContent.selection) // Restore original cursor position
        }

        // Apply pending color
        pendingCursorColor?.let { color ->
            val tempSelection = TextRange(insertionStart, insertionEnd)
            val tempContent = processedContent.copy(selection = tempSelection)
            processedContent = RichTextFormatter.formatColor(tempContent, color)
                .copy(selection = newContent.selection)
        }

        return processedContent
    }

    /**
     * Clear pending styles after they've been applied
     */
    private fun clearPendingStylesIfApplied(oldContent: TextFieldValue, newContent: TextFieldValue) {
        if (newContent.text.length > oldContent.text.length) {
            // Text was added, so pending styles were likely applied
            Log.d("NoteEditViewModel", "Clearing pending styles after application")
            // Don't clear immediately - keep for next character
            // This allows continuous formatting
        }
    }

    /**
     * Determine if a content change is significant enough for history
     */
    private fun isSignificantChange(oldContent: TextFieldValue, newContent: TextFieldValue): Boolean {
        val textChanged = oldContent.text != newContent.text
        val formattingChanged = oldContent.annotatedString != newContent.annotatedString
        val lengthDifference = Math.abs(oldContent.text.length - newContent.text.length)

        return textChanged || formattingChanged || lengthDifference > 1
    }

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
            onContentChange(newContentTfvUpdated)
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
            // Clear pending styles when switching to plain text
            pendingCursorStyles.clear()
            pendingCursorColor = null
        } else {
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
                isDeleted = currentState.note.isDeleted,
                deletedTimestamp = currentState.note.deletedTimestamp
            ) ?: Note(
                title = title,
                content = contentHtml,
                createdTimestamp = System.currentTimeMillis(),
                timestamp = System.currentTimeMillis(),
                color = colorIndex,
                isPinned = currentState.isPinned,
                isBookmarked = currentState.isBookmarked,
                isDeleted = false,
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

    /**
     * Enhanced format style detection - CRITICAL for maintaining formatting state
     */
    private fun updateCurrentFormatStyles(contentValue: TextFieldValue) {
        val selection = contentValue.selection
        val currentStyles = mutableSetOf<String>()
        val annotatedString = contentValue.annotatedString

        if (annotatedString.text.isEmpty()) {
            // Use pending styles for empty content
            currentStyles.addAll(pendingCursorStyles)
            val color = pendingCursorColor ?: Color.Unspecified
            _state.update {
                it.copy(
                    currentFormatStyles = currentStyles,
                    currentSelectedTextColor = color
                )
            }
            return
        }

        val activeRange = when {
            selection.collapsed -> {
                // For cursor position, check styles at or before cursor
                val pos = selection.start.coerceAtMost(annotatedString.text.length)
                if (pos > 0) {
                    // Check the character before cursor to inherit styles
                    TextRange(pos - 1, pos)
                } else {
                    TextRange(pos, (pos + 1).coerceAtMost(annotatedString.text.length))
                }
            }
            else -> selection
        }

        var activeColor = Color.Unspecified

        // Analyze span styles in the active range
        annotatedString.spanStyles.filter { spanStyle ->
            spanStyle.start < activeRange.end && spanStyle.end > activeRange.start
        }.forEach { spanStyle ->
            val item = spanStyle.item

            // Check each formatting type
            if (item.fontWeight == FontWeight.Bold) {
                currentStyles.add("BOLD")
            }
            if (item.fontStyle == FontStyle.Italic) {
                currentStyles.add("ITALIC")
            }

            item.textDecoration?.let { decoration ->
                if (decoration.contains(TextDecoration.Underline)) {
                    currentStyles.add("UNDERLINE")
                }
                if (decoration.contains(TextDecoration.LineThrough)) {
                    currentStyles.add("STRIKETHROUGH")
                }
            }

            // Track the most recent color
            if (item.color != Color.Unspecified) {
                activeColor = item.color
            }
        }

        // Check for URL annotations (links)
        val urlAnnotations = annotatedString.getStringAnnotations("URL", activeRange.start, activeRange.end)
        if (urlAnnotations.isNotEmpty()) {
            currentStyles.add("LINK")
        }

        // Check paragraph styles for blockquotes
        annotatedString.paragraphStyles.filter { paraStyle ->
            paraStyle.start < activeRange.end && paraStyle.end > activeRange.start
        }.forEach { paraStyle ->
            if (paraStyle.item.textIndent == TextIndent(16.sp, 16.sp)) {
                currentStyles.add("BLOCKQUOTE")
            }
        }

        // Update pending styles for cursor - CRITICAL for next character formatting
        if (selection.collapsed) {
            pendingCursorStyles.clear()
            pendingCursorStyles.addAll(currentStyles)
            if (activeColor != Color.Unspecified) {
                pendingCursorColor = activeColor
            }

            Log.d("NoteEditViewModel", "Updated pending styles at cursor: $pendingCursorStyles")
        }

        _state.update {
            it.copy(
                currentFormatStyles = currentStyles,
                currentSelectedTextColor = activeColor
            )
        }
    }

    fun handleFormatAction(action: RichTextFormatAction) {
        when (action) {
            RichTextFormatAction.BOLD -> toggleStyle(RichTextFormatter.StyleType.BOLD)
            RichTextFormatAction.ITALIC -> toggleStyle(RichTextFormatter.StyleType.ITALIC)
            RichTextFormatAction.UNDERLINE -> toggleStyle(RichTextFormatter.StyleType.UNDERLINE)
            RichTextFormatAction.STRIKETHROUGH -> toggleStyle(RichTextFormatter.StyleType.STRIKETHROUGH)
            RichTextFormatAction.BLOCKQUOTE -> toggleParagraphStyle(RichTextFormatter.ParagraphStyleType.BLOCKQUOTE)
            RichTextFormatAction.LINK -> {
                _state.update { it.copy(showLinkDialog = true) }
            }
            RichTextFormatAction.COLOR -> {
                // Color handling is done via setTextColor method
            }
            RichTextFormatAction.UNDO -> undo()
            RichTextFormatAction.REDO -> redo()
            RichTextFormatAction.HEADER1 -> TODO()
            RichTextFormatAction.HEADER2 -> TODO()
            RichTextFormatAction.LIST_BULLET -> TODO()
            RichTextFormatAction.LIST_NUMBERED -> TODO()
        }
    }

    /**
     * Enhanced text color setting with cursor support
     */
    fun setTextColor(color: Color) {
        val currentContent = _state.value.content
        val selection = currentContent.selection

        if (selection.collapsed) {
            // Set color for future typing at cursor
            pendingCursorColor = color
            _state.update { it.copy(currentSelectedTextColor = color) }
            Log.d("NoteEditViewModel", "Set pending cursor color: $color")
        } else {
            // Apply color to selected text immediately
            val newTextFieldValue = RichTextFormatter.formatColor(currentContent, color)
            onContentChange(newTextFieldValue)
            _state.update { it.copy(currentSelectedTextColor = color) }
        }
    }

    fun applyLink(url: String, text: String) {
        val currentContent = _state.value.content
        val selection = currentContent.selection
        val linkTextToShow = if (text.isBlank()) url else text
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

    /**
     * Enhanced style toggling with cursor position support
     */
    private fun toggleStyle(styleType: RichTextFormatter.StyleType) {
        val currentContent = _state.value.content
        val selection = currentContent.selection

        if (selection.collapsed) {
            // Handle cursor position formatting - update pending styles
            val styleKey = when (styleType) {
                RichTextFormatter.StyleType.BOLD -> "BOLD"
                RichTextFormatter.StyleType.ITALIC -> "ITALIC"
                RichTextFormatter.StyleType.UNDERLINE -> "UNDERLINE"
                RichTextFormatter.StyleType.STRIKETHROUGH -> "STRIKETHROUGH"
            }

            if (pendingCursorStyles.contains(styleKey)) {
                pendingCursorStyles.remove(styleKey)
                Log.d("NoteEditViewModel", "Removed $styleKey from pending styles")
            } else {
                pendingCursorStyles.add(styleKey)
                Log.d("NoteEditViewModel", "Added $styleKey to pending styles")
            }

            // Update the UI to reflect the pending change
            updateCurrentFormatStyles(currentContent)
        } else {
            // Apply formatting to selected text immediately
            val newContent = RichTextFormatter.toggleStyle(currentContent, styleType)
            onContentChange(newContent)
        }
    }

    private fun toggleParagraphStyle(styleType: RichTextFormatter.ParagraphStyleType) {
        val newContent = RichTextFormatter.toggleParagraphStyle(_state.value.content, styleType)
        onContentChange(newContent)
    }

    /**
     * Enhanced history management
     */
    private fun addToHistory(value: TextFieldValue) {
        if (isUpdatingFromHistory) return

        // Don't add if it's identical to current history item
        if (contentHistory.getOrNull(currentHistoryIndex)?.annotatedString == value.annotatedString) {
            return
        }

        // Remove future history if we're not at the end
        if (currentHistoryIndex < contentHistory.size - 1) {
            contentHistory.subList(currentHistoryIndex + 1, contentHistory.size).clear()
        }

        contentHistory.add(value)

        // Maintain history size limit
        if (contentHistory.size > maxHistorySize) {
            contentHistory.removeAt(0)
        }

        currentHistoryIndex = contentHistory.size - 1
        updateUndoRedoState()
    }

    private fun undo() {
        if (canUndo()) {
            isUpdatingFromHistory = true
            currentHistoryIndex--
            val previousContent = contentHistory[currentHistoryIndex]
            _state.update { it.copy(content = previousContent) }
            updateCurrentFormatStyles(previousContent)
            updateUndoRedoState()
            isUpdatingFromHistory = false
            Log.d("NoteEditViewModel", "Undo performed")
        }
    }

    private fun redo() {
        if (canRedo()) {
            isUpdatingFromHistory = true
            currentHistoryIndex++
            val nextContent = contentHistory[currentHistoryIndex]
            _state.update { it.copy(content = nextContent) }
            updateCurrentFormatStyles(nextContent)
            updateUndoRedoState()
            isUpdatingFromHistory = false
            Log.d("NoteEditViewModel", "Redo performed")
        }
    }

    private fun canUndo(): Boolean = currentHistoryIndex > 0
    private fun canRedo(): Boolean = currentHistoryIndex < contentHistory.size - 1

    private fun updateUndoRedoState() {
        _state.update { it.copy(canUndo = canUndo(), canRedo = canRedo()) }
    }
}