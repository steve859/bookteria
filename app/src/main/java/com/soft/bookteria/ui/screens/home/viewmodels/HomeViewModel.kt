package com.soft.bookteria.ui.screens.home.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soft.bookteria.api.BookApi
import com.soft.bookteria.api.models.Book
import com.soft.bookteria.api.models.BookCollection
import com.soft.bookteria.helpers.Paginator
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import com.soft.bookteria.helpers.Error
import com.soft.bookteria.helpers.NetworkObserver
import kotlinx.coroutines.launch


sealed class UserAction{
    object CloseClicked : UserAction()
    object SearchClicked : UserAction()
    data class TextFieldInput(
        val text: String,
        val networkStatus: NetworkObserver.Status
    ) : UserAction()
}

@HiltViewModel
open class HomeViewModel @Inject constructor(
    private val bookAPI: BookApi
) : ViewModel() {
    var allBooksState by mutableStateOf(AllBooksStates())
    var searchBarState by mutableStateOf(SearchBarStates())
    
    private var searchJob: Job? = null
    
    private val pagination = Paginator<Long, BookCollection>(
        initialPage = 1L,
        loadPage = { page ->
            try {
                if (page == 1L) delay(400L)
                bookAPI.getAllBooks(page) // Fixed to English only
                
            } catch (exc: Exception) {
                Result.failure(exc)
            }
        },
        getNextPage = { bookCollection ->
            // Return next page number, or null if no more pages
            if (bookCollection.books.isNotEmpty()) {
                allBooksState.page + 1L
            } else {
                null
            }
        },
        onError = { error ->
            allBooksState = allBooksState.copy(
                error = error?.localizedMessage ?: Error.UNKNOWN_ERROR.message
            )
        },
        onSuccess = { bookCollection, newPage ->
            val books = run {
                val filteredBooks = bookCollection.books.filter {
                    it.formats.applicationepubzip != null
                } as ArrayList<Book>
                
                // Remove specific book with ID 1513
                val index = filteredBooks.indexOfFirst { it.id == 1513L }
                if (index != -1) {
                    filteredBooks.removeAt(index)
                }
                filteredBooks
            }
            
            allBooksState = allBooksState.copy(
                items = (allBooksState.items + books),
                page = newPage,
                endReached = books.isEmpty()
            )
        },
        onLoadingChanged = { isLoading ->
            allBooksState = allBooksState.copy(isLoading = isLoading)
        }
    )
    
    fun loadNextItems() {
        viewModelScope.launch {
            pagination.loadNextItems()
        }
    }
    
    fun reloadItems() {
        pagination.reset()
        allBooksState = AllBooksStates()
        loadNextItems()
    }
    
    fun onAction(userAction: UserAction) {
        when (userAction) {
            UserAction.CloseClicked -> {
                searchBarState = searchBarState.copy(isVisible = false)
            }
            
            UserAction.SearchClicked -> {
                searchBarState = searchBarState.copy(isVisible = true)
            }
            
            is UserAction.TextFieldInput -> {
                searchBarState = searchBarState.copy(text = userAction.text)
                if (userAction.networkStatus == NetworkObserver.Status.Avaiable) {
                    searchJob?.cancel()
                    searchJob = viewModelScope.launch {
                        if (userAction.text.isNotBlank()) {
                            searchBarState = searchBarState.copy(isSearching = true)
                        }
                        delay(500L)
                        searchBooks(userAction.text)
                    }
                }
            }
        
        }
    }
    
    private suspend fun searchBooks(query: String) {
        if (query.isBlank()) return
        val bookSet = bookAPI.searchBooks(query)
        val books = bookSet.getOrNull()?.books?.filter { it.formats.applicationepubzip != null }
        searchBarState = searchBarState.copy(
            returnedBookList = books ?: emptyList(),
            isSearching = false
        )
    }
}

data class AllBooksStates(
    val isLoading: Boolean = false,
    val items: List<Book> = emptyList(),
    val error: String? = null,
    val endReached: Boolean = false,
    val page: Long = 1L
)

data class SearchBarStates(
    val text: String = "",
    val isHintVisible: Boolean = false,
    val isSearching: Boolean = false,
    val returnedBookList: List<Book> = emptyList(),
    val isVisible: Boolean = false
)

