package com.soft.bookteria.database.library

import android.icu.text.DateFormat
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.File
import java.io.IOException
import java.util.Date

@Entity(tableName = "book_library")
data class LibraryObject (
    @ColumnInfo(name = "book_id")
    val bookId: Int,
    @ColumnInfo(name = "title")
    val title: String,
    @ColumnInfo(name = "authors")
    val authors: String,
    @ColumnInfo(name = "file_path")
    val filePath: String,
    @ColumnInfo(name = "create_at")
    val createAt: Long,
    @ColumnInfo(name = "is_external", defaultValue = "false")
    val isExternal: Boolean
)   {
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0
    
    fun isExists(): Boolean {
        return File(filePath).exists()
    }
    
    fun deleteFile(): Boolean {
        return try{
            File(filePath).delete()
        } catch (e: IOException) {
            false
        }
    }
    
    fun getFileName(): String {
        return File(filePath).name
    }

    fun getFileSize(): String{
        val file = File(filePath)
        val bytes = file.length()
        val mb = bytes / 1000 / 1000
        val kb = bytes / 1000
        return if (bytes < 1000) {
            "$bytes B"
        } else if(bytes < 1000000) {
            "$kb KB"
        } else {
            "$mb MB"
        }
    }
    
    fun getDownloadDate(): String{
        val date = Date(createAt)
        return DateFormat.getDateInstance().format(date)
    }
}