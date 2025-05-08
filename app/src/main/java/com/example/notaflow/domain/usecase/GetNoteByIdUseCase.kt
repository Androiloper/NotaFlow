package com.example.notaflow.domain.usecase

import com.example.notaflow.data.local.entity.Note
import com.example.notaflow.data.repository.NoteRepository
import javax.inject.Inject

class GetNoteByIdUseCase @Inject constructor(
    private val repository: NoteRepository
) {
    suspend operator fun invoke(id: Long): Note? {
        return repository.getNoteById(id)
    }
}