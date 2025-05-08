// data/local/entity/Note.kt
package com.example.notaflow.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val content: String,
    val colorHex: String = "#FFFFFF", // Default white color
    val createdAt: Date = Date(),
    val modifiedAt: Date = Date(),
    // New field for soft delete
    val isDeleted: Boolean = false,
    // Timestamp for when the note was marked as deleted
    val deletedAt: Date? = null
)