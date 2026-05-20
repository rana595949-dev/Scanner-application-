package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [Folder::class, Document::class, Page::class],
    version = 1,
    exportSchema = false
)
abstract class ScannerDatabase : RoomDatabase() {
    abstract fun folderDao(): FolderDao
    abstract fun documentDao(): DocumentDao
    abstract fun pageDao(): PageDao

    companion object {
        @Volatile
        private var INSTANCE: ScannerDatabase? = null

        fun getDatabase(context: Context): ScannerDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ScannerDatabase::class.java,
                    "scanner_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
