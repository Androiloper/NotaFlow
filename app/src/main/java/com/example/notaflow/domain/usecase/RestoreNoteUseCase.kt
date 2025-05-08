package com.example.notaflow.domain.usecase

import android.util.Log
import com.example.notaflow.data.repository.NoteRepository
import javax.inject.Inject

class RestoreNoteUseCase @Inject constructor(
    private val repository: NoteRepository
) {
    suspend operator fun invoke(noteId: Long) {
        Log.d("NotaFlow", "RestoreNoteUseCase: Restoring note ID $noteId")
        repository.restoreNote(noteId)
    }
}