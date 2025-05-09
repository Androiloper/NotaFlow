package com.example.notaflow.ui.screens.noteedit

data class NoteEditState(
    val noteId: Long = 0,
    val title: String = "",
    val content: String = "",
    val colorHex: String = "#FFFFFF", // Default white color
    val isSaving: Boolean = false,
    val saveCompleted: Boolean = false,
    val error: String? = null,
    val isNewNote: Boolean = true,
    // New fields for rich text support
    val isRichText: Boolean = false,
    val richTextContent: String = ""
)