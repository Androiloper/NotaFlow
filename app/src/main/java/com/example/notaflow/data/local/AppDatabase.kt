package com.example.notaflow.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.notaflow.data.local.dao.NoteDao
import com.example.notaflow.data.local.entity.Note
import com.example.notaflow.utils.DateConverter

@Database(entities = [Note::class], version = 1, exportSchema = true)
@TypeConverters(DateConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
}