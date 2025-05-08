// data/local/dao/NoteDao.kt
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
import java.util.Date

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes WHERE isDeleted = 0 ORDER BY modifiedAt DESC")
    fun getAllActiveNotes(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE id = :noteId")
    suspend fun getNoteById(noteId: Long): Note?

    @Query("SELECT * FROM notes WHERE isDeleted = 0 AND (title LIKE '%' || :searchQuery || '%' OR content LIKE '%' || :searchQuery || '%')")
    fun searchNotes(searchQuery: String): Flow<List<Note>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: Note): Long

    @Update
    suspend fun updateNote(note: Note)

    @Delete
    suspend fun hardDeleteNote(note: Note)

    // New methods for soft delete

    @Query("UPDATE notes SET isDeleted = 1, deletedAt = :timestamp WHERE id = :noteId")
    suspend fun softDeleteNote(noteId: Long, timestamp: Date = Date())

    @Query("UPDATE notes SET isDeleted = 0, deletedAt = NULL WHERE id = :noteId")
    suspend fun restoreNote(noteId: Long)

    // Get recently deleted notes (useful for trash bin feature later)
    @Query("SELECT * FROM notes WHERE isDeleted = 1 ORDER BY deletedAt DESC")
    fun getDeletedNotes(): Flow<List<Note>>

    // Permanently delete notes that have been in trash for more than 30 days
    @Query("DELETE FROM notes WHERE isDeleted = 1 AND deletedAt < :cutoffDate")
    suspend fun cleanupOldDeletedNotes(cutoffDate: Date)

    // Transaction to ensure atomicity
    @Transaction
    suspend fun safeRestoreNote(noteId: Long) {
        restoreNote(noteId)
    }
}