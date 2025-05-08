// di/DomainModule.kt
package com.example.notaflow.di

import com.example.notaflow.data.repository.NoteRepository
import com.example.notaflow.domain.usecase.DeleteNoteUseCase
import com.example.notaflow.domain.usecase.GetNoteByIdUseCase
import com.example.notaflow.domain.usecase.GetNotesUseCase
import com.example.notaflow.domain.usecase.RestoreNoteUseCase
import com.example.notaflow.domain.usecase.SaveNoteUseCase
import com.example.notaflow.domain.usecase.SearchNotesUseCase
import com.example.notaflow.domain.usecase.SoftDeleteNoteUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object DomainModule {

    @Provides
    fun provideGetNotesUseCase(repository: NoteRepository): GetNotesUseCase {
        return GetNotesUseCase(repository)
    }

    @Provides
    fun provideGetNoteByIdUseCase(repository: NoteRepository): GetNoteByIdUseCase {
        return GetNoteByIdUseCase(repository)
    }

    @Provides
    fun provideSearchNotesUseCase(repository: NoteRepository): SearchNotesUseCase {
        return SearchNotesUseCase(repository)
    }

    @Provides
    fun provideSaveNoteUseCase(repository: NoteRepository): SaveNoteUseCase {
        return SaveNoteUseCase(repository)
    }

    @Provides
    fun provideDeleteNoteUseCase(repository: NoteRepository): DeleteNoteUseCase {
        return DeleteNoteUseCase(repository)
    }

    @Provides
    fun provideSoftDeleteNoteUseCase(repository: NoteRepository): SoftDeleteNoteUseCase {
        return SoftDeleteNoteUseCase(repository)
    }

    @Provides
    fun provideRestoreNoteUseCase(repository: NoteRepository): RestoreNoteUseCase {
        return RestoreNoteUseCase(repository)
    }
}