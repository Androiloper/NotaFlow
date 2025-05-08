package com.example.notaflow.data.repository

import android.util.Log
import com.example.notaflow.data.local.dao.NoteDao
import com.example.notaflow.data.local.entity.Note
import kotlinx.coroutines.flow.Flow
import java.util.Calendar
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoteRepositoryImpl @Inject constructor(
    private val noteDao: NoteDao
) : NoteRepository {

    override fun getAllActiveNotes(): Flow<List<Note>> = noteDao.getAllActiveNotes()

    override suspend fun getNoteById(id: Long): Note? = noteDao.getNoteById(id)

    override fun searchNotes(query: String): Flow<List<Note>> = noteDao.searchNotes(query)

    override suspend fun insertNote(note: Note): Long {
        val noteToInsert = note.copy(isDeleted = false, deletedAt = null)
        return noteDao.insertNote(noteToInsert)
    }

    override suspend fun updateNote(note: Note) {
        val noteToUpdate = note.copy(isDeleted = false, deletedAt = null)
        noteDao.updateNote(noteToUpdate)
    }

    override suspend fun hardDeleteNote(note: Note) = noteDao.hardDeleteNote(note)

    override suspend fun softDeleteNote(noteId: Long) {
        Log.d("NotaFlow", "Repository: Soft deleting note ID $noteId")
        noteDao.softDeleteNote(noteId, Date())
        Log.d("NotaFlow", "Repository: Note ID $noteId marked as deleted")
    }

    override suspend fun restoreNote(noteId: Long) {
        Log.d("NotaFlow", "Repository: Restoring note ID $noteId")
        try {
            noteDao.safeRestoreNote(noteId)
            Log.d("NotaFlow", "Repository: Note ID $noteId restored successfully")
        } catch (e: Exception) {
            Log.e("NotaFlow", "Repository: Error restoring note ID $noteId", e)
            throw e
        }
    }

    override fun getDeletedNotes(): Flow<List<Note>> = noteDao.getDeletedNotes()

    override suspend fun cleanupOldDeletedNotes(daysToKeep: Int) {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -daysToKeep)
        val cutoffDate = calendar.time
        noteDao.cleanupOldDeletedNotes(cutoffDate)
    }
}