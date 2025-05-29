// domain/usecase/SaveNoteUseCase.kt
package com.example.notaflow.domain.usecase

import android.util.Log
import com.example.notaflow.data.local.entity.Note
import com.example.notaflow.data.repository.NoteRepository
import javax.inject.Inject

class SaveNoteUseCase @Inject constructor(
    private val repository: NoteRepository
) {
    suspend operator fun invoke(note: Note): Long {
        // Make a copy of the note with updated timestamps
        val currentTime = System.currentTimeMillis()

        val noteToSave = if (note.id == 0L) {
            // New note - use current time for both timestamps
            note.copy(
                createdTimestamp = currentTime,
                timestamp = currentTime
            )
        } else {
            // Existing note - preserve creation timestamp, update modification timestamp
            note.copy(timestamp = currentTime)
        }

        // Log the operation for debugging
        Log.d("NotaFlow", "SaveNoteUseCase: Saving note ID=${noteToSave.id}, " +
                "title='${noteToSave.title}', isNew=${note.id == 0L}")

        return if (note.id == 0L) {
            // New note - insert and return new ID
            val newId = repository.insertNote(noteToSave)
            Log.d("NotaFlow", "SaveNoteUseCase: Inserted new note with ID=$newId")
            newId
        } else {
            // Existing note - update and return existing ID
            repository.updateNote(noteToSave)
            Log.d("NotaFlow", "SaveNoteUseCase: Updated existing note with ID=${note.id}")
            note.id
        }
    }
}