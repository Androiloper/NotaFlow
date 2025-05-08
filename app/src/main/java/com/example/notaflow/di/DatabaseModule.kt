package com.example.notaflow.di

import android.content.Context
import androidx.room.Room
import com.example.notaflow.data.local.AppDatabase
import com.example.notaflow.data.local.dao.NoteDao
import com.example.notaflow.data.repository.NoteRepository
import com.example.notaflow.data.repository.NoteRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        // Use the factory method with migrations instead of direct builder
        return AppDatabase.buildDatabase(context)
    }

    @Provides
    fun provideNoteDao(appDatabase: AppDatabase): NoteDao {
        return appDatabase.noteDao()
    }

    @Provides
    @Singleton
    fun provideNoteRepository(noteDao: NoteDao): NoteRepository {
        return NoteRepositoryImpl(noteDao)
    }
}