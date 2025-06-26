package com.soft.bookteria.ui.screens.details.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.util.copy
import com.soft.bookteria.api.BookApi
import com.soft.bookteria.api.models.Book
import com.soft.bookteria.api.models.BookCollection
import com.soft.bookteria.database.library.LibraryDAO
import com.soft.bookteria.database.library.LibraryObject
import com.soft.bookteria.database.progress.ProgressDAO
import com.soft.bookteria.database.progress.ProgressData
import com.soft.bookteria.helpers.Downloader
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

data class DetailsUIState(
    val isLoading: Boolean = true,
    val bookCollection: BookCollection = BookCollection(0, null, null, emptyList()),
    val error: String? = null,
    val isDownloading: Boolean = false,
    val downloadMessage: String? = null
)

@HiltViewModel
class DetailsViewModel @Inject constructor(
    private val bookAPI: BookApi,
    val libraryDao: LibraryDAO,
    val progressDAO: ProgressDAO,
    val downloader: Downloader
) : ViewModel(){
    private val _uiState = MutableStateFlow(DetailsUIState())
    val uiState: StateFlow<DetailsUIState> = _uiState
    private val _isDoawnloaded = MutableStateFlow(false)
    val isDownloaded: StateFlow<Boolean> = _isDoawnloaded
    
    init {
        // Dọn dẹp và sửa chữa khi ViewModel được khởi tạo
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Dọn dẹp file tạm
                downloader.cleanupTempFiles()
                
                // Đảm bảo thư mục lưu trữ tồn tại
                val directoriesOk = downloader.ensureDirectoriesExist()
                if (!directoriesOk) {
                    Log.e("DetailsViewModel", "Failed to prepare storage directories!")
                }
                
                // Dọn dẹp cơ sở dữ liệu
                libraryDao.cleanupMissingFiles()
                
                // Sửa chữa cơ sở dữ liệu (chạy không đồng bộ)
                repairLibraryDatabase()
                
                Log.d("DetailsViewModel", "Initial cleanup and repair completed")
            } catch (e: Exception) {
                Log.e("DetailsViewModel", "Error during initial cleanup", e)
            }
        }
    }
    
    fun loadBookDetails(bookId: Long){
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = DetailsUIState(isLoading = true)
            try {
                val bookCollection = bookAPI.getBookById(bookId).getOrNull()
                if (bookCollection != null && bookCollection.books.isNotEmpty()) {
                    _uiState.value = DetailsUIState(isLoading = false, bookCollection = bookCollection)
                } else {
                    _uiState.value = DetailsUIState(
                        isLoading = false,
                        bookCollection = BookCollection(0, null, null, emptyList()),
                        error = "Book not found"
                    )
                }
            } catch (exc: Exception) {
                _uiState.value = DetailsUIState(
                    isLoading = false,
                    bookCollection = BookCollection(0, null, null, emptyList()),
                    error = exc.localizedMessage ?: "Unknown error"
                )
            }
        }
    }
    
    fun checkIfDownloaded(bookId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            // Trước tiên, dọn dẹp các mục thư viện với file không tồn tại
            try {
                libraryDao.cleanupMissingFiles()
                Log.d("DetailsViewModel", "Cleaned up missing files in library")
            } catch (e: Exception) {
                Log.e("DetailsViewModel", "Error cleaning up missing files", e)
            }
            
            // Kiểm tra lại xem sách có được tải xuống không
            val downloaded = libraryDao.checkIfDownloaded(bookId)
            Log.d("DetailsViewModel", "Book $bookId downloaded: $downloaded")
            _isDoawnloaded.value = downloaded
        }
    }

    fun downloadBook(
        book: Book,
        onResult: (success: Boolean, message: String?) -> Unit,
        downloadProgressListener: ((progress: Float, status: Int) -> Unit)? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Set downloading state to true
                _uiState.value = _uiState.value.copy(isDownloading = true)
                
                downloader.downloadBook(
                    book,
                    onResult = { success, message ->
                        // Set downloading state to false
                        _uiState.value = _uiState.value.copy(isDownloading = false)
                        
                        if (success) {
                            // Save book information to database after successful download
                            val libraryObject = LibraryObject(
                                bookId = book.id.toInt(),
                                title = book.title,
                                authors = book.authors.joinToString(", ") { it.name },
                                filePath = message ?: "", // message contains the file path
                                createAt = System.currentTimeMillis(),
                                isExternal = false
                            )
                            libraryDao.insert(libraryObject)
                            Log.d("DetailsViewModel", "Inserted libraryObject: $libraryObject")
                            // Gọi lại checkIfDownloaded để cập nhật UI ngay sau khi insert
                            checkIfDownloaded(book.id)
                            _uiState.value = _uiState.value.copy(downloadMessage = "Download completed successfully!")
                        } else {
                            _uiState.value = _uiState.value.copy(downloadMessage = "Download failed: ${message ?: "Unknown error"}")
                        }
                        onResult(success, message)
                    },
                    downloadProgressListener = downloadProgressListener
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isDownloading = false)
                onResult(false, e.localizedMessage)
            }
        }
    }

    fun clearDownloadMessage() {
        _uiState.value = _uiState.value.copy(downloadMessage = null)
    }
    
    // Không sử dụng trực tiếp, đã được thay thế bởi getLibraryObjectIdSuspend
    // Giữ lại để tương thích ngược với code khác nếu có
    fun getLibraryObjectId(bookId: Long): Int? {
        Log.d("DetailsViewModel", "WARNING: Calling deprecated getLibraryObjectId on main thread!")
        // Trả về null để buộc phải sử dụng hàm suspend thay thế
        return null
    }
    
    // Phương thức suspend để truy vấn database an toàn
    suspend fun getLibraryObjectIdSuspend(bookId: Long): Int? {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("DetailsViewModel", "Getting library object for bookId=$bookId")
                
                // Đầu tiên thực hiện dọn dẹp để đảm bảo không có mục không hợp lệ
                try {
                    libraryDao.cleanupMissingFiles()
                } catch (e: Exception) {
                    Log.e("DetailsViewModel", "Error during cleanup", e)
                }
                
                val libraryObject = libraryDao.getObjectByBookId(bookId.toInt())
                Log.d("DetailsViewModel", "Queried libraryObject for bookId=$bookId: $libraryObject")
                
                if (libraryObject == null) {
                    Log.e("DetailsViewModel", "Library object not found for bookId=$bookId")
                    return@withContext null
                }
                
                // Kiểm tra chi tiết file
                val file = File(libraryObject.filePath)
                val exists = file.exists()
                val canRead = file.canRead()
                val fileSize = file.length()
                
                Log.d("DetailsViewModel", "File check for ${libraryObject.title}: exists=$exists, canRead=$canRead, size=$fileSize bytes, path=${libraryObject.filePath}")
                
                // Check if the book file exists
                if (!libraryObject.isExists()) {
                    Log.e("DetailsViewModel", "Book file does not exist or is not readable at path: ${libraryObject.filePath}")
                    // Xóa mục không hợp lệ khỏi cơ sở dữ liệu
                    libraryDao.delete(libraryObject)
                    return@withContext null
                }
                
                libraryObject.id
            } catch (e: Exception) {
                Log.e("DetailsViewModel", "Error checking library object", e)
                null
            }
        }
    }
    
    fun getProgressData(bookId: Long): ProgressData? {
        return try {
            val libraryObject = libraryDao.getObjectByBookId(bookId.toInt())
            libraryObject?.let { libraryObj ->
                progressDAO.getProgressByLibraryObjectId(libraryObj.id)
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Sửa chữa cơ sở dữ liệu thư viện bằng cách:
     * 1. Xóa các mục không có file thực tế
     * 2. Kiểm tra tính hợp lệ của tất cả các file
     * Trả về số lượng mục đã được xóa
     */
    fun repairLibraryDatabase(): Int {
        var removedCount = 0
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Đảm bảo thư mục lưu trữ tồn tại
                downloader.ensureDirectoriesExist()
                
                // Lấy tất cả các mục thư viện
                val allBooks = libraryDao.getAll()
                Log.d("DetailsViewModel", "Starting library repair. Found ${allBooks.size} books in database")
                
                // Kiểm tra từng mục
                for (book in allBooks) {
                    val file = File(book.filePath)
                    var isValid = file.exists() && file.canRead() && file.length() > 0
                    
                    // Nếu file tồn tại, còn kiểm tra nội dung
                    if (isValid) {
                        isValid = downloader.validateEpubFile(file)
                    }
                    
                    // Nếu không hợp lệ, xóa khỏi cơ sở dữ liệu
                    if (!isValid) {
                        Log.w("DetailsViewModel", "Removing invalid book from library: ${book.title} (ID: ${book.id})")
                        libraryDao.delete(book)
                        removedCount++
                    }
                }
                
                Log.d("DetailsViewModel", "Library repair complete. Removed $removedCount invalid entries")
            } catch (e: Exception) {
                Log.e("DetailsViewModel", "Error during library repair", e)
            }
        }
        
        return removedCount
    }
}