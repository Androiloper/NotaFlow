package com.example.notaflow.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.notaflow.data.local.entity.Note
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes WHERE isDeleted = 0 ORDER BY timestamp DESC")
    fun getAllActiveNotes(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE id = :noteId")
    suspend fun getNoteById(noteId: Long): Note? // If Note ID is Int, change param to Int

    @Query("SELECT * FROM notes WHERE isDeleted = 0 AND (title LIKE '%' || :searchQuery || '%' OR content LIKE '%' || :searchQuery || '%') ORDER BY timestamp DESC")
    fun searchNotes(searchQuery: String): Flow<List<Note>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: Note): Long

    @Update
    suspend fun updateNote(note: Note)

    @Delete
    suspend fun hardDeleteNote(note: Note)

    @Query("UPDATE notes SET isDeleted = 1, deletedTimestamp = :timestamp WHERE id = :noteId")
    suspend fun softDeleteNote(noteId: Long, timestamp: Long = System.currentTimeMillis()) // If Note ID is Int, change param

    @Query("UPDATE notes SET isDeleted = 0, deletedTimestamp = NULL WHERE id = :noteId")
    suspend fun restoreNote(noteId: Long) // If Note ID is Int, change param

    @Query("SELECT * FROM notes WHERE isDeleted = 1 AND deletedTimestamp IS NOT NULL ORDER BY deletedTimestamp DESC")
    fun getDeletedNotes(): Flow<List<Note>>

    @Query("DELETE FROM notes WHERE isDeleted = 1 AND deletedTimestamp IS NOT NULL AND deletedTimestamp < :cutoffTimestamp")
    suspend fun cleanupOldDeletedNotes(cutoffTimestamp: Long)

    @Transaction
    suspend fun safeRestoreNote(noteId: Long) { // If Note ID is Int, change param
        val note = getNoteById(noteId)
        note?.let {
            val restoredNote = it.copy(
                isDeleted = false,
                deletedTimestamp = null,
                timestamp = System.currentTimeMillis()
            )
            updateNote(restoredNote)
        }
    }

    @Query("SELECT * FROM notes ORDER BY timestamp DESC")
    fun getAllNotesIncludingDeleted(): Flow<List<Note>>

    @Query("SELECT COUNT(*) FROM notes WHERE isDeleted = 0")
    fun countActiveNotes(): Flow<Int>
}
