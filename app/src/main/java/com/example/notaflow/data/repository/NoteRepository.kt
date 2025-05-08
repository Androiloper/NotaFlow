package com.example.notaflow.data.repository

import com.example.notaflow.data.local.entity.Note
import kotlinx.coroutines.flow.Flow
import java.util.Date

interface NoteRepository {
    fun getAllActiveNotes(): Flow<List<Note>>

    suspend fun getNoteById(id: Long): Note?

    fun searchNotes(query: String): Flow<List<Note>>

    suspend fun insertNote(note: Note): Long

    suspend fun updateNote(note: Note)

    // Hard delete (completely removes from database)
    suspend fun hardDeleteNote(note: Note)

    // Soft delete (marks as deleted but keeps in database)
    suspend fun softDeleteNote(noteId: Long)

    // Restore a soft-deleted note
    suspend fun restoreNote(noteId: Long)

    // Get all notes that are in the "trash"
    fun getDeletedNotes(): Flow<List<Note>>

    // Clean up notes that have been in trash for too long
    suspend fun cleanupOldDeletedNotes(daysToKeep: Int = 30)
}