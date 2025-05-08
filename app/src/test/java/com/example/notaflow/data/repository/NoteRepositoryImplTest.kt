package com.example.notaflow.data.repository

import com.example.notaflow.data.local.dao.NoteDao
import com.example.notaflow.data.local.entity.Note
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import java.util.Date

class NoteRepositoryImplTest {

    private lateinit var noteDao: NoteDao
    private lateinit var repository: NoteRepositoryImpl

    @Before
    fun setUp() {
        noteDao = mockk()
        repository = NoteRepositoryImpl(noteDao)
    }

    @Test
    fun `getAllNotes returns data from dao`() = runBlocking {
        // Given
        val mockNotes = listOf(
            Note(
                id = 1,
                title = "Test Note 1",
                content = "Test content 1",
                colorHex = "#FFFFFF",
                createdAt = Date(),
                modifiedAt = Date()
            )
        )

        coEvery { noteDao.getAllNotes() } returns flowOf(mockNotes)

        // When
        val result = repository.getAllNotes().first()

        // Then
        assertEquals(mockNotes, result)
    }

    @Test
    fun `getNoteById returns note from dao`() = runBlocking {
        // Given
        val noteId = 1L
        val mockNote = Note(
            id = noteId,
            title = "Test Note",
            content = "Test content",
            colorHex = "#FFFFFF",
            createdAt = Date(),
            modifiedAt = Date()
        )

        coEvery { noteDao.getNoteById(noteId) } returns mockNote

        // When
        val result = repository.getNoteById(noteId)

        // Then
        assertNotNull(result)
        assertEquals(mockNote, result)
    }

    @Test
    fun `searchNotes returns filtered notes from dao`() = runBlocking {
        // Given
        val query = "test"
        val mockNotes = listOf(
            Note(
                id = 1,
                title = "Test Note 1",
                content = "Test content 1",
                colorHex = "#FFFFFF",
                createdAt = Date(),
                modifiedAt = Date()
            )
        )

        coEvery { noteDao.searchNotes(query) } returns flowOf(mockNotes)

        // When
        val result = repository.searchNotes(query).first()

        // Then
        assertEquals(mockNotes, result)
    }

    @Test
    fun `insertNote calls dao insertNote`() = runBlocking {
        // Given
        val note = Note(
            title = "Test Note",
            content = "Test content",
            colorHex = "#FFFFFF"
        )

        val expectedId = 101L
        coEvery { noteDao.insertNote(note) } returns expectedId

        // When
        val result = repository.insertNote(note)

        // Then
        assertEquals(expectedId, result)
        coVerify { noteDao.insertNote(note) }
    }

    @Test
    fun `updateNote calls dao updateNote`() = runBlocking {
        // Given
        val note = Note(
            id = 1,
            title = "Test Note",
            content = "Test content",
            colorHex = "#FFFFFF"
        )

        coEvery { noteDao.updateNote(note) } returns Unit

        // When
        repository.updateNote(note)

        // Then
        coVerify { noteDao.updateNote(note) }
    }

    @Test
    fun `deleteNote calls dao deleteNote`() = runBlocking {
        // Given
        val note = Note(
            id = 1,
            title = "Test Note",
            content = "Test content",
            colorHex = "#FFFFFF"
        )

        coEvery { noteDao.deleteNote(note) } returns Unit

        // When
        repository.deleteNote(note)

        // Then
        coVerify { noteDao.deleteNote(note) }
    }
}