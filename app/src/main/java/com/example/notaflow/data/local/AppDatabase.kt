package com.example.notaflow.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.notaflow.data.local.dao.NoteDao
import com.example.notaflow.data.local.entity.Note
import com.example.notaflow.utils.DateConverter

// Increase version number from 1 to 2
@Database(entities = [Note::class], version = 2, exportSchema = true)
@TypeConverters(DateConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao

    companion object {
        // Migration from version 1 to 2
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add the new columns to the notes table
                database.execSQL("ALTER TABLE notes ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE notes ADD COLUMN deletedAt INTEGER DEFAULT NULL")
            }
        }

        // Factory method to create the database with migrations
        fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "nota_flow_db"
            )
                .addMigrations(MIGRATION_1_2)
                .build()
        }
    }
}