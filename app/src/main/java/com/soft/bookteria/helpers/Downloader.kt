package com.soft.bookteria.helpers

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.util.Log
import com.soft.bookteria.api.models.Book
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.security.MessageDigest

class Downloader(private val context: Context) {

    companion object {
        private const val TAG = "Downloader"
        const val BOOKS_FOLDER = "ebooks"
        const val TEMP_FOLDER = "temp_books"
        private const val MAX_FILENAME_LENGTH = 100

        fun createFileName(title: String): String {
            val sanitizedTitle = title
                .replace(":", ";")
                .replace("\"", "")
                .replace("/", "／")
                .replace("\\", "＼")
                .replace("?", "")
                .replace("*", "")
                .replace("<", "")
                .replace(">", "")
                .replace("|", "")
                .trim()
                .replace("\\s+".toRegex(), "_")
            val digest = MessageDigest.getInstance("SHA-256")
            val hashBytes = digest.digest(title.toByteArray(Charsets.UTF_8))
            val hash = hashBytes.joinToString("") { "%02x".format(it) }.take(6)
            val extension = "_$hash.epub"
            val allowedTitleLength = MAX_FILENAME_LENGTH - extension.length
            val safeTitle =
                if (sanitizedTitle.isNotEmpty()) sanitizedTitle.take(allowedTitleLength) else "unknown"
            return "$safeTitle$extension"
        }
    }

    private val downloadJob = Job()
    private val downloadScope = CoroutineScope(Dispatchers.IO + downloadJob)
    private val downloadManager by lazy { context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager }

    data class DownloadInfo(
        val downloadId: Long,
        var status: Int = DownloadManager.STATUS_RUNNING,
        val progress: MutableStateFlow<Float> = MutableStateFlow(0f)
    )

    private val runningDownloads = HashMap<Long, DownloadInfo>()

    @SuppressLint("Range")
    fun downloadBook(
        book: Book,
        onResult: (Boolean, String?) -> Unit,
        downloadProgressListener: ((Float, Int) -> Unit)? = null
    ) {
        if (runningDownloads.containsKey(book.id)) {
            onResult(false, "Book is already downloading")
            return
        }
        
        // Đảm bảo các thư mục cần thiết tồn tại trước khi tải xuống
        if (!ensureDirectoriesExist()) {
            onResult(false, "Cannot prepare storage directories")
            return
        }

        val filename = createFileName(book.title)
        val tempFolder = File(context.getExternalFilesDir(null), TEMP_FOLDER)
        val tempFile = File(tempFolder, filename)
        
        // Xóa file tạm cũ nếu tồn tại
        if (tempFile.exists()) {
            try {
                tempFile.delete()
                Log.d(TAG, "Deleted existing temp file: ${tempFile.absolutePath}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete existing temp file", e)
            }
        }
        
        Log.d(TAG, "downloadBook: Destination file path: ${tempFile.absolutePath}")

        val downloadUri = Uri.parse(book.formats.applicationepubzip)
        val request = DownloadManager.Request(downloadUri)
        request.setTitle(book.title)
            .setDescription(book.authors.joinToString(", ") { it.name })
            .setDestinationUri(Uri.fromFile(tempFile))
            .setAllowedOverRoaming(true)
            .setAllowedOverMetered(true)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)

        Log.d(TAG, "downloadBook: Starting download for book: ${book.title}")
        val downloadId = downloadManager.enqueue(request)

