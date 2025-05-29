package com.example.notaflow.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.compose.ui.graphics.Color
import java.util.Date // Keep if you use Date elsewhere, not directly used in this snippet

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val content: String, // Stores rich text content (HTML)
    val createdTimestamp: Long = System.currentTimeMillis(),
    val timestamp: Long = System.currentTimeMillis(), // Represents last modified time
    val color: Int = 0, // Index for noteColors list
    val isPinned: Boolean = false,
    val isBookmarked: Boolean = false,
    val isDeleted: Boolean = false,
    val folderId: Int? = null
) {
    companion object {
        val noteColors = listOf(
            Color(0xFF90CAF9), // Default Blue - Placeholder for Theme's Primary Container in UI
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

        // Helper to find index of a given color, useful for saving.
        // Returns 0 (default index) if color not found.
        fun indexOfColor(colorToFind: Color): Int {
            val index = noteColors.indexOf(colorToFind)
            return if (index != -1) index else 0
        }
    }
}
