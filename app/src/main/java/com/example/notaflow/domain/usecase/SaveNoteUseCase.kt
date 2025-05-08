package com.example.notaflow.domain.usecase

import com.example.notaflow.data.local.entity.Note
import com.example.notaflow.data.repository.NoteRepository
import java.util.Date
import javax.inject.Inject

class SaveNoteUseCase @Inject constructor(
    private val repository: NoteRepository
) {
    suspend operator fun invoke(note: Note): Long {
        // Update the modified timestamp
        val noteToSave = note.copy(modifiedAt = Date())

        return if (note.id == 0L) {
            // New note
            repository.insertNote(noteToSave)
        } else {
            // Existing note
            repository.updateNote(noteToSave)
            note.id
        }
    }
}