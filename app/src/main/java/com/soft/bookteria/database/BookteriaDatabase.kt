package com.soft.bookteria.database

import android.content.Context
import android.provider.SyncStateContract
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import com.soft.bookteria.database.library.LibraryDAO
import com.soft.bookteria.database.library.LibraryObject
import com.soft.bookteria.database.progress.ProgressDAO
import com.soft.bookteria.database.progress.ProgressData
import com.soft.bookteria.helpers.Constants
import com.soft.bookteria.helpers.Constants.DATABASE_NAME
const val DATABASE_VERSION = 5
@Database(
    entities = [LibraryObject::class, ProgressData::class],
    version = DATABASE_VERSION,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3),
        AutoMigration(from = 4, to = 5),
    ]
)
abstract class BookteriaDatabase : RoomDatabase() {
    abstract fun libraryDAO(): LibraryDAO
    abstract fun progressDAO(): ProgressDAO
    
    companion object {
        private val migrate = Migration(3, 4) { database ->
            database.execSQL("ALTER TABLE reader_table RENAME COLUMN book_id TO library_item_id")
        }
        @Volatile
        private var INSTANCE: BookteriaDatabase? = null
        fun getInstance(context: Context): BookteriaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BookteriaDatabase::class.java,
                    Constants.DATABASE_NAME
                )
                    .addMigrations(migrate)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}