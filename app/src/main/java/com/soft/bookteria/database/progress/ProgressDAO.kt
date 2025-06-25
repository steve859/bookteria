package com.soft.bookteria.database.progress

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete
import com.soft.bookteria.database.library.LibraryObject
import kotlinx.coroutines.flow.Flow

@Dao
interface ProgressDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(progressData: ProgressData)
    
    @Query("SELECT * FROM reader_table WHERE library_object_id = :libraryObjectId")
    fun getProgressByLibraryObjectId(libraryObjectId: Int): ProgressData?
    
    @Update
    fun update(progressData: ProgressData)
    
    @Delete
    fun delete(progressData: ProgressData)
    
    @Query("DELETE FROM reader_table WHERE library_object_id = :libraryObjectId")
    fun deleteByLibraryObjectId(libraryObjectId: Int)
    
    @Query("SELECT * FROM reader_table WHERE library_object_id = :libraryObjectId")
    fun getReaderData(libraryObjectId: Int): ProgressData?
    
    @Query("SELECT * FROM reader_table")
    fun getAllReaderObjects(): List<ProgressData>
    
    @Query("SELECT * FROM reader_table WHERE library_object_id = :libraryObjectId")
    fun getReaderAsFlow(libraryObjectId: Int): Flow<ProgressData>?
    
    
}