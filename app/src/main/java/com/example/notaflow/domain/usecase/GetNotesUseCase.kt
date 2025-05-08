package com.example.notaflow.domain.usecase

import com.example.notaflow.data.local.entity.Note
import com.example.notaflow.data.repository.NoteRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetNotesUseCase @Inject constructor(
    private val repository: NoteRepository
) {
    operator fun invoke(): Flow<List<Note>> {
        // Only returns active (non-deleted) notes
        return repository.getAllActiveNotes()
    }
}