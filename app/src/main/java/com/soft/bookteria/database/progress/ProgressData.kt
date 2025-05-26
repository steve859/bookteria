package com.soft.bookteria.database.progress

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Locale

@Entity(tableName = "reader_table")
data class ProgressData (
    @ColumnInfo(name = "library_object_id")
    val libraryObjectId: Int,
    @ColumnInfo(name = "last_chapter_index")
    val lastChapterIndex: Int,
    @ColumnInfo(name = "last_chapter_offset")
    val lastChapterOffset: Int,
    @ColumnInfo(name = "last_read", defaultValue = "0")
    val lastRead: Long = System.currentTimeMillis()
) {
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0

    fun getProgressPercentage(totalChapters: Int) =
        String.format(
            locale = Locale.US,
            format ="%.2f",
            ((lastChapterIndex + 1).toFloat() / totalChapters.toFloat()) * 100f
        )
        
}