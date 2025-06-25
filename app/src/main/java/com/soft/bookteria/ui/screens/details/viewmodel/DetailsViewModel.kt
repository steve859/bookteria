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
    
    fun getLibraryObjectId(bookId: Long): Int? {
        return try {
            val libraryObject = libraryDao.getObjectByBookId(bookId.toInt())
            Log.d("DetailsViewModel", "Queried libraryObject for bookId=$bookId: $libraryObject")
            libraryObject?.id
        } catch (e: Exception) {
            null
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
}