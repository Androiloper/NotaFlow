package com.example.notaflow.ui.screens.noteedit

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.TextFieldValue
import com.example.notaflow.data.local.entity.Note

// Extension function to convert Color to Hex String (RRGGBB for simplicity, no alpha)
fun Color.toHexString(): String {
    val red = (this.red * 255).toInt()
    val green = (this.green * 255).toInt()
    val blue = (this.blue * 255).toInt()
    return String.format("#%02X%02X%02X", red, green, blue)
}

data class NoteEditState(
    val note: Note? = null,
    val title: TextFieldValue = TextFieldValue(""),
    val content: TextFieldValue = TextFieldValue(""),
    val noteColorHex: String = Note.noteColors.first().toHexString(), // Initialize with default
    val isPinned: Boolean = false,
    val isBookmarked: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val showColorPicker: Boolean = false,
    val showLinkDialog: Boolean = false,
    val currentLinkUrl: String = "",
    val currentLinkText: String = "",
    val currentSelectedTextColor: Color = Color.Unspecified,
    val currentFormatStyles: Set<String> = emptySet(),
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val isRichText: Boolean = true,
    val saveCompleted: Boolean = false,
    val isNewNote: Boolean = true
)