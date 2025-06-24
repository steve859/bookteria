package com.soft.bookteria.ui.screens.details.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.util.copy
import com.soft.bookteria.api.BookApi
import com.soft.bookteria.api.models.Book
import com.soft.bookteria.api.models.BookCollection
import com.soft.bookteria.database.library.LibraryDAO
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
    val error: String? = null
)

@HiltViewModel
class DetailsViewModel @Inject constructor(
    private val bookAPI: BookApi,
    val libraryDao: LibraryDAO,
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
    
    fun checkIfDownloaded(bookdId: Long){
        viewModelScope.launch(Dispatchers.IO) {
            _isDoawnloaded.value = libraryDao.checkIfDownloaded(bookdId)
        }
    }

    fun downloadBook(
        book: Book,
        onResult: (success: Boolean, message: String?) -> Unit,
        downloadProgressListener: ((progress: Float, status: Int) -> Unit)? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                downloader.downloadBook(
                    book,
                    onResult = { success, message ->
                        if (success) _isDoawnloaded.value = true
                        onResult(success, message)
                    },
                    downloadProgressListener = downloadProgressListener
                )
            } catch (e: Exception) {
                onResult(false, e.localizedMessage)
            }
        }
    }
}