        downloadScope.launch {
            var isDownloadFinished = false
            var progress = 0f
            var status: Int = DownloadManager.STATUS_RUNNING
            runningDownloads[book.id] = DownloadInfo(downloadId)

            while (!isDownloadFinished) {
                val cursor: Cursor =
                    downloadManager.query(DownloadManager.Query().setFilterById(downloadId))
                if (cursor.moveToFirst()) {
                    status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))
                    when (status) {
                        DownloadManager.STATUS_RUNNING -> {
                            val totalBytes: Long =
                                cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                            if (totalBytes > 0) {
                                val downloadedBytes: Long =
                                    cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                                progress = (downloadedBytes * 100 / totalBytes).toFloat() / 100
                            }
                        }
                        DownloadManager.STATUS_SUCCESSFUL -> {
                            Log.d(TAG, "downloadBook: Download successful for book: ${book.title}")
                            isDownloadFinished = true
                            progress = 1f
                            val booksFolder = File(context.filesDir, BOOKS_FOLDER)
                            if (!booksFolder.exists()) booksFolder.mkdirs()
                            val bookFile = File(booksFolder, filename)

                            // Make sure the temp file exists, is not empty, and is a valid EPUB
                            if (!tempFile.exists() || tempFile.length() <= 0) {
                                Log.e(TAG, "Downloaded file not found or empty: ${tempFile.absolutePath}")
                                onResult(false, "Downloaded file not found or empty")
                                return@launch
                            }

                            try {
                                tempFile.copyTo(bookFile, true)
                                tempFile.delete()

                                // Verify the copied file exists, is not empty, and is a valid EPUB
                                if (bookFile.exists() && bookFile.length() > 0) {
                                    // Kiểm tra tính hợp lệ của file EPUB
                                    if (validateEpubFile(bookFile)) {
                                        Log.d(TAG, "Book file successfully created and validated: ${bookFile.absolutePath}")
                                        onResult(true, bookFile.absolutePath)
                                    } else {
                                        Log.e(TAG, "Book file created but validation failed: ${bookFile.absolutePath}")
                                        bookFile.delete() // Xóa file không hợp lệ
                                        onResult(false, "Downloaded file is not a valid EPUB")
                                    }
                                } else {
                                    Log.e(TAG, "Failed to create book file: ${bookFile.absolutePath}")
                                    onResult(false, "Failed to save book file")
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error copying book file", e)
                                onResult(false, "Error saving book: ${e.message}")
                            }
                        }
                        DownloadManager.STATUS_PAUSED, DownloadManager.STATUS_PENDING -> {
                            Log.d(TAG, "downloadBook: Download pending for book: ${book.title}")
                        }
                        DownloadManager.STATUS_FAILED -> {
                            Log.d(TAG, "downloadBook: Download failed for book: ${book.title}")
                            isDownloadFinished = true
                            progress = 0f
                            onResult(false, "Download failed")
                        }
                    }
                } else {
                    Log.d(TAG, "downloadBook: Download cancelled for book: ${book.title}")
                    isDownloadFinished = true
                    progress = 0f
                    status = DownloadManager.STATUS_FAILED
                    onResult(false, "Download cancelled")
                }
                runningDownloads[book.id]?.status = status
                downloadProgressListener?.invoke(progress, status)
                runningDownloads[book.id]?.progress?.value = progress
                cursor.close()
            }
            delay(500L)
            runningDownloads.remove(book.id)
        }
    }

    // Gọi phương thức này khi ứng dụng khởi động để dọn dẹp các file tạm
    fun cleanupTempFiles() {
        try {
            val tempFolder = File(context.getExternalFilesDir(null), TEMP_FOLDER)
            if (tempFolder.exists()) {
                tempFolder.listFiles()?.forEach { file ->
                    try {
                        if (file.isFile) {
                            val deleted = file.delete()
                            Log.d(TAG, "Cleanup temp file ${file.name}: deleted=$deleted")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error deleting temp file: ${file.absolutePath}", e)
                    }
                }
            }

            // Kiểm tra và tạo thư mục sách nếu chưa tồn tại
            val booksFolder = File(context.filesDir, BOOKS_FOLDER)
            if (!booksFolder.exists()) {
                booksFolder.mkdirs()
                Log.d(TAG, "Created books folder: ${booksFolder.absolutePath}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during temp file cleanup", e)
        }
    }

    /**
     * Kiểm tra và tạo các thư mục cần thiết để lưu trữ sách
     * Trả về true nếu tất cả các thư mục đều sẵn sàng để sử dụng
     */
    fun ensureDirectoriesExist(): Boolean {
        try {
            // Kiểm tra thư mục books chính
            val booksFolder = File(context.filesDir, BOOKS_FOLDER)
            val booksFolderReady = if (!booksFolder.exists()) {
                val created = booksFolder.mkdirs()
                if (!created) {
                    Log.e(TAG, "Failed to create books directory: ${booksFolder.absolutePath}")
                }
                created
            } else {
                val canWrite = booksFolder.canWrite()
                if (!canWrite) {
                    Log.e(TAG, "Books directory is not writable: ${booksFolder.absolutePath}")
                }
                canWrite
            }

            // Kiểm tra thư mục tạm
            val tempFolder = File(context.getExternalFilesDir(null), TEMP_FOLDER)
            val tempFolderReady = if (!tempFolder.exists()) {
                val created = tempFolder.mkdirs()
                if (!created) {
                    Log.e(TAG, "Failed to create temp directory: ${tempFolder.absolutePath}")
                }
                created
            } else {
                val canWrite = tempFolder.canWrite()
                if (!canWrite) {
                    Log.e(TAG, "Temp directory is not writable: ${tempFolder.absolutePath}")
                }
                canWrite
            }

            // Ghi log kết quả
            Log.d(TAG, "Directory status - Books: $booksFolderReady, Temp: $tempFolderReady")

            return booksFolderReady && tempFolderReady
        } catch (e: Exception) {
            Log.e(TAG, "Error ensuring directories exist", e)
            return false
        }
    }

    fun isBookCurrentlyDownloading(bookId: Long) = runningDownloads.containsKey(bookId)

    fun getRunningDownload(bookId: Long) = runningDownloads[bookId]

    fun cancelDownload(downloadId: Long?) = downloadId?.let { downloadManager.remove(it) }

    /**
     * Kiểm tra tính hợp lệ cơ bản của file EPUB
     * Trả về true nếu file có vẻ là một file EPUB hợp lệ
     */
    fun validateEpubFile(file: File): Boolean {
        if (!file.exists() || !file.canRead() || file.length() <= 0) {
            Log.e(TAG, "File does not exist, is not readable, or is empty: ${file.absolutePath}")
            return false
        }
        
        try {
            // EPUB là file ZIP, nên phải có header PK
            file.inputStream().use { inputStream ->
                val header = ByteArray(4)
                val bytesRead = inputStream.read(header)
                
                if (bytesRead < 4) {
                    Log.e(TAG, "File too small to be a valid EPUB: ${file.absolutePath}")
                    return false
                }
                
                // Kiểm tra header PK\x03\x04 (magic number của ZIP file)
                val isPkHeader = header[0] == 0x50.toByte() && header[1] == 0x4B.toByte() && 
                                header[2] == 0x03.toByte() && header[3] == 0x04.toByte()
                
                if (!isPkHeader) {
                    Log.e(TAG, "File is not a valid ZIP/EPUB (invalid header): ${file.absolutePath}")
                    return false
                }
            }
            
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error validating EPUB file: ${e.message}", e)
            return false
        }
    }

    /**
     * Kiểm tra tính hợp lệ của đường dẫn file
     * Trả về đường dẫn hợp lệ hoặc null nếu không thể sửa chữa
     */
    fun validateFilePath(filePath: String): String? {
        try {
            // Kiểm tra độ dài đường dẫn
            if (filePath.length > 255) {
                Log.e(TAG, "File path too long (${filePath.length} chars): $filePath")
                return null
            }
            
            // Kiểm tra tính hợp lệ của đường dẫn
            val file = File(filePath)
            if (file.parentFile?.exists() != true) {
                Log.e(TAG, "Parent directory does not exist: ${file.parent}")
                
                // Thử tạo thư mục cha
                if (!file.parentFile?.mkdirs()!!) {
                    Log.e(TAG, "Failed to create parent directory")
                    return null
                }
            }
            
            return filePath
        } catch (e: Exception) {
            Log.e(TAG, "Error validating file path: ${e.message}", e)
            return null
        }
    }
}