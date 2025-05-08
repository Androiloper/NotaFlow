package com.example.notaflow.domain.usecase

import android.util.Log
import com.example.notaflow.data.local.entity.Note
import com.example.notaflow.data.repository.NoteRepository
import javax.inject.Inject

class SoftDeleteNoteUseCase @Inject constructor(
    private val repository: NoteRepository
) {
    suspend operator fun invoke(note: Note) {
        Log.d("NotaFlow", "SoftDeleteNoteUseCase: Soft deleting note ID ${note.id}")
        repository.softDeleteNote(note.id)
    }
}