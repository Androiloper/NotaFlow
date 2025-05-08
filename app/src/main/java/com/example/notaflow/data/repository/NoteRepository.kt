package com.example.notaflow.data.repository

import com.example.notaflow.data.local.entity.Note
import kotlinx.coroutines.flow.Flow

interface NoteRepository {
    fun getAllNotes(): Flow<List<Note>>

    suspend fun getNoteById(id: Long): Note?

    fun searchNotes(query: String): Flow<List<Note>>

    suspend fun insertNote(note: Note): Long

    suspend fun updateNote(note: Note)

    suspend fun deleteNote(note: Note)
}