package com.example.notaflow.domain.usecase

import com.example.notaflow.data.local.entity.Note
import com.example.notaflow.data.repository.NoteRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.Date

class GetNotesUseCaseTest {

    private lateinit var noteRepository: NoteRepository
    private lateinit var getNotesUseCase: GetNotesUseCase

    @Before
    fun setUp() {
        noteRepository = mockk()
        getNotesUseCase = GetNotesUseCase(noteRepository)
    }

    @Test
    fun `invoke returns notes from repository`() = runBlocking {
        // Given
        val mockNotes = listOf(
            Note(
                id = 1,
                title = "Test Note 1",
                content = "Test content 1",
                colorHex = "#FFFFFF",
                createdAt = Date(),
                modifiedAt = Date()
            ),
            Note(
                id = 2,
                title = "Test Note 2",
                content = "Test content 2",
                colorHex = "#FFCCCC",
                createdAt = Date(),
                modifiedAt = Date()
            )
        )

        coEvery { noteRepository.getAllNotes() } returns flowOf(mockNotes)

        // When
        val result = getNotesUseCase().first()

        // Then
        assertEquals(mockNotes, result)
        assertEquals(2, result.size)
        assertEquals("Test Note 1", result[0].title)
        assertEquals("Test Note 2", result[1].title)
    }
}