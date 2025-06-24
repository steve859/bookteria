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

        val filename = createFileName(book.title)
        val tempFolder = File(context.getExternalFilesDir(null), TEMP_FOLDER)
        if (!tempFolder.exists()) tempFolder.mkdirs()
        val tempFile = File(tempFolder, filename)
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
                            tempFile.copyTo(bookFile, true)
                            tempFile.delete()
                            onResult(true, bookFile.absolutePath)
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

    fun isBookCurrentlyDownloading(bookId: Long) = runningDownloads.containsKey(bookId)

    fun getRunningDownload(bookId: Long) = runningDownloads[bookId]

    fun cancelDownload(downloadId: Long?) = downloadId?.let { downloadManager.remove(it) }
}