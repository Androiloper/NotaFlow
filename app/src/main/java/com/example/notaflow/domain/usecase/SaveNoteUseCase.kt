// domain/usecase/SaveNoteUseCase.kt
package com.example.notaflow.domain.usecase

import android.util.Log
import com.example.notaflow.data.local.entity.Note
import com.example.notaflow.data.repository.NoteRepository
import java.util.Date
import javax.inject.Inject

class SaveNoteUseCase @Inject constructor(
    private val repository: NoteRepository
) {
    suspend operator fun invoke(note: Note): Long {
        // Make a copy of the note with updated timestamps
        val noteToSave = if (note.id == 0L) {
            // New note - use current date for both timestamps
            note.copy(createdAt = Date(), modifiedAt = Date())
        } else {
            // Existing note - preserve creation date, update modified date
            note.copy(modifiedAt = Date())
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