package com.soft.bookteria.database.library

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface LibraryDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(libraryObject: LibraryObject)

    @Delete
    fun delete(libraryObject: LibraryObject)
    
    @Query("SELECT * FROM book_library")
    fun getAll(): List<LibraryObject>
    
    @Query("SELECT * FROM book_library WHERE book_id = :bookId")
    fun getBookById(bookId: Int): LibraryObject?
    
//    @Query("SELECT * FROM book_library WHERE file_path = :filePath")
//    fun getBookByFilePath(filePath: String): LibraryObject?
    
    @Query("DELETE FROM book_library WHERE book_id = :bookId")
    fun deleteByBookId(bookId: Int)
    
    @Query("SELECT * FROM book_library WHERE id = :id")
    fun getObjectById(id: Int): LibraryObject?

}