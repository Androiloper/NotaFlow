package com.example.notaflow.data.local

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.notaflow.data.local.dao.NoteDao
import com.example.notaflow.data.local.entity.Note
import com.example.notaflow.utils.DateConverter

@Database(entities = [Note::class], version = 4, exportSchema = true)
@TypeConverters(DateConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao

    companion object {
        private const val TAG = "AppDatabase"

        // Migration from version 1 to 2
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE notes ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE notes ADD COLUMN deletedAt INTEGER DEFAULT NULL")
            }
        }

        // Migration from version 2 to 3
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add rich text columns to the notes table
                database.execSQL("ALTER TABLE notes ADD COLUMN isRichText INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE notes ADD COLUMN richTextContent TEXT NOT NULL DEFAULT ''")
            }
        }

        // Migration from version 3 to 4
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                try {
                    // Create temporary table with the new schema
                    database.execSQL("""
                        CREATE TABLE notes_temp (
                            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            title TEXT NOT NULL,
                            content TEXT NOT NULL,
                            createdTimestamp INTEGER NOT NULL,
                            timestamp INTEGER NOT NULL,
                            color INTEGER NOT NULL,
                            isPinned INTEGER NOT NULL DEFAULT 0,
                            isBookmarked INTEGER NOT NULL DEFAULT 0,
                            isDeleted INTEGER NOT NULL DEFAULT 0,
                            deletedTimestamp INTEGER,
                            folderId INTEGER,
                            isRichText INTEGER NOT NULL DEFAULT 0,
                            richTextContent TEXT NOT NULL DEFAULT ''
                        )
                    """)

                    // Copy data from old table to new table
                    // Handle column name differences explicitly
                    database.execSQL("""
                        INSERT INTO notes_temp (
                            id, title, content, createdTimestamp, timestamp,
                            color, isPinned, isBookmarked, isDeleted, deletedTimestamp,
                            folderId, isRichText, richTextContent
                        )
                        SELECT
                            id, title, content, createdAt, modifiedAt,
                            CASE WHEN colorHex = '#FFFFFF' THEN 0
                                 WHEN colorHex = '#F28B82' THEN 1
                                 WHEN colorHex = '#FBBC04' THEN 2
                                 WHEN colorHex = '#FFF475' THEN 3
                                 WHEN colorHex = '#CBFF90' THEN 4
                                 WHEN colorHex = '#A7FFEB' THEN 5
                                 WHEN colorHex = '#CAF0F8' THEN 6
                                 WHEN colorHex = '#D7AEFB' THEN 7
                                 WHEN colorHex = '#AFCBFA' THEN 8
                                 WHEN colorHex = '#E6C9A8' THEN 9
                                 ELSE 0 END,
                            0, 0, isDeleted, deletedAt,
                            folderId, isRichText, richTextContent
                        FROM notes
                    """)

                    // Drop the old table
                    database.execSQL("DROP TABLE notes")

                    // Rename the temporary table to the target table name
                    database.execSQL("ALTER TABLE notes_temp RENAME TO notes")

                    Log.d(TAG, "Migration 3->4 completed successfully")
                } catch (e: Exception) {
                    Log.e(TAG, "Error during migration 3->4: ${e.message}")

                    // If the first approach fails, try a more conservative migration
                    try {
                        // Create a completely new table with required schema
                        database.execSQL("""
                            CREATE TABLE IF NOT EXISTS notes_new (
                                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                title TEXT NOT NULL,
                                content TEXT NOT NULL,
                                createdTimestamp INTEGER NOT NULL,
                                timestamp INTEGER NOT NULL,
                                color INTEGER NOT NULL,
                                isPinned INTEGER NOT NULL DEFAULT 0,
                                isBookmarked INTEGER NOT NULL DEFAULT 0,
                                isDeleted INTEGER NOT NULL DEFAULT 0,
                                deletedTimestamp INTEGER,
                                folderId INTEGER
                            )
                        """)

                        // Drop the old table - since migration failed, we can't recover the data
                        database.execSQL("DROP TABLE IF EXISTS notes")

                        // Rename the new table
                        database.execSQL("ALTER TABLE notes_new RENAME TO notes")

                        Log.d(TAG, "Migration 3->4 completed with fallback strategy (data loss)")
                    } catch (e2: Exception) {
                        Log.e(TAG, "Fatal error during migration fallback: ${e2.message}")
                        // At this point, we need manual intervention
                        throw RuntimeException("Database migration failed and recovery failed. Please clear app data.", e2)
                    }
                }
            }
        }

        // Factory method to create the database with migrations
        fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "nota_flow_db"
            )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                .fallbackToDestructiveMigration() // In case all migrations fail, reset database
                .build()
        }
    }
}