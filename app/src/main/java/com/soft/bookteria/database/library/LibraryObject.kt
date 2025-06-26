package com.soft.bookteria.database.library

import android.icu.text.DateFormat
import android.util.Log
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
        try {
            val file = File(filePath)
            
            // Kiểm tra file có tồn tại không
            if (!file.exists()) {
                Log.e("LibraryObject", "File does not exist at path: $filePath")
                return false
            }
            
            // Kiểm tra file có thể đọc không
            if (!file.canRead()) {
                Log.e("LibraryObject", "File is not readable at path: $filePath")
                return false
            }
            
            // Kiểm tra kích thước file có > 0 không
            val fileLength = file.length()
            if (fileLength <= 0) {
                Log.e("LibraryObject", "File is empty (size=$fileLength bytes) at path: $filePath")
                return false
            }
            
            // Kiểm tra nội dung file có thể đọc được không
            try {
                file.inputStream().use { inputStream ->
                    // Đọc một số byte đầu tiên để xác nhận file có thể mở được
                    val buffer = ByteArray(1024)
                    val bytesRead = inputStream.read(buffer)
                    
                    if (bytesRead <= 0) {
                        Log.e("LibraryObject", "File cannot be read properly at path: $filePath")
                        return false
                    }
                }
            } catch (e: Exception) {
                Log.e("LibraryObject", "Error reading file content: ${e.message}", e)
                return false
            }
            
            Log.d("LibraryObject", "File check SUCCESS - Path: $filePath, Size: $fileLength bytes")
            return true
        } catch (e: Exception) {
            Log.e("LibraryObject", "Error checking if file exists: ${e.message}", e)
            return false
        }
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