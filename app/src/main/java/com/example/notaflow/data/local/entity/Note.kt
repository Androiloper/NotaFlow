package com.example.notaflow.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.compose.ui.graphics.Color // Keep for companion object if colors are defined here

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0, // Assuming your DAO getNoteById uses Long, ensure ID type matches or cast in DAO
    val title: String,
    val content: String, // Stores rich text content (HTML)
    val createdTimestamp: Long = System.currentTimeMillis(),
    val timestamp: Long = System.currentTimeMillis(), // Represents last modification time
    val color: Int = 0, // Index for noteColors list
    val isPinned: Boolean = false,
    val isBookmarked: Boolean = false,
    val isDeleted: Boolean = false,      // For soft delete flag
    val deletedTimestamp: Long? = null,  // Timestamp for when it was soft-deleted
    val folderId: Int? = null
) {
    companion object {
        // Example static colors, if you manage them here.
        // Ensure this list aligns with how NoteEditState gets its default hex color.
        val noteColors = listOf(
            Color(0xFF90CAF9), // Default Blue
            Color(0xFFF48FB1), // Pink
            Color(0xFF80CBC4), // Teal
            Color(0xFFFFF176), // Yellow
            Color(0xFFCE93D8), // Purple
            Color(0xFF4FC3F7), // Light Blue
            Color(0xFFA5D6A7), // Green
            Color(0xFFFFAB91)  // Orange
        )

        fun getColorByIndex(index: Int): Color {
            return noteColors.getOrElse(index) { noteColors.first() }
        }

        fun indexOfColor(colorToFind: Color): Int {
            val index = noteColors.indexOf(colorToFind)
            return if (index != -1) index else 0 // Default to first color's index if not found
        }
    }
}
