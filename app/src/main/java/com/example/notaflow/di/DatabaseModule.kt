package com.example.notaflow.di

import android.content.Context
import androidx.room.Room
import com.example.notaflow.data.local.AppDatabase
import com.example.notaflow.data.local.dao.NoteDao
import com.example.notaflow.data.repository.NoteRepository
import com.example.notaflow.data.repository.NoteRepositoryImpl
import com.example.notaflow.utils.RichTextConverter
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

    // Remove this method or fix it if RichTextConverter should be provided differently
    // The issue is that RichTextConverter is likely an object (singleton) and cannot be instantiated
    // If you need to provide RichTextConverter as a dependency, you'll need to reference it directly
    /*
    @Provides
    @Singleton
    fun provideRichTextConverter(): RichTextConverter {
        // Don't try to instantiate RichTextConverter if it's an object
        // Simply return the singleton instance
        return RichTextConverter
    }
    */

    @Provides
    @Singleton
    fun provideNoteRepository(noteDao: NoteDao): NoteRepository {
        return NoteRepositoryImpl(noteDao)
    }
}