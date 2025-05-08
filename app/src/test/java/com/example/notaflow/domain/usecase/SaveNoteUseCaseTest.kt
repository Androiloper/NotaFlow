package com.example.notaflow.domain.usecase

import com.example.notaflow.data.local.entity.Note
import com.example.notaflow.data.repository.NoteRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.Date

class SaveNoteUseCaseTest {

    private lateinit var noteRepository: NoteRepository
    private lateinit var saveNoteUseCase: SaveNoteUseCase

    @Before
    fun setUp() {
        noteRepository = mockk(relaxed = true)
        saveNoteUseCase = SaveNoteUseCase(noteRepository)
    }

    @Test
    fun `invoke calls insertNote for new note`() = runBlocking {
        // Given
        val newNote = Note(
            id = 0, // Id of 0 indicates a new note
            title = "New Test Note",
            content = "New test content",
            colorHex = "#FFFFFF"
        )

        val expectedId = 101L
        coEvery { noteRepository.insertNote(any()) } returns expectedId

        // When
        val result = saveNoteUseCase(newNote)

        // Then
        assertEquals(expectedId, result)
        coVerify { noteRepository.insertNote(any()) }
    }

    @Test
    fun `invoke calls updateNote for existing note`() = runBlocking {
        // Given
        val existingNote = Note(
            id = 42, // Non-zero id indicates existing note
            title = "Existing Test Note",
            content = "Existing test content",
            colorHex = "#FFCCCC"
        )

        // When
        val result = saveNoteUseCase(existingNote)

        // Then
        assertEquals(42L, result)
        coVerify { noteRepository.updateNote(any()) }
    }
}