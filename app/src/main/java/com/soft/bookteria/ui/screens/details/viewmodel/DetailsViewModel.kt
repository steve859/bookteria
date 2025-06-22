package com.soft.bookteria.ui.screens.details.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.util.copy
import com.soft.bookteria.api.BookApi
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
    val bookDownloader: Downloader
) : ViewModel(){
    private val _uiState = MutableStateFlow(DetailsUIState())
    val uiState: StateFlow<DetailsUIState> = _uiState
    private val _isDoawnloaded = MutableStateFlow(false)
    val isDownloaded: StateFlow<Boolean> = _isDoawnloaded
    
    fun loadBookDetails(bookId: Long){
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = DetailsUIState(isLoading = true)
            try{
                val bookCollection = bookAPI.getBookById(bookId).getOrNull()!!
                //val extraInfo = bookAPI.
                _uiState.value = DetailsUIState(bookCollection = bookCollection)
            } catch (exc: Exception){
                _uiState.value = DetailsUIState(error = exc.message)
            }
        }
    }
    
    fun checkIfDownloaded(bookdId: Long){
        viewModelScope.launch(Dispatchers.IO) {
            _isDoawnloaded.value = libraryDao.checkIfDownloaded(bookdId)
        }

    }
}