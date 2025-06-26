package com.soft.bookteria.database.library

import android.util.Log
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface LibraryDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(libraryObject: LibraryObject)

    @Delete
    fun delete(libraryObject: LibraryObject)
    
    @Query("SELECT * FROM book_library")
    fun getAll(): List<LibraryObject>
    
//    @Query("SELECT * FROM book_library WHERE book_id = :bookId")
//    fun getBookById(bookId: Int): LibraryObject?
    
//    @Query("SELECT * FROM book_library WHERE file_path = :filePath")
//    fun getBookByFilePath(filePath: String): LibraryObject?
    
    @Query("DELETE FROM book_library WHERE book_id = :bookId")
    fun deleteByBookId(bookId: Int)
    
    @Query("SELECT * FROM book_library WHERE id = :id")
    fun getObjectById(id: Int): LibraryObject?
    
    @Query("SELECT EXISTS(SELECT 1 FROM book_library WHERE book_id = :bookId)")
    fun checkIfDownloaded(bookId: Long): Boolean
    
    @Query("SELECT * FROM book_library WHERE book_id = :bookId")
    fun getObjectByBookId(bookId: Int): LibraryObject?
    
    // Delete any library entry that refers to a non-existent file
    @Transaction
    suspend fun cleanupMissingFiles() {
        val allBooks = getAll()
        for (book in allBooks) {
            try {
                if (!book.isExists()) {
                    delete(book)
                    Log.d("LibraryDAO", "Deleted non-existent book: ${book.title} (ID: ${book.id}, path: ${book.filePath})")
                }
            } catch (e: Exception) {
                Log.e("LibraryDAO", "Error checking/deleting book: ${book.id}", e)
            }
        }
    }
